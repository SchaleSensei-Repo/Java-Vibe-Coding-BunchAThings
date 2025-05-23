package com.BingoGameApp;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel {
    private BingoSettings settings;
    private BingoCard player1Card, player2Card;
    private JLabel player1ScoreLabel, player2ScoreLabel;
    private JLabel player1BingoLabel, player2BingoLabel;
    private JLabel generatedNumberLabel;
    private JButton generateNumberButton;
    private List<Integer> calledNumbers;
    private Set<Integer> availableNumbersForCall;
    private int player1Score = 0;
    private int player2Score = 0;
    private int player1BingoCount = 0;
    private int player2BingoCount = 0;
    private Random random = new Random();

    public GamePanel(BingoSettings settings) {
        this.settings = settings;
        setLayout(new BorderLayout());

        resetGame();

        // --- Top Panel: Generated Number ---
        JPanel topPanel = new JPanel(new FlowLayout());
        generatedNumberLabel = new JLabel("Generated Number: -");
        generatedNumberLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(generatedNumberLabel);
        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel: Bingo Cards ---
        JPanel cardsPanel = new JPanel(new GridLayout(1, settings.getPlayerCount()));
        player1Card = new BingoCard(settings.getMinCardNumber(), settings.getMaxCardNumber());
        cardsPanel.add(createPlayerCardPanel("Player 1", player1Card, settings.isAutoSelectNumbers() ? null : 0));
        if (settings.getPlayerCount() == 2) {
            player2Card = new BingoCard(settings.getMinCardNumber(), settings.getMaxCardNumber());
            cardsPanel.add(createPlayerCardPanel("Player 2", player2Card, settings.isAutoSelectNumbers() ? null : 1));
        }
        add(cardsPanel, BorderLayout.CENTER);

        // --- Bottom Panel: Controls & Scores ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel scoreAndBingoPanel = new JPanel(new GridLayout(1, settings.getPlayerCount()));

        JPanel p1StatsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        player1ScoreLabel = new JLabel("Player 1 Score: 0");
        player1ScoreLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        player1BingoLabel = new JLabel("Bingos: 0");
        player1BingoLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        p1StatsPanel.add(player1ScoreLabel);
        p1StatsPanel.add(new JLabel(" | "));
        p1StatsPanel.add(player1BingoLabel);
        scoreAndBingoPanel.add(p1StatsPanel);

        if (settings.getPlayerCount() == 2) {
            JPanel p2StatsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            player2ScoreLabel = new JLabel("Player 2 Score: 0");
            player2ScoreLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            player2BingoLabel = new JLabel("Bingos: 0");
            player2BingoLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            p2StatsPanel.add(player2ScoreLabel);
            p2StatsPanel.add(new JLabel(" | "));
            p2StatsPanel.add(player2BingoLabel);
            scoreAndBingoPanel.add(p2StatsPanel);
        }
        bottomPanel.add(scoreAndBingoPanel, BorderLayout.WEST);

        generateNumberButton = new JButton("Generate New Number(s)");
        generateNumberButton.setFont(new Font("Arial", Font.BOLD, 18));
        generateNumberButton.addActionListener(e -> generateNumbers());
        bottomPanel.add(generateNumberButton, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);
        updateScoresAndBingos();
    }

    private JPanel createPlayerCardPanel(String playerName, BingoCard card, Integer playerIndexForManualSelect) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(playerName));
        panel.add(card, BorderLayout.CENTER);

        if (playerIndexForManualSelect != null) {
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 5; c++) {
                    final int row = r;
                    final int col = c;
                    JButton cellButton = card.getCellButton(r, c);
                    cellButton.addActionListener(e -> handleManualSelection(playerIndexForManualSelect, card, row, col));
                }
            }
        }
        return panel;
    }

    private void resetGame() {
        player1Score = 0;
        player2Score = 0;
        player1BingoCount = 0;
        player2BingoCount = 0;
        calledNumbers = new ArrayList<>();
        availableNumbersForCall = new HashSet<>();
        for (int i = settings.getMinCardNumber(); i <= settings.getMaxCardNumber(); i++) {
            availableNumbersForCall.add(i);
        }
        if (player1Card != null) player1Card.reset();
        if (player2Card != null) player2Card.reset();
        if (generatedNumberLabel != null) generatedNumberLabel.setText("Generated: -");
        if (generateNumberButton != null) generateNumberButton.setEnabled(true);
    }

    private void generateNumbers() {
        if (availableNumbersForCall.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All numbers have been called! Game over.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            generateNumberButton.setEnabled(false);
            return;
        }

        if (settings.isAwardPointsOnGenerate()) {
            int points = calculateAwardedPoints(settings.isRandomizeGeneratePoints(), 
                                                settings.getMinGeneratePoints(), 
                                                settings.getMaxGeneratePoints(), 
                                                settings.getExactGeneratePoints());
            addPoints(1, points);
            if (settings.getPlayerCount() == 2) {
                addPoints(2, points);
            }
        }

        List<Integer> newlyGeneratedNumbers = new ArrayList<>();
        for (int i = 0; i < settings.getNumbersPerGeneration(); i++) {
            if (availableNumbersForCall.isEmpty()) {
                break;
            }
            int randomIndex = random.nextInt(availableNumbersForCall.size());
            int generatedNum = (int) availableNumbersForCall.toArray()[randomIndex];
            availableNumbersForCall.remove(generatedNum);
            newlyGeneratedNumbers.add(generatedNum);
            calledNumbers.add(generatedNum);
        }

        generatedNumberLabel.setText("Generated: " + newlyGeneratedNumbers.toString());

        if (settings.isAutoSelectNumbers()) {
            autoSelectAndScore(newlyGeneratedNumbers);
        }
        updateScoresAndBingos();
    }

    private void autoSelectAndScore(List<Integer> generatedNums) {
        for (int num : generatedNums) {
            boolean player1Marked = player1Card.markNumber(num);
            if (player1Marked) {
                addPoints(1, calculateAwardedPoints(settings.isRandomizePoints(), settings.getMinRandomPoints(), settings.getMaxRandomPoints(), settings.getExactPoints()));
                checkForBingoWinCondition(1, player1Card);
            }
            if (settings.getPlayerCount() == 2) {
                boolean player2Marked = player2Card.markNumber(num);
                if (player2Marked) {
                    addPoints(2, calculateAwardedPoints(settings.isRandomizePoints(), settings.getMinRandomPoints(), settings.getMaxRandomPoints(), settings.getExactPoints()));
                    checkForBingoWinCondition(2, player2Card);
                }
            }
        }
    }

    private void handleManualSelection(int playerIndex, BingoCard card, int r, int c) {
        int clickedNumber = card.getNumberAt(r, c);

        if (card.isMarked(r, c)) {
            JOptionPane.showMessageDialog(this, "This number is already marked!", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (calledNumbers.contains(clickedNumber)) {
            card.markNumber(clickedNumber);
            addPoints(playerIndex + 1, calculateAwardedPoints(settings.isRandomizePoints(), settings.getMinRandomPoints(), settings.getMaxRandomPoints(), settings.getExactPoints()));
            checkForBingoWinCondition(playerIndex + 1, card);
            updateScoresAndBingos();
        } else {
            JOptionPane.showMessageDialog(this, clickedNumber + " was not a generated number!", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void addPoints(int playerNumber, int pointsToAdd) {
        if (playerNumber == 1) {
            player1Score += pointsToAdd;
        } else if (playerNumber == 2) {
            player2Score += pointsToAdd;
        }
    }

    private int calculateAwardedPoints(boolean randomize, int minRand, int maxRand, int exact) {
        if (randomize) {
            if (minRand > maxRand) {
                int temp = minRand;
                minRand = maxRand;
                maxRand = temp;
            }
            return random.nextInt(maxRand - minRand + 1) + minRand;
        } else {
            return exact;
        }
    }

    private void updateScoresAndBingos() {
        player1ScoreLabel.setText("Player 1 Score: " + player1Score);
        player1BingoLabel.setText("Bingos: " + player1BingoCount);
        if (settings.getPlayerCount() == 2) {
            player2ScoreLabel.setText("Player 2 Score: " + player2Score);
            player2BingoLabel.setText("Bingos: " + player2BingoCount);
        }
    }

    private void checkForBingoWinCondition(int playerNumber, BingoCard card) {
        List<List<Point>> newBingoLines = card.checkAndGetNewBingoLines();

        if (!newBingoLines.isEmpty()) {
            if (settings.isAwardPointsOnBingo()) {
                int bingoPoints = calculateAwardedPoints(settings.isRandomizeBingoPoints(),
                                                        settings.getMinBingoPoints(),
                                                        settings.getMaxBingoPoints(),
                                                        settings.getExactBingoPoints());
                addPoints(playerNumber, bingoPoints);
            }

            for (List<Point> line : newBingoLines) {
                card.highlightLine(line, new Color(0, 150, 0));
            }
            
            if (playerNumber == 1) {
                player1BingoCount = card.getBingoCount();
            } else if (playerNumber == 2) {
                player2BingoCount = card.getBingoCount();
            }
            updateScoresAndBingos();

            int currentPlayerBingoCount = (playerNumber == 1) ? player1BingoCount : player2BingoCount;

            if (currentPlayerBingoCount >= settings.getBingosNeededToWin()) {
                String winnerMessage;
                boolean gameOver = true; // Flag to indicate game over

                if (settings.getPlayerCount() == 1) {
                    winnerMessage = "BINGO! You win with " + currentPlayerBingoCount + " bingos!";
                } else {
                    int otherPlayerNumber = (playerNumber == 1) ? 2 : 1;
                    int otherPlayerBingoCount = (playerNumber == 1) ? player2BingoCount : player1BingoCount;

                    // If both players meet or exceed the bingo requirement in the same turn
                    if (otherPlayerBingoCount >= settings.getBingosNeededToWin()) {
                        // Tie breaker: most points
                        if (player1Score > player2Score) {
                            winnerMessage = "It's a TIE BREAKER! Player 1 wins with " + player1Score + " points!";
                        } else if (player2Score > player1Score) {
                            winnerMessage = "It's a TIE BREAKER! Player 2 wins with " + player2Score + " points!";
                        } else {
                            winnerMessage = "It's a perfect TIE! Both players have " + player1Score + " points and " + settings.getBingosNeededToWin() + " bingos!";
                        }
                    } else {
                        winnerMessage = "BINGO! Player " + playerNumber + " wins with " + currentPlayerBingoCount + " bingos!";
                    }
                }

                JOptionPane.showMessageDialog(this, winnerMessage, "Game Over!", JOptionPane.INFORMATION_MESSAGE);
                generateNumberButton.setEnabled(false);
                
                int response = JOptionPane.showConfirmDialog(this, "Play again?", "Game Over", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    resetGame();
                    updateScoresAndBingos();
                }
            }
        }
    }
}