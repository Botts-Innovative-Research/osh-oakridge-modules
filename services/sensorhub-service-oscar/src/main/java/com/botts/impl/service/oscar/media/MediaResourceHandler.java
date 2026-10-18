package com.botts.impl.service.oscar.media;

import com.botts.api.service.bucket.IObjectHandler;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Authenticated, read-only event media API layered on the bucket service. */
public final class MediaResourceHandler implements IObjectHandler {
    static final int MAX_MEDIA_SETS_PER_EVENT = 8;
    static final long MAX_EVENT_ASSET_BYTES = 64L * 1024L * 1024L;
    static final long MAX_EVENT_PIXELS = 120_000_000L;
    static final int MAX_JSON_RESPONSE_BYTES = 1024 * 1024;
    private static final Pattern OBJECT_PATTERN = Pattern.compile("(?:media(?:/.*)?|retained-media/.*)");
    private static final Pattern FRAME_NAME = Pattern.compile("frame-0[1-5]\\.jpg");
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (value, type, context) -> context.serialize(value.toString()))
            .registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (value, type, context) -> Instant.parse(value.getAsString()))
            .disableHtmlEscaping()
            .create();

    private final MediaSetStore mediaStore;

    public MediaResourceHandler(MediaSetStore mediaStore) {
        this.mediaStore = mediaStore;
    }

    @Override
    public void doGet(RequestContext ctx) throws IOException, SecurityException {
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(ctx.getBucketName()).get);
        if (!MediaSetStore.VIDEO_BUCKET.equals(ctx.getBucketName()))
            throw ServiceErrors.notFound(ctx.getObjectKey());

        String key = ctx.getObjectKey().replace('\\', '/');
        // Internal catalog paths contain job state and source metadata. They are never served
        // directly; all reads must go through the event-scoped whitelist below.
        if (MediaSetStore.isInternalMediaKey(key))
            throw ServiceErrors.notFound(key);
        if (!key.startsWith(MediaSetStore.API_PREFIX))
            throw ServiceErrors.notFound(key);

        String[] parts = key.split("/", -1);
        if (parts.length < 2 || parts[1].isBlank())
            throw ServiceErrors.badRequest("An event ID is required");
        String eventId = parts[1];
        List<MediaSetStore.ManifestEntry> matches;
        try {
            matches = mediaStore.findEvent(eventId, MAX_MEDIA_SETS_PER_EVENT + 1);
        } catch (IllegalArgumentException e) {
            throw ServiceErrors.badRequest("Invalid event ID");
        }
        if (matches.isEmpty())
            throw ServiceErrors.notFound("No retained media exists for event " + eventId);
        validateEventBounds(matches);

        if (parts.length == 2) {
            writeEventResponse(ctx, eventId, matches);
            return;
        }
        if (parts.length != 4 || parts[2].isBlank() || parts[3].isBlank())
            throw ServiceErrors.notFound(key);

        MediaSetStore.ManifestEntry entry = matches.stream()
                .filter(candidate -> MediaSetStore.apiCameraToken(candidate.manifest().cameraUid())
                        .equals(parts[2]))
                .findFirst()
                .orElseThrow(() -> ServiceErrors.notFound("Camera media is unavailable"));
        if ("manifest.json".equals(parts[3])) {
            writeMediaSetResponse(ctx, entry.manifest());
            return;
        }
        if (!FRAME_NAME.matcher(parts[3]).matches())
            throw ServiceErrors.notFound(parts[3]);

        MediaManifest.Asset asset = entry.manifest().assets().stream()
                .filter(candidate -> candidate.objectKey().endsWith("/" + parts[3]))
                .findFirst()
                .orElseThrow(() -> ServiceErrors.notFound(parts[3]));
        streamAsset(ctx, asset);
    }

    private void writeEventResponse(RequestContext ctx, String eventId,
                                    List<MediaSetStore.ManifestEntry> entries) throws IOException {
        List<MediaSetResponse> mediaSets = new ArrayList<>();
        for (MediaSetStore.ManifestEntry entry : entries)
            mediaSets.add(response(entry.manifest()));
        EventMediaResponse result = new EventMediaResponse(MediaManifest.CURRENT_SCHEMA_VERSION,
                eventId, List.copyOf(mediaSets));
        writeJson(ctx, result);
    }

    private void writeMediaSetResponse(RequestContext ctx, MediaManifest manifest) throws IOException {
        writeJson(ctx, response(manifest));
    }

    private MediaSetResponse response(MediaManifest manifest) {
        String cameraToken = MediaSetStore.apiCameraToken(manifest.cameraUid());
        String base = MediaSetStore.API_PREFIX + manifest.eventId() + "/" + cameraToken;
        List<AssetResponse> assets = new ArrayList<>();
        for (MediaManifest.Asset asset : manifest.assets()) {
            String fileName = asset.objectKey().substring(asset.objectKey().lastIndexOf('/') + 1);
            assets.add(new AssetResponse(asset, base + "/" + fileName));
        }
        return new MediaSetResponse(manifest, base + "/manifest.json", List.copyOf(assets));
    }

    private void streamAsset(RequestContext ctx, MediaManifest.Asset asset) throws IOException {
        byte[] verified;
        try {
            verified = mediaStore.readVerifiedAsset(asset);
        } catch (IOException invalidAsset) {
            throw ServiceErrors.notFound("Retained frame is unavailable");
        }
        HttpServletResponse response = ctx.getResponse();
        response.setContentType("image/jpeg");
        response.setContentLengthLong(verified.length);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, max-age=300");
        response.getOutputStream().write(verified);
    }

    private static void validateEventBounds(List<MediaSetStore.ManifestEntry> entries)
            throws IOException {
        if (entries.size() > MAX_MEDIA_SETS_PER_EVENT)
            throw ServiceErrors.requestRejected("Retained media event exceeds the camera limit");
        long bytes = 0;
        long pixels = 0;
        try {
            for (MediaSetStore.ManifestEntry entry : entries) {
                for (MediaManifest.Asset asset : entry.manifest().assets()) {
                    bytes = Math.addExact(bytes, asset.bytes());
                    pixels = Math.addExact(pixels,
                            Math.multiplyExact((long) asset.width(), (long) asset.height()));
                }
            }
        } catch (ArithmeticException overflow) {
            throw ServiceErrors.requestRejected("Retained media event size overflows safety limits");
        }
        if (bytes > MAX_EVENT_ASSET_BYTES || pixels > MAX_EVENT_PIXELS)
            throw ServiceErrors.requestRejected("Retained media event exceeds safety limits");
    }

    private static void writeJson(RequestContext ctx, Object value) throws IOException {
        String json = GSON.toJson(value);
        int encodedBytes = json.getBytes(StandardCharsets.UTF_8).length;
        if (encodedBytes > MAX_JSON_RESPONSE_BYTES)
            throw ServiceErrors.requestRejected("Retained media response exceeds the JSON limit");
        ctx.getResponse().setContentType("application/json");
        ctx.getResponse().setCharacterEncoding("UTF-8");
        ctx.getResponse().setContentLength(encodedBytes);
        ctx.getResponse().setHeader("X-Content-Type-Options", "nosniff");
        ctx.getResponse().getWriter().write(json);
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException {
        throw ServiceErrors.unsupportedOperation("The retained-media API is read-only");
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException {
        throw ServiceErrors.unsupportedOperation("The retained-media API is read-only");
    }

    @Override
    public void doDelete(RequestContext ctx) throws IOException {
        throw ServiceErrors.unsupportedOperation("The retained-media API is read-only");
    }

    @Override
    public String getObjectPattern() {
        return OBJECT_PATTERN.pattern();
    }

    public record EventMediaResponse(int schemaVersion, String eventId,
                                     List<MediaSetResponse> mediaSets) {}

    public record MediaSetResponse(MediaManifest manifest, String manifestPath,
                                   List<AssetResponse> assets) {}

    public record AssetResponse(MediaManifest.Asset asset, String path) {}
}
