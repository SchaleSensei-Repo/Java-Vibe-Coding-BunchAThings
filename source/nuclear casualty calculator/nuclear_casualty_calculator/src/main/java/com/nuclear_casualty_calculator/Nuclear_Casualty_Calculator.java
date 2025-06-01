package com.nuclear_casualty_calculator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Nuclear_Casualty_Calculator extends JFrame {

    // --- Input Fields ---
    private JTextField populationField;
    private JRadioButton warheadInputRadio, radiationInputRadio;
    private JTextField warheadQuantityField, warheadPayloadField;
    private JRadioButton strikeAllAtOnceRadio, strikePeriodicRadio;
    private JTextField periodicWarheadsInitialField, periodicYearsField;
    private JTextField radiationLevelField;
    private JCheckBox nuclearWinterCheckbox;
    private JCheckBox humanitarianAidCheckbox;

    // --- Panels for layout ---
    private JPanel warheadPanel;
    private JPanel periodicStrikePanel;
    private JPanel radiationPanel;

    // --- Output ---
    private JTextArea resultsArea;

    // --- Constants for settings file ---
    private static final String SETTINGS_FILE = "nuclear_settings.ini";
    private static final DecimalFormat df = new DecimalFormat("#,##0");
    private static final DecimalFormat pf = new DecimalFormat("0.00%");
    private static final DecimalFormat doseRateFormat = new DecimalFormat("#,##0.000");

    private static final double EFFECTIVE_MSV_PER_MT_PROMPT_LOCAL_MODEL_PARAM = 250; 
    private static final double FALLOUT_RATE_PER_MT_AT_H1_FACTOR = 1000; 
    private static final double HYPOTHETICAL_EXPOSURE_RATE_FOR_ACUTE_TABLE = 1000; 

    private static final double EMERGENCY_DOSE_LIMIT_FOR_MAX_EXPOSURE_COLUMN_MSV = 100.0;
    private static final double EVAC_DOSE_LIMIT_PREPARED_MSV = 100.0;
    private static final double EVAC_DURATION_PREPARED_HOURS = 3.0;
    private static final double EVAC_DOSE_LIMIT_UNPREPARED_MSV = 50.0;
    private static final double EVAC_DURATION_UNPREPARED_HOURS = 2.0;
    private static final double REDUCED_RISK_OUTDOOR_ACTIVITY_RATE_MSV_HR = 0.01;

    // Preference for showing startup disclaimer
    private boolean prefShowStartupDisclaimer = true;


    public Nuclear_Casualty_Calculator() {
        setTitle("Nuclear Casualty Estimator (Illustrative Model)");

        // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Keep this or use a WindowListener

    // More explicit shutdown handling
        addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent windowEvent) {
            // Perform any cleanup here if necessary (e.g., save final data)
            System.out.println("Window closing event triggered. Attempting System.exit(0)...");
            dispose();
            System.exit(0); // Force exit
        }
        });

        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 960); 
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents(); 
        loadSettings(); 
        
        showDisclaimerDialog(false); 
    }

    private void showDisclaimerDialog(boolean isLaunchedFromAboutMenu) {
        // If it's a startup call AND the preference is to NOT show it, then return.
        if (!isLaunchedFromAboutMenu && !prefShowStartupDisclaimer) {
            return; 
        }

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        String disclaimerText = "<html><body style='width: 350px;'>"
            + "<b>Disclaimer & Warning:</b><br><br>"
            + "This Nuclear Casualty Estimator is a highly simplified and illustrative model. "
            + "The calculations and outputs are <b>NOT scientifically accurate</b> and should <b>NEVER</b> "
            + "be used for real-world planning, prediction, decision-making, or to incite fear.<br><br>"
            + "The subject matter is inherently grim. This tool is provided for conceptual "
            + "understanding of potential factors in such scenarios and as a programming exercise only."
            + "<br><br>Use with extreme caution and critical thinking. No liability is assumed for any "
            + "interpretation or use of this illustrative tool.</html>";
        JLabel messageLabel = new JLabel(disclaimerText);
        panel.add(messageLabel, BorderLayout.CENTER);

        JCheckBox dontShowAgainCheckbox = new JCheckBox("Do not show this message again at startup");
        dontShowAgainCheckbox.setSelected(!prefShowStartupDisclaimer); // Reflect current preference: checked if pref is false
        
        // Always show checkbox if launched from "About" or if pref is currently true (meaning user hasn't opted out yet)
        if (isLaunchedFromAboutMenu || prefShowStartupDisclaimer) {
            panel.add(dontShowAgainCheckbox, BorderLayout.SOUTH);
        }

        String dialogTitle = isLaunchedFromAboutMenu ? "About This Estimator" : "Important Disclaimer";

        // Inside showDisclaimerDialog, before or after JOptionPane
        java.awt.Toolkit.getDefaultToolkit().beep();

        JOptionPane.showMessageDialog(this, panel, dialogTitle, JOptionPane.WARNING_MESSAGE);

        // Update preference based on checkbox (only if it was visible and thus interactable) and save it immediately
        if (isLaunchedFromAboutMenu || prefShowStartupDisclaimer) { 
            boolean newPreferenceValueForShowing = !dontShowAgainCheckbox.isSelected(); // if checkbox is selected, pref for showing is false
            if (this.prefShowStartupDisclaimer != newPreferenceValueForShowing) {
                this.prefShowStartupDisclaimer = newPreferenceValueForShowing;
                saveDisclaimerPreferenceOnly();
            }
        }
    }
    
    private void saveDisclaimerPreferenceOnly() {
        Properties props = new Properties();
        File settingsFile = new File(SETTINGS_FILE);

        if (settingsFile.exists()) {
            try (InputStream input = new FileInputStream(settingsFile)) {
                props.load(input);
            } catch (IOException ex) {
                System.err.println("Error loading existing settings to save disclaimer pref: " + ex.getMessage());
            }
        }
        
        props.setProperty("showStartupDisclaimer", String.valueOf(prefShowStartupDisclaimer));

        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            props.store(output, "Nuclear Calculator Settings");
        } catch (IOException io) {
            System.err.println("Error saving disclaimer preference: " + io.getMessage());
        }
    }


    private void initComponents() {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Population
        inputPanel.add(new JLabel("City Population (millions):"), gbc);
        gbc.gridx++;
        populationField = new JTextField("37", 10);
        inputPanel.add(populationField, gbc);

        // Input Type Selection
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        warheadInputRadio = new JRadioButton("Warhead Input", true);
        radiationInputRadio = new JRadioButton("Radiation Level Input");
        ButtonGroup inputTypeGroup = new ButtonGroup();
        inputTypeGroup.add(warheadInputRadio);
        inputTypeGroup.add(radiationInputRadio);
        JPanel inputTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputTypePanel.add(new JLabel("Input Type:"));
        inputTypePanel.add(warheadInputRadio);
        inputTypePanel.add(radiationInputRadio);
        inputPanel.add(inputTypePanel, gbc);
        gbc.gridwidth = 1; 

        ActionListener inputTypeListener = e -> updateInputPanels();
        warheadInputRadio.addActionListener(inputTypeListener);
        radiationInputRadio.addActionListener(inputTypeListener);

        // --- Warhead Panel ---
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        warheadPanel = new JPanel(new GridBagLayout());
        warheadPanel.setBorder(BorderFactory.createTitledBorder("Warhead Details"));
        GridBagConstraints wgbc = new GridBagConstraints();
        wgbc.anchor = GridBagConstraints.WEST;
        wgbc.insets = new Insets(2,2,2,2);

        wgbc.gridx = 0; wgbc.gridy = 0;
        warheadPanel.add(new JLabel("Warhead Quantity:"), wgbc);
        wgbc.gridx++;
        warheadQuantityField = new JTextField("1", 5);
        warheadPanel.add(warheadQuantityField, wgbc);

        wgbc.gridx = 0; wgbc.gridy++;
        warheadPanel.add(new JLabel("Payload per Warhead (kT):"), wgbc);
        wgbc.gridx++;
        warheadPayloadField = new JTextField("1000", 5);
        warheadPanel.add(warheadPayloadField, wgbc);

        warheadQuantityField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> updateInputPanels()); }
            public void removeUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> updateInputPanels()); }
            public void insertUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> updateInputPanels()); }
        });


        wgbc.gridx = 0; wgbc.gridy++; wgbc.gridwidth = 2;
        strikeAllAtOnceRadio = new JRadioButton("All hit at once", true);
        strikePeriodicRadio = new JRadioButton("Periodic strikes");
        ButtonGroup strikeTypeGroup = new ButtonGroup();
        strikeTypeGroup.add(strikeAllAtOnceRadio);
        strikeTypeGroup.add(strikePeriodicRadio);
        JPanel strikeTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        strikeTypePanel.add(strikeAllAtOnceRadio);
        strikeTypePanel.add(strikePeriodicRadio);
        warheadPanel.add(strikeTypePanel, wgbc);
        wgbc.gridwidth = 1;

        ActionListener strikeTypeListener = e -> updateInputPanels();
        strikeAllAtOnceRadio.addActionListener(strikeTypeListener);
        strikePeriodicRadio.addActionListener(strikeTypeListener);

        wgbc.gridx = 0; wgbc.gridy++; wgbc.gridwidth = 2;
        periodicStrikePanel = new JPanel(new GridBagLayout());
        periodicStrikePanel.setBorder(BorderFactory.createTitledBorder("Periodic Strike Details"));
        GridBagConstraints psgbc = new GridBagConstraints();
        psgbc.anchor = GridBagConstraints.WEST;
        psgbc.insets = new Insets(2,2,2,2);

        psgbc.gridx = 0; psgbc.gridy = 0;
        periodicStrikePanel.add(new JLabel("Initial Warheads (at once):"), psgbc);
        psgbc.gridx++;
        periodicWarheadsInitialField = new JTextField("1", 5);
        periodicStrikePanel.add(periodicWarheadsInitialField, psgbc);

        psgbc.gridx = 0; psgbc.gridy++;
        periodicStrikePanel.add(new JLabel("Rest hit over (years):"), psgbc);
        psgbc.gridx++;
        periodicYearsField = new JTextField("5", 5);
        periodicStrikePanel.add(periodicYearsField, psgbc);
        warheadPanel.add(periodicStrikePanel, wgbc);
        
        inputPanel.add(warheadPanel, gbc);

        gbc.gridy++;
        radiationPanel = new JPanel(new GridBagLayout());
        radiationPanel.setBorder(BorderFactory.createTitledBorder("Radiation Details"));
        GridBagConstraints rgbc = new GridBagConstraints();
        rgbc.anchor = GridBagConstraints.WEST;
        rgbc.insets = new Insets(2,2,2,2);

        rgbc.gridx = 0; rgbc.gridy = 0;
        radiationPanel.add(new JLabel("Radiation Level (mSv):"), rgbc);
        rgbc.gridx++;
        radiationLevelField = new JTextField("1000", 10);
        radiationPanel.add(radiationLevelField, rgbc);
        inputPanel.add(radiationPanel, gbc);

        gbc.gridy++; gbc.gridwidth = 1;
        gbc.gridx = 0;
        nuclearWinterCheckbox = new JCheckBox("Enable Nuclear Winter", false);
        inputPanel.add(nuclearWinterCheckbox, gbc);

        gbc.gridx = 1;
        humanitarianAidCheckbox = new JCheckBox("Enable Humanitarian Aid", false);
        inputPanel.add(humanitarianAidCheckbox, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton calculateButton = new JButton("Calculate Casualties");
        JButton saveButton = new JButton("Save Settings");
        JButton loadButton = new JButton("Load Settings");
        JButton defaultsButton = new JButton("Restore Defaults");
        JButton aboutButton = new JButton("About"); 


        calculateButton.addActionListener(e -> performCalculation());
        saveButton.addActionListener(e -> saveSettings());
        loadButton.addActionListener(e -> loadSettings());
        defaultsButton.addActionListener(e -> restoreDefaultSettings());
        aboutButton.addActionListener(e -> showDisclaimerDialog(true)); 

        buttonPanel.add(calculateButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(defaultsButton);
        buttonPanel.add(aboutButton); 

        resultsArea = new JTextArea(45, 115); 
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultsArea);

        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void restoreDefaultSettings() {
        populationField.setText("37");
        warheadInputRadio.setSelected(true); 
        warheadQuantityField.setText("1"); 
        warheadPayloadField.setText("1000");
        strikeAllAtOnceRadio.setSelected(true); 
        periodicWarheadsInitialField.setText("1");
        periodicYearsField.setText("5");
        radiationLevelField.setText("1000");
        nuclearWinterCheckbox.setSelected(false);
        humanitarianAidCheckbox.setSelected(false);
        // NOTE: Disclaimer preference is NOT reset by "Restore Defaults"
        // If user wants to see it again at startup, they can re-enable via About dialog
        updateInputPanels(); 
        resultsArea.setText("Input values restored to defaults.\n(Startup disclaimer preference remains unchanged.)\n");
    }

    private void updateInputPanels() {
        boolean warheadInputSelected = warheadInputRadio.isSelected();
        warheadPanel.setVisible(warheadInputSelected);
        radiationPanel.setVisible(!warheadInputSelected);

        if (warheadInputSelected) {
            int quantity = 1; 
            try {
                if (!warheadQuantityField.getText().trim().isEmpty()) {
                     quantity = Integer.parseInt(warheadQuantityField.getText().trim());
                }
            } catch (NumberFormatException e) { /* use default */ }
            
            boolean multiWarheads = quantity > 1;
            strikeAllAtOnceRadio.setEnabled(multiWarheads);
            strikePeriodicRadio.setEnabled(multiWarheads);

            if (!multiWarheads) {
                strikeAllAtOnceRadio.setSelected(true); 
            }
            periodicStrikePanel.setVisible(multiWarheads && strikePeriodicRadio.isSelected());

        } else {
            periodicStrikePanel.setVisible(false);
        }
        this.revalidate();
        this.repaint();
    }
    
    private String formatTableRow(String label, String val1, String val2, String val3) {
        return String.format("%-33s | %12s | %11s | %15s\n", label, val1, val2, val3);
    }
    private String formatTableHeader() {
        return String.format("%-33s | %12s | %11s | %15s\n", "Phase/Event", "Value", "% of Init.", "Pop. Remaining");
    }
     private String formatSeparator() {
        return "----------------------------------+--------------+-------------+-----------------\n";
    }

    private static class AcuteRadiationDoseEffect {
        double minDoseSv; 
        double maxDoseSv; 
        String effectDescription;

        public AcuteRadiationDoseEffect(double minDoseSv, double maxDoseSv, String effectDescription) {
            this.minDoseSv = minDoseSv;
            this.maxDoseSv = maxDoseSv;
            this.effectDescription = effectDescription;
        }

        public String getDoseRangeMsv() {
            if (maxDoseSv == Double.MAX_VALUE) return String.format("> %.0f mSv (> %.0f Sv)", minDoseSv * 1000, minDoseSv);
            if (minDoseSv == 0 && maxDoseSv == 0.25) return String.format("%.0f - %.0f mSv (0 - %.2f Sv)", minDoseSv*1000, maxDoseSv * 1000, maxDoseSv);
            return String.format("%.0f - %.0f mSv (%.2f - %.1f Sv)", minDoseSv * 1000, maxDoseSv * 1000, minDoseSv, maxDoseSv);
        }
    }

    private List<AcuteRadiationDoseEffect> getAcuteRadiationEffectsData() {
        List<AcuteRadiationDoseEffect> effects = new ArrayList<>();
        effects.add(new AcuteRadiationDoseEffect(0, 0.25, "Few/no immediate symptoms. Possible slight blood changes. Increased long-term cancer risk."));
        effects.add(new AcuteRadiationDoseEffect(0.251, 1.0, "Mild: Nausea, vomiting in 10-50% (3-6h), <24h. Fatigue. Temp. lymphocyte drop. Recovery usual."));
        effects.add(new AcuteRadiationDoseEffect(1.001, 2.0, "Moderate: Nausea, vomiting 50-90% (2-4h), 1-2d. Hair loss (~2wks). Blood cell drops. Medical attention advised."));
        effects.add(new AcuteRadiationDoseEffect(2.001, 4.0, "Severe: Nausea/vomit <1h (70-100%), diarrhea, fever. Hair loss, hemorrhage, infection. Hospital vital. ~15-50% mortality (30d w/o care)."));
        effects.add(new AcuteRadiationDoseEffect(4.001, 6.0, "Very Severe: Symptoms rapid. High mortality (~50-90% in 2-4wks) even w/ care (LD50/30). Bone marrow destruction."));
        effects.add(new AcuteRadiationDoseEffect(6.001, 10.0, "Critical: Symptoms immediate/severe. Survival unlikely (~90-100% mortality). GI/Cardiovascular collapse."));
        effects.add(new AcuteRadiationDoseEffect(10.001, Double.MAX_VALUE, "Lethal: Incapacitation (mins-hr). Death (hrs-2 days). Central Nervous System syndrome."));
        return effects;
    }
    
    private void generateAcuteRadiationEffectsTable(StringBuilder sb) {
        sb.append("\nAcute Radiation Effects (Illustrative):\n");
        sb.append(String.format("(Hypothetical time to dose based on continuous exposure at %,.0f mSv/hour)\n", HYPOTHETICAL_EXPOSURE_RATE_FOR_ACUTE_TABLE));
        
        String headerCol1 = "Dose Range";
        String headerCol2 = "Time to Max Dose";
        String headerCol3 = "Typical Acute Effects on Humans";
        int col1Width = 36; 
        int col2Width = 20;
        int col3Width = 90;

        String dashes1 = "-".repeat(col1Width);
        String dashes2 = "-".repeat(col2Width);
        String dashes3 = "-".repeat(col3Width);

        sb.append(String.format("%s+%s+%s\n", dashes1, dashes2, dashes3).replace(" ", "-"));
        sb.append(String.format("%-" + col1Width + "s | %-" + col2Width + "s | %s\n", headerCol1, headerCol2, headerCol3));
        sb.append(String.format("%s+%s+%s\n", dashes1, dashes2, dashes3).replace(" ", "-"));

        List<AcuteRadiationDoseEffect> effectsData = getAcuteRadiationEffectsData();
        for (AcuteRadiationDoseEffect effect : effectsData) {
            double timeToMaxDoseHours = effect.maxDoseSv * 1000 / HYPOTHETICAL_EXPOSURE_RATE_FOR_ACUTE_TABLE;
            String timeStr;
            if (effect.maxDoseSv == Double.MAX_VALUE) {
                timeStr = String.format("> %.1f hrs", effect.minDoseSv * 1000 / HYPOTHETICAL_EXPOSURE_RATE_FOR_ACUTE_TABLE);
            } else if (timeToMaxDoseHours < 0.016) { 
                 timeStr = String.format("< 1 min");
            } else if (timeToMaxDoseHours < 1.0) {
                timeStr = String.format("%.0f mins", timeToMaxDoseHours * 60);
            } else {
                timeStr = String.format("%.1f hrs", timeToMaxDoseHours);
            }
            sb.append(String.format("%-" + col1Width + "s | %-" + col2Width + "s | %s\n", effect.getDoseRangeMsv(), timeStr, effect.effectDescription));
        }
        sb.append(String.format("%s+%s+%s\n", dashes1, dashes2, dashes3).replace(" ", "-"));
        sb.append("*Note: Individual responses vary. Medical treatment can alter outcomes. This is simplified.\n");
    }
    
    private String formatTimeDuration(double totalHours) {
        if (totalHours < 0) return "N/A";
        if (totalHours == Double.POSITIVE_INFINITY || totalHours > 876000) return ">100 years";
        if (totalHours < (1.0/60.0)) return "< 1 min"; 

        int days = (int) (totalHours / 24);
        double remainingHours = totalHours % 24;
        int hours = (int) remainingHours;
        int minutes = (int) ((remainingHours - hours) * 60);

        StringBuilder sbTime = new StringBuilder();
        if (days > 0) {
            sbTime.append(days).append("d ");
            if (days > 365 * 2) { 
                 return String.format("~%.1f years", totalHours / 8760.0);
            }
             if (days > 7 && hours == 0 && minutes == 0) return sbTime.toString().trim(); 
        }
        if (hours > 0 || days > 0) { 
             sbTime.append(hours).append("h ");
        }
        sbTime.append(minutes).append("m");
        return sbTime.toString().trim();
    }

    private String getNotesAndRecommendations(double currentRateMSvHr) {
        StringBuilder notes = new StringBuilder();
        if (currentRateMSvHr > 100) { 
            notes.append("P: DEEPEST SHELTER. NO OUTDOOR. Monitor comms. / U: ABSOLUTE MAX SHELTER. Improvise. Pray.");
        } else if (currentRateMSvHr > 10) { 
            notes.append("P: Stay in shelter. Min. essential tasks if shielded & brief. / U: Max shelter. Avoid all exposure.");
        } else if (currentRateMSvHr > 1) { 
            notes.append("P: Limit exposure. Short, necessary outdoor tasks w/ PPE. / U: Strict shelter. Risk calc for any exit.");
        } else if (currentRateMSvHr > 0.1) { 
            notes.append("P: Caution outdoors. PPE. Rotate personnel for tasks. / U: Very brief exit if CRITICAL. High risk.");
        } else if (currentRateMSvHr > REDUCED_RISK_OUTDOOR_ACTIVITY_RATE_MSV_HR) { 
             notes.append("P: Outdoor tasks w/ caution & PPE. Monitor dose. / U: Brief exits for essentials, minimize time.");
        } else { 
             notes.append("P: 'Reduced Risk' but long-term exposure adds up. Monitor. / U: Still elevated. Limit time outdoors.");
        }
        return notes.toString();
    }

    private void generateFalloutDecayTable(StringBuilder sb, double initialFalloutRateH1, boolean isPeriodicScenario) {
        sb.append("\nIllustrative Fallout Decay & Dose Rate (Approximate t^-1.2 rule):\n");
        if (initialFalloutRateH1 <= 0.000001) {
            sb.append("  (Initial fallout rate is effectively zero; no decay table generated.)\n");
            return;
        }
        sb.append(String.format("  (Based on an estimated initial HOT ZONE dose rate of %,.1f mSv/hr at H+1 hour)\n", initialFalloutRateH1));

        String headerCol1 = "Time Since Det."; 
        String headerCol2 = "Decay Factor";   
        String headerCol3 = "Est. Rad.Level(mSv/hr)"; 
        String headerCol4 = String.format("Max Safe Exp.(%smSv)", df.format(EMERGENCY_DOSE_LIMIT_FOR_MAX_EXPOSURE_COLUMN_MSV)); 
        String headerCol5 = "Notes & Recs (P:Prepared U:Unprepared)"; 

        int col1W = 20, col2W = 15, col3W = 25, col4W = 28;
        int col5W = 60; 
        
        String dashes1 = "-".repeat(col1W);
        String dashes2 = "-".repeat(col2W);
        String dashes3 = "-".repeat(col3W);
        String dashes4 = "-".repeat(col4W);
        String dashes5 = "-".repeat(col5W);

        sb.append(String.format("%s+%s+%s+%s+%s\n", dashes1, dashes2, dashes3, dashes4, dashes5));
        sb.append(String.format("%-"+col1W+"s | %-"+col2W+"s | %-"+col3W+"s | %-"+col4W+"s | %s\n", 
                                headerCol1, headerCol2, headerCol3, headerCol4, headerCol5));
        sb.append(String.format("%s+%s+%s+%s+%s\n", dashes1, dashes2, dashes3, dashes4, dashes5));

        double[] timePointsHours = {1, 2, 4, 7, 12, 24, 48, 7*24, 14*24, 30*24, 90*24, 180*24, 365*24}; 
        String[] timeLabels = {"H+1h", "H+2h", "H+4h", "H+7h", "H+12h", "H+1 Day", "H+2 Days", "H+1 Week", "H+2 Weeks", "H+1 Month", "H+3 Months", "H+6 Months", "H+1 Year"};

        for (int i = 0; i < timePointsHours.length; i++) {
            double t = timePointsHours[i];
            double decayFactor = Math.pow(t, -1.2); 
            if (t == 1) decayFactor = 1.0; 

            double currentRate = initialFalloutRateH1 * decayFactor;
            String maxSafeExpTimeStr = "Instantly Overlimit";
            if (currentRate > 0.000001) { 
                 maxSafeExpTimeStr = formatTimeDuration(EMERGENCY_DOSE_LIMIT_FOR_MAX_EXPOSURE_COLUMN_MSV / currentRate);
            } else {
                maxSafeExpTimeStr = ">Very Long";
            }
            
            String notes = getNotesAndRecommendations(currentRate);

            sb.append(String.format("%-"+col1W+"s | %-"+col2W+".4f | %-"+col3W+"s | %-"+col4W+"s | %s\n", 
                                    timeLabels[i], 
                                    decayFactor, 
                                    doseRateFormat.format(currentRate),
                                    maxSafeExpTimeStr,
                                    notes
                                    ));
        }
        sb.append(String.format("%s+%s+%s+%s+%s\n", dashes1, dashes2, dashes3, dashes4, dashes5));
        sb.append("* Max Safe Exp. is illustrative time to reach 100 mSv at current rate. Does NOT imply safety.\n");
        sb.append("* Notes are general. P: Prepared (good shelter, supplies, PPE). U: Unprepared.\n");
        sb.append("* Model uses t^-1.2 decay. Actual decay is complex. Shielding is critical.\n");
        if (isPeriodicScenario) {
            sb.append("* PERIODIC STRIKES would re-contaminate, making these decay projections unreliable over time.\n");
        }
        
        sb.append("\nCalculated Safe Outdoor Timings (Highly Illustrative):\n");
        sb.append("----------------------------------------------------\n");

        if (initialFalloutRateH1 > 0.000001) {
            double targetRatePrepared = EVAC_DOSE_LIMIT_PREPARED_MSV / EVAC_DURATION_PREPARED_HOURS;
            double tEvacPrepared = Math.pow(initialFalloutRateH1 / targetRatePrepared, 1.0/1.2);
            sb.append(String.format("Est. Earliest Evac Window (Prepared - Target: <%s mSv in %s hrs): %s\n", 
                                    df.format(EVAC_DOSE_LIMIT_PREPARED_MSV), df.format(EVAC_DURATION_PREPARED_HOURS), formatTimeDuration(tEvacPrepared)));

            double targetRateUnprepared = EVAC_DOSE_LIMIT_UNPREPARED_MSV / EVAC_DURATION_UNPREPARED_HOURS;
            double tEvacUnprepared = Math.pow(initialFalloutRateH1 / targetRateUnprepared, 1.0/1.2);
            sb.append(String.format("Est. Earliest Evac Window (Unprepared - Target: <%s mSv in %s hrs): %s\n", 
                                    df.format(EVAC_DOSE_LIMIT_UNPREPARED_MSV), df.format(EVAC_DURATION_UNPREPARED_HOURS), formatTimeDuration(tEvacUnprepared)));

            double tReducedRisk = Math.pow(initialFalloutRateH1 / REDUCED_RISK_OUTDOOR_ACTIVITY_RATE_MSV_HR, 1.0/1.2);
            sb.append(String.format("Est. Time for 'Reduced Risk' Outdoor Activity (Rate ~%s mSv/hr): %s\n", 
                                    doseRateFormat.format(REDUCED_RISK_OUTDOOR_ACTIVITY_RATE_MSV_HR), formatTimeDuration(tReducedRisk)));
        } else {
             sb.append("  (Initial fallout rate too low for meaningful 'earliest time' calculations.)\n");
        }
        sb.append("* These are THEORETICAL minimum times. Actual decisions require real-time measurements & expert advice.\n");
        if (isPeriodicScenario) {
            sb.append("* WARNING: Periodic strikes render these 'earliest time' calculations highly UNRELIABLE after the initial event.\n");
        }
    }

    private void performCalculation() {
        resultsArea.setText("Calculating...\n\n");
        StringBuilder sb = new StringBuilder();

        try {
            double populationMillions = Double.parseDouble(populationField.getText());
            long totalPopulation = (long) (populationMillions * 1_000_000);

            sb.append("DISCLAIMER: This is a highly simplified, illustrative model.\n");
            sb.append("Results are NOT scientifically accurate or predictive and are intended\n");
            sb.append("for conceptual understanding of interdependencies only.\n");
            sb.append("---------------------------------------------------------------------\n\n");
            sb.append("Input Parameters Summary:\n");
            sb.append(String.format("  Initial City Population: %s\n", df.format(totalPopulation)));

            long currentPopulation = totalPopulation;
            long totalInitialDeaths = 0;
            long mainFalloutDeaths = 0;
            long periodicStrikeDirectAttritionDeaths = 0;
            long periodicStrikeFalloutDeaths = 0;
            long postEventDeaths = 0; 

            boolean isNuclearWinter = nuclearWinterCheckbox.isSelected();
            boolean hasHumanitarianAid = humanitarianAidCheckbox.isSelected();

            sb.append("\nScenario Modifiers & Context:\n");
            sb.append("-----------------------------\n");
            if (isNuclearWinter) {
                sb.append("Nuclear Winter Scenario Details:\n");
                sb.append("  - SELECTED: Nuclear Winter effects are enabled.\n");
                sb.append("  - Implication: Simulates global climatic catastrophe (blocked sunlight, cooling, famine).\n");
                sb.append("  - Impact on Aid: Large-scale external aid becomes largely untenable. Any modeled aid benefit\n");
                sb.append("    is extremely limited and its effectiveness severely compromised.\n");
                sb.append("  - Overall: Long-term survival prospects drastically reduced globally.\n\n");
            } else {
                sb.append("Nuclear Winter Scenario Details:\n");
                sb.append("  - NOT SELECTED: Global Nuclear Winter effects are not factored in.\n\n");
            }
            if (hasHumanitarianAid) {
                sb.append("Humanitarian Aid Scenario Details:\n");
                sb.append("  - SELECTED: Humanitarian Aid effects are enabled.\n");
                sb.append("  - Assumption: Aid (medical, food, shelter) from OUTSIDE the affected city/region.\n");
                if (isNuclearWinter) {
                    sb.append("  - With Nuclear Winter: Aid capacity is CRITICALLY UNDERMINED. Realistically, large-scale\n");
                    sb.append("    external aid would be nearly impossible.\n");
                } else {
                    sb.append("  - Without Nuclear Winter: Aid assumed from less affected regions; delivery is challenging.\n");
                }
                sb.append("  - Effect: Reduces certain modeled casualty rates.\n\n");
            } else {
                sb.append("Humanitarian Aid Scenario Details:\n");
                sb.append("  - NOT SELECTED: No significant external humanitarian aid is assumed.\n");
                sb.append("  - Implication: Casualties from untreated effects, lack of resources are higher.\n\n");
            }
            sb.append("-----------------------------\n\n");

            boolean isPeriodicStrikeScenario = false;
            int initialWarheadsForCalc = 0;
            int remainingWarheadsPeriodic = 0;
            int periodicYearsForCalc = 0;
            double payloadKT_perWarhead = 0;
            double totalYieldMT_forFalloutDecayTable = 0;


            if (warheadInputRadio.isSelected()) {
                sb.append("Input Mode: Warheads\n");
                int quantity = Integer.parseInt(warheadQuantityField.getText());
                payloadKT_perWarhead = Double.parseDouble(warheadPayloadField.getText());
                double totalYieldKT = quantity * payloadKT_perWarhead;
                totalYieldMT_forFalloutDecayTable = totalYieldKT / 1000.0;
                sb.append(String.format("  Total Warheads: %d, Payload/Warhead: %.0f kT, Total Yield: %.2f MT\n",
                                        quantity, payloadKT_perWarhead, totalYieldMT_forFalloutDecayTable));

                double initialImpactYieldMT = totalYieldMT_forFalloutDecayTable;
                initialWarheadsForCalc = quantity;

                if (quantity > 1 && strikePeriodicRadio.isSelected()) {
                    isPeriodicStrikeScenario = true;
                    initialWarheadsForCalc = Integer.parseInt(periodicWarheadsInitialField.getText());
                    periodicYearsForCalc = Integer.parseInt(periodicYearsField.getText());
                    if (initialWarheadsForCalc >= quantity) {
                        initialWarheadsForCalc = quantity;
                        isPeriodicStrikeScenario = false;
                        remainingWarheadsPeriodic = 0;
                        sb.append("  Strike Pattern: All warheads effectively hit at once (Initial Impact Yield: %.2f MT).\n".formatted(totalYieldMT_forFalloutDecayTable));
                    } else {
                        initialImpactYieldMT = (initialWarheadsForCalc * payloadKT_perWarhead) / 1000.0;
                        remainingWarheadsPeriodic = quantity - initialWarheadsForCalc;
                        sb.append(String.format("  Periodic Strike: %d warheads initially (%.2f MT), rest (%d) over %d years.\n",
                                                initialWarheadsForCalc, initialImpactYieldMT, remainingWarheadsPeriodic, periodicYearsForCalc));
                        sb.append("    - Context: Prolonged attacks create perpetual crisis, destroy recovery attempts,\n");
                        sb.append("      generate new casualties, spread contamination, and complicate aid efforts.\n");
                    }
                } else {
                    sb.append("  Strike Pattern: All warheads hit at once (Initial Impact Yield: %.2f MT).\n".formatted(initialImpactYieldMT));
                }
                sb.append("\n");

                double directDeathFactor = Math.min(0.90, initialImpactYieldMT * 0.20);
                long directDeaths = (long) (currentPopulation * directDeathFactor);
                long popAfterDirectDeaths = currentPopulation - directDeaths;
                double severeInjuryFactor = Math.min(0.80, initialImpactYieldMT * 0.25);
                long severeInjuries = (long) (popAfterDirectDeaths * severeInjuryFactor);
                long deathsFromInjuries = (long) (severeInjuries * (hasHumanitarianAid ? 0.40 : 0.60));
                totalInitialDeaths = directDeaths + deathsFromInjuries;
                currentPopulation -= totalInitialDeaths;

                sb.append("Speculative Radiation Commentary (based on total yield of %.2f MT):\n".formatted(totalYieldMT_forFalloutDecayTable));
                sb.append("  - Prompt Radiation: Could cause lethal doses (>5000 mSv) near hypocenters.\n");
                sb.append("  - Fallout: Could produce high radiation rates (hundreds of mSv/hr) downwind.\n");
                sb.append("  (Note: Generalized statements, not precise city-wide average calculations.)\n\n");

            } else { // Radiation Input
                sb.append("Input Mode: Radiation Level\n");
                double radiationMSV = Double.parseDouble(radiationLevelField.getText());
                sb.append(String.format("  Assumed Average Prompt/Early Effective Radiation Exposure: %.0f mSv\n\n", radiationMSV));
                
                totalYieldMT_forFalloutDecayTable = radiationMSV / EFFECTIVE_MSV_PER_MT_PROMPT_LOCAL_MODEL_PARAM;
                if (totalYieldMT_forFalloutDecayTable < 0.00001) totalYieldMT_forFalloutDecayTable = 0; 

                double refPayloadKT = 1000;
                try {
                    if (!warheadPayloadField.getText().trim().isEmpty()) {
                        refPayloadKT = Double.parseDouble(warheadPayloadField.getText().trim());
                        payloadKT_perWarhead = refPayloadKT; 
                    } else {
                        payloadKT_perWarhead = refPayloadKT; 
                    }
                } catch (NumberFormatException e) { payloadKT_perWarhead = refPayloadKT; }
                if (refPayloadKT <= 0) refPayloadKT = 1000;

                double estimatedEquivalentMT = Math.max(0, radiationMSV / EFFECTIVE_MSV_PER_MT_PROMPT_LOCAL_MODEL_PARAM);
                int numFullWarheads = (int) ((estimatedEquivalentMT * 1000.0) / refPayloadKT);
                double remainingKT_fromRad = (estimatedEquivalentMT * 1000.0) % refPayloadKT;

                sb.append("Speculative Warhead Equivalent (for input radiation of %.0f mSv):\n".formatted(radiationMSV));
                sb.append(String.format("  (Derived total equivalent yield for radiation context: %.2f MT)\n", estimatedEquivalentMT));
                sb.append("  - Assuming a reference payload of %.0f kT per warhead:\n".formatted(refPayloadKT));
                sb.append("  - This radiation level *might roughly correspond* to the effects of:\n");
                sb.append(String.format("    %d warhead(s) of %.0f kT payload", numFullWarheads, refPayloadKT));
                if (remainingKT_fromRad > 1) {
                    sb.append(String.format(", plus an additional impact\n    equivalent to ~%.0f kT yield.\n", remainingKT_fromRad));
                } else {
                    sb.append(".\n");
                }
                sb.append("  (Note: Highly speculative reverse estimation.)\n\n");

                double directRadDeathFactor = Math.min(0.95, radiationMSV / 5000.0);
                long directRadDeaths = (long) (currentPopulation * directRadDeathFactor);
                long popAfterDirectRadDeaths = currentPopulation - directRadDeaths;
                double severeSicknessFactor = Math.min(0.90, radiationMSV / 2000.0);
                long severeSickness = (long) (popAfterDirectRadDeaths * severeSicknessFactor);
                long deathsFromSickness = (long) (severeSickness * (hasHumanitarianAid ? 0.30 : 0.50));
                totalInitialDeaths = directRadDeaths + deathsFromSickness;
                currentPopulation -= totalInitialDeaths;
            }

            long popAfterInitialImpact = currentPopulation;

            // --- Main Fallout Casualties ---
            if (currentPopulation > 0) {
                double falloutDeathFactor = 0.0;
                falloutDeathFactor = Math.min(0.75, totalYieldMT_forFalloutDecayTable * 0.08); 
                
                if (hasHumanitarianAid) {
                    falloutDeathFactor /= (isNuclearWinter ? 1.2 : 1.8); 
                }
                mainFalloutDeaths = (long) (currentPopulation * falloutDeathFactor);
                if (mainFalloutDeaths > currentPopulation) mainFalloutDeaths = currentPopulation;
                currentPopulation -= mainFalloutDeaths;
            }
            long popAfterMainFallout = currentPopulation;

            // --- Periodic Strike Direct Attrition ---
            if (isPeriodicStrikeScenario && remainingWarheadsPeriodic > 0 && currentPopulation > 0) {
                double periodicDirectAttritionFactor = 0.02; 
                periodicDirectAttritionFactor += Math.min(0.10, ((double) remainingWarheadsPeriodic / Math.max(1, periodicYearsForCalc)) * 0.01);
                if (hasHumanitarianAid) {
                    periodicDirectAttritionFactor *= (isNuclearWinter ? 0.9 : 0.7); 
                }
                periodicDirectAttritionFactor = Math.min(0.30, periodicDirectAttritionFactor);
                periodicStrikeDirectAttritionDeaths = (long) (currentPopulation * periodicDirectAttritionFactor);
                if (periodicStrikeDirectAttritionDeaths > currentPopulation) periodicStrikeDirectAttritionDeaths = currentPopulation;
                currentPopulation -= periodicStrikeDirectAttritionDeaths;
            }
            long popAfterPeriodicDirectAttrition = currentPopulation;

            // --- Periodic Strike Fallout Deaths ---
            if (isPeriodicStrikeScenario && remainingWarheadsPeriodic > 0 && currentPopulation > 0) {
                double periodicFalloutYieldMT = (remainingWarheadsPeriodic * payloadKT_perWarhead) / 1000.0;
                double periodicFalloutDeathFactor = Math.min(0.60, periodicFalloutYieldMT * 0.05);
                if (hasHumanitarianAid) {
                     periodicFalloutDeathFactor /= (isNuclearWinter ? 1.1 : 1.5); 
                }
                periodicStrikeFalloutDeaths = (long) (currentPopulation * periodicFalloutDeathFactor);
                if (periodicStrikeFalloutDeaths > currentPopulation) periodicStrikeFalloutDeaths = currentPopulation;
                currentPopulation -= periodicStrikeFalloutDeaths;
            }
            long popAfterPeriodicFallout = currentPopulation;

            // --- Long-Term Post-Event Deaths ---
            if (currentPopulation > 0) {
                double longTermDeathFactor = 0.15;
                if (isNuclearWinter) {
                    longTermDeathFactor *= 3.0; 
                }
                if (hasHumanitarianAid) {
                    longTermDeathFactor /= (isNuclearWinter ? 1.15 : 1.8);
                }
                if (isPeriodicStrikeScenario && remainingWarheadsPeriodic > 0) {
                    longTermDeathFactor *= 1.3; 
                }
                longTermDeathFactor = Math.min(0.99, longTermDeathFactor);
                postEventDeaths = (long) (currentPopulation * longTermDeathFactor);
                if (postEventDeaths > currentPopulation) postEventDeaths = currentPopulation;
                currentPopulation -= postEventDeaths;
            }

            // --- Summary Table ---
            sb.append("Casualty Breakdown & Population Estimates:\n");
            sb.append(formatTableHeader());
            sb.append(formatSeparator());
            sb.append(formatTableRow("Initial Population", "", "", df.format(totalPopulation)));

            if (warheadInputRadio.isSelected()) {
                double initialImpactYieldMT_forTableSection = totalYieldMT_forFalloutDecayTable;
                if (isPeriodicStrikeScenario && initialWarheadsForCalc < Integer.parseInt(warheadQuantityField.getText())) {
                    initialImpactYieldMT_forTableSection = (initialWarheadsForCalc * payloadKT_perWarhead / 1000.0);
                }
                long directBlastThermalDeaths = (long) (totalPopulation * Math.min(0.90, initialImpactYieldMT_forTableSection * 0.20));
                long injuriesLeadingToDeath = totalInitialDeaths - directBlastThermalDeaths;
                if (injuriesLeadingToDeath < 0) injuriesLeadingToDeath = 0;

                sb.append(formatTableRow("  Direct Blast/Thermal", df.format(directBlastThermalDeaths), pf.format((double) directBlastThermalDeaths / totalPopulation), ""));
                sb.append(formatTableRow("  From Severe Injuries", df.format(injuriesLeadingToDeath), pf.format((double) injuriesLeadingToDeath / totalPopulation), ""));
            } else {
                double inputMSV = Double.parseDouble(radiationLevelField.getText());
                long directRadDeaths = (long) (totalPopulation * Math.min(0.95, inputMSV / 5000.0));
                long deathsFromSickness = totalInitialDeaths - directRadDeaths;
                if (deathsFromSickness < 0) deathsFromSickness = 0;

                sb.append(formatTableRow("  Direct Radiation Deaths", df.format(directRadDeaths), pf.format((double) directRadDeaths / totalPopulation), ""));
                sb.append(formatTableRow("  From Severe Sickness", df.format(deathsFromSickness), pf.format((double) deathsFromSickness / totalPopulation), ""));
            }
            sb.append(formatTableRow("Subtotal Initial Deaths", df.format(totalInitialDeaths), pf.format((double) totalInitialDeaths / totalPopulation), df.format(popAfterInitialImpact)));
            sb.append(formatSeparator());
            sb.append(formatTableRow("Main Fallout (Overall Yield)", df.format(mainFalloutDeaths), pf.format((double) mainFalloutDeaths / totalPopulation), df.format(popAfterMainFallout)));

            if (isPeriodicStrikeScenario && remainingWarheadsPeriodic > 0) {
                sb.append(formatSeparator());
                sb.append(formatTableRow("Periodic Strike Direct Attrition", df.format(periodicStrikeDirectAttritionDeaths), pf.format((double) periodicStrikeDirectAttritionDeaths / totalPopulation), df.format(popAfterPeriodicDirectAttrition)));
                sb.append(formatSeparator());
                sb.append(formatTableRow("Periodic Strike Fallout", df.format(periodicStrikeFalloutDeaths), pf.format((double) periodicStrikeFalloutDeaths / totalPopulation), df.format(popAfterPeriodicFallout)));
            }

            sb.append(formatSeparator());
            sb.append(formatTableRow("Long-Term Post-Event Deaths", df.format(postEventDeaths), pf.format((double) postEventDeaths / totalPopulation), df.format(currentPopulation)));
            sb.append(formatSeparator());

            long totalDeaths = totalInitialDeaths + mainFalloutDeaths + periodicStrikeDirectAttritionDeaths + periodicStrikeFalloutDeaths + postEventDeaths;
            if (totalDeaths > totalPopulation) totalDeaths = totalPopulation;
            long finalSurvivors = totalPopulation - totalDeaths;
            if (finalSurvivors < 0) finalSurvivors = 0;

            sb.append(formatTableRow("TOTAL DEATHS", df.format(totalDeaths), pf.format((double) totalDeaths / totalPopulation), ""));
            sb.append(formatTableRow("FINAL SURVIVORS", df.format(finalSurvivors), pf.format((double) finalSurvivors / totalPopulation), df.format(finalSurvivors)));
            sb.append("---------------------------------------------------------------------\n");

            generateAcuteRadiationEffectsTable(sb);
            double initialFalloutRateH1 = totalYieldMT_forFalloutDecayTable * FALLOUT_RATE_PER_MT_AT_H1_FACTOR;
            generateFalloutDecayTable(sb, initialFalloutRateH1, isPeriodicStrikeScenario);


            resultsArea.append(sb.toString());
            resultsArea.setCaretPosition(0); 

        } catch (NumberFormatException e) {
            resultsArea.append("Error: Invalid number in one of the input fields.\n" + e.getMessage());
            JOptionPane.showMessageDialog(this, "Please enter valid numbers in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            resultsArea.append("An unexpected error occurred: " + e.getMessage() + "\n");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            resultsArea.append(sw.toString());
        }
    }

    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("population", populationField.getText());
        props.setProperty("warheadInput", String.valueOf(warheadInputRadio.isSelected()));
        props.setProperty("warheadQuantity", warheadQuantityField.getText());
        props.setProperty("warheadPayload", warheadPayloadField.getText());
        props.setProperty("strikeAllAtOnce", String.valueOf(strikeAllAtOnceRadio.isSelected()));
        props.setProperty("periodicWarheadsInitial", periodicWarheadsInitialField.getText());
        props.setProperty("periodicYears", periodicYearsField.getText());
        props.setProperty("radiationLevel", radiationLevelField.getText());
        props.setProperty("nuclearWinter", String.valueOf(nuclearWinterCheckbox.isSelected()));
        props.setProperty("humanitarianAid", String.valueOf(humanitarianAidCheckbox.isSelected()));
        props.setProperty("showStartupDisclaimer", String.valueOf(this.prefShowStartupDisclaimer)); 


        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            props.store(output, "Nuclear Calculator Settings");
            resultsArea.append("\nSettings saved to " + SETTINGS_FILE + "\n");
        } catch (IOException io) {
            resultsArea.append("\nError saving settings: " + io.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "Error saving settings: " + io.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSettings() {
        Properties props = new Properties();
        File settingsFile = new File(SETTINGS_FILE);
        
        Runnable applyDefaultsAndUpdateUI = () -> {
            populationField.setText("37");
            warheadInputRadio.setSelected(true); 
            warheadQuantityField.setText("1"); 
            warheadPayloadField.setText("1000");
            strikeAllAtOnceRadio.setSelected(true); 
            periodicWarheadsInitialField.setText("1");
            periodicYearsField.setText("5");
            radiationLevelField.setText("1000");
            nuclearWinterCheckbox.setSelected(false);
            humanitarianAidCheckbox.setSelected(false);
            this.prefShowStartupDisclaimer = true; 
            updateInputPanels(); 
        };

        if (!settingsFile.exists()) {
             resultsArea.setText("No settings file found ("+SETTINGS_FILE+"). Using defaults.\n");
             applyDefaultsAndUpdateUI.run();
             return;
        }

        try (InputStream input = new FileInputStream(SETTINGS_FILE)) {
            props.load(input);

            populationField.setText(props.getProperty("population", "37"));
            
            boolean isWarheadInput = Boolean.parseBoolean(props.getProperty("warheadInput", "true"));
            if (isWarheadInput) {
                warheadInputRadio.setSelected(true);
            } else {
                radiationInputRadio.setSelected(true);
            }
            
            warheadQuantityField.setText(props.getProperty("warheadQuantity", "1")); 
            warheadPayloadField.setText(props.getProperty("warheadPayload", "1000"));

            boolean isStrikeAllAtOnce = Boolean.parseBoolean(props.getProperty("strikeAllAtOnce", "true"));
             if (isStrikeAllAtOnce) {
                strikeAllAtOnceRadio.setSelected(true);
            } else {
                strikePeriodicRadio.setSelected(true);
            }

            periodicWarheadsInitialField.setText(props.getProperty("periodicWarheadsInitial", "1"));
            periodicYearsField.setText(props.getProperty("periodicYears", "5"));
            radiationLevelField.setText(props.getProperty("radiationLevel", "1000"));
            nuclearWinterCheckbox.setSelected(Boolean.parseBoolean(props.getProperty("nuclearWinter", "false")));
            humanitarianAidCheckbox.setSelected(Boolean.parseBoolean(props.getProperty("humanitarianAid", "false")));
            this.prefShowStartupDisclaimer = Boolean.parseBoolean(props.getProperty("showStartupDisclaimer", "true"));
            
            resultsArea.append("Settings loaded from " + SETTINGS_FILE + "\n");

        } catch (IOException ex) {
            resultsArea.append("Error loading settings: " + ex.getMessage() + ". Using defaults.\n");
            applyDefaultsAndUpdateUI.run();
        } finally {
            SwingUtilities.invokeLater(this::updateInputPanels);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Nuclear_Casualty_Calculator gui = new Nuclear_Casualty_Calculator();
            gui.setVisible(true);
        });
    }
}