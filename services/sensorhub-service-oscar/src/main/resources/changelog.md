# OSCAR Service Change Log
All notable changes to this project will be documented in this file. 

## [Unreleased]
### Changed
- Retained-media processed-observation IDs now use at most twelve compact, checksummed five-minute
  index files per UTC hour instead of one JSON/inode per occupancy. The partition removes the
  100,000-record/hour cache cliff (the single-file threshold now requires more than 1.2 million
  evenly distributed occupancies/hour). Direct-addressed checkpointed reclamation deletes only
  complete hours strictly behind the late-arrival overlap, runs in legacy frame mode, and never
  scans a shard or the event tree.
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
