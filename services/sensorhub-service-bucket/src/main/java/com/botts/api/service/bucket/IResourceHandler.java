package com.botts.api.service.bucket;

import com.botts.impl.service.bucket.util.RequestContext;

import java.io.IOException;

public interface IResourceHandler {

    void doGet(RequestContext ctx) throws IOException, SecurityException;

    void doPost(RequestContext ctx) throws IOException, SecurityException;

    void doPut(RequestContext ctx) throws IOException, SecurityException;

    void doDelete(RequestContext ctx) throws IOException, SecurityException;

}
