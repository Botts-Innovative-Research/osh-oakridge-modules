package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.datastore.DataStoreException;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

public class BucketHandler extends AbstractHandler {

    IBucketStore bucketStore;
    IBucketService service;

    public BucketHandler(IBucketService service) {
        this.service = service;
        this.bucketStore = service.getBucketStore();
    }

    @Override
    public void list(RequestContext ctx) throws IOException {
        try {
            printBuckets(ctx.getOutputStream());
        } catch (DataStoreException e) {

        }
    }

    private void printBuckets(ServletOutputStream out) throws DataStoreException {
        bucketStore.listBuckets().forEach(bucketName -> {
            try {
                out.println(bucketName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void get(RequestContext ctx) {

    }

    @Override
    public void create(RequestContext ctx) {

    }

    @Override
    public void update(RequestContext ctx) {

    }

    @Override
    public void delete(RequestContext ctx) {

    }
}
