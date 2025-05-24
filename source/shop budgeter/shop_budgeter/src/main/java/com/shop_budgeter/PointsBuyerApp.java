package com.shop_budgeter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PointsBuyerApp extends JFrame {

    // --- Data Model for Items ---
    private static class Item {
        private String id;
        private String name;
        private long points;
        private int priority;
        private long availability;

        public Item(String id, String name, long points, int priority, long availability) {
            this.id = id;
            this.name = name;
            this.points = points;
            this.priority = priority;
            this.availability = availability;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public long getPoints() { return points; }
        public int getPriority() { return priority; }
        public long getAvailability() { return availability; }

        // Setters for mutable properties
        public void setName(String name) { this.name = name; }
        public void setPoints(long points) { this.points = points; }
        public void setPriority(int priority) { this.priority = priority; }
        public void setAvailability(long availability) { this.availability = availability; }
    }

    private static final String CONFIG_FILE = "budget_points_settings.ini";
    private List<Item> items; // Stores list of Item objects
    private long budgetPoints; // Stores the total budget
    private String selectedItemId; // Tracks the ID of the currently selected item in the JTable for editing

    // UI Components
    private JTextField budgetEntry;
    private JTextField itemNameEntry;
    private JTextField itemPointsEntry;
    private JTextField itemPriorityEntry;
    private JTextField itemAvailabilityEntry;
    private JTable itemTable;
    private DefaultTableModel itemTableModel; // Model for the JTable
    private JTextArea resultsText;

    private NumberFormat numberFormat; // For formatting numbers with commas

    public PointsBuyerApp() {
        super("Points Item Buyer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 750); // Initial window size
        setLocationRelativeTo(null); // Center the window

        items = new ArrayList<>();
        budgetPoints = 0;
        selectedItemId = null;
        numberFormat = NumberFormat.getNumberInstance(Locale.US); // US locale for comma thousands separator

        createWidgets();
        loadSettings();
        updateBudgetDisplay();
        loadItemsToTable();
    }

    private void createWidgets() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10)); // Outer layout with spacing

        // --- Budget Frame ---
        JPanel budgetFrame = new JPanel(new GridBagLayout());
        budgetFrame.setBorder(BorderFactory.createTitledBorder("Total Budget Points"));
        GridBagConstraints gbcBudget = new GridBagConstraints(); // Use distinct GBC for clarity
        gbcBudget.insets = new Insets(5, 5, 5, 5);
        gbcBudget.anchor = GridBagConstraints.WEST;

        gbcBudget.gridx = 0; gbcBudget.gridy = 0;
        budgetFrame.add(new JLabel("Available Points:"), gbcBudget);

        gbcBudget.gridx = 1; gbcBudget.fill = GridBagConstraints.HORIZONTAL; gbcBudget.weightx = 1.0;
        budgetEntry = new JTextField(25);
        budgetEntry.setHorizontalAlignment(JTextField.RIGHT);
        budgetEntry.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateBudgetFromEntry();
            }
        });
        budgetEntry.addActionListener(e -> updateBudgetFromEntry()); // Enter key
        budgetFrame.add(budgetEntry, gbcBudget);

        contentPane.add(budgetFrame, BorderLayout.NORTH);

        // --- Main Content Area (holds Item Input/Edit and Item List) ---
        JPanel mainContentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMain = new GridBagConstraints(); // Distinct GBC for main content panel
        gbcMain.insets = new Insets(5, 10, 5, 10); // Padding around sections
        gbcMain.fill = GridBagConstraints.BOTH; // Allow components to fill their cells
        gbcMain.weightx = 1.0; // Allow horizontal expansion

        // --- Item Input/Edit Frame ---
        JPanel itemInputFrame = new JPanel(new GridBagLayout());
        itemInputFrame.setBorder(BorderFactory.createTitledBorder("Add/Edit Item Details"));
        GridBagConstraints gbcInput = new GridBagConstraints(); // Distinct GBC for input frame
        gbcInput.insets = new Insets(2, 5, 2, 5); // Smaller padding within input fields
        gbcInput.anchor = GridBagConstraints.WEST;

        gbcInput.gridx = 0; gbcInput.gridy = 0; gbcInput.weightx = 0; gbcInput.fill = GridBagConstraints.NONE;
        itemInputFrame.add(new JLabel("Item Name:"), gbcInput);
        gbcInput.gridx = 1; gbcInput.weightx = 1.0; gbcInput.fill = GridBagConstraints.HORIZONTAL;
        itemNameEntry = new JTextField(40);
        itemInputFrame.add(itemNameEntry, gbcInput);

        gbcInput.gridx = 0; gbcInput.gridy = 1; gbcInput.weightx = 0; gbcInput.fill = GridBagConstraints.NONE;
        itemInputFrame.add(new JLabel("Points Value:"), gbcInput);
        gbcInput.gridx = 1; gbcInput.weightx = 1.0; gbcInput.fill = GridBagConstraints.HORIZONTAL;
        itemPointsEntry = new JTextField(40);
        itemPointsEntry.setHorizontalAlignment(JTextField.RIGHT);
        itemInputFrame.add(itemPointsEntry, gbcInput);

        gbcInput.gridx = 0; gbcInput.gridy = 2; gbcInput.weightx = 0; gbcInput.fill = GridBagConstraints.NONE;
        itemInputFrame.add(new JLabel("Priority (1-99, higher is more):"), gbcInput);
        gbcInput.gridx = 1; gbcInput.weightx = 1.0; gbcInput.fill = GridBagConstraints.HORIZONTAL;
        itemPriorityEntry = new JTextField("1", 40);
        itemPriorityEntry.setHorizontalAlignment(JTextField.RIGHT);
        itemInputFrame.add(itemPriorityEntry, gbcInput);

        gbcInput.gridx = 0; gbcInput.gridy = 3; gbcInput.weightx = 0; gbcInput.fill = GridBagConstraints.NONE;
        itemInputFrame.add(new JLabel("Availability (Max quantity):"), gbcInput);
        gbcInput.gridx = 1; gbcInput.weightx = 1.0; gbcInput.fill = GridBagConstraints.HORIZONTAL;
        itemAvailabilityEntry = new JTextField("1", 40);
        itemAvailabilityEntry.setHorizontalAlignment(JTextField.RIGHT);
        itemInputFrame.add(itemAvailabilityEntry, gbcInput);

        JPanel buttonFrame = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton addItemButton = new JButton("Add New Item");
        addItemButton.addActionListener(e -> addOrUpdateItem(true));
        buttonFrame.add(addItemButton);

        JButton editItemButton = new JButton("Save Changes to Selected Item");
        editItemButton.addActionListener(e -> addOrUpdateItem(false));
        buttonFrame.add(editItemButton);

        gbcInput.gridx = 0; gbcInput.gridy = 4; gbcInput.gridwidth = 2; gbcInput.fill = GridBagConstraints.NONE; gbcInput.anchor = GridBagConstraints.CENTER;
        itemInputFrame.add(buttonFrame, gbcInput);

        // Add itemInputFrame to mainContentPanel (at the top, fixed height)
        gbcMain.gridx = 0; gbcMain.gridy = 0; gbcMain.weighty = 0; // Does not expand vertically
        mainContentPanel.add(itemInputFrame, gbcMain);


        // --- Item List Frame (JTable) ---
        JPanel itemListFrame = new JPanel(new BorderLayout());
        itemListFrame.setBorder(BorderFactory.createTitledBorder("Current Items"));

        String[] columnNames = {"ID", "Item Name", "Points Value", "Priority", "Availability"};
        itemTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 2) return Long.class;
                if (column == 3) return Integer.class;
                if (column == 4) return Long.class;
                return String.class;
            }
        };
        itemTable = new JTable(itemTableModel);
        itemTable.removeColumn(itemTable.getColumnModel().getColumn(0)); // Hide ID column

        // Column widths and alignment (must be done before setting preferredScrollableViewportSize accurately)
        TableColumn nameColumn = itemTable.getColumnModel().getColumn(0);
        nameColumn.setPreferredWidth(200);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        TableColumn pointsColumn = itemTable.getColumnModel().getColumn(1);
        pointsColumn.setPreferredWidth(100);
        pointsColumn.setCellRenderer(rightRenderer);

        TableColumn priorityColumn = itemTable.getColumnModel().getColumn(2);
        priorityColumn.setPreferredWidth(70);
        priorityColumn.setCellRenderer(centerRenderer);

        TableColumn availabilityColumn = itemTable.getColumnModel().getColumn(3);
        availabilityColumn.setPreferredWidth(90);
        availabilityColumn.setCellRenderer(centerRenderer);


        // --- FIX: Ensure the JTable's scroll pane gets a reasonable size ---
        // Calculate preferred width based on sum of column widths
        int preferredTableWidth = 0;
        for (int i = 0; i < itemTable.getColumnModel().getColumnCount(); i++) {
            preferredTableWidth += itemTable.getColumnModel().getColumn(i).getPreferredWidth();
        }
        // Add a small buffer for grid lines, scrollbar, etc.
        preferredTableWidth += itemTable.getIntercellSpacing().width * (itemTable.getColumnCount() -1);
        preferredTableWidth += 15; // Extra buffer for scrollbar or padding

        // Calculate preferred height based on a target number of rows (e.g., 15 rows)
        int targetRows = 15; // Increased target rows for more visible space
        int rowHeight = itemTable.getRowHeight();
        int headerHeight = itemTable.getTableHeader().getPreferredSize().height;
        int preferredTableHeight = (rowHeight * targetRows) + headerHeight;

        itemTable.setPreferredScrollableViewportSize(new Dimension(preferredTableWidth, preferredTableHeight));
        itemTable.setFillsViewportHeight(true); // Makes the table stretch vertically within its scroll pane


        // Selection Listener for the table
        itemTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    onItemSelect();
                }
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(itemTable);
        // It's also good practice to give the scroll pane a minimum size if it's placed in a flexible layout
        // tableScrollPane.setMinimumSize(new Dimension(300, 200)); // Consider this if still too small
        itemListFrame.add(tableScrollPane, BorderLayout.CENTER);

        JButton removeButton = new JButton("Remove Selected Item");
        removeButton.addActionListener(e -> removeSelectedItem());
        itemListFrame.add(removeButton, BorderLayout.SOUTH);

        // Add itemListFrame to mainContentPanel (below input frame, consumes remaining vertical space)
        gbcMain.gridx = 0; gbcMain.gridy = 1; gbcMain.weighty = 1.0; // This is the crucial part: expands vertically
        mainContentPanel.add(itemListFrame, gbcMain);

        contentPane.add(mainContentPanel, BorderLayout.CENTER); // mainContentPanel expands to fill JFrame's center


        // --- Actions Frame ---
        JPanel actionsFrame = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton calculateButton = new JButton("Calculate What I Can Buy");
        calculateButton.addActionListener(e -> calculateItemsToBuy());
        actionsFrame.add(calculateButton);

        // --- Results Frame ---
        JPanel resultsFrame = new JPanel(new BorderLayout());
        resultsFrame.setBorder(BorderFactory.createTitledBorder("Calculation Results"));
        resultsText = new JTextArea(8, 40); // Slightly reduced initial height for results to give more to table
        resultsText.setEditable(false);
        resultsText.setLineWrap(true);
        resultsText.setWrapStyleWord(true);
        JScrollPane resultsScrollPane = new JScrollPane(resultsText);
        resultsFrame.add(resultsScrollPane, BorderLayout.CENTER);

        // Combine Actions and Results frames into a single panel for the SOUTH of the main window
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(actionsFrame, BorderLayout.NORTH);
        bottomPanel.add(resultsFrame, BorderLayout.CENTER); // Results will expand in this panel
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
    }

    private String formatNumber(long num) {
        return numberFormat.format(num);
    }

    private long unformatNumber(String numStr) throws ParseException {
        return numberFormat.parse(numStr).longValue();
    }

    private void updateBudgetFromEntry() {
        try {
            long newBudget = unformatNumber(budgetEntry.getText().trim());
            if (newBudget < 0) {
                JOptionPane.showMessageDialog(this, "Budget points cannot be negative.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                updateBudgetDisplay();
                return;
            }
            budgetPoints = newBudget;
            updateBudgetDisplay();
            saveSettings();
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid whole number for budget points.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            updateBudgetDisplay();
        }
    }

    private void updateBudgetDisplay() {
        budgetEntry.setText(formatNumber(budgetPoints));
    }

    private boolean validateItemInputs(String name, String pointsStr, String priorityStr, String availabilityStr) {
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Item Name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        long points;
        try {
            points = unformatNumber(pointsStr);
            if (points <= 0) {
                JOptionPane.showMessageDialog(this, "Points Value must be a positive whole number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Points Value must be a valid whole number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        int priority;
        try {
            priority = Integer.parseInt(priorityStr);
            if (!(1 <= priority && priority <= 99)) {
                JOptionPane.showMessageDialog(this, "Priority must be an integer between 1 and 99.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Priority must be a valid whole number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        long availability;
        try {
            availability = unformatNumber(availabilityStr);
            if (availability < 0) {
                JOptionPane.showMessageDialog(this, "Availability cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Availability must be a valid whole number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void addOrUpdateItem(boolean isNewItem) {
        String name = itemNameEntry.getText().trim();
        String pointsStr = itemPointsEntry.getText().trim();
        String priorityStr = itemPriorityEntry.getText().trim();
        String availabilityStr = itemAvailabilityEntry.getText().trim();

        if (!validateItemInputs(name, pointsStr, priorityStr, availabilityStr)) {
            return;
        }

        try {
            long points = unformatNumber(pointsStr);
            int priority = Integer.parseInt(priorityStr);
            long availability = unformatNumber(availabilityStr);

            if (isNewItem) {
                String id = UUID.randomUUID().toString();
                Item newItem = new Item(id, name, points, priority, availability);
                items.add(newItem);
                itemTableModel.addRow(new Object[]{newItem.getId(), newItem.getName(), formatNumber(newItem.getPoints()), newItem.getPriority(), newItem.getAvailability()});
            } else {
                if (selectedItemId == null) {
                    JOptionPane.showMessageDialog(this, "Please select an item from the list to edit.", "No Item Selected", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Item itemToUpdate = null;
                for (Item item : items) {
                    if (item.getId().equals(selectedItemId)) {
                        itemToUpdate = item;
                        break;
                    }
                }

                if (itemToUpdate != null) {
                    itemToUpdate.setName(name);
                    itemToUpdate.setPoints(points);
                    itemToUpdate.setPriority(priority);
                    itemToUpdate.setAvailability(availability);

                    int selectedRow = itemTable.getSelectedRow();
                    if (selectedRow != -1) {
                        itemTableModel.setValueAt(name, selectedRow, 1);
                        itemTableModel.setValueAt(formatNumber(points), selectedRow, 2);
                        itemTableModel.setValueAt(priority, selectedRow, 3);
                        itemTableModel.setValueAt(availability, selectedRow, 4);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Selected item not found in internal list. Please re-select.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            clearItemInputs();
            selectedItemId = null;
            saveSettings();
        } catch (ParseException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred during number parsing: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onItemSelect() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow != -1) {
            selectedItemId = (String) itemTableModel.getValueAt(selectedRow, 0);

            Item selectedItem = null;
            for (Item item : items) {
                if (item.getId().equals(selectedItemId)) {
                    selectedItem = item;
                    break;
                }
            }

            if (selectedItem != null) {
                itemNameEntry.setText(selectedItem.getName());
                itemPointsEntry.setText(formatNumber(selectedItem.getPoints()));
                itemPriorityEntry.setText(String.valueOf(selectedItem.getPriority()));
                itemAvailabilityEntry.setText(String.valueOf(selectedItem.getAvailability()));
            }
        } else {
            clearItemInputs();
            selectedItemId = null;
        }
    }

    private void clearItemInputs() {
        itemNameEntry.setText("");
        itemPointsEntry.setText("");
        itemPriorityEntry.setText("1");
        itemAvailabilityEntry.setText("1");
        itemTable.clearSelection();
    }

    private void removeSelectedItem() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove the selected item?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String idToRemove = (String) itemTableModel.getValueAt(selectedRow, 0);
        items.removeIf(item -> item.getId().equals(idToRemove));
        itemTableModel.removeRow(selectedRow);

        if (selectedItemId != null && selectedItemId.equals(idToRemove)) {
            clearItemInputs();
            selectedItemId = null;
        }

        saveSettings();
        JOptionPane.showMessageDialog(this, "Selected item has been removed.", "Item Removed", JOptionPane.INFORMATION_MESSAGE);
    }

    private void calculateItemsToBuy() {
        resultsText.setText("");

        if (items.isEmpty()) {
            resultsText.append("No items available to buy. Please add items first.\n");
            return;
        }

        long currentBudget = budgetPoints;
        if (currentBudget <= 0) {
            resultsText.append("Your budget is 0 or negative. Cannot buy any items.\n");
            return;
        }

        List<Item> sortedItems = items.stream()
                .sorted(Comparator
                        .<Item>comparingInt(Item::getPriority).reversed()
                        .thenComparingLong(Item::getPoints))
                .collect(Collectors.toList());

        List<String> boughtSummary = new ArrayList<>();
        
        for (Item item : sortedItems) {
            if (currentBudget <= 0) {
                break;
            }

            String itemName = item.getName();
            long itemPoints = item.getPoints();
            long itemAvailability = item.getAvailability();

            if (itemPoints <= 0) {
                continue;
            }
            if (itemAvailability == 0) {
                continue;
            }

            long quantityByBudget = currentBudget / itemPoints;
            long quantityToBuy = Math.min(quantityByBudget, itemAvailability);

            if (quantityToBuy > 0) {
                long totalCostForItem = quantityToBuy * itemPoints;
                boughtSummary.add(
                    String.format("  - %s: %d item(s) (Total cost: %s points)",
                        itemName, quantityToBuy, formatNumber(totalCostForItem))
                );
                currentBudget -= totalCostForItem;
            }
        }
        
        if (!boughtSummary.isEmpty()) {
            resultsText.append(String.format("Based on your budget of %s points, you can buy:\n", formatNumber(budgetPoints)));
            for (String line : boughtSummary) {
                resultsText.append(line + "\n");
            }
            resultsText.append(String.format("\nRemaining Budget: %s points\n", formatNumber(currentBudget)));
        } else {
            resultsText.append(String.format("With %s points, you cannot afford any items.\n", formatNumber(budgetPoints)));
            resultsText.append("Consider increasing your budget or adding cheaper items.\n");
        }
    }

    private void loadSettings() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            String currentSection = "";
            items.clear();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                } else if (currentSection.equals("Budget")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2 && parts[0].trim().equals("total_points")) {
                        try {
                            budgetPoints = unformatNumber(parts[1].trim());
                        } catch (ParseException e) {
                            System.err.println("Warning: Could not parse budget points: " + parts[1]);
                            budgetPoints = 0;
                        }
                    }
                } else if (currentSection.equals("Items")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String itemValue = parts[1].trim();
                        String[] itemParts = itemValue.split(",", 4);

                        String id = UUID.randomUUID().toString();
                        String name = "";
                        long points = 0;
                        int priority = 1;
                        long availability = 1;

                        if (itemParts.length >= 3) {
                            name = itemParts[0].trim();
                            try {
                                points = unformatNumber(itemParts[1].trim());
                                priority = Integer.parseInt(itemParts[2].trim());
                            } catch (ParseException | NumberFormatException e) {
                                System.err.println("Skipping malformed item entry (numeric) in INI: " + itemValue);
                                continue;
                            }
                        } else {
                            System.err.println("Skipping malformed item entry (parts count) in INI: " + itemValue);
                            continue;
                        }

                        if (itemParts.length == 4) {
                            try {
                                availability = unformatNumber(itemParts[3].trim());
                            } catch (ParseException | NumberFormatException e) {
                                System.err.println("Skipping malformed item availability in INI: " + itemParts[3]);
                                availability = 1;
                            }
                        }

                        items.add(new Item(id, name, points, priority, availability));
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading settings: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSettings() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            writer.write("[Budget]\n");
            writer.write("total_points=" + budgetPoints + "\n\n");

            writer.write("[Items]\n");
            for (Item item : items) {
                writer.write("item_" + item.getId() + "=" +
                             item.getName() + "," +
                             item.getPoints() + "," +
                             item.getPriority() + "," +
                             item.getAvailability() + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadItemsToTable() {
        itemTableModel.setRowCount(0);
        
        for (Item item : items) {
            itemTableModel.addRow(new Object[]{
                item.getId(),
                item.getName(),
                formatNumber(item.getPoints()),
                item.getPriority(),
                item.getAvailability()
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PointsBuyerApp().setVisible(true);
        });
    }
}