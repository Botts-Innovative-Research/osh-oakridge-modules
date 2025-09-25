package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IResourceHandler;
import org.sensorhub.impl.module.ModuleSecurity;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class BucketStorageServlet extends HttpServlet {

    private final String rootUrl;
    private final ExecutorService threadPool;
    private final ModuleSecurity securityHandler;
    private final Logger log;

    public BucketStorageServlet(BucketStorageService service, ModuleSecurity securityHandler) {
        this.threadPool = service.getThreadPool();
        this.securityHandler = securityHandler;
        this.log = service.getLogger();

        var endpointUrl = service.getPublicEndpointUrl();
        this.rootUrl = endpointUrl.endsWith("/") ? endpointUrl.substring(0, endpointUrl.length()-1) : endpointUrl;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getContextPath();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
    }

}
