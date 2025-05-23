package com.deal.gui;

import com.deal.logic.GameLogic;
import com.deal.model.Bag;
import com.deal.model.GameSettings;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class GamePanel extends JPanel {
    private GameLogic gameLogic;
    private GameSettings settings;
    private GameFrame parentFrame;

    private Map<Integer, JButton> bagButtons; // Map bag ID to its button
    private JLabel chosenBagDisplay;
    private JPanel bagGridPanel;

    private JTextArea openedBagValuesDisplay; // For tracking opened bags
    private JScrollPane openedBagScrollPane;

    private JTextArea offerHistoryDisplay; // New: For tracking banker offers
    private JScrollPane offerHistoryScrollPane;

    private JButton newGameButton; // Only New Game/Settings button remains on main panel

    private NumberFormat currencyFormat; // This is already doing the job!

    public GamePanel(GameLogic gameLogic, GameSettings settings, GameFrame parentFrame) {
        this.gameLogic = gameLogic;
        this.settings = settings;
        this.parentFrame = parentFrame;
        this.bagButtons = new HashMap<>();
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US); // Ensures currency symbol and thousand separators

        setLayout(new BorderLayout(10, 10)); // Main layout for the game panel
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Panel: Info Area ---
        JPanel infoPanel = new JPanel(new BorderLayout());
        chosenBagDisplay = new JLabel("Your Chosen Bag: None", SwingConstants.CENTER);
        chosenBagDisplay.setFont(new Font("Arial", Font.BOLD, 18));
        infoPanel.add(chosenBagDisplay, BorderLayout.NORTH);
        // Banker offer display removed from here, handled by dialog

        add(infoPanel, BorderLayout.NORTH);

        // --- Center Panel: Bags Grid ---
        bagGridPanel = new JPanel();
        add(bagGridPanel, BorderLayout.CENTER);

        // --- Right Panels Container (Tracking + Offers) ---
        JPanel sidePanelsContainer = new JPanel();
        sidePanelsContainer.setLayout(new BoxLayout(sidePanelsContainer, BoxLayout.Y_AXIS)); // Stack panels vertically

        // Opened Bag Values Panel
        JPanel openedBagsPanel = new JPanel(new BorderLayout());
        openedBagsPanel.setBorder(BorderFactory.createTitledBorder("Opened Bag Values"));
        openedBagValuesDisplay = new JTextArea(15, 25); // Increased columns for more room
        openedBagValuesDisplay.setEditable(false);
        openedBagValuesDisplay.setFont(new Font("Monospaced", Font.PLAIN, 12));
        openedBagScrollPane = new JScrollPane(openedBagValuesDisplay);
        openedBagScrollPane.setPreferredSize(new Dimension(250, 300)); // Set a preferred size for the scroll pane
        openedBagsPanel.add(openedBagScrollPane, BorderLayout.CENTER);
        sidePanelsContainer.add(openedBagsPanel);

        // Offer History Panel (New)
        JPanel offerHistoryPanel = new JPanel(new BorderLayout());
        offerHistoryPanel.setBorder(BorderFactory.createTitledBorder("Banker Offer History"));
        offerHistoryDisplay = new JTextArea(10, 25); // Set rows/cols
        offerHistoryDisplay.setEditable(false);
        offerHistoryDisplay.setFont(new Font("Monospaced", Font.PLAIN, 12));
        offerHistoryScrollPane = new JScrollPane(offerHistoryDisplay);
        offerHistoryScrollPane.setPreferredSize(new Dimension(250, 200)); // Set a preferred size
        offerHistoryPanel.add(offerHistoryScrollPane, BorderLayout.CENTER);
        sidePanelsContainer.add(offerHistoryPanel);

        add(sidePanelsContainer, BorderLayout.EAST); // Add the container to the EAST

        // --- Bottom Panel: Action Button ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        newGameButton = new JButton("New Game / Settings");
        newGameButton.addActionListener(e -> parentFrame.showSettingsPanel());
        actionPanel.add(newGameButton);
        add(actionPanel, BorderLayout.SOUTH);

        // Initial setup
        updateGUI();
    }

    public void updateGUI() {
        // 1. Rebuild bag grid panel
        bagGridPanel.removeAll();
        bagButtons.clear(); // Clear the map of old buttons

        int numBags = settings.getNumberOfBags();
        int cols = (int) Math.ceil(Math.sqrt(numBags));
        int rows = (int) Math.ceil((double) numBags / cols);
        bagGridPanel.setLayout(new GridLayout(rows, cols, 5, 5));

        // Create buttons for each bag
        for (Bag bag : gameLogic.getAllBags()) {
            JButton bagButton = new JButton(String.valueOf(bag.getId()));
            bagButton.setFont(new Font("Arial", Font.BOLD, 20));
            bagButton.setPreferredSize(new Dimension(80, 80));

            bagButtons.put(bag.getId(), bagButton); // Store button by bag ID

            // Add action listener
            bagButton.addActionListener(e -> handleBagClick(bag.getId())); // Always add listener

            bagGridPanel.add(bagButton);
        }

        // 2. Update button states and texts based on current game logic state
        updateBagButtonStates();

        // 3. Update top info display
        if (gameLogic.getPlayerChosenBag() != null) {
            chosenBagDisplay.setText("Your Chosen Bag: #" + gameLogic.getPlayerChosenBag().getId());
        } else {
            chosenBagDisplay.setText("Your Chosen Bag: Select One!");
        }

        // 4. Update tracking displays
        updateOpenedBagValuesDisplay();
        openedBagScrollPane.setVisible(settings.getTrackingMode() != 0); // Hide/Show based on setting

        updateOfferHistoryDisplay();
        offerHistoryScrollPane.setVisible(true); // Always show offer history for now, could be setting-driven too

        // 5. Handle game over state
        if (gameLogic.isGameOver()) {
            setAllBagButtonsEnabled(false); // Disable all bags
            newGameButton.setEnabled(true); // Allow starting a new game
        } else {
            newGameButton.setEnabled(true); // Always callable unless during offer
        }

        // Disable new game button during offer state
        if (gameLogic.getCurrentState() == GameLogic.GameState.BANKER_OFFER) {
            newGameButton.setEnabled(false);
        }

        revalidate();
        repaint();
    }

    /**
     * Iterates through all bags and updates the text, color, and enabled state of their corresponding buttons.
     */
    private void updateBagButtonStates() {
        GameLogic.GameState currentState = gameLogic.getCurrentState();
        Bag playerChosenBag = gameLogic.getPlayerChosenBag();

        for (Bag bag : gameLogic.getAllBags()) {
            JButton button = bagButtons.get(bag.getId());
            if (button == null) continue; // Should not happen if map is correctly populated

            if (bag.isOpened()) {
                button.setBackground(Color.LIGHT_GRAY);
                button.setForeground(Color.DARK_GRAY);
                // *** CHANGE HERE: Format value as currency ***
                button.setText(currencyFormat.format(bag.getValue()));
                button.setEnabled(false); // Opened bags are always disabled
            } else if (bag.isChosen()) {
                button.setBackground(Color.YELLOW);
                button.setForeground(Color.BLACK);
                button.setText("Your Case");
                // The chosen bag is only clickable during INITIAL_CHOICE (to select it).
                // Once chosen, it's not clickable for opening, only implicitly opened at game end.
                button.setEnabled(currentState == GameLogic.GameState.INITIAL_CHOICE);
            } else {
                // Unopened, non-chosen bag
                button.setBackground(new Color(173, 216, 230)); // Light blue
                button.setForeground(Color.BLACK);
                // Enable if in initial choice or opening bags state, and not during banker offer
                button.setEnabled(currentState == GameLogic.GameState.INITIAL_CHOICE || currentState == GameLogic.GameState.OPENING_BAGS);
            }
        }
    }

    /**
     * Helper method to enable/disable all bag buttons, used for general game state transitions (e.g., banker offer, game over).
     * @param enabled True to enable, false to disable.
     */
    private void setAllBagButtonsEnabled(boolean enabled) {
        for (JButton button : bagButtons.values()) {
            button.setEnabled(enabled);
        }
    }

    private void updateOpenedBagValuesDisplay() {
        StringBuilder sb = new StringBuilder();
        int trackingMode = settings.getTrackingMode();

        if (trackingMode == 0) { // No tracking
            openedBagValuesDisplay.setText("");
            return;
        }

        sb.append("Opened Bags:\n");
        gameLogic.getOpenedBags().stream()
                .sorted((b1, b2) -> Integer.compare(b1.getValue(), b2.getValue()))
                .forEach(bag -> {
                    if (trackingMode == 1) { // Full tracking
                        sb.append(String.format("  #%d: %s\n", bag.getId(), currencyFormat.format(bag.getValue())));
                    } else if (trackingMode == 2) { // Value-only tracking
                        sb.append(String.format("  %s\n", currencyFormat.format(bag.getValue())));
                    }
                });

        sb.append("\nRemaining Unopened Values:\n");
        gameLogic.getRemainingUnopenedBagValues().stream()
                .filter(b -> !b.isChosen() && !b.isOpened()) // Only unchosen, unopened bags
                .sorted((b1, b2) -> Integer.compare(b1.getValue(), b2.getValue()))
                .forEach(bag -> {
                    if (trackingMode == 1) { // Full tracking
                        sb.append(String.format("  #%d: %s\n", bag.getId(), currencyFormat.format(bag.getValue())));
                    } else if (trackingMode == 2) { // Value-only tracking
                        sb.append(String.format("  %s\n", currencyFormat.format(bag.getValue())));
                    }
                });

        if (gameLogic.isGameOver() && gameLogic.getPlayerChosenBag() != null) { // Only show chosen bag value at game over
            sb.append("\nYour Chosen Bag Value:\n");
            sb.append(String.format("  #%d: %s\n", gameLogic.getPlayerChosenBag().getId(), currencyFormat.format(gameLogic.getPlayerChosenBag().getValue())));
        }
        openedBagValuesDisplay.setText(sb.toString());
        openedBagValuesDisplay.setCaretPosition(0); // Scroll to top
    }

    private void updateOfferHistoryDisplay() {
        StringBuilder sb = new StringBuilder();
        if (gameLogic.getBankerOfferHistory().isEmpty()) {
            sb.append("No offers yet.");
        } else {
            sb.append("Offer History:\n");
            for (int i = 0; i < gameLogic.getBankerOfferHistory().size(); i++) {
                sb.append(String.format("  Offer %d: %s\n", (i + 1), currencyFormat.format(gameLogic.getBankerOfferHistory().get(i))));
            }
        }
        offerHistoryDisplay.setText(sb.toString());
        offerHistoryDisplay.setCaretPosition(0); // Scroll to top
    }


    private void handleBagClick(int bagId) {
        if (gameLogic.getCurrentState() == GameLogic.GameState.INITIAL_CHOICE) {
            // Player is choosing their main bag
            gameLogic.selectInitialBag(bagId);
            updateGUI(); // Re-render to show chosen bag and enable other bags for opening
            return;
        }

        if (gameLogic.getCurrentState() == GameLogic.GameState.OPENING_BAGS) {
            Bag clickedBag = gameLogic.getAllBags().stream().filter(b -> b.getId() == bagId).findFirst().orElse(null);

            if (clickedBag == null || clickedBag.isOpened() || clickedBag.isChosen()) {
                return; // Cannot click an opened bag or the chosen bag for these actions.
            }

            String[] options = {"Open Bag", "Swap with My Chosen Bag", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "What do you want to do with Bag #" + bagId + "?",
                    "Bag Action",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 0) { // Open Bag
                Bag openedBag = gameLogic.openBag(bagId);
                if (openedBag != null) {
                    // *** CHANGE HERE: Format value as currency ***
                    JOptionPane.showMessageDialog(this, "Bag #" + openedBag.getId() + " contained: " + currencyFormat.format(openedBag.getValue()), "Bag Opened!", JOptionPane.INFORMATION_MESSAGE);
                    if (gameLogic.shouldBankerOffer()) {
                        long currentOffer = gameLogic.calculateBankerOffer();
                        gameLogic.addBankerOfferToHistory(currentOffer); // Add to history before showing dialog
                        showBankerOfferDialog(currentOffer);
                    } else if (gameLogic.isGameOver()) {
                        handleGameOver(false);
                    }
                }
            } else if (choice == 1) { // Swap with My Chosen Bag
                if (gameLogic.getPlayerChosenBag() == null) {
                    JOptionPane.showMessageDialog(this, "Error: Your chosen bag is not set. This shouldn't happen.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int oldChosenBagId = gameLogic.getPlayerChosenBag().getId();
                Bag newChosenBag = gameLogic.swapBags(bagId);
                if (newChosenBag != null) {
                    JOptionPane.showMessageDialog(this, "You swapped your Bag #" + oldChosenBagId + " for Bag #" + newChosenBag.getId() + "!", "Swap Complete", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to swap bags.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            // If choice is 2 (Cancel), do nothing.

            updateGUI(); // Always update GUI after an action
        }
    }

    private void showBankerOfferDialog(long offer) {
        setAllBagButtonsEnabled(false); // Disable all bag buttons while offer dialog is open

        // Custom panel for the dialog content
        JPanel offerPanel = new JPanel(new BorderLayout(10, 10));
        JLabel offerLabel = new JLabel("Banker's Offer: " + currencyFormat.format(offer), SwingConstants.CENTER);
        offerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        offerLabel.setForeground(new Color(0, 100, 0)); // Dark green color

        offerPanel.add(offerLabel, BorderLayout.CENTER);
        offerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Options for the dialog
        String[] options = {"DEAL!", "NO DEAL!"};
        int choice = JOptionPane.showOptionDialog(
                this,
                offerPanel, // Pass the custom panel
                "BANKER'S OFFER!",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, // No custom icon
                options,
                options[0] // Default button
        );

        if (choice == JOptionPane.YES_OPTION) { // DEAL!
            handleDeal(offer);
        } else { // NO DEAL! (includes closing the dialog)
            handleNoDeal();
        }
        updateGUI(); // Update GUI after dialog closes
    }

    private void handleDeal(long offeredAmount) {
        if (gameLogic.getCurrentState() == GameLogic.GameState.BANKER_OFFER) {
            JOptionPane.showMessageDialog(this, "You DEALT for: " + currencyFormat.format(offeredAmount) + "!", "DEAL!", JOptionPane.INFORMATION_MESSAGE);
            gameLogic.endGame();
            handleGameOver(true);
        }
    }

    private void handleNoDeal() {
        if (gameLogic.getCurrentState() == GameLogic.GameState.BANKER_OFFER) {
            JOptionPane.showMessageDialog(this, "NO DEAL! Continue playing.", "NO DEAL!", JOptionPane.INFORMATION_MESSAGE);
            gameLogic.resumeOpeningBags();
            updateGUI();
            // Check for game over *after* resuming if this "no deal" was the penultimate choice
            if (gameLogic.isGameOver()) {
                handleGameOver(false);
            }
        }
    }

    private void handleGameOver(boolean dealt) {
        gameLogic.endGame(); // Set game state to Game_Over
        Bag chosenBag = gameLogic.getPlayerChosenBag();
        String finalMessage;
        if (dealt) {
            // The offer value was already presented and captured by handleDeal
            finalMessage = "Congratulations! You accepted the deal of " + currencyFormat.format(gameLogic.getBankerOfferHistory().get(gameLogic.getBankerOfferHistory().size() - 1)) + ".\n\nYour chosen bag (#" + chosenBag.getId() + ") contained: " + currencyFormat.format(chosenBag.getValue());
        } else {
            // If no deal and all bags opened, player gets their chosen bag's value
            // *** CHANGE HERE: Format value as currency ***
            finalMessage = "Game Over! You did not make a deal.\n\nYour chosen bag (#" + chosenBag.getId() + ") contained: " + currencyFormat.format(chosenBag.getValue());
        }

        // Force open the player's chosen bag for display at the end of the game
        // This makes sure its value is visible when game is over
        if (chosenBag != null && !chosenBag.isOpened()) {
            chosenBag.open();
        }

        JOptionPane.showMessageDialog(this, finalMessage, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        updateGUI(); // Final update to show chosen bag value if it wasn't opened
    }
}