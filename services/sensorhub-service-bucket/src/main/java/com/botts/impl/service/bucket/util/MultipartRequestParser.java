package com.botts.impl.service.bucket.util;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipartRequestParser {

    private static final int FILE_SIZE_THRESHOLD = 1024 * 1024; // 1MB
    private static final String MULTIPART = "multipart/";
    private static final String DATA_TEMP_DIR_PROPERTY = "oscar.data.tmpdir";

    /**
     * Check if request has multipart content type.
     * Unlike ServletFileUpload.isMultipartContent(), this supports PUT requests as well.
     */
    private static boolean isMultipartContent(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith(MULTIPART);
    }

    public record MultipartFile(String fieldName, String fileName, String contentType, InputStream inputStream,
                                long size) {
    }

    public record MultipartParseResult(List<MultipartFile> files, Map<String, String> formFields,
                                       List<FileItem> fileItems) implements AutoCloseable {
        @Override
            public void close() {
                for (FileItem item : fileItems) {
                    try {
                        item.delete();
                    } catch (Exception ignored) {}
                }
            }
        }

    // parse multipart request
    public static MultipartParseResult parse(HttpServletRequest request)
            throws InvalidRequestException {
        return parse(request, UploadPolicy.DEFAULT_MAX_FILE_SIZE_BYTES);
    }

    public static MultipartParseResult parse(HttpServletRequest request, long maxFileSizeBytes)
            throws InvalidRequestException {

        // Check content type directly to support both POST and PUT requests
        // (ServletFileUpload.isMultipartContent only allows POST)
        if (!isMultipartContent(request))
            throw ServiceErrors.badRequest("Not a multipart request");

        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(FILE_SIZE_THRESHOLD);
        try {
            factory.setRepository(dataTempDirectory());
        } catch (IOException e) {
            throw ServiceErrors.internalError("Invalid upload temporary directory: " + e.getMessage());
        }

        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setFileSizeMax(maxFileSizeBytes);
        upload.setSizeMax(Math.multiplyExact(maxFileSizeBytes, 2));
        upload.setHeaderEncoding("UTF-8");

        List<FileItem> items;
        try {
            items = upload.parseRequest(request);
        } catch (FileUploadException e) {
            if (e.getMessage() != null && e.getMessage().contains("size")) {
                throw ServiceErrors.badRequest("File size exceeds maximum allowed: " +
                    (maxFileSizeBytes / (1024 * 1024)) + "MB");
            }
            throw ServiceErrors.badRequest("Failed to parse multipart request: " + e.getMessage());
        }

        List<MultipartFile> files = new ArrayList<>();
        Map<String, String> formFields = new HashMap<>();

        for (FileItem item : items) {
            if (item.isFormField()) {
                try {
                    formFields.put(item.getFieldName(), item.getString("UTF-8"));
                } catch (Exception e) {
                    formFields.put(item.getFieldName(), item.getString());
                }
            } else {
                // file
                if (item.getSize() > 0) {
                    try {
                        files.add(new MultipartFile(
                            item.getFieldName(),
                            item.getName(),
                            item.getContentType(),
                            item.getInputStream(),
                            item.getSize()
                        ));
                    } catch (IOException e) {
                        throw ServiceErrors.internalError("Failed to read uploaded file: " + e.getMessage());
                    }
                }
            }
        }

        if (files.isEmpty())
            throw ServiceErrors.badRequest("No files found in multipart request");

        return new MultipartParseResult(files, formFields, items);
    }

    static File dataTempDirectory() throws IOException {
        String configured = System.getProperty(DATA_TEMP_DIR_PROPERTY);
        boolean managed = configured != null;
        if (managed && configured.isBlank())
            throw new IOException(DATA_TEMP_DIR_PROPERTY + " must not be blank");
        String selected = managed ? configured : System.getProperty("java.io.tmpdir");
        if (selected == null || selected.isBlank())
            throw new IOException("temporary directory is not configured");

        Path requested = Path.of(selected);
        if (managed && !requested.isAbsolute())
            throw new IOException(DATA_TEMP_DIR_PROPERTY + " must be absolute");

        Path normalized = requested.toAbsolutePath().normalize();
        Path real = normalized.toRealPath();
        if (managed && !normalized.equals(real))
            throw new IOException(DATA_TEMP_DIR_PROPERTY + " must not contain symbolic links or aliases");
        if (!Files.isDirectory(real) || !Files.isWritable(real))
            throw new IOException("temporary directory must be an existing writable directory");
        if (managed) {
            String nativeTemp = System.getProperty("java.io.tmpdir");
            if (nativeTemp != null && !nativeTemp.isBlank()
                && real.equals(Path.of(nativeTemp).toAbsolutePath().normalize().toRealPath()))
                throw new IOException(DATA_TEMP_DIR_PROPERTY + " must differ from java.io.tmpdir");
        }
        return real.toFile();
    }
}
