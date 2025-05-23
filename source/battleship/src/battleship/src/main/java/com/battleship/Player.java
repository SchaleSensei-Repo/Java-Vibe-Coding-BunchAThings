package com.battleship;
import java.util.Map;
import java.util.TreeMap;

public class Player {
    private String name;
    private GameSettings settings;
    private Board ownBoard;
    private Board trackingBoard;
    private int shotsFired;
    private int hitsMade;
    private int missesMade;

    public Player(String name, GameSettings settings) {
        this.name = name;
        this.settings = settings;
        this.ownBoard = new Board(settings.boardRows, settings.boardCols);
        this.trackingBoard = new Board(settings.boardRows, settings.boardCols);
        this.shotsFired = 0;
        this.hitsMade = 0;
        this.missesMade = 0;

        initializeBoards();
    }

    private void initializeBoards() {
        if (!ownBoard.autoPlaceShips(settings.shipConfigs)) {
            // This case should ideally be caught by the GUI before player initialization
            // but is here as a fallback or for non-GUI usage
            throw new RuntimeException("Ship placement failed for " + name + "!");
        }
    }

    public Object[] takeShot(Board opponentBoard, int row, int col) {
        Object[] result = opponentBoard.fireAt(row, col);
        String resultMsg = (String) result[0];
        Ship hitShip = (Ship) result[1];

        // Only increment shots if it was a valid new shot (not already shot here or out of bounds)
        if (!"Invalid coordinates".equals(resultMsg) && !"Already shot here".equals(resultMsg) && !"Error in firing logic".equals(resultMsg)) {
            this.shotsFired++; // Increment total shots fired
        }

        if (resultMsg.equals("Hit!") || resultMsg.startsWith("Hit and Sunk")) {
            trackingBoard.setCellState(row, col, Board.HIT); // FIX: Use setter method
            this.hitsMade++; // Increment hits
        } else if (resultMsg.equals("Miss!")) {
            trackingBoard.setCellState(row, col, Board.MISS); // FIX: Use setter method
            this.missesMade++; // Increment misses
        }
        return result;
    }

    public int getShotsPerTurn() {
        if (settings.dynamicFires) {
            return (int) ownBoard.getShips().stream().filter(s -> !s.isSunk()).count(); // Number of own unsunk ships
        } else {
            return settings.firesPerTurn;
        }
    }

    public String getName() {
        return name;
    }

    public Board getOwnBoard() {
        return ownBoard;
    }

    public Board getTrackingBoard() {
        return trackingBoard;
    }

    public int getShotsFired() {
        return shotsFired;
    }

    public int getHitsMade() {
        return hitsMade;
    }

    public int getMissesMade() {
        return missesMade;
    }

    public String getUnsunkShipsInfo() {
        Map<String, Integer> shipCounts = new TreeMap<>(); // TreeMap to keep them sorted by name
        ownBoard.getShips().stream()
                .filter(s -> !s.isSunk())
                .forEach(s -> shipCounts.merge(s.getName().replace("_", " ").toLowerCase(), 1, Integer::sum));

        if (shipCounts.isEmpty()) {
            return "None";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : shipCounts.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(" (").append(entry.getValue()).append(")");
        }
        return sb.toString();
    }

    public int getUnsunkShipCount() {
        return (int) ownBoard.getShips().stream().filter(s -> !s.isSunk()).count();
    }
}