package com.snake_ladder;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class GameLogic {
    public static final Random random = new Random();
    private GameSettings settings;
    private List<Player> players;
    private List<BoardTile> board;
    private int currentPlayerIndex;
    private Snake_and_Ladder_Warp_Variation gui;
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private boolean processingAiTurn = false;
    private int nextEliminationOrder = 1;


    private static final Color[] PLAYER_COLORS = {
        Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE,
        Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.PINK
    };

    public GameLogic(GameSettings settings, Snake_and_Ladder_Warp_Variation gui) {
        this.settings = settings;
        this.gui = gui;
        this.players = new ArrayList<>();
        this.board = new ArrayList<>();
    }

    public List<Player> getPlayers() { return players; }
    public List<BoardTile> getBoard() { return board; }
    public Player getCurrentPlayer() {
        if (players == null || players.isEmpty() || currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }
    public boolean isGameStarted() { return gameStarted; }
    public boolean isGameOver() { return gameOver; }
    public GameSettings getSettings() { return settings; }

    public void setupNewGame() {
        gameOver = false;
        gameStarted = true;
        processingAiTurn = false;
        nextEliminationOrder = 1;
        players.clear();
        for (int i = 0; i < settings.numPlayers; i++) {
            Player p = new Player(
                settings.playerNames[i],
                settings.initialPoints,
                settings.initialLives,
                settings.playerIsHuman[i],
                PLAYER_COLORS[i % PLAYER_COLORS.length],
                this.settings // Pass GameSettings
            );
            players.add(p);
        }
        generateBoard();
        currentPlayerIndex = 0;
        log("New game started with " + settings.numPlayers + " players.");
        if (getCurrentPlayer() != null) {
           log(getCurrentPlayer().getName() + "'s turn.");
        } else {
           log("Error: No current player after setup.");
        }
        if (gui != null) gui.updateGameDisplay();
        checkComputerTurn();
    }

    private void generateBoard() {
        board.clear();
        board.add(new BoardTile(TileEffect.START));
        List<TileEffect> weightedEffectPool = new ArrayList<>();
        settings.tileProbabilities.forEach((effect, weight) -> {
            for (int i = 0; i < weight; i++) { weightedEffectPool.add(effect); }
        });
        if (weightedEffectPool.isEmpty()) {
            log("Warning: All tile probabilities are zero. Using default NORMAL tiles.");
            for(TileEffect te : TileEffect.getRandomizableEffects()) weightedEffectPool.add(te);
        }
        for (int i = 1; i < settings.numTiles - 1; i++) {
            TileEffect randomEffect = weightedEffectPool.get(random.nextInt(weightedEffectPool.size()));
            BoardTile tile;
            switch (randomEffect) {
                case WARP_FORWARD:
                    tile = new BoardTile(randomEffect, settings.warpForwardMin, settings.warpForwardMax, settings.warpForwardStatic);
                    if(settings.warpForwardStatic) tile.setValue1(settings.warpForwardStaticValue);
                    break;
                case WARP_BACKWARD:
                    tile = new BoardTile(randomEffect, settings.warpBackwardMin, settings.warpBackwardMax, settings.warpBackwardStatic);
                     if(settings.warpBackwardStatic) tile.setValue1(settings.warpBackwardStaticValue);
                    break;
                case GIVE_POINTS:
                    tile = new BoardTile(randomEffect, settings.givePointsMin, settings.givePointsMax, settings.givePointsStatic);
                     if(settings.givePointsStatic) tile.setValue1(settings.givePointsStaticValue);
                    break;
                case TAKE_POINTS:
                    tile = new BoardTile(randomEffect, settings.takePointsMin, settings.takePointsMax, settings.takePointsStatic);
                    if(settings.takePointsStatic) tile.setValue1(settings.takePointsStaticValue);
                    break;
                default: tile = new BoardTile(randomEffect); break;
            }
            board.add(tile);
        }
        board.add(new BoardTile(TileEffect.FINISH));
        log("Board generated with " + settings.numTiles + " tiles.");
    }

    public int rollDice() {
        Player currentPlayer = getCurrentPlayer();
        if (gameOver || !gameStarted || currentPlayer == null) return 0;
        int totalRoll = 0;
        StringBuilder rollDetails = new StringBuilder("Rolled: ");
        for (int i = 0; i < settings.numDice; i++) {
            int roll = random.nextInt(settings.numSidesPerDie) + 1;
            totalRoll += roll;
            rollDetails.append(roll);
            if (i < settings.numDice - 1) rollDetails.append(" + ");
        }
        if (settings.numDice > 1) rollDetails.append(" = ").append(totalRoll);
        log(currentPlayer.getName() + " " + rollDetails.toString());
        return totalRoll;
    }

    public boolean processPlayerMove(int diceRoll) {
        Player currentPlayer = getCurrentPlayer();
        if (gameOver || !gameStarted || currentPlayer == null || currentPlayer.isEliminated()) {
            return gameOver;
        }
        int newTileIndex = currentPlayer.getCurrentTileIndex() + diceRoll;
        if (newTileIndex >= board.size() - 1) {
            newTileIndex = board.size() - 1;
            currentPlayer.setCurrentTileIndex(newTileIndex);
            log(currentPlayer.getName() + " moved to tile " + currentPlayer.getCurrentTileNumber() + " (FINISH!)");
            gameOver = true;
        } else {
            currentPlayer.setCurrentTileIndex(newTileIndex);
            log(currentPlayer.getName() + " moved to tile " + currentPlayer.getCurrentTileNumber());
            applyTileEffect(currentPlayer, board.get(newTileIndex)); // This might change points
        }

        // Apply negative point penalties if game is not over from the move itself
        if (!gameOver) {
            checkAndApplyNegativePointPenalties(currentPlayer); // This might set gameOver if player eliminated
        }
        
        if (gui != null) gui.updateGameDisplay();
        return gameOver;
    }

    public void humanPlayerTurn(int diceRoll) {
        if (processPlayerMove(diceRoll)) { // processPlayerMove now includes penalty check
            determineWinnerAndShowDialog();
        } else {
            nextTurn();
        }
    }

    private void applyTileEffect(Player player, BoardTile tile) {
        TileEffect effectToApply = tile.getEffect();
        int value = 0;
        if (effectToApply == TileEffect.RANDOM_EFFECT) {
            TileEffect[] possibleEffects = TileEffect.getEffectsForRandomTile();
            effectToApply = possibleEffects[random.nextInt(possibleEffects.length)];
            log(player.getName() + " landed on Random Tile! Got effect: " + effectToApply.getDisplayName());
             switch (effectToApply) {
                case WARP_FORWARD: value = settings.warpForwardStatic ? settings.warpForwardStaticValue : random.nextInt(settings.warpForwardMax - settings.warpForwardMin + 1) + settings.warpForwardMin; break;
                case WARP_BACKWARD: value = settings.warpBackwardStatic ? settings.warpBackwardStaticValue : random.nextInt(settings.warpBackwardMax - settings.warpBackwardMin + 1) + settings.warpBackwardMin; break;
                case GIVE_POINTS: value = settings.givePointsStatic ? settings.givePointsStaticValue : random.nextInt(settings.givePointsMax - settings.givePointsMin + 1) + settings.givePointsMin; break;
                case TAKE_POINTS: value = settings.takePointsStatic ? settings.takePointsStaticValue : random.nextInt(settings.takePointsMax - settings.takePointsMin + 1) + settings.takePointsMin; break;
                default: break;
            }
        } else if (tile.getEffect() == TileEffect.WARP_FORWARD || tile.getEffect() == TileEffect.WARP_BACKWARD ||
                   tile.getEffect() == TileEffect.GIVE_POINTS || tile.getEffect() == TileEffect.TAKE_POINTS) {
            value = tile.getActualValue();
        }
        switch (effectToApply) {
            case NORMAL: log("It's a Normal Tile. Nothing happens."); break;
            case WARP_FORWARD:
                int targetForward = Math.min(player.getCurrentTileIndex() + value, board.size() - 1);
                log(player.getName() + " warped FORWARD by " + value + " tiles to tile " + (targetForward + 1));
                player.setCurrentTileIndex(targetForward);
                if (targetForward == board.size() - 1) {
                    log(player.getName() + " warped to FINISH!");
                    gameOver = true;
                }
                break;
            case WARP_BACKWARD:
                int targetBackward = Math.max(player.getCurrentTileIndex() - value, 0);
                log(player.getName() + " warped BACKWARD by " + value + " tiles to tile " + (targetBackward + 1));
                player.setCurrentTileIndex(targetBackward);
                break;
            case GIVE_POINTS:
                player.addPoints(value);
                log(player.getName() + " gained " + value + " points. Total: " + player.getFormattedPoints());
                break;
            case TAKE_POINTS:
                player.takePoints(value); // Player.takePoints just subtracts
                log(player.getName() + " lost " + value + " points. Total: " + player.getFormattedPoints());
                // Penalty check will happen after processPlayerMove calls checkAndApplyNegativePointPenalties
                break;
            case GIVE_LIFE: player.addLife(); log(player.getName() + " gained a life. Lives: " + player.getLives()); break;
            case TAKE_LIFE: player.takeLife(); log(player.getName() + " lost a life. Lives: " + player.getLives()); checkElimination(player); break;
            case GO_TO_START: player.setCurrentTileIndex(0); log(player.getName() + " sent back to Start!"); break;
            case HARSH_GO_TO_START:
                player.setCurrentTileIndex(0); player.takeLife(); player.resetPoints();
                log(player.getName() + " HARSHLY sent to Start! Lost a life and points reset. Lives: " + player.getLives());
                checkElimination(player);
                break;
            default: break;
        }
    }

    private void checkAndApplyNegativePointPenalties(Player player) {
        if (player == null || player.isEliminated() || player.getPoints() >= 0) {
            return; // No penalty needed or player already out
        }

        log(player.getName() + " has negative points (" + player.getFormattedPoints() + "). Applying penalty as per rule: " + settings.negativePointsPenaltyRule);

        boolean lifeLost = false;
        switch (settings.negativePointsPenaltyRule) {
            case ALLOW_NEGATIVE:
                // If the rule is ALLOW_NEGATIVE, we then check the legacy boolean 'allowNegativePoints'.
                // If 'allowNegativePoints' is false, then clamp to 0. Otherwise, negatives are truly allowed.
                if (!settings.allowNegativePoints) {
                    log(player.getName() + "'s points clamped to 0 (legacy allowNegativePoints=false).");
                    player.setPoints(0);
                } else {
                     log(player.getName() + "'s points remain negative as per settings.");
                }
                break;

            case LOSE_LIFE_RESET_POINTS:
                log(player.getName() + " loses a life and points reset to 0.");
                player.takeLife();
                lifeLost = true;
                player.setPoints(0);
                break;

            case LOSE_LIFE_RESET_POINTS_TO_START:
                log(player.getName() + " loses a life, points reset to 0, and goes to Start.");
                player.takeLife();
                lifeLost = true;
                player.setPoints(0);
                player.setCurrentTileIndex(0);
                break;
        }
        
        if (lifeLost) {
            checkElimination(player); // Check if losing a life eliminated them
        }

        if (gui != null) {
            gui.updateGameDisplay(); // Update UI after penalty
        }
    }


    private void checkElimination(Player player) {
        if (player.getLives() <= 0) {
            if (settings.respawnEliminatedPlayers) {
                log(player.getName() + " ran out of lives! Respawning at start.");
                player.respawn();
                 if (gui != null) gui.updateGameDisplay();
            } else {
                if (!player.isEliminated()) {
                    player.setEliminated(true, nextEliminationOrder++);
                    log(player.getName() + " ran out of lives and is ELIMINATED! (Order: " + player.getEliminationOrder() + ")");
                }
                checkSinglePlayerRemaining();
            }
        }
    }

    private void checkSinglePlayerRemaining() {
        if (!settings.instantFinishIfOneLeft || players.size() <=1) {
            return;
        }
        long activePlayers = players.stream().filter(p -> !p.isEliminated()).count();
        if (activePlayers <= 1) {
            log("Only " + activePlayers + " player(s) remaining. Game Over!");
            gameOver = true;
        }
    }

    private void nextTurn() {
        if (gameOver) {
            determineWinnerAndShowDialog();
            return;
        }
        if (players.isEmpty()) {
            log("Error: No players in game. Ending game.");
            gameOver = true;
            determineWinnerAndShowDialog();
            return;
        }
        int attempts = 0;
        Player current;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            current = getCurrentPlayer();
            attempts++;
            if (current == null) {
                log("Error: Current player became null during nextTurn loop. Ending game.");
                gameOver = true;
                break;
            }
            if (attempts > players.size() * 2 && current.isEliminated()) {
                log("Safety break: All remaining players appear to be eliminated. Ending game.");
                gameOver = true;
                break;
            }
        } while (current.isEliminated());

        if (gameOver) {
            determineWinnerAndShowDialog();
            return;
        }
        Player newCurrentPlayer = getCurrentPlayer();
        if (newCurrentPlayer != null) {
            log(newCurrentPlayer.getName() + "'s turn.");
        } else {
             log("Error: New current player is NULL after loop. Ending Game.");
             gameOver = true;
             determineWinnerAndShowDialog();
             return;
        }
        if (gui != null) gui.updatePlayerTurnIndicator();
        checkComputerTurn();
    }

    private void checkComputerTurn() {
        if (gameOver || !gameStarted || processingAiTurn) {
            if (gameOver && !processingAiTurn && gui != null && !gui.isGameOverDialogShowing()) {
                determineWinnerAndShowDialog();
            }
            return;
        }
        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer == null) {
            return;
        }
        if (!currentPlayer.isHuman() && !currentPlayer.isEliminated()) {
            processingAiTurn = true;
            log("Computer player " + currentPlayer.getName() + " is thinking...");
            if (gui != null) {
                gui.disableRollButton();
            }
            Timer aiTurnTimer = new Timer(750, e -> {
                boolean endedByThisAiTurn = false;
                try {
                    if (!gameOver) {
                        int aiRoll = rollDice();
                        endedByThisAiTurn = processPlayerMove(aiRoll); // processPlayerMove includes penalty check
                    } else {
                        endedByThisAiTurn = true;
                    }
                } catch (Exception ex) {
                    log("AI Timer - EXCEPTION for " + currentPlayer.getName() + ": " + ex.getMessage());
                    ex.printStackTrace();
                    endedByThisAiTurn = true;
                } finally {
                    processingAiTurn = false;
                    if (endedByThisAiTurn || gameOver) {
                         if (gui != null && !gui.isGameOverDialogShowing()) {
                            determineWinnerAndShowDialog();
                        }
                    } else {
                        nextTurn();
                    }
                    if (gui != null) {
                        gui.enableRollButton();
                    }
                }
            });
            aiTurnTimer.setRepeats(false);
            aiTurnTimer.start();
        } else if (currentPlayer.isHuman() && !currentPlayer.isEliminated() && gui != null) {
            gui.enableRollButton();
        }
    }

    private void determineWinnerAndShowDialog() {
        if (gui == null) { return; }
        if (gui.isGameOverDialogShowing()) { return; }

        Player overallWinner = null;
        List<Player> tempPlayerListForWinnerDet = new ArrayList<>(players);
        Collections.sort(tempPlayerListForWinnerDet, Comparator
            .comparingInt(Player::getPoints).reversed()
            .thenComparingInt(Player::getLives).reversed()
            .thenComparingInt(Player::getCurrentTileIndex).reversed()
        );

        if (settings.winCondition == GameSettings.WinCondition.FIRST_TO_FINISH) {
            overallWinner = players.stream()
                .filter(p -> !p.isEliminated() && p.getCurrentTileIndex() == board.size() - 1)
                .findFirst()
                .orElseGet(() -> {
                    long activePlayers = players.stream().filter(p -> !p.isEliminated()).count();
                    if (settings.instantFinishIfOneLeft && activePlayers == 1) {
                        return players.stream().filter(p -> !p.isEliminated()).findFirst().orElse(null);
                    }
                    return tempPlayerListForWinnerDet.stream().filter(p -> !p.isEliminated()).findFirst().orElse(null);
                });
        } else {
            overallWinner = tempPlayerListForWinnerDet.stream().filter(p -> !p.isEliminated()).findFirst().orElse(null);
        }

        List<Player> leaderboardList = new ArrayList<>();
        final Player finalOverallWinner = overallWinner;

        if (finalOverallWinner != null) {
            leaderboardList.add(finalOverallWinner);
        }

        List<Player> remainingActivePlayers = players.stream()
            .filter(p -> !p.isEliminated() && p != finalOverallWinner)
            .collect(Collectors.toList());
        List<Player> eliminatedPlayers = players.stream()
            .filter(Player::isEliminated)
            .collect(Collectors.toList());

        Collections.sort(remainingActivePlayers, Comparator
            .comparingInt(Player::getPoints).reversed()
            .thenComparingInt(Player::getLives).reversed()
            .thenComparingInt(Player::getCurrentTileIndex).reversed()
        );
        leaderboardList.addAll(remainingActivePlayers);

        Collections.sort(eliminatedPlayers, Comparator
            .comparingInt(Player::getPoints).reversed()
            .thenComparingInt(Player::getLives).reversed()
            .thenComparingInt(Player::getCurrentTileIndex).reversed()
            .thenComparingInt(Player::getEliminationOrder)
        );
        leaderboardList.addAll(eliminatedPlayers);

        log("Final Leaderboard Order (Size: " + leaderboardList.size() + "):");
        if (leaderboardList.isEmpty() && (players != null && !players.isEmpty())) {
            log("LeaderboardList is empty, but players list is not. Creating fallback list.");
            List<Player> fallbackList = new ArrayList<>(players);
            Collections.sort(fallbackList, 
                Comparator.comparing(Player::isEliminated)
                .thenComparing(Player::getPoints, Comparator.reverseOrder())
                .thenComparing(Player::getLives, Comparator.reverseOrder())
                .thenComparing(Player::getCurrentTileIndex, Comparator.reverseOrder())
                .thenComparing(Player::getEliminationOrder) 
            );
            if (finalOverallWinner != null) {
                fallbackList.remove(finalOverallWinner);
                fallbackList.add(0, finalOverallWinner);
            }
            leaderboardList.clear();
            leaderboardList.addAll(fallbackList);
        } else if (leaderboardList.isEmpty() && (players == null || players.isEmpty())) {
            log("LeaderboardList and players list are both empty. Nothing to rank.");
        }
        
        for(int i=0; i < leaderboardList.size(); i++) {
            Player p = leaderboardList.get(i);
            log((i+1) + ". " + p.getName() + " Pts: " + p.getPoints() + " Lives: " + p.getLives() + 
                " Tile: " + p.getCurrentTileNumber() + (p.isEliminated() ? " (E"+p.getEliminationOrder()+")" : ""));
        }

        String winnerMessageText;
        if (finalOverallWinner != null) {
            winnerMessageText = "Game Over! Winner: " + finalOverallWinner.getName();
        } else {
            winnerMessageText = "Game Over! No clear winner.";
        }
        log(winnerMessageText);

        final String finalWinnerMessageText = winnerMessageText;
        final List<Player> finalLeaderboardListForDialog = leaderboardList;

        SwingUtilities.invokeLater(() -> {
            if (gui.isDisplayable() && !gui.isGameOverDialogShowing()) {
                 gui.showGameOverDialog(finalWinnerMessageText, finalLeaderboardListForDialog);
            }
        });
    }

    private void log(String message) {
        System.out.println(message);
        if (gui != null) {
            SwingUtilities.invokeLater(() -> {
                if (gui.isDisplayable()) {
                    gui.logMessage(message);
                }
            });
        }
    }
}