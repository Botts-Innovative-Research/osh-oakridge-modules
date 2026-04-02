package org.sensorhub.impl.utils.rad.webid;

import com.botts.impl.service.bucket.util.MultipartRequestParser;
import com.botts.impl.service.bucket.util.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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