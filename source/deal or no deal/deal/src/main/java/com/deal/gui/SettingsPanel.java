package com.deal.gui;

import com.deal.model.GameSettings;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {
    private GameSettings settings;
    private GameFrame parentFrame;

    private JTextField numBagsField;
    private JTextField minBagValueField;
    private JTextField maxBagValueField;
    private JTextField minOfferValueField;
    private JTextField maxOfferValueField;
    private JTextField offerFreqField;
    private JRadioButton noTrackingRadio; // New
    private JRadioButton fullTrackingRadio; // New
    private JRadioButton valueOnlyTrackingRadio; // New


    public SettingsPanel(GameSettings settings, GameFrame parentFrame) {
        this.settings = settings;
        this.parentFrame = parentFrame;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("Game Settings", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        gbc.gridwidth = 1; // Reset to 1 column width

        // Number of Bags
        gbc.gridy++;
        add(new JLabel("Number of Bags:"), gbc);
        gbc.gridx = 1;
        numBagsField = new JTextField(String.valueOf(settings.getNumberOfBags()), 10);
        add(numBagsField, gbc);

        // Min Bag Value
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Min Bag Value:"), gbc);
        gbc.gridx = 1;
        minBagValueField = new JTextField(String.valueOf(settings.getMinBagValue()), 10);
        add(minBagValueField, gbc);

        // Max Bag Value
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Max Bag Value:"), gbc);
        gbc.gridx = 1;
        maxBagValueField = new JTextField(String.valueOf(settings.getMaxBagValue()), 10);
        add(maxBagValueField, gbc);

        // Min Offer Value
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Min Offer Value:"), gbc);
        gbc.gridx = 1;
        minOfferValueField = new JTextField(String.valueOf(settings.getMinOfferValue()), 10);
        add(minOfferValueField, gbc);

        // Max Offer Value
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Max Offer Value:"), gbc);
        gbc.gridx = 1;
        maxOfferValueField = new JTextField(String.valueOf(settings.getMaxOfferValue()), 10);
        add(maxOfferValueField, gbc);


        // Offer Frequency
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Offer Frequency (bags per round):"), gbc);
        gbc.gridx = 1;
        offerFreqField = new JTextField(String.valueOf(settings.getOfferRoundFrequency()), 10);
        add(offerFreqField, gbc);

        // Tracking Options (New)
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel trackingOptionsPanel = new JPanel(new GridLayout(3, 1)); // Use GridLayout for radio buttons
        trackingOptionsPanel.setBorder(BorderFactory.createTitledBorder("Bag Tracking Options"));

        ButtonGroup trackingGroup = new ButtonGroup();
        noTrackingRadio = new JRadioButton("No Tracking");
        fullTrackingRadio = new JRadioButton("Full Tracking (ID & Value)");
        valueOnlyTrackingRadio = new JRadioButton("Value-Only Tracking (no ID)");

        trackingGroup.add(noTrackingRadio);
        trackingGroup.add(fullTrackingRadio);
        trackingGroup.add(valueOnlyTrackingRadio);

        trackingOptionsPanel.add(noTrackingRadio);
        trackingOptionsPanel.add(fullTrackingRadio);
        trackingOptionsPanel.add(valueOnlyTrackingRadio);
        add(trackingOptionsPanel, gbc);


        // Buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

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
        add(buttonPanel, gbc);

        loadSettingsIntoFields(); // Load settings into UI fields on creation
    }

    private void loadSettingsIntoFields() {
        numBagsField.setText(String.valueOf(settings.getNumberOfBags()));
        minBagValueField.setText(String.valueOf(settings.getMinBagValue()));
        maxBagValueField.setText(String.valueOf(settings.getMaxBagValue()));
        minOfferValueField.setText(String.valueOf(settings.getMinOfferValue()));
        maxOfferValueField.setText(String.valueOf(settings.getMaxOfferValue()));
        offerFreqField.setText(String.valueOf(settings.getOfferRoundFrequency()));

        switch (settings.getTrackingMode()) {
            case 0: noTrackingRadio.setSelected(true); break;
            case 1: fullTrackingRadio.setSelected(true); break;
            case 2: valueOnlyTrackingRadio.setSelected(true); break;
        }
    }

    private boolean saveSettings() {
        try {
            settings.setNumberOfBags(Integer.parseInt(numBagsField.getText()));
            settings.setMinBagValue(Integer.parseInt(minBagValueField.getText()));
            settings.setMaxBagValue(Integer.parseInt(maxBagValueField.getText()));
            settings.setMinOfferValue(Integer.parseInt(minOfferValueField.getText()));
            settings.setMaxOfferValue(Integer.parseInt(maxOfferValueField.getText()));
            settings.setOfferRoundFrequency(Integer.parseInt(offerFreqField.getText()));

            if (noTrackingRadio.isSelected()) {
                settings.setTrackingMode(0);
            } else if (fullTrackingRadio.isSelected()) {
                settings.setTrackingMode(1);
            } else if (valueOnlyTrackingRadio.isSelected()) {
                settings.setTrackingMode(2);
            }

            // Basic validation (more robust validation needed for production)
            if (settings.getMinBagValue() >= settings.getMaxBagValue() ||
                settings.getMinOfferValue() >= settings.getMaxOfferValue() ||
                settings.getNumberOfBags() < 2 || settings.getOfferRoundFrequency() < 1) {
                JOptionPane.showMessageDialog(this, "Please check your input values. Max must be greater than Min. Number of bags must be at least 2. Offer frequency must be at least 1.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            settings.saveSettings();
            JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}