package com.saicone.onetimepack.util;

import com.google.common.base.Suppliers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ProtocolVersion {

    private static final List<Class<?>> MAGIC_CLASSES = new ArrayList<>();

    static {
        try {
            MAGIC_CLASSES.add(Class.forName("dev.simplix.protocolize.api.util.ProtocolVersions"));
        } catch (Throwable ignored) { }
        try {
            MAGIC_CLASSES.add(Class.forName("net.md_5.bungee.protocol.ProtocolConstants"));
        } catch (Throwable ignored) { }
    }

    private static final Supplier<Map<String, Integer>> protocols = Suppliers.memoize(() -> {
        final Map<String, Integer> map = new HashMap<>();
        for (Class<?> clazz : MAGIC_CLASSES) {
            for (Field field : clazz.getDeclaredFields()) {
                final String name = field.getName().toUpperCase();
                if (name.startsWith("MINECRAFT_")) {
                    try {
                        map.put(name.substring(10).replace('_', '.').toLowerCase(), (int) field.get(null));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return map;
    });

    public static int getProtocol(@Nullable Object object) {
        if (object instanceof Integer) {
            return (int) object;
        } else if (object instanceof Number) {
            try {
                return Integer.parseInt(String.valueOf(object));
            } catch (NumberFormatException ignored) { }
        } else if (object instanceof String) {
            final String s = ((String) object).trim().toLowerCase();
            if (s.indexOf('.') > 0) {
                return getProtocol(s, -1);
            }
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return getProtocol(s, -1);
            }
        }
        return -1;
    }

    public static int getProtocol(@NotNull String s, int def) {
        return getProtocols().getOrDefault(s, def);
    }

    @NotNull
    public static Map<String, Integer> getProtocols() {
        return protocols.get();
    }
}