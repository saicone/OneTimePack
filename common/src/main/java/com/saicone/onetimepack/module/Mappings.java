package com.saicone.onetimepack.module;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.saicone.onetimepack.ProxyResourcePack;
import com.saicone.onetimepack.util.FileUtils;
import com.saicone.onetimepack.util.ProtocolVersion;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mappings {

    private final File folder;
    private final String fileName;

    public Mappings(@NotNull File folder, @NotNull String fileName) {
        this.folder = folder;
        this.fileName = fileName;
    }

    @NotNull
    public File getFolder() {
        return folder;
    }

    @NotNull
    public String getFileName() {
        return fileName;
    }

    @Nullable
    public Map<String, List<ProtocolIdMapping>> load() {
        // Load mappings file
        final JsonObject jsonFile = loadPluginFile();
        if (jsonFile == null) {
            log(1, "Build-in mappings will be used by default");
            return null;
        }

        // Check external
        final JsonObject external = jsonFile.getAsJsonObject("external");
        if (external == null || !external.get("enabled").getAsBoolean()) {
            log(3, "Mappings from " + fileName + " file will be used");
            return load(jsonFile);
        }

        // Load mappings from url
        final JsonObject jsonUrl = loadUrlFile(external.get("url").getAsString());
        if (jsonUrl == null) {
            log(1, "Mappings from " + fileName + " file will be used instead");
            return load(jsonFile);
        }
        log(3, "Mappings from url will be used");
        return load(jsonUrl);
    }

    @Nullable
    private Map<String, List<ProtocolIdMapping>> load(@NotNull JsonObject json) {
        final JsonObject packets = json.getAsJsonObject("packet");
        if (packets == null) {
            log(1, "The provided json file doesn't contains 'packet' configuration");
            return null;
        }
        final Map<String, List<ProtocolIdMapping>> mappings = new HashMap<>();
        for (String name : packets.keySet()) {
            final JsonObject packet = packets.getAsJsonObject(name);
            final List<ProtocolIdMapping> list = new ArrayList<>();
            for (String s : packet.keySet()) {
                for (String ver : s.split("\\|")) {
                    String[] version = ver.split("-");
                    int start = ProtocolVersion.getProtocol(version.length >= 1 ? version[0].trim() : s);
                    int end = version.length >= 2 ? ProtocolVersion.getProtocol(version[1].trim()) : start;
                    if (start < 0 || end < 0) {
                        log(1, "The parameter '" + ver + "' inside '" + s + "' is not a valid version range for " + name + " packet, so will be ignored");
                        continue;
                    }

                    int id = packet.get(s).getAsInt();
                    list.add(AbstractProtocolMapping.rangedIdMapping(start, end, id));
                }
            }
            if (list.isEmpty()) {
                log(2, "The packet '" + name + "' has empty mappings");
            } else {
                log(3, "Loaded " + list.size() + " mappings for " + name + " packet");
            }
            mappings.put(name, list);
        }
        if (mappings.isEmpty()) {
            log(2, "The provided json file doesn't have any mapping");
        }
        return mappings;
    }

    @Nullable
    private JsonObject loadPluginFile() {
        final File file = FileUtils.saveResource(folder, fileName, false);
        if (file != null) {
            final String lines = FileUtils.readFromFile(file);
            if (lines != null) {
                if (!lines.trim().isEmpty()) {
                    return JsonParser.parseString(lines).getAsJsonObject();
                } else {
                    log(1, "The file " + fileName + " is empty");
                }
            } else {
                log(1, "Cannot read " + fileName + " file");
            }
        } else {
            log(1, "Cannot load " + fileName + " file from plugin JAR");
        }
        return null;
    }

    @Nullable
    private JsonObject loadUrlFile(@Nullable String url) {
        if (url != null) {
            if (!url.trim().isEmpty()) {
                final String lines = FileUtils.readFromUrl(url);
                if (lines != null) {
                    if (!lines.trim().isEmpty()) {
                        return JsonParser.parseString(lines).getAsJsonObject();
                    } else {
                        log(1, "The url data is empty");
                    }
                } else {
                    log(1, "Cannot retrieve data from mappings url");
                }
            } else {
                log(1, "The provided URL cannot be empty");
            }
        } else {
            log(1, "The file " + fileName + " doesn't have any configured URL");
        }
        return null;
    }

    private static void log(int level, @NotNull String s) {
        ProxyResourcePack.get().getProvider().log(level, s);
    }
}
