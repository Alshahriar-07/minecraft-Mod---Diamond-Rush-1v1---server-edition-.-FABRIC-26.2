package com.diamondrush.match;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * Server-side diamond detection. The client is never asked who won: the block
 * break is observed on the server, after the block was legitimately destroyed.
 */
public final class DiamondDetectionManager {

    private final MatchManager matches;

    public DiamondDetectionManager(MatchManager matches) {
        this.matches = matches;
    }

    public void register() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                matches.onDiamondMined(serverPlayer);
            }
        });

        // Players may only mine while their match is live; this blocks
        // pre-mining during READY/COUNTDOWN and blocks after the result.
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }
            if (matches.isParticipant(serverPlayer) && !matches.canMine(serverPlayer)) {
                return false;
            }
            return true;
        });
    }
}
