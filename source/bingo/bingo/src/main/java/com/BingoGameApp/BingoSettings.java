package com.BingoGameApp;
import java.io.*;
import java.util.Properties;

public class BingoSettings {
    private static final String SETTINGS_FILE = "bingo_settings.ini";
    private Properties properties;

    // Default values
    private int playerCount = 1; // 1 for solo, 2 for two players
    private int numbersPerGeneration = 1;

    // Points for marking a matching number on card (from previous version)
    private boolean randomizePoints = true;
    private int minRandomPoints = 5;
    private int maxRandomPoints = 20;
    private int exactPoints = 10;

    // NEW: Points for clicking Generate button
    private boolean awardPointsOnGenerate = false; // Default: No points on generate
    private boolean randomizeGeneratePoints = true;
    private int minGeneratePoints = -5; // Can be negative
    private int maxGeneratePoints = 5;
    private int exactGeneratePoints = 0; // Can be negative

    // NEW: Additional points for scoring a Bingo
    private boolean awardPointsOnBingo = true; // Default: Yes, award points on bingo
    private boolean randomizeBingoPoints = true;
    private int minBingoPoints = 50;
    private int maxBingoPoints = 100;
    private int exactBingoPoints = 75;

    private int minCardNumber = 1;
    private int maxCardNumber = 75;
    private boolean autoSelectNumbers = false; // Default to manual selection
    private int bingosNeededToWin = 1; // Default to 1 bingo to win

    public BingoSettings() {
        properties = new Properties();
        loadSettings();
    }

    private void loadSettings() {
        try (InputStream input = new FileInputStream(SETTINGS_FILE)) {
            properties.load(input);
            playerCount = Integer.parseInt(properties.getProperty("playerCount", String.valueOf(playerCount)));
            numbersPerGeneration = Integer.parseInt(properties.getProperty("numbersPerGeneration", String.valueOf(numbersPerGeneration)));
            randomizePoints = Boolean.parseBoolean(properties.getProperty("randomizePoints", String.valueOf(randomizePoints)));
            minRandomPoints = Integer.parseInt(properties.getProperty("minRandomPoints", String.valueOf(minRandomPoints)));
            maxRandomPoints = Integer.parseInt(properties.getProperty("maxRandomPoints", String.valueOf(maxRandomPoints)));
            exactPoints = Integer.parseInt(properties.getProperty("exactPoints", String.valueOf(exactPoints)));

            // NEW: Load Generate Button Points
            awardPointsOnGenerate = Boolean.parseBoolean(properties.getProperty("awardPointsOnGenerate", String.valueOf(awardPointsOnGenerate)));
            randomizeGeneratePoints = Boolean.parseBoolean(properties.getProperty("randomizeGeneratePoints", String.valueOf(randomizeGeneratePoints)));
            minGeneratePoints = Integer.parseInt(properties.getProperty("minGeneratePoints", String.valueOf(minGeneratePoints)));
            maxGeneratePoints = Integer.parseInt(properties.getProperty("maxGeneratePoints", String.valueOf(maxGeneratePoints)));
            exactGeneratePoints = Integer.parseInt(properties.getProperty("exactGeneratePoints", String.valueOf(exactGeneratePoints)));

            // NEW: Load Bingo Bonus Points
            awardPointsOnBingo = Boolean.parseBoolean(properties.getProperty("awardPointsOnBingo", String.valueOf(awardPointsOnBingo)));
            randomizeBingoPoints = Boolean.parseBoolean(properties.getProperty("randomizeBingoPoints", String.valueOf(randomizeBingoPoints)));
            minBingoPoints = Integer.parseInt(properties.getProperty("minBingoPoints", String.valueOf(minBingoPoints)));
            maxBingoPoints = Integer.parseInt(properties.getProperty("maxBingoPoints", String.valueOf(maxBingoPoints)));
            exactBingoPoints = Integer.parseInt(properties.getProperty("exactBingoPoints", String.valueOf(exactBingoPoints)));

            minCardNumber = Integer.parseInt(properties.getProperty("minCardNumber", String.valueOf(minCardNumber)));
            maxCardNumber = Integer.parseInt(properties.getProperty("maxCardNumber", String.valueOf(maxCardNumber)));
            autoSelectNumbers = Boolean.parseBoolean(properties.getProperty("autoSelectNumbers", String.valueOf(autoSelectNumbers)));
            bingosNeededToWin = Integer.parseInt(properties.getProperty("bingosNeededToWin", String.valueOf(bingosNeededToWin)));

            System.out.println("Settings loaded from " + SETTINGS_FILE);
        } catch (FileNotFoundException e) {
            System.out.println("Settings file not found. Using default settings.");
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading settings: " + e.getMessage());
        }
    }

    public void saveSettings() {
        properties.setProperty("playerCount", String.valueOf(playerCount));
        properties.setProperty("numbersPerGeneration", String.valueOf(numbersPerGeneration));
        properties.setProperty("randomizePoints", String.valueOf(randomizePoints));
        properties.setProperty("minRandomPoints", String.valueOf(minRandomPoints));
        properties.setProperty("maxRandomPoints", String.valueOf(maxRandomPoints));
        properties.setProperty("exactPoints", String.valueOf(exactPoints));

        // NEW: Save Generate Button Points
        properties.setProperty("awardPointsOnGenerate", String.valueOf(awardPointsOnGenerate));
        properties.setProperty("randomizeGeneratePoints", String.valueOf(randomizeGeneratePoints));
        properties.setProperty("minGeneratePoints", String.valueOf(minGeneratePoints));
        properties.setProperty("maxGeneratePoints", String.valueOf(maxGeneratePoints));
        properties.setProperty("exactGeneratePoints", String.valueOf(exactGeneratePoints));

        // NEW: Save Bingo Bonus Points
        properties.setProperty("awardPointsOnBingo", String.valueOf(awardPointsOnBingo));
        properties.setProperty("randomizeBingoPoints", String.valueOf(randomizeBingoPoints));
        properties.setProperty("minBingoPoints", String.valueOf(minBingoPoints));
        properties.setProperty("maxBingoPoints", String.valueOf(maxBingoPoints));
        properties.setProperty("exactBingoPoints", String.valueOf(exactBingoPoints));

        properties.setProperty("minCardNumber", String.valueOf(minCardNumber));
        properties.setProperty("maxCardNumber", String.valueOf(maxCardNumber));
        properties.setProperty("autoSelectNumbers", String.valueOf(autoSelectNumbers));
        properties.setProperty("bingosNeededToWin", String.valueOf(bingosNeededToWin));

        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(output, "Bingo Game Settings");
            System.out.println("Settings saved to " + SETTINGS_FILE);
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    // --- Getters and Setters (updated for new fields) ---

    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }

    public int getNumbersPerGeneration() { return numbersPerGeneration; }
    public void setNumbersPerGeneration(int numbersPerGeneration) { this.numbersPerGeneration = Math.max(1, numbersPerGeneration); }

    // On-card match points
    public boolean isRandomizePoints() { return randomizePoints; }
    public void setRandomizePoints(boolean randomizePoints) { this.randomizePoints = randomizePoints; }
    public int getMinRandomPoints() { return minRandomPoints; }
    public void setMinRandomPoints(int minRandomPoints) { this.minRandomPoints = minRandomPoints; } // Allows negative
    public int getMaxRandomPoints() { return maxRandomPoints; }
    public void setMaxRandomPoints(int maxRandomPoints) { this.maxRandomPoints = maxRandomPoints; } // Allows negative, handled by validation
    public int getExactPoints() { return exactPoints; }
    public void setExactPoints(int exactPoints) { this.exactPoints = exactPoints; } // Allows negative

    // NEW: Generate Button points
    public boolean isAwardPointsOnGenerate() { return awardPointsOnGenerate; }
    public void setAwardPointsOnGenerate(boolean awardPointsOnGenerate) { this.awardPointsOnGenerate = awardPointsOnGenerate; }
    public boolean isRandomizeGeneratePoints() { return randomizeGeneratePoints; }
    public void setRandomizeGeneratePoints(boolean randomizeGeneratePoints) { this.randomizeGeneratePoints = randomizeGeneratePoints; }
    public int getMinGeneratePoints() { return minGeneratePoints; }
    public void setMinGeneratePoints(int minGeneratePoints) { this.minGeneratePoints = minGeneratePoints; }
    public int getMaxGeneratePoints() { return maxGeneratePoints; }
    public void setMaxGeneratePoints(int maxGeneratePoints) { this.maxGeneratePoints = maxGeneratePoints; }
    public int getExactGeneratePoints() { return exactGeneratePoints; }
    public void setExactGeneratePoints(int exactGeneratePoints) { this.exactGeneratePoints = exactGeneratePoints; }

    // NEW: Bingo Bonus points
    public boolean isAwardPointsOnBingo() { return awardPointsOnBingo; }
    public void setAwardPointsOnBingo(boolean awardPointsOnBingo) { this.awardPointsOnBingo = awardPointsOnBingo; }
    public boolean isRandomizeBingoPoints() { return randomizeBingoPoints; }
    public void setRandomizeBingoPoints(boolean randomizeBingoPoints) { this.randomizeBingoPoints = randomizeBingoPoints; }
    public int getMinBingoPoints() { return minBingoPoints; }
    public void setMinBingoPoints(int minBingoPoints) { this.minBingoPoints = minBingoPoints; }
    public int getMaxBingoPoints() { return maxBingoPoints; }
    public void setMaxBingoPoints(int maxBingoPoints) { this.maxBingoPoints = maxBingoPoints; }
    public int getExactBingoPoints() { return exactBingoPoints; }
    public void setExactBingoPoints(int exactBingoPoints) { this.exactBingoPoints = exactBingoPoints; }

    // Card Number Range
    public int getMinCardNumber() { return minCardNumber; }
    public void setMinCardNumber(int minCardNumber) { this.minCardNumber = Math.max(0, minCardNumber); } // Min card number still non-negative
    public int getMaxCardNumber() { return maxCardNumber; }
    public void setMaxCardNumber(int maxCardNumber) { this.maxCardNumber = Math.max(this.minCardNumber + 24, maxCardNumber); }

    public boolean isAutoSelectNumbers() { return autoSelectNumbers; }
    public void setAutoSelectNumbers(boolean autoSelectNumbers) { this.autoSelectNumbers = autoSelectNumbers; }
    
    // Bingo Win Condition
    public int getBingosNeededToWin() { return bingosNeededToWin; }
    public void setBingosNeededToWin(int bingosNeededToWin) { this.bingosNeededToWin = Math.max(1, bingosNeededToWin); }
}