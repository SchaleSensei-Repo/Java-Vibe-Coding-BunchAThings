package com.snake_ladder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter; // Added for window listener
import java.awt.event.WindowEvent;   // Added for window listener
import java.util.List;

public class MainFrame extends JFrame {
    private GameSettings gameSettings;
    private GameLogic gameLogic;
    private BoardPanel boardPanel;
    private JTextArea logArea;
    private JLabel currentPlayerLabel;
    private JLabel currentTurnColorIndicator;
    private JPanel playersInfoPanel;
    private JButton rollDiceButton;
    private JLabel diceResultLabel;
    private JPanel legendPanel;
    private JDialog currentGameOverDialog = null; // To track the game over dialog instance


    

    public MainFrame() {
        gameSettings = new GameSettings();
        gameLogic = new GameLogic(gameSettings, this);

        setTitle("Warp Game (Snake & Ladder Variant)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGameItem = new JMenuItem("New Game");
        newGameItem.addActionListener(e -> startNewGame());
        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(e -> openSettingsDialog());
        JMenuItem resetSettingsItem = new JMenuItem("Reset Settings to Default");
        resetSettingsItem.addActionListener(e -> {
            gameSettings.resetToDefaults();
            gameSettings.saveSettings();
            JOptionPane.showMessageDialog(this, "Settings reset to defaults and saved.", "Settings Reset", JOptionPane.INFORMATION_MESSAGE);
             if (gameLogic.isGameStarted()) {
                boardPanel.resetDimensionsRecalculatedFlag();
                boardPanel.repaint();
                logMessage("Settings reset. Board display updated. Consider starting a New Game for all changes.");
            }
        });
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        gameMenu.add(newGameItem);
        gameMenu.add(settingsItem);
        gameMenu.add(resetSettingsItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);

        boardPanel = new BoardPanel(gameLogic);
        JScrollPane boardScrollPane = new JScrollPane(boardPanel);
        boardScrollPane.setPreferredSize(new Dimension(600, 600));
        add(boardScrollPane, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        playersInfoPanel = new JPanel();
        playersInfoPanel.setLayout(new BoxLayout(playersInfoPanel, BoxLayout.Y_AXIS));
        playersInfoPanel.setBorder(BorderFactory.createTitledBorder("Players"));
        JScrollPane playersInfoScrollPane = new JScrollPane(playersInfoPanel);
        playersInfoScrollPane.setPreferredSize(new Dimension(250, 150));
        rightPanel.add(playersInfoScrollPane);

        JPanel turnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        currentTurnColorIndicator = new JLabel("■ ");
        currentTurnColorIndicator.setFont(currentTurnColorIndicator.getFont().deriveFont(Font.BOLD, 14f));
        turnPanel.add(currentTurnColorIndicator);
        currentPlayerLabel = new JLabel("Current Player: -");
        turnPanel.add(currentPlayerLabel);
        rightPanel.add(turnPanel);

        legendPanel = createLegendPanel();
        rightPanel.add(legendPanel);

        JPanel dicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rollDiceButton = new JButton("Roll Dice");
        rollDiceButton.addActionListener(e -> {
            System.out.println("MF: Roll Dice button clicked."); // CONSOLE LOG
            if (gameLogic.isGameStarted() && !gameLogic.isGameOver()) {
                Player cp = gameLogic.getCurrentPlayer();
                if (cp != null && cp.isHuman() && !cp.isEliminated()) {
                    System.out.println("MF: Human player " + cp.getName() + " rolling."); // CONSOLE LOG
                    int roll = gameLogic.rollDice();
                    diceResultLabel.setText("Dice: " + roll);
                    //gameLogic.processTurn(roll);
                    gameLogic.humanPlayerTurn(roll);
                } else {
                    logMessage("Not a human player's turn or game not active. Current player: " + (cp != null ? cp.getName() : "null"));
                    System.out.println("MF: Roll Dice - Not human or game not active. CP: " + (cp != null ? cp.getName() : "null") + " isHuman: " + (cp != null ? cp.isHuman() : "N/A")); // CONSOLE LOG
                }
            } else {
                 System.out.println("MF: Roll Dice - Game not started or over. Started: " + gameLogic.isGameStarted() + " Over: " + gameLogic.isGameOver()); // CONSOLE LOG
            }
        });
        dicePanel.add(rollDiceButton);
        diceResultLabel = new JLabel("Dice: -");
        dicePanel.add(diceResultLabel);
        rightPanel.add(dicePanel);

        rollDiceButton.setEnabled(false);

        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Game Log"));
        rightPanel.add(logScrollPane);

        add(rightPanel, BorderLayout.EAST);

        pack();
        setMinimumSize(new Dimension(850, 650));
        setLocationRelativeTo(null);
        setVisible(true);

        logMessage("Welcome! Go to Game > New Game to start, or Game > Settings to configure.");
    }

    private JPanel createLegendPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Tile Legend"));
        Font legendFont = new Font("SansSerif", Font.PLAIN, 10);
        for (TileEffect effect : TileEffect.values()) {
            if (effect == TileEffect.NORMAL || effect == TileEffect.START || effect == TileEffect.FINISH) {
                continue;
            }
            String abbr = BoardPanel.getEffectAbbreviation(effect);
            if (!abbr.isEmpty()) {
                JLabel abbrLabel = new JLabel(abbr + ":");
                abbrLabel.setFont(legendFont);
                panel.add(abbrLabel);
                JLabel nameLabel = new JLabel(effect.getDisplayName());
                nameLabel.setFont(legendFont);
                panel.add(nameLabel);
            }
        }
        panel.setMaximumSize(new Dimension(250, 150));
        return panel;
    }


    private void startNewGame() {
        System.out.println("MF: startNewGame called."); // CONSOLE LOG
        if (currentGameOverDialog != null && currentGameOverDialog.isVisible()) {
            currentGameOverDialog.dispose(); // Close any existing game over dialog
            currentGameOverDialog = null;
        }
        logArea.setText("");
        boardPanel.resetDimensionsRecalculatedFlag();
        gameLogic.setupNewGame(); // This now calls checkComputerTurn
        rollDiceButton.setEnabled(gameLogic.getCurrentPlayer() != null && gameLogic.getCurrentPlayer().isHuman()); // Enable if first player is human
        diceResultLabel.setText("Dice: -");
        updateGameDisplay();
    }

    private void openSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(this, gameSettings);
        dialog.setVisible(true);
        if (dialog.wereSettingsSaved()) {
            logMessage("Settings saved. Start a New Game to apply all changes, or board will update visual settings on next repaint.");
            boardPanel.resetDimensionsRecalculatedFlag();
            boardPanel.repaint();
        }
    }

    public void logMessage(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    public void updateGameDisplay() {
        System.out.println("MF: updateGameDisplay called."); // CONSOLE LOG
        updatePlayersInfo();
        updatePlayerTurnIndicator();
        boardPanel.repaint();
    }

    public void updatePlayersInfo() {
        playersInfoPanel.removeAll();
        if (gameLogic.getPlayers() != null) {
            for (Player p : gameLogic.getPlayers()) {
                JPanel playerRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
                playerRowPanel.setOpaque(false);
                JLabel colorIndicator = new JLabel("■");
                colorIndicator.setForeground(p.getColor());
                colorIndicator.setFont(colorIndicator.getFont().deriveFont(Font.BOLD, 12f));
                playerRowPanel.add(colorIndicator);
                JLabel playerLabel = new JLabel(p.toString());
                if (p.isEliminated()) {
                    playerLabel.setForeground(Color.GRAY);
                } else {
                    playerLabel.setForeground(Color.BLACK);
                }
                if (gameLogic.getCurrentPlayer() == p && !p.isEliminated()) {
                    playerLabel.setFont(playerLabel.getFont().deriveFont(Font.BOLD));
                }
                playerRowPanel.add(playerLabel);
                playersInfoPanel.add(playerRowPanel);
            }
        }
        playersInfoPanel.revalidate();
        playersInfoPanel.repaint();
    }

    public void updatePlayerTurnIndicator() {
        currentPlayerLabel.setForeground(Color.BLACK);
        Player cp = gameLogic.getCurrentPlayer();
        System.out.println("MF: updatePlayerTurnIndicator. CurrentPlayer: " + (cp != null ? cp.getName() : "null") + ", isGameOver: " + gameLogic.isGameOver()); // CONSOLE LOG

        if (gameLogic.isGameOver()){
            currentPlayerLabel.setText("Game Over!");
            currentTurnColorIndicator.setText("");
            rollDiceButton.setEnabled(false);
        } else if (cp != null) {
            currentPlayerLabel.setText("Turn: " + cp.getName());
            currentTurnColorIndicator.setForeground(cp.getColor());
            currentTurnColorIndicator.setText("■ ");
            rollDiceButton.setEnabled(cp.isHuman() && !cp.isEliminated() && !gameLogic.isGameOver());
        } else {
            currentPlayerLabel.setText("Turn: -");
            currentTurnColorIndicator.setText("");
            rollDiceButton.setEnabled(false);
        }
    }

    public void showGameOverDialog(String winnerMessage, List<Player> rankedPlayers) {
        System.out.println("MF: showGameOverDialog called. Winner msg: " + winnerMessage + ". Players in list: " + (rankedPlayers != null ? rankedPlayers.size() : "null list"));
        if (rankedPlayers != null) {
            for(int i=0; i < rankedPlayers.size(); i++) {
                Player p = rankedPlayers.get(i);
                System.out.println("MF: Leaderboard Player " + (i+1) + ": " + p.getName() + " Pts: " + p.getPoints() + " Lives: " + p.getLives() + " Tile: " + p.getCurrentTileNumber() + " Elim: " + p.isEliminated());
            }
        } else {
             System.out.println("MF: rankedPlayers list is NULL in showGameOverDialog");
             return; // Cannot proceed if list is null
        }


        if (currentGameOverDialog != null && currentGameOverDialog.isVisible()) {
            System.out.println("MF: Game over dialog already showing, returning.");
            return;
        }
        rollDiceButton.setEnabled(false);
        diceResultLabel.setText("Game Over!");

        currentGameOverDialog = new JDialog(this, "Game Over", true);
        currentGameOverDialog.setLayout(new BorderLayout(10, 10));
        currentGameOverDialog.setSize(450, 350);
        currentGameOverDialog.setLocationRelativeTo(this);

        JLabel winnerLabel = new JLabel(winnerMessage, SwingConstants.CENTER);
        winnerLabel.setFont(winnerLabel.getFont().deriveFont(Font.BOLD, 16f));
        currentGameOverDialog.add(winnerLabel, BorderLayout.NORTH);

        String[] columnNames = {"Rank", "Player", "Points", "Lives", "Tile"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable leaderboardTable = new JTable(tableModel);
        leaderboardTable.setFillsViewportHeight(true);

        if (rankedPlayers.isEmpty()) {
            System.out.println("MF: rankedPlayers list is EMPTY. Table will be empty.");
            tableModel.addRow(new Object[]{"-", "No Players Ranked", "-", "-", "-"}); // Show something if empty
        } else {
            for (int i = 0; i < rankedPlayers.size(); i++) {
                Player p = rankedPlayers.get(i);
                Object[] rowData = {
                    i + 1,
                    p.getName() + (p.isEliminated() ? " (E)" : ""),
                    p.getFormattedPoints(),
                    p.getLives(),
                    p.getCurrentTileNumber()
                };
                System.out.println("MF: Adding to table: " + (i+1) + ", " + p.getName() + ", " + p.getFormattedPoints());
                tableModel.addRow(rowData);
            }
        }

        leaderboardTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        leaderboardTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        leaderboardTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        leaderboardTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        leaderboardTable.getColumnModel().getColumn(4).setPreferredWidth(50);

        JScrollPane tableScrollPane = new JScrollPane(leaderboardTable);
        tableScrollPane.setPreferredSize(new Dimension(400, 200)); // Set preferred size for scroll pane
        currentGameOverDialog.add(tableScrollPane, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (currentGameOverDialog != null) currentGameOverDialog.dispose();
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        currentGameOverDialog.add(buttonPanel, BorderLayout.SOUTH);

        currentGameOverDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                System.out.println("MF: Game Over Dialog CLOSED."); // CONSOLE LOG
                currentGameOverDialog = null; // Reset when closed
            }
        });
        
        System.out.println("MF: Making Game Over Dialog visible."); // CONSOLE LOG
        updatePlayersInfo(); // Update player info one last time
        updatePlayerTurnIndicator(); // Update turn indicator to show "Game Over!"
        currentGameOverDialog.setVisible(true);
    }

    public boolean isGameOverDialogShowing() {
        return currentGameOverDialog != null && currentGameOverDialog.isVisible();
    }

    public void disableRollButton() {
        System.out.println("MF: disableRollButton called."); // CONSOLE LOG
        rollDiceButton.setEnabled(false);
    }

    public void enableRollButton() {
        Player cp = gameLogic.getCurrentPlayer();
        boolean enable = gameLogic.isGameStarted() && !gameLogic.isGameOver() &&
                         cp != null && cp.isHuman() && !cp.isEliminated();
        System.out.println("MF: enableRollButton called. Should enable: " + enable + " (CP: " + (cp != null ? cp.getName() : "null") + ")"); // CONSOLE LOG
        rollDiceButton.setEnabled(enable);
    }

    public static void main(String[] args) {
    GlobalExceptionHandler.register(); // REGISTER THE HANDLER FIRST

    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
        // e.printStackTrace(); // This would now go to GlobalExceptionHandler if it happens after registration
    }
    SwingUtilities.invokeLater(MainFrame::new);
}
}