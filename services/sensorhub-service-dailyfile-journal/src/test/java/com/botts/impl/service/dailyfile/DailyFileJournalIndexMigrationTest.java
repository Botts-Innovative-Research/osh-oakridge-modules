package com.botts.impl.service.dailyfile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.OptionalLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DailyFileJournalIndexMigrationTest {
    private static final String MODULE_CLASS = DailyFileJournalService.class.getCanonicalName();
    private static final String CONFIG_CLASS =
            DailyFileJournalServiceConfig.class.getCanonicalName();

    private Path root;

    @Before
    public void setUp() throws IOException {
        root = Files.createTempDirectory("dailyfile-index-migration-cli-").toRealPath();
    }

    @After
    public void tearDown() throws IOException {
        if (root == null || !Files.exists(root))
            return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    public void configuredMigrationUsesCustomRootJournalAndZoneWithoutTouchingDefault()
            throws Exception {
        Path state = Files.createDirectories(root.resolve("custom-state"));
        Path defaultJournal = Files.createDirectories(
                state.resolve("files/.dailyfile-journal"));
        Path untouched = Files.writeString(defaultJournal.resolve("do-not-touch"), "sentinel",
                StandardCharsets.UTF_8);
        Path customRoot = root.resolve("custom-storage");
        Path customJournal = customRoot.resolve("private/custom-journal");
        String laneId = "urn:test:custom-config-lane";
        Path laneDay = Files.createDirectories(customJournal.resolve("segments")
                .resolve(DailyFileJournalStore.directoryId(laneId)).resolve("2026-10-01"));
        Files.writeString(laneDay.getParent().resolve("lane.id"), laneId,
                StandardCharsets.UTF_8);
        Files.writeString(laneDay.resolve("000000.open"), "legacy segment",
                StandardCharsets.UTF_8);
        Path ledger = Files.createDirectories(customJournal.resolve("ledger/legacy-lane"))
                .resolve("2026-10-01.json");
        Files.writeString(ledger, "{\"zoneId\":\"America/New_York\"}",
                StandardCharsets.UTF_8);
        Path config = writeConfig(state.resolve("config.json"), """
                [{
                  "objClass":"%s",
                  "moduleClass":"%s",
                  "rootDirectory":"%s",
                  "journalDirectory":"private/custom-journal",
                  "zoneId":"America/New_York"
                }]
                """.formatted(CONFIG_CLASS, MODULE_CLASS, jsonString(customRoot)));

        OptionalLong indexed = DailyFileJournalIndexMigration.migrateConfiguredJournal(config);

        assertTrue(indexed.isPresent());
        assertEquals(1L, indexed.getAsLong());
        Path metadata = customRoot.resolve(
                "private/custom-journal/journal-metadata.v1.json");
        assertTrue(Files.isRegularFile(metadata));
        assertTrue(Files.readString(metadata).contains("\"zoneId\":\"America/New_York\""));
        assertEquals("sentinel", Files.readString(untouched));
        assertFalse(Files.exists(defaultJournal.resolve("journal-metadata.v1.json")));
    }

    @Test
    public void configuredMigrationResolvesRelativeRootAgainstConfigParent() throws Exception {
        Path state = Files.createDirectories(root.resolve("relative-state"));
        Path config = writeConfig(state.resolve("config.json"), """
                [{
                  "moduleClass":"%s",
                  "rootDirectory":"nested/../relative-storage",
                  "journalDirectory":"journal",
                  "zoneId":"UTC"
                }]
                """.formatted(MODULE_CLASS));

        OptionalLong indexed = DailyFileJournalIndexMigration.migrateConfiguredJournal(config);

        assertTrue(indexed.isPresent());
        assertTrue(Files.isRegularFile(state.resolve(
                "relative-storage/journal/journal-metadata.v1.json")));
        assertFalse(Files.exists(root.resolve("relative-storage")));
    }

    @Test
    public void configuredMigrationUsesServiceDefaultsWhenFieldsAreAbsent() throws Exception {
        Path state = Files.createDirectories(root.resolve("default-state"));
        Path config = writeConfig(state.resolve("config.json"),
                "[{\"objClass\":\"" + CONFIG_CLASS + "\"}]");

        OptionalLong indexed = DailyFileJournalIndexMigration.migrateConfiguredJournal(config);

        assertTrue(indexed.isPresent());
        Path metadata = state.resolve("files/.dailyfile-journal/journal-metadata.v1.json");
        assertTrue(Files.isRegularFile(metadata));
        assertTrue(Files.readString(metadata).contains("\"zoneId\":\"UTC\""));
    }

    @Test
    public void explicitRecoveryArgumentsRemainSupported() throws Exception {
        Path storage = root.resolve("explicit-storage");

        DailyFileJournalIndexMigration.main(new String[] {
                storage.toString(), "America/New_York", "private-journal"
        });

        Path metadata = storage.resolve("private-journal/journal-metadata.v1.json");
        assertTrue(Files.isRegularFile(metadata));
        assertTrue(Files.readString(metadata).contains("\"zoneId\":\"America/New_York\""));
    }

    @Test
    public void configuredMigrationSkipsWhenJournalModuleIsAbsent() throws Exception {
        Path state = Files.createDirectories(root.resolve("no-module-state"));
        Path config = writeConfig(state.resolve("config.json"), """
                [{
                  "objClass":"example.OtherConfig",
                  "moduleClass":"example.OtherModule"
                }]
                """);

        DailyFileJournalIndexMigration.main(new String[] {"--config", config.toString()});

        assertFalse(Files.exists(state.resolve("files")));
    }

    @Test
    public void configuredMigrationRejectsDuplicateJournalModulesBeforeMutation()
            throws Exception {
        Path state = Files.createDirectories(root.resolve("duplicate-state"));
        String module = "{\"objClass\":\"" + CONFIG_CLASS
                + "\",\"moduleClass\":\"" + MODULE_CLASS + "\"}";
        Path config = writeConfig(state.resolve("config.json"), "[" + module + "," + module + "]");

        IOException failure = assertThrows(IOException.class,
                () -> DailyFileJournalIndexMigration.migrateConfiguredJournal(config));

        assertTrue(failure.getMessage().contains("more than one"));
        assertFalse(Files.exists(state.resolve("files")));
    }

    @Test
    public void configuredMigrationRejectsMalformedAndNonArrayConfiguration() throws Exception {
        Path state = Files.createDirectories(root.resolve("invalid-state"));
        Path malformed = writeConfig(state.resolve("malformed.json"), "[{");
        Path nonArray = writeConfig(state.resolve("non-array.json"), "{}");
        Path trailingComma = writeConfig(state.resolve("trailing-comma.json"),
                "[{\"moduleClass\":\"" + MODULE_CLASS + "\",}]");

        IOException malformedFailure = assertThrows(IOException.class,
                () -> DailyFileJournalIndexMigration.migrateConfiguredJournal(malformed));
        IOException nonArrayFailure = assertThrows(IOException.class,
                () -> DailyFileJournalIndexMigration.migrateConfiguredJournal(nonArray));
        IOException trailingCommaFailure = assertThrows(IOException.class,
                () -> DailyFileJournalIndexMigration.migrateConfiguredJournal(trailingComma));

        assertTrue(malformedFailure.getMessage().contains("parse OSCAR configuration"));
        assertTrue(nonArrayFailure.getMessage().contains("not a module array"));
        assertTrue(trailingCommaFailure.getMessage().contains("parse OSCAR configuration"));
        assertFalse(Files.exists(state.resolve("files")));
    }

    @Test
    public void configuredMigrationRejectsInvalidJournalFieldTypesBeforeMutation()
            throws Exception {
        Path state = Files.createDirectories(root.resolve("invalid-field-state"));
        Path config = writeConfig(state.resolve("config.json"), """
                [{
                  "moduleClass":"%s",
                  "rootDirectory":42
                }]
                """.formatted(MODULE_CLASS));

        IOException failure = assertThrows(IOException.class,
                () -> DailyFileJournalIndexMigration.migrateConfiguredJournal(config));

        assertTrue(failure.getMessage().contains("must be a string: rootDirectory"));
        assertFalse(Files.exists(state.resolve("files")));
    }

    @Test
    public void configuredMigrationRejectsSymlinkedConfig() throws Exception {
        Path state = Files.createDirectories(root.resolve("linked-config-state"));
        Path target = writeConfig(state.resolve("target.json"), "[]");
        Path link = state.resolve("config.json");
        try {
            Files.createSymbolicLink(link, target.getFileName());
        } catch (UnsupportedOperationException | IOException e) {
            return;
        }

        IOException failure = assertThrows(IOException.class,
                () -> DailyFileJournalIndexMigration.migrateConfiguredJournal(link));

        assertTrue(failure.getMessage().contains("symbolic link"));
    }

    @Test
    public void configuredMigrationRejectsOversizedConfigBeforeParsing() throws Exception {
        Path state = Files.createDirectories(root.resolve("oversized-state"));
        Path config = state.resolve("config.json");
        try (FileChannel channel = FileChannel.open(config, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            channel.position(DailyFileJournalIndexMigration.MAX_CONFIG_BYTES);
            channel.write(ByteBuffer.wrap(new byte[] {'\n'}));
        }

        IOException failure = assertThrows(IOException.class,
                () -> DailyFileJournalIndexMigration.migrateConfiguredJournal(config));

        assertTrue(failure.getMessage().contains("exceeds"));
        assertFalse(Files.exists(state.resolve("files")));
    }

    private static Path writeConfig(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, contents, StandardCharsets.UTF_8);
    }

    private static String jsonString(Path path) {
        return path.toString().replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
