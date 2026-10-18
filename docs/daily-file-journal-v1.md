# OSCAR daily-file journal v1

The daily-file journal is the authoritative path from an RPM message to the legacy daily CSV.
Rapiscan records are appended before CSV parsing, Aspect records are appended immediately after
their register snapshot is rendered, and RS350 payloads are appended before XML normalization or
parsing. A malformed input record is therefore retained even when no parsed sensor observation can
be produced.

The implementation is in the `sensorhub-service-dailyfile-journal` module. Drivers depend only on
the capability interfaces in `sensorhub-utils-rad`; they do not depend on the concrete service.

## Storage and durability contract

The configured storage root contains private recovery state and public legacy artifacts:

```text
files/
  .dailyfile-journal/
    journal-metadata.v1.json
    segments/{lane-directory}/lane.id
    segments/{lane-directory}/{YYYY-MM-DD}/000000.open
    segments/{lane-directory}/{YYYY-MM-DD}/000000.seg
    ledger/{lane-directory}/{YYYY-MM-DD}.json
    backup-confirmations/{lane-directory}/{YYYY-MM-DD}.json
    observation-purge-checkpoints/{full-feed-uid-hash}.json
  dailyfiles/
    {readable-lane-suffix}-{full-lane-uid-hash}_{YYYY-MM-DD}.csv
    {readable-lane-suffix}-{full-lane-uid-hash}_{YYYY-MM-DD}.csv.manifest.v1.json
```

The 12-hex-character filename suffix is derived from the complete stable lane UID. Human-readable
lane suffixes may repeat between sites, so they are never used alone as an identity.

Each lane has one active `.open` segment. The segment begins with a versioned header. Every record
is framed as a 32-bit payload length, a CRC32C, and a payload containing the exact UTF-8 input text
plus its seconds/nanoseconds timestamp. Appends are synchronous and serialized per lane; there is no
in-memory handoff queue and no overflow/drop policy. A failed append is returned to the driver as an
I/O error. A required driver treats that failure as fatal to its reader rather than continuing as if
the audit record were accepted.

The active channel is forced when any configured bound is reached:

- `fsyncIntervalMillis` wall-clock time;
- `maxUnforcedRecords` accepted records; or
- `maxUnforcedBytes` accepted framed bytes.

Closing a writer or the service also forces its active segment. `maxSegmentBytes` and
`maxSegmentAgeSeconds` rotate the current segment by forcing it and atomically renaming `.open` to
`.seg` before a new `.open` is created.

At startup, every `.open` segment is scanned frame by frame. A short final header or payload is an
interrupted append and is truncated back to the last valid frame, then forced. A complete frame with
an invalid length, timestamp, or CRC is treated as corruption and fails service initialization; it
is never silently skipped. Completed prior days are sealed and publication is retried. Ledger files
are atomically replaced after every force and after successful publication.

The journal requires same-directory atomic rename support. Initialization or publication fails on a
filesystem that cannot provide it. The journal and publication directories must remain under the
same configured storage root and must not be symlinked. The journal metadata binds the root to its
calendar zone before records are accepted; changing `zoneId` requires a new empty journal root.

## Day boundary and publication

`zoneId` is an IANA `ZoneId`; the default is `UTC`. A record is assigned once to the local calendar
date containing its captured `Instant`. The resulting immutable day window is
`[day.atStartOfDay(zone), nextDay.atStartOfDay(zone))`. The recorded instants are persisted in the
ledger and manifest, so daylight-saving days correctly contain 23 or 25 hours and a later timezone
configuration change cannot alter an already published artifact.

After the end of a day, sealed records are rendered in journal order. Each legacy line is:

```text
{exact raw record},{HH-mm-ss.SSS in zoneId},{HH-mm-ss.SSS UTC}\r\n
```

There is no header and the raw record is not re-quoted or parsed. The CSV is written to a hidden
same-directory partial, forced, and atomically renamed. Its v1 manifest is then written, forced, and
atomically renamed. The manifest is the commit marker and includes the exact half-open window, lane
UID, zone, record and byte counts, filename, publication time, and SHA-256 of the CSV. The ledger is
marked `PUBLISHED` last.

Publication is immutable and idempotent. Recovery accepts an existing CSV only when its length and
SHA-256 match the journal rendering, and accepts an existing manifest only when its lane, window,
counts, and checksum match. A mismatch stops publication instead of overwriting evidence.
`isPublished` revalidates the ledger, manifest, CSV existence, window, identity, and current CSV
checksum before it authorizes database cleanup.

Compatibility observations are purged by an oldest-first, per-feed catch-up. Each feed has an
atomic durable checkpoint containing the last fully completed local day. The first run discovers
the earliest journal or database day; after downtime, every eligible day through yesterday is
visited rather than only the immediately preceding day. A checkpoint advances only after the exact
half-open day is either empty or has a valid immutable CSV/manifest and its database cleanup has
completed. A failed or unpublished day blocks later days for that feed but not other feeds. A late
database record behind the high-water mark is detected from the stream's oldest observation and
replayed safely. `OSCARServiceConfig.dailyFilePurgeWorkLimit` bounds total lane/day attempts per
cycle (default `50`), and the single-threaded/synchronized task cannot overlap itself.

Sealed private `.seg` files are not deleted merely because the public CSV exists. After the
coordinated backup has passed its structural, HMAC, PostgreSQL, archive-generation, and payload
checks and has been atomically marked `VERIFIED`, the backup workflow runs the offline confirmation
CLI against its securely extracted `oscar-state` snapshot. The CLI requires exact equality between
snapshot and stopped live state for every sealed segment name, byte count, and SHA-256; it also
validates the published CSV, manifest, and ledger. It rejects linked, open, missing, extra, or
changed entries and writes the exact immutable segment inventory plus the authenticated backup ID
and state-archive hash. The journal recomputes that inventory immediately before pruning, so a late
old-day segment invalidates the receipt. Only a matching receipt and elapsed
`sealedJournalRetentionDays` permit removal. A value of `0` disables segment pruning. CSVs,
manifests, ledgers, confirmations, and purge checkpoints remain application state.

With OSCAR stopped and the already authenticated state archive extracted read-only at
`/verified-snapshot`, the supported command is:

```sh
docker compose run --rm --no-deps --user 10001:10001 --entrypoint java \
  --volume "$snapshot_dir:/verified-snapshot:ro" oscar \
  -cp '/opt/oscar/lib/*' \
  com.botts.impl.service.dailyfile.DailyFileBackupConfirmationCli \
  /var/lib/oscar /verified-snapshot "$backup_id" "$oscar_state_sha256"
```

A nonzero exit blocks journal-pruning authorization but does not invalidate the already verified
recovery point. Do not restart OSCAR until the comparison completes.

## Module configuration

Add project `:sensorhub-service-dailyfile-journal` to the distribution and load this module before
the RPM drivers:

```json
{
  "objClass": "com.botts.impl.service.dailyfile.DailyFileJournalServiceConfig",
  "rootDirectory": "files",
  "journalDirectory": ".dailyfile-journal",
  "publicationDirectory": "dailyfiles",
  "zoneId": "UTC",
  "dailyFileObservationMode": "LATEST",
  "fsyncIntervalMillis": 1000,
  "maxUnforcedRecords": 128,
  "maxUnforcedBytes": 1048576,
  "maxSegmentBytes": 67108864,
  "maxSegmentAgeSeconds": 900,
  "maxRecordBytes": 4194304,
  "sealedJournalRetentionDays": 30,
  "id": "daily-file-journal",
  "autoStart": true,
  "moduleClass": "com.botts.impl.service.dailyfile.DailyFileJournalService",
  "name": "Daily File Journal"
}
```

Each `RapiscanConfig`, `AspectConfig`, and `RS350Config` has the following nested policy. These are
the defaults: capture is enabled and required so a module-order or disk problem cannot silently
bypass capture. Set `enabled` to `false` only for an intentional non-journaling test or migration:

```json
"dailyFileJournal": {
  "enabled": true,
  "required": true,
  "startupTimeoutSeconds": 10
}
```

`dailyFileObservationMode` controls only the compatibility `dailyFile` observation stream. It does
not change journal capture or CSV publication:

- `FULL` (service default for upgrade compatibility) publishes every compatibility observation and
  retains the completed day until its journal manifest is verified.
- `LATEST` publishes for live consumers but the OSCAR maintenance task removes older values,
  retaining the newest observation.
- `NONE` does not publish the compatibility observation. Raw journal capture still occurs.

The 3.9.2 standard production profile selects `LATEST`: the lane system subscribes to each live
`dailyFile` event to derive occupancy transitions, so `NONE` is not operationally equivalent. The
direct journal and immutable daily CSV remain the historical record. `LATEST` still incurs the
initial database/WAL write because OSH currently couples live observation delivery with storage,
but it bounds the compatibility table to the newest value between maintenance cycles. Select
`FULL` only for an external consumer that requires queryable raw observation history.

When the configured database implements its own tiered hot/cold lifecycle, set
`OSCARServiceConfig.delegateObservationLifecycleToDatabase` to `true`. OSCAR then skips only its
legacy occupancy-gated high-volume purge. Connection-status maintenance and manifest-gated
daily-file observation cleanup continue; gamma/neutron lifecycle belongs to the tiered database.
Set `OSCARServiceConfig.dailyFilePurgeWorkLimit` to the maximum lane/day windows one scheduled
catch-up may attempt; the standard production configuration uses `50`.

## Operations

Treat the journal root as application state, not as a scratch or bucket-cache directory. Back it up
with the corresponding configuration and preserve file permissions. Do not edit, concatenate,
rename, or delete `.open`, `.seg`, ledger, CSV, or manifest files while the service is running.

Useful checks after local midnight are:

1. A prior-day CSV and `.manifest.v1.json` both exist.
2. `sha256sum` (or the platform equivalent) of the CSV equals the manifest's `sha256`.
3. `recordCount` and `byteCount` are plausible for the lane.
4. The corresponding ledger state is `PUBLISHED` and its window exactly matches the manifest.
5. Logs contain no CRC, immutable-publication, atomic-rename, or periodic-force error.

If startup reports a truncated tail, the service has already removed only the incomplete final
frame and forced the repair. If it reports a CRC mismatch or immutable-publication mismatch, keep
all files, stop the affected driver, and copy the lane/day directory plus CSV, manifest, and ledger
for investigation. Do not delete a conflicting artifact to make startup succeed without first
preserving it as evidence.

Disk-full and permission failures are fail-visible. Required drivers stop accepting their input
reader after an append error; operators should restore writable space/permissions and restart the
journal and affected driver. The journal does not discard older records to regain space.

## Developer verification

Run the self-contained targeted checks from the distribution worktree root:

```sh
./gradlew :sensorhub-service-dailyfile-journal:test --no-daemon
./gradlew :sensorhub-service-oscar:test \
  --tests com.botts.impl.service.oscar.purge.DatabasePurgerSafetyTest --no-daemon
./gradlew :sensorhub-driver-rapiscan:compileJava \
  :sensorhub-driver-aspect:compileJava \
  :sensorhub-driver-rs350:compileJava --no-daemon
```

The journal suite covers exact legacy bytes, full-UID filename hashing, SHA-256 manifests, persisted
publication validation, backup-gated sealed-segment expiry, durable purge checkpoints, DST windows,
size/age rotation, partial-tail recovery, CRC corruption, oversize-record rejection, and concurrent
per-lane writers. Purger safety tests cover exact midnight scheduling, DST windows, manifest gating,
exclusive end boundaries, bounded oldest-first restart catch-up, lane-scoped occupancy, and
delegated high-volume lifecycle.
