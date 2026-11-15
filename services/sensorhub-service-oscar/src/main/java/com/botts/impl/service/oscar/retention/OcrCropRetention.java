package com.botts.impl.service.oscar.retention;

import com.botts.impl.service.oscar.media.MediaManifest;
import com.botts.impl.service.oscar.media.MediaSetStore;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Date-partitioned lifecycle manager for OCR evidence crops under {@code ocr/crops/<yyyy-MM-dd>/}.
 *
 * <p>Unlike a fixed file-count FIFO cap, this sweeper inspects only top-level {@code yyyy-MM-dd}
 * partition names during age sweeps ({@code O(days)} without opening recent day directories) and
 * aligns crop lifetime with retained camera middle frames ({@code frame-3.jpg}), media holds,
 * operator adjudications, and disk storage-pressure thresholds.</p>
 */
public final class OcrCropRetention {

    public static final String OCR_BUCKET_DIR = "ocr";
    public static final String CROPS_SUBDIR = "crops";
    public static final String OBS_FILENAME_PREFIX = "obs-";

    private static final Pattern DATE_DIR_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private final Path storagePath;
    private final MediaSetStore mediaStore;
    private final Predicate<String> operatorAdjudicatedPredicate;
    private final Logger log;

    public record SweepResult(int deletedFiles, long freedBytes) {}

    private record DatePartition(LocalDate date, Path dir) {}

    public OcrCropRetention(Path storagePath,
                            MediaSetStore mediaStore,
                            Predicate<String> operatorAdjudicatedPredicate,
                            Logger log) {
        this.storagePath = Objects.requireNonNull(storagePath, "storagePath");
        this.mediaStore = Objects.requireNonNull(mediaStore, "mediaStore");
        this.operatorAdjudicatedPredicate = operatorAdjudicatedPredicate != null
                ? operatorAdjudicatedPredicate : id -> false;
        this.log = Objects.requireNonNull(log, "log");
    }

    /**
     * Prunes {@code ocr/crops/yyyy-MM-dd/} partitions strictly older than {@code retentionDays}
     * UTC days, preserving crops whose occupancy still retains its middle frame
     * ({@code frame-3.jpg}), is under a media hold, or has been adjudicated by an operator.
     */
    public SweepResult pruneExpiredDatePartitions(Instant now, int retentionDays, int maxDeletes)
            throws IOException {
        if (retentionDays <= 0 || maxDeletes <= 0)
            return new SweepResult(0, 0L);

        List<DatePartition> partitions = listDatePartitions();
        if (partitions.isEmpty())
            return new SweepResult(0, 0L);

        LocalDate cutoffDate = LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(retentionDays);
        Map<String, Boolean> protectionCache = new HashMap<>();
        int deletedFiles = 0;
        long freedBytes = 0L;

        for (DatePartition partition : partitions) {
            if (!partition.date().isBefore(cutoffDate))
                break;
            if (deletedFiles >= maxDeletes || Thread.currentThread().isInterrupted())
                break;

            List<Path> files = listRegularCropFiles(partition.dir());
            for (Path file : files) {
                if (deletedFiles >= maxDeletes || Thread.currentThread().isInterrupted())
                    break;
                Optional<String> obsId = extractOccupancyObsId(file.getFileName().toString());
                if (obsId.isPresent() && isProtectedFromAgePruning(obsId.get(), protectionCache))
                    continue;

                long size = safeSize(file);
                if (Files.deleteIfExists(file)) {
                    deletedFiles++;
                    freedBytes += size;
                }
            }

            deleteDirectoryIfEmpty(partition.dir());
        }

        if (deletedFiles > 0) {
            log.info("Pruned {} expired OCR evidence crop(s) ({} bytes) older than {} day(s)",
                    deletedFiles, freedBytes, retentionDays);
        }
        return new SweepResult(deletedFiles, freedBytes);
    }

    /**
     * Reclaims oldest eligible OCR evidence crops under disk storage pressure ({@code 85% -> 80%}),
     * skipping files newer than {@code cutoff} and crops linked to held or operator-adjudicated
     * occupancies.
     */
    public SweepResult reclaimUnderStoragePressure(Instant cutoff, long bytesNeeded, int maxDeletes)
            throws IOException {
        if (bytesNeeded <= 0 || maxDeletes <= 0)
            return new SweepResult(0, 0L);

        List<DatePartition> partitions = listDatePartitions();
        if (partitions.isEmpty())
            return new SweepResult(0, 0L);

        Map<String, Boolean> protectionCache = new HashMap<>();
        int deletedFiles = 0;
        long freedBytes = 0L;

        for (DatePartition partition : partitions) {
            if (deletedFiles >= maxDeletes || freedBytes >= bytesNeeded || Thread.currentThread().isInterrupted())
                break;

            List<Path> files = listRegularCropFiles(partition.dir());
            for (Path file : files) {
                if (deletedFiles >= maxDeletes || freedBytes >= bytesNeeded || Thread.currentThread().isInterrupted())
                    break;
                Instant modified = safeLastModified(file);
                if (modified != null && modified.isAfter(cutoff))
                    continue;

                Optional<String> obsId = extractOccupancyObsId(file.getFileName().toString());
                if (obsId.isPresent() && isProtectedFromPressureCleanup(obsId.get(), protectionCache))
                    continue;

                long size = safeSize(file);
                if (Files.deleteIfExists(file)) {
                    deletedFiles++;
                    freedBytes += size;
                }
            }

            deleteDirectoryIfEmpty(partition.dir());
        }

        return new SweepResult(deletedFiles, freedBytes);
    }

    public static Optional<String> extractOccupancyObsId(String keyOrFileName) {
        if (keyOrFileName == null || keyOrFileName.isBlank())
            return Optional.empty();
        String norm = keyOrFileName.replace('\\', '/');
        int slash = norm.lastIndexOf('/');
        String fileName = slash >= 0 ? norm.substring(slash + 1) : norm;
        if (!fileName.startsWith(OBS_FILENAME_PREFIX) || !fileName.endsWith(".jpg"))
            return Optional.empty();
        int sep = fileName.lastIndexOf('_');
        if (sep <= OBS_FILENAME_PREFIX.length())
            return Optional.empty();
        String encoded = fileName.substring(OBS_FILENAME_PREFIX.length(), sep);
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            String obsId = new String(decoded, StandardCharsets.UTF_8).trim();
            return obsId.isEmpty() ? Optional.empty() : Optional.of(obsId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private boolean isProtectedFromAgePruning(String obsId, Map<String, Boolean> cache) {
        return cache.computeIfAbsent(obsId, id -> {
            try {
                // Keep OCR crop for at least the lifetime of the occupancy's retained middle frame (frame-3.jpg) or hold
                if (!mediaStore.findEvent(id, 1).isEmpty())
                    return true;
            } catch (Exception ignored) {
            }
            try {
                return operatorAdjudicatedPredicate.test(id);
            } catch (Exception ignored) {
                return false;
            }
        });
    }

    private boolean isProtectedFromPressureCleanup(String obsId, Map<String, Boolean> cache) {
        return cache.computeIfAbsent(obsId, id -> {
            try {
                for (MediaSetStore.ManifestEntry entry : mediaStore.findEvent(id, 8)) {
                    MediaManifest m = entry.manifest();
                    if (m.isHeld() || m.lifecycleState() == MediaManifest.LifecycleState.HELD)
                        return true;
                }
            } catch (Exception ignored) {
            }
            try {
                return operatorAdjudicatedPredicate.test(id);
            } catch (Exception ignored) {
                return false;
            }
        });
    }

    private List<DatePartition> listDatePartitions() throws IOException {
        Path ocrDir = storagePath.resolve(OCR_BUCKET_DIR);
        if (!Files.isDirectory(ocrDir, LinkOption.NOFOLLOW_LINKS))
            return List.of();
        Path cropsRoot = ocrDir.resolve(CROPS_SUBDIR);
        if (!Files.isDirectory(cropsRoot, LinkOption.NOFOLLOW_LINKS))
            return List.of();

        List<DatePartition> partitions = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cropsRoot)) {
            for (Path child : stream) {
                if (!Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS))
                    continue;
                String name = child.getFileName().toString();
                if (!DATE_DIR_PATTERN.matcher(name).matches())
                    continue;
                try {
                    partitions.add(new DatePartition(LocalDate.parse(name), child));
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        partitions.sort(Comparator.comparing(DatePartition::date));
        return partitions;
    }

    private static List<Path> listRegularCropFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS))
            return List.of();
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS))
                    continue;
                String name = entry.getFileName().toString();
                if (name.endsWith(".jpg") && !name.contains(".partial"))
                    files.add(entry);
            }
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return files;
    }

    private static void deleteDirectoryIfEmpty(Path dir) {
        try {
            if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS))
                return;
            try (DirectoryStream<Path> check = Files.newDirectoryStream(dir)) {
                if (check.iterator().hasNext())
                    return;
            }
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
        }
    }

    private static long safeSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0L;
        }
    }

    private static Instant safeLastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toInstant();
        } catch (IOException e) {
            return null;
        }
    }
}
