package com.saicone.onetimepack;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Level;

public class BungeePlugin extends Plugin implements Listener, ProxyResourcePack.Provider {

    private static BungeePlugin instance;

    public static BungeePlugin get() {
        return instance;
    }

    public BungeePlugin() {
        new ProxyResourcePack(this);
        instance = this;
    }

    @Override
    public void onLoad() {
        ProxyResourcePack.get().onLoad();
    }

    @Override
    public void onEnable() {
        ProxyResourcePack.get().onEnable();
        this.getProxy().getPluginManager().registerListener(this, this);
    }

    @Override
    public void onDisable() {
        ProxyResourcePack.get().onDisable();
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        ProxyResourcePack.get().getPacketHandler().clear(event.getPlayer().getUniqueId());
    }

    @Override
    public @NotNull File getPluginFolder() {
        return getDataFolder();
    }

    @Override
    public int getProxyProtocol() {
        return ProtocolConstants.SUPPORTED_VERSION_IDS.get(ProtocolConstants.SUPPORTED_VERSION_IDS.size() - 1);
    }

    @Override
    public void log(int level, @NotNull String s) {
        switch (level) {
            case 1:
                getLogger().log(Level.SEVERE, s);
                break;
            case 2:
                getLogger().log(Level.WARNING, s);
                break;
            default:
                getLogger().log(Level.INFO, s);
                break;
        }
    }

    @Override
    public void run(@NotNull Runnable runnable, boolean async) {
        if (async) {
            getProxy().getScheduler().runAsync(this, runnable);
        } else {
            runnable.run();
        }
    }
}