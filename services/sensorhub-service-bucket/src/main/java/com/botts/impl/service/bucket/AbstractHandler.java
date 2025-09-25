package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IResourceHandler;
import org.sensorhub.api.security.IPermission;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AbstractHandler implements IResourceHandler {

    @Override
    public void list(RequestContext ctx) throws IOException {
        var sec = ctx.getSecurityHandler();
        if (!sec.hasPermission(sec.api_list)) {
            sendUnauthorized(sec.api_list, ctx);
        }
    }

    @Override
    public void get(RequestContext ctx) throws IOException {
        var sec = ctx.getSecurityHandler();
        if (!sec.hasPermission(sec.api_get)) {
            sendUnauthorized(sec.api_get, ctx);
        }
    }

    @Override
    public void create(RequestContext ctx) throws IOException {
        var sec = ctx.getSecurityHandler();
        if (!sec.hasPermission(sec.api_create)) {
            sendUnauthorized(sec.api_create, ctx);
        }
    }

    @Override
    public void update(RequestContext ctx) throws IOException {
        var sec = ctx.getSecurityHandler();
        if (!sec.hasPermission(sec.api_put)) {
            sendUnauthorized(sec.api_put, ctx);
        }
    }

    @Override
    public void delete(RequestContext ctx) throws IOException {
        var sec = ctx.getSecurityHandler();
        if (!sec.hasPermission(sec.api_delete)) {
            sendUnauthorized(sec.api_delete, ctx);
        }
    }

    protected void sendError(int code, String msg, RequestContext ctx) throws IOException {
        var accept = ctx.getRequest().getHeader("Accept");

        if (accept == null || accept.contains("json"))
        {
            ctx.getResponse().setStatus(code);
            if (msg != null)
            {
                var json =
                        "{\n" +
                                "  \"status\": " + code + ",\n" +
                                "  \"message\": \"" + msg.replace("\"", "\\\"") + "\"\n" +
                                "}";
                ctx.getOutputStream().write(json.getBytes());
            }
        }
        else
            ctx.getResponse().sendError(code, msg);
    }

    protected void sendUnauthorized(IPermission perm, RequestContext ctx) throws IOException {
        sendError(HttpServletResponse.SC_UNAUTHORIZED, perm.getErrorMessage(), ctx);
    }

}
