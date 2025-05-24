package com.snake_ladder;

public enum TileEffect {
    NORMAL("Normal Tile"),
    WARP_FORWARD("Warp Forward"),
    WARP_BACKWARD("Warp Backward"),
    GIVE_POINTS("Give Points"),
    TAKE_POINTS("Take Away Points"),
    GIVE_LIFE("Give Life"),
    TAKE_LIFE("Take Away Life"),
    GO_TO_START("Go to Start"),
    HARSH_GO_TO_START("Harsh Go to Start"),
    RANDOM_EFFECT("Random Effect"), // This one!
    START("Start Tile"), 
    FINISH("Finish Tile"); 

    private final String displayName;

    TileEffect(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Get effects that can be randomly assigned or appear on the board during generation
    public static TileEffect[] getRandomizableEffects() {
        return new TileEffect[]{
                NORMAL, WARP_FORWARD, WARP_BACKWARD, GIVE_POINTS, TAKE_POINTS,
                GIVE_LIFE, TAKE_LIFE, GO_TO_START, HARSH_GO_TO_START,
                RANDOM_EFFECT // ADDED RANDOM_EFFECT HERE
        };
    }

    // Get effects that can be chosen by the RANDOM_EFFECT tile itself
    public static TileEffect[] getEffectsForRandomTile() {
         return new TileEffect[]{ // These are the effects the RANDOM tile can *become*
                WARP_FORWARD, WARP_BACKWARD, GIVE_POINTS, TAKE_POINTS,
                GIVE_LIFE, TAKE_LIFE, GO_TO_START, HARSH_GO_TO_START, NORMAL
        };
    }
}