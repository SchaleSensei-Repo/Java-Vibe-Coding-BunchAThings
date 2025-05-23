package com.millebornes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // Added for stream().map().toList() in updateGUI

public class MilleBornesGame {
    private SettingsManager settings;
    private Deck deck;
    private List<Player> players;
    private int currentPlayerIdx;
    private boolean gameOver;
    private List<String> logMessages;

    public MilleBornesGame(SettingsManager settingsManager) {
        this.settings = settingsManager;
        this.deck = new Deck();
        this.players = new ArrayList<>();
        this.logMessages = new ArrayList<>();
        setupPlayers(settings.getGameMode()); // Initial setup based on loaded settings
        startGame(); // Start a game on initialization
    }

    private void setupPlayers(GameMode gameMode) {
        players.clear();
        if (gameMode == GameMode.SOLO) {
            players.add(new Player("Player"));
        } else { // TWO_PLAYER_HOTSEAT
            players.add(new Player("Player 1"));
            players.add(new Player("Player 2"));
        }
    }

    public void startGame() {
        deck.resetAndShuffle();
        setupPlayers(settings.getGameMode()); // Re-setup players if game mode changed
        
        for (Player player : players) {
            player.resetPlayerState(); // Reset player's individual state
            for (int i = 0; i < 6; i++) { // Deal 6 cards to each player
                Card card = deck.drawCard();
                if (card != null) {
                    player.addCardToHand(card);
                }
            }
        }
        currentPlayerIdx = 0;
        gameOver = false;
        logMessages.clear();
        log("Game started!");
        log(getCurrentPlayer().getName() + "'s turn. Draw a card!");
    }

    public void log(String message) {
        logMessages.add(message);
        // Keep log trimmed to prevent excessive memory usage and for display
        if (logMessages.size() > 15) {
            // Create a new sublist containing only the last 15 messages
            logMessages = new ArrayList<>(logMessages.subList(logMessages.size() - 15, logMessages.size()));
        }
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIdx);
    }

    public Player getOpponentPlayer() {
        if (players.size() < 2) {
            return null; // No opponent in solo mode
        }
        return players.get((currentPlayerIdx + 1) % players.size());
    }

    // --- FIX START ---
    // This method was causing the UnsupportedOperationException
    public int getCurrentPlayerIdx() {
        return currentPlayerIdx;
    }
    // --- FIX END ---

    public String drawCardForCurrentPlayer() {
        if (gameOver) {
            return "Game is over!";
        }

        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer.getHand().size() >= 7) { // Max 6 cards at end of turn, draw to 7. Cannot draw if already 7.
            return "Your hand is full (max 7 cards after draw). Play or discard a card first.";
        }

        Card card = deck.drawCard();
        if (card != null) {
            currentPlayer.addCardToHand(card);
            log(currentPlayer.getName() + " drew a " + card.getName() + ".");
            return null; // No error
        } else {
            // Draw pile is empty, check if reshuffle is allowed by settings
            if (settings.isReshuffleEmptyPile()) {
                if (deck.reshuffleDiscardIntoDraw()) {
                    log("Draw pile empty. Reshuffling discard pile into draw pile.");
                    // Try drawing again after reshuffle
                    return drawCardForCurrentPlayer(); // Recursive call
                } else {
                    return "No cards left to draw (draw and discard piles are empty).";
                }
            } else {
                return "Draw pile is empty. No cards left to draw.";
            }
        }
    }

    public String playCard(Card cardToPlay, String targetPlayerName) {
        if (gameOver) {
            return "Game is over!";
        }

        Player currentPlayer = getCurrentPlayer();
        Player opponentPlayer = getOpponentPlayer();
        Player targetPlayer = currentPlayer; // Default target is self

        if (settings.getGameMode() == GameMode.TWO_PLAYER_HOTSEAT) {
            if (cardToPlay.getType() == CardType.HAZARD) {
                // In 2-player mode, hazards target opponent
                if (opponentPlayer != null && targetPlayerName != null && targetPlayerName.equals(opponentPlayer.getName())) {
                    targetPlayer = opponentPlayer;
                } else {
                    return "Hazard cards must be played on the opponent (" + opponentPlayer.getName() + ").";
                }
            } else if ((cardToPlay.getType() == CardType.REMEDY || cardToPlay.getType() == CardType.SAFETY) &&
                       opponentPlayer != null && targetPlayerName != null && targetPlayerName.equals(opponentPlayer.getName())) {
                return "Remedy and Safety cards can only be played on yourself.";
            }
        }

        // Validate play
        String validationMessage = validatePlay(currentPlayer, cardToPlay, targetPlayer);
        if (validationMessage != null) {
            return validationMessage;
        }

        // Apply effect
        applyCardEffect(currentPlayer, cardToPlay, targetPlayer);
        currentPlayer.removeCardFromHand(cardToPlay);
        deck.discardCard(cardToPlay);
        log(currentPlayer.getName() + " played " + cardToPlay.getName() + " on " + targetPlayer.getName() + ".");

        // Check win condition for the current player
        if (currentPlayer.getTotalDistance() >= settings.getGoalDistance()) {
            gameOver = true;
            log("GAME OVER! " + currentPlayer.getName() + " reached " + settings.getGoalDistance() + " miles and won!");
            return null; // Game won
        }

        endTurn();
        return null; // No error, play successful
    }

    public String discardCard(Card cardToDiscard) {
        if (gameOver) {
            return "Game is over!";
        }
        Player currentPlayer = getCurrentPlayer();
        if (!currentPlayer.getHand().contains(cardToDiscard)) {
            return "Selected card not in hand.";
        }

        currentPlayer.removeCardFromHand(cardToDiscard);
        deck.discardCard(cardToDiscard);
        log(currentPlayer.getName() + " discarded " + cardToDiscard.getName() + ".");
        endTurn();
        return null; // No error
    }

    private String validatePlay(Player player, Card card, Player targetPlayer) {
        Map<String, Object> playerStatus = player.getCurrentStatus();
        Map<String, Object> targetStatus = targetPlayer.getCurrentStatus();

        if (card.getType() == CardType.DISTANCE) {
            if (!(Boolean) playerStatus.get("can_move")) {
                return "Cannot play distance card: You are stopped or have an active hazard.";
            }
            if ((Boolean) playerStatus.get("is_speed_limited") && card.getValue() > 50) {
                return "Cannot play distance card: Speed Limit is active, max 50 miles.";
            }
            if (player.getTotalDistance() + card.getValue() > settings.getGoalDistance()) {
                return "Cannot exceed " + settings.getGoalDistance() + " miles.";
            }
            if (card.getValue() == 200 && player.getLimit200MilesPlayed() >= 2) {
                return "Cannot play more than two 200-mile cards.";
            }
        } else if (card.getType() == CardType.HAZARD) {
            if (settings.getGameMode() == GameMode.SOLO) {
                // In solo mode, hazards are played on self
                if (card.getEffect() == HazardEffect.STOP && (Boolean) targetStatus.get("is_stopped")) {
                    return "You are already stopped.";
                }
                if (card.getEffect() == HazardEffect.SPEED_LIMIT && (Boolean) targetStatus.get("is_speed_limited")) {
                    return "You are already speed limited.";
                }
                if (targetStatus.get("needs_remedy") != null &&
                    (card.getEffect() == HazardEffect.FLAT_TIRE || card.getEffect() == HazardEffect.ACCIDENT || card.getEffect() == HazardEffect.OUT_OF_GAS)) {
                    return "You already have an active hazard (" + ((HazardEffect) targetStatus.get("needs_remedy")).name().replace("_"," ").toLowerCase() + ").";
                }
            } else { // TWO_PLAYER_HOTSEAT: hazards target opponent
                if (targetPlayer == player) { // Should be caught by GUI, but good check
                    return "Hazard cards must be played on your opponent.";
                }

                // Check if opponent has corresponding safety
                HazardEffect safetyEffect = GameConstants.HAZARD_SAFETY_MAP.get(card.getEffect());
                // Using stream().anyMatch() for safetyPile check as it was originally Python's `in {s.hazard_type for s in safety_pile}`
                if (safetyEffect != null && targetPlayer.getSafetyPile().stream().anyMatch(s -> s.getEffect() == safetyEffect)) {
                    // Need a helper for title case here
                    return targetPlayer.getName() + " has " + titleCase(safetyEffect.name().replace("_"," ")) + " safety and is immune.";
                }
                // Check if target player already has the specific hazard (or a general hazard)
                if (card.getEffect() == HazardEffect.STOP && (Boolean) targetStatus.get("is_stopped")) {
                    return targetPlayer.getName() + " is already stopped.";
                }
                if (card.getEffect() == HazardEffect.SPEED_LIMIT && (Boolean) targetStatus.get("is_speed_limited")) {
                    return targetPlayer.getName() + " is already speed limited.";
                }
                if (targetStatus.get("needs_remedy") != null &&
                    (card.getEffect() == HazardEffect.FLAT_TIRE || card.getEffect() == HazardEffect.ACCIDENT || card.getEffect() == HazardEffect.OUT_OF_GAS)) {
                    // Need a helper for title case here
                    return targetPlayer.getName() + " already has an active hazard (" + titleCase(((HazardEffect) targetStatus.get("needs_remedy")).name().replace("_"," ")) + ").";
                }
            }
        } else if (card.getType() == CardType.REMEDY) {
            if (targetPlayer != player) {
                return "Remedy cards can only be played on yourself.";
            }

            HazardEffect neededHazard = GameConstants.REMEDY_HAZARD_MAP.get(card.getEffect());
            if (card.getEffect() == HazardEffect.GO) {
                // Can play Go if needs to start (hasn't played go ever) OR if stopped
                if (!player.hasPlayedGoEver() && player.getBattlePile().isEmpty()) {
                    // This is a valid initial Go play
                } else if (player.getBattlePile().containsKey(HazardEffect.STOP)) {
                    // This is a valid Go play to counter Stop
                } else {
                    return "Go card can only be played to start or to counter a Stop card.";
                }
            } else if (neededHazard != null && !player.getBattlePile().containsKey(neededHazard)) {
                return card.getName() + " can only be played if " + neededHazard.name().replace("_"," ") + " is active.";
            } else if (neededHazard == null) {
                return "Invalid remedy card effect."; // Should not happen with correct mapping
            }

        } else if (card.getType() == CardType.SAFETY) {
            if (targetPlayer != player) {
                return "Safety cards can only be played on yourself.";
            }
            if (player.getSafetyPile().stream().anyMatch(s -> s.getEffect() == card.getEffect())) {
                return "This safety card has already been played.";
            }
        }
        return null; // Valid play
    }

    private void applyCardEffect(Player player, Card card, Player targetPlayer) {
        if (card.getType() == CardType.DISTANCE) {
            player.getDistancePile().add(card);
            player.addDistance(card.getValue());
            if (card.getValue() == 200) {
                player.increment200MilesPlayed();
            }
        } else if (card.getType() == CardType.HAZARD) {
            targetPlayer.addCardToBattlePile(card);
        } else if (card.getType() == CardType.REMEDY) {
            // 'Go' and 'End of Speed Limit' are added to battle pile to show active status
            if (card.getEffect() == HazardEffect.GO || card.getEffect() == HazardEffect.END_OF_SPEED_LIMIT) {
                player.addCardToBattlePile(card);
            }
            // All remedies remove their corresponding hazard
            HazardEffect hazardRemoved = GameConstants.REMEDY_HAZARD_MAP.get(card.getEffect());
            if (hazardRemoved != null) {
                player.removeCardFromBattlePile(hazardRemoved);
            }
        } else if (card.getType() == CardType.SAFETY) {
            player.addCardToSafetyPile(card);
            // Safeties also immediately clear corresponding hazards
            if (card.getEffect() == HazardEffect.RIGHT_OF_WAY) {
                player.removeCardFromBattlePile(HazardEffect.STOP);
                player.removeCardFromBattlePile(HazardEffect.SPEED_LIMIT);
            } else if (card.getEffect() == HazardEffect.PUNCTURE_PROOF) {
                player.removeCardFromBattlePile(HazardEffect.FLAT_TIRE);
            } else if (card.getEffect() == HazardEffect.DRIVING_ACE) {
                player.removeCardFromBattlePile(HazardEffect.ACCIDENT);
            } else if (card.getEffect() == HazardEffect.EXTRA_TANK) {
                player.removeCardFromBattlePile(HazardEffect.OUT_OF_GAS);
            }
        }
    }

    private void endTurn() {
        if (!gameOver) {
            Player currentPlayer = getCurrentPlayer();
            // In a real game, if hand > 6, they'd discard. Here, GUI prevents it.
            if (currentPlayer.getHand().size() > 6) {
                log("Warning: " + currentPlayer.getName() + " ended turn with " + currentPlayer.getHand().size() + " cards. Max 6 allowed.");
            }

            currentPlayerIdx = (currentPlayerIdx + 1) % players.size();
            Player nextPlayer = getCurrentPlayer();
            
            if (settings.getGameMode() == GameMode.SOLO) {
                log("Your turn. (Solo Mode)");
            } else {
                log(nextPlayer.getName() + "'s turn.");
            }

            // Custom rule: Redraw if hand is empty AND setting enabled
            if (settings.isRedrawEmptyHand() && nextPlayer.getHand().isEmpty()) {
                log(nextPlayer.getName() + "'s hand is empty. Redrawing 6 cards.");
                for (int i = 0; i < 6; i++) {
                    Card card = deck.drawCard();
                    if (card != null) {
                        nextPlayer.addCardToHand(card);
                    } else {
                        // No more cards to draw even after reshuffle if pile is truly exhausted
                        if (deck.reshuffleDiscardIntoDraw()) {
                            log("Reshuffled discard pile to redraw empty hand.");
                            card = deck.drawCard();
                            if (card != null) nextPlayer.addCardToHand(card);
                        } else {
                            log("No more cards to redraw for empty hand (piles exhausted).");
                            break;
                        }
                    }
                }
            }
        }
    }

    // Getters for GUI to display game state
    public List<Player> getPlayers() {
        return players;
    }

    public List<String> getLogMessages() {
        return logMessages;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    // Helper for title casing strings (needed for log messages and status)
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
}