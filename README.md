# osh-oakridge-modules

## OSCAR operator documentation

The complete OSCAR 3.9.2 administration and operations manual is available in every supported language:

- [English](services/sensorhub-service-oscar/src/main/resources/com/botts/impl/service/oscar/i18n/README.md)
- [Español](services/sensorhub-service-oscar/src/main/resources/com/botts/impl/service/oscar/i18n/README_es.md)
- [Français](services/sensorhub-service-oscar/src/main/resources/com/botts/impl/service/oscar/i18n/README_fr.md)
- [Ελληνικά](services/sensorhub-service-oscar/src/main/resources/com/botts/impl/service/oscar/i18n/README_el.md)

The administration UI selects the matching manual in the OSCAR Service Module's README tab. Training screenshots used by all four manuals are maintained under [`docs/oscar-operator-manual/images`](docs/oscar-operator-manual/images).

The versioned five-JPEG retention contract, crash-safety rules, and Viewer API are documented in
[`docs/retained-media-v1.md`](docs/retained-media-v1.md).

The durable RPM journal, immutable legacy daily-file format, recovery procedure, configuration,
and operator checks are documented in
[`docs/daily-file-journal-v1.md`](docs/daily-file-journal-v1.md).

OSCAR 3.9.2 uses the tiered PostgreSQL/Parquet database, required direct daily-file journal, and
exact-five JPEG retained-media implementation in this repository. Deployment-level archive,
retention-hold, portable-export, coordinated-backup, restore, and deletion commands are documented
in the matching 3.9.2 `osh-oakridge-buildnode` release under `documentation/`; operators must use
those supported launchers instead of deleting cataloged files or database rows directly.

## Tests

The default Gradle test suite is self-contained. `WebIdIntegrationTests` keeps its serialization, local database, HTTP server, bucket-service, and handler-registration coverage enabled on every run. Tests that call the live WebID API or connect to an RPM are skipped unless they are explicitly enabled:

```sh
OSCAR_RUN_WEBID_EXTERNAL_TESTS=true \
OSCAR_WEBID_TEST_RPM_HOST=192.0.2.10 \
../../gradlew :sensorhub-service-oscar:test \
  --tests com.botts.impl.service.oscar.WebIdIntegrationTests
```

Use only an authorized test RPM or emulator. The optional settings are `OSCAR_WEBID_TEST_RPM_PORT` (default `1601`), `OSCAR_WEBID_TEST_RPM_START_TIMEOUT_MS` (default `10000`), and `OSCAR_WEBID_TEST_API_URL` (default `https://full-spectrum.sandia.gov/api/v1`). RPM startup always has a finite timeout so an unreachable test endpoint fails instead of hanging the build.

### External hardware tests

The default Gradle suite also skips only the driver and lane integration tests that require an external RPM or Axis/Sony camera. Self-contained lane, FFmpeg file, lifecycle, reconnect, HLS, timestamp, and transcoding tests remain enabled. Run the RPM tests against an authorized test device or emulator with:

```sh
OSCAR_RUN_RPM_HARDWARE_TESTS=true \
RPM_HOST=192.0.2.10 \
../../gradlew \
  :sensorhub-driver-aspect:test \
  :sensorhub-driver-rapiscan:test \
  :sensorhub-system-lane:test
```

The Aspect test connects to port `502`. The Rapiscan tests connect to ports `1601` through `1624`.

Run the external camera matrix with `OSCAR_RUN_CAMERA_HARDWARE_TESTS=true` and configure the stream URI needed by each selected Axis/Sony test: `AXIS_H264_AUTH_IP`, `AXIS_H264_NO_AUTH_IP`, `AXIS_MJPEG_AUTH_IP`, `AXIS_MJPEG_NO_AUTH_IP`, `SONY_H264_AUTH_IP`, `SONY_H264_NO_AUTH_IP`, `SONY_MJPEG_AUTH_IP`, or `SONY_MJPEG_NO_AUTH_IP`. For example:

```sh
OSCAR_RUN_CAMERA_HARDWARE_TESTS=true \
AXIS_H264_NO_AUTH_IP=rtsp://192.0.2.20/example \
../../gradlew :sensorhub-driver-ffmpeg:test \
  --tests org.sensorhub.impl.sensor.ffmpeg.tests.axis.AxisH264NoAuth
```

Each external-hardware test has a finite 10-minute limit. Set `OSCAR_EXTERNAL_HARDWARE_TEST_TIMEOUT_MS` to a positive integer to change it for slower authorized test hardware. These opt-in unit-test flags do not disable or relax the emulator-backed OSCAR release regression suite.
