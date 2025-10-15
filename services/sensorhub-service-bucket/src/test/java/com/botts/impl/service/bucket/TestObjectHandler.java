package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.botts.impl.service.bucket.util.RequestContext;

import java.io.IOException;
import java.util.regex.Pattern;

public class TestObjectHandler extends DefaultObjectHandler {

    private final Pattern pattern = Pattern.compile(".*\\.test");

    public TestObjectHandler(IBucketStore bucketStore) {
        super(bucketStore);
    }

    @Override
    public String getObjectPattern() {
        return pattern.pattern();
    }

    @Override
    public void doGet(RequestContext ctx) throws IOException, SecurityException {
        System.out.println("Successfully using the test object handler!");
        super.doGet(ctx);
    }

}
