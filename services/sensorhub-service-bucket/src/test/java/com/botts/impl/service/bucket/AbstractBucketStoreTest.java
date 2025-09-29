package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IBucketStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public abstract class AbstractBucketStoreTest {

    IBucketStore bucketStore;
    protected static final String TEST_BUCKET = "videos";

    abstract IBucketStore initBucketStore() throws IOException;

    @Before
    public void setup() throws IOException {
        bucketStore = initBucketStore();
    }

    /// Bucket tests

    private void addTestBucket() throws DataStoreException {
        bucketStore.createBucket(TEST_BUCKET);
    }

    @Test
    public void testAddBucket() throws DataStoreException {
        addTestBucket();
        assertTrue(bucketStore.bucketExists(TEST_BUCKET));
    }

    @Test
    public void testListBuckets() throws DataStoreException {
        addTestBucket();
        List<String> buckets = bucketStore.listBuckets();
        assertFalse(buckets.isEmpty());
        System.out.println(buckets);
    }

    @Test
    public void testInvalidBucket() {
        assertFalse(bucketStore.bucketExists("invalid-bucket"));
    }

    @Test
    public void testGetNumBuckets() throws DataStoreException {
        var test1 = "test1";
        var test2 = "test2";
        bucketStore.createBucket(test1);
        bucketStore.createBucket(test2);
        long numBuckets = bucketStore.getNumBuckets();
        assertTrue(numBuckets == 2);
        bucketStore.deleteBucket(test1);
        assertTrue(numBuckets == 1);
    }

    @Test
    public void testAddAndDeleteBucket() throws DataStoreException {
        String tempBucket = "temp";
        bucketStore.createBucket(tempBucket);
        assertTrue(bucketStore.bucketExists(tempBucket));
        bucketStore.deleteBucket(tempBucket);
        assertFalse(bucketStore.bucketExists(tempBucket));
    }

    /// Object tests

    private String addTestObject() throws FileNotFoundException, DataStoreException {
        InputStream stream = new FileInputStream("src/test/resources/test-object.txt");
        String key = bucketStore.createObject(TEST_BUCKET, stream, Collections.emptyMap());
        assertNotNull(key);
        assertFalse(key.isBlank());
        return key;
    }

    @Test
    public void testAddObjectNoKey() throws DataStoreException, FileNotFoundException {
        addTestBucket();
        String key = addTestObject();
        System.out.println(key);
        assertTrue(bucketStore.objectExists(TEST_BUCKET, key));
    }

    @Test
    public void testGetTestObjectData() throws DataStoreException, IOException {
        addTestBucket();
        String key = addTestObject();
        assertTrue(bucketStore.objectExists(TEST_BUCKET, key));
        InputStream stream = bucketStore.getObject(TEST_BUCKET, key);
        String objectContents = new String(stream.readAllBytes());
        assertNotNull(objectContents);
        assertFalse(objectContents.isBlank());
        System.out.println(objectContents);
    }

    @After
    public void cleanup() throws DataStoreException {
        List<String> buckets = bucketStore.listBuckets();
        for (var bucket : buckets)
            bucketStore.deleteBucket(bucket);
    }

}
