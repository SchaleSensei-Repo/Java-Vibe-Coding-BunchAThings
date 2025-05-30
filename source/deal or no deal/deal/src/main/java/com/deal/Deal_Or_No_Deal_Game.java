package com.deal;

import com.deal.gui.GameFrame;

import javax.swing.*;

public class Deal_Or_No_Deal_Game {
    public static void main(String[] args) {
        // Ensure the GUI runs on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new GameFrame().setVisible(true);
        });
    }
}