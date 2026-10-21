# OSCAR Service Change Log
All notable changes to this project will be documented in this file. 

## [Unreleased]
### Changed
- The English, Spanish, French, and Greek operator manuals now describe the exact 3.9.2 production
  storage policy: two-day archive eligibility plus one-day verified hot-row grace, immediate
  asynchronous five-frame media eligibility plus 24-hour source grace, bounded 30-day
  sealed-journal cleanup with an optional backup gate, managed backup disabled by default with
  opt-in single-snapshot replacement, and PostgreSQL external WAL archiving disabled.
- Video retention now defaults to producing five JPEGs asynchronously as soon as completed clips
  are discovered, while preserving the source MP4 for a further 24-hour rollback grace after
  successful publication. The forward scanner uses
  cached occupancy datastream IDs; a delayed contiguous cursor guarantees coverage inside the
  configured late-arrival allowance, while restart-safe frozen full-history sweeps recover later
  commits and lost catalog state. Both paths are durably bounded instead of replaying a full
  24-hour interval every five minutes. Backwards wall-clock corrections clamp active queries to the
  current cutoff without discarding their frozen ends. A range-less occupancy stream is probed
  directly and forces the safe epoch-discovery fallback only when it contains an eligible row.
- Retained-media processed-observation IDs now use at most twelve compact, checksummed five-minute
  index files per UTC hour instead of one JSON/inode per occupancy. The partition removes the
  100,000-record/hour cache cliff (the single-file threshold now requires more than 1.2 million
  evenly distributed occupancies/hour). Direct-addressed checkpointed reclamation deletes only
  complete hours strictly behind both the current cutoff and durable delayed cursor, runs in legacy
  frame mode, and never scans a shard or the event tree.
- Permanent per-camera `.deleted.json` tombstones are replaced by an exact, checksummed,
  append-only deletion index with a fixed file ceiling. Cross-process locks and stamped bounded
  caches keep concurrent stores coherent; short incomplete append tails recover automatically,
  while complete-record corruption and irregular index objects fail closed.
- Added the `metadataReclamationWorkLimit` operator control (default `200`) and documented that the
  count is UTC-hour buckets per cycle. The complete retained-media internal prefix is one
  backup/restore consistency unit, and pre-release prototype state requires a fresh 3.9.2 start.

## [1.1.0] - 2025-10-29
### Removed
- Removed the client config output from the OSCARServiceModule
