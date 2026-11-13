package com.diamondrush.match;

/**
 * Lifecycle of a single best-of-five game.
 */
public enum MatchPhase {
    /** Both players are in the arena and must independently press READY. */
    READY,
    /** Three second countdown, players frozen at their spawns. */
    COUNTDOWN,
    /** Live game: the first player to mine a diamond wins. */
    PLAYING,
    /** Someone won this game; both players must press PLAY AGAIN. */
    RESULT,
    /** A player reached three wins; the tournament is over. */
    CHAMPION
}
