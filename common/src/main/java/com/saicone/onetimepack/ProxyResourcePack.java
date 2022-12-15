package com.saicone.onetimepack;

import com.saicone.onetimepack.core.PacketHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ProxyResourcePack {

    private static ProxyResourcePack instance;

    private final Provider provider;
    private final PacketHandler packetHandler;

    @NotNull
    public static ProxyResourcePack get() {
        return instance;
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
