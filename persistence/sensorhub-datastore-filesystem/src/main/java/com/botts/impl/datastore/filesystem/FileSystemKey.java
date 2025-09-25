package com.botts.impl.datastore.filesystem;

import com.botts.api.datastore.bucket.BucketStorageKey;

public class FileSystemKey extends BucketStorageKey {

    public FileSystemKey(String rootDir, String key) {
        super(rootDir, key);
    }

}
