package model.greenhouse;

import controller.ui_menus.greenhouse.PotController;

public class Pot {
    private PotPlant potPlant;
    private boolean isLocked;
    private final PotController potController;
    private final int row;
    private final int col;

    public static final int UNLOCK_COST = 2000;

    public Pot(boolean isLocked, int row, int col) {
        this.isLocked = isLocked;
        this.row = row;
        this.col = col;
        potController = new PotController(this);
    }

    public void freePot() {
        this.potPlant = null;
    }

    public void unlockPot() {
        isLocked = false;
    }

    public static int getUnlockCost() {
        return UNLOCK_COST;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public PotPlant getPotPlant() {
        return potPlant;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public PotController getPotController() {
        return potController;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }

    public void setPotPlant(PotPlant potPlant) {
        this.potPlant = potPlant;
    }
}
