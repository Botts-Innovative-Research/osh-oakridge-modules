package com.botts.api.service.bucket;

import com.botts.impl.service.bucket.BucketSecurity;

import java.util.concurrent.ExecutorService;

public interface IBucketService {

    IBucketStore getBucketStore();

    String getPublicEndpointUrl();

    ExecutorService getThreadPool();

}
