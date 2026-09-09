package model.pitches.obstacles;


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
