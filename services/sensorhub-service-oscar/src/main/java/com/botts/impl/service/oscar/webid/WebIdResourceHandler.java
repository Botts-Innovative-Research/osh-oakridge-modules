package com.botts.impl.service.oscar.webid;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;

public class WebIdResourceHandler extends DefaultObjectHandler {

    public WebIdResourceHandler(IBucketStore bucketStore) {
        super(bucketStore);
    }


}
