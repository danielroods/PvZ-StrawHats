package model.collections.zombie.zombie_effect;

import model.collections.Faction;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieState;
import model.match_mechanisms.vector.Position;
import model.projectile.zombie_projectile.ZombiePeaProjectile;
import model.utils.GameSession;
import service.GameClock;

public class PeashooterZombieEffect implements ZombieEffectStatus {

    private static final double MUZZLE_OFFSET_X = -0.35;

    private final double shotInterval;
    private final int projectileDamage;
    private final int shotsPerVolley;
    private final double volleyGap;

    private double firingClock;
    private int shotsLeftInVolley;
    private double volleyClock;
    private double lastMuzzleFlash = -1.0;

    public PeashooterZombieEffect(int projectileDamage, double shotInterval) {
        this(projectileDamage, shotInterval, 1, 0.2);
    }

    public PeashooterZombieEffect(int projectileDamage, double shotInterval,
                                  int shotsPerVolley, double volleyGap) {
        this.projectileDamage = projectileDamage;
        this.shotInterval = Math.max(0.1, shotInterval);
        this.shotsPerVolley = Math.max(1, shotsPerVolley);
        this.volleyGap = Math.max(0.05, volleyGap);
    }

    @Override
    public void applyTickEffect(Zombie target, GameSession session) {
        if (!target.isAlive() || target.getPosition() == null || session == null) return;
        if (target.getFaction() != Faction.ZOMBIES) return;

        double delta = GameClock.SECONDS_PER_TICK;
        if (lastMuzzleFlash >= 0) lastMuzzleFlash += delta;

        if (shotsLeftInVolley > 0) {
            volleyClock += delta;
            if (volleyClock >= volleyGap) {
                volleyClock = 0;
                shotsLeftInVolley--;
                fire(target, session);
            }
            return;
        }

        firingClock += delta;
        if (firingClock < shotInterval) return;
        if (!hasTargetAhead(target, session)) return;

        firingClock = 0;
        shotsLeftInVolley = shotsPerVolley - 1;
        volleyClock = 0;
        fire(target, session);
    }

    private void fire(Zombie shooter, GameSession session) {
        if (!shooter.isAlive() || shooter.getPosition() == null) return;
        if (shooter.getZombieState() == ZombieState.DEAD) return;
        Position muzzle = new Position(shooter.getPosition().x() + MUZZLE_OFFSET_X,
                shooter.getPosition().y());
        session.addZombieProjectile(new ZombiePeaProjectile(muzzle, projectileDamage, session));
        lastMuzzleFlash = 0.0;
    }

    private boolean hasTargetAhead(Zombie shooter, GameSession session) {
        int row = (int) Math.round(shooter.getPosition().y());
        double shooterX = shooter.getPosition().x();
        for (Plant plant : session.getPlants()) {
            if (plant == null || !plant.isAlive() || plant.getPosition() == null) continue;
            if ((int) plant.getPosition().y() != row) continue;
            if (plant.getPosition().x() < shooterX) return true;
        }
        return false;
    }

    public double secondsSinceLastShot() {
        return lastMuzzleFlash;
    }
}
