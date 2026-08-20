package com.botts.impl.service.oscar.federation.registry;

import java.util.Collection;
import java.util.Optional;
import com.botts.impl.service.oscar.federation.model.RemoteNodeConnection;

public interface RemoteNodeRegistry
{
    Optional<RemoteNodeConnection> findByUid(String uid);
    Collection<RemoteNodeConnection> list();
    void add(RemoteNodeConnection connection);
    void replace(RemoteNodeConnection connection);
    void remove(String uid);
}
