package model.collections.zombie.zombie_effect;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.pitches.Cell;
import model.pitches.Environment;
import model.utils.GameSession;
import service.GameClock;


public class ParrotCompanionEffect implements ZombieEffectStatus {

    private static final double PARROT_ACTION_SPEED_FACTOR = 1.8;

    private enum Stage { IDLE, RELEASE, FLY_OUT, CARRY, FLY_BACK, DIE }

    private final double raidCooldown;
    private double cooldownTimer;
    private Stage stage = Stage.IDLE;
    private double stageTimer;

    public ParrotCompanionEffect(double raidCooldown) {
        this.raidCooldown = raidCooldown <= 0 ? 12.0 : raidCooldown;
        this.cooldownTimer = this.raidCooldown;
    }

    @Override
    public void applyTickEffect(Zombie captain, GameSession session) {
        if (!captain.isAlive() || captain.getPosition() == null || session == null) return;

        double dt = GameClock.SECONDS_PER_TICK;

        if (stage == Stage.IDLE) {
            cooldownTimer += dt;
            if (cooldownTimer >= raidCooldown && launchParrot(captain, session)) {
                cooldownTimer = 0;
            }
            return;
        }

        stageTimer -= dt;
        if (stageTimer > 0) return;

        switch (stage) {
            case RELEASE -> beginStage(captain, Stage.FLY_OUT, CaptainParrot.CLIP_FLY, 0.6, true);
            case FLY_OUT -> {
                stealNearestPlant(captain, session);
                beginStage(captain, Stage.CARRY, CaptainParrot.CLIP_CARRY, 0.6, false);
            }
            case CARRY -> beginStage(captain, Stage.FLY_BACK, CaptainParrot.CLIP_FLYBACK, 0.6, true);
            case FLY_BACK -> beginStage(captain, Stage.DIE, CaptainParrot.CLIP_LAND, 0.4, false);
            case DIE -> stage = Stage.IDLE;
            default -> stage = Stage.IDLE;
        }
    }

    private boolean launchParrot(Zombie captain, GameSession session) {
        if (session.getEnvironment() == null) return false;
        beginStage(captain, Stage.RELEASE, CaptainParrot.CLIP_RELEASE, 0.5, false);
        return true;
    }

    private void beginStage(Zombie captain, Stage next, String clip, double fallbackDuration, boolean loop) {
        stage = next;
        double duration = model.collections.animations.AnimationFactory
                .exactClipDurationForPath(CaptainParrot.PAM, clip);
        
        
        
        double baseDuration = duration > 0 ? duration : fallbackDuration;
        stageTimer = baseDuration * PARROT_ACTION_SPEED_FACTOR;
        captain.setActionAnimationState(clip, stageTimer, loop);
    }

    
    private void stealNearestPlant(Zombie captain, GameSession session) {
        Environment env = session.getEnvironment();
        if (env == null) return;

        int row = (int) Math.round(captain.getPosition().y());
        double captainX = captain.getPosition().x();

        for (int col = (int) captainX; col >= 0; col--) {
            Cell cell = env.getCell(row, col);
            if (cell == null) continue;
            Plant plant = cell.getPlant();
            if (plant != null && plant.isAlive()) {
                plant.setAlive(false);
                cell.setPlant(null);
                return;
            }
        }
    }
}