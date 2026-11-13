package com.diamondrush.net;

/**
 * Actions a client may request. Every action is validated again on the server;
 * nothing here is trusted.
 */
public enum GuiAction {
    READY,
    PLAY_AGAIN,
    NEW_TOURNAMENT,
    LEAVE
}
