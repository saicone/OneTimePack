package com.saicone.onetimepack.core.packet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.core.BungeeProcessor;
import com.saicone.onetimepack.core.PackResult;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.ChatSerializer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class ResourcePackPush extends DefinedPacket {

    public static final int MAX_HASH_LENGTH = 40;

    private UUID uniqueId; // Added in 1.20.3
    private String url;
    private String hash;

    // Added in 1.17
    private boolean forced;
    private boolean hasPromptMessage;
    private BaseComponent prompt;

    private transient Protocol protocol;

    public ResourcePackPush() {
    }

    @Deprecated(since = "1.17")
    public ResourcePackPush(@Nullable String url, @Nullable String hash) {
        this.url = url;
        this.hash = hash;
    }

    @Deprecated(since = "1.20.3")
    public ResourcePackPush(@Nullable String url, @Nullable String hash, boolean forced, boolean hasPromptMessage, @Nullable BaseComponent prompt) {
        this.url = url;
        this.hash = hash;
        this.forced = forced;
        this.hasPromptMessage = hasPromptMessage;
        this.prompt = prompt;
    }

    public ResourcePackPush(@Nullable UUID uniqueId, @Nullable String url, @Nullable String hash, boolean forced) {
        this.uniqueId = uniqueId;
        this.url = url;
        this.hash = hash;
        this.forced = forced;
    }

    public ResourcePackPush(@Nullable UUID uniqueId, @Nullable String url, @Nullable String hash, boolean forced, boolean hasPromptMessage, @Nullable BaseComponent prompt) {
        this.uniqueId = uniqueId;
        this.url = url;
        this.hash = hash;
        this.forced = forced;
        this.hasPromptMessage = hasPromptMessage;
        this.prompt = prompt;
    }

    public boolean isForced() {
        return forced;
    }

    public boolean hasPromptMessage() {
        return hasPromptMessage;
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
    public BaseComponent getPrompt() {
        return prompt;
    }

    @Nullable
    public JsonElement getJsonPrompt() {
        if (prompt == null) {
            return null;
        }
        return getJsonPrompt(ChatSerializer.forVersion(ProtocolConstants.MINECRAFT_1_16).toJson(prompt));
    }

    @NotNull
    private JsonElement getJsonPrompt(@NotNull JsonElement json) {
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                getJsonPrompt(element);
            }
        } else if (json.isJsonObject()) {
            final JsonObject object = json.getAsJsonObject();
            if (object.has("color")) {
                object.addProperty("color", object.get("color").getAsString().toLowerCase());
            }
            if (object.has("extra")) {
                getJsonPrompt(object.get("extra"));
            }
        }

        return json;
    }

    @NotNull
    public Protocol getProtocol() {
        return protocol == null ? Protocol.GAME : protocol;
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

    public void setPrompt(@Nullable BaseComponent prompt) {
        this.hasPromptMessage = prompt != null;
        this.prompt = prompt;
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        if (OneTimePack.get().getPacketHandler() instanceof BungeeProcessor processor) {
            processor.onPackPush(this, handler);
        }
    }

    @Override
    public void read(ByteBuf buf, Protocol protocol, ProtocolConstants.Direction direction, int protocolVersion) {
        this.protocol = protocol;

        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_3) {
            uniqueId = readUUID(buf);
        }
        url = readString(buf);
        hash = readString(buf, MAX_HASH_LENGTH);
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_17) {
            forced = buf.readBoolean();
            hasPromptMessage = buf.readBoolean();
            if (hasPromptMessage) {
                prompt = readBaseComponent(buf, protocolVersion);
            }
        } else {
            forced = false;
            hasPromptMessage = false;
        }
    }

    @Override
    public void write(ByteBuf buf, Protocol protocol, ProtocolConstants.Direction direction, int protocolVersion) {
        this.protocol = protocol;

        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_3) {
            writeUUID(uniqueId, buf);
        }
        writeString(url, buf);
        writeString(hash, buf);
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_17) {
            buf.writeBoolean(forced);
            buf.writeBoolean(hasPromptMessage);
            if (hasPromptMessage) {
                writeBaseComponent(prompt, buf, protocolVersion);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResourcePackPush that = (ResourcePackPush) obj;

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
        return "ClientboundResourcePackPushPacket{" +
                (uniqueId != null ? "uniqueId='" + uniqueId + "', " : "") +
                "url='" + url + '\'' +
                ", hash='" + hash + '\'' +
                ", forced=" + forced +
                ", hasPromptMessage=" + hasPromptMessage +
                (hasPromptMessage ? ", promptMessage='" + prompt + '\'' : "") +
                '}';
    }

    @NotNull
    @SuppressWarnings("deprecation")
    public ResourcePackStatus asStatus(@NotNull PackResult result, int protocolVersion) {
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_3) {
            final UUID id = uniqueId != null ? uniqueId : UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
            return new ResourcePackStatus(id, result);
        } else if (protocolVersion >= ProtocolConstants.MINECRAFT_1_10) {
            return new ResourcePackStatus(result);
        } else {
            return new ResourcePackStatus(hash, result);
        }
    }
}
