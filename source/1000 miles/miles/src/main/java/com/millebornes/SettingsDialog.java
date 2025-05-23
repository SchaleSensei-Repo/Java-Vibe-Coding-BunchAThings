package com.millebornes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SettingsDialog extends JDialog {
    private SettingsManager settingsManager;
    private JTextField goalDistanceField;
    private JCheckBox redrawHandCheckbox;
    private JCheckBox reshufflePileCheckbox;
    private ButtonGroup gameModeGroup;
    private JRadioButton soloModeRadio;
    private JRadioButton twoPlayerModeRadio;

    public SettingsDialog(JFrame parent, SettingsManager settingsManager) {
        super(parent, "Settings", true); // true for modal dialog
        this.settingsManager = settingsManager;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(350, 280);
        setLocationRelativeTo(parent); // Center relative to parent

        setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Goal Distance
        JPanel goalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        goalPanel.add(new JLabel("Goal Distance (miles):"));
        goalDistanceField = new JTextField(5);
        goalDistanceField.setText(String.valueOf(settingsManager.getGoalDistance()));
        goalPanel.add(goalDistanceField);
        contentPanel.add(goalPanel);

        // Redraw Empty Hand
        redrawHandCheckbox = new JCheckBox("Redraw cards if hand empty (custom rule)");
        redrawHandCheckbox.setSelected(settingsManager.isRedrawEmptyHand());
        contentPanel.add(redrawHandCheckbox);

        // Reshuffle Empty Pile
        reshufflePileCheckbox = new JCheckBox("Reshuffle discard pile if draw pile empty");
        reshufflePileCheckbox.setSelected(settingsManager.isReshuffleEmptyPile());
        contentPanel.add(reshufflePileCheckbox);

        // Game Mode
        JPanel modePanel = new JPanel();
        modePanel.setLayout(new BoxLayout(modePanel, BoxLayout.Y_AXIS));
        modePanel.setBorder(BorderFactory.createTitledBorder("Game Mode"));
        
        gameModeGroup = new ButtonGroup();
        soloModeRadio = new JRadioButton(GameMode.SOLO.getDisplayName());
        soloModeRadio.setActionCommand(GameMode.SOLO.name());
        twoPlayerModeRadio = new JRadioButton(GameMode.TWO_PLAYER_HOTSEAT.getDisplayName());
        twoPlayerModeRadio.setActionCommand(GameMode.TWO_PLAYER_HOTSEAT.name());

        gameModeGroup.add(soloModeRadio);
        gameModeGroup.add(twoPlayerModeRadio);
        modePanel.add(soloModeRadio);
        modePanel.add(twoPlayerModeRadio);

        if (settingsManager.getGameMode() == GameMode.SOLO) {
            soloModeRadio.setSelected(true);
        } else {
            twoPlayerModeRadio.setSelected(true);
        }
        contentPanel.add(modePanel);

        add(contentPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
            }
        });
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Close the dialog
            }
        });
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void saveSettings() {
        try {
            int newGoal = Integer.parseInt(goalDistanceField.getText());
            if (newGoal <= 0) {
                JOptionPane.showMessageDialog(this, "Goal distance must be a positive number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if game mode changed
            GameMode oldMode = settingsManager.getGameMode();
            GameMode newMode = GameMode.valueOf(gameModeGroup.getSelection().getActionCommand());

            settingsManager.setGoalDistance(newGoal);
            settingsManager.setRedrawEmptyHand(redrawHandCheckbox.isSelected());
            settingsManager.setReshuffleEmptyPile(reshufflePileCheckbox.isSelected());
            settingsManager.setGameMode(newMode);
            settingsManager.saveSettings();

            JOptionPane.showMessageDialog(this, "Settings saved.", "Success", JOptionPane.INFORMATION_MESSAGE);

            if (oldMode != newMode) {
                JOptionPane.showMessageDialog(this, "Game mode has changed. Please start a New Game for changes to take effect.", "Game Mode Change", JOptionPane.INFORMATION_MESSAGE);
            }
            dispose(); // Close the dialog
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for Goal Distance.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}