package com.botts.impl.service.bucket;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final BucketServlet servlet;

    private String bucketName = "";
    private String objectKey = "";

    public RequestContext(HttpServletRequest request, HttpServletResponse response, BucketServlet servlet) throws IOException {
        this.request = request;
        this.response = response;
        this.servlet = servlet;

        var pathInfo = request.getPathInfo();
        if (pathInfo == null)
            return;

        String[] parts = pathInfo.split("/", 3);

        if (parts.length > 1)
            bucketName = parts[1];

        if ((parts.length == 2 && !bucketName.isBlank()) || (parts.length == 3 && parts[2].isBlank()))
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

    public BucketServlet getServlet() {
        return servlet;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public ServletInputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    public BucketSecurity getSecurityHandler() {
        return servlet.getSecurityHandler();
    }

}
