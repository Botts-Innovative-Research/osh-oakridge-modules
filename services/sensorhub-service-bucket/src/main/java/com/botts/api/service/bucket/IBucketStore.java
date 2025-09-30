package com.botts.api.service.bucket;

import org.sensorhub.api.datastore.DataStoreException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface IBucketStore {

    String BUCKET_NOT_FOUND = "Bucket not found";
    String FAILED_CREATE_BUCKET = "Failed to create bucket ";
    String FAILED_DELETE_BUCKET = "Failed to delete bucket ";
    String FAILED_GET_BUCKET = "Failed to get bucket ";
    String FAILED_LIST_BUCKETS = "Failed to list buckets";

    String OBJECT_NOT_FOUND = "Object not found in bucket ";
    String FAILED_CREATE_OBJECT = "Failed to create object in bucket ";
    String FAILED_PUT_OBJECT = "Failed to put object in bucket ";
    String FAILED_DELETE_OBJECT = "Failed to delete object in bucket ";
    String FAILED_GET_OBJECT = "Failed to get object from bucket ";
    String FAILED_LIST_OBJECTS = "Failed to list objects in bucket ";

    String UNABLE_DETERMINE_MIME_TYPE = "Unable to determine mime type for object ";


    // Buckets
    boolean bucketExists(String bucketName);

    void createBucket(String bucketName) throws DataStoreException;

    void deleteBucket(String bucketName) throws DataStoreException;

    List<String> listBuckets() throws DataStoreException;

    long getNumBuckets();

    // Objects

    boolean objectExists(String bucketName, String objectName);

    String createObject(String bucketName, InputStream data, Map<String, String> metadata) throws DataStoreException;

    void putObject(String bucketName, String key, InputStream data, Map<String, String> metadata) throws DataStoreException;

    OutputStream putObject(String bucketName, String key, Map<String, String> metadata) throws DataStoreException;

    InputStream getObject(String bucketName, String key) throws DataStoreException;

    String getObjectMimeType(String bucketName, String key) throws DataStoreException;

    void deleteObject(String bucketName, String key) throws DataStoreException;

    List<String> listObjects(String bucketName) throws DataStoreException;

    long getNumObjects(String bucketName);

    String getResourceURI(String bucketName, String key) throws DataStoreException;

}
