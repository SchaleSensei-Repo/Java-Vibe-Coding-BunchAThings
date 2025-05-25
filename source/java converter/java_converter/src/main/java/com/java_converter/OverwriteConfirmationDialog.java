// src/com/example/converter/OverwriteConfirmationDialog.java
package com.java_converter;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OverwriteConfirmationDialog extends JDialog {

    private boolean confirmed = false; // True if user clicked "Overwrite Selected", false if "Cancel"
    private List<JCheckBox> fileCheckboxes; // Holds all checkboxes for easy manipulation
    private List<Path> selectedPathsToOverwrite; // Paths of files the user approved for overwrite

    /**
     * Constructs the overwrite confirmation dialog.
     * @param parent The parent JFrame for modal behavior and centering.
     * @param existingFiles A list of Path objects for files that already exist in the output directory.
     */
    public OverwriteConfirmationDialog(JFrame parent, List<Path> existingFiles) {
        super(parent, "Confirm Overwrite", true); // 'true' makes it a modal dialog
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Close operation
        setSize(550, 450); // Initial increased size (width, height)
        setLocationRelativeTo(parent); // Center relative to the parent frame

        fileCheckboxes = new ArrayList<>();
        selectedPathsToOverwrite = new ArrayList<>();

        initComponents(existingFiles);
    }

    private void initComponents(List<Path> existingFiles) {
        setLayout(new BorderLayout(10, 10)); // Outer layout for padding

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Message Label ---
        JLabel messageLabel = new JLabel("The following files already exist in the output directory. Check which ones to overwrite:", SwingConstants.CENTER);
        contentPanel.add(messageLabel, BorderLayout.NORTH);

        // --- Scrollable List of Files with Checkboxes ---
        JPanel filesListPanel = new JPanel();
        filesListPanel.setLayout(new BoxLayout(filesListPanel, BoxLayout.Y_AXIS)); // Stack checkboxes vertically
        filesListPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0)); // Padding

        for (Path file : existingFiles) {
            // Display filename and a hint of its parent directory for context
            String displayPath = file.getFileName().toString();
            if (file.getParent() != null) {
                displayPath += " (in " + file.getParent().getFileName() + ")";
            }

            JCheckBox checkBox = new JCheckBox(displayPath);
            checkBox.setSelected(true); // Default: all files selected for overwrite
            checkBox.putClientProperty("filePath", file); // Store the actual Path object with the checkbox
            fileCheckboxes.add(checkBox); // Add to our list for easy iteration
            filesListPanel.add(checkBox); // Add to the panel
        }

        JScrollPane scrollPane = new JScrollPane(filesListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEtchedBorder()); // Visual border
        contentPanel.add(scrollPane, BorderLayout.CENTER);


        // --- Control Buttons (Select All/Deselect All, Overwrite/Cancel) ---
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5)); // For inner padding/spacing

        // Select All / Deselect All buttons
        JPanel selectAllPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> setAllCheckboxes(true));
        JButton deselectAllButton = new JButton("Deselect All");
        deselectAllButton.addActionListener(e -> setAllCheckboxes(false));
        selectAllPanel.add(selectAllButton);
        selectAllPanel.add(deselectAllButton);
        controlPanel.add(selectAllPanel, BorderLayout.NORTH);

        // Action buttons
        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10)); // Wider gap, more padding
        JButton overwriteButton = new JButton("Overwrite Selected");
        overwriteButton.setFont(overwriteButton.getFont().deriveFont(Font.BOLD, 14)); // Make button stand out
        overwriteButton.addActionListener(e -> {
            confirmed = true;
            // Collect all paths from checkboxes that are currently selected
            selectedPathsToOverwrite = fileCheckboxes.stream()
                    .filter(JCheckBox::isSelected)
                    .map(cb -> (Path) cb.getClientProperty("filePath"))
                    .collect(Collectors.toList());
            dispose(); // Close the dialog
        });
        actionButtonsPanel.add(overwriteButton);

        JButton cancelButton = new JButton("Cancel Conversion");
        cancelButton.addActionListener(e -> {
            confirmed = false; // User cancelled
            selectedPathsToOverwrite.clear(); // Ensure no files are marked for overwrite
            dispose(); // Close the dialog
        });
        actionButtonsPanel.add(cancelButton);
        controlPanel.add(actionButtonsPanel, BorderLayout.SOUTH);

        contentPanel.add(controlPanel, BorderLayout.SOUTH); // Add control panel to content
        add(contentPanel); // Add content panel to dialog
    }

    /**
     * Sets the selected state of all checkboxes in the dialog.
     * @param selected True to select all, false to deselect all.
     */
    private void setAllCheckboxes(boolean selected) {
        for (JCheckBox cb : fileCheckboxes) {
            cb.setSelected(selected);
        }
    }

    /**
     * Returns true if the user confirmed the overwrite (clicked "Overwrite Selected"), false otherwise.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Returns the list of Path objects that the user chose to overwrite.
     * This list is only valid if isConfirmed() is true.
     */
    public List<Path> getSelectedFilesToOverwrite() {
        return selectedPathsToOverwrite;
    }
}