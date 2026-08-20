package com.botts.impl.service.oscar.federation.model;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

public final class RemoteNodeConnection
{
    private final String oscarSystemUid;
    private final URI upstreamBaseUri;
    private final String credentialId;
    private final boolean enabled;
    private final boolean allowPrivateNetwork;
    private final Instant lastSuccessfulConnection;
    private final String lastFailureCategory;

    public RemoteNodeConnection(String oscarSystemUid, URI upstreamBaseUri, String credentialId,
            boolean enabled, boolean allowPrivateNetwork, Instant lastSuccessfulConnection, String lastFailureCategory)
    {
        this.oscarSystemUid = Objects.requireNonNull(oscarSystemUid);
        this.upstreamBaseUri = Objects.requireNonNull(upstreamBaseUri);
        this.credentialId = credentialId;
        this.enabled = enabled;
        this.allowPrivateNetwork = allowPrivateNetwork;
        this.lastSuccessfulConnection = lastSuccessfulConnection;
        this.lastFailureCategory = lastFailureCategory;
    }

    public String getOscarSystemUid() { return oscarSystemUid; }
    public URI getUpstreamBaseUri() { return upstreamBaseUri; }
    public String getCredentialId() { return credentialId; }
    public boolean isEnabled() { return enabled; }
    public boolean isPrivateNetworkAllowed() { return allowPrivateNetwork; }
    public Instant getLastSuccessfulConnection() { return lastSuccessfulConnection; }
    public String getLastFailureCategory() { return lastFailureCategory; }
}
