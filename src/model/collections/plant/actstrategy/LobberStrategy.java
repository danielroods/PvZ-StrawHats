package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.LobArcMove;
import model.projectile.Projectile;
import model.projectile.hit.*;
import model.projectile.targeting.TargetFinder;
import model.projectile.targeting.TargetScan;
import model.utils.GameSession;

import java.util.List;

public class LobberStrategy implements ActStrategy {
    private static final double HORIZONTAL_SPEED = 4.5;
    private static final double MIN_DISTANCE_X = 0.6;
    private static final double BOOSTED_BLIND_LOB_DISTANCE = 4.0;
    private static final double BASE_BUTTER_CHANCE = 0.25;
    private static final int BUTTER_ASSET_VARIANT = 1;
    private static final double KERNEL_PULT_BUTTER_ATTACK_DURATION = 0.5;
    private static final double BARRAGE_STAGGER_SECONDS = 0.12;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        boolean boosted = user.isPlantFoodActive();
        Position startPos = user.getPosition();

        if (boosted) {
            List<Zombie> activeZombies = session.getZombies().stream()
                    .filter(z -> TargetFinder.isTargetable(user, z, true)
                            && z.getPosition().x() >= startPos.x())
                    .toList();

            if (!activeZombies.isEmpty()) {
                int index = 0;
                for (Zombie z : activeZombies) {
                    spawnLobbedProjectile(user, startPos, z.getPosition(), z, session,
                            BARRAGE_STAGGER_SECONDS * index++);
                }
            } else {
                for (int r = 0; r < session.getEnvironment().getRows(); r++) {
                    Position dummyTarget = new Position(startPos.x() + BOOSTED_BLIND_LOB_DISTANCE, r);
                    spawnLobbedProjectile(user, startPos, dummyTarget, null, session,
                            BARRAGE_STAGGER_SECONDS * r);
                }
            }
            user.setInternalTimer(user.getActionInterval());
            return;
        }

        TargetScan scan = TargetFinder.inLaneAhead(user, session,
                TargetFinder.LANE_ROW_TOLERANCE, true);
        Position targetPos = scan.aimPosition();
        if (targetPos == null) return;

        spawnLobbedProjectile(user, startPos, targetPos, scan.zombie(), session, 0.0);
        user.setInternalTimer(user.getActionInterval());
    }

    private void spawnLobbedProjectile(Plant user, Position startPos, Position rawTargetPos,
                                       Zombie targetZombie, GameSession session,
                                       double extraDelay) {
        double horizontalSpeed = session.projectileSpeed(HORIZONTAL_SPEED);
        double targetSpeedX = (targetZombie != null && targetZombie.getSpeed() != null)
                ? targetZombie.getSpeed().x() : 0.0;

        double distanceX = Math.max(MIN_DISTANCE_X, rawTargetPos.x() - startPos.x());

        double closingSpeed = horizontalSpeed - targetSpeedX;
        if (closingSpeed <= 0.1) closingSpeed = horizontalSpeed;
        double timeOfFlight = distanceX / closingSpeed;
        double landingX = startPos.x() + horizontalSpeed * timeOfFlight;
        double landingY = rawTargetPos.y();

        double peakHeight = Math.max(1.2, Math.min(2.2, distanceX * 0.28));

        HitEffectStrategy hitEffect = buildHitEffect(user);

        LobArcMove arc = new LobArcMove(startPos.x(), startPos.y(), landingX, landingY,
                peakHeight, horizontalSpeed);

        Projectile projectile = new Projectile(user,
                startPos,
                new Position(horizontalSpeed, 0.0),
                targetZombie,
                user.getDamage(),
                arc,
                hitEffect
        );
        projectile.setSpawnDelaySeconds(projectile.getSpawnDelaySeconds() + extraDelay);

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
        int splashBonus = (int) Math.round(user.getSpecialUpgrade("SPLASH_DAMAGE_BUFF", 0));
        if (user.getTags().contains(PlantTag.FIRE)) return new FireHit(areaLength, 1.0);
        if (user.getTags().contains(PlantTag.ICE)) {
            return new IceHit(areaLength, 5.0 + user.getSpecialUpgrade("CHILL_DURATION_EXT", 0));
        }
        if (user.getTags().contains(PlantTag.POISON)) return new PoisonHit(areaLength);
        if (user.getName().equalsIgnoreCase("Kernel-pult")) {
            double butterChance = BASE_BUTTER_CHANCE + user.getSpecialUpgrade("BUTTER_CHANCE_BUFF", 0);
            
            
            if (user.isPlantFoodActive() || Math.random() < butterChance) {
                return new ButterHit(areaLength);
            }
        }
        if (user.getTags().contains(PlantTag.PIERCE)) return new PierceHit(-1);
        return new NormalHit(areaLength, splashBonus);
    }
}
