package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.projectile.hit.PierceHit;
import model.utils.GameSession;

public class StrikeStrategy implements ActStrategy {

    public static final double STRIKE_SPEED = 5.0;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        Zombie target = findNearestInLane(user, session);
        if (target == null && !user.isPlantFoodActive()) return;

        user.setInternalTimer(user.getActionInterval());

        int pierceCount = (int) user.getAbilityValue();
        Projectile projectile = new Projectile(user,
                user.getPosition(),
                new Position(session.projectileSpeed(STRIKE_SPEED), 0), target,
                user.getDamage(),
                new StraightMove(),
                new PierceHit(pierceCount)
        );
        projectile.setMaxTravelDistance(user.getAttackRange());
        session.getProjectiles().add(projectile);
    }


    private Zombie findNearestInLane(Plant user, GameSession session) {
        double plantRow = user.getPosition().y();
        double plantCol = user.getPosition().x();

        Zombie nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive()) continue;
            Position zp = zombie.getPosition();

            if (Math.abs(zp.y() - plantRow) < 0.5 && zp.x() > plantCol && user.isWithinAttackRange(zp)) {
                double dist = zp.x() - plantCol;
                if (dist < minDist) {
                    minDist = dist;
                    nearest = zombie;
                }
            }
        }
        return nearest;
    }
}
