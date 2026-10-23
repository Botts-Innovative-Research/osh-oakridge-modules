package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable cursors for bounded walks of the sharded retained-media maintenance indexes. */
public record MediaMaintenanceCheckpoint(
        int schemaVersion,
        int lifecycleShard,
        String lifecycleAfterKey,
        int retryShard,
        String retryAfterKey,
        String catalogAfterKey,
        Instant updatedAt) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int SHARD_COUNT = 256;

    public MediaMaintenanceCheckpoint {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "Unsupported media maintenance checkpoint schema: " + schemaVersion);
        checkShard(lifecycleShard);
        checkShard(retryShard);
        lifecycleAfterKey = lifecycleAfterKey == null ? "" : lifecycleAfterKey;
        retryAfterKey = retryAfterKey == null ? "" : retryAfterKey;
        catalogAfterKey = catalogAfterKey == null ? "" : catalogAfterKey;
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** Backward-compatible constructor for schema-v1 checkpoints written before catalog sweeps. */
    public MediaMaintenanceCheckpoint(int schemaVersion, int lifecycleShard,
                                      String lifecycleAfterKey, int retryShard,
                                      String retryAfterKey, Instant updatedAt) {
        this(schemaVersion, lifecycleShard, lifecycleAfterKey, retryShard, retryAfterKey, "",
                updatedAt);
    }

    public static MediaMaintenanceCheckpoint initial(Instant at) {
        return new MediaMaintenanceCheckpoint(CURRENT_SCHEMA_VERSION, 0, "", 0, "", "", at);
    }

    public MediaMaintenanceCheckpoint withLifecycle(Cursor cursor, Instant at) {
        return new MediaMaintenanceCheckpoint(schemaVersion, cursor.shard(), cursor.afterKey(),
                retryShard, retryAfterKey, catalogAfterKey, at);
    }

    public MediaMaintenanceCheckpoint withRetry(Cursor cursor, Instant at) {
        return new MediaMaintenanceCheckpoint(schemaVersion, lifecycleShard, lifecycleAfterKey,
                cursor.shard(), cursor.afterKey(), catalogAfterKey, at);
    }

    public MediaMaintenanceCheckpoint withCatalog(String afterKey, Instant at) {
        return new MediaMaintenanceCheckpoint(schemaVersion, lifecycleShard, lifecycleAfterKey,
                retryShard, retryAfterKey, afterKey, at);
    }

    public Cursor lifecycleCursor() {
        return new Cursor(lifecycleShard, lifecycleAfterKey);
    }

    public Cursor retryCursor() {
        return new Cursor(retryShard, retryAfterKey);
    }

    public record Cursor(int shard, String afterKey) {
        public Cursor {
            checkShard(shard);
            afterKey = afterKey == null ? "" : afterKey;
        }
    }

    private static void checkShard(int shard) {
        if (shard < 0 || shard >= SHARD_COUNT)
            throw new IllegalArgumentException("maintenance shard must be from 0 through 255");
    }
}
