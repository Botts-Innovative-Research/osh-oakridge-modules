package com.botts.impl.service.bucket.handler;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;
import org.sensorhub.api.datastore.DataStoreException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class MP4Handler extends DefaultObjectHandler {

    private static final Pattern PATTERN = Pattern.compile(".*\\.(mp4)$");

    public MP4Handler(IBucketStore bucketStore) {
        super(bucketStore);
    }

    @Override
    public void doGet(RequestContext ctx) throws IOException, SecurityException {
        var bucketName = ctx.getBucketName();
        var objectKey = Paths.get(ctx.getObjectKey()).toString();
        boolean objectExists = bucketStore.objectExists(bucketName, objectKey);
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).get);

        if (!objectExists)
            throw ServiceErrors.notFound(objectKey + " in bucket " + bucketName);

        String mimeType;
        try {
            mimeType = bucketStore.getObjectMimeType(bucketName, objectKey);
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.UNABLE_DETERMINE_MIME_TYPE + objectKey);
        }

        if (mimeType == null || mimeType.isBlank())
            mimeType =  "application/octet-stream";

        var response = ctx.getResponse();
        response.setContentType(mimeType);
        var request = ctx.getRequest();

        try {
            long size = bucketStore.getObjectSize(bucketName, objectKey);
            String rangeHeader = request.getHeader("Range");

            if (rangeHeader == null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setHeader("Content-Length", String.valueOf(size));

                try (InputStream in = bucketStore.getObject(bucketName, objectKey);
                     OutputStream out = response.getOutputStream()) {
                    in.transferTo(out);
                }
                return;
            }

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Accept-Ranges", "bytes");

            long start = 0;
            long end = size - 1;
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            if (!ranges[0].isEmpty()) start = Long.parseLong(ranges[0]);
            if (ranges.length > 1 && !ranges[1].isEmpty()) end = Long.parseLong(ranges[1]);
            if (end >= size) end = size - 1;
            long contentLength = end - start + 1;

            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + size);
            response.setHeader("Content-Length", String.valueOf(contentLength));

            try (InputStream in = bucketStore.getObject(bucketName, objectKey);
                 OutputStream out = response.getOutputStream()) {

                in.skip(start); // skip to start of range
                byte[] buffer = new byte[8192];
                long remaining = contentLength;
                int bytesRead;
                while ((bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1 && remaining > 0) {
                    out.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }

        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_GET_OBJECT + bucketName);
        }
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
        throw ServiceErrors.unsupportedOperation("POST not supported on videos");
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {
        throw ServiceErrors.unsupportedOperation("PUT not supported on videos");
    }

    @Override
    public void doDelete(RequestContext ctx) throws IOException, SecurityException {
        throw ServiceErrors.unsupportedOperation("DELETE not supported on videos");
    }

    @Override
    public String getObjectPattern() {
        return PATTERN.pattern();
    }

}
