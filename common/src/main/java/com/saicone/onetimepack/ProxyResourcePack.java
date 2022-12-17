package com.saicone.onetimepack;

import com.saicone.onetimepack.core.PacketHandler;
import com.saicone.onetimepack.module.TinySettings;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ProxyResourcePack {

    private static ProxyResourcePack instance;
    public static TinySettings SETTINGS = new TinySettings("settings.yml");
    private static int logLevel = 2;

    private final Provider provider;
    private final PacketHandler packetHandler;

    @NotNull
    public static ProxyResourcePack get() {
        return instance;
    }

    public static int getLogLevel() {
        return logLevel;
    }

    public static void log(int level, @NotNull String s) {
        if (logLevel >= level) {
            get().getProvider().log(level, s);
        }
    }

    public ProxyResourcePack(@NotNull Provider provider) {
        if (instance != null) {
            throw new RuntimeException(ProxyResourcePack.class.getSimpleName() + " is already initialized");
        }
        instance = this;
        this.provider = provider;
        this.packetHandler = new PacketHandler(this);
    }

    public void onLoad() {
        SETTINGS.load(provider.getPluginFolder());
        logLevel = SETTINGS.getInt("Plugin.LogLevel", 2);
        packetHandler.onLoad();
    }

    public void onEnable() {
        packetHandler.onEnable();
    }

    public void onDisable() {
        packetHandler.onDisable();
    }

    @NotNull
    public Provider getProvider() {
        return provider;
    }

    @NotNull
    public PacketHandler getPacketHandler() {
        return packetHandler;
    }

    public interface Provider {

        @NotNull
        File getPluginFolder();

        int getProxyProtocol();

        void log(int level, @NotNull String s);

        default void run(@NotNull Runnable runnable) {
            run(runnable, false);
        }

        void run(@NotNull Runnable runnable, boolean async);
    }
}
