package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import com.botts.impl.service.bucket.util.MultipartRequestParser;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.datastore.DataStoreException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for multipart/form-data file upload functionality.
 * Verifies that files uploaded via multipart requests are stored
 * and retrieved with identical content (no extra data added).
 */
public class MultipartUploadTest {

    private static final String TEST_BUCKET = "multipart-test-bucket";
    private static final Path TEST_ROOT = Path.of("src/test/resources/test-multipart-root");

    private IBucketStore bucketStore;

    @Before
    public void setup() throws IOException, DataStoreException {
        // Clean up any existing test directory
        if (Files.exists(TEST_ROOT)) {
            Files.walk(TEST_ROOT)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
        bucketStore = new FileSystemBucketStore(TEST_ROOT);
        bucketStore.createBucket(TEST_BUCKET);
    }

    @After
    public void cleanup() throws DataStoreException {
        if (bucketStore != null) {
            List<String> buckets = bucketStore.listBuckets();
            for (var bucket : buckets) {
                bucketStore.deleteBucket(bucket);
            }
        }
        // Clean up test directory
        try {
            if (Files.exists(TEST_ROOT)) {
                Files.walk(TEST_ROOT)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException ignored) {}
    }

    /**
     * Test that file content is stored and retrieved exactly as uploaded.
     * This tests the core storage functionality without multipart parsing.
     */
    @Test
    public void testContentIntegrity() throws DataStoreException, IOException {
        String originalContent = "Name,Age,City\nJohn,30,NYC\nJane,25,LA\n";
        byte[] originalBytes = originalContent.getBytes(StandardCharsets.UTF_8);

        // Store content
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", "text/csv");

        String objectKey = bucketStore.createObject(TEST_BUCKET, inputStream, metadata);
        assertNotNull("Object key should not be null", objectKey);

        // Retrieve content
        InputStream retrievedStream = bucketStore.getObject(TEST_BUCKET, objectKey);
        byte[] retrievedBytes = retrievedStream.readAllBytes();
        retrievedStream.close();

        // Verify content matches exactly
        assertArrayEquals("Retrieved content should match original exactly",
            originalBytes, retrievedBytes);
        assertEquals("Retrieved string should match original",
            originalContent, new String(retrievedBytes, StandardCharsets.UTF_8));
    }

    /**
     * Test that binary content is stored and retrieved exactly as uploaded.
     */
    @Test
    public void testBinaryContentIntegrity() throws DataStoreException, IOException {
        // Create binary content with various byte values including edge cases
        byte[] originalBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            originalBytes[i] = (byte) i;
        }

        // Store content
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/octet-stream");

        String objectKey = bucketStore.createObject(TEST_BUCKET, inputStream, metadata);

        // Retrieve content
        InputStream retrievedStream = bucketStore.getObject(TEST_BUCKET, objectKey);
        byte[] retrievedBytes = retrievedStream.readAllBytes();
        retrievedStream.close();

        // Verify content matches exactly
        assertArrayEquals("Retrieved binary content should match original exactly",
            originalBytes, retrievedBytes);
    }

    /**
     * Test that CSV content with special characters is preserved.
     */
    @Test
    public void testCsvContentWithSpecialCharacters() throws DataStoreException, IOException {
        String csvContent = "Name,Description,Value\n" +
            "Item1,\"Contains, comma\",100\n" +
            "Item2,\"Has \"\"quotes\"\"\",200\n" +
            "Item3,\"Multi\nline\",300\n";
        byte[] originalBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", "text/csv");

        String objectKey = bucketStore.createObject(TEST_BUCKET, inputStream, metadata);

        InputStream retrievedStream = bucketStore.getObject(TEST_BUCKET, objectKey);
        byte[] retrievedBytes = retrievedStream.readAllBytes();
        retrievedStream.close();

        assertArrayEquals("CSV with special characters should be preserved exactly",
            originalBytes, retrievedBytes);
    }

    /**
     * Test PUT with content replacement.
     */
    @Test
    public void testPutReplacesContent() throws DataStoreException, IOException {
        String objectKey = "test-file.txt";

        // Initial upload
        String initialContent = "Initial content";
        bucketStore.putObject(TEST_BUCKET, objectKey,
            new ByteArrayInputStream(initialContent.getBytes(StandardCharsets.UTF_8)),
            Collections.emptyMap());

        // Verify initial content
        InputStream stream1 = bucketStore.getObject(TEST_BUCKET, objectKey);
        assertEquals(initialContent, new String(stream1.readAllBytes(), StandardCharsets.UTF_8));
        stream1.close();

        // Replace with new content
        String newContent = "Completely different content";
        bucketStore.putObject(TEST_BUCKET, objectKey,
            new ByteArrayInputStream(newContent.getBytes(StandardCharsets.UTF_8)),
            Collections.emptyMap());

        // Verify new content (should not contain any of the old content)
        InputStream stream2 = bucketStore.getObject(TEST_BUCKET, objectKey);
        String retrieved = new String(stream2.readAllBytes(), StandardCharsets.UTF_8);
        stream2.close();

        assertEquals("Content should be completely replaced", newContent, retrieved);
        assertFalse("Old content should not be present", retrieved.contains("Initial"));
    }

    /**
     * Test that nested directory paths work correctly.
     */
    @Test
    public void testNestedDirectoryPaths() throws DataStoreException, IOException {
        String objectKey = "level1/level2/level3/deep-file.csv";
        String content = "a,b,c\n1,2,3\n";

        bucketStore.putObject(TEST_BUCKET, objectKey,
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
            Collections.emptyMap());

        assertTrue("Object should exist", bucketStore.objectExists(TEST_BUCKET, objectKey));

        InputStream stream = bucketStore.getObject(TEST_BUCKET, objectKey);
        String retrieved = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        stream.close();

        assertEquals("Content in nested directory should be preserved", content, retrieved);
    }

    /**
     * Test multipart request detection for POST request.
     */
    @Test
    public void testMultipartDetectionPost() {
        // Create a mock POST request with multipart content type
        HttpServletRequest mockRequest = createMockRequest("POST",
            "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW",
            new byte[0]);

        // Standard ServletFileUpload only accepts POST
        assertTrue("POST with multipart content should be detected as multipart by ServletFileUpload",
            ServletFileUpload.isMultipartContent(mockRequest));
    }

    /**
     * Test that PUT requests with multipart content are supported by our parser.
     * (Apache Commons FileUpload's isMultipartContent() returns false for PUT)
     */
    @Test
    public void testMultipartDetectionPut() {
        // Create a mock PUT request with multipart content type
        HttpServletRequest mockRequest = createMockRequest("PUT",
            "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW",
            new byte[0]);

        // Standard ServletFileUpload does NOT consider PUT as multipart
        assertFalse("PUT with multipart content is NOT detected by standard ServletFileUpload",
            ServletFileUpload.isMultipartContent(mockRequest));

        // But our content type check should work
        String contentType = mockRequest.getContentType();
        assertTrue("Content type should still indicate multipart",
            contentType != null && contentType.toLowerCase().startsWith("multipart/"));
    }

    /**
     * Test parsing a real multipart request body (POST).
     */
    @Test
    public void testParseMultipartPost() throws Exception {
        String boundary = "----WebKitFormBoundaryTest123";
        String fileContent = "col1,col2,col3\nval1,val2,val3\n";
        String filename = "test.csv";

        byte[] multipartBody = createMultipartBody(boundary, filename, "text/csv", fileContent);

        HttpServletRequest mockRequest = createMockRequest("POST",
            "multipart/form-data; boundary=" + boundary,
            multipartBody);

        // Parse the multipart request
        MultipartRequestParser.MultipartParseResult result = MultipartRequestParser.parse(mockRequest);

        assertNotNull("Parse result should not be null", result);
        assertEquals("Should have exactly one file", 1, result.files().size());

        MultipartRequestParser.MultipartFile file = result.files().get(0);
        assertEquals("Filename should match", filename, file.fileName());
        assertEquals("Content type should match", "text/csv", file.contentType());

        // Read the file content from the parsed result
        byte[] parsedContent = file.inputStream().readAllBytes();
        assertEquals("Parsed file content should match original",
            fileContent, new String(parsedContent, StandardCharsets.UTF_8));

        result.close();
    }

    /**
     * Test parsing a multipart PUT request.
     */
    @Test
    public void testParseMultipartPut() throws Exception {
        String boundary = "----WebKitFormBoundaryTest456";
        String fileContent = "id,name,value\n1,test,100\n";
        String filename = "data.csv";

        byte[] multipartBody = createMultipartBody(boundary, filename, "text/csv", fileContent);

        HttpServletRequest mockRequest = createMockRequest("PUT",
            "multipart/form-data; boundary=" + boundary,
            multipartBody);

        // Our parser should work for PUT requests too
        MultipartRequestParser.MultipartParseResult result = MultipartRequestParser.parse(mockRequest);

        assertNotNull("Parse result should not be null for PUT", result);
        assertEquals("Should have exactly one file", 1, result.files().size());

        MultipartRequestParser.MultipartFile file = result.files().get(0);
        byte[] parsedContent = file.inputStream().readAllBytes();
        assertEquals("Parsed file content should match original for PUT",
            fileContent, new String(parsedContent, StandardCharsets.UTF_8));

        result.close();
    }

    /**
     * Test full round-trip: parse multipart, store, and retrieve.
     */
    @Test
    public void testMultipartRoundTrip() throws Exception {
        String boundary = "----WebKitFormBoundaryRoundTrip";
        String originalContent = "header1,header2,header3\ndata1,data2,data3\nmore1,more2,more3\n";
        String filename = "roundtrip.csv";

        byte[] multipartBody = createMultipartBody(boundary, filename, "text/csv", originalContent);

        HttpServletRequest mockRequest = createMockRequest("POST",
            "multipart/form-data; boundary=" + boundary,
            multipartBody);

        // Parse multipart
        MultipartRequestParser.MultipartParseResult result = MultipartRequestParser.parse(mockRequest);
        MultipartRequestParser.MultipartFile file = result.files().get(0);

        // Store using bucket store
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", file.contentType());
        String objectKey = bucketStore.createObject(TEST_BUCKET, file.inputStream(), metadata);
        result.close();

        // Retrieve and verify
        InputStream retrievedStream = bucketStore.getObject(TEST_BUCKET, objectKey);
        String retrievedContent = new String(retrievedStream.readAllBytes(), StandardCharsets.UTF_8);
        retrievedStream.close();

        assertEquals("Round-trip content should match exactly", originalContent, retrievedContent);
    }

    // Helper methods

    private byte[] createMultipartBody(String boundary, String filename, String contentType, String content)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), true);

        // File part
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
              .append(filename).append("\"\r\n");
        writer.append("Content-Type: ").append(contentType).append("\r\n");
        writer.append("\r\n");
        writer.flush();

        baos.write(content.getBytes(StandardCharsets.UTF_8));

        writer.append("\r\n");
        writer.append("--").append(boundary).append("--").append("\r\n");
        writer.flush();

        return baos.toByteArray();
    }

    private HttpServletRequest createMockRequest(String method, String contentType, byte[] body) {
        return new MockHttpServletRequest(method, contentType, body);
    }

    /**
     * Simple mock HttpServletRequest for testing multipart parsing.
     */
    private static class MockHttpServletRequest implements HttpServletRequest {
        private final String method;
        private final String contentType;
        private final byte[] body;
        private final Map<String, String> headers = new HashMap<>();

        MockHttpServletRequest(String method, String contentType, byte[] body) {
            this.method = method;
            this.contentType = contentType;
            this.body = body;
            headers.put("Content-Type", contentType);
            headers.put("Content-Length", String.valueOf(body.length));
        }

        @Override public String getMethod() { return method; }
        @Override public String getContentType() { return contentType; }
        @Override public int getContentLength() { return body.length; }
        @Override public long getContentLengthLong() { return body.length; }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return bais.read(); }
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {}
            };
        }

        @Override public String getHeader(String name) {
            return headers.get(name);
        }

        @Override public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(headers.keySet());
        }

        // Required interface methods - return null/defaults for unused methods
        @Override public String getAuthType() { return null; }
        @Override public javax.servlet.http.Cookie[] getCookies() { return null; }
        @Override public long getDateHeader(String name) { return -1; }
        @Override public Enumeration<String> getHeaders(String name) {
            String val = headers.get(name);
            return val != null ? Collections.enumeration(List.of(val)) : Collections.emptyEnumeration();
        }
        @Override public int getIntHeader(String name) { return -1; }
        @Override public String getPathInfo() { return null; }
        @Override public String getPathTranslated() { return null; }
        @Override public String getContextPath() { return ""; }
        @Override public String getQueryString() { return null; }
        @Override public String getRemoteUser() { return null; }
        @Override public boolean isUserInRole(String role) { return false; }
        @Override public java.security.Principal getUserPrincipal() { return null; }
        @Override public String getRequestedSessionId() { return null; }
        @Override public String getRequestURI() { return "/test"; }
        @Override public StringBuffer getRequestURL() { return new StringBuffer("http://localhost/test"); }
        @Override public String getServletPath() { return ""; }
        @Override public javax.servlet.http.HttpSession getSession(boolean create) { return null; }
        @Override public javax.servlet.http.HttpSession getSession() { return null; }
        @Override public String changeSessionId() { return null; }
        @Override public boolean isRequestedSessionIdValid() { return false; }
        @Override public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override public boolean isRequestedSessionIdFromURL() { return false; }
        @Override public boolean isRequestedSessionIdFromUrl() { return false; }
        @Override public boolean authenticate(javax.servlet.http.HttpServletResponse resp) { return false; }
        @Override public void login(String u, String p) {}
        @Override public void logout() {}
        @Override public java.util.Collection<javax.servlet.http.Part> getParts() { return null; }
        @Override public javax.servlet.http.Part getPart(String name) { return null; }
        @Override public <T extends javax.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> cls) { return null; }
        @Override public Object getAttribute(String name) { return null; }
        @Override public Enumeration<String> getAttributeNames() { return Collections.emptyEnumeration(); }
        @Override public String getCharacterEncoding() { return "UTF-8"; }
        @Override public void setCharacterEncoding(String env) {}
        @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body))); }
        @Override public String getParameter(String name) { return null; }
        @Override public Enumeration<String> getParameterNames() { return Collections.emptyEnumeration(); }
        @Override public String[] getParameterValues(String name) { return null; }
        @Override public Map<String, String[]> getParameterMap() { return Collections.emptyMap(); }
        @Override public String getProtocol() { return "HTTP/1.1"; }
        @Override public String getScheme() { return "http"; }
        @Override public String getServerName() { return "localhost"; }
        @Override public int getServerPort() { return 8080; }
        @Override public String getRemoteAddr() { return "127.0.0.1"; }
        @Override public String getRemoteHost() { return "localhost"; }
        @Override public void setAttribute(String name, Object o) {}
        @Override public void removeAttribute(String name) {}
        @Override public Locale getLocale() { return Locale.getDefault(); }
        @Override public Enumeration<Locale> getLocales() { return Collections.enumeration(List.of(Locale.getDefault())); }
        @Override public boolean isSecure() { return false; }
        @Override public javax.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        @Override public String getRealPath(String path) { return null; }
        @Override public int getRemotePort() { return 0; }
        @Override public String getLocalName() { return "localhost"; }
        @Override public String getLocalAddr() { return "127.0.0.1"; }
        @Override public int getLocalPort() { return 8080; }
        @Override public javax.servlet.ServletContext getServletContext() { return null; }
        @Override public javax.servlet.AsyncContext startAsync() { return null; }
        @Override public javax.servlet.AsyncContext startAsync(javax.servlet.ServletRequest req, javax.servlet.ServletResponse resp) { return null; }
        @Override public boolean isAsyncStarted() { return false; }
        @Override public boolean isAsyncSupported() { return false; }
        @Override public javax.servlet.AsyncContext getAsyncContext() { return null; }
        @Override public javax.servlet.DispatcherType getDispatcherType() { return javax.servlet.DispatcherType.REQUEST; }
    }
}
