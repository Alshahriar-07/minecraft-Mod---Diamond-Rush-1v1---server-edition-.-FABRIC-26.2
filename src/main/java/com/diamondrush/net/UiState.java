package com.diamondrush.net;

/**
 * Which screen the client should present. The server is the only authority on
 * this value; clients merely render it.
 */
public enum UiState {
    /** No screen: the player is not in a match. */
    NONE,
    WAITING,
    READY,
    COUNTDOWN,
    MATCH,
    RESULT,
    CHAMPION
}
