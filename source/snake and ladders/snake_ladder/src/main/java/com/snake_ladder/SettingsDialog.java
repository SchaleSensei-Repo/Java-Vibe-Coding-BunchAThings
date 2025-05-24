package com.snake_ladder;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

public class SettingsDialog extends JDialog {
    private GameSettings settings;
    private boolean saved = false;

    // Input fields
    private JSpinner numPlayersSpinner;
    private JSpinner initialLivesSpinner, initialPointsSpinner;
    private JSpinner numDiceSpinner, numSidesPerDieSpinner;
    private JSpinner numTilesSpinner;
    private JCheckBox hideTileValuesCheck;
    private JCheckBox[] playerIsHumanChecks = new JCheckBox[8];
    private JTextField[] playerNamesFields = new JTextField[8];
    private JPanel playerSettingsPanel;


    // Tile effect value fields
    private JSpinner wfMin, wfMax, wfStaticVal; private JCheckBox wfStaticCheck;
    private JSpinner wbMin, wbMax, wbStaticVal; private JCheckBox wbStaticCheck;
    private JSpinner gpMin, gpMax, gpStaticVal; private JCheckBox gpStaticCheck;
    private JSpinner tpMin, tpMax, tpStaticVal; private JCheckBox tpStaticCheck;

    // Probabilities
    private EnumMap<TileEffect, JSpinner> probabilitySpinners = new EnumMap<>(TileEffect.class);

    // Game rules
    private JRadioButton winFirstToFinishRadio, winMostPointsRadio;
    private JCheckBox respawnCheck, instantFinishCheck;
    private JCheckBox allowNegativePointsCheck; // ADDED


    public SettingsDialog(Frame owner, GameSettings settings) {
        super(owner, "Game Settings", true);
        this.settings = settings;
        setLayout(new BorderLayout());
        setSize(700, 850); 
        setLocationRelativeTo(owner);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("General", createGeneralPanel());
        tabbedPane.addTab("Player Details", createPlayerDetailsPanel());
        tabbedPane.addTab("Tile Effects", createTileEffectsPanel());
        tabbedPane.addTab("Probabilities", createProbabilitiesPanel());
        tabbedPane.addTab("Rules", createRulesPanel());
        add(tabbedPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> { applySettings(); saved = true; setVisible(false); });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> { saved = false; setVisible(false); });
        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.addActionListener(e -> { settings.resetToDefaults(); loadSettingsToUI(); });
        buttonPanel.add(resetButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        loadSettingsToUI();
    }

    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        int gridy = 0;

        gbc.gridx = 0; gbc.gridy = gridy; panel.add(new JLabel("Number of Players (1-8):"), gbc);
        numPlayersSpinner = new JSpinner(new SpinnerNumberModel(settings.numPlayers, 1, 8, 1));
        gbc.gridx = 1; panel.add(numPlayersSpinner, gbc);
        numPlayersSpinner.addChangeListener(e -> updatePlayerSettingsPanelVisibility());
        gridy++;

        gbc.gridx = 0; gbc.gridy = gridy; panel.add(new JLabel("Initial Lives:"), gbc);
        initialLivesSpinner = new JSpinner(new SpinnerNumberModel(settings.initialLives, 1, 99, 1));
        gbc.gridx = 1; panel.add(initialLivesSpinner, gbc);
        gridy++;

        gbc.gridx = 0; gbc.gridy = gridy; panel.add(new JLabel("Initial Points:"), gbc);
        initialPointsSpinner = new JSpinner(new SpinnerNumberModel(settings.initialPoints, 0, 1000000, 1));
        gbc.gridx = 1; panel.add(initialPointsSpinner, gbc);
        gridy++;

        gbc.gridx = 0; gbc.gridy = gridy; panel.add(new JLabel("Number of Dice:"), gbc);
        numDiceSpinner = new JSpinner(new SpinnerNumberModel(settings.numDice, 1, 5, 1));
        gbc.gridx = 1; panel.add(numDiceSpinner, gbc);
        gridy++;

        gbc.gridx = 0; gbc.gridy = gridy; panel.add(new JLabel("Sides per Die:"), gbc);
        numSidesPerDieSpinner = new JSpinner(new SpinnerNumberModel(settings.numSidesPerDie, 2, 20, 1));
        gbc.gridx = 1; panel.add(numSidesPerDieSpinner, gbc);
        gridy++;

        gbc.gridx = 0; gbc.gridy = gridy; panel.add(new JLabel("Number of Tiles:"), gbc);
        numTilesSpinner = new JSpinner(new SpinnerNumberModel(settings.numTiles, 10, 500, 10));
        gbc.gridx = 1; panel.add(numTilesSpinner, gbc);
        gridy++;
        
        gbc.gridx = 0; gbc.gridy = gridy; panel.add(new JLabel("Hide Values on Tiles:"), gbc);
        hideTileValuesCheck = new JCheckBox();
        gbc.gridx = 1; panel.add(hideTileValuesCheck, gbc);
        
        return panel;
    }

    private JPanel createPlayerDetailsPanel() {
        playerSettingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2,2,2,2);
        gbc.anchor = GridBagConstraints.WEST;
        for(int i=0; i < 8; i++) {
            gbc.gridy = i;
            gbc.gridx = 0; playerSettingsPanel.add(new JLabel("Player " + (i+1) + ":"), gbc);
            gbc.gridx = 1; playerNamesFields[i] = new JTextField(settings.playerNames[i],10); playerSettingsPanel.add(playerNamesFields[i], gbc);
            gbc.gridx = 2; playerIsHumanChecks[i] = new JCheckBox("Human", settings.playerIsHuman[i]); playerSettingsPanel.add(playerIsHumanChecks[i], gbc);
        }
        updatePlayerSettingsPanelVisibility(); 
        return playerSettingsPanel;
    }

    private void updatePlayerSettingsPanelVisibility() {
        if (numPlayersSpinner == null || playerSettingsPanel == null) return; 
        int numActivePlayers = (int) numPlayersSpinner.getValue();
        for (int i = 0; i < 8; i++) {
            playerSettingsPanel.getComponent(i * 3).setVisible(i < numActivePlayers); 
            playerSettingsPanel.getComponent(i * 3 + 1).setVisible(i < numActivePlayers); 
            playerSettingsPanel.getComponent(i * 3 + 2).setVisible(i < numActivePlayers); 
        }
        playerSettingsPanel.revalidate();
        playerSettingsPanel.repaint();
    }

    private JPanel createTileEffectsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2,2,2,2);
        gbc.anchor = GridBagConstraints.WEST;
        int gridy = 0;

        gbc.gridx=0; gbc.gridy=gridy; panel.add(new JLabel("Warp Forward:"), gbc);
        wfMin = new JSpinner(new SpinnerNumberModel(settings.warpForwardMin,1,50,1)); 
        gbc.gridx=1; panel.add(wfMin, gbc);
        wfMax = new JSpinner(new SpinnerNumberModel(settings.warpForwardMax,1,50,1)); 
        gbc.gridx=2; panel.add(wfMax, gbc);
        wfStaticCheck = new JCheckBox("Static", settings.warpForwardStatic); 
        gbc.gridx=3; panel.add(wfStaticCheck, gbc);
        wfStaticVal = new JSpinner(new SpinnerNumberModel(settings.warpForwardStaticValue,1,50,1)); 
        gbc.gridx=4; panel.add(wfStaticVal, gbc);
        gridy++;

        gbc.gridx=0; gbc.gridy=gridy; panel.add(new JLabel("Warp Backward:"), gbc);
        wbMin = new JSpinner(new SpinnerNumberModel(settings.warpBackwardMin,1,50,1)); 
        gbc.gridx=1; panel.add(wbMin, gbc);
        wbMax = new JSpinner(new SpinnerNumberModel(settings.warpBackwardMax,1,50,1)); 
        gbc.gridx=2; panel.add(wbMax, gbc);
        wbStaticCheck = new JCheckBox("Static", settings.warpBackwardStatic); 
        gbc.gridx=3; panel.add(wbStaticCheck, gbc);
        wbStaticVal = new JSpinner(new SpinnerNumberModel(settings.warpBackwardStaticValue,1,50,1)); 
        gbc.gridx=4; panel.add(wbStaticVal, gbc);
        gridy++;

        gbc.gridx=0; gbc.gridy=gridy; panel.add(new JLabel("Give Points:"), gbc);
        gpMin = new JSpinner(new SpinnerNumberModel(settings.givePointsMin,1,5000,1)); 
        gbc.gridx=1; panel.add(gpMin, gbc);
        gpMax = new JSpinner(new SpinnerNumberModel(settings.givePointsMax,1,5000,1)); 
        gbc.gridx=2; panel.add(gpMax, gbc);
        gpStaticCheck = new JCheckBox("Static", settings.givePointsStatic); 
        gbc.gridx=3; panel.add(gpStaticCheck, gbc);
        gpStaticVal = new JSpinner(new SpinnerNumberModel(settings.givePointsStaticValue,1,5000,1)); 
        gbc.gridx=4; panel.add(gpStaticVal, gbc);
        gridy++;

        gbc.gridx=0; gbc.gridy=gridy; panel.add(new JLabel("Take Points:"), gbc);
        tpMin = new JSpinner(new SpinnerNumberModel(settings.takePointsMin,1,5000,1)); 
        gbc.gridx=1; panel.add(tpMin, gbc);
        tpMax = new JSpinner(new SpinnerNumberModel(settings.takePointsMax,1,5000,1)); 
        gbc.gridx=2; panel.add(tpMax, gbc);
        tpStaticCheck = new JCheckBox("Static", settings.takePointsStatic); 
        gbc.gridx=3; panel.add(tpStaticCheck, gbc);
        tpStaticVal = new JSpinner(new SpinnerNumberModel(settings.takePointsStaticValue,1,5000,1)); 
        gbc.gridx=4; panel.add(tpStaticVal, gbc);
        
        wfStaticVal.setEnabled(wfStaticCheck.isSelected());
        wbStaticVal.setEnabled(wbStaticCheck.isSelected());
        gpStaticVal.setEnabled(gpStaticCheck.isSelected());
        tpStaticVal.setEnabled(tpStaticCheck.isSelected());

        wfStaticCheck.addActionListener(e -> wfStaticVal.setEnabled(wfStaticCheck.isSelected()));
        wbStaticCheck.addActionListener(e -> wbStaticVal.setEnabled(wbStaticCheck.isSelected()));
        gpStaticCheck.addActionListener(e -> gpStaticVal.setEnabled(gpStaticCheck.isSelected()));
        tpStaticCheck.addActionListener(e -> tpStaticVal.setEnabled(tpStaticCheck.isSelected()));
        return panel;
    }

    private JComponent createProbabilitiesPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5)); 
        for (TileEffect effect : TileEffect.getRandomizableEffects()) {
            panel.add(new JLabel(effect.getDisplayName() + " Weight:"));
            int initialValue = settings.tileProbabilities.getOrDefault(effect, 10);
            int minValue = 0;
            int maxValue = 100;
            int stepSize = 1;
            SpinnerNumberModel model = new SpinnerNumberModel(initialValue, minValue, maxValue, stepSize);
            JSpinner spinner = new JSpinner(model);
            probabilitySpinners.put(effect, spinner);
            panel.add(spinner);
        }
        return new JScrollPane(panel); 
    }
    
    private JPanel createRulesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        int gridy = 0;

        gbc.gridx = 0; gbc.gridy = gridy; gbc.gridwidth = 2; panel.add(new JLabel("Win Condition:"), gbc);
        gridy++; gbc.gridwidth = 1;
        winFirstToFinishRadio = new JRadioButton("First to Finish Tile", settings.winCondition == GameSettings.WinCondition.FIRST_TO_FINISH);
        winMostPointsRadio = new JRadioButton("Most Points (when first player finishes)", settings.winCondition == GameSettings.WinCondition.MOST_POINTS_AT_FINISH);
        ButtonGroup winGroup = new ButtonGroup(); 
        winGroup.add(winFirstToFinishRadio); 
        winGroup.add(winMostPointsRadio);
        gbc.gridx = 0; gbc.gridy = gridy; panel.add(winFirstToFinishRadio, gbc);
        gbc.gridx = 1; panel.add(winMostPointsRadio, gbc);
        gridy++;

        respawnCheck = new JCheckBox("Respawn eliminated players at start (reset lives/points)", settings.respawnEliminatedPlayers);
        gbc.gridx = 0; gbc.gridy = gridy; gbc.gridwidth = 2; panel.add(respawnCheck, gbc);
        gridy++;

        instantFinishCheck = new JCheckBox("Instantly finish game if only one player remains active", settings.instantFinishIfOneLeft);
        gbc.gridx = 0; gbc.gridy = gridy; gbc.gridwidth = 2; panel.add(instantFinishCheck, gbc);
        gridy++;

        allowNegativePointsCheck = new JCheckBox("Allow points to go into negative values", settings.allowNegativePoints); // Initialize with setting
        gbc.gridx = 0; gbc.gridy = gridy; gbc.gridwidth = 2; panel.add(allowNegativePointsCheck, gbc);

        return panel;
    }

    private void loadSettingsToUI() {
        numPlayersSpinner.setValue(settings.numPlayers);
        initialLivesSpinner.setValue(settings.initialLives);
        initialPointsSpinner.setValue(settings.initialPoints);
        for(int i=0; i < 8; i++) {
            playerIsHumanChecks[i].setSelected(settings.playerIsHuman[i]);
            playerNamesFields[i].setText(settings.playerNames[i]);
        }
        updatePlayerSettingsPanelVisibility();

        numDiceSpinner.setValue(settings.numDice);
        numSidesPerDieSpinner.setValue(settings.numSidesPerDie);
        numTilesSpinner.setValue(settings.numTiles);
        hideTileValuesCheck.setSelected(settings.hideTileValuesOnBoard);

        wfMin.setValue(settings.warpForwardMin); wfMax.setValue(settings.warpForwardMax);
        wfStaticCheck.setSelected(settings.warpForwardStatic); wfStaticVal.setValue(settings.warpForwardStaticValue);
        wfStaticVal.setEnabled(settings.warpForwardStatic);

        wbMin.setValue(settings.warpBackwardMin); wbMax.setValue(settings.warpBackwardMax);
        wbStaticCheck.setSelected(settings.warpBackwardStatic); wbStaticVal.setValue(settings.warpBackwardStaticValue);
        wbStaticVal.setEnabled(settings.warpBackwardStatic);
        
        gpMin.setValue(settings.givePointsMin); gpMax.setValue(settings.givePointsMax);
        gpStaticCheck.setSelected(settings.givePointsStatic); gpStaticVal.setValue(settings.givePointsStaticValue);
        gpStaticVal.setEnabled(settings.givePointsStatic);

        tpMin.setValue(settings.takePointsMin); tpMax.setValue(settings.takePointsMax);
        tpStaticCheck.setSelected(settings.takePointsStatic); tpStaticVal.setValue(settings.takePointsStaticValue);
        tpStaticVal.setEnabled(settings.takePointsStatic);

        for (Map.Entry<TileEffect, JSpinner> entry : probabilitySpinners.entrySet()) {
            JSpinner spinner = entry.getValue();
            if (spinner != null) { 
                 spinner.setValue(settings.tileProbabilities.getOrDefault(entry.getKey(), 10));
            }
        }
        
        if (settings.winCondition == GameSettings.WinCondition.FIRST_TO_FINISH) {
            winFirstToFinishRadio.setSelected(true);
        } else {
            winMostPointsRadio.setSelected(true);
        }
        respawnCheck.setSelected(settings.respawnEliminatedPlayers);
        instantFinishCheck.setSelected(settings.instantFinishIfOneLeft);
        allowNegativePointsCheck.setSelected(settings.allowNegativePoints); // ADDED
    }

    private void applySettings() {
        settings.numPlayers = (int) numPlayersSpinner.getValue();
        settings.initialLives = (int) initialLivesSpinner.getValue();
        settings.initialPoints = (int) initialPointsSpinner.getValue();
        for(int i=0; i < 8; i++) {
            settings.playerIsHuman[i] = playerIsHumanChecks[i].isSelected();
            settings.playerNames[i] = playerNamesFields[i].getText();
        }
        settings.numDice = (int) numDiceSpinner.getValue();
        settings.numSidesPerDie = (int) numSidesPerDieSpinner.getValue();
        settings.numTiles = (int) numTilesSpinner.getValue();
        settings.hideTileValuesOnBoard = hideTileValuesCheck.isSelected();

        settings.warpForwardMin = (int) wfMin.getValue(); settings.warpForwardMax = (int) wfMax.getValue();
        settings.warpForwardStatic = wfStaticCheck.isSelected(); settings.warpForwardStaticValue = (int) wfStaticVal.getValue();
        settings.warpBackwardMin = (int) wbMin.getValue(); settings.warpBackwardMax = (int) wbMax.getValue();
        settings.warpBackwardStatic = wbStaticCheck.isSelected(); settings.warpBackwardStaticValue = (int) wbStaticVal.getValue();
        settings.givePointsMin = (int) gpMin.getValue(); settings.givePointsMax = (int) gpMax.getValue();
        settings.givePointsStatic = gpStaticCheck.isSelected(); settings.givePointsStaticValue = (int) gpStaticVal.getValue();
        settings.takePointsMin = (int) tpMin.getValue(); settings.takePointsMax = (int) tpMax.getValue();
        settings.takePointsStatic = tpStaticCheck.isSelected(); settings.takePointsStaticValue = (int) tpStaticVal.getValue();

        for (Map.Entry<TileEffect, JSpinner> entry : probabilitySpinners.entrySet()) {
             if (entry.getValue() != null) { 
                settings.tileProbabilities.put(entry.getKey(), (Integer) entry.getValue().getValue());
            }
        }
        settings.winCondition = winFirstToFinishRadio.isSelected() ? GameSettings.WinCondition.FIRST_TO_FINISH : GameSettings.WinCondition.MOST_POINTS_AT_FINISH;
        settings.respawnEliminatedPlayers = respawnCheck.isSelected();
        settings.instantFinishIfOneLeft = instantFinishCheck.isSelected();
        settings.allowNegativePoints = allowNegativePointsCheck.isSelected(); // ADDED

        settings.saveSettings();
    }

    public boolean wereSettingsSaved() { return saved; }
}