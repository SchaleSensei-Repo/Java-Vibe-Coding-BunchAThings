package com.millebornes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    private String name;
    private List<Card> hand;
    private List<Card> distancePile;
    private Map<HazardEffect, Card> battlePile; // Stores active hazards/status cards
    private List<Card> safetyPile; // Permanently active safety cards
    private int totalDistance;
    private int limit200MilesPlayed;
    private boolean hasPlayedGoEver; // To ensure 'Go' is played at least once to start

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
        this.distancePile = new ArrayList<>();
        this.battlePile = new HashMap<>();
        this.safetyPile = new ArrayList<>();
        resetPlayerState(); // Initialize/reset
    }

    // Call this at the start of each new game for player state
    public void resetPlayerState() {
        hand.clear();
        distancePile.clear();
        battlePile.clear();
        safetyPile.clear();
        totalDistance = 0;
        limit200MilesPlayed = 0;
        hasPlayedGoEver = false;
    }

    public Map<String, Object> getCurrentStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("can_move", false);
        status.put("is_stopped", false);
        status.put("is_speed_limited", false);
        status.put("needs_remedy", null); // HazardEffect enum or null

        // Check safeties
        List<HazardEffect> safetiesActive = new ArrayList<>();
        for (Card safety : safetyPile) {
            safetiesActive.add(safety.getEffect());
        }

        // Check raw battle pile status
        boolean isStoppedByCard = battlePile.containsKey(HazardEffect.STOP);
        boolean isSpeedLimitedByCard = battlePile.containsKey(HazardEffect.SPEED_LIMIT);
        boolean isFlatTireByCard = battlePile.containsKey(HazardEffect.FLAT_TIRE);
        boolean isAccidentByCard = battlePile.containsKey(HazardEffect.ACCIDENT);
        boolean isOutOfGasByCard = battlePile.containsKey(HazardEffect.OUT_OF_GAS);

        // Apply safeties to determine effective status
        if (safetiesActive.contains(HazardEffect.RIGHT_OF_WAY)) {
            isStoppedByCard = false;
            isSpeedLimitedByCard = false;
        }
        if (safetiesActive.contains(HazardEffect.PUNCTURE_PROOF)) {
            isFlatTireByCard = false;
        }
        if (safetiesActive.contains(HazardEffect.DRIVING_ACE)) {
            isAccidentByCard = false;
        }
        if (safetiesActive.contains(HazardEffect.EXTRA_TANK)) {
            isOutOfGasByCard = false;
        }

        status.put("is_stopped", isStoppedByCard);
        status.put("is_speed_limited", isSpeedLimitedByCard);

        if (isFlatTireByCard) {
            status.put("needs_remedy", HazardEffect.FLAT_TIRE);
        } else if (isAccidentByCard) {
            status.put("needs_remedy", HazardEffect.ACCIDENT);
        } else if (isOutOfGasByCard) {
            status.put("needs_remedy", HazardEffect.OUT_OF_GAS);
        }

        // Player can move if 'Go' has been played, is not stopped, and has no other active hazards
        status.put("can_move", hasPlayedGoEver && !isStoppedByCard && status.get("needs_remedy") == null);

        return status;
    }

    public void addCardToBattlePile(Card card) {
        if (card.getEffect() == HazardEffect.GO) {
            battlePile.remove(HazardEffect.STOP); // Go removes Stop
            battlePile.put(HazardEffect.GO, card);
            hasPlayedGoEver = true;
        } else if (card.getEffect() == HazardEffect.STOP) {
            battlePile.remove(HazardEffect.GO); // Stop removes Go
            battlePile.put(HazardEffect.STOP, card);
        } else if (card.getEffect() == HazardEffect.SPEED_LIMIT) {
            battlePile.put(HazardEffect.SPEED_LIMIT, card);
        } else if (card.getEffect() == HazardEffect.FLAT_TIRE ||
                   card.getEffect() == HazardEffect.ACCIDENT ||
                   card.getEffect() == HazardEffect.OUT_OF_GAS) {
            // Only one of these "major" hazards can be active at a time. Remove any existing one.
            battlePile.remove(HazardEffect.FLAT_TIRE);
            battlePile.remove(HazardEffect.ACCIDENT);
            battlePile.remove(HazardEffect.OUT_OF_GAS);
            battlePile.put(card.getEffect(), card);
        }
    }

    public void removeCardFromBattlePile(HazardEffect hazardEffectToRemedy) {
        // For remedies (Go, End of Speed Limit, Spare Tire, Repairs, Gasoline)
        // 'Go' itself is not removed by a remedy, but is a status
        if (hazardEffectToRemedy == HazardEffect.GO) {
            // No direct removal needed here, it's a status
        } else if (hazardEffectToRemedy == HazardEffect.END_OF_SPEED_LIMIT) {
            battlePile.remove(HazardEffect.SPEED_LIMIT);
        } else if (hazardEffectToRemedy == HazardEffect.SPARE_TIRE) {
            battlePile.remove(HazardEffect.FLAT_TIRE);
        } else if (hazardEffectToRemedy == HazardEffect.REPAIRS) {
            battlePile.remove(HazardEffect.ACCIDENT);
        } else if (hazardEffectToRemedy == HazardEffect.GASOLINE) {
            battlePile.remove(HazardEffect.OUT_OF_GAS);
        }
        // Also called by safeties to clear corresponding hazards
        else if (hazardEffectToRemedy == HazardEffect.STOP) { // For Right of Way
            battlePile.remove(HazardEffect.STOP);
        } else if (hazardEffectToRemedy == HazardEffect.FLAT_TIRE) { // For Puncture Proof
            battlePile.remove(HazardEffect.FLAT_TIRE);
        } else if (hazardEffectToRemedy == HazardEffect.ACCIDENT) { // For Driving Ace
            battlePile.remove(HazardEffect.ACCIDENT);
        } else if (hazardEffectToRemedy == HazardEffect.OUT_OF_GAS) { // For Extra Tank
            battlePile.remove(HazardEffect.OUT_OF_GAS);
        }
    }

    public void addCardToSafetyPile(Card card) {
        // Ensure only one of each safety can be played
        boolean alreadyHasSafety = safetyPile.stream()
                                            .anyMatch(s -> s.getEffect() == card.getEffect());
        if (!alreadyHasSafety) {
            safetyPile.add(card);
        }
    }

    // Getters
    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public List<Card> getDistancePile() {
        return distancePile;
    }

    public Map<HazardEffect, Card> getBattlePile() {
        return battlePile;
    }

    public List<Card> getSafetyPile() {
        return safetyPile;
    }

    public int getTotalDistance() {
        return totalDistance;
    }

    public int getLimit200MilesPlayed() {
        return limit200MilesPlayed;
    }

    public boolean hasPlayedGoEver() {
        return hasPlayedGoEver;
    }

    // Setters/Mutators for game logic
    public void addCardToHand(Card card) {
        if (card != null) {
            hand.add(card);
        }
    }

    public void removeCardFromHand(Card card) {
        hand.remove(card);
    }

    public void addDistance(int distance) {
        this.totalDistance += distance;
    }

    public void increment200MilesPlayed() {
        this.limit200MilesPlayed++;
    }

    void removeCardFromBattle_Pile(HazardEffect hazardEffect) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}