package com.saicone.onetimepack;

import com.saicone.onetimepack.core.Processor;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class VelocityPlugin implements OneTimePack.Provider {

    private static VelocityPlugin instance;

    public static VelocityPlugin get() {
        return instance;
    }

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    public VelocityPlugin(ProxyServer server, Logger logger, Path dataDirectory) {
        instance = this;
        this.proxy = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        new OneTimePack(this, initProcessor());
    }

    @NotNull
    protected Processor<?, ?> initProcessor() {
        throw new RuntimeException("Velocity plugin not implemented");
    }

    public void onEnable(ProxyInitializeEvent event) {
        OneTimePack.get().onLoad();
        OneTimePack.get().onEnable();
        getProxy().getCommandManager().register(getProxy().getCommandManager().metaBuilder("onetimepack").plugin(this).build(), new VelocityCommand());
    }

    public void onDisable(ProxyShutdownEvent event) {
        OneTimePack.get().onDisable();
    }

    public void onDisconnect(DisconnectEvent event) {
        OneTimePack.get().getPacketHandler().clear(event.getPlayer().getUniqueId());
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

    @NotNull
    public static TextComponent parseComponent(@NotNull String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    public static class VelocityCommand implements SimpleCommand {

        private static final String VERSION = "${version}";
        private static final List<String> SUGGESTIONS = List.of("reload");

        @Override
        public void execute(Invocation invocation) {
            final CommandSource source = invocation.source();
            if (invocation.arguments().length >= 1 && invocation.arguments()[0].equalsIgnoreCase("reload")) {
                final long start = System.currentTimeMillis();
                OneTimePack.get().onReload();
                final long time = System.currentTimeMillis() - start;
                source.sendMessage(parseComponent("&aPlugin successfully reloaded [&f" + time + " ms&a]"));
                return;
            }
            source.sendMessage(parseComponent("&a&lOneTimePack &e&lv" + VERSION));
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("onetimepack.use");
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            return SUGGESTIONS;
        }
    }
}
