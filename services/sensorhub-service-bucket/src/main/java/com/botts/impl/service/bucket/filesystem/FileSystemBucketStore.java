package com.botts.impl.service.bucket.filesystem;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.util.InvalidRequestException;
import com.botts.impl.service.bucket.util.UploadPolicy;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
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
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
    );
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
    );
    private final Path rootDirectory;
    private final long maxFileSizeBytes;

    public FileSystemBucketStore(Path rootDirectory) throws IOException {
        this(rootDirectory, UploadPolicy.DEFAULT_MAX_FILE_SIZE_MB);
    }

    public FileSystemBucketStore(Path rootDirectory, long maxFileSizeMb) throws IOException {
        this.rootDirectory = rootDirectory;
        this.maxFileSizeBytes = UploadPolicy.maxFileSizeBytes(maxFileSizeMb);
        if (!Files.exists(rootDirectory)) {
            createDirectories(rootDirectory);
        }
        setDirectoryPermissions(rootDirectory);
    }

    private Path getBucketPath(String bucketName) {
        return rootDirectory.resolve(bucketName);
    }

    private void createDirectories(Path directory) throws IOException {
        Files.createDirectories(directory);
        setDirectoryPermissions(directory);
    }

    private void createObjectParentDirectories(Path bucketPath, Path parent) throws IOException {
        Files.createDirectories(parent);
        setDirectoryPermissions(bucketPath);
        Path current = bucketPath;
        Path relativeParent = bucketPath.relativize(parent);
        for (Path segment : relativeParent) {
            current = current.resolve(segment);
            setDirectoryPermissions(current);
        }
    }

    private void setDirectoryPermissions(Path directory) throws IOException {
        setPermissions(directory, DIRECTORY_PERMISSIONS);
    }

    private void setFilePermissions(Path file) throws IOException {
        setPermissions(file, FILE_PERMISSIONS);
    }

    private void setPermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
        if (Files.getFileStore(path).supportsFileAttributeView("posix"))
            Files.setPosixFilePermissions(path, permissions);
        else if (!Files.isDirectory(path))
            path.toFile().setExecutable(false, false);
    }

    private Path resolveObjectPath(String bucketName, String key) throws DataStoreException {
        try {
            UploadPolicy.validateObjectKey(key);
            Path bucketPath = getBucketPath(bucketName);
            Path realBucketPath = bucketPath.toRealPath();
            Path resolved = realBucketPath.resolve(key.replace('\\', '/')).normalize();
            if (!resolved.startsWith(realBucketPath))
                throw new DataStoreException("Object key must stay within the bucket");

            Path parent = resolved.getParent();
            if (parent != null) {
                Path current = realBucketPath;
                Path relativeParent = realBucketPath.relativize(parent);
                for (Path segment : relativeParent) {
                    current = current.resolve(segment);
                    if (Files.isSymbolicLink(current))
                        throw new DataStoreException("Object path contains a symbolic link");
                }
            }
            if (Files.isSymbolicLink(resolved))
                throw new DataStoreException("Object path is a symbolic link");
            return resolved;
        } catch (IOException e) {
            throw new DataStoreException("Invalid object key: " + key, e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        Path path = getBucketPath(bucketName);
        return Files.exists(path);
    }

    @Override
    public void createBucket(String bucketName) throws DataStoreException {
        try {
            createDirectories(getBucketPath(bucketName));
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
            Path path = resolveObjectPath(bucketName, objectName);
            return Files.exists(path) && path.toFile().isFile() && !Files.isSymbolicLink(path);
        } catch (DataStoreException e) {
            return false;
        }
    }

    @Override
    public boolean objectExists(String relativePath) {
        return Files.exists(getBucketPath(relativePath));
    }

    @Override
    public String createObject(String bucketName, InputStream data, Map<String, String> metadata) throws DataStoreException {
        try {
            UploadPolicy.validateMetadata(metadata);
        } catch (InvalidRequestException e) {
            throw new DataStoreException("Upload is not allowed", e);
        }

        String uuid = UUID.randomUUID().toString();

        var contentType = metadata != null ? metadata.get("Content-Type") : null;
        if (contentType != null)
            uuid += MIME_EXTENSION_MAP.get(contentType);

        putObject(bucketName, uuid, data, metadata);

        return uuid;
    }

    @Override
    public void putObject(String bucketName, String key, InputStream data, Map<String, String> metadata) throws DataStoreException {
        Path resolved = null;
        try {
            UploadPolicy.validateUpload(key, metadata);
            Path path = getBucketPath(bucketName);
            if (!Files.exists(path))
                throw new DataStoreException(BUCKET_NOT_FOUND, new IllegalArgumentException());
            Path bucketPath = path.toRealPath();
            resolved = resolveObjectPath(bucketName, key);
            // Create parent directories if they don't exist (for nested keys like "subdir/file.txt")
            Path parent = resolved.getParent();
            if (parent != null && !Files.exists(parent))
                createObjectParentDirectories(bucketPath, parent);
            Files.copy(new LimitedInputStream(data), resolved, StandardCopyOption.REPLACE_EXISTING);
            setFilePermissions(resolved);
        } catch (IOException e) {
            if (resolved != null) {
                try {
                    Files.deleteIfExists(resolved);
                } catch (IOException ignored) {}
            }
            throw new DataStoreException(FAILED_PUT_OBJECT + bucketName, e);
        }
    }

    @Override
    public OutputStream putObject(String bucketName, String key, Map<String, String> metadata) throws DataStoreException {
        Path path = getBucketPath(bucketName);
        if (!Files.exists(path))
            throw new DataStoreException(BUCKET_NOT_FOUND, new IllegalArgumentException());
        try {
            UploadPolicy.validateUpload(key, metadata);
            Path bucketPath = path.toRealPath();
            var filePath = resolveObjectPath(bucketName, key);
            if (!Files.exists(filePath)) {
                createObjectParentDirectories(bucketPath, filePath.getParent());
                Files.createFile(filePath);
            }
            setFilePermissions(filePath);
            return new LimitedOutputStream(Files.newOutputStream(filePath), filePath);
        } catch (IOException e) {
            throw new DataStoreException(FAILED_PUT_OBJECT + bucketName, e);
        }
    }

    @Override
    public InputStream getObject(String bucketName, String key) throws DataStoreException {
        try {
            Path file = resolveObjectPath(bucketName, key);
            if (!Files.exists(file))
                throw new DataStoreException(OBJECT_NOT_FOUND + bucketName, new IllegalArgumentException());
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new DataStoreException(FAILED_GET_OBJECT + bucketName, e);
        }
    }

    @Override
    public long getObjectSize(String bucketName, String key) throws DataStoreException {
        Path file = resolveObjectPath(bucketName, key);
        if (!Files.exists(file))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName, new IllegalArgumentException());
        if (!file.toFile().isFile())
            throw new DataStoreException("Object is not readable");
        return file.toFile().length();
    }

    @Override
    public String getObjectMimeType(String bucketName, String key) throws DataStoreException {
        Path path = resolveObjectPath(bucketName, key);
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
            throw new DataStoreException("Unable to resolve mime type", e);
        }
    }

    @Override
    public void deleteObject(String bucketName, String key) throws DataStoreException {
        try {
            Path file = resolveObjectPath(bucketName, key);
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

    @Override
    public String getRelativeResourceURI(String bucketName, String key) throws DataStoreException {
        if (!bucketExists(bucketName))
            throw new DataStoreException(BUCKET_NOT_FOUND);
        if (!objectExists(bucketName, key))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName);
        return Paths.get(bucketName, key).toString();
    }


    private class LimitedInputStream extends FilterInputStream {

        private long bytesRead;

        LimitedInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int read = super.read();
            if (read != -1)
                countBytes(1);
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0)
                countBytes(read);
            return read;
        }

        private void countBytes(long count) throws IOException {
            bytesRead += count;
            if (bytesRead > maxFileSizeBytes)
                throw new IOException("File size exceeds maximum allowed: " +
                        (maxFileSizeBytes / (1024 * 1024)) + "MB");
        }
    }

    private class LimitedOutputStream extends FilterOutputStream {

        private final Path filePath;
        private long bytesWritten;

        LimitedOutputStream(OutputStream out, Path filePath) {
            super(out);
            this.filePath = filePath;
        }

        @Override
        public void write(int b) throws IOException {
            countBytes(1);
            super.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            countBytes(len);
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (bytesWritten > maxFileSizeBytes)
                    Files.deleteIfExists(filePath);
                else if (Files.exists(filePath))
                    setFilePermissions(filePath);
            }
        }

        private void countBytes(long count) throws IOException {
            bytesWritten += count;
            if (bytesWritten > maxFileSizeBytes)
                throw new IOException("File size exceeds maximum allowed: " +
                        (maxFileSizeBytes / (1024 * 1024)) + "MB");
        }
    }

}
