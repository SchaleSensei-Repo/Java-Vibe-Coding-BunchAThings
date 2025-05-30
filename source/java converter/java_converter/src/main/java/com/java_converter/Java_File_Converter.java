// src/com/example/converter/Java_File_Converter.java
package com.java_converter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections; // For Collections.emptyList()
import java.util.List;
import java.util.Map; // For HashMap

public class Java_File_Converter extends JFrame {

    private JTextField inputPathField;
    private JTextField outputPathField;
    private DefaultListModel<String> selectedFilesListModel;
    private JList<String> selectedFilesList;
    private JCheckBox modeToggle;
    private JLabel currentModeLabel;
    private JLabel statusLabel;
    private JTextArea logTextArea; // For displaying conversion output

    private FileConverterSettings settings;

    public Java_File_Converter() {
        super("Java/Text File Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 650); // Slightly increased height for the log panel
        setLocationRelativeTo(null); // Center the window

        settings = new FileConverterSettings();
        initComponents();
        loadSavedSettings();
        updateModeLabel(); // Initial update for the mode label

        this.setVisible(true); // Make the JFrame visible
    }

    private void initComponents() {
        // --- Main Panel with BorderLayout ---
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Corrected: BorderFactory

        // --- Top Panel for Input/Output Paths ---
        JPanel pathPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        // Input Path
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        inputPathField = new JTextField(40);
        inputPathField.setEditable(false);
        JButton browseInputButton = new JButton("Browse Input");
        browseInputButton.addActionListener(e -> browseInputLocation());
        inputPanel.add(new JLabel("Input Location:"));
        inputPanel.add(inputPathField);
        inputPanel.add(browseInputButton);
        pathPanel.add(inputPanel);

        // Output Path
        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        outputPathField = new JTextField(40);
        outputPathField.setEditable(false);
        JButton browseOutputButton = new JButton("Browse Output");
        browseOutputButton.addActionListener(e -> browseOutputLocation());
        outputPanel.add(new JLabel("Output Location:"));
        outputPanel.add(outputPathField);
        outputPanel.add(browseOutputButton);
        pathPanel.add(outputPanel);

        mainPanel.add(pathPanel, BorderLayout.NORTH);

        // --- Center Panel with JSplitPane for Files List and Log ---
        selectedFilesListModel = new DefaultListModel<>();
        selectedFilesList = new JList<>(selectedFilesListModel);
        selectedFilesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane filesScrollPane = new JScrollPane(selectedFilesList);
        filesScrollPane.setBorder(BorderFactory.createTitledBorder("Selected Files for Conversion"));

        // Clear files button
        JButton clearFilesButton = new JButton("Clear Selected Files");
        clearFilesButton.addActionListener(e -> selectedFilesListModel.clear());
        JPanel filesListControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filesListControlPanel.add(clearFilesButton);

        JPanel filesListPanel = new JPanel(new BorderLayout());
        filesListPanel.add(filesScrollPane, BorderLayout.CENTER);
        filesListPanel.add(filesListControlPanel, BorderLayout.SOUTH);


        // Log Text Area
        logTextArea = new JTextArea(10, 40); // 10 rows, 40 cols (preferred size hint)
        logTextArea.setEditable(false); // Make it read-only
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        logTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logTextArea.setBackground(Color.LIGHT_GRAY); // Make it distinct
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Conversion Log"));

        // JSplitPane to combine files list and log
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, filesListPanel, logScrollPane);
        splitPane.setResizeWeight(0.6); // Give more space to the files list initially
        splitPane.setDividerLocation(0.6); // Set initial divider position (60% for files, 40% for log)

        mainPanel.add(splitPane, BorderLayout.CENTER);


        // --- Bottom Panel for Controls and Status ---
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));

        // Mode Toggle
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        modeToggle = new JCheckBox("Convert .java to .txt");
        modeToggle.setSelected(true); // Default to .java to .txt
        modeToggle.addItemListener(e -> updateModeLabel());
        modePanel.add(modeToggle);

        currentModeLabel = new JLabel("Current Operation: .java to .txt", SwingConstants.CENTER);
        modePanel.add(currentModeLabel);
        controlPanel.add(modePanel, BorderLayout.NORTH);


        // Convert Button
        JButton convertButton = new JButton("Convert Files");
        convertButton.setFont(new Font("Arial", Font.BOLD, 16));
        convertButton.addActionListener(e -> convertFiles());
        controlPanel.add(convertButton, BorderLayout.CENTER);

        // Status Label
        statusLabel = new JLabel("Ready.", SwingConstants.CENTER);
        statusLabel.setForeground(Color.BLUE);
        controlPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Save settings on window close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveCurrentSettings();
            }
        });
    }

    private void loadSavedSettings() {
        inputPathField.setText(settings.getInputDir());
        outputPathField.setText(settings.getOutputDir());
        modeToggle.setSelected(settings.isJavaToTxtMode());
        updateModeLabel(); // Ensure label is updated when settings load
    }

    private void saveCurrentSettings() {
        settings.saveSettings(
                inputPathField.getText(),
                outputPathField.getText(),
                modeToggle.isSelected()
        );
    }

    private void browseInputLocation() {
        JFileChooser fileChooser = new JFileChooser(inputPathField.getText());
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true); // Allow multiple file selection
        fileChooser.setAcceptAllFileFilterUsed(false); // Only show specific filters, remove "All Files" option

        // Determine the filter based on the current mode
        boolean isJavaToTxt = modeToggle.isSelected();
        FileNameExtensionFilter filter;
        String requiredExt; // The extension files *must* have to be considered for conversion

        if (isJavaToTxt) {
            filter = new FileNameExtensionFilter("Java Source Files (*.java)", "java");
            requiredExt = ".java";
        } else {
            filter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
            requiredExt = ".txt";
        }
        fileChooser.setFileFilter(filter); // Apply the filter

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selected = fileChooser.getSelectedFiles();
            if (selected.length > 0) {
                // Update input path field to the directory of the first selected item
                // or the selected directory itself
                File firstSelected = selected[0];
                if (firstSelected.isDirectory()) {
                    inputPathField.setText(firstSelected.getAbsolutePath());
                } else {
                    inputPathField.setText(firstSelected.getParentFile().getAbsolutePath());
                }

                // Add selected files/directory contents to the list model
                for (File file : selected) {
                    if (file.isFile()) {
                        // Only add files that match the required extension
                        if (file.getName().toLowerCase().endsWith(requiredExt)) {
                            // Prevent duplicates in the list
                            if (!selectedFilesListModel.contains(file.getAbsolutePath())) {
                                selectedFilesListModel.addElement(file.getAbsolutePath());
                            }
                        } else {
                            logMessage("Skipped during selection: " + file.getName() + " (doesn't match expected extension " + requiredExt + ")", false); // Not an error, just an informative skip
                        }
                    } else if (file.isDirectory()) {
                        // If a directory is selected, add all files matching the required extension from it
                        File[] filesInDir = file.listFiles((dir, name) -> name.toLowerCase().endsWith(requiredExt));
                        if (filesInDir != null) {
                            int filesAddedFromDir = 0;
                            for (File f : filesInDir) {
                                if (f.isFile()) {
                                    // Prevent duplicates
                                    if (!selectedFilesListModel.contains(f.getAbsolutePath())) {
                                        selectedFilesListModel.addElement(f.getAbsolutePath());
                                        filesAddedFromDir++;
                                    }
                                }
                            }
                            logMessage("Added " + filesAddedFromDir + " " + requiredExt + " files from directory: " + file.getName(), false);
                        } else {
                             logMessage("No " + requiredExt + " files found in directory: " + file.getName(), false);
                        }
                    }
                }
            }
        }
    }

    private void browseOutputLocation() {
        JFileChooser fileChooser = new JFileChooser(outputPathField.getText());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            outputPathField.setText(selectedDir.getAbsolutePath());
        }
    }

    private void updateModeLabel() {
        if (modeToggle.isSelected()) {
            currentModeLabel.setText("Current Operation: .java to .txt");
        } else {
            currentModeLabel.setText("Current Operation: .txt to .java");
        }
    }

    private void convertFiles() {
        logTextArea.setText(""); // Clear previous log
        logMessage("Starting conversion...", false);
        statusLabel.setText("Processing...");
        statusLabel.setForeground(Color.BLACK); // Set to a neutral color during processing

        if (selectedFilesListModel.isEmpty()) {
            statusLabel.setText("No files selected for conversion.");
            statusLabel.setForeground(Color.ORANGE);
            logMessage("Conversion aborted: No files selected.", true);
            return;
        }

        String outputDirPath = outputPathField.getText();
        if (outputDirPath.isEmpty()) {
            statusLabel.setText("Please select an output directory.");
            statusLabel.setForeground(Color.RED);
            logMessage("Conversion aborted: No output directory selected.", true);
            return;
        }

        Path outputDir = Paths.get(outputDirPath);
        if (!Files.isDirectory(outputDir)) {
            try {
                Files.createDirectories(outputDir);
                logMessage("Output directory created: " + outputDir.getFileName(), false);
                statusLabel.setText("Output directory created: " + outputDir.getFileName());
                statusLabel.setForeground(Color.BLUE);
            } catch (IOException e) {
                statusLabel.setText("Error creating output directory.");
                statusLabel.setForeground(Color.RED);
                logMessage("Error creating output directory '" + outputDir.getFileName() + "': " + e.getMessage(), true);
                return;
            }
        }

        boolean isJavaToTxt = modeToggle.isSelected();
        String newExtension = isJavaToTxt ? ".txt" : ".java";
        String requiredExtension = isJavaToTxt ? ".java" : ".txt";

        // --- Step 1: Prepare potential conversions and identify existing output files ---
        // Map to hold Input Path -> Expected Output Path for VALID candidates
        Map<Path, Path> validInputToExpectedOutputMap = new java.util.HashMap<>();
        for (int i = 0; i < selectedFilesListModel.size(); i++) {
            Path inputFile = Paths.get(selectedFilesListModel.getElementAt(i));
            String fileName = inputFile.getFileName().toString();

            if (fileName.toLowerCase().endsWith(requiredExtension)) {
                String baseName = getBaseName(fileName, requiredExtension);
                validInputToExpectedOutputMap.put(inputFile, outputDir.resolve(baseName + newExtension));
            } else {
                logMessage("Skipping " + fileName + ": Incorrect file type for current mode (expected " + requiredExtension + ").", false);
                // This file is not a valid candidate for conversion in current mode.
            }
        }

        if (validInputToExpectedOutputMap.isEmpty()) {
            statusLabel.setText("No valid files found for conversion in current mode.");
            statusLabel.setForeground(Color.ORANGE);
            logMessage("Conversion aborted: No valid files for current operation mode.", true);
            return;
        }

        // Collect actual existing output files from the predicted ones
        List<Path> existingOutputFiles = new ArrayList<>();
        for (Path outputFile : validInputToExpectedOutputMap.values()) {
            if (Files.exists(outputFile)) {
                existingOutputFiles.add(outputFile);
            }
        }

        // --- Step 2: Handle Overwrite Confirmation Dialog if needed ---
        List<Path> approvedForOverwrite = Collections.emptyList(); // Default: empty list (no overwrite)
        if (!existingOutputFiles.isEmpty()) {
            OverwriteConfirmationDialog dialog = new OverwriteConfirmationDialog(this, existingOutputFiles);
            dialog.setVisible(true); // This call blocks until the dialog is closed

            if (dialog.isConfirmed()) {
                approvedForOverwrite = dialog.getSelectedFilesToOverwrite();
                logMessage("User approved overwriting " + approvedForOverwrite.size() + " files.", false);
            } else {
                statusLabel.setText("Conversion cancelled by user.");
                statusLabel.setForeground(Color.ORANGE);
                logMessage("Conversion cancelled by user.", true);
                return; // User explicitly cancelled the entire conversion
            }
        }

        // --- Step 3: Perform the actual conversion based on user's decisions ---
        int successfulConversions = 0;
        int skippedConversions = 0; // Skipped due to extension mismatch or existing & not approved
        int failedConversions = 0;

        for (Map.Entry<Path, Path> entry : validInputToExpectedOutputMap.entrySet()) {
            Path inputFile = entry.getKey();
            Path outputFile = entry.getValue();

            // Check if outputFile exists and if it was NOT approved for overwrite
            if (Files.exists(outputFile) && !approvedForOverwrite.contains(outputFile)) {
                logMessage("Skipping " + inputFile.getFileName() + ": Output file already exists and was not approved for overwrite.", false);
                skippedConversions++;
                continue; // Move to the next file
            }

            // Perform conversion
            try {
                String content = Files.readString(inputFile); // Requires Java 11+
                Files.writeString(outputFile, content);
                successfulConversions++;
                logMessage("Converted: " + inputFile.getFileName() + " -> " + outputFile.getFileName(), false);
            } catch (IOException e) {
                logMessage("Error converting " + inputFile.getFileName() + ": " + e.getMessage(), true);
                failedConversions++;
            }
        }

        // --- Step 4: Update final status message ---
        String finalStatusMessage = "";
        if (successfulConversions > 0) {
            finalStatusMessage += successfulConversions + " converted";
            statusLabel.setForeground(Color.GREEN);
        }
        if (skippedConversions > 0) {
            if (!finalStatusMessage.isEmpty()) finalStatusMessage += ", ";
            finalStatusMessage += skippedConversions + " skipped";
            if (successfulConversions == 0 && failedConversions == 0) {
                statusLabel.setForeground(Color.BLUE); // Informative skip
            } else if (successfulConversions > 0 && failedConversions == 0) {
                statusLabel.setForeground(Color.GREEN); // Still good, just some skipped
            } else {
                statusLabel.setForeground(Color.ORANGE); // Mix of success/fail/skip
            }
        }
        if (failedConversions > 0) {
            if (!finalStatusMessage.isEmpty()) finalStatusMessage += ", ";
            finalStatusMessage += failedConversions + " failed";
            statusLabel.setForeground(Color.RED); // Red if any failed
        }

        if (finalStatusMessage.isEmpty()) {
            finalStatusMessage = "No files were processed.";
            statusLabel.setForeground(Color.BLUE);
        }

        statusLabel.setText("Conversion finished: " + finalStatusMessage + ".");
        logMessage("Conversion process finished. " + finalStatusMessage + ".", false);
    }

    private String getBaseName(String fileName, String extension) {
        if (fileName.toLowerCase().endsWith(extension)) {
            return fileName.substring(0, fileName.length() - extension.length());
        }
        return fileName;
    }

    /**
     * Appends a message to the log text area and optionally to the console.
     * @param message The message to log.
     * @param isError If true, the message is an error and is also printed to System.err.
     */
    private void logMessage(String message, boolean isError) {
        // Ensure GUI updates are on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(message + "\n");
            // Scroll to the bottom
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            if (isError) {
                System.err.println("LOG (Error): " + message);
            } else {
                System.out.println("LOG: " + message);
            }
        });
    }

    public static void main(String[] args) {
        // Ensure GUI updates are done on the Event Dispatch Thread
        SwingUtilities.invokeLater(Java_File_Converter::new);
    }
}