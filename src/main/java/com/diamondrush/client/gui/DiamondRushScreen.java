package com.diamondrush.client.gui;

import com.diamondrush.client.CustomGuiManager;
import com.diamondrush.net.GuiAction;
import com.diamondrush.net.GuiStatePayload;
import com.diamondrush.net.UiState;
import com.diamondrush.util.Format;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The custom Diamond Rush interface. Everything is drawn from the mod's own PNG
 * assets (no vanilla dirt background, no default buttons) and every button maps
 * to a server-validated action.
 */
public final class DiamondRushScreen extends Screen {

    private static final int PANEL_W = 380;
    private static final int PANEL_H = 280;
    private static final int BUTTON_Y = 210;
    private static final int HINT_Y = 248;

    private static final int CYAN = 0xFF3FE0F5;
    private static final int CYAN_DIM = 0xFF1E93A8;
    private static final int GREEN = 0xFF57E86B;
    private static final int GOLD = 0xFFFFC542;
    private static final int WHITE = 0xFFEDF6FA;
    private static final int GREY = 0xFF8FA3B0;
    private static final int RED = 0xFFFF6B6B;

    private GuiStatePayload state;
    private final List<HotButton> buttons = new ArrayList<>();
    private int left;
    private int top;
    private float uiScale = 1.0F;

    public DiamondRushScreen(GuiStatePayload state) {
        super(Component.literal("Diamond Rush 1v1"));
        this.state = state;
    }

    /** Called when the server pushes a fresh snapshot. */
    public void update(GuiStatePayload payload) {
        this.state = payload;
        rebuildButtons();
    }

    @Override
    protected void init() {
        // Shrink the whole interface on small windows so the panel always fits.
        this.uiScale = Math.min(1.0F, Math.min((this.width - 16.0F) / PANEL_W, (this.height - 12.0F) / PANEL_H));
        this.left = Math.round((this.width - PANEL_W) / 2.0F);
        this.top = Math.round((this.height - PANEL_H) / 2.0F);
        rebuildButtons();
    }

    private void rebuildButtons() {
        buttons.clear();
        int center = centerX();
        int y = top + BUTTON_Y;
        switch (state.state()) {
            case WAITING -> addButton(center - 110, y, 220, 28, "LEAVE QUEUE", GuiAction.LEAVE,
                    GuiTextures.BUTTON_LEAVE, GuiTextures.BUTTON_LEAVE_HOVER, true);
            case READY -> addButton(center - 110, y, 220, 28,
                    state.youReady() ? "READY ✓" : "READY", GuiAction.READY,
                    GuiTextures.BUTTON_READY, GuiTextures.BUTTON_READY_HOVER, !state.youReady());
            case RESULT -> addButton(center - 110, y, 220, 28,
                    state.youPlayAgain() ? "WAITING FOR OPPONENT" : "PLAY AGAIN", GuiAction.PLAY_AGAIN,
                    GuiTextures.BUTTON_PLAY_AGAIN, GuiTextures.BUTTON_PLAY_AGAIN_HOVER, !state.youPlayAgain());
            case CHAMPION -> addButton(center - 110, y, 220, 28,
                    state.youPlayAgain() ? "WAITING FOR OPPONENT" : "NEW TOURNAMENT", GuiAction.NEW_TOURNAMENT,
                    GuiTextures.BUTTON_NEW_TOURNAMENT, GuiTextures.BUTTON_NEW_TOURNAMENT_HOVER, !state.youPlayAgain());
            default -> {
            }
        }
    }

    private void addButton(int x, int y, int width, int height, String label, GuiAction action,
                           Identifier texture, Identifier hoverTexture, boolean enabled) {
        buttons.add(new HotButton(x, y, width, height, label, action, texture, hoverTexture, enabled));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawBackdrop(graphics);
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(this.width / 2.0F, this.height / 2.0F);
        pose.scale(this.uiScale, this.uiScale);
        pose.translate(-this.width / 2.0F, -this.height / 2.0F);
        graphics.blit(GuiTextures.PANEL, left, top, left + PANEL_W, top + PANEL_H, 0.0F, 1.0F, 0.0F, 1.0F);
        // Cyan accent line along the top of the panel.
        graphics.fill(left + 6, top + 4, left + PANEL_W - 6, top + 5, 0xAA3FE0F5);
        graphics.fill(left + 6, top + PANEL_H - 5, left + PANEL_W - 6, top + PANEL_H - 4, 0x661E93A8);

        image(graphics, GuiTextures.LOGO, centerX(), top + 10, 320, 56);
        centered(graphics, "1v1 DIAMOND MINING TOURNAMENT", centerX(), top + 70, CYAN_DIM);
        if (!state.arena().isEmpty()) {
            String arena = "ARENA " + state.arena().toUpperCase(Locale.ROOT);
            graphics.text(this.font, arena, left + PANEL_W - 14 - this.font.width(arena), top + 20, CYAN_DIM, true);
        }

        switch (state.state()) {
            case WAITING -> drawWaiting(graphics);
            case READY -> drawReady(graphics);
            case COUNTDOWN -> drawCountdown(graphics);
            case MATCH -> drawMatch(graphics);
            case RESULT -> drawResult(graphics);
            case CHAMPION -> drawChampion(graphics);
            case NONE -> {
            }
        }

        float localMouseX = this.width / 2.0F + (mouseX - this.width / 2.0F) / this.uiScale;
        float localMouseY = this.height / 2.0F + (mouseY - this.height / 2.0F) / this.uiScale;
        drawButtons(graphics, localMouseX, localMouseY);
        drawHint(graphics);
        pose.popMatrix();
    }

    private void drawBackdrop(GuiGraphicsExtractor graphics) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xF005080C, 0xF0001A24);
        // Faint diamond lattice accent in the corners.
        graphics.fill(0, 0, this.width, 2, 0x553FE0F5);
        graphics.fill(0, this.height - 2, this.width, this.height, 0x551E93A8);
    }

    private void drawWaiting(GuiGraphicsExtractor graphics) {
        int center = centerX();
        textScaled(graphics, "WAITING FOR OPPONENT", center, top + 120, 1.65F, WHITE);
        centered(graphics, "YOU: " + state.you(), center, top + 156, CYAN);
        if (!state.footer().isEmpty()) {
            centered(graphics, state.footer(), center, top + 176, GOLD);
        }
        centered(graphics, "Invite a friend, then both run: /diamondrush join", center, top + 186, GREY);
    }

    private void drawReady(GuiGraphicsExtractor graphics) {
        int center = centerX();
        centered(graphics, "PLAYER 1  VS  PLAYER 2", center, top + 96, GREY);
        drawNames(graphics, center, top + 110);
        centered(graphics, "CURRENT SCORE", center, top + 132, CYAN_DIM);
        drawScorePlate(graphics, center, top + 144);
        drawReadyChips(graphics, center, top + 190);
        centered(graphics, "BOTH PLAYERS MUST CLICK READY", center, top + 230, GOLD);
    }

    private void drawCountdown(GuiGraphicsExtractor graphics) {
        int center = centerX();
        centered(graphics, "COUNTDOWN", center, top + 100, CYAN_DIM);
        String number = state.countdown() <= 0 ? "GO!" : String.valueOf(state.countdown());
        textScaled(graphics, number, center, top + 118, 4.0F, state.countdown() <= 0 ? GREEN : WHITE);
        drawNames(graphics, center, top + 176);
        centered(graphics, "SCORE  " + state.yourScore() + " - " + state.opponentScore(), center, top + 194, GOLD);
        centered(graphics, "GET READY TO MINE", center, top + 230, GREY);
    }

    private void drawMatch(GuiGraphicsExtractor graphics) {
        int center = centerX();
        boolean live = (System.currentTimeMillis() / 500L) % 2L == 0L;
        graphics.fill(center - 46, top + 101, center - 38, top + 109, live ? RED : 0xFF7A2B2B);
        centered(graphics, "LIVE MATCH", center + 4, top + 101, WHITE);
        drawNames(graphics, center, top + 116);
        centered(graphics, "MATCH TIME", center, top + 138, CYAN_DIM);
        textScaled(graphics, Format.clock(state.matchSeconds() * 1000L), center, top + 148, 2.0F, CYAN);
        centered(graphics, "SCORE  " + state.yourScore() + " - " + state.opponentScore(), center, top + 176, GOLD);
        image(graphics, GuiTextures.DIAMOND, center - 88, top + 194, 22, 22);
        centered(graphics, "FIRST DIAMOND WINS THE MATCH", center + 12, top + 200, WHITE);
    }

    private void drawResult(GuiGraphicsExtractor graphics) {
        int center = centerX();
        boolean youWon = state.headline().equalsIgnoreCase(state.you());
        image(graphics, youWon ? GuiTextures.VICTORY : GuiTextures.DEFEAT, center, top + 88, 300, 40);
        String headline = state.headline().isEmpty() ? "MATCH OVER" : state.headline() + " WINS!";
        textScaled(graphics, headline, center, top + 134, 1.5F, youWon ? GOLD : WHITE);
        centered(graphics, "SCORE", center, top + 156, CYAN_DIM);
        drawScorePlate(graphics, center, top + 166);
        centered(graphics, "MATCH TIME: " + Format.clock(state.matchSeconds() * 1000L), center, top + 190, CYAN);
        centered(graphics, "NEXT MATCH", center, top + 226, GOLD);
        String status = state.youPlayAgain()
                ? "WAITING FOR OPPONENT TO CLICK PLAY AGAIN"
                : "CLICK PLAY AGAIN WHEN YOU ARE READY";
        centered(graphics, status, center, top + 236, GREY);
    }

    private void drawChampion(GuiGraphicsExtractor graphics) {
        int center = centerX();
        image(graphics, GuiTextures.CHAMPION, center, top + 84, 84, 84);
        boolean youWon = state.headline().equalsIgnoreCase(state.you());
        String headline = state.headline().isEmpty() ? "CHAMPION" : state.headline().toUpperCase(Locale.ROOT);
        textScaled(graphics, headline, center, top + 166, 1.5F, GOLD);
        centered(graphics, "CHAMPION", center, top + 182, GOLD);
        centered(graphics, "FINAL SCORE", center, top + 194, CYAN_DIM);
        centered(graphics, state.yourScore() + "  -  " + state.opponentScore(), center, top + 204, WHITE);
        String status = state.youPlayAgain()
                ? "WAITING FOR OPPONENT TO CLICK NEW TOURNAMENT"
                : "BOTH PLAYERS MUST CLICK NEW TOURNAMENT";
        centered(graphics, status, center, top + 236, GREY);
        if (!youWon) {
            centered(graphics, "GG - RUN IT BACK?", center, top + 226, CYAN_DIM);
        }
    }

    private void drawNames(GuiGraphicsExtractor graphics, int center, int y) {
        int gap = 32;
        graphics.text(this.font, "YOU", center - gap - this.font.width("YOU"), y - 10, CYAN_DIM, true);
        graphics.text(this.font, "OPPONENT", center + gap, y - 10, CYAN_DIM, true);
        graphics.text(this.font, state.you(), center - gap - this.font.width(state.you()), y, WHITE, true);
        graphics.text(this.font, state.opponent(), center + gap, y, WHITE, true);
        centered(graphics, "VS", center, y, GOLD);
    }

    private void drawScorePlate(GuiGraphicsExtractor graphics, int center, int y) {
        image(graphics, GuiTextures.SCOREBOARD, center, y, 240, 40);
        textScaled(graphics, state.yourScore() + "  -  " + state.opponentScore(), center, y + 12, 2.0F, CYAN);
    }

    private void drawReadyChips(GuiGraphicsExtractor graphics, int center, int y) {
        chip(graphics, center - 150, y, 146, "YOU", state.youReady());
        chip(graphics, center + 4, y, 146, "OPPONENT", state.opponentReady());
    }

    private void chip(GuiGraphicsExtractor graphics, int x, int y, int width, String label, boolean active) {
        int accent = active ? GREEN : 0xFF44515C;
        graphics.fill(x, y, x + width, y + 22, 0xAA0C141C);
        graphics.fill(x, y, x + 3, y + 22, accent);
        graphics.fill(x, y, x + width, y + 1, accent);
        String text = label + "  " + (active ? "READY" : "WAITING");
        graphics.text(this.font, text, x + 9, y + 7, active ? GREEN : GREY, true);
    }

    private void drawButtons(GuiGraphicsExtractor graphics, float mouseX, float mouseY) {
        for (HotButton button : buttons) {
            boolean hovered = button.enabled
                    && mouseX >= button.x && mouseX <= button.x + button.width
                    && mouseY >= button.y && mouseY <= button.y + button.height;
            Identifier texture = hovered ? button.hoverTexture : button.texture;
            graphics.blit(texture, button.x, button.y, button.x + button.width, button.y + button.height,
                    0.0F, 1.0F, 0.0F, 1.0F);
            if (!button.enabled) {
                graphics.fill(button.x, button.y, button.x + button.width, button.y + button.height, 0x99000000);
            }
            int color = button.enabled ? (hovered ? 0xFFFFFFFF : WHITE) : 0xFF7C8A94;
            centered(graphics, button.label, button.x + button.width / 2,
                    button.y + (button.height - 8) / 2, color);
        }
    }

    private void drawHint(GuiGraphicsExtractor graphics) {
        if (state.footer().isEmpty() || state.state() == UiState.WAITING) {
            return;
        }
        if (state.state() == UiState.RESULT || state.state() == UiState.CHAMPION) {
            return;
        }
        centered(graphics, state.footer(), centerX(), top + HINT_Y, GREY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double localX = this.width / 2.0D + (event.x() - this.width / 2.0D) / this.uiScale;
            double localY = this.height / 2.0D + (event.y() - this.height / 2.0D) / this.uiScale;
            for (HotButton button : buttons) {
                if (button.enabled
                        && localX >= button.x && localX <= button.x + button.width
                        && localY >= button.y && localY <= button.y + button.height) {
                    CustomGuiManager.send(button.action);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return state.state() == UiState.WAITING
                || state.state() == UiState.RESULT
                || state.state() == UiState.CHAMPION;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }

    private int centerX() {
        return left + PANEL_W / 2;
    }

    private void image(GuiGraphicsExtractor graphics, Identifier texture, int centerX, int y, int width, int height) {
        graphics.blit(texture, centerX - width / 2, y, centerX + width / 2, y + height,
                0.0F, 1.0F, 0.0F, 1.0F);
    }

    private void centered(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color) {
        graphics.text(this.font, text, centerX - this.font.width(text) / 2, y, color, true);
    }

    private void textScaled(GuiGraphicsExtractor graphics, String text, int centerX, int y, float scale, int color) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) centerX, (float) y);
        pose.scale(scale, scale);
        graphics.text(this.font, text, -this.font.width(text) / 2, 0, color, true);
        pose.popMatrix();
    }

    private static final class HotButton {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final String label;
        private final GuiAction action;
        private final Identifier texture;
        private final Identifier hoverTexture;
        private final boolean enabled;

        private HotButton(int x, int y, int width, int height, String label, GuiAction action,
                          Identifier texture, Identifier hoverTexture, boolean enabled) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.label = label;
            this.action = action;
            this.texture = texture;
            this.hoverTexture = hoverTexture;
            this.enabled = enabled;
        }
    }
}
