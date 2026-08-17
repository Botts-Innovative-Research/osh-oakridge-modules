package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import org.junit.Test;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class FileSystemStoreTest extends AbstractBucketStoreTest {

    private static final Path TEST_ROOT = Path.of("src/test/resources/test-root");

    IBucketStore bucketStore;

    @Override
    IBucketStore initBucketStore() throws IOException {
        bucketStore = new FileSystemBucketStore(TEST_ROOT);
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

}
