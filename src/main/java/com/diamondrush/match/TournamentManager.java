package com.diamondrush.match;

import com.diamondrush.stats.StatisticsManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registry of active tournaments plus the tournament-level rules (first to
 * three, champion recording). Keeping this separate from {@link MatchManager}
 * means match mechanics and series rules can be reasoned about independently.
 */
public final class TournamentManager {

    private final Map<UUID, Tournament> byPlayer = new HashMap<>();
    private final Map<UUID, Tournament> byId = new HashMap<>();
    private final List<Tournament> ordered = new ArrayList<>();
    private final ScoreManager scores;
    private final StatisticsManager statistics;

    public TournamentManager(ScoreManager scores, StatisticsManager statistics) {
        this.scores = scores;
        this.statistics = statistics;
    }

    public Tournament create(ServerPlayer player1, ServerPlayer player2) {
        Tournament tournament = new Tournament(
                player1.getUUID(),
                player2.getUUID(),
                player1.getName().getString(),
                player2.getName().getString()
        );
        byPlayer.put(tournament.player1, tournament);
        byPlayer.put(tournament.player2, tournament);
        byId.put(tournament.id, tournament);
        ordered.add(tournament);
        return tournament;
    }

    public Tournament find(UUID player) {
        return player == null ? null : byPlayer.get(player);
    }

    public Tournament byId(UUID id) {
        return id == null ? null : byId.get(id);
    }

    public Collection<Tournament> all() {
        return List.copyOf(ordered);
    }

    public int activeCount() {
        return ordered.size();
    }

    /**
     * Creates the next game in the series. Match numbers start at one and are
     * derived from the current score, which also keeps the score token unique.
     */
    public Match newMatch(Tournament tournament, String arena) {
        int number = tournament.score1 + tournament.score2 + 1;
        Match match = new Match(tournament, number, arena);
        tournament.match = match;
        return match;
    }

    /**
     * Records a match win exactly once, updates statistics and closes the
     * tournament when a player reaches three wins.
     *
     * @return true when the series is over
     */
    public boolean recordWin(Tournament tournament, Match match, UUID winner,
                             String winnerName, String loserName, long durationMillis) {
        if (!scores.awardMatchWin(tournament, winner, match.number)) {
            return tournament.finished;
        }
        UUID loser = tournament.other(winner);
        if (loser == null) {
            return tournament.finished;
        }
        statistics.recordMatchWin(winner, winnerName, loser, loserName, durationMillis);
        if (tournament.hasWinner()) {
            tournament.finished = true;
            tournament.champion = winner;
            statistics.recordTournamentWin(winner, winnerName);
            return true;
        }
        return false;
    }

    public void remove(Tournament tournament) {
        if (tournament == null) {
            return;
        }
        byPlayer.remove(tournament.player1, tournament);
        byPlayer.remove(tournament.player2, tournament);
        byId.remove(tournament.id);
        ordered.remove(tournament);
        scores.forget(tournament);
    }

    public void clear() {
        byPlayer.clear();
        byId.clear();
        ordered.clear();
    }
}
