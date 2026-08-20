package com.botts.impl.service.oscar.federation.model;

public final class FederationNodeView
{
    private final String uid;
    private final boolean local;
    private final boolean enabled;
    private final String upstreamBaseUrl;

    public FederationNodeView(String uid, boolean local, boolean enabled, String upstreamBaseUrl)
    {
        this.uid = uid;
        this.local = local;
        this.enabled = enabled;
        this.upstreamBaseUrl = upstreamBaseUrl;
    }

    public String getUid() { return uid; }
    public boolean isLocal() { return local; }
    public boolean isEnabled() { return enabled; }
    public String getUpstreamBaseUrl() { return upstreamBaseUrl; }
}
