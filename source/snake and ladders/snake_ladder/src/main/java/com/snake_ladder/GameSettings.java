package com.snake_ladder;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

public class GameSettings {
    private static final String SETTINGS_FILE = "snake_ladder_settings.ini";

    // Player settings
    public int numPlayers = 1;
    public int initialLives = 3;
    public int initialPoints = 1000;
    public boolean[] playerIsHuman = new boolean[8];
    public String[] playerNames = new String[8];
    public boolean allowNegativePoints = true; // ADDED

    // Dice settings
    public int numDice = 1;
    public int numSidesPerDie = 6;

    // Board settings
    public int numTiles = 100;
    public boolean hideTileValuesOnBoard = false;

    // Tile effect value settings
    public int warpForwardMin = 1;
    public int warpForwardMax = 25;
    public boolean warpForwardStatic = false;
    public int warpForwardStaticValue = 10;

    public int warpBackwardMin = 1;
    public int warpBackwardMax = 25;
    public boolean warpBackwardStatic = false;
    public int warpBackwardStaticValue = 10;

    public int givePointsMin = 100;
    public int givePointsMax = 1000;
    public boolean givePointsStatic = false;
    public int givePointsStaticValue = 500;

    public int takePointsMin = 100;
    public int takePointsMax = 1000;
    public boolean takePointsStatic = false;
    public int takePointsStaticValue = 500;

    // Tile effect probabilities
    public EnumMap<TileEffect, Integer> tileProbabilities = new EnumMap<>(TileEffect.class);

    // Game rules
    public enum WinCondition { FIRST_TO_FINISH, MOST_POINTS_AT_FINISH }
    public WinCondition winCondition = WinCondition.FIRST_TO_FINISH;
    public boolean respawnEliminatedPlayers = false;
    public boolean instantFinishIfOneLeft = false;

    public GameSettings() {
        Arrays.fill(playerIsHuman, true);
        for(int i=0; i < playerNames.length; i++) playerNames[i] = "Player " + (i+1);
        loadDefaultProbabilities();
        loadSettings();
    }

    private void loadDefaultProbabilities() {
        tileProbabilities.clear();
        for (TileEffect effect : TileEffect.getRandomizableEffects()) {
            tileProbabilities.put(effect, 10);
        }
    }

    public void resetToDefaults() {
        numPlayers = 1;
        initialLives = 3;
        initialPoints = 1000;
        Arrays.fill(playerIsHuman, true);
        for(int i=0; i < playerNames.length; i++) playerNames[i] = "Player " + (i+1);
        allowNegativePoints = true; // ADDED Default for reset

        numDice = 1;
        numSidesPerDie = 6;
        numTiles = 100;
        hideTileValuesOnBoard = false;

        warpForwardMin = 1; warpForwardMax = 25; warpForwardStatic = false; warpForwardStaticValue = 10;
        warpBackwardMin = 1; warpBackwardMax = 25; warpBackwardStatic = false; warpBackwardStaticValue = 10;
        givePointsMin = 100; givePointsMax = 1000; givePointsStatic = false; givePointsStaticValue = 500;
        takePointsMin = 100; takePointsMax = 1000; takePointsStatic = false; takePointsStaticValue = 500;
        
        loadDefaultProbabilities();

        winCondition = WinCondition.FIRST_TO_FINISH;
        respawnEliminatedPlayers = false;
        instantFinishIfOneLeft = false;
        System.out.println("Settings reset to defaults.");
    }

    public void loadSettings() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(SETTINGS_FILE)) {
            props.load(input);

            numPlayers = Integer.parseInt(props.getProperty("numPlayers", String.valueOf(numPlayers)));
            initialLives = Integer.parseInt(props.getProperty("initialLives", String.valueOf(initialLives)));
            initialPoints = Integer.parseInt(props.getProperty("initialPoints", String.valueOf(initialPoints)));
            for(int i=0; i < 8; i++) {
                playerIsHuman[i] = Boolean.parseBoolean(props.getProperty("playerIsHuman" + i, String.valueOf(playerIsHuman[i])));
                playerNames[i] = props.getProperty("playerName" + i, playerNames[i]);
            }
            allowNegativePoints = Boolean.parseBoolean(props.getProperty("allowNegativePoints", String.valueOf(allowNegativePoints))); // ADDED

            numDice = Integer.parseInt(props.getProperty("numDice", String.valueOf(numDice)));
            numSidesPerDie = Integer.parseInt(props.getProperty("numSidesPerDie", String.valueOf(numSidesPerDie)));
            numTiles = Integer.parseInt(props.getProperty("numTiles", String.valueOf(numTiles)));
            hideTileValuesOnBoard = Boolean.parseBoolean(props.getProperty("hideTileValuesOnBoard", String.valueOf(hideTileValuesOnBoard)));

            warpForwardMin = Integer.parseInt(props.getProperty("warpForwardMin", String.valueOf(warpForwardMin)));
            warpForwardMax = Integer.parseInt(props.getProperty("warpForwardMax", String.valueOf(warpForwardMax)));
            warpForwardStatic = Boolean.parseBoolean(props.getProperty("warpForwardStatic", String.valueOf(warpForwardStatic)));
            warpForwardStaticValue = Integer.parseInt(props.getProperty("warpForwardStaticValue", String.valueOf(warpForwardStaticValue)));
            
            warpBackwardMin = Integer.parseInt(props.getProperty("warpBackwardMin", String.valueOf(warpBackwardMin)));
            warpBackwardMax = Integer.parseInt(props.getProperty("warpBackwardMax", String.valueOf(warpBackwardMax)));
            warpBackwardStatic = Boolean.parseBoolean(props.getProperty("warpBackwardStatic", String.valueOf(warpBackwardStatic)));
            warpBackwardStaticValue = Integer.parseInt(props.getProperty("warpBackwardStaticValue", String.valueOf(warpBackwardStaticValue)));

            givePointsMin = Integer.parseInt(props.getProperty("givePointsMin", String.valueOf(givePointsMin)));
            givePointsMax = Integer.parseInt(props.getProperty("givePointsMax", String.valueOf(givePointsMax)));
            givePointsStatic = Boolean.parseBoolean(props.getProperty("givePointsStatic", String.valueOf(givePointsStatic)));
            givePointsStaticValue = Integer.parseInt(props.getProperty("givePointsStaticValue", String.valueOf(givePointsStaticValue)));

            takePointsMin = Integer.parseInt(props.getProperty("takePointsMin", String.valueOf(takePointsMin)));
            takePointsMax = Integer.parseInt(props.getProperty("takePointsMax", String.valueOf(takePointsMax)));
            takePointsStatic = Boolean.parseBoolean(props.getProperty("takePointsStatic", String.valueOf(takePointsStatic)));
            takePointsStaticValue = Integer.parseInt(props.getProperty("takePointsStaticValue", String.valueOf(takePointsStaticValue)));

            for (TileEffect effect : TileEffect.getRandomizableEffects()) {
                tileProbabilities.put(effect, Integer.parseInt(props.getProperty("prob_" + effect.name(), String.valueOf(tileProbabilities.getOrDefault(effect, 10)))));
            }

            winCondition = WinCondition.valueOf(props.getProperty("winCondition", winCondition.name()));
            respawnEliminatedPlayers = Boolean.parseBoolean(props.getProperty("respawnEliminatedPlayers", String.valueOf(respawnEliminatedPlayers)));
            instantFinishIfOneLeft = Boolean.parseBoolean(props.getProperty("instantFinishIfOneLeft", String.valueOf(instantFinishIfOneLeft)));

            System.out.println("Settings loaded from " + SETTINGS_FILE);

        } catch (IOException | NumberFormatException e) {
            System.out.println("Could not load settings, using defaults: " + e.getMessage());
            resetToDefaults();
        }
    }

    public void saveSettings() {
        Properties props = new Properties();
        props.setProperty("numPlayers", String.valueOf(numPlayers));
        props.setProperty("initialLives", String.valueOf(initialLives));
        props.setProperty("initialPoints", String.valueOf(initialPoints));
        for(int i=0; i < 8; i++) {
            props.setProperty("playerIsHuman" + i, String.valueOf(playerIsHuman[i]));
            props.setProperty("playerName" + i, playerNames[i]);
        }
        props.setProperty("allowNegativePoints", String.valueOf(allowNegativePoints)); // ADDED

        props.setProperty("numDice", String.valueOf(numDice));
        props.setProperty("numSidesPerDie", String.valueOf(numSidesPerDie));
        props.setProperty("numTiles", String.valueOf(numTiles));
        props.setProperty("hideTileValuesOnBoard", String.valueOf(hideTileValuesOnBoard));

        props.setProperty("warpForwardMin", String.valueOf(warpForwardMin));
        props.setProperty("warpForwardMax", String.valueOf(warpForwardMax));
        props.setProperty("warpForwardStatic", String.valueOf(warpForwardStatic));
        props.setProperty("warpForwardStaticValue", String.valueOf(warpForwardStaticValue));

        props.setProperty("warpBackwardMin", String.valueOf(warpBackwardMin));
        props.setProperty("warpBackwardMax", String.valueOf(warpBackwardMax));
        props.setProperty("warpBackwardStatic", String.valueOf(warpBackwardStatic));
        props.setProperty("warpBackwardStaticValue", String.valueOf(warpBackwardStaticValue));

        props.setProperty("givePointsMin", String.valueOf(givePointsMin));
        props.setProperty("givePointsMax", String.valueOf(givePointsMax));
        props.setProperty("givePointsStatic", String.valueOf(givePointsStatic));
        props.setProperty("givePointsStaticValue", String.valueOf(givePointsStaticValue));

        props.setProperty("takePointsMin", String.valueOf(takePointsMin));
        props.setProperty("takePointsMax", String.valueOf(takePointsMax));
        props.setProperty("takePointsStatic", String.valueOf(takePointsStatic));
        props.setProperty("takePointsStaticValue", String.valueOf(takePointsStaticValue));

        for (Map.Entry<TileEffect, Integer> entry : tileProbabilities.entrySet()) {
            props.setProperty("prob_" + entry.getKey().name(), String.valueOf(entry.getValue()));
        }

        props.setProperty("winCondition", winCondition.name());
        props.setProperty("respawnEliminatedPlayers", String.valueOf(respawnEliminatedPlayers));
        props.setProperty("instantFinishIfOneLeft", String.valueOf(instantFinishIfOneLeft));

        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            props.store(output, "Snake and Ladder Game Settings");
            System.out.println("Settings saved to " + SETTINGS_FILE);
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }
}