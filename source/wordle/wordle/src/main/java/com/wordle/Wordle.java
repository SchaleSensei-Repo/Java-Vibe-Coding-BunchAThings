package com.wordle;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Wordle extends JFrame {

    // --- Constants for Wordle Colors ---
    private static final Color COLOR_GREEN = Color.decode("#6aaa64");
    private static final Color COLOR_YELLOW = Color.decode("#c9b458");
    private static final Color COLOR_GREY = Color.decode("#787c7e");
    private static final Color COLOR_DEFAULT_BG = Color.decode("#ffffff");
    private static final Color COLOR_TEXT = Color.WHITE; // Text color for highlighted cells
    private static final Color COLOR_CELL_BORDER = Color.decode("#d3d6da");
    private static final Color COLOR_DEFAULT_CELL_TEXT = Color.BLACK;

    // --- Settings File ---
    private static final String SETTINGS_FILE = "wordle_settings.ini";

    private Properties config;
    private String lastFilePath;
    private String lastFolderPath;

    private List<String> wordList;
    private String targetWord;
    private int targetWordLength;
    private int guessCount;
    private Set<Character> wrongLettersGuessed; // Store unique letters definitively NOT in the word

    // GUI Components
    private JLabel statusLabel;
    private JLabel wrongLettersDisplayLabel;
    private JLabel gameMessageLabel;
    private JTextField inputEntry;
    private JButton submitButton;
    private JButton hintButton;

    private JPanel guessesDisplayContainer; // Panel to hold all guess rows
    private JScrollPane scrollPane; // Scroll pane for the guesses container

    public Wordle() {
        super("PyWordle - Dynamic Wordle (Java)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle closing manually
        setSize(600, 800);
        setMinimumSize(new Dimension(400, 600));
        getContentPane().setBackground(COLOR_DEFAULT_BG);

        config = new Properties();
        wordList = new ArrayList<>();
        wrongLettersGuessed = new HashSet<>();

        loadSettings();
        createWidgets();
        initialGameStart();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClosing();
            }
        });
    }

    private void loadSettings() {
        try (InputStream input = new FileInputStream(SETTINGS_FILE)) {
            config.load(input);
            lastFilePath = config.getProperty("last_file_path", "");
            lastFolderPath = config.getProperty("last_folder_path", System.getProperty("user.dir"));
            if (!new File(lastFolderPath).isDirectory()) {
                lastFolderPath = System.getProperty("user.dir"); // Fallback if folder invalid
            }
        } catch (IOException ex) {
            // File not found or error, use defaults
            lastFilePath = "";
            lastFolderPath = System.getProperty("user.dir");
        }
    }

    private void saveSettings() {
        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            config.setProperty("last_file_path", lastFilePath);
            config.setProperty("last_folder_path", lastFolderPath);
            config.store(output, null);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving settings: " + ex.getMessage(), "Settings Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onClosing() {
        saveSettings();
        dispose();
    }

    private void createWidgets() {
        // --- Top Control Panel ---
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(COLOR_DEFAULT_BG);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(controlPanel, BorderLayout.NORTH);

        statusLabel = new JLabel("Select a word list file to start!");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(statusLabel);
        controlPanel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(COLOR_DEFAULT_BG);
        JButton fileButton = new JButton("Select Word List File");
        fileButton.addActionListener(e -> selectFile());
        buttonPanel.add(fileButton);

        hintButton = new JButton("Get Hint");
        hintButton.addActionListener(e -> provideHint());
        hintButton.setEnabled(false); // Disabled initially
        buttonPanel.add(hintButton);
        controlPanel.add(buttonPanel);
        controlPanel.add(Box.createVerticalStrut(5));

        wrongLettersDisplayLabel = new JLabel("Wrong Letters: None");
        wrongLettersDisplayLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        wrongLettersDisplayLabel.setForeground(Color.RED);
        wrongLettersDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(wrongLettersDisplayLabel);
        controlPanel.add(Box.createVerticalStrut(5));
        
        gameMessageLabel = new JLabel("");
        gameMessageLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        gameMessageLabel.setForeground(Color.BLUE);
        gameMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(gameMessageLabel);

        // --- Scrollable Area ---
        guessesDisplayContainer = new JPanel();
        guessesDisplayContainer.setBackground(COLOR_DEFAULT_BG);
        guessesDisplayContainer.setLayout(new BoxLayout(guessesDisplayContainer, BoxLayout.Y_AXIS)); // Stack guess rows vertically
        guessesDisplayContainer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Padding
        guessesDisplayContainer.setAlignmentX(Component.CENTER_ALIGNMENT); // Center the entire guess area

        // Input Panel (always at the bottom of the scrollable content)
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        inputPanel.setBackground(COLOR_DEFAULT_BG);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Padding
        inputEntry = new JTextField(15); // Initial width
        inputEntry.setFont(new Font("Arial", Font.PLAIN, 18));
        inputEntry.addActionListener(e -> handleGuess()); // Bind Enter key
        inputPanel.add(inputEntry);

        submitButton = new JButton("Submit Guess");
        submitButton.addActionListener(e -> handleGuess());
        inputPanel.add(submitButton);
        inputPanel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center the input panel

        // This wrapper panel holds both the guesses container and the input panel
        // It is important that the guesses container is added FIRST, so it's at the top of the guesses area
        // and the input panel is added SECOND, so it's at the bottom
        JPanel innerScrollContentWrapper = new JPanel();
        innerScrollContentWrapper.setLayout(new BoxLayout(innerScrollContentWrapper, BoxLayout.Y_AXIS));
        innerScrollContentWrapper.setBackground(COLOR_DEFAULT_BG);
        innerScrollContentWrapper.add(guessesDisplayContainer);
        innerScrollContentWrapper.add(inputPanel);
        
        scrollPane = new JScrollPane(innerScrollContentWrapper);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Make scrolling smoother
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // --- Reset Button Panel ---
        JPanel resetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        resetPanel.setBackground(COLOR_DEFAULT_BG);
        resetPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton resetButton = new JButton("Reset Game (Select New File)");
        resetButton.addActionListener(e -> resetGameAndSelectFile());
        resetPanel.add(resetButton);
        add(resetPanel, BorderLayout.SOUTH);

        // Initially disable input and hint button
        setInputState(false);
    }
    
    // Helper to enable/disable input and hint buttons
    private void setInputState(boolean enabled) {
        inputEntry.setEnabled(enabled);
        submitButton.setEnabled(enabled);
        hintButton.setEnabled(enabled);
    }

    private void initialGameStart() {
        if (!lastFilePath.isEmpty() && new File(lastFilePath).exists()) {
            if (loadWordList(lastFilePath)) {
                startNewRound();
                statusLabel.setText("Loaded: " + new File(lastFilePath).getName());
            } else {
                statusLabel.setText("Last file was empty or invalid. Select a new file!");
                setInputState(false);
            }
        } else {
            statusLabel.setText("No word list loaded. Select a file to start!");
            setInputState(false);
        }
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser(lastFolderPath);
        fileChooser.setDialogTitle("Select Word List File");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text/JSON Files", "txt", "json"));

        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            lastFilePath = selectedFile.getAbsolutePath();
            lastFolderPath = selectedFile.getParent(); // Update folder for next time

            if (loadWordList(lastFilePath)) {
                startNewRound();
                statusLabel.setText("Loaded: " + selectedFile.getName());
            } else {
                statusLabel.setText("Selected file was empty or invalid. Try another!");
                setInputState(false);
            }
        }
    }

    private void resetGameAndSelectFile() {
        resetGameBoard();
        targetWord = "";
        targetWordLength = 0;
        wordList.clear();
        gameMessageLabel.setText("");
        inputEntry.setText("");
        setInputState(false);
        statusLabel.setText("Game reset. Select a new word list file.");
        updateWrongLettersDisplay(); // Clear wrong letters display
        selectFile();
    }

    private boolean loadWordList(String filePath) {
        wordList.clear();
        String fileExtension = "";
        int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filePath.length() - 1) {
            fileExtension = filePath.substring(dotIndex + 1).toLowerCase();
        }

        try {
            if (fileExtension.equals("txt")) {
                try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String filteredWord = line.trim().replaceAll("[^a-zA-Z0-9 ]", "").toUpperCase();
                        if (!filteredWord.isEmpty()) {
                            wordList.add(filteredWord);
                        }
                    }
                }
            } else if (fileExtension.equals("json")) {
                StringBuilder jsonContent = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonContent.append(line);
                    }
                }
                // --- Basic JSON Array Parser ---
                // Assumes a simple array of strings like ["word1", "word2"]
                String jsonString = jsonContent.toString().trim();
                if (jsonString.startsWith("[") && jsonString.endsWith("]")) {
                    jsonString = jsonString.substring(1, jsonString.length() - 1); // Remove brackets
                    String[] words = jsonString.split(",");
                    for (String word : words) {
                        String filteredWord = word.trim().replaceAll("^\"|\"$", "") // Remove leading/trailing quotes
                                .replaceAll("[^a-zA-Z0-9 ]", "") // Filter invalid chars
                                .toUpperCase();
                        if (!filteredWord.isEmpty()) {
                            wordList.add(filteredWord);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "JSON file must contain a simple array of strings (e.g., [\"word1\", \"word2\"]).", "File Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else {
                JOptionPane.showMessageDialog(this, "Unsupported file type. Please select .txt or .json.", "File Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (wordList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "The selected file contains no valid words (after filtering out invalid characters).", "Empty List", JOptionPane.WARNING_MESSAGE);
                statusLabel.setText("No valid words in file. Select another.");
                setInputState(false);
                return false;
            } else {
                statusLabel.setText("Loaded " + wordList.size() + " valid words.");
                return true;
            }

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "File not found: " + filePath, "File Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("File not found.");
            setInputState(false);
            return false;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "An I/O error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Error loading file.");
            setInputState(false);
            return false;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Error loading file.");
            setInputState(false);
            return false;
        }
    }

    private void startNewRound() {
        if (wordList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No word list loaded. Please select a file.", "Game Error", JOptionPane.WARNING_MESSAGE);
            statusLabel.setText("No word list loaded.");
            setInputState(false);
            return;
        }

        targetWord = wordList.get(new Random().nextInt(wordList.size()));
        targetWordLength = targetWord.length();
        
        resetGameBoard(); // Clears previous guess rows and wrong letters
        guessCount = 0;
        inputEntry.setText(""); // Clear input for new round
        gameMessageLabel.setText("Guess the " + targetWordLength + "-character word!");
        setInputState(true); // Enable input and hint button
        
        // Adjust input entry width based on target word length, with a minimum
        inputEntry.setColumns(Math.max(targetWordLength + 2, 15));
        
        // Ensure scrollbar is at the bottom (showing input field)
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
            inputEntry.requestFocusInWindow(); // Focus the input field
        });
        // System.out.println("DEBUG: Target word is " + targetWord); // For testing purposes
    }

    private void resetGameBoard() {
        guessesDisplayContainer.removeAll();
        guessesDisplayContainer.revalidate();
        guessesDisplayContainer.repaint();
        guessCount = 0;
        wrongLettersGuessed.clear(); // Reset wrong letters
        updateWrongLettersDisplay(); // Update display
        // scrollPane.getVerticalScrollBar().setValue(0); // Optionally scroll to top after reset
    }

    private void handleGuess() {
        if (targetWord == null || targetWord.isEmpty()) {
            gameMessageLabel.setText("Please select a word list first!");
            gameMessageLabel.setForeground(Color.RED);
            return;
        }

        String rawGuess = inputEntry.getText().trim();
        String guess = rawGuess.replaceAll("[^a-zA-Z0-9 ]", "").toUpperCase();

        if (guess.isEmpty()) {
            gameMessageLabel.setText("Guess cannot be empty!");
            gameMessageLabel.setForeground(Color.RED);
            return;
        }
            
        if (guess.length() != targetWordLength) {
            gameMessageLabel.setText("Guess must be " + targetWordLength + " characters long!");
            gameMessageLabel.setForeground(Color.RED);
            return;
        }

        // --- Validation for already wrong letters ---
        Set<Character> offendingChars = new HashSet<>();
        for (char c : guess.toCharArray()) {
            if (c != ' ' && wrongLettersGuessed.contains(c)) {
                offendingChars.add(c);
            }
        }
        
        if (!offendingChars.isEmpty()) {
            List<Character> sortedOffending = new ArrayList<>(offendingChars);
            Collections.sort(sortedOffending);
            gameMessageLabel.setText("Cannot use definitively wrong letters: " + sortedOffending.toString().replaceAll("[\\[\\]]", ""));
            gameMessageLabel.setForeground(Color.ORANGE);
            return;
        }
        // --- END NEW VALIDATION ---
            
        gameMessageLabel.setText(""); // Clear previous message

        displayGuess(guess); // This creates and displays the new guess row
        guessCount++;

        if (guess.equals(targetWord)) {
            gameMessageLabel.setText("Congratulations! You guessed the word: '" + targetWord + "'!");
            gameMessageLabel.setForeground(COLOR_GREEN);
            JOptionPane.showMessageDialog(this, "You won! The word was '" + targetWord + "'.\nStarting a new round...", "PyWordle", JOptionPane.INFORMATION_MESSAGE);
            setInputState(false); // Temporarily disable input and hint
            Timer timer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    startNewRound();
                    ((Timer) e.getSource()).stop(); // Stop the timer after execution
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
        
        // Scroll to bottom after each guess to keep input visible
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }

    private void displayGuess(String guess) {
        JPanel guessRowFrame = new JPanel();
        guessRowFrame.setBackground(COLOR_DEFAULT_BG);
        guessRowFrame.setLayout(new GridBagLayout()); // Use GridBagLayout for flexible cells within the row
        guessRowFrame.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0)); // Vertical padding for rows
        guessRowFrame.setAlignmentX(Component.CENTER_ALIGNMENT); // Center the row horizontally

        // Pre-calculate character counts in target word for accurate yellow/grey logic
        java.util.Map<Character, Integer> targetCharCounts = new java.util.HashMap<>();
        for (char c : targetWord.toCharArray()) {
            targetCharCounts.put(c, targetCharCounts.getOrDefault(c, 0) + 1);
        }

        Color[] charColors = new Color[targetWordLength];
        // Initialize all to GREY first
        for (int i = 0; i < targetWordLength; i++) {
            charColors[i] = COLOR_GREY;
        }

        // Create a mutable copy of target word's character counts to track used characters
        java.util.Map<Character, Integer> remainingTargetChars = new java.util.HashMap<>(targetCharCounts);
        
        // First pass: Check for green (correct letter, correct position)
        for (int i = 0; i < targetWordLength; i++) {
            if (i < guess.length() && guess.charAt(i) == targetWord.charAt(i)) {
                charColors[i] = COLOR_GREEN;
                remainingTargetChars.put(guess.charAt(i), remainingTargetChars.get(guess.charAt(i)) - 1);
            }
        }

        // Second pass: Check for yellow (correct letter, wrong position)
        for (int i = 0; i < targetWordLength; i++) {
            if (i < guess.length() && charColors[i] == COLOR_GREY) { // Only process if not already green
                char currentChar = guess.charAt(i);
                if (remainingTargetChars.getOrDefault(currentChar, 0) > 0) {
                    charColors[i] = COLOR_YELLOW;
                    remainingTargetChars.put(currentChar, remainingTargetChars.get(currentChar) - 1);
                }
            }
        }
            
        // --- Update wrong_letters_guessed set based on colored cells ---
        // Collect all characters that were NOT grey (i.e., green or yellow) in this guess.
        Set<Character> matchedInThisGuessChars = new HashSet<>();
        for (int i = 0; i < guess.length(); i++) {
            if (charColors[i] == COLOR_GREEN || charColors[i] == COLOR_YELLOW) {
                matchedInThisGuessChars.add(guess.charAt(i));
            }
        }

        // Now, for every character in the guess that turned grey:
        // If that character was NOT matched as green or yellow anywhere else in THIS SAME GUESS,
        // then it's a definitively wrong letter (not in the target word).
        for (int i = 0; i < guess.length(); i++) {
            char charInGuess = guess.charAt(i);
            if (charColors[i] == COLOR_GREY) {
                // Only add to wrong_letters_guessed if this char was not found as green/yellow
                // anywhere else in this guess. Also, ignore spaces for this check.
                if (charInGuess != ' ' && !matchedInThisGuessChars.contains(charInGuess)) {
                    wrongLettersGuessed.add(charInGuess);
                }
            }
        }
        // --- END WRONG LETTERS LOGIC ---


        // Populate labels within the newly created guess_row_frame
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3); // Padding around cells
        gbc.weightx = 1.0; // Make columns expand evenly
        gbc.fill = GridBagConstraints.BOTH; // Fill cell space

        for (int i = 0; i < targetWordLength; i++) {
            String charToDisplay = (i < guess.length()) ? String.valueOf(guess.charAt(i)) : "";
            JLabel label = new JLabel(charToDisplay, SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 20));
            label.setBackground(charColors[i]);
            label.setForeground(charColors[i] == COLOR_GREY ? COLOR_DEFAULT_CELL_TEXT : COLOR_TEXT); // Text color changes for grey cells
            label.setOpaque(true); // Make background color visible
            label.setBorder(BorderFactory.createLineBorder(charColors[i], 1)); // Border color matches background

            gbc.gridx = i;
            gbc.gridy = 0;
            guessRowFrame.add(label, gbc);
        }
        
        // --- FIX: Add the new guess row at the END of the guessesDisplayContainer ---
        // This makes new guesses appear below old ones (stacking downwards)
        guessesDisplayContainer.add(guessRowFrame); 
        guessesDisplayContainer.revalidate();
        guessesDisplayContainer.repaint();

        updateWrongLettersDisplay();
    }

    private void updateWrongLettersDisplay() {
        if (!wrongLettersGuessed.isEmpty()) {
            List<Character> sortedWrongLetters = new ArrayList<>(wrongLettersGuessed);
            Collections.sort(sortedWrongLetters);
            StringBuilder sb = new StringBuilder("Wrong Letters: ");
            for (int i = 0; i < sortedWrongLetters.size(); i++) {
                sb.append(sortedWrongLetters.get(i));
                if (i < sortedWrongLetters.size() - 1) {
                    sb.append(", ");
                }
            }
            wrongLettersDisplayLabel.setText(sb.toString());
        } else {
            wrongLettersDisplayLabel.setText("Wrong Letters: None");
        }
    }

    private void provideHint() {
        if (targetWord == null || targetWord.isEmpty()) {
            gameMessageLabel.setText("Load a word list to get a hint!");
            gameMessageLabel.setForeground(Color.RED);
            return;
        }

        List<String> possibleHints = new ArrayList<>();
        for (String word : wordList) {
            if (word.length() == targetWordLength && !word.equals(targetWord)) {
                // Check if this word contains any of the definitively wrong letters
                boolean containsWrongLetter = false;
                for (char c : word.toCharArray()) {
                    if (c != ' ' && wrongLettersGuessed.contains(c)) {
                        containsWrongLetter = true;
                        break;
                    }
                }
                
                if (!containsWrongLetter) {
                    possibleHints.add(word);
                }
            }
        }
        
        if (possibleHints.isEmpty()) {
            gameMessageLabel.setText("Hint not available. No other suitable words found.");
            gameMessageLabel.setForeground(Color.ORANGE);
        } else {
            String hintWord = possibleHints.get(new Random().nextInt(possibleHints.size()));
            gameMessageLabel.setText("Hint: Try '" + hintWord + "'");
            gameMessageLabel.setForeground(new Color(128, 0, 128)); // Purple
        }
    }

    public static void main(String[] args) {
        // Ensure GUI updates are done on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            Wordle app = new Wordle();
            app.setVisible(true);
        });
    }
}