package com.botts.impl.service.bucket.filesystem;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FileSystemBucketStoreDurabilityTest {

    @Test
    public void retriesParentBarrierForEveryNestedObjectDirectory() throws Exception {
        Path temporary = Files.createTempDirectory("bucket-directory-durability-");
        try {
            Path bucket = Files.createDirectory(temporary.resolve("videos"));
            Path target = bucket.resolve("year/month/day");
            Set<Path> durableEntries = new HashSet<>();
            List<Path> forcedDirectories = new ArrayList<>();
            AtomicBoolean failSecondBarrier = new AtomicBoolean(true);

            IOException failure = assertThrows(IOException.class,
                    () -> FileSystemBucketStore.createObjectParentDirectoriesDurably(
                            bucket, target, durableEntries, directory -> {
                                forcedDirectories.add(directory);
                                if (directory.equals(bucket.resolve("year"))
                                        && failSecondBarrier.getAndSet(false)) {
                                    throw new IOException("injected directory barrier failure");
                                }
                            }));
            assertTrue(failure.getMessage().contains("injected"));
            assertTrue(Files.isDirectory(bucket.resolve("year/month")));
            assertFalse(Files.exists(target));

            FileSystemBucketStore.createObjectParentDirectoriesDurably(
                    bucket, target, durableEntries, forcedDirectories::add);

            assertEquals(List.of(
                    bucket,
                    bucket.resolve("year"),
                    bucket.resolve("year"),
                    bucket.resolve("year/month")), forcedDirectories);
            assertTrue(Files.isDirectory(target));
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
