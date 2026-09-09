package model.pitches.obstacles;

/// crater blocks planting
public class Crater implements Obstacle {
    @Override
    public boolean blocksPlanting() {
        return true;
    }

    @Override
    public String getName() {
        return "Crater";
    }
}
