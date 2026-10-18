package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Versioned catalog entry for the five still images retained from one occupancy camera clip.
 *
 * <p>The occupancy observation's legacy {@code videoPaths} value remains the source of the
 * {@link Source#objectKey()} reference.  This manifest is additive and gives new clients a typed,
 * integrity-checkable representation that can evolve independently of that legacy field.</p>
 */
public record MediaManifest(
        int schemaVersion,
        String mediaSetId,
        String eventId,
        String laneUid,
        String cameraUid,
        Instant createdAt,
        int jpegQuality,
        LifecycleState lifecycleState,
        Hold hold,
        Source source,
        List<Asset> assets) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int RETAINED_FRAME_COUNT = 5;
    /** Backend and Viewer share these hard limits so published assets are always consumable. */
    public static final int MAX_JPEG_DIMENSION = 6_000;
    public static final long MAX_JPEG_PIXELS = 20_000_000L;
    public static final long MAX_JPEG_BYTES = 12L * 1024L * 1024L;

    public MediaManifest {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException("Unsupported media manifest schema version: " + schemaVersion);
        requireText(mediaSetId, "mediaSetId");
        requireText(eventId, "eventId");
        requireText(laneUid, "laneUid");
        requireText(cameraUid, "cameraUid");
        Objects.requireNonNull(createdAt, "createdAt");
        if (jpegQuality < 1 || jpegQuality > 100)
            throw new IllegalArgumentException("jpegQuality must be between 1 and 100");
        Objects.requireNonNull(lifecycleState, "lifecycleState");
        Objects.requireNonNull(hold, "hold");
        Objects.requireNonNull(source, "source");
        assets = List.copyOf(Objects.requireNonNull(assets, "assets"));
        if (assets.size() != RETAINED_FRAME_COUNT)
            throw new IllegalArgumentException("A published media manifest must contain exactly five assets");
        EnumSet<FramePosition> positions = EnumSet.noneOf(FramePosition.class);
        HashSet<Long> decodedOrdinals = new HashSet<>();
        for (Asset asset : assets) {
            if (!positions.add(asset.position()))
                throw new IllegalArgumentException("A published media manifest must contain each frame position once");
            if (!decodedOrdinals.add(asset.sourceFrameOrdinal()))
                throw new IllegalArgumentException("Retained assets must come from five distinct decoded frames");
        }
    }

    public boolean isHeld() {
        return hold.active();
    }

    public MediaManifest withLifecycleState(LifecycleState state) {
        return new MediaManifest(schemaVersion, mediaSetId, eventId, laneUid, cameraUid, createdAt, jpegQuality,
                state, hold, source, assets);
    }

    public MediaManifest withSource(Source replacement) {
        return new MediaManifest(schemaVersion, mediaSetId, eventId, laneUid, cameraUid, createdAt, jpegQuality,
                lifecycleState, hold, replacement, assets);
    }

    public enum LifecycleState {
        AVAILABLE,
        HELD,
        DELETING,
        DELETED
    }

    public enum SourceState {
        PRESERVED_FOR_ROLLBACK,
        ELIGIBLE_FOR_DELETION,
        DELETED
    }

    public enum AssetState {
        PUBLISHED
    }

    public enum FramePosition {
        FIRST,
        QUARTER,
        MIDDLE,
        THREE_QUARTERS,
        LAST
    }

    public record Hold(boolean active, String reason, Instant placedAt) {
        public Hold {
            if (active) {
                requireText(reason, "hold.reason");
                Objects.requireNonNull(placedAt, "hold.placedAt");
            }
        }

        public static Hold none() {
            return new Hold(false, "", null);
        }
    }

    public record Source(
            String objectKey,
            String sha256,
            long bytes,
            String codec,
            long durationMicros,
            Instant rollbackDeleteAfter,
            SourceState state) {
        public Source {
            requireText(objectKey, "source.objectKey");
            requireSha256(sha256, "source.sha256");
            if (bytes <= 0)
                throw new IllegalArgumentException("source.bytes must be positive");
            requireText(codec, "source.codec");
            if (durationMicros < 0)
                throw new IllegalArgumentException("source.durationMicros must not be negative");
            Objects.requireNonNull(rollbackDeleteAfter, "source.rollbackDeleteAfter");
            Objects.requireNonNull(state, "source.state");
        }

        public Source withState(SourceState replacement) {
            return new Source(objectKey, sha256, bytes, codec, durationMicros,
                    rollbackDeleteAfter, replacement);
        }
    }

    public record Asset(
            FramePosition position,
            String objectKey,
            String contentType,
            String sha256,
            long bytes,
            int width,
            int height,
            long sourceFrameOrdinal,
            long sourcePts,
            double sourcePtsSeconds,
            AssetState state) {
        public Asset {
            Objects.requireNonNull(position, "asset.position");
            requireText(objectKey, "asset.objectKey");
            if (!"image/jpeg".equals(contentType))
                throw new IllegalArgumentException("Retained frame content type must be image/jpeg");
            requireSha256(sha256, "asset.sha256");
            if (bytes <= 0)
                throw new IllegalArgumentException("asset.bytes must be positive");
            if (bytes > MAX_JPEG_BYTES)
                throw new IllegalArgumentException("asset.bytes exceeds the retained JPEG limit");
            if (width <= 0 || height <= 0)
                throw new IllegalArgumentException("asset dimensions must be positive");
            if (width > MAX_JPEG_DIMENSION || height > MAX_JPEG_DIMENSION)
                throw new IllegalArgumentException("asset dimensions exceed the retained JPEG limit");
            long pixels = (long) width * (long) height;
            if (pixels > MAX_JPEG_PIXELS)
                throw new IllegalArgumentException("asset pixel count exceeds the retained JPEG limit");
            if (sourceFrameOrdinal < 0)
                throw new IllegalArgumentException("asset.sourceFrameOrdinal must not be negative");
            if (!Double.isFinite(sourcePtsSeconds))
                throw new IllegalArgumentException("asset.sourcePtsSeconds must be finite");
            Objects.requireNonNull(state, "asset.state");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " is required");
    }

    private static void requireSha256(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException(field + " must be a lowercase SHA-256 digest");
    }
}
