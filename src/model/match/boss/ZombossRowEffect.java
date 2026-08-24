package model.match.boss;

import model.collections.plant.Plant;
import model.utils.GameSession;

public final class ZombossRowEffect {

    private final String pamPath;
    private final String clip;
    private final int row;
    private final double duration;
    private final boolean burning;

    private double elapsed;

    public ZombossRowEffect(String pamPath, String clip, int row, double duration,
                            boolean burning) {
        this.pamPath = pamPath;
        this.clip = clip;
        this.row = row;
        this.duration = Math.max(0.1, duration);
        this.burning = burning;
    }

    public String getPamPath() { return pamPath; }

    public String getClip() { return clip; }

    public int getRow() { return row; }

    public double getElapsed() { return elapsed; }

    public double getDuration() { return duration; }

    public boolean isDone() { return elapsed >= duration; }

    public void tick(double deltaSeconds, GameSession session) {
        elapsed += Math.max(0.0, deltaSeconds);
        if (!burning || session == null) return;
        for (Plant plant : ZombossLawn.plantsInRow(session, row)) {
            if (!ZombossLawn.isFireProof(plant)) ZombossLawn.destroyPlant(session, plant);
        }
    }
}
