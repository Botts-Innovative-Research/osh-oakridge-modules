package com.botts.impl.service.bucket.util;

import com.botts.impl.service.bucket.BucketSecurity;
import com.botts.impl.service.bucket.BucketServlet;
import com.google.gson.stream.JsonWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final BucketServlet servlet;

    private String bucketName = "";
    private String objectKey = "";
    private JsonWriter jsonWriter = null;
    private Map<String, String> headers;

    public RequestContext(HttpServletRequest request, HttpServletResponse response, BucketServlet servlet) throws IOException {
        this.request = request;
        this.response = response;
        this.servlet = servlet;

        var headersIter = request.getHeaderNames().asIterator();
        headers = new HashMap<>();
        while (headersIter.hasNext()) {
            var headerName = headersIter.next();
            headers.put(headerName, request.getHeader(headerName));
        }

        var pathInfo = request.getPathInfo();
        if (pathInfo == null)
            return;

        String[] parts = pathInfo.split("/", 3);

        if (parts.length > 1)
            bucketName = parts[1];

        if ((parts.length == 3 && !bucketName.isBlank()) && !parts[2].isBlank())
            objectKey = parts[2];
    }

    public boolean hasBucketName() {
        return bucketName != null && !bucketName.isBlank();
    }

    public boolean hasObjectKey() {
        return hasBucketName() && (objectKey != null && !objectKey.isBlank());
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public String getResourceURL(String resource) {
        String url = request.getRequestURL().toString();

        if (url.endsWith("/"))
            url += resource;
        else
            url += "/" + resource;
        return url;
    }

    public BucketSecurity getSecurityHandler() {
        return servlet.getSecurityHandler();
    }

    public JsonWriter getJsonWriter() throws IOException {
        if (jsonWriter == null)
            jsonWriter = new JsonWriter(response.getWriter());
        return jsonWriter;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

}
