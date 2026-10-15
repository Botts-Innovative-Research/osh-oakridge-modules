package org.sensorhub.impl.sensor.ffmpeg.controls.hls;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;
import net.opengis.swe.v20.Category;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.sensor.ffmpeg.controls.HLSControl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class HlsStreamHandler extends DefaultObjectHandler {

    private static final Pattern HLS_PATTERN = Pattern.compile(".*\\.(m3u8|ts)$");
    static final long STREAM_TIMEOUT_MS = 30000;
    private static final long CLEANUP_INTERVAL_MS = 1000;
    private static final String M3U8_SUFFIX = ".m3u8";

    private final Map<String, ActiveStream> activeStreams = new ConcurrentHashMap<>();
    private final String streamBucketName;

    private record ActiveStream(HLSControl<?> control, long lastAccessMillis) {}

    public HlsStreamHandler(IBucketService service) {
        this(service, HLSControl.VIDEO_BUCKET);
    }

    public HlsStreamHandler(IBucketService service, String streamBucketName) {
        super(service.getBucketStore());
        this.streamBucketName = Objects.requireNonNull(streamBucketName, "streamBucketName");
        if (!(service.getThreadPool() instanceof ScheduledExecutorService scheduler)) {
            throw new IllegalStateException("Bucket service must be running with a scheduled executor");
        }

        // Use the bucket service's lifecycle-owned executor. BucketService shuts this executor
        // down when the service stops, which also cancels this periodic task and releases its
        // reference to the handler. A process-wide executor would retain stopped services.
        scheduler.scheduleAtFixedRate(this::cleanupInactiveStreams,
                CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void doGet(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = Paths.get(ctx.getObjectKey()).toString();
        checkGetPermission(ctx, bucketName);

        boolean manifestRequest = objectKey.endsWith(M3U8_SUFFIX);
        if (manifestRequest) {
            // A client polling while FFmpeg waits for its first keyframe is active even though
            // there is intentionally no placeholder manifest to serve yet.
            refreshManifestActivity(bucketName, objectKey, System.currentTimeMillis());
        }

        boolean objectExists = bucketStore.objectExists(bucketName, objectKey);

        if (!objectExists)
            throw ServiceErrors.notFound(objectKey + " in bucket " + bucketName);

        if (manifestRequest) {
            try {
                if (bucketStore.getObjectSize(bucketName, objectKey) == 0) {
                    throw ServiceErrors.notFound(objectKey + " is not ready");
                }
            } catch (DataStoreException e) {
                throw ServiceErrors.internalError(IBucketStore.FAILED_GET_OBJECT + bucketName);
            }
        }

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
            try (InputStream objectData = bucketStore.getObject(bucketName, objectKey)) {
                objectData.transferTo(ctx.getResponse().getOutputStream());
            }
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_GET_OBJECT + bucketName);
        }
    }

    protected void checkGetPermission(RequestContext ctx, String bucketName) {
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).get);
    }

    boolean refreshManifestActivity(String bucketName, String objectKey, long accessTime) {
        if (!streamBucketName.equals(bucketName) || !objectKey.endsWith(M3U8_SUFFIX)) {
            return false;
        }

        return activeStreams.computeIfPresent(objectKey,
                (key, stream) -> new ActiveStream(stream.control(), accessTime)) != null;
    }

    // Called by ffmpeg hlscontrols
    public void addControl(String streamName, HLSControl<?> control) {
        // Start the timeout at stream creation. A stream that is never requested must not run
        // forever, while authorized manifest polling (including pre-manifest 404s) refreshes it.
        activeStreams.put(streamName, new ActiveStream(control, System.currentTimeMillis()));
    }

    public void removeControl(String stream, HLSControl<?> control) {
        activeStreams.computeIfPresent(stream,
                (key, activeStream) -> activeStream.control() == control ? null : activeStream);
    }

    private void stopStream(HLSControl<?> control) {
        if (control != null) {
            var command = control.getCommandDescription().clone();
            command.renewDataBlock();
            ((Category)command.getComponent(0)).setValue(HLSControl.CMD_END_STREAM);
            control.submitCommand(new CommandData.Builder().withParams(command.getData()).withCommandStream(BigId.NONE).build());
        }
    }

    void cleanupInactiveStreams(long now) {
        for (var entry : activeStreams.entrySet()) {
            if (now - entry.getValue().lastAccessMillis() >= STREAM_TIMEOUT_MS
                    && activeStreams.remove(entry.getKey(), entry.getValue())) {
                // Conditional removal ensures a manifest request racing this cleanup pass can
                // atomically replace the activity record without having its stream stopped.
                stopStream(entry.getValue().control());
            }
        }
    }

    private void cleanupInactiveStreams() {
        cleanupInactiveStreams(System.currentTimeMillis());
    }

    boolean isActiveStream(String streamName) {
        return activeStreams.containsKey(streamName);
    }

    long getLastAccessMillis(String streamName) {
        ActiveStream stream = activeStreams.get(streamName);
        return stream != null ? stream.lastAccessMillis() : -1L;
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
