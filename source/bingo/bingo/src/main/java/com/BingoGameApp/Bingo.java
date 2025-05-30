package com.BingoGameApp;

// Bingo.java
import javax.swing.*;
import java.awt.*;

public class Bingo extends JFrame {
    private BingoSettings settings;
    private JPanel currentPanel;

    public Bingo() {
        super("Bingo Game");
        settings = new BingoSettings();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        showSettingsPanel();
    }

    private void showSettingsPanel() {
        if (currentPanel != null) {
            remove(currentPanel);
        }
        currentPanel = new SettingsPanel(settings, this::startGame);
        add(currentPanel);
        revalidate();
        repaint();
    }

    private void startGame(BingoSettings updatedSettings) {
        this.settings = updatedSettings;
        if (currentPanel != null) {
            remove(currentPanel);
        }
        // MODIFIED: Pass a Runnable that will call showSettingsPanel when invoked
        currentPanel = new GamePanel(settings, this::showSettingsPanel);
        add(currentPanel);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Bingo().setVisible(true));
    }
}