package com.diamondrush.match;

import java.util.UUID;

/**
 * Mutable state of one game inside a tournament. All access happens on the
 * server thread, so the simple fields are safe and the winner check stays atomic.
 */
public final class Match {

    public final Tournament tournament;
    public final UUID player1;
    public final UUID player2;
    public final int number;
    public final String arena;

    public MatchPhase phase = MatchPhase.READY;
    public boolean ready1;
    public boolean ready2;
    public boolean again1;
    public boolean again2;

    public int countdownTicks;
    public long startedAtMillis;
    public long durationMillis;
    public UUID winner;

    public Match(Tournament tournament, int number, String arena) {
        this.tournament = tournament;
        this.player1 = tournament.player1;
        this.player2 = tournament.player2;
        this.number = number;
        this.arena = arena;
    }

    public boolean contains(UUID player) {
        return player != null && (player.equals(player1) || player.equals(player2));
    }

    /** Returns 1 for the first player, 2 for the second, 0 when not a member. */
    public int slotOf(UUID player) {
        if (player == null) {
            return 0;
        }
        if (player.equals(player1)) {
            return 1;
        }
        return player.equals(player2) ? 2 : 0;
    }

    public UUID opponentOf(UUID player) {
        if (player == null) {
            return null;
        }
        if (player.equals(player1)) {
            return player2;
        }
        return player.equals(player2) ? player1 : null;
    }

    public boolean isReady(int slot) {
        return slot == 1 ? ready1 : ready2;
    }

    public void setReady(int slot, boolean ready) {
        if (slot == 1) {
            ready1 = ready;
        } else if (slot == 2) {
            ready2 = ready;
        }
    }

    public boolean isReadyToPlay(int slot) {
        return slot == 1 ? ready1 : ready2;
    }

    public boolean isAgain(int slot) {
        return slot == 1 ? again1 : again2;
    }

    public void setAgain(int slot, boolean value) {
        if (slot == 1) {
            again1 = value;
        } else if (slot == 2) {
            again2 = value;
        }
    }

    public boolean isFinished() {
        return phase == MatchPhase.RESULT || phase == MatchPhase.CHAMPION;
    }

    public long elapsedMillis() {
        if (phase == MatchPhase.PLAYING) {
            return System.currentTimeMillis() - startedAtMillis;
        }
        return durationMillis;
    }

    public String describe() {
        return "Match " + number + " on " + arena + " (" + phase + ")";
    }
}
