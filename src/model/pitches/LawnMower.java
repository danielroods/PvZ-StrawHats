package model.pitches;

import model.collections.zombie.Zombie;
import model.utils.GameSession;
import view.GeneralPrinter;

import java.util.List;

public class LawnMower {
    public enum MowerState { IDLE, TRANSITION, ATTACK, DEAD }

    private final int rowNumber;
    private Cell[] row;
    private boolean isUsed;

    private MowerState state = MowerState.IDLE;
    private double xPosition = -0.5;
    private double stateTimer = 0.0;

    public LawnMower(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public void tick(double deltaTime, GameSession session) {
        if (state == MowerState.IDLE || state == MowerState.DEAD) return;

        stateTimer += deltaTime;
        if (state == MowerState.TRANSITION) {
            if (stateTimer >= 0.75) {
                state = MowerState.ATTACK;
                stateTimer = 0.0;
            }
        } else if (state == MowerState.ATTACK) {
            xPosition += 6.0 * deltaTime;

            if (session != null) {
                for (Zombie zombie : session.getZombies()) {
                    if (zombie.isBoss()) continue;
                    if (zombie.isAlive() && zombie.getPosition() != null && Math.round(zombie.getPosition().y()) == rowNumber) {
                        if (zombie.getPosition().x() <= xPosition + 0.6 && zombie.getPosition().x() >= xPosition - 1.0) {
                            zombie.setHp(0);
                            GeneralPrinter.print(" - " + zombie.getName() + " was shredded by the lawn mower in row " + (rowNumber + 1) + "!");
                        }
                    }
                }
            }

            if (session != null && xPosition > session.getCols() + 1) {
                state = MowerState.DEAD;
            }
        }
    }

    public boolean killZombiesInRow(List<Zombie> zombiesInRow) {
        if (state == MowerState.DEAD) {
            GeneralPrinter.print("The zombie ate your brain; LOSER!!!");
            return false;
        }
        if (state == MowerState.IDLE) {
            GeneralPrinter.print("The lawn mower in row " + (rowNumber + 1) + " is triggered!");
            state = MowerState.TRANSITION;
            stateTimer = 0.0;
            isUsed = true;
        }
        return true;
    }

    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { this.isUsed = used; }
    public int getRowNumber() { return rowNumber; }
    public Cell[] getRow() { return row; }
    public void setRow(Cell[] row) { this.row = row; }

    public MowerState getState() { return state; }
    public double getXPosition() { return xPosition; }
    public double getStateTimer() { return stateTimer; }
}