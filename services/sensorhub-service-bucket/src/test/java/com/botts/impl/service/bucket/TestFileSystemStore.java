package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;

import java.io.IOException;
import java.nio.file.Path;

public class TestFileSystemStore extends AbstractBucketStoreTest {

    IBucketStore bucketStore;

    @Override
    IBucketStore initBucketStore() throws IOException {
        bucketStore = new FileSystemBucketStore(Path.of("src/test/resources/test-bucket"));
        return bucketStore;
    }

}
