package com.invest_contrib;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Properties;

public class InvestmentCalculatorGUI extends JFrame {

    // --- Input Fields ---
    private JFormattedTextField targetAmountField;
    private JFormattedTextField startingAmountField;
    private JFormattedTextField timePeriodsField;
    private JFormattedTextField returnRateField;
    private JFormattedTextField additionalContributionField;
    private JCheckBox targetAmountEnabledCheckBox;

    // --- Radio Buttons ---
    private JRadioButton monthlyRadio;
    private JRadioButton yearlyRadio;
    private JRadioButton beginningPeriodRadio;
    private JRadioButton endPeriodRadio;
    private ButtonGroup frequencyGroup;
    private ButtonGroup timingGroup;

    // --- Result Labels ---
    private JLabel endBalanceLabel;
    private JLabel startingAmountResultLabel;
    private JLabel totalContributionsLabel;
    private JLabel totalInterestLabel;

    // --- Result Table ---
    private JTable resultTable;
    private DefaultTableModel tableModel;

    // --- Settings ---
    private JComboBox<String> currencySymbolComboBox;
    private Properties appSettings;
    private final String SETTINGS_FILE = "invest_contribs_settings.ini";
    private NumberFormat currentCurrencyFormat; // For DISPLAYING currency in results and table

    // NEW: Formatter for general non-negative integer inputs (e.g., Starting Amount, Target Amount)
    private NumberFormatter nonNegativeIntegerInputFormatter;
    // NEW: Formatter for additional/reduction contribution (allows negative integers)
    private NumberFormatter signedIntegerInputFormatter;
    // (timePeriodsField and returnRateField will use their existing appropriate formatters)

    public InvestmentCalculatorGUI() {
        super("Investment Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null); // Center the window

        appSettings = new Properties();
        loadSettings(); // Load settings at startup

        // Initialize currentCurrencyFormat for DISPLAY PURPOSES only
        currentCurrencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        String loadedCurrencyCode = appSettings.getProperty("currency.code", Currency.getInstance(Locale.getDefault()).getCurrencyCode());
        try {
            currentCurrencyFormat.setCurrency(Currency.getInstance(loadedCurrencyCode));
        } catch (IllegalArgumentException e) {
            currentCurrencyFormat.setCurrency(Currency.getInstance(Locale.getDefault()));
        }
        currentCurrencyFormat.setMinimumFractionDigits(2);
        currentCurrencyFormat.setMaximumFractionDigits(2);
        currentCurrencyFormat.setRoundingMode(RoundingMode.HALF_EVEN);

        // --- Initialize Input Formatter for non-negative integers (e.g., Starting Amount, Target Amount) ---
        NumberFormat nonNegativeIntegerFormat = NumberFormat.getIntegerInstance();
        nonNegativeIntegerFormat.setGroupingUsed(true); // Enable thousands separator
        nonNegativeIntegerFormat.setParseIntegerOnly(true); // Ensure only integers are parsed
        nonNegativeIntegerInputFormatter = new NumberFormatter(nonNegativeIntegerFormat);
        nonNegativeIntegerInputFormatter.setValueClass(Long.class); // Use Long to handle potentially large values
        nonNegativeIntegerInputFormatter.setMinimum(0L); // Enforce non-negative
        nonNegativeIntegerInputFormatter.setAllowsInvalid(false); // Prevents invalid characters
        nonNegativeIntegerInputFormatter.setOverwriteMode(false); // Allows normal insertion/pasting

        // --- Initialize Input Formatter for additional/reduction contribution (allows negative integers) ---
        NumberFormat signedIntegerFormat = NumberFormat.getIntegerInstance();
        signedIntegerFormat.setGroupingUsed(true);
        signedIntegerFormat.setParseIntegerOnly(true);
        signedIntegerInputFormatter = new NumberFormatter(signedIntegerFormat);
        signedIntegerInputFormatter.setValueClass(Long.class); // Use Long
        signedIntegerInputFormatter.setAllowsInvalid(false);
        signedIntegerInputFormatter.setOverwriteMode(false);


        initComponents();
        populateSettings(); // Apply loaded settings to GUI components
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10)); // Main layout

        // --- Input Panel ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(new TitledBorder("Investment Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // NumberFormatter for Percentage
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMinimumFractionDigits(2);
        percentFormat.setMaximumFractionDigits(4);
        percentFormat.setRoundingMode(RoundingMode.HALF_EVEN);
        NumberFormatter percentFormatter = new NumberFormatter(percentFormat);
        percentFormatter.setValueClass(Double.class);
        percentFormatter.setAllowsInvalid(false);
        percentFormatter.setOverwriteMode(true); // Good for percentage input

        // NumberFormatter for Time Periods (integer)
        NumberFormatter timePeriodsFormatter = new NumberFormatter();
        timePeriodsFormatter.setValueClass(Integer.class);
        timePeriodsFormatter.setMinimum(0); // Time periods must be positive
        timePeriodsFormatter.setAllowsInvalid(false);
        timePeriodsFormatter.setOverwriteMode(true);


        // Target Amount (Optional) - now uses nonNegativeIntegerInputFormatter
        targetAmountEnabledCheckBox = new JCheckBox("Enable Target Amount (Optional)");
        targetAmountEnabledCheckBox.addActionListener(e -> targetAmountField.setEnabled(targetAmountEnabledCheckBox.isSelected()));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; inputPanel.add(targetAmountEnabledCheckBox, gbc);

        gbc.gridwidth = 1; // Reset gridwidth
        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("Target Amount:"), gbc);
        targetAmountField = new JFormattedTextField(nonNegativeIntegerInputFormatter); // Applied new formatter
        targetAmountField.setColumns(15);
        targetAmountField.setValue(0L); // Set initial value as Long
        targetAmountField.setEnabled(false); // Initially disabled
        gbc.gridx = 1; gbc.gridy = 1; inputPanel.add(targetAmountField, gbc);

        // Starting Amount - now uses nonNegativeIntegerInputFormatter
        gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(new JLabel("Starting Amount:"), gbc);
        startingAmountField = new JFormattedTextField(nonNegativeIntegerInputFormatter); // Applied new formatter
        startingAmountField.setColumns(15);
        startingAmountField.setValue(0L); // Set initial value as Long
        gbc.gridx = 1; gbc.gridy = 2; inputPanel.add(startingAmountField, gbc);

        // Time Periods - uses timePeriodsFormatter
        gbc.gridx = 0; gbc.gridy = 3; inputPanel.add(new JLabel("Time Periods (Years):"), gbc);
        timePeriodsField = new JFormattedTextField(timePeriodsFormatter);
        timePeriodsField.setColumns(15);
        timePeriodsField.setValue(10); // Default to 10 years
        gbc.gridx = 1; gbc.gridy = 3; inputPanel.add(timePeriodsField, gbc);

        // Return Rate
        gbc.gridx = 0; gbc.gridy = 4; inputPanel.add(new JLabel("Annual Return Rate (%):"), gbc);
        returnRateField = new JFormattedTextField(percentFormatter);
        returnRateField.setColumns(15);
        returnRateField.setValue(0.07); // Default to 7%
        gbc.gridx = 1; gbc.gridy = 4; inputPanel.add(returnRateField, gbc);

        // Additional/Reduction Contribution - now uses signedIntegerInputFormatter
        gbc.gridx = 0; gbc.gridy = 5; inputPanel.add(new JLabel("Additional/Reduction Contribution:"), gbc);
        additionalContributionField = new JFormattedTextField(signedIntegerInputFormatter); // Applied new formatter
        additionalContributionField.setColumns(15);
        additionalContributionField.setValue(0L); // Set initial value as Long
        gbc.gridx = 1; gbc.gridy = 5; inputPanel.add(additionalContributionField, gbc);

        // --- Contribution Frequency Radio Buttons ---
        JPanel frequencyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        frequencyPanel.setBorder(new TitledBorder("Contribution Frequency"));
        monthlyRadio = new JRadioButton("Monthly");
        yearlyRadio = new JRadioButton("Yearly");
        frequencyGroup = new ButtonGroup();
        frequencyGroup.add(monthlyRadio);
        frequencyGroup.add(yearlyRadio);
        frequencyPanel.add(monthlyRadio);
        frequencyPanel.add(yearlyRadio);
        monthlyRadio.setSelected(true); // Default to monthly

        // --- Contribution Timing Radio Buttons ---
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timingPanel.setBorder(new TitledBorder("Contribution Timing"));
        beginningPeriodRadio = new JRadioButton("Beginning of Period");
        endPeriodRadio = new JRadioButton("End of Period");
        timingGroup = new ButtonGroup();
        timingGroup.add(beginningPeriodRadio);
        timingGroup.add(endPeriodRadio);
        timingPanel.add(beginningPeriodRadio);
        timingPanel.add(endPeriodRadio);
        endPeriodRadio.setSelected(true); // Default to end of period

        // Group frequency and timing panels to control their size
        JPanel radioButtonsWrapper = new JPanel(new GridLayout(2, 1, 0, 5)); // 2 rows, 1 column, 5 vertical gap
        radioButtonsWrapper.add(frequencyPanel);
        radioButtonsWrapper.add(timingPanel);


        // Combine input and radio buttons into a left panel
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.add(inputPanel, BorderLayout.NORTH);
        leftPanel.add(radioButtonsWrapper, BorderLayout.CENTER); // Use the new wrapper panel here

        add(leftPanel, BorderLayout.WEST);

        // --- Controls Panel (Calculate, Reset) ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton calculateButton = new JButton("Calculate Investment");
        calculateButton.addActionListener(new CalculateButtonListener());
        controlPanel.add(calculateButton);

        JButton resetButton = new JButton("Reset All");
        resetButton.addActionListener(e -> resetAll());
        controlPanel.add(resetButton);

        // --- Settings Panel ---
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        settingsPanel.setBorder(new TitledBorder("Settings"));
        settingsPanel.add(new JLabel("Currency Symbol:"));
        String[] currencyCodes = {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR"}; // Common currency codes
        currencySymbolComboBox = new JComboBox<>(currencyCodes);
        currencySymbolComboBox.addActionListener(e -> {
            String selectedCode = (String) currencySymbolComboBox.getSelectedItem();
            try {
                Currency newCurrency = Currency.getInstance(selectedCode);
                currentCurrencyFormat.setCurrency(newCurrency); // Update currency for display
                // Input formatters are not affected by currency symbol change
                appSettings.setProperty("currency.code", selectedCode);
                saveSettings();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Invalid currency code selected.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        settingsPanel.add(currencySymbolComboBox);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(controlPanel, BorderLayout.CENTER);
        topPanel.add(settingsPanel, BorderLayout.NORTH);

        add(topPanel, BorderLayout.NORTH);

        // --- Result Summary Panel ---
        JPanel resultSummaryPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        resultSummaryPanel.setBorder(new TitledBorder("Investment Summary"));
        endBalanceLabel = new JLabel("End Balance: ");
        startingAmountResultLabel = new JLabel("Starting Amount: ");
        totalContributionsLabel = new JLabel("Total Contributions: ");
        totalInterestLabel = new JLabel("Total Interest: ");

        resultSummaryPanel.add(new JLabel("End Balance:"));
        resultSummaryPanel.add(endBalanceLabel);
        resultSummaryPanel.add(new JLabel("Starting Amount (Initial Capital):"));
        resultSummaryPanel.add(startingAmountResultLabel);
        resultSummaryPanel.add(new JLabel("Total Contributions:"));
        resultSummaryPanel.add(totalContributionsLabel);
        resultSummaryPanel.add(new JLabel("Total Interest Earned:"));
        resultSummaryPanel.add(totalInterestLabel);

        // --- Result Table Panel ---
        tableModel = new DefaultTableModel(new Object[]{"Year", "Deposit", "Interest", "Ending Balance"}, 0);
        resultTable = new JTable(tableModel);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Year-by-Year Breakdown"));
        scrollPane.setPreferredSize(new Dimension(500, 300)); // Give it a preferred size

        // Combine result summary and table into a right panel
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.add(resultSummaryPanel, BorderLayout.NORTH);
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.CENTER);
    }

    // This method is now a NO-OP for input fields, as they use dedicated integer/percent formatters.
    // It exists mainly to reflect the *idea* of updating formatters.
    // The currency format for *display* is handled by currentCurrencyFormat directly.
    private void updateCurrencyFormatters() {
        // No need to re-apply formatters to input fields as their format doesn't change with currency symbol.
    }


    private class CalculateButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                // Save current inputs to settings before calculation
                saveCurrentInputsToSettings();

                // Parse input values - these now come from integer formatters, cast to double for calculations
                double startingAmount = ((Number) startingAmountField.getValue()).doubleValue();
                int timePeriods = ((Number) timePeriodsField.getValue()).intValue();
                double annualRate = ((Number) returnRateField.getValue()).doubleValue();
                double additionalContribution = ((Number) additionalContributionField.getValue()).doubleValue();
                boolean isTargetAmountEnabled = targetAmountEnabledCheckBox.isSelected();
                double targetAmount = isTargetAmountEnabled ? ((Number) targetAmountField.getValue()).doubleValue() : 0.0;


                boolean isMonthly = monthlyRadio.isSelected();
                boolean isBeginning = beginningPeriodRadio.isSelected();

                // --- Input Validation ---
                // Formatters help, but explicit checks add robustness, especially against edge cases or non-numeric paste if setAllowsInvalid(true) was used
                if (startingAmount < 0) {
                    JOptionPane.showMessageDialog(InvestmentCalculatorGUI.this, "Starting amount cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (timePeriods <= 0) {
                    JOptionPane.showMessageDialog(InvestmentCalculatorGUI.this, "Time periods must be greater than zero.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (annualRate < 0) {
                    JOptionPane.showMessageDialog(InvestmentCalculatorGUI.this, "Return rate cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Clear previous results
                tableModel.setRowCount(0);

                double currentBalance = startingAmount;
                double totalContributionsMade = 0;
                double totalInterestEarned = 0;

                // Simulation loop for year-by-year breakdown
                for (int year = 1; year <= timePeriods; year++) {
                    double depositsThisYear = 0;
                    double interestThisYear = 0;

                    int periodsPerYear = isMonthly ? 12 : 1;
                    // Corrected: additionalContribution is an annual amount, divide by periods if monthly.
                    double periodicContribution = additionalContribution / periodsPerYear;
                    double effectivePeriodicRate = isMonthly ? annualRate / 12 : annualRate;

                    for (int period = 1; period <= periodsPerYear; period++) {
                        if (isBeginning) {
                            currentBalance += periodicContribution;
                            depositsThisYear += periodicContribution;
                        }
                        double periodInterest = currentBalance * effectivePeriodicRate;
                        currentBalance += periodInterest;
                        interestThisYear += periodInterest;
                        if (!isBeginning) {
                            currentBalance += periodicContribution;
                            depositsThisYear += periodicContribution;
                        }
                    }

                    // Add row to table - using currentCurrencyFormat for display
                    tableModel.addRow(new Object[]{
                            year,
                            currentCurrencyFormat.format(depositsThisYear),
                            currentCurrencyFormat.format(interestThisYear),
                            currentCurrencyFormat.format(currentBalance)
                    });

                    totalContributionsMade += depositsThisYear;
                    totalInterestEarned += interestThisYear;
                }

                totalContributionsMade = totalContributionsMade; // This variable already correctly accumulates periodic deposits.

                // Update summary labels - using currentCurrencyFormat for display
                endBalanceLabel.setText(currentCurrencyFormat.format(currentBalance));
                startingAmountResultLabel.setText(currentCurrencyFormat.format(startingAmount));
                totalContributionsLabel.setText(currentCurrencyFormat.format(totalContributionsMade));
                totalInterestLabel.setText(currentCurrencyFormat.format(totalInterestEarned));

                // Check if target amount is met
                if (isTargetAmountEnabled && currentBalance < targetAmount) {
                    JOptionPane.showMessageDialog(InvestmentCalculatorGUI.this,
                            String.format("Target amount of %s was NOT met. Current balance: %s",
                                    currentCurrencyFormat.format(targetAmount),
                                    currentCurrencyFormat.format(currentBalance)),
                            "Target Not Met", JOptionPane.INFORMATION_MESSAGE);
                } else if (isTargetAmountEnabled && currentBalance >= targetAmount) {
                     JOptionPane.showMessageDialog(InvestmentCalculatorGUI.this,
                            String.format("Target amount of %s was MET! Current balance: %s",
                                    currentCurrencyFormat.format(targetAmount),
                                    currentCurrencyFormat.format(currentBalance)),
                            "Target Met", JOptionPane.INFORMATION_MESSAGE);
                }


            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(InvestmentCalculatorGUI.this, "Invalid number format. Please ensure all fields contain valid numbers. Error: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (ClassCastException ex) {
                JOptionPane.showMessageDialog(InvestmentCalculatorGUI.this, "An internal error occurred with input types. Please check your inputs. Error: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadSettings() {
        try (InputStream input = new FileInputStream(SETTINGS_FILE)) {
            appSettings.load(input);
        } catch (FileNotFoundException ex) {
            System.out.println("Settings file not found. Creating a new one on save.");
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSettings() {
        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            appSettings.store(output, "Investment Calculator Settings");
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateSettings() {
        // Load target amount
        String targetAmountStr = appSettings.getProperty("target.amount");
        if (targetAmountStr != null && !targetAmountStr.isEmpty()) {
            try {
                // Parse as Long since input is now integer
                long value = Long.parseLong(targetAmountStr);
                targetAmountField.setValue(value);
            } catch (NumberFormatException e) {
                targetAmountField.setValue(0L);
            }
        } else {
            targetAmountField.setValue(0L);
        }
        boolean targetEnabled = Boolean.parseBoolean(appSettings.getProperty("target.enabled", "false"));
        targetAmountEnabledCheckBox.setSelected(targetEnabled);
        targetAmountField.setEnabled(targetEnabled);


        // Load starting amount
        String startingAmountStr = appSettings.getProperty("starting.amount");
        if (startingAmountStr != null && !startingAmountStr.isEmpty()) {
            try {
                long value = Long.parseLong(startingAmountStr);
                startingAmountField.setValue(value);
            } catch (NumberFormatException e) {
                startingAmountField.setValue(0L);
            }
        } else {
            startingAmountField.setValue(0L);
        }

        // Load time periods
        String timePeriodsStr = appSettings.getProperty("time.periods");
        if (timePeriodsStr != null && !timePeriodsStr.isEmpty()) {
            try {
                int value = Integer.parseInt(timePeriodsStr);
                timePeriodsField.setValue(value);
            } catch (NumberFormatException e) {
                timePeriodsField.setValue(10);
            }
        } else {
            timePeriodsField.setValue(10);
        }

        // Load return rate
        String returnRateStr = appSettings.getProperty("return.rate");
        if (returnRateStr != null && !returnRateStr.isEmpty()) {
            try {
                double value = Double.parseDouble(returnRateStr);
                returnRateField.setValue(value);
            } catch (NumberFormatException e) {
                returnRateField.setValue(0.07);
            }
        } else {
            returnRateField.setValue(0.07);
        }

        // Load additional contribution
        String additionalContributionStr = appSettings.getProperty("additional.contribution");
        if (additionalContributionStr != null && !additionalContributionStr.isEmpty()) {
            try {
                long value = Long.parseLong(additionalContributionStr);
                additionalContributionField.setValue(value);
            } catch (NumberFormatException e) {
                additionalContributionField.setValue(0L);
            }
        } else {
            additionalContributionField.setValue(0L);
        }

        // Load radio button selections
        boolean isMonthly = Boolean.parseBoolean(appSettings.getProperty("contribution.frequency.monthly", "true"));
        boolean isBeginning = Boolean.parseBoolean(appSettings.getProperty("contribution.timing.beginning", "false"));

        if (isMonthly) {
            monthlyRadio.setSelected(true);
        } else {
            yearlyRadio.setSelected(true);
        }

        if (isBeginning) {
            beginningPeriodRadio.setSelected(true);
        } else {
            endPeriodRadio.setSelected(true);
        }

        // Load currency symbol (this affects currentCurrencyFormat for DISPLAY)
        String savedCurrencyCode = appSettings.getProperty("currency.code");
        if (savedCurrencyCode != null) {
            currencySymbolComboBox.setSelectedItem(savedCurrencyCode);
            try {
                currentCurrencyFormat.setCurrency(Currency.getInstance(savedCurrencyCode));
            } catch (IllegalArgumentException e) {
                currencySymbolComboBox.setSelectedItem(Currency.getInstance(Locale.getDefault()).getCurrencyCode().toUpperCase());
                currentCurrencyFormat.setCurrency(Currency.getInstance(Locale.getDefault()));
            }
        } else {
            currencySymbolComboBox.setSelectedItem(Currency.getInstance(Locale.getDefault()).getCurrencyCode().toUpperCase());
        }

    }

    private void saveCurrentInputsToSettings() {
        // Values from JFormattedTextFields are already Numbers (Long or Double)
        appSettings.setProperty("target.amount", String.valueOf(targetAmountField.getValue()));
        appSettings.setProperty("target.enabled", String.valueOf(targetAmountEnabledCheckBox.isSelected()));
        appSettings.setProperty("starting.amount", String.valueOf(startingAmountField.getValue()));
        appSettings.setProperty("time.periods", String.valueOf(timePeriodsField.getValue()));
        appSettings.setProperty("return.rate", String.valueOf(returnRateField.getValue()));
        appSettings.setProperty("additional.contribution", String.valueOf(additionalContributionField.getValue()));
        appSettings.setProperty("contribution.frequency.monthly", String.valueOf(monthlyRadio.isSelected()));
        appSettings.setProperty("contribution.timing.beginning", String.valueOf(beginningPeriodRadio.isSelected()));
        // Currency symbol is saved immediately on selection in its ActionListener

        saveSettings();
    }

    private void resetAll() {
        // Reset input fields to default values
        targetAmountField.setValue(0L);
        targetAmountEnabledCheckBox.setSelected(false);
        targetAmountField.setEnabled(false);
        startingAmountField.setValue(0L);
        timePeriodsField.setValue(10);
        returnRateField.setValue(0.07);
        additionalContributionField.setValue(0L);

        // Reset radio buttons
        monthlyRadio.setSelected(true);
        endPeriodRadio.setSelected(true);

        // Clear result labels
        endBalanceLabel.setText("");
        startingAmountResultLabel.setText("");
        totalContributionsLabel.setText("");
        totalInterestLabel.setText("");

        // Clear result table
        tableModel.setRowCount(0);

        // Reset currency symbol to default locale and save settings
        currencySymbolComboBox.setSelectedItem(Currency.getInstance(Locale.getDefault()).getCurrencyCode().toUpperCase());
        currentCurrencyFormat.setCurrency(Currency.getInstance(Locale.getDefault()));

        // Clear app settings and save an empty/default settings file
        appSettings.clear();
        // Save default values as strings
        appSettings.setProperty("target.amount", "0");
        appSettings.setProperty("target.enabled", "false");
        appSettings.setProperty("starting.amount", "0");
        appSettings.setProperty("time.periods", "10");
        appSettings.setProperty("return.rate", "0.07");
        appSettings.setProperty("additional.contribution", "0");
        appSettings.setProperty("contribution.frequency.monthly", "true");
        appSettings.setProperty("contribution.timing.beginning", "false");
        appSettings.setProperty("currency.code", Currency.getInstance(Locale.getDefault()).getCurrencyCode());

        saveSettings();
        JOptionPane.showMessageDialog(this, "All settings, inputs, and results have been reset to default.", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new InvestmentCalculatorGUI().setVisible(true);
        });
    }
}