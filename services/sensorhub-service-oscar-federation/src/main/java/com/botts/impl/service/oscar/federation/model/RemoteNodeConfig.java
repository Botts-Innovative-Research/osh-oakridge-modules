package com.botts.impl.service.oscar.federation.model;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Persistable, non-secret connection metadata. Credentials are referenced by
 * opaque ID and must be stored outside OSH module configuration.
 */
public class RemoteNodeConfig
{
    @DisplayInfo.Required
    @DisplayInfo(label = "OSCAR System UID")
    public String oscarSystemUid;

    @DisplayInfo.Required
    @DisplayInfo(label = "Upstream Base URL")
    public String upstreamBaseUrl;

    @DisplayInfo(label = "Credential Reference")
    public String credentialId;

    @DisplayInfo(label = "Enabled")
    public boolean enabled = true;

    @DisplayInfo(label = "Allow Private Network", desc = "Allow this target to resolve to private addresses when the service policy also permits it")
    public boolean allowPrivateNetwork;

    public RemoteNodeConfig() {}

    public RemoteNodeConfig(String oscarSystemUid, String upstreamBaseUrl, String credentialId,
            boolean enabled, boolean allowPrivateNetwork)
    {
        this.oscarSystemUid = oscarSystemUid;
        this.upstreamBaseUrl = upstreamBaseUrl;
        this.credentialId = credentialId;
        this.enabled = enabled;
        this.allowPrivateNetwork = allowPrivateNetwork;
    }
}
