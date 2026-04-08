package org.sensorhub.impl.utils.rad.webid;

import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.impl.utils.rad.cambio.*;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.sensorhub.impl.utils.rad.output.N42Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WebIdAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(WebIdAnalyzer.class);
    ISensorHub hub;
    //IBucketStore bucketStore;
    WebIdClient webIdClient;
    IdEncoder obsIdEncoder;

    private static final String RADDATA_PREFIX = "RADDATA://";

    public WebIdAnalyzer(WebIdClient webIdClient, ISensorHub hub) {
        this.hub = hub;
        //this.bucketStore = bucketStore;
        this.webIdClient = webIdClient;
        this.obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();
    }

    private void publishN42(WebIdRequestContext webIdContext) {
        String key = webIdContext.getObjectKey();
        N42Output<?> n42output;
        String charset = StandardCharsets.UTF_8.name();
        String occupancyObsId = webIdContext.occupancyObsId;

        if (!isN42(key) && !webIdContext.isMultipartRequest())
            return;

        // Grab corresponding lane n42 output
        try {
            n42output = ((N42Output<?>) ((SensorSystem) hub.getModuleRegistry().getLoadedModules().stream().filter(module -> {
                try {
                    if (module instanceof SensorSystem system) {
                        return system.getUniqueIdentifier().equals(webIdContext.laneUid);
                    }
                } catch (Exception ignored) {}
                return false;
            }).toList().get(0)).getOutputs().get(N42Output.SENSOR_OUTPUT_NAME));
        } catch (Exception e) {
            logger.error("Failed to get lane module for UID: {}", webIdContext.laneUid, e);
            return;
        }

        Path objectDir = Path.of(webIdContext.getDirectory());
        // Multipart
        if (webIdContext.isMultipartRequest()) {
            try {
                if (webIdContext.hasForegroundFileName() && isN42(webIdContext.foregroundFileName)) {
                    n42output.onNewMessage(new String(webIdContext.foregroundData, charset), objectDir.resolve(webIdContext.getForegroundFileName()).toString(), occupancyObsId);
                }
                if (webIdContext.hasBackgroundFileName() && isN42(webIdContext.backgroundFileName)) {
                    n42output.onNewMessage(new String(webIdContext.backgroundData, charset), objectDir.resolve(webIdContext.getBackgroundFileName()).toString(), occupancyObsId);
                }
            } catch (Exception e) {
                logger.error("Failed to parse multipart request", e);
            }
        } else {
            if (key.endsWith(".n42")) {
                try {
                    n42output.onNewMessage(new String(webIdContext.foregroundData, charset), objectDir.resolve(key).toString(), occupancyObsId);
                } catch (Exception e) {
                    logger.error("Failed to parse N42 file: {}", key, e);
                }
            }
        }
    }

    private void publishN42(String laneUid, String n42Data, String fileName, String occupancyObsId) {
        if (!isN42(fileName))
            return;

        N42Output<?> n42output;
        try {
            n42output = (N42Output<?>) ((AbstractSensorModule<?>) hub.getModuleRegistry().getLoadedModules().stream().filter(module -> {
                try {
                    if (module instanceof AbstractSensorModule<?> sensor) {
                        return sensor.getUniqueIdentifier().equals(laneUid);
                    }
                } catch (Exception ignored) {}
                return false;
            }).toList().get(0)).getOutputs().get(N42Output.SENSOR_OUTPUT_NAME);
        } catch (Exception e) {
            logger.error("Failed to get lane module for UID: {}", laneUid, e);
            return;
        }
        try {
            n42output.onNewMessage(n42Data, fileName, occupancyObsId);
        } catch (Exception e) {
            logger.error("Failed to parse N42 file: {}", laneUid, e);
        }
    }

    public WebIdAnalysis processWebIdRequest(WebIdRequestContext webIdContext) {
        // Check if web ID is reachable
        WebIdAnalysis analysis = null;
        boolean reachable = webIdClient != null && webIdClient.isReachable();
        logger.info("WebID reachability check: client={}, reachable={}", webIdClient != null ? "present" : "null", reachable);
        if (reachable) {
            analysis = processWebIdAnalysis(webIdContext);
        } else {
            // offline: convert to N42 via Cambio if needed, then publish
            logger.info("WebID unreachable, entering offline conversion path (objectKey={}, foregroundFileName={})",
                    webIdContext.getObjectKey(), webIdContext.getForegroundFileName());
            processOfflineConversion(webIdContext);
        }
        return analysis;
    }

    private void processOfflineConversion(WebIdRequestContext webIdContext) {
        // For multipart POSTs the URL object key is blank; the real filename lives on the multipart part.
        String effectiveName = webIdContext.hasForegroundFileName()
                ? webIdContext.getForegroundFileName()
                : webIdContext.getObjectKey();
        boolean alreadyN42 = effectiveName != null && !effectiveName.isBlank() && isN42(effectiveName);

        if (alreadyN42) {
            publishN42(webIdContext);
            return;
        }

        // Non-N42 file: convert via Cambio then publish
        if (webIdContext.foregroundData == null) {
            logger.warn("No foreground data available for offline Cambio conversion");
            return;
        }

        logger.info("Starting Cambio conversion: foregroundData={} bytes, foregroundFileName={}, objectKey={}",
                webIdContext.foregroundData.length, webIdContext.foregroundFileName, webIdContext.getObjectKey());

        try {
            String sourceFileName = (effectiveName != null && !effectiveName.isBlank())
                    ? effectiveName
                    : UUID.randomUUID().toString();
            logger.info("Calling CambioConverter.convertToN42 with sourceFileName={}", sourceFileName);
            byte[] n42Bytes = new CambioConverter().convertToN42(new ByteArrayInputStream(webIdContext.foregroundData), sourceFileName);
            logger.info("Cambio conversion returned {} bytes", n42Bytes != null ? n42Bytes.length : 0);
            // Use .n42 extension so publishN42 accepts it
            String n42FileName = sourceFileName.replaceAll("\\.[^.]+$", ".n42");
            logger.info("Cambio conversion succeeded for offline file: {} -> {}", sourceFileName, n42FileName);
            String sanitized = sanitizeInterSpecNamespace(new String(n42Bytes, StandardCharsets.UTF_8));
            publishN42(webIdContext.laneUid, sanitized, n42FileName, webIdContext.occupancyObsId);
        } catch (Throwable e) {
            logger.error("Cambio conversion failed for offline file: {}", effectiveName, e);
        }
    }

    private WebIdAnalysis processWebIdAnalysis(WebIdRequestContext webIdContext) {

        try {
            if (webIdContext.foregroundData == null)
                throw new IllegalArgumentException("Foreground data is null");

            // Check if this is QR code data (RADDATA:// format)
            boolean isQrCodeData = isRadDataQrCode(webIdContext.foregroundData);

            WebIdAnalysis analysis;
            // Try sending to WebID first with raw data
            try {
                var request = createWebIdRequest(webIdContext);
                analysis = webIdClient.analyze(request);
                publishN42(webIdContext);
                logger.info("WebID analysis succeeded with raw data");
            } catch (Exception e) {
                // If QR code data failed, don't try Cambio conversion - just rethrow
                if (isQrCodeData)
                    throw new IOException("WebID analysis failed for QR code data", e);

                // For other formats, try converting to N42 via Cambio and retry
                logger.info("WebID analysis failed with raw data, attempting Cambio conversion: {}", e.getMessage());

                byte[] n42Bytes;
                String fileName;
                if (webIdContext.hasForegroundFileName() && isN42(webIdContext.foregroundFileName)) {
                    n42Bytes = webIdContext.foregroundData;
                    fileName = webIdContext.foregroundFileName;
                } else {
                    String sourceFileName;
                    if (webIdContext.foregroundFileName != null && !webIdContext.foregroundFileName.isBlank()) {
                        sourceFileName = webIdContext.foregroundFileName;
                    } else {
                        sourceFileName = UUID.randomUUID().toString();
                    }
                    n42Bytes = new CambioConverter().convertToN42(new ByteArrayInputStream(webIdContext.foregroundData), sourceFileName);
                    // Use .n42 extension so publishN42 accepts it
                    fileName = sourceFileName.replaceAll("\\.[^.]+$", "") + ".n42";
                }

                publishN42(webIdContext.laneUid, sanitizeInterSpecNamespace(new String(n42Bytes, StandardCharsets.UTF_8)), fileName, webIdContext.occupancyObsId);

                WebIdRequest.Builder requestBuilder = new WebIdRequest.Builder()
                        .foreground(new ByteArrayInputStream(n42Bytes))
                        .drf(webIdContext.drf);

                if (webIdContext.backgroundData != null)
                    requestBuilder.background(new ByteArrayInputStream(webIdContext.backgroundData));

                // client chooses whether to synthesize background data
                requestBuilder.synthesizeBackground(webIdContext.synthesizeBackground);

                analysis = webIdClient.analyze(requestBuilder.build());
                logger.info("WebID analysis succeeded after Cambio conversion");
            }

            analysis.setSampleTime(Instant.now());
            analysis.setOccupancyObsId(webIdContext.occupancyObsId != null ? webIdContext.occupancyObsId : "");
            /*
            try {
                analysisJson = analysis.toString();
                Map<String, String> jsonMetadata = new HashMap<>();
                jsonMetadata.put("Content-Type", "application/json");
                //bucketStore.createObject(webIdContext.getBucketName(), new ByteArrayInputStream(analysisJson.getBytes()), jsonMetadata);
            } catch (DataStoreException e) {
                logger.error("Failed to store WebId analysis JSON in bucket", e);
            }

             */

            // Insert WebId observation into lane database
            String encodedWebIdObsId = null;
            if (webIdContext.laneUid != null && !webIdContext.laneUid.isBlank()) {
                try {
                    var laneDb = hub.getSystemDriverRegistry().getDatabase(webIdContext.laneUid);
                    if (laneDb == null) {
                        logger.error("No database found for lane UID: {}", webIdContext.laneUid);
                        return analysis;
                    }

                    var obsStore = laneDb.getObservationStore();

                    // Find the WebId datastream
                    var dsKeys = obsStore.getDataStreams().selectKeys(new DataStreamFilter.Builder()
                            .withSystems()
                            .withUniqueIDs(webIdContext.laneUid)
                            .done()
                            .withOutputNames("webIdAnalysis")
                            .build()).toList();

                    if (dsKeys.isEmpty()) {
                        logger.error("No webIdAnalysis datastream found for lane: {}", webIdContext.laneUid);
                        return analysis;
                    }

                    var dsId = dsKeys.get(0).getInternalID();

                    // Create DataBlock from analysis
                    DataBlock webIdDataBlock = WebIdAnalysis.fromWebIdAnalysis(analysis);

                    // Insert observation
                    var txnHandler = new SystemDatabaseTransactionHandler(hub.getEventBus(), laneDb);
                    var obsId = txnHandler.getDataStreamHandler(dsId).addObs(webIdDataBlock);

                    encodedWebIdObsId = obsIdEncoder.encodeID(obsId);
                    logger.info("WebId analysis observation stored with ID: {}", encodedWebIdObsId);
                } catch (Exception e) {
                    logger.error("Failed to insert WebId observation into lane database", e);
                }
            }

            // Attach WebId obs ID to occupancy observation
            if (encodedWebIdObsId != null && webIdContext.occupancyObsId != null && !webIdContext.occupancyObsId.isBlank() && webIdContext.laneUid != null) {
                try {
                    BigId decodedOccObsId = obsIdEncoder.decodeID(webIdContext.occupancyObsId);
                    var laneDb = hub.getSystemDriverRegistry().getDatabase(webIdContext.laneUid);
                    if (laneDb == null) {
                        logger.error("No database found for lane UID when updating occupancy: {}", webIdContext.laneUid);
                        return analysis;
                    }

                    var obsStore = laneDb.getObservationStore();
                    var obs = obsStore.get(decodedOccObsId);
                    if (obs == null) {
                        logger.error("Occupancy observation not found for ID: {}", webIdContext.occupancyObsId);
                        return analysis;
                    }

                    var occupancy = Occupancy.toOccupancy(obs.getResult());
                    occupancy.addWebIdObsId(encodedWebIdObsId);

                    DataBlock newOccupancyResult = Occupancy.fromOccupancy(occupancy);
                    obs.getResult().setUnderlyingObject(newOccupancyResult.getUnderlyingObject());
                    obs.getResult().updateAtomCount();

                    obsStore.put(decodedOccObsId, obs);
                    logger.info("Occupancy observation {} updated with WebId obs ID: {}", webIdContext.occupancyObsId, encodedWebIdObsId);
                } catch (Exception e) {
                    logger.error("Failed to update occupancy observation with WebId obs ID", e);
                }
            }
        } catch (Exception e) {
            logger.error("WebId analysis processing failed", e);
        }
        return null;
    }

    private WebIdRequest createWebIdRequest(WebIdRequestContext webIdContext) {
        WebIdRequest.Builder requestBuilder = new WebIdRequest.Builder()
                .foreground(new ByteArrayInputStream(webIdContext.foregroundData));

        // include background but if no background data then we need to synthesize it
        if (webIdContext.backgroundData != null)
            requestBuilder.background(new ByteArrayInputStream(webIdContext.backgroundData));

        // client chooses whether to synthesize background data
        requestBuilder.synthesizeBackground(webIdContext.synthesizeBackground);

        if (webIdContext.drf != null && !webIdContext.drf.isBlank())
            requestBuilder.drf(webIdContext.drf);

        return requestBuilder.build();
    }

    /**
     * SpecUtils-generated N42 files include InterSpec-specific extension elements with an
     * "InterSpec:" prefix that is never declared as a namespace, causing JAXB unmarshal to fail.
     * Strip those elements (and any stray xmlns:InterSpec attributes) before parsing.
     */
    private String sanitizeInterSpecNamespace(String n42Xml) {
        if (n42Xml == null || n42Xml.isEmpty())
            return n42Xml;
        // Remove paired <InterSpec:Foo ...>...</InterSpec:Foo> elements (DOTALL for multi-line)
        String cleaned = n42Xml.replaceAll("(?s)<InterSpec:[^/>]*?>.*?</InterSpec:[^>]*?>", "");
        // Remove self-closing <InterSpec:Foo .../>
        cleaned = cleaned.replaceAll("<InterSpec:[^>]*?/>", "");
        // Remove any leftover xmlns:InterSpec="..." attributes
        cleaned = cleaned.replaceAll("\\s+xmlns:InterSpec=\"[^\"]*\"", "");
        return cleaned;
    }

    private boolean isN42(String fileName) {
        return fileName.endsWith(".n42") || fileName.endsWith(".xml");
    }

    /**
     * Checks if the data is QR code data in RADDATA:// format.
     */
    private boolean isRadDataQrCode(byte[] data) {
        if (data == null || data.length < RADDATA_PREFIX.length()) {
            return false;
        }
        String prefix = new String(data, 0, RADDATA_PREFIX.length(), StandardCharsets.UTF_8);
        return RADDATA_PREFIX.equals(prefix);
    }
}
