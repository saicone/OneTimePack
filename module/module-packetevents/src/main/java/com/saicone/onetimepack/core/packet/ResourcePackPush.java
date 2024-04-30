package com.saicone.onetimepack.core.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.saicone.onetimepack.OneTimePack;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class ResourcePackPush extends PacketWrapper<ResourcePackPush> implements CommonPacketWrapper<ResourcePackPush> {

    public static final int MAX_HASH_LENGTH = 40;

    private final ConnectionState state;

    private UUID uniqueId; // Added in 1.20.3
    private String url;
    private String hash;

    // Added in 1.17
    private boolean forced;
    private boolean hasPromptMessage;
    private Component prompt;

    public ResourcePackPush(@NotNull PacketSendEvent event) {
        super(event, false);
        this.state = event.getPacketType() == PacketType.Configuration.Server.RESOURCE_PACK_SEND ? ConnectionState.CONFIGURATION : ConnectionState.PLAY;
        readEvent(event);
    }

    public ResourcePackPush(@Nullable String url, @Nullable String hash) {
        this(ConnectionState.PLAY, null, url, hash, false, false, null);
    }

    public ResourcePackPush(@Nullable String url, @Nullable String hash, boolean forced, boolean hasPromptMessage, @Nullable Component prompt) {
        this(ConnectionState.PLAY, null, url, hash, forced, hasPromptMessage, prompt);
    }

    public ResourcePackPush(@NotNull ConnectionState state, @Nullable UUID uniqueId, @Nullable String url, @Nullable String hash, boolean forced, boolean hasPromptMessage, @Nullable Component prompt) {
        super(state == ConnectionState.CONFIGURATION ? PacketType.Configuration.Server.RESOURCE_PACK_SEND : PacketType.Play.Server.RESOURCE_PACK_SEND);
        this.state = state;
        this.uniqueId = uniqueId;
        this.url = url;
        this.hash = hash;
        this.forced = forced;
        this.hasPromptMessage = hasPromptMessage;
        this.prompt = prompt;
    }

    @NotNull
    @Override
    public ConnectionState getState() {
        return state;
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
    public Component getPrompt() {
        return prompt;
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

    public void setPrompt(@Nullable Component prompt) {
        this.prompt = prompt;
        this.hasPromptMessage = prompt != null;
    }

    @Override
    public void read() {
        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_3)) {
            uniqueId = readUUID();
        }

        url = readString();
        hash = readString(MAX_HASH_LENGTH);
        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            forced = readBoolean();
            hasPromptMessage = readBoolean();
            if (hasPromptMessage) {
                prompt = readComponent();
            }
        }

        if (OneTimePack.getLogLevel() >= 4) {
            OneTimePack.log(4, "[" + getState().name() + "] Packet#read() = " + this);
        }
    }

    @Override
    public void write() {
        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_3)) {
            writeUUID(uniqueId);
        }

        writeString(url);
        writeString(hash, MAX_HASH_LENGTH);
        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            writeBoolean(forced);
            writeBoolean(hasPromptMessage);
            if (hasPromptMessage) {
                writeComponent(prompt);
            }
        }
    }

    @Override
    public @NotNull ResourcePackPush copy() {
        return new ResourcePackPush(state, uniqueId, url, hash, forced, hasPromptMessage, prompt);
    }

    @Override
    public void copy(ResourcePackPush wrapper) {
        uniqueId = wrapper.uniqueId;
        url = wrapper.url;
        hash = wrapper.hash;
        forced = wrapper.forced;
        hasPromptMessage = wrapper.hasPromptMessage;
        prompt = wrapper.prompt;
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
        return Objects.equals(prompt, that.prompt);
    }

    @Override
    public int hashCode() {
        int result = uniqueId != null ? uniqueId.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (hash != null ? hash.hashCode() : 0);
        result = 31 * result + (forced ? 1 : 0);
        result = 31 * result + (hasPromptMessage ? 1 : 0);
        result = 31 * result + (prompt != null ? prompt.hashCode() : 0);
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
                (hasPromptMessage ? ", promptMessage='" + AdventureSerializer.toJson(prompt) + '\'' : "") +
                '}';
    }
}
