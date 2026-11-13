package com.diamondrush.arena;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.storage.LevelData;

import java.util.Set;

/**
 * Moves players into arenas and keeps them there. Teleports, respawn anchors and
 * the anti-walk-away clamp during the countdown all go through this class.
 */
public final class SpawnManager {

    /**
     * Teleports a player to their assigned arena spawn and anchors their respawn
     * point to the same block, so dying never ejects them from the match.
     */
    public boolean teleportToArena(ServerPlayer player, Arena arena, int slot) {
        SpawnPoint spawn = arena.spawn(slot);
        if (spawn == null || !spawn.isFinite()) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }
        ServerLevel level = server.getLevel(spawn.dimensionKey());
        if (level == null) {
            level = server.overworld();
        }
        // Force the destination chunk to load before moving the player.
        level.getChunk(spawn.blockPos().getX() >> 4, spawn.blockPos().getZ() >> 4);

        boolean moved = player.teleportTo(level, spawn.x, spawn.y, spawn.z, Set.<Relative>of(),
                spawn.yaw, spawn.pitch, false);

        player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(spawn.dimensionKey(), spawn.blockPos(), spawn.yaw, spawn.pitch), true), false);
        restorePlayerState(player);
        return moved;
    }

    /**
     * Called every tick during the countdown. Players who try to leave their
     * spawn early are snapped back; block breaking is cancelled elsewhere.
     */
    public void enforceSpawn(ServerPlayer player, Arena arena, int slot) {
        SpawnPoint spawn = arena.spawn(slot);
        if (spawn == null) {
            return;
        }
        if (player.level().dimension().identifier().toString().equals(spawn.dimension)) {
            double dx = player.getX() - spawn.x;
            double dz = player.getZ() - spawn.z;
            double dy = player.getY() - spawn.y;
            if (dx * dx + dy * dy + dz * dz <= 4.0D) {
                return;
            }
        }
        teleportToArena(player, arena, slot);
    }

    public void restorePlayerState(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.removeAllEffects();
        player.clearFire();
        player.resetFallDistance();
        player.setAirSupply(player.getMaxAirSupply());
    }
}
