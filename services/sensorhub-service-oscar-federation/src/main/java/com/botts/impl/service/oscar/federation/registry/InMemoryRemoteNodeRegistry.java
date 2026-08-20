package com.botts.impl.service.oscar.federation.registry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.botts.impl.service.oscar.federation.model.RemoteNodeConnection;

public class InMemoryRemoteNodeRegistry implements RemoteNodeRegistry
{
    private final ConcurrentHashMap<String, RemoteNodeConnection> nodes = new ConcurrentHashMap<>();

    @Override
    public Optional<RemoteNodeConnection> findByUid(String uid)
    {
        return Optional.ofNullable(nodes.get(uid));
    }

    @Override
    public Collection<RemoteNodeConnection> list()
    {
        return List.copyOf(nodes.values());
    }

    @Override
    public void add(RemoteNodeConnection connection)
    {
        if (nodes.putIfAbsent(connection.getOscarSystemUid(), connection) != null)
            throw new IllegalArgumentException("OSCAR system UID is already registered");
    }

    @Override
    public void replace(RemoteNodeConnection connection)
    {
        nodes.put(connection.getOscarSystemUid(), connection);
    }

    @Override
    public void remove(String uid)
    {
        nodes.remove(uid);
    }
}
