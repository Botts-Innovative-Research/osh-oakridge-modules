package com.botts.impl.service.bucket.filesystem;

import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class FileSystemBucketStore implements IBucketStore {

    private static final Map<String, String> MIME_EXTENSION_MAP = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/gif", ".gif"),
            Map.entry("video/mp4", ".mp4"),
            Map.entry("application/pdf", ".pdf"),
            Map.entry("text/plain", ".txt"),
            Map.entry("application/json", ".json")
    );
    private final Path rootDirectory;

    public FileSystemBucketStore(Path rootDirectory) throws IOException {
        this.rootDirectory = rootDirectory;
        if (!Files.exists(rootDirectory)) {
            Files.createDirectories(rootDirectory);
        }
    }

    private Path getBucketPath(String bucketName) {
        return rootDirectory.resolve(bucketName);
    }

    @Override
    public boolean bucketExists(String bucketName) {
        Path path = getBucketPath(bucketName);
        return Files.exists(path);
    }

    @Override
    public void createBucket(String bucketName) throws DataStoreException {
        try {
            Files.createDirectories(getBucketPath(bucketName));
        } catch (IOException e) {
            throw new DataStoreException(FAILED_CREATE_BUCKET + bucketName, e);
        }
    }

    @Override
    public void deleteBucket(String bucketName) throws DataStoreException {
        try {
            Path path = getBucketPath(bucketName);
            if (Files.exists(path))
                Files.walk(path).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            throw new DataStoreException(FAILED_DELETE_BUCKET + bucketName, e);
        }
    }

    @Override
    public List<String> listBuckets() throws DataStoreException {
        try {
            return Files.list(rootDirectory)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .toList();
        } catch (IOException e) {
            throw new DataStoreException(FAILED_LIST_BUCKETS, e);
        }
    }

    @Override
    public long getNumBuckets() {
        try {
            return Files.list(rootDirectory)
                    .filter(Files::isDirectory)
                    .count();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public boolean objectExists(String bucketName, String objectName) {
        Path path = getBucketPath(bucketName).resolve(objectName);
        return Files.exists(path) && path.toFile().isFile();
    }

    @Override
    public String createObject(String bucketName, InputStream data, Map<String, String> metadata) throws DataStoreException {
        String uuid = UUID.randomUUID().toString();

        var contentType = metadata.get("Content-Type");
        if (contentType != null)
            uuid += MIME_EXTENSION_MAP.get(contentType);

        putObject(bucketName, uuid, data, metadata);

        return uuid;
    }

    @Override
    public void putObject(String bucketName, String key, InputStream data, Map<String, String> metadata) throws DataStoreException {
        try {
            Path path = getBucketPath(bucketName);
            if (!Files.exists(path))
                throw new DataStoreException(BUCKET_NOT_FOUND, new IllegalArgumentException());
            Path resolved = path.resolve(key);
            if (!Files.exists(resolved))
                Files.createDirectories(resolved);
            Files.copy(data, resolved, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DataStoreException(FAILED_PUT_OBJECT + bucketName, e);
        }
    }

    @Override
    public OutputStream putObject(String bucketName, String key, Map<String, String> metadata) throws DataStoreException {
        Path path = getBucketPath(bucketName);
        if (!Files.exists(path))
            throw new DataStoreException(BUCKET_NOT_FOUND, new IllegalArgumentException());
        try {
            var filePath = path.resolve(key).toFile().toPath();
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                Files.createFile(path.resolve(key));
            }
            return new FileOutputStream(path.resolve(key).toFile());
        } catch (IOException e) {
            throw new DataStoreException(FAILED_PUT_OBJECT + bucketName, e);
        }
    }

    @Override
    public InputStream getObject(String bucketName, String key) throws DataStoreException {
        try {
            Path file = getBucketPath(bucketName).resolve(key);
            if (!Files.exists(file))
                throw new DataStoreException(OBJECT_NOT_FOUND + bucketName, new IllegalArgumentException());
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new DataStoreException(FAILED_GET_OBJECT + bucketName, e);
        }
    }

    @Override
    public String getObjectMimeType(String bucketName, String key) throws DataStoreException {
        Path path = getBucketPath(bucketName).resolve(key);
        if (!Files.exists(path))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName, new IllegalArgumentException());
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            throw new DataStoreException("Unable to resolve mime type", e);
        }
    }

    @Override
    public void deleteObject(String bucketName, String key) throws DataStoreException {
        try {
            Path file = getBucketPath(bucketName).resolve(key);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new DataStoreException(FAILED_DELETE_OBJECT + bucketName, e);
        }
    }

    @Override
    public List<String> listObjects(String bucketName) throws DataStoreException {
        try {
            Path path = getBucketPath(bucketName);
            if (!Files.exists(path))
                throw new DataStoreException(BUCKET_NOT_FOUND, new IllegalArgumentException());
            try (Stream<Path> stream = Files.walk(path)) {
                return stream.filter(Files::isRegularFile)
                        .map(p -> path.relativize(p).toString())
                        .toList();
            }
        } catch (IOException e) {
            throw new DataStoreException(FAILED_LIST_OBJECTS + bucketName, e);
        }
    }

    @Override
    public long getNumObjects(String bucketName) {
        Path path = getBucketPath(bucketName);
        if (!Files.exists(path))
            return -1;
        try (Stream<Path> stream = Files.walk(path)) {
            return stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public String getResourceURI(String bucketName, String key) throws DataStoreException {
        if (!bucketExists(bucketName))
            throw new DataStoreException(BUCKET_NOT_FOUND);
        if (!objectExists(bucketName, key))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName);
        return rootDirectory.resolve(bucketName).resolve(key).toString();
    }

}
