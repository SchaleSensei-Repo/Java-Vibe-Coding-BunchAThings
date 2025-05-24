package com.snake_ladder;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.Locale;

public class Player {
    private String name;
    private int points;
    private int lives;
    private int currentTileIndex;
    private boolean isHuman;
    private Color color;
    private boolean isEliminated;
    private int initialLives;
    private int initialPoints;
    private GameSettings gameSettings;
    private int eliminationOrder = Integer.MAX_VALUE; // Higher value means not eliminated or eliminated later

    public Player(String name, int initialPoints, int initialLives, boolean isHuman, Color color, GameSettings settings) {
        this.name = name;
        this.points = initialPoints;
        this.lives = initialLives;
        this.initialLives = initialLives;
        this.initialPoints = initialPoints;
        this.currentTileIndex = 0;
        this.isHuman = isHuman;
        this.color = color;
        this.isEliminated = false;
        this.gameSettings = settings;
    }

    public String getName() { return name; }
    public int getPoints() { return points; }
    public void addPoints(int amount) { this.points += amount; }

    public void takePoints(int amount) {
        this.points -= amount;
        if (gameSettings != null && !gameSettings.allowNegativePoints) {
            if (this.points < 0) {
                this.points = 0;
            }
        }
    }
    
    public void resetPoints() { this.points = initialPoints; }
    public int getLives() { return lives; }
    public void addLife() { this.lives++; }

    public void takeLife() {
        this.lives--;
        if (this.lives < 0) this.lives = 0;
    }
    
    public void resetLives() { this.lives = initialLives; }
    public int getCurrentTileIndex() { return currentTileIndex; }
    public int getCurrentTileNumber() { return currentTileIndex + 1; }
    public void setCurrentTileIndex(int currentTileIndex) { this.currentTileIndex = currentTileIndex; }
    public boolean isHuman() { return isHuman; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    public boolean isEliminated() { return isEliminated; }

    // Modified to accept elimination order
    public void setEliminated(boolean eliminated, int eliminationOrderNumber) {
        this.isEliminated = eliminated;
        if (eliminated) {
            // Only set if not already set to a lower (earlier) order.
            // This prevents overwriting an earlier elimination if somehow called multiple times for the same elimination event.
            if (this.eliminationOrder == Integer.MAX_VALUE) {
                 this.eliminationOrder = eliminationOrderNumber;
            }
        } else {
            this.eliminationOrder = Integer.MAX_VALUE; // Reset if un-eliminated (e.g., respawn)
        }
    }
    
    // Overload for simple setEliminated(true) without explicit order (e.g. initial state if needed)
    // However, game logic should always use the one with order for actual elimination events.
    public void setEliminated(boolean eliminated) {
        this.isEliminated = eliminated;
        if (!eliminated) {
            this.eliminationOrder = Integer.MAX_VALUE;
        }
        // If setting to true without an order, it implies it's not part of the game's sequence.
        // For actual game eliminations, the GameLogic should call the version with eliminationOrderNumber.
    }
    
    public int getEliminationOrder() {
        return eliminationOrder;
    }

    public void respawn() {
        this.currentTileIndex = 0;
        this.points = initialPoints;
        this.lives = initialLives;
        this.isEliminated = false;
        this.eliminationOrder = Integer.MAX_VALUE; // Reset on respawn
    }

    public String getFormattedPoints() {
        return NumberFormat.getNumberInstance(Locale.US).format(points);
    }

    @Override
    public String toString() {
        return name + " (Tile: " + getCurrentTileNumber() + 
               ", Pts: " + getFormattedPoints() + 
               ", Lives: " + lives + 
               (isEliminated ? " [ELIMINATED])" : ")");
    }
}