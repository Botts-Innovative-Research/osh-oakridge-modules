package com.botts.api.service.bucket;

import java.util.concurrent.ExecutorService;

public interface IBucketService {

    IBucketStore getBucketStore();

    String getPublicEndpointUrl();

    ExecutorService getThreadPool();

    void registerObjectHandler(IObjectHandler handler);

    void unregisterObjectHandler(IObjectHandler handler);

    IObjectHandler getObjectHandler(String bucketName, String objectKey);

}
