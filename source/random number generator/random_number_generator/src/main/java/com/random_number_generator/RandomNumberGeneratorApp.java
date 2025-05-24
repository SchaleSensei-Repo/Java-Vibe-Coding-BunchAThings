package com.random_number_generator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Hashtable; // Used for JSlider labels
import java.util.Locale;
import java.util.Random;

public class RandomNumberGeneratorApp extends JFrame {

    // --- GUI Components ---
    private JTextField minField;
    private JTextField maxField;
    private JLabel currentOutputLabel;
    private JLabel totalOutputLabel;
    private JButton generateButton;
    private JButton resetButton;
    private JTable logTable;
    private DefaultTableModel tableModel;
    private JScrollPane logScrollPane;

    // --- Bias Components ---
    private JRadioButton noneBiasRadio;
    private JRadioButton sliderBiasRadio;
    private JRadioButton exactBiasRadio;
    private ButtonGroup biasButtonGroup; // To ensure only one bias option is selected
    private JSlider biasSlider;
    private JTextField exactBiasField;

    // --- Application State Variables ---
    private long totalSum = 0; // Use long to prevent overflow for total sum
    private int iteration = 0;
    private Random random = new Random();
    private NumberFormat numberFormat; // For thousand separators

    public RandomNumberGeneratorApp() {
        // --- Frame Setup ---
        setTitle("Random Number Generator");
        setSize(800, 650); // Increased size to accommodate new bias settings
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        // Initialize NumberFormat for thousand separators (e.g., using US locale)
        numberFormat = NumberFormat.getNumberInstance(Locale.US);

        // --- Initialize Core Components ---
        minField = new JTextField("1", 10); // Default min value
        maxField = new JTextField("100", 10); // Default max value
        currentOutputLabel = new JLabel("Current: N/A", SwingConstants.CENTER);
        totalOutputLabel = new JLabel("Total: N/A", SwingConstants.CENTER);
        generateButton = new JButton("Generate Number");
        resetButton = new JButton("Reset History");

        // Set font for output labels for better visibility
        Font outputFont = new Font("SansSerif", Font.BOLD, 18);
        currentOutputLabel.setFont(outputFont);
        totalOutputLabel.setFont(outputFont);

        // --- Bias Setting Components ---
        noneBiasRadio = new JRadioButton("None", true); // Default selected bias
        sliderBiasRadio = new JRadioButton("Slider Bias");
        exactBiasRadio = new JRadioButton("Exact Number Bias");

        biasButtonGroup = new ButtonGroup(); // Group radio buttons so only one can be selected
        biasButtonGroup.add(noneBiasRadio);
        biasButtonGroup.add(sliderBiasRadio);
        biasButtonGroup.add(exactBiasRadio);

        biasSlider = new JSlider(0, 100, 50); // Min=0 (Min Bias), Max=100 (Max Bias), Initial=50 (No Bias)
        biasSlider.setMajorTickSpacing(50);
        biasSlider.setMinorTickSpacing(10);
        biasSlider.setPaintTicks(true);
        biasSlider.setPaintLabels(true);
        // Custom labels for the slider
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("Min Bias"));
        labelTable.put(50, new JLabel("No Bias"));
        labelTable.put(100, new JLabel("Max Bias"));
        biasSlider.setLabelTable(labelTable);
        biasSlider.setEnabled(false); // Disabled by default as "None" is selected

        exactBiasField = new JTextField(10);
        exactBiasField.setEnabled(false); // Disabled by default

        // --- Layout Panels ---
        setLayout(new BorderLayout(10, 10)); // Outer layout for main panels

        // 1. Top Section Panel (contains Input/Bias controls and Output display)
        JPanel topSectionPanel = new JPanel(new BorderLayout(10, 10));

        // 1.1. Input and Controls Panel (North of topSectionPanel)
        // This panel holds the Min/Max fields + Generate button AND the Bias Settings
        JPanel inputControlsAndBiasPanel = new JPanel(new BorderLayout(20, 0)); // Use BorderLayout for alignment
        inputControlsAndBiasPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Padding

        // Sub-panel for Min/Max input fields and Generate Button
        JPanel inputFieldsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        inputFieldsPanel.setBorder(BorderFactory.createTitledBorder("Input Range"));
        inputFieldsPanel.add(new JLabel("Min:"));
        inputFieldsPanel.add(minField);
        inputFieldsPanel.add(new JLabel("Max:"));
        inputFieldsPanel.add(maxField);
        inputFieldsPanel.add(generateButton);

        // Sub-panel for Bias Settings
        JPanel biasSettingsPanel = new JPanel();
        biasSettingsPanel.setBorder(BorderFactory.createTitledBorder("Bias Settings"));
        biasSettingsPanel.setLayout(new BoxLayout(biasSettingsPanel, BoxLayout.Y_AXIS)); // Vertical layout for components

        // Radio Buttons Sub-panel
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Align radio buttons to the left
        radioPanel.add(noneBiasRadio);
        radioPanel.add(sliderBiasRadio);
        radioPanel.add(exactBiasRadio);
        biasSettingsPanel.add(radioPanel);

        // Slider Sub-panel
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Align slider to the left, smaller gap
        sliderPanel.add(new JLabel("Strength:"));
        sliderPanel.add(biasSlider);
        biasSettingsPanel.add(sliderPanel);

        // Exact Bias Sub-panel
        JPanel exactBiasPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Align field to the left, smaller gap
        exactBiasPanel.add(new JLabel("Towards:"));
        exactBiasPanel.add(exactBiasField);
        biasSettingsPanel.add(exactBiasPanel);

        // Add input fields panel to the center and bias settings to the east of inputControlsAndBiasPanel
        inputControlsAndBiasPanel.add(inputFieldsPanel, BorderLayout.CENTER);
        inputControlsAndBiasPanel.add(biasSettingsPanel, BorderLayout.EAST);

        topSectionPanel.add(inputControlsAndBiasPanel, BorderLayout.NORTH);

        // 1.2. Output Display Panel (Center of topSectionPanel)
        JPanel outputPanel = new JPanel(new GridLayout(1, 2, 10, 0)); // 1 row, 2 columns for current and total output
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output"));
        outputPanel.add(currentOutputLabel);
        outputPanel.add(totalOutputLabel);

        topSectionPanel.add(outputPanel, BorderLayout.CENTER);

        // Add the combined top section to the main frame's NORTH region
        add(topSectionPanel, BorderLayout.NORTH);

        // 2. Log Table Panel (Center of Frame)
        String[] columnNames = {"Iteration", "Current Output", "Total Output"};
        tableModel = new DefaultTableModel(columnNames, 0); // 0 rows initially
        logTable = new JTable(tableModel);
        logTable.setFillsViewportHeight(true); // Table uses all available vertical space
        logTable.getTableHeader().setReorderingAllowed(false); // Prevent column reordering
        logScrollPane = new JScrollPane(logTable);

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Generation Log"));
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        add(logPanel, BorderLayout.CENTER);

        // 3. Reset Button Panel (South of Frame)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(resetButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Add Action Listeners ---
        generateButton.addActionListener(new GenerateButtonListener());
        resetButton.addActionListener(new ResetButtonListener());

        // Action listeners for bias radio buttons to enable/disable associated controls
        ActionListener biasRadioListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Enable slider if Slider Bias is selected, otherwise disable
                biasSlider.setEnabled(sliderBiasRadio.isSelected());
                // Enable exact field if Exact Number Bias is selected, otherwise disable
                exactBiasField.setEnabled(exactBiasRadio.isSelected());
            }
        };
        noneBiasRadio.addActionListener(biasRadioListener);
        sliderBiasRadio.addActionListener(biasRadioListener);
        exactBiasRadio.addActionListener(biasRadioListener);
    }

    // --- Action Listener for Generate Button ---
    private class GenerateButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int min = Integer.parseInt(minField.getText());
                int max = Integer.parseInt(maxField.getText());

                // Input Validation for Min/Max range
                if (min > max) {
                    JOptionPane.showMessageDialog(RandomNumberGeneratorApp.this,
                            "Minimum number cannot be greater than maximum number.",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int currentNumber; // The number generated in this iteration

                // --- Apply Bias Logic based on selected radio button ---
                if (noneBiasRadio.isSelected()) {
                    // No bias: standard uniform random generation
                    currentNumber = random.nextInt(max - min + 1) + min;
                } else if (sliderBiasRadio.isSelected()) {
                    // Slider Bias: generate multiple numbers and pick min/max based on slider position
                    int sliderValue = biasSlider.getValue(); // 0-100

                    // Determine how many numbers to generate and compare (strength of bias)
                    // If slider is 50 (no bias), numPicks = 1
                    // If slider is 0 or 100 (max bias), numPicks = 6
                    int numPicks = 1 + Math.abs(sliderValue - 50) / 10;
                    int bestCandidate;

                    if (sliderValue < 50) { // Bias towards Minimum
                        bestCandidate = Integer.MAX_VALUE; // Initialize with largest possible value
                        for (int i = 0; i < numPicks; i++) {
                            bestCandidate = Math.min(bestCandidate, random.nextInt(max - min + 1) + min);
                        }
                    } else if (sliderValue > 50) { // Bias towards Maximum
                        bestCandidate = Integer.MIN_VALUE; // Initialize with smallest possible value
                        for (int i = 0; i < numPicks; i++) {
                            bestCandidate = Math.max(bestCandidate, random.nextInt(max - min + 1) + min);
                        }
                    } else { // sliderValue == 50, effectively no bias (same as 'None' for this logic)
                        bestCandidate = random.nextInt(max - min + 1) + min;
                    }
                    currentNumber = bestCandidate;

                } else if (exactBiasRadio.isSelected()) {
                    // Exact Number Bias: blend a random number with a user-specified target
                    try {
                        int targetNumber = Integer.parseInt(exactBiasField.getText());

                        // Cap targetNumber within the min/max range to avoid issues
                        targetNumber = Math.max(min, Math.min(max, targetNumber));

                        // Generate a base random number within the full range
                        int baseNum = random.nextInt(max - min + 1) + min;

                        // Blend the base random number with the target number
                        // Weight of 0.6 for target, 0.4 for random (results in a noticeable pull towards target)
                        currentNumber = (int) (baseNum * 0.4 + targetNumber * 0.6);

                        // Ensure the blended number is still within the min/max range
                        currentNumber = Math.max(min, Math.min(max, currentNumber));

                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(RandomNumberGeneratorApp.this,
                                "Please enter a valid integer for the exact bias number.",
                                "Input Error",
                                JOptionPane.ERROR_MESSAGE);
                        return; // Stop execution if exact bias number is invalid
                    }
                } else {
                    // Fallback, should not be reached if radio buttons are properly grouped
                    currentNumber = random.nextInt(max - min + 1) + min;
                }

                // --- Common Logic for all bias types ---
                totalSum += currentNumber; // Add to total sum
                iteration++; // Increment iteration count

                // Update output labels with thousand separators
                currentOutputLabel.setText("Current: " + numberFormat.format(currentNumber));
                totalOutputLabel.setText("Total: " + numberFormat.format(totalSum));

                // Add new row to the log table
                tableModel.addRow(new Object[]{
                        iteration,
                        numberFormat.format(currentNumber),
                        numberFormat.format(totalSum)
                });

                // Scroll to the last row to show the latest entry
                logTable.scrollRectToVisible(logTable.getCellRect(logTable.getRowCount() - 1, 0, true));

            } catch (NumberFormatException ex) {
                // Catch errors if min/max fields do not contain valid integers
                JOptionPane.showMessageDialog(RandomNumberGeneratorApp.this,
                        "Please enter valid integer numbers for Min and Max.",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- Action Listener for Reset Button ---
    private class ResetButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Reset application state variables
            totalSum = 0;
            iteration = 0;

            // Clear output labels
            currentOutputLabel.setText("Current: N/A");
            totalOutputLabel.setText("Total: N/A");

            // Reset input fields to default values
            minField.setText("1");
            maxField.setText("100");

            // Reset bias settings to their default state
            noneBiasRadio.setSelected(true); // Select "None" bias
            biasSlider.setValue(50); // Reset slider to center ("No Bias")
            biasSlider.setEnabled(false); // Disable slider
            exactBiasField.setText(""); // Clear exact bias field
            exactBiasField.setEnabled(false); // Disable exact bias field

            // Clear all rows from the log table
            tableModel.setRowCount(0);
        }
    }

    public static void main(String[] args) {
        // Ensure GUI updates are done on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            RandomNumberGeneratorApp app = new RandomNumberGeneratorApp();
            app.setVisible(true); // Make the JFrame visible
        });
    }
}