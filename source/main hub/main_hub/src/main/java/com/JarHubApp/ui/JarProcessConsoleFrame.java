package com.JarHubApp.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class JarProcessConsoleFrame extends JFrame {
    private final JTextArea consoleTextArea;
    private final String jarName;
    private final File logDirectory;

    private FileOutputStream outputLogStream;
    private FileOutputStream errorLogStream;
    private FileOutputStream mergedLogStream;

    private final boolean doSaveOutput;
    private final boolean doSaveError;
    private final Runnable onDisposeCallback;
    private boolean processHasFinished = false;


    public JarProcessConsoleFrame(String jarName, File logDirectory, boolean saveOutput, boolean saveError, Runnable onDisposeCallback) {
        this.jarName = jarName;
        this.logDirectory = logDirectory;
        this.doSaveOutput = saveOutput;
        this.doSaveError = saveError;
        this.onDisposeCallback = onDisposeCallback;

        setTitle("Console: " + jarName);
        setSize(700, 400);
        setLocationByPlatform(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleTextArea.setWrapStyleWord(true);
        consoleTextArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(consoleTextArea);
        add(scrollPane, BorderLayout.CENTER);

        consoleTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCaret(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCaret(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCaret(); }
            private void updateCaret() {
                SwingUtilities.invokeLater(() -> consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength()));
            }
        });

        setupLogStreams();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeLogStreams();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                if (onDisposeCallback != null) {
                    onDisposeCallback.run();
                }
            }
        });
    }

    private void setupLogStreams() {
        if (!doSaveOutput && !doSaveError) {
            return;
        }

        String baseName = jarName.endsWith(".jar") ? jarName.substring(0, jarName.length() - 4) : jarName;
        try {
            if (doSaveOutput && doSaveError) {
                File logFile = new File(logDirectory, baseName + ".log");
                mergedLogStream = new FileOutputStream(logFile, true);
                appendMessageToConsoleAndLog("[INFO] Logging combined output/error to: " + logFile.getAbsolutePath() + "\n", false);
            } else {
                if (doSaveOutput) {
                    File logFile = new File(logDirectory, baseName + "_output.log");
                    outputLogStream = new FileOutputStream(logFile, true);
                    appendMessageToConsoleAndLog("[INFO] Logging output to: " + logFile.getAbsolutePath() + "\n", false);
                }
                if (doSaveError) {
                    File logFile = new File(logDirectory, baseName + "_error.log");
                    errorLogStream = new FileOutputStream(logFile, true);
                    appendMessageToConsoleAndLog("[INFO] Logging error to: " + logFile.getAbsolutePath() + "\n", true);
                }
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> consoleTextArea.append("[ERROR] Could not open log file: " + e.getMessage() + "\n"));
            System.err.println("JarHubApp: Error opening log file for " + jarName + ": " + e.getMessage());
        }
    }

    public synchronized void appendOutput(String line) {
        if (processHasFinished && !this.isVisible()) return; // Don't append if already marked finished and hidden
        String textToLog = line + "\n";
        String consoleText = line + "\n";
        if (mergedLogStream != null) {
            textToLog = "[OUT] " + line + "\n";
        }
        
        final String finalConsoleText = consoleText;
        SwingUtilities.invokeLater(() -> consoleTextArea.append(finalConsoleText));

        writeToStream(textToLog, mergedLogStream != null ? mergedLogStream : outputLogStream);
    }

    public synchronized void appendError(String line) {
        if (processHasFinished && !this.isVisible()) return;
        String textToLog = line + "\n";
        String consoleText = line + "\n";
        if (mergedLogStream != null) {
            textToLog = "[ERR] " + line + "\n";
        }

        final String finalConsoleText = consoleText;
        SwingUtilities.invokeLater(() -> consoleTextArea.append(finalConsoleText));
        
        writeToStream(textToLog, mergedLogStream != null ? mergedLogStream : errorLogStream);
    }
    
    private void appendMessageToConsoleAndLog(String message, boolean isError) {
        SwingUtilities.invokeLater(() -> consoleTextArea.append(message));
        if (isError) {
            writeToStream(message, mergedLogStream != null ? mergedLogStream : errorLogStream);
        } else {
            writeToStream(message, mergedLogStream != null ? mergedLogStream : outputLogStream);
        }
    }

    private void writeToStream(String text, OutputStream stream) {
        if (stream != null) {
            try {
                stream.write(text.getBytes(StandardCharsets.UTF_8));
                stream.flush();
            } catch (IOException e) {
                System.err.println("JarHubApp: Error writing to JAR log for " + jarName + ": " + e.getMessage());
            }
        }
    }

    public void processFinished(int exitCode) {
        processHasFinished = true;
        String message = "\n--- Process " + jarName + " finished with exit code: " + exitCode + " ---\n";
        appendMessageToConsoleAndLog(message, exitCode != 0);
        // Log streams are closed by windowClosing event triggered by dispose()
        setTitle("Console: " + jarName + " (Finished: " + exitCode + ")");
    }

    private void closeLogStreams() {
        try { if (outputLogStream != null) { outputLogStream.close(); outputLogStream = null;} } catch (IOException e) {e.printStackTrace();}
        try { if (errorLogStream != null) { errorLogStream.close(); errorLogStream = null;} } catch (IOException e) {e.printStackTrace();}
        try { if (mergedLogStream != null) { mergedLogStream.close(); mergedLogStream = null;} } catch (IOException e) {e.printStackTrace();}
    }

    public boolean hasProcessFinished() {
        return processHasFinished;
    }
}