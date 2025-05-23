package com.deal.model;

public class Bag {
    private final int id;
    private final int value;
    private boolean opened; // True if the bag has been opened
    private boolean chosen; // True if this is the player's initially chosen bag

    public Bag(int id, int value) {
        this.id = id;
        this.value = value;
        this.opened = false;
        this.chosen = false;
    }

    public int getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    public boolean isOpened() {
        return opened;
    }

    public void open() {
        this.opened = true;
    }

    public boolean isChosen() {
        return chosen;
    }

    public void setChosen(boolean chosen) {
        this.chosen = chosen;
    }

    @Override
    public String toString() {
        return "Bag " + id + (opened ? " (Opened: $" + value + ")" : (chosen ? " (Your Case)" : ""));
    }
}