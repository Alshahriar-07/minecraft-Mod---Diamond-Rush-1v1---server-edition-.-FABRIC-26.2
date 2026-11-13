package com.diamondrush.client.gui;

import net.minecraft.resources.Identifier;

/**
 * Identifiers for the mod's PNG assets. Every one of these files is shipped in
 * {@code assets/diamondrush/textures/gui/} and is addressed by the same path.
 */
public final class GuiTextures {

    public static final Identifier PANEL = id("panel");
    public static final Identifier LOGO = id("logo");
    public static final Identifier SCOREBOARD = id("scoreboard");
    public static final Identifier DIAMOND = id("diamond");
    public static final Identifier CHAMPION = id("champion");
    public static final Identifier VICTORY = id("victory");
    public static final Identifier DEFEAT = id("defeat");

    public static final Identifier BUTTON_READY = id("button_ready");
    public static final Identifier BUTTON_READY_HOVER = id("button_ready_hover");
    public static final Identifier BUTTON_PLAY_AGAIN = id("button_play_again");
    public static final Identifier BUTTON_PLAY_AGAIN_HOVER = id("button_play_again_hover");
    public static final Identifier BUTTON_NEW_TOURNAMENT = id("button_new_tournament");
    public static final Identifier BUTTON_NEW_TOURNAMENT_HOVER = id("button_new_tournament_hover");
    public static final Identifier BUTTON_LEAVE = id("button_leave");
    public static final Identifier BUTTON_LEAVE_HOVER = id("button_leave_hover");

    private GuiTextures() {
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("diamondrush", "textures/gui/" + name + ".png");
    }
}
