package com.score_tracking;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.NumberFormat;
import java.util.Locale;

public class ScoreTrackerApp extends JFrame {

    // --- Variables ---
    private int roundNumber;
    private double initialPoints; // Corresponds to self.initial_points in Python (though the calculation base is nextRoundInitialPoints)
    private double addModifier;
    private double subtractModifier;
    private double currentCalculatedScore;

    // This internal variable tracks the actual base score for the *next* round's calculation
    private double nextRoundInitialPoints;

    // --- UI Components ---
    private JLabel roundLabel;
    private JTextField initialPointsEntry;
    private JTextField addModifierEntry;
    private JTextField subtractModifierEntry;
    private JLabel currentScoreDisplay;
    private JButton setInitialBtn;
    private JButton calculateBtn;
    private JButton resetBtn;

    private JTable logTable; // JTable is the equivalent of ttk.Treeview for tabular data
    private DefaultTableModel logTableModel;
    private JScrollPane logScrollPane;

    // --- Formatters ---
    // Use NumberFormat for locale-aware formatting (e.g., comma for thousands)
    private NumberFormat numberFormat;

    public ScoreTrackerApp() {
        // --- Frame Setup ---
        setTitle("Score Tracker");
        setSize(800, 600); // Set initial window size
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close operation
        setLocationRelativeTo(null); // Center the window on screen
        setResizable(true); // Allow resizing

        // --- Initialize Formatters ---
        numberFormat = NumberFormat.getNumberInstance(Locale.US); // Use US locale for comma separator

        // --- UI Layout ---
        // Use BorderLayout for the main frame to place controls at top and log in center
        setLayout(new BorderLayout(10, 10)); // 10px horizontal and vertical gap

        // Main panels
        JPanel controlsPanel = createControlsPanel();
        JPanel logPanel = createLogPanel();

        add(controlsPanel, BorderLayout.NORTH);
        add(logPanel, BorderLayout.CENTER);

        // Initialize the app state silently at startup
        _initializeAppState();

        setVisible(true); // Make the frame visible
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout()); // Use GridBagLayout for flexible grid
        panel.setBorder(BorderFactory.createTitledBorder("Score Controls")); // Equivalent to LabelFrame

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding around components (top, left, bottom, right)
        gbc.fill = GridBagConstraints.HORIZONTAL; // Components fill their display area horizontally

        // Round Number Display
        roundLabel = new JLabel("Round: 1"); // Initial text
        roundLabel.setFont(new Font("Helvetica", Font.BOLD, 14));
        gbc.gridx = 0; // Column 0
        gbc.gridy = 0; // Row 0
        gbc.gridwidth = 2; // Spans 2 columns
        gbc.anchor = GridBagConstraints.WEST; // Align to west (left)
        panel.add(roundLabel, gbc);

        // Reset Button
        resetBtn = new JButton("Reset All");
        resetBtn.addActionListener(e -> _resetApp()); // Lambda for ActionListener
        gbc.gridx = 2; // Column 2
        gbc.gridy = 0; // Row 0
        gbc.gridwidth = 1; // Spans 1 column
        gbc.anchor = GridBagConstraints.EAST; // Align to east (right)
        panel.add(resetBtn, gbc);

        // Initial Points Input
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Set Initial Points:"), gbc);

        initialPointsEntry = new JTextField(15); // Width hint
        // Add FocusListener to format on focus lost
        initialPointsEntry.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                _formatEntryOnFocusOut(initialPointsEntry);
                // After formatting and parsing, also set current score directly if this is the initial points entry
                currentCalculatedScore = _parseNumberFromEntry(initialPointsEntry);
                _updateCurrentScoreDisplay();
            }
        });
        // Add ActionListener to handle Enter key press
        initialPointsEntry.addActionListener(e -> _setInitialPointsFromEntry());
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0; // Make this column expand horizontally
        panel.add(initialPointsEntry, gbc);

        setInitialBtn = new JButton("Apply Initial Points");
        setInitialBtn.addActionListener(e -> _setInitialPointsFromEntry());
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.0; // Reset weight for button column
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(setInitialBtn, gbc);

        // Add Points Input
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Add Points:"), gbc);

        addModifierEntry = new JTextField(15);
        addModifierEntry.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                _formatEntryOnFocusOut(addModifierEntry);
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        panel.add(addModifierEntry, gbc);

        // Subtract Points Input
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Subtract Points:"), gbc);

        subtractModifierEntry = new JTextField(15);
        subtractModifierEntry.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                _formatEntryOnFocusOut(subtractModifierEntry);
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        panel.add(subtractModifierEntry, gbc);

        // Calculate Button
        calculateBtn = new JButton("Calculate Round");
        calculateBtn.addActionListener(e -> _calculateRound());
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2; // Spans 2 columns
        gbc.fill = GridBagConstraints.HORIZONTAL; // Fill horizontally
        panel.add(calculateBtn, gbc);

        // Current Score Display
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE; // Don't fill horizontally for label
        panel.add(new JLabel("Current Total Score:"), gbc);

        currentScoreDisplay = new JLabel("0"); // Initial text
        currentScoreDisplay.setFont(new Font("Helvetica", Font.BOLD, 16));
        currentScoreDisplay.setForeground(Color.BLUE);
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Make it fill for appearance
        panel.add(currentScoreDisplay, gbc);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Round Log"));

        String[] columnNames = {"Round", "Initial Points", "Add Points", "Subtract Points", "Total Points"};
        logTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make cells non-editable
            }
        };
        logTable = new JTable(logTableModel);
        logTable.setFillsViewportHeight(true); // Table fills the viewport height
        logTable.getTableHeader().setReorderingAllowed(false); // Prevent column reordering

        // Set preferred column widths and alignment for a cleaner look
        logTable.getColumnModel().getColumn(0).setPreferredWidth(70);  // Round
        logTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(JLabel.CENTER); } // Center align
        });

        logTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Initial
        logTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(JLabel.RIGHT); } // Right align
        });

        logTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Add
        logTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(JLabel.RIGHT); }
        });

        logTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Subtract
        logTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(JLabel.RIGHT); }
        });

        logTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Total
        logTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(JLabel.RIGHT); }
        });

        logScrollPane = new JScrollPane(logTable); // Add scrollbar to the table
        panel.add(logScrollPane, BorderLayout.CENTER);

        return panel;
    }

    // --- Helper Functions ---

    /**
     * Formats a double value with thousands separators, showing as an integer if it has no decimal part.
     */
    private String _formatNumber(double value) {
        // If the value is a whole number, format as an integer
        if (value == (long) value) {
            return String.format(Locale.US, "%,d", (long) value); // Use long to avoid .0
        } else {
            // Otherwise, format as a decimal number
            return numberFormat.format(value);
        }
    }

    /**
     * Reads value from JTextField, removes commas, and converts to double.
     * Displays an error message if parsing fails.
     */
    private double _parseNumberFromEntry(JTextField entryWidget) {
        try {
            String rawValue = entryWidget.getText().replace(",", ""); // Remove commas for parsing
            if (rawValue.isEmpty()) {
                return 0.0; // Treat empty input as 0
            }
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException e) {
            // Determine the field name for the error message
            String fieldName = "";
            if (entryWidget == initialPointsEntry) fieldName = "Set Initial Points";
            else if (entryWidget == addModifierEntry) fieldName = "Add Points";
            else if (entryWidget == subtractModifierEntry) fieldName = "Subtract Points";

            JOptionPane.showMessageDialog(this,
                    "Please enter a valid number in the '" + fieldName + "' field.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            return 0.0; // Return 0.0 on error to prevent calculation issues
        }
    }

    /**
     * Updates a JTextField with a formatted number.
     */
    private void _updateEntryDisplay(JTextField entryWidget, double value) {
        String formattedValue = _formatNumber(value);
        entryWidget.setText(formattedValue);
    }

    /**
     * Formats the number in an entry widget when it loses focus.
     */
    private void _formatEntryOnFocusOut(JTextField entryWidget) {
        double numericValue = _parseNumberFromEntry(entryWidget); // This also handles error messages
        _updateEntryDisplay(entryWidget, numericValue);
    }

    /**
     * Updates the round number label.
     */
    private void _updateRoundDisplay() {
        roundLabel.setText("Round: " + roundNumber);
    }

    /**
     * Updates the current total score label.
     */
    private void _updateCurrentScoreDisplay() {
        currentScoreDisplay.setText(_formatNumber(currentCalculatedScore));
    }

    // --- Core Logic Functions ---

    /**
     * Sets the initial state of the application without a confirmation dialog.
     */
    private void _initializeAppState() {
        roundNumber = 1;
        initialPoints = 0.0;
        addModifier = 0.0;
        subtractModifier = 0.0;
        currentCalculatedScore = 0.0;
        nextRoundInitialPoints = 0.0; // This truly defines the base for the first calculation

        // Update all UI components with initial/reset values
        _updateRoundDisplay();
        _updateCurrentScoreDisplay();
        _updateEntryDisplay(initialPointsEntry, initialPoints);
        _updateEntryDisplay(addModifierEntry, addModifier);
        _updateEntryDisplay(subtractModifierEntry, subtractModifier);

        // Clear JTable model
        logTableModel.setRowCount(0); // Clears all rows from the table
    }

    /**
     * Sets the initial points from the user input field.
     */
    private void _setInitialPointsFromEntry() {
        double newInitial = _parseNumberFromEntry(initialPointsEntry);
        initialPoints = newInitial; // Update the 'initialPoints' variable
        nextRoundInitialPoints = newInitial; // This is the base for the *next* calculation
        currentCalculatedScore = newInitial; // Directly update the current score display
        _updateEntryDisplay(initialPointsEntry, newInitial); // Re-format the entry
        _updateCurrentScoreDisplay(); // Refresh current score label

        JOptionPane.showMessageDialog(this,
                "Initial points for Round " + roundNumber + " set to: " + _formatNumber(newInitial),
                "Initial Points Set",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Performs the score calculation for the current round.
     */
    private void _calculateRound() {
        // Use the internal tracking variable for this round's initial points
        double initial = nextRoundInitialPoints;

        // Get modifier values, handling potential errors and formatting
        addModifier = _parseNumberFromEntry(addModifierEntry);
        subtractModifier = _parseNumberFromEntry(subtractModifierEntry);

        // Re-format modifier entries immediately after parsing to ensure consistency
        _updateEntryDisplay(addModifierEntry, addModifier);
        _updateEntryDisplay(subtractModifierEntry, subtractModifier);

        // Perform calculation
        double total = initial + addModifier - subtractModifier;
        currentCalculatedScore = total;
        _updateCurrentScoreDisplay(); // Update current score label

        // Add to log table
        logTableModel.addRow(new Object[]{
            roundNumber,
            _formatNumber(initial),
            _formatNumber(addModifier),
            _formatNumber(subtractModifier),
            _formatNumber(total)
        });

        // Scroll to the bottom of the table
        // This ensures the newest entry is always visible
        int lastRowIndex = logTableModel.getRowCount() - 1;
        if (lastRowIndex >= 0) {
            logTable.scrollRectToVisible(logTable.getCellRect(lastRowIndex, 0, true));
        }

        // Prepare for next round
        roundNumber++;
        _updateRoundDisplay(); // Update round number label
        nextRoundInitialPoints = total; // The calculated total becomes initial for next round

        // Update the initial points entry display for the *next* round
        // This makes the "Set Initial Points" field automatically show the result for the next round's base.
        _updateEntryDisplay(initialPointsEntry, total);

        // The lines below were previously here but are now removed/commented out
        // to keep the add/subtract fields persistent, as requested.
        // addModifier = 0.0;
        // subtractModifier = 0.0;
        // _updateEntryDisplay(addModifierEntry, addModifier);
        // _updateEntryDisplay(subtractModifierEntry, subtractModifier);
    }

    /**
     * Resets all numbers and clears the log with user confirmation.
     */
    private void _resetApp() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset all data and start a new game?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            _initializeAppState(); // Use the silent initialization
            JOptionPane.showMessageDialog(this,
                    "All data has been reset.",
                    "Reset Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Set system look and feel for better native integration
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Run the application on the Event Dispatch Thread (EDT)
        // All Swing UI updates should happen on the EDT.
        SwingUtilities.invokeLater(ScoreTrackerApp::new);
    }
}