package com.diamondrush.match;

import com.diamondrush.DiamondRushMod;
import com.diamondrush.net.GuiStatePayload;
import com.diamondrush.net.NetworkHandler;
import com.diamondrush.net.UiState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Two-slot matchmaking queue. A player only ever enters a match when a second
 * player is present, and queued players are validated again when pairing.
 */
public final class MatchmakingManager {

    private final MatchManager matches;
    private final Deque<UUID> queue = new ArrayDeque<>();

    public MatchmakingManager(MatchManager matches) {
        this.matches = matches;
    }

    public int queued() {
        return queue.size();
    }

    public boolean isQueued(UUID player) {
        return queue.contains(player);
    }

    public void clear() {
        queue.clear();
    }

    /** Adds a player to the queue or pairs them with the waiting player. */
    public void join(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (matches.isParticipant(player)) {
            player.sendSystemMessage(Component.literal(ChatFormatting.RED
                    + "You are already in a Diamond Rush match. Use /diamondrush leave to forfeit."));
            matches.sendState(player);
            return;
        }
        pruneOffline();
        if (queue.contains(player.getUUID())) {
            sendWaiting(player);
            return;
        }
        UUID partnerId = queue.pollFirst();
        if (partnerId == null) {
            queue.addLast(player.getUUID());
            sendWaiting(player);
            player.sendSystemMessage(Component.literal(ChatFormatting.AQUA
                    + "Queued for Diamond Rush 1v1. Waiting for an opponent..."));
            return;
        }
        ServerPlayer partner = online(partnerId);
        if (partner == null) {
            queue.addLast(player.getUUID());
            sendWaiting(player);
            return;
        }
        String error = matches.beginTournament(partner, player);
        if (error != null) {
            queue.addLast(partnerId);
            queue.addLast(player.getUUID());
            player.sendSystemMessage(Component.literal(ChatFormatting.RED + error));
            partner.sendSystemMessage(Component.literal(ChatFormatting.RED + error));
            sendWaiting(player);
            sendWaiting(partner);
        }
    }

    /** Removes a queued player (no-op when they are already in a match). */
    public void leave(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (queue.remove(player.getUUID())) {
            NetworkHandler.send(player, GuiStatePayload.none());
            player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + "You left the Diamond Rush queue."));
        }
    }

    public void onDisconnect(ServerPlayer player) {
        if (player != null) {
            queue.remove(player.getUUID());
        }
    }

    /**
     * Admin helper for /diamondrush start: pairs the two longest waiting
     * players. Returns null on success or a message describing why it failed.
     */
    public String forceStart() {
        pruneOffline();
        if (queue.size() < 2) {
            return "At least two queued players are required.";
        }
        UUID firstId = queue.pollFirst();
        UUID secondId = queue.pollFirst();
        ServerPlayer first = online(firstId);
        ServerPlayer second = online(secondId);
        if (first == null || second == null) {
            if (firstId != null) {
                queue.addFirst(firstId);
            }
            if (secondId != null) {
                queue.addFirst(secondId);
            }
            return "Queued players are no longer online.";
        }
        String error = matches.beginTournament(first, second);
        if (error != null) {
            queue.addFirst(secondId);
            queue.addFirst(firstId);
        }
        return error;
    }

    /** Re-opens the waiting screen for a queued player. */
    public void sendWaiting(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int position = queue.contains(player.getUUID()) ? 1 : 0;
        NetworkHandler.send(player, new GuiStatePayload(
                UiState.WAITING,
                player.getName().getString(),
                "",
                0, 0, 0, 0,
                false, false, false, false,
                "",
                position == 0 ? "" : "QUEUE " + queue.size() + "/2 - WAITING FOR CHALLENGER",
                ""
        ));
    }

    /** Sends the correct screen after a reconnect or /diamondrush menu. */
    public void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (matches.isParticipant(player)) {
            matches.sendState(player);
        } else if (queue.contains(player.getUUID())) {
            sendWaiting(player);
        } else {
            NetworkHandler.send(player, GuiStatePayload.none());
        }
    }

    private void pruneOffline() {
        queue.removeIf(uuid -> online(uuid) == null);
    }

    private ServerPlayer online(UUID uuid) {
        MinecraftServer server = DiamondRushMod.server();
        return server == null ? null : server.getPlayerList().getPlayer(uuid);
    }
}
