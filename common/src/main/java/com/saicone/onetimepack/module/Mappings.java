package com.saicone.onetimepack.module;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.saicone.onetimepack.OneTimePack;
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

    private Map<String, Map<String, List<ProtocolIdMapping>>> loaded = new HashMap<>();

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
    public List<ProtocolIdMapping> getMappings(@NotNull String name, @NotNull String protocol) {
        if (loaded.containsKey(name)) {
            return loaded.get(name).get(protocol);
        } else {
            return null;
        }
    }

    public boolean contains(@NotNull String name) {
        return loaded.containsKey(name);
    }

    public void load() {
        // Load mappings file
        final JsonObject jsonFile = loadPluginFile();
        if (jsonFile == null) {
            OneTimePack.log(1, "Build-in mappings will be used by default");
            loaded = new HashMap<>();
            return;
        }

        // Check external
        final JsonObject external = jsonFile.getAsJsonObject("external");
        if (external == null || !external.get("enabled").getAsBoolean()) {
            OneTimePack.log(3, "Mappings from " + fileName + " file will be used");
            loaded = load(jsonFile);
            return;
        }

        // Load mappings from url
        final JsonObject jsonUrl = loadUrlFile(external.get("url").getAsString());
        if (jsonUrl == null) {
            OneTimePack.log(1, "Mappings from " + fileName + " file will be used instead");
            loaded = load(jsonFile);
            return;
        }
        OneTimePack.log(3, "Mappings from url will be used");
        loaded = load(jsonUrl);
    }

    @NotNull
    private Map<String, Map<String, List<ProtocolIdMapping>>> load(@NotNull JsonObject json) {
        final Map<String, Map<String, List<ProtocolIdMapping>>> loaded = new HashMap<>();
        for (String s : json.keySet()) {
            final String key = s.trim().toLowerCase();
            if (!key.startsWith("packet")) {
                continue;
            }
            final Map<String, List<ProtocolIdMapping>> mappings = loadMappings(json.getAsJsonObject(s));
            if (mappings == null || mappings.isEmpty()) {
                OneTimePack.log(1, "The provided json file doesn't contains mappings on '" + s + "' configuration");
                continue;
            }
            final String[] split = key.split("-", 2);
            final String protocol = split.length > 1 ? split[1] : "play";
            for (Map.Entry<String, List<ProtocolIdMapping>> entry : mappings.entrySet()) {
                loaded.computeIfAbsent(entry.getKey(), __ -> new HashMap<>()).put(protocol, entry.getValue());
            }
        }
        return loaded;
    }

    @Nullable
    private Map<String, List<ProtocolIdMapping>> loadMappings(@Nullable JsonObject packets) {
        if (packets == null) {
            return null;
        }
        final Map<String, List<ProtocolIdMapping>> mappings = new HashMap<>();
        for (String name : packets.keySet()) {
            final JsonObject packet = packets.getAsJsonObject(name);
            final List<ProtocolIdMapping> list = new ArrayList<>();
            for (String s : packet.keySet()) {
                for (String ver : s.split("\\|")) {
                    String[] version = ver.split("-");
                    int start = ProtocolVersion.getProtocol(version.length >= 1 ? version[0] : s);
                    int end = version.length >= 2 ? ProtocolVersion.getProtocol(version[1]) : start;
                    if (start < 0 || end < 0) {
                        OneTimePack.log(1, "The parameter '" + ver + "' inside '" + s + "' is not a valid version range for " + name + " packet, so will be ignored");
                        continue;
                    }

                    int id = packet.get(s).getAsInt();
                    list.add(AbstractProtocolMapping.rangedIdMapping(start, end, id));
                    OneTimePack.log(3, "Added ranged mapping for " + name + ": " + start + ',' + end + ',' + id);
                }
            }
            if (list.isEmpty()) {
                OneTimePack.log(2, "The packet '" + name + "' has empty mappings");
            } else {
                OneTimePack.log(3, "Loaded " + list.size() + " mappings for " + name + " packet");
            }
            mappings.put(name, list);
        }
        if (mappings.isEmpty()) {
            OneTimePack.log(2, "The provided json file doesn't have any mapping");
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
                    OneTimePack.log(1, "The file " + fileName + " is empty");
                }
            } else {
                OneTimePack.log(1, "Cannot read " + fileName + " file");
            }
        } else {
            OneTimePack.log(1, "Cannot load " + fileName + " file from plugin JAR");
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
                        OneTimePack.log(1, "The url data is empty");
                    }
                } else {
                    OneTimePack.log(1, "Cannot retrieve data from mappings url");
                }
            } else {
                OneTimePack.log(1, "The provided URL cannot be empty");
            }
        } else {
            OneTimePack.log(1, "The file " + fileName + " doesn't have any configured URL");
        }
        return null;
    }
}
