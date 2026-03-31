package com.botts.impl.service.oscar.webid;

import com.botts.impl.service.bucket.util.MultipartRequestParser;
import com.botts.impl.service.bucket.util.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebIdRequestContext {
    private static final Logger logger = LoggerFactory.getLogger(WebIdRequestContext.class);

    String bucketName;
    String objectKey;
    String occupancyObsId;
    String laneUid;
    String drf;
    boolean webIdEnabled;
    boolean synthesizeBackground;
    boolean isMultipartRequest;

    String foregroundFileName = null;
    byte[] foregroundData = null;
    String backgroundFileName = null;
    byte[] backgroundData = null;

    public WebIdRequestContext(RequestContext ctx) {
        bucketName = ctx.getBucketName();
        objectKey = ctx.getObjectKey();
        isMultipartRequest = ctx.isMultipartRequest();
        occupancyObsId = ctx.getRequest().getParameter("occupancyObsId");
        laneUid = ctx.getRequest().getParameter("laneUid");
        String webIdEnabledParam = ctx.getRequest().getParameter("webIdEnabled");
        webIdEnabled = Boolean.parseBoolean(webIdEnabledParam);
        drf = ctx.getRequest().getParameter("drf");
        String synthesizeBackgroundParam = ctx.getRequest().getParameter("synthesizeBackground");
        synthesizeBackground = Boolean.parseBoolean(synthesizeBackgroundParam);

        if (isMultipartRequest) {
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



    private WebIdRequestContext() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOccupancyObsId() {
        return occupancyObsId;
    }

    public String getLaneUid() {
        return laneUid;
    }

    public String getDrf() {
        return drf;
    }

    public boolean isWebIdEnabled() {
        return webIdEnabled;
    }

    public boolean isSynthesizeBackground() {
        return synthesizeBackground;
    }

    public boolean isMultipartRequest() {
        return isMultipartRequest;
    }

    public String getForegroundFileName() {
        return foregroundFileName;
    }

    public byte[] getForegroundData() {
        return foregroundData;
    }

    public String getBackgroundFileName() {
        return backgroundFileName;
    }

    public byte[] getBackgroundData() {
        return backgroundData;
    }

    public static class Builder {
        private final WebIdRequestContext context = new WebIdRequestContext();

        public Builder bucketName(String bucketName) {
            context.bucketName = bucketName;
            return this;
        }

        public Builder objectKey(String objectKey) {
            context.objectKey = objectKey;
            return this;
        }

        public Builder occupancyObsId(String occupancyObsId) {
            context.occupancyObsId = occupancyObsId;
            return this;
        }

        public Builder laneUid(String laneUid) {
            context.laneUid = laneUid;
            return this;
        }

        public Builder drf(String drf) {
            context.drf = drf;
            return this;
        }

        public Builder webIdEnabled(boolean webIdEnabled) {
            context.webIdEnabled = webIdEnabled;
            return this;
        }

        public Builder synthesizeBackground(boolean synthesizeBackground) {
            context.synthesizeBackground = synthesizeBackground;
            return this;
        }

        public Builder multipartRequest(boolean multipartRequest) {
            context.isMultipartRequest = multipartRequest;
            return this;
        }

        public Builder foregroundFile(String fileName, byte[] data) {
            context.foregroundFileName = fileName;
            context.foregroundData = data;
            return this;
        }

        public Builder backgroundFile(String fileName, byte[] data) {
            context.backgroundFileName = fileName;
            context.backgroundData = data;
            return this;
        }

        public WebIdRequestContext build() {
            return context;
        }
    }
}