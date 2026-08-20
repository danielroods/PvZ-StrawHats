package model.match.mini_games.izombie;

import model.collections.Item;
import model.match_mechanisms.vector.Position;
import model.pitches.obstacles.Obstacle;

public class Brain extends Item implements Obstacle {

    public static final int BRAIN_HP = 300;

    private final int row;
    private boolean eaten;

    public Brain(Position position) {
        super(position, BRAIN_HP);
        setPosition(position);
        this.row = (int) Math.round(position.y());
    }

    public int getRow() { return row; }

    public boolean isEaten() { return eaten; }

    public void markEaten() {
        eaten = true;
        setHP(0);
    }

    public double getRemainingRatio() {
        return Math.max(0.0, Math.min(1.0, getHP() / (double) BRAIN_HP));
    }

    @Override
    public void tick() {
        if (!eaten && getHP() <= 0) eaten = true;
    }

    @Override
    public boolean blocksPlanting() { return true; }

    @Override
    public String getName() { return "Brain"; }
}
