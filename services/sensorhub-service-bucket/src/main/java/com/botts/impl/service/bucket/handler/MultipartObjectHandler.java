package com.botts.impl.service.bucket.handler;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.util.InvalidRequestException;
import com.botts.impl.service.bucket.util.MultipartRequestParser;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.bucket.util.ServiceErrors;
import com.google.gson.stream.JsonWriter;
import org.sensorhub.api.datastore.DataStoreException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipartObjectHandler {

    private final IBucketStore bucketStore;

    public MultipartObjectHandler(IBucketStore bucketStore) {
        this.bucketStore = bucketStore;
    }

    /**
     * Handle POST with multipart/form-data
     * Creates multiple objects (one per file) with auto-generated UUIDs
     *
     * @param ctx Request context
     * @throws IOException on I/O errors
     * @throws SecurityException on permission errors
     */
    public void handleMultipartPost(RequestContext ctx)
            throws IOException, SecurityException {

        var bucketName = ctx.getBucketName();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).create);

        MultipartRequestParser.MultipartParseResult result = null;
        try {
            // Parse multipart request
            result = MultipartRequestParser.parse(ctx.getRequest());

            // Extract metadata from form fields
            Map<String, String> baseMetadata = new HashMap<>(ctx.getHeaders());

            String customContentType = result.formFields().get("contentType");
            String tags = result.formFields().get("tags");
            String description = result.formFields().get("description");

            // Create objects for each file
            List<String> createdObjects = new ArrayList<>();
            for (MultipartRequestParser.MultipartFile file : result.files()) {

                // Build metadata for this file
                Map<String, String> fileMetadata = new HashMap<>(baseMetadata);

                // Use file's content type or override from form field
                if (customContentType != null && !customContentType.isBlank()) {
                    fileMetadata.put("Content-Type", customContentType);
                } else if (file.contentType() != null) {
                    fileMetadata.put("Content-Type", file.contentType());
                }

                if (tags != null) {
                    fileMetadata.put("X-Tags", tags);
                }
                if (description != null) {
                    fileMetadata.put("X-Description", description);
                }
                if (file.fileName() != null) {
                    fileMetadata.put("X-Original-Filename", file.fileName());
                }

                // Create object
                String objectKey = bucketStore.createObject(
                    bucketName,
                    file.inputStream(),
                    fileMetadata
                );

                if (objectKey == null) {
                    throw ServiceErrors.internalError(IBucketStore.FAILED_CREATE_OBJECT + bucketName);
                }

                createdObjects.add(objectKey);
            }

            // Return JSON response with created object URLs
            ctx.getResponse().setStatus(HttpServletResponse.SC_CREATED);

            JsonWriter json = ctx.getJsonWriter();
            json.beginObject();
            json.name("count").value(createdObjects.size());
            json.name("objects");
            json.beginArray();
            for (String key : createdObjects) {
                json.beginObject();
                json.name("key").value(key);
                json.name("url").value(ctx.getResourceURL(key));
                json.endObject();
            }
            json.endArray();
            json.endObject();
            json.flush();

        } catch (InvalidRequestException e) {
            throw e;
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_CREATE_OBJECT + bucketName);
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    /**
     * Handle PUT with multipart/form-data
     * Only accepts single file upload to specified key
     *
     * @param ctx Request context
     * @throws InvalidRequestException if multiple files provided
     * @throws IOException on I/O errors
     * @throws SecurityException on permission errors
     */
    public void handleMultipartPut(RequestContext ctx)
            throws IOException, SecurityException {

        var bucketName = ctx.getBucketName();
        var objectKey = ctx.getObjectKey();
        var sec = ctx.getSecurityHandler();
        sec.checkPermission(sec.getBucketPermission(bucketName).put);

        MultipartRequestParser.MultipartParseResult result = null;
        try {
            result = MultipartRequestParser.parse(ctx.getRequest());

            // PUT only accepts single file
            if (result.files().size() > 1) {
                throw ServiceErrors.badRequest(
                    "PUT only accepts single file. Use POST for multiple files."
                );
            }

            MultipartRequestParser.MultipartFile file = result.files().get(0);

            // Build metadata
            Map<String, String> metadata = new HashMap<>(ctx.getHeaders());

            String customContentType = result.formFields().get("contentType");
            if (customContentType != null && !customContentType.isBlank()) {
                metadata.put("Content-Type", customContentType);
            } else if (file.contentType() != null) {
                metadata.put("Content-Type", file.contentType());
            }

            String tags = result.formFields().get("tags");
            String description = result.formFields().get("description");
            if (tags != null) metadata.put("X-Tags", tags);
            if (description != null) metadata.put("X-Description", description);
            if (file.fileName() != null) {
                metadata.put("X-Original-Filename", file.fileName());
            }

            // Determine status code
            int successStatus = bucketStore.objectExists(bucketName, objectKey) ?
                HttpServletResponse.SC_OK : HttpServletResponse.SC_CREATED;

            // Put object
            bucketStore.putObject(bucketName, objectKey, file.inputStream(), metadata);

            ctx.getResponse().setStatus(successStatus);

        } catch (InvalidRequestException e) {
            throw e;
        } catch (DataStoreException e) {
            throw ServiceErrors.internalError(IBucketStore.FAILED_PUT_OBJECT + bucketName);
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }
}
