package com.millebornes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private List<Card> allCards;
    private List<Card> drawPile;
    private List<Card> discardPile;

    public Deck() {
        this.allCards = GameConstants.createDeckCards();
        this.drawPile = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        resetAndShuffle();
    }

    public void resetAndShuffle() {
        drawPile.clear();
        discardPile.clear();
        drawPile.addAll(allCards); // Add all cards back to draw pile
        Collections.shuffle(drawPile);
    }

    public Card drawCard() {
        if (drawPile.isEmpty()) {
            return null;
        }
        return drawPile.remove(0); // Remove from top
    }

    public void discardCard(Card card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    public boolean reshuffleDiscardIntoDraw() {
        if (!discardPile.isEmpty()) {
            drawPile.addAll(discardPile);
            discardPile.clear();
            Collections.shuffle(drawPile);
            return true;
        }
        return false;
    }

    public int getDrawPileSize() {
        return drawPile.size();
    }
    
    public int getDiscardPileSize() {
        return discardPile.size();
    }
}