package com.diamondrush.net;

import com.diamondrush.match.MatchManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Payload registration and the two directions of GUI networking. The client
 * only ever receives snapshots and sends small intent packets.
 */
public final class NetworkHandler {

    private NetworkHandler() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(GuiStatePayload.TYPE, GuiStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GuiActionPayload.TYPE, GuiActionPayload.STREAM_CODEC);
    }

    public static void registerServerReceivers(MatchManager matches) {
        ServerPlayNetworking.registerGlobalReceiver(GuiActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> matches.handleAction(context.player(), payload.action())));
    }

    public static void send(ServerPlayer player, GuiStatePayload payload) {
        if (player == null || payload == null) {
            return;
        }
        if (ServerPlayNetworking.canSend(player, GuiStatePayload.TYPE)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
