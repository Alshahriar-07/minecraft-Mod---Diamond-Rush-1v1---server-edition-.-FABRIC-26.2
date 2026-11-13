package com.diamondrush.match;

import com.diamondrush.arena.Arena;
import com.diamondrush.arena.ArenaManager;
import com.diamondrush.arena.SpawnManager;
import com.diamondrush.net.GuiAction;
import com.diamondrush.net.GuiStatePayload;
import com.diamondrush.net.NetworkHandler;
import com.diamondrush.net.UiState;
import com.diamondrush.player.InventoryManager;
import com.diamondrush.stats.StatisticsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative state machine for every match: ready checks, countdown,
 * live timer, diamond wins, rematches, disconnects and respawns. Every mutation
 * runs on the server thread and is re-validated here regardless of what a client
 * claims.
 */
public final class MatchManager {

    public static final int COUNTDOWN_SECONDS = 3;
    private static final int COUNTDOWN_TICKS = COUNTDOWN_SECONDS * 20;
    private static final int NEEDED_WINS = Tournament.WINS_REQUIRED;

    private final ArenaManager arenas;
    private final SpawnManager spawns;
    private final InventoryManager inventories;
    private final TournamentManager tournaments;
    private final StatisticsManager statistics;
    private final PlayerReadyManager ready = new PlayerReadyManager();

    private int tickCounter;

    public MatchManager(ArenaManager arenas, SpawnManager spawns, InventoryManager inventories,
                        TournamentManager tournaments, StatisticsManager statistics) {
        this.arenas = arenas;
        this.spawns = spawns;
        this.inventories = inventories;
        this.tournaments = tournaments;
        this.statistics = statistics;
    }

    public Tournament tournamentOf(UUID player) {
        return tournaments.find(player);
    }

    public boolean isParticipant(ServerPlayer player) {
        return player != null && tournaments.find(player.getUUID()) != null;
    }

    /** Block breaking is only legal during a live match. */
    public boolean canMine(ServerPlayer player) {
        Tournament tournament = tournaments.find(player == null ? null : player.getUUID());
        return tournament != null && tournament.match != null
                && tournament.match.phase == MatchPhase.PLAYING;
    }

    public StatisticsManager statistics() {
        return statistics;
    }

    public TournamentManager tournaments() {
        return tournaments;
    }

    /**
     * Pairs two players and starts a tournament.
     *
     * @return null on success, or a human readable error message
     */
    public String beginTournament(ServerPlayer player1, ServerPlayer player2) {
        if (player1 == null || player2 == null) {
            return "Both players must be online.";
        }
        if (player1.getUUID().equals(player2.getUUID())) {
            return "A player cannot play against themselves.";
        }
        if (tournaments.find(player1.getUUID()) != null) {
            return player1.getName().getString() + " is already in a match.";
        }
        if (tournaments.find(player2.getUUID()) != null) {
            return player2.getName().getString() + " is already in a match.";
        }
        MinecraftServer server = player1.level().getServer();
        if (server == null) {
            return "No server available.";
        }
        Arena arena = arenas.pickArena(server, null);
        if (arena == null) {
            return "No usable arena available. Ask an admin to create one.";
        }
        arenas.reserve(arena.name);
        Tournament tournament = tournaments.create(player1, player2);
        Match match = tournaments.newMatch(tournament, arena.name);
        setupMatch(tournament, match);
        message(tournament, ChatFormatting.AQUA + "Match found on arena " + ChatFormatting.WHITE + arena.name
                + ChatFormatting.GRAY + " - both players must press READY.");
        sendBoth(tournament);
        return null;
    }

    private void setupMatch(Tournament tournament, Match match) {
        Arena arena = arenas.get(match.arena);
        for (int slot = 1; slot <= 2; slot++) {
            ServerPlayer player = online(tournament, slot == 1 ? tournament.player1 : tournament.player2);
            if (player == null) {
                continue;
            }
            inventories.prepareForMatch(player);
            if (arena != null) {
                if (!spawns.teleportToArena(player, arena, slot)) {
                    player.sendSystemMessage(Component.literal(ChatFormatting.RED
                            + "Arena spawn " + slot + " is invalid. Tell an admin to re-set it."));
                }
            }
        }
        match.phase = MatchPhase.READY;
        match.ready1 = false;
        match.ready2 = false;
        match.again1 = false;
        match.again2 = false;
        match.winner = null;
        match.countdownTicks = 0;
        match.durationMillis = 0L;
    }

    /** Routes a validated client action. */
    public void handleAction(ServerPlayer player, GuiAction action) {
        if (player == null || action == null) {
            return;
        }
        Tournament tournament = tournaments.find(player.getUUID());
        if (tournament == null || tournament.match == null) {
            NetworkHandler.send(player, GuiStatePayload.none());
            return;
        }
        Match match = tournament.match;
        switch (action) {
            case READY -> onReady(player, tournament, match);
            case PLAY_AGAIN -> onPlayAgain(player, tournament, match);
            case NEW_TOURNAMENT -> onNewTournament(player, tournament, match);
            case LEAVE -> forfeit(player, "left the match");
        }
    }

    private void onReady(ServerPlayer player, Tournament tournament, Match match) {
        if (!ready.markReady(match, player.getUUID())) {
            sendState(player, tournament, match);
            return;
        }
        if (ready.bothReady(match)) {
            match.phase = MatchPhase.COUNTDOWN;
            match.countdownTicks = COUNTDOWN_TICKS;
            message(tournament, ChatFormatting.GREEN + "Both players ready. Countdown started!");
            announce(tournament, ChatFormatting.GRAY + "First diamond mined wins the match. Good luck.");
        }
        sendBoth(tournament);
    }

    private void onPlayAgain(ServerPlayer player, Tournament tournament, Match match) {
        if (!ready.markPlayAgain(match, player.getUUID())) {
            sendState(player, tournament, match);
            return;
        }
        if (ready.bothAgain(match)) {
            startNextMatch(tournament);
        } else {
            sendBoth(tournament);
        }
    }

    private void onNewTournament(ServerPlayer player, Tournament tournament, Match match) {
        if (!ready.markNewTournament(match, player.getUUID())) {
            sendState(player, tournament, match);
            return;
        }
        if (!ready.bothAgain(match)) {
            sendBoth(tournament);
            return;
        }
        ServerPlayer first = online(tournament, tournament.player1);
        ServerPlayer second = online(tournament, tournament.player2);
        endTournament(tournament, null);
        if (first == null || second == null) {
            return;
        }
        String error = beginTournament(first, second);
        if (error != null) {
            first.sendSystemMessage(Component.literal(ChatFormatting.RED + error));
            second.sendSystemMessage(Component.literal(ChatFormatting.RED + error));
            sendNone(first);
            sendNone(second);
        }
    }

    /** Both players pressed PLAY AGAIN: move to a different arena and restart. */
    private void startNextMatch(Tournament tournament) {
        ServerPlayer first = online(tournament, tournament.player1);
        ServerPlayer second = online(tournament, tournament.player2);
        if (first == null || second == null) {
            endTournament(tournament, "Opponent disconnected.");
            if (first != null) {
                sendNone(first);
            }
            if (second != null) {
                sendNone(second);
            }
            return;
        }
        String previousArena = tournament.match == null ? null : tournament.match.arena;
        arenas.release(previousArena);
        Arena next = arenas.pickArena(first.level().getServer(), previousArena);
        if (next == null) {
            next = arenas.pickArena(first.level().getServer(), null);
        }
        if (next == null) {
            arenas.reserve(previousArena);
            message(tournament, ChatFormatting.RED + "No arena available for the next match.");
            if (tournament.match != null) {
                tournament.match.again1 = false;
                tournament.match.again2 = false;
                sendBoth(tournament);
            }
            return;
        }
        arenas.reserve(next.name);
        Match match = tournaments.newMatch(tournament, next.name);
        setupMatch(tournament, match);
        message(tournament, ChatFormatting.AQUA + "Next match on arena " + ChatFormatting.WHITE + next.name
                + ChatFormatting.GRAY + " - inventories reset, press READY.");
        sendBoth(tournament);
    }

    /**
     * Called from the server-side block break hook. The winner write is guarded
     * by phase and a null winner, so two simultaneous breaks cannot both win.
     */
    public void onDiamondMined(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Tournament tournament = tournaments.find(player.getUUID());
        if (tournament == null || tournament.match == null) {
            return;
        }
        Match match = tournament.match;
        if (match.phase != MatchPhase.PLAYING || match.winner != null) {
            return;
        }
        match.winner = player.getUUID();
        match.durationMillis = Math.max(1L, System.currentTimeMillis() - match.startedAtMillis);

        String winnerName = tournament.nameOf(match.winner);
        String loserName = tournament.nameOf(tournament.other(match.winner));
        boolean seriesOver = tournaments.recordWin(tournament, match, match.winner,
                winnerName, loserName, match.durationMillis);

        if (seriesOver) {
            match.phase = MatchPhase.CHAMPION;
            tournament.finished = true;
            tournament.champion = match.winner;
            arenas.release(match.arena);
        } else {
            match.phase = MatchPhase.RESULT;
        }
        sendBoth(tournament);

        String result = ChatFormatting.AQUA + winnerName + ChatFormatting.WHITE + " mined the diamond in "
                + com.diamondrush.util.Format.clock(match.durationMillis) + "!";
        announce(tournament, result);
        broadcastServer(tournament, ChatFormatting.GOLD + "[Diamond Rush] " + ChatFormatting.WHITE
                + winnerName + " won game " + match.number + " (" + tournament.score1 + " - " + tournament.score2 + ")");
        if (seriesOver) {
            broadcastServer(tournament, ChatFormatting.GOLD + "[Diamond Rush] " + ChatFormatting.YELLOW
                    + winnerName + " IS THE CHAMPION! Final score " + tournament.score1 + " - " + tournament.score2);
        }
    }

    private void forfeit(ServerPlayer player, String reason) {
        Tournament tournament = tournaments.find(player.getUUID());
        if (tournament == null) {
            return;
        }
        ServerPlayer opponent = online(tournament, tournament.other(player.getUUID()));
        endTournamentWithForfeit(tournament, player.getUUID(), reason);
        if (opponent != null) {
            opponent.sendSystemMessage(Component.literal(ChatFormatting.RED
                    + player.getName().getString() + " " + reason + ". Tournament ended."));
            sendNone(opponent);
            inventories.reset(opponent);
        }
        sendNone(player);
    }

    /**
     * Handles a disconnect without leaving the opponent stuck in a dead match.
     */
    public void onPlayerDisconnect(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Tournament tournament = tournaments.find(player.getUUID());
        if (tournament == null) {
            return;
        }
        UUID opponentId = tournament.other(player.getUUID());
        ServerPlayer opponent = online(tournament, opponentId);
        Match match = tournament.match;
        boolean opponentShouldWinCurrent = match != null
                && (match.phase == MatchPhase.READY || match.phase == MatchPhase.COUNTDOWN || match.phase == MatchPhase.PLAYING)
                && match.winner == null;
        if (opponentShouldWinCurrent && opponent != null) {
            match.winner = opponentId;
            match.durationMillis = match.phase == MatchPhase.PLAYING
                    ? Math.max(1L, System.currentTimeMillis() - match.startedAtMillis)
                    : 0L;
            tournaments.recordWin(tournament, match, opponentId,
                    tournament.nameOf(opponentId), tournament.nameOf(player.getUUID()), match.durationMillis);
            match.phase = tournament.finished ? MatchPhase.CHAMPION : MatchPhase.RESULT;
            if (tournament.finished) {
                arenas.release(match.arena);
            }
        }
        endTournament(tournament, null);
        if (opponent != null) {
            opponent.sendSystemMessage(Component.literal(ChatFormatting.RED
                    + player.getName().getString() + " disconnected. Tournament ended."));
            inventories.reset(opponent);
            sendNone(opponent);
        }
    }

    private void endTournamentWithForfeit(Tournament tournament, UUID leaver, String reason) {
        Match match = tournament.match;
        ServerPlayer opponent = online(tournament, tournament.other(leaver));
        if (match != null && match.winner == null && opponent != null
                && (match.phase == MatchPhase.READY || match.phase == MatchPhase.COUNTDOWN || match.phase == MatchPhase.PLAYING)) {
            match.winner = opponent.getUUID();
            match.durationMillis = match.phase == MatchPhase.PLAYING
                    ? Math.max(1L, System.currentTimeMillis() - match.startedAtMillis)
                    : 0L;
            tournaments.recordWin(tournament, match, opponent.getUUID(),
                    tournament.nameOf(opponent.getUUID()), tournament.nameOf(leaver), match.durationMillis);
        }
        endTournament(tournament, null);
    }

    /** Called from ServerPlayerEvents.AFTER_RESPAWN. */
    public void onPlayerRespawn(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Tournament tournament = tournaments.find(player.getUUID());
        if (tournament == null || tournament.match == null) {
            return;
        }
        Arena arena = arenas.get(tournament.match.arena);
        if (arena == null) {
            return;
        }
        int slot = tournament.slotOf(player.getUUID());
        if (slot == 0) {
            return;
        }
        inventories.prepareForMatch(player);
        spawns.teleportToArena(player, arena, slot);
        sendState(player, tournament, tournament.match);
        player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                + "Death does not end the match - the timer keeps running."));
    }

    /** Called once per server tick. */
    public void tick(MinecraftServer server) {
        tickCounter++;
        for (Tournament tournament : tournaments.all()) {
            Match match = tournament.match;
            if (match == null) {
                continue;
            }
            switch (match.phase) {
                case READY -> keepInArena(tournament, match);
                case COUNTDOWN -> tickCountdown(server, tournament, match);
                // During PLAYING players roam freely; only death respawns pull
                // them back to their arena spawn.
                default -> {
                }
            }
        }
    }

    private void tickCountdown(MinecraftServer server, Tournament tournament, Match match) {
        keepInArena(tournament, match);
        match.countdownTicks--;
        if (match.countdownTicks <= 0) {
            match.phase = MatchPhase.PLAYING;
            match.startedAtMillis = System.currentTimeMillis();
            match.durationMillis = 0L;
            sendBoth(tournament);
            message(tournament, ChatFormatting.GREEN + "" + ChatFormatting.BOLD + "GO! "
                    + ChatFormatting.RESET + ChatFormatting.WHITE + "Mine a diamond first to win the game.");
        } else if (match.countdownTicks % 20 == 0) {
            sendBoth(tournament);
        }
    }

    private void keepInArena(Tournament tournament, Match match) {
        Arena arena = arenas.get(match.arena);
        if (arena == null) {
            return;
        }
        for (int slot = 1; slot <= 2; slot++) {
            ServerPlayer player = online(tournament, slot == 1 ? tournament.player1 : tournament.player2);
            if (player != null) {
                spawns.enforceSpawn(player, arena, slot);
            }
        }
    }

    /**
     * Sends the authoritative snapshot to a player, or a closed screen when they
     * are no longer part of a match.
     */
    public void sendState(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Tournament tournament = tournaments.find(player.getUUID());
        if (tournament == null || tournament.match == null) {
            sendNone(player);
            return;
        }
        sendState(player, tournament, tournament.match);
    }

    public void sendState(ServerPlayer player, Tournament tournament, Match match) {
        if (player == null || tournament == null || match == null) {
            return;
        }
        int slot = match.slotOf(player.getUUID());
        if (slot == 0) {
            sendNone(player);
            return;
        }
        boolean first = slot == 1;
        UiState state = switch (match.phase) {
            case READY -> UiState.READY;
            case COUNTDOWN -> UiState.COUNTDOWN;
            case PLAYING -> UiState.MATCH;
            case RESULT -> UiState.RESULT;
            case CHAMPION -> UiState.CHAMPION;
        };
        String headline = "";
        if (match.phase == MatchPhase.RESULT || match.phase == MatchPhase.CHAMPION) {
            UUID winner = match.winner;
            headline = winner == null ? "" : tournament.nameOf(winner);
        }
        String footer = switch (match.phase) {
            case READY -> "BOTH PLAYERS MUST CLICK READY";
            case COUNTDOWN -> "GET READY TO MINE";
            case PLAYING -> "FIRST DIAMOND WINS THE MATCH";
            case RESULT -> "BOTH PLAYERS MUST CLICK PLAY AGAIN";
            case CHAMPION -> "FINAL SCORE";
        };
        int countdown = match.phase == MatchPhase.COUNTDOWN
                ? Math.max(0, (match.countdownTicks + 19) / 20)
                : 0;
        NetworkHandler.send(player, new GuiStatePayload(
                state,
                first ? tournament.name1 : tournament.name2,
                first ? tournament.name2 : tournament.name1,
                first ? tournament.score1 : tournament.score2,
                first ? tournament.score2 : tournament.score1,
                (int) (match.elapsedMillis() / 1000L),
                countdown,
                match.isReady(slot),
                match.isReady(first ? 2 : 1),
                match.isAgain(slot),
                match.isAgain(first ? 2 : 1),
                headline,
                footer,
                match.arena
        ));
    }

    public void sendNone(ServerPlayer player) {
        NetworkHandler.send(player, GuiStatePayload.none());
    }

    private void sendBoth(Tournament tournament) {
        for (int slot = 1; slot <= 2; slot++) {
            ServerPlayer player = online(tournament, slot == 1 ? tournament.player1 : tournament.player2);
            if (player != null) {
                sendState(player, tournament, tournament.match);
            }
        }
        // Deliberately no client trust: state can only be produced here.
    }

    private ServerPlayer online(Tournament tournament, UUID uuid) {
        MinecraftServer server = com.diamondrush.DiamondRushMod.server();
        if (server == null || uuid == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(uuid);
    }

    private void message(Tournament tournament, String text) {
        Component component = Component.literal(ChatFormatting.GOLD + "[Diamond Rush] " + ChatFormatting.RESET + text);
        for (int slot = 1; slot <= 2; slot++) {
            ServerPlayer player = online(tournament, slot == 1 ? tournament.player1 : tournament.player2);
            if (player != null) {
                player.sendSystemMessage(component);
            }
        }
    }

    private void announce(Tournament tournament, String text) {
        message(tournament, text);
    }

    private void broadcastServer(Tournament tournament, String text) {
        MinecraftServer server = com.diamondrush.DiamondRushMod.server();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
        }
    }

    /** Ends a tournament and frees its arena. */
    public void endTournament(Tournament tournament, String reason) {
        if (tournament == null) {
            return;
        }
        if (tournament.match != null) {
            arenas.release(tournament.match.arena);
        }
        tournaments.remove(tournament);
        if (reason != null) {
            message(tournament, reason);
        }
        for (int slot = 1; slot <= 2; slot++) {
            ServerPlayer player = online(tournament, slot == 1 ? tournament.player1 : tournament.player2);
            if (player != null) {
                sendNone(player);
            }
        }
        statistics.save();
    }

    /**
     * Server shutdown cleanup: players must not carry match kits into the next
     * session, and arenas must be released.
     */
    public void shutdown() {
        for (Tournament tournament : List.copyOf(tournaments.all())) {
            for (int slot = 1; slot <= 2; slot++) {
                ServerPlayer player = online(tournament, slot == 1 ? tournament.player1 : tournament.player2);
                if (player != null) {
                    inventories.reset(player);
                    sendNone(player);
                }
            }
            if (tournament.match != null) {
                arenas.release(tournament.match.arena);
            }
        }
        tournaments.clear();
        statistics.save();
    }

    /** Admin stop: ends every active tournament. */
    public int stopAll() {
        List<Tournament> active = List.copyOf(tournaments.all());
        for (Tournament tournament : active) {
            ServerPlayer first = online(tournament, tournament.player1);
            ServerPlayer second = online(tournament, tournament.player2);
            if (first != null) {
                first.sendSystemMessage(Component.literal(ChatFormatting.RED + "The match was stopped by an admin."));
                inventories.reset(first);
                sendNone(first);
            }
            if (second != null) {
                second.sendSystemMessage(Component.literal(ChatFormatting.RED + "The match was stopped by an admin."));
                inventories.reset(second);
                sendNone(second);
            }
            endTournament(tournament, null);
        }
        tournaments.clear();
        return active.size();
    }

    public int neededWins() {
        return NEEDED_WINS;
    }
}
