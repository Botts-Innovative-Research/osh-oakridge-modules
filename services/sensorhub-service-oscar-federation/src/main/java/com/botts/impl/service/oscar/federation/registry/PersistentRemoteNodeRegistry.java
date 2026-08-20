package com.botts.impl.service.oscar.federation.registry;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.botts.impl.service.oscar.federation.model.RemoteNodeConfig;
import com.botts.impl.service.oscar.federation.model.RemoteNodeConnection;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PersistentRemoteNodeRegistry implements RemoteNodeRegistry
{
    private static final Gson GSON = new Gson();
    private final ConcurrentHashMap<String, RemoteNodeConnection> nodes = new ConcurrentHashMap<>();
    private final Path stateFile;

    public PersistentRemoteNodeRegistry(Path stateFile, Collection<RemoteNodeConfig> initialNodes) throws IOException
    {
        this.stateFile = stateFile;
        if (Files.exists(stateFile))
            load();
        else if (initialNodes != null)
        {
            for (RemoteNodeConfig node : initialNodes)
                addWithoutPersisting(fromConfig(node));
            persist();
        }
    }

    @Override public Optional<RemoteNodeConnection> findByUid(String uid) { return Optional.ofNullable(nodes.get(uid)); }
    @Override public Collection<RemoteNodeConnection> list() { return List.copyOf(nodes.values()); }

    @Override
    public synchronized void add(RemoteNodeConnection connection)
    {
        addWithoutPersisting(connection);
        try
        {
            persist();
        }
        catch (RuntimeException e)
        {
            nodes.remove(connection.getOscarSystemUid(), connection);
            throw e;
        }
    }

    @Override
    public synchronized void replace(RemoteNodeConnection connection)
    {
        if (!nodes.containsKey(connection.getOscarSystemUid()))
            throw new IllegalArgumentException("OSCAR system UID is not registered");
        RemoteNodeConnection previous = nodes.put(connection.getOscarSystemUid(), connection);
        try
        {
            persist();
        }
        catch (RuntimeException e)
        {
            nodes.put(connection.getOscarSystemUid(), previous);
            throw e;
        }
    }

    @Override
    public synchronized void remove(String uid)
    {
        RemoteNodeConnection removed = nodes.remove(uid);
        if (removed != null)
        {
            try
            {
                persist();
            }
            catch (RuntimeException e)
            {
                nodes.put(uid, removed);
                throw e;
            }
        }
    }

    private void addWithoutPersisting(RemoteNodeConnection connection)
    {
        if (nodes.putIfAbsent(connection.getOscarSystemUid(), connection) != null)
            throw new IllegalArgumentException("OSCAR system UID is already registered");
    }

    private void load() throws IOException
    {
        List<RemoteNodeConfig> configs = GSON.fromJson(Files.readString(stateFile, StandardCharsets.UTF_8),
            new TypeToken<List<RemoteNodeConfig>>(){}.getType());
        if (configs != null)
            for (RemoteNodeConfig config : configs)
                addWithoutPersisting(fromConfig(config));
    }

    private synchronized void persist()
    {
        try
        {
            Path parent = stateFile.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            List<RemoteNodeConfig> configs = nodes.values().stream()
                .map(PersistentRemoteNodeRegistry::toConfig)
                .sorted(java.util.Comparator.comparing(config -> config.oscarSystemUid))
                .toList();
            Path temp = Files.createTempFile(parent, "federation-nodes-", ".tmp");
            Files.writeString(temp, GSON.toJson(configs), StandardCharsets.UTF_8);
            try
            {
                Files.move(temp, stateFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException e)
            {
                Files.move(temp, stateFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot persist federation registry", e);
        }
    }

    private static RemoteNodeConnection fromConfig(RemoteNodeConfig config)
    {
        return new RemoteNodeConnection(config.oscarSystemUid, URI.create(config.upstreamBaseUrl),
            config.credentialId, config.enabled, config.allowPrivateNetwork, null, null);
    }

    private static RemoteNodeConfig toConfig(RemoteNodeConnection node)
    {
        return new RemoteNodeConfig(node.getOscarSystemUid(), node.getUpstreamBaseUri().toString(),
            node.getCredentialId(), node.isEnabled(), node.isPrivateNetworkAllowed());
    }
}
