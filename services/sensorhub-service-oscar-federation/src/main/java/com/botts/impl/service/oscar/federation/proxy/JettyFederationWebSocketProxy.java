package com.botts.impl.service.oscar.federation.proxy;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.botts.impl.service.oscar.federation.credentials.FederationCredentialStore;
import com.botts.impl.service.oscar.federation.model.RemoteNodeConnection;
import com.botts.impl.service.oscar.federation.security.BasicAuth;
import com.botts.impl.service.oscar.federation.security.UpstreamEndpointPolicy;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.slf4j.Logger;

public class JettyFederationWebSocketProxy implements FederationWebSocketProxy
{
    private static final String MQTT = "mqtt";
    private final WebSocketClient client;
    private final FederationCredentialStore credentialStore;
    private final UpstreamEndpointPolicy endpointPolicy;
    private final Duration connectTimeout;
    private final Logger log;

    public JettyFederationWebSocketProxy(WebSocketClient client,
            FederationCredentialStore credentialStore, UpstreamEndpointPolicy endpointPolicy,
            Duration connectTimeout, Logger log)
    {
        this.client = client;
        this.credentialStore = credentialStore;
        this.endpointPolicy = endpointPolicy;
        this.connectTimeout = connectTimeout;
        this.log = log;
    }

    @Override
    public Object createMqttBridge(RemoteNodeConnection node,
            ServletUpgradeRequest request, ServletUpgradeResponse response)
    {
        if (!request.getSubProtocols().contains(MQTT))
            throw new IllegalArgumentException("MQTT WebSocket subprotocol is required");

        URI base = endpointPolicy.validateAndNormalize(node.getUpstreamBaseUri(), node.isPrivateNetworkAllowed());
        URI upstream = mqttUri(base);
        String authorization;
        var credentials = credentialStore.retrieve(node.getCredentialId())
            .orElseThrow(() -> new IllegalStateException("Federation credentials are unavailable"));
        try (credentials)
        {
            authorization = BasicAuth.headerValue(credentials);
        }

        response.setAcceptedSubProtocol(MQTT);
        return new BrowserSocket(client, upstream, authorization, connectTimeout, log);
    }

    private static URI mqttUri(URI base)
    {
        try
        {
            String scheme = "https".equalsIgnoreCase(base.getScheme()) ? "wss" : "ws";
            return new URI(scheme, null, base.getHost(), base.getPort(), "/mqtt", null, null);
        }
        catch (java.net.URISyntaxException e)
        {
            throw new IllegalArgumentException("Invalid MQTT upstream URI", e);
        }
    }

    private static final class BrowserSocket implements WebSocketListener
    {
        private final WebSocketClient client;
        private final URI upstreamUri;
        private final String authorization;
        private final Duration timeout;
        private final Logger log;
        private final AtomicBoolean closing = new AtomicBoolean();
        private volatile Session browser;
        private volatile Session upstream;

        BrowserSocket(WebSocketClient client, URI upstreamUri, String authorization,
                Duration timeout, Logger log)
        {
            this.client = client;
            this.upstreamUri = upstreamUri;
            this.authorization = authorization;
            this.timeout = timeout;
            this.log = log;
        }

        @Override
        public void onWebSocketConnect(Session session)
        {
            browser = session;
            try
            {
                ClientUpgradeRequest request = new ClientUpgradeRequest();
                request.setSubProtocols(MQTT);
                request.setHeader("Authorization", authorization);
                upstream = client.connect(new UpstreamSocket(this), upstreamUri, request)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }
            catch (Exception e)
            {
                log.warn("Cannot establish federated MQTT WebSocket to target", e);
                closeBoth(StatusCode.SERVER_ERROR, "Cannot connect to federation upstream");
            }
        }

        @Override
        public void onWebSocketBinary(byte[] payload, int offset, int length)
        {
            sendBinary(upstream, payload, offset, length);
        }

        @Override
        public void onWebSocketText(String message)
        {
            sendText(upstream, message);
        }

        @Override public void onWebSocketClose(int statusCode, String reason) { closeBoth(statusCode, reason); }
        @Override public void onWebSocketError(Throwable cause) { log.debug("Browser federation WebSocket error", cause); closeBoth(StatusCode.SERVER_ERROR, "WebSocket error"); }

        void upstreamBinary(byte[] payload, int offset, int length) { sendBinary(browser, payload, offset, length); }
        void upstreamText(String message) { sendText(browser, message); }
        void upstreamClosed(int statusCode, String reason) { closeBoth(statusCode, reason); }
        void upstreamError(Throwable cause) { log.debug("Upstream federation WebSocket error", cause); closeBoth(StatusCode.SERVER_ERROR, "Upstream WebSocket error"); }

        private void sendBinary(Session target, byte[] payload, int offset, int length)
        {
            if (target == null || !target.isOpen())
            {
                closeBoth(StatusCode.SERVER_ERROR, "Federation peer is unavailable");
                return;
            }
            try
            {
                target.getRemote().sendBytes(ByteBuffer.wrap(payload, offset, length));
            }
            catch (IOException e)
            {
                closeBoth(StatusCode.SERVER_ERROR, "WebSocket forwarding failed");
            }
        }

        private void sendText(Session target, String message)
        {
            if (target == null || !target.isOpen())
            {
                closeBoth(StatusCode.SERVER_ERROR, "Federation peer is unavailable");
                return;
            }
            try
            {
                target.getRemote().sendString(message);
            }
            catch (IOException e)
            {
                closeBoth(StatusCode.SERVER_ERROR, "WebSocket forwarding failed");
            }
        }

        private void closeBoth(int code, String reason)
        {
            if (!closing.compareAndSet(false, true))
                return;
            if (browser != null && browser.isOpen())
                browser.close(code, reason);
            if (upstream != null && upstream.isOpen())
                upstream.close(code, reason);
        }
    }

    private static final class UpstreamSocket implements WebSocketListener
    {
        private final BrowserSocket bridge;
        UpstreamSocket(BrowserSocket bridge) { this.bridge = bridge; }
        @Override public void onWebSocketConnect(Session session) {}
        @Override public void onWebSocketBinary(byte[] payload, int offset, int length) { bridge.upstreamBinary(payload, offset, length); }
        @Override public void onWebSocketText(String message) { bridge.upstreamText(message); }
        @Override public void onWebSocketClose(int statusCode, String reason) { bridge.upstreamClosed(statusCode, reason); }
        @Override public void onWebSocketError(Throwable cause) { bridge.upstreamError(cause); }
    }
}
