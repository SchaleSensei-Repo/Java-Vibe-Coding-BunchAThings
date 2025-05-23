package com.millebornes;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap; // <--- ADD THIS IMPORT IF NOT PRESENT
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MilleBornesGUI extends JFrame {
    private SettingsManager settingsManager;
    private MilleBornesGame game;

    private JPanel playerInfoPanel;
    private List<JPanel> playerFrames; // To hold references to the LabelFrames (JPanels with titled borders)
    private List<Map<String, JLabel>> playerLabels; // List of maps, each map holding JLabel references for a player

    private JPanel handPanel;
    private List<JButton> cardButtons;
    private int selectedCardIndex = -1; // Index of the card selected in the current player's hand

    private JButton drawButton;
    private JButton playButton;
    private JButton discardButton;
    private JTextArea logTextArea;

    public MilleBornesGUI() {
        super("Mille Bornes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 700);
        setResizable(false);
        setLocationRelativeTo(null); // Center window on screen

        settingsManager = new SettingsManager(GameConstants.SETTINGS_FILE);
        game = new MilleBornesGame(settingsManager);

        playerFrames = new ArrayList<>();
        playerLabels = new ArrayList<>();
        cardButtons = new ArrayList<>();

        createMenu();
        createWidgets();
        updateGUI(); // Initial GUI update
    }

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newGameItem = new JMenuItem("New Game");
        newGameItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });
        fileMenu.add(newGameItem);

        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSettingsWindow();
            }
        });
        fileMenu.add(settingsItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void createWidgets() {
        setLayout(new BorderLayout(10, 10)); // Main layout with some gaps

        // Top frame for player info
        playerInfoPanel = new JPanel();
        playerInfoPanel.setBorder(BorderFactory.createEtchedBorder());
        playerInfoPanel.setLayout(new GridLayout(1, 2, 5, 0)); // 1 row, 2 columns for players

        // Create player info frames dynamically (always create 2, hide one if solo)
        for (int i = 0; i < 2; i++) {
            JPanel playerFrame = new JPanel();
            playerFrame.setLayout(new BoxLayout(playerFrame, BoxLayout.Y_AXIS));
            playerFrame.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Player " + (i + 1) + " Info"));
            playerFrame.setPreferredSize(new Dimension(380, 150)); // Set preferred size for consistency

            playerFrames.add(playerFrame); // Store reference
            playerInfoPanel.add(playerFrame); // Add to info panel

            // Labels for each player
            // LINE 105 IS HERE: Corrected from ExceptioHashMap to HashMap
            java.util.Map<String, JLabel> pLabels = new HashMap<>();
            pLabels.put("name", new JLabel("Name:"));
            pLabels.put("distance", new JLabel("Distance: 0"));
            pLabels.put("status", new JLabel("Status:"));
            pLabels.put("safeties", new JLabel("Safeties:"));
            pLabels.put("battle_pile", new JLabel("Current State:"));

            for (JLabel label : pLabels.values()) {
                label.setAlignmentX(Component.LEFT_ALIGNMENT); // Align labels to the left
                playerFrame.add(label);
            }
            playerLabels.add(pLabels);
        }
        add(playerInfoPanel, BorderLayout.NORTH);

        // Middle frame for current player's hand
        handPanel = new JPanel();
        handPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Your Hand"));
        handPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5)); // Cards flow from left
        add(handPanel, BorderLayout.CENTER);

        // Bottom panel for actions and log
        JPanel actionLogPanel = new JPanel(new BorderLayout(5, 5));
        actionLogPanel.setBorder(BorderFactory.createEtchedBorder());

        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        drawButton = new JButton("Draw Card");
        drawButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                drawCardAction();
            }
        });
        actionButtonsPanel.add(drawButton);

        playButton = new JButton("Play Selected Card");
        playButton.setEnabled(false);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playCardAction();
            }
        });
        actionButtonsPanel.add(playButton);

        discardButton = new JButton("Discard Selected Card");
        discardButton.setEnabled(false);
        discardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                discardCardAction();
            }
        });
        actionButtonsPanel.add(discardButton);
        actionLogPanel.add(actionButtonsPanel, BorderLayout.NORTH);

        logTextArea = new JTextArea(10, 1);
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        actionLogPanel.add(scrollPane, BorderLayout.CENTER);

        add(actionLogPanel, BorderLayout.SOUTH);
    }

    private void startGame() {
        game.startGame();
        selectedCardIndex = -1;
        updateGUI();
    }

    private void openSettingsWindow() {
        SettingsDialog settingsDialog = new SettingsDialog(this, settingsManager);
        settingsDialog.setVisible(true);
        // After dialog closes, settingsManager might have new values.
        // Update the game instance's settings.
        // Game mode change requires starting a new game, which is handled in SettingsDialog.
        updateGUI(); // Refresh GUI to reflect any non-game-mode setting changes
    }

    private void selectCard(int index) {
        selectedCardIndex = index;
        for (int i = 0; i < cardButtons.size(); i++) {
            JButton btn = cardButtons.get(i);
            if (i == selectedCardIndex) {
                btn.setBorder(BorderFactory.createLineBorder(Color.BLUE, 3)); // Highlight selected
            } else {
                btn.setBorder(UIManager.getBorder("Button.border")); // Reset to default
            }
        }
        playButton.setEnabled(true);
        discardButton.setEnabled(true);
    }

    private void drawCardAction() {
        String errorMessage = game.drawCardForCurrentPlayer();
        if (errorMessage != null) {
            JOptionPane.showMessageDialog(this, errorMessage, "Draw Error", JOptionPane.ERROR_MESSAGE);
        }
        updateGUI();
    }

    private void playCardAction() {
        if (selectedCardIndex == -1) {
            JOptionPane.showMessageDialog(this, "Please select a card to play.", "No Card Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        Card cardToPlay = currentPlayer.getHand().get(selectedCardIndex);
        String targetPlayerName = null; // Default: target self

        // In 2-player hotseat mode, if playing a hazard, target opponent automatically
        if (settingsManager.getGameMode() == GameMode.TWO_PLAYER_HOTSEAT && cardToPlay.getType() == CardType.HAZARD) {
            Player opponentPlayer = game.getOpponentPlayer();
            if (opponentPlayer != null) {
                targetPlayerName = opponentPlayer.getName();
            } else {
                JOptionPane.showMessageDialog(this, "Could not find opponent to target with hazard card.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String errorMessage = game.playCard(cardToPlay, targetPlayerName);
        if (errorMessage != null) {
            JOptionPane.showMessageDialog(this, errorMessage, "Play Error", JOptionPane.ERROR_MESSAGE);
        }

        selectedCardIndex = -1; // Reset selection
        updateGUI();

        if (game.isGameOver()) {
            JOptionPane.showMessageDialog(this, game.getLogMessages().get(game.getLogMessages().size() - 1), "Game Over", JOptionPane.INFORMATION_MESSAGE);
            drawButton.setEnabled(false);
            playButton.setEnabled(false);
            discardButton.setEnabled(false);
        }
    }

    private void discardCardAction() {
        if (selectedCardIndex == -1) {
            JOptionPane.showMessageDialog(this, "Please select a card to discard.", "No Card Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        Card cardToDiscard = currentPlayer.getHand().get(selectedCardIndex);

        String errorMessage = game.discardCard(cardToDiscard);
        if (errorMessage != null) {
            JOptionPane.showMessageDialog(this, errorMessage, "Discard Error", JOptionPane.ERROR_MESSAGE);
        }

        selectedCardIndex = -1; // Reset selection
        updateGUI();
    }

    private void updateGUI() {
        Player currentPlayer = game.getCurrentPlayer();

        // Update Player Info frames
        // Manage visibility and content for 1 or 2 players
        for (int i = 0; i < playerFrames.size(); i++) {
            JPanel playerFrameWidget = playerFrames.get(i);
            boolean playerExistsInGame = (i < game.getPlayers().size());

            if (settingsManager.getGameMode() == GameMode.SOLO && i == 1) {
                // In solo mode, hide the second player frame
                playerFrameWidget.setVisible(false);
            } else {
                playerFrameWidget.setVisible(true);
                if (playerExistsInGame) {
                    Player player = game.getPlayers().get(i);
                    Map<String, JLabel> labels = playerLabels.get(i);

                    labels.get("name").setText(player.getName() + (i == game.getCurrentPlayerIdx() ? " (CURRENT)" : ""));
                    labels.get("distance").setText("Distance: " + player.getTotalDistance() + " / " + settingsManager.getGoalDistance() + " Miles");

                    Map<String, Object> status = player.getCurrentStatus();
                    List<String> statusText = new ArrayList<>();
                    if (!player.hasPlayedGoEver()) {
                        statusText.add("Needs Go to Start");
                    }
                    if ((Boolean) status.get("is_stopped")) {
                        statusText.add("STOPPED!");
                    }
                    if ((Boolean) status.get("is_speed_limited")) {
                        statusText.add("Speed Limited (Max 50)");
                    }
                    if (status.get("needs_remedy") != null) {
                        statusText.add("Needs Remedy: " + titleCase(((HazardEffect) status.get("needs_remedy")).name().replace("_", " ")));
                    }
                    labels.get("status").setText("Status: " + (statusText.isEmpty() ? "Clear" : String.join(", ", statusText)));

                    String safetiesText = player.getSafetyPile().isEmpty() ? "None" : String.join(", ", player.getSafetyPile().stream().map(Card::getName).toList());
                    labels.get("safeties").setText("Safeties: " + safetiesText);

                    String battlePileText = player.getBattlePile().isEmpty() ? "Clear" : String.join(", ", player.getBattlePile().values().stream().map(Card::getName).toList());
                    labels.get("battle_pile").setText("Current State: " + battlePileText);
                } else {
                    // Clear labels for non-existent player in 2-player mode if for some reason it's visible but player doesn't exist
                    Map<String, JLabel> labels = playerLabels.get(i);
                    labels.get("name").setText("Name:");
                    labels.get("distance").setText("Distance: 0");
                    labels.get("status").setText("Status:");
                    labels.get("safeties").setText("Safeties:");
                    labels.get("battle_pile").setText("Current State:");
                }
            }
        }
        playerInfoPanel.revalidate(); // Revalidate layout after changing visibility/packing
        playerInfoPanel.repaint();

        // Update Current Player's Hand
        handPanel.removeAll(); // Clear existing buttons
        cardButtons.clear();

        if (!game.isGameOver()) {
            drawButton.setEnabled(currentPlayer.getHand().size() < 7);
            playButton.setEnabled(selectedCardIndex != -1);
            discardButton.setEnabled(selectedCardIndex != -1);

            for (int i = 0; i < currentPlayer.getHand().size(); i++) {
                Card card = currentPlayer.getHand().get(i);
                JButton btn = new JButton(card.getName());
                final int idx = i; // Effective final for lambda
                btn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectCard(idx);
                    }
                });
                cardButtons.add(btn);
                handPanel.add(btn);
                if (i == selectedCardIndex) {
                    btn.setBorder(BorderFactory.createLineBorder(Color.BLUE, 3)); // Re-highlight selected
                }
            }
        } else { // Game over, disable all action buttons
            drawButton.setEnabled(false);
            playButton.setEnabled(false);
            discardButton.setEnabled(false);
        }
        handPanel.revalidate(); // Revalidate layout after adding/removing buttons
        handPanel.repaint();

        // Update Game Log
        logTextArea.setText("");
        for (String msg : game.getLogMessages()) {
            logTextArea.append(msg + "\n");
        }
        logTextArea.setCaretPosition(logTextArea.getDocument().getLength()); // Scroll to bottom
    }

    // Helper for title casing enum names
    private static String titleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String[] words = text.split(" ");
        StringBuilder titleCaseText = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                titleCaseText.append(Character.toUpperCase(word.charAt(0)))
                             .append(word.substring(1).toLowerCase());
            }
            titleCaseText.append(" ");
        }
        return titleCaseText.toString().trim();
    }


    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MilleBornesGUI().setVisible(true);
            }
        });
    }
}