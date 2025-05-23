package com.millebornes;

import java.util.Objects;

public class Card {
    private String name;
    private CardType type;
    private int value; // For distance cards
    private HazardEffect effect; // For hazards, remedies, safeties

    public Card(String name, CardType type, int value, HazardEffect effect) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.effect = effect;
    }

    public Card(String name, CardType type, HazardEffect effect) { // Constructor for non-distance cards
        this(name, type, 0, effect);
    }

    // Getters
    public String getName() {
        return name;
    }

    public CardType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public HazardEffect getEffect() {
        return effect;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return name.equals(card.name) && type == card.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}