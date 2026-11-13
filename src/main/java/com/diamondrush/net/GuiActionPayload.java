package com.diamondrush.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * A single validated client request such as "I am ready".
 */
public record GuiActionPayload(GuiAction action) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GuiActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("diamondrush", "gui_action"));

    public static final StreamCodec<ByteBuf, GuiActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeByte(payload.action.ordinal()),
            buffer -> {
                GuiAction[] actions = GuiAction.values();
                int ordinal = buffer.readUnsignedByte();
                if (ordinal < 0 || ordinal >= actions.length) {
                    return new GuiActionPayload(null);
                }
                return new GuiActionPayload(actions[ordinal]);
            }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
