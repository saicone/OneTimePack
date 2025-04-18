package com.saicone.onetimepack;

import com.saicone.onetimepack.core.PacketUser;
import com.saicone.onetimepack.core.Processor;
import com.saicone.onetimepack.module.TinySettings;
import com.saicone.onetimepack.module.TinyYaml;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.UUID;
import java.util.function.Supplier;

public class OneTimePack {

    private static OneTimePack instance;
    public static TinySettings SETTINGS = new TinyYaml("settings.yml");
    private static int logLevel = 2;

    private final Provider provider;
    private final Processor<?, ?, ?> processor;

    @NotNull
    public static OneTimePack get() {
        return instance;
    }

    public static void log(int level, @NotNull String s) {
        if (logLevel >= level) {
            get().getProvider().log(level, s);
        }
    }

    public static void log(int level, @NotNull Supplier<String> msg) {
        if (logLevel >= level) {
            get().getProvider().log(level, msg.get());
        }
    }

    public static void log(int level, @NotNull Throwable throwable) {
        if (logLevel >= level) {
            throwable.printStackTrace();
        }
    }

    public static void log(int level, @NotNull Throwable throwable, @NotNull String s) {
        if (logLevel >= level) {
            get().getProvider().log(level, s);
            throwable.printStackTrace();
        }
    }

    public OneTimePack(@NotNull Provider provider, @NotNull Processor<?, ?, ?> processor) {
        if (instance != null) {
            throw new RuntimeException(OneTimePack.class.getSimpleName() + " is already initialized");
        }
        instance = this;
        this.provider = provider;
        this.processor = processor;
    }

    public void onLoad() {
        SETTINGS.load(provider.getPluginFolder());
        logLevel = SETTINGS.getInt("plugin.log-level", 2);
        processor.load();
    }

    public void onReload() {
        SETTINGS.load(provider.getPluginFolder());
        logLevel = SETTINGS.getInt("plugin.log-level", 2);
        processor.reload();
    }

    public void onEnable() {
        processor.enable();
    }

    public void onDisable() {
        processor.disable();
    }

    @NotNull
    public Provider getProvider() {
        return provider;
    }

    public @NotNull Processor<?, ?, ?> getPacketHandler() {
        return processor;
    }

    public interface Provider {

        @NotNull
        <PackT> PacketUser<PackT> getUser(@NotNull UUID uniqueId);

        @NotNull
        File getPluginFolder();

        void log(int level, @NotNull String s);
    }
}
