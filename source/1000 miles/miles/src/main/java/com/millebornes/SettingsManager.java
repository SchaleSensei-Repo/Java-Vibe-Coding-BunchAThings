package com.millebornes;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SettingsManager {
    private Properties settings;
    private String filename;

    private int goalDistance;
    private boolean redrawEmptyHand;
    private boolean reshuffleEmptyPile;
    private GameMode gameMode;

    public SettingsManager(String filename) {
        this.filename = filename;
        this.settings = new Properties();
        loadSettings();
    }

    public void loadSettings() {
        try (FileInputStream fis = new FileInputStream(filename)) {
            settings.load(fis);
        } catch (IOException e) {
            // File not found or other IO error, load defaults
            System.out.println("Settings file not found or error loading, using defaults.");
        }

        goalDistance = Integer.parseInt(settings.getProperty("goal_distance", "1000"));
        redrawEmptyHand = Boolean.parseBoolean(settings.getProperty("redraw_empty_hand", "true"));
        reshuffleEmptyPile = Boolean.parseBoolean(settings.getProperty("reshuffle_empty_pile", "true"));
        
        String modeStr = settings.getProperty("game_mode", GameMode.TWO_PLAYER_HOTSEAT.name());
        try {
            gameMode = GameMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            gameMode = GameMode.TWO_PLAYER_HOTSEAT; // Fallback to default if invalid
        }
    }

    public void saveSettings() {
        settings.setProperty("goal_distance", String.valueOf(goalDistance));
        settings.setProperty("redraw_empty_hand", String.valueOf(redrawEmptyHand));
        settings.setProperty("reshuffle_empty_pile", String.valueOf(reshuffleEmptyPile));
        settings.setProperty("game_mode", gameMode.name());

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            settings.store(fos, "Mille Bornes Game Settings");
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    // Getters
    public int getGoalDistance() {
        return goalDistance;
    }

    public boolean isRedrawEmptyHand() {
        return redrawEmptyHand;
    }

    public boolean isReshuffleEmptyPile() {
        return reshuffleEmptyPile;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    // Setters (used by SettingsDialog)
    public void setGoalDistance(int goalDistance) {
        this.goalDistance = goalDistance;
    }

    public void setRedrawEmptyHand(boolean redrawEmptyHand) {
        this.redrawEmptyHand = redrawEmptyHand;
    }

    public void setReshuffleEmptyPile(boolean reshuffleEmptyPile) {
        this.reshuffleEmptyPile = reshuffleEmptyPile;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }
}