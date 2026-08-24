package model.match.boss;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public final class ZombossSkyStrike {

    public record Config(String reticlePath, String reticleClip,
                         String fallPath, String fallClip,
                         String impactPath, String impactClip,
                         double fallSeconds, boolean sparesFire) { }

    public enum Stage { LOCK, FALL, IMPACT, DONE }

    private final Config config;
    private final int row;
    private final int col;

    private Stage stage;
    private double stageElapsed;
    private final double lockSeconds;
    private final double impactSeconds;

    public ZombossSkyStrike(Config config, int row, int col) {
        this.config = config;
        this.row = row;
        this.col = col;
        this.lockSeconds = config.reticlePath() == null ? 0.0
                : durationOf(config.reticlePath(), config.reticleClip(), 0.85);
        this.impactSeconds = durationOf(config.impactPath(), config.impactClip(), 1.0);
        this.stage = config.reticlePath() == null ? Stage.FALL : Stage.LOCK;
    }

    private static double durationOf(String path, String clip, double fallback) {
        double length = AnimationFactory.exactClipDurationForPath(path, clip);
        return length > 0 ? length : fallback;
    }

    public int getRow() { return row; }

    public int getCol() { return col; }

    public Stage getStage() { return stage; }

    public double getStageElapsed() { return stageElapsed; }

    public boolean isDone() { return stage == Stage.DONE; }

    public String getCurrentPath() {
        return switch (stage) {
            case LOCK -> config.reticlePath();
            case FALL -> config.fallPath();
            case IMPACT -> config.impactPath();
            case DONE -> null;
        };
    }

    public String getCurrentClip() {
        return switch (stage) {
            case LOCK -> config.reticleClip();
            case FALL -> config.fallClip();
            case IMPACT -> config.impactClip();
            case DONE -> null;
        };
    }

    public double getCurrentClipTime() {
        String path = getCurrentPath();
        String clip = getCurrentClip();
        if (path == null || clip == null) return 0.0;
        double length = AnimationFactory.exactClipDurationForPath(path, clip);
        if (length <= 0) return stageElapsed;
        return stage == Stage.FALL ? stageElapsed % length : Math.min(stageElapsed, length);
    }

    public double getFallProgress() {
        if (stage != Stage.FALL) return stage == Stage.LOCK ? 0.0 : 1.0;
        return Math.min(1.0, stageElapsed / Math.max(0.01, config.fallSeconds()));
    }

    public void tick(double deltaSeconds, GameSession session) {
        if (stage == Stage.DONE) return;
        stageElapsed += Math.max(0.0, deltaSeconds);
        switch (stage) {
            case LOCK -> {
                if (stageElapsed >= lockSeconds) advance(Stage.FALL);
            }
            case FALL -> {
                if (stageElapsed >= config.fallSeconds()) {
                    advance(Stage.IMPACT);
                    detonate(session);
                }
            }
            case IMPACT -> {
                if (stageElapsed >= impactSeconds) advance(Stage.DONE);
            }
            default -> { }
        }
    }

    private void advance(Stage next) {
        stage = next;
        stageElapsed = 0.0;
    }

    private void detonate(GameSession session) {
        if (session == null) return;
        Plant plant = session.getPlantAt(row, col);
        if (plant == null || !plant.isAlive()) return;
        if (config.sparesFire() && plant.getTags() != null
                && plant.getTags().contains(PlantTag.FIRE)) {
            return;
        }
        ZombossLawn.destroyPlant(session, plant);
    }

    public Position getPosition() {
        return new Position(col, row);
    }
}
