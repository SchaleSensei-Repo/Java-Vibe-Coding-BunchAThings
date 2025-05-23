package com.deal.gui;

import com.deal.logic.GameLogic;
import com.deal.model.GameSettings;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {
    private GameSettings settings;
    private GameLogic gameLogic;

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private SettingsPanel settingsPanel;
    private GamePanel gamePanel;

    public GameFrame() {
        setTitle("Deal or No Deal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null); // Center on screen

        settings = new GameSettings(); // Load settings on startup
        gameLogic = new GameLogic(settings); // Initialize game logic with settings

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        settingsPanel = new SettingsPanel(settings, this);
        gamePanel = new GamePanel(gameLogic, settings, this);

        mainPanel.add(settingsPanel, "Settings");
        mainPanel.add(gamePanel, "Game");

        add(mainPanel);

        showSettingsPanel(); // Start with settings panel
    }

    public void showSettingsPanel() {
        cardLayout.show(mainPanel, "Settings");
        setTitle("Deal or No Deal - Settings");
    }

    public void showGamePanel() {
        // Ensure game logic is fresh based on potentially changed settings
        gameLogic.startGame();
        gamePanel.updateGUI(); // Refresh game panel with new game state
        cardLayout.show(mainPanel, "Game");
        setTitle("Deal or No Deal - Game In Progress");
    }

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            new GameFrame().setVisible(true);
        });
    }
}