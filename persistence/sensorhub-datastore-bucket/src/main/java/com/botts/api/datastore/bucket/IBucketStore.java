package com.botts.api.datastore.bucket;

import org.sensorhub.api.datastore.IDataStore;
import org.sensorhub.api.datastore.ValueField;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

public interface IBucketStore extends IDataStore<BucketStorageKey, BucketStorageObject, IBucketStore.BucketStorageField, BucketStorageQueryFilter> {

    class BucketStorageField extends ValueField {
        // TODO: Add associated metadata

        public BucketStorageField(String name) {
            super(name);
        }
    }

    // Bucket management

    void createBucket(String bucketName, Map<String, String> metadata);

    void deleteBucket(String bucketName);

    boolean bucketExists(String bucketName);

    Stream<String> listBuckets();

    // Object storage

    BucketStorageKey putObject(String bucketName, String key, InputStream data, String contentType, Map<String, String> metadata);

    BucketStorageObject getObject(String bucketName, String key);

    InputStream getObjectStream(String bucketName, String key);

    boolean deleteObject(String bucketName, String key);

    void copyObject(String sourceBucket, String sourceKey, String targetBucket, String targetKey);

    Stream<BucketStorageObject> listObjects(String bucketName);

    // TODO: Generate URL for S3, GCP, etc. instead of direct download


}
