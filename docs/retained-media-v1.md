# OSCAR retained media v1

OSCAR retains an additive still-image representation of an occupancy clip. The occupancy
observation's legacy `videoPaths` field is unchanged, so older clients can continue to use the
source video while newer clients use the event media API.

## Media-set contract

A media set is keyed by all three stable identities:

- the occupancy observation's public ID (the event ID);
- the lane system UID; and
- the camera system UID.

Schema version 1 is committed only when five distinct decoded source frames have been written.
Their semantic positions are `FIRST`, `QUARTER`, `MIDDLE`, `THREE_QUARTERS`, and `LAST`. Each
asset is a separately decodable, opaque baseline sRGB JPEG. The configured JPEG quality defaults
to 85 and must be from 1 through 100. Backend and Viewer enforce the same safety envelope: at most
6,000 pixels in either dimension, 20,000,000 pixels total, and 12 MiB encoded per JPEG. Dimensions
are checked with overflow-safe arithmetic before BGR or `BufferedImage` allocation, and verification
rejects an oversized file before reading or decoding it.

The manifest records the JPEG quality plus the source object key, codec, duration, byte count,
SHA-256 digest, rollback deadline, and source lifecycle state. Each JPEG records its semantic position, object key,
content type, SHA-256 digest, byte count, width, height, decoded-frame ordinal, source PTS, PTS in
seconds, and asset lifecycle state. The decoded-frame ordinal, rather than the JPEG hash, proves
that the five selections are distinct; a motionless clip can legitimately encode identical image
content at different times.

Internal objects use this layout in the `videos` bucket:

```text
retained-media/v1/events/{base64url-event}/
  lanes/{base64url-lane}/cameras/{base64url-camera}/
    frame-01.jpg
    frame-02.jpg
    frame-03.jpg
    frame-04.jpg
    frame-05.jpg
    manifest.json
    .job.json
    .deleted.json
```

The internal paths are an implementation detail and cannot be downloaded through the bucket API.

## Publication and recovery

Every frame and JSON document is written to a same-directory partial, flushed with `fsync`,
verified, and atomically renamed. All five JPEGs are published before `manifest.json`; the
manifest is the commit marker. Partials are hidden from bucket existence, listing, metadata, and
read operations. FFmpeg source recordings use the same hidden-partial/atomic-publication rule.

An identity-aware durable job ledger makes repeated occupancy queries idempotent. A retry removes
uncommitted JPEGs before decoding again. A corrupt source, fewer than five decoded frames, a
verification failure, or any publication failure leaves the source video in place and records a
failed job. Failed and interrupted jobs are added to a durable, 256-shard retry index with
exponential backoff (five minutes initially, capped at 24 hours). A successful first pass also
preserves the source until its rollback deadline. The default grace is 24 hours; a separate durable
lifecycle-index item advances deletion later, so source cleanup does not depend on repeatedly
querying historical observations.

Every manifest publication reserves a direct-addressed catalog-reconciliation slot and a pressure
slot before `manifest.json` is renamed. A crash before the rename leaves a harmless stale slot; a
crash after it cannot strand a committed set without durable reconciliation work. The catalog queue
is swept cyclically at startup and during maintenance, with a frozen tail and bounded work limit,
so it repairs ledger/lifecycle state without enumerating or sorting the manifest tree. Lifecycle
work is made durable before a recovered `PUBLISHED` ledger may clear its retry entry. Emergency
pressure deletion first commits `.deleted.json`, then removes the set and its retry/lifecycle and
queue entries. That terminal marker prevents restart recovery or a racing retry from resurrecting
intentionally deleted media.

The occupancy scan itself is forward-only. Its high-water mark is stored as
`retained-media/v1/.retention-scan-checkpoint.json`, using the same write/fsync/verify/atomic-rename
protocol as manifests. On a new empty installation the cursor begins one normal query window before
the current eligibility cutoff, closing the concurrent-first-write race without an unbounded scan.
On an upgrade it begins at the earliest eligible occupancy result-time range exposed by the OSH
datastream catalog. If an eligible record exists but a custom store cannot expose a range, the
bounded fallback seed is durably written before the first epoch-based query. A restart therefore
resumes the saved position instead of beginning again at epoch. Closed window boundaries are
intentionally replayed and are safe because media identities are idempotent.

A strictly advancing event-time cursor alone could miss a record committed after its old timestamp
window was scanned. Each cycle therefore rescans `lateArrivalOverlapHours` immediately behind the
forward cursor (24 hours by default). Durable, sharded processed-observation markers suppress
duplicate decoding. A separate durable reconciliation cursor also revalidates
`reconciliationCatchUpHours` of older history per cycle (24 hours by default), deliberately
ignoring processed markers so lost catalog state is eventually rebuilt without a full-history scan.

One scheduled cycle advances through no more than `maximumCatchUpHours` of event time (six hours by
default), divided into `videoQueryPeriod` windows. Scheduling uses one low-priority thread with a
fixed delay, so cycles cannot overlap or build a fixed-rate backlog. The same thread inspects at
most `maintenanceWorkLimit` direct-addressed catalog-reconciliation, sharded lifecycle, and sharded
retry entries per cycle (200 total by default); their queue heads/cursors are durable. Media failures that have a
durable retry index entry do not block unrelated occupancies, while failure to persist either the
job or its retry marker keeps the current observation window uncheckpointed for safe replay.

Storage-pressure cleanup considers only complete catalogued media sets. It deletes a set as a unit,
writes `DELETING` before removing assets, and removes the manifest last, so an interrupted cleanup
can resume. Active holds and source rollback grace are honored. Uncatalogued legacy videos and
daily-file/adjudication data are not pressure-deletion candidates. Every manifest publication first
reserves a direct-addressed monotonic pressure-queue slot. Cleanup reads at most
`maximumMediaSetsPerCycle` slots (200 by default) and durably advances the head, so neither its scan
work nor memory grows with catalog size and a restart resumes rather than enumerating the tree.
Held, rollback-protected, and not-yet-old sets move to the tail at most once per cycle. Missing or
partially reserved slots are skipped idempotently; terminal deletion removes both queue membership
and pending work so reconciliation cannot recreate deleted media.

## Viewer API

The OSCAR module registers a read-only handler on the existing authenticated bucket service:

```text
GET /buckets/videos/media/{eventId}
GET /buckets/videos/media/{eventId}/{cameraToken}/manifest.json
GET /buckets/videos/media/{eventId}/{cameraToken}/frame-01.jpg
...
GET /buckets/videos/media/{eventId}/{cameraToken}/frame-05.jpg
```

`cameraToken` is the URL-safe Base64 encoding returned in the event response paths. The event
response may contain more than one camera set. JSON and JPEG responses carry `nosniff`; JPEGs are
served with a private cache policy. `POST`, `PUT`, and `DELETE` return `405`. Requests for internal
catalog paths return `404`.

Viewer should request the event endpoint first and use only the returned manifest and asset paths.
A `404` means no v1 media set exists; Viewer must then fall back to the observation's legacy
`videoPaths` behavior. Authentication and the `videos` bucket's GET permission are enforced by the
bucket service.

## Configuration

The existing video-retention configuration adds:

- `jpegQuality` (default `85`);
- `sourceRollbackGraceHours` (default `24`);
- `maximumCatchUpHours` (default `6`);
- `lateArrivalOverlapHours` (default `24`);
- `reconciliationCatchUpHours` (default `24`); and
- `maintenanceWorkLimit` (default `200`).

Increasing `maximumCatchUpHours` clears a historical backlog faster but permits more database and
transcoding work in one cycle. Increasing either reconciliation interval discovers older missing
work faster at the cost of more database reads. Increasing `maintenanceWorkLimit` shortens catalog,
retry, and lifecycle queue latency but performs more small storage operations per cycle. Keep the
defaults unless measured site throughput and storage latency justify a change.

When frame retention is enabled, `frameRetentionCount` must be exactly `5`. Disabling frame
retention preserves the legacy whole-video deletion mode.
