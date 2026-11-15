package com.botts.impl.service.oscar.webid;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.botts.impl.service.bucket.util.InvalidRequestException;
import com.botts.impl.service.bucket.util.MultipartRequestParser;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.UploadPolicy;
import com.google.gson.JsonArray;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.sensorhub.impl.utils.rad.webid.WebIdAnalyzer;
import org.sensorhub.impl.utils.rad.webid.WebIdClient;
import org.sensorhub.impl.utils.rad.webid.WebIdRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class WebIdResourceHandler extends DefaultObjectHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebIdResourceHandler.class);
    private static final Pattern SPECTROSCOPIC_OBJECT_PATTERN =
            Pattern.compile("(?i).*\\.(n42|spe|spc|chn|pcf|cnf|tka|mca|xml)");

    private final ISensorHub hub;
    private final WebIdClient webIdClient;
    private final WebIdAnalyzer webIdAnalyzer;

    private record ExtraAttachment(String fileName, String contentType, byte[] data) {}

    private record ParsedUpload(
            WebIdRequestContext webIdContext,
            String foregroundContentType,
            String backgroundContentType,
            List<ExtraAttachment> extraAttachments
    ) {}

    public WebIdResourceHandler(IBucketStore bucketStore, ISensorHub hub, WebIdClient webIdClient) {
        super(bucketStore);
        this.hub = hub;
        this.webIdClient = webIdClient;
        this.webIdAnalyzer = new WebIdAnalyzer(webIdClient, hub);
    }

    public WebIdClient getWebIdClient() {
        return webIdClient;
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = ctx.getObjectKey();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).put);

        ParsedUpload parsed = parseUpload(ctx);
        WebIdRequestContext webIdContext = parsed.webIdContext();

        if (webIdContext.getForegroundData() != null) {
            String targetKey = (objectKey != null && !objectKey.isBlank())
                    ? objectKey
                    : webIdContext.getForegroundFileName();
            if (targetKey != null && !targetKey.isBlank()) {
                Map<String, String> metadata = buildObjectMetadata(ctx, targetKey, parsed.foregroundContentType());
                UploadPolicy.validateUpload(targetKey, metadata);
                try {
                    boolean existed = bucketStore.objectExists(bucketName, targetKey);
                    bucketStore.putObject(
                            bucketName,
                            targetKey,
                            new ByteArrayInputStream(webIdContext.getForegroundData()),
                            metadata
                    );
                    ctx.getResponse().setStatus(existed ? HttpServletResponse.SC_OK : HttpServletResponse.SC_CREATED);
                } catch (DataStoreException e) {
                    throw new IOException(IBucketStore.FAILED_PUT_OBJECT + bucketName, e);
                }
            } else {
                ctx.getResponse().setStatus(HttpServletResponse.SC_OK);
            }
        } else {
            ctx.getResponse().setStatus(HttpServletResponse.SC_OK);
        }

        if (webIdContext.isWebIdEnabled()) {
            var analysis = webIdAnalyzer.processWebIdRequest(webIdContext);
            if (analysis != null) {
                try {
                    storeAnalysisJson(webIdContext.getBucketName(), analysis);
                } catch (DataStoreException e) {
                    throw new IOException(IBucketStore.FAILED_PUT_OBJECT + bucketName, e);
                }
            }
        } else {
            webIdAnalyzer.publishLocalN42Report(webIdContext);
        }
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).create);

        ParsedUpload parsed = parseUpload(ctx);
        WebIdRequestContext webIdContext = parsed.webIdContext();
        List<String> resourceURIs = new ArrayList<>();

        // Store foreground first so returned [fgURI, bgURI] aligns with AdjudicationDetail upload order
        if (webIdContext.getForegroundData() != null) {
            try {
                if (webIdContext.hasForegroundFileName()) {
                    String fgKey = webIdContext.getForegroundFileName();
                    Map<String, String> fgMeta = buildObjectMetadata(ctx, fgKey, parsed.foregroundContentType());
                    UploadPolicy.validateUpload(fgKey, fgMeta);
                    bucketStore.putObject(
                            bucketName,
                            fgKey,
                            new ByteArrayInputStream(webIdContext.getForegroundData()),
                            fgMeta
                    );
                    resourceURIs.add(bucketStore.getRelativeResourceURI(bucketName, fgKey));
                } else {
                    Map<String, String> anonMeta = buildObjectMetadata(ctx, null, parsed.foregroundContentType());
                    UploadPolicy.validateMetadata(anonMeta);
                    String newObjectKey = bucketStore.createObject(
                            bucketName,
                            new ByteArrayInputStream(webIdContext.getForegroundData()),
                            anonMeta
                    );
                    if (newObjectKey == null || newObjectKey.isBlank()) {
                        throw new IOException(IBucketStore.FAILED_CREATE_OBJECT + bucketName);
                    }
                    String relUri = bucketStore.getRelativeResourceURI(bucketName, newObjectKey);
                    ctx.getResponse().setHeader("Location", relUri);
                    resourceURIs.add(relUri);
                }
                ctx.getResponse().setStatus(HttpServletResponse.SC_CREATED);
            } catch (InvalidRequestException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("Failed to put foreground data in bucket store", e);
            }
        }

        // Store background second when paired foreground + background files are uploaded
        if (webIdContext.getBackgroundData() != null
                && webIdContext.getBackgroundFileName() != null
                && !webIdContext.getBackgroundFileName().isBlank()) {
            String bgKey = webIdContext.getBackgroundFileName();
            Map<String, String> bgMeta = buildObjectMetadata(ctx, bgKey, parsed.backgroundContentType());
            UploadPolicy.validateUpload(bgKey, bgMeta);
            try {
                bucketStore.putObject(
                        bucketName,
                        bgKey,
                        new ByteArrayInputStream(webIdContext.getBackgroundData()),
                        bgMeta
                );
                resourceURIs.add(bucketStore.getRelativeResourceURI(bucketName, bgKey));
                ctx.getResponse().setStatus(HttpServletResponse.SC_CREATED);
            } catch (DataStoreException e) {
                logger.error("Failed to put background data from {} in bucket store", bgKey, e);
            }
        }

        // Preserve any extra attachments sent in the same multipart request
        for (ExtraAttachment extra : parsed.extraAttachments()) {
            if (extra.data() == null) {
                continue;
            }
            try {
                if (extra.fileName() != null && !extra.fileName().isBlank()) {
                    Map<String, String> meta = buildObjectMetadata(ctx, extra.fileName(), extra.contentType());
                    UploadPolicy.validateUpload(extra.fileName(), meta);
                    bucketStore.putObject(bucketName, extra.fileName(), new ByteArrayInputStream(extra.data()), meta);
                    resourceURIs.add(bucketStore.getRelativeResourceURI(bucketName, extra.fileName()));
                } else {
                    Map<String, String> meta = buildObjectMetadata(ctx, null, extra.contentType());
                    UploadPolicy.validateMetadata(meta);
                    String createdKey = bucketStore.createObject(bucketName, new ByteArrayInputStream(extra.data()), meta);
                    if (createdKey != null && !createdKey.isBlank()) {
                        resourceURIs.add(bucketStore.getRelativeResourceURI(bucketName, createdKey));
                    }
                }
                ctx.getResponse().setStatus(HttpServletResponse.SC_CREATED);
            } catch (InvalidRequestException e) {
                throw e;
            } catch (DataStoreException e) {
                throw new IOException(IBucketStore.FAILED_CREATE_OBJECT + bucketName, e);
            }
        }

        if (!resourceURIs.isEmpty()) {
            JsonArray jsonArray = new JsonArray();
            for (var resourceURI : resourceURIs) {
                jsonArray.add(resourceURI);
            }
            ctx.getResponse().getWriter().write(jsonArray.toString());
        }

        if (webIdContext.isWebIdEnabled()) {
            var analysis = webIdAnalyzer.processWebIdRequest(webIdContext);
            if (analysis != null) {
                try {
                    storeAnalysisJson(webIdContext.getBucketName(), analysis);
                } catch (DataStoreException e) {
                    throw new IOException(IBucketStore.FAILED_CREATE_OBJECT + bucketName, e);
                }
            }
        } else {
            webIdAnalyzer.publishLocalN42Report(webIdContext);
        }
    }

    private void storeAnalysisJson(String bucketName, WebIdAnalysis analysis) throws DataStoreException {
        String analysisJson = analysis.toString();
        Map<String, String> jsonMetadata = new HashMap<>();
        jsonMetadata.put("Content-Type", "application/json");
        bucketStore.createObject(bucketName, new ByteArrayInputStream(analysisJson.getBytes(StandardCharsets.UTF_8)), jsonMetadata);
    }

    private static Map<String, String> buildObjectMetadata(RequestContext ctx, String fileName, String contentType) {
        Map<String, String> metadata = new HashMap<>(ctx.getHeaders());
        if (contentType != null && !contentType.isBlank()) {
            metadata.put("Content-Type", contentType);
        }
        if (fileName != null && !fileName.isBlank()) {
            metadata.put("X-Original-Filename", fileName);
        }
        return metadata;
    }

    private ParsedUpload parseUpload(RequestContext ctx) throws InvalidRequestException {
        var webIdRequestBuilder = WebIdRequestContext.builder();
        boolean isMultipartRequest = ctx.isMultipartRequest();
        String bucketName = ctx.getBucketName();
        String objectKey = ctx.getObjectKey();

        webIdRequestBuilder.bucketName(bucketName)
                .objectKey(objectKey)
                .multipartRequest(isMultipartRequest)
                .occupancyObsId(ctx.getRequest().getParameter("occupancyObsId"))
                .laneUid(ctx.getRequest().getParameter("laneUid"))
                .webIdEnabled(Boolean.parseBoolean(ctx.getRequest().getParameter("webIdEnabled")))
                .drf(ctx.getRequest().getParameter("drf"))
                .directory("")
                .synthesizeBackground(Boolean.parseBoolean(ctx.getRequest().getParameter("synthesizeBackground")));

        String fgContentType = null;
        String bgContentType = null;
        boolean hasForeground = false;
        boolean hasBackground = false;
        List<ExtraAttachment> extras = new ArrayList<>();

        if (isMultipartRequest) {
            try (var parseResult = MultipartRequestParser.parse(ctx.getRequest(), UploadPolicy.DEFAULT_MAX_FILE_SIZE_BYTES)) {
                for (var file : parseResult.files()) {
                    String fileName = file.fileName();
                    if ((fileName == null || fileName.isBlank()) && ctx.hasObjectKey() && !hasForeground) {
                        fileName = objectKey;
                    }
                    String fieldName = file.fieldName() != null ? file.fieldName() : "";
                    byte[] bytes;
                    try (var fileStream = file.inputStream()) {
                        bytes = fileStream.readAllBytes();
                    }

                    if ("background".equalsIgnoreCase(fieldName) && !hasBackground) {
                        webIdRequestBuilder.backgroundFile(fileName, bytes);
                        bgContentType = file.contentType();
                        hasBackground = true;
                    } else if (!hasForeground && ("foreground".equalsIgnoreCase(fieldName)
                            || "file".equalsIgnoreCase(fieldName)
                            || !"background".equalsIgnoreCase(fieldName))) {
                        webIdRequestBuilder.foregroundFile(fileName, bytes);
                        fgContentType = file.contentType();
                        hasForeground = true;
                    } else {
                        extras.add(new ExtraAttachment(fileName, file.contentType(), bytes));
                    }
                }
            } catch (InvalidRequestException e) {
                throw e;
            } catch (IOException e) {
                logger.error("Unable to parse multipart data", e);
            }
        } else {
            try (var contextInputStream = ctx.getRequest().getInputStream()) {
                String foregroundFileName = ctx.hasObjectKey() ? ctx.getObjectKey() : "";
                byte[] foregroundData = contextInputStream.readAllBytes();
                webIdRequestBuilder.foregroundFile(foregroundFileName, foregroundData);
                fgContentType = ctx.getRequest().getContentType();
            } catch (IOException e) {
                logger.error("Unable to retrieve request context input stream", e);
            }
        }

        return new ParsedUpload(webIdRequestBuilder.build(), fgContentType, bgContentType, extras);
    }

    @Override
    public boolean canHandleRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
            return false;
        }
        String pathInfo = request.getPathInfo() != null ? request.getPathInfo().toLowerCase(Locale.ROOT) : "";
        if (pathInfo.contains("alarm-read-state") || pathInfo.contains("vehicle-search")) {
            return false;
        }
        if (Boolean.parseBoolean(request.getParameter("webIdEnabled"))) {
            return true;
        }
        String occupancyObsId = request.getParameter("occupancyObsId");
        String laneUid = request.getParameter("laneUid");
        return occupancyObsId != null && !occupancyObsId.isBlank()
                && laneUid != null && !laneUid.isBlank();
    }

    @Override
    public String getObjectPattern() {
        return SPECTROSCOPIC_OBJECT_PATTERN.pattern();
    }
}
