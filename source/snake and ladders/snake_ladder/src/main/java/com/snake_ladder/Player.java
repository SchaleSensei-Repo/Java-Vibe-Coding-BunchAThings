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
    private GameSettings gameSettingsRef; // Reference to game settings
    private int eliminationOrder = Integer.MAX_VALUE;

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
        this.gameSettingsRef = settings; // Store settings reference
    }

    public String getName() { return name; }
    public int getPoints() { return points; }
    public void addPoints(int amount) { this.points += amount; }

    // Simplified: just subtracts points. GameLogic handles penalties/clamping.
    public void takePoints(int amount) {
        this.points -= amount;
    }
    
    // Method to directly set points to a value (e.g., 0)
    public void setPoints(int newPoints) {
        this.points = newPoints;
    }
    
    public void resetPoints() { this.points = initialPoints; } // Resets to initial configured points
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

    public void setEliminated(boolean eliminated, int eliminationOrderNumber) {
        this.isEliminated = eliminated;
        if (eliminated) {
            if (this.eliminationOrder == Integer.MAX_VALUE) { // Only set if not already set
                 this.eliminationOrder = eliminationOrderNumber;
            }
        } else {
            this.eliminationOrder = Integer.MAX_VALUE;
        }
    }
    
    public void setEliminated(boolean eliminated) { // Overload for simpler calls if order not relevant
        this.isEliminated = eliminated;
        if (!eliminated) {
            this.eliminationOrder = Integer.MAX_VALUE;
        }
    }
    
    public int getEliminationOrder() {
        return eliminationOrder;
    }

    public void respawn() {
        this.currentTileIndex = 0;
        this.points = initialPoints;
        this.lives = initialLives;
        this.isEliminated = false;
        this.eliminationOrder = Integer.MAX_VALUE;
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