package com.diamondrush.stats;

/**
 * Persisted per-player statistics. Public fields keep the Gson format stable
 * and easy to edit by hand.
 */
public final class PlayerStats {

    public String uuid = "";
    public String name = "";
    public int matchWins;
    public int matchesPlayed;
    public int tournamentWins;
    public long fastestMatchMillis = -1L;

    public PlayerStats() {
    }

    public PlayerStats(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public void recordMatchWin(long durationMillis) {
        matchWins++;
        matchesPlayed++;
        if (durationMillis > 0L && (fastestMatchMillis <= 0L || durationMillis < fastestMatchMillis)) {
            fastestMatchMillis = durationMillis;
        }
    }

    public void recordMatchLoss() {
        matchesPlayed++;
    }

    public String summary() {
        String fastest = fastestMatchMillis <= 0L
                ? "n/a"
                : com.diamondrush.util.Format.clock(fastestMatchMillis);
        return name + " | Match wins: " + matchWins
                + " | Matches: " + matchesPlayed
                + " | Tournaments won: " + tournamentWins
                + " | Fastest win: " + fastest;
    }
}
