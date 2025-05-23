package com.battleship;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Board {
    public static final int WATER = 0;
    public static final int SHIP = 1;
    public static final int HIT = 2; // Hit ship
    public static final int MISS = 3; // Missed shot

    private int rows;
    private int cols;
    private int[][] grid; // This is private
    private List<Ship> ships; // List of Ship objects

    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new int[rows][cols];
        this.ships = new ArrayList<>();
        initializeGrid();
    }

    private void initializeGrid() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = WATER;
            }
        }
    }

    // New method to allow setting cell state externally
    public void setCellState(int r, int c, int state) {
        if (r >= 0 && r < rows && c >= 0 && c < cols) {
            this.grid[r][c] = state;
        }
    }

    public boolean placeShip(Ship ship, int startRow, int startCol, String orientation) {
        // orientation: "H" for horizontal, "V" for vertical
        if (orientation.equals("H")) {
            if (startCol + ship.getLength() > cols) {
                return false; // Out of bounds
            }
            for (int c = 0; c < ship.getLength(); c++) {
                if (grid[startRow][startCol + c] != WATER) {
                    return false; // Overlap
                }
            }
            for (int c = 0; c < ship.getLength(); c++) {
                grid[startRow][startCol + c] = SHIP;
                ship.addCoordinate(startRow, startCol + c);
            }
        } else if (orientation.equals("V")) {
            if (startRow + ship.getLength() > rows) {
                return false; // Out of bounds
            }
            for (int r = 0; r < ship.getLength(); r++) {
                if (grid[startRow + r][startCol] != WATER) {
                    return false; // Overlap
                }
            }
            for (int r = 0; r < ship.getLength(); r++) {
                grid[startRow + r][startCol] = SHIP;
                ship.addCoordinate(startRow + r, startCol);
            }
        } else {
            return false; // Invalid orientation
        }

        this.ships.add(ship);
        return true;
    }

    public boolean autoPlaceShips(Map<String, GameSettings.ShipConfig> shipConfigs) {
        initializeGrid(); // Reset board
        ships.clear();

        List<Map.Entry<String, GameSettings.ShipConfig>> shuffledShipTypes = new ArrayList<>(shipConfigs.entrySet());
        Collections.shuffle(shuffledShipTypes); // Randomize placement order of ship types

        Random random = new Random();
        List<String> orientations = List.of("H", "V"); // Java 9+ for List.of

        for (Map.Entry<String, GameSettings.ShipConfig> shipTypeEntry : shuffledShipTypes) {
            String name = shipTypeEntry.getKey();
            int length = shipTypeEntry.getValue().length();
            int quantity = shipTypeEntry.getValue().quantity();

            if (length <= 0) continue; // Skip ships with non-positive length

            for (int i = 0; i < quantity; i++) {
                boolean placed = false;
                int attempts = 0;
                while (!placed && attempts < 1000) { // Prevent infinite loop
                    Ship ship = new Ship(name, length);
                    String orientation = orientations.get(random.nextInt(orientations.size()));
                    int row, col;

                    if (orientation.equals("H")) {
                        if (length > cols) { // Ship too long for board
                            System.out.println("Skipping " + name + ": length " + length + " > board cols " + cols);
                            break; // Break inner loop, cannot place this ship
                        }
                        row = random.nextInt(rows);
                        col = random.nextInt(cols - length + 1);
                    } else { // "V"
                        if (length > rows) { // Ship too tall for board
                            System.out.println("Skipping " + name + ": length " + length + " > board rows " + rows);
                            break; // Break inner loop, cannot place this ship
                        }
                        row = random.nextInt(rows - length + 1);
                        col = random.nextInt(cols);
                    }

                    placed = placeShip(ship, row, col, orientation);
                    attempts++;
                }
                if (!placed) {
                    System.out.println("Warning: Could not place ship " + name + " (length " + length + ") on a " + rows + "x" + cols + " board after " + attempts + " attempts. Board might be too small or too crowded.");
                    // If even one ship fails to place, the entire placement attempt is considered a failure
                    return false; 
                }
            }
        }
        return true; // All ships placed successfully
    }

    public Object[] fireAt(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return new Object[]{"Invalid coordinates", null}; // Invalid shot
        }

        int currentState = grid[row][col];
        if (currentState == HIT || currentState == MISS) {
            return new Object[]{"Already shot here", null}; // Already shot at this location
        }

        if (currentState == SHIP) {
            grid[row][col] = HIT;
            // Find which ship was hit
            Ship hitShip = null;
            for (Ship ship : ships) {
                for (Point coord : ship.getCoordinates()) {
                    if (coord.x == row && coord.y == col) {
                        hitShip = ship;
                        break;
                    }
                }
                if (hitShip != null) break;
            }
            
            if (hitShip != null) {
                if (hitShip.hit()) { // Check if ship sunk
                    return new Object[]{"Hit and Sunk " + hitShip.getName() + "!", hitShip};
                } else {
                    return new Object[]{"Hit!", hitShip};
                }
            }
        } else if (currentState == WATER) {
            grid[row][col] = MISS;
            return new Object[]{"Miss!", null};
        }

        return new Object[]{"Error in firing logic", null};
    }

    public boolean allShipsSunk() {
        if (ships.isEmpty()) { // If there are no ships on the board (e.g. all removed via settings), then they are all "sunk"
            return true;
        }
        for (Ship ship : ships) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return true;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getCellState(int r, int c) {
        return grid[r][c];
    }

    public List<Ship> getShips() {
        return ships;
    }
}