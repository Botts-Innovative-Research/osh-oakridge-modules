package com.botts.impl.service.bucket;

import com.botts.impl.service.bucket.handler.BucketHandler;
import com.botts.impl.service.bucket.util.InvalidRequestException;
import com.botts.impl.service.bucket.util.RequestContext;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.impl.module.ModuleSecurity;
import org.slf4j.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static javax.servlet.http.HttpServletResponse.*;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

public class BucketServlet extends HttpServlet {

    static final String LOG_REQUEST_MSG = "{} {}{} (from ip={}, user={})";
    static final String INTERNAL_ERROR_MSG = "Internal server error";
    static final String INTERNAL_ERROR_LOG_MSG = INTERNAL_ERROR_MSG + " while processing request " + LOG_REQUEST_MSG;
    static final String ACCESS_DENIED_ERROR_MSG = "Permission denied";
    static final String JSON_CONTENT_TYPE = "application/json";

    private final String rootUrl;
    private final ExecutorService threadPool;
    private final ModuleSecurity securityHandler;
    private final Logger log;
    private final BucketSecurity sec;
    private final BucketHandler bucketHandler;

    public BucketServlet(BucketService service, ModuleSecurity securityHandler, BucketHandler bucketHandler) {
        this.sec = (BucketSecurity) securityHandler;
        this.threadPool = service.getThreadPool();
        this.securityHandler = securityHandler;
        this.log = service.getLogger();

        var endpointUrl = service.getPublicEndpointUrl();
        this.rootUrl = endpointUrl.endsWith("/") ? endpointUrl.substring(0, endpointUrl.length()-1) : endpointUrl;
        this.bucketHandler = bucketHandler;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setContentType(JSON_CONTENT_TYPE);
            var ctx = new RequestContext(req, resp, this);

            final AsyncContext aCtx = req.startAsync(req, resp);
            CompletableFuture.runAsync(() -> {
                try {
                    setCurrentUser(req);
                    bucketHandler.doGet(ctx);
                } catch (InvalidRequestException e) {
                    handleInvalidRequestException(req, resp, e);
                } catch (SecurityException e) {
                    handleAuthException(req, resp, e);
                } catch (Exception e) {
                    if (e.getCause() instanceof InvalidRequestException) {
                        handleInvalidRequestException(req, resp, (InvalidRequestException) e.getCause());
                    } else {
                        logError(req, e);
                        sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, req, resp);
                    }
                } finally {
                    clearCurrentUser();
                    aCtx.complete();
                }
            }, threadPool);
        } catch (Exception e) {
            logError(req, e);
            sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setContentType(JSON_CONTENT_TYPE);
            var ctx = new RequestContext(req, resp, this);

            final AsyncContext aCtx = req.startAsync(req, resp);
            CompletableFuture.runAsync(() -> {
                try {
                    setCurrentUser(req);
                    bucketHandler.doPost(ctx);
                } catch (InvalidRequestException e) {
                    handleInvalidRequestException(req, resp, e);
                } catch (SecurityException e) {
                    handleAuthException(req, resp, e);
                } catch (Exception e) {
                    if (e.getCause() instanceof InvalidRequestException) {
                        handleInvalidRequestException(req, resp, (InvalidRequestException) e.getCause());
                    } else {
                        logError(req, e);
                        sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, req, resp);
                    }
                } finally {
                    clearCurrentUser();
                    aCtx.complete();
                }
            }, threadPool);
        } catch (Exception e) {
            logError(req, e);
            sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, req, resp);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setContentType(JSON_CONTENT_TYPE);
            var ctx = new RequestContext(req, resp, this);

            final AsyncContext aCtx = req.startAsync(req, resp);
            CompletableFuture.runAsync(() -> {
                try {
                    setCurrentUser(req);
                    bucketHandler.doPut(ctx);
                } catch (InvalidRequestException e) {
                    handleInvalidRequestException(req, resp, e);
                } catch (SecurityException e) {
                    handleAuthException(req, resp, e);
                } catch (Exception e) {
                    if (e.getCause() instanceof InvalidRequestException) {
                        handleInvalidRequestException(req, resp, (InvalidRequestException) e.getCause());
                    } else {
                        logError(req, e);
                        sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, req, resp);
                    }
                } finally {
                    clearCurrentUser();
                    aCtx.complete();
                }
            }, threadPool);
        } catch (Exception e) {
            logError(req, e);
            sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, req, resp);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setContentType(JSON_CONTENT_TYPE);
            var ctx = new RequestContext(req, resp, this);

            final AsyncContext aCtx = req.startAsync(req, resp);
            CompletableFuture.runAsync(() -> {
                try {
                    setCurrentUser(req);
                    bucketHandler.doDelete(ctx);
                    ctx.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
                } catch (InvalidRequestException e) {
                    handleInvalidRequestException(req, resp, e);
                } catch (SecurityException e) {
                    handleAuthException(req, resp, e);
                } catch (Exception e) {
                    if (e.getCause() instanceof InvalidRequestException) {
                        handleInvalidRequestException(req, resp, (InvalidRequestException) e.getCause());
                    } else {
                        logError(req, e);
                        sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, req, resp);
                    }
                } finally {
                    clearCurrentUser();
                    aCtx.complete();
                }
            }, threadPool);
        } catch (Exception e) {
            logError(req, e);
            sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, req, resp);
        }
    }

    private void handleInvalidRequestException(HttpServletRequest req, HttpServletResponse resp, InvalidRequestException e) {
        log.debug("Invalid request ({}): {}", e.getErrorCode(), e.getMessage());

        switch (e.getErrorCode())
        {
            case UNSUPPORTED_OPERATION:
                sendError(SC_METHOD_NOT_ALLOWED, e.getMessage(), req, resp);
                break;

            case BAD_REQUEST:
            case BAD_PAYLOAD:
            case REQUEST_REJECTED:
                sendError(SC_BAD_REQUEST, e.getMessage(), req, resp);
                break;

            case NOT_FOUND:
                sendError(SC_NOT_FOUND, e.getMessage(), req, resp);
                break;

            case FORBIDDEN:
                sendError(SC_FORBIDDEN, e.getMessage(), req, resp);
                break;

            case REQUEST_ACCEPTED_TIMEOUT:
                sendError(202, e.getMessage(), req, resp);
                break;

            default:
                sendError(SC_INTERNAL_SERVER_ERROR, e.getMessage(), req, resp);
        }
    }

    private void sendError(int code, String msg, HttpServletRequest req, HttpServletResponse resp) {
        try {
            var accept = req.getHeader("Accept");

            if (accept == null || accept.contains("json")) {
                resp.setStatus(code);
                if (msg != null) {
                    var json =
                            "{\n" +
                                    "  \"status\": " + code + ",\n" +
                                    "  \"message\": \"" + msg.replace("\"", "\\\"") + "\"\n" +
                                    "}";
                    resp.getOutputStream().write(json.getBytes());
                }
            } else
                resp.sendError(code, msg);
        } catch (IOException e) {
            log.error("Could not send error response", e);
        }
    }

    public BucketSecurity getSecurityHandler() {
        return sec;
    }

    private void setCurrentUser(HttpServletRequest req) {
        String userID = ISecurityManager.ANONYMOUS_USER;
        if (req.getRemoteUser() != null)
            userID = req.getRemoteUser();
        securityHandler.setCurrentUser(userID);
    }

    private void clearCurrentUser() {
        securityHandler.clearCurrentUser();
    }

    protected void handleAuthException(HttpServletRequest req, HttpServletResponse resp, SecurityException e)
    {
        try
        {
            log.debug("Not authorized: {}", e.getMessage());

            if (req != null && resp != null)
            {
                if (req.getRemoteUser() == null)
                    req.authenticate(resp);
                else
                    sendError(SC_FORBIDDEN, ACCESS_DENIED_ERROR_MSG, req, resp);
            }
        }
        catch (Exception e1)
        {
            log.error("Could not send authentication request", e1);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logRequest(req);
        super.service(req, resp);
    }

    protected void logRequest(HttpServletRequest req)
    {
        if (log.isInfoEnabled())
            logRequestInfo(req, null);
    }


    protected void logError(HttpServletRequest req, Throwable e)
    {
        if (log.isErrorEnabled())
            logRequestInfo(req, e);
    }


    protected void logRequestInfo(HttpServletRequest req, Throwable error)
    {
        String method = req.getMethod();
        String url = req.getRequestURI();
        String ip = req.getRemoteAddr();
        String user = req.getRemoteUser() != null ? req.getRemoteUser() : "anonymous";

        // if proxy header present, use source ip instead of proxy ip
        String proxyHeader = req.getHeader("X-Forwarded-For");
        if (proxyHeader != null)
        {
            String[] ips = proxyHeader.split(",");
            if (ips.length >= 1)
                ip = ips[0];
        }

        // detect websocket upgrade
        if ("websocket".equalsIgnoreCase(req.getHeader("Upgrade")))
            method += "/Websocket";

        // append decoded request if any
        String query = "";
        if (req.getQueryString() != null)
            query = "?" + req.getQueryString();

        if (error != null)
            log.error(INTERNAL_ERROR_LOG_MSG, method, url, query, ip, user, error);
        else
            log.info(LOG_REQUEST_MSG, method, url, query, ip, user);
    }

}
