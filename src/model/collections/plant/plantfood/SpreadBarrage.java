package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.match_mechanisms.vector.Position;
import model.projectile.LaneShiftMove;
import model.projectile.MoveStrategy;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.projectile.hit.NormalHit;
import model.utils.GameSession;

import java.util.List;

public class SpreadBarrage implements PlantFoodEffect {
    private static final double SHOT_SPEED = 4.2;
    private static final double TAIL_SECONDS = 0.6;
    private static final double LANE_EPSILON = 1.0e-6;

    private final int volleys;
    private final double fireInterval;
    private final int damagePerShot;
    private final List<Position> directions;

    private double elapsed = 0.0;
    private int fired = 0;

    public SpreadBarrage(int volleys, double fireInterval, int damagePerShot,
                         List<Position> directions) {
        this.volleys = Math.max(1, volleys);
        this.fireInterval = fireInterval > 0 ? fireInterval : 0.2;
        this.damagePerShot = Math.max(1, damagePerShot);
        this.directions = directions;
    }

    public static List<Position> eightWay() {
        return List.of(
                new Position(1, 0), new Position(-1, 0),
                new Position(0, 1), new Position(0, -1),
                new Position(1, 1), new Position(1, -1),
                new Position(-1, 1), new Position(-1, -1));
    }

    @Override
    public double getDurationSeconds() {
        return volleys * fireInterval + TAIL_SECONDS;
    }

    @Override
    public boolean drivesActStrategy() {
        return true;
    }

    @Override
    public void reset() {
        elapsed = 0.0;
        fired = 0;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        fireVolley(plant, session);
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        GameSession session = GameSession.peekInstance();
        while (elapsed >= fireInterval && fired < volleys) {
            elapsed -= fireInterval;
            fireVolley(plant, session);
        }
    }

    private void fireVolley(Plant plant, GameSession session) {
        if (session == null || fired >= volleys || plant.getPosition() == null) return;
        fired++;

        double speed = session.projectileSpeed(SHOT_SPEED);
        for (Position direction : directions) {
            Position velocity = direction.normalize().scale(speed);
            Projectile shot = new Projectile(plant,
                    plant.getPosition(),
                    velocity,
                    null,
                    damagePerShot,
                    buildMoveStrategy(plant, direction, speed),
                    new NormalHit(1)
            );
            session.getProjectiles().add(shot);
        }

        if (fired >= volleys) plant.setInternalTimer(plant.getActionInterval());
    }

    private static MoveStrategy buildMoveStrategy(Plant plant, Position direction, double speed) {
        double dx = direction.x();
        double dy = direction.y();
        if (dx == 0 || Math.abs(Math.abs(dy) - 1.0) > LANE_EPSILON) return new StraightMove();
        double laneY = Math.round(plant.getPosition().y()) + dy;
        return new LaneShiftMove(laneY, Math.signum(dx) * speed);
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
