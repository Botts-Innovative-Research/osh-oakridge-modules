# FFmpeg Video Driver

This driver publishes video from a file or an FFmpeg-compatible network stream.

## Connection

- Set **File Path** for recorded video, or set **Connection String** for a live stream.
- For file playback, set the frame rate, buffer size, and optional looping behavior.
- For a network stream, select TCP when required; otherwise the driver uses UDP.
- Enable **Ignore Data Timestamps** when the source has no usable timestamps.

## Outputs

- Enable **HLS** to publish an HLS file-path output.
- Enable **Video Frames** to publish binary video frames.
- Set the stream ID, source position, connection timeout, and reconnection behavior as needed.
