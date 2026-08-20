package com.botts.impl.service.oscar.federation.discovery;

public final class DiscoveredOscarNode
{
    private final String oscarSystemUid;

    public DiscoveredOscarNode(String oscarSystemUid)
    {
        this.oscarSystemUid = oscarSystemUid;
    }

    public String getOscarSystemUid()
    {
        return oscarSystemUid;
    }
}
