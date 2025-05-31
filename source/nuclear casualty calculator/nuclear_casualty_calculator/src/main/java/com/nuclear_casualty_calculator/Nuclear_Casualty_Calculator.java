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


    // Illustrative constant for mSv <-> yield reverse estimation
    private static final double EFFECTIVE_MSV_PER_MT_PROMPT_LOCAL_MODEL_PARAM = 250; // mSv total effective dose / MT (invented)
    private static final double FALLOUT_RATE_PER_MT_AT_H1_FACTOR = 1000; // mSv/hr at H+1 per MT in a hot zone (invented)
    private static final double HYPOTHETICAL_EXPOSURE_RATE_FOR_ACUTE_TABLE = 1000; // mSv/hr for Table 1

    public Nuclear_Casualty_Calculator() {
        setTitle("Nuclear Casualty Estimator (Illustrative Model)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 960); // Adjusted width for wider tables
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        loadSettings();
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
        gbc.gridwidth = 1; // Reset gridwidth

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


        calculateButton.addActionListener(e -> performCalculation());
        saveButton.addActionListener(e -> saveSettings());
        loadButton.addActionListener(e -> loadSettings());
        defaultsButton.addActionListener(e -> restoreDefaultSettings());

        buttonPanel.add(calculateButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(defaultsButton);

        resultsArea = new JTextArea(40, 95); // Adjusted columns for wider tables
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
        updateInputPanels(); 
        resultsArea.setText("Input values restored to defaults.\n");
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

    // Helper class for Acute Radiation Effects Table
    private static class AcuteRadiationDoseEffect {
        double minDoseSv; // Min dose for this range (Sv)
        double maxDoseSv; // Max dose for this range (Sv)
        String effectDescription;

        public AcuteRadiationDoseEffect(double minDoseSv, double maxDoseSv, String effectDescription) {
            this.minDoseSv = minDoseSv;
            this.maxDoseSv = maxDoseSv;
            this.effectDescription = effectDescription;
        }

        public String getDoseRangeMsv() {
            if (maxDoseSv == Double.MAX_VALUE) return String.format("> %.0f mSv (> %.0f Sv)", minDoseSv * 1000, minDoseSv);
            if (minDoseSv == 0 && maxDoseSv == 0.25) return String.format("%.0f - %.0f mSv (0 - %.2f Sv)", minDoseSv*1000, maxDoseSv * 1000, maxDoseSv); // Special for first entry for alignment
            return String.format("%.0f - %.0f mSv (%.2f - %.1f Sv)", minDoseSv * 1000, maxDoseSv * 1000, minDoseSv, maxDoseSv);
        }
    }

    private List<AcuteRadiationDoseEffect> getAcuteRadiationEffectsData() {
        List<AcuteRadiationDoseEffect> effects = new ArrayList<>();
        effects.add(new AcuteRadiationDoseEffect(0, 0.25, "Few/no immediate symptoms. Possible slight blood changes. Increased long-term cancer risk."));
        effects.add(new AcuteRadiationDoseEffect(0.251, 1.0, "Mild: Nausea, vomiting in 10-50% within 3-6 hrs, lasting <24h. Fatigue. Temporary drop in lymphocytes. Full recovery usual."));
        effects.add(new AcuteRadiationDoseEffect(1.001, 2.0, "Moderate: Nausea, vomiting in 50-90% within 2-4 hrs, lasting 1-2 days. Hair loss (~2wks). Significant blood cell drops. Medical attention advised. Recovery likely."));
        effects.add(new AcuteRadiationDoseEffect(2.001, 4.0, "Severe: Nausea/vomiting <1hr (70-100%), diarrhea, fever. Severe hair loss, hemorrhage, infection risk. Hospitalization essential. ~15-50% mortality in 30d without treatment."));
        effects.add(new AcuteRadiationDoseEffect(4.001, 6.0, "Very Severe: Symptoms rapid. High mortality (~50-90% in 2-4wks) even with intensive care (LD50/30 range). Bone marrow destruction."));
        effects.add(new AcuteRadiationDoseEffect(6.001, 10.0, "Critical: Symptoms immediate/severe. Survival unlikely (~90-100% mortality). Gastrointestinal/cardiovascular collapse."));
        effects.add(new AcuteRadiationDoseEffect(10.001, Double.MAX_VALUE, "Lethal: Incapacitation (minutes-hour). Death (hours-2 days). Central Nervous System syndrome."));
        return effects;
    }
    
    private void generateAcuteRadiationEffectsTable(StringBuilder sb) {
        sb.append("\nAcute Radiation Effects (Illustrative):\n");
        sb.append(String.format("(Hypothetical time to dose based on continuous exposure at %,.0f mSv/hour)\n", HYPOTHETICAL_EXPOSURE_RATE_FOR_ACUTE_TABLE));
        
        String headerCol1 = "Dose Range";
        String headerCol2 = "Time to Max Dose";
        String headerCol3 = "Typical Acute Effects on Humans";
        int col1Width = 36; // Increased width for Dose Range
        int col2Width = 20;

        String dashes1 = "-".repeat(col1Width);
        String dashes2 = "-".repeat(col2Width);
        String dashes3 = "-".repeat(Math.max(headerCol3.length(), 45)); // Adjust length for effect header

        sb.append(String.format("%s+%s+%s\n", dashes1, dashes2, dashes3).replace(" ", "-")); // More solid line
        sb.append(String.format("%-" + col1Width + "s | %-" + col2Width + "s | %s\n", headerCol1, headerCol2, headerCol3));
        sb.append(String.format("%s+%s+%s\n", dashes1, dashes2, dashes3).replace(" ", "-"));

        List<AcuteRadiationDoseEffect> effectsData = getAcuteRadiationEffectsData();
        for (AcuteRadiationDoseEffect effect : effectsData) {
            double timeToMaxDoseHours = effect.maxDoseSv * 1000 / HYPOTHETICAL_EXPOSURE_RATE_FOR_ACUTE_TABLE;
            String timeStr;
            if (effect.maxDoseSv == Double.MAX_VALUE) {
                timeStr = String.format("> %.1f hrs", effect.minDoseSv * 1000 / HYPOTHETICAL_EXPOSURE_RATE_FOR_ACUTE_TABLE);
            } else if (timeToMaxDoseHours < 0.01) { // less than ~30 seconds
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

    private void generateFalloutDecayTable(StringBuilder sb, double initialFalloutRateH1, boolean isPeriodicScenario) {
        sb.append("\nIllustrative Fallout Decay & Dose Rate (Approximate t^-1.2 rule):\n");
        if (initialFalloutRateH1 <= 0.00001) { // Check for effectively zero rate
            sb.append("  (No significant fallout generating event modeled for this table / rate is effectively zero)\n");
            return;
        }
        sb.append(String.format("  (Based on an estimated initial HOT ZONE dose rate of %,.1f mSv/hr at H+1 hour)\n", initialFalloutRateH1));

        String headerCol1 = "Time Since Det.";
        String headerCol2 = "Decay Factor";
        String headerCol3 = "Approx. Dose Rate (mSv/hr)";
        int col1Width = 18;
        int col2Width = 15;
        
        String dashes1 = "-".repeat(col1Width);
        String dashes2 = "-".repeat(col2Width);
        String dashes3 = "-".repeat(headerCol3.length());

        sb.append(String.format("%s+%s+%s\n", dashes1, dashes2, dashes3).replace(" ", "-"));
        sb.append(String.format("%-" + col1Width + "s | %-" + col2Width + "s | %s\n", headerCol1, headerCol2, headerCol3));
        sb.append(String.format("%s+%s+%s\n", dashes1, dashes2, dashes3).replace(" ", "-"));

        double[] timePointsHours = {1, 7, 49, (49*7), (343*7), (2401*7), 8760}; // H+1h, H+7h, H+~2d, H+~2w, H+~3.5m, H+~1yr(approx)
        String[] timeLabels = {"H+1 Hour", "H+7 Hours", "H+2 Days (49h)", "H+2 Weeks (343h)", "H+3.5 Months (2401h)", "H+~1 Year (approx)"};
        // Corrected labels for more clarity on powers of 7
        timePointsHours = new double[]{1, 7, 7*7, 7*7*7, 7*7*7*7, 7*7*7*7*7, 8760}; // H+1h, H+7h, H+49h, H+343h, H+2401h, H+16807h, H+1yr
        timeLabels = new String[]{"H+1 Hour", "H+7 Hours", "H+49 Hours (~2d)", "H+343 Hours (~2w)", "H+2401 Hours (~3.5m)", "H+16807 Hrs (~23m)", "H+1 Year (8760h)"};


        for (int i = 0; i < timePointsHours.length; i++) {
            double t = timePointsHours[i];
            double decayFactor = Math.pow(t, -1.2); 
            if (t == 1) decayFactor = 1.0; 

            double currentRate = initialFalloutRateH1 * decayFactor;
            sb.append(String.format("%-" + col1Width + "s | %-" + col2Width + ".4f | %s\n", 
                                    timeLabels[i], 
                                    decayFactor, 
                                    doseRateFormat.format(currentRate)));
        }
        sb.append(String.format("%s+%s+%s\n", dashes1, dashes2, dashes3).replace(" ", "-"));
        sb.append("*Note: Represents ideal decay in a heavily affected, undisturbed area.\n");
        sb.append("  Actual rates vary (distance, shielding, weather, ground type).\n");
        if (isPeriodicScenario) {
            sb.append("  Periodic strikes would create NEW fallout, re-contaminating areas.\n");
        }
        sb.append("  Long-term accumulated dose requires integration of rates over exposure time.\n");
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
                if (totalYieldMT_forFalloutDecayTable < 0.00001) totalYieldMT_forFalloutDecayTable = 0; // Effectively zero for table if too small

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
                // Use totalYieldMT_forFalloutDecayTable as it's consistently derived total yield for fallout context
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
                // Determine initial impact yield used for this specific calculation section
                double initialImpactYieldMT_forTableSection = totalYieldMT_forFalloutDecayTable; // Default to total yield
                if (isPeriodicStrikeScenario && initialWarheadsForCalc < Integer.parseInt(warheadQuantityField.getText())) {
                    // If periodic, initial impact calculation for initial deaths uses only initial warheads
                    initialImpactYieldMT_forTableSection = (initialWarheadsForCalc * payloadKT_perWarhead / 1000.0);
                }
                long directBlastThermalDeaths = (long) (totalPopulation * Math.min(0.90, initialImpactYieldMT_forTableSection * 0.20));
                long injuriesLeadingToDeath = totalInitialDeaths - directBlastThermalDeaths;
                if (injuriesLeadingToDeath < 0) injuriesLeadingToDeath = 0; // Ensure not negative

                sb.append(formatTableRow("  Direct Blast/Thermal", df.format(directBlastThermalDeaths), pf.format((double) directBlastThermalDeaths / totalPopulation), ""));
                sb.append(formatTableRow("  From Severe Injuries", df.format(injuriesLeadingToDeath), pf.format((double) injuriesLeadingToDeath / totalPopulation), ""));
            } else {
                // For radiation input, base initial deaths on total population and input mSv factors
                double inputMSV = Double.parseDouble(radiationLevelField.getText());
                long directRadDeaths = (long) (totalPopulation * Math.min(0.95, inputMSV / 5000.0));
                long deathsFromSickness = totalInitialDeaths - directRadDeaths;
                if (deathsFromSickness < 0) deathsFromSickness = 0; // Ensure not negative

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