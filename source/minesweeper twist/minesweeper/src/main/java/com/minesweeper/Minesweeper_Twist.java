package com.minesweeper;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Minesweeper_Twist extends JFrame {

    // --- Constants for colors and visual settings ---
    // Removed MAX_BOARD_DIM as per request

    private static final Color FLAG_COLOR = Color.BLUE;
    private static final Color MINE_COLOR_BG = Color.RED;
    private static final Color DEFAULT_TILE_BG = Color.LIGHT_GRAY;
    private static final Color REVEALED_TILE_BG = UIManager.getColor("Button.background"); // Default light grey for revealed, looks like a standard Swing button background

    private static final Color[] NUMBER_COLORS = {
        null, // Index 0 is not used as numbers start from 1
        Color.BLUE,      // 1
        new Color(0, 128, 0), // Green (darker) // 2
        Color.RED,       // 3
        new Color(128, 0, 128), // Purple // 4
        new Color(165, 42, 42), // Brown // 5
        new Color(0, 128, 128), // Teal // 6
        Color.BLACK,     // 7
        Color.DARK_GRAY  // 8
    };


    private MinesweeperSettings settings;
    private JLabel scoreLabel;
    private JPanel boardContainer; // The panel holding all JButtons
    private JButton[][] buttons;
    private int[][] boardData; // -1 for mine, 0-8 for adjacent mine count
    private ArrayList<Point> minesCoords; // List of (row, col) for mine locations
    private int nonMineTilesToDiscover;
    private boolean gameOverState;
    private Random random;
    private DecimalFormat scoreFormatter;
    private int score; // 'score' is correctly declared here

    private int width;
    private int height;

    public Minesweeper_Twist(String filename) {
        this.settings = new MinesweeperSettings(filename);
        this.random = new Random();
        this.scoreFormatter = new DecimalFormat("#,###"); // For thousand separators

        setTitle("Minesweeper with a Twist");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(300, 300)); // Minimum window size

        setupUI();
        newGame();
        pack(); // Adjusts window size to fit components
        setLocationRelativeTo(null); // Center the window on the screen
        setVisible(true);
    }

    private void setupUI() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        scoreLabel = new JLabel("Score: " + scoreFormatter.format(0), SwingConstants.LEFT);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(scoreLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> newGame());
        buttonPanel.add(newGameButton);

        JButton settingsButton = new JButton("Settings");
        settingsButton.addActionListener(e -> openSettings());
        buttonPanel.add(settingsButton);

        topPanel.add(buttonPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- Scrolling Panel for the board ---
        boardContainer = new JPanel(new GridBagLayout());
        boardContainer.setBackground(DEFAULT_TILE_BG); // Set background for consistency
        JScrollPane scrollPane = new JScrollPane(boardContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // No border for the scroll pane itself
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Make scrolling smoother
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void newGame() {
        gameOverState = false;
        score = settings.getSetting("game.initial_points", 100);
        updateScoreDisplay();

        // Clear existing buttons from the boardContainer
        boardContainer.removeAll();
        boardContainer.revalidate(); // Revalidate the layout
        boardContainer.repaint();   // Repaint to show changes

        buttons = null; // Clear old button references
        boardData = null; // Clear old board data
        minesCoords = new ArrayList<>();

        width = settings.getSetting("game.board_width", 10);
        height = settings.getSetting("game.board_height", 10);

        // Dynamic warning for large boards
        int totalCells = width * height;
        if (totalCells > 2500) { // More than 50x50 (2500 cells)
            JOptionPane.showMessageDialog(this,
                "Generating a very large board (" + width + "x" + height + ")... This will take a significant amount of time and memory, and the application may become unresponsive or crash. Please be patient.",
                "Generating Large Board - WARNING!",
                JOptionPane.WARNING_MESSAGE);
        } else if (totalCells > 500) { // e.g. 20x25 = 500
            JOptionPane.showMessageDialog(this,
                "Generating a large board (" + width + "x" + height + ")... This may take a few moments. The application might appear unresponsive briefly.",
                "Generating Board",
                JOptionPane.INFORMATION_MESSAGE);
        }


        boardData = new int[height][width];
        buttons = new JButton[height][width];

        // Determine button pixel size and font size based on dimensions
        int buttonPixelSize;
        Font buttonFont;
        int maxDim = Math.max(width, height);

        if (maxDim <= 12) { // e.g., 9x9 or 10x10 (classic sizes)
            buttonPixelSize = 40;
            buttonFont = new Font("Arial", Font.BOLD, 20);
        } else if (maxDim <= 18) { // e.g., 16x16
            buttonPixelSize = 35;
            buttonFont = new Font("Arial", Font.BOLD, 18);
        } else if (maxDim <= 30) { // e.g., 20x20 or 30x30
            buttonPixelSize = 30;
            buttonFont = new Font("Arial", Font.BOLD, 16);
        } else if (maxDim <= 50) { // up to 50x50
            buttonPixelSize = 25;
            buttonFont = new Font("Arial", Font.BOLD, 14);
        } else if (maxDim <= 75) { // 50x50 to 75x75 (larger)
            buttonPixelSize = 20;
            buttonFont = new Font("Arial", Font.BOLD, 12);
        } else { // Very large boards
            buttonPixelSize = 18; // Smallest practical button size
            buttonFont = new Font("Arial", Font.BOLD, 10); // Smallest readable font
        }
        
        Dimension buttonDim = new Dimension(buttonPixelSize, buttonPixelSize);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0); // No padding between grid cells to make them look uniform

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                JButton button = new JButton();
                button.setPreferredSize(buttonDim);
                button.setMinimumSize(buttonDim);
                button.setMaximumSize(buttonDim);
                button.setFont(buttonFont);
                button.setBackground(DEFAULT_TILE_BG);
                button.setFocusPainted(false); // No annoying focus border
                button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1)); // Small border for visual separation

                // Left click listener
                int row = r; // Effectively final for lambda
                int col = c; // Effectively final for lambda
                button.addActionListener(e -> onLeftClick(row, col));

                // Right click listener
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            onRightClick(row, col);
                        }
                    }
                });

                gbc.gridx = c;
                gbc.gridy = r;
                boardContainer.add(button, gbc);
                buttons[r][c] = button;
            }
        }
        
        placeMines();
        calculateAdjacentMines();
        nonMineTilesToDiscover = (width * height) - minesCoords.size();
        updateScoreDisplay();

        // Revalidate and repaint after all buttons are added
        boardContainer.revalidate();
        boardContainer.repaint();
    }

    private void placeMines() {
        String minesMode = settings.getSetting("game.mines_mode");
        int numMines;
        if (minesMode.equals("random")) {
            int minMines = settings.getSetting("game.mines_min", 10);
            int maxMines = settings.getSetting("game.mines_max", 15);
            numMines = random.nextInt(maxMines - minMines + 1) + minMines;
        } else { // exact
            numMines = settings.getSetting("game.mines_exact", 12);
        }

        int totalCells = width * height;
        if (totalCells <= 0) {
            numMines = 0; // No cells, no mines
        } else if (totalCells > 1) { // If board is not 1x1, ensure at least one non-mine tile
            numMines = Math.min(numMines, totalCells - 1);
        } else { // 1x1 board, can be a mine or not
            numMines = Math.min(numMines, totalCells);
        }
        numMines = Math.max(numMines, 0); // Ensure no negative mines

        ArrayList<Point> allCells = new ArrayList<>();
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                allCells.add(new Point(r, c));
            }
        }
        Collections.shuffle(allCells, random); // Shuffle to pick random locations

        for (int i = 0; i < numMines; i++) {
            Point p = allCells.get(i);
            boardData[p.x][p.y] = -1; // Mark as mine
            minesCoords.add(p);
        }
    }

    private void calculateAdjacentMines() {
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (boardData[r][c] == -1) { // It's a mine, skip
                    continue;
                }

                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) {
                            continue; // Skip the cell itself
                        }

                        int nr = r + dr;
                        int nc = c + dc;
                        // Check bounds and if neighbor is a mine
                        if (nr >= 0 && nr < height && nc >= 0 && nc < width) {
                            if (boardData[nr][nc] == -1) {
                                count++;
                            }
                        }
                    }
                }
                boardData[r][c] = count; // Set the count for this cell
            }
        }
    }

    private void onLeftClick(int r, int c) {
        if (gameOverState) {
            return;
        }

        JButton currentButton = buttons[r][c];
        // If button is already revealed (background changed to REVEALED_TILE_BG)
        // or if it's currently flagged (text is "F"), do nothing.
        // Also check if it's disabled, as flagged buttons are disabled.
        if (currentButton.getBackground().equals(REVEALED_TILE_BG) || (currentButton.getText().equals("F") && !currentButton.isEnabled())) {
            return;
        }
        
        // If it was flagged, remove the flag visual before revealing
        if (currentButton.getText().equals("F")) {
            currentButton.setText("");
            currentButton.setForeground(Color.BLACK); // Reset color
            currentButton.setEnabled(true); // Re-enable for reveal logic
        }

        // Apply points for click, if enabled
        if (settings.getSetting("scoring.points_per_click_enabled", false)) {
            applyPoints("scoring.points_per_click", "click");
        }

        if (boardData[r][c] == -1) { // Clicked a mine
            revealMine(r, c);
        } else { // Clicked a safe tile
            revealCell(r, c);
            checkGameOver();
        }
        updateScoreDisplay();
    }

    private void onRightClick(int r, int c) {
        if (gameOverState) {
            return;
        }

        JButton currentButton = buttons[r][c];
        if (currentButton.getBackground().equals(REVEALED_TILE_BG)) { // Already revealed
            return;
        }
        
        // Apply points for flag placement
        applyPoints("scoring.flag_placement", "flag");

        // Toggle flag state
        if (currentButton.getText().equals("F")) {
            currentButton.setText("");
            currentButton.setForeground(Color.BLACK); // Reset color
            currentButton.setEnabled(true); // Enable for left-click
            currentButton.setBackground(DEFAULT_TILE_BG); // Reset background
        } else {
            currentButton.setText("F");
            currentButton.setForeground(FLAG_COLOR);
            currentButton.setEnabled(false); // Disable for left-click while flagged
            currentButton.setBackground(DEFAULT_TILE_BG); // Keep default background
        }
        updateScoreDisplay();
    }

    private void revealMine(int r, int c) {
        JButton button = buttons[r][c];
        button.setText("M");
        button.setBackground(MINE_COLOR_BG);
        button.setForeground(Color.WHITE);
        button.setEnabled(false); // Disable button
        applyPoints("scoring.mine_discovery", "mine");
    }

    private void revealCell(int r, int c) {
        Queue<Point> cellsToProcess = new LinkedList<>();
        HashSet<Point> newlyRevealedSafeCells = new HashSet<>();

        cellsToProcess.add(new Point(r, c));

        while (!cellsToProcess.isEmpty()) {
            Point current = cellsToProcess.poll();
            int currR = current.x;
            int currC = current.y;

            // Basic bounds check and ensure we haven't processed this exact cell in this cascade
            if (currR < 0 || currR >= height || currC < 0 || currC >= width || newlyRevealedSafeCells.contains(current)) {
                continue;
            }

            JButton currentButton = buttons[currR][currC];
            
            // Skip if already revealed (by checking background color or enabled state) or is a mine
            if (currentButton.getBackground().equals(REVEALED_TILE_BG) || boardData[currR][currC] == -1) {
                continue;
            }

            // If it was flagged, remove the flag visual
            if (currentButton.getText().equals("F")) {
                currentButton.setText("");
                currentButton.setForeground(Color.BLACK); // Reset color
            }

            // Mark as revealed and disable further clicks on this button
            currentButton.setBackground(REVEALED_TILE_BG);
            currentButton.setEnabled(false); // Disable button clicks

            // Add to the set of newly revealed safe cells for point calculation
            newlyRevealedSafeCells.add(current);
            nonMineTilesToDiscover--; // Decrement counter for game over

            int adjacentMines = boardData[currR][currC];
            if (adjacentMines > 0) { // Number cell
                currentButton.setText(String.valueOf(adjacentMines));
                if (adjacentMines >= 1 && adjacentMines < NUMBER_COLORS.length) {
                     currentButton.setForeground(NUMBER_COLORS[adjacentMines]);
                } else {
                    currentButton.setForeground(Color.BLACK); // Fallback for unexpected number
                }
            } else { // Empty cell (0 adjacent mines), continue cascade
                currentButton.setText(""); // Keep empty
                // Add all neighbors to the queue for potential revelation
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) {
                            continue;
                        }
                        int nr = currR + dr;
                        int nc = currC + dc;
                        // Only add to queue if within bounds and not already revealed/flagged/mine
                        if (nr >= 0 && nr < height && nc >= 0 && nc < width &&
                            buttons[nr][nc].getBackground().equals(DEFAULT_TILE_BG) && // Check if still unrevealed
                            !buttons[nr][nc].getText().equals("F") && // Not flagged
                            boardData[nr][nc] != -1) // Not a mine
                        {
                            cellsToProcess.add(new Point(nr, nc));
                        }
                    }
                }
            }
        }

        // Apply points for each newly discovered non-mine tile in this cascade
        if (settings.getSetting("scoring.points_per_discovered_tile_enabled", false)) {
            for (Point p : newlyRevealedSafeCells) { // Iterate over the actual distinct cells revealed
                applyPoints("scoring.points_per_discovered_tile", "discovered_tile");
            }
        }
    }

    private void applyPoints(String keyPrefix, String actionType) {
        String mode = settings.getSetting(keyPrefix + "_mode");
        int points = 0;
        if (mode.equals("random")) {
            int minP = settings.getSetting(keyPrefix + "_min", 0);
            int maxP = settings.getSetting(keyPrefix + "_max", 0);
            points = random.nextInt(maxP - minP + 1) + minP;
        } else { // exact
            points = settings.getSetting(keyPrefix + "_exact", 0);
        }

        score += points;
        // System.out.println("DEBUG: Applied " + points + " points for " + actionType + ". Current score: " + score); // Uncomment for debugging score flow
    }

    private void updateScoreDisplay() {
        scoreLabel.setText("Score: " + scoreFormatter.format(score));
    }

    private void checkGameOver() {
        if (nonMineTilesToDiscover <= 0) { // Use <= 0 to handle potential edge cases if mines count leads to 0
            gameOverState = true;
            JOptionPane.showMessageDialog(this, "Congratulations! You've discovered all non-mine tiles!\nFinal Score: " + scoreFormatter.format(score), "Game Over!", JOptionPane.INFORMATION_MESSAGE);
            disableAllButtons();
        }
    }

    private void disableAllButtons() {
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                // Only disable if not already revealed to preserve visual state
                if (buttons[r][c].getBackground().equals(DEFAULT_TILE_BG)) {
                    buttons[r][c].setEnabled(false);
                }
            }
        }
    }

    private void openSettings() {
        // No longer passing MAX_BOARD_DIM
        SettingsWindow settingsWindow = new SettingsWindow(this, settings, this::newGame);
        settingsWindow.setVisible(true); // Show the modal dialog
    }

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new Minesweeper_Twist("minesweeper_twist_settings.ini");
        });
    }
}