package com.snake_ladder;

public class BoardTile {
    private TileEffect effect;
    private int value1; // Min value or static value
    private int value2; // Max value (if randomized)
    private boolean isStaticValue; // True if value1 is static, false if randomized between value1 and value2

    public BoardTile(TileEffect effect) {
        this.effect = effect;
        this.isStaticValue = true; // Default, can be changed
    }

    // Constructor for effects with values
    public BoardTile(TileEffect effect, int value1, int value2, boolean isStaticValue) {
        this.effect = effect;
        this.value1 = value1;
        this.value2 = value2;
        this.isStaticValue = isStaticValue;
    }
    
    public BoardTile(TileEffect effect, int staticValue) {
        this.effect = effect;
        this.value1 = staticValue;
        this.isStaticValue = true;
    }


    public TileEffect getEffect() {
        return effect;
    }

    public void setEffect(TileEffect effect) {
        this.effect = effect;
    }

    public int getValue1() {
        return value1;
    }

    public void setValue1(int value1) {
        this.value1 = value1;
    }

    public int getValue2() {
        return value2;
    }

    public void setValue2(int value2) {
        this.value2 = value2;
    }

    public boolean isStaticValue() {
        return isStaticValue;
    }

    public void setStaticValue(boolean staticValue) {
        this.isStaticValue = staticValue;
    }

    public int getActualValue() {
        if (isStaticValue || value1 == value2) {
            return value1;
        }
        // Randomized
        return GameLogic.random.nextInt(value2 - value1 + 1) + value1;
    }

    @Override
    public String toString() {
        String valStr = "";
        if (effect == TileEffect.WARP_FORWARD || effect == TileEffect.WARP_BACKWARD ||
            effect == TileEffect.GIVE_POINTS || effect == TileEffect.TAKE_POINTS) {
            if (isStaticValue) {
                valStr = " (" + value1 + ")";
            } else {
                valStr = " (" + value1 + "-" + value2 + ")";
            }
        }
        return effect.getDisplayName() + valStr;
    }
}