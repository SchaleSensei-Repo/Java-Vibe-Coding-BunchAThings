package com.deal.model;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class GameSettings {
    private static final String SETTINGS_FILE = "deal_settings.ini";

    private int numberOfBags = 26;
    private int minBagValue = 1;
    private int maxBagValue = 1000000;
    private int minOfferValue = 5000;
    private int maxOfferValue = 750000;
    private int offerRoundFrequency = 3;
    private int trackingMode = 0; // 0: None, 1: Full, 2: Value Only

    public GameSettings() {
        loadSettings();
    }

    public void loadSettings() {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(SETTINGS_FILE)) {
            properties.load(reader);
            this.numberOfBags = Integer.parseInt(properties.getProperty("numberOfBags", String.valueOf(this.numberOfBags)));
            this.minBagValue = Integer.parseInt(properties.getProperty("minBagValue", String.valueOf(this.minBagValue)));
            this.maxBagValue = Integer.parseInt(properties.getProperty("maxBagValue", String.valueOf(this.maxBagValue)));
            this.minOfferValue = Integer.parseInt(properties.getProperty("minOfferValue", String.valueOf(this.minOfferValue)));
            this.maxOfferValue = Integer.parseInt(properties.getProperty("maxOfferValue", String.valueOf(this.maxOfferValue)));
            this.offerRoundFrequency = Integer.parseInt(properties.getProperty("offerRoundFrequency", String.valueOf(this.offerRoundFrequency)));
            this.trackingMode = Integer.parseInt(properties.getProperty("trackingMode", String.valueOf(this.trackingMode)));
        } catch (IOException | NumberFormatException e) {
            System.err.println("Could not load settings from " + SETTINGS_FILE + ". Using default values.");
            saveSettings(); // Save defaults if loading failed (e.g., file not found or malformed)
        }
    }

    public void saveSettings() {
        Properties properties = new Properties();
        properties.setProperty("numberOfBags", String.valueOf(this.numberOfBags));
        properties.setProperty("minBagValue", String.valueOf(this.minBagValue));
        properties.setProperty("maxBagValue", String.valueOf(this.maxBagValue));
        properties.setProperty("minOfferValue", String.valueOf(this.minOfferValue));
        properties.setProperty("maxOfferValue", String.valueOf(this.maxOfferValue));
        properties.setProperty("offerRoundFrequency", String.valueOf(this.offerRoundFrequency));
        properties.setProperty("trackingMode", String.valueOf(this.trackingMode));

        try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
            properties.store(writer, "Deal or No Deal Game Settings");
            System.out.println("Settings saved to " + SETTINGS_FILE);
        } catch (IOException e) {
            System.err.println("Could not save settings to " + SETTINGS_FILE + ". Error: " + e.getMessage());
        }
    }

    // --- Getters and Setters ---
    public int getNumberOfBags() { return numberOfBags; }
    public void setNumberOfBags(int numberOfBags) { this.numberOfBags = numberOfBags; }

    public int getMinBagValue() { return minBagValue; }
    public void setMinBagValue(int minBagValue) { this.minBagValue = minBagValue; }

    public int getMaxBagValue() { return maxBagValue; }
    public void setMaxBagValue(int maxBagValue) { this.maxBagValue = maxBagValue; }

    public int getMinOfferValue() { return minOfferValue; }
    public void setMinOfferValue(int minOfferValue) { this.minOfferValue = minOfferValue; }

    public int getMaxOfferValue() { return maxOfferValue; }
    public void setMaxOfferValue(int maxOfferValue) { this.maxOfferValue = maxOfferValue; }

    public int getOfferRoundFrequency() { return offerRoundFrequency; }
    public void setOfferRoundFrequency(int offerRoundFrequency) { this.offerRoundFrequency = offerRoundFrequency; }

    public int getTrackingMode() { return trackingMode; }
    public void setTrackingMode(int trackingMode) { this.trackingMode = trackingMode; }
}