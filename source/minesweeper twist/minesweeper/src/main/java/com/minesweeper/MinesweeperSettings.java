package com.minesweeper;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.swing.JOptionPane;

public class MinesweeperSettings {
    private final String filename;
    private Properties config;
    private final Properties defaultSettings;

    public MinesweeperSettings(String filename) {
        this.filename = filename;
        this.config = new Properties();

        // Define default settings
        defaultSettings = new Properties();
        defaultSettings.setProperty("game.initial_points", "100");
        defaultSettings.setProperty("game.board_width", "10");
        defaultSettings.setProperty("game.board_height", "10");
        defaultSettings.setProperty("game.mines_mode", "random");
        defaultSettings.setProperty("game.mines_min", "10");
        defaultSettings.setProperty("game.mines_max", "15");
        defaultSettings.setProperty("game.mines_exact", "12");

        defaultSettings.setProperty("scoring.mine_discovery_mode", "random");
        defaultSettings.setProperty("scoring.mine_discovery_min", "-50");
        defaultSettings.setProperty("scoring.mine_discovery_max", "10");
        defaultSettings.setProperty("scoring.mine_discovery_exact", "-20");

        defaultSettings.setProperty("scoring.points_per_click_enabled", "false");
        defaultSettings.setProperty("scoring.points_per_click_mode", "random");
        defaultSettings.setProperty("scoring.points_per_click_min", "-5");
        defaultSettings.setProperty("scoring.points_per_click_max", "5");
        defaultSettings.setProperty("scoring.points_per_click_exact", "1");

        defaultSettings.setProperty("scoring.points_per_discovered_tile_enabled", "false");
        defaultSettings.setProperty("scoring.points_per_discovered_tile_mode", "random");
        defaultSettings.setProperty("scoring.points_per_discovered_tile_min", "1");
        defaultSettings.setProperty("scoring.points_per_discovered_tile_max", "5");
        defaultSettings.setProperty("scoring.points_per_discovered_tile_exact", "2");

        defaultSettings.setProperty("scoring.flag_placement_mode", "random");
        defaultSettings.setProperty("scoring.flag_placement_min", "-5");
        defaultSettings.setProperty("scoring.flag_placement_max", "5");
        defaultSettings.setProperty("scoring.flag_placement_exact", "0");

        loadSettings();
    }

    public void loadSettings() {
        try (FileInputStream fis = new FileInputStream(filename)) {
            config.load(fis);
            // After loading, merge with defaults to ensure all keys are present
            // This is important if new settings are added in a future version
            for (String key : defaultSettings.stringPropertyNames()) {
                if (!config.containsKey(key)) {
                    config.setProperty(key, defaultSettings.getProperty(key));
                }
            }
            saveSettings(); // Save any newly added defaults
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error reading settings file: " + e.getMessage() + "\nLoading default settings.", "Settings Error", JOptionPane.ERROR_MESSAGE);
            createDefaultSettings();
        }
    }

    private void createDefaultSettings() {
        config.clear();
        config.putAll(defaultSettings);
        saveSettings();
    }

    public void saveSettings() {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            config.store(fos, "Minesweeper Twist Game Settings");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving settings file: " + e.getMessage(), "Settings Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getSetting(String key) {
        // Return default if not found (though loadSettings tries to ensure all are present)
        return config.getProperty(key, defaultSettings.getProperty(key));
    }

    public int getSetting(String key, int defaultValue) {
        try {
            return Integer.parseInt(getSetting(key));
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse setting '" + key + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    public boolean getSetting(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(getSetting(key));
        } catch (Exception e) {
            System.err.println("Warning: Could not parse setting '" + key + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    public void setSetting(String key, String value) {
        config.setProperty(key, value);
        saveSettings(); // Save immediately after setting
    }
}