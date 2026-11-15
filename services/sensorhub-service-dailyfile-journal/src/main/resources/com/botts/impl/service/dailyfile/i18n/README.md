# Durable OSCAR Daily File Journal Service

The **Durable OSCAR Daily File Journal Service** records raw Radiation Portal Monitor (RPM) messages into a crash-recoverable binary journal and publishes verified daily CSV files (`dailyfiles/`) along with cryptographic integrity manifests (`SHA-256`).

## Plain-Language Terms

| Term | Meaning |
| --- | --- |
| **Daily File** | A standard CSV log containing one calendar day of raw RPM status, background (`GB`, `NB`), occupancy start/stop (`GS`, `GX`), gamma/neutron count (`GC`, `NC`), and alarm (`GA`, `NA`, `NS`) lines for a lane. |
| **Journal Segment** | An append-only binary file that captures each incoming RPM line with a `CRC32C` checksum before daily CSV publication. If power is lost mid-day, OSCAR replays the journal on startup so no lane messages are lost. |
| **Manifest & Ledger** | Verification files written when a calendar day closes. They record the exact line count, byte size, and `SHA-256` checksum of the published CSV so operators and auditors can confirm the file is complete and unaltered. |
| **Fsync** | Flushing buffered operating-system writes to physical disk so accepted RPM messages survive an unexpected host reboot. |

## Configuration Reference

- **Storage Root**: Base folder containing both the private binary journal and the published daily CSV files.
- **Journal Directory**: Subfolder inside **Storage Root** that stores active and sealed binary journal segments and daily ledgers.
- **Publication Directory**: Subfolder inside **Storage Root** where completed daily CSV files and their `.manifest.json` integrity files are published.
- **Daily File Time Zone**: Local IANA time zone (for example, `UTC` or `America/New_York`) used to determine midnight rollover for daily files.
- **Daily File Observation Mode**:
  - `NONE` *(recommended default with `PARQUET_PRIMARY`)*: Writes the durable binary journal and daily CSV files directly without duplicating `dailyFile` observations into the SensorHub observation database.
  - `LATEST` / `FULL`: Publishes compatibility `dailyFile` observations to the observation database and automatically cleans up database rows once a completed day's CSV and manifest are verified.
- **Fsync Interval (milliseconds)**, **Max Unforced Records**, and **Max Unforced Bytes**: Control how frequently accepted RPM lines are flushed to physical disk (`1000 ms`, `256 records`, or `262144 bytes` by default).
- **Max Segment Bytes** and **Max Segment Age (seconds)**: Control when an active journal file rolls over to a new segment file.
- **Max Record Bytes**: Maximum allowed size of a single raw RPM message in UTF-8 bytes.
- **Sealed Journal Retention (days)**: Number of days to keep sealed binary journal segments after a day closes and its published CSV passes verification (`0` retains sealed segments indefinitely).
- **Require Backup Before Sealed Journal Deletion**: When enabled, OSCAR will not delete an expired sealed journal day until an external backup verification marker confirms the day has been archived off-host.

<details class="oscar-video-guide">
  <summary>Video guide (#13, 19.4s): Review Retained 5-Frame Media, Daily File Output &amp; Adjudicate</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/13-event-details-adjudication-and-daily-file.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/13-event-details-adjudication-and-daily-file.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Video guide (#03, 11.4s): Verify Parquet Cold-Tier Archives &amp; Single-Snapshot Backup</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/03-archive-and-snapshot-backup.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/03-archive-and-snapshot-backup.vtt" srclang="en" label="English" default>
  </video>
</details>
