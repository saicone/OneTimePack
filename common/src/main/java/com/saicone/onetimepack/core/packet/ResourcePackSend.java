package com.saicone.onetimepack.core.packet;

import com.saicone.onetimepack.OneTimePack;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.util.ProtocolUtil;
import dev.simplix.protocolize.data.util.NamedBinaryTagUtil;
import io.netty.buffer.ByteBuf;
import net.querz.nbt.tag.Tag;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

public class ResourcePackSend extends AbstractPacket {

    public static final List<ProtocolIdMapping> DEFAULT_MAPPINGS = Arrays.asList(
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_8, MINECRAFT_1_8, 0x48),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_9, MINECRAFT_1_11_2, 0x32),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_12, MINECRAFT_1_12, 0x33),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_12_1, MINECRAFT_1_12_2, 0x34),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_13, MINECRAFT_1_13_2, 0x37),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_14, MINECRAFT_1_14_4, 0x39),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x3A),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x39),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16_2, MINECRAFT_1_16_5, 0x38),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x3C),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19, MINECRAFT_1_19, 0x3A),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_1, MINECRAFT_1_19_2, 0x3D),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x3C),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_4, MINECRAFT_1_20_1, 0x40),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x42),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x44)
    );

    public static final List<ProtocolIdMapping> DEFAULT_MAPPINGS_CONFIGURATION = Arrays.asList(
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_1, MINECRAFT_1_20_2, 0x06),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x07)
    );

    public static void register() {
        register(Protocol.PLAY, DEFAULT_MAPPINGS, ResourcePackSend.Play.class);
        register(Protocol.CONFIGURATION, DEFAULT_MAPPINGS_CONFIGURATION, ResourcePackSend.Configuration.class);
    }

    public static void register(Function<String, List<ProtocolIdMapping>> provider) {
        if (provider == null) {
            register();
            return;
        }
        final List<ProtocolIdMapping> play = provider.apply("play");
        register(Protocol.PLAY, play == null ? DEFAULT_MAPPINGS : play, ResourcePackSend.Play.class);
        final List<ProtocolIdMapping> configuration = provider.apply("configuration");
        register(Protocol.CONFIGURATION, configuration == null ? DEFAULT_MAPPINGS_CONFIGURATION : configuration, ResourcePackSend.Configuration.class);
    }

    public static void register(Protocol protocol, List<ProtocolIdMapping> mappings, Class<? extends ResourcePackSend> clazz) {
        if (mappings.isEmpty()) {
            return;
        }
        Protocolize.protocolRegistration().registerPacket(
                mappings,
                protocol,
                PacketDirection.CLIENTBOUND,
                clazz
        );
    }

    public static BiPredicate<ResourcePackSend, ResourcePackSend> comparator(String name) {
        final boolean nonNull = name.charAt(0) == '!';
        switch ((nonNull ? name.substring(1) : name).toUpperCase()) {
            case "UUID":
                if (nonNull) {
                    return (pack1, pack2) -> pack1.getUniqueId() != null && pack2.getUniqueId() != null && pack1.getUniqueId().equals(pack2.getUniqueId());
                }
                return (pack1, pack2) -> Objects.equals(pack1.getUniqueId(), pack2.getUniqueId());
            case "URL":
                if (nonNull) {
                    return (pack1, pack2) -> pack1.getUrl() != null && pack2.getUrl() != null && pack1.getUrl().equals(pack2.getUrl());
                }
                return (pack1, pack2) -> Objects.equals(pack1.getUrl(), pack2.getUrl());
            case "HASH":
                if (nonNull) {
                    return (pack1, pack2) -> pack1.getHash() != null && pack2.getHash() != null && pack1.getHash().equals(pack2.getHash());
                }
                return (pack1, pack2) -> Objects.equals(pack1.getHash(), pack2.getHash());
            case "PROMPT":
                if (nonNull) {
                    return (pack1, pack2) -> pack1.getPrompt() != null && pack2.getPrompt() != null && pack1.getPrompt().equals(pack2.getPrompt());
                }
                return (pack1, pack2) -> Objects.equals(pack1.getPrompt(), pack2.getPrompt());
            case "ALL":
                return ResourcePackSend::equals;
            case "ANY":
                return (pack1, pack2) -> true;
            default:
                OneTimePack.log(2, "The pack comparator '" + name + "' is not valid");
                return null;
        }
    }

    private UUID uniqueId; // Added in 1.20.3
    private String url;
    private String hash;

    // Added in 1.17
    private boolean forced;
    private boolean hasPromptMessage;
    private String promptJson; // Removed in 1.20.3

    // Added in 1.20.3
    private Tag<?> promptTag;

    public ResourcePackSend() {
    }

    public ResourcePackSend(String url, String hash) {
        this.url = url;
        this.hash = hash;
    }

    public ResourcePackSend(UUID uniqueId, String url, String hash, boolean forced) {
        this.uniqueId = uniqueId;
        this.url = url;
        this.hash = hash;
        this.forced = forced;
    }

    public ResourcePackSend(String url, String hash, boolean forced, boolean hasPromptMessage, String promptJson) {
        this.url = url;
        this.hash = hash;
        this.forced = forced;
        this.hasPromptMessage = hasPromptMessage;
        this.promptJson = promptJson;
    }

    public ResourcePackSend(UUID uniqueId, String url, String hash, boolean forced, boolean hasPromptMessage, Tag<?> promptTag) {
        this.uniqueId = uniqueId;
        this.url = url;
        this.hash = hash;
        this.forced = forced;
        this.hasPromptMessage = hasPromptMessage;
        this.promptTag = promptTag;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getUrl() {
        return url;
    }

    public String getHash() {
        return hash;
    }

    public Object getPrompt() {
        return promptJson != null ? promptJson : promptTag;
    }

    public String getPromptJson() {
        return promptJson;
    }

    public Tag<?> getPromptTag() {
        return promptTag;
    }

    public Protocol getProtocol() {
        return this instanceof Play ? Protocol.PLAY : Protocol.CONFIGURATION;
    }

    public boolean isForced() {
        return forced;
    }

    public boolean hasPromptMessage() {
        return hasPromptMessage;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setForced(boolean forced) {
        this.forced = forced;
    }

    public void setHasPromptMessage(boolean hasPromptMessage) {
        this.hasPromptMessage = hasPromptMessage;
    }

    public void setPromptJson(String promptJson) {
        this.promptJson = promptJson;
        this.hasPromptMessage = promptJson != null;
    }

    public void setPromptTag(Tag<?> promptTag) {
        this.promptTag = promptTag;
        this.hasPromptMessage = promptTag != null;
    }

    @Override
    public void read(ByteBuf buf, PacketDirection direction, int protocol) {
        if (protocol >= MINECRAFT_1_20_3) {
            uniqueId = ProtocolUtil.readUniqueId(buf);
        }
        url = ProtocolUtil.readString(buf);
        hash = ProtocolUtil.readString(buf);
        if (protocol >= MINECRAFT_1_17) {
            forced = buf.readBoolean();
            hasPromptMessage = buf.readBoolean();
            if (hasPromptMessage) {
                if (protocol >= MINECRAFT_1_20_3) {
                    try {
                        promptTag = NamedBinaryTagUtil.readTag(buf, protocol);
                    } catch (IOException e) {
                        hasPromptMessage = false;
                        if (OneTimePack.getLogLevel() >= 2) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    promptJson = ProtocolUtil.readString(buf);
                }
            }
        } else {
            forced = false;
            hasPromptMessage = false;
        }
        if (OneTimePack.getLogLevel() >= 4) {
            OneTimePack.log(4, "[" + getProtocol().name() + "] Packet#read() = " + this);
        }
    }

    @Override
    public void write(ByteBuf buf, PacketDirection direction, int protocol) {
        if (protocol >= MINECRAFT_1_20_3) {
            ProtocolUtil.writeUniqueId(buf, uniqueId);
        }
        ProtocolUtil.writeString(buf, url);
        ProtocolUtil.writeString(buf, hash);
        if (protocol >= MINECRAFT_1_17) {
            buf.writeBoolean(forced);
            buf.writeBoolean(hasPromptMessage);
            if (hasPromptMessage) {
                if (protocol >= MINECRAFT_1_20_3) {
                    try {
                        NamedBinaryTagUtil.writeTag(buf, promptTag, protocol);
                    } catch (IOException e) {
                        if (OneTimePack.getLogLevel() >= 2) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    ProtocolUtil.writeString(buf, promptJson);
                }
            }
        }
    }

    public ResourcePackSend copy() {
        if (hasPromptMessage) {
            if (promptJson == null) {
                return new ResourcePackSend(uniqueId, url, hash, forced, true, promptTag);
            } else {
                return new ResourcePackSend(url, hash, forced, true, promptJson);
            }
        }
        return new ResourcePackSend(uniqueId, url, hash, forced);
    }

    public ResourcePackSend.Play asPlay() {
        if (this instanceof ResourcePackSend.Play) {
            return (ResourcePackSend.Play) this;
        }
        if (hasPromptMessage) {
            if (promptJson == null) {
                return new ResourcePackSend.Play(uniqueId, url, hash, forced, true, promptTag);
            } else {
                return new ResourcePackSend.Play(url, hash, forced, true, promptJson);
            }
        }
        return new ResourcePackSend.Play(uniqueId, url, hash, forced);
    }

    public ResourcePackSend.Configuration asConfiguration() {
        if (this instanceof ResourcePackSend.Configuration) {
            return (ResourcePackSend.Configuration) this;
        }
        if (hasPromptMessage) {
            if (promptJson == null) {
                return new ResourcePackSend.Configuration(uniqueId, url, hash, forced, true, promptTag);
            } else {
                return new ResourcePackSend.Configuration(url, hash, forced, true, promptJson);
            }
        }
        return new ResourcePackSend.Configuration(uniqueId, url, hash, forced);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourcePackSend that = (ResourcePackSend) o;

        if (forced != that.forced) return false;
        if (hasPromptMessage != that.hasPromptMessage) return false;
        if (!Objects.equals(uniqueId, that.uniqueId)) return false;
        if (!Objects.equals(url, that.url)) return false;
        if (!Objects.equals(hash, that.hash)) return false;
        if (!Objects.equals(promptJson, that.promptJson)) return false;
        return Objects.equals(promptTag, that.promptTag);
    }

    @Override
    public int hashCode() {
        int result = uniqueId != null ? uniqueId.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (hash != null ? hash.hashCode() : 0);
        result = 31 * result + (forced ? 1 : 0);
        result = 31 * result + (hasPromptMessage ? 1 : 0);
        result = 31 * result + (promptJson != null ? promptJson.hashCode() : 0);
        result = 31 * result + (promptTag != null ? promptTag.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ClientboundResourcePackPush{" +
                (uniqueId != null ? "uniqueId='" + uniqueId + "', " : "") +
                "url='" + url + '\'' +
                ", hash='" + hash + '\'' +
                ", forced=" + forced +
                ", hasPromptMessage=" + hasPromptMessage +
                (hasPromptMessage ? ", promptMessage='" + (promptJson != null ? promptJson : promptTag) + '\'' : "") +
                '}';
    }

    public static class Play extends ResourcePackSend {
        public Play() {
        }

        public Play(String url, String hash) {
            super(url, hash);
        }

        public Play(UUID uniqueId, String url, String hash, boolean forced) {
            super(uniqueId, url, hash, forced);
        }

        public Play(String url, String hash, boolean forced, boolean hasPromptMessage, String promptJson) {
            super(url, hash, forced, hasPromptMessage, promptJson);
        }

        public Play(UUID uniqueId, String url, String hash, boolean forced, boolean hasPromptMessage, Tag<?> promptTag) {
            super(uniqueId, url, hash, forced, hasPromptMessage, promptTag);
        }
    }

    public static class Configuration extends ResourcePackSend {
        public Configuration() {
        }

        public Configuration(String url, String hash) {
            super(url, hash);
        }

        public Configuration(UUID uniqueId, String url, String hash, boolean forced) {
            super(uniqueId, url, hash, forced);
        }

        public Configuration(String url, String hash, boolean forced, boolean hasPromptMessage, String promptJson) {
            super(url, hash, forced, hasPromptMessage, promptJson);
        }

        public Configuration(UUID uniqueId, String url, String hash, boolean forced, boolean hasPromptMessage, Tag<?> promptTag) {
            super(uniqueId, url, hash, forced, hasPromptMessage, promptTag);
        }
    }
}
