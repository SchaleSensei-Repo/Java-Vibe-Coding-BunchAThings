package com.battleship;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder; // New import for LineBorder
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class BattleshipGameGUI extends JFrame {

    private GameSettings gameSettings;
    private Player player1;
    private Player player2;
    private Player currentPlayer;
    private Map<Player, Player> opponentMap;
    private int currentPlayerIdx;
    private int currentShotsTaken;
    private int maxShotsThisTurn;
    private boolean gameOver;
    private Point selectedTargetCoords;

    // GUI Components
    private JLabel currentPlayerLabel;
    private JLabel messageLabel;
    
    public JPanel ownBoardFrame; 
    public JPanel targetBoardFrame;
    
    public Map<Point, JButton> ownBoardButtons; 
    public Map<Point, JButton> targetBoardButtons;
    
    private JLabel coordDisplayLabel;
    private JButton fireButton;

    // Statistics Labels
    private JLabel p1UnsunkShipsLabel;
    private JLabel p1ShipsListLabel;
    private JLabel p1ShotsLeftLabel;
    private JLabel p1HitsLabel;
    private JLabel p1MissesLabel;

    private JLabel p2UnsunkShipsLabel;
    private JLabel p2ShipsListLabel;
    private JLabel p2ShotsLeftLabel;
    private JLabel p2HitsLabel;
    private JLabel p2MissesLabel;


    public BattleshipGameGUI() {
        super("Battleship");
        gameSettings = new GameSettings(); // Load/create settings on startup
        ownBoardButtons = new HashMap<>();
        targetBoardButtons = new HashMap<>();
        opponentMap = new HashMap<>();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // Gap between components

        setupGUI();
        startNewGame(); // Initial game start
    }

    private void setupGUI() {
        // --- Top Info Panel ---
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new EmptyBorder(10, 10, 0, 10)); // Top padding

        currentPlayerLabel = new JLabel("", SwingConstants.CENTER);
        currentPlayerLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Smaller font
        currentPlayerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(currentPlayerLabel);

        messageLabel = new JLabel("Welcome to Battleship!", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 12)); // Smaller font
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(messageLabel);

        add(infoPanel, BorderLayout.NORTH);

        // --- Boards Container ---
        JPanel boardsContainer = new JPanel(new GridLayout(1, 2, 10, 0)); // 1 row, 2 cols, 10px horizontal gap
        boardsContainer.setBorder(new EmptyBorder(0, 10, 0, 10)); // Left/right padding

        // Your Own Board
        ownBoardFrame = new JPanel(new BorderLayout()); // Initialize instance variable
        ownBoardFrame.setBorder(BorderFactory.createTitledBorder("Your Ships"));
        createBoardGrid(ownBoardFrame, ownBoardButtons, false); // Not a target board
        boardsContainer.add(ownBoardFrame);

        // Opponent's Target Board
        targetBoardFrame = new JPanel(new BorderLayout()); // Initialize instance variable
        targetBoardFrame.setBorder(BorderFactory.createTitledBorder("Opponent's Waters (Click to Target)"));
        createBoardGrid(targetBoardFrame, targetBoardButtons, true); // Is a target board
        boardsContainer.add(targetBoardFrame);

        add(boardsContainer, BorderLayout.CENTER);


        // --- Input and Fire Panel ---
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        inputPanel.add(new JLabel("Target:"));
        coordDisplayLabel = new JLabel("--");
        coordDisplayLabel.setFont(new Font("Arial", Font.BOLD, 10)); // Smaller font
        inputPanel.add(coordDisplayLabel);

        fireButton = new JButton("Fire!");
        fireButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processShot();
            }
        });
        fireButton.setEnabled(false); // Disabled until target selected
        inputPanel.add(fireButton);


        // --- Statistics Panel ---
        JPanel statsFrame = new JPanel(new GridLayout(1, 2, 20, 0)); // 1 row, 2 cols, 20px horizontal gap
        statsFrame.setBorder(BorderFactory.createTitledBorder("Game Statistics"));
        statsFrame.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Player 1 Stats Panel
        JPanel p1StatsPanel = new JPanel();
        p1StatsPanel.setLayout(new BoxLayout(p1StatsPanel, BoxLayout.Y_AXIS));
        p1StatsPanel.add(new JLabel("Player 1", SwingConstants.LEFT)).setFont(new Font("Arial", Font.BOLD, 11)); // Smaller font
        p1UnsunkShipsLabel = new JLabel("Unsunk Ships: --"); p1UnsunkShipsLabel.setFont(new Font("Arial", Font.PLAIN, 10)); p1StatsPanel.add(p1UnsunkShipsLabel);
        p1ShipsListLabel = new JLabel("Types Left: --"); p1ShipsListLabel.setFont(new Font("Arial", Font.PLAIN, 10)); p1StatsPanel.add(p1ShipsListLabel);
        p1ShotsLeftLabel = new JLabel("Shots Left This Turn: --"); p1ShotsLeftLabel.setFont(new Font("Arial", Font.PLAIN, 10)); p1StatsPanel.add(p1ShotsLeftLabel);
        p1HitsLabel = new JLabel("Total Hits: --"); p1HitsLabel.setFont(new Font("Arial", Font.PLAIN, 10)); p1StatsPanel.add(p1HitsLabel);
        p1MissesLabel = new JLabel("Total Misses: --"); p1MissesLabel.setFont(new Font("Arial", Font.PLAIN, 10)); p1StatsPanel.add(p1MissesLabel);
        statsFrame.add(p1StatsPanel);

        // Player 2 Stats Panel
        JPanel p2StatsPanel = new JPanel();
        p2StatsPanel.setLayout(new BoxLayout(p2StatsPanel, BoxLayout.Y_AXIS));
        p2StatsPanel.add(new JLabel("Player 2", SwingConstants.LEFT)).setFont(new Font("Arial", Font.BOLD, 11)); // Smaller font
        p2UnsunkShipsLabel = new JLabel("Unsunk Ships: --"); p2UnsunkShipsLabel.setFont(new Font("Arial", Font.PLAIN, 10)); p2StatsPanel.add(p2UnsunkShipsLabel);
        p2ShipsListLabel = new JLabel("Types Left: --"); p2ShipsListLabel.setFont(new Font("Arial", Font.PLAIN, 10)); p2StatsPanel.add(p2ShipsListLabel);
        p2ShotsLeftLabel = new JLabel("Shots Left This Turn: --"); p2ShotsLeftLabel.setFont(new Font("Arial", Font.PLAIN, 10)); p2StatsPanel.add(p2ShotsLeftLabel);
        p2HitsLabel = new JLabel("Total Hits: --"); p2HitsLabel.setFont(new Font("Arial", Font.PLAIN, 10)); p2StatsPanel.add(p2HitsLabel);
        p2MissesLabel = new JLabel("Total Misses: --"); p2MissesLabel.setFont(new Font("Arial", Font.PLAIN, 10)); p2StatsPanel.add(p2MissesLabel);
        statsFrame.add(p2StatsPanel);


        // --- Control Buttons Panel ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10)); 
        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> startNewGame());
        controlPanel.add(newGameButton);

        JButton settingsButton = new JButton("Settings");
        settingsButton.addActionListener(e -> openSettings());
        controlPanel.add(settingsButton);

        JButton quitButton = new JButton("Quit");
        quitButton.addActionListener(e -> System.exit(0));
        controlPanel.add(quitButton);

        // Create a single bottom panel to hold stats, input, and control buttons
        JPanel overallBottomPanel = new JPanel();
        overallBottomPanel.setLayout(new BoxLayout(overallBottomPanel, BoxLayout.Y_AXIS));
        overallBottomPanel.add(statsFrame);
        overallBottomPanel.add(inputPanel);
        overallBottomPanel.add(controlPanel);

        add(overallBottomPanel, BorderLayout.SOUTH); // Add the combined panel to SOUTH

        pack(); // Adjusts window size to fit components
        setLocationRelativeTo(null); // Center the window on the screen
        setVisible(true);
    }

    public void createBoardGrid(JPanel parentFrame, Map<Point, JButton> buttonMap, boolean isTargetBoard) {
        // Clear existing components
        for (Component comp : parentFrame.getComponents()) {
            // Remove only the actual grid panel, not the TitledBorder itself
            // The grid panel is usually the one added with BorderLayout.CENTER
            if (comp instanceof JPanel && !(comp instanceof JLabel) && ((JPanel)comp).getLayout() instanceof GridLayout) {
                 parentFrame.remove(comp);
            }
        }
        
        JPanel gridPanel = new JPanel(new GridLayout(gameSettings.boardRows + 1, gameSettings.boardCols + 1));
        buttonMap.clear();

        // Add empty top-left corner
        gridPanel.add(new JLabel(""));

        // Column headers (A, B, C...)
        for (int c = 0; c < gameSettings.boardCols; c++) {
            JLabel colLabel = new JLabel(String.valueOf((char) ('A' + c)), SwingConstants.CENTER);
            colLabel.setFont(new Font("Arial", Font.BOLD, 9)); // Smaller font for headers
            gridPanel.add(colLabel);
        }

        // Grid buttons
        for (int r = 0; r < gameSettings.boardRows; r++) {
            // Row headers (0, 1, 2...)
            JLabel rowLabel = new JLabel(String.valueOf(r), SwingConstants.CENTER);
            rowLabel.setFont(new Font("Arial", Font.BOLD, 9)); // Smaller font for headers
            gridPanel.add(rowLabel);

            for (int c = 0; c < gameSettings.boardCols; c++) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(28, 28)); // Slightly smaller button size
                btn.setMinimumSize(new Dimension(28, 28));
                btn.setMaximumSize(new Dimension(28, 28));
                btn.setOpaque(true); // Ensure background color is visible
                // FIX: Add border line to cells
                btn.setBorder(new LineBorder(Color.GRAY, 1)); // 1-pixel gray border
                // btn.setBorderPainted(false); // Removed this line to show border

                btn.setFont(new Font("Arial", Font.BOLD, 10)); // Smaller font for 'X' and 'M'

                final int row = r; // Effectively final for lambda
                final int col = c; // Effectively final for lambda

                if (isTargetBoard) {
                    btn.addActionListener(e -> onTargetCellClick(row, col));
                } else {
                    btn.setEnabled(false); // Own board not clickable for firing
                }
                gridPanel.add(btn);
                buttonMap.put(new Point(r, c), btn);
            }
        }
        parentFrame.add(gridPanel, BorderLayout.CENTER); // Add the grid to the parent frame
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    public void updateAllBoardDisplays() {
        // Update Your Own Board (Player's Defense)
        Board ownBoard = currentPlayer.getOwnBoard();
        for (int r = 0; r < gameSettings.boardRows; r++) {
            for (int c = 0; c < gameSettings.boardCols; c++) {
                JButton button = ownBoardButtons.get(new Point(r, c));
                int cellState = ownBoard.getCellState(r, c);

                Color color = Color.BLUE.brighter(); // Light blue for Water
                String text = "";

                // Hide ships on own board, only show hits/misses
                if (cellState == Board.HIT) {
                    color = Color.RED; // Hit ship part (on player's own board)
                    text = "X";
                } else if (cellState == Board.MISS) {
                    color = Color.WHITE; // Missed shot (on player's own board)
                    text = "M";
                }
                // If cellState is Board.SHIP or Board.WATER, it defaults to lightblue with no text.

                button.setBackground(color);
                button.setText(text);
                button.setEnabled(false); // Ensure own board buttons remain disabled
            }
        }

        // Update Your Target Board (Player's Attack)
        Board trackingBoard = currentPlayer.getTrackingBoard();
        for (int r = 0; r < gameSettings.boardRows; r++) {
            for (int c = 0; c < gameSettings.boardCols; c++) {
                JButton button = targetBoardButtons.get(new Point(r, c));
                int cellState = trackingBoard.getCellState(r, c);

                Color color = Color.BLUE.brighter(); // Light blue for Water
                String text = "";

                if (cellState == Board.HIT) {
                    color = Color.RED; // Hit opponent's ship
                    text = "X";
                } else if (cellState == Board.MISS) {
                    color = Color.WHITE; // Missed opponent's ship
                    text = "M";
                }

                button.setBackground(color);
                button.setText(text);
                button.setEnabled(!gameOver); // Enable if not game over
            }
        }
        updateStatisticsDisplay(); // Update stats after board update
    }

    private void updateStatisticsDisplay() {
        // Player 1 Stats
        p1UnsunkShipsLabel.setText("Unsunk Ships: " + player1.getUnsunkShipCount());
        p1ShipsListLabel.setText("Types Left: " + player1.getUnsunkShipsInfo());
        p1HitsLabel.setText("Total Hits: " + player1.getHitsMade());
        p1MissesLabel.setText("Total Misses: " + player1.getMissesMade());

        // Player 2 Stats
        p2UnsunkShipsLabel.setText("Unsunk Ships: " + player2.getUnsunkShipCount());
        p2ShipsListLabel.setText("Types Left: " + player2.getUnsunkShipsInfo());
        p2HitsLabel.setText("Total Hits: " + player2.getHitsMade());
        p2MissesLabel.setText("Total Misses: " + player2.getMissesMade());
        
        // Display current player's shots left
        if (currentPlayer == player1) {
            p1ShotsLeftLabel.setText("Shots Left This Turn: " + (maxShotsThisTurn - currentShotsTaken));
            p2ShotsLeftLabel.setText("Shots Left This Turn: --");
        } else {
            p1ShotsLeftLabel.setText("Shots Left This Turn: --");
            p2ShotsLeftLabel.setText("Shots Left This Turn: " + (maxShotsThisTurn - currentShotsTaken));
        }
    }


    public void startNewGame() {
        try {
            player1 = new Player("Player 1", gameSettings);
            player2 = new Player("Player 2", gameSettings);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Game Initialization Error", JOptionPane.ERROR_MESSAGE);
            gameOver = true;
            fireButton.setEnabled(false);
            messageLabel.setText("Game cannot start due to ship placement error. Check settings.");
            return;
        }

        opponentMap.put(player1, player2);
        opponentMap.put(player2, player1);
        currentPlayerIdx = 0;
        gameOver = false;
        currentShotsTaken = 0;
        selectedTargetCoords = null;

        updatePlayerTurn(); // This will also call updateAllBoardDisplays and updateStatisticsDisplay
        messageLabel.setText("Game started! Select your target.");
        fireButton.setEnabled(false);
        coordDisplayLabel.setText("--");
    }

    private void updatePlayerTurn() {
        currentPlayer = (currentPlayerIdx == 0) ? player1 : player2;
        currentPlayerLabel.setText(currentPlayer.getName() + "'s Turn");
        maxShotsThisTurn = currentPlayer.getShotsPerTurn();
        currentShotsTaken = 0; // Reset shots for new turn
        messageLabel.setText("Fire! You have " + maxShotsThisTurn + " shot(s) this turn. Select a target.");
        updateAllBoardDisplays(); // Update both boards and stats for the new player
        fireButton.setEnabled(false); // Disable until target selected
        selectedTargetCoords = null;
        coordDisplayLabel.setText("--");
    }

    private void onTargetCellClick(int row, int col) {
        if (gameOver) {
            return;
        }

        // Check if already shot here on the tracking board
        if (currentPlayer.getTrackingBoard().getCellState(row, col) == Board.HIT ||
            currentPlayer.getTrackingBoard().getCellState(row, col) == Board.MISS) {
            messageLabel.setText("Coordinates " + (char)('A' + col) + row + " already shot here. Choose another cell.");
            fireButton.setEnabled(false);
            selectedTargetCoords = null;
            coordDisplayLabel.setText("--");
            return;
        }

        selectedTargetCoords = new Point(row, col);
        coordDisplayLabel.setText(String.valueOf((char)('A' + col)) + row);
        messageLabel.setText("Target selected: " + (char)('A' + col) + row + ". Press Fire!");
        fireButton.setEnabled(true);
    }

    private void processShot() {
        if (gameOver) {
            JOptionPane.showMessageDialog(this, "The game is over. Start a new game.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (selectedTargetCoords == null) {
            messageLabel.setText("Please select a target on the opponent's board.");
            return;
        }

        int row = selectedTargetCoords.x;
        int col = selectedTargetCoords.y;
        Player opponentPlayer = opponentMap.get(currentPlayer);

        Object[] result = currentPlayer.takeShot(opponentPlayer.getOwnBoard(), row, col);
        String resultMsg = (String) result[0];
        // Ship hitShip = (Ship) result[1]; // Not directly used here, but good to have

        if (resultMsg.equals("Invalid coordinates") || resultMsg.equals("Error in firing logic")) {
             messageLabel.setText("Error: Shot out of bounds or logic error!");
        } else if (resultMsg.equals("Already shot here")) {
            messageLabel.setText("You already shot at these coordinates. Try again.");
        } else {
            messageLabel.setText("Result: " + resultMsg);
            updateAllBoardDisplays(); // Update both boards and stats instantly after the shot!
            selectedTargetCoords = null; // Clear selected coords after shot
            coordDisplayLabel.setText("--");
            fireButton.setEnabled(false); // Disable fire button until new target selected

            if (opponentPlayer.getOwnBoard().allShipsSunk()) {
                gameOver = true;
                JOptionPane.showMessageDialog(this, "All " + opponentPlayer.getName() + "'s ships are sunk! " + currentPlayer.getName() + " wins!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                fireButton.setEnabled(false);
                messageLabel.setText(currentPlayer.getName() + " wins! Game Over.");
                return;
            }

            currentShotsTaken++;
            if (currentShotsTaken >= maxShotsThisTurn) {
                // Instant turn switch
                currentPlayerIdx = (currentPlayerIdx + 1) % 2; // (0 or 1)
                updatePlayerTurn(); // Immediately update for the next player
                messageLabel.setText(currentPlayer.getName() + "'s turn. Select your target.");
            } else {
                messageLabel.setText("Result: " + resultMsg + ". You have " + (maxShotsThisTurn - currentShotsTaken) + " shot(s) left. Select your next target.");
            }
        }
    }

    private void openSettings() {
        SettingsDialog settingsDialog = new SettingsDialog(this, gameSettings, this);
        settingsDialog.setVisible(true); // Display the modal dialog
    }

    public static void main(String[] args) {
        // Ensure GUI updates run on the Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new BattleshipGameGUI();
            }
        });
    }
}