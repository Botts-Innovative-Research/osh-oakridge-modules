package com.botts.impl.service.bucket.filesystem;

import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            Map.entry("text/csv", ".csv"),
            Map.entry("application/json", ".json"),
            Map.entry("application/test", ".test")
    );
    private final Path rootDirectory;

    public FileSystemBucketStore(Path rootDirectory) throws IOException {
        this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
        if (!Files.exists(this.rootDirectory)) {
            Files.createDirectories(this.rootDirectory);
        }
    }

    private Path getBucketPath(String bucketName) throws DataStoreException {
        Path path = rootDirectory.resolve(bucketName).toAbsolutePath().normalize();
        if (!path.startsWith(rootDirectory)) {
            throw new DataStoreException("Illegal bucket name: " + bucketName);
        }
        return path;
    }

    private Path getObjectPath(String bucketName, String key) throws DataStoreException {
        Path bucketPath = getBucketPath(bucketName);
        Path objectPath = bucketPath.resolve(key).toAbsolutePath().normalize();
        if (!objectPath.startsWith(bucketPath)) {
            throw new DataStoreException("Illegal object key: " + key);
        }
        return objectPath;
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            Path path = getBucketPath(bucketName);
            return Files.exists(path);
        } catch (DataStoreException e) {
            return false;
        }
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
        try {
            Path path = getObjectPath(bucketName, objectName);
            return Files.exists(path) && path.toFile().isFile();
        } catch (DataStoreException e) {
            return false;
        }
    }

    @Override
    public boolean objectExists(String relativePath) {
        try {
            Path path = rootDirectory.resolve(relativePath).toAbsolutePath().normalize();
            if (!path.startsWith(rootDirectory))
                return false;
            return Files.exists(path);
        } catch (Exception e) {
            return false;
        }
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
            Path bucketPath = getBucketPath(bucketName);
            if (!Files.exists(bucketPath))
                throw new DataStoreException(BUCKET_NOT_FOUND, new IllegalArgumentException());

            Path resolved = getObjectPath(bucketName, key);
            Path parent = resolved.getParent();
            if (parent != null && !Files.exists(parent))
                Files.createDirectories(parent);

            Files.copy(data, resolved, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DataStoreException(FAILED_PUT_OBJECT + bucketName, e);
        }
    }

    @Override
    public OutputStream putObject(String bucketName, String key, Map<String, String> metadata) throws DataStoreException {
        try {
            Path bucketPath = getBucketPath(bucketName);
            if (!Files.exists(bucketPath))
                throw new DataStoreException(BUCKET_NOT_FOUND, new IllegalArgumentException());

            Path resolved = getObjectPath(bucketName, key);
            Path parent = resolved.getParent();
            if (parent != null && !Files.exists(parent))
                Files.createDirectories(parent);

            if (!Files.exists(resolved))
                Files.createFile(resolved);

            return new FileOutputStream(resolved.toFile());
        } catch (IOException e) {
            throw new DataStoreException(FAILED_PUT_OBJECT + bucketName, e);
        }
    }

    @Override
    public InputStream getObject(String bucketName, String key) throws DataStoreException {
        try {
            Path file = getObjectPath(bucketName, key);
            if (!Files.exists(file))
                throw new DataStoreException(OBJECT_NOT_FOUND + bucketName, new IllegalArgumentException());
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new DataStoreException(FAILED_GET_OBJECT + bucketName, e);
        }
    }

    @Override
    public long getObjectSize(String bucketName, String key) throws DataStoreException {
        Path file = getObjectPath(bucketName, key);
        if (!Files.exists(file))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName, new IllegalArgumentException());
        if (!file.toFile().isFile())
            throw new DataStoreException(org.sensorhub.ui.I18N.get("error.bucket.not_readable"));
        return file.toFile().length();
    }

    @Override
    public String getObjectMimeType(String bucketName, String key) throws DataStoreException {
        Path path = getObjectPath(bucketName, key);
        if (!Files.exists(path))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName, new IllegalArgumentException());

        try {
            String mimeType = Files.probeContentType(path);
            if (mimeType != null)
                return mimeType;

            String lowerKey = key.toLowerCase();
            for (Map.Entry<String, String> entry : MIME_EXTENSION_MAP.entrySet()) {
                String mime = entry.getKey();
                String extension = entry.getValue();
                if (lowerKey.endsWith(extension))
                    return mime;
            }

            return "application/octet-stream";
        } catch (IOException e) {
            throw new DataStoreException(org.sensorhub.ui.I18N.get("error.bucket.resolve_mime"), e);
        }
    }

    @Override
    public void deleteObject(String bucketName, String key) throws DataStoreException {
        try {
            Path file = getObjectPath(bucketName, key);
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
        try {
            Path path = getBucketPath(bucketName);
            if (!Files.exists(path))
                return -1;
            try (Stream<Path> stream = Files.walk(path)) {
                return stream.filter(Files::isRegularFile).count();
            }
        } catch (IOException | DataStoreException e) {
            return -1;
        }
    }

    @Override
    public String getResourceURI(String bucketName, String key) throws DataStoreException {
        if (!bucketExists(bucketName))
            throw new DataStoreException(BUCKET_NOT_FOUND);
        if (!objectExists(bucketName, key))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName);
        return getObjectPath(bucketName, key).toString();
    }

    @Override
    public String getRelativeResourceURI(String bucketName, String key) throws DataStoreException {
        if (!bucketExists(bucketName))
            throw new DataStoreException(BUCKET_NOT_FOUND);
        if (!objectExists(bucketName, key))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName);
        return Paths.get(bucketName, key).toString();
    }


}
