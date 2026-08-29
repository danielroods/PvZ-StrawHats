package model.match.boss;

import model.collections.plant.Plant;
import model.utils.GameSession;

public final class ZombossRowEffect {

    private final String pamPath;
    private final String clip;
    private final int row;
    private final double duration;
    private final boolean burning;
    private final double warningSeconds;
    private final int minColumn;

    private double elapsed;
    private boolean burned;

    public ZombossRowEffect(String pamPath, String clip, int row, double duration,
                            boolean burning) {
        this(pamPath, clip, row, duration, burning, 0.0, 0);
    }

    public ZombossRowEffect(String pamPath, String clip, int row, double duration,
                            boolean burning, double warningSeconds, int minColumn) {
        this.pamPath = pamPath;
        this.clip = clip;
        this.row = row;
        this.duration = Math.max(0.1, duration);
        this.burning = burning;
        this.warningSeconds = Math.max(0.0, warningSeconds);
        this.minColumn = Math.max(0, minColumn);
    }

    public double getWarningSeconds() { return warningSeconds; }

    public boolean isWarning() { return elapsed < warningSeconds; }

    public int getMinColumn() { return minColumn; }

    public String getPamPath() { return pamPath; }

    public String getClip() { return clip; }

    public int getRow() { return row; }

    public double getElapsed() { return elapsed; }

    public double getDuration() { return duration; }

    public boolean isDone() { return elapsed >= duration; }

    public void tick(double deltaSeconds, GameSession session) {
        elapsed += Math.max(0.0, deltaSeconds);
        if (!burning || burned || session == null || elapsed < warningSeconds) return;
        burned = true;
        for (Plant plant : ZombossLawn.plantsInRow(session, row)) {
            if (Math.round(plant.getPosition().x()) < minColumn) continue;
            if (!ZombossLawn.isFireProof(plant)) ZombossLawn.destroyPlant(session, plant);
        }
    }
}
