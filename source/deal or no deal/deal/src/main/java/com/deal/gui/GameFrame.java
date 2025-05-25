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
        // --- MODIFICATION HERE: Increased width to accommodate side-by-side settings ---
        setSize(1300, 700); // Increased width from 1000 to 1300
        // --- END MODIFICATION ---
        setLocationRelativeTo(null); // Center on screen

        settings = new GameSettings(); // Load settings on startup
        gameLogic = new GameLogic(settings); // Initialize game logic with settings

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        settingsPanel = new SettingsPanel(settings, this);
        gamePanel = new GamePanel(gameLogic, settings, this);

        // Wrap the settingsPanel in a JScrollPane to handle vertical overflow automatically.
        // This is still useful in case one of the columns becomes very tall or on smaller monitors.
        JScrollPane settingsScrollPane = new JScrollPane(settingsPanel);
        settingsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        settingsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // No horizontal scroll needed

        mainPanel.add(settingsScrollPane, "Settings");
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