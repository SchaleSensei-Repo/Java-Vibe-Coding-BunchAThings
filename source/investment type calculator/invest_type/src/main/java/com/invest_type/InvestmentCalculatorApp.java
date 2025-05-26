package com.invest_type;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class InvestmentCalculatorApp extends JFrame {

    // --- Input Fields ---
    private JTextField targetIncomeField;
    private JTextField nominalReturnField;
    private JCheckBox customNominalNameCheckBox;
    private JTextField customNominalNameField;
    private JTextField taxRateField;
    private JTextField inflationRateField;
    private JTextField retirementPeriodField;

    // --- Single Result Output ---
    private JTextPane singleResultPane;

    // --- Multiple Income/Investment Inputs ---
    private JTextField addIncomeGoalField;
    private JTextField addNominalReturnField;
    private JCheckBox addCustomNominalNameCheckBox;
    private JTextField addCustomNominalNameField;
    private JButton addIncomeGoalButton;
    private JButton addInvestmentTypeButton;

    private JList<String> incomeGoalsList;
    private DefaultListModel<String> incomeGoalsListModel;
    private List<Double> incomeGoalsRawValues; // To store raw income values for saving

    private JList<String> investmentTypesList;
    private DefaultListModel<String> investmentTypesListModel;
    private Map<String, Double> nominalReturnsMap; // Stores custom names and their values

    // --- Multiple Result Table ---
    private JTable multiResultTable;
    private DefaultTableModel multiResultTableModel;

    // --- Settings ---
    private JComboBox<String> currencySymbolChooser;
    private String currentCurrencySymbol = "$";
    private final String SETTINGS_FILE = "invest_with_yield_settings.ini";

    // --- Default Values (for reset) ---
    private final String DEFAULT_TARGET_INCOME = "55000";
    private final String DEFAULT_NOMINAL_RETURN = "4.5";
    private final String DEFAULT_TAX_RATE = "15";
    private final String DEFAULT_INFLATION_RATE = "3";
    private final String DEFAULT_RETIREMENT_PERIOD = "30";
    private final String DEFAULT_CURRENCY_SYMBOL = "$";

    // --- Financial Calculations ---
    private static class Financials {

        /**
         * Calculates the real yield after tax and inflation.
         * Formula: ((1 + (nominalYield * (1 - taxRate))) / (1 + inflationRate)) - 1
         *
         * @param nominalYield  Expected nominal return/yield (e.g., 0.05 for 5%)
         * @param taxRate       Tax rate on investments (e.g., 0.15 for 15%)
         * @param inflationRate Inflation rate (e.g., 0.03 for 3%)
         * @return Real yield as a decimal.
         */
        public static double calculateRealYield(double nominalYield, double taxRate, double inflationRate) {
            double afterTaxNominalYield = nominalYield * (1 - taxRate);
            // Ensure divisor is not zero or negative for safety, though unlikely with inflation rates normally
            if (1 + inflationRate <= 0) {
                return Double.NaN; // Or throw an exception
            }
            return ((1 + afterTaxNominalYield) / (1 + inflationRate)) - 1;
        }

        /**
         * Calculates the required pre-tax income.
         * Formula: targetAfterTaxIncome / (1 - taxRate)
         *
         * @param targetAfterTaxIncome Target after-tax annual income.
         * @param taxRate              Tax rate on investments (e.g., 0.15 for 15%).
         * @return Required pre-tax income.
         */
        public static double calculateRequiredPreTaxIncome(double targetAfterTaxIncome, double taxRate) {
            if (taxRate >= 1.0) { // Avoid division by zero or negative
                return Double.POSITIVE_INFINITY;
            }
            return targetAfterTaxIncome / (1 - taxRate);
        }

        /**
         * Calculates the required investment capital to generate a perpetual real income.
         * Formula: targetAfterTaxIncome / realYield
         *
         * @param targetAfterTaxIncome Target after-tax annual income.
         * @param realYield            Real yield (e.g., 0.0125 for 1.25%).
         * @return Required investment capital.
         */
        public static double calculateRequiredInvestmentCapital(double targetAfterTaxIncome, double realYield) {
            if (realYield <= 0) { // Cannot generate perpetual income with zero or negative real yield
                return Double.POSITIVE_INFINITY;
            }
            return targetAfterTaxIncome / realYield;
        }
    }

    public InvestmentCalculatorApp() {
        setTitle("Investment Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750); // Increased size to accommodate all features
        setLocationRelativeTo(null); // Center the window

        nominalReturnsMap = new LinkedHashMap<>(); // Use LinkedHashMap to preserve insertion order
        incomeGoalsRawValues = new ArrayList<>(); // Initialize the raw values list

        initComponents();
        loadSettings();

        // Add WindowListener to save settings on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveSettings(); // Call saveSettings when the window is about to close
                super.windowClosing(e); // Allow the default close operation to proceed
            }
        });
    }

    private void initComponents() {
        // Use a JTabbedPane to organize the different sections
        JTabbedPane tabbedPane = new JTabbedPane();

        // --- Single Calculation Tab ---
        JPanel singleCalcPanel = new JPanel(new BorderLayout(10, 10));
        singleCalcPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Padding

        // Input form
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Target after-tax annual income
        gbc.gridx = 0;
        gbc.gridy = row;
        inputPanel.add(new JLabel("Target after-tax annual income:"), gbc);
        gbc.gridx = 1;
        targetIncomeField = new JTextField(DEFAULT_TARGET_INCOME, 15);
        inputPanel.add(targetIncomeField, gbc);
        row++;

        // Expected nominal return/yield
        gbc.gridx = 0;
        gbc.gridy = row;
        inputPanel.add(new JLabel("Expected nominal return/yield (%):"), gbc);
        gbc.gridx = 1;
        nominalReturnField = new JTextField(DEFAULT_NOMINAL_RETURN, 15);
        inputPanel.add(nominalReturnField, gbc);
        row++;

        // Custom Name for Nominal Return
        gbc.gridx = 0;
        gbc.gridy = row;
        customNominalNameCheckBox = new JCheckBox("Custom Name for Yield:");
        inputPanel.add(customNominalNameCheckBox, gbc);
        gbc.gridx = 1;
        customNominalNameField = new JTextField(15);
        customNominalNameField.setEnabled(false); // Disabled by default
        inputPanel.add(customNominalNameField, gbc);
        customNominalNameCheckBox.addActionListener(e -> customNominalNameField.setEnabled(customNominalNameCheckBox.isSelected()));
        row++;

        // Tax rate on investments
        gbc.gridx = 0;
        gbc.gridy = row;
        inputPanel.add(new JLabel("Tax rate on investments (%):"), gbc);
        gbc.gridx = 1;
        taxRateField = new JTextField(DEFAULT_TAX_RATE, 15);
        inputPanel.add(taxRateField, gbc);
        row++;

        // Inflation rate
        gbc.gridx = 0;
        gbc.gridy = row;
        inputPanel.add(new JLabel("Inflation rate (%):"), gbc);
        gbc.gridx = 1;
        inflationRateField = new JTextField(DEFAULT_INFLATION_RATE, 15);
        inputPanel.add(inflationRateField, gbc);
        row++;

        // Retirement period (years)
        gbc.gridx = 0;
        gbc.gridy = row;
        inputPanel.add(new JLabel("Retirement period (years):"), gbc);
        gbc.gridx = 1;
        retirementPeriodField = new JTextField(DEFAULT_RETIREMENT_PERIOD, 15);
        inputPanel.add(retirementPeriodField, gbc);
        row++;

        // Calculate Button
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2; // Span across two columns
        JButton calculateSingleButton = new JButton("Calculate Investment");
        calculateSingleButton.addActionListener(e -> calculateSingleInvestment());
        inputPanel.add(calculateSingleButton, gbc);
        row++;

        // Single Result Display
        singleResultPane = new JTextPane();
        singleResultPane.setEditable(false);
        singleResultPane.setContentType("text/html"); // For basic formatting
        JScrollPane singleResultScrollPane = new JScrollPane(singleResultPane);
        singleResultScrollPane.setPreferredSize(new Dimension(400, 200));

        singleCalcPanel.add(inputPanel, BorderLayout.NORTH);
        singleCalcPanel.add(singleResultScrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Single Calculation", singleCalcPanel);

        // --- Multiple Calculations Tab ---
        JPanel multiCalcPanel = new JPanel(new BorderLayout(10, 10));
        multiCalcPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel multiInputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints multiGbc = new GridBagConstraints();
        multiGbc.insets = new Insets(5, 5, 5, 5);
        multiGbc.fill = GridBagConstraints.HORIZONTAL;

        int multiRow = 0;

        // Add Income Goal Section
        multiGbc.gridx = 0;
        multiGbc.gridy = multiRow;
        multiGbc.gridwidth = 2;
        multiInputPanel.add(new JLabel("--- Add Income Goal ---"), multiGbc);
        multiRow++;

        multiGbc.gridx = 0;
        multiGbc.gridy = multiRow;
        multiGbc.gridwidth = 1;
        multiInputPanel.add(new JLabel("Income Goal ($):"), multiGbc);
        multiGbc.gridx = 1;
        addIncomeGoalField = new JTextField(10);
        multiInputPanel.add(addIncomeGoalField, multiGbc);
        multiGbc.gridx = 2;
        addIncomeGoalButton = new JButton("Add Goal");
        addIncomeGoalButton.addActionListener(e -> addIncomeGoal());
        multiInputPanel.add(addIncomeGoalButton, multiGbc);
        multiRow++;

        // Income Goals List
        multiGbc.gridx = 0;
        multiGbc.gridy = multiRow;
        multiGbc.gridwidth = 3;
        incomeGoalsListModel = new DefaultListModel<>();
        incomeGoalsList = new JList<>(incomeGoalsListModel);
        JScrollPane incomeGoalsScrollPane = new JScrollPane(incomeGoalsList);
        incomeGoalsScrollPane.setPreferredSize(new Dimension(300, 80));
        multiInputPanel.add(incomeGoalsScrollPane, multiGbc);
        multiRow++;

        multiGbc.gridx = 0;
        multiGbc.gridy = multiRow;
        multiGbc.gridwidth = 3;
        JButton removeIncomeGoalButton = new JButton("Remove Selected Goal");
        removeIncomeGoalButton.addActionListener(e -> removeSelectedIncomeGoal());
        multiInputPanel.add(removeIncomeGoalButton, multiGbc);
        multiRow++;

        // Add Investment Type Section
        multiGbc.gridx = 0;
        multiGbc.gridy = multiRow;
        multiGbc.gridwidth = 3;
        multiInputPanel.add(new JLabel("--- Add Investment Type ---"), multiGbc);
        multiRow++;

        multiGbc.gridx = 0;
        multiGbc.gridy = multiRow;
        multiGbc.gridwidth = 1;
        multiInputPanel.add(new JLabel("Nominal Return (%):"), multiGbc);
        multiGbc.gridx = 1;
        addNominalReturnField = new JTextField(10);
        multiInputPanel.add(addNominalReturnField, multiGbc);
        multiGbc.gridx = 2;
        addInvestmentTypeButton = new JButton("Add Type");
        addInvestmentTypeButton.addActionListener(e -> addInvestmentType());
        multiInputPanel.add(addInvestmentTypeButton, multiGbc);
        multiRow++;

        multiGbc.gridx = 0;
        multiGbc.gridy = multiRow;
        addCustomNominalNameCheckBox = new JCheckBox("Custom Name:");
        multiInputPanel.add(addCustomNominalNameCheckBox, multiGbc);
        multiGbc.gridx = 1;
        addCustomNominalNameField = new JTextField(10);
        addCustomNominalNameField.setEnabled(false);
        multiInputPanel.add(addCustomNominalNameField, multiGbc);
        addCustomNominalNameCheckBox.addActionListener(e -> addCustomNominalNameField.setEnabled(addCustomNominalNameCheckBox.isSelected()));
        multiRow++;

        // Investment Types List
        multiGbc.gridx = 0;
        multiGbc.gridy = multiRow;
        multiGbc.gridwidth = 3;
        investmentTypesListModel = new DefaultListModel<>();
        investmentTypesList = new JList<>(investmentTypesListModel);
        JScrollPane investmentTypesScrollPane = new JScrollPane(investmentTypesList);
        investmentTypesScrollPane.setPreferredSize(new Dimension(300, 80));
        multiInputPanel.add(investmentTypesScrollPane, multiGbc);
        multiRow++;

        multiGbc.gridx = 0;
        multiGbc.gridy = multiRow;
        multiGbc.gridwidth = 3;
        JButton removeInvestmentTypeButton = new JButton("Remove Selected Type");
        removeInvestmentTypeButton.addActionListener(e -> removeSelectedInvestmentType());
        multiInputPanel.add(removeInvestmentTypeButton, multiGbc);
        multiRow++;

        // Calculate Multiple Button
        multiGbc.gridx = 0;
        multiGbc.gridy = multiRow;
        multiGbc.gridwidth = 3;
        JButton calculateMultiButton = new JButton("Calculate All Combinations");
        calculateMultiButton.addActionListener(e -> calculateMultipleInvestments());
        multiInputPanel.add(calculateMultiButton, multiGbc);
        multiRow++;

        multiCalcPanel.add(multiInputPanel, BorderLayout.NORTH);

        // Multi Result Table
        // Added "Nominal Return" column
        String[] columnNames = {"Income Goal Per Year", "Investment Type", "Nominal Return", "Required Pre-Tax Income", "Real Yield", "Required Capital"};
        multiResultTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make cells non-editable
            }
        };
        multiResultTable = new JTable(multiResultTableModel);
        multiResultTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        multiResultTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane multiResultScrollPane = new JScrollPane(multiResultTable);
        multiResultScrollPane.setPreferredSize(new Dimension(800, 300)); // Larger for table

        multiCalcPanel.add(multiResultScrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("Multiple Calculations", multiCalcPanel);

        // --- Settings Tab ---
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints settingsGbc = new GridBagConstraints();
        settingsGbc.insets = new Insets(5, 5, 5, 5);
        settingsGbc.fill = GridBagConstraints.HORIZONTAL;

        settingsGbc.gridx = 0;
        settingsGbc.gridy = 0;
        settingsPanel.add(new JLabel("Currency Symbol:"), settingsGbc);
        settingsGbc.gridx = 1;
        String[] currencySymbols = {"$", "€", "£", "¥", "₹"};
        currencySymbolChooser = new JComboBox<>(currencySymbols);
        currencySymbolChooser.addActionListener(e -> {
            currentCurrencySymbol = (String) currencySymbolChooser.getSelectedItem();
            saveSettings(); // Save immediately when currency changes
            refreshIncomeGoalsListDisplay(); // Re-format existing list items
        });
        settingsPanel.add(currencySymbolChooser, settingsGbc);

        // Reset Button
        settingsGbc.gridx = 0;
        settingsGbc.gridy = 1;
        settingsGbc.gridwidth = 2; // Span across two columns
        JButton resetButton = new JButton("Reset All Inputs to Defaults");
        resetButton.addActionListener(e -> resetAllInputs());
        settingsPanel.add(resetButton, settingsGbc);
        
        // Add a stretch component to push everything to the top left
        settingsGbc.gridx = 2;
        settingsGbc.gridy = 0; // Back to first row for alignment
        settingsGbc.weightx = 1.0;
        settingsGbc.weighty = 1.0;
        settingsGbc.gridheight = GridBagConstraints.REMAINDER; // Occupy remaining vertical space
        settingsPanel.add(new JPanel(), settingsGbc);


        tabbedPane.addTab("Settings", settingsPanel);

        // --- Add tabbed pane to frame ---
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void resetAllInputs() {
        // Reset single calculation tab fields
        targetIncomeField.setText(DEFAULT_TARGET_INCOME);
        nominalReturnField.setText(DEFAULT_NOMINAL_RETURN);
        customNominalNameCheckBox.setSelected(false);
        customNominalNameField.setText("");
        customNominalNameField.setEnabled(false);
        taxRateField.setText(DEFAULT_TAX_RATE);
        inflationRateField.setText(DEFAULT_INFLATION_RATE);
        retirementPeriodField.setText(DEFAULT_RETIREMENT_PERIOD);
        singleResultPane.setText(""); // Clear single result display

        // Reset multiple calculation tab lists and fields
        incomeGoalsRawValues.clear();
        incomeGoalsListModel.clear();
        nominalReturnsMap.clear();
        investmentTypesListModel.clear();
        addIncomeGoalField.setText("");
        addNominalReturnField.setText("");
        addCustomNominalNameCheckBox.setSelected(false);
        addCustomNominalNameField.setText("");
        addCustomNominalNameField.setEnabled(false);
        multiResultTableModel.setRowCount(0); // Clear multi result table

        // Reset currency symbol
        currentCurrencySymbol = DEFAULT_CURRENCY_SYMBOL;
        currencySymbolChooser.setSelectedItem(DEFAULT_CURRENCY_SYMBOL);
        refreshIncomeGoalsListDisplay(); // Re-format if currency was different before reset

        JOptionPane.showMessageDialog(this, "All inputs have been reset to default values.", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
        saveSettings(); // Save the reset state
    }

    private void addIncomeGoal() {
        try {
            double income = Double.parseDouble(addIncomeGoalField.getText());
            if (income <= 0) {
                JOptionPane.showMessageDialog(this, "Income goal must be a positive number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            incomeGoalsRawValues.add(income); // Store the raw value
            incomeGoalsListModel.addElement(formatCurrency(income)); // Add formatted string for display
            addIncomeGoalField.setText(""); // Clear field
            saveSettings(); // Save settings after modification
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for income goal.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedIncomeGoal() {
        int selectedIndex = incomeGoalsList.getSelectedIndex();
        if (selectedIndex != -1) {
            incomeGoalsListModel.remove(selectedIndex);
            incomeGoalsRawValues.remove(selectedIndex); // Remove raw value too
            saveSettings(); // Save settings after modification
        } else {
            JOptionPane.showMessageDialog(this, "Please select an income goal to remove.", "Selection Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void addInvestmentType() {
        try {
            double nominalReturnInput = Double.parseDouble(addNominalReturnField.getText()); // Read as percentage
            double nominalReturnDecimal = nominalReturnInput / 100.0; // Convert to decimal for calculation

            String name = nominalReturnInput + "% Nominal"; // Default name
            if (addCustomNominalNameCheckBox.isSelected()) {
                String customName = addCustomNominalNameField.getText().trim();
                if (customName.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Custom name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                name = customName;
            }

            if (nominalReturnsMap.containsKey(name)) {
                JOptionPane.showMessageDialog(this, "An investment type with this name already exists.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            nominalReturnsMap.put(name, nominalReturnDecimal); // Store decimal value
            investmentTypesListModel.addElement(name); // Display name
            addNominalReturnField.setText("");
            addCustomNominalNameField.setText("");
            addCustomNominalNameCheckBox.setSelected(false);
            addCustomNominalNameField.setEnabled(false);
            saveSettings(); // Save settings after modification

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for nominal return.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedInvestmentType() {
        int selectedIndex = investmentTypesList.getSelectedIndex();
        if (selectedIndex != -1) {
            String selectedName = investmentTypesListModel.getElementAt(selectedIndex);
            nominalReturnsMap.remove(selectedName);
            investmentTypesListModel.remove(selectedIndex);
            saveSettings(); // Save settings after modification
        } else {
            JOptionPane.showMessageDialog(this, "Please select an investment type to remove.", "Selection Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void calculateSingleInvestment() {
        try {
            double targetIncome = Double.parseDouble(targetIncomeField.getText());
            double nominalReturn = Double.parseDouble(nominalReturnField.getText()) / 100.0;
            double taxRate = Double.parseDouble(taxRateField.getText()) / 100.0;
            double inflationRate = Double.parseDouble(inflationRateField.getText()) / 100.0;
            int retirementPeriod = Integer.parseInt(retirementPeriodField.getText());

            if (targetIncome <= 0 || nominalReturn < 0 || taxRate < 0 || inflationRate < 0 || retirementPeriod <= 0) {
                JOptionPane.showMessageDialog(this, "All inputs must be positive numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (taxRate >= 1.0) {
                JOptionPane.showMessageDialog(this, "Tax rate must be less than 100%.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double realYield = Financials.calculateRealYield(nominalReturn, taxRate, inflationRate);
            double requiredPreTaxIncome = Financials.calculateRequiredPreTaxIncome(targetIncome, taxRate);
            double requiredInvestmentCapital = Financials.calculateRequiredInvestmentCapital(targetIncome, realYield); // Perpetual real income

            String nominalReturnName = nominalReturnField.getText() + "% nominal yield";
            if (customNominalNameCheckBox.isSelected() && !customNominalNameField.getText().trim().isEmpty()) {
                nominalReturnName = customNominalNameField.getText().trim();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<html><body>");

            // Required Investment Capital
            sb.append("<b>Required Investment Capital:</b><br>")
              .append("To earn ").append(formatCurrency(targetIncome)).append("/year (after tax) for ")
              .append(retirementPeriod).append(" years, you need to invest:<br>")
              .append("<b>").append(formatCurrency(requiredInvestmentCapital)).append("</b> ")
              .append("in a portfolio (").append(nominalReturnName)
              .append(", ").append(formatPercentage(taxRate)).append(" tax, ")
              .append(formatPercentage(inflationRate)).append(" inflation).<br>");
            sb.append("<font color='red'><i>Note: This capital calculation assumes a perpetual real income stream where the principal is largely preserved. Actual capital required may vary based on specific withdrawal strategies and market performance.</i></font><br><br>");

            // Real Yield
            sb.append("<b>Real Yield:</b><br>")
              .append("Nominal Yield: ").append(formatPercentage(nominalReturn)).append("<br>")
              .append("Tax Rate: ").append(formatPercentage(taxRate)).append("<br>")
              .append("Inflation Rate: ").append(formatPercentage(inflationRate)).append("<br>")
              .append("Real Yield (after tax & inflation): <b>").append(formatPercentage(realYield)).append("</b><br><br>");

            // Required Pre-Tax Income
            sb.append("<b>Required Pre-Tax Income:</b><br>")
              .append("To get ").append(formatCurrency(targetIncome)).append(" after tax annually, you need to earn ")
              .append("<b>").append(formatCurrency(requiredPreTaxIncome)).append("</b> before tax.")
              .append("</body></html>");

            singleResultPane.setText(sb.toString());

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for all input fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateMultipleInvestments() {
        multiResultTableModel.setRowCount(0); // Clear previous results

        if (incomeGoalsRawValues.isEmpty() || nominalReturnsMap.isEmpty()) { // Check raw values list
            JOptionPane.showMessageDialog(this, "Please add at least one income goal and one investment type.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double taxRate = Double.parseDouble(taxRateField.getText()) / 100.0;
            double inflationRate = Double.parseDouble(inflationRateField.getText()) / 100.0;

            if (taxRate < 0 || inflationRate < 0) {
                JOptionPane.showMessageDialog(this, "Tax and Inflation rates must be non-negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (taxRate >= 1.0) {
                JOptionPane.showMessageDialog(this, "Tax rate must be less than 100%.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }


            for (Double targetIncome : incomeGoalsRawValues) { // Iterate over raw values
                for (Map.Entry<String, Double> entry : nominalReturnsMap.entrySet()) {
                    String investmentTypeName = entry.getKey();
                    double nominalReturn = entry.getValue(); // Already in decimal form

                    double realYield = Financials.calculateRealYield(nominalReturn, taxRate, inflationRate);
                    double requiredPreTaxIncome = Financials.calculateRequiredPreTaxIncome(targetIncome, taxRate);
                    double requiredInvestmentCapital = Financials.calculateRequiredInvestmentCapital(targetIncome, realYield);

                    multiResultTableModel.addRow(new Object[]{
                        formatCurrency(targetIncome),
                        investmentTypeName,
                        formatPercentage(nominalReturn), // Added nominal return here
                        formatCurrency(requiredPreTaxIncome),
                        formatPercentage(realYield),
                        formatCurrency(requiredInvestmentCapital)
                    });
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please ensure Tax Rate and Inflation Rate are valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private String formatCurrency(double value) {
        DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");
        return currentCurrencySymbol + currencyFormat.format(value);
    }

    private String formatPercentage(double value) {
        DecimalFormat percentFormat = new DecimalFormat("0.00%");
        return percentFormat.format(value);
    }

    // Helper to refresh the display of income goals when currency changes
    private void refreshIncomeGoalsListDisplay() {
        DefaultListModel<String> tempModel = new DefaultListModel<>();
        for (Double income : incomeGoalsRawValues) {
            tempModel.addElement(formatCurrency(income));
        }
        incomeGoalsListModel.clear();
        for (int i = 0; i < tempModel.size(); i++) {
            incomeGoalsListModel.addElement(tempModel.getElementAt(i));
        }
    }

    private void saveSettings() {
        Properties properties = new Properties();
        try {
            properties.setProperty("currencySymbol", currentCurrencySymbol);
            properties.setProperty("targetIncome", targetIncomeField.getText());
            properties.setProperty("nominalReturn", nominalReturnField.getText());
            properties.setProperty("customNominalNameChecked", String.valueOf(customNominalNameCheckBox.isSelected()));
            properties.setProperty("customNominalName", customNominalNameField.getText());
            properties.setProperty("taxRate", taxRateField.getText());
            properties.setProperty("inflationRate", inflationRateField.getText());
            properties.setProperty("retirementPeriod", retirementPeriodField.getText());

            // Save multi-input items
            // Income Goals: Store raw numeric values directly
            StringBuilder incomeGoalsSb = new StringBuilder();
            for (Double income : incomeGoalsRawValues) { // Iterate over raw values
                incomeGoalsSb.append(income).append(",");
            }
            if (incomeGoalsSb.length() > 0) incomeGoalsSb.setLength(incomeGoalsSb.length() - 1); // Remove trailing comma
            properties.setProperty("incomeGoals", incomeGoalsSb.toString());

            // Investment Types
            StringBuilder investmentTypesSb = new StringBuilder();
            for (Map.Entry<String, Double> entry : nominalReturnsMap.entrySet()) {
                // Ensure no commas in custom names or they will mess up splitting
                String key = entry.getKey().replace(",", "");
                investmentTypesSb.append(key).append("=").append(entry.getValue()).append(",");
            }
            if (investmentTypesSb.length() > 0) investmentTypesSb.setLength(investmentTypesSb.length() - 1);
            properties.setProperty("investmentTypes", investmentTypesSb.toString());

            try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
                properties.store(fos, "Investment Calculator Settings");
            }
            // System.out.println("Settings saved to: " + new File(SETTINGS_FILE).getAbsolutePath()); // Debugging line
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving settings: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSettings() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            properties.load(fis);

            // Load single input fields
            targetIncomeField.setText(properties.getProperty("targetIncome", DEFAULT_TARGET_INCOME));
            nominalReturnField.setText(properties.getProperty("nominalReturn", DEFAULT_NOMINAL_RETURN));
            customNominalNameCheckBox.setSelected(Boolean.parseBoolean(properties.getProperty("customNominalNameChecked", "false")));
            customNominalNameField.setText(properties.getProperty("customNominalName", ""));
            customNominalNameField.setEnabled(customNominalNameCheckBox.isSelected());
            taxRateField.setText(properties.getProperty("taxRate", DEFAULT_TAX_RATE));
            inflationRateField.setText(properties.getProperty("inflationRate", DEFAULT_INFLATION_RATE));
            retirementPeriodField.setText(properties.getProperty("retirementPeriod", DEFAULT_RETIREMENT_PERIOD));

            // Load currency setting first, so formatting in lists is correct during load
            currentCurrencySymbol = properties.getProperty("currencySymbol", DEFAULT_CURRENCY_SYMBOL);
            for (int i = 0; i < currencySymbolChooser.getItemCount(); i++) {
                if (currencySymbolChooser.getItemAt(i).equals(currentCurrencySymbol)) {
                    currencySymbolChooser.setSelectedIndex(i);
                    break;
                }
            }

            // Load multi-input items
            incomeGoalsListModel.clear();
            incomeGoalsRawValues.clear(); // Clear raw values list too
            String incomeGoalsStr = properties.getProperty("incomeGoals", "");
            if (!incomeGoalsStr.isEmpty()) {
                for (String income : incomeGoalsStr.split(",")) {
                    try {
                        double incomeValue = Double.parseDouble(income);
                        incomeGoalsRawValues.add(incomeValue); // Add to raw values
                        incomeGoalsListModel.addElement(formatCurrency(incomeValue)); // Add formatted to display model
                    } catch (NumberFormatException ignore) {
                        // Skip invalid entries
                    }
                }
            }

            nominalReturnsMap.clear();
            investmentTypesListModel.clear();
            String investmentTypesStr = properties.getProperty("investmentTypes", "");
            if (!investmentTypesStr.isEmpty()) {
                for (String typeEntry : investmentTypesStr.split(",")) {
                    String[] parts = typeEntry.split("=");
                    if (parts.length == 2) {
                        try {
                            String name = parts[0];
                            double value = Double.parseDouble(parts[1]);
                            nominalReturnsMap.put(name, value);
                            investmentTypesListModel.addElement(name);
                        } catch (NumberFormatException ignore) {
                            // Skip invalid entries
                        }
                    }
                }
            }

        } catch (FileNotFoundException ex) {
            // No settings file yet, use defaults - this is normal on first run
            // System.out.println("Settings file not found, using defaults."); // Debugging line
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error loading settings: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        SwingUtilities.invokeLater(() -> {
            InvestmentCalculatorApp app = new InvestmentCalculatorApp();
            app.setVisible(true);
        });
    }
}