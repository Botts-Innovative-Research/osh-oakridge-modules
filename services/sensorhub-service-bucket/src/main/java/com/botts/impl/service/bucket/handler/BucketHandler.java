package com.botts.impl.service.bucket.handler;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.api.service.bucket.IResourceHandler;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;
import org.sensorhub.api.datastore.DataStoreException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BucketHandler implements IResourceHandler {

    IBucketStore bucketStore;
    IResourceHandler objectHandler;

    public BucketHandler(IBucketStore bucketStore, IResourceHandler objectHandler) {
        this.bucketStore = bucketStore;
        this.objectHandler = objectHandler;
    }

    @Override
    public void doGet(RequestContext ctx) throws IOException {
        var sec = ctx.getSecurityHandler();

        // Only handle /buckets at this level
        if (!ctx.hasBucketName()) {
            sec.checkPermission(sec.api_list);
            listBuckets(ctx);
        } else {
            sec.checkPermission(sec.api_get);
            var bucketName = ctx.getBucketName();
            if (!bucketStore.bucketExists(bucketName))
                throw ServiceErrors.notFound(bucketName);

            if (!sec.getBucketPermissions().containsKey(bucketName))
                throw ServiceErrors.forbidden(bucketName);

            var bucketPerms = sec.getBucketPermissions().get(bucketName);
            if (ctx.hasObjectKey()) {
                sec.checkPermission(bucketPerms.get);
                objectHandler.doGet(ctx);
            } else {
                sec.checkPermission(bucketPerms.list);
                listObjects(ctx);
            }
        }
    }

    private void listBuckets(RequestContext ctx) throws IOException {
        try {
            var buckets = bucketStore.listBuckets();
            var json = ctx.getJsonWriter();
            json.beginArray();
            for (var bucket : buckets)
                json.value(ctx.getResourceURL(bucket));
            json.endArray();
            json.flush();
            json.close();
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_LIST_BUCKETS);
        }
    }

    private void listObjects(RequestContext ctx) throws IOException {
        try {
            var objects = bucketStore.listObjects(ctx.getBucketName());
            var json = ctx.getJsonWriter();
            json.beginArray();
            for (var obj : objects)
                json.value(ctx.getResourceURL(obj));
            json.endArray();
            json.flush();
            json.close();
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_LIST_OBJECTS);
        }
    }

    // Don't allow creation of buckets with no bucket name from /buckets
    @Override
    public void doPost(RequestContext ctx) throws IOException {
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.api_create);
        var bucketName = ctx.getBucketName();

        if (ctx.hasBucketName()) {
            if (!bucketStore.bucketExists(bucketName))
                throw ServiceErrors.notFound(bucketName);
            objectHandler.doPost(ctx);
        } else
            throw ServiceErrors.unsupportedOperation("Creating bucket via POST is not currently supported");
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException {
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.api_put);

        if (!ctx.hasBucketName())
            throw ServiceErrors.unsupportedOperation("PUT requires /buckets/{nonExistingBucket}");

        var bucketName = ctx.getBucketName();
        boolean bucketExists = bucketStore.bucketExists(bucketName);

        if (!ctx.hasObjectKey()) {
            if (bucketExists)
                throw ServiceErrors.unsupportedOperation("Updating existing buckets is not currently supported");

            try {
                bucketStore.createBucket(bucketName);
                // Add permissions for this bucket
                sec.addBucket(bucketName);
                ctx.getResponse().setStatus(HttpServletResponse.SC_CREATED);
                ctx.getResponse().setHeader("Location", ctx.getRequest().getRequestURL().toString());
            } catch (DataStoreException e) {
                throw ServiceErrors.internalError(IBucketStore.FAILED_CREATE_BUCKET + bucketName);
            }
        } else {
            if (!bucketExists)
                throw ServiceErrors.notFound(bucketName);

            if (!sec.getBucketPermissions().containsKey(bucketName))
                throw ServiceErrors.forbidden(bucketName);

            objectHandler.doPut(ctx);
        }
    }

    @Override
    public void doDelete(RequestContext ctx) throws IOException {
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.api_delete);

        if (!ctx.hasBucketName())
            throw ServiceErrors.badRequest("Must specify a bucket to delete");

        var bucketName = ctx.getBucketName();
        if (!bucketStore.bucketExists(bucketName))
            throw ServiceErrors.notFound(bucketName);

        if (!sec.getBucketPermissions().containsKey(bucketName))
            throw ServiceErrors.forbidden(bucketName);

        if (!ctx.hasObjectKey()) {
            try {
                bucketStore.deleteBucket(bucketName);
            } catch (DataStoreException e) {
                throw ServiceErrors.internalError(IBucketStore.FAILED_DELETE_BUCKET + bucketName);
            }
        } else {
            objectHandler.doDelete(ctx);
        }
    }

}
