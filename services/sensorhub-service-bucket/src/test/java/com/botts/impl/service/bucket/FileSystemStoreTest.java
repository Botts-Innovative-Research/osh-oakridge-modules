package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import com.botts.impl.service.bucket.util.UploadPolicy;
import org.junit.Test;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class FileSystemStoreTest extends AbstractBucketStoreTest {

    private static final Path TEST_ROOT = Path.of("src/test/resources/test-root");
    private static final long TEST_MAX_FILE_SIZE_MB = 1;
    private static final long TEST_MAX_FILE_SIZE_BYTES = UploadPolicy.maxFileSizeBytes(TEST_MAX_FILE_SIZE_MB);

    IBucketStore bucketStore;

    @Override
    IBucketStore initBucketStore() throws IOException {
        bucketStore = new FileSystemBucketStore(TEST_ROOT, TEST_MAX_FILE_SIZE_MB);
        return bucketStore;
    }

    @Test
    public void testRejectTraversalUploadPath() throws Exception {
        bucketStore.createBucket(TEST_BUCKET);

        assertPutRejected("../config.txt", Collections.emptyMap());
        assertPutRejected("../../config.txt", Collections.emptyMap());
        assertPutRejected("nested/../../config.txt", Collections.emptyMap());
        assertPutRejected("/tmp/config.txt", Collections.emptyMap());
        assertPutRejected("C:\\temp\\config.txt", Collections.emptyMap());
    }

    @Test
    public void testBucketCreateAndDeleteCannotEscapeStoreRoot() throws Exception {
        Path absoluteRoot = TEST_ROOT.toAbsolutePath().normalize();
        Path outside = absoluteRoot.resolveSibling("bucket-escape-target");
        Path sentinel = outside.resolve("sentinel.txt");
        Files.createDirectories(outside);
        Files.writeString(sentinel, "must survive");
        try {
            assertBucketCreateRejected("../" + outside.getFileName());
            assertBucketCreateRejected("nested/bucket");
            assertBucketCreateRejected("nested\\bucket");
            assertBucketCreateRejected(outside.toString());
            assertFalse(bucketStore.bucketExists("../" + outside.getFileName()));

            try {
                bucketStore.deleteBucket("../" + outside.getFileName());
                fail("Expected escaping bucket delete to be rejected");
            } catch (DataStoreException expected) {
                // The external directory must never become a deletion root.
            }
            assertEquals("must survive", Files.readString(sentinel));
        } finally {
            Files.deleteIfExists(sentinel);
            Files.deleteIfExists(outside);
        }
    }

    @Test
    public void testBucketSymlinkIsNeverTreatedAsManagedDirectory() throws Exception {
        Path absoluteRoot = TEST_ROOT.toAbsolutePath().normalize();
        Path outside = absoluteRoot.resolveSibling("bucket-link-target");
        Path sentinel = outside.resolve("sentinel.txt");
        Path link = absoluteRoot.resolve("linked-bucket");
        Files.createDirectories(outside);
        Files.writeString(sentinel, "must survive");
        try {
            try {
                Files.createSymbolicLink(link, outside);
            } catch (UnsupportedOperationException | IOException e) {
                return;
            }
            assertFalse(bucketStore.bucketExists("linked-bucket"));
            try {
                bucketStore.deleteBucket("linked-bucket");
                fail("Expected linked bucket delete to be rejected");
            } catch (DataStoreException expected) {
                // Never follow a bucket-level link during recursive deletion.
            }
            assertEquals("must survive", Files.readString(sentinel));
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(sentinel);
            Files.deleteIfExists(outside);
        }
    }

    @Test
    public void testRejectBlockedUploadExtensions() throws Exception {
        bucketStore.createBucket(TEST_BUCKET);

        assertPutRejected("index.html", Map.of("Content-Type", "text/plain"));
        assertPutRejected("scripts/app.js", Map.of("Content-Type", "text/plain"));
        assertPutRejected("bin/tool.exe", Map.of("Content-Type", "application/octet-stream"));
        assertPutRejected("scripts/start.sh", Map.of("Content-Type", "text/plain"));
    }

    @Test
    public void testRejectBlockedUploadContentTypes() throws Exception {
        bucketStore.createBucket(TEST_BUCKET);

        assertPutRejected("file.txt", Map.of("Content-Type", "text/html; charset=UTF-8"));
        assertPutRejected("file.txt", Map.of("Content-Type", "application/javascript"));

        try {
            bucketStore.createObject(TEST_BUCKET, testData(), Map.of("Content-Type", "text/html"));
            fail("Expected HTML content type to be rejected");
        } catch (DataStoreException expected) {
            // expected
        }
    }

    @Test
    public void testRejectUploadThroughSymlink() throws Exception {
        bucketStore.createBucket(TEST_BUCKET);

        Path bucketPath = TEST_ROOT.resolve(TEST_BUCKET);
        Path outsideDir = TEST_ROOT.resolve("outside").toAbsolutePath().normalize();
        Files.createDirectories(outsideDir);
        Path link = bucketPath.resolve("link");
        try {
            Files.createSymbolicLink(link, outsideDir);
        } catch (UnsupportedOperationException | IOException e) {
            return;
        }

        assertPutRejected("link/file.txt", Collections.emptyMap());
    }

    @Test
    public void testRejectOversizedInputStreamUpload() throws Exception {
        bucketStore.createBucket(TEST_BUCKET);

        try {
            bucketStore.putObject(TEST_BUCKET, "too-large.txt",
                    new FixedSizeInputStream(TEST_MAX_FILE_SIZE_BYTES + 1),
                    Collections.emptyMap());
            fail("Expected oversized upload to be rejected");
        } catch (DataStoreException expected) {
            assertFalse("Oversized object should not exist",
                    bucketStore.objectExists(TEST_BUCKET, "too-large.txt"));
        }
    }

    @Test
    public void testRejectOversizedOutputStreamUpload() throws Exception {
        bucketStore.createBucket(TEST_BUCKET);

        byte[] chunk = new byte[1024 * 1024];
        OutputStream out = bucketStore.putObject(TEST_BUCKET, "too-large-output.txt", Collections.emptyMap());
        try {
            for (int i = 0; i <= TEST_MAX_FILE_SIZE_BYTES / chunk.length; i++)
                out.write(chunk);
            fail("Expected oversized output stream upload to be rejected");
        } catch (IOException expected) {
            // expected
        } finally {
            try {
                out.close();
            } catch (IOException ignored) {}
        }

        assertFalse("Oversized output stream object should not exist",
                bucketStore.objectExists(TEST_BUCKET, "too-large-output.txt"));
    }

    @Test
    public void testCreatedFilesAndDirectoriesUseRestrictedPermissions() throws Exception {
        bucketStore.createBucket(TEST_BUCKET);

        String objectKey = "nested/path/file.txt";
        bucketStore.putObject(TEST_BUCKET, objectKey, testData(), Collections.emptyMap());

        Path bucketPath = TEST_ROOT.resolve(TEST_BUCKET);
        Path nestedDir = bucketPath.resolve("nested");
        Path file = bucketPath.resolve(objectKey);

        if (!Files.getFileStore(file).supportsFileAttributeView("posix")) {
            assertFalse("Uploaded file should not be executable", file.toFile().canExecute());
            return;
        }

        Set<PosixFilePermission> bucketPermissions = Files.getPosixFilePermissions(bucketPath);
        Set<PosixFilePermission> directoryPermissions = Files.getPosixFilePermissions(nestedDir);
        Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(file);

        assertEquals(Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
        ), bucketPermissions);
        assertEquals(bucketPermissions, directoryPermissions);
        assertEquals(Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
        ), filePermissions);
        assertFalse("Uploaded file should not be executable", file.toFile().canExecute());
    }

    @Test
    public void testConcurrentOutputStreamCreationDoesNotRace() throws Exception {
        bucketStore.createBucket(TEST_BUCKET);
        String objectKey = "clips/camera/concurrent.mp4";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = concurrentPut(executor, ready, start, objectKey);
            Future<?> second = concurrentPut(executor, ready, start, objectKey);
            ready.await();
            start.countDown();

            assertFutureSucceeded(first);
            assertFutureSucceeded(second);
            assertTrue(bucketStore.objectExists(TEST_BUCKET, objectKey));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testPartialObjectsAreNeverListedOrReadableAsPublishedObjects() throws Exception {
        bucketStore.createBucket(TEST_BUCKET);
        String partialKey = "clips/camera/recording.mp4.partial-job";
        Path partial = Path.of(bucketStore.getWritableResourceURI(TEST_BUCKET, partialKey));
        Files.writeString(partial, "in progress");

        assertFalse(bucketStore.objectExists(TEST_BUCKET, partialKey));
        assertFalse(bucketStore.listObjects(TEST_BUCKET).contains(partialKey));
        assertEquals(0, bucketStore.getNumObjects(TEST_BUCKET));
        try {
            bucketStore.getObject(TEST_BUCKET, partialKey);
            fail("A partial must not be readable");
        } catch (DataStoreException expected) {
            // Hidden until atomically renamed.
        }
        try {
            bucketStore.getObjectSize(TEST_BUCKET, partialKey);
            fail("A partial must not expose metadata");
        } catch (DataStoreException expected) {
            // Hidden until atomically renamed.
        }

        String finalKey = "clips/camera/recording.mp4";
        Files.move(partial, Path.of(bucketStore.getWritableResourceURI(TEST_BUCKET, finalKey)));
        assertTrue(bucketStore.objectExists(TEST_BUCKET, finalKey));
        assertTrue(bucketStore.listObjects(TEST_BUCKET).contains(finalKey));
    }

    @Test
    public void testPrefixListingIsScopedAndHidesPartials() throws Exception {
        bucketStore.createBucket(TEST_BUCKET);
        bucketStore.putObject(TEST_BUCKET, "retained/event-a/frame-01.jpg", testData(), Collections.emptyMap());
        bucketStore.putObject(TEST_BUCKET, "retained/event-b/frame-01.jpg", testData(), Collections.emptyMap());
        Path partial = Path.of(bucketStore.getWritableResourceURI(
                TEST_BUCKET, "retained/event-a/frame-02.jpg.partial-job"));
        Files.writeString(partial, "in progress");

        assertEquals(List.of("retained/event-a/frame-01.jpg"),
                bucketStore.listObjects(TEST_BUCKET, "retained/event-a/"));
        assertTrue(bucketStore.listObjects(TEST_BUCKET, "retained/missing/").isEmpty());
    }

    private Future<?> concurrentPut(ExecutorService executor, CountDownLatch ready,
                                    CountDownLatch start, String objectKey) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            try (OutputStream output = bucketStore.putObject(
                    TEST_BUCKET, objectKey, Collections.emptyMap())) {
                output.write(1);
            }
            return null;
        });
    }

    private void assertFutureSucceeded(Future<?> future) throws Exception {
        try {
            future.get();
        } catch (ExecutionException e) {
            throw new AssertionError("Concurrent object creation failed", e.getCause());
        }
    }

    private void assertPutRejected(String key, Map<String, String> metadata) throws IOException {
        try {
            bucketStore.putObject(TEST_BUCKET, key, testData(), metadata);
            fail("Expected upload to be rejected: " + key);
        } catch (DataStoreException expected) {
            assertFalse("Rejected object should not exist: " + key, bucketStore.objectExists(TEST_BUCKET, key));
        }
    }

    private void assertBucketCreateRejected(String bucketName) {
        try {
            bucketStore.createBucket(bucketName);
            fail("Expected bucket name to be rejected: " + bucketName);
        } catch (DataStoreException expected) {
            // expected
        }
    }

    private ByteArrayInputStream testData() {
        return new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    }

    private static class FixedSizeInputStream extends InputStream {

        private long remaining;

        FixedSizeInputStream(long size) {
            this.remaining = size;
        }

        @Override
        public int read() {
            if (remaining <= 0)
                return -1;
            remaining--;
            return 0;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (remaining <= 0)
                return -1;
            int bytesRead = (int) Math.min(len, remaining);
            remaining -= bytesRead;
            return bytesRead;
        }
    }

}
