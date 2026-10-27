package com.botts.api.service.bucket;

import org.sensorhub.api.datastore.DataStoreException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface IBucketStore {

    String BUCKET_NOT_FOUND = "Bucket not found";
    String FAILED_CREATE_BUCKET = "Failed to create bucket ";
    String FAILED_DELETE_BUCKET = "Failed to delete bucket ";
    String FAILED_GET_BUCKET = "Failed to get bucket ";
    String FAILED_LIST_BUCKETS = "Failed to list buckets";

    String OBJECT_NOT_FOUND = "Object not found in bucket ";
    String FAILED_CREATE_OBJECT = "Failed to create object in bucket ";
    String FAILED_PUT_OBJECT = "Failed to put object in bucket ";
    String FAILED_DELETE_OBJECT = "Failed to delete object in bucket ";
    String FAILED_GET_OBJECT = "Failed to get object from bucket ";
    String FAILED_LIST_OBJECTS = "Failed to list objects in bucket ";

    String UNABLE_DETERMINE_MIME_TYPE = "Unable to determine mime type for object ";


    // Buckets
    boolean bucketExists(String bucketName);

    void createBucket(String bucketName) throws DataStoreException;

    void deleteBucket(String bucketName) throws DataStoreException;

    List<String> listBuckets() throws DataStoreException;

    long getNumBuckets();

    // Objects

    boolean objectExists(String bucketName, String objectName);

    boolean objectExists(String relativePath);

    String createObject(String bucketName, InputStream data, Map<String, String> metadata) throws DataStoreException;

    void putObject(String bucketName, String key, InputStream data, Map<String, String> metadata) throws DataStoreException;

    OutputStream putObject(String bucketName, String key, Map<String, String> metadata) throws DataStoreException;

    /**
     * Opens an object for appending. The object is created if it does not exist and is never truncated.
     * Each write is checked against the store's maximum object size (seeded with the current object size).
     */
    OutputStream appendObject(String bucketName, String key, Map<String, String> metadata) throws DataStoreException;

    InputStream getObject(String bucketName, String key) throws DataStoreException;

    long getObjectSize(String bucketName, String key) throws DataStoreException;

    String getObjectMimeType(String bucketName, String key) throws DataStoreException;

    void deleteObject(String bucketName, String key) throws DataStoreException;

    List<String> listObjects(String bucketName) throws DataStoreException;

    /**
     * Lists objects whose normalized key starts with {@code keyPrefix}.
     *
     * <p>The default keeps third-party stores source-compatible. Stores that can constrain their
     * native listing should override this method so callers do not have to walk an entire bucket.
     * Returned keys are still relative to the bucket, exactly like {@link #listObjects(String)}.</p>
     */
    default List<String> listObjects(String bucketName, String keyPrefix) throws DataStoreException {
        String normalizedPrefix = keyPrefix == null ? "" : keyPrefix.replace('\\', '/');
        return listObjects(bucketName).stream()
                .filter(key -> key.replace('\\', '/').startsWith(normalizedPrefix))
                .toList();
    }

    /**
     * Lists at most {@code limit} objects under a prefix whose normalized keys end in a suffix.
     * Filesystem/object stores should override this so traversal itself stops at the limit; the
     * default preserves compatibility for third-party implementations.
     */
    default List<String> listObjects(String bucketName, String keyPrefix, String keySuffix,
                                     int limit) throws DataStoreException {
        if (limit < 1)
            throw new IllegalArgumentException("Object listing limit must be positive");
        String normalizedSuffix = keySuffix == null ? "" : keySuffix.replace('\\', '/');
        return listObjects(bucketName, keyPrefix).stream()
                .filter(key -> key.replace('\\', '/').endsWith(normalizedSuffix))
                .limit(limit)
                .toList();
    }

    long getNumObjects(String bucketName);

    String getResourceURI(String bucketName, String key) throws DataStoreException;

    /**
     * Returns a validated URI that an internal producer may use to create an object directly.
     * The bucket and parent directories are prepared, but the object itself is deliberately not
     * created. This prevents callers such as streaming muxers from exposing an empty placeholder
     * before they have produced a complete resource.
     */
    default String getWritableResourceURI(String bucketName, String key) throws DataStoreException {
        throw new DataStoreException("Direct writable resource URIs are not supported by this bucket store");
    }

    String getRelativeResourceURI(String bucketName, String key) throws DataStoreException;
}
