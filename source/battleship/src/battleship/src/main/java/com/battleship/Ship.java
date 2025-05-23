package com.battleship;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Ship {
    private String name;
    private int length;
    private int hits;
    private List<Point> coordinates; // (row, col) tuples
    private boolean sunk;

    public Ship(String name, int length) {
        this.name = name;
        this.length = length;
        this.hits = 0;
        this.coordinates = new ArrayList<>();
        this.sunk = false;
    }

    public boolean hit() {
        this.hits++;
        if (this.hits == this.length) {
            this.sunk = true;
            return true; // Ship sunk
        }
        return false; // Ship hit but not sunk
    }

    public String getName() {
        return name;
    }

    public int getLength() {
        return length;
    }

    public List<Point> getCoordinates() {
        return coordinates;
    }

    public boolean isSunk() {
        return sunk;
    }

    public void addCoordinate(int r, int c) {
        coordinates.add(new Point(r, c));
    }

    @Override
    public String toString() {
        return "Ship(" + name + ", Len:" + length + ", Hits:" + hits + ", Sunk:" + sunk + ")";
    }
}