package com.slot;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat; // Added for number formatting

public class Slot_Machine extends JFrame {

    // --- Game Settings & State ---
    private static final String SETTINGS_FILE = "slot_settings.ini";

    // Descriptive names for the slot machine symbols
    private static final String[] SYMBOL_NAMES = {"cherry", "orange", "lemon", "bell", "star", "seven"};

    // Path prefix for loading symbol images from resources
    private static final String SYMBOL_IMAGE_PATH_PREFIX = "/com/slot/resources/images/";

    // Map to store loaded ImageIcons for each symbol name
    private Map<String, ImageIcon> symbolImageMap = new HashMap<>();

    // Colors for the symbols (primarily for background highlight or other visual cues)
    private static final Map<String, Color> SYMBOL_COLORS = new HashMap<>();
    static {
        SYMBOL_COLORS.put("cherry", new Color(220, 20, 60)); // Crimson
        SYMBOL_COLORS.put("orange", new Color(255, 140, 0)); // Dark Orange
        SYMBOL_COLORS.put("lemon", new Color(255, 255, 0)); // Yellow
        SYMBOL_COLORS.put("bell", new Color(255, 215, 0)); // Gold
        SYMBOL_COLORS.put("star", new Color(0, 191, 255)); // Deep Sky Blue
        SYMBOL_COLORS.put("seven", new Color(34, 139, 34)); // Forest Green
    }

    // Default Payouts (now multipliers)
    private final Map<String, Double> defaultPayouts = new LinkedHashMap<>();
    {
        defaultPayouts.put("cherry", 10.0);
        defaultPayouts.put("orange", 15.0);
        defaultPayouts.put("lemon", 20.0);
        defaultPayouts.put("bell", 25.0);
        defaultPayouts.put("star", 50.0);
        defaultPayouts.put("seven", 100.0);
    }

    // Line definitions for checking wins (coordinates: [row][col])
    private static final List<int[][]> LINES = new ArrayList<>();
    static {
        // Horizontal lines (rows 0, 1, 2)
        LINES.add(new int[][]{{0, 0}, {0, 1}, {0, 2}}); // Line 1: Top (index 0)
        LINES.add(new int[][]{{1, 0}, {1, 1}, {1, 2}}); // Line 2: Middle (index 1)
        LINES.add(new int[][]{{2, 0}, {2, 1}, {2, 2}}); // Line 3: Bottom (index 2)

        // Diagonal lines
        LINES.add(new int[][]{{0, 0}, {1, 1}, {2, 2}}); // Line 4: Diagonal TL-BR (index 3)
        LINES.add(new int[][]{{2, 0}, {1, 1}, {0, 2}}); // Line 5: Diagonal BL-TR (index 4)

        // Vertical lines for 8-line option
        LINES.add(new int[][]{{0, 0}, {1, 0}, {2, 0}}); // Line 6: Vertical Left (index 5)
        LINES.add(new int[][]{{0, 1}, {1, 1}, {2, 1}}); // Line 7: Vertical Middle (index 6)
        LINES.add(new int[][]{{0, 2}, {1, 2}, {2, 2}}); // Line 8: Vertical Right (index 7)
    }

    // Configurable settings (values are read from/written to settings.ini)
    private int initialPointsSetting = 1000;
    private int paymentPerSpinSetting = 10; // This is now 'Payment per Line'
    private int linesPlayedSetting = 1;
    private Map<String, Double> currentPayouts = new HashMap<>(defaultPayouts);
    private int customRollsSetting = 1;
    private boolean fixedSpinCostSetting = false;

    // Game state variables (dynamic, not directly saved to settings.ini)
    private int currentPoints;
    private String[][] currentReelSymbols = new String[3][3];
    private Random random = new Random();

    // --- GUI Components ---
    private JLabel currentPointsLabel;
    private JLabel lastWinLossLabel;
    private JLabel messageLabel;
    private JLabel[][] reelDisplayLabels = new JLabel[3][3];

    // Settings GUI components
    private JTextField initialPointsField;
    private JTextField paymentPerSpinField;
    private JRadioButton oneLineRadio, threeLinesRadio, fiveLinesRadio, eightLinesRadio;
    private Map<String, JTextField> payoutFields = new LinkedHashMap<>();
    private JTextField customRollsField;
    private JCheckBox fixedSpinCostCheckbox;

    // Control buttons (roll buttons are specific to the "Roll Buttons" tab)
    private JButton[] rollButtons;
    private JButton resetButton; // Located in settings panel
    private JButton saveSettingsButton; // Located in settings panel

    // --- Winning Log Components ---
    private DefaultTableModel winningLogTableModel;
    private JTable winningLogTable;
    // Removed DateTimeFormatter as time column is removed from log

    // Inner class to hold spin result details for logging
    private static class WinDetails {
        private int winAmount;
        private int linesWon;
        private Set<String> winningSymbols; // Use Set to store unique winning symbols

        public WinDetails(int winAmount, int linesWon, Set<String> winningSymbols) {
            this.winAmount = winAmount;
            this.linesWon = linesWon;
            this.winningSymbols = winningSymbols;
        }

        public int getWinAmount() { return winAmount; }
        public int getLinesWon() { return linesWon; }
        public Set<String> getWinningSymbols() { return winningSymbols; }
    }

    public Slot_Machine() {
        setTitle("Java Casino Slot Machine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        loadSymbolImages();

        // --- Main Layout ---
        setLayout(new BorderLayout(10, 10));

        // 1. Info Panel (NORTH)
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        currentPointsLabel = new JLabel("Points: Loading...");
        currentPointsLabel.setFont(new Font("Arial", Font.BOLD, 18));
        lastWinLossLabel = new JLabel("Last: ---");
        lastWinLossLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        messageLabel = new JLabel("Loading settings...");
        messageLabel.setFont(new Font("Arial", Font.ITALIC, 16));

        infoPanel.add(currentPointsLabel);
        infoPanel.add(new JSeparator(SwingConstants.VERTICAL));
        infoPanel.add(lastWinLossLabel);
        infoPanel.add(new JSeparator(SwingConstants.VERTICAL));
        infoPanel.add(messageLabel);
        add(infoPanel, BorderLayout.NORTH);

        loadSettings();
        currentPoints = initialPointsSetting;
        updateInfoPanel();

        // 2. Reels Panel (CENTER)
        JPanel reelsPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        reelsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 20, 10, 20),
                BorderFactory.createLineBorder(Color.DARK_GRAY, 3)
        ));

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                JLabel label = new JLabel();
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);

                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                        new EmptyBorder(2, 2, 2, 2)
                ));
                label.setOpaque(true);
                label.setBackground(Color.WHITE);
                reelDisplayLabels[r][c] = label;
                reelsPanel.add(label);

                currentReelSymbols[r][c] = SYMBOL_NAMES[random.nextInt(SYMBOL_NAMES.length)];
                reelDisplayLabels[r][c].setIcon(symbolImageMap.get(currentReelSymbols[r][c]));
            }
        }
        add(reelsPanel, BorderLayout.CENTER);

        // --- JTabbedPane for controls and log ---
        JTabbedPane bottomTabbedPane = new JTabbedPane();
        add(bottomTabbedPane, BorderLayout.SOUTH);

        // --- Tab 1: Game Settings ---
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Game Settings (Press Enter or click Save to Apply)"));
        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.fill = GridBagConstraints.HORIZONTAL;
        sgbc.insets = new Insets(3, 3, 3, 3);

        int sRow = 0;
        sgbc.gridx = 0; sgbc.gridy = sRow; settingsPanel.add(new JLabel("Initial Points:"), sgbc);
        sgbc.gridx = 1; initialPointsField = new JTextField(String.valueOf(initialPointsSetting), 8); settingsPanel.add(initialPointsField, sgbc);
        sRow++;

        sgbc.gridx = 0; sgbc.gridy = sRow; settingsPanel.add(new JLabel("Payment per Line:"), sgbc);
        sgbc.gridx = 1; paymentPerSpinField = new JTextField(String.valueOf(paymentPerSpinSetting), 8); settingsPanel.add(paymentPerSpinField, sgbc);
        sRow++;

        sgbc.gridx = 0; sgbc.gridy = sRow; settingsPanel.add(new JLabel("Lines Played:"), sgbc);
        JPanel lineRadioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        ButtonGroup lineGroup = new ButtonGroup();
        oneLineRadio = new JRadioButton("1 Line");
        threeLinesRadio = new JRadioButton("3 Lines");
        fiveLinesRadio = new JRadioButton("5 Lines");
        eightLinesRadio = new JRadioButton("8 Lines");

        lineGroup.add(oneLineRadio); lineGroup.add(threeLinesRadio); lineGroup.add(fiveLinesRadio); lineGroup.add(eightLinesRadio);
        lineRadioPanel.add(oneLineRadio); lineRadioPanel.add(threeLinesRadio); lineRadioPanel.add(fiveLinesRadio); lineRadioPanel.add(eightLinesRadio);

        sgbc.gridx = 1; settingsPanel.add(lineRadioPanel, sgbc);
        sRow++;

        sgbc.gridx = 0; sgbc.gridy = sRow;
        sgbc.gridwidth = 2;
        fixedSpinCostCheckbox = new JCheckBox("Fixed Spin Cost (Base per Spin)");
        settingsPanel.add(fixedSpinCostCheckbox, sgbc);
        sgbc.gridwidth = 1;
        sRow++;

        // IMPORTANT: Call updateLinesRadioButtons() AFTER all radio buttons and checkbox are initialized
        updateLinesRadioButtons();

        sgbc.gridx = 0; sgbc.gridy = sRow;
        sgbc.gridwidth = 2;
        settingsPanel.add(new JLabel("Payout Multipliers (3-match):"), sgbc);
        sRow++;

        JPanel payoutPanel = new JPanel(new GridBagLayout());
        GridBagConstraints pgbc = new GridBagConstraints();
        pgbc.insets = new Insets(2, 8, 2, 8);
        pgbc.anchor = GridBagConstraints.WEST;
        Font payoutLabelFont = new Font("Arial", Font.PLAIN, 14);

        int pCol = 0;
        int pRow = 0;
        for (String symbolName : SYMBOL_NAMES) {
            pgbc.gridx = pCol; pgbc.gridy = pRow; pgbc.weightx = 0; pgbc.fill = GridBagConstraints.NONE;
            JLabel symbolLabel = new JLabel(symbolName + ":");
            symbolLabel.setFont(payoutLabelFont);
            payoutPanel.add(symbolLabel, pgbc);

            pgbc.gridx = pCol + 1; pgbc.gridy = pRow; pgbc.weightx = 1.0; pgbc.fill = GridBagConstraints.HORIZONTAL;
            JTextField payoutField = new JTextField(String.valueOf(currentPayouts.getOrDefault(symbolName, 0.0)), 4);
            payoutFields.put(symbolName, payoutField);
            payoutPanel.add(payoutField, pgbc);

            if (pCol == 0) { pCol = 2; } else { pCol = 0; pRow++; }
        }

        sgbc.gridx = 0; sgbc.gridy = sRow; sgbc.gridwidth = 2; sgbc.weightx = 1.0;
        settingsPanel.add(payoutPanel, sgbc);
        sgbc.gridwidth = 1; sRow++;

        sgbc.gridx = 0; sgbc.gridy = sRow; settingsPanel.add(new JLabel("Custom Rolls (Number):"), sgbc);
        sgbc.gridx = 1; customRollsField = new JTextField(String.valueOf(customRollsSetting), 8); settingsPanel.add(customRollsField, sgbc);
        sRow++;

        // --- Reset and Save Buttons are now here in the settings panel ---
        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        resetButton = new JButton("Reset Points");
        resetButton.addActionListener(e -> resetPoints());
        actionButtonsPanel.add(resetButton);

        saveSettingsButton = new JButton("Save Settings");
        saveSettingsButton.addActionListener(e -> saveSettings());
        actionButtonsPanel.add(saveSettingsButton);

        sgbc.gridx = 0; sgbc.gridy = sRow;
        sgbc.gridwidth = 2;
        sgbc.anchor = GridBagConstraints.EAST;
        settingsPanel.add(actionButtonsPanel, sgbc);
        // sRow is not incremented here as this is the last component for settingsPanel

        bottomTabbedPane.addTab("Game Settings", settingsPanel);

        // --- Tab 2: Roll Buttons (formerly "Controls") ---
        JPanel rollButtonsTabPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        rollButtonsTabPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        rollButtons = new JButton[6];
        String[] rollTexts = {"1 Roll", "3 Rolls", "5 Rolls", "10 Rolls", "100 Rolls", "Custom"};
        int[] rollNumbers = {1, 3, 5, 10, 100, -1};

        for (int i = 0; i < rollButtons.length; i++) {
            JButton button = new JButton(rollTexts[i]);
            int numRolls = rollNumbers[i];
            button.addActionListener(e -> startRolls(numRolls));
            rollButtons[i] = button;
            rollButtonsTabPanel.add(button);
        }

        bottomTabbedPane.addTab("Roll Buttons", rollButtonsTabPanel);

        // --- Tab 3: Win Log ---
        JPanel winLogPanel = new JPanel(new BorderLayout());
        winLogPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- MODIFIED: Removed "Time" column ---
        String[] columnNames = {"Points Won", "# Lines", "Winning Symbols"};
        winningLogTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        winningLogTable = new JTable(winningLogTableModel);
        winningLogTable.setFillsViewportHeight(true);
        winningLogTable.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // --- MODIFIED: Adjusted column widths after removing "Time" ---
        winningLogTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Points Won
        winningLogTable.getColumnModel().getColumn(1).setPreferredWidth(80); // # Lines
        winningLogTable.getColumnModel().getColumn(2).setPreferredWidth(200); // Winning Symbols

        JScrollPane scrollPane = new JScrollPane(winningLogTable);
        scrollPane.setPreferredSize(new Dimension(500, 150));

        winLogPanel.add(scrollPane, BorderLayout.CENTER);
        bottomTabbedPane.addTab("Win Log", winLogPanel);

        // Add action listeners for settings changes (on Enter press or radio button click)
        ActionListener applySettingsListener = e -> applySettings();
        initialPointsField.addActionListener(applySettingsListener);
        paymentPerSpinField.addActionListener(applySettingsListener);
        oneLineRadio.addActionListener(applySettingsListener);
        threeLinesRadio.addActionListener(applySettingsListener);
        fiveLinesRadio.addActionListener(applySettingsListener);
        eightLinesRadio.addActionListener(applySettingsListener);
        fixedSpinCostCheckbox.addActionListener(applySettingsListener);
        customRollsField.addActionListener(applySettingsListener);
        for (JTextField field : payoutFields.values()) {
            field.addActionListener(applySettingsListener);
        }

        pack();
        setLocationRelativeTo(null);
    }

    private void loadSymbolImages() {
        for (String symbolName : SYMBOL_NAMES) {
            String imageFileName = symbolName + ".png";
            URL imageUrl = getClass().getResource(SYMBOL_IMAGE_PATH_PREFIX + imageFileName);
            if (imageUrl != null) {
                ImageIcon icon = new ImageIcon(imageUrl);
                Image image = icon.getImage();
                Image scaledImage = image.getScaledInstance(70, 70, Image.SCALE_SMOOTH);
                symbolImageMap.put(symbolName, new ImageIcon(scaledImage));
            } else {
                System.err.println("Error: Image not found for symbol '" + symbolName + "' at " + SYMBOL_IMAGE_PATH_PREFIX + imageFileName);
                symbolImageMap.put(symbolName, new ImageIcon(new BufferedImage(70, 70, BufferedImage.TYPE_INT_ARGB))); // Transparent placeholder
            }
        }
    }

    private void updateLinesRadioButtons() {
        if (linesPlayedSetting == 1) oneLineRadio.setSelected(true);
        else if (linesPlayedSetting == 3) threeLinesRadio.setSelected(true);
        else if (linesPlayedSetting == 5) fiveLinesRadio.setSelected(true);
        else if (linesPlayedSetting == 8) eightLinesRadio.setSelected(true);
        fixedSpinCostCheckbox.setSelected(fixedSpinCostSetting);
    }

    private void applySettings() {
        boolean settingsChanged = false;
        try {
            int newInitialPoints = Integer.parseInt(initialPointsField.getText());
            if (newInitialPoints > 0) {
                if (initialPointsSetting != newInitialPoints) { initialPointsSetting = newInitialPoints; settingsChanged = true; }
            } else { messageLabel.setText("Initial Points must be > 0."); initialPointsField.setText(String.valueOf(initialPointsSetting)); }
        } catch (NumberFormatException ex) { messageLabel.setText("Invalid Initial Points. Enter a number."); initialPointsField.setText(String.valueOf(initialPointsSetting)); }

        try {
            int newPayment = Integer.parseInt(paymentPerSpinField.getText());
            if (newPayment > 0) {
                if (paymentPerSpinSetting != newPayment) { paymentPerSpinSetting = newPayment; settingsChanged = true; }
            } else { messageLabel.setText("Payment per line must be > 0."); paymentPerSpinField.setText(String.valueOf(paymentPerSpinSetting)); }
        } catch (NumberFormatException ex) { messageLabel.setText("Invalid Payment per line. Enter a number."); paymentPerSpinField.setText(String.valueOf(paymentPerSpinSetting)); }

        int oldLinesSetting = linesPlayedSetting;
        if (oneLineRadio.isSelected()) linesPlayedSetting = 1;
        else if (threeLinesRadio.isSelected()) linesPlayedSetting = 3;
        else if (fiveLinesRadio.isSelected()) linesPlayedSetting = 5;
        else if (eightLinesRadio.isSelected()) linesPlayedSetting = 8;
        if (oldLinesSetting != linesPlayedSetting) { settingsChanged = true; }

        boolean oldFixedSpinCostSetting = fixedSpinCostSetting;
        fixedSpinCostSetting = fixedSpinCostCheckbox.isSelected();
        if (oldFixedSpinCostSetting != fixedSpinCostSetting) { settingsChanged = true; }

        Map<String, Double> newPayouts = new HashMap<>();
        boolean payoutError = false;
        boolean payoutChanged = false;
        for (Map.Entry<String, JTextField> entry : payoutFields.entrySet()) {
            String symbol = entry.getKey();
            JTextField field = entry.getValue();
            try {
                double payout = Double.parseDouble(field.getText());
                if (payout >= 0.0) {
                    newPayouts.put(symbol, payout);
                    if (currentPayouts.getOrDefault(symbol, 0.0).doubleValue() != payout) { payoutChanged = true; }
                } else { messageLabel.setText("Payout multiplier for " + symbol + " must be >= 0."); payoutError = true; field.setText(String.valueOf(currentPayouts.getOrDefault(symbol, 0.0))); }
            } catch (NumberFormatException ex) { messageLabel.setText("Invalid payout multiplier for " + symbol + ". Enter a decimal number."); payoutError = true; field.setText(String.valueOf(currentPayouts.getOrDefault(symbol, 0.0))); }
        }
        if (!payoutError) {
            if (payoutChanged || !currentPayouts.equals(newPayouts)) {
                currentPayouts.clear();
                currentPayouts.putAll(newPayouts);
                settingsChanged = true;
            }
        }

        try {
            int newCustomRolls = Integer.parseInt(customRollsField.getText());
            if (newCustomRolls > 0) {
                if (customRollsSetting != newCustomRolls) { customRollsSetting = newCustomRolls; settingsChanged = true; }
            } else { messageLabel.setText("Custom Rolls must be > 0."); customRollsField.setText(String.valueOf(customRollsSetting)); }
        } catch (NumberFormatException ex) { messageLabel.setText("Invalid Custom Rolls. Enter a number."); customRollsField.setText(String.valueOf(customRollsSetting)); }

        if (settingsChanged && !payoutError) { messageLabel.setText("Settings applied."); } else if (!payoutError) { messageLabel.setText("No settings changed."); }
    }

    private void resetPoints() {
        currentPoints = initialPointsSetting;
        // --- NEW: Clear the log table on reset ---
        winningLogTableModel.setRowCount(0);
        updateInfoPanel();
        messageLabel.setText("Points reset to initial setting.");
    }

    private void startRolls(int numRolls) {
        final int rollsToPerform = (numRolls == -1) ? customRollsSetting : numRolls;

        int costPerSingleSpin = fixedSpinCostSetting ? paymentPerSpinSetting : (paymentPerSpinSetting * linesPlayedSetting);
        int totalCost = costPerSingleSpin * rollsToPerform;

        if (currentPoints < totalCost) {
            messageLabel.setText("Not enough points for " + rollsToPerform + " rolls! Need " + NumberFormat.getIntegerInstance().format(totalCost) + " points.");
            return;
        }

        setControlsEnabled(false);
        new SpinWorker(rollsToPerform).execute();
    }

    private void setControlsEnabled(boolean enabled) {
        for (JButton button : rollButtons) { button.setEnabled(enabled); }
        resetButton.setEnabled(enabled);
        saveSettingsButton.setEnabled(enabled);
        initialPointsField.setEnabled(enabled);
        paymentPerSpinField.setEnabled(enabled);
        oneLineRadio.setEnabled(enabled);
        threeLinesRadio.setEnabled(enabled);
        fiveLinesRadio.setEnabled(enabled);
        eightLinesRadio.setEnabled(enabled);
        fixedSpinCostCheckbox.setEnabled(enabled);
        customRollsField.setEnabled(enabled);
        for(JTextField field : payoutFields.values()) { field.setEnabled(enabled); }
    }

    private class SpinWorker extends SwingWorker<Void, Void> {
        private final int rollsToPerform;

        public SpinWorker(int rolls) { this.rollsToPerform = rolls; }

        @Override
        protected Void doInBackground() throws Exception {
            for (int i = 0; i < rollsToPerform; i++) {
                spinReels();
                Thread.sleep(300);
            }
            return null;
        }

        @Override
        protected void done() {
            setControlsEnabled(true);
            try {
                get();
                messageLabel.setText("Finished " + rollsToPerform + " rolls. Current points: " + NumberFormat.getIntegerInstance().format(currentPoints));
            } catch (InterruptedException | ExecutionException e) {
                messageLabel.setText("Error during rolls: " + e.getMessage());
                e.printStackTrace();
            }
            updateInfoPanel();
        }
    }

    private void spinReels() {
        unhighlightAllLines();

        int costPerSpin = fixedSpinCostSetting ? paymentPerSpinSetting : (paymentPerSpinSetting * linesPlayedSetting);
        currentPoints -= costPerSpin;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                currentReelSymbols[r][c] = SYMBOL_NAMES[random.nextInt(SYMBOL_NAMES.length)];
                reelDisplayLabels[r][c].setIcon(symbolImageMap.get(currentReelSymbols[r][c]));
            }
        }

        WinDetails winDetails = checkWin();
        currentPoints += winDetails.getWinAmount();

        updateInfoPanel(costPerSpin, winDetails.getWinAmount());
        logWinDetails(winDetails); // Log the spin result
    }

    private WinDetails checkWin() {
        double totalRawWinThisSpin = 0.0;
        int linesMatched = 0;
        Set<String> uniqueWinningSymbols = new HashSet<>();

        List<int[][]> linesToCheck = new ArrayList<>();
        if (linesPlayedSetting == 1) { linesToCheck.add(LINES.get(1)); }
        else if (linesPlayedSetting == 3) { linesToCheck.add(LINES.get(0)); linesToCheck.add(LINES.get(1)); linesToCheck.add(LINES.get(2)); }
        else if (linesPlayedSetting == 5) { linesToCheck.addAll(LINES.subList(0, 5)); }
        else if (linesPlayedSetting == 8) { linesToCheck.addAll(LINES); }

        for (int[][] line : linesToCheck) {
            String sym1 = currentReelSymbols[line[0][0]][line[0][1]];
            String sym2 = currentReelSymbols[line[1][0]][line[1][1]];
            String sym3 = currentReelSymbols[line[2][0]][line[2][1]];

            if (sym1.equals(sym2) && sym2.equals(sym3)) {
                linesMatched++;
                double payoutMultiplier = currentPayouts.getOrDefault(sym1, 0.0);
                totalRawWinThisSpin += (payoutMultiplier * paymentPerSpinSetting);
                uniqueWinningSymbols.add(sym1);
                highlightWinningLine(line);
            }
        }
        return new WinDetails((int) Math.round(totalRawWinThisSpin), linesMatched, uniqueWinningSymbols);
    }

    private void highlightWinningLine(int[][] line) {
        for (int[] coord : line) { reelDisplayLabels[coord[0]][coord[1]].setBackground(Color.YELLOW); }

        javax.swing.Timer timer = new javax.swing.Timer(700, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int[] coord : line) {
                    if (reelDisplayLabels[coord[0]][coord[1]].getBackground() == Color.YELLOW) {
                        reelDisplayLabels[coord[0]][coord[1]].setBackground(Color.WHITE);
                    }
                }
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void unhighlightAllLines() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                reelDisplayLabels[r][c].setBackground(Color.WHITE);
            }
        }
    }

    private void updateInfoPanel() { updateInfoPanel(-1, -1); }

    private void updateInfoPanel(int cost, int win) {
        // --- MODIFIED: Format currentPoints with thousand separator ---
        currentPointsLabel.setText("Points: " + NumberFormat.getIntegerInstance().format(currentPoints));
        if (cost == -1 && win == -1) {
            lastWinLossLabel.setText("Last: ---");
            messageLabel.setText("Welcome! Adjust settings or click a Roll button.");
        } else {
            int netChange = win - cost;
            if (netChange > 0) {
                lastWinLossLabel.setText("Last: +" + netChange + " (Won " + win + ")");
                messageLabel.setText("You won!");
            } else if (netChange < 0) {
                lastWinLossLabel.setText("Last: " + netChange + " (Lost " + cost + ")");
                messageLabel.setText("Better luck next time!");
            } else {
                lastWinLossLabel.setText("Last: " + netChange + " (Broke Even)");
                messageLabel.setText("Broke even!");
            }
        }
    }

    // Method to log spin results to the table
    private void logWinDetails(WinDetails details) {
        // --- If no points were won, do not log ---
        if (details.getWinAmount() <= 0) {
            return;
        }

        // Removed timestamp as time column is no longer present
        // String timestamp = LocalDateTime.now().format(timeFormatter);

        String symbolsString = details.getWinningSymbols().isEmpty() ? "---" : String.join(", ", details.getWinningSymbols());

        // --- MODIFIED: Removed timestamp from row data ---
        winningLogTableModel.addRow(new Object[]{
            details.getWinAmount(),
            details.getLinesWon(),
            symbolsString
        });

        // Auto-scroll to the bottom of the table
        int lastRow = winningLogTable.getRowCount() - 1;
        if (lastRow >= 0) {
            winningLogTable.scrollRectToVisible(winningLogTable.getCellRect(lastRow, 0, true));
        }
    }

    private void loadSettings() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            properties.load(fis);

            initialPointsSetting = Integer.parseInt(properties.getProperty("initialPoints", String.valueOf(initialPointsSetting)));
            paymentPerSpinSetting = Integer.parseInt(properties.getProperty("paymentPerSpin", String.valueOf(paymentPerSpinSetting)));
            linesPlayedSetting = Integer.parseInt(properties.getProperty("linesPlayed", String.valueOf(linesPlayedSetting)));
            customRollsSetting = Integer.parseInt(properties.getProperty("customRolls", String.valueOf(customRollsSetting)));
            fixedSpinCostSetting = Boolean.parseBoolean(properties.getProperty("fixedSpinCost", String.valueOf(fixedSpinCostSetting)));

            currentPayouts.clear();
            for (String symbolName : SYMBOL_NAMES) {
                double payout = Double.parseDouble(properties.getProperty("payout." + symbolName, String.valueOf(defaultPayouts.getOrDefault(symbolName, 0.0))));
                currentPayouts.put(symbolName, payout);
            }

            messageLabel.setText("Settings loaded successfully.");
            System.out.println("Settings loaded successfully from " + SETTINGS_FILE);
        } catch (FileNotFoundException e) {
            System.out.println("Settings file not found, using default settings and creating a new one.");
            saveSettings();
            messageLabel.setText("Settings file created with defaults.");
        } catch (IOException | NumberFormatException e) {
            messageLabel.setText("Error loading settings: " + e.getMessage() + ". Defaulting settings.");
            System.err.println("Error loading settings from " + SETTINGS_FILE + ": " + e.getMessage());
            currentPayouts.clear();
            currentPayouts.putAll(defaultPayouts);
            initialPointsSetting = 1000;
            paymentPerSpinSetting = 10;
            linesPlayedSetting = 1;
            customRollsSetting = 1;
            fixedSpinCostSetting = false;
        }
    }

    private void saveSettings() {
        Properties properties = new Properties();
        properties.setProperty("initialPoints", String.valueOf(initialPointsSetting));
        properties.setProperty("paymentPerSpin", String.valueOf(paymentPerSpinSetting));
        properties.setProperty("linesPlayed", String.valueOf(linesPlayedSetting));
        properties.setProperty("customRolls", String.valueOf(customRollsSetting));
        properties.setProperty("fixedSpinCost", String.valueOf(fixedSpinCostSetting));

        for (Map.Entry<String, Double> entry : currentPayouts.entrySet()) {
            properties.setProperty("payout." + entry.getKey(), String.valueOf(entry.getValue()));
        }

        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(fos, "Slot Machine Game Settings");
            messageLabel.setText("Settings saved to " + SETTINGS_FILE);
            System.out.println("Settings saved successfully to " + SETTINGS_FILE);
        } catch (IOException e) {
            messageLabel.setText("Error saving settings: " + e.getMessage());
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Slot_Machine().setVisible(true);
        });
    }
}