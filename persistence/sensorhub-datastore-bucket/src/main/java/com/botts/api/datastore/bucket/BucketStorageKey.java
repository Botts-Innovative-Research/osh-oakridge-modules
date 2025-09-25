package com.botts.api.datastore.bucket;

public class BucketStorageKey {

    private String bucketName;
    private String key;

    public BucketStorageKey(String bucketName, String key) {
        this.bucketName = bucketName;
        this.key = key;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getKey() {
        return key;
    }

}
