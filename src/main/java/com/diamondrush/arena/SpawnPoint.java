package com.diamondrush.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * A persisted arena starting position. The dimension is stored as a serialized
 * {@link Identifier} so the value survives server restarts without relying on
 * registry numeric ids.
 */
public final class SpawnPoint {

    public String dimension = "minecraft:overworld";
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public SpawnPoint() {
    }

    public SpawnPoint(String dimension, double x, double y, double z, float yaw, float pitch) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static SpawnPoint of(ServerPlayer player) {
        ServerLevel level = player.level();
        return new SpawnPoint(
                level.dimension().identifier().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }

    public ResourceKey<Level> dimensionKey() {
        try {
            return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension));
        } catch (RuntimeException exception) {
            return Level.OVERWORLD;
        }
    }

    public BlockPos blockPos() {
        return BlockPos.containing(x, y, z);
    }

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Float.isFinite(yaw) && Float.isFinite(pitch);
    }

    public String describe() {
        return String.format(java.util.Locale.ROOT, "%s [%.1f, %.1f, %.1f]", dimension, x, y, z);
    }
}
