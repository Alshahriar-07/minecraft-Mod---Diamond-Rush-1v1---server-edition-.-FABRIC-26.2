package com.diamondrush.client;

import com.diamondrush.net.GuiStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client initializer: receives authoritative GUI snapshots from the server.
 * No gameplay decisions are made on this side.
 */
public final class DiamondRushClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(GuiStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> CustomGuiManager.handle(payload)));
    }
}
