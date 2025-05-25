package com.deal.model;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class GameSettings {
    private static final String SETTINGS_FILE = "deal_settings.ini";

    private int numberOfBags;
    private int minBagValue;
    private int maxBagValue;
    private int minOfferValue;
    private int maxOfferValue;
    private int offerRoundFrequency;
    private int trackingMode; // 0: None, 1: Full, 2: Value Only

    // Settings for Banker Offer Bias
    private boolean bankerBiasEnabled;
    private int bankerBiasMagnitude; // 0: Bias towards minOfferValue, 50: Neutral, 100: Bias towards maxOfferValue

    // Settings for Late Game Banker Offering
    private boolean lateGameOfferEnabled;
    private int lateGameTriggerBags; // Number of unopened, non-chosen bags remaining to trigger late game offers
    private double lateGameOfferFactorBoost; // Additional factor (e.g., 0.1 for 10% more aggressive offers)

    // Settings for Bag Value Generation Bias
    private boolean bagValueBiasEnabled;
    private int bagValueBiasMagnitude; // 0: Bias towards minBagValue, 50: Neutral, 100: Bias towards maxBagValue
    private double bagValueBiasStrength; // How strongly the bias affects the value generation (e.g., 1.0 for linear, 3.0 for stronger curve)

    // New settings for Authentic Mode
    private boolean authenticModeEnabled;
    private boolean allowBagSwap; // New option to allow/disallow bag swapping

    // Authentic Game Values (US version)
    public static final List<Integer> AUTHENTIC_BAG_VALUES = Collections.unmodifiableList(Arrays.asList(
            1, 5, 10, 25, 50, 75, 100, 200, 300, 400, 500, 750, // Small values, up to 750
            1000, 5000, 10000, 25000, 50000, 75000, 100000, // Mid values
            200000, 300000, 400000, 500000, 750000, 1000000, // Large values
            1 // Replaced 0.01 with 1 to use integers, but could be handled by a specific 0.01 value if needed
    ));
    // The original 0.01 value is often simplified to 1 in integer-based DOND simulations.
    // If you need actual cents, you'd need to switch bag values to double or use an integer scaled by 100.
    // For now, let's treat 0.01 as 1.

    // Authentic Offer Sequence (bags to open per round before an offer)
    public static final List<Integer> AUTHENTIC_OFFER_SEQUENCE = Collections.unmodifiableList(Arrays.asList(
            6, 5, 4, 3, 2, 1, 1, 1, 1, 1 // Total 23 bags opened for 10 offers.
                                        // The chosen bag (1) + final remaining bag (1) + 24 opened.
                                        // Correct for 26 bags: 1 chosen, 25 to open.
                                        // 6+5+4+3+2+1+1+1+1+1 = 25 bags opened.
    ));


    public GameSettings() {
        // Initialize with default values first, then load from file or set authentic defaults
        resetToDefaultAuthentic(); // Default will now be authentic
        loadSettings(); // Attempt to load custom settings, overriding defaults
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
            
            this.bankerBiasEnabled = Boolean.parseBoolean(properties.getProperty("bankerBiasEnabled", String.valueOf(this.bankerBiasEnabled)));
            this.bankerBiasMagnitude = Integer.parseInt(properties.getProperty("bankerBiasMagnitude", String.valueOf(this.bankerBiasMagnitude)));

            this.lateGameOfferEnabled = Boolean.parseBoolean(properties.getProperty("lateGameOfferEnabled", String.valueOf(this.lateGameOfferEnabled)));
            this.lateGameTriggerBags = Integer.parseInt(properties.getProperty("lateGameTriggerBags", String.valueOf(this.lateGameTriggerBags)));
            this.lateGameOfferFactorBoost = Double.parseDouble(properties.getProperty("lateGameOfferFactorBoost", String.valueOf(this.lateGameOfferFactorBoost)));

            this.bagValueBiasEnabled = Boolean.parseBoolean(properties.getProperty("bagValueBiasEnabled", String.valueOf(this.bagValueBiasEnabled)));
            this.bagValueBiasMagnitude = Integer.parseInt(properties.getProperty("bagValueBiasMagnitude", String.valueOf(this.bagValueBiasMagnitude)));
            this.bagValueBiasStrength = Double.parseDouble(properties.getProperty("bagValueBiasStrength", String.valueOf(this.bagValueBiasStrength)));

            // Load new authentic mode settings
            this.authenticModeEnabled = Boolean.parseBoolean(properties.getProperty("authenticModeEnabled", String.valueOf(this.authenticModeEnabled)));
            this.allowBagSwap = Boolean.parseBoolean(properties.getProperty("allowBagSwap", String.valueOf(this.allowBagSwap)));


        } catch (IOException | NumberFormatException e) {
            System.err.println("Could not load settings from " + SETTINGS_FILE + ". Using defaults and saving them.");
            resetToDefaultAuthentic(); // Reset to authentic defaults if load fails
            saveSettings(); // Save these defaults to create the file
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
        
        properties.setProperty("bankerBiasEnabled", String.valueOf(this.bankerBiasEnabled));
        properties.setProperty("bankerBiasMagnitude", String.valueOf(this.bankerBiasMagnitude));

        properties.setProperty("lateGameOfferEnabled", String.valueOf(this.lateGameOfferEnabled));
        properties.setProperty("lateGameTriggerBags", String.valueOf(this.lateGameTriggerBags));
        properties.setProperty("lateGameOfferFactorBoost", String.valueOf(this.lateGameOfferFactorBoost));

        properties.setProperty("bagValueBiasEnabled", String.valueOf(this.bagValueBiasEnabled));
        properties.setProperty("bagValueBiasMagnitude", String.valueOf(this.bagValueBiasMagnitude));
        properties.setProperty("bagValueBiasStrength", String.valueOf(this.bagValueBiasStrength));

        // Save new authentic mode settings
        properties.setProperty("authenticModeEnabled", String.valueOf(this.authenticModeEnabled));
        properties.setProperty("allowBagSwap", String.valueOf(this.allowBagSwap));

        try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
            properties.store(writer, "Deal or No Deal Game Settings");
            System.out.println("Settings saved to " + SETTINGS_FILE);
        } catch (IOException e) {
            System.err.println("Could not save settings to " + SETTINGS_FILE + ". Error: " + e.getMessage());
        }
    }

    /**
     * Resets all game settings to default values, mimicking the authentic game show experience.
     */
    public void resetToDefaultAuthentic() {
        this.numberOfBags = AUTHENTIC_BAG_VALUES.size(); // 26
        this.minBagValue = Collections.min(AUTHENTIC_BAG_VALUES); // 1
        this.maxBagValue = Collections.max(AUTHENTIC_BAG_VALUES); // 1,000,000

        // Min/Max offer value are broad, as actual offers vary greatly
        this.minOfferValue = 0; // Banker can offer low
        this.maxOfferValue = 1000000; // Banker can offer high (up to max bag value)

        this.offerRoundFrequency = 3; // Default for non-authentic mode, ignored in authentic
        this.trackingMode = 2; // Value Only Tracking

        this.bankerBiasEnabled = false;
        this.bankerBiasMagnitude = 50; // Neutral

        this.lateGameOfferEnabled = true;
        // Late game starts when 6 bags (including player's case) remain for the final 5 single-bag rounds
        // (total bags = 26. 6+5+4+3+2 = 20 bags opened, 26-20 = 6 bags remaining)
        this.lateGameTriggerBags = 6;
        this.lateGameOfferFactorBoost = 0.2; // A good boost for late game offers

        this.bagValueBiasEnabled = false;
        this.bagValueBiasMagnitude = 50; // Neutral
        this.bagValueBiasStrength = 1.0; // No power effect, linear distribution (even if bias enabled, it's neutral)

        this.authenticModeEnabled = true; // Set authentic mode enabled by default
        this.allowBagSwap = false; // Authentic game does not allow bag swap (only final decision)
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

    // Getters and Setters for Banker Offer Bias
    public boolean isBankerBiasEnabled() { return bankerBiasEnabled; }
    public void setBankerBiasEnabled(boolean bankerBiasEnabled) { this.bankerBiasEnabled = bankerBiasEnabled; }

    public int getBankerBiasMagnitude() { return bankerBiasMagnitude; }
    public void setBankerBiasMagnitude(int bankerBiasMagnitude) { this.bankerBiasMagnitude = bankerBiasMagnitude; }

    // Getters and Setters for Late Game Banker Offering
    public boolean isLateGameOfferEnabled() { return lateGameOfferEnabled; }
    public void setLateGameOfferEnabled(boolean lateGameOfferEnabled) { this.lateGameOfferEnabled = lateGameOfferEnabled; }

    public int getLateGameTriggerBags() { return lateGameTriggerBags; }
    public void setLateGameTriggerBags(int lateGameTriggerBags) { this.lateGameTriggerBags = lateGameTriggerBags; }

    public double getLateGameOfferFactorBoost() { return lateGameOfferFactorBoost; }
    public void setLateGameOfferFactorBoost(double lateGameOfferFactorBoost) { this.lateGameOfferFactorBoost = lateGameOfferFactorBoost; }

    // Getters and Setters for Bag Value Generation Bias
    public boolean isBagValueBiasEnabled() { return bagValueBiasEnabled; }
    public void setBagValueBiasEnabled(boolean bagValueBiasEnabled) { this.bagValueBiasEnabled = bagValueBiasEnabled; }

    public int getBagValueBiasMagnitude() { return bagValueBiasMagnitude; }
    public void setBagValueBiasMagnitude(int bagValueBiasMagnitude) { this.bagValueBiasMagnitude = bagValueBiasMagnitude; }

    public double getBagValueBiasStrength() { return bagValueBiasStrength; }
    public void setBagValueBiasStrength(double bagValueBiasStrength) { this.bagValueBiasStrength = bagValueBiasStrength; }

    // New Getters and Setters for Authentic Mode
    public boolean isAuthenticModeEnabled() { return authenticModeEnabled; }
    public void setAuthenticModeEnabled(boolean authenticModeEnabled) { this.authenticModeEnabled = authenticModeEnabled; }

    public boolean isAllowBagSwap() { return allowBagSwap; }
    public void setAllowBagSwap(boolean allowBagSwap) { this.allowBagSwap = allowBagSwap; }
}