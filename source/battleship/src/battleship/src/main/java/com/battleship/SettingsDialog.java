package com.battleship;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class SettingsDialog extends JDialog {
    private GameSettings settings;
    private BattleshipGameGUI gameGUI; // Reference to the main GUI to trigger restarts

    private JTextField rowsField;
    private JTextField colsField;
    private Map<String, JTextField> shipLengthFields;
    private Map<String, JTextField> shipQuantityFields;
    private JTextField firesPerTurnField;
    private JCheckBox dynamicFiresCheckbox;

    public SettingsDialog(JFrame parent, GameSettings settings, BattleshipGameGUI gameGUI) {
        super(parent, "Game Settings", true); // true makes it modal
        this.settings = settings;
        this.gameGUI = gameGUI;
        this.shipLengthFields = new HashMap<>();
        this.shipQuantityFields = new HashMap<>();

        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Close on X button

        JTabbedPane tabbedPane = new JTabbedPane();

        // --- Board Settings Tab ---
        JPanel boardPanel = new JPanel(new GridBagLayout());
        boardPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        boardPanel.add(new JLabel("Board Rows:"), gbc);
        gbc.gridx = 1;
        rowsField = new JTextField(String.valueOf(settings.boardRows), 5);
        boardPanel.add(rowsField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        boardPanel.add(new JLabel("Board Columns:"), gbc);
        gbc.gridx = 1;
        colsField = new JTextField(String.valueOf(settings.boardCols), 5);
        boardPanel.add(colsField, gbc);

        tabbedPane.addTab("Board", boardPanel);

        // --- Ship Settings Tab ---
        JPanel shipsPanel = new JPanel(new GridBagLayout());
        shipsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5;
        shipsPanel.add(new JLabel("Ship Type"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.25;
        shipsPanel.add(new JLabel("Length"), gbc);
        gbc.gridx = 2; gbc.weightx = 0.25;
        shipsPanel.add(new JLabel("Quantity"), gbc);

        int rowOffset = 1;
        // Collect all ship types from current settings and default settings
        List<String> allShipTypes = new ArrayList<>(settings.shipConfigs.keySet());
        // FIX: Use the public getter for defaultShipSettings
        for (String defaultShipType : settings.getDefaultShipSettings().keySet()) {
             if (!allShipTypes.contains(defaultShipType)) {
                 allShipTypes.add(defaultShipType);
             }
        }
        // Sort ship types for consistent display (e.g., by default length then alphabetically)
        Collections.sort(allShipTypes, (s1, s2) -> {
            // FIX: Use the public getter for defaultShipSettings
            GameSettings.ShipConfig config1 = settings.shipConfigs.getOrDefault(s1, new GameSettings.ShipConfig(Integer.parseInt(settings.getDefaultShipSettings().getOrDefault(s1, "0,0").split(",")[0]), 0));
            GameSettings.ShipConfig config2 = settings.shipConfigs.getOrDefault(s2, new GameSettings.ShipConfig(Integer.parseInt(settings.getDefaultShipSettings().getOrDefault(s2, "0,0").split(",")[0]), 0));
            int lengthCompare = Integer.compare(config2.length(), config1.length()); // Sort by length descending
            if (lengthCompare != 0) return lengthCompare;
            return s1.compareTo(s2); // Then alphabetically
        });


        for (String shipType : allShipTypes) {
            GameSettings.ShipConfig shipConfig = settings.shipConfigs.getOrDefault(shipType, new GameSettings.ShipConfig(0,0)); // Get current or default 0,0
            
            gbc.gridx = 0; gbc.gridy = rowOffset; gbc.weightx = 0.5;
            shipsPanel.add(new JLabel(shipType.replace("_", " ").substring(0, 1).toUpperCase() + shipType.replace("_", " ").substring(1) + ":"), gbc);

            gbc.gridx = 1; gbc.weightx = 0.25;
            JTextField lengthField = new JTextField(String.valueOf(shipConfig.length()), 3);
            shipsPanel.add(lengthField, gbc);
            shipLengthFields.put(shipType, lengthField);

            gbc.gridx = 2; gbc.weightx = 0.25;
            JTextField quantityField = new JTextField(String.valueOf(shipConfig.quantity()), 3);
            shipsPanel.add(quantityField, gbc);
            shipQuantityFields.put(shipType, quantityField);

            rowOffset++;
        }
        tabbedPane.addTab("Ships", shipsPanel);

        // --- Gameplay Settings Tab ---
        JPanel gameplayPanel = new JPanel(new GridBagLayout());
        gameplayPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        gbc = new GridBagConstraints(); // Reset GBC for this panel
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        gameplayPanel.add(new JLabel("Fixed Fires per Turn:"), gbc);
        gbc.gridx = 1;
        firesPerTurnField = new JTextField(String.valueOf(settings.firesPerTurn), 5);
        gameplayPanel.add(firesPerTurnField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        dynamicFiresCheckbox = new JCheckBox("Dynamic Fires (shots = unsunk ships)", settings.dynamicFires);
        gameplayPanel.add(dynamicFiresCheckbox, gbc);

        tabbedPane.addTab("Gameplay", gameplayPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // --- Action Buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save & Apply");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndApply();
            }
        });
        buttonPanel.add(saveButton);

        JButton resetButton = new JButton("Reset to Default");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetSettings();
            }
        });
        buttonPanel.add(resetButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Close the dialog
            }
        });
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        pack(); // Adjusts window size to fit components
        setLocationRelativeTo(parent); // Center relative to parent frame
    }

    private void saveAndApply() {
        try {
            int newRows = Integer.parseInt(rowsField.getText());
            int newCols = Integer.parseInt(colsField.getText());
            if (newRows <= 0 || newCols <= 0) {
                throw new IllegalArgumentException("Board dimensions must be positive.");
            }
            settings.boardRows = newRows;
            settings.boardCols = newCols;

            Map<String, GameSettings.ShipConfig> newShipConfigs = new LinkedHashMap<>(); // Use LinkedHashMap to preserve order for GUI
            // Iterate over the sorted list of ship types used to populate the GUI
            List<String> sortedShipTypes = new ArrayList<>(shipLengthFields.keySet());
            Collections.sort(sortedShipTypes, (s1, s2) -> {
                // Re-sort based on current lengths if values have changed (optional, but good for consistency)
                int l1 = Integer.parseInt(shipLengthFields.get(s1).getText());
                int l2 = Integer.parseInt(shipLengthFields.get(s2).getText());
                int lengthCompare = Integer.compare(l2, l1); // Sort by length descending
                if (lengthCompare != 0) return lengthCompare;
                return s1.compareTo(s2); // Then alphabetically
            });

            for (String shipType : sortedShipTypes) {
                int length = Integer.parseInt(shipLengthFields.get(shipType).getText());
                int quantity = Integer.parseInt(shipQuantityFields.get(shipType).getText());
                if (length < 0) { // Length can be 0 if quantity is 0
                    throw new IllegalArgumentException(shipType.replace("_", " ").substring(0, 1).toUpperCase() + shipType.replace("_", " ").substring(1) + " length cannot be negative.");
                }
                if (quantity < 0) {
                    throw new IllegalArgumentException(shipType.replace("_", " ").substring(0, 1).toUpperCase() + shipType.replace("_", " ").substring(1) + " quantity cannot be negative.");
                }
                newShipConfigs.put(shipType, new GameSettings.ShipConfig(length, quantity));
            }
            settings.shipConfigs = newShipConfigs;

            int newFiresPerTurn = Integer.parseInt(firesPerTurnField.getText());
            if (newFiresPerTurn <= 0) {
                throw new IllegalArgumentException("Fires per turn must be positive.");
            }
            settings.firesPerTurn = newFiresPerTurn;
            settings.dynamicFires = dynamicFiresCheckbox.isSelected();

            settings.saveSettings();
            JOptionPane.showMessageDialog(this, "Settings saved and applied. A new game will start with these settings.", "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            // FIX: Correctly access public ownBoardFrame and targetBoardFrame
            gameGUI.createBoardGrid(gameGUI.ownBoardFrame, gameGUI.ownBoardButtons, false);
            gameGUI.createBoardGrid(gameGUI.targetBoardFrame, gameGUI.targetBoardButtons, true);
            gameGUI.startNewGame(); // Trigger game restart in main GUI
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for all fields.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Validation Error: " + ex.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void resetSettings() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to reset all settings to default? This will restart the game.", "Reset Settings", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            settings.resetToDefault();
            JOptionPane.showMessageDialog(this, "Settings reset to default. A new game will start with default settings.", "Settings Reset", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            // FIX: Correctly access public ownBoardFrame and targetBoardFrame
            gameGUI.createBoardGrid(gameGUI.ownBoardFrame, gameGUI.ownBoardButtons, false);
            gameGUI.createBoardGrid(gameGUI.targetBoardFrame, gameGUI.targetBoardButtons, true);
            gameGUI.startNewGame(); // Trigger game restart in main GUI
        }
    }
}