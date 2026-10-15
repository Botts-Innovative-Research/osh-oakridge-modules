# FFmpeg Change Log
All notable changes to this project will be documented in this file.

## [Unreleased]

### Changed
- **Breaking API change for OSCAR 4.0.0:** `MpegTsProcessor.getAvStream()` was removed because
  it returned a borrowed native `AVStream` pointer that could become invalid during reconnect or
  shutdown. Integrations must call `getVideoStreamSnapshot()` instead and free the snapshot's
  owned `codecParameters()` with `avcodec_parameters_free` when finished. Time-base, duration, and
  average-frame-rate values in the snapshot are immutable Java primitives.
- MP4 and HLS output now require complete video metadata, preserve packet timing, finalize or remove
  partial output safely, and release native queues deterministically across reconnect and shutdown.
- Non-H.264 transcoding now preserves rational frame rates such as `30000/1001`, flushes delayed
  decoder and encoder output at end of input, and reports codec failures instead of silently
  treating them as normal retry states.
- Video-retention compaction validates an actually decoded frame before a backup-protected,
  durable replacement; missing stream durations fall back to container metadata, truly unknown
  durations preserve the source, and timed-out workers defer cleanup until native processing stops.

## [2.1.0] - 2025-10-14

### Added
- File output and corresponding control interface.
### Changed
- Tests changed to reflect OSCAR needs, with most being removed. Current tests focus on camera connectivity and decoding.
