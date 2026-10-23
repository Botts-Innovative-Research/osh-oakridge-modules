package com.botts.impl.service.dailyfile;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Offline command-line entry point for upgrading pre-index daily-file journals.
 *
 * <p>Stop OSCAR before running this command. It is safe to rerun after interruption: immutable
 * work markers are created idempotently and journal metadata is published only after the complete
 * legacy directory tree has been indexed.</p>
 */
public final class DailyFileJournalIndexMigration {
    static final int MAX_CONFIG_BYTES = 16 * 1024 * 1024;

    private static final String MODULE_CLASS = DailyFileJournalService.class.getCanonicalName();
    private static final String CONFIG_CLASS =
            DailyFileJournalServiceConfig.class.getCanonicalName();

    private DailyFileJournalIndexMigration() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--config".equals(args[0])) {
            if (args.length != 2) {
                printUsage();
                System.exit(2);
                return;
            }
            OptionalLong indexed = migrateConfiguredJournal(Path.of(args[1]));
            if (indexed.isEmpty()) {
                System.out.println("Daily File Journal module is not configured; "
                        + "lifecycle-index migration skipped.");
            } else {
                printCompletion(indexed.getAsLong());
            }
            return;
        }
        if (args.length < 2 || args.length > 3) {
            printUsage();
            System.exit(2);
        }
        DailyFileJournalServiceConfig config = new DailyFileJournalServiceConfig();
        config.rootDirectory = args[0];
        config.zoneId = args[1];
        if (args.length == 3)
            config.journalDirectory = args[2];

        long indexed = DailyFileJournalStore.migrateLegacyJournalIndex(
                Path.of(config.rootDirectory), config);
        printCompletion(indexed);
    }

    static OptionalLong migrateConfiguredJournal(Path configPath) throws IOException {
        ConfiguredJournal journal = readConfiguredJournal(configPath);
        if (journal == null)
            return OptionalLong.empty();
        return OptionalLong.of(DailyFileJournalStore.migrateLegacyJournalIndex(
                journal.rootDirectory, journal.config));
    }

    private static ConfiguredJournal readConfiguredJournal(Path suppliedPath) throws IOException {
        Path configPath = requireConfigFile(suppliedPath);
        DailyFileJournalServiceConfig selected = parseJournalConfig(readConfig(configPath));
        if (selected == null)
            return null;

        Path configuredRoot;
        try {
            Path value = Path.of(selected.rootDirectory);
            configuredRoot = (value.isAbsolute()
                    ? value : configPath.getParent().resolve(value)).toAbsolutePath().normalize();
            validateJournalDirectory(selected.journalDirectory);
            ZoneId.of(selected.zoneId);
        } catch (InvalidPathException | DateTimeException e) {
            throw new IOException("Daily File Journal configuration contains an invalid path or "
                    + "time zone", e);
        }
        return new ConfiguredJournal(configuredRoot, selected);
    }

    private static Path requireConfigFile(Path suppliedPath) throws IOException {
        if (suppliedPath == null)
            throw new IOException("OSCAR configuration path is required");
        Path configPath = suppliedPath.toAbsolutePath().normalize();
        rejectSymbolicLinkComponents(configPath);
        Path parent = configPath.getParent();
        if (parent == null || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(parent)) {
            throw new IOException("OSCAR configuration parent is missing or is a symbolic link: "
                    + parent);
        }
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(configPath, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new IOException("OSCAR configuration is missing or unreadable: " + configPath,
                    e);
        }
        if (!attributes.isRegularFile() || attributes.isSymbolicLink())
            throw new IOException("OSCAR configuration is not a regular, non-symbolic-link file: "
                    + configPath);
        if (attributes.size() > MAX_CONFIG_BYTES)
            throw new IOException("OSCAR configuration exceeds " + MAX_CONFIG_BYTES + " bytes");
        return configPath;
    }

    private static void rejectSymbolicLinkComponents(Path path) throws IOException {
        Path current = path.getRoot();
        if (current == null)
            throw new IOException("OSCAR configuration path is not absolute: " + path);
        for (Path component : path) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current))
                throw new IOException("OSCAR configuration path contains a symbolic link: "
                        + current);
        }
    }

    private static String readConfig(Path configPath) throws IOException {
        byte[] encoded;
        try (InputStream input = Files.newInputStream(configPath, StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            encoded = input.readNBytes(MAX_CONFIG_BYTES + 1);
        }
        if (encoded.length > MAX_CONFIG_BYTES)
            throw new IOException("OSCAR configuration exceeds " + MAX_CONFIG_BYTES + " bytes");

        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded)).toString();
        } catch (CharacterCodingException e) {
            throw new IOException("OSCAR configuration is not valid UTF-8", e);
        }
    }

    private static DailyFileJournalServiceConfig parseJournalConfig(String json)
            throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            if (reader.peek() != JsonToken.BEGIN_ARRAY)
                throw new IOException("OSCAR configuration root is not a module array");
            reader.beginArray();
            DailyFileJournalServiceConfig selected = null;
            while (reader.hasNext()) {
                if (reader.peek() != JsonToken.BEGIN_OBJECT)
                    throw new IOException("OSCAR configuration module entry is not an object");
                ParsedModule module = readModule(reader);
                if (!module.isDailyFileJournal())
                    continue;
                if (selected != null)
                    throw new IOException("OSCAR configuration contains more than one Daily File "
                            + "Journal module");
                selected = module.toConfig();
            }
            reader.endArray();
            if (reader.peek() != JsonToken.END_DOCUMENT)
                throw new IOException("OSCAR configuration contains trailing content");
            return selected;
        } catch (MalformedJsonException | EOFException e) {
            throw new IOException("Unable to parse OSCAR configuration", e);
        } catch (IllegalStateException e) {
            throw new IOException("Unable to parse OSCAR configuration", e);
        }
    }

    private static ParsedModule readModule(JsonReader reader) throws IOException {
        ParsedModule module = new ParsedModule();
        Set<String> fields = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String field = reader.nextName();
            if (!fields.add(field))
                throw new IOException("OSCAR configuration module contains duplicate field: "
                        + field);
            switch (field) {
                case "moduleClass":
                    module.moduleClass = readRequiredString(reader, field);
                    break;
                case "objClass":
                    module.configClass = readRequiredString(reader, field);
                    break;
                case "rootDirectory":
                    module.rootDirectory = readPotentialString(reader, module, field);
                    break;
                case "journalDirectory":
                    module.journalDirectory = readPotentialString(reader, module, field);
                    break;
                case "zoneId":
                    module.zoneId = readPotentialString(reader, module, field);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return module;
    }

    private static String readRequiredString(JsonReader reader, String field) throws IOException {
        if (reader.peek() != JsonToken.STRING)
            throw new IOException("OSCAR configuration field must be a string: " + field);
        String value = reader.nextString();
        if (value.isBlank())
            throw new IOException("OSCAR configuration field must not be blank: " + field);
        return value;
    }

    private static String readPotentialString(JsonReader reader, ParsedModule module, String field)
            throws IOException {
        if (reader.peek() == JsonToken.STRING)
            return reader.nextString();
        module.invalidField = field;
        reader.skipValue();
        return null;
    }

    private static void validateJournalDirectory(String configured) throws IOException {
        Path value = Path.of(configured);
        if (value.isAbsolute())
            throw new IOException("journalDirectory must be relative to rootDirectory");
        Path normalized = value.normalize();
        if (normalized.toString().isBlank() || normalized.startsWith(".."))
            throw new IOException("journalDirectory must stay inside rootDirectory");
    }

    private static void printCompletion(long indexed) {
        System.out.println("Daily-file lifecycle index migration complete; created "
                + indexed + " work marker(s).");
    }

    private static void printUsage() {
        System.err.println("Usage: DailyFileJournalIndexMigration --config <config-json>");
        System.err.println("   or: DailyFileJournalIndexMigration "
                + "<storage-root> <zone-id> [journal-directory]");
    }

    private static final class ConfiguredJournal {
        private final Path rootDirectory;
        private final DailyFileJournalServiceConfig config;

        private ConfiguredJournal(Path rootDirectory, DailyFileJournalServiceConfig config) {
            this.rootDirectory = rootDirectory;
            this.config = config;
        }
    }

    private static final class ParsedModule {
        private String moduleClass;
        private String configClass;
        private String rootDirectory;
        private String journalDirectory;
        private String zoneId;
        private String invalidField;

        private boolean isDailyFileJournal() throws IOException {
            boolean moduleMatches = MODULE_CLASS.equals(moduleClass);
            boolean configMatches = CONFIG_CLASS.equals(configClass);
            if (moduleMatches && configClass != null && !configMatches)
                throw new IOException("Daily File Journal module has an unexpected objClass");
            if (configMatches && moduleClass != null && !moduleMatches) {
                throw new IOException(
                        "Daily File Journal configuration has an unexpected moduleClass");
            }
            return moduleMatches || configMatches;
        }

        private DailyFileJournalServiceConfig toConfig() throws IOException {
            if (invalidField != null)
                throw new IOException("OSCAR configuration field must be a string: "
                        + invalidField);
            DailyFileJournalServiceConfig config = new DailyFileJournalServiceConfig();
            if (rootDirectory != null)
                config.rootDirectory = requireNonBlank(rootDirectory, "rootDirectory");
            if (journalDirectory != null)
                config.journalDirectory = requireNonBlank(journalDirectory, "journalDirectory");
            if (zoneId != null)
                config.zoneId = requireNonBlank(zoneId, "zoneId");
            return config;
        }

        private static String requireNonBlank(String value, String field) throws IOException {
            if (value.isBlank())
                throw new IOException("OSCAR configuration field must not be blank: " + field);
            return value;
        }
    }
}
