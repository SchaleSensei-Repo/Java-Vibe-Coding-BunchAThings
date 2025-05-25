package com.deal.gui;

import com.deal.model.GameSettings;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Hashtable;
import java.util.Collections;

public class SettingsPanel extends JPanel {
    private GameSettings settings;
    private GameFrame parentFrame;

    // --- New Authentic Mode Checkbox ---
    private JCheckBox authenticModeCheckbox;

    // --- Core Game Settings Components ---
    private JTextField numBagsField;
    private JTextField minBagValueField;
    private JTextField maxBagValueField;
    private JTextField minOfferValueField;
    private JTextField maxOfferValueField;
    private JTextField offerFreqField;
    private JRadioButton noTrackingRadio;
    private JRadioButton fullTrackingRadio;
    private JRadioButton valueOnlyTrackingRadio;
    private ButtonGroup trackingGroup; // Keep reference to button group

    // --- New Allow Bag Swap Checkbox ---
    private JCheckBox allowBagSwapCheckbox;

    // --- Advanced Bias Settings Components ---
    // Banker Offer Bias
    private JCheckBox enableBankerBiasCheckbox;
    private JSlider bankerBiasSlider;
    private JLabel bankerBiasDescriptionLabel;

    // Late Game Banker Offering
    private JCheckBox enableLateGameOfferCheckbox;
    private JTextField lateGameTriggerBagsField;
    private JTextField lateGameOfferFactorBoostField;

    // Bag Value Generation Bias
    private JCheckBox enableBagValueBiasCheckbox;
    private JSlider bagValueBiasSlider;
    private JLabel bagValueBiasDescriptionLabel;
    private JTextField bagValueBiasStrengthField;


    public SettingsPanel(GameSettings settings, GameFrame parentFrame) {
        this.settings = settings;
        this.parentFrame = parentFrame;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Title ---
        JLabel titleLabel = new JLabel("Game Settings", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(titleLabel, gbc);

        // --- New: Authentic Mode Checkbox ---
        gbc.gridy++;
        authenticModeCheckbox = new JCheckBox("Enable Authentic Game Show Mode");
        authenticModeCheckbox.addActionListener(e -> {
            // Update the settings object immediately when this checkbox is toggled
            settings.setAuthenticModeEnabled(authenticModeCheckbox.isSelected());
            if (authenticModeCheckbox.isSelected()) {
                settings.resetToDefaultAuthentic(); // Apply authentic settings
                loadSettingsIntoFields(); // Reload all fields to reflect authentic mode
            }
            toggleAuthenticMode(authenticModeCheckbox.isSelected());
        });
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        add(authenticModeCheckbox, gbc);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // --- Core Settings Panel (Left Column) ---
        JPanel coreSettingsPanel = createCoreSettingsPanel();
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(coreSettingsPanel, gbc);

        // --- Advanced Bias Settings Panel (Right Column) ---
        JPanel advancedSettingsPanel = createAdvancedSettingsPanel();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(advancedSettingsPanel, gbc);

        // --- Buttons at the bottom (Spans both columns) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        JButton saveButton = new JButton("Save Settings");
        saveButton.addActionListener(e -> saveSettings());
        buttonPanel.add(saveButton);

        JButton startGameButton = new JButton("Start Game");
        startGameButton.addActionListener(e -> {
            if (saveSettings()) {
                parentFrame.showGamePanel();
            }
        });
        buttonPanel.add(startGameButton);

        JButton resetButton = new JButton("Reset to Default");
        resetButton.addActionListener(e -> resetSettingsToDefault());
        buttonPanel.add(resetButton);


        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        add(buttonPanel, gbc);

        loadSettingsIntoFields();
        toggleAuthenticMode(settings.isAuthenticModeEnabled());
    }

    private JPanel createCoreSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Core Game Parameters"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        panel.add(new JLabel("Number of Bags:"), gbc);
        gbc.gridx = 1;
        numBagsField = new JTextField(10);
        panel.add(numBagsField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Min Bag Value:"), gbc);
        gbc.gridx = 1;
        minBagValueField = new JTextField(10);
        panel.add(minBagValueField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Max Bag Value:"), gbc);
        gbc.gridx = 1;
        maxBagValueField = new JTextField(10);
        panel.add(maxBagValueField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Min Offer Value:"), gbc);
        gbc.gridx = 1;
        minOfferValueField = new JTextField(10);
        panel.add(minOfferValueField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Max Offer Value:"), gbc);
        gbc.gridx = 1;
        maxOfferValueField = new JTextField(10);
        panel.add(maxOfferValueField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Offer Frequency (bags per round):"), gbc);
        gbc.gridx = 1;
        offerFreqField = new JTextField(10);
        panel.add(offerFreqField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel trackingOptionsPanel = new JPanel(new GridLayout(3, 1));
        trackingOptionsPanel.setBorder(BorderFactory.createTitledBorder("Bag Tracking Options"));
        trackingGroup = new ButtonGroup();
        noTrackingRadio = new JRadioButton("No Tracking");
        fullTrackingRadio = new JRadioButton("Full Tracking (ID & Value)");
        valueOnlyTrackingRadio = new JRadioButton("Value-Only Tracking (no ID)");
        trackingGroup.add(noTrackingRadio); trackingGroup.add(fullTrackingRadio); trackingGroup.add(valueOnlyTrackingRadio);
        trackingOptionsPanel.add(noTrackingRadio); trackingOptionsPanel.add(fullTrackingRadio); trackingOptionsPanel.add(valueOnlyTrackingRadio);
        panel.add(trackingOptionsPanel, gbc);

        gbc.gridy++;
        allowBagSwapCheckbox = new JCheckBox("Allow Bag Swapping");
        panel.add(allowBagSwapCheckbox, gbc);
        return panel;
    }

    private JPanel createAdvancedSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Advanced Game Modifiers"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JPanel bankerBiasPanel = new JPanel(new GridBagLayout());
        bankerBiasPanel.setBorder(BorderFactory.createTitledBorder("Banker Offer Bias"));
        GridBagConstraints bankerBiasGbc = new GridBagConstraints();
        bankerBiasGbc.insets = new Insets(2, 5, 2, 5); bankerBiasGbc.fill = GridBagConstraints.HORIZONTAL;
        bankerBiasGbc.gridx = 0; bankerBiasGbc.gridy = 0; bankerBiasGbc.gridwidth = 2;
        enableBankerBiasCheckbox = new JCheckBox("Enable Banker Offer Bias");
        enableBankerBiasCheckbox.addActionListener(e -> setBankerBiasControlsEnabled(enableBankerBiasCheckbox.isSelected()));
        bankerBiasPanel.add(enableBankerBiasCheckbox, bankerBiasGbc);
        bankerBiasGbc.gridy++;
        bankerBiasDescriptionLabel = new JLabel("Neutral", SwingConstants.CENTER);
        bankerBiasPanel.add(bankerBiasDescriptionLabel, bankerBiasGbc);
        bankerBiasGbc.gridy++;
        bankerBiasSlider = new JSlider(0, 100, 50);
        bankerBiasSlider.setMajorTickSpacing(25); bankerBiasSlider.setMinorTickSpacing(5);
        bankerBiasSlider.setPaintTicks(true); bankerBiasSlider.setPaintLabels(true);
        Hashtable<Integer, JLabel> bankerLabelTable = new Hashtable<>();
        bankerLabelTable.put(0, new JLabel("Min Offer")); bankerLabelTable.put(50, new JLabel("Neutral")); bankerLabelTable.put(100, new JLabel("Max Offer"));
        bankerBiasSlider.setLabelTable(bankerLabelTable);
        bankerBiasSlider.addChangeListener(e -> updateBankerBiasDescriptionLabel(bankerBiasSlider.getValue()));
        bankerBiasPanel.add(bankerBiasSlider, bankerBiasGbc);
        panel.add(bankerBiasPanel, gbc);

        gbc.gridy++;
        JPanel lateGamePanel = new JPanel(new GridBagLayout());
        lateGamePanel.setBorder(BorderFactory.createTitledBorder("Late Game Banker Offering"));
        GridBagConstraints lgGbc = new GridBagConstraints();
        lgGbc.insets = new Insets(2, 5, 2, 5); lgGbc.fill = GridBagConstraints.HORIZONTAL; lgGbc.anchor = GridBagConstraints.NORTHWEST;
        enableLateGameOfferCheckbox = new JCheckBox("Enable Late Game Offers");
        enableLateGameOfferCheckbox.addActionListener(e -> setLateGameControlsEnabled(enableLateGameOfferCheckbox.isSelected()));
        lgGbc.gridx = 0; lgGbc.gridy = 0; lgGbc.gridwidth = 2;
        lateGamePanel.add(enableLateGameOfferCheckbox, lgGbc);
        lgGbc.gridy++; lgGbc.gridx = 0; lgGbc.gridwidth = 1;
        lateGamePanel.add(new JLabel("Trigger when bags left <= :"), lgGbc);
        lgGbc.gridx = 1;
        lateGameTriggerBagsField = new JTextField(8);
        lateGamePanel.add(lateGameTriggerBagsField, lgGbc);
        lgGbc.gridy++; lgGbc.gridx = 0;
        lateGamePanel.add(new JLabel("Offer Boost Factor (e.g., 0.1 for 10%):"), lgGbc);
        lgGbc.gridx = 1;
        lateGameOfferFactorBoostField = new JTextField(8);
        lateGamePanel.add(lateGameOfferFactorBoostField, lgGbc);
        panel.add(lateGamePanel, gbc);

        gbc.gridy++;
        JPanel bagValueBiasPanel = new JPanel(new GridBagLayout());
        bagValueBiasPanel.setBorder(BorderFactory.createTitledBorder("Bag Value Generation Bias"));
        GridBagConstraints bagBiasGbc = new GridBagConstraints();
        bagBiasGbc.insets = new Insets(2, 5, 2, 5); bagBiasGbc.fill = GridBagConstraints.HORIZONTAL; bagBiasGbc.anchor = GridBagConstraints.NORTHWEST;
        enableBagValueBiasCheckbox = new JCheckBox("Enable Bag Value Generation Bias");
        enableBagValueBiasCheckbox.addActionListener(e -> setBagValueBiasControlsEnabled(enableBagValueBiasCheckbox.isSelected()));
        bagBiasGbc.gridx = 0; bagBiasGbc.gridy = 0; bagBiasGbc.gridwidth = 2;
        bagValueBiasPanel.add(enableBagValueBiasCheckbox, bagBiasGbc);
        bagBiasGbc.gridy++;
        bagValueBiasDescriptionLabel = new JLabel("Neutral", SwingConstants.CENTER);
        bagValueBiasPanel.add(bagValueBiasDescriptionLabel, bagBiasGbc);
        bagBiasGbc.gridy++;
        bagValueBiasSlider = new JSlider(0, 100, 50);
        bagValueBiasSlider.setMajorTickSpacing(25); bagValueBiasSlider.setMinorTickSpacing(5);
        bagValueBiasSlider.setPaintTicks(true); bagValueBiasSlider.setPaintLabels(true);
        Hashtable<Integer, JLabel> bagLabelTable = new Hashtable<>();
        bagLabelTable.put(0, new JLabel("Lower Values")); bagLabelTable.put(50, new JLabel("Even Mix")); bagLabelTable.put(100, new JLabel("Higher Values"));
        bagValueBiasSlider.setLabelTable(bagLabelTable);
        bagValueBiasSlider.addChangeListener(e -> updateBagValueBiasDescriptionLabel(bagValueBiasSlider.getValue()));
        bagValueBiasPanel.add(bagValueBiasSlider, bagBiasGbc);
        bagBiasGbc.gridy++; bagBiasGbc.gridx = 0; bagBiasGbc.gridwidth = 1;
        bagValueBiasPanel.add(new JLabel("Bias Strength (e.g., 3.0):"), bagBiasGbc);
        bagBiasGbc.gridx = 1;
        bagValueBiasStrengthField = new JTextField(8);
        bagValueBiasPanel.add(bagValueBiasStrengthField, bagBiasGbc);
        panel.add(bagValueBiasPanel, gbc);
        return panel;
    }

    private void loadSettingsIntoFields() {
        // This ensures the checkbox state is correct before toggleAuthenticMode might change it
        authenticModeCheckbox.setSelected(settings.isAuthenticModeEnabled());

        // Load all fields based on the settings object
        numBagsField.setText(String.valueOf(settings.getNumberOfBags()));
        minBagValueField.setText(String.valueOf(settings.getMinBagValue()));
        maxBagValueField.setText(String.valueOf(settings.getMaxBagValue()));
        minOfferValueField.setText(String.valueOf(settings.getMinOfferValue()));
        maxOfferValueField.setText(String.valueOf(settings.getMaxOfferValue()));
        offerFreqField.setText(String.valueOf(settings.getOfferRoundFrequency()));

        if (settings.getTrackingMode() == 0) noTrackingRadio.setSelected(true);
        else if (settings.getTrackingMode() == 1) fullTrackingRadio.setSelected(true);
        else if (settings.getTrackingMode() == 2) valueOnlyTrackingRadio.setSelected(true);
        
        allowBagSwapCheckbox.setSelected(settings.isAllowBagSwap());

        enableBankerBiasCheckbox.setSelected(settings.isBankerBiasEnabled());
        bankerBiasSlider.setValue(settings.getBankerBiasMagnitude());
        updateBankerBiasDescriptionLabel(settings.getBankerBiasMagnitude());

        enableLateGameOfferCheckbox.setSelected(settings.isLateGameOfferEnabled());
        lateGameTriggerBagsField.setText(String.valueOf(settings.getLateGameTriggerBags()));
        lateGameOfferFactorBoostField.setText(String.valueOf(settings.getLateGameOfferFactorBoost()));

        enableBagValueBiasCheckbox.setSelected(settings.isBagValueBiasEnabled());
        bagValueBiasSlider.setValue(settings.getBagValueBiasMagnitude());
        bagValueBiasStrengthField.setText(String.valueOf(settings.getBagValueBiasStrength()));
        updateBagValueBiasDescriptionLabel(settings.getBagValueBiasMagnitude());

        // After loading, apply the correct enabled/disabled state
        toggleAuthenticMode(settings.isAuthenticModeEnabled());
    }

    private boolean saveSettings() {
        try {
            settings.setAuthenticModeEnabled(authenticModeCheckbox.isSelected());

            if (settings.isAuthenticModeEnabled()) {
                settings.resetToDefaultAuthentic();
            } else {
                settings.setNumberOfBags(Integer.parseInt(numBagsField.getText()));
                settings.setMinBagValue(Integer.parseInt(minBagValueField.getText()));
                settings.setMaxBagValue(Integer.parseInt(maxBagValueField.getText()));
                settings.setMinOfferValue(Integer.parseInt(minOfferValueField.getText()));
                settings.setMaxOfferValue(Integer.parseInt(maxOfferValueField.getText()));
                settings.setOfferRoundFrequency(Integer.parseInt(offerFreqField.getText()));

                if (noTrackingRadio.isSelected()) settings.setTrackingMode(0);
                else if (fullTrackingRadio.isSelected()) settings.setTrackingMode(1);
                else if (valueOnlyTrackingRadio.isSelected()) settings.setTrackingMode(2);
                
                settings.setAllowBagSwap(allowBagSwapCheckbox.isSelected());

                settings.setBankerBiasEnabled(enableBankerBiasCheckbox.isSelected());
                settings.setBankerBiasMagnitude(bankerBiasSlider.getValue());

                settings.setLateGameOfferEnabled(enableLateGameOfferCheckbox.isSelected());
                settings.setLateGameTriggerBags(Integer.parseInt(lateGameTriggerBagsField.getText()));
                settings.setLateGameOfferFactorBoost(Double.parseDouble(lateGameOfferFactorBoostField.getText()));

                settings.setBagValueBiasEnabled(enableBagValueBiasCheckbox.isSelected());
                settings.setBagValueBiasMagnitude(bagValueBiasSlider.getValue());
                settings.setBagValueBiasStrength(Double.parseDouble(bagValueBiasStrengthField.getText()));
            }

            if (!settings.isAuthenticModeEnabled()) {
                if (settings.getMinBagValue() >= settings.getMaxBagValue() ||
                    settings.getMinOfferValue() >= settings.getMaxOfferValue() ||
                    settings.getNumberOfBags() < 2 || settings.getOfferRoundFrequency() < 1 ||
                    settings.getLateGameTriggerBags() < 1 || settings.getLateGameOfferFactorBoost() < 0 ||
                    settings.getBagValueBiasStrength() < 0) {
                    JOptionPane.showMessageDialog(this, "Input Error: Check values (Max > Min, Counts >= 1 or 2, Factors >= 0).", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                if (settings.getLateGameTriggerBags() >= settings.getNumberOfBags()) {
                    JOptionPane.showMessageDialog(this, "Late Game Trigger Bags must be less than total bags.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                long possibleUniqueValues = (long)settings.getMaxBagValue() - settings.getMinBagValue() + 1;
                if (settings.getNumberOfBags() > possibleUniqueValues) {
                    JOptionPane.showMessageDialog(this, "Number of bags exceeds possible unique values. Adjust Min/Max Bag Value or Number of Bags.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

            settings.saveSettings();
            JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred while saving settings: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    private void resetSettingsToDefault() {
        settings.resetToDefaultAuthentic();
        loadSettingsIntoFields();
        // toggleAuthenticMode(true) is called within loadSettingsIntoFields if authenticMode is true
        JOptionPane.showMessageDialog(this, "Settings reset to authentic game show defaults!", "Reset Successful", JOptionPane.INFORMATION_MESSAGE);
    }

    private void toggleAuthenticMode(boolean isAuthentic) {
        // Core Game Parameters
        numBagsField.setEnabled(!isAuthentic);
        minBagValueField.setEnabled(!isAuthentic);
        maxBagValueField.setEnabled(!isAuthentic);
        minOfferValueField.setEnabled(!isAuthentic);
        maxOfferValueField.setEnabled(!isAuthentic);
        offerFreqField.setEnabled(!isAuthentic);
        noTrackingRadio.setEnabled(!isAuthentic);
        fullTrackingRadio.setEnabled(!isAuthentic);
        valueOnlyTrackingRadio.setEnabled(!isAuthentic);
        allowBagSwapCheckbox.setEnabled(!isAuthentic);

        // Advanced Game Modifiers - Main checkboxes
        enableBankerBiasCheckbox.setEnabled(!isAuthentic);
        enableLateGameOfferCheckbox.setEnabled(!isAuthentic);
        enableBagValueBiasCheckbox.setEnabled(!isAuthentic);

        if (isAuthentic) {
            // Disable all sub-controls for advanced settings
            setBankerBiasControlsEnabled(false);
            setLateGameControlsEnabled(false);
            setBagValueBiasControlsEnabled(false);

            // Set UI fields to reflect authentic values (even though disabled)
            numBagsField.setText(String.valueOf(GameSettings.AUTHENTIC_BAG_VALUES.size()));
            minBagValueField.setText(String.valueOf(Collections.min(GameSettings.AUTHENTIC_BAG_VALUES)));
            maxBagValueField.setText(String.valueOf(Collections.max(GameSettings.AUTHENTIC_BAG_VALUES)));
            minOfferValueField.setText(String.valueOf(settings.getMinOfferValue())); // Use default authentic
            maxOfferValueField.setText(String.valueOf(settings.getMaxOfferValue())); // Use default authentic
            offerFreqField.setText("N/A (Authentic Rounds)");
            valueOnlyTrackingRadio.setSelected(true);
            allowBagSwapCheckbox.setSelected(false);

            enableBankerBiasCheckbox.setSelected(false);
            enableLateGameOfferCheckbox.setSelected(true); // Late game is part of authentic
            lateGameTriggerBagsField.setText(String.valueOf(settings.getLateGameTriggerBags()));
            lateGameOfferFactorBoostField.setText(String.valueOf(settings.getLateGameOfferFactorBoost()));
            enableBagValueBiasCheckbox.setSelected(false);

        } else {
            // When unchecking authentic mode, re-enable advanced sub-controls based on THEIR respective checkboxes
            setBankerBiasControlsEnabled(enableBankerBiasCheckbox.isSelected());
            setLateGameControlsEnabled(enableLateGameOfferCheckbox.isSelected());
            setBagValueBiasControlsEnabled(enableBagValueBiasCheckbox.isSelected());
            // The main checkboxes themselves are already enabled above.
            // No need to call loadSettingsIntoFields() here again, as it would cause a loop.
            // The values should persist from what was last saved or loaded.
        }
    }

    private void setBankerBiasControlsEnabled(boolean enabled) {
        bankerBiasSlider.setEnabled(enabled);
        bankerBiasDescriptionLabel.setEnabled(enabled);
    }

    private void updateBankerBiasDescriptionLabel(int sliderValue) {
        String description;
        if (sliderValue < 45) {
            int percent = (int) ((50.0 - sliderValue) / 50.0 * 100);
            description = String.format("Bias: %d%% Towards Minimum Offer", percent);
        } else if (sliderValue > 55) {
            int percent = (int) ((sliderValue - 50.0) / 50.0 * 100);
            description = String.format("Bias: %d%% Towards Maximum Offer", percent);
        } else {
            description = "Bias: Neutral (No significant bias)";
        }
        bankerBiasDescriptionLabel.setText(description);
    }

    private void setLateGameControlsEnabled(boolean enabled) {
        lateGameTriggerBagsField.setEnabled(enabled);
        lateGameOfferFactorBoostField.setEnabled(enabled);
    }

    private void setBagValueBiasControlsEnabled(boolean enabled) {
        bagValueBiasSlider.setEnabled(enabled);
        bagValueBiasDescriptionLabel.setEnabled(enabled);
        bagValueBiasStrengthField.setEnabled(enabled);
    }

    private void updateBagValueBiasDescriptionLabel(int sliderValue) {
        String description;
        if (sliderValue < 45) {
            int percent = (int) ((50.0 - sliderValue) / 50.0 * 100);
            description = String.format("Bias: %d%% Towards Lower Bag Values", percent);
        } else if (sliderValue > 55) {
            int percent = (int) ((sliderValue - 50.0) / 50.0 * 100);
            description = String.format("Bias: %d%% Towards Higher Bag Values", percent);
        } else {
            description = "Bias: Even Mix of Bag Values";
        }
        bagValueBiasDescriptionLabel.setText(description);
    }
}