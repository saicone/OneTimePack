package com.saicone.onetimepack;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Plugin(id = "onetimepack", name = "OneTimePack", description = "Send the same resource pack only one time", version = "${version}", authors = "Rubenicos", dependencies = {@Dependency(id = "protocolize")})
public class VelocityPlugin implements ProxyResourcePack.Provider {

    private static VelocityPlugin instance;

    public static VelocityPlugin get() {
        return instance;
    }

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        new ProxyResourcePack(this);
        instance = this;
        this.proxy = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        ProxyResourcePack.get().onLoad();
        ProxyResourcePack.get().onEnable();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        ProxyResourcePack.get().onDisable();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        ProxyResourcePack.get().getPacketHandler().clear(event.getPlayer().getUniqueId());
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public @NotNull File getPluginFolder() {
        return dataDirectory.toFile();
    }

    @Override
    public int getProxyProtocol() {
        return ProtocolVersion.MAXIMUM_VERSION.getProtocol();
    }

    @Override
    public void log(int level, @NotNull String s) {
        switch (level) {
            case 1:
                getLogger().error(s);
                break;
            case 2:
                getLogger().warn(s);
                break;
            default:
                getLogger().info(s);
                break;
        }
    }

    @Override
    public void run(@NotNull Runnable runnable, boolean async) {
        getProxy().getScheduler().buildTask(this, runnable).schedule();
    }
}
