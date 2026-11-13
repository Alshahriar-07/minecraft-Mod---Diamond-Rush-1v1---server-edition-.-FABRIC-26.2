package com.diamondrush.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Full GUI snapshot sent by the server. Sending a whole snapshot keeps the
 * client stateless: reconnecting or reopening the menu can never show stale
 * scores or a fake winner.
 */
public record GuiStatePayload(
        UiState state,
        String you,
        String opponent,
        int yourScore,
        int opponentScore,
        int matchSeconds,
        int countdown,
        boolean youReady,
        boolean opponentReady,
        boolean youPlayAgain,
        boolean opponentPlayAgain,
        String headline,
        String footer,
        String arena
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GuiStatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("diamondrush", "gui_state"));

    public static final StreamCodec<ByteBuf, GuiStatePayload> STREAM_CODEC = StreamCodec.of(
            GuiStatePayload::write,
            GuiStatePayload::read
    );

    public static GuiStatePayload none() {
        return new GuiStatePayload(UiState.NONE, "", "", 0, 0, 0, 0,
                false, false, false, false, "", "", "");
    }

    private static void write(ByteBuf buffer, GuiStatePayload payload) {
        buffer.writeByte(payload.state.ordinal());
        writeString(buffer, payload.you);
        writeString(buffer, payload.opponent);
        buffer.writeInt(payload.yourScore);
        buffer.writeInt(payload.opponentScore);
        buffer.writeInt(payload.matchSeconds);
        buffer.writeInt(payload.countdown);
        buffer.writeBoolean(payload.youReady);
        buffer.writeBoolean(payload.opponentReady);
        buffer.writeBoolean(payload.youPlayAgain);
        buffer.writeBoolean(payload.opponentPlayAgain);
        writeString(buffer, payload.headline);
        writeString(buffer, payload.footer);
        writeString(buffer, payload.arena);
    }

    private static GuiStatePayload read(ByteBuf buffer) {
        UiState[] states = UiState.values();
        int ordinal = buffer.readUnsignedByte();
        UiState state = ordinal >= 0 && ordinal < states.length ? states[ordinal] : UiState.NONE;
        return new GuiStatePayload(
                state,
                readString(buffer),
                readString(buffer),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                readString(buffer),
                readString(buffer),
                readString(buffer)
        );
    }

    /** Bounded UTF-8 string so a malicious packet cannot allocate unbounded memory. */
    private static void writeString(ByteBuf buffer, String value) {
        String safe = value == null ? "" : value;
        if (safe.length() > 512) {
            safe = safe.substring(0, 512);
        }
        byte[] bytes = safe.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    private static String readString(ByteBuf buffer) {
        int length = buffer.readUnsignedShort();
        if (length > 512) {
            throw new IllegalArgumentException("String field too long: " + length);
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
