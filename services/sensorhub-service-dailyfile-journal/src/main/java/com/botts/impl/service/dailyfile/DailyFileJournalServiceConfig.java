package com.botts.impl.service.dailyfile;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileObservationMode;

/** Configuration for the durable direct daily-file journal. */
public class DailyFileJournalServiceConfig extends ModuleConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "Storage Root",
            desc = "Root containing the private journal and the public dailyfiles directory")
    public String rootDirectory = "files";

    @DisplayInfo(label = "Journal Directory",
            desc = "Private directory below Storage Root containing framed segments and ledgers")
    public String journalDirectory = ".dailyfile-journal";

    @DisplayInfo(label = "Publication Directory",
            desc = "Directory below Storage Root receiving legacy CSVs and manifests")
    public String publicationDirectory = "dailyfiles";

    @DisplayInfo(label = "Daily File Time Zone",
            desc = "IANA ZoneId used for daily rollover and local timestamps")
    public String zoneId = "UTC";

    @DisplayInfo(label = "Daily File Observation Mode",
            desc = "FULL and LATEST publish live compatibility observations and clean only completed manifest-validated days; NONE disables them")
    public DailyFileObservationMode dailyFileObservationMode = DailyFileObservationMode.FULL;

    @DisplayInfo(label = "Fsync Interval (milliseconds)",
            desc = "Maximum wall-clock interval between periodic journal forces")
    public long fsyncIntervalMillis = 1000;

    @DisplayInfo(label = "Max Unforced Records",
            desc = "Force a segment after this many accepted records even if the timer has not fired")
    public int maxUnforcedRecords = 128;

    @DisplayInfo(label = "Max Unforced Bytes",
            desc = "Force a segment after this many accepted bytes even if the timer has not fired")
    public long maxUnforcedBytes = 1024 * 1024;

    @DisplayInfo(label = "Max Segment Bytes",
            desc = "Rotate a lane segment after it reaches this size")
    public long maxSegmentBytes = 64L * 1024 * 1024;

    @DisplayInfo(label = "Max Segment Age (seconds)",
            desc = "Rotate an active lane segment after this age")
    public long maxSegmentAgeSeconds = 15 * 60;

    @DisplayInfo(label = "Max Record Bytes",
            desc = "Reject a single RPM payload larger than this UTF-8 byte count")
    public int maxRecordBytes = 4 * 1024 * 1024;

    @DisplayInfo(label = "Sealed Journal Retention (days)",
            desc = "Days to retain sealed journal segments after publication; deletion also requires a verified external-backup confirmation. Zero retains segments indefinitely.")
    public int sealedJournalRetentionDays = 30;

    public DailyFileJournalServiceConfig() {
        this.moduleClass = DailyFileJournalService.class.getCanonicalName();
    }
}
