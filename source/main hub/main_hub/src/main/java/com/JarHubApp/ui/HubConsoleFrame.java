package com.JarHubApp.ui;

import javax.swing.*;
import java.awt.*;

public class HubConsoleFrame extends JFrame {
    private JTextArea consoleTextArea;

    public HubConsoleFrame() {
        setTitle("JarHubApp - Hub Console");
        setSize(750, 450);
        setLocationByPlatform(true);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // Don't exit app, just hide

        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        consoleTextArea.setWrapStyleWord(true);
        consoleTextArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(consoleTextArea);
        add(scrollPane, BorderLayout.CENTER);

        // Ensure caret auto-scrolls
        consoleTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCaret(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCaret(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCaret(); }
            private void updateCaret() {
                SwingUtilities.invokeLater(() -> consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength()));
            }
        });
    }

    public JTextArea getTextArea() {
        return consoleTextArea;
    }
}