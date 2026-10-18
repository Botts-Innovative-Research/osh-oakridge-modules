# FFmpeg Change Log
All notable changes to this project will be documented in this file.

## [Unreleased]

### Changed
- **Internal integration change for OSCAR 3.9.1:** `MpegTsProcessor.getAvStream()` was removed because
  it returned a borrowed native `AVStream` pointer that could become invalid during reconnect or
  shutdown. Integrations must call `getVideoStreamSnapshot()` instead and free the snapshot's
  owned `codecParameters()` with `avcodec_parameters_free` when finished. Time-base, duration, and
  average-frame-rate values in the snapshot are immutable Java primitives.
- MP4 and HLS output now require complete video metadata, preserve packet timing, finalize or remove
  partial output safely, and release native queues deterministically across reconnect and shutdown.
- MP4 packet delivery now stays ordered with recording close, preventing healthy occupancy clips
  from being finalized before queued video packets are written.
- Live HLS now waits for the first H.264 keyframe, publishes playlists and segments atomically,
  and returns not-ready responses instead of exposing an empty manifest or a segment that begins
  mid-GOP. Authorized pre-manifest polling keeps the stream alive while preparation completes.
- Cancelling HLS before its first keyframe now closes at the packet-listener generation boundary,
  removes incomplete artifacts without reporting a false empty-recording failure, and still
  propagates real packet-write, trailer, or native cleanup failures.
- Recording startup now reads an independently owned stream-metadata cache instead of waiting for
  a blocking input read, preventing short occupancies from ending before MP4 output becomes active.
- Non-H.264 transcoding now preserves rational frame rates such as `30000/1001`, flushes delayed
  decoder and encoder output at end of input, and reports codec failures instead of silently
  treating them as normal retry states.
- Video-retention compaction validates an actually decoded frame before a backup-protected,
  durable replacement; missing stream durations fall back to container metadata, truly unknown
  durations preserve the source, and timed-out workers defer cleanup until native processing stops.
- Occupancy MP4 commands now execute in a bounded per-camera FIFO, publish only an exact validated
  generation-qualified path, cancel non-alarm output, and remove hidden partial or late unpublished
  output without replacing or deleting an older immutable recording on a name collision.

## [2.1.0] - 2025-10-14

### Added
- File output and corresponding control interface.
### Changed
- Tests changed to reflect OSCAR needs, with most being removed. Current tests focus on camera connectivity and decoding.
