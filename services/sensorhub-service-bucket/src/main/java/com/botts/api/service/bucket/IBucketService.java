package com.botts.api.service.bucket;

import java.util.concurrent.ExecutorService;

public interface IBucketService {

    IBucketStore getBucketStore();

    String getPublicEndpointUrl();

    ExecutorService getThreadPool();

}
