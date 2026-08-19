package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.obstacles.Grave;
import model.projectile.ArcMove;
import model.projectile.Projectile;
import model.projectile.hit.*;
import model.utils.GameSession;

public class LobberStrategy implements ActStrategy {
    private static final double GRAVITY = 15.0;
    private static final double HORIZONTAL_SPEED = 4.0;
    private static final double MIN_DISTANCE_X = 0.1;
    private static final double BOOSTED_BLIND_LOB_DISTANCE = 4.0;
    private static final double BASE_BUTTER_CHANCE = 0.25;
    private static final int BUTTER_ASSET_VARIANT = 1;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        Zombie target = findNearestInLane(user, session);
        Cell grave = target == null ? findNearestGraveInLane(user, session) : null;
        boolean boosted = user.isPlantFoodActive();
        if (target == null && grave == null && !boosted) return;

        Position startPos = user.getPosition();
        Position targetPos;
        if (target != null) {
            targetPos = target.getPosition();
        } else if (grave != null) {
            targetPos = new Position(grave.getCol(), grave.getRow());
        } else {
            targetPos = new Position(startPos.x() + BOOSTED_BLIND_LOB_DISTANCE, startPos.y());
        }
        if (targetPos == null) return;

        double distanceX = targetPos.x() - startPos.x();
        if (distanceX < MIN_DISTANCE_X) return;

        double distanceY = targetPos.y() - startPos.y();
        double timeOfFlight = distanceX / HORIZONTAL_SPEED;
        double initialVelocityY = (distanceY - 0.5 * GRAVITY * timeOfFlight * timeOfFlight) / timeOfFlight;

        Position initialVelocity = new Position(HORIZONTAL_SPEED, initialVelocityY);
        HitEffectStrategy hitEffect = buildHitEffect(user);

        Projectile projectile = new Projectile(user,
                startPos,
                initialVelocity,
                target,
                user.getDamage(),
                new ArcMove(GRAVITY),
                hitEffect
        );
        if (hitEffect instanceof ButterHit) projectile.setAssetVariant(BUTTER_ASSET_VARIANT);
        session.getProjectiles().add(projectile);

        user.setInternalTimer(user.getActionInterval());
    }

    private HitEffectStrategy buildHitEffect(Plant user) {
        int areaLength = user.getTags().contains(PlantTag.AOE) ? 3 : 1;
        if (user.getTags().contains(PlantTag.FIRE)) return new FireHit(areaLength, 1.0);
        if (user.getTags().contains(PlantTag.ICE)) {
            return new IceHit(areaLength, 5.0 + user.getSpecialUpgrade("CHILL_DURATION_EXT", 0));
        }
        if (user.getTags().contains(PlantTag.POISON)) return new PoisonHit(areaLength);
        double butterChance = BASE_BUTTER_CHANCE + user.getSpecialUpgrade("BUTTER_CHANCE_BUFF", 0);
        if (user.getName().equalsIgnoreCase("Kernel-pult") && Math.random() < butterChance) {
            return new ButterHit(areaLength);
        }
        if (user.getTags().contains(PlantTag.PIERCE)) return new PierceHit(-1);
        return new NormalHit(areaLength);
    }

    private Cell findNearestGraveInLane(Plant user, GameSession session) {
        double plantRow = user.getPosition().y();
        double plantCol = user.getPosition().x();
        Cell nearest = null;
        double minX = Double.MAX_VALUE;

        for (int row = 0; row < session.getEnvironment().getRows(); row++) {
            for (int col = 0; col < session.getEnvironment().getCols(); col++) {
                Cell cell = session.getEnvironment().getCell(row, col);
                if (cell == null || !(cell.getObstacle() instanceof Grave)) continue;
                if (Math.abs(row - plantRow) < 0.5 && col > plantCol && col < minX) {
                    minX = col;
                    nearest = cell;
                }
            }
        }
        return nearest;
    }

    private Zombie findNearestInLane(Plant user, GameSession session) {
        double plantRow = user.getPosition().y();
        double plantCol = user.getPosition().x();
        Zombie nearest = null;
        double minX = Double.MAX_VALUE;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive()) continue;
            Position zp = zombie.getPosition();
            if (zp == null) continue;

            if (Math.abs(zp.y() - plantRow) < 0.5 && zp.x() > plantCol) {
                if (zp.x() < minX) {
                    minX = zp.x();
                    nearest = zombie;
                }
            }
        }
        return nearest;
    }
}
