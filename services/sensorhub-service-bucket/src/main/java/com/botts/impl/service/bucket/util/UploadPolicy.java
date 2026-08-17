package com.botts.impl.service.bucket.util;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class UploadPolicy {

    public static final long DEFAULT_MAX_FILE_SIZE_MB = 100;
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = maxFileSizeBytes(DEFAULT_MAX_FILE_SIZE_MB);

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".html", ".htm", ".shtml", ".xhtml",
            ".js", ".mjs", ".cjs", ".jsx",
            ".exe", ".dll", ".com", ".bat", ".cmd", ".scr", ".msi",
            ".jar", ".war", ".ear",
            ".sh", ".bash", ".zsh", ".ksh", ".csh", ".fish", ".run", ".bin",
            ".ps1", ".psm1", ".psd1", ".vbs", ".vbe", ".jscript", ".wsf", ".wsh",
            ".app", ".dmg", ".pkg", ".deb", ".rpm",
            ".so", ".dylib", ".elf"
    );

    private static final Set<String> BLOCKED_CONTENT_TYPES = Set.of(
            "text/html",
            "application/xhtml+xml",
            "application/javascript",
            "application/x-javascript",
            "text/javascript",
            "application/ecmascript",
            "text/ecmascript",
            "application/x-msdownload",
            "application/x-msdos-program",
            "application/vnd.microsoft.portable-executable",
            "application/x-msi",
            "application/x-sh",
            "application/x-shellscript",
            "application/x-executable",
            "application/x-mach-binary",
            "application/java-archive"
    );

    private UploadPolicy() {
    }

    public static long maxFileSizeBytes(long maxFileSizeMb) {
        if (maxFileSizeMb <= 0)
            throw new IllegalArgumentException("Max file size must be greater than zero");
        return Math.multiplyExact(maxFileSizeMb, 1024L * 1024L);
    }

    public static void validateUpload(String objectKey, Map<String, String> metadata) throws InvalidRequestException {
        validateObjectKey(objectKey);
        validateFileName(objectKey);
        validateMetadata(metadata);
    }

    public static void validateMetadata(Map<String, String> metadata) throws InvalidRequestException {
        String contentType = getContentType(metadata);
        if (contentType == null)
            return;

        String normalizedContentType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (BLOCKED_CONTENT_TYPES.contains(normalizedContentType))
            throw ServiceErrors.badRequest("File type is not allowed: " + normalizedContentType);
    }

    public static void validateObjectKey(String objectKey) throws InvalidRequestException {
        if (objectKey == null || objectKey.isBlank())
            throw ServiceErrors.badRequest("Object key is required");
        if (objectKey.indexOf('\0') >= 0)
            throw ServiceErrors.badRequest("Object key contains invalid characters");
        if (objectKey.length() >= 2 && Character.isLetter(objectKey.charAt(0)) && objectKey.charAt(1) == ':')
            throw ServiceErrors.badRequest("Object key must be relative to the bucket");

        String normalizedSeparators = objectKey.replace('\\', '/');
        if (normalizedSeparators.startsWith("/"))
            throw ServiceErrors.badRequest("Object key must be relative to the bucket");

        for (String segment : normalizedSeparators.split("/")) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment))
                throw ServiceErrors.badRequest("Object key must stay within the bucket");
        }

        try {
            Path keyPath = Path.of(normalizedSeparators).normalize();
            if (keyPath.isAbsolute() || keyPath.startsWith(".."))
                throw ServiceErrors.badRequest("Object key must stay within the bucket");
        } catch (InvalidPathException e) {
            throw ServiceErrors.badRequest("Object key contains invalid characters");
        }
    }

    private static void validateFileName(String objectKey) throws InvalidRequestException {
        String lowerKey = objectKey.toLowerCase(Locale.ROOT);
        for (String extension : BLOCKED_EXTENSIONS) {
            if (lowerKey.endsWith(extension))
                throw ServiceErrors.badRequest("File extension is not allowed: " + extension);
        }
    }

    private static String getContentType(Map<String, String> metadata) {
        if (metadata == null)
            return null;
        String contentType = metadata.get("Content-Type");
        if (contentType != null)
            return contentType;
        return metadata.get("content-type");
    }
}
