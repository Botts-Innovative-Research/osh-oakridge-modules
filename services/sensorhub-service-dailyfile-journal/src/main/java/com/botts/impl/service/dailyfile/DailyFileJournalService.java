package com.botts.impl.service.dailyfile;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileObservationMode;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileWriterHandle;
import org.sensorhub.impl.utils.rad.dailyfile.IDailyFileJournalService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/** OSH module wrapper around {@link DailyFileJournalStore}. */
public class DailyFileJournalService extends AbstractModule<DailyFileJournalServiceConfig>
        implements IDailyFileJournalService {

    private DailyFileJournalStore store;

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();
        try {
            store = new DailyFileJournalStore(Path.of(config.rootDirectory), config, getLogger());
        } catch (Exception e) {
            throw new SensorHubException("Unable to start the daily-file journal", e);
        }
    }

    @Override
    protected void doStop() throws SensorHubException {
        if (store != null) {
            try {
                store.close();
            } catch (IOException e) {
                throw new SensorHubException("Unable to close the daily-file journal cleanly", e);
            } finally {
                store = null;
            }
        }
    }

    @Override
    public DailyFileWriterHandle openWriter(String laneId) throws IOException {
        ensureInitialized();
        return store.openWriter(laneId);
    }

    @Override
    public DailyFileObservationMode getObservationMode() {
        return config.dailyFileObservationMode != null
                ? config.dailyFileObservationMode : DailyFileObservationMode.FULL;
    }

    @Override
    public ZoneId getJournalZoneId() {
        return ZoneId.of(config.zoneId);
    }

    @Override
    public boolean isPublished(String laneId, Instant startInclusive, Instant endExclusive) {
        return store != null && store.isPublished(laneId, startInclusive, endExclusive);
    }

    @Override
    public void confirmPublishedBackup(String laneId, Instant startInclusive, Instant endExclusive,
                                       String publishedSha256, String backupReference)
            throws IOException {
        ensureInitialized();
        store.confirmPublishedBackup(laneId, startInclusive, endExclusive, publishedSha256,
                backupReference);
    }

    @Override
    public boolean isBackupConfirmed(String laneId, Instant startInclusive,
                                     Instant endExclusive) {
        return store != null && store.isBackupConfirmed(laneId, startInclusive, endExclusive);
    }

    @Override
    public Optional<LocalDate> readObservationPurgeCheckpoint(String feedId) throws IOException {
        ensureInitialized();
        return store.readObservationPurgeCheckpoint(feedId);
    }

    @Override
    public void writeObservationPurgeCheckpoint(String feedId, LocalDate processedThrough)
            throws IOException {
        ensureInitialized();
        store.writeObservationPurgeCheckpoint(feedId, processedThrough);
    }

    @Override
    public Optional<LocalDate> findEarliestJournalDay(String laneId) throws IOException {
        ensureInitialized();
        return store.findEarliestJournalDay(laneId);
    }

    private void ensureInitialized() throws IOException {
        if (store == null)
            throw new IOException("Daily-file journal is not initialized");
    }
}
