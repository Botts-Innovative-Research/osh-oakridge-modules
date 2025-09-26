package com.botts.impl.service.bucket.handler;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.api.service.bucket.IResourceHandler;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;
import org.sensorhub.api.datastore.DataStoreException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class ObjectHandler implements IResourceHandler {

    private final IBucketStore bucketStore;

    public ObjectHandler(IBucketStore bucketStore) {
        this.bucketStore = bucketStore;
    }

    @Override
    public void doGet(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = ctx.getObjectKey();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).get);

        boolean objectExists = bucketStore.objectExists(bucketName, objectKey);
        if (!objectExists)
            throw ServiceErrors.notFound(objectKey + " in bucket " + bucketName);

        String mimeType;
        try {
            mimeType = bucketStore.getObjectMimeType(bucketName, objectKey);
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.UNABLE_DETERMINE_MIME_TYPE + objectKey);
        }

        if (mimeType == null || mimeType.isBlank())
            throw ServiceErrors.internalError(IBucketStore.UNABLE_DETERMINE_MIME_TYPE + objectKey);

        try {
            ctx.getResponse().setContentType(mimeType);
            InputStream objectData = bucketStore.getObject(bucketName, objectKey);
            objectData.transferTo(ctx.getResponse().getOutputStream());
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_GET_OBJECT + bucketName);
        }
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
        // /buckets/{bucketName}
        var bucketName = ctx.getBucketName();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).create);

        if (ctx.hasObjectKey())
            throw ServiceErrors.unsupportedOperation("You can only create an object from the root of a bucket");

        if (ctx.getHeaders().get("Content-Type") == null)
            throw ServiceErrors.badRequest("Content-Type header is required");

        String newObjectKey;
        try (InputStream data = ctx.getRequest().getInputStream()) {
            newObjectKey = bucketStore.createObject(bucketName, data, ctx.getHeaders());
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_CREATE_OBJECT + bucketName);
        }

        if (newObjectKey == null)
            throw ServiceErrors.internalError(IBucketStore.FAILED_CREATE_OBJECT + bucketName);

        ctx.getResponse().setStatus(HttpServletResponse.SC_CREATED);
        ctx.getResponse().setHeader("Location", ctx.getResourceURL(newObjectKey));
    }

    // /buckets/{bucketName}/{newOrExistingObjectKey}
    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = ctx.getObjectKey();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).put);

        int successStatus = bucketStore.objectExists(bucketName, objectKey) ?
                HttpServletResponse.SC_OK : HttpServletResponse.SC_CREATED;

        try (InputStream data = ctx.getRequest().getInputStream()) {
            bucketStore.putObject(bucketName, objectKey, data, ctx.getHeaders());
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_PUT_OBJECT + bucketName);
        }

        ctx.getResponse().setStatus(successStatus);
    }

    // /buckets/{bucketName}/{objectKey}
    @Override
    public void doDelete(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = ctx.getObjectKey();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).delete);

        try {
            bucketStore.deleteObject(bucketName, objectKey);
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_DELETE_OBJECT + bucketName);
        }
    }

}
