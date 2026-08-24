package model.match.boss;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.utils.GameSession;


public final class ZombossShark {

    public static final String PAM =
            "768/FULL/EFFECTS/ZOMBOSS_SHARK_PROJECTILE/ZOMBOSS_SHARK_PROJECTILE.PAM";

    private static final double APPROACH_SECONDS = 1.8;
    private static final double HOLD_SECONDS = 1.0;

    public enum Stage { APPROACH, HOLD, ATTACK, LEAVE, DONE }

    private final int row;
    private final double targetColumn;
    private final double startColumn;

    private Stage stage = Stage.APPROACH;
    private double stageElapsed;
    private double column;
    private boolean bitten;

    public ZombossShark(int row, double targetColumn, double startColumn) {
        this.row = row;
        this.targetColumn = targetColumn;
        this.startColumn = startColumn;
        this.column = startColumn;
    }

    public int getRow() { return row; }

    public double getColumn() { return column; }

    public Stage getStage() { return stage; }

    public boolean isDone() { return stage == Stage.DONE; }

    public String getClip() {
        return switch (stage) {
            case APPROACH, HOLD -> "idle";
            case ATTACK -> "attack";
            case LEAVE -> "submerge";
            case DONE -> null;
        };
    }

    public double getClipTime() {
        String clip = getClip();
        if (clip == null) return 0.0;
        double length = AnimationFactory.exactClipDurationForPath(PAM, clip);
        if (length <= 0) return stageElapsed;
        if (stage == Stage.APPROACH || stage == Stage.HOLD) return stageElapsed % length;
        return Math.min(stageElapsed, length);
    }

    public void tick(double deltaSeconds, GameSession session) {
        if (stage == Stage.DONE) return;
        stageElapsed += Math.max(0.0, deltaSeconds);
        switch (stage) {
            case APPROACH -> {
                double progress = Math.min(1.0, stageElapsed / APPROACH_SECONDS);
                double eased = progress * progress * (3.0 - 2.0 * progress);
                column = startColumn + (targetColumn - startColumn) * eased;
                if (progress >= 1.0) advance(Stage.HOLD);
            }
            case HOLD -> {
                if (stageElapsed >= HOLD_SECONDS) {
                    Plant victim = session == null ? null
                            : session.getPlantAt(row, (int) Math.round(targetColumn));
                    advance(victim != null && victim.isAlive() ? Stage.ATTACK : Stage.LEAVE);
                }
            }
            case ATTACK -> {
                if (!bitten && stageElapsed >= clipLength("attack") * 0.5) {
                    bitten = true;
                    Plant victim = session == null ? null
                            : session.getPlantAt(row, (int) Math.round(targetColumn));
                    ZombossLawn.destroyPlant(session, victim);
                }
                if (stageElapsed >= clipLength("attack")) advance(Stage.LEAVE);
            }
            case LEAVE -> {
                if (stageElapsed >= clipLength("submerge")) advance(Stage.DONE);
            }
            default -> { }
        }
    }

    private double clipLength(String clip) {
        double length = AnimationFactory.exactClipDurationForPath(PAM, clip);
        return length > 0 ? length : 1.0;
    }

    private void advance(Stage next) {
        stage = next;
        stageElapsed = 0.0;
        bitten = false;
    }
}
