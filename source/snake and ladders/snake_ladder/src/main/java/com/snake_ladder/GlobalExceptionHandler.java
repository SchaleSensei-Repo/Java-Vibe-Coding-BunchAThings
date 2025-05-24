package com.snake_ladder; // Or your package

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GlobalExceptionHandler implements UncaughtExceptionHandler {

    private static final String LOG_FILE_NAME = "snake_ladder_errors.log";

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        // Log to a file
        logExceptionToFile(t, e);

        // Optionally, show a dialog to the user (but be careful,
        // if the EDT is dead, this dialog might not show or might also freeze)
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        // Try to show a dialog, but this might fail if EDT is compromised
        try {
            String message = "An unexpected error occurred:\n" + e.getMessage() +
                             "\n\nPlease check " + LOG_FILE_NAME + " for details.";
            if (SwingUtilities.isEventDispatchThread() || t.getName().startsWith("AWT-EventQueue")) {
                 // If already on EDT, or it's an EDT thread, show dialog directly (though it might be risky)
                JOptionPane.showMessageDialog(null, message + "\n\n" + stackTrace.substring(0, Math.min(stackTrace.length(), 1000)), "Critical Error", JOptionPane.ERROR_MESSAGE);
            } else {
                // If not on EDT, try to schedule dialog on EDT
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, message, "Critical Error", JOptionPane.ERROR_MESSAGE)
                );
            }
        } catch (Throwable dialogError) {
            System.err.println("Error showing error dialog: " + dialogError.getMessage());
            // Fallback if even showing dialog fails
        }

        // For critical EDT errors, it might be best to exit
        if (t.getName().startsWith("AWT-EventQueue")) {
            System.err.println("Exiting due to critical EDT error.");
            // System.exit(1); // Uncomment to force exit on EDT error
        }
    }

    private void logExceptionToFile(Thread t, Throwable e) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE_NAME, true))) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            writer.println("------------------------------------------------------");
            writer.println("Timestamp: " + sdf.format(new Date()));
            writer.println("Thread: " + t.getName() + " (ID: " + t.getId() + ")");
            writer.println("Exception: " + e.getClass().getName());
            writer.println("Message: " + e.getMessage());
            writer.println("Stack Trace:");
            e.printStackTrace(writer);
            writer.println("------------------------------------------------------");
            writer.println();
        } catch (IOException ioex) {
            System.err.println("Could not write to error log file: " + ioex.getMessage());
            e.printStackTrace(); // Print to stderr as a fallback
        }
    }

    public static void register() {
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
        // For Swing applications, it's also good to set this property for the EDT specifically
        System.setProperty("sun.awt.exception.handler", GlobalExceptionHandler.class.getName());
    }
}