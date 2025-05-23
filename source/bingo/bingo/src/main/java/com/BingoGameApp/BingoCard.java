package com.BingoGameApp;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class BingoCard extends JPanel {
    private static final int CARD_SIZE = 5;
    private JButton[][] cells;
    private int[][] numbers;
    private boolean[][] marked;
    private int minNum, maxNum;

    private Set<String> completedBingoLines;

    public BingoCard(int minNum, int maxNum) {
        this.minNum = minNum;
        this.maxNum = maxNum;
        setLayout(new GridLayout(CARD_SIZE, CARD_SIZE));
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        cells = new JButton[CARD_SIZE][CARD_SIZE];
        numbers = new int[CARD_SIZE][CARD_SIZE];
        marked = new boolean[CARD_SIZE][CARD_SIZE];
        completedBingoLines = new HashSet<>();

        generateCardNumbers();
        createCardGUI();
    }

    private void generateCardNumbers() {
        List<Integer> availableNumbers = new ArrayList<>();
        for (int i = minNum; i <= maxNum; i++) {
            availableNumbers.add(i);
        }
        Collections.shuffle(availableNumbers);

        if (availableNumbers.size() < CARD_SIZE * CARD_SIZE) {
            JOptionPane.showMessageDialog(this,
                "Not enough unique numbers in the specified range (" + minNum + "-" + maxNum + ") for a 5x5 card. Please adjust settings.",
                "Card Generation Warning", JOptionPane.WARNING_MESSAGE);
        }

        for (int r = 0; r < CARD_SIZE; r++) {
            for (int c = 0; c < CARD_SIZE; c++) {
                if (availableNumbers.size() > 0) {
                    numbers[r][c] = availableNumbers.remove(0);
                } else {
                    numbers[r][c] = 0;
                }
                marked[r][c] = false;
            }
        }
    }

    private void createCardGUI() {
        for (int r = 0; r < CARD_SIZE; r++) {
            for (int c = 0; c < CARD_SIZE; c++) {
                JButton cell = new JButton(String.valueOf(numbers[r][c]));
                cell.setFont(new Font("Arial", Font.BOLD, 18));
                cell.setBackground(Color.LIGHT_GRAY);
                cell.setOpaque(true);
                cell.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                cells[r][c] = cell;
                add(cell);
            }
        }
    }

    public boolean markNumber(int num) {
        for (int r = 0; r < CARD_SIZE; r++) {
            for (int c = 0; c < CARD_SIZE; c++) {
                if (numbers[r][c] == num && !marked[r][c]) {
                    marked[r][c] = true;
                    if (!cells[r][c].getBackground().equals(new Color(0, 150, 0))) {
                         cells[r][c].setBackground(Color.GREEN);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public int getNumberAt(int r, int c) {
        if (r >= 0 && r < CARD_SIZE && c >= 0 && c < CARD_SIZE) {
            return numbers[r][c];
        }
        return -1;
    }

    public JButton getCellButton(int r, int c) {
        if (r >= 0 && r < CARD_SIZE && c >= 0 && c < CARD_SIZE) {
            return cells[r][c];
        }
        return null;
    }

    public boolean isMarked(int r, int c) {
        if (r >= 0 && r < CARD_SIZE && c >= 0 && c < CARD_SIZE) {
            return marked[r][c];
        }
        return false;
    }

    public List<List<Point>> checkAndGetNewBingoLines() {
        List<List<Point>> newlyAchievedLines = new ArrayList<>();

        // Check Rows
        for (int r = 0; r < CARD_SIZE; r++) {
            String lineId = "R" + r;
            if (!completedBingoLines.contains(lineId)) {
                boolean bingoRow = true;
                List<Point> linePoints = new ArrayList<>();
                for (int c = 0; c < CARD_SIZE; c++) {
                    linePoints.add(new Point(r, c));
                    if (!marked[r][c]) {
                        bingoRow = false;
                        break;
                    }
                }
                if (bingoRow) {
                    completedBingoLines.add(lineId);
                    newlyAchievedLines.add(linePoints);
                }
            }
        }

        // Check Columns
        for (int c = 0; c < CARD_SIZE; c++) {
            String lineId = "C" + c;
            if (!completedBingoLines.contains(lineId)) {
                boolean bingoCol = true;
                List<Point> linePoints = new ArrayList<>();
                for (int r = 0; r < CARD_SIZE; r++) {
                    linePoints.add(new Point(r, c));
                    if (!marked[r][c]) {
                        bingoCol = false;
                        break;
                    }
                }
                if (bingoCol) {
                    completedBingoLines.add(lineId);
                    newlyAchievedLines.add(linePoints);
                }
            }
        }

        // Check Diagonal 1 (top-left to bottom-right)
        String diag1Id = "D1";
        if (!completedBingoLines.contains(diag1Id)) {
            boolean bingoDiag1 = true;
            List<Point> linePoints = new ArrayList<>();
            for (int i = 0; i < CARD_SIZE; i++) {
                linePoints.add(new Point(i, i));
                if (!marked[i][i]) {
                    bingoDiag1 = false;
                    break;
                }
            }
            if (bingoDiag1) {
                completedBingoLines.add(diag1Id);
                newlyAchievedLines.add(linePoints);
            }
        }

        // Check Diagonal 2 (top-right to bottom-left)
        String diag2Id = "D2";
        if (!completedBingoLines.contains(diag2Id)) {
            boolean bingoDiag2 = true;
            List<Point> linePoints = new ArrayList<>();
            for (int i = 0; i < CARD_SIZE; i++) {
                linePoints.add(new Point(i, CARD_SIZE - 1 - i));
                if (!marked[i][CARD_SIZE - 1 - i]) {
                    bingoDiag2 = false;
                    break;
                }
            }
            if (bingoDiag2) {
                completedBingoLines.add(diag2Id);
                newlyAchievedLines.add(linePoints);
            }
        }
        return newlyAchievedLines;
    }

    public void highlightLine(List<Point> line, Color color) {
        for (Point p : line) {
            if (p.x >= 0 && p.x < CARD_SIZE && p.y >= 0 && p.y < CARD_SIZE) {
                cells[p.x][p.y].setBackground(color);
            }
        }
    }

    public int getBingoCount() {
        return completedBingoLines.size();
    }

    public void reset() {
        completedBingoLines.clear();
        
        for (int r = 0; r < CARD_SIZE; r++) {
            for (int c = 0; c < CARD_SIZE; c++) {
                marked[r][c] = false;
                cells[r][c].setBackground(Color.LIGHT_GRAY);
            }
        }
        
        generateCardNumbers();
        
        for (int r = 0; r < CARD_SIZE; r++) {
            for (int c = 0; c < CARD_SIZE; c++) {
                cells[r][c].setText(String.valueOf(numbers[r][c]));
            }
        }
    }
}