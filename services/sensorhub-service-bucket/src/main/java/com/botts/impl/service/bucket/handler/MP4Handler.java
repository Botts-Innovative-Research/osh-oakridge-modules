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
            response.setHeader("Accept-Ranges", "bytes");

            if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setHeader("Content-Length", String.valueOf(size));

                try (InputStream in = bucketStore.getObject(bucketName, objectKey);
                     OutputStream out = response.getOutputStream()) {
                    byte[] buffer = new byte[262_144];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                return;
            }

            long start = 0;
            long end = size - 1;
            String rangeSpec = rangeHeader.substring("bytes=".length()).trim();
            int commaIdx = rangeSpec.indexOf(',');
            if (commaIdx >= 0) {
                rangeSpec = rangeSpec.substring(0, commaIdx).trim();
            }
            String[] ranges = rangeSpec.split("-", -1);
            try {
                if (ranges[0].isEmpty()) {
                    long suffixLength = ranges.length > 1 && !ranges[1].isEmpty()
                            ? Long.parseLong(ranges[1]) : 0L;
                    start = Math.max(0L, size - Math.max(1L, suffixLength));
                } else {
                    start = Long.parseLong(ranges[0]);
                    if (ranges.length > 1 && !ranges[1].isEmpty()) {
                        end = Long.parseLong(ranges[1]);
                    }
                }
            } catch (NumberFormatException badRange) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + size);
                return;
            }

            if (size == 0 || start < 0 || start >= size || end < start) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + size);
                return;
            }
            if (end >= size) end = size - 1;
            long contentLength = end - start + 1;

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + size);
            response.setHeader("Content-Length", String.valueOf(contentLength));

            try (InputStream in = bucketStore.getObject(bucketName, objectKey);
                 OutputStream out = response.getOutputStream()) {

                in.skipNBytes(start);
                byte[] buffer = new byte[262_144];
                long remaining = contentLength;
                int bytesRead;
                while (remaining > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
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
