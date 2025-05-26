package com.json_beauty_compare;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonToolGUI extends JFrame {

    private JTextArea leftTextArea, rightTextArea;
    private JTextField leftTitleField, rightTitleField;
    private JPanel leftPanel, rightPanel;

    private JButton loadLeftButton, loadRightButton;
    private JButton beautifyButton, compareButton, beautifyAndCompareButton;
    private JCheckBox ignoreValueDifferencesCheckbox;

    private static final String SETTINGS_FILE = "json_settings.ini";
    private Properties settings;

    private String lastLeftFileLocation = "";
    private String lastRightFileLocation = "";

    // --- DifferenceDetail Inner Class ---
    static class DifferenceDetail {
        enum DiffType {
            MISSING_IN_LEFT, MISSING_IN_RIGHT, VALUE_MISMATCH, TYPE_MISMATCH, ARRAY_LENGTH_MISMATCH
        }
        DiffType type; String path; Object leftValue; Object rightValue;
        public DifferenceDetail(DiffType type, String path, Object leftValue, Object rightValue) {
            this.type = type; this.path = path; this.leftValue = leftValue; this.rightValue = rightValue;
        }
        public String getLeftValueString() { return valueToString(leftValue); }
        public String getRightValueString() { return valueToString(rightValue); }
        private String valueToString(Object val) {
            if (val == null || JSONObject.NULL.equals(val)) return "null";
            if (val instanceof String) return "\"" + val + "\""; return val.toString();
        }
        @Override
        public String toString() {
            String lValStr = getLeftValueString(); String rValStr = getRightValueString();
            String displayPath = path;
            switch (type) {
                case MISSING_IN_LEFT: return String.format("Path: %s (Present in Right as: %s)", displayPath, rValStr);
                case MISSING_IN_RIGHT: return String.format("Path: %s (Present in Left as: %s)", displayPath, lValStr);
                case VALUE_MISMATCH: return String.format("Path: %s\n  Left : %s\n  Right: %s", displayPath, lValStr, rValStr);
                case TYPE_MISMATCH:
                    String leftType = (leftValue == null || JSONObject.NULL.equals(leftValue)) ? "null" : leftValue.getClass().getSimpleName();
                    String rightType = (rightValue == null || JSONObject.NULL.equals(rightValue)) ? "null" : rightValue.getClass().getSimpleName();
                    return String.format("Path: %s (Type Mismatch)\n  Left : %s (%s)\n  Right: %s (%s)", displayPath, leftType, lValStr, rightType, rValStr);
                case ARRAY_LENGTH_MISMATCH: return String.format("Path: %s (Array Length Mismatch)\n  Left length : %s\n  Right length: %s", displayPath, lValStr, rValStr);
                default: return "Unknown difference at " + displayPath;
            }
        }
    }
    // --- End of DifferenceDetail Inner Class ---

    public JsonToolGUI() {
        setTitle("JSON Utility Tool"); setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 800); setLocationRelativeTo(null);
        initComponents(); settings = new Properties(); loadSettings(); applySettings(); layoutComponents(); addListeners();
    }

    private void initComponents() {
        leftTitleField = new JTextField("Before"); leftTextArea = new JTextArea();
        leftTextArea.setLineWrap(true); leftTextArea.setWrapStyleWord(true);
        loadLeftButton = new JButton("Load Left JSON");
        leftPanel = new JPanel(new BorderLayout(5, 5)); leftPanel.setBorder(new TitledBorder("Left JSON"));
        JPanel leftTopPanel = new JPanel(new BorderLayout(5,0));
        leftTopPanel.add(new JLabel("Title:"), BorderLayout.WEST); leftTopPanel.add(leftTitleField, BorderLayout.CENTER);
        leftTopPanel.add(loadLeftButton, BorderLayout.EAST); leftPanel.add(leftTopPanel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(leftTextArea), BorderLayout.CENTER);
        rightTitleField = new JTextField("After"); rightTextArea = new JTextArea();
        rightTextArea.setLineWrap(true); rightTextArea.setWrapStyleWord(true);
        loadRightButton = new JButton("Load Right JSON");
        rightPanel = new JPanel(new BorderLayout(5, 5)); rightPanel.setBorder(new TitledBorder("Right JSON"));
        JPanel rightTopPanel = new JPanel(new BorderLayout(5,0));
        rightTopPanel.add(new JLabel("Title:"), BorderLayout.WEST); rightTopPanel.add(rightTitleField, BorderLayout.CENTER);
        rightTopPanel.add(loadRightButton, BorderLayout.EAST); rightPanel.add(rightTopPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(rightTextArea), BorderLayout.CENTER);
        beautifyButton = new JButton("Beautify Both"); compareButton = new JButton("Compare");
        beautifyAndCompareButton = new JButton("Beautify & Compare");
        ignoreValueDifferencesCheckbox = new JCheckBox("Ignore Value Differences", false);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel mainInputPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        mainInputPanel.setBorder(new EmptyBorder(10,10,0,10));
        mainInputPanel.add(leftPanel); mainInputPanel.add(rightPanel); add(mainInputPanel, BorderLayout.CENTER);
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomButtonPanel.add(beautifyButton); bottomButtonPanel.add(compareButton);
        bottomButtonPanel.add(beautifyAndCompareButton); bottomButtonPanel.add(ignoreValueDifferencesCheckbox);
        add(bottomButtonPanel, BorderLayout.SOUTH);
    }

    private void addListeners() {
        loadLeftButton.addActionListener(e -> loadJsonFile(leftTextArea, true));
        loadRightButton.addActionListener(e -> loadJsonFile(rightTextArea, false));
        beautifyButton.addActionListener(e -> beautifyAllAction());
        compareButton.addActionListener(e -> compareAction());
        beautifyAndCompareButton.addActionListener(e -> beautifyAndCompareAction());
        leftTitleField.addActionListener(e -> updatePanelTitles());
        leftTitleField.addFocusListener(new java.awt.event.FocusAdapter() { public void focusLost(java.awt.event.FocusEvent evt) { updatePanelTitles(); }});
        rightTitleField.addActionListener(e -> updatePanelTitles());
        rightTitleField.addFocusListener(new java.awt.event.FocusAdapter() { public void focusLost(java.awt.event.FocusEvent evt) { updatePanelTitles(); }});
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { saveSettings(); dispose(); System.exit(0); }});
    }

    private void updatePanelTitles() {
        ((TitledBorder) leftPanel.getBorder()).setTitle(leftTitleField.getText()); leftPanel.repaint();
        ((TitledBorder) rightPanel.getBorder()).setTitle(rightTitleField.getText()); rightPanel.repaint();
    }

    private void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                settings.load(input);
                lastLeftFileLocation = settings.getProperty("left.file.location", "");
                lastRightFileLocation = settings.getProperty("right.file.location", "");
                ignoreValueDifferencesCheckbox.setSelected(Boolean.parseBoolean(settings.getProperty("ignore.values", "false")));
            } catch (IOException ex) { JOptionPane.showMessageDialog(this, "Error loading settings: " + ex.getMessage(), "Settings Error", JOptionPane.ERROR_MESSAGE); }
        } else { settings.setProperty("ignore.values", Boolean.toString(ignoreValueDifferencesCheckbox.isSelected())); }
    }

    private void applySettings() {
        leftTitleField.setText(settings.getProperty("left.title", "Before"));
        rightTitleField.setText(settings.getProperty("right.title", "After"));
        updatePanelTitles();
    }

    private void saveSettings() {
        settings.setProperty("left.title", leftTitleField.getText());
        settings.setProperty("right.title", rightTitleField.getText());
        settings.setProperty("left.file.location", lastLeftFileLocation);
        settings.setProperty("right.file.location", lastRightFileLocation);
        settings.setProperty("ignore.values", Boolean.toString(ignoreValueDifferencesCheckbox.isSelected()));
        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) { settings.store(output, "JSON Tool Settings"); }
        catch (IOException ex) { JOptionPane.showMessageDialog(this, "Error saving settings: " + ex.getMessage(), "Settings Error", JOptionPane.ERROR_MESSAGE); }
    }

    private void loadJsonFile(JTextArea targetTextArea, boolean isLeft) {
        JFileChooser fileChooser = new JFileChooser(); String lastDir = isLeft ? lastLeftFileLocation : lastRightFileLocation;
        if (lastDir != null && !lastDir.isEmpty()) {
            File currentDirCandidate = new File(lastDir);
            if (currentDirCandidate.isDirectory()) fileChooser.setCurrentDirectory(currentDirCandidate);
            else if (currentDirCandidate.getParentFile() != null && currentDirCandidate.getParentFile().isDirectory()) fileChooser.setCurrentDirectory(currentDirCandidate.getParentFile());
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON & Text Files", "json", "txt");
        fileChooser.setFileFilter(filter);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                targetTextArea.setText(new String(Files.readAllBytes(selectedFile.toPath())));
                if (isLeft) lastLeftFileLocation = selectedFile.getParent(); else lastRightFileLocation = selectedFile.getParent();
            } catch (IOException ex) { JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private String beautifyJson(String jsonString) throws JSONException {
        if (jsonString == null || jsonString.trim().isEmpty()) return "";
        String trimmedJsonString = jsonString.trim(); Object parsed;
        if (trimmedJsonString.startsWith("{")) { parsed = new JSONObject(trimmedJsonString); processJsonObjectForNestedBeautification((JSONObject) parsed); }
        else if (trimmedJsonString.startsWith("[")) { parsed = new JSONArray(trimmedJsonString); processJsonArrayForNestedBeautification((JSONArray) parsed); }
        else { throw new JSONException("Input string is not a valid JSON Object or Array."); }
        if (parsed instanceof JSONObject) return ((JSONObject)parsed).toString(2); return ((JSONArray)parsed).toString(2);
    }
    private void processJsonObjectForNestedBeautification(JSONObject jsonObj) throws JSONException {
        for (String key : jsonObj.keySet()) { jsonObj.put(key, tryBeautifyValue(jsonObj.get(key))); }
    }
    private void processJsonArrayForNestedBeautification(JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) { jsonArray.put(i, tryBeautifyValue(jsonArray.get(i))); }
    }
    private Object tryBeautifyValue(Object value) throws JSONException {
        if (value instanceof String) {
            String strValue = ((String) value).trim();
            if (strValue.startsWith("{") && strValue.endsWith("}")) { try { JSONObject nestedObj = new JSONObject(strValue); processJsonObjectForNestedBeautification(nestedObj); return nestedObj; } catch (JSONException e) { return value; } }
            else if (strValue.startsWith("[") && strValue.endsWith("]")) { try { JSONArray nestedArr = new JSONArray(strValue); processJsonArrayForNestedBeautification(nestedArr); return nestedArr; } catch (JSONException e) { return value; } }
            return value;
        } else if (value instanceof JSONObject) { processJsonObjectForNestedBeautification((JSONObject) value); return value; }
        else if (value instanceof JSONArray) { processJsonArrayForNestedBeautification((JSONArray) value); return value; }
        return value;
    }

    private void beautifyAllAction() {
        String errorMessages = ""; boolean beautified = false;
        if (!leftTextArea.getText().trim().isEmpty()) { try { leftTextArea.setText(beautifyJson(leftTextArea.getText())); beautified = true; } catch (JSONException e) { errorMessages += "Left JSON Error: " + e.getMessage() + "\n"; } }
        if (!rightTextArea.getText().trim().isEmpty()) { try { rightTextArea.setText(beautifyJson(rightTextArea.getText())); beautified = true; } catch (JSONException e) { errorMessages += "Right JSON Error: " + e.getMessage() + "\n"; } }
        if (!errorMessages.isEmpty()) JOptionPane.showMessageDialog(this, errorMessages, "Beautify Error", JOptionPane.ERROR_MESSAGE);
        else if (beautified) JOptionPane.showMessageDialog(this, "JSON(s) beautified successfully!", "Beautify Success", JOptionPane.INFORMATION_MESSAGE);
        else JOptionPane.showMessageDialog(this, "Nothing to beautify (text areas are empty).", "Beautify Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private Object parseJsonStringToObject(String jsonString) throws JSONException {
        if (jsonString == null || jsonString.trim().isEmpty()) { throw new JSONException("JSON string is empty or null."); }
        String trimmedJsonString = jsonString.trim(); Object parsed;
        if (trimmedJsonString.startsWith("{")) { parsed = new JSONObject(trimmedJsonString); processJsonObjectForNestedBeautification((JSONObject) parsed); }
        else if (trimmedJsonString.startsWith("[")) { parsed = new JSONArray(trimmedJsonString); processJsonArrayForNestedBeautification((JSONArray) parsed); }
        else { throw new JSONException("String is not a valid JSON Object or Array for comparison."); }
        return parsed;
    }

    private List<DifferenceDetail> findDifferences(Object obj1, Object obj2, String currentPath, boolean ignoreValues) {
        List<DifferenceDetail> diffs = new ArrayList<>();
        boolean obj1IsNull = (obj1 == null || JSONObject.NULL.equals(obj1)); boolean obj2IsNull = (obj2 == null || JSONObject.NULL.equals(obj2));
        if (obj1IsNull && obj2IsNull) return diffs;
        if (obj1IsNull) { diffs.add(new DifferenceDetail(DifferenceDetail.DiffType.MISSING_IN_LEFT, currentPath, obj1, obj2)); return diffs; }
        if (obj2IsNull) { diffs.add(new DifferenceDetail(DifferenceDetail.DiffType.MISSING_IN_RIGHT, currentPath, obj1, obj2)); return diffs; }
        if (!obj1.getClass().equals(obj2.getClass())) { diffs.add(new DifferenceDetail(DifferenceDetail.DiffType.TYPE_MISMATCH, currentPath, obj1, obj2)); return diffs; }
        if (obj1 instanceof JSONObject) {
            JSONObject jsonObj1 = (JSONObject) obj1; JSONObject jsonObj2 = (JSONObject) obj2;
            Set<String> keys1 = jsonObj1.keySet(); Set<String> keys2 = jsonObj2.keySet(); Set<String> allKeys = new HashSet<>(keys1); allKeys.addAll(keys2);
            for (String key : allKeys) {
                String newPath = currentPath.isEmpty() ? key : currentPath + "." + key; Object val1 = jsonObj1.opt(key); Object val2 = jsonObj2.opt(key);
                if (jsonObj1.has(key) && !jsonObj2.has(key)) { diffs.add(new DifferenceDetail(DifferenceDetail.DiffType.MISSING_IN_RIGHT, newPath, val1, JSONObject.NULL)); }
                else if (!jsonObj1.has(key) && jsonObj2.has(key)) { diffs.add(new DifferenceDetail(DifferenceDetail.DiffType.MISSING_IN_LEFT, newPath, JSONObject.NULL, val2)); }
                else { diffs.addAll(findDifferences(val1, val2, newPath, ignoreValues)); }
            }
        } else if (obj1 instanceof JSONArray) {
            JSONArray jsonArr1 = (JSONArray) obj1; JSONArray jsonArr2 = (JSONArray) obj2;
            int len1 = jsonArr1.length(); int len2 = jsonArr2.length();
            if (len1 != len2) { diffs.add(new DifferenceDetail(DifferenceDetail.DiffType.ARRAY_LENGTH_MISMATCH, currentPath, len1, len2)); }
            int minLen = Math.min(len1, len2);
            for (int i = 0; i < minLen; i++) { diffs.addAll(findDifferences(jsonArr1.get(i), jsonArr2.get(i), currentPath + "[" + i + "]", ignoreValues)); }
        } else { if (!obj1.equals(obj2)) { if (!ignoreValues) { diffs.add(new DifferenceDetail(DifferenceDetail.DiffType.VALUE_MISMATCH, currentPath, obj1, obj2)); } } }
        return diffs;
    }

    private void compareAction() {
        String leftJsonStr = leftTextArea.getText(); String rightJsonStr = rightTextArea.getText();
        if (leftJsonStr.trim().isEmpty() || rightJsonStr.trim().isEmpty()) { JOptionPane.showMessageDialog(this, "Both JSON fields must contain text to compare.", "Comparison Error", JOptionPane.WARNING_MESSAGE); return; }
        Object parsedLeft, parsedRight; String beautifiedLeft = "", beautifiedRight = "";
        try { parsedLeft = parseJsonStringToObject(leftJsonStr); beautifiedLeft = (parsedLeft instanceof JSONObject) ? ((JSONObject)parsedLeft).toString(2) : ((JSONArray)parsedLeft).toString(2); }
        catch (JSONException e) { JOptionPane.showMessageDialog(this, "Left JSON is invalid: " + e.getMessage(), "Comparison Error", JOptionPane.ERROR_MESSAGE); return; }
        try { parsedRight = parseJsonStringToObject(rightJsonStr); beautifiedRight = (parsedRight instanceof JSONObject) ? ((JSONObject)parsedRight).toString(2) : ((JSONArray)parsedRight).toString(2); }
        catch (JSONException e) { JOptionPane.showMessageDialog(this, "Right JSON is invalid: " + e.getMessage(), "Comparison Error", JOptionPane.ERROR_MESSAGE); return; }
        List<DifferenceDetail> differences = findDifferences(parsedLeft, parsedRight, "", ignoreValueDifferencesCheckbox.isSelected());
        if (differences.isEmpty()) { JOptionPane.showMessageDialog(this, "The JSON structures are semantically identical.", "Comparison Result", JOptionPane.INFORMATION_MESSAGE); }
        else { new ComparisonResultDialog(this, leftTitleField.getText(), rightTitleField.getText(), beautifiedLeft, beautifiedRight, differences).setVisible(true); }
    }

    private void beautifyAndCompareAction() {
        String originalLeft = leftTextArea.getText(); String originalRight = rightTextArea.getText();
        String errorMessages = ""; boolean leftOk = false, rightOk = false;
        if (!originalLeft.trim().isEmpty()) { try { leftTextArea.setText(beautifyJson(originalLeft)); leftOk = true; } catch (JSONException e) { errorMessages += "Left JSON Error during beautify: " + e.getMessage() + "\n"; } }
        else errorMessages += "Left JSON is empty.\n";
        if (!originalRight.trim().isEmpty()) { try { rightTextArea.setText(beautifyJson(originalRight)); rightOk = true; } catch (JSONException e) { errorMessages += "Right JSON Error during beautify: " + e.getMessage() + "\n"; } }
        else errorMessages += "Right JSON is empty.\n";
        if (!leftOk || !rightOk) {
            String finalMessage = "Cannot compare due to errors or empty fields:\n" + errorMessages;
            JOptionPane.showMessageDialog(this, finalMessage, "Beautify & Compare Info/Error", JOptionPane.WARNING_MESSAGE);
            if (!leftOk && !originalLeft.trim().isEmpty()) leftTextArea.setText(originalLeft);
            if (!rightOk && !originalRight.trim().isEmpty()) rightTextArea.setText(originalRight); return;
        }
        compareAction();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e) { System.err.println("Couldn't set system LookAndFeel: " + e); }
            new JsonToolGUI().setVisible(true);
        });
    }

    // --- ComparisonResultDialog Inner Class (was separate, now inner for single file) ---
    static class ComparisonResultDialog extends JDialog {
        // More distinct default highlight colors
        private final Color HIGHLIGHT_COLOR_VALUE_MISMATCH = new Color(255, 255, 153); // Brighter Yellow
        private final Color HIGHLIGHT_COLOR_MISSING_KEY_CONTEXT = new Color(255, 179, 179); // Brighter Pink/Red
        private final Color HIGHLIGHT_COLOR_TYPE_MISMATCH = new Color(179, 217, 255); // Brighter Blue

        public ComparisonResultDialog(Frame owner, String guiLeftTitle, String guiRightTitle,
                                      String leftJson, String rightJson,
                                      List<JsonToolGUI.DifferenceDetail> differences) {
            super(owner, "Comparison: '" + guiLeftTitle + "' vs '" + guiRightTitle + "'", true);
            setSize(1000, 700); setLocationRelativeTo(owner); setLayout(new BorderLayout(5,5));

            JTextPane leftJsonPane = createJsonTextPane(leftJson);
            JTextPane rightJsonPane = createJsonTextPane(rightJson);
            JTextArea leftMissingKeysArea = new JTextArea(5, 40);
            JTextArea rightMissingKeysArea = new JTextArea(5, 40);
            configureMissingKeysArea(leftMissingKeysArea, "Keys/Paths missing from '" + guiLeftTitle + "' (present in '" + guiRightTitle + "')");
            configureMissingKeysArea(rightMissingKeysArea, "Keys/Paths missing from '" + guiRightTitle + "' (present in '" + guiLeftTitle + "')");

            for (JsonToolGUI.DifferenceDetail diff : differences) {
                String displayPath = diff.path;
                if (displayPath.startsWith("data.")) {
                    displayPath = displayPath.substring("data.".length());
                    if (displayPath.isEmpty() && diff.path.equals("data")) {
                        displayPath = "(root 'data' object differs)";
                    } else if (displayPath.isEmpty()){
                        displayPath = diff.path;
                    }
                }

                switch (diff.type) {
                    case MISSING_IN_LEFT:
                        leftMissingKeysArea.append("• " + displayPath + " (Value in right: " + diff.getRightValueString() + ")\n");
                        highlightLineContainingPath(rightJsonPane, diff.path, HIGHLIGHT_COLOR_MISSING_KEY_CONTEXT);
                        break;
                    case MISSING_IN_RIGHT:
                        rightMissingKeysArea.append("• " + displayPath + " (Value in left: " + diff.getLeftValueString() + ")\n");
                        highlightLineContainingPath(leftJsonPane, diff.path, HIGHLIGHT_COLOR_MISSING_KEY_CONTEXT);
                        break;
                    case VALUE_MISMATCH:
                        highlightLineContainingPath(leftJsonPane, diff.path, HIGHLIGHT_COLOR_VALUE_MISMATCH);
                        highlightLineContainingPath(rightJsonPane, diff.path, HIGHLIGHT_COLOR_VALUE_MISMATCH);
                        break;
                    case TYPE_MISMATCH:
                    case ARRAY_LENGTH_MISMATCH: // Treat array length mismatch similar to type for highlighting
                        highlightLineContainingPath(leftJsonPane, diff.path, HIGHLIGHT_COLOR_TYPE_MISMATCH);
                        highlightLineContainingPath(rightJsonPane, diff.path, HIGHLIGHT_COLOR_TYPE_MISMATCH);
                        break;
                }
            }

            JPanel topSplitPanel = new JPanel(new GridLayout(1,2,5,5));
            JScrollPane leftJsonScrollPane = new JScrollPane(leftJsonPane);
            leftJsonScrollPane.setBorder(new TitledBorder(guiLeftTitle + " (Beautified)"));
            topSplitPanel.add(leftJsonScrollPane);
            JScrollPane rightJsonScrollPane = new JScrollPane(rightJsonPane);
            rightJsonScrollPane.setBorder(new TitledBorder(guiRightTitle + " (Beautified)"));
            topSplitPanel.add(rightJsonScrollPane);

            JPanel bottomMissingKeysPanel = new JPanel(new GridLayout(1,2,5,5));
            bottomMissingKeysPanel.add(new JScrollPane(leftMissingKeysArea));
            bottomMissingKeysPanel.add(new JScrollPane(rightMissingKeysArea));

            JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplitPanel, bottomMissingKeysPanel);
            mainSplitPane.setResizeWeight(0.75); mainSplitPane.setDividerLocation(0.75);
            add(mainSplitPane, BorderLayout.CENTER);

            JButton closeButton = new JButton("Close"); closeButton.addActionListener(e -> dispose());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttonPanel.add(closeButton);
            add(buttonPanel, BorderLayout.SOUTH);
        }

        private JTextPane createJsonTextPane(String json) {
            JTextPane textPane = new JTextPane(); textPane.setContentType("text/plain");
            textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); textPane.setEditable(false);
            textPane.setText(json); textPane.setCaretPosition(0); return textPane;
        }

        private void configureMissingKeysArea(JTextArea textArea, String title) {
            textArea.setBorder(new TitledBorder(title)); textArea.setEditable(false);
            textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            textArea.setLineWrap(true); textArea.setWrapStyleWord(true);
        }

        private void highlightLineContainingPath(JTextPane textPane, String fullPath, Color color) {
            if (fullPath == null || fullPath.isEmpty()) return;

            // The key to search for in the text is the last segment of the path.
            // For path "data.user.name", keyToSearch is "name".
            // For path "data.items[0].id", keyToSearch is "id".
            // For path "data.items[0]", keyToSearch is "items[0]" (or just "items" for broader array match).
            // For path "data.items", keyToSearch is "items".
            String keyToSearch = fullPath;
            if (fullPath.contains(".")) {
                keyToSearch = fullPath.substring(fullPath.lastIndexOf('.') + 1);
            }
            // For "items[0]", we want to find lines starting with "items": or the element itself.
            // A simpler regex for the line start:
            String regexKeyPart = keyToSearch;
            if (keyToSearch.contains("[")) { // e.g. "items[0]"
                regexKeyPart = keyToSearch.substring(0, keyToSearch.indexOf('[')); // "items"
            }


            Highlighter highlighter = textPane.getHighlighter();
            Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(color);
            Document doc = textPane.getDocument();
            String textContent;
            try {
                textContent = doc.getText(0, doc.getLength());
            } catch (BadLocationException e) {
                System.err.println("Error getting text from JTextPane for highlighting: " + e.getMessage());
                return;
            }

            // Regex to find lines like: "key": value  OR  "key" : value OR "arrayKey": [
            // It tries to match the start of a line that declares the key.
            // Using Pattern.quote on regexKeyPart to handle special characters in keys.
            Pattern pattern = Pattern.compile("^\\s*\"" + Pattern.quote(regexKeyPart) + "\"\\s*:\\s*.*$", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(textContent);

            while (matcher.find()) {
                try {
                    // For array elements like "items[0]", the regex using 'regexKeyPart' ("items")
                    // will highlight the line where "items": [...] is declared.
                    // A more precise highlight for the *specific element* "items[0]" inside the array
                    // is much harder without a full JSON parser that tracks line/char numbers for each token.
                    // This line-based approach is a good first step.
                    highlighter.addHighlight(matcher.start(), matcher.end(), painter);
                } catch (BadLocationException e) {
                    // This should ideally not happen if matcher.start/end are valid.
                     System.err.println("BadLocationException during highlight: " + e.getMessage() + " for path " + fullPath);
                }
            }
        }
    }
}