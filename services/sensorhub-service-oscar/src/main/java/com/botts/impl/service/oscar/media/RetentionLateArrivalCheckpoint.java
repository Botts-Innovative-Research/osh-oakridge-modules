/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/**
 * Durable state for one bounded late-arrival reconciliation sweep.
 *
 * <p>The sweep bounds are snapshotted before the first query, and a completed
 * {@code windowEnd} is the contiguous delayed cursor from which the next sweep begins. Advancing
 * {@code processedThrough} only after a successful window makes a crash replay that window rather
 * than skip it.  {@code lastCompletedAt} rate-limits complete sweeps without delaying recovery of
 * an interrupted sweep.</p>
 */
public record RetentionLateArrivalCheckpoint(
        int schemaVersion,
        Instant windowStart,
        Instant processedThrough,
        Instant windowEnd,
        Instant lastCompletedAt,
        Instant updatedAt) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public RetentionLateArrivalCheckpoint {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "Unsupported late-arrival checkpoint schema: " + schemaVersion);
        Objects.requireNonNull(windowStart, "windowStart");
        Objects.requireNonNull(processedThrough, "processedThrough");
        Objects.requireNonNull(windowEnd, "windowEnd");
        Objects.requireNonNull(lastCompletedAt, "lastCompletedAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (processedThrough.isBefore(windowStart) || processedThrough.isAfter(windowEnd))
            throw new IllegalArgumentException(
                    "processedThrough must be inside the late-arrival sweep");
        if (lastCompletedAt.isAfter(updatedAt))
            throw new IllegalArgumentException("lastCompletedAt must not follow updatedAt");
    }

    public static RetentionLateArrivalCheckpoint idle(Instant cursor, Instant updatedAt) {
        return new RetentionLateArrivalCheckpoint(CURRENT_SCHEMA_VERSION, cursor, cursor, cursor,
                Instant.EPOCH, updatedAt);
    }

    public RetentionLateArrivalCheckpoint begin(Instant start, Instant end, Instant now) {
        return new RetentionLateArrivalCheckpoint(CURRENT_SCHEMA_VERSION, start, start, end,
                earlier(lastCompletedAt, now), now);
    }

    public RetentionLateArrivalCheckpoint advance(Instant cursor, Instant now) {
        return new RetentionLateArrivalCheckpoint(CURRENT_SCHEMA_VERSION, windowStart, cursor,
                windowEnd, earlier(lastCompletedAt, now), now);
    }

    public RetentionLateArrivalCheckpoint complete(Instant now) {
        return new RetentionLateArrivalCheckpoint(CURRENT_SCHEMA_VERSION, windowStart, windowEnd,
                windowEnd, now, now);
    }

    public boolean active() {
        return processedThrough.isBefore(windowEnd);
    }

    private static Instant earlier(Instant left, Instant right) {
        return left.compareTo(right) <= 0 ? left : right;
    }
}

