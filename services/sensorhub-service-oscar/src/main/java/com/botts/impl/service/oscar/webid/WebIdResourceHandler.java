package com.botts.impl.service.oscar.webid;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.botts.impl.service.bucket.util.MultipartRequestParser;
import com.botts.impl.service.bucket.util.RequestContext;
import org.sensorhub.impl.utils.rad.cambio.CambioConverter;
import com.google.gson.JsonArray;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.sensorhub.impl.utils.rad.webid.WebIdAnalyzer;
import org.sensorhub.impl.utils.rad.webid.WebIdClient;
import org.sensorhub.impl.utils.rad.webid.WebIdRequest;
import org.sensorhub.impl.utils.rad.webid.WebIdRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class WebIdResourceHandler extends DefaultObjectHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebIdResourceHandler.class);
    private static final Set<String> WEB_ID_FILE_EXTENSIONS = new CambioConverter().getSupportedInputFormats();

    private static final String RADDATA_PREFIX = "RADDATA://";

    private final Pattern pattern;
    private final ISensorHub hub;

    private final WebIdClient webIdClient;
    private final WebIdAnalyzer webIdAnalyzer;

    public WebIdResourceHandler(IBucketStore bucketStore, ISensorHub hub, WebIdClient webIdClient) {
        super(bucketStore);
        this.hub = hub;
        this.webIdClient = webIdClient;
        webIdAnalyzer = new WebIdAnalyzer(webIdClient, hub);

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
        var webIdContext = createWebIdRequestContext(ctx);

        if (!webIdContext.isWebIdEnabled())
            return;

        // PUT should only update one resource, so prioritize POST for FG + BG
        if (webIdContext.getForegroundData() != null && webIdContext.hasForegroundFileName()) {
            // Update the object in the bucket
            try {
                bucketStore.putObject(bucketName, objectKey, new ByteArrayInputStream(webIdContext.getForegroundData()), ctx.getHeaders());
            } catch (DataStoreException e) {
                throw new IOException(IBucketStore.FAILED_PUT_OBJECT + bucketName, e);
            }
        }

        ctx.getResponse().setStatus(HttpServletResponse.SC_OK);

        var analysis = webIdAnalyzer.processWebIdRequest(webIdContext);

        if (analysis != null) {
            try {
                storeAnalysisJson(webIdContext.getBucketName(), analysis);
            } catch (DataStoreException e) {
                throw new IOException(IBucketStore.FAILED_PUT_OBJECT + bucketName, e);
            }
        }
    }

    private void storeAnalysisJson(String bucketName, WebIdAnalysis analysis) throws DataStoreException {
        String analysisJson = analysis.toString();
        Map<String, String> jsonMetadata = new HashMap<>();
        jsonMetadata.put("Content-Type", "application/json");
        bucketStore.createObject(bucketName, new ByteArrayInputStream(analysisJson.getBytes()), jsonMetadata);
    }

    public WebIdClient getWebIdClient() {
        return webIdClient;
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).create);

        // Extract query parameters
        var webIdContext = createWebIdRequestContext(ctx);

        if (!webIdContext.isWebIdEnabled())
            return;

        List<String> resourceURIs = new ArrayList<>();

        // Save background data in bucket
        if (webIdContext.getBackgroundData() != null
                // ensure we have a filename for put requests
                && webIdContext.getBackgroundFileName() != null
                && !webIdContext.getBackgroundFileName().isBlank()) {
            try {
                bucketStore.putObject(bucketName, webIdContext.getBackgroundFileName(),
                        new ByteArrayInputStream(webIdContext.getBackgroundData()), ctx.getHeaders());
                // add to generated resource URI list
                resourceURIs.add(bucketStore.getRelativeResourceURI(bucketName, webIdContext.getBackgroundFileName()));
            } catch (DataStoreException e) {
                logger.error("Failed to put background data from {} in bucket store", webIdContext.getBackgroundFileName(), e);
            }
        }

        // For POST, we can have foreground data with no filename (i.e. QR code)
        if (webIdContext.getForegroundData() != null) {
            try {
                // put if foreground + background
                if (webIdContext.hasForegroundFileName()) {
                    bucketStore.putObject(bucketName, webIdContext.getForegroundFileName(),
                            new ByteArrayInputStream(webIdContext.getForegroundData()), ctx.getHeaders());
                    // add to generated resource URI list
                    resourceURIs.add(bucketStore.getRelativeResourceURI(bucketName, webIdContext.getForegroundFileName()));
                } else {
                    // create object with random UUID if just foreground data
                    String newObjectKey = bucketStore.createObject(bucketName,
                            new ByteArrayInputStream(webIdContext.getForegroundData()), ctx.getHeaders());
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

        var analysis = webIdAnalyzer.processWebIdRequest(webIdContext);

        if (analysis != null) {
            try {
                storeAnalysisJson(webIdContext.getBucketName(), analysis);
            } catch (DataStoreException e) {
                throw new IOException(IBucketStore.FAILED_CREATE_OBJECT + bucketName, e);
            }
        }
    }

    private void processCambioConversion(WebIdRequestContext webIdContext) {
        // TODO make sure nothing special required for cambio conversion
    }

    private WebIdRequestContext createWebIdRequestContext(RequestContext ctx) {
        var webIdRequestBuilder = WebIdRequestContext.builder();
        boolean isMultipartRequest = ctx.isMultipartRequest();
        String bucketName = ctx.getBucketName();
        String objectKey = ctx.getObjectKey();
        String bucketDir = "";

        try {
            bucketStore.getResourceURI(bucketName, "");
        } catch (DataStoreException e) {
            logger.warn("Could not resolve bucket URI", e);
        }

        webIdRequestBuilder.bucketName(bucketName)
                .objectKey(objectKey)
                .multipartRequest(isMultipartRequest)
                .occupancyObsId(ctx.getRequest().getParameter("occupancyObsId"))
                .laneUid(ctx.getRequest().getParameter("laneUid"))
                .webIdEnabled(Boolean.parseBoolean(ctx.getRequest().getParameter("webIdEnabled")))
                .drf(ctx.getRequest().getParameter("drf"))
                .directory(bucketDir)
                .synthesizeBackground(Boolean.parseBoolean(ctx.getRequest().getParameter("synthesizeBackground")));
        /*
        objectKey = ctx.getObjectKey();
        isMultipartRequest = ctx.isMultipartRequest();
        occupancyObsId = ctx.getRequest().getParameter("occupancyObsId");
        laneUid = ctx.getRequest().getParameter("laneUid");
        String webIdEnabledParam = ctx.getRequest().getParameter("webIdEnabled");
        webIdEnabled = Boolean.parseBoolean(webIdEnabledParam);
        drf = ctx.getRequest().getParameter("drf");
        String synthesizeBackgroundParam = ctx.getRequest().getParameter("synthesizeBackground");
        synthesizeBackground = Boolean.parseBoolean(synthesizeBackgroundParam);

         */


        if (isMultipartRequest) {
            try (var parseResult = MultipartRequestParser.parse(ctx.getRequest())) {

                for (var file : parseResult.files()) {
                    String fileName = file.fileName();
                    String fieldName = file.fieldName();

                    if ("foreground".equals(fieldName)) {
                        byte[] foregroundData;
                        try (var fileStream = file.inputStream()) {
                            foregroundData = fileStream.readAllBytes();
                        }
                        webIdRequestBuilder.foregroundFile(fileName, foregroundData);
                    } else if ("background".equals(fieldName)) {
                        byte[] backgroundData;
                        try (var fileStream = file.inputStream()) {
                            backgroundData = fileStream.readAllBytes();
                        }
                        webIdRequestBuilder.backgroundFile(fileName, backgroundData);
                    }
                }
            } catch (IOException e) {
                logger.error("Unable to parse multipart data", e);
            }
        } else {
            try (var contextInputStream = ctx.getRequest().getInputStream()) {
                String foregroundFileName = "";
                byte[] foregroundData;
                foregroundData = contextInputStream.readAllBytes();
                if (ctx.hasObjectKey())
                    foregroundFileName = ctx.getObjectKey();

                webIdRequestBuilder.foregroundFile(foregroundFileName, foregroundData);
            } catch (IOException e) {
                logger.error("Unable to retrieve request context input stream", e);
            }
        }
        return webIdRequestBuilder.build();
    }



    private WebIdRequest createWebIdRequest(WebIdRequestContext webIdContext) {
        WebIdRequest.Builder requestBuilder = new WebIdRequest.Builder()
                .foreground(new ByteArrayInputStream(webIdContext.getForegroundData()));

        // include background but if no background data then we need to synthesize it
        if (webIdContext.getBackgroundData() != null)
            requestBuilder.background(new ByteArrayInputStream(webIdContext.getBackgroundData()));

        // client chooses whether to synthesize background data
        requestBuilder.synthesizeBackground(webIdContext.isSynthesizeBackground());

        if (webIdContext.getDrf() != null && !webIdContext.getDrf().isBlank())
            requestBuilder.drf(webIdContext.getDrf());

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
