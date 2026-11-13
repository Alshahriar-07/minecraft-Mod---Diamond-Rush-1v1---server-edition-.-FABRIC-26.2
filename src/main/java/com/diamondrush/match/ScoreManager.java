package com.diamondrush.match;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The only place tournament scores are mutated. Each match number is awarded at
 * most once per tournament, so a duplicated packet, a second "diamond" event in
 * the same tick, or two simultaneous block breaks can never add two points.
 */
public final class ScoreManager {

    private final Set<String> awardedMatches = new HashSet<>();

    /**
     * Awards a match win. Returns true when the score actually changed.
     */
    public boolean awardMatchWin(Tournament tournament, UUID winner, int matchNumber) {
        if (tournament == null || winner == null || !tournament.contains(winner)) {
            return false;
        }
        String token = tournament.id + "#" + matchNumber;
        if (!awardedMatches.add(token)) {
            return false;
        }
        if (winner.equals(tournament.player1)) {
            tournament.score1++;
        } else {
            tournament.score2++;
        }
        return true;
    }

    public void forget(Tournament tournament) {
        if (tournament != null) {
            awardedMatches.removeIf(token -> token.startsWith(tournament.id + "#"));
        }
    }
}
