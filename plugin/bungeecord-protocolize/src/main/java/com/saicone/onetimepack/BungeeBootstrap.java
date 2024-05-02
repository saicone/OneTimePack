package com.saicone.onetimepack;

import com.saicone.onetimepack.core.MappedProtocolizeProcessor;
import com.saicone.onetimepack.core.Processor;
import net.md_5.bungee.protocol.packet.StartConfiguration;
import org.jetbrains.annotations.NotNull;

public class BungeeBootstrap extends BungeePlugin {
    @Override
    protected @NotNull Processor<?, ?, ?> initProcessor() {
        return new MappedProtocolizeProcessor<>(StartConfiguration.class);
    }
}
