package com.snake_ladder;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BoardPanel extends JPanel {
    private GameLogic gameLogic;
    private static final int MIN_TILE_SIZE = 50; 
    private int actualTileWidth = MIN_TILE_SIZE;
    private int actualTileHeight = MIN_TILE_SIZE;
    private boolean dimensionsRecalculated = false; 
    private boolean lastHideSettingState = false; 
    private static final int TEXT_PADDING = 5; 
    private static final int PLAYER_DOT_BASE_SIZE = 10;

    private int tilesPerRow = 10; 

    public BoardPanel(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
        setPreferredSize(new Dimension(MIN_TILE_SIZE * 10, MIN_TILE_SIZE * 10));
        resetDimensionsRecalculatedFlag(); 
    }
    
    public void resetDimensionsRecalculatedFlag() {
        this.dimensionsRecalculated = false;
    }

    public void recalculateBoardLayout(Graphics g) {
        if (g == null || gameLogic == null || gameLogic.getBoard() == null || gameLogic.getBoard().isEmpty()) {
            actualTileWidth = MIN_TILE_SIZE;
            actualTileHeight = MIN_TILE_SIZE;
            if (gameLogic != null && gameLogic.getBoard() != null && !gameLogic.getBoard().isEmpty()) {
                 tilesPerRow = (int) Math.ceil(Math.sqrt(gameLogic.getBoard().size()));
                 if (tilesPerRow == 0) tilesPerRow = 1;
                 int numRows = (int) Math.ceil((double)gameLogic.getBoard().size() / tilesPerRow);
                 setPreferredSize(new Dimension(actualTileWidth * tilesPerRow, actualTileHeight * numRows));
            } else {
                setPreferredSize(new Dimension(MIN_TILE_SIZE * 10, MIN_TILE_SIZE * 10));
            }
            revalidate();
            return;
        }

        FontMetrics fm = g.getFontMetrics();
        int maxContentWidth = 0;
        int maxContentHeight = 0;

        for (int i = 0; i < gameLogic.getBoard().size(); i++) {
            BoardTile tile = gameLogic.getBoard().get(i);
            int currentTileContentWidth = 0;
            int currentTileContentHeight = 0;

            String line1 = String.valueOf(i + 1);
            currentTileContentWidth = Math.max(currentTileContentWidth, fm.stringWidth(line1));
            currentTileContentHeight += fm.getHeight();

            String line2 = getEffectAbbreviation(tile.getEffect());
            if (!line2.isEmpty()) {
                currentTileContentWidth = Math.max(currentTileContentWidth, fm.stringWidth(line2));
                currentTileContentHeight += fm.getHeight();
            }

            if (!gameLogic.getSettings().hideTileValuesOnBoard &&
                (tile.getEffect() == TileEffect.WARP_FORWARD || tile.getEffect() == TileEffect.WARP_BACKWARD ||
                 tile.getEffect() == TileEffect.GIVE_POINTS || tile.getEffect() == TileEffect.TAKE_POINTS)) {
                
                String line3;
                if (tile.isStaticValue()) {
                    line3 = NumberFormatterUtil.formatNumberShort(tile.getValue1());
                } else {
                    line3 = NumberFormatterUtil.formatNumberShort(tile.getValue1()) + "-" + NumberFormatterUtil.formatNumberShort(tile.getValue2());
                }
                currentTileContentWidth = Math.max(currentTileContentWidth, fm.stringWidth(line3));
                currentTileContentHeight += fm.getHeight();
            }
            maxContentWidth = Math.max(maxContentWidth, currentTileContentWidth);
            maxContentHeight = Math.max(maxContentHeight, currentTileContentHeight);
        }

        actualTileWidth = Math.max(MIN_TILE_SIZE, maxContentWidth + 2 * TEXT_PADDING);
        actualTileHeight = Math.max(MIN_TILE_SIZE, maxContentHeight + 2 * TEXT_PADDING);
        
        tilesPerRow = (int) Math.ceil(Math.sqrt(gameLogic.getBoard().size()));
        if (tilesPerRow == 0 && gameLogic.getBoard().size() > 0) tilesPerRow = 1; 
        else if (tilesPerRow == 0) { 
             setPreferredSize(new Dimension(MIN_TILE_SIZE*10, MIN_TILE_SIZE*10)); 
             revalidate();
             return;
        }

        int numRows = (int) Math.ceil((double) gameLogic.getBoard().size() / tilesPerRow);
        
        Dimension newPrefSize = new Dimension(actualTileWidth * tilesPerRow, actualTileHeight * numRows);
        if (!newPrefSize.equals(getPreferredSize())) {
            setPreferredSize(newPrefSize);
            revalidate(); 
        }
        dimensionsRecalculated = true;
        lastHideSettingState = gameLogic.getSettings().hideTileValuesOnBoard;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameLogic == null) {
             g.drawString("Game logic not initialized.", 20, 20);
             return;
        }
        
        if (!dimensionsRecalculated || (gameLogic.getSettings().hideTileValuesOnBoard != lastHideSettingState)) { 
            recalculateBoardLayout(g);
        }
        
        if (gameLogic.getBoard() == null || gameLogic.getBoard().isEmpty()) {
            g.drawString("Game not started or board not generated.", 20, 20);
            return;
        }

        List<BoardTile> board = gameLogic.getBoard();
        List<Player> players = gameLogic.getPlayers();
        FontMetrics fm = g.getFontMetrics();

        for (int i = 0; i < board.size(); i++) {
            int tileNum = i + 1;
            BoardTile tile = board.get(i);

            int row = i / tilesPerRow;
            int col = i % tilesPerRow;
            if (row % 2 != 0) { 
                col = tilesPerRow - 1 - col;
            }

            int x = col * actualTileWidth; 
            int y = row * actualTileHeight; 

            g.setColor(getTileColor(tile.getEffect()));
            g.fillRect(x, y, actualTileWidth, actualTileHeight);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, actualTileWidth, actualTileHeight);

            Color originalTextColor = Color.BLACK; 
            if (tile.getEffect() == TileEffect.HARSH_GO_TO_START || tile.getEffect() == TileEffect.GO_TO_START) {
                g.setColor(Color.WHITE);
            } else {
                g.setColor(originalTextColor);
            }

            int currentTextY = y + TEXT_PADDING + fm.getAscent();
            
            g.drawString(String.valueOf(tileNum), x + TEXT_PADDING, currentTextY);
            currentTextY += fm.getHeight();

            String effectAbbr = getEffectAbbreviation(tile.getEffect());
             if (!effectAbbr.isEmpty()) {
                g.drawString(effectAbbr, x + TEXT_PADDING, currentTextY);
                currentTextY += fm.getHeight();
            }
            
            if (!gameLogic.getSettings().hideTileValuesOnBoard &&
                (tile.getEffect() == TileEffect.WARP_FORWARD || tile.getEffect() == TileEffect.WARP_BACKWARD ||
                 tile.getEffect() == TileEffect.GIVE_POINTS || tile.getEffect() == TileEffect.TAKE_POINTS)) {
                
                String valStr;
                if (tile.isStaticValue()) {
                    valStr = NumberFormatterUtil.formatNumberShort(tile.getValue1()); // Use formatter
                } else {
                    valStr = NumberFormatterUtil.formatNumberShort(tile.getValue1()) + "-" + 
                             NumberFormatterUtil.formatNumberShort(tile.getValue2()); // Use formatter
                }
                g.drawString(valStr, x + TEXT_PADDING, currentTextY);
            }
            
            if (tile.getEffect() == TileEffect.HARSH_GO_TO_START || tile.getEffect() == TileEffect.GO_TO_START) {
                 g.setColor(originalTextColor);
            }
        }

        if (players != null) {
            int dynamicPlayerDotSize = Math.max(PLAYER_DOT_BASE_SIZE / 2, Math.min(PLAYER_DOT_BASE_SIZE * 2, actualTileHeight / 5));
            if (dynamicPlayerDotSize <= 0) dynamicPlayerDotSize = PLAYER_DOT_BASE_SIZE;

            for (Player player : players) {
                if (player.isEliminated()) continue;

                int tileIndex = player.getCurrentTileIndex();
                int pRow = tileIndex / tilesPerRow;
                int pCol = tileIndex % tilesPerRow;
                if (pRow % 2 != 0) {
                    pCol = tilesPerRow - 1 - pCol;
                }

                int playersOnThisTileCount = 0;
                int playerOrderOnTile = 0;
                for(Player pCheck : players) {
                    if (!pCheck.isEliminated() && pCheck.getCurrentTileIndex() == tileIndex) {
                        if (pCheck == player) {
                            playerOrderOnTile = playersOnThisTileCount;
                        }
                        playersOnThisTileCount++;
                    }
                }
                
                int tileCenterX = pCol * actualTileWidth + actualTileWidth / 2;
                int tileCenterY = pRow * actualTileHeight + actualTileHeight / 2;

                int offsetX = 0;
                int offsetY = 0; // Basic staggering, can be improved
                if (playersOnThisTileCount > 1) {
                    int totalWidthForDots = playersOnThisTileCount * dynamicPlayerDotSize;
                    int spacing = dynamicPlayerDotSize / 3;
                    if (playersOnThisTileCount > 1) totalWidthForDots += (playersOnThisTileCount -1) * spacing;

                    offsetX = (playerOrderOnTile * (dynamicPlayerDotSize + spacing)) - (totalWidthForDots / 2) + (dynamicPlayerDotSize/2) ;
                }

                int playerX = tileCenterX - (dynamicPlayerDotSize / 2) + offsetX;
                int playerY = tileCenterY - (dynamicPlayerDotSize / 2) + offsetY;

                g.setColor(player.getColor());
                g.fillOval(playerX, playerY, dynamicPlayerDotSize, dynamicPlayerDotSize);
                g.setColor(Color.BLACK);
                g.drawOval(playerX, playerY, dynamicPlayerDotSize, dynamicPlayerDotSize);
            }
        }
    }
    
    public static String getEffectAbbreviation(TileEffect effect) {
        switch (effect) {
            case WARP_FORWARD: return "W+";
            case WARP_BACKWARD: return "W-";
            case GIVE_POINTS: return "P+";
            case TAKE_POINTS: return "P-";
            case GIVE_LIFE: return "L+";
            case TAKE_LIFE: return "L-";
            case GO_TO_START: return "S";
            case HARSH_GO_TO_START: return "HS";
            case RANDOM_EFFECT: return "RND";
            case START: return "ST";
            case FINISH: return "FIN";
            default: return "";
        }
    }

    private Color getTileColor(TileEffect effect) {
        switch (effect) {
            case START: return Color.LIGHT_GRAY;
            case FINISH: return new Color(255, 215, 0);
            case WARP_FORWARD: return new Color(144, 238, 144); 
            case WARP_BACKWARD: return new Color(255, 182, 193); 
            case GIVE_POINTS: return new Color(173, 216, 230); 
            case TAKE_POINTS: return new Color(255, 228, 181); 
            case GIVE_LIFE: return Color.CYAN;
            case TAKE_LIFE: return Color.MAGENTA;
            case GO_TO_START: return new Color(100, 100, 100); 
            case HARSH_GO_TO_START: return new Color(60, 60, 60); 
            case RANDOM_EFFECT: return Color.ORANGE;
            case NORMAL: default: return Color.WHITE;
        }
    }
}