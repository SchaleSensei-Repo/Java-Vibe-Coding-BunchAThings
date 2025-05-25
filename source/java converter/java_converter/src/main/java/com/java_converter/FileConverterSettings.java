// src/com/example/converter/FileConverterSettings.java
package com.java_converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class FileConverterSettings {

    private static final String SETTINGS_FILE_NAME = "java_converter.ini";
    private final Path settingsFilePath;
    private final Properties properties;

    public FileConverterSettings() {
        this.settingsFilePath = Paths.get(SETTINGS_FILE_NAME);
        this.properties = new Properties();
        loadSettings();
    }

    private void loadSettings() {
        if (Files.exists(settingsFilePath)) {
            try (InputStream is = Files.newInputStream(settingsFilePath)) {
                properties.load(is);
            } catch (IOException e) {
                System.err.println("Error loading settings: " + e.getMessage());
                // Optionally show a GUI error dialog
            }
        }
    }

    public void saveSettings(String inputDir, String outputDir, boolean isJavaToTxtMode) {
        properties.setProperty("inputDir", inputDir);
        properties.setProperty("outputDir", outputDir);
        properties.setProperty("isJavaToTxtMode", String.valueOf(isJavaToTxtMode));

        try (OutputStream os = Files.newOutputStream(settingsFilePath)) {
            properties.store(os, "Java File Converter Settings");
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
            // Optionally show a GUI error dialog
        }
    }

    public String getInputDir() {
        return properties.getProperty("inputDir", ""); // Default empty string
    }

    public String getOutputDir() {
        return properties.getProperty("outputDir", ""); // Default empty string
    }

    public boolean isJavaToTxtMode() {
        return Boolean.parseBoolean(properties.getProperty("isJavaToTxtMode", "true")); // Default true (java to txt)
    }
}