package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.module.TinySettings;
import com.saicone.onetimepack.util.ValueComparator;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServerGroup<PackT> {

    private final String id;
    private final Set<String> servers;
    private final Map<ProtocolState, ProtocolOptions<PackT>> protocols;

    @NotNull
    public static <T> ServerGroup<T> valueOf(@NotNull String id, @NotNull ValueComparator.Provider<T> provider) {
        final TinySettings config = OneTimePack.SETTINGS;
        return new ServerGroup<>(
                id,
                new HashSet<>(config.getStringList("group." + id + ".servers")),
                Map.of(
                        ProtocolState.PLAY, ProtocolOptions.valueOf(id, ProtocolState.PLAY, provider),
                        ProtocolState.CONFIGURATION, ProtocolOptions.valueOf(id, ProtocolState.CONFIGURATION, provider)
                )
        );
    }

    public ServerGroup(@NotNull String id, @NotNull Set<String> servers, @NotNull Map<ProtocolState, ProtocolOptions<PackT>> protocols) {
        this.id = id;
        this.servers = servers;
        this.protocols = protocols;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public Set<String> getServers() {
        return servers;
    }

    @NotNull
    public ProtocolOptions<PackT> getOptions(@NotNull ProtocolState state) {
        return protocols.get(state);
    }
}
