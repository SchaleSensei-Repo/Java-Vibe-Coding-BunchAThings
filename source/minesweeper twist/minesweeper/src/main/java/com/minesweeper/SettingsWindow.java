package com.minesweeper;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class SettingsWindow extends JDialog {
    private MinesweeperSettings settings;
    private Runnable newGameCallback;
    // Removed maxBoardDim field as there's no hard limit now

    private JTextField initialPointsField;
    private JTextField boardWidthField;
    private JTextField boardHeightField;

    private JRadioButton minesRandomRadio;
    private JRadioButton minesExactRadio;
    private JTextField minesMinField;
    private JTextField minesMaxField;
    private JTextField minesExactField;

    private JRadioButton mineDiscoveryRandomRadio;
    private JRadioButton mineDiscoveryExactRadio;
    private JTextField mineDiscoveryMinField;
    private JTextField mineDiscoveryMaxField;
    private JTextField mineDiscoveryExactField;

    private JCheckBox ppcEnabledCheckBox;
    private JRadioButton ppcRandomRadio;
    private JRadioButton ppcExactRadio;
    private JTextField ppcMinField;
    private JTextField ppcMaxField;
    private JTextField ppcExactField;

    private JCheckBox ppdtEnabledCheckBox;
    private JRadioButton ppdtRandomRadio;
    private JRadioButton ppdtExactRadio;
    private JTextField ppdtMinField;
    private JTextField ppdtMaxField;
    private JTextField ppdtExactField;

    private JRadioButton flagPlacementRandomRadio;
    private JRadioButton flagPlacementExactRadio;
    private JTextField flagPlacementMinField;
    private JTextField flagPlacementMaxField;
    private JTextField flagPlacementExactField;

    // Constructor no longer takes maxBoardDim
    public SettingsWindow(JFrame parent, MinesweeperSettings settings, Runnable newGameCallback) {
        super(parent, "Game Settings", true); // true makes it modal
        this.settings = settings;
        this.newGameCallback = newGameCallback;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClosing();
            }
        });
        setLayout(new BorderLayout());
        setResizable(false);

        createWidgets();
        loadCurrentSettingsToUI();
        pack(); // Pack components to determine dialog size
        setLocationRelativeTo(parent); // Center relative to parent frame
    }

    private void createWidgets() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Game Settings
        JPanel gameFrame = createTitledPanel("Game Settings");
        gameFrame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gameFrame.add(new JLabel("Initial Points:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; initialPointsField = new JTextField(10); gameFrame.add(initialPointsField, gbc);

        // Updated JLabel text
        gbc.gridx = 0; gbc.gridy = 1; gameFrame.add(new JLabel("Board Width (Positive Integer):"), gbc);
        gbc.gridx = 1; boardWidthField = new JTextField(10); gameFrame.add(boardWidthField, gbc);

        // Updated JLabel text
        gbc.gridx = 0; gbc.gridy = 2; gameFrame.add(new JLabel("Board Height (Positive Integer):"), gbc);
        gbc.gridx = 1; boardHeightField = new JTextField(10); gameFrame.add(boardHeightField, gbc);

        // Mines Settings sub-panel
        JPanel minesFrame = createTitledPanel("Number of Mines");
        minesFrame.setLayout(new GridBagLayout());
        gbc.fill = GridBagConstraints.NONE; // Reset fill for mines frame
        gbc.gridwidth = 1; // Reset gridwidth
        gbc.anchor = GridBagConstraints.WEST;

        ButtonGroup minesModeGroup = new ButtonGroup();
        minesRandomRadio = new JRadioButton("Random");
        minesExactRadio = new JRadioButton("Exact");
        minesModeGroup.add(minesRandomRadio);
        minesModeGroup.add(minesExactRadio);

        minesRandomRadio.addActionListener(e -> toggleMinesFields());
        minesExactRadio.addActionListener(e -> toggleMinesFields());

        gbc.gridx = 0; gbc.gridy = 0; minesFrame.add(minesRandomRadio, gbc);
        gbc.gridx = 1; minesFrame.add(new JLabel("Min:"), gbc);
        gbc.gridx = 2; minesMinField = new JTextField(5); minesFrame.add(minesMinField, gbc);
        gbc.gridx = 3; minesFrame.add(new JLabel("Max:"), gbc);
        gbc.gridx = 4; minesMaxField = new JTextField(5); minesFrame.add(minesMaxField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; minesFrame.add(minesExactRadio, gbc);
        gbc.gridx = 1; minesFrame.add(new JLabel("Exact:"), gbc);
        gbc.gridx = 2; minesExactField = new JTextField(5); minesFrame.add(minesExactField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gameFrame.add(minesFrame, gbc); // Add minesFrame to gameFrame

        mainPanel.add(gameFrame);

        // Scoring Settings
        JPanel scoringFrame = createTitledPanel("Scoring Settings");
        scoringFrame.setLayout(new GridBagLayout());
        gbc.gridwidth = 1; // Reset for scoring
        gbc.fill = GridBagConstraints.NONE;

        // Mine Discovery Points
        addPointsSection(scoringFrame, "Mine Discovery Points", "mine_discovery", 0, null);
        
        // Points Per Click Section
        ppcEnabledCheckBox = new JCheckBox("Points Per Click Enabled");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; scoringFrame.add(ppcEnabledCheckBox, gbc);
        ppcEnabledCheckBox.addActionListener(e -> togglePPCFields());
        addPointsSection(scoringFrame, "", "points_per_click", 4, ppcEnabledCheckBox);

        // Points Per Discovered Tile Section
        ppdtEnabledCheckBox = new JCheckBox("Points Per Discovered Tile Enabled");
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; scoringFrame.add(ppdtEnabledCheckBox, gbc);
        ppdtEnabledCheckBox.addActionListener(e -> togglePPDTFields());
        addPointsSection(scoringFrame, "", "points_per_discovered_tile", 8, ppdtEnabledCheckBox);

        // Flag Placement Points
        addPointsSection(scoringFrame, "Flag Placement Points", "flag_placement", 12, null);

        mainPanel.add(scoringFrame);

        // Action Buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save Settings");
        saveButton.addActionListener(e -> saveSettings());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> onClosing());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private void addPointsSection(JPanel parentFrame, String title, String prefix, int startRow, JCheckBox enabledCheckBox) {
        JPanel frame = createTitledPanel(title);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        ButtonGroup modeGroup = new ButtonGroup();
        JRadioButton randomRadio = new JRadioButton("Random");
        JRadioButton exactRadio = new JRadioButton("Exact");
        modeGroup.add(randomRadio);
        modeGroup.add(exactRadio);

        JTextField minField = new JTextField(5);
        JTextField maxField = new JTextField(5);
        JTextField exactField = new JTextField(5);

        randomRadio.addActionListener(e -> togglePointsFields(prefix, randomRadio, exactRadio, minField, maxField, exactField, enabledCheckBox));
        exactRadio.addActionListener(e -> togglePointsFields(prefix, randomRadio, exactRadio, minField, maxField, exactField, enabledCheckBox));

        // Store references to components for later access
        switch (prefix) {
            case "mine_discovery":
                mineDiscoveryRandomRadio = randomRadio; mineDiscoveryExactRadio = exactRadio;
                mineDiscoveryMinField = minField; mineDiscoveryMaxField = maxField; mineDiscoveryExactField = exactField;
                break;
            case "points_per_click":
                ppcRandomRadio = randomRadio; ppcExactRadio = exactRadio;
                ppcMinField = minField; ppcMaxField = maxField; ppcExactField = exactField;
                break;
            case "points_per_discovered_tile":
                ppdtRandomRadio = randomRadio; ppdtExactRadio = exactRadio;
                ppdtMinField = minField; ppdtMaxField = maxField; ppdtExactField = exactField;
                break;
            case "flag_placement":
                flagPlacementRandomRadio = randomRadio; flagPlacementExactRadio = exactRadio;
                flagPlacementMinField = minField; flagPlacementMaxField = maxField; flagPlacementExactField = exactField;
                break;
        }

        gbc.gridx = 0; gbc.gridy = 0; frame.add(randomRadio, gbc);
        gbc.gridx = 1; frame.add(new JLabel("Min:"), gbc);
        gbc.gridx = 2; frame.add(minField, gbc);
        gbc.gridx = 3; frame.add(new JLabel("Max:"), gbc);
        gbc.gridx = 4; frame.add(maxField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; frame.add(exactRadio, gbc);
        gbc.gridx = 1; frame.add(new JLabel("Exact:"), gbc);
        gbc.gridx = 2; frame.add(exactField, gbc);

        gbc.gridx = 0; gbc.gridy = startRow; gbc.gridwidth = GridBagConstraints.REMAINDER; parentFrame.add(frame, gbc);
    }

    private void loadCurrentSettingsToUI() {
        initialPointsField.setText(settings.getSetting("game.initial_points"));
        boardWidthField.setText(settings.getSetting("game.board_width"));
        boardHeightField.setText(settings.getSetting("game.board_height"));

        String minesMode = settings.getSetting("game.mines_mode");
        if (minesMode.equals("random")) { minesRandomRadio.setSelected(true); } else { minesExactRadio.setSelected(true); }
        minesMinField.setText(settings.getSetting("game.mines_min"));
        minesMaxField.setText(settings.getSetting("game.mines_max"));
        minesExactField.setText(settings.getSetting("game.mines_exact"));

        loadPointsSection("mine_discovery", mineDiscoveryRandomRadio, mineDiscoveryExactRadio, mineDiscoveryMinField, mineDiscoveryMaxField, mineDiscoveryExactField, null);

        ppcEnabledCheckBox.setSelected(settings.getSetting("scoring.points_per_click_enabled", false));
        loadPointsSection("points_per_click", ppcRandomRadio, ppcExactRadio, ppcMinField, ppcMaxField, ppcExactField, ppcEnabledCheckBox);

        ppdtEnabledCheckBox.setSelected(settings.getSetting("scoring.points_per_discovered_tile_enabled", false));
        loadPointsSection("points_per_discovered_tile", ppdtRandomRadio, ppdtExactRadio, ppdtMinField, ppdtMaxField, ppdtExactField, ppdtEnabledCheckBox);

        loadPointsSection("flag_placement", flagPlacementRandomRadio, flagPlacementExactRadio, flagPlacementMinField, flagPlacementMaxField, flagPlacementExactField, null);

        // Call toggle methods to set initial state of enabled/disabled fields
        toggleMinesFields();
        togglePointsFields("mine_discovery", mineDiscoveryRandomRadio, mineDiscoveryExactRadio, mineDiscoveryMinField, mineDiscoveryMaxField, mineDiscoveryExactField, null);
        togglePPCFields();
        togglePPDTFields();
        togglePointsFields("flag_placement", flagPlacementRandomRadio, flagPlacementExactRadio, flagPlacementMinField, flagPlacementMaxField, flagPlacementExactField, null);
    }

    private void loadPointsSection(String prefix, JRadioButton randomRadio, JRadioButton exactRadio,
                                 JTextField minField, JTextField maxField, JTextField exactField, JCheckBox enabledCheckBox) {
        String mode = settings.getSetting("scoring." + prefix + "_mode");
        if (mode.equals("random")) { randomRadio.setSelected(true); } else { exactRadio.setSelected(true); }
        minField.setText(settings.getSetting("scoring." + prefix + "_min"));
        maxField.setText(settings.getSetting("scoring." + prefix + "_max"));
        exactField.setText(settings.getSetting("scoring." + prefix + "_exact"));
    }

    private void toggleMinesFields() {
        boolean isRandom = minesRandomRadio.isSelected();
        minesMinField.setEnabled(isRandom);
        minesMaxField.setEnabled(isRandom);
        minesExactField.setEnabled(!isRandom);
    }

    private void togglePointsFields(String prefix, JRadioButton randomRadio, JRadioButton exactRadio,
                                  JTextField minField, JTextField maxField, JTextField exactField, JCheckBox enabledCheckBox) {
        boolean isEnabled = (enabledCheckBox == null) || enabledCheckBox.isSelected();
        boolean isRandom = randomRadio.isSelected();

        minField.setEnabled(isEnabled && isRandom);
        maxField.setEnabled(isEnabled && isRandom);
        exactField.setEnabled(isEnabled && !isRandom);
    }

    private void togglePPCFields() {
        togglePointsFields("points_per_click", ppcRandomRadio, ppcExactRadio, ppcMinField, ppcMaxField, ppcExactField, ppcEnabledCheckBox);
    }

    private void togglePPDTFields() {
        togglePointsFields("points_per_discovered_tile", ppdtRandomRadio, ppdtExactRadio, ppdtMinField, ppdtMaxField, ppdtExactField, ppdtEnabledCheckBox);
    }

    private void saveSettings() {
        try {
            int initialPoints = Integer.parseInt(initialPointsField.getText());
            settings.setSetting("game.initial_points", String.valueOf(initialPoints));

            int width = Integer.parseInt(boardWidthField.getText());
            int height = Integer.parseInt(boardHeightField.getText());
            
            // Removed hard limit validation for board dimensions
            if (width < 1 || height < 1) {
                throw new IllegalArgumentException("Board width and height must be positive integers.");
            }
            
            settings.setSetting("game.board_width", String.valueOf(width));
            settings.setSetting("game.board_height", String.valueOf(height));

            String minesMode = minesRandomRadio.isSelected() ? "random" : "exact";
            settings.setSetting("game.mines_mode", minesMode);
            
            // Recalculate total cells for validation
            int totalCellsForValidation = width * height;

            if (minesMode.equals("random")) {
                int minesMin = Integer.parseInt(minesMinField.getText());
                int minesMax = Integer.parseInt(minesMaxField.getText());
                if (minesMin < 0 || minesMax < 0) {
                    throw new IllegalArgumentException("Min and Max mines cannot be negative.");
                }
                if (minesMin > minesMax) {
                    throw new IllegalArgumentException("Min mines must be less than or equal to Max mines.");
                }
                // Warning if mines are too high
                if (minesMax >= totalCellsForValidation && totalCellsForValidation > 0) {
                    JOptionPane.showMessageDialog(this, "Maximum mines is very high for the board size. It will be capped at board size - 1 if possible during game generation.", "Warning", JOptionPane.WARNING_MESSAGE);
                }
                settings.setSetting("game.mines_min", String.valueOf(minesMin));
                settings.setSetting("game.mines_max", String.valueOf(minesMax));
            } else {
                int minesExact = Integer.parseInt(minesExactField.getText());
                if (minesExact < 0) {
                    throw new IllegalArgumentException("Exact mines cannot be negative.");
                }
                if (minesExact >= totalCellsForValidation && totalCellsForValidation > 0) {
                     JOptionPane.showMessageDialog(this, "Exact mines is very high for the board size. It will be capped at board size - 1 if possible during game generation.", "Warning", JOptionPane.WARNING_MESSAGE);
                }
                settings.setSetting("game.mines_exact", String.valueOf(minesExact));
            }

            savePointsSection("mine_discovery", mineDiscoveryRandomRadio, mineDiscoveryExactRadio, mineDiscoveryMinField, mineDiscoveryMaxField, mineDiscoveryExactField);
            settings.setSetting("scoring.points_per_click_enabled", String.valueOf(ppcEnabledCheckBox.isSelected()));
            savePointsSection("points_per_click", ppcRandomRadio, ppcExactRadio, ppcMinField, ppcMaxField, ppcExactField);
            settings.setSetting("scoring.points_per_discovered_tile_enabled", String.valueOf(ppdtEnabledCheckBox.isSelected()));
            savePointsSection("points_per_discovered_tile", ppdtRandomRadio, ppdtExactRadio, ppdtMinField, ppdtMaxField, ppdtExactField);
            savePointsSection("flag_placement", flagPlacementRandomRadio, flagPlacementExactRadio, flagPlacementMinField, flagPlacementMaxField, flagPlacementExactField);

            settings.saveSettings(); // Ensure settings are written to file
            dispose(); // Close settings window
            newGameCallback.run(); // Trigger new game in main application

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number format in one of the fields.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void savePointsSection(String prefix, JRadioButton randomRadio, JRadioButton exactRadio,
                                 JTextField minField, JTextField maxField, JTextField exactField) {
        String mode = randomRadio.isSelected() ? "random" : "exact";
        settings.setSetting("scoring." + prefix + "_mode", mode);
        if (mode.equals("random")) {
            int minVal = Integer.parseInt(minField.getText());
            int maxVal = Integer.parseInt(maxField.getText());
            if (minVal > maxVal) {
                throw new IllegalArgumentException("Min value for " + prefix.replace("_", " ") + " must be less than or equal to Max value.");
            }
            settings.setSetting("scoring." + prefix + "_min", String.valueOf(minVal));
            settings.setSetting("scoring." + prefix + "_max", String.valueOf(maxVal));
        } else {
            int exactVal = Integer.parseInt(exactField.getText());
            settings.setSetting("scoring." + prefix + "_exact", String.valueOf(exactVal));
        }
    }

    private void onClosing() {
        dispose(); // Close the dialog
    }
}