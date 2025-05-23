package com.deal;

import com.deal.gui.GameFrame;

import javax.swing.*;

public class DealOrNoDealGame {
    public static void main(String[] args) {
        // Ensure the GUI runs on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new GameFrame().setVisible(true);
        });
    }
}