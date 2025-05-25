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
    private List<Long> bankerOfferHistory;

    // New for Authentic Mode Offer Sequence
    private List<Integer> authenticOfferSequence;
    private int currentAuthenticRoundIndex;


    public GameLogic(GameSettings settings) {
        this.settings = settings;
        this.openedBags = new ArrayList<>();
        this.bankerOfferHistory = new ArrayList<>();
        this.authenticOfferSequence = GameSettings.AUTHENTIC_OFFER_SEQUENCE; // Initialize with static constant
        this.currentState = GameState.INITIAL_CHOICE;
        initializeBags();
    }

    private void initializeBags() {
        allBags = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        Random rand = new Random();

        if (settings.isAuthenticModeEnabled()) {
            // Use authentic predefined values
            values.addAll(GameSettings.AUTHENTIC_BAG_VALUES);
            // Ensure values count matches number of bags in settings, just in case
            while (values.size() > settings.getNumberOfBags()) {
                values.remove(values.size() - 1); // Remove from end if too many
            }
            // If fewer values than bags, this might be problematic.
            // For authentic mode, settings.getNumberOfBags() should always be AUTHENTIC_BAG_VALUES.size()
        } else {
            // Generate unique random values with bias
            for (int i = 0; i < settings.getNumberOfBags(); i++) {
                int value;
                do {
                    double randomDouble = rand.nextDouble();

                    if (settings.isBagValueBiasEnabled()) {
                        int biasMagnitude = settings.getBagValueBiasMagnitude();
                        double normalizedBias = (double)biasMagnitude / 100.0;
                        double powerEffect = settings.getBagValueBiasStrength();

                        if (normalizedBias < 0.5) {
                            double power = 1.0 + (0.5 - normalizedBias) * 2 * powerEffect;
                            randomDouble = Math.pow(randomDouble, power);
                        } else if (normalizedBias > 0.5) {
                            double power = 1.0 + (normalizedBias - 0.5) * 2 * powerEffect;
                            randomDouble = 1.0 - Math.pow(1.0 - randomDouble, power);
                        }
                    }

                    value = (int) (settings.getMinBagValue() + randomDouble * (settings.getMaxBagValue() - settings.getMinBagValue()));
                    value = Math.max(settings.getMinBagValue(), value);
                    value = Math.min(settings.getMaxBagValue(), value);

                } while (values.contains(value));
                values.add(value);
            }
        }

        for (int i = 0; i < values.size(); i++) {
            allBags.add(new Bag(i + 1, values.get(i)));
        }

        Collections.shuffle(allBags);
        System.out.println("Bags initialized with values: " + values);
        this.bagsOpenedThisRound = 0;
    }

    public void startGame() {
        initializeBags();
        playerChosenBag = null;
        openedBags.clear();
        bankerOfferHistory.clear();
        bagsOpenedThisRound = 0;
        currentState = GameState.INITIAL_CHOICE;
        currentAuthenticRoundIndex = 0; // Reset for authentic mode
        System.out.println("--- New Game Started ---");
    }

    public void selectInitialBag(int bagId) {
        if (currentState != GameState.INITIAL_CHOICE) return;

        for (Bag bag : allBags) {
            if (bag.getId() == bagId) {
                playerChosenBag = bag;
                playerChosenBag.setChosen(true);
                currentState = GameState.OPENING_BAGS;
                System.out.println("Player chose Bag " + bagId + " as their case.");
                System.out.println("Game State changed to: " + currentState);
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
        if (currentState != GameState.OPENING_BAGS) {
            System.out.println("Cannot open bag " + bagId + ". Current state is: " + currentState);
            return null;
        }

        Bag bagToOpen = allBags.stream()
                               .filter(b -> b.getId() == bagId && !b.isOpened() && !b.isChosen())
                               .findFirst()
                               .orElse(null);

        if (bagToOpen != null) {
            bagToOpen.open();
            openedBags.add(bagToOpen);
            bagsOpenedThisRound++;
            System.out.println("Opened Bag " + bagId + " with value: $" + bagToOpen.getValue() + " | Bags Opened This Round: " + bagsOpenedThisRound);
            return bagToOpen;
        }
        System.out.println("Bag " + bagId + " cannot be opened (already opened or chosen).");
        return null;
    }

    /**
     * Swaps the player's chosen bag with another unopened, non-chosen bag.
     * @param bagIdToSwapWith The ID of the bag to swap with.
     * @return The new chosen Bag object, or null if swap failed.
     */
    public Bag swapBags(int bagIdToSwapWith) {
        if (currentState != GameState.OPENING_BAGS) return null;
        if (!settings.isAllowBagSwap()) { // New check
            System.out.println("Bag swapping is not allowed in current settings.");
            return null;
        }

        Bag otherBag = allBags.stream()
                              .filter(b -> b.getId() == bagIdToSwapWith && !b.isOpened() && !b.isChosen())
                              .findFirst()
                              .orElse(null);

        if (otherBag != null && playerChosenBag != null) {
            playerChosenBag.setChosen(false);
            otherBag.setChosen(true);

            Bag temp = playerChosenBag;
            playerChosenBag = otherBag;
            otherBag = temp;

            System.out.println("Player swapped their chosen bag with Bag " + bagIdToSwapWith);
            return playerChosenBag;
        }
        System.out.println("Failed to swap bags. Other bag was null, or player chosen bag was null.");
        return null;
    }

    public long calculateBankerOffer() {
        List<Bag> remainingBags = allBags.stream()
                                         .filter(b -> !b.isOpened() && !b.isChosen())
                                         .collect(Collectors.toList());

        if (playerChosenBag != null) {
            remainingBags.add(playerChosenBag);
        }

        if (remainingBags.isEmpty()) {
            System.out.println("No remaining bags for offer calculation. Returning 0.");
            return 0;
        }

        long sumOfRemainingValues = remainingBags.stream().mapToLong(Bag::getValue).sum();
        double averageRemainingValue = (double) sumOfRemainingValues / remainingBags.size();

        double offerFactor = 0.5;
        int totalBags = settings.getNumberOfBags();
        int bagsLeft = remainingBags.size();

        if (totalBags > 1) {
            offerFactor = 0.4 + (0.5 * (1.0 - (double)(bagsLeft -1) / (totalBags - 1)));
            offerFactor = Math.min(offerFactor, 0.99);
        }
        if (bagsLeft == 1) offerFactor = 1.0;

        // Apply Late Game Offer Boost
        if (settings.isLateGameOfferEnabled() && bagsLeft <= settings.getLateGameTriggerBags()) {
            offerFactor = offerFactor + (1.0 - offerFactor) * settings.getLateGameOfferFactorBoost();
            offerFactor = Math.min(offerFactor, 0.999);
            System.out.println("LATE GAME BOOST APPLIED! New offer factor: " + String.format("%.3f", offerFactor));
        }

        long calculatedOffer = (long) (averageRemainingValue * offerFactor);
        System.out.println("Base calculated offer (before bias/clamp): " + calculatedOffer);

        // Apply Banker Offer Bias (only if not in authentic mode, or if authentic mode allows bias, which it doesn't here)
        if (settings.isBankerBiasEnabled() && !settings.isAuthenticModeEnabled()) {
            int biasMagnitude = settings.getBankerBiasMagnitude();
            double biasStrength = 0.7;

            if (biasMagnitude < 50) {
                double normalizedBias = (50.0 - biasMagnitude) / 50.0;
                long amountToShift = (long) ((calculatedOffer - settings.getMinOfferValue()) * normalizedBias * biasStrength);
                calculatedOffer -= amountToShift;
                System.out.println("Bias towards MIN applied. Shifted by: -" + amountToShift);
            } else if (biasMagnitude > 50) {
                double normalizedBias = (biasMagnitude - 50.0) / 50.0;
                long amountToShift = (long) ((settings.getMaxOfferValue() - calculatedOffer) * normalizedBias * biasStrength);
                calculatedOffer += amountToShift;
                System.out.println("Bias towards MAX applied. Shifted by: +" + amountToShift);
            }
        }

        calculatedOffer = Math.max(settings.getMinOfferValue(), calculatedOffer);
        calculatedOffer = Math.min(settings.getMaxOfferValue(), calculatedOffer);
        System.out.println("Final calculated offer: " + calculatedOffer);
        return calculatedOffer;
    }

    public boolean shouldBankerOffer() {
        System.out.println("\n--- Should Banker Offer Check ---");
        System.out.println("Current state: " + currentState);

        if (currentState != GameState.OPENING_BAGS) {
            System.out.println("Not in OPENING_BAGS state. No offer.");
            return false;
        }

        long bagsAvailableToOpenCount = allBags.stream().filter(b -> !b.isOpened() && !b.isChosen()).count();
        System.out.println("Bags available to open (excluding chosen bag): " + bagsAvailableToOpenCount);

        if (bagsAvailableToOpenCount == 0) {
            System.out.println("No more bags to open. Game over.");
            return false;
        }

        boolean triggerOffer = false;

        if (settings.isAuthenticModeEnabled()) {
            // Authentic Mode: Fixed rounds sequence
            if (currentAuthenticRoundIndex < authenticOfferSequence.size()) {
                int bagsRequiredThisRound = authenticOfferSequence.get(currentAuthenticRoundIndex);
                if (bagsOpenedThisRound == bagsRequiredThisRound) {
                    triggerOffer = true;
                    System.out.println("AUTHENTIC MODE. Bags opened this round: " + bagsOpenedThisRound + ". Bags required: " + bagsRequiredThisRound + ". Offer triggered.");
                } else {
                    System.out.println("AUTHENTIC MODE. Bags opened this round: " + bagsOpenedThisRound + ". Bags required: " + bagsRequiredThisRound + ". No offer yet.");
                }
            } else {
                // If we've exhausted the authentic offer sequence, it means all required bags are opened.
                // The game is usually over or down to final 2 bags.
                System.out.println("AUTHENTIC MODE. All authentic offer rounds completed. No more automatic offers.");
            }
        } else {
            // Non-Authentic Mode: Existing late game or periodic logic
            if (settings.isLateGameOfferEnabled() && bagsAvailableToOpenCount <= settings.getLateGameTriggerBags()) {
                triggerOffer = (bagsOpenedThisRound > 0);
                System.out.println("LATE GAME PHASE. Trigger condition (bagsOpenedThisRound > 0): " + triggerOffer);
            } else {
                triggerOffer = (bagsOpenedThisRound > 0 && bagsOpenedThisRound % settings.getOfferRoundFrequency() == 0);
                System.out.println("NORMAL GAME PHASE. Trigger condition (bagsOpenedThisRound > 0 && % frequency == 0): " + triggerOffer);
            }
        }
        System.out.println("Bags opened this round (before potential reset): " + bagsOpenedThisRound);


        if (triggerOffer) {
            currentState = GameState.BANKER_OFFER;
            bagsOpenedThisRound = 0; // Reset for the next round
            if (settings.isAuthenticModeEnabled() && currentAuthenticRoundIndex < authenticOfferSequence.size()) {
                currentAuthenticRoundIndex++; // Move to next round's requirement for authentic mode
            }
            System.out.println("Banker OFFER triggered! State changed to BANKER_OFFER. Bags opened this round reset to 0.");
            return true;
        }
        System.out.println("Banker OFFER NOT triggered.");
        return false;
    }

    public void addBankerOfferToHistory(long offer) {
        bankerOfferHistory.add(offer);
        System.out.println("Offer added to history: " + offer);
    }

    public void resumeOpeningBags() {
        currentState = GameState.OPENING_BAGS;
        System.out.println("Game State changed to: " + currentState + " (resuming after No Deal).");
    }

    public void endGame() {
        currentState = GameState.GAME_OVER;
        System.out.println("--- Game Ended ---");
        System.out.println("Final State: " + currentState);
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
        return allBags.stream()
                .filter(b -> !b.isOpened())
                .collect(Collectors.toList());
    }

    public boolean isGameOver() {
        boolean gameOver = getBagsAvailableToOpen().isEmpty() || currentState == GameState.GAME_OVER;
        System.out.println("Checking isGameOver(): " + gameOver + " (Bags available to open: " + getBagsAvailableToOpen().size() + ", CurrentState: " + currentState + ")");
        return gameOver;
    }
}