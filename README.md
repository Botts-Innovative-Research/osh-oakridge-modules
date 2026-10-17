# osh-oakridge-modules

## OSCAR operator documentation

The complete OSCAR 3.9.1 administration and operations manual is available in every supported language:

- [English](services/sensorhub-service-oscar/src/main/resources/com/botts/impl/service/oscar/i18n/README.md)
- [Español](services/sensorhub-service-oscar/src/main/resources/com/botts/impl/service/oscar/i18n/README_es.md)
- [Français](services/sensorhub-service-oscar/src/main/resources/com/botts/impl/service/oscar/i18n/README_fr.md)
- [Ελληνικά](services/sensorhub-service-oscar/src/main/resources/com/botts/impl/service/oscar/i18n/README_el.md)

The administration UI selects the matching manual in the OSCAR Service Module's README tab. Training screenshots used by all four manuals are maintained under [`docs/oscar-operator-manual/images`](docs/oscar-operator-manual/images).

## Tests

The default Gradle test suite is self-contained. `WebIdIntegrationTests` keeps its serialization, local database, HTTP server, bucket-service, and handler-registration coverage enabled on every run. Tests that call the live WebID API or connect to an RPM are skipped unless they are explicitly enabled:

```sh
OSCAR_RUN_WEBID_EXTERNAL_TESTS=true \
OSCAR_WEBID_TEST_RPM_HOST=192.0.2.10 \
../../gradlew :sensorhub-service-oscar:test \
  --tests com.botts.impl.service.oscar.WebIdIntegrationTests
```

Use only an authorized test RPM or emulator. The optional settings are `OSCAR_WEBID_TEST_RPM_PORT` (default `1601`), `OSCAR_WEBID_TEST_RPM_START_TIMEOUT_MS` (default `10000`), and `OSCAR_WEBID_TEST_API_URL` (default `https://full-spectrum.sandia.gov/api/v1`). RPM startup always has a finite timeout so an unreachable test endpoint fails instead of hanging the build.
