package com.saicone.onetimepack.util;

import com.google.common.base.Suppliers;
import com.saicone.onetimepack.ProxyResourcePack;
import dev.simplix.protocolize.api.util.ProtocolVersions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ProtocolVersion {

    private static final Supplier<Map<String, Integer>> protocols = Suppliers.memoize(() -> {
        final Map<String, Integer> map = new HashMap<>();
        for (Field field : ProtocolVersions.class.getDeclaredFields()) {
            final String name = field.getName().toUpperCase();
            if (name.startsWith("MINECRAFT_")) {
                try {
                    map.put(name.substring(10).replace('_', '.'), (int) field.get(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        map.put("latest", ProtocolVersions.MINECRAFT_LATEST);
        map.put("proxy", ProxyResourcePack.get().getProvider().getProxyProtocol());
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
            final String s = ((String) object).trim();
            if (s.indexOf('.') > 0) {
                return getProtocol(s);
            }
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return getProtocol(s);
            }
        }
        return -1;
    }

    public static int getProtocol(@NotNull String s) {
        return getProtocols().getOrDefault(s, -1);
    }

    @NotNull
    public static Map<String, Integer> getProtocols() {
        return protocols.get();
    }
}
