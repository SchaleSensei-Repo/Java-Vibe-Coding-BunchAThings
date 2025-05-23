package com.deal.logic;

import com.deal.model.Bag;
import com.deal.model.GameSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GameLogic {

    public enum GameState {
        INITIAL_CHOICE,     // Player is choosing their main bag
        OPENING_BAGS,       // Player is opening bags in a round
        BANKER_OFFER,       // Banker has made an offer, waiting for Deal/No Deal
        GAME_OVER           // Game has ended
    }

    private GameSettings settings;
    private List<Bag> allBags;
    private Bag playerChosenBag;
    private List<Bag> openedBags; // Bags that have been opened and revealed
    private int bagsOpenedThisRound;
    private GameState currentState;
    private List<Long> bankerOfferHistory; // New: To store past offers

    public GameLogic(GameSettings settings) {
        this.settings = settings;
        this.openedBags = new ArrayList<>();
        this.bankerOfferHistory = new ArrayList<>(); // Initialize history list
        this.currentState = GameState.INITIAL_CHOICE;
        initializeBags(); // Initial setup
    }

    private void initializeBags() {
        allBags = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        Random rand = new Random();

        // Generate unique random values for each bag
        for (int i = 0; i < settings.getNumberOfBags(); i++) {
            int value;
            do {
                value = rand.nextInt(settings.getMaxBagValue() - settings.getMinBagValue() + 1) + settings.getMinBagValue();
            } while (values.contains(value)); // Ensure unique values
            values.add(value);
            allBags.add(new Bag(i + 1, value));
        }
        Collections.shuffle(allBags); // Randomize positions
        System.out.println("Bags initialized with values: " + values);
        this.bagsOpenedThisRound = 0;
    }

    public void startGame() {
        initializeBags();
        playerChosenBag = null;
        openedBags.clear();
        bankerOfferHistory.clear(); // Clear history for new game
        bagsOpenedThisRound = 0;
        currentState = GameState.INITIAL_CHOICE;
    }

    public void selectInitialBag(int bagId) {
        if (currentState != GameState.INITIAL_CHOICE) return;

        for (Bag bag : allBags) {
            if (bag.getId() == bagId) {
                playerChosenBag = bag;
                playerChosenBag.setChosen(true);
                currentState = GameState.OPENING_BAGS;
                System.out.println("Player chose Bag " + bagId + " as their case.");
                break;
            }
        }
    }

    /**
     * Opens a non-chosen bag.
     * @param bagId The ID of the bag to open.
     * @return The opened Bag object, or null if the bag couldn't be opened.
     */
    public Bag openBag(int bagId) {
        if (currentState != GameState.OPENING_BAGS) return null;

        Bag bagToOpen = allBags.stream()
                               .filter(b -> b.getId() == bagId && !b.isOpened() && !b.isChosen())
                               .findFirst()
                               .orElse(null);

        if (bagToOpen != null) {
            bagToOpen.open();
            openedBags.add(bagToOpen);
            bagsOpenedThisRound++;
            System.out.println("Opened Bag " + bagId + " with value: $" + bagToOpen.getValue());
            return bagToOpen;
        }
        return null;
    }

    /**
     * Swaps the player's chosen bag with another unopened, non-chosen bag.
     * @param bagIdToSwapWith The ID of the bag to swap with.
     * @return The new chosen Bag object, or null if swap failed.
     */
    public Bag swapBags(int bagIdToSwapWith) {
        if (currentState != GameState.OPENING_BAGS) return null;

        Bag otherBag = allBags.stream()
                              .filter(b -> b.getId() == bagIdToSwapWith && !b.isOpened() && !b.isChosen())
                              .findFirst()
                              .orElse(null);

        if (otherBag != null && playerChosenBag != null) {
            // Unmark current chosen bag
            playerChosenBag.setChosen(false);
            // Mark the new bag as chosen
            otherBag.setChosen(true);

            // Swap references
            Bag temp = playerChosenBag;
            playerChosenBag = otherBag;
            otherBag = temp; // The old chosen bag is now just a regular unopened bag

            System.out.println("Player swapped their chosen bag with Bag " + bagIdToSwapWith);
            return playerChosenBag;
        }
        return null;
    }

    public long calculateBankerOffer() {
        List<Bag> remainingBags = allBags.stream()
                                         .filter(b -> !b.isOpened() && !b.isChosen())
                                         .collect(Collectors.toList());

        // Add the player's chosen bag to remaining bags for calculation purposes
        if (playerChosenBag != null) {
            remainingBags.add(playerChosenBag);
        }

        if (remainingBags.isEmpty()) {
            return 0; // No bags left, offer is 0
        }

        long sumOfRemainingValues = remainingBags.stream().mapToLong(Bag::getValue).sum();
        double averageRemainingValue = (double) sumOfRemainingValues / remainingBags.size();

        // Simple offer logic: a percentage of the average.
        // This factor could be dynamic based on the number of bags left
        // For example, offer increases as fewer bags are left.
        // Example logic:
        double offerFactor = 0.5; // Base factor
        int totalBags = settings.getNumberOfBags();
        int bagsLeft = remainingBags.size();
        if (totalBags > 1) { // Avoid division by zero
            // A more aggressive offer as fewer bags remain
            offerFactor = 0.4 + (0.5 * (1.0 - (double)(bagsLeft -1) / (totalBags - 1)));
            // Ensure factor doesn't exceed 1.0 (or chosen bag value, etc.)
            offerFactor = Math.min(offerFactor, 0.99); // Capped at 99% of average
        }
        if (bagsLeft == 1) offerFactor = 1.0; // If only chosen bag left, offer could be its value, or just 0 (game over)

        long calculatedOffer = (long) (averageRemainingValue * offerFactor);

        // Clamp the offer within min/max specified by settings
        calculatedOffer = Math.max(settings.getMinOfferValue(), calculatedOffer);
        calculatedOffer = Math.min(settings.getMaxOfferValue(), calculatedOffer);

        return calculatedOffer;
    }

    public boolean shouldBankerOffer() {
        if (currentState != GameState.OPENING_BAGS) return false;

        // Ensure there's more than one bag left (the chosen bag + at least one other to open)
        long bagsAvailableToOpenCount = allBags.stream().filter(b -> !b.isOpened() && !b.isChosen()).count();
        if (bagsAvailableToOpenCount == 0) { // All non-chosen bags are opened
            return false; // No more offers, game is ending (player will open their chosen bag)
        }

        // Check if enough bags have been opened in this round
        boolean triggerOffer = (bagsOpenedThisRound > 0 && bagsOpenedThisRound % settings.getOfferRoundFrequency() == 0);

        if (triggerOffer) {
            currentState = GameState.BANKER_OFFER;
            bagsOpenedThisRound = 0; // Reset for the next round
            return true;
        }
        return false;
    }

    public void addBankerOfferToHistory(long offer) {
        bankerOfferHistory.add(offer);
    }

    public void resumeOpeningBags() {
        currentState = GameState.OPENING_BAGS;
    }

    public void endGame() {
        currentState = GameState.GAME_OVER;
    }

    // --- Getters for GUI to display state ---
    public List<Bag> getAllBags() {
        return allBags;
    }

    public Bag getPlayerChosenBag() {
        return playerChosenBag;
    }

    public List<Bag> getOpenedBags() {
        return openedBags;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public List<Long> getBankerOfferHistory() {
        return bankerOfferHistory;
    }

    public List<Bag> getBagsAvailableToOpen() {
        return allBags.stream()
                .filter(b -> !b.isOpened() && !b.isChosen())
                .collect(Collectors.toList());
    }

    public List<Bag> getRemainingUnopenedBagValues() {
        // Includes chosen bag and other unopened bags
        return allBags.stream()
                .filter(b -> !b.isOpened())
                .collect(Collectors.toList());
    }

    public boolean isGameOver() {
        // Game is over if all but chosen bag are opened, or deal is made
        return getBagsAvailableToOpen().isEmpty() || currentState == GameState.GAME_OVER;
    }
}