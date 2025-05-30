package com.JarHubApp;

import com.JarHubApp.io.TextAreaOutputStream;
import com.JarHubApp.io.TeeOutputStream;
import com.JarHubApp.ui.HubConsoleFrame;
import com.JarHubApp.ui.JarProcessConsoleFrame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

public class Jar_Main_Hub extends JFrame {

    private static final String SETTINGS_FILE = "hub_setting.ini";
    private static final String FOLDER_PATH_KEY = "folderPath";
    private static final String HUB_LOG_OUTPUT_KEY = "logHubOutput";
    private static final String HUB_LOG_ERROR_KEY = "logHubError";
    private static final String SHOW_HUB_CONSOLE_KEY = "showHubConsole";
    private static final String ATTACH_CONSOLE_TO_JARS_KEY = "attachConsoleToJars";
    private static final String SAVE_JAR_OUTPUT_KEY = "saveJarOutput";
    private static final String SAVE_JAR_ERROR_KEY = "saveJarError";

    // Window Size/Location Persistence Keys
    private static final String WINDOW_X_KEY = "windowX";
    private static final String WINDOW_Y_KEY = "windowY";
    private static final String WINDOW_WIDTH_KEY = "windowWidth";
    private static final String WINDOW_HEIGHT_KEY = "windowHeight";


    private static final int ITEMS_PER_ROW = 2;
    private static final int MAX_ROWS_PER_PAGE = 10;
    private static final int ITEMS_PER_PAGE = ITEMS_PER_ROW * MAX_ROWS_PER_PAGE;

    // Reverted UI Sizing (closer to 2 prompts ago)
    private static final int DEFAULT_FRAME_WIDTH = 850;
    private static final int DEFAULT_FRAME_HEIGHT = 750; 
    private static final int ITEM_BUTTON_WIDTH = 320; 
    private static final int ITEM_BUTTON_HEIGHT = 35;
    private static final int ITEM_PANEL_HGAP = 5; 
    private static final int ITEM_PANEL_VGAP = 10;


    private Properties settings;
    private File currentRootFolder;
    private File currentDisplayFolder;

    private JPanel itemsPanel;
    private JPanel navigationPanel;
    private JPanel settingsPanelNorth;
    private JPanel settingsPanelSouth;
    private JLabel currentPathLabel;
    private JLabel pageInfoLabel;

    private List<File> allItemsInCurrentDisplayFolder;
    private int currentPage = 0;
    private int totalPages = 0;

    private JButton firstPageButton, prevPageButton, nextPageButton, lastPageButton, upButton;

    private JCheckBox logHubOutputCheckBox;
    private JCheckBox logHubErrorCheckBox;
    private JCheckBox showHubConsoleCheckBox;
    private static PrintStream originalSystemOut;
    private static PrintStream originalSystemErr;
    private static FileOutputStream hubCombinedLogStream;
    private static FileOutputStream hubOutputOnlyLogStream;
    private static FileOutputStream hubErrorOnlyLogStream;
    private static HubConsoleFrame hubConsoleFrame;

    private JCheckBox attachConsoleToJarsCheckBox;
    private JCheckBox saveJarOutputCheckBox;
    private JCheckBox saveJarErrorCheckBox;
    private final List<JarProcessConsoleFrame> activeJarConsoles = Collections.synchronizedList(new ArrayList<>());

    private static FileLock lock;
    private static FileChannel channel;
    private static File lockFileHandle;

    public Jar_Main_Hub() {
        setTitle("JAR Hub Application");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Load window size/location settings BEFORE setting size/location
        settings = new Properties(); // Initialize settings early for window size
        loadWindowPreferences(); // Loads X, Y, Width, Height

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Jar_Main_Hub: Main window closing. Cleaning up...");
                saveWindowPreferences(); // Save window size/location on close

                List<JarProcessConsoleFrame> consolesToClose = new ArrayList<>(activeJarConsoles);
                for (JarProcessConsoleFrame jarConsole : consolesToClose) {
                    if (jarConsole != null && jarConsole.isDisplayable()) {
                        System.out.println("Jar_Main_Hub: Disposing console for " + jarConsole.getTitle());
                        jarConsole.dispose();
                    }
                }
                try { TimeUnit.MILLISECONDS.sleep(200); } catch (InterruptedException interruptedException) { Thread.currentThread().interrupt(); }
                activeJarConsoles.clear();

                if (hubConsoleFrame != null && hubConsoleFrame.isDisplayable()) {
                    System.out.println("Jar_Main_Hub: Disposing Hub console.");
                    hubConsoleFrame.dispose();
                }

                closeHubLogStreams();
                releaseSingleInstanceLock();
                System.out.println("Jar_Main_Hub: Exiting.");
                System.exit(0);
            }
        });
        // Set initial size from defaults or loaded preferences
        // setSize() is handled in loadWindowPreferences or defaults if not found

        //setLocationRelativeTo(null); // This will be overridden if location is saved
        setLayout(new BorderLayout(10, 10)); // Main border layout gaps

        // settings = new Properties(); // Already initialized
        allItemsInCurrentDisplayFolder = new ArrayList<>();

        initComponents();
        loadSettings();   // Loads other app settings (folder, logging options)
        
        System.out.println("Jar_Main_Hub: Constructor finished.");
    }

    private void loadWindowPreferences() {
        File settingsFile = new File(SETTINGS_FILE);
        int x = -1, y = -1, width = DEFAULT_FRAME_WIDTH, height = DEFAULT_FRAME_HEIGHT;
        boolean locationSet = false;

        if (settingsFile.exists()) {
            try (InputStream input = new FileInputStream(settingsFile)) {
                settings.load(input); // Load all settings once
                try {
                    width = Integer.parseInt(settings.getProperty(WINDOW_WIDTH_KEY, String.valueOf(DEFAULT_FRAME_WIDTH)));
                    height = Integer.parseInt(settings.getProperty(WINDOW_HEIGHT_KEY, String.valueOf(DEFAULT_FRAME_HEIGHT)));
                    x = Integer.parseInt(settings.getProperty(WINDOW_X_KEY, "-1"));
                    y = Integer.parseInt(settings.getProperty(WINDOW_Y_KEY, "-1"));
                    if (x != -1 && y != -1) {
                        locationSet = true;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Jar_Main_Hub: Error parsing window size/location from settings. Using defaults. " + e.getMessage());
                    // Defaults are already set
                }
            } catch (IOException e) {
                System.err.println("Jar_Main_Hub: Error loading settings file for window prefs: " + e.getMessage());
                // Defaults will be used
            }
        }
        setSize(width, height);
        if (locationSet) {
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null); // Center if no location saved
        }
    }

    private void saveWindowPreferences() {
        if (settings == null) settings = new Properties(); // Should not happen if constructor is correct
        Point location = getLocation();
        Dimension size = getSize();
        settings.setProperty(WINDOW_X_KEY, String.valueOf(location.x));
        settings.setProperty(WINDOW_Y_KEY, String.valueOf(location.y));
        settings.setProperty(WINDOW_WIDTH_KEY, String.valueOf(size.width));
        settings.setProperty(WINDOW_HEIGHT_KEY, String.valueOf(size.height));
        // The rest of the settings are saved in saveSettings() if needed.
        // This call ensures window prefs are written along with other settings if saveSettings is called.
        // However, settings are typically saved when options change or on specific actions.
        // For window close, we specifically want to save window state.
        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            settings.store(output, "JAR Hub Settings");
            System.out.println("Jar_Main_Hub: Window preferences saved.");
        } catch (IOException e) {
            System.err.println("Jar_Main_Hub: Error saving window preferences: " + e.getMessage());
        }
    }


    private void initComponents() {
        settingsPanelNorth = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5)); 
        JButton setFolderButton = new JButton("Set Root Folder");
        currentPathLabel = new JLabel("No folder selected.");
        settingsPanelNorth.add(setFolderButton);
        settingsPanelNorth.add(currentPathLabel);
        add(settingsPanelNorth, BorderLayout.NORTH);

        setFolderButton.addActionListener(e -> selectAndSetRootFolder());

        itemsPanel = new JPanel();
        itemsPanel.setLayout(new GridLayout(MAX_ROWS_PER_PAGE, ITEMS_PER_ROW, ITEM_PANEL_HGAP, ITEM_PANEL_VGAP));
        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        settingsPanelSouth = new JPanel(new GridLayout(0, 1, 5, 5)); 
        settingsPanelSouth.setBorder(BorderFactory.createEmptyBorder(5,5,5,5)); 

        JPanel hubLoggingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2)); 
        hubLoggingPanel.setBorder(BorderFactory.createTitledBorder("Hub Logging & Console"));
        logHubOutputCheckBox = new JCheckBox("Log Hub Output");
        logHubErrorCheckBox = new JCheckBox("Log Hub Error");
        showHubConsoleCheckBox = new JCheckBox("Show Hub Console");

        logHubOutputCheckBox.addActionListener(e -> { saveSettings(); setupHubLogging(); });
        logHubErrorCheckBox.addActionListener(e -> { saveSettings(); setupHubLogging(); });
        showHubConsoleCheckBox.addActionListener(e -> {
            saveSettings(); 
            setupHubLogging(); 
        });

        hubLoggingPanel.add(logHubOutputCheckBox);
        hubLoggingPanel.add(logHubErrorCheckBox);
        hubLoggingPanel.add(showHubConsoleCheckBox);
        settingsPanelSouth.add(hubLoggingPanel);

        JPanel jarDebugPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2)); 
        jarDebugPanel.setBorder(BorderFactory.createTitledBorder("JAR Execution Options"));
        attachConsoleToJarsCheckBox = new JCheckBox("Attach Console to launched JARs");
        saveJarOutputCheckBox = new JCheckBox("Save JAR Output");
        saveJarErrorCheckBox = new JCheckBox("Save JAR Error");
        attachConsoleToJarsCheckBox.addActionListener(e -> saveSettings());
        saveJarOutputCheckBox.addActionListener(e -> saveSettings());
        saveJarErrorCheckBox.addActionListener(e -> saveSettings());
        jarDebugPanel.add(attachConsoleToJarsCheckBox);
        jarDebugPanel.add(saveJarOutputCheckBox);
        jarDebugPanel.add(saveJarErrorCheckBox);
        settingsPanelSouth.add(jarDebugPanel);
        
        JPanel bottomOuterPanel = new JPanel(new BorderLayout(0,5)); 
        navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 3)); 
        firstPageButton = new JButton("<< First");
        prevPageButton = new JButton("< Prev");
        pageInfoLabel = new JLabel("Page 0 of 0");
        nextPageButton = new JButton("Next >");
        lastPageButton = new JButton("Last >>");
        upButton = new JButton("Up One Level");

        Dimension navButtonSize = new Dimension(85, 28); // Slightly larger nav buttons
        firstPageButton.setPreferredSize(navButtonSize);
        prevPageButton.setPreferredSize(navButtonSize);
        nextPageButton.setPreferredSize(navButtonSize);
        lastPageButton.setPreferredSize(navButtonSize);
        upButton.setPreferredSize(new Dimension(120, 28));


        firstPageButton.addActionListener(e -> goToPage(0));
        prevPageButton.addActionListener(e -> goToPage(currentPage - 1));
        nextPageButton.addActionListener(e -> goToPage(currentPage + 1));
        lastPageButton.addActionListener(e -> goToPage(totalPages - 1));
        upButton.addActionListener(e -> goUpOneLevel());

        navigationPanel.add(upButton);
        navigationPanel.add(firstPageButton);
        navigationPanel.add(prevPageButton);
        navigationPanel.add(pageInfoLabel);
        navigationPanel.add(nextPageButton);
        navigationPanel.add(lastPageButton);
        
        bottomOuterPanel.add(settingsPanelSouth, BorderLayout.NORTH);
        bottomOuterPanel.add(navigationPanel, BorderLayout.CENTER);
        add(bottomOuterPanel, BorderLayout.SOUTH);
        System.out.println("Jar_Main_Hub: initComponents finished.");
    }

    private void loadSettings() {
        // Window preferences are loaded separately in loadWindowPreferences()
        // This method loads other application-specific settings.
        // `settings` Properties object should already be loaded by loadWindowPreferences.
        if (settings.isEmpty()) { // If settings file didn't exist or was empty
            File settingsFile = new File(SETTINGS_FILE);
            if (settingsFile.exists()) {
                try (InputStream input = new FileInputStream(settingsFile)) {
                    settings.load(input);
                } catch (IOException e) {
                     System.err.println("Jar_Main_Hub: Error reloading settings in loadSettings: " + e.getMessage());
                }
            }
        }

        String folderPath = settings.getProperty(FOLDER_PATH_KEY);
        if (folderPath != null && !folderPath.isEmpty()) {
            File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
                this.currentRootFolder = folder;
                this.currentDisplayFolder = folder;
                currentPathLabel.setText("Root: " + currentRootFolder.getAbsolutePath());
                scanAndDisplayFolder(currentDisplayFolder);
            } else {
                currentPathLabel.setText("Saved path not found: " + folderPath);
                this.currentRootFolder = null;
                this.currentDisplayFolder = null;
                scanAndDisplayFolder(null);
            }
        } else {
            // Don't overwrite currentPathLabel if it was set by scanAndDisplayFolder from a valid folderPath
            if (this.currentRootFolder == null) {
                 currentPathLabel.setText("No root folder selected.");
                 scanAndDisplayFolder(null);
            }
        }

        logHubOutputCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty(HUB_LOG_OUTPUT_KEY, "false")));
        logHubErrorCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty(HUB_LOG_ERROR_KEY, "false")));
        showHubConsoleCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty(SHOW_HUB_CONSOLE_KEY, "false")));
        System.out.println("Jar_Main_Hub: Loaded SHOW_HUB_CONSOLE_KEY in loadSettings: " + showHubConsoleCheckBox.isSelected());

        attachConsoleToJarsCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty(ATTACH_CONSOLE_TO_JARS_KEY, "false")));
        saveJarOutputCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty(SAVE_JAR_OUTPUT_KEY, "false")));
        saveJarErrorCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty(SAVE_JAR_ERROR_KEY, "false")));
        
        updateUpButtonState();
        System.out.println("Jar_Main_Hub: loadSettings (app-specific) finished.");
    }

    private void saveSettings() {
        // This saves application-specific settings. Window preferences are saved by saveWindowPreferences().
        if (currentRootFolder != null) {
            settings.setProperty(FOLDER_PATH_KEY, currentRootFolder.getAbsolutePath());
        }
        settings.setProperty(HUB_LOG_OUTPUT_KEY, String.valueOf(logHubOutputCheckBox.isSelected()));
        settings.setProperty(HUB_LOG_ERROR_KEY, String.valueOf(logHubErrorCheckBox.isSelected()));
        settings.setProperty(SHOW_HUB_CONSOLE_KEY, String.valueOf(showHubConsoleCheckBox.isSelected())); 
        settings.setProperty(ATTACH_CONSOLE_TO_JARS_KEY, String.valueOf(attachConsoleToJarsCheckBox.isSelected()));
        settings.setProperty(SAVE_JAR_OUTPUT_KEY, String.valueOf(saveJarOutputCheckBox.isSelected()));
        settings.setProperty(SAVE_JAR_ERROR_KEY, String.valueOf(saveJarErrorCheckBox.isSelected()));
        System.out.println("Jar_Main_Hub: Saving app-specific settings. SHOW_HUB_CONSOLE_KEY: " + showHubConsoleCheckBox.isSelected());

        // Do not write to file here. Let saveWindowPreferences handle the actual write on close,
        // or if another part of the app needs to force a full settings save.
        // Forcing a write here for every minor setting change can be inefficient.
        // However, to ensure changes are persisted if app crashes before clean exit:
        // try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
        //    settings.store(output, "JAR Hub Settings");
        // } catch (IOException e) {
        //    JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage(), "Settings Error", JOptionPane.ERROR_MESSAGE);
        // }
    }

    private synchronized void setupHubLogging() {
        System.out.println("Jar_Main_Hub: setupHubLogging called. Checkbox state: " + showHubConsoleCheckBox.isSelected());
        closeHubLogStreamsInternal();

        boolean logOutput = logHubOutputCheckBox.isSelected();
        boolean logError = logHubErrorCheckBox.isSelected();
        boolean showConsole = showHubConsoleCheckBox.isSelected();

        List<OutputStream> currentOutTargets = new ArrayList<>();
        if (originalSystemOut != null) currentOutTargets.add(originalSystemOut);
        
        List<OutputStream> currentErrTargets = new ArrayList<>();
        if (originalSystemErr != null) currentErrTargets.add(originalSystemErr);

        if (showConsole) {
            if (hubConsoleFrame == null) {
                System.out.println("Jar_Main_Hub: HubConsoleFrame is null in setupHubLogging, creating new one.");
                hubConsoleFrame = new HubConsoleFrame();
            }
            TextAreaOutputStream consoleOutStream = new TextAreaOutputStream(hubConsoleFrame.getTextArea());
            currentOutTargets.add(consoleOutStream);
            currentErrTargets.add(consoleOutStream);

            // Visibility is handled in main() for initial launch,
            // and by checkbox listener for subsequent changes.
            // This ensures it's visible if checkbox is checked.
            if (hubConsoleFrame != null && !hubConsoleFrame.isVisible() && (this.isDisplayable() || SwingUtilities.isEventDispatchThread())) {
                SwingUtilities.invokeLater(() -> {
                    if (hubConsoleFrame != null && showHubConsoleCheckBox.isSelected() && !hubConsoleFrame.isVisible()) {
                         System.out.println("Jar_Main_Hub: Making HubConsoleFrame visible via setupHubLogging (because checkbox is selected).");
                         hubConsoleFrame.setVisible(true);
                    }
                });
            }
        } else {
            if (hubConsoleFrame != null && hubConsoleFrame.isVisible()) {
                System.out.println("Jar_Main_Hub: Making HubConsoleFrame invisible via setupHubLogging.");
                SwingUtilities.invokeLater(() -> hubConsoleFrame.setVisible(false));
            }
        }

        try {
            if (logOutput && logError) {
                hubCombinedLogStream = new FileOutputStream("hub.log", true);
                currentOutTargets.add(hubCombinedLogStream);
                currentErrTargets.add(hubCombinedLogStream);
            } else {
                if (logOutput) {
                    hubOutputOnlyLogStream = new FileOutputStream("hub_output.log", true);
                    currentOutTargets.add(hubOutputOnlyLogStream);
                }
                if (logError) {
                    hubErrorOnlyLogStream = new FileOutputStream("hub_error.log", true);
                    currentErrTargets.add(hubErrorOnlyLogStream);
                }
            }
        } catch (IOException e) {
            String errorMsg = "Jar_Main_Hub: Error setting up hub log file: " + e.getMessage();
            if (originalSystemErr != null) originalSystemErr.println(errorMsg); else System.err.println(errorMsg);
            if (this.isVisible()) {
                 JOptionPane.showMessageDialog(this, "Error setting up hub log file: " + e.getMessage(), "Logging Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        System.setOut(new PrintStream(new TeeOutputStream(currentOutTargets), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new TeeOutputStream(currentErrTargets), true, StandardCharsets.UTF_8));
        System.out.println("Jar_Main_Hub: Hub logging (re)configured. Output targets: " + currentOutTargets.size() + ", Error targets: " + currentErrTargets.size());
    }

    private static void closeHubLogStreamsInternal() {
        try { if (hubCombinedLogStream != null) { hubCombinedLogStream.close(); hubCombinedLogStream = null; } } catch (IOException e) { (originalSystemErr != null ? originalSystemErr : System.err).println("Error closing hub.log: " + e.getMessage());}
        try { if (hubOutputOnlyLogStream != null) { hubOutputOnlyLogStream.close(); hubOutputOnlyLogStream = null; } } catch (IOException e) { (originalSystemErr != null ? originalSystemErr : System.err).println("Error closing hub_output.log: " + e.getMessage());}
        try { if (hubErrorOnlyLogStream != null) { hubErrorOnlyLogStream.close(); hubErrorOnlyLogStream = null; } } catch (IOException e) { (originalSystemErr != null ? originalSystemErr : System.err).println("Error closing hub_error.log: " + e.getMessage());}
    }
    
    private static void closeHubLogStreams() {
        System.out.println("Jar_Main_Hub: Closing hub log streams initiated...");
        closeHubLogStreamsInternal();
    }

    private void selectAndSetRootFolder() {
        JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        if (currentRootFolder != null && currentRootFolder.exists()) {
            fileChooser.setCurrentDirectory(currentRootFolder);
        }
        fileChooser.setDialogTitle("Select Root Folder for JARs and Subfolders");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            this.currentRootFolder = selectedFolder;
            this.currentDisplayFolder = selectedFolder;
            currentPathLabel.setText("Root: " + currentRootFolder.getAbsolutePath());
            saveSettings(); // Save folder path
            saveWindowPreferences(); // And current window state, as an action was performed
            scanAndDisplayFolder(currentDisplayFolder);
        }
        updateUpButtonState();
    }

    private void scanAndDisplayFolder(File folderToScan) {
        this.currentDisplayFolder = folderToScan;
        allItemsInCurrentDisplayFolder.clear();
        itemsPanel.removeAll();

        if (currentRootFolder == null) {
             currentPathLabel.setText("No root folder set. Please set one.");
        } else if (folderToScan == null || !folderToScan.exists() || !folderToScan.isDirectory()) {
            currentPathLabel.setText("Displaying: Invalid Path");
            itemsPanel.setLayout(new BorderLayout());
            itemsPanel.add(new JLabel("Selected folder is invalid or does not exist.", SwingConstants.CENTER), BorderLayout.CENTER);
        } else {
            currentPathLabel.setText("Displaying: " + currentDisplayFolder.getAbsolutePath());
            if (!(itemsPanel.getLayout() instanceof GridLayout) ||
                ((GridLayout)itemsPanel.getLayout()).getRows() != MAX_ROWS_PER_PAGE ||
                ((GridLayout)itemsPanel.getLayout()).getColumns() != ITEMS_PER_ROW) {
                itemsPanel.setLayout(new GridLayout(MAX_ROWS_PER_PAGE, ITEMS_PER_ROW, ITEM_PANEL_HGAP, ITEM_PANEL_VGAP));
            }

            File[] files = folderToScan.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() || (file.isFile() && file.getName().toLowerCase().endsWith(".jar"))) {
                        allItemsInCurrentDisplayFolder.add(file);
                    }
                }
                allItemsInCurrentDisplayFolder.sort(Comparator
                        .comparing(File::isDirectory, Comparator.reverseOrder())
                        .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            }
        }

        currentPage = 0;
        updatePaginationControls();
        displayCurrentPage();
        updateUpButtonState();
        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private void updatePaginationControls() {
        if (allItemsInCurrentDisplayFolder.isEmpty()) {
            totalPages = 0;
        } else {
            totalPages = (int) Math.ceil((double) allItemsInCurrentDisplayFolder.size() / ITEMS_PER_PAGE);
        }
        if (totalPages == 0) currentPage = 0;

        pageInfoLabel.setText(String.format("Page %d of %d", currentPage + 1, Math.max(1, totalPages)));

        firstPageButton.setEnabled(currentPage > 0 && totalPages > 1);
        prevPageButton.setEnabled(currentPage > 0 && totalPages > 1);
        nextPageButton.setEnabled(currentPage < totalPages - 1 && totalPages > 1);
        lastPageButton.setEnabled(currentPage < totalPages - 1 && totalPages > 1);
    }

    private void displayCurrentPage() {
        itemsPanel.removeAll(); 

        if (!(itemsPanel.getLayout() instanceof GridLayout) ||
            ((GridLayout)itemsPanel.getLayout()).getRows() != MAX_ROWS_PER_PAGE ||
            ((GridLayout)itemsPanel.getLayout()).getColumns() != ITEMS_PER_ROW) {
            itemsPanel.setLayout(new GridLayout(MAX_ROWS_PER_PAGE, ITEMS_PER_ROW, ITEM_PANEL_HGAP, ITEM_PANEL_VGAP));
        }

        int itemsAddedThisPage = 0;
        if (currentDisplayFolder != null && allItemsInCurrentDisplayFolder.isEmpty() && itemsPanel.getLayout() instanceof GridLayout) {
            String message = "No JARs or subfolders found in this directory.";
            JPanel messageCell = new JPanel(new FlowLayout(FlowLayout.CENTER));
            messageCell.add(new JLabel(message, SwingConstants.CENTER));
            itemsPanel.add(messageCell);
            itemsAddedThisPage = 1;
        } else if (!allItemsInCurrentDisplayFolder.isEmpty()) {
            int startIndex = currentPage * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItemsInCurrentDisplayFolder.size());
            for (int i = startIndex; i < endIndex; i++) {
                File item = allItemsInCurrentDisplayFolder.get(i);
                JButton itemButton = new JButton();
                itemButton.setHorizontalAlignment(SwingConstants.LEFT);
                itemButton.setPreferredSize(new Dimension(ITEM_BUTTON_WIDTH, ITEM_BUTTON_HEIGHT));

                if (item.isDirectory()) {
                    itemButton.setText("📁 " + item.getName());
                    itemButton.setToolTipText("Open folder: " + item.getName());
                    itemButton.addActionListener(e -> scanAndDisplayFolder(item));
                } else if (item.isFile() && item.getName().toLowerCase().endsWith(".jar")) {
                    itemButton.setText("📦 " + item.getName());
                    itemButton.setToolTipText("Run JAR: " + item.getName());
                    itemButton.addActionListener(e -> runJar(item));
                }
                
                JPanel cellWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                cellWrapper.add(itemButton);
                itemsPanel.add(cellWrapper);
                itemsAddedThisPage++;
            }
        }

        for (int i = itemsAddedThisPage; i < ITEMS_PER_PAGE; i++) {
            itemsPanel.add(new JPanel()); 
        }

        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private void goToPage(int pageNumber) {
        if (totalPages == 0) {
            currentPage = 0;
        } else if (pageNumber < 0) {
            currentPage = 0;
        } else if (pageNumber >= totalPages) {
            currentPage = totalPages - 1;
        } else {
            currentPage = pageNumber;
        }
        updatePaginationControls();
        displayCurrentPage();
    }

    private void runJar(File jarFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath());
            pb.directory(jarFile.getParentFile()); 

            System.out.println("Jar_Main_Hub: Attempting to launch JAR: " + jarFile.getAbsolutePath());
            System.out.println("Jar_Main_Hub: Working directory for JAR: " + (jarFile.getParentFile() != null ? jarFile.getParentFile().getAbsolutePath() : "null"));

            boolean attachConsole = attachConsoleToJarsCheckBox.isSelected();
            boolean saveOutput = saveJarOutputCheckBox.isSelected();
            boolean saveError = saveJarErrorCheckBox.isSelected();

            JarProcessConsoleFrame consoleFrame = null;
            if (attachConsole || saveOutput || saveError) {
                final JarProcessConsoleFrame[] consoleFrameHolder = new JarProcessConsoleFrame[1];
                Runnable onDisposeCallback = () -> {
                    if (consoleFrameHolder[0] != null) {
                        activeJarConsoles.remove(consoleFrameHolder[0]);
                        System.out.println("Jar_Main_Hub: Removed console for " + consoleFrameHolder[0].getTitle() + " from active list.");
                    }
                };
                
                consoleFrame = new JarProcessConsoleFrame(
                        jarFile.getName(),
                        jarFile.getParentFile(),
                        saveOutput,
                        saveError,
                        onDisposeCallback);
                consoleFrameHolder[0] = consoleFrame;
                
                activeJarConsoles.add(consoleFrame);
                System.out.println("Jar_Main_Hub: Added console for " + jarFile.getName() + " to active list. Total: " + activeJarConsoles.size());

                if (attachConsole) {
                    consoleFrame.setVisible(true);
                }
            }

            final Process process = pb.start();
            final JarProcessConsoleFrame finalConsoleFrame = consoleFrame;

            new Thread(() -> {
                try (InputStream inputStream = process.getInputStream();
                     java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (finalConsoleFrame != null) {
                            finalConsoleFrame.appendOutput(line);
                        } else {
                            if (originalSystemOut != null) originalSystemOut.println("[Child OUT " + jarFile.getName() + "] " + line);
                        }
                    }
                } catch (IOException e) {
                     if (originalSystemErr != null) originalSystemErr.println("Jar_Main_Hub: IOException while reading output stream of " + jarFile.getName() + ": " + e.getMessage());
                }
            }, "JarOut-" + jarFile.getName()).start();

            new Thread(() -> {
                try (InputStream errorStream = process.getErrorStream();
                     java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (finalConsoleFrame != null) {
                            finalConsoleFrame.appendError(line);
                        } else {
                            if (originalSystemErr != null) originalSystemErr.println("[Child ERR " + jarFile.getName() + "] " + line);
                        }
                    }
                } catch (IOException e) {
                    if (originalSystemErr != null) originalSystemErr.println("Jar_Main_Hub: IOException while reading error stream of " + jarFile.getName() + ": " + e.getMessage());
                }
            }, "JarErr-" + jarFile.getName()).start();

            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    System.out.println("Jar_Main_Hub: " + jarFile.getName() + " (PID: " + process.pid() + ") exited with code " + exitCode);
                    if (finalConsoleFrame != null) {
                        finalConsoleFrame.processFinished(exitCode);
                        if (attachConsoleToJarsCheckBox.isSelected() && finalConsoleFrame.isDisplayable()) {
                           SwingUtilities.invokeLater(finalConsoleFrame::dispose);
                        }
                    }
                } catch (InterruptedException e) {
                    System.err.println("Jar_Main_Hub: Interrupted while waiting for " + jarFile.getName() + " to exit.");
                    if (finalConsoleFrame != null) {
                        finalConsoleFrame.appendError("Jar_Main_Hub: Monitoring interrupted for " + jarFile.getName());
                        finalConsoleFrame.processFinished(-1);
                         if (attachConsoleToJarsCheckBox.isSelected() && finalConsoleFrame.isDisplayable()) {
                           SwingUtilities.invokeLater(finalConsoleFrame::dispose);
                        }
                    }
                    Thread.currentThread().interrupt(); 
                }
            }, "JarMon-" + jarFile.getName()).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error running JAR '" + jarFile.getName() + "':\n" + e.getMessage(), "JAR Execution Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Jar_Main_Hub: IOException on starting JAR " + jarFile.getName() + ":");
            e.printStackTrace(originalSystemErr != null ? originalSystemErr : System.err);
        }
    }

    private void goUpOneLevel() {
        if (currentDisplayFolder != null && currentRootFolder != null &&
                !currentDisplayFolder.equals(currentRootFolder)) {
            File parent = currentDisplayFolder.getParentFile();
            if (parent != null && isPathOrSubpath(currentRootFolder.toPath(), parent.toPath())) {
                scanAndDisplayFolder(parent);
            } else {
                scanAndDisplayFolder(currentRootFolder);
            }
        } else if (currentRootFolder != null) {
            scanAndDisplayFolder(currentRootFolder);
        }
        updateUpButtonState();
    }

    private boolean isPathOrSubpath(Path mainPath, Path potentialSubpath) {
        if (mainPath == null || potentialSubpath == null) return false;
        try {
            Path absoluteMainPath = mainPath.toAbsolutePath().normalize();
            Path absolutePotentialSubpath = potentialSubpath.toAbsolutePath().normalize();
            return absolutePotentialSubpath.startsWith(absoluteMainPath);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateUpButtonState() {
        if (currentDisplayFolder != null && currentRootFolder != null &&
                !currentDisplayFolder.getAbsolutePath().equals(currentRootFolder.getAbsolutePath()) &&
                 isPathOrSubpath(currentRootFolder.toPath(), currentDisplayFolder.toPath())
            ) {
            upButton.setEnabled(true);
        } else {
            upButton.setEnabled(false);
        }
    }

    static boolean acquireSingleInstanceLock() {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir == null) tempDir = ".";
        lockFileHandle = new File(tempDir, "Jar_Main_Hub.lock");

        try {
            channel = new RandomAccessFile(lockFileHandle, "rw").getChannel();
            lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                return false;
            }
             Runtime.getRuntime().addShutdownHook(new Thread(Jar_Main_Hub::releaseSingleInstanceLockOnly));
            return true;
        } catch (OverlappingFileLockException e) {
            try { if (channel != null) channel.close(); } catch (IOException ioe) { /* ignore */ }
            return false;
        } catch (IOException e) {
            (originalSystemErr != null ? originalSystemErr : System.err).println("IOException while trying to acquire lock: " + e.getMessage());
            try { if (channel != null) channel.close(); } catch (IOException ioe) { /* ignore */ }
            return false;
        }
    }
    
    static void releaseSingleInstanceLockOnly() {
         try {
            if (lock != null && lock.isValid()) lock.release();
            if (channel != null && channel.isOpen()) channel.close();
            if (lockFileHandle != null && lockFileHandle.exists()) Files.deleteIfExists(lockFileHandle.toPath());
        } catch (IOException e) {
            (originalSystemErr != null ? originalSystemErr : System.err).println("Error releasing single instance lock: " + e.getMessage());
        }
    }

    private static void releaseSingleInstanceLock() {
        // saveWindowPreferences() is called by windowClosing handler
        closeHubLogStreams();
        releaseSingleInstanceLockOnly();
    }

    public static void main(String[] args) {
        originalSystemOut = System.out; 
        originalSystemErr = System.err;
        System.out.println("Jar_Main_Hub: Application starting...");

        boolean cmdLineShowHubConsole = false;
        for (String arg : args) {
            if ("--console".equalsIgnoreCase(arg)) {
                cmdLineShowHubConsole = true;
                System.out.println("Jar_Main_Hub: --console argument detected.");
                break;
            }
        }

        if (!acquireSingleInstanceLock()) {
            final String message = "Jar Hub Application is already running or the lock file is inaccessible.";
            (originalSystemErr != null ? originalSystemErr : System.err).println(message);
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(null, message, "Application Already Running", JOptionPane.WARNING_MESSAGE)
            );
            System.exit(1);
            return;
        }
        
        final boolean finalCmdLineShowHubConsole = cmdLineShowHubConsole;
        SwingUtilities.invokeLater(() -> {
            System.out.println("Jar_Main_Hub: SwingUtilities.invokeLater in main running.");
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                 (originalSystemErr != null ? originalSystemErr : System.err).println("Nimbus L&F not available: " + e.getMessage());
            }
            
            // Create Jar_Main_Hub instance. Constructor calls loadWindowPreferences(), then initComponents(), then loadSettings().
            Jar_Main_Hub app = new Jar_Main_Hub(); 

            // Logic for Hub Console visibility based on args and settings
            boolean makeHubConsoleVisibleThisSession = app.showHubConsoleCheckBox.isSelected(); // Start with loaded setting
            if (finalCmdLineShowHubConsole) {
                makeHubConsoleVisibleThisSession = true; // --console overrides setting to true
                if (!app.showHubConsoleCheckBox.isSelected()) {
                    app.showHubConsoleCheckBox.setSelected(true); // Update checkbox to reflect override
                    // app.saveSettings(); // Optional: persist --console action
                }
            }
            
            if (makeHubConsoleVisibleThisSession) {
                if (hubConsoleFrame == null) { // Ensure it's created if needed
                    hubConsoleFrame = new HubConsoleFrame();
                }
                System.out.println("Jar_Main_Hub: Showing Hub Console BEFORE main app window.");
                hubConsoleFrame.setVisible(true); // Show Hub console first
            }

            // Now setup all logging, which will include hubConsoleFrame if it was made visible
            app.setupHubLogging(); 
            
            // Finally, show the main application window
            app.setVisible(true);
            System.out.println("Jar_Main_Hub: Main application window set to visible.");
        });
    }
}