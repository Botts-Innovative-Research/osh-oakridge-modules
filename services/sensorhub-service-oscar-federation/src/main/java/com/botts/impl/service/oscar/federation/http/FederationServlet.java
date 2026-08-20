package com.botts.impl.service.oscar.federation.http;

import java.io.IOException;
import java.net.URI;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.botts.impl.service.oscar.federation.proxy.FederationHttpProxy;
import com.botts.impl.service.oscar.federation.proxy.FederationWebSocketProxy;
import com.botts.impl.service.oscar.federation.registry.RemoteNodeRegistry;
import com.botts.impl.service.oscar.federation.registry.FederationNodeManager;
import com.botts.impl.service.oscar.federation.security.FederationAuthorizer;
import com.botts.impl.service.oscar.federation.model.NodeRegistrationRequest;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;

public class FederationServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private final RemoteNodeRegistry registry;
    private final FederationAuthorizer authorizer;
    private final FederationHttpProxy httpProxy;
    private final FederationWebSocketProxy webSocketProxy;
    private final FederationNodeManager nodeManager;
    private final int maxManagementBodyBytes;
    private final Gson gson = new Gson();
    private WebSocketServerFactory wsFactory;

    private static final class RegistrationPayload
    {
        String upstreamBaseUrl;
        String username;
        String password;
        boolean allowPrivateNetwork;
        boolean enabled = true;
    }

    public FederationServlet(RemoteNodeRegistry registry, FederationNodeManager nodeManager,
            FederationAuthorizer authorizer, FederationHttpProxy httpProxy,
            FederationWebSocketProxy webSocketProxy, int maxManagementBodyBytes)
    {
        this.registry = registry;
        this.nodeManager = nodeManager;
        this.authorizer = authorizer;
        this.httpProxy = httpProxy;
        this.webSocketProxy = webSocketProxy;
        this.maxManagementBodyBytes = maxManagementBodyBytes;
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        try
        {
            WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
            policy.setMaxBinaryMessageSize(1024 * 1024);
            wsFactory = new WebSocketServerFactory(getServletContext(), policy);
            wsFactory.start();
        }
        catch (Exception e)
        {
            throw new ServletException("Cannot initialize federation WebSocket endpoint", e);
        }
    }

    @Override
    public void destroy()
    {
        try
        {
            if (wsFactory != null)
                wsFactory.stop();
        }
        catch (Exception ignored) {}
        super.destroy();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        response.setHeader("X-Content-Type-Options", "nosniff");

        if ("OPTIONS".equals(request.getMethod()))
        {
            response.setHeader("Allow", "GET,POST,PUT,DELETE,OPTIONS");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        FederationRoute route = FederationRoute.parse(request.getPathInfo());
        if (route.getKind() == FederationRoute.Kind.INVALID)
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid federation route");
            return;
        }

        if (route.getKind() == FederationRoute.Kind.NODE_MANAGEMENT)
        {
            if (!authorizer.canManageNodes(request))
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            else
                handleManagement(route, request, response);
            return;
        }

        String targetUid = route.getTargetUid().orElseThrow();
        if (!authorizer.canAccessNode(request, targetUid))
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        var node = registry.findByUid(targetUid);
        if (node.isEmpty() || !node.get().isEnabled())
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Federation target is not registered or enabled");
            return;
        }

        if (route.getKind() == FederationRoute.Kind.MQTT_WEBSOCKET)
            handleWebSocket(node.get(), request, response);
        else
            httpProxy.proxy(node.get(), route.getUpstreamPath().orElseThrow(), request, response);
    }

    private void handleManagement(FederationRoute route, HttpServletRequest request,
            HttpServletResponse response) throws IOException
    {
        response.setHeader("Cache-Control", "no-store");
        String uid = route.getTargetUid().orElse(null);
        try
        {
            switch (request.getMethod())
            {
                case "GET":
                    if (uid != null)
                    {
                        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }
                    writeJson(response, HttpServletResponse.SC_OK, nodeManager.list());
                    return;
                case "POST":
                    if (uid != null)
                    {
                        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }
                    writeJson(response, HttpServletResponse.SC_CREATED,
                        withRegistrationRequest(request, nodeManager::register));
                    return;
                case "PUT":
                    if (uid == null)
                    {
                        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }
                    writeJson(response, HttpServletResponse.SC_OK,
                        withRegistrationRequest(request, registration -> nodeManager.update(uid, registration)));
                    return;
                case "DELETE":
                    if (uid == null)
                    {
                        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }
                    nodeManager.remove(uid);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    return;
                default:
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        }
        catch (IllegalArgumentException e)
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
        catch (IllegalStateException e)
        {
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
        }
    }

    private <T> T withRegistrationRequest(HttpServletRequest request,
            java.util.function.Function<NodeRegistrationRequest, T> operation) throws IOException
    {
        byte[] body = request.getInputStream().readNBytes(maxManagementBodyBytes + 1);
        if (body.length > maxManagementBodyBytes)
            throw new IllegalArgumentException("Federation registration request is too large");
        RegistrationPayload payload;
        try
        {
            payload = gson.fromJson(new String(body, java.nio.charset.StandardCharsets.UTF_8), RegistrationPayload.class);
        }
        catch (JsonParseException e)
        {
            throw new IllegalArgumentException("Invalid registration JSON", e);
        }
        finally
        {
            java.util.Arrays.fill(body, (byte)0);
        }
        if (payload == null || payload.upstreamBaseUrl == null || payload.username == null || payload.password == null)
            throw new IllegalArgumentException("upstreamBaseUrl, username, and password are required");

        char[] password = payload.password.toCharArray();
        NodeRegistrationRequest registration = new NodeRegistrationRequest(
            URI.create(payload.upstreamBaseUrl), payload.username, password,
            payload.allowPrivateNetwork, payload.enabled);
        java.util.Arrays.fill(password, '\0');
        try
        {
            return operation.apply(registration);
        }
        finally
        {
            registration.clearPassword();
            payload.password = null;
        }
    }

    private void handleWebSocket(com.botts.impl.service.oscar.federation.model.RemoteNodeConnection node,
            HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if (!wsFactory.isUpgradeRequest(request, response))
        {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "MQTT federation requires a WebSocket upgrade");
            return;
        }
        wsFactory.acceptWebSocket((upgradeRequest, upgradeResponse) -> {
            try
            {
                return webSocketProxy.createMqttBridge(node, upgradeRequest, upgradeResponse);
            }
            catch (RuntimeException e)
            {
                try { upgradeResponse.sendError(HttpServletResponse.SC_BAD_GATEWAY, e.getMessage()); }
                catch (IOException ignored) {}
                return null;
            }
        }, request, response);
    }

    private void writeJson(HttpServletResponse response, int status, Object value) throws IOException
    {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        gson.toJson(value, response.getWriter());
    }
}
