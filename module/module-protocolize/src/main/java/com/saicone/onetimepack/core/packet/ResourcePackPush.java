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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

public class ResourcePackPush extends AbstractPacket {

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
        register(Protocol.PLAY, DEFAULT_MAPPINGS, ResourcePackPush.Play.class);
        register(Protocol.CONFIGURATION, DEFAULT_MAPPINGS_CONFIGURATION, ResourcePackPush.Configuration.class);
    }

    public static void register(@Nullable Function<String, List<ProtocolIdMapping>> provider) {
        if (provider == null) {
            register();
            return;
        }
        final List<ProtocolIdMapping> play = provider.apply("play");
        register(Protocol.PLAY, play == null ? DEFAULT_MAPPINGS : play, ResourcePackPush.Play.class);
        final List<ProtocolIdMapping> configuration = provider.apply("configuration");
        register(Protocol.CONFIGURATION, configuration == null ? DEFAULT_MAPPINGS_CONFIGURATION : configuration, ResourcePackPush.Configuration.class);
    }

    public static void register(@NotNull Protocol protocol, @NotNull List<ProtocolIdMapping> mappings, Class<? extends ResourcePackPush> clazz) {
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

    private UUID uniqueId; // Added in 1.20.3
    private String url;
    private String hash;

    // Added in 1.17
    private boolean forced;
    private boolean hasPromptMessage;
    private String promptJson; // Removed in 1.20.3

    // Added in 1.20.3
    private Tag<?> promptTag;

    public ResourcePackPush() {
    }

    public ResourcePackPush(@Nullable String url, @Nullable String hash) {
        this.url = url;
        this.hash = hash;
    }

    public ResourcePackPush(@Nullable UUID uniqueId, @Nullable String url, @Nullable String hash, boolean forced) {
        this.uniqueId = uniqueId;
        this.url = url;
        this.hash = hash;
        this.forced = forced;
    }

    public ResourcePackPush(@Nullable String url, @Nullable String hash, boolean forced, boolean hasPromptMessage, @Nullable String promptJson) {
        this.url = url;
        this.hash = hash;
        this.forced = forced;
        this.hasPromptMessage = hasPromptMessage;
        this.promptJson = promptJson;
    }

    public ResourcePackPush(@Nullable UUID uniqueId, @Nullable String url, @Nullable String hash, boolean forced, boolean hasPromptMessage, @Nullable Tag<?> promptTag) {
        this.uniqueId = uniqueId;
        this.url = url;
        this.hash = hash;
        this.forced = forced;
        this.hasPromptMessage = hasPromptMessage;
        this.promptTag = promptTag;
    }

    @Nullable
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getHash() {
        return hash;
    }

    @Nullable
    public Object getPrompt() {
        return promptJson != null ? promptJson : promptTag;
    }

    @Nullable
    public String getPromptJson() {
        return promptJson;
    }

    @Nullable
    public Tag<?> getPromptTag() {
        return promptTag;
    }

    @NotNull
    public Protocol getProtocol() {
        return this instanceof Play ? Protocol.PLAY : Protocol.CONFIGURATION;
    }

    public boolean isForced() {
        return forced;
    }

    public boolean hasPromptMessage() {
        return hasPromptMessage;
    }

    public void setUniqueId(@Nullable UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    public void setHash(@Nullable String hash) {
        this.hash = hash;
    }

    public void setForced(boolean forced) {
        this.forced = forced;
    }

    public void setHasPromptMessage(boolean hasPromptMessage) {
        this.hasPromptMessage = hasPromptMessage;
    }

    public void setPromptJson(@Nullable String promptJson) {
        this.promptJson = promptJson;
        this.hasPromptMessage = promptJson != null;
    }

    public void setPromptTag(@Nullable Tag<?> promptTag) {
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
                        OneTimePack.log(2, e);
                    }
                } else {
                    promptJson = ProtocolUtil.readString(buf);
                }
            }
        } else {
            forced = false;
            hasPromptMessage = false;
        }
        OneTimePack.log(4, () -> "[" + getProtocol().name() + "] Packet#read() = " + this);
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
                        OneTimePack.log(2, e);
                    }
                } else {
                    ProtocolUtil.writeString(buf, promptJson);
                }
            }
        }
    }

    @NotNull
    public ResourcePackPush copy() {
        if (hasPromptMessage) {
            if (promptJson == null) {
                return new ResourcePackPush(uniqueId, url, hash, forced, true, promptTag);
            } else {
                return new ResourcePackPush(url, hash, forced, true, promptJson);
            }
        }
        return new ResourcePackPush(uniqueId, url, hash, forced);
    }

    @NotNull
    public ResourcePackPush.Play asPlay() {
        if (this instanceof ResourcePackPush.Play) {
            return (ResourcePackPush.Play) this;
        }
        if (hasPromptMessage) {
            if (promptJson == null) {
                return new ResourcePackPush.Play(uniqueId, url, hash, forced, true, promptTag);
            } else {
                return new ResourcePackPush.Play(url, hash, forced, true, promptJson);
            }
        }
        return new ResourcePackPush.Play(uniqueId, url, hash, forced);
    }

    @NotNull
    public ResourcePackPush.Configuration asConfiguration() {
        if (this instanceof ResourcePackPush.Configuration) {
            return (ResourcePackPush.Configuration) this;
        }
        if (hasPromptMessage) {
            if (promptJson == null) {
                return new ResourcePackPush.Configuration(uniqueId, url, hash, forced, true, promptTag);
            } else {
                return new ResourcePackPush.Configuration(url, hash, forced, true, promptJson);
            }
        }
        return new ResourcePackPush.Configuration(uniqueId, url, hash, forced);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourcePackPush that = (ResourcePackPush) o;

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

    public static class Play extends ResourcePackPush {
        public Play() {
        }

        public Play(@Nullable String url, @Nullable String hash) {
            super(url, hash);
        }

        public Play(@Nullable UUID uniqueId, @Nullable String url, @Nullable String hash, boolean forced) {
            super(uniqueId, url, hash, forced);
        }

        public Play(@Nullable String url, @Nullable String hash, boolean forced, boolean hasPromptMessage, @Nullable String promptJson) {
            super(url, hash, forced, hasPromptMessage, promptJson);
        }

        public Play(@Nullable UUID uniqueId, @Nullable String url, @Nullable String hash, boolean forced, boolean hasPromptMessage, @Nullable Tag<?> promptTag) {
            super(uniqueId, url, hash, forced, hasPromptMessage, promptTag);
        }
    }

    public static class Configuration extends ResourcePackPush {
        public Configuration() {
        }

        public Configuration(@Nullable String url, @Nullable String hash) {
            super(url, hash);
        }

        public Configuration(@Nullable UUID uniqueId, @Nullable String url, @Nullable String hash, boolean forced) {
            super(uniqueId, url, hash, forced);
        }

        public Configuration(@Nullable String url, @Nullable String hash, boolean forced, boolean hasPromptMessage, @Nullable String promptJson) {
            super(url, hash, forced, hasPromptMessage, promptJson);
        }

        public Configuration(@Nullable UUID uniqueId, @Nullable String url, @Nullable String hash, boolean forced, boolean hasPromptMessage, @Nullable Tag<?> promptTag) {
            super(uniqueId, url, hash, forced, hasPromptMessage, promptTag);
        }
    }
}