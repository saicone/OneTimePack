package com.saicone.onetimepack;

import com.saicone.onetimepack.core.Processor;
import com.saicone.onetimepack.module.TinySettings;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class OneTimePack {

    private static OneTimePack instance;
    public static TinySettings SETTINGS = new TinySettings("settings.yml");
    private static int logLevel = 2;

    private final Provider provider;
    private final Processor<?, ?> processor;

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

    public OneTimePack(@NotNull Provider provider, @NotNull Processor<?, ?> processor) {
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

    @NotNull
    public Processor<?, ?> getPacketHandler() {
        return processor;
    }

    public interface Provider {

        @NotNull
        File getPluginFolder();

        void log(int level, @NotNull String s);
    }
}
