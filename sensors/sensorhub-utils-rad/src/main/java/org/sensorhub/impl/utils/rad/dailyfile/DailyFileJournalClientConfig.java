package org.sensorhub.impl.utils.rad.dailyfile;

import org.sensorhub.api.config.DisplayInfo;

/** Per-driver policy for attaching to the node's journal service. */
public class DailyFileJournalClientConfig {

    @DisplayInfo(label = "Enable Daily File Journal",
            desc = "Write each raw RPM record to the durable direct journal before parsing")
    public boolean enabled = true;

    @DisplayInfo(label = "Require Daily File Journal",
            desc = "Fail sensor startup if the configured journal service is unavailable")
    public boolean required = true;

    @DisplayInfo(label = "Journal Startup Timeout (seconds)",
            desc = "How long a required driver waits for the journal module to start")
    public int startupTimeoutSeconds = 10;
}
