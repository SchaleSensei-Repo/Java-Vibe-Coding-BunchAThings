package com.BingoGameApp;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.function.Function;

public class SettingsPanel extends JPanel {
    private BingoSettings settings;
    private JTextField minCardNumField, maxCardNumField;
    private JTextField exactPointsField, minRandomPointsField, maxRandomPointsField;

    // NEW: Generate Button Points fields
    private JCheckBox awardPointsOnGenerateCheckbox;
    private JRadioButton exactGeneratePointsBtn, randomGeneratePointsBtn;
    private JTextField exactGeneratePointsField, minGeneratePointsField, maxGeneratePointsField;

    // NEW: Bingo Bonus Points fields
    private JCheckBox awardPointsOnBingoCheckbox;
    private JRadioButton exactBingoPointsBtn, randomBingoPointsBtn;
    private JTextField exactBingoPointsField, minBingoPointsField, maxBingoPointsField;


    private JSpinner numbersPerGenSpinner;
    private JRadioButton soloPlayerBtn, twoPlayersBtn;
    private JCheckBox autoSelectNumbersCheckbox;
    private JSpinner bingosNeededSpinner;

    private Consumer<BingoSettings> startGameCallback;
    private final JRadioButton exactPointsBtn;
    private final JRadioButton randomPointsBtn;

    public SettingsPanel(BingoSettings settings, Consumer<BingoSettings> startGameCallback) {
        this.settings = settings;
        this.startGameCallback = startGameCallback;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // --- Player Count ---
        addLabel(this, "Players:", gbc, 0, row);
        soloPlayerBtn = new JRadioButton("Solo");
        twoPlayersBtn = new JRadioButton("2 Players");
        ButtonGroup playerGroup = new ButtonGroup();
        playerGroup.add(soloPlayerBtn);
        playerGroup.add(twoPlayersBtn);
        soloPlayerBtn.addActionListener(e -> settings.setPlayerCount(1));
        twoPlayersBtn.addActionListener(e -> settings.setPlayerCount(2));
        JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        playerPanel.add(soloPlayerBtn);
        playerPanel.add(twoPlayersBtn);
        gbc.gridx = 1;
        gbc.gridy = row++;
        add(playerPanel, gbc);

        // --- Numbers per Generation ---
        addLabel(this, "Numbers per Generation:", gbc, 0, row);
        numbersPerGenSpinner = new JSpinner(new SpinnerNumberModel(settings.getNumbersPerGeneration(), 1, 10, 1));
        numbersPerGenSpinner.addChangeListener(e -> settings.setNumbersPerGeneration((Integer) numbersPerGenSpinner.getValue()));
        gbc.gridx = 1;
        gbc.gridy = row++;
        add(numbersPerGenSpinner, gbc);

        // --- Points for marking a number on card ---
        addLabel(this, "Points for Card Match:", gbc, 0, row);
        exactPointsBtn = new JRadioButton("Exact Points");
        randomPointsBtn = new JRadioButton("Randomized Points");
        ButtonGroup pointsGroup = new ButtonGroup();
        pointsGroup.add(exactPointsBtn);
        pointsGroup.add(randomPointsBtn);

        exactPointsField = new JTextField(5);
        minRandomPointsField = new JTextField(5);
        maxRandomPointsField = new JTextField(5);

        exactPointsBtn.addActionListener(e -> {
            settings.setRandomizePoints(false);
            toggleCardMatchPointFields();
        });
        randomPointsBtn.addActionListener(e -> {
            settings.setRandomizePoints(true);
            toggleCardMatchPointFields();
        });
        
        JPanel exactPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        exactPanel.add(exactPointsBtn);
        exactPanel.add(new JLabel("Value:"));
        exactPanel.add(exactPointsField);

        JPanel randomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        randomPanel.add(randomPointsBtn);
        randomPanel.add(new JLabel("Min:"));
        randomPanel.add(minRandomPointsField);
        randomPanel.add(new JLabel("Max:"));
        randomPanel.add(maxRandomPointsField);

        gbc.gridx = 1;
        gbc.gridy = row++;
        add(exactPanel, gbc);
        gbc.gridy = row++;
        add(randomPanel, gbc);

        addIntegerValidation(exactPointsField, settings::setExactPoints, null);
        addIntegerValidation(minRandomPointsField, settings::setMinRandomPoints, null);
        addIntegerValidation(maxRandomPointsField, settings::setMaxRandomPoints, null);
        
        toggleCardMatchPointFields(); // Initial state

        // --- NEW: Points for clicking Generate button ---
        addSeparator(this, gbc, row++);
        awardPointsOnGenerateCheckbox = new JCheckBox("Award Points on Generate Button Click");
        awardPointsOnGenerateCheckbox.addActionListener(e -> {
            settings.setAwardPointsOnGenerate(awardPointsOnGenerateCheckbox.isSelected());
            toggleGeneratePointFields();
        });
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; add(awardPointsOnGenerateCheckbox, gbc);
        gbc.gridwidth = 1;

        addLabel(this, "Generate Btn Points:", gbc, 0, row);
        exactGeneratePointsBtn = new JRadioButton("Exact Points");
        randomGeneratePointsBtn = new JRadioButton("Randomized Points");
        ButtonGroup generatePointsGroup = new ButtonGroup();
        generatePointsGroup.add(exactGeneratePointsBtn);
        generatePointsGroup.add(randomGeneratePointsBtn);

        exactGeneratePointsField = new JTextField(5);
        minGeneratePointsField = new JTextField(5);
        maxGeneratePointsField = new JTextField(5);

        exactGeneratePointsBtn.addActionListener(e -> {
            settings.setRandomizeGeneratePoints(false);
            toggleGeneratePointFields();
        });
        randomGeneratePointsBtn.addActionListener(e -> {
            settings.setRandomizeGeneratePoints(true);
            toggleGeneratePointFields();
        });

        JPanel exactGeneratePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        exactGeneratePanel.add(exactGeneratePointsBtn);
        exactGeneratePanel.add(new JLabel("Value:"));
        exactGeneratePanel.add(exactGeneratePointsField);

        JPanel randomGeneratePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        randomGeneratePanel.add(randomGeneratePointsBtn);
        randomGeneratePanel.add(new JLabel("Min:"));
        randomGeneratePanel.add(minGeneratePointsField);
        randomGeneratePanel.add(new JLabel("Max:"));
        randomGeneratePanel.add(maxGeneratePointsField);

        gbc.gridx = 1;
        gbc.gridy = row++;
        add(exactGeneratePanel, gbc);
        gbc.gridy = row++;
        add(randomGeneratePanel, gbc);

        addIntegerValidation(exactGeneratePointsField, settings::setExactGeneratePoints, null);
        addIntegerValidation(minGeneratePointsField, settings::setMinGeneratePoints, null);
        addIntegerValidation(maxGeneratePointsField, settings::setMaxGeneratePoints, null);
        
        toggleGeneratePointFields(); // Initial state

        // --- NEW: Additional points for scoring a Bingo ---
        addSeparator(this, gbc, row++);
        awardPointsOnBingoCheckbox = new JCheckBox("Award Additional Points on Bingo");
        awardPointsOnBingoCheckbox.addActionListener(e -> {
            settings.setAwardPointsOnBingo(awardPointsOnBingoCheckbox.isSelected());
            toggleBingoPointFields();
        });
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; add(awardPointsOnBingoCheckbox, gbc);
        gbc.gridwidth = 1;

        addLabel(this, "Bingo Bonus Points:", gbc, 0, row);
        exactBingoPointsBtn = new JRadioButton("Exact Points");
        randomBingoPointsBtn = new JRadioButton("Randomized Points");
        ButtonGroup bingoPointsGroup = new ButtonGroup();
        bingoPointsGroup.add(exactBingoPointsBtn);
        bingoPointsGroup.add(randomBingoPointsBtn);

        exactBingoPointsField = new JTextField(5);
        minBingoPointsField = new JTextField(5);
        maxBingoPointsField = new JTextField(5);

        exactBingoPointsBtn.addActionListener(e -> {
            settings.setRandomizeBingoPoints(false);
            toggleBingoPointFields();
        });
        randomBingoPointsBtn.addActionListener(e -> {
            settings.setRandomizeBingoPoints(true);
            toggleBingoPointFields();
        });
        
        JPanel exactBingoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        exactBingoPanel.add(exactBingoPointsBtn);
        exactBingoPanel.add(new JLabel("Value:"));
        exactBingoPanel.add(exactBingoPointsField);

        JPanel randomBingoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        randomBingoPanel.add(randomBingoPointsBtn);
        randomBingoPanel.add(new JLabel("Min:"));
        randomBingoPanel.add(minBingoPointsField);
        randomBingoPanel.add(new JLabel("Max:"));
        randomBingoPanel.add(maxBingoPointsField);

        gbc.gridx = 1;
        gbc.gridy = row++;
        add(exactBingoPanel, gbc);
        gbc.gridy = row++;
        add(randomBingoPanel, gbc);

        addIntegerValidation(exactBingoPointsField, settings::setExactBingoPoints, null);
        addIntegerValidation(minBingoPointsField, settings::setMinBingoPoints, null);
        addIntegerValidation(maxBingoPointsField, settings::setMaxBingoPoints, null);

        toggleBingoPointFields(); // Initial state

        // --- Number Range for Cards ---
        addSeparator(this, gbc, row++);
        addLabel(this, "Card Number Range:", gbc, 0, row);
        minCardNumField = new JTextField(5);
        maxCardNumField = new JTextField(5);

        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rangePanel.add(new JLabel("Min:"));
        rangePanel.add(minCardNumField);
        rangePanel.add(new JLabel("Max:"));
        rangePanel.add(maxCardNumField);
        gbc.gridx = 1;
        gbc.gridy = row++;
        add(rangePanel, gbc);

        // Card number range must be non-negative
        addIntegerValidation(minCardNumField, settings::setMinCardNumber, val -> val >= 0);
        addIntegerValidation(maxCardNumField, settings::setMaxCardNumber, val -> val >= 0);

        // --- Bingos Needed to Win ---
        addLabel(this, "Bingos to Win:", gbc, 0, row);
        bingosNeededSpinner = new JSpinner(new SpinnerNumberModel(settings.getBingosNeededToWin(), 1, 12, 1));
        bingosNeededSpinner.addChangeListener(e -> settings.setBingosNeededToWin((Integer) bingosNeededSpinner.getValue()));
        gbc.gridx = 1;
        gbc.gridy = row++;
        add(bingosNeededSpinner, gbc);

        // --- Auto/Manual Select Option ---
        autoSelectNumbersCheckbox = new JCheckBox("Auto-select numbers on card (when generated)");
        autoSelectNumbersCheckbox.addActionListener(e -> settings.setAutoSelectNumbers(autoSelectNumbersCheckbox.isSelected()));
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        add(autoSelectNumbersCheckbox, gbc);
        gbc.gridwidth = 1;

        // --- Save and Start Buttons ---
        JButton saveButton = new JButton("Save Settings");
        saveButton.addActionListener(e -> {
            applyUiToSettings();
            settings.saveSettings();
        });

        JButton startGameButton = new JButton("Start Game");
        startGameButton.addActionListener(e -> {
            applyUiToSettings();
            if (validateSettings()) {
                startGameCallback.accept(settings);
            } else {
                JOptionPane.showMessageDialog(this, "Please correct invalid settings before starting.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(saveButton);
        buttonPanel.add(startGameButton);
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        // Initial population from settings
        populateUiFromSettings();
    }

    private void addLabel(JPanel panel, String text, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(text), gbc);
    }

    private void addSeparator(JPanel panel, GridBagConstraints gbc, int y) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JSeparator(), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
    }

    private void toggleCardMatchPointFields() {
        exactPointsField.setEnabled(!randomPointsBtn.isSelected());
        minRandomPointsField.setEnabled(randomPointsBtn.isSelected());
        maxRandomPointsField.setEnabled(randomPointsBtn.isSelected());
    }

    private void toggleGeneratePointFields() {
        boolean awardChecked = awardPointsOnGenerateCheckbox.isSelected();
        exactGeneratePointsBtn.setEnabled(awardChecked);
        randomGeneratePointsBtn.setEnabled(awardChecked);
        exactGeneratePointsField.setEnabled(awardChecked && !randomGeneratePointsBtn.isSelected());
        minGeneratePointsField.setEnabled(awardChecked && randomGeneratePointsBtn.isSelected());
        maxGeneratePointsField.setEnabled(awardChecked && randomGeneratePointsBtn.isSelected());
    }

    private void toggleBingoPointFields() {
        boolean awardChecked = awardPointsOnBingoCheckbox.isSelected();
        exactBingoPointsBtn.setEnabled(awardChecked);
        randomBingoPointsBtn.setEnabled(awardChecked);
        exactBingoPointsField.setEnabled(awardChecked && !randomBingoPointsBtn.isSelected());
        minBingoPointsField.setEnabled(awardChecked && randomBingoPointsBtn.isSelected());
        maxBingoPointsField.setEnabled(awardChecked && randomBingoPointsBtn.isSelected());
    }

    private void addIntegerValidation(JTextField field, Consumer<Integer> setter, Function<Integer, Boolean> additionalValidation) {
        ActionListener validateAction = e -> parseAndValidate(field, setter, additionalValidation);
        field.addActionListener(validateAction);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                parseAndValidate(field, setter, additionalValidation);
            }
        });
    }

    private void parseAndValidate(JTextField field, Consumer<Integer> setter, Function<Integer, Boolean> additionalValidation) {
        try {
            int value = Integer.parseInt(field.getText());
            if (additionalValidation != null && !additionalValidation.apply(value)) {
                throw new NumberFormatException("Additional validation failed.");
            }
            setter.accept(value);
            field.setBackground(Color.WHITE);
            field.setToolTipText(null);
        } catch (NumberFormatException ex) {
            field.setBackground(Color.RED);
            String msg = "Please enter a valid integer.";
            if (additionalValidation != null) {
                msg += " (Must meet additional criteria)";
            }
            field.setToolTipText(msg);
        }
    }

    private void populateUiFromSettings() {
        soloPlayerBtn.setSelected(settings.getPlayerCount() == 1);
        twoPlayersBtn.setSelected(settings.getPlayerCount() == 2);
        numbersPerGenSpinner.setValue(settings.getNumbersPerGeneration());

        // Card Match Points
        exactPointsBtn.setSelected(!settings.isRandomizePoints());
        randomPointsBtn.setSelected(settings.isRandomizePoints());
        exactPointsField.setText(String.valueOf(settings.getExactPoints()));
        minRandomPointsField.setText(String.valueOf(settings.getMinRandomPoints()));
        maxRandomPointsField.setText(String.valueOf(settings.getMaxRandomPoints()));
        toggleCardMatchPointFields();

        // Generate Button Points
        awardPointsOnGenerateCheckbox.setSelected(settings.isAwardPointsOnGenerate());
        exactGeneratePointsBtn.setSelected(!settings.isRandomizeGeneratePoints());
        randomGeneratePointsBtn.setSelected(settings.isRandomizeGeneratePoints());
        exactGeneratePointsField.setText(String.valueOf(settings.getExactGeneratePoints()));
        minGeneratePointsField.setText(String.valueOf(settings.getMinGeneratePoints()));
        maxGeneratePointsField.setText(String.valueOf(settings.getMaxGeneratePoints()));
        toggleGeneratePointFields();

        // Bingo Bonus Points
        awardPointsOnBingoCheckbox.setSelected(settings.isAwardPointsOnBingo());
        exactBingoPointsBtn.setSelected(!settings.isRandomizeBingoPoints());
        randomBingoPointsBtn.setSelected(settings.isRandomizeBingoPoints());
        exactBingoPointsField.setText(String.valueOf(settings.getExactBingoPoints()));
        minBingoPointsField.setText(String.valueOf(settings.getMinBingoPoints()));
        maxBingoPointsField.setText(String.valueOf(settings.getMaxBingoPoints()));
        toggleBingoPointFields();

        minCardNumField.setText(String.valueOf(settings.getMinCardNumber()));
        maxCardNumField.setText(String.valueOf(settings.getMaxCardNumber()));
        bingosNeededSpinner.setValue(settings.getBingosNeededToWin());
        autoSelectNumbersCheckbox.setSelected(settings.isAutoSelectNumbers());
    }

    private void applyUiToSettings() {
        parseAndValidate(exactPointsField, settings::setExactPoints, null);
        parseAndValidate(minRandomPointsField, settings::setMinRandomPoints, null);
        parseAndValidate(maxRandomPointsField, settings::setMaxRandomPoints, null);

        parseAndValidate(exactGeneratePointsField, settings::setExactGeneratePoints, null);
        parseAndValidate(minGeneratePointsField, settings::setMinGeneratePoints, null);
        parseAndValidate(maxGeneratePointsField, settings::setMaxGeneratePoints, null);

        parseAndValidate(exactBingoPointsField, settings::setExactBingoPoints, null);
        parseAndValidate(minBingoPointsField, settings::setMinBingoPoints, null);
        parseAndValidate(maxBingoPointsField, settings::setMaxBingoPoints, null);

        parseAndValidate(minCardNumField, settings::setMinCardNumber, val -> val >= 0);
        parseAndValidate(maxCardNumField, settings::setMaxCardNumber, val -> val >= 0);
    }

    private boolean validateSettings() {
        if (exactPointsField.getBackground().equals(Color.RED) ||
            minRandomPointsField.getBackground().equals(Color.RED) ||
            maxRandomPointsField.getBackground().equals(Color.RED) ||
            exactGeneratePointsField.getBackground().equals(Color.RED) ||
            minGeneratePointsField.getBackground().equals(Color.RED) ||
            maxGeneratePointsField.getBackground().equals(Color.RED) ||
            exactBingoPointsField.getBackground().equals(Color.RED) ||
            minBingoPointsField.getBackground().equals(Color.RED) ||
            maxBingoPointsField.getBackground().equals(Color.RED) ||
            minCardNumField.getBackground().equals(Color.RED) ||
            maxCardNumField.getBackground().equals(Color.RED)) {
            JOptionPane.showMessageDialog(this, "Please correct highlighted (red) fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (settings.getMaxCardNumber() < settings.getMinCardNumber() + 24) {
            JOptionPane.showMessageDialog(this, "Max card number must be at least " + (settings.getMinCardNumber() + 24) + " to generate a 5x5 card.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        if (settings.isRandomizePoints() && settings.getMinRandomPoints() > settings.getMaxRandomPoints()) {
            JOptionPane.showMessageDialog(this, "Card Match: Min random points cannot be greater than maximum random points.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (settings.isAwardPointsOnGenerate() && settings.isRandomizeGeneratePoints() && settings.getMinGeneratePoints() > settings.getMaxGeneratePoints()) {
            JOptionPane.showMessageDialog(this, "Generate Button: Min random points cannot be greater than maximum random points.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (settings.isAwardPointsOnBingo() && settings.isRandomizeBingoPoints() && settings.getMinBingoPoints() > settings.getMaxBingoPoints()) {
            JOptionPane.showMessageDialog(this, "Bingo Bonus: Min random points cannot be greater than maximum random points.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        return true;
    }
}