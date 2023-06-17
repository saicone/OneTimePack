package com.saicone.onetimepack;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Plugin(id = "onetimepack", name = "OneTimePack", description = "Send the same resource pack only one time", version = "${version}", authors = "Rubenicos", dependencies = {@Dependency(id = "protocolize")})
public class VelocityPlugin implements OneTimePack.Provider {

    private static VelocityPlugin instance;

    public static VelocityPlugin get() {
        return instance;
    }

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        new OneTimePack(this);
        instance = this;
        this.proxy = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        OneTimePack.get().onLoad();
        OneTimePack.get().onEnable();
        getProxy().getCommandManager().register(getProxy().getCommandManager().metaBuilder("onetimepack").plugin(this).build(), new VelocityCommand());
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        OneTimePack.get().onDisable();
    }

    @Subscribe
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
