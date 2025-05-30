package com.random_list_picker;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret; // For auto-scrolling JTextArea
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// You will need to add the Gson library to your project's classpath.
// Download it from: https://mvnrepository.com/artifact/com.google.code.gson/gson
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class Random_List_Picker extends JFrame {

    private JList<String> fileListbox;
    private DefaultListModel<String> fileListModel; // Model to manage items in JList
    private JTextArea resultsText;
    private List<String> selectedFiles; // Stores full paths of selected files
    private String lastDirectory;
    private final String settingsFile = "line_picker_settings.ini"; // Name matches Python, content is JSON

    // Inner class to represent the settings structure for Gson
    private static class AppSettings {
        List<String> files;
        String last_dir;
    }

    public Random_List_Picker() {
        // --- JFrame setup (equivalent to master in Tkinter) ---
        setTitle("Random List Picker");
        setSize(600, 700); // Initial window size
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle closing manually for saving settings
        setResizable(true); // Allow resizing
        setLocationRelativeTo(null); // Center the window on screen

        selectedFiles = new ArrayList<>();
        lastDirectory = System.getProperty("user.dir"); // Default to current working directory

        createWidgets();
        loadSettings();

        // Bind the closing event to save settings
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClosing();
            }
        });
    }

    private void createWidgets() {
        // Use GridBagLayout for the main content panel for flexible resizing
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding around components

        // --- Frame for file selection ---
        JPanel fileFrame = new JPanel(new BorderLayout(5, 5));
        fileFrame.setBorder(BorderFactory.createTitledBorder("Selected Text Files"));

        fileListModel = new DefaultListModel<>();
        fileListbox = new JList<>(fileListModel);
        fileListbox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileListbox.setBorder(BorderFactory.createEtchedBorder()); // Visual border
        JScrollPane fileListScrollPane = new JScrollPane(fileListbox);
        fileListScrollPane.setPreferredSize(new Dimension(500, 150)); // Initial size hint for the listbox
        fileFrame.add(fileListScrollPane, BorderLayout.CENTER);

        JPanel fileButtonsFrame = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JButton addFilesButton = new JButton("Add File(s)");
        addFilesButton.addActionListener(e -> addFiles());
        fileButtonsFrame.add(addFilesButton);

        JButton removeFileButton = new JButton("Remove Selected");
        removeFileButton.addActionListener(e -> removeSelectedFile());
        fileButtonsFrame.add(removeFileButton);
        fileFrame.add(fileButtonsFrame, BorderLayout.SOUTH);

        // Add fileFrame to contentPanel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weighty = 0.3; // Proportion of vertical space
        contentPanel.add(fileFrame, gbc);

        // --- Frame for action buttons ---
        JPanel actionFrame = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5)); // Increased horizontal gap

        JButton pickButton = new JButton("Pick Random");
        pickButton.setFont(new Font("Arial", Font.BOLD, 12));
        pickButton.setPreferredSize(new Dimension(150, 40)); // Fixed size for emphasis
        pickButton.addActionListener(e -> pickRandomItems());
        actionFrame.add(pickButton);

        JButton resetButton = new JButton("Reset All");
        resetButton.setFont(new Font("Arial", Font.PLAIN, 12));
        resetButton.setPreferredSize(new Dimension(150, 40)); // Fixed size
        resetButton.addActionListener(e -> resetApplication());
        actionFrame.add(resetButton);

        // Add actionFrame to contentPanel
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Only fill horizontally
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 0; // Don't expand vertically
        contentPanel.add(actionFrame, gbc);

        // --- Frame for results ---
        JPanel resultsFrame = new JPanel(new BorderLayout(5, 5));
        resultsFrame.setBorder(BorderFactory.createTitledBorder("Random Selections"));

        resultsText = new JTextArea(15, 60); // Rows, columns as initial size hint
        resultsText.setLineWrap(true); // wrap=tk.WORD
        resultsText.setWrapStyleWord(true); // wrap=tk.WORD
        resultsText.setFont(new Font("Consolas", Font.PLAIN, 14));
        resultsText.setEditable(false); // Make it read-only (state=tk.DISABLED)
        JScrollPane resultsScrollPane = new JScrollPane(resultsText);
        // Ensure scrollbar is always visible or based on content
        resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // No horizontal scroll
        resultsFrame.add(resultsScrollPane, BorderLayout.CENTER);

        // Make sure the caret automatically scrolls to the end when new text is added
        DefaultCaret caret = (DefaultCaret) resultsText.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // Add resultsFrame to contentPanel
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.weighty = 0.7; // Proportion of vertical space (more space for results)
        contentPanel.add(resultsFrame, gbc);

        // Add the main content panel to the JFrame
        this.add(contentPanel, BorderLayout.CENTER);
    }

    private void addFiles() {
        JFileChooser fileChooser = new JFileChooser(lastDirectory);
        fileChooser.setMultiSelectionEnabled(true);
        // Filter for text files
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        fileChooser.setDialogTitle("Select Text Files");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            if (files.length > 0) {
                lastDirectory = files[0].getParentFile().getAbsolutePath();
            }

            // Add new files, avoiding duplicates
            for (File file : files) {
                String path = file.getAbsolutePath();
                if (!selectedFiles.contains(path)) {
                    selectedFiles.add(path);
                }
            }
            updateFileListbox();
            saveSettings(); // Save immediately after adding files
        }
    }

    private void removeSelectedFile() {
        int selectedIndex = fileListbox.getSelectedIndex();
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(this, "Please select a file to remove.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Get the path from the model and remove it from our internal list
        String pathToRemove = fileListModel.getElementAt(selectedIndex);
        selectedFiles.remove(pathToRemove);

        updateFileListbox(); // Update the JList display
        saveSettings(); // Save after removing files
    }

    private void updateFileListbox() {
        fileListModel.clear(); // Clear existing entries
        for (String path : selectedFiles) {
            // Display only the filename in the listbox, but internal list has full path
            // The Python code displayed full path, so let's stick to that for consistency.
            fileListModel.addElement(path);
        }
    }

    private void pickRandomItems() {
        resultsText.setEditable(true); // Enable editing to clear/insert
        resultsText.setText(""); // Clear previous results

        if (selectedFiles.isEmpty()) {
            resultsText.append("Please select at least one text file first.\n");
            resultsText.setEditable(false);
            return;
        }

        Random random = new Random();
        StringBuilder sb = new StringBuilder(); // To build the results string efficiently

        for (String filePath : selectedFiles) {
            File file = new File(filePath);
            String fileName = file.getName(); // os.path.basename equivalent
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                List<String> items = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) { // Filter out empty lines
                        items.add(trimmedLine);
                    }
                }

                if (!items.isEmpty()) {
                    String selectedItem = items.get(random.nextInt(items.size())); // random.choice
                    sb.append(String.format("From '%s': %s\n", fileName, selectedItem));
                } else {
                    sb.append(String.format("From '%s': (File is empty or contains no valid items)\n", fileName));
                }
            } catch (FileNotFoundException e) {
                sb.append(String.format("Error: File not found - '%s'\n", fileName));
            } catch (IOException e) {
                sb.append(String.format("Error reading '%s': %s\n", fileName, e.getMessage()));
            }
        }
        resultsText.append(sb.toString());
        resultsText.setEditable(false); // Make it read-only again
    }

    private void resetApplication() {
        int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all selected files and results?",
                "Reset Application",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            selectedFiles.clear();
            lastDirectory = System.getProperty("user.dir"); // Reset to current directory
            updateFileListbox();

            resultsText.setEditable(true);
            resultsText.setText(""); // Clear results
            resultsText.setEditable(false);

            saveSettings(); // Save the reset state
        }
    }

    private void loadSettings() {
        File settingsFileObj = new File(settingsFile);
        if (settingsFileObj.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(settingsFileObj), StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                AppSettings settings = gson.fromJson(reader, AppSettings.class);

                if (settings != null) {
                    selectedFiles.clear(); // Clear existing defaults
                    if (settings.files != null) {
                        // Ensure loaded files still exist and are valid before displaying
                        for (String filePath : settings.files) {
                            if (new File(filePath).exists()) {
                                selectedFiles.add(filePath);
                            }
                        }
                    }
                    if (settings.last_dir != null) {
                        lastDirectory = settings.last_dir;
                    }
                    updateFileListbox();
                }
            } catch (JsonSyntaxException e) {
                JOptionPane.showMessageDialog(this,
                        String.format("Could not read settings file '%s'. It might be corrupted or not valid JSON.\nError: %s", settingsFile, e.getMessage()),
                        "Error", JOptionPane.ERROR_MESSAGE);
                // Reset to default if settings file is corrupted
                selectedFiles.clear();
                lastDirectory = System.getProperty("user.dir");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        String.format("An I/O error occurred while loading settings from '%s': %s", settingsFile, e.getMessage()),
                        "Error", JOptionPane.ERROR_MESSAGE);
                // Reset to default on I/O error
                selectedFiles.clear();
                lastDirectory = System.getProperty("user.dir");
            }
        }
        // If settings file doesn't exist, initial values (empty list, current dir) are used.
    }

    private void saveSettings() {
        AppSettings settings = new AppSettings();
        settings.files = new ArrayList<>(selectedFiles); // Create a copy to store
        settings.last_dir = lastDirectory;

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(settingsFile), StandardCharsets.UTF_8)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create(); // indent for pretty printing
            gson.toJson(settings, writer);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    String.format("Could not save settings to '%s': %s", settingsFile, e.getMessage()),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onClosing() {
        saveSettings();
        dispose(); // Release resources and close the frame
    }

    public static void main(String[] args) {
        // Ensure GUI updates are done on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            Random_List_Picker app = new Random_List_Picker();
            app.setVisible(true);
        });
    }
}