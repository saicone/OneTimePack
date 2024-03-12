package com.saicone.onetimepack;

import com.saicone.onetimepack.core.PacketHandler;
import com.saicone.onetimepack.module.TinySettings;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class OneTimePack {

    private static OneTimePack instance;
    public static TinySettings SETTINGS = new TinySettings("settings.yml");
    private static int logLevel = 2;

    private final Provider provider;
    private final PacketHandler packetHandler;

    @NotNull
    public static OneTimePack get() {
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

    public OneTimePack(@NotNull Provider provider) {
        if (instance != null) {
            throw new RuntimeException(OneTimePack.class.getSimpleName() + " is already initialized");
        }
        instance = this;
        this.provider = provider;
        this.packetHandler = new PacketHandler();
    }

    public void onLoad() {
        SETTINGS.load(provider.getPluginFolder());
        logLevel = SETTINGS.getInt("plugin.log-level", 2);
    }

    public void onReload() {
        SETTINGS.load(provider.getPluginFolder());
        logLevel = SETTINGS.getInt("plugin.log-level", 2);
        packetHandler.onReload();
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

        void log(int level, @NotNull String s);
    }
}
