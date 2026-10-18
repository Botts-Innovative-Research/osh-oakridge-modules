package org.sensorhub.impl.utils.rad.dailyfile;

import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Driver-side binding to the optional journal service. It centralizes startup
 * failure semantics without adding a dependency from RPM drivers to the
 * concrete service module.
 */
public class DailyFileJournalConnection implements AutoCloseable {

    private final DailyFileWriterHandle writer;
    private final DailyFileObservationMode observationMode;
    private final boolean required;

    private DailyFileJournalConnection(DailyFileWriterHandle writer,
                                       DailyFileObservationMode observationMode,
                                       boolean required) {
        this.writer = writer;
        this.observationMode = observationMode;
        this.required = required;
    }

    public static DailyFileJournalConnection connect(ISensorHub hub, String laneId,
                                                     DailyFileJournalClientConfig config,
                                                     Logger log) throws SensorHubException {
        Objects.requireNonNull(hub, "hub");
        Objects.requireNonNull(log, "log");
        DailyFileJournalClientConfig policy = config != null ? config : new DailyFileJournalClientConfig();
        if (!policy.enabled)
            return new DailyFileJournalConnection(null, DailyFileObservationMode.FULL, false);

        IDailyFileJournalService service = hub.getModuleRegistry()
                .getModuleByType(IDailyFileJournalService.class);
        boolean serviceStarted = !(service instanceof IModule<?> module) || module.isStarted();
        if ((service != null && !serviceStarted) || (service == null && policy.required)) {
            try {
                service = hub.getModuleRegistry()
                        .waitForModuleType(IDailyFileJournalService.class, ModuleEvent.ModuleState.STARTED)
                        .get(Math.max(1, policy.startupTimeoutSeconds), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SensorHubException("Interrupted while waiting for the daily-file journal", e);
            } catch (ExecutionException | TimeoutException e) {
                if (policy.required)
                    throw new SensorHubException("Required daily-file journal is not available", e);
                log.warn("Optional daily-file journal did not start for lane {} within {} second(s)",
                        laneId, Math.max(1, policy.startupTimeoutSeconds));
                service = null;
            }
        }

        if (service == null) {
            log.warn("No daily-file journal service is available for lane {}; continuing because it is optional",
                    laneId);
            return new DailyFileJournalConnection(null, DailyFileObservationMode.FULL, false);
        }

        try {
            return new DailyFileJournalConnection(service.openWriter(laneId),
                    service.getObservationMode(), policy.required);
        } catch (IOException e) {
            if (policy.required)
                throw new SensorHubException("Required daily-file journal writer cannot be opened for " + laneId, e);
            log.error("Daily-file journal writer cannot be opened for {}; continuing because it is optional",
                    laneId, e);
            return new DailyFileJournalConnection(null, DailyFileObservationMode.FULL, false);
        }
    }

    public void append(String rawLine, Instant timestamp) throws IOException {
        if (writer != null)
            writer.append(rawLine, timestamp);
    }

    public boolean isAttached() {
        return writer != null;
    }

    public boolean isRequired() {
        return required;
    }

    public DailyFileObservationMode getObservationMode() {
        return observationMode;
    }

    public boolean shouldPublishLegacyObservation() {
        return observationMode != DailyFileObservationMode.NONE;
    }

    @Override
    public void close() throws IOException {
        if (writer != null)
            writer.close();
    }
}
