package com.botts.impl.service.oscar.federation.model;

import java.net.URI;

/**
 * Write-only registration input. This type must never be used as a node-list
 * response or persisted directly in module configuration.
 */
public final class NodeRegistrationRequest
{
    private final URI upstreamBaseUri;
    private final String username;
    private final char[] password;
    private final boolean allowPrivateNetwork;
    private final boolean enabled;

    public NodeRegistrationRequest(URI upstreamBaseUri, String username, char[] password)
    {
        this(upstreamBaseUri, username, password, false, true);
    }

    public NodeRegistrationRequest(URI upstreamBaseUri, String username, char[] password,
            boolean allowPrivateNetwork, boolean enabled)
    {
        this.upstreamBaseUri = upstreamBaseUri;
        this.username = username;
        this.password = password == null ? new char[0] : password.clone();
        this.allowPrivateNetwork = allowPrivateNetwork;
        this.enabled = enabled;
    }

    public URI getUpstreamBaseUri() { return upstreamBaseUri; }
    public String getUsername() { return username; }
    public char[] copyPassword() { return password.clone(); }
    public boolean isPrivateNetworkAllowed() { return allowPrivateNetwork; }
    public boolean isEnabled() { return enabled; }

    public void clearPassword()
    {
        java.util.Arrays.fill(password, '\0');
    }
}
