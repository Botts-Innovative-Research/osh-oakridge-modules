package org.sensorhub.impl.sensor.ffmpeg.controls.hls;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.sensor.ffmpeg.controls.HLSControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class HlsStreamHandler extends DefaultObjectHandler {

    private static final Logger logger = LoggerFactory.getLogger(HlsStreamHandler.class);

    private static final Pattern HLS_PATTERN = Pattern.compile(".*\\.(m3u8|ts)$");
    // A stream is torn down (and its files deleted) after this long without a manifest
    // request. Browsers legitimately pause polling for tens of seconds (background-tab
    // throttling, transient network loss) and then resume; keep the timeout above that.
    private static final long STREAM_TIMEOUT_MS = 90000;
    private static final long CLEANUP_PERIOD_MS = 30000;
    private static final String M3U8_SUFFIX = ".m3u8";

    private final Map<String, Long> activeStreams = new ConcurrentHashMap<>();
    private final Map<String, HLSControl<?>> activeControls = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public HlsStreamHandler(IBucketService service) {
        super(service.getBucketStore());
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::cleanupInactiveStreams, CLEANUP_PERIOD_MS, CLEANUP_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void doGet(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = Paths.get(ctx.getObjectKey()).toString();
        boolean objectExists = bucketStore.objectExists(bucketName, objectKey);
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).get);

        if (!objectExists)
            throw ServiceErrors.notFound(objectKey + " in bucket " + bucketName);

        String mimeType;
        try {
            mimeType = bucketStore.getObjectMimeType(bucketName, objectKey);
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.UNABLE_DETERMINE_MIME_TYPE + objectKey);
        }

        if (mimeType == null || mimeType.isBlank())
            throw ServiceErrors.internalError(IBucketStore.UNABLE_DETERMINE_MIME_TYPE + objectKey);

        try {
            ctx.getResponse().setContentType(mimeType);
            // Live HLS objects must never be cached: the manifest mutates in place and
            // segment names are reused after a stream restart, and Cloudflare fronts
            // this endpoint in some deployments.
            ctx.getResponse().setHeader("Cache-Control", "no-store");
            InputStream objectData;
            objectData = bucketStore.getObject(bucketName, objectKey);
            objectData.transferTo(ctx.getResponse().getOutputStream());
            objectData.close();
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_GET_OBJECT + bucketName);
        }

        // Should repeatedly request these
        if (objectKey.endsWith(M3U8_SUFFIX))
            activeStreams.put(objectKey, System.currentTimeMillis());
    }

    // Called by ffmpeg hlscontrols
    public void addControl(String streamName, HLSControl<?> control) {
        activeControls.put(streamName, control);
    }

    public void removeControl(String stream, HLSControl<?> control) {
        if (activeControls.remove(stream, control)) {
            activeStreams.remove(stream);
        }
    }

    private void cleanupInactiveStreams() {
        // This runs via scheduleAtFixedRate: a single escaped exception silently
        // cancels all future runs (which is how the previous implementation — a
        // synthetic CommandData that fails id validation — killed the watchdog on
        // its first reap). Nothing may escape this method.
        try {
            long now = System.currentTimeMillis();
            var expired = new ArrayList<String>();
            for (var entry : activeStreams.entrySet()) {
                if (now - entry.getValue() > STREAM_TIMEOUT_MS)
                    expired.add(entry.getKey());
            }
            for (String key : expired) {
                activeStreams.remove(key);
                HLSControl<?> control = activeControls.remove(key);
                if (control != null) {
                    logger.info("HLS stream {} had no manifest requests for {} ms; stopping it", key, STREAM_TIMEOUT_MS);
                    try {
                        control.endStreamNow();
                    } catch (Exception e) {
                        logger.error("Failed to stop inactive HLS stream {}", key, e);
                    }
                } else {
                    // Reaping without a control means requests and controls are being
                    // tracked by different handler instances and the mux keeps running.
                    logger.warn("Expired HLS stream {} has no registered control in this handler; its mux may still be running", key);
                }
            }
        } catch (Exception e) {
            logger.error("HLS inactive-stream cleanup pass failed", e);
        }
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
        throw ServiceErrors.unsupportedOperation("POST not supported on video streams");
        //super.doPost(ctx);
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {
        throw ServiceErrors.unsupportedOperation("PUT not supported on video streams");
        //super.doPut(ctx);
    }

    @Override
    public void doDelete(RequestContext ctx) throws IOException, SecurityException {
        throw ServiceErrors.unsupportedOperation("DELETE not supported on video streams");
        //super.doDelete(ctx);
    }

    @Override
    public String getObjectPattern() {
        return HLS_PATTERN.pattern();
    }

}
