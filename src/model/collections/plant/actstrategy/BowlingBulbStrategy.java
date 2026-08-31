package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.RollingBounceMove;
import model.projectile.Projectile;
import model.projectile.hit.PierceHit;
import model.utils.GameSession;

public class BowlingBulbStrategy implements ActStrategy {
    private static final double[] AMMO_DAMAGE_MULTIPLIER = {1.0, 3.0, 4.5};
    private static final double[] AMMO_RELOAD_SECONDS = {2.0, 5.0, 10.0};
    private static final double ROLL_SPEED = 4.8;
    private static final int RICOCHET_HITS = 3;

    private int ammoIndex = 0;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        Zombie target = findNearestInLane(user, session);
        if (target == null && !user.isPlantFoodActive()) return;

        int index = Math.floorMod(ammoIndex, AMMO_DAMAGE_MULTIPLIER.length);
        int damage = (int) Math.round(user.getDamage() * AMMO_DAMAGE_MULTIPLIER[index]);

        double rollSpeed = session.projectileSpeed(ROLL_SPEED);
        Projectile bulb = new Projectile(user,
                user.getPosition(),
                new Position(rollSpeed, 0),
                target,
                damage,
                new RollingBounceMove(rollSpeed),
                new PierceHit(RICOCHET_HITS)
        );
        bulb.setAssetVariant(index);
        session.getProjectiles().add(bulb);

        user.setInternalTimer(reloadFor(user, index));
        ammoIndex = index + 1;
    }

    private double reloadFor(Plant user, int index) {
        double baseReload = AMMO_RELOAD_SECONDS[index];
        double intervalShift = user.getActionInterval() - AMMO_RELOAD_SECONDS[0];
        return Math.max(0.5, baseReload + intervalShift);
    }

    private Zombie findNearestInLane(Plant user, GameSession session) {
        Position userPos = user.getPosition();
        Zombie nearest = null;
        double shortest = Double.MAX_VALUE;
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position zombiePos = zombie.getPosition();
            if (Math.abs(zombiePos.y() - userPos.y()) >= 1.5 || zombiePos.x() <= userPos.x()) continue;
            double distance = zombiePos.x() - userPos.x();
            if (distance < shortest) {
                shortest = distance;
                nearest = zombie;
            }
        }
        return nearest;
    }
}
