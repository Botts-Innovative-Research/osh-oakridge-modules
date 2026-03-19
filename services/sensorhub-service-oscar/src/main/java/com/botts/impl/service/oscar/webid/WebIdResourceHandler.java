package com.botts.impl.service.oscar.webid;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.botts.impl.service.bucket.util.InvalidRequestException;
import com.botts.impl.service.bucket.util.MultipartRequestParser;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.oscar.cambio.CambioConverter;
import com.botts.impl.system.lane.LaneSystem;
import com.google.gson.JsonArray;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.utils.OshAsserts;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.sensorhub.impl.utils.rad.output.N42Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

public class WebIdResourceHandler extends DefaultObjectHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebIdResourceHandler.class);
    private static final Set<String> WEB_ID_FILE_EXTENSIONS = new CambioConverter().getSupportedInputFormats();

    private static final String RADDATA_PREFIX = "RADDATA://";

    private final Pattern pattern;
    private final ISensorHub hub;
    private final WebIdClient webIdClient;
    private final IdEncoder obsIdEncoder;

    public WebIdResourceHandler(IBucketStore bucketStore, ISensorHub hub, WebIdClient webIdClient) {
        super(bucketStore);
        this.hub = hub;
        this.webIdClient = webIdClient;
        this.obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();

        String joinedExtensions = String.join("|", WEB_ID_FILE_EXTENSIONS);
        String regex = new StringBuilder(".*\\.(")
                .append(joinedExtensions)
                .append(")")
                .toString();
        pattern = Pattern.compile(regex);
    }

    private static class WebIdRequestContext {
        RequestContext parentContext;
        String occupancyObsId;
        String laneUid;
        String drf;
        boolean webIdEnabled;
        boolean synthesizeBackground;

        String foregroundFileName = null;
        byte[] foregroundData = null;
        String backgroundFileName = null;
        byte[] backgroundData = null;

        public WebIdRequestContext(RequestContext ctx) {
            parentContext = ctx;
            occupancyObsId = ctx.getRequest().getParameter("occupancyObsId");
            laneUid = ctx.getRequest().getParameter("laneUid");
            String webIdEnabledParam = ctx.getRequest().getParameter("webIdEnabled");
            webIdEnabled = Boolean.parseBoolean(webIdEnabledParam);
            drf = ctx.getRequest().getParameter("drf");
            String synthesizeBackgroundParam = ctx.getRequest().getParameter("synthesizeBackground");
            synthesizeBackground = Boolean.parseBoolean(synthesizeBackgroundParam);

            if (ctx.isMultipartRequest()) {
                try (var parseResult = MultipartRequestParser.parse(ctx.getRequest())) {

                    for (var file : parseResult.files()) {
                        String fileName = file.fileName();
                        String fieldName = file.fieldName();

                        if ("foreground".equals(fieldName)) {
                            foregroundFileName = fileName;
                            try (var fileStream = file.inputStream()) {
                                foregroundData = fileStream.readAllBytes();
                            }
                        } else if ("background".equals(fieldName)) {
                            backgroundFileName = fileName;
                            try (var fileStream = file.inputStream()) {
                                backgroundData = fileStream.readAllBytes();
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.error("Unable to parse multipart data", e);
                }
            } else {
                try (var contextInputStream = ctx.getRequest().getInputStream()) {
                    foregroundData = contextInputStream.readAllBytes();
                    if (ctx.hasObjectKey())
                        foregroundFileName = ctx.getObjectKey();
                } catch (IOException e) {
                    logger.error("Unable to retrieve request context input stream", e);
                }
            }
        }

        public boolean hasForegroundFileName() {
            return foregroundFileName != null && !foregroundFileName.isBlank();
        }

        public boolean hasBackgroundFileName() {
            return backgroundFileName != null && !backgroundFileName.isBlank();
        }
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = ctx.getObjectKey();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).put);

        // Extract query parameters
        var webIdContext = new WebIdRequestContext(ctx);

        if (!webIdContext.webIdEnabled)
            return;

        // PUT should only update one resource, so prioritize POST for FG + BG
        if (webIdContext.foregroundData != null && webIdContext.hasForegroundFileName()) {
            // Update the object in the bucket
            try {
                bucketStore.putObject(bucketName, objectKey, new ByteArrayInputStream(webIdContext.foregroundData), ctx.getHeaders());
            } catch (DataStoreException e) {
                throw new IOException(IBucketStore.FAILED_PUT_OBJECT + bucketName, e);
            }
        }

        ctx.getResponse().setStatus(HttpServletResponse.SC_OK);

        // Check if web ID is reachable
        if (webIdClient != null && webIdClient.isReachable()) {
            processWebIdAnalysis(webIdContext);
        } else {
            // offline conversion to N42 only
//            processCambioConversion(webIdContext);
        }
        publishN42(webIdContext);
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).create);

        // Extract query parameters
        var webIdContext = new WebIdRequestContext(ctx);

        if (!webIdContext.webIdEnabled)
            return;

        List<String> resourceURIs = new ArrayList<>();

        // Save background data in bucket
        if (webIdContext.backgroundData != null
                // ensure we have a filename for put requests
                && webIdContext.backgroundFileName != null
                && !webIdContext.backgroundFileName.isBlank()) {
            try {
                bucketStore.putObject(bucketName, webIdContext.backgroundFileName,
                        new ByteArrayInputStream(webIdContext.backgroundData), ctx.getHeaders());
                // add to generated resource URI list
                resourceURIs.add(bucketStore.getRelativeResourceURI(bucketName, webIdContext.backgroundFileName));
            } catch (DataStoreException e) {
                logger.error("Failed to put background data from {} in bucket store", webIdContext.backgroundFileName, e);
            }
        }

        // For POST, we can have foreground data with no filename (i.e. QR code)
        if (webIdContext.foregroundData != null) {
            try {
                // put if foreground + background
                if (webIdContext.hasForegroundFileName()) {
                    bucketStore.putObject(bucketName, webIdContext.foregroundFileName,
                            new ByteArrayInputStream(webIdContext.foregroundData), ctx.getHeaders());
                    // add to generated resource URI list
                    resourceURIs.add(bucketStore.getRelativeResourceURI(bucketName, webIdContext.foregroundFileName));
                } else {
                    // create object with random UUID if just foreground data
                    String newObjectKey = bucketStore.createObject(bucketName,
                            new ByteArrayInputStream(webIdContext.foregroundData), ctx.getHeaders());
                    if (newObjectKey == null || newObjectKey.isBlank())
                        throw new IOException(IBucketStore.FAILED_CREATE_OBJECT + bucketName);

                    // set location header for single resource
                    ctx.getResponse().setHeader("Location", bucketStore.getRelativeResourceURI(bucketName, newObjectKey));
                }
                ctx.getResponse().setStatus(HttpServletResponse.SC_CREATED);
            } catch (Exception e) {
                throw new IOException("Failed to put foreground data in bucket store", e);
            }
        }

        if (!resourceURIs.isEmpty()) {
            JsonArray jsonArray = new JsonArray();
            for (var resourceURI : resourceURIs)
                jsonArray.add(resourceURI);
            ctx.getResponse().getWriter().write(jsonArray.toString());
        }

        // Check if web ID is reachable
        if (webIdClient != null && webIdClient.isReachable()) {
            processWebIdAnalysis(webIdContext);
        } else {
            // offline conversion to N42 only
//            processCambioConversion(webIdContext);
        }
        publishN42(webIdContext);
    }

    private void processCambioConversion(WebIdRequestContext webIdContext) {
        // TODO make sure nothing special required for cambio conversion
    }

    private void publishN42(WebIdRequestContext webIdContext) {
        var parentContext = webIdContext.parentContext;
        String key = parentContext.getObjectKey();
        N42Output<?> n42output;
        String charset = parentContext.getRequest().getCharacterEncoding() != null ? parentContext.getRequest().getCharacterEncoding() : StandardCharsets.UTF_8.name();

        // Grab corresponding lane n42 output
        try {
            n42output = (N42Output<?>) ((AbstractSensorModule<?>) hub.getModuleRegistry().getLoadedModules().stream().filter(module -> {
                try {
                    if (module instanceof AbstractSensorModule<?> sensor) {
                        return sensor.getUniqueIdentifier().equals(webIdContext.laneUid);
                    }
                } catch (Exception ignored) {}
                return false;
            }).toList().get(0)).getOutputs().get(N42Output.SENSOR_OUTPUT_NAME);
        } catch (Exception e) {
            logger.error("Failed to get lane module for UID: {}", webIdContext.laneUid, e);
            return;
        }

        // Multipart
        if (parentContext.isMultipartRequest()) {
            try {
                if (webIdContext.hasForegroundFileName()) {
                    n42output.onNewMessage(new String(webIdContext.foregroundData, charset));
                }
                if (webIdContext.hasBackgroundFileName()) {
                    n42output.onNewMessage(new String(webIdContext.backgroundData, charset));
                }
            } catch (Exception e) {
                logger.error("Failed to parse multipart request", e);
            }
        } else {
            if (key.endsWith(".n42")) {
                try {
                    n42output.onNewMessage(new String(webIdContext.foregroundData, charset));
                } catch (Exception e) {
                    logger.error("Failed to parse N42 file: {}", key, e);
                }
            }
        }
    }

    private void processWebIdAnalysis(WebIdRequestContext webIdContext) {
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
                // TODO maybe n42 pub here
                logger.info("WebID analysis succeeded with raw data");
            } catch (Exception e) {
                // If QR code data failed, don't try Cambio conversion - just rethrow
                if (isQrCodeData)
                    throw new IOException("WebID analysis failed for QR code data", e);

                // For other formats, try converting to N42 via Cambio and retry
                logger.info("WebID analysis failed with raw data, attempting Cambio conversion: {}", e.getMessage());

                byte[] n42Bytes;
                if (webIdContext.hasForegroundFileName() && (webIdContext.foregroundFileName.endsWith(".n42")
                        || webIdContext.foregroundFileName.endsWith(".xml"))) {
                    n42Bytes = webIdContext.foregroundData;
                } else {
                    String fileName;
                    if (webIdContext.foregroundFileName != null && !webIdContext.foregroundFileName.isBlank()) {
                        fileName = webIdContext.foregroundFileName;
                    } else {
                        fileName = UUID.randomUUID() + ".n42";
                    }
                    n42Bytes = new CambioConverter().convertToN42(new ByteArrayInputStream(webIdContext.foregroundData), fileName);
                }

                // TODO n42 pub here

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

            // Save analysis JSON to bucket store
            try {
                String analysisJson = analysis.toString();
                Map<String, String> jsonMetadata = new HashMap<>();
                jsonMetadata.put("Content-Type", "application/json");
                bucketStore.createObject(webIdContext.parentContext.getBucketName(), new ByteArrayInputStream(analysisJson.getBytes()), jsonMetadata);
            } catch (DataStoreException e) {
                logger.error("Failed to store WebId analysis JSON in bucket", e);
            }

            // Insert WebId observation into lane database
            String encodedWebIdObsId = null;
            if (webIdContext.laneUid != null && !webIdContext.laneUid.isBlank()) {
                try {
                    var laneDb = hub.getSystemDriverRegistry().getDatabase(webIdContext.laneUid);
                    if (laneDb == null) {
                        logger.error("No database found for lane UID: {}", webIdContext.laneUid);
                        return;
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
                        return;
                    }

                    var dsId = dsKeys.get(0).getInternalID();

                    // Create DataBlock from analysis
                    DataBlock webIdDataBlock = WebIdAnalysis.fromWebIdAnalysis(analysis);

                    // Insert observation
                    var obsId = obsStore.add(new ObsData.Builder()
                            .withDataStream(dsId)
                            .withPhenomenonTime(Instant.now())
                            .withResultTime(Instant.now())
                            .withResult(webIdDataBlock)
                            .build());

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
                        return;
                    }

                    var obsStore = laneDb.getObservationStore();
                    var obs = obsStore.get(decodedOccObsId);
                    if (obs == null) {
                        logger.error("Occupancy observation not found for ID: {}", webIdContext.occupancyObsId);
                        return;
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
     * Checks if the data is QR code data in RADDATA:// format.
     */
    private boolean isRadDataQrCode(byte[] data) {
        if (data == null || data.length < RADDATA_PREFIX.length()) {
            return false;
        }
        String prefix = new String(data, 0, RADDATA_PREFIX.length(), StandardCharsets.UTF_8);
        return RADDATA_PREFIX.equals(prefix);
    }

    /**
     * Returns true if this handler should handle the request based on the webIdEnabled query parameter.
     * This allows QR code data to be POSTed directly to the bucket root without specifying a filename.
     */
    @Override
    public boolean canHandleRequest(HttpServletRequest request) {
        String webIdEnabledParam = request.getParameter("webIdEnabled");
        return Boolean.parseBoolean(webIdEnabledParam);
    }

    @Override
    public String getObjectPattern() {
        return this.pattern.pattern();
    }
}
