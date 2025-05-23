package com.hangman;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

// Import for JSON parsing - REQUIRES 'org.json' LIBRARY
// See "Important Note on JSON Parsing" above for how to include this dependency.
import org.json.JSONArray;
import org.json.JSONException;


public class HangmanGUI extends JFrame {

    // --- Constants ---
    private static final String SETTINGS_FILE = "hangman_settings.ini";

    // --- GUI Components ---
    private JLabel filePathLabel;
    private JLabel wordDisplayLabel;
    private JLabel incorrectGuessesLabel;
    private JLabel correctlyGuessedLabel;
    private JLabel messageLabel;
    private JTextField guessEntry;
    private JButton chooseFileButton;
    private JButton guessButton;
    private JButton newRoundButton;
    private JButton quitButton;

    // --- Game State Variables ---
    private Properties settings; // Stores last used file path and folder
    private List<String> wordList; // The currently loaded list of words/phrases
    private String currentFilePath; // Path of the currently loaded word list file
    private String currentWord; // The word/phrase for the current round
    private Set<Character> guessedChars; // Characters correctly guessed (lowercase)
    private Set<Character> incorrectGuesses; // Characters guessed incorrectly (lowercase)
    private Set<Character> guessableCharsTarget; // All unique letters/numbers/spaces in currentWord (lowercase)
    private Random random;

    /**
     * Constructor for the HangmanGUI. Sets up the main window and initializes game components.
     */
    public HangmanGUI() {
        super("Hangman Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400); // Set fixed window size
        setResizable(false); // Disable resizing
        setLocationRelativeTo(null); // Center window on screen

        random = new Random();
        guessedChars = new HashSet<>();
        incorrectGuesses = new HashSet<>();
        guessableCharsTarget = new HashSet<>();

        loadSettings(); // Load settings (including last file path) at startup
        createWidgets(); // Create and arrange all GUI components
        initializeGame(); // Start the game logic (auto-load or prompt)
    }

    /**
     * Loads settings from the hangman_settings.ini file.
     */
    private void loadSettings() {
        settings = new Properties();
        try (InputStream input = new FileInputStream(SETTINGS_FILE)) {
            settings.load(input);
        } catch (IOException ex) {
            // File not found or other IO error, will use default empty settings
            System.out.println("Settings file not found or could not be read. Starting with default.");
        }
    }

    /**
     * Saves current settings to the hangman_settings.ini file.
     */
    private void saveSettings() {
        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            settings.store(output, "Hangman Game Settings");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving settings: " + ex.getMessage(), "Settings Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates and arranges all GUI components (labels, buttons, text fields).
     */
    private void createWidgets() {
        setLayout(new BorderLayout()); // Use BorderLayout for the main frame

        // --- Top Panel for File Controls ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filePathLabel = new JLabel("No file selected.");
        filePathLabel.setPreferredSize(new Dimension(400, 20)); // Give it some preferred width
        controlPanel.add(filePathLabel);

        chooseFileButton = new JButton("Choose Word List");
        chooseFileButton.addActionListener(e -> chooseWordList()); // Lambda for event listener
        controlPanel.add(chooseFileButton);
        add(controlPanel, BorderLayout.NORTH);

        // --- Center Panel for Game Display ---
        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS)); // Vertical box layout
        gamePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding
        
        wordDisplayLabel = new JLabel("_ _ _ _ _");
        wordDisplayLabel.setFont(new Font("Helvetica", Font.BOLD, 28));
        wordDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center horizontally
        gamePanel.add(wordDisplayLabel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 10))); // Vertical spacer

        incorrectGuessesLabel = new JLabel("Incorrect Guesses: None");
        incorrectGuessesLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));
        incorrectGuessesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gamePanel.add(incorrectGuessesLabel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer

        correctlyGuessedLabel = new JLabel("Correctly Guessed: None");
        correctlyGuessedLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));
        correctlyGuessedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gamePanel.add(correctlyGuessedLabel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer

        messageLabel = new JLabel("Welcome! Choose a word list to begin.");
        messageLabel.setFont(new Font("Helvetica", Font.PLAIN, 14));
        messageLabel.setForeground(Color.BLUE); // Default message color
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gamePanel.add(messageLabel);
        
        add(gamePanel, BorderLayout.CENTER);


        // --- Bottom Panel for Input and Actions ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Guess Input Sub-Panel (centered horizontally)
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputPanel.add(new JLabel("Your Guess:"));
        guessEntry = new JTextField(1); // Visual width for 1 character
        guessEntry.setFont(new Font("Helvetica", Font.PLAIN, 16));
        // DocumentListener to enforce single character input programmatically
        guessEntry.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                // If text length exceeds 1, truncate it to the first character
                if (guessEntry.getText().length() > 1) {
                    SwingUtilities.invokeLater(() -> { // Schedule on EDT to avoid concurrent modification issues
                        String text = guessEntry.getText();
                        guessEntry.setText(String.valueOf(text.charAt(0))); 
                    });
                }
            }
            // These methods are required by the interface but not used for this logic
            @Override public void removeUpdate(DocumentEvent e) {}
            @Override public void changedUpdate(DocumentEvent e) {}
        });
        guessEntry.addActionListener(e -> processGuess()); // Bind Enter key to process guess
        inputPanel.add(guessEntry);

        guessButton = new JButton("Guess");
        guessButton.addActionListener(e -> processGuess());
        inputPanel.add(guessButton);
        bottomPanel.add(inputPanel, BorderLayout.NORTH);

        // Action Buttons Sub-Panel (centered horizontally)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        newRoundButton = new JButton("New Round");
        newRoundButton.addActionListener(e -> startNewRound());
        actionPanel.add(newRoundButton);

        quitButton = new JButton("Quit");
        quitButton.addActionListener(e -> System.exit(0)); // Exit the application
        actionPanel.add(quitButton);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Initializes the game state at startup. Attempts to auto-load the last used file.
     */
    private void initializeGame() {
        String lastFilePath = settings.getProperty("last_file_path", ""); // Get last path from settings
        messageLabel.setForeground(Color.BLUE); // Set default color

        // Attempt to auto-load the last used file if it exists
        if (!lastFilePath.isEmpty() && new File(lastFilePath).exists()) {
            loadAndSetWordList(lastFilePath); // Try to load it
            if (wordList != null && !wordList.isEmpty()) { // Check if loading was successful
                messageLabel.setText("Last word list loaded automatically. Starting new round!");
                // Color is already BLUE from default
            } else { // Loading failed (e.g., file corrupted, empty)
                filePathLabel.setText("Last file could not be loaded. Please choose a new one.");
                messageLabel.setText("Failed to load last word list. Please choose a new one."); // Set text first
                messageLabel.setForeground(Color.ORANGE); // Then set color
            }
        } else { // No last file path or file doesn't exist
            filePathLabel.setText("No previous file found. Please choose a word list.");
            messageLabel.setText("Please choose a word list."); // Set text first
            // Color is already BLUE from default
        }
        updateGameControlStates(false); // Set initial button states (game not over yet)
    }

    /**
     * Opens a file dialog for the user to select a word list file (.txt or .json).
     */
    private void chooseWordList() {
        JFileChooser fileChooser = new JFileChooser();
        String lastFolderPath = settings.getProperty("last_folder_path", System.getProperty("user.dir")); // Get last folder or current dir
        if (new File(lastFolderPath).isDirectory()) {
            fileChooser.setCurrentDirectory(new File(lastFolderPath)); // Set initial directory
        }

        // Add file filters
        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text files (*.txt)", "txt");
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("JSON files (*.json)", "json");
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.addChoosableFileFilter(jsonFilter);
        fileChooser.setFileFilter(txtFilter); // Default selected filter

        int result = fileChooser.showOpenDialog(this); // Show the dialog

        if (result == JFileChooser.APPROVE_OPTION) { // If user selected a file
            String selectedFilePath = fileChooser.getSelectedFile().getAbsolutePath();
            loadAndSetWordList(selectedFilePath);
        } else { // If user cancelled the dialog
            if (wordList == null || wordList.isEmpty()) { // Only show message if no list is currently loaded
                messageLabel.setText("File selection cancelled. Please choose a word list."); // Set text first
                messageLabel.setForeground(Color.ORANGE); // Then set color
            }
        }
    }

    /**
     * Loads words from the given file path and sets it as the active word list for the game.
     * Handles .txt and .json formats.
     * @param filePath The path to the word list file.
     */
    private void loadAndSetWordList(String filePath) {
        List<String> loadedWords = null;
        File file = new File(filePath);
        String fileName = file.getName();
        String fileExtension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            fileExtension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        try {
            if (fileExtension.equals("txt")) {
                loadedWords = loadWordsFromTxt(file);
            } else if (fileExtension.equals("json")) {
                loadedWords = loadWordsFromJson(file); // This requires org.json
            } else {
                JOptionPane.showMessageDialog(this, "Unsupported file extension. Only .txt and .json are supported.", "File Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (IOException | JSONException ex) { // Catch JSONException specifically for JSON parsing errors
            JOptionPane.showMessageDialog(this, "Error reading file " + fileName + ": " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (loadedWords != null && !loadedWords.isEmpty()) {
            wordList = loadedWords;
            currentFilePath = filePath;
            settings.setProperty("last_file_path", filePath); // Save the last used file path
            settings.setProperty("last_folder_path", file.getParent()); // Save the folder path
            saveSettings(); // Persist settings
            filePathLabel.setText("Loaded: " + fileName);
            startNewRound(); // Automatically start a new round with the loaded list
        } else {
            wordList = null; // Clear list if empty or failed to load
            currentFilePath = null;
            filePathLabel.setText("No file selected.");
            messageLabel.setText("Failed to load word list. Please try again."); // Set text first
            messageLabel.setForeground(Color.RED); // Then set color
        }
        updateGameControlStates(false); // Update button states after load attempt
    }

    /**
     * Reads words from a .txt file, one word/phrase per line.
     * @param file The .txt file to read.
     * @return A list of words/phrases.
     * @throws IOException If there's an error reading the file.
     */
    private List<String> loadWordsFromTxt(File file) throws IOException {
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    words.add(line);
                }
            }
        }
        if (words.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No valid words found in " + file.getName() + ".", "File Warning", JOptionPane.WARNING_MESSAGE);
        }
        return words;
    }

    /**
     * Reads words from a .json file, expecting a JSON array of strings.
     * @param file The .json file to read.
     * @return A list of words/phrases.
     * @throws IOException If there's an error reading the file.
     * @throws JSONException If there's an error parsing the JSON content.
     */
    private List<String> loadWordsFromJson(File file) throws IOException, JSONException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }

        JSONArray jsonArray = new JSONArray(content.toString()); // Requires org.json
        List<String> words = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object item = jsonArray.get(i);
            if (item instanceof String) { // Ensure the JSON elements are strings
                String word = ((String) item).trim();
                if (!word.isEmpty()) {
                    words.add(word);
                }
            }
        }
        if (words.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No valid words found in " + file.getName() + " (JSON file must contain a list of strings).", "File Warning", JOptionPane.WARNING_MESSAGE);
        }
        return words;
    }

    /**
     * Resets game state and starts a new round with a randomly selected word.
     */
    private void startNewRound() {
        if (wordList == null || wordList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please choose a word list file before starting a new round.", "No Word List", JOptionPane.WARNING_MESSAGE);
            messageLabel.setText("Please choose a word list first!"); // Set text first
            messageLabel.setForeground(Color.RED); // Then set color
            updateGameControlStates(false);
            return;
        }

        currentWord = wordList.get(random.nextInt(wordList.size())); // Pick a random word
        guessedChars.clear();
        incorrectGuesses.clear();
        guessableCharsTarget.clear();

        // Populate guessableCharsTarget with unique letters, numbers, and spaces from the word
        for (char ch : currentWord.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(ch) || ch == ' ') {
                guessableCharsTarget.add(ch);
            }
        }

        messageLabel.setText("Guess the word/phrase!");
        messageLabel.setForeground(Color.BLUE);
        guessEntry.setText(""); // Clear previous guess input
        updateDisplay(); // Update all labels to reflect new game state
        updateGameControlStates(false); // Enable input, disable 'New Round' button
        guessEntry.requestFocusInWindow(); // Set keyboard focus to the guess entry field

        // Handle edge case: if the word has no guessable characters (e.g., "---!!!")
        if (guessableCharsTarget.isEmpty()) {
            messageLabel.setText("Word: \"" + currentWord + "\". No letters/numbers/spaces to guess. Automatically won!"); // Set text first
            messageLabel.setForeground(Color.MAGENTA); // Then set color
            wordDisplayLabel.setText(currentWord); // Show full word immediately
            endGameWon(); // End the game as won
        }
    }

    /**
     * Generates the string to display the current state of the word.
     * Hidden guessable characters are shown as '_', others are revealed.
     * @return The formatted word display string.
     */
    private String displayWordState() {
        StringBuilder display = new StringBuilder();
        for (char ch : currentWord.toCharArray()) {
            if (Character.isLetterOrDigit(ch) || ch == ' ') {
                if (!guessedChars.contains(Character.toLowerCase(ch))) {
                    display.append('_'); // Hide if not guessed
                } else {
                    display.append(ch); // Show if guessed
                }
            } else {
                display.append(ch); // Always show non-guessable characters
            }
            display.append(' '); // Add space for readability between characters/underscores
        }
        return display.toString().trim(); // Trim any trailing space
    }

    /**
     * Updates all dynamic labels on the GUI (word display, incorrect guesses, correctly guessed).
     */
    private void updateDisplay() {
        wordDisplayLabel.setText(displayWordState());
        
        String incorrectStr = "Incorrect Guesses: " + (incorrectGuesses.isEmpty() ? "None" : sortedSetToString(incorrectGuesses));
        incorrectGuessesLabel.setText(incorrectStr);
        
        String correctlyStr = "Correctly Guessed: " + (guessedChars.isEmpty() ? "None" : sortedSetToString(guessedChars));
        correctlyGuessedLabel.setText(correctlyStr);
    }

    /**
     * Converts a set of characters to a comma-separated string, sorted alphabetically.
     * @param set The set of characters.
     * @return A sorted, comma-separated string of characters.
     */
    private String sortedSetToString(Set<Character> set) {
        return set.stream() // Use Java Streams for conciseness
                .sorted() // Sort characters
                .map(String::valueOf) // Convert each character to a String
                .collect(Collectors.joining(", ")); // Join with ", "
    }

    /**
     * Processes the user's guess from the text field.
     */
    private void processGuess() {
        if (currentWord == null || currentWord.isEmpty()) {
            messageLabel.setText("Please start a new round or choose a word list."); // Set text first
            messageLabel.setForeground(Color.RED); // Then set color
            JOptionPane.showMessageDialog(this, "Please load a word list and start a new round.", "Game Not Ready", JOptionPane.WARNING_MESSAGE);
            guessEntry.setText(""); // Clear input
            return;
        }

        String guessText = guessEntry.getText().trim().toLowerCase();
        guessEntry.setText(""); // Clear the entry field after getting input

        // Input validation:
        if (guessText.isEmpty()) {
            messageLabel.setText("Please enter a guess."); // Set text first
            messageLabel.setForeground(Color.RED); // Then set color
            return;
        }
        // This check is crucial as the DocumentListener only ensures 1 char *while typing*,
        // but copy-pasting or other methods could bypass it.
        if (guessText.length() != 1) { 
            messageLabel.setText("Please enter only one character."); // Set text first
            messageLabel.setForeground(Color.RED); // Then set color
            return;
        }

        char guess = guessText.charAt(0);

        // Validate that the input is a letter, number, or space
        if (!(Character.isLetterOrDigit(guess) || guess == ' ')) {
            messageLabel.setText("Invalid input. Please enter a letter, number, or space."); // Set text first
            messageLabel.setForeground(Color.RED); // Then set color
            return;
        }

        // Check if the character has already been guessed (correctly or incorrectly)
        if (guessedChars.contains(guess) || incorrectGuesses.contains(guess)) {
            messageLabel.setText("You already guessed '" + guess + "'. Try a different one."); // Set text first
            messageLabel.setForeground(Color.ORANGE); // Then set color
            return;
        }

        // Process the guess based on whether it's in the target word
        if (guessableCharsTarget.contains(guess)) {
            messageLabel.setText("Good guess! '" + guess + "' is in the word."); // Set text first
            messageLabel.setForeground(Color.GREEN); // Then set color
            guessedChars.add(guess);
        } else {
            messageLabel.setText("Sorry, '" + guess + "' is not in the word."); // Set text first
            messageLabel.setForeground(Color.RED); // Then set color
            incorrectGuesses.add(guess);
        }

        updateDisplay(); // Refresh GUI display
        checkWinCondition(); // Check if the game is won
        guessEntry.requestFocusInWindow(); // Keep keyboard focus on the guess entry field
    }

    /**
     * Checks if the current game round has been won.
     */
    private void checkWinCondition() {
        boolean allGuessed = true;
        // Iterate through all required guessable characters
        for (char ch : guessableCharsTarget) {
            if (!guessedChars.contains(ch)) {
                allGuessed = false;
                break; // If any required char isn't guessed, not won
            }
        }

        // Win condition is met if all guessable chars are found AND there were guessable chars to begin with
        if (allGuessed && !guessableCharsTarget.isEmpty()) { 
            endGameWon();
        } 
        // Special case: If the word has no guessable characters (e.g., "---!!!"), it's an instant win.
        else if (guessableCharsTarget.isEmpty() && currentWord != null) { 
            endGameWon(); 
        }
    }

    /**
     * Actions to take when the game is won (display message, update buttons).
     */
    private void endGameWon() {
        messageLabel.setText("CONGRATULATIONS! You've guessed the word!"); // Set text first
        messageLabel.setForeground(Color.MAGENTA); // Then set color
        wordDisplayLabel.setText(currentWord); // Ensure the full word is visible
        updateGameControlStates(true); // Disable guess input, enable 'New Round' button
    }

    /**
     * Manages the enabled/disabled state of game control buttons and input field.
     * @param isGameOver True if the current round has ended (won), false otherwise.
     */
    private void updateGameControlStates(boolean isGameOver) {
        boolean wordListLoaded = (wordList != null && !wordList.isEmpty());

        if (wordListLoaded && !isGameOver) { // Game is active and not won yet
            guessEntry.setEnabled(true);
            guessButton.setEnabled(true);
            newRoundButton.setEnabled(false); // Cannot start new round mid-game
        } else if (wordListLoaded && isGameOver) { // Game over (won) and a list is loaded
            guessEntry.setEnabled(false);
            guessButton.setEnabled(false);
            newRoundButton.setEnabled(true); // Enable "New Round" button
        } else { // No word list loaded or other non-playable state
            guessEntry.setEnabled(false);
            guessButton.setEnabled(false);
            newRoundButton.setEnabled(false);
        }

        // The 'Choose Word List' button is always enabled as it's the primary way to start/reset
        chooseFileButton.setEnabled(true);
    }

    /**
     * Main method to launch the Hangman GUI application.
     * Uses SwingUtilities.invokeLater to ensure GUI creation is on the Event Dispatch Thread.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HangmanGUI game = new HangmanGUI();
            game.setVisible(true); // Make the main window visible
        });
    }
}