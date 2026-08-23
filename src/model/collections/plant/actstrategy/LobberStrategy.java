package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.obstacles.Grave;
import model.projectile.MoveStrategy;
import model.projectile.Projectile;
import model.projectile.hit.*;
import model.utils.GameSession;
import service.GameClock;

import java.util.List;

public class LobberStrategy implements ActStrategy {
    private static final double HORIZONTAL_SPEED = 4.5;
    private static final double MIN_DISTANCE_X = 0.1;
    private static final double BOOSTED_BLIND_LOB_DISTANCE = 4.0;
    private static final double BASE_BUTTER_CHANCE = 0.25;
    private static final int BUTTER_ASSET_VARIANT = 1;
    // Kernel-pult's butter throw uses a separate "attack2" clip so it visually
    // reads differently from a normal kernel lob ("attack").
    private static final double KERNEL_PULT_BUTTER_ATTACK_DURATION = 0.5;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        boolean boosted = user.isPlantFoodActive();
        Position startPos = user.getPosition();

        if (boosted) {
            List<Zombie> activeZombies = session.getZombies().stream()
                    .filter(z -> z != null && z.isAlive())
                    .toList();

            if (!activeZombies.isEmpty()) {
                for (Zombie z : activeZombies) {
                    spawnLobbedProjectile(user, startPos, z.getPosition(), z, session);
                }
            } else {
                for (int r = 0; r < session.getEnvironment().getRows(); r++) {
                    Position dummyTarget = new Position(startPos.x() + BOOSTED_BLIND_LOB_DISTANCE, r);
                    spawnLobbedProjectile(user, startPos, dummyTarget, null, session);
                }
            }
            user.setInternalTimer(user.getActionInterval());
            return;
        }

        Zombie target = findNearestInLane(user, session);
        Cell grave = target == null ? findNearestGraveInLane(user, session) : null;
        if (target == null && grave == null) return;

        Position targetPos;
        if (target != null) {
            targetPos = target.getPosition();
        } else {
            targetPos = new Position(grave.getCol(), grave.getRow());
        }

        spawnLobbedProjectile(user, startPos, targetPos, target, session);
        user.setInternalTimer(user.getActionInterval());
    }

    private void spawnLobbedProjectile(Plant user, Position startPos, Position rawTargetPos, Zombie targetZombie, GameSession session) {
        double targetX = rawTargetPos.x();
        double targetY = rawTargetPos.y();
        double targetSpeedX = (targetZombie != null && targetZombie.getSpeed() != null) ? targetZombie.getSpeed().x() : 0.0;

        double distanceX = targetX - startPos.x();
        if (distanceX < MIN_DISTANCE_X) distanceX = MIN_DISTANCE_X;

        double relativeSpeedX = HORIZONTAL_SPEED - targetSpeedX;
        if (relativeSpeedX <= 0) relativeSpeedX = HORIZONTAL_SPEED;

        double timeOfFlight = distanceX / relativeSpeedX;
        final double predictedTargetX = startPos.x() + (HORIZONTAL_SPEED * timeOfFlight);
        final double finalTargetY = targetY;

        final double peakHeight = Math.max(1.2, Math.min(2.2, distanceX * 0.28));

        HitEffectStrategy hitEffect = buildHitEffect(user);

        MoveStrategy smoothParabolaMove = new MoveStrategy() {
            private double currentX = startPos.x();

            @Override
            public void move(Projectile projectile) {
                double actualSpeedX = projectile.getSpeed().x();
                if (actualSpeedX == 0) actualSpeedX = HORIZONTAL_SPEED;

                currentX += actualSpeedX * GameClock.SECONDS_PER_TICK;

                double totalDistX = predictedTargetX - startPos.x();
                if (totalDistX <= MIN_DISTANCE_X) totalDistX = MIN_DISTANCE_X;

                double progress = (currentX - startPos.x()) / totalDistX;
                if (progress > 1.0) progress = 1.0;

                double currentBaseY = startPos.y() + progress * (finalTargetY - startPos.y());

                double arcOffset = -4.0 * peakHeight * progress * (1.0 - progress);

                projectile.setPosition(new Position(currentX, currentBaseY + arcOffset));
            }
        };

        Projectile projectile = new Projectile(user,
                startPos,
                new Position(HORIZONTAL_SPEED, 0.0),
                targetZombie,
                user.getDamage(),
                smoothParabolaMove,
                hitEffect
        );

        if (!user.isPlantFoodActive()) {
            projectile.setLobberTargetOnly(true);
        }

        if (hitEffect instanceof ButterHit) {
            projectile.setAssetVariant(BUTTER_ASSET_VARIANT);
            if (user.getName().equalsIgnoreCase("Kernel-pult")) {
                user.setVisualAnimationState("attack2", KERNEL_PULT_BUTTER_ATTACK_DURATION);
            }
        }
        session.getProjectiles().add(projectile);
    }

    private HitEffectStrategy buildHitEffect(Plant user) {
        int areaLength = user.getTags().contains(PlantTag.AOE) ? 3 : 1;
        if (user.getTags().contains(PlantTag.FIRE)) return new FireHit(areaLength, 1.0);
        if (user.getTags().contains(PlantTag.ICE)) {
            return new IceHit(areaLength, 5.0 + user.getSpecialUpgrade("CHILL_DURATION_EXT", 0));
        }
        if (user.getTags().contains(PlantTag.POISON)) return new PoisonHit(areaLength);
        if (user.getName().equalsIgnoreCase("Kernel-pult")) {
            double butterChance = BASE_BUTTER_CHANCE + user.getSpecialUpgrade("BUTTER_CHANCE_BUFF", 0);
            // Plant Food ("Butter Barrage") always butters every zombie it hits;
            // outside of that it's the normal random chance per shot.
            if (user.isPlantFoodActive() || Math.random() < butterChance) {
                return new ButterHit(areaLength);
            }
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