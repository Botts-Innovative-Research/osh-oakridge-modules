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
import java.util.Map;
import java.util.Set;

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

    private void assertPutRejected(String key, Map<String, String> metadata) throws IOException {
        try {
            bucketStore.putObject(TEST_BUCKET, key, testData(), metadata);
            fail("Expected upload to be rejected: " + key);
        } catch (DataStoreException expected) {
            assertFalse("Rejected object should not exist: " + key, bucketStore.objectExists(TEST_BUCKET, key));
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
