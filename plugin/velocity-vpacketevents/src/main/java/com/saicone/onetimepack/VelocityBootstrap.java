package com.saicone.onetimepack;

import com.google.inject.Inject;
import com.saicone.onetimepack.core.Processor;
import com.saicone.onetimepack.core.VPacketEventsProcessor;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "onetimepack",
        name = "OneTimePack",
        description = "Send the same resource pack only one time",
        version = "${version}",
        authors = "Rubenicos",
        dependencies = {@Dependency(id = "vpacketevents")}
)
public class VelocityBootstrap extends VelocityPlugin {

    @Inject
    public VelocityBootstrap(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        super(server, logger, dataDirectory);
    }

    @Override
    protected @NotNull Processor<?, ?, ?> initProcessor() {
        return new VPacketEventsProcessor(getProxy(), this);
    }

    @Subscribe
    @Override
    public void onEnable(ProxyInitializeEvent event) {
        super.onEnable(event);
    }

    @Subscribe
    @Override
    public void onDisable(ProxyShutdownEvent event) {
        super.onDisable(event);
    }

    @Subscribe
    @Override
    public void onDisconnect(DisconnectEvent event) {
        super.onDisconnect(event);
    }
}
