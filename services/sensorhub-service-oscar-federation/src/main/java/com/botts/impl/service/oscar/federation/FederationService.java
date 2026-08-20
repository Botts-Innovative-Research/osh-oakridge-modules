package com.botts.impl.service.oscar.federation;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import com.botts.impl.service.oscar.federation.credentials.EncryptedFileCredentialStore;
import com.botts.impl.service.oscar.federation.credentials.FederationCredentialStore;
import com.botts.impl.service.oscar.federation.discovery.HttpOscarNodeDiscovery;
import com.botts.impl.service.oscar.federation.http.FederationServlet;
import com.botts.impl.service.oscar.federation.identity.OshLocalOscarIdentityProvider;
import com.botts.impl.service.oscar.federation.proxy.JettyFederationWebSocketProxy;
import com.botts.impl.service.oscar.federation.proxy.StreamingFederationHttpProxy;
import com.botts.impl.service.oscar.federation.registry.FederationNodeManager;
import com.botts.impl.service.oscar.federation.registry.PersistentRemoteNodeRegistry;
import com.botts.impl.service.oscar.federation.registry.RemoteNodeRegistry;
import com.botts.impl.service.oscar.federation.security.AuthenticatedUserAuthorizer;
import com.botts.impl.service.oscar.federation.security.UpstreamEndpointPolicy;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.impl.service.HttpServer;

/** Secure one-hop federation gateway mounted before the normal /sensorhub servlet context. */
public class FederationService extends AbstractHttpServiceModule<FederationServiceConfig>
{
    private FederationServlet servlet;
    private RemoteNodeRegistry registry;
    private FederationCredentialStore credentialStore;
    private WebSocketClient webSocketClient;
    private HandlerCollection serverHandlers;
    private Handler federationHandler;

    @Override
    protected void doStart() throws SensorHubException
    {
        super.doStart();
        if (!(httpServer instanceof HttpServer))
            throw new SensorHubException("OSCAR federation requires the standard OSH Jetty HTTP server");
        if (config.security == null || !config.security.requireAuth)
            throw new SensorHubException("OSCAR federation requires authenticated HTTP access");
        if (config.administratorRole == null || config.administratorRole.isBlank())
            throw new SensorHubException("Federation administratorRole must be configured");
        if (config.credentialKeyFile == null || config.credentialKeyFile.isBlank())
            throw new SensorHubException("Federation credentialKeyFile must be configured");
        if (config.connectTimeoutSeconds <= 0 || config.requestTimeoutSeconds <= 0 || config.maxRegistrationBodyBytes <= 0)
            throw new SensorHubException("Federation timeout and request-size settings must be positive");

        try
        {
            var state = getParentHub().getModuleRegistry().getStateManager(getLocalID());
            Path credentialState = state.getDataFile("federation-credentials.json").toPath();
            Path registryState = state.getDataFile("federation-nodes.json").toPath();
            credentialStore = new EncryptedFileCredentialStore(credentialState, Path.of(config.credentialKeyFile));
            registry = new PersistentRemoteNodeRegistry(registryState, config.remoteNodes);

            Duration connectTimeout = Duration.ofSeconds(config.connectTimeoutSeconds);
            Duration requestTimeout = Duration.ofSeconds(config.requestTimeoutSeconds);
            UpstreamEndpointPolicy endpointPolicy = new UpstreamEndpointPolicy(
                config.allowInsecureHttp, config.allowPrivateNetworkTargets);
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

            var localIdentity = new OshLocalOscarIdentityProvider(getParentHub().getModuleRegistry());
            var discovery = new HttpOscarNodeDiscovery(client, endpointPolicy, requestTimeout);
            var nodeManager = new FederationNodeManager(
                registry, credentialStore, discovery, localIdentity, endpointPolicy);

            webSocketClient = new WebSocketClient();
            webSocketClient.getPolicy().setMaxBinaryMessageSize(1024 * 1024);
            webSocketClient.setConnectTimeout(connectTimeout.toMillis());
            webSocketClient.start();

            servlet = new FederationServlet(
                registry,
                nodeManager,
                new AuthenticatedUserAuthorizer(config.administratorRole),
                new StreamingFederationHttpProxy(client, credentialStore, endpointPolicy, requestTimeout),
                new JettyFederationWebSocketProxy(webSocketClient, credentialStore,
                    endpointPolicy, connectTimeout, getLogger()),
                config.maxRegistrationBodyBytes);

            deployRootContext((HttpServer)httpServer);
            getLogger().info("OSCAR federation gateway started at {}", config.endPoint);
        }
        catch (SensorHubException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            stopWebSocketClient();
            throw new SensorHubException("Cannot start OSCAR federation service", e);
        }
    }

    private void deployRootContext(HttpServer server) throws Exception
    {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(config.endPoint);
        context.addServlet(new ServletHolder(servlet), "/*");

        ConstraintSecurityHandler existingSecurity =
            (ConstraintSecurityHandler)server.getServletHandler().getSecurityHandler();
        if (config.security.requireAuth && existingSecurity == null)
            throw new SensorHubException("Federation requires an authenticated OSH HTTP server");

        if (existingSecurity != null)
        {
            ConstraintSecurityHandler federationSecurity = new ConstraintSecurityHandler();
            federationSecurity.setAuthenticator(existingSecurity.getAuthenticator());
            federationSecurity.setLoginService(existingSecurity.getLoginService());
            federationSecurity.setHandler(context);

            Constraint constraint = new Constraint();
            constraint.setAuthenticate(config.security.requireAuth);
            constraint.setRoles(new String[] { Constraint.ANY_AUTH });
            org.eclipse.jetty.security.ConstraintMapping mapping = new org.eclipse.jetty.security.ConstraintMapping();
            mapping.setConstraint(constraint);
            mapping.setPathSpec("/*");
            mapping.setMethodOmissions(new String[] { "OPTIONS" });
            federationSecurity.addConstraintMapping(mapping);
            federationHandler = federationSecurity;
        }
        else
            federationHandler = context;

        serverHandlers = (HandlerCollection)server.getJettyServer().getHandler();
        federationHandler.setServer(server.getJettyServer());
        federationHandler.start();
        serverHandlers.addHandler(federationHandler);
    }

    @Override
    protected void doStop() throws SensorHubException
    {
        if (serverHandlers != null && federationHandler != null)
        {
            serverHandlers.removeHandler(federationHandler);
            try { federationHandler.stop(); }
            catch (Exception e) { getLogger().warn("Cannot stop federation HTTP handler", e); }
        }
        federationHandler = null;
        serverHandlers = null;
        servlet = null;
        stopWebSocketClient();
        registry = null;
        credentialStore = null;
        super.doStop();
    }

    private void stopWebSocketClient()
    {
        if (webSocketClient != null)
        {
            try { webSocketClient.stop(); }
            catch (Exception e) { getLogger().warn("Cannot stop federation WebSocket client", e); }
            webSocketClient = null;
        }
    }

    public RemoteNodeRegistry getRegistry()
    {
        return registry;
    }
}
