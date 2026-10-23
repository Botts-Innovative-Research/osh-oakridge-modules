package com.botts.impl.service.bucket.filesystem;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.util.InvalidRequestException;
import com.botts.impl.service.bucket.util.UploadPolicy;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Set<Path> durableDirectoryEntries = ConcurrentHashMap.newKeySet();

    public FileSystemBucketStore(Path rootDirectory) throws IOException {
        this(rootDirectory, UploadPolicy.DEFAULT_MAX_FILE_SIZE_MB);
    }

    public FileSystemBucketStore(Path rootDirectory, long maxFileSizeMb) throws IOException {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory")
                .toAbsolutePath().normalize();
        this.maxFileSizeBytes = UploadPolicy.maxFileSizeBytes(maxFileSizeMb);
        if (!Files.exists(this.rootDirectory)) {
            createDirectories(this.rootDirectory);
        }
        setDirectoryPermissions(this.rootDirectory);
        forceDirectoryEntry(this.rootDirectory);
        durableDirectoryEntries.add(this.rootDirectory);
    }

    private Path getBucketPath(String bucketName) {
        if (bucketName == null || bucketName.isBlank()
                || bucketName.equals(".") || bucketName.equals("..")
                || bucketName.indexOf('/') >= 0 || bucketName.indexOf('\\') >= 0
                || bucketName.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Bucket name must be one non-empty path component");
        }

        Path component = Path.of(bucketName);
        if (component.isAbsolute() || component.getRoot() != null || component.getNameCount() != 1)
            throw new IllegalArgumentException("Bucket name must be one relative path component");
        Path resolved = rootDirectory.resolve(component).normalize();
        if (!resolved.startsWith(rootDirectory) || !rootDirectory.equals(resolved.getParent()))
            throw new IllegalArgumentException("Bucket name must stay within the bucket root");
        return resolved;
    }

    private Path requireConfinedBucketDirectory(String bucketName) throws IOException {
        Path bucketPath = getBucketPath(bucketName);
        if (Files.isSymbolicLink(bucketPath)
                || !Files.isDirectory(bucketPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Bucket is not a regular directory: " + bucketName);
        }
        Path realRoot = rootDirectory.toRealPath();
        Path realBucket = bucketPath.toRealPath();
        if (!realRoot.equals(realBucket.getParent()))
            throw new IOException("Bucket escapes the configured root: " + bucketName);
        if (!durableDirectoryEntries.contains(realBucket)) {
            forceDirectory(realBucket.getParent());
            durableDirectoryEntries.add(realBucket);
        }
        return realBucket;
    }

    private Path resolveStoreRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()
                || relativePath.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Relative object path is required");
        }
        Path relative = Path.of(relativePath.replace('\\', '/'));
        if (relative.isAbsolute() || relative.getRoot() != null)
            throw new IllegalArgumentException("Object path must be relative to the bucket root");
        for (Path component : relative) {
            if (component.toString().equals(".."))
                throw new IllegalArgumentException("Object path must stay within the bucket root");
        }
        Path resolved = rootDirectory.resolve(relative).normalize();
        if (!resolved.startsWith(rootDirectory))
            throw new IllegalArgumentException("Object path must stay within the bucket root");
        return resolved;
    }

    private void createDirectories(Path directory) throws IOException {
        Path normalized = directory.toAbsolutePath().normalize();
        List<Path> missing = new java.util.ArrayList<>();
        Path existing = normalized;
        while (existing != null && Files.notExists(existing, LinkOption.NOFOLLOW_LINKS)) {
            missing.add(existing);
            existing = existing.getParent();
        }
        if (existing == null || !Files.isDirectory(existing, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(existing))
            throw new IOException("Bucket root has no regular existing ancestor: " + normalized);
        if (existing.getParent() != null)
            forceDirectory(existing.getParent());
        for (int index = missing.size() - 1; index >= 0; index--) {
            Path component = missing.get(index);
            Files.createDirectory(component);
            setDirectoryPermissions(component);
            forceDirectory(component.getParent());
            durableDirectoryEntries.add(component);
        }
    }

    private void createObjectParentDirectories(Path bucketPath, Path parent) throws IOException {
        createObjectParentDirectoriesDurably(bucketPath, parent, durableDirectoryEntries,
                FileSystemBucketStore::forceDirectory);
        setDirectoryPermissions(bucketPath);
        Path current = bucketPath;
        Path relativeParent = bucketPath.relativize(parent);
        for (Path segment : relativeParent) {
            current = current.resolve(segment);
            setDirectoryPermissions(current);
        }
    }

    static void createObjectParentDirectoriesDurably(Path bucketPath, Path parent,
                                                      Set<Path> durableEntries,
                                                      DirectoryForcer directoryForcer)
            throws IOException {
        Objects.requireNonNull(durableEntries, "durableEntries");
        Objects.requireNonNull(directoryForcer, "directoryForcer");
        Path root = bucketPath.toAbsolutePath().normalize();
        Path target = parent.toAbsolutePath().normalize();
        if (!target.startsWith(root))
            throw new IOException("Object parent escapes the bucket root: " + target);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root))
            throw new IOException("Bucket root is linked or not a directory: " + root);

        Path current = root;
        for (Path component : root.relativize(target)) {
            current = current.resolve(component);
            boolean created = false;
            try {
                Files.createDirectory(current);
                created = true;
            } catch (FileAlreadyExistsException ignored) {
                // Validate a concurrent or pre-existing directory below before trusting it.
            }
            if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(current))
                throw new IOException("Object parent is linked or not a directory: " + current);
            if (created || !durableEntries.contains(current)) {
                directoryForcer.force(current.getParent());
                durableEntries.add(current);
            }
        }
    }

    private void forceDirectoryEntry(Path directory) throws IOException {
        if (directory.getParent() != null)
            forceDirectory(directory.getParent());
    }

    private static void forceDirectory(Path directory) throws IOException {
        if (directory == null)
            return;
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (UnsupportedOperationException e) {
            // Directory fsync is unavailable on some supported filesystem/JDK combinations.
        } catch (IOException e) {
            if (!(isWindows() && e instanceof AccessDeniedException))
                throw e;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT)
                .startsWith("windows");
    }

    @FunctionalInterface
    interface DirectoryForcer {
        void force(Path directory) throws IOException;
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
            Path realBucketPath = requireConfinedBucketDirectory(bucketName);
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
        } catch (IOException | IllegalArgumentException e) {
            throw new DataStoreException("Invalid object key: " + key, e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            requireConfinedBucketDirectory(bucketName);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void createBucket(String bucketName) throws DataStoreException {
        try {
            Path path = getBucketPath(bucketName);
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                requireConfinedBucketDirectory(bucketName);
            } else {
                Files.createDirectory(path);
                requireConfinedBucketDirectory(bucketName);
                setDirectoryPermissions(path);
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new DataStoreException(FAILED_CREATE_BUCKET + bucketName, e);
        }
    }

    @Override
    public void deleteBucket(String bucketName) throws DataStoreException {
        try {
            Path path = getBucketPath(bucketName);
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                requireConfinedBucketDirectory(bucketName);
                try (Stream<Path> paths = Files.walk(path)) {
                    for (Path candidate : paths.sorted((left, right) -> right.compareTo(left)).toList())
                        Files.delete(candidate);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
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
        if (isPartialObjectKey(objectName))
            return false;
        try {
            Path path = resolveObjectPath(bucketName, objectName);
            return Files.exists(path) && path.toFile().isFile() && !Files.isSymbolicLink(path);
        } catch (DataStoreException e) {
            return false;
        }
    }

    @Override
    public boolean objectExists(String relativePath) {
        if (isPartialObjectKey(relativePath))
            return false;
        try {
            Path path = resolveStoreRelativePath(relativePath);
            return Files.exists(path) && path.toRealPath().startsWith(rootDirectory.toRealPath());
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
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
            if (parent != null)
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
            createObjectParentDirectories(bucketPath, filePath.getParent());
            // Files.newOutputStream atomically creates a missing object and truncates an existing
            // one. Avoid a check-then-create sequence here because concurrent writers could both
            // observe a missing object and one would fail with FileAlreadyExistsException.
            var outputStream = Files.newOutputStream(filePath);
            try {
                setFilePermissions(filePath);
                return new LimitedOutputStream(outputStream, filePath);
            } catch (IOException e) {
                outputStream.close();
                throw e;
            }
        } catch (IOException e) {
            throw new DataStoreException(FAILED_PUT_OBJECT + bucketName, e);
        }
    }

    @Override
    public InputStream getObject(String bucketName, String key) throws DataStoreException {
        if (isPartialObjectKey(key))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName);
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
        if (isPartialObjectKey(key))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName);
        Path file = resolveObjectPath(bucketName, key);
        if (!Files.exists(file))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName, new IllegalArgumentException());
        if (!file.toFile().isFile())
            throw new DataStoreException("Object is not readable");
        return file.toFile().length();
    }

    @Override
    public String getObjectMimeType(String bucketName, String key) throws DataStoreException {
        if (isPartialObjectKey(key))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName);
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
                        .filter(key -> !isPartialObjectKey(key))
                        .toList();
            }
        } catch (IOException e) {
            throw new DataStoreException(FAILED_LIST_OBJECTS + bucketName, e);
        }
    }

    @Override
    public List<String> listObjects(String bucketName, String keyPrefix) throws DataStoreException {
        try {
            Path bucketPath = getBucketPath(bucketName);
            if (!Files.exists(bucketPath))
                throw new DataStoreException(BUCKET_NOT_FOUND, new IllegalArgumentException());

            String normalizedPrefix = keyPrefix == null ? "" : keyPrefix.replace('\\', '/');
            while (normalizedPrefix.endsWith("/"))
                normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
            if (normalizedPrefix.isEmpty())
                return listObjects(bucketName);

            Path prefixPath = resolveObjectPath(bucketName, normalizedPrefix);
            if (!Files.exists(prefixPath))
                return List.of();
            if (!Files.isDirectory(prefixPath) || Files.isSymbolicLink(prefixPath))
                return objectExists(bucketName, normalizedPrefix) ? List.of(normalizedPrefix) : List.of();

            Path realBucketPath = bucketPath.toRealPath();
            try (Stream<Path> stream = Files.walk(prefixPath)) {
                return stream.filter(Files::isRegularFile)
                        .filter(candidate -> !Files.isSymbolicLink(candidate))
                        .map(candidate -> realBucketPath.relativize(candidate).toString())
                        .filter(key -> !isPartialObjectKey(key))
                        .toList();
            }
        } catch (IOException e) {
            throw new DataStoreException(FAILED_LIST_OBJECTS + bucketName, e);
        }
    }

    @Override
    public List<String> listObjects(String bucketName, String keyPrefix, String keySuffix, int limit)
            throws DataStoreException {
        if (limit < 1)
            throw new IllegalArgumentException("Object listing limit must be positive");
        try {
            Path bucketPath = getBucketPath(bucketName);
            if (!Files.exists(bucketPath))
                throw new DataStoreException(BUCKET_NOT_FOUND, new IllegalArgumentException());

            String normalizedPrefix = keyPrefix == null ? "" : keyPrefix.replace('\\', '/');
            while (normalizedPrefix.endsWith("/"))
                normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
            String normalizedSuffix = keySuffix == null ? "" : keySuffix.replace('\\', '/');
            Path prefixPath = normalizedPrefix.isEmpty()
                    ? bucketPath.toRealPath() : resolveObjectPath(bucketName, normalizedPrefix);
            if (!Files.exists(prefixPath))
                return List.of();
            Path realBucketPath = bucketPath.toRealPath();
            if (!Files.isDirectory(prefixPath) || Files.isSymbolicLink(prefixPath)) {
                if (!objectExists(bucketName, normalizedPrefix)
                        || !normalizedPrefix.endsWith(normalizedSuffix))
                    return List.of();
                return List.of(normalizedPrefix);
            }

            // Filtering precedes limit, and the stream short-circuits Files.walk traversal as soon
            // as enough matching objects are found. No whole-prefix list is materialized.
            try (Stream<Path> stream = Files.walk(prefixPath)) {
                return stream.filter(Files::isRegularFile)
                        .filter(candidate -> !Files.isSymbolicLink(candidate))
                        .map(candidate -> realBucketPath.relativize(candidate).toString())
                        .map(key -> key.replace('\\', '/'))
                        .filter(key -> !isPartialObjectKey(key))
                        .filter(key -> key.endsWith(normalizedSuffix))
                        .limit(limit)
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
            return stream.filter(Files::isRegularFile)
                    .map(candidate -> path.relativize(candidate).toString())
                    .filter(key -> !isPartialObjectKey(key))
                    .count();
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
    public String getWritableResourceURI(String bucketName, String key) throws DataStoreException {
        if (!bucketExists(bucketName))
            throw new DataStoreException(BUCKET_NOT_FOUND);

        try {
            UploadPolicy.validateUpload(key, Map.of());
            Path bucketPath = getBucketPath(bucketName).toRealPath();
            Path objectPath = resolveObjectPath(bucketName, key);
            Path parent = objectPath.getParent();
            if (parent != null)
                createObjectParentDirectories(bucketPath, parent);
            return objectPath.toString();
        } catch (IOException e) {
            throw new DataStoreException(FAILED_PUT_OBJECT + bucketName, e);
        }
    }

    @Override
    public String getRelativeResourceURI(String bucketName, String key) throws DataStoreException {
        if (!bucketExists(bucketName))
            throw new DataStoreException(BUCKET_NOT_FOUND);
        if (!objectExists(bucketName, key))
            throw new DataStoreException(OBJECT_NOT_FOUND + bucketName);
        return Paths.get(bucketName, key).toString();
    }

    private static boolean isPartialObjectKey(String key) {
        if (key == null)
            return false;
        String normalized = key.replace('\\', '/');
        return normalized.contains(".partial-") || normalized.endsWith(".partial");
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
