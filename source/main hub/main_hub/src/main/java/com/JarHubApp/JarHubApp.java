package com.JarHubApp;
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
    private static final int ITEMS_PER_PAGE = 12; // Adjusted for 2 columns (e.g., 6 rows of 2)

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

    // For single instance lock
    private static FileLock lock;
    private static FileChannel channel;
    private static File lockFileHandle;


    public JarHubApp() {
        setTitle("JAR Hub Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 650); // Slightly wider for 2 columns
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
        // --- IMPROVEMENT: Two-column layout ---
        itemsPanel.setLayout(new GridLayout(0, 2, 10, 10)); // 0 rows (dynamic), 2 columns, 10px hgap/vgap
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
                        scanAndDisplayFolder(null); // Clear display
                    }
                } else {
                     scanAndDisplayFolder(null); // No path in settings, clear display
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading settings: " + e.getMessage(),
                        "Settings Error", JOptionPane.ERROR_MESSAGE);
                scanAndDisplayFolder(null); // Error, clear display
            }
        } else {
            currentPathLabel.setText("No settings file found. Please set a folder.");
            scanAndDisplayFolder(null); // No settings, clear display
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
            itemsPanel.setLayout(new BorderLayout()); // Reset to simple layout for message
            itemsPanel.add(new JLabel("Selected folder is invalid or does not exist.", SwingConstants.CENTER), BorderLayout.CENTER);
        } else {
            currentPathLabel.setText("Displaying: " + currentDisplayFolder.getAbsolutePath());
            // --- Ensure GridLayout is set for item display ---
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
        itemsPanel.removeAll(); // Clear before adding new page items

        if (allItemsInCurrentDisplayFolder.isEmpty()) {
            // Ensure layout is suitable for a single message
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
            // Ensure GridLayout is active for item display
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
        if (totalPages == 0) { // No items, no pages to go to
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
            pb.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error running JAR '" + jarFile.getName() + "':\n" + e.getMessage(),
                    "JAR Execution Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
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
            // Resolve to absolute paths to handle relative paths correctly
            Path absoluteMainPath = mainPath.toAbsolutePath().normalize();
            Path absolutePotentialSubpath = potentialSubpath.toAbsolutePath().normalize();
            return absolutePotentialSubpath.startsWith(absoluteMainPath);
        } catch (Exception e) {
            // IO Error or other issues resolving paths
            return false;
        }
    }

    private void updateUpButtonState() {
        if (currentDisplayFolder != null && currentRootFolder != null &&
                !currentDisplayFolder.getAbsolutePath().equals(currentRootFolder.getAbsolutePath()) &&
                 isPathOrSubpath(currentRootFolder.toPath(), currentDisplayFolder.toPath()) // Ensure current is actually under root
            ) {
            upButton.setEnabled(true);
        } else {
            upButton.setEnabled(false);
        }
    }

    private static boolean acquireSingleInstanceLock() {
        // --- IMPROVEMENT: Single Instance Validation ---
        // Use a temporary directory for the lock file for better cross-platform compatibility
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir == null) { // Fallback if temp dir is not available
            tempDir = "."; // Current directory (less ideal)
        }
        lockFileHandle = new File(tempDir, "JarHubApp.lock");

        try {
            // Create a RandomAccessFile and get its channel
            // "rw" mode is required for locking
            channel = new RandomAccessFile(lockFileHandle, "rw").getChannel();

            // Try to acquire an exclusive lock on the file channel
            lock = channel.tryLock();

            if (lock == null) {
                // Lock is held by another instance (or was not obtainable)
                channel.close(); // Close the channel as we couldn't get the lock
                return false;
            }

            // Add a shutdown hook to release the lock and delete the file when the JVM exits
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                releaseSingleInstanceLock();
            }));
            return true; // Lock acquired

        } catch (OverlappingFileLockException e) {
            // This exception means the lock is already held by another process (or this one, if called twice)
            // Log or handle: System.err.println("Lock already held (OverlappingFileLockException): " + e.getMessage());
            try { if (channel != null) channel.close(); } catch (IOException ioe) { /* ignore */ }
            return false;
        } catch (IOException e) {
            // Other IO errors (e.g., permissions, disk full)
            System.err.println("IOException while trying to acquire lock: " + e.getMessage());
            e.printStackTrace();
            try { if (channel != null) channel.close(); } catch (IOException ioe) { /* ignore */ }
            return false; // Could not acquire lock due to IO error
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
            // Optionally delete the lock file, though the OS should release the lock on process end.
            // File deletion ensures a cleaner state if the app is restarted.
            if (lockFileHandle != null && lockFileHandle.exists()) {
                 Files.deleteIfExists(lockFileHandle.toPath());
            }
        } catch (IOException e) {
            System.err.println("Error releasing single instance lock: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        if (!acquireSingleInstanceLock()) {
            JOptionPane.showMessageDialog(null,
                    "Jar Hub Application is already running or the lock file is inaccessible.",
                    "Application Already Running", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                // Nimbus not available, use default.
            }
            new JarHubApp().setVisible(true);
        });
    }
}