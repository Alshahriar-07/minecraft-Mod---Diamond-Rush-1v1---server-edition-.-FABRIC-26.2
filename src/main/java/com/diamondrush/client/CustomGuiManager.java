package com.diamondrush.client;

import com.diamondrush.client.gui.DiamondRushScreen;
import com.diamondrush.net.GuiAction;
import com.diamondrush.net.GuiActionPayload;
import com.diamondrush.net.GuiStatePayload;
import com.diamondrush.net.UiState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Client-side screen manager. The server decides which state exists; this class
 * only opens, refreshes or closes the custom screen.
 */
public final class CustomGuiManager {

    private static GuiStatePayload lastState = GuiStatePayload.none();

    private CustomGuiManager() {
    }

    public static GuiStatePayload lastState() {
        return lastState;
    }

    public static void handle(GuiStatePayload payload) {
        lastState = payload;
        Minecraft client = Minecraft.getInstance();
        if (payload.state() == UiState.NONE) {
            if (client.gui.screen() instanceof DiamondRushScreen) {
                client.gui.setScreen(null);
            }
            return;
        }
        if (client.player == null) {
            return;
        }
        if (client.gui.screen() instanceof DiamondRushScreen screen) {
            screen.update(payload);
        } else {
            client.gui.setScreen(new DiamondRushScreen(payload));
        }
    }

    public static void send(GuiAction action) {
        if (ClientPlayNetworking.canSend(GuiActionPayload.TYPE)) {
            ClientPlayNetworking.send(new GuiActionPayload(action));
        }
    }
}
