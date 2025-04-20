package com.saicone.onetimepack;

import com.saicone.onetimepack.core.BungeeProcessor;
import com.saicone.onetimepack.core.Processor;
import org.jetbrains.annotations.NotNull;

public class BungeeBootstrap extends BungeePlugin {
    @Override
    protected @NotNull Processor<?, ?, ?> initProcessor() {
        return new BungeeProcessor();
    }
}
