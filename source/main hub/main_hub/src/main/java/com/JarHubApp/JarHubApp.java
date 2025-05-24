package com.JarHubApp; // Assuming this is your package name

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

public class JarHubApp extends JFrame {

    private static final String SETTINGS_FILE = "hub_setting.ini";
    private static final String FOLDER_PATH_KEY = "folderPath";
    private static final int ITEMS_PER_PAGE = 12;

    private Properties settings;
    private File currentRootFolder;
    private File currentDisplayFolder;

    private JPanel itemsPanel;
    private JPanel navigationPanel;
    private JPanel settingsPanel;
    private JLabel currentPathLabel;
    private JLabel pageInfoLabel;

    private List<File> allItemsInCurrentDisplayFolder;
    private int currentPage = 0;
    private int totalPages = 0;

    private JButton firstPageButton, prevPageButton, nextPageButton, lastPageButton, upButton;

    private static FileLock lock;
    private static FileChannel channel;
    private static File lockFileHandle;


    public JarHubApp() {
        setTitle("JAR Hub Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        settings = new Properties();
        allItemsInCurrentDisplayFolder = new ArrayList<>();

        initComponents();
        loadSettings();
    }

    private void initComponents() {
        settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton setFolderButton = new JButton("Set Root Folder");
        currentPathLabel = new JLabel("No folder selected.");
        settingsPanel.add(setFolderButton);
        settingsPanel.add(currentPathLabel);
        add(settingsPanel, BorderLayout.NORTH);

        setFolderButton.addActionListener(e -> selectAndSetRootFolder());

        itemsPanel = new JPanel();
        itemsPanel.setLayout(new GridLayout(0, 2, 10, 10));
        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        firstPageButton = new JButton("<< First");
        prevPageButton = new JButton("< Prev");
        pageInfoLabel = new JLabel("Page 0 of 0");
        nextPageButton = new JButton("Next >");
        lastPageButton = new JButton("Last >>");
        upButton = new JButton("Up One Level");

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
        add(navigationPanel, BorderLayout.SOUTH);
    }

    private void loadSettings() {
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            try (InputStream input = new FileInputStream(settingsFile)) {
                settings.load(input);
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
                     scanAndDisplayFolder(null);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading settings: " + e.getMessage(),
                        "Settings Error", JOptionPane.ERROR_MESSAGE);
                scanAndDisplayFolder(null);
            }
        } else {
            currentPathLabel.setText("No settings file found. Please set a folder.");
            scanAndDisplayFolder(null);
        }
        updateUpButtonState();
    }

    private void saveSettings() {
        if (currentRootFolder != null) {
            settings.setProperty(FOLDER_PATH_KEY, currentRootFolder.getAbsolutePath());
            try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
                settings.store(output, "JAR Hub Settings");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage(),
                        "Settings Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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
            saveSettings();
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
            itemsPanel.setLayout(new GridLayout(0, 2, 10, 10));

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

        if (allItemsInCurrentDisplayFolder.isEmpty()) {
            if (!(itemsPanel.getLayout() instanceof BorderLayout)) {
                itemsPanel.setLayout(new BorderLayout());
            }
            String message = "No JARs or subfolders found.";
            if (currentDisplayFolder == null && currentRootFolder == null) {
                message = "Please set a root folder.";
            } else if (currentDisplayFolder == null) {
                message = "Cannot display items: current folder is not set or invalid.";
            }
            itemsPanel.add(new JLabel(message, SwingConstants.CENTER), BorderLayout.CENTER);
        } else {
             if (!(itemsPanel.getLayout() instanceof GridLayout) || ((GridLayout)itemsPanel.getLayout()).getColumns() != 2) {
                itemsPanel.setLayout(new GridLayout(0, 2, 10, 10));
            }
            int startIndex = currentPage * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItemsInCurrentDisplayFolder.size());
            for (int i = startIndex; i < endIndex; i++) {
                File item = allItemsInCurrentDisplayFolder.get(i);
                JButton itemButton = new JButton();
                itemButton.setHorizontalAlignment(SwingConstants.LEFT);
                if (item.isDirectory()) {
                    itemButton.setText("📁 " + item.getName());
                    itemButton.setToolTipText("Open folder: " + item.getName());
                    itemButton.addActionListener(e -> scanAndDisplayFolder(item));
                } else if (item.isFile() && item.getName().toLowerCase().endsWith(".jar")) {
                    itemButton.setText("📦 " + item.getName());
                    itemButton.setToolTipText("Run JAR: " + item.getName());
                    itemButton.addActionListener(e -> runJar(item));
                }
                itemsPanel.add(itemButton);
            }
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

    // --- REVISED runJar METHOD ---
    private void runJar(File jarFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath());
            pb.directory(jarFile.getParentFile()); // Set working directory for the JAR

            // Log before starting
            System.out.println("JarHubApp: Attempting to launch JAR: " + jarFile.getAbsolutePath());
            System.out.println("JarHubApp: Working directory for JAR: " + (jarFile.getParentFile() != null ? jarFile.getParentFile().getAbsolutePath() : "null"));

            final Process process = pb.start(); // Start the process

            // Asynchronously consume the child process's output stream
            new Thread(() -> {
                // Using try-with-resources for automatic closing of streams
                try (InputStream inputStream = process.getInputStream();
                     java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
                    String line;
                    // Log to JarHubApp's console (if JarHubApp is run from a console)
                    System.out.println("--- Output from " + jarFile.getName() + " (PID: " + process.pid() + ") ---");
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Child OUT " + jarFile.getName() + "] " + line);
                    }
                    System.out.println("--- End Output from " + jarFile.getName() + " ---");
                } catch (IOException e) {
                    // This error is for JarHubApp's reading, not the child process itself
                    // System.err.println("JarHubApp: IOException while reading output stream of " + jarFile.getName() + ": " + e.getMessage());
                }
            }).start();

            // Asynchronously consume the child process's error stream
            new Thread(() -> {
                try (InputStream errorStream = process.getErrorStream();
                     java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream))) {
                    String line;
                    System.err.println("--- Error from " + jarFile.getName() + " (PID: " + process.pid() + ") ---");
                    while ((line = reader.readLine()) != null) {
                        System.err.println("[Child ERR " + jarFile.getName() + "] " + line);
                    }
                    System.err.println("--- End Error from " + jarFile.getName() + " ---");
                } catch (IOException e) {
                    // System.err.println("JarHubApp: IOException while reading error stream of " + jarFile.getName() + ": " + e.getMessage());
                }
            }).start();

            // Optional: Monitor process completion
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    System.out.println("JarHubApp: " + jarFile.getName() + " (PID: " + process.pid() + ") exited with code " + exitCode);
                } catch (InterruptedException e) {
                    System.err.println("JarHubApp: Interrupted while waiting for " + jarFile.getName() + " to exit.");
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error running JAR '" + jarFile.getName() + "':\n" + e.getMessage(),
                    "JAR Execution Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("JarHubApp: IOException on starting JAR " + jarFile.getName() + ":");
            e.printStackTrace(); // Print stack trace for JarHubApp's error
        }
    }
    // --- END REVISED runJar METHOD ---

    private void goUpOneLevel() {
        if (currentDisplayFolder != null && currentRootFolder != null &&
                !currentDisplayFolder.equals(currentRootFolder)) {
            File parent = currentDisplayFolder.getParentFile();
            if (parent != null && isPathOrSubpath(currentRootFolder.toPath(), parent.toPath())) {
                scanAndDisplayFolder(parent);
            } else {
                // If parent is null or not under root, go back to root
                scanAndDisplayFolder(currentRootFolder);
            }
        } else if (currentRootFolder != null) {
            // If already at root or currentDisplayFolder is null, ensure we display root
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

    private static boolean acquireSingleInstanceLock() {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir == null) {
            tempDir = ".";
        }
        lockFileHandle = new File(tempDir, "JarHubApp.lock");

        try {
            channel = new RandomAccessFile(lockFileHandle, "rw").getChannel();
            lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                return false;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(JarHubApp::releaseSingleInstanceLock));
            return true;
        } catch (OverlappingFileLockException e) {
            try { if (channel != null) channel.close(); } catch (IOException ioe) { /* ignore */ }
            return false;
        } catch (IOException e) {
            System.err.println("IOException while trying to acquire lock: " + e.getMessage());
            // e.printStackTrace(); // Usually too verbose for normal operation
            try { if (channel != null) channel.close(); } catch (IOException ioe) { /* ignore */ }
            return false;
        }
    }

    private static void releaseSingleInstanceLock() {
        try {
            if (lock != null && lock.isValid()) {
                lock.release();
            }
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (lockFileHandle != null && lockFileHandle.exists()) {
                 Files.deleteIfExists(lockFileHandle.toPath());
            }
        } catch (IOException e) {
            System.err.println("Error releasing single instance lock: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (!acquireSingleInstanceLock()) {
            JOptionPane.showMessageDialog(null,
                    "Jar Hub Application is already running or the lock file is inaccessible.",
                    "Application Already Running", JOptionPane.WARNING_MESSAGE);
            System.exit(0); // Exit if another instance is running
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                // Attempt to set Nimbus Look and Feel for a more modern appearance
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                // If Nimbus is not available, the default L&F will be used.
                // You might want to log this error or just ignore it.
                // System.err.println("Nimbus L&F not available: " + e.getMessage());
            }
            new JarHubApp().setVisible(true);
        });
    }
}