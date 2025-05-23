package com.battleship;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections; // Added for unmodifiableMap
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class GameSettings {
    private static final String SETTINGS_FILE = "battleship_settings.ini";

    // Record for ship configuration (Java 16+)
    public record ShipConfig(int length, int quantity) {}

    private Properties config;
    private Map<String, String> defaultBoardSettings;
    private Map<String, String> defaultShipSettings; // This field was private
    private Map<String, String> defaultGameplaySettings;

    // Attributes derived from settings
    public int boardRows;
    public int boardCols;
    public Map<String, ShipConfig> shipConfigs;
    public int firesPerTurn;
    public boolean dynamicFires;

    public GameSettings() {
        this.config = new Properties();

        // Initialize default settings maps
        defaultBoardSettings = new HashMap<>();
        defaultBoardSettings.put("rows", "10");
        defaultBoardSettings.put("cols", "10");

        defaultShipSettings = new HashMap<>();
        defaultShipSettings.put("carrier", "5,1");
        defaultShipSettings.put("battleship", "4,1");
        defaultShipSettings.put("cruiser", "3,1");
        defaultShipSettings.put("submarine", "3,1");
        defaultShipSettings.put("destroyer", "2,1");

        defaultGameplaySettings = new HashMap<>();
        defaultGameplaySettings.put("fires_per_turn", "1");
        defaultGameplaySettings.put("dynamic_fires", "False");

        // FIX: Initialize shipConfigs map immediately to prevent NullPointerException
        this.shipConfigs = new HashMap<>();

        loadSettings();
    }

    // FIX: Public getter for defaultShipSettings
    public Map<String, String> getDefaultShipSettings() {
        return Collections.unmodifiableMap(defaultShipSettings); // Return an unmodifiable map for safety
    }

    public void loadSettings() {
        boolean fileExists = new java.io.File(SETTINGS_FILE).exists();

        if (fileExists) {
            try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
                config.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading settings file: " + e.getMessage());
                resetToDefaultInternal();
            }
        } else {
            resetToDefaultInternal();
        }
        applySettingsToAttributes();
    }

    private void resetToDefaultInternal() {
        config.clear();
        for (Map.Entry<String, String> entry : defaultBoardSettings.entrySet()) {
            config.setProperty("board." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : defaultShipSettings.entrySet()) {
            config.setProperty("ships." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : defaultGameplaySettings.entrySet()) {
            config.setProperty("gameplay." + entry.getKey(), entry.getValue());
        }
        applySettingsToAttributes();
        saveSettings();
    }

    public void resetToDefault() {
        resetToDefaultInternal();
    }

    private void applySettingsToAttributes() {
        boardRows = Integer.parseInt(config.getProperty("board.rows", defaultBoardSettings.get("rows")));
        boardCols = Integer.parseInt(config.getProperty("board.cols", defaultBoardSettings.get("cols")));

        shipConfigs.clear();
        for (Map.Entry<String, String> entry : defaultShipSettings.entrySet()) {
            String shipType = entry.getKey();
            String valueStr = config.getProperty("ships." + shipType, entry.getValue());
            try {
                String[] parts = valueStr.split(",");
                int length = Integer.parseInt(parts[0].trim());
                int quantity = Integer.parseInt(parts[1].trim());
                if (length < 0 || quantity < 0) {
                    throw new IllegalArgumentException("Invalid length or quantity.");
                }
                shipConfigs.put(shipType, new ShipConfig(length, quantity));
            } catch (Exception e) {
                System.err.println("Warning: Invalid ship config for " + shipType + ": '" + valueStr + "'. Using default.");
                String[] defaultParts = entry.getValue().split(",");
                shipConfigs.put(shipType, new ShipConfig(Integer.parseInt(defaultParts[0]), Integer.parseInt(defaultParts[1])));
            }
        }

        firesPerTurn = Integer.parseInt(config.getProperty("gameplay.fires_per_turn", defaultGameplaySettings.get("fires_per_turn")));
        dynamicFires = Boolean.parseBoolean(config.getProperty("gameplay.dynamic_fires", defaultGameplaySettings.get("dynamic_fires")));
    }

    public void saveSettings() {
        config.setProperty("board.rows", String.valueOf(boardRows));
        config.setProperty("board.cols", String.valueOf(boardCols));

        for (Map.Entry<String, ShipConfig> entry : shipConfigs.entrySet()) {
            config.setProperty("ships." + entry.getKey(), entry.getValue().length() + "," + entry.getValue().quantity());
        }

        config.setProperty("gameplay.fires_per_turn", String.valueOf(firesPerTurn));
        config.setProperty("gameplay.dynamic_fires", String.valueOf(dynamicFires));

        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            config.store(fos, "Battleship Game Settings");
        } catch (IOException e) {
            System.err.println("Error saving settings file: " + e.getMessage());
        }
    }
}