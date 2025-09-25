package com.botts.api.service.bucket;

import com.botts.impl.service.bucket.RequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface IResourceHandler {

    void list(RequestContext ctx) throws IOException, SecurityException;

    void get(RequestContext ctx) throws IOException, SecurityException;

    void create(RequestContext ctx) throws IOException, SecurityException;

    void update(RequestContext ctx) throws IOException, SecurityException;

    void delete(RequestContext ctx) throws IOException, SecurityException;

}
