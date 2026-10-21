package com.botts.impl.service.dailyfile;

import java.nio.file.Path;

/**
 * Offline command-line entry point for upgrading pre-index daily-file journals.
 *
 * <p>Stop OSCAR before running this command. It is safe to rerun after interruption: immutable
 * work markers are created idempotently and journal metadata is published only after the complete
 * legacy directory tree has been indexed.</p>
 */
public final class DailyFileJournalIndexMigration {

    private DailyFileJournalIndexMigration() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: DailyFileJournalIndexMigration "
                    + "<storage-root> <zone-id> [journal-directory]");
            System.exit(2);
        }
        DailyFileJournalServiceConfig config = new DailyFileJournalServiceConfig();
        config.rootDirectory = args[0];
        config.zoneId = args[1];
        if (args.length == 3)
            config.journalDirectory = args[2];

        long indexed = DailyFileJournalStore.migrateLegacyJournalIndex(
                Path.of(config.rootDirectory), config);
        System.out.println("Daily-file lifecycle index migration complete; created "
                + indexed + " work marker(s).");
    }
}
