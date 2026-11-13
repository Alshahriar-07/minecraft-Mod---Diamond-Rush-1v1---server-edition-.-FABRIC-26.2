package com.diamondrush.match;

import java.util.UUID;

/**
 * A best-of-five series between two players. First to three match wins takes
 * the tournament.
 */
public final class Tournament {

    public static final int WINS_REQUIRED = 3;

    public final UUID id = UUID.randomUUID();
    public final UUID player1;
    public final UUID player2;
    public String name1;
    public String name2;

    public int score1;
    public int score2;
    public boolean finished;
    public UUID champion;
    public Match match;

    public Tournament(UUID player1, UUID player2, String name1, String name2) {
        this.player1 = player1;
        this.player2 = player2;
        this.name1 = name1;
        this.name2 = name2;
    }

    public boolean contains(UUID player) {
        return player != null && (player.equals(player1) || player.equals(player2));
    }

    public int slotOf(UUID player) {
        if (player == null) {
            return 0;
        }
        if (player.equals(player1)) {
            return 1;
        }
        return player.equals(player2) ? 2 : 0;
    }

    public UUID other(UUID player) {
        if (player == null) {
            return null;
        }
        if (player.equals(player1)) {
            return player2;
        }
        return player.equals(player2) ? player1 : null;
    }

    public String nameOf(UUID player) {
        if (player == null) {
            return "?";
        }
        if (player.equals(player1)) {
            return name1;
        }
        return player.equals(player2) ? name2 : "?";
    }

    public int scoreOf(UUID player) {
        if (player == null) {
            return 0;
        }
        if (player.equals(player1)) {
            return score1;
        }
        return player.equals(player2) ? score2 : 0;
    }

    public boolean hasWinner() {
        return score1 >= WINS_REQUIRED || score2 >= WINS_REQUIRED;
    }
}
