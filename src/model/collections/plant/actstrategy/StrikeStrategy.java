package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.projectile.hit.PierceHit;
import model.projectile.targeting.TargetFinder;
import model.projectile.targeting.TargetScan;
import model.utils.GameSession;

public class StrikeStrategy implements ActStrategy {

    public static final double STRIKE_SPEED = 5.0;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        TargetScan scan = TargetFinder.inLaneAhead(user, session,
                TargetFinder.LANE_ROW_TOLERANCE, false);
        if (!scan.hasTarget() && !user.isPlantFoodActive()) return;

        user.setInternalTimer(user.getActionInterval());

        int pierceCount = (int) user.getAbilityValue();
        Projectile projectile = new Projectile(user,
                user.getPosition(),
                new Position(session.projectileSpeed(STRIKE_SPEED), 0), scan.zombie(),
                user.getDamage(),
                new StraightMove(),
                new PierceHit(pierceCount)
        );
        projectile.setMaxTravelDistance(user.getAttackRange());
        session.getProjectiles().add(projectile);
    }
}
