package com.diamondrush.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Guarantees a clean, identical starting state for every player before a match:
 * no leftover items, armour, offhand, experience or potion effects, followed by
 * a fair starting kit that is actually capable of mining diamond ore.
 */
public final class InventoryManager {

    /**
     * Clears everything a player could carry over from a previous match.
     * In modern Minecraft the armour and offhand slots are part of the player
     * inventory, so {@link net.minecraft.world.entity.player.Inventory#clearContent()}
     * clears them too.
     */
    public void reset(ServerPlayer player) {
        player.getInventory().clearContent();
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.clearFire();
        player.resetFallDistance();
        player.setAirSupply(player.getMaxAirSupply());
    }

    /**
     * Clears the player and hands out the Diamond Rush starting kit.
     */
    public void prepareForMatch(ServerPlayer player) {
        reset(player);
        player.getInventory().setItem(0, new ItemStack(Items.IRON_PICKAXE));
        player.getInventory().setItem(1, new ItemStack(Items.TORCH, 32));
        player.getInventory().setItem(2, new ItemStack(Items.COOKED_BEEF, 16));
    }
}
