package com.botts.impl.service.bucket.handler;

import com.botts.api.service.bucket.IBucketService;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

// TODO: Move this to ffmpeg module when HLS supported
public class HlsStreamHandler extends DefaultObjectHandler {

    private static final Pattern HLS_PATTERN = Pattern.compile(".*\\.(m3u8|ts)$");
    private static final long STREAM_TIMEOUT_MS = 30000;

    private final Map<String, Long> activeStreams = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public HlsStreamHandler(IBucketService service) {
        super(service.getBucketStore());
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::cleanupInactiveStreams, STREAM_TIMEOUT_MS, STREAM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void doGet(RequestContext ctx) throws IOException, SecurityException {
        super.doGet(ctx);
        // TODO: Map object key to a ffmpeg driver control so we can target the correct streams to cancel
        // Should repeatedly request these
        if (ctx.getObjectKey().endsWith(".m3u8"))
            activeStreams.put(ctx.getObjectKey(), System.currentTimeMillis());
    }

    private void stopStream() {
        // TODO: Send ffmpeg stop stream command
    }

    private void cleanupInactiveStreams() {
        long now = System.currentTimeMillis();
        activeStreams.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > STREAM_TIMEOUT_MS) {
                stopStream();
                return true;
            }
            return false;
        });
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
        throw ServiceErrors.unsupportedOperation("POST not supported on video streams");
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {
        throw ServiceErrors.unsupportedOperation("PUT not supported on video streams");
    }

    @Override
    public void doDelete(RequestContext ctx) throws IOException, SecurityException {
        throw ServiceErrors.unsupportedOperation("DELETE not supported on video streams");
    }

    @Override
    public String getObjectPattern() {
        return HLS_PATTERN.pattern();
    }

}
