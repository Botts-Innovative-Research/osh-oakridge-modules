package com.botts.impl.service.dailyfile;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Offline bridge between the authenticated release backup workflow and journal segment expiry.
 * OSCAR must be stopped while this command runs. The caller supplies a securely extracted snapshot
 * only after the enclosing backup checksum manifest and HMAC have been verified.
 */
public final class DailyFileBackupConfirmationCli {
    private static final String MODULE_CLASS = DailyFileJournalService.class.getCanonicalName();
    private static final Pattern BACKUP_ID = Pattern.compile("[0-9]{8}T[0-9]{6}Z-[0-9a-f]{8}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Gson GSON = new Gson();

    private DailyFileBackupConfirmationCli() {
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: DailyFileBackupConfirmationCli LIVE_STATE_ROOT "
                    + "VERIFIED_SNAPSHOT_ROOT BACKUP_ID OSCAR_STATE_SHA256");
            System.exit(2);
        }
        try {
            int confirmed = confirmVerifiedSnapshot(Path.of(args[0]), Path.of(args[1]),
                    args[2], args[3]);
            System.out.println("Confirmed " + confirmed
                    + " daily-file lane/day publication(s) from verified backup " + args[2]);
        } catch (Exception e) {
            System.err.println("Daily-file backup confirmation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    static int confirmVerifiedSnapshot(Path liveStateRoot, Path snapshotStateRoot,
                                       String backupId, String oscarStateSha256)
            throws IOException {
        if (backupId == null || !BACKUP_ID.matcher(backupId).matches())
            throw new IOException("Backup ID is invalid");
        if (oscarStateSha256 == null || !SHA256.matcher(oscarStateSha256).matches())
            throw new IOException("Authenticated OSCAR state SHA-256 is invalid");

        Path liveState = requireDirectory(liveStateRoot, "live state root");
        Path snapshotState = requireDirectory(snapshotStateRoot, "verified snapshot root");
        DailyFileJournalServiceConfig config = readConfig(liveState.resolve("config.json"));
        Path liveStorage = configuredStorageRoot(liveState, config.rootDirectory);
        Path relativeStorage = liveState.relativize(liveStorage);
        Path snapshotStorage = requireDirectory(snapshotState.resolve(relativeStorage),
                "snapshot journal storage root");
        Path snapshotJournal = configuredChild(snapshotStorage, config.journalDirectory,
                "journalDirectory");
        Path snapshotPublications = configuredChild(snapshotStorage, config.publicationDirectory,
                "publicationDirectory");
        Path ledgers = requireDirectory(snapshotJournal.resolve("ledger"),
                "snapshot journal ledger");
        Path segments = requireDirectory(snapshotJournal.resolve("segments"),
                "snapshot journal segments");
        ZoneId zone = ZoneId.of(config.zoneId);
        String backupReference = "oscar-backup:" + backupId
                + "/oscar-state.tar.gz#sha256=" + oscarStateSha256;

        int confirmed = 0;
        try (DailyFileJournalStore live = new DailyFileJournalStore(liveStorage, config,
                Clock.systemUTC(), LoggerFactory.getLogger(DailyFileBackupConfirmationCli.class), false);
             DirectoryStream<Path> laneLedgers = Files.newDirectoryStream(ledgers)) {
            for (Path laneLedger : laneLedgers) {
                if (!isDirectoryWithoutLinks(ledgers, laneLedger))
                    continue;
                String directoryId = laneLedger.getFileName().toString();
                Path snapshotLane = segments.resolve(directoryId);
                Path laneIdFile = snapshotLane.resolve("lane.id");
                if (!isRegularFileWithoutLinks(segments, laneIdFile))
                    throw new IOException("Snapshot ledger has no matching lane identity: "
                            + directoryId);
                String laneId = Files.readString(laneIdFile, StandardCharsets.UTF_8);
                if (!DailyFileJournalStore.directoryId(laneId).equals(directoryId))
                    throw new IOException("Snapshot lane identity does not match its directory");

                try (DirectoryStream<Path> ledgerFiles = Files.newDirectoryStream(
                        laneLedger, "*.json")) {
                    for (Path ledger : ledgerFiles) {
                        requireRegularFile(laneLedger, ledger, "snapshot publication ledger");
                        JsonObject ledgerJson = readObject(ledger);
                        if (!"PUBLISHED".equals(text(ledgerJson, "state")))
                            continue;
                        LocalDate day = dayFromLedger(ledger, ledgerJson);
                        DailyFileJournalStore.DayWindow window =
                                DailyFileJournalStore.DayWindow.forDate(day, zone);
                        String csvName = DailyFileJournalStore.publicationFileId(laneId)
                                + "_" + day + ".csv";
                        String manifestName = csvName + ".manifest.v1.json";
                        Path daySegments = snapshotLane.resolve(day.toString());
                        if (!containsOnlySealedSegments(snapshotLane, daySegments))
                            continue; // This backup cannot authorize deletion of this lane/day.
                        List<DailyFileJournalStore.SealedSegmentIdentity> snapshotInventory =
                                DailyFileJournalStore.sealedSegmentInventory(daySegments);
                        Path liveDaySegments = liveStorage.resolve(config.journalDirectory)
                                .resolve("segments").resolve(directoryId).resolve(day.toString());
                        List<DailyFileJournalStore.SealedSegmentIdentity> liveInventory =
                                DailyFileJournalStore.sealedSegmentInventory(liveDaySegments);
                        if (!snapshotInventory.equals(liveInventory))
                            throw new IOException("Live sealed-segment inventory differs from "
                                    + "verified snapshot for " + laneId + " day " + day);
                        Path csv = requireRegularFile(snapshotPublications,
                                snapshotPublications.resolve(csvName), "snapshot daily CSV");
                        Path manifest = requireRegularFile(snapshotPublications,
                                snapshotPublications.resolve(manifestName), "snapshot daily manifest");
                        String checksum = DailyFileJournalStore.sha256(csv);
                        validatePublicationJson(ledgerJson, readObject(manifest), laneId, day,
                                window, csvName, manifestName, checksum, Files.size(csv));
                        live.confirmPublishedBackup(laneId, window.startInclusive(),
                                window.endExclusive(), checksum, backupReference,
                                snapshotInventory);
                        confirmed++;
                    }
                }
            }
        }
        return confirmed;
    }

    private static DailyFileJournalServiceConfig readConfig(Path configPath) throws IOException {
        requireRegularFile(configPath.getParent(), configPath, "live OSCAR configuration");
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray())
                throw new IOException("OSCAR configuration root is not an array");
            JsonArray modules = root.getAsJsonArray();
            for (JsonElement element : modules) {
                if (!element.isJsonObject())
                    continue;
                JsonObject object = element.getAsJsonObject();
                if (object.has("moduleClass")
                        && MODULE_CLASS.equals(object.get("moduleClass").getAsString()))
                    return GSON.fromJson(object, DailyFileJournalServiceConfig.class);
            }
            throw new IOException("Daily File Journal module is absent from config.json");
        } catch (RuntimeException e) {
            throw new IOException("Unable to parse Daily File Journal configuration", e);
        }
    }

    private static Path configuredStorageRoot(Path stateRoot, String configured) throws IOException {
        Path value = Path.of(Objects.requireNonNull(configured, "rootDirectory"));
        Path resolved = (value.isAbsolute() ? value : stateRoot.resolve(value)).normalize()
                .toAbsolutePath();
        if (!resolved.startsWith(stateRoot))
            throw new IOException("Daily File Journal storage is outside the backed-up OSCAR state");
        return requireDirectory(resolved, "live journal storage root");
    }

    private static Path configuredChild(Path root, String configured, String field)
            throws IOException {
        Path child = Path.of(Objects.requireNonNull(configured, field));
        if (child.isAbsolute())
            throw new IOException(field + " must be relative to the journal storage root");
        Path resolved = root.resolve(child).normalize();
        if (!resolved.startsWith(root))
            throw new IOException(field + " escapes the journal storage root");
        return requireDirectory(resolved, "snapshot " + field);
    }

    private static LocalDate dayFromLedger(Path ledger, JsonObject json) throws IOException {
        String name = ledger.getFileName().toString();
        if (!name.endsWith(".json"))
            throw new IOException("Invalid publication ledger name: " + name);
        try {
            LocalDate fromName = LocalDate.parse(name.substring(0, name.length() - 5));
            LocalDate fromJson = LocalDate.parse(text(json, "day"));
            if (!fromName.equals(fromJson))
                throw new IOException("Publication ledger day does not match its filename");
            return fromName;
        } catch (RuntimeException e) {
            throw new IOException("Invalid publication ledger day", e);
        }
    }

    private static boolean containsOnlySealedSegments(Path laneRoot, Path dayDirectory)
            throws IOException {
        if (!isDirectoryWithoutLinks(laneRoot, dayDirectory))
            return false;
        boolean sealed = false;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dayDirectory)) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (name.endsWith(".open"))
                    return false;
                if (name.endsWith(".seg")) {
                    requireRegularFile(dayDirectory, entry, "snapshot sealed journal segment");
                    sealed = true;
                }
            }
        }
        return sealed;
    }

    private static void validatePublicationJson(JsonObject ledger, JsonObject manifest,
                                                String laneId, LocalDate day,
                                                DailyFileJournalStore.DayWindow window,
                                                String csvName, String manifestName,
                                                String checksum, long bytes) throws IOException {
        requireEquals(ledger, "laneId", laneId);
        requireEquals(ledger, "day", day.toString());
        requireEquals(ledger, "windowStartInclusive", window.startInclusive().toString());
        requireEquals(ledger, "windowEndExclusive", window.endExclusive().toString());
        requireEquals(ledger, "dataFile", csvName);
        requireEquals(ledger, "manifestFile", manifestName);
        requireEquals(ledger, "sha256", checksum);
        requireLong(ledger, "byteCount", bytes);
        requireLong(manifest, "schemaVersion", 1);
        requireEquals(manifest, "laneId", laneId);
        requireEquals(manifest, "windowStartInclusive", window.startInclusive().toString());
        requireEquals(manifest, "windowEndExclusive", window.endExclusive().toString());
        requireEquals(manifest, "dataFile", csvName);
        requireEquals(manifest, "sha256", checksum);
        requireLong(manifest, "byteCount", bytes);
        requireLong(manifest, "recordCount", longValue(ledger, "recordCount"));
    }

    private static JsonObject readObject(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement value = JsonParser.parseReader(reader);
            if (!value.isJsonObject())
                throw new IOException("JSON document is not an object: " + path);
            return value.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Invalid JSON document: " + path, e);
        }
    }

    private static String text(JsonObject object, String field) throws IOException {
        try {
            if (!object.has(field) || object.get(field).isJsonNull())
                throw new IOException("Required field is missing: " + field);
            return object.get(field).getAsString();
        } catch (RuntimeException e) {
            throw new IOException("Invalid text field: " + field, e);
        }
    }

    private static long longValue(JsonObject object, String field) throws IOException {
        try {
            if (!object.has(field) || object.get(field).isJsonNull())
                throw new IOException("Required field is missing: " + field);
            return object.get(field).getAsLong();
        } catch (RuntimeException e) {
            throw new IOException("Invalid integer field: " + field, e);
        }
    }

    private static void requireEquals(JsonObject object, String field, String expected)
            throws IOException {
        if (!expected.equals(text(object, field)))
            throw new IOException("Snapshot publication field does not match: " + field);
    }

    private static void requireLong(JsonObject object, String field, long expected)
            throws IOException {
        if (longValue(object, field) != expected)
            throw new IOException("Snapshot publication field does not match: " + field);
    }

    private static Path requireDirectory(Path path, String description) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(normalized))
            throw new IOException(description + " is missing or is a symbolic link: " + normalized);
        return normalized;
    }

    private static boolean isDirectoryWithoutLinks(Path root, Path path) throws IOException {
        try {
            requireInside(root, path);
            return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isSymbolicLink(path);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isRegularFileWithoutLinks(Path root, Path path) throws IOException {
        try {
            requireInside(root, path);
            return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isSymbolicLink(path);
        } catch (IOException e) {
            return false;
        }
    }

    private static Path requireRegularFile(Path root, Path path, String description)
            throws IOException {
        requireInside(root, path);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path))
            throw new IOException(description + " is missing or is a symbolic link: " + path);
        return path;
    }

    private static void requireInside(Path root, Path path) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(normalizedRoot))
            throw new IOException("Snapshot path escapes its expected root: " + path);
        Path relative = normalizedRoot.relativize(normalized);
        Path current = normalizedRoot;
        for (Path element : relative) {
            current = current.resolve(element);
            if (Files.isSymbolicLink(current))
                throw new IOException("Snapshot path contains a symbolic link: " + current);
        }
    }
}
