package com.botts.impl.service.oscar.webid;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.botts.impl.service.bucket.util.MultipartRequestParser;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.oscar.cambio.CambioConverter;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = ctx.getObjectKey();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).put);

        // Extract query parameters
        String occupancyObsId = ctx.getRequest().getParameter("occupancyObsId");
        String laneUid = ctx.getRequest().getParameter("laneUid");
        String webIdEnabledParam = ctx.getRequest().getParameter("webIdEnabled");
        boolean webIdEnabled = Boolean.parseBoolean(webIdEnabledParam);
        String drf = ctx.getRequest().getParameter("drf");

        // Buffer the file bytes for both storage and analysis
        byte[] fileBytes;
        String filename = null;

        if (ctx.isMultipartRequest()) {
            try (var parseResult = MultipartRequestParser.parse(ctx.getRequest())) {
                var file = parseResult.files().get(0);
                filename = file.fileName();
                fileBytes = file.inputStream().readAllBytes();
            }
        } else {
            fileBytes = ctx.getRequest().getInputStream().readAllBytes();
        }

        // Update the object in the bucket
        try {
            bucketStore.putObject(bucketName, objectKey, new ByteArrayInputStream(fileBytes), ctx.getHeaders());
        } catch (DataStoreException e) {
            throw new IOException(IBucketStore.FAILED_PUT_OBJECT + bucketName, e);
        }

        ctx.getResponse().setStatus(HttpServletResponse.SC_OK);

        // If WebId is enabled and client is available, process synchronously
        if (webIdEnabled && webIdClient != null) {
            processWebIdAnalysis(fileBytes, filename, laneUid, occupancyObsId, bucketName, drf);
        }
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).create);

        // Extract query parameters
        String occupancyObsId = ctx.getRequest().getParameter("occupancyObsId");
        String laneUid = ctx.getRequest().getParameter("laneUid");
        String webIdEnabledParam = ctx.getRequest().getParameter("webIdEnabled");
        boolean webIdEnabled = Boolean.parseBoolean(webIdEnabledParam);
        String drf = ctx.getRequest().getParameter("drf");

        // Buffer the file bytes for both storage and analysis
        byte[] fileBytes;
        String filename = null;

        if (ctx.isMultipartRequest()) {
            try (var parseResult = MultipartRequestParser.parse(ctx.getRequest())) {
                var file = parseResult.files().get(0);
                filename = file.fileName();
                fileBytes = file.inputStream().readAllBytes();
            }
        } else {
            fileBytes = ctx.getRequest().getInputStream().readAllBytes();
        }

        // Store the file in the bucket
        String newObjectKey;
        try {
            newObjectKey = bucketStore.createObject(bucketName, new ByteArrayInputStream(fileBytes), ctx.getHeaders());
        } catch (DataStoreException e) {
            throw new IOException(IBucketStore.FAILED_CREATE_OBJECT + bucketName, e);
        }

        if (newObjectKey == null)
            throw new IOException(IBucketStore.FAILED_CREATE_OBJECT + bucketName);

        ctx.getResponse().setStatus(HttpServletResponse.SC_CREATED);
        ctx.getResponse().setHeader("Location", ctx.getResourceURL(newObjectKey));

        // If WebID is enabled and client is available, process synchronously
        if (webIdEnabled && webIdClient != null) {
            processWebIdAnalysis(fileBytes, filename, laneUid, occupancyObsId, bucketName, drf);
        }
    }

    private void processWebIdAnalysis(byte[] fileBytes, String filename, String laneUid, String occupancyObsId, String bucketName, String drf) {
        try {
            // Check if this is QR code data (RADDATA:// format)
            boolean isQrCodeData = isRadDataQrCode(fileBytes);

            WebIdAnalysis analysis = null;

            // Try sending to WebID first with raw data
            try {
                WebIdRequest.Builder requestBuilder = new WebIdRequest.Builder()
                        .foreground(new ByteArrayInputStream(fileBytes))
                        .synthesizeBackground(true);

                if (drf != null && !drf.isBlank()) {
                    requestBuilder.drf(drf);
                }

                analysis = webIdClient.analyze(requestBuilder.build());
                logger.info("WebID analysis succeeded with raw data");
            } catch (Exception e) {
                // If QR code data failed, don't try Cambio conversion - just rethrow
                if (isQrCodeData) {
                    throw new IOException("WebID analysis failed for QR code data", e);
                }

                // For other formats, try converting to N42 via Cambio and retry
                logger.info("WebID analysis failed with raw data, attempting Cambio conversion: {}", e.getMessage());

                byte[] n42Bytes;
                if (filename != null && (filename.endsWith(".n42") || filename.endsWith(".xml"))) {
                    n42Bytes = fileBytes;
                } else {
                    n42Bytes = new CambioConverter().convertToN42(new ByteArrayInputStream(fileBytes), filename);
                }

                WebIdRequest.Builder requestBuilder = new WebIdRequest.Builder()
                        .foreground(new ByteArrayInputStream(n42Bytes))
                        .synthesizeBackground(true);

                if (drf != null && !drf.isBlank()) {
                    requestBuilder.drf(drf);
                }

                analysis = webIdClient.analyze(requestBuilder.build());
                logger.info("WebID analysis succeeded after Cambio conversion");
            }

            analysis.setSampleTime(Instant.now());
            analysis.setOccupancyObsId(occupancyObsId != null ? occupancyObsId : "");

            // Save analysis JSON to bucket store
            try {
                String analysisJson = analysis.toString();
                Map<String, String> jsonMetadata = new HashMap<>();
                jsonMetadata.put("Content-Type", "application/json");
                bucketStore.createObject(bucketName, new ByteArrayInputStream(analysisJson.getBytes()), jsonMetadata);
            } catch (DataStoreException e) {
                logger.error("Failed to store WebId analysis JSON in bucket", e);
            }

            // Insert WebId observation into lane database
            String encodedWebIdObsId = null;
            if (laneUid != null && !laneUid.isBlank()) {
                try {
                    var laneDb = hub.getSystemDriverRegistry().getDatabase(laneUid);
                    if (laneDb == null) {
                        logger.error("No database found for lane UID: {}", laneUid);
                        return;
                    }

                    var obsStore = laneDb.getObservationStore();

                    // Find the WebId datastream
                    var dsKeys = obsStore.getDataStreams().selectKeys(new DataStreamFilter.Builder()
                            .withSystems()
                            .withUniqueIDs(laneUid)
                            .done()
                            .withOutputNames("webIdAnalysis")
                            .build()).toList();

                    if (dsKeys.isEmpty()) {
                        logger.error("No webIdAnalysis datastream found for lane: {}", laneUid);
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
            if (encodedWebIdObsId != null && occupancyObsId != null && !occupancyObsId.isBlank() && laneUid != null) {
                try {
                    BigId decodedOccObsId = obsIdEncoder.decodeID(occupancyObsId);
                    var laneDb = hub.getSystemDriverRegistry().getDatabase(laneUid);
                    if (laneDb == null) {
                        logger.error("No database found for lane UID when updating occupancy: {}", laneUid);
                        return;
                    }

                    var obsStore = laneDb.getObservationStore();
                    var obs = obsStore.get(decodedOccObsId);
                    if (obs == null) {
                        logger.error("Occupancy observation not found for ID: {}", occupancyObsId);
                        return;
                    }

                    var occupancy = Occupancy.toOccupancy(obs.getResult());
                    occupancy.addWebIdObsId(encodedWebIdObsId);

                    DataBlock newOccupancyResult = Occupancy.fromOccupancy(occupancy);
                    obs.getResult().setUnderlyingObject(newOccupancyResult.getUnderlyingObject());
                    obs.getResult().updateAtomCount();

                    obsStore.put(decodedOccObsId, obs);
                    logger.info("Occupancy observation {} updated with WebId obs ID: {}", occupancyObsId, encodedWebIdObsId);
                } catch (Exception e) {
                    logger.error("Failed to update occupancy observation with WebId obs ID", e);
                }
            }
        } catch (Exception e) {
            logger.error("WebId analysis processing failed", e);
        }
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
