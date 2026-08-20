package com.botts.impl.service.oscar.federation.registry;

import java.time.Instant;
import java.util.List;
import com.botts.impl.service.oscar.federation.credentials.FederationCredentialStore;
import com.botts.impl.service.oscar.federation.credentials.FederationCredentials;
import com.botts.impl.service.oscar.federation.discovery.OscarNodeDiscovery;
import com.botts.impl.service.oscar.federation.identity.LocalOscarIdentityProvider;
import com.botts.impl.service.oscar.federation.model.FederationNodeView;
import com.botts.impl.service.oscar.federation.model.NodeRegistrationRequest;
import com.botts.impl.service.oscar.federation.model.RemoteNodeConnection;
import com.botts.impl.service.oscar.federation.security.UpstreamEndpointPolicy;

public class FederationNodeManager
{
    private final RemoteNodeRegistry registry;
    private final FederationCredentialStore credentialStore;
    private final OscarNodeDiscovery discovery;
    private final LocalOscarIdentityProvider localIdentity;
    private final UpstreamEndpointPolicy endpointPolicy;

    public FederationNodeManager(RemoteNodeRegistry registry, FederationCredentialStore credentialStore,
            OscarNodeDiscovery discovery, LocalOscarIdentityProvider localIdentity,
            UpstreamEndpointPolicy endpointPolicy)
    {
        this.registry = registry;
        this.credentialStore = credentialStore;
        this.discovery = discovery;
        this.localIdentity = localIdentity;
        this.endpointPolicy = endpointPolicy;
    }

    public List<FederationNodeView> list()
    {
        List<FederationNodeView> nodes = new java.util.ArrayList<>();
        nodes.add(new FederationNodeView(localIdentity.getLocalOscarSystemUid(), true, true, null));
        registry.list().stream()
            .sorted(java.util.Comparator.comparing(RemoteNodeConnection::getOscarSystemUid))
            .map(node -> new FederationNodeView(node.getOscarSystemUid(), false, node.isEnabled(),
                node.getUpstreamBaseUri().toString()))
            .forEach(nodes::add);
        return nodes;
    }

    public FederationNodeView register(NodeRegistrationRequest request)
    {
        var normalizedUri = endpointPolicy.validateAndNormalize(
            request.getUpstreamBaseUri(), request.isPrivateNetworkAllowed());
        String uid = discovery.discover(request).getOscarSystemUid();
        if (uid.equals(localIdentity.getLocalOscarSystemUid()))
            throw new IllegalArgumentException("The local OSCAR node cannot be registered as a remote target");
        if (registry.findByUid(uid).isPresent())
            throw new IllegalArgumentException("OSCAR system UID is already registered");

        char[] password = request.copyPassword();
        String credentialId;
        try (FederationCredentials credentials = new FederationCredentials(request.getUsername(), password))
        {
            credentialId = credentialStore.store(credentials);
        }
        finally
        {
            java.util.Arrays.fill(password, '\0');
        }

        var connection = new RemoteNodeConnection(uid, normalizedUri, credentialId,
            request.isEnabled(), request.isPrivateNetworkAllowed(), Instant.now(), null);
        try
        {
            registry.add(connection);
        }
        catch (RuntimeException e)
        {
            credentialStore.remove(credentialId);
            throw e;
        }
        return new FederationNodeView(uid, false, connection.isEnabled(), normalizedUri.toString());
    }

    public FederationNodeView update(String expectedUid, NodeRegistrationRequest request)
    {
        RemoteNodeConnection current = registry.findByUid(expectedUid)
            .orElseThrow(() -> new IllegalArgumentException("OSCAR system UID is not registered"));
        var normalizedUri = endpointPolicy.validateAndNormalize(
            request.getUpstreamBaseUri(), request.isPrivateNetworkAllowed());
        discovery.verify(expectedUid, request);

        char[] password = request.copyPassword();
        String replacementCredentialId;
        try (FederationCredentials credentials = new FederationCredentials(request.getUsername(), password))
        {
            replacementCredentialId = credentialStore.store(credentials);
        }
        finally
        {
            java.util.Arrays.fill(password, '\0');
        }

        var replacement = new RemoteNodeConnection(expectedUid, normalizedUri, replacementCredentialId,
            request.isEnabled(), request.isPrivateNetworkAllowed(), Instant.now(), null);
        try
        {
            registry.replace(replacement);
        }
        catch (RuntimeException e)
        {
            credentialStore.remove(replacementCredentialId);
            throw e;
        }
        credentialStore.remove(current.getCredentialId());
        return new FederationNodeView(expectedUid, false, replacement.isEnabled(), normalizedUri.toString());
    }

    public void remove(String uid)
    {
        RemoteNodeConnection current = registry.findByUid(uid)
            .orElseThrow(() -> new IllegalArgumentException("OSCAR system UID is not registered"));
        registry.remove(uid);
        credentialStore.remove(current.getCredentialId());
    }
}
