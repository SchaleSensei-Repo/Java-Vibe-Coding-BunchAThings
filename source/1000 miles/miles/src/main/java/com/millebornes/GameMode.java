package com.millebornes;

public enum GameMode {
    SOLO("Solo (1 Player)"),
    TWO_PLAYER_HOTSEAT("2 Players (Hot Seat)");

    private final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}