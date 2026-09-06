package model.collections.zombie.zombie_effect;

import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieSequence;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.utils.GameSession;
import model.projectile.zombie_projectile.FutureGargantuarBeamProjectile;
import service.GameClock;

public class FutureGargantuarLaser implements ZombieEffectStatus {

    public static final String CLIP_LASER_START = "laser_start";
    public static final String CLIP_LASER_IDLE = "laser_idle";
    public static final String CLIP_LASER_END = "laser_end";

    private static final double FALLBACK_START_SECONDS = 0.8;
    private static final double FALLBACK_IDLE_SECONDS = 1.0;
    private static final double FALLBACK_END_SECONDS = 0.6;

    private final int laserDamage;
    private final double cooldownSeconds;
    private final int rangeColumns;

    private double cooldownTimer;

    public FutureGargantuarLaser(int laserDamage, double cooldownSeconds, int rangeColumns) {
        this.laserDamage = laserDamage <= 0 ? 1800 : laserDamage;
        this.cooldownSeconds = cooldownSeconds <= 0 ? 9.0 : cooldownSeconds;
        this.rangeColumns = rangeColumns <= 0 ? 9 : rangeColumns;
    }

    @Override
    public void applyTickEffect(Zombie gargantuar, GameSession session) {
        if (gargantuar == null || !gargantuar.isAlive() || session == null) return;
        if (gargantuar.getPosition() == null || gargantuar.isSequenceActive()) return;

        cooldownTimer += GameClock.SECONDS_PER_TICK;
        if (cooldownTimer < cooldownSeconds) return;

        Plant target = findTargetAhead(gargantuar, session);
        if (target == null) return;

        cooldownTimer = 0;
        String pam = ZombieAnimationRegistry.pathForCurrentSeason(gargantuar.getAlias());
        double beamSeconds = clipSeconds(pam, CLIP_LASER_IDLE, FALLBACK_IDLE_SECONDS);

        gargantuar.playSequence(new ZombieSequence()
                .add(CLIP_LASER_START, clipSeconds(pam, CLIP_LASER_START, FALLBACK_START_SECONDS))
                .add(CLIP_LASER_IDLE, beamSeconds, true,
                        (self, ctx) -> fireBeam(self, target, ctx, beamSeconds))
                .add(CLIP_LASER_END, clipSeconds(pam, CLIP_LASER_END, FALLBACK_END_SECONDS)));
    }

    private void fireBeam(Zombie gargantuar, Plant lockedTarget, GameSession session,
                          double beamSeconds) {
        if (session == null || gargantuar.getPosition() == null) return;

        Plant target = lockedTarget != null && lockedTarget.isAlive()
                ? lockedTarget : findTargetAhead(gargantuar, session);
        if (target == null || target.getPosition() == null) return;

        Position beamTarget = target.getPosition();
        target.takeDamage(laserDamage, gargantuar);
        session.addZombieProjectile(new FutureGargantuarBeamProjectile(
                gargantuar.getPosition(), beamTarget, beamSeconds, session));
        view.GeneralPrinter.print(gargantuar.getName() + " fired its laser at "
                + target.getName() + ".");
    }

    private Plant findTargetAhead(Zombie gargantuar, GameSession session) {
        if (session.getEnvironment() == null || gargantuar.getPosition() == null) return null;

        int row = (int) Math.round(gargantuar.getPosition().y());
        int col = (int) Math.floor(gargantuar.getPosition().x());
        for (int step = 1; step <= rangeColumns; step++) {
            int scanCol = col - step;
            if (scanCol < 0) break;
            Cell cell = session.getEnvironment().getCell(row, scanCol);
            if (cell != null && cell.getPlant() != null && cell.getPlant().isAlive()) {
                return cell.getPlant();
            }
        }
        return null;
    }

    private static double clipSeconds(String pamPath, String clip, double fallback) {
        float duration = AnimationFactory.exactClipDurationForPath(pamPath, clip);
        return duration > 0f ? duration : fallback;
    }
}
