package com.botts.impl.service.oscar.federation.discovery;

import com.botts.impl.service.oscar.federation.model.NodeRegistrationRequest;

public interface OscarNodeDiscovery
{
    DiscoveredOscarNode discover(NodeRegistrationRequest request);
    void verify(String expectedUid, NodeRegistrationRequest request);
}
