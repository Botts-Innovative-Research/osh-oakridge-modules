# OSCAR retained media v1

OSCAR retains an additive still-image representation of an occupancy clip. The occupancy
observation's legacy `videoPaths` field is unchanged, so older clients can continue to use the
source video while newer clients use the event media API.

## Source-recording ownership

New source MP4s use `clips/{camera-token}/{timestamp}-g{generation}.mp4`. Start, close, and cancel
commands are serialized in a bounded FIFO for each camera, and only the validated path returned by
the matching successful close may enter `videoPaths`. The lane service binds that result to the
exact occupancy start time, lifecycle epoch, lane, camera, and recording generation; it never
selects a file by directory order, timestamp proximity, or a mutable current-filename field.

Non-alarm, stopped, superseded, timed-out, failed, or otherwise unpublished recordings are canceled
or removed. A close that completes after its occupancy was invalidated remains observed so its
validated final path is deleted. Publication uses a no-replace final link: a collision preserves
the pre-existing immutable clip, while a failure after this attempt created a final link reports
that owned path for cleanup. Hidden partial files are never API-visible and are deleted directly.

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

OSCAR-managed camera recordings wait, within a bounded close deadline, for at least five muxed
video packets before a saved MP4 is published. Very short alarms therefore receive post-roll and
remain eligible for the five distinct-frame contract. Legacy or external clips that still contain
fewer than five decoded frames remain preserved as MP4s and receive a failed conversion ledger.

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

retained-media/v1/deletion-index/{first-byte}/{first-two-bytes}.idx
retained-media/v1/deletion-index/{first-byte}/.index.lock
retained-media/v1/retention-processed-index/{19-digit-UTC-epoch-hour}-{00..11}.idx
retained-media/v1/retention-processed-index/.index.lock
retained-media/v1/.metadata-reclamation-checkpoint.json
```

The internal paths are an implementation detail and cannot be downloaded through the bucket API.

## Publication and recovery

Every frame and JSON document is written to a same-directory partial, flushed with `fsync`,
verified, and atomically renamed. All five JPEGs are published before `manifest.json`; the
manifest is the commit marker. Partials are hidden from bucket existence, listing, metadata, and
read operations. Each newly created managed parent-directory entry is parent-fsynced before
publication continues. FFmpeg source recordings use the same hidden-partial/atomic-publication
rule.

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
pressure deletion first appends an exact 32-byte media-set digest plus CRC32C to a durable deletion
index and forces that file before removing any media. The index has a fixed 65,536-data-file
ceiling plus at most 256 lock files, uses 36 bytes per unique deletion plus one 16-byte header per
populated file, and is included in normal storage backups. It replaces a permanent `.deleted.json`
inode per camera set while preserving exact (non-probabilistic) lookup.
The 36-byte records are deliberately durable rather than independently time-pruned: historical
reconciliation may revisit any occupancy that still exists in the database, and removing only its
deletion record could resurrect evidence that pressure cleanup intentionally removed. They may be
reclaimed only by a future coordinated operation that also proves the source occupancy can no
longer be reconciled; a tombstone-only TTL is unsafe.

Every retained-media logical unlink whose completion permits lifecycle, queue, or checkpoint state
to advance is followed by an `fsync` of its containing directory before retryable work is cleared.
Best-effort cleanup of never-published staging partials does not advance durable lifecycle state and
is excluded from that guarantee. If a crash nevertheless replays an older manifest directory
entry, the exact terminal deletion index remains authoritative: catalog or historical
reconciliation resumes the idempotent deletion instead of merely discarding its work item. A
restart does not treat a currently missing object as durable proof: it forces the nearest existing
ancestor (without recreating missing directories) before clearing work or committing the source's
`DELETED` state. Source rollback deletion uses the same barrier.

All exact-index lookups and appends take a stable on-disk lock (plus a JVM lock stripe), reload when
another live process changes the file stamp, and never rely on a stale negative or positive result.
A bounded LRU may retain at most 32 index files and 200,000 total digests, with at most 100,000 from
one file (approximately 25 MiB using a conservative 128-byte-per-record capacity estimate).
Processed IDs divide every UTC hour into twelve deterministic five-minute files. Thus the
100,000-record single-file threshold is not an hourly cliff: it is reached only above 1.2 million
evenly distributed occupancies/hour, or more than 6.6 completed occupancies/second/lane at a
50-lane site. That is well beyond a physically meaningful RPM lane rate. A burst above 100,000
occupancies in one five-minute interval remains correct, but that file is streamed and fully
validated instead of retained in heap; operators should treat such a rate as bad source data or a
capacity alarm. A short, incomplete append tail is the only corruption truncated automatically.
Any complete record with an invalid checksum—including the final record—or any directory, symlink,
unreadable file, or bad header fails closed for operator repair rather than silently losing state.
Deletion indexes additionally reject any digest found in the wrong shard.

If a deletion-index header, checksum, or shard error is reported, do not delete or truncate that
index file: it is the durable proof that pressure-deleted media must not be reconstructed. Stop the
retention writer and restore the affected shard—or preferably the complete retained-media prefix—
from a coordinated, known-good snapshot before resuming. A live file-by-file copy is not a
consistency-safe substitute for a point-in-time snapshot or a copy taken while the writer is
stopped.

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
forward cursor (24 hours by default). Processed observation IDs are stored as exact 32-byte
SHA-256-plus-CRC32C records in at most twelve compact five-minute files per UTC event-time hour; no
occupancy creates its own marker inode. Reclamation advances by direct hour addresses and performs
exactly twelve bounded probes per inspected hour; it never lists or walks a shard. An hour is
eligible only when its end is **strictly before** the overlap's durable lower bound, so the hour
containing the inclusive boundary remains protected. Each pass inspects at most
`metadataReclamationWorkLimit` hours, including missing hours, and persists the next hour only after
all twelve files are gone and each unlink or confirmed absence has crossed a fresh parent-directory
durability barrier. The file count is therefore bounded by twelve times the overlap plus a
checkpointed cleanup backlog, while stored bytes remain proportional to the exact IDs required
inside that window. Historical reconciliation
deliberately neither consults nor rewrites this index, so it cannot recreate reclaimed state.
Reclamation also runs in legacy `frameRetentionCount=0` mode; corrupt index metadata is reported
and retried but does not prevent independent catalog, lifecycle, or retry maintenance. A separate durable
reconciliation cursor also revalidates `reconciliationCatchUpHours` of older history per cycle (24
hours by default), deliberately ignoring processed markers so lost catalog state is eventually
rebuilt without a full-history scan.

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
- `reconciliationCatchUpHours` (default `24`);
- `maintenanceWorkLimit` (default `200`); and
- `metadataReclamationWorkLimit` (default `200`).

Increasing `maximumCatchUpHours` clears a historical backlog faster but permits more database and
transcoding work in one cycle. Increasing either reconciliation interval discovers older missing
work faster at the cost of more database reads. Increasing `maintenanceWorkLimit` shortens catalog,
retry, and lifecycle queue latency but performs more small storage operations per cycle.
`metadataReclamationWorkLimit` counts directly addressed **UTC hour buckets**, not occupancies,
five-minute files, or tombstones; each hour costs a fixed twelve probes and the default can reclaim
up to 200 backlog hours per cycle. A smaller value does not weaken crash safety but takes more
cycles to catch up after a long outage.

Back up and restore the entire `videos/retained-media/v1` internal prefix as one consistency unit,
using a point-in-time filesystem snapshot or a copy taken while the retention writer is stopped.
The deletion indexes, metadata-reclamation checkpoint, scan/reconciliation checkpoints, manifests,
ledgers, and queue state jointly prevent replay or resurrection. Empty `.index.lock` files are
coordination artifacts and carry no logical state. Restoring only media assets while omitting
internal metadata is unsupported.

This compact metadata layout first ships with the new OSCAR 3.9.2 retained-media design. It does
not walk the complete event tree to migrate pre-release prototype `retention-processed/*.json` or
`.deleted.json` files because that would reintroduce unbounded startup and maintenance I/O.
Test/pre-release installations must start 3.9.2 with a fresh database and retained-media prefix, or
restore both from a coordinated 3.9.2 backup. Older OSCAR binaries do not understand these indexes;
a downgrade requires restoring the database and complete retained-media prefix from the same
coordinated pre-3.9.2 backup. Never run an older binary directly against 3.9.2 storage.

When frame retention is enabled, `frameRetentionCount` must be exactly `5`. Disabling frame
retention preserves the legacy whole-video deletion mode.
