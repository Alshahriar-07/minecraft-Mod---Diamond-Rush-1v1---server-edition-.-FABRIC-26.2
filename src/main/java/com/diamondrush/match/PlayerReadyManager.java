package com.diamondrush.match;

import java.util.UUID;

/**
 * Validates and records the two independent confirmations required by the
 * design: READY before a game, PLAY AGAIN after a game, NEW TOURNAMENT after a
 * champion. Server-side validation happens here, never on the client.
 */
public final class PlayerReadyManager {

    /**
     * Marks a player ready. Returns true only when the request was valid and
     * this player had not already pressed the button.
     */
    public boolean markReady(Match match, UUID player) {
        if (match == null || match.phase != MatchPhase.READY) {
            return false;
        }
        int slot = match.slotOf(player);
        if (slot == 0 || match.isReady(slot)) {
            return false;
        }
        match.setReady(slot, true);
        return true;
    }

    public boolean bothReady(Match match) {
        return match != null && match.ready1 && match.ready2;
    }

    public boolean markPlayAgain(Match match, UUID player) {
        if (match == null || match.phase != MatchPhase.RESULT) {
            return false;
        }
        int slot = match.slotOf(player);
        if (slot == 0 || match.isAgain(slot)) {
            return false;
        }
        match.setAgain(slot, true);
        return true;
    }

    public boolean markNewTournament(Match match, UUID player) {
        if (match == null || match.phase != MatchPhase.CHAMPION) {
            return false;
        }
        int slot = match.slotOf(player);
        if (slot == 0 || match.isAgain(slot)) {
            return false;
        }
        match.setAgain(slot, true);
        return true;
    }

    public boolean bothAgain(Match match) {
        return match != null && match.again1 && match.again2;
    }
}
