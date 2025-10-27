package org.sensorhub.impl.sensor.ffmpeg.controls.hls;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;
import net.opengis.swe.v20.Category;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.sensor.ffmpeg.controls.HLSControl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class HlsStreamHandler extends DefaultObjectHandler {

    private static final Pattern HLS_PATTERN = Pattern.compile(".*\\.(m3u8|ts)$");
    private static final long STREAM_TIMEOUT_MS = 30000;

    private final Map<String, Long> activeStreams = new ConcurrentHashMap<>();
    private final Map<String, HLSControl<?>> activeControls = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public HlsStreamHandler(IBucketService service) {
        super(service.getBucketStore());
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::cleanupInactiveStreams, STREAM_TIMEOUT_MS, STREAM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void doGet(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = ctx.getObjectKey();
        boolean objectExists;
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).get);

        // Assignment is intentional here
        // Temp file is required so that ffmpeg can continue writing to the playlist file
        if ( !(objectExists = bucketStore.objectExists(bucketName, objectKey)) ) {
            if (objectExists = bucketStore.objectExists(bucketName, objectKey + ".temp")) {
                objectKey += ".temp";
            }
        }

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
            InputStream objectData;

            if (objectKey.endsWith(".m3u8")) {
                // Need to make to rename this file so that ffmpeg can continue writing playlist
                var original = new File(bucketStore.getResourceURI(bucketName, objectKey));
                var tempFile = new File(bucketStore.getResourceURI(bucketName, objectKey) + ".temp");
                original.renameTo(tempFile);
                objectData = bucketStore.getObject(bucketName, objectKey + ".temp");
                objectData.transferTo(ctx.getResponse().getOutputStream());
                bucketStore.deleteObject(bucketName, objectKey + ".temp");
            } else {
                objectData = bucketStore.getObject(bucketName, objectKey);
                objectData.transferTo(ctx.getResponse().getOutputStream());
            }

        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_GET_OBJECT + bucketName);
        }
        // TODO: Map object key to a ffmpeg driver control so we can target the correct streams to cancel
        // Should repeatedly request these
        if (ctx.getObjectKey().endsWith(".m3u8"))
            activeStreams.put(ctx.getObjectKey(), System.currentTimeMillis());
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

    private void stopStream(HLSControl<?> control) {
        if (control != null) {
            var command = control.getCommandDescription().clone();
            command.renewDataBlock();
            ((Category)command.getComponent(0)).setValue(HLSControl.CMD_END_STREAM);
            control.submitCommand(new CommandData.Builder().withParams(command.getData()).build());
        }
    }

    private void cleanupInactiveStreams() {
        long now = System.currentTimeMillis();
        // Remove active streams after timeout
        activeStreams.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > STREAM_TIMEOUT_MS) {
                stopStream(activeControls.remove(entry.getKey())); // Remove the corresponding control and stop its stream
                return true;
            }
            return false;
        });
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
