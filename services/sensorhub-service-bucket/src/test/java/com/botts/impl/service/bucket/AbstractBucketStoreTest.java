package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IBucketStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public abstract class AbstractBucketStoreTest {

    IBucketStore bucketStore;
    protected static final String TEST_BUCKET = "test-bucket";
    protected static final String TEST_OBJECT_PATH = "src/test/resources/test-object.txt";

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
        cleanup();
        var test1 = "test1";
        var test2 = "test2";
        bucketStore.createBucket(test1);
        bucketStore.createBucket(test2);
        long numBuckets = bucketStore.getNumBuckets();
        List<String> buckets = bucketStore.listBuckets();
        System.out.println(buckets);
        assertTrue(numBuckets == 2);
        bucketStore.deleteBucket(test1);
        numBuckets = bucketStore.getNumBuckets();
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

    private InputStream getTestObjectInputStream() throws FileNotFoundException {
        return new FileInputStream(TEST_OBJECT_PATH);
    }

    private InputStream getTestObjectData() throws IOException {
        var baos = new ByteArrayOutputStream();
        baos.write("hello im a new object".getBytes(StandardCharsets.UTF_8));
        InputStream stream = new ByteArrayInputStream(baos.toByteArray());
        return stream;
    }

    private String addTestObject() throws FileNotFoundException, DataStoreException {
        var stream = getTestObjectInputStream();
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

    @Test
    public void testPutObjectWithInputStream() throws DataStoreException, FileNotFoundException {
        // No path in key
        addTestBucket();
        String testObjName = "new-object.txt";
        bucketStore.putObject(TEST_BUCKET, testObjName, getTestObjectInputStream(), Collections.emptyMap());
        assertTrue(bucketStore.objectExists(TEST_BUCKET, testObjName));
        // With path in key
        bucketStore.putObject(TEST_BUCKET, "subdir1/subdir2/subdir3/" + testObjName, getTestObjectInputStream(), Collections.emptyMap());
        assertTrue(bucketStore.objectExists(TEST_BUCKET, testObjName));
    }

    @Test
    public void testPutObjectWithOutputStream() throws DataStoreException, IOException {
        addTestBucket();
        String testObjName = "new-object2.txt";
        OutputStream out = bucketStore.putObject(TEST_BUCKET, testObjName, Collections.emptyMap());
        getTestObjectData().transferTo(out);

        // Test object exists and has the test data
        assertTrue(bucketStore.objectExists(TEST_BUCKET, testObjName));
        InputStream stream = bucketStore.getObject(TEST_BUCKET, testObjName);
        String objectContents = new String(stream.readAllBytes());
        assertNotNull(objectContents);
        assertFalse(objectContents.isBlank());
        System.out.println(objectContents);
    }

    @Test
    public void testOverwriteObjectWithOutputStream() throws DataStoreException, IOException {
        testPutObjectWithOutputStream();

        // Overwrite object
        String testObjName = "new-object2.txt";
        OutputStream out = bucketStore.putObject(TEST_BUCKET, testObjName, Collections.emptyMap());
        getTestObjectData().transferTo(out);

        // Test object exists and has the test data
        assertTrue(bucketStore.objectExists(TEST_BUCKET, testObjName));
        InputStream stream = bucketStore.getObject(TEST_BUCKET, testObjName);
        String objectContents = new String(stream.readAllBytes());
        assertNotNull(objectContents);
        assertFalse(objectContents.isBlank());
        System.out.println(objectContents);
    }

    @Test
    public void testGetObjectMimeType() throws DataStoreException, FileNotFoundException {
        addTestBucket();
        String key = "new-object3.txt";
        bucketStore.putObject(TEST_BUCKET, key, getTestObjectInputStream(), Collections.emptyMap());
        assertTrue(bucketStore.objectExists(TEST_BUCKET, key));
        String mimeType = bucketStore.getObjectMimeType(TEST_BUCKET, key);
        assertNotNull(mimeType);
        assertFalse(mimeType.isBlank());
        System.out.println(mimeType);
    }

    @Test
    public void testDeleteObject() throws DataStoreException, FileNotFoundException {
        addTestBucket();
        String key = addTestObject();
        assertTrue(bucketStore.objectExists(TEST_BUCKET, key));
        bucketStore.deleteObject(TEST_BUCKET, key);
        assertFalse(bucketStore.objectExists(TEST_BUCKET, key));
    }

    @Test
    public void testGetAllObjectsAndNumObjects() throws DataStoreException, FileNotFoundException {
        addTestBucket();
        int numObjects = 1000;
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < numObjects; i++) {
            String key = addTestObject();
            expected.add(key);
            System.out.println("Added object " + key);
        }

        List<String> result = bucketStore.listObjects(TEST_BUCKET);
        assertTrue(result.size()>1);
        assertEquals(result.size(), expected.size());
        System.out.println(result);

        long numRes = bucketStore.getNumObjects(TEST_BUCKET);
        assertTrue(numRes > 1);
        assertEquals(expected.size(), numRes);
        System.out.println(numRes);
    }

    @Test
    public void testGetResourceURI() throws DataStoreException, FileNotFoundException {
        addTestBucket();
        String key = addTestObject();
        String resourceURI = bucketStore.getResourceURI(TEST_BUCKET, key);
        assertNotNull(resourceURI);
        assertFalse(resourceURI.isBlank());
        System.out.println(resourceURI);
    }

    @After
    public void cleanup() throws DataStoreException {
        List<String> buckets = bucketStore.listBuckets();
        for (var bucket : buckets)
            bucketStore.deleteBucket(bucket);
    }

}
