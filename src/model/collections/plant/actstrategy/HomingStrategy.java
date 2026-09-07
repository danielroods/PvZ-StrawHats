package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.HomingMove;
import model.projectile.Projectile;
import model.projectile.hit.HypnotizeHit;
import model.projectile.hit.NormalHit;
import model.projectile.targeting.PlantTarget;
import model.projectile.targeting.TargetFinder;
import model.utils.GameSession;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class HomingStrategy implements ActStrategy {

    public static final double HOMING_SPEED = 5.0;
    public static final String PRIORITIZE_GARGANTUARS_TAG = "PRIORITIZE_GARGANTUARS";
    private static final double TURN_RATE_PER_SECOND = 6.0;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        List<Zombie> zombies = session.getZombies();
        boolean isMagic = user.getTags().contains(PlantTag.MAGIC);
        boolean randomTargeting = isMagic || user.getName().equalsIgnoreCase("Electric Blueberry");

        Zombie zombie = null;
        if (user.hasSpecialUpgrade(PRIORITIZE_GARGANTUARS_TAG)) {
            zombie = randomTargeting ? randomTarget(user, gargantuars(user, zombies))
                    : nearestTarget(user, gargantuars(user, zombies));
        }
        if (zombie == null) {
            zombie = randomTargeting ? randomTarget(user, zombies) : nearestTarget(user, zombies);
        }

        PlantTarget target = zombie != null ? PlantTarget.ofZombie(zombie)
                : TargetFinder.anywhere(user, session, false).nearest();
        if (target == null || target.getPosition() == null) return;

        session.getProjectiles().add(buildProjectile(user, target, isMagic, session));
        user.setInternalTimer(user.getActionInterval());
    }

    private Projectile buildProjectile(Plant user, PlantTarget target, boolean isMagic,
                                       GameSession session) {
        double speed = session.projectileSpeed(HOMING_SPEED);
        Position toTarget = target.getPosition().sub(user.getPosition());
        Position direction = toTarget.length() > 0 ? toTarget.normalize() : Position.of(1, 0);
        Position velocity = direction.scale(speed);

        if (isMagic) {
            int pierceCount = (int) user.getAbilityValue();
            return new Projectile(user,
                    user.getPosition(), velocity, target.getZombie(),
                    user.getDamage(), new HomingMove(target, speed, TURN_RATE_PER_SECOND),
                    new HypnotizeHit(pierceCount)
            );
        }

        return new Projectile(user,
                user.getPosition(), velocity, target.getZombie(),
                user.getDamage(), new HomingMove(target, speed, TURN_RATE_PER_SECOND),
                new NormalHit(1)
        );
    }

    private List<Zombie> gargantuars(Plant user, List<Zombie> zombies) {
        return zombies.stream()
                .filter(zombie -> TargetFinder.isTargetable(user, zombie, false))
                .filter(zombie -> zombie.getRace() == model.collections.zombie.ZombieRace.GARGANTUAR
                        || (zombie.getName() != null
                        && zombie.getName().toLowerCase().contains("gargantuar")))
                .toList();
    }

    private Zombie randomTarget(Plant user, List<Zombie> zombies) {
        List<Zombie> alive = zombies.stream()
                .filter(zombie -> TargetFinder.isTargetable(user, zombie, false)).toList();
        if (alive.isEmpty()) return null;
        return alive.get(ThreadLocalRandom.current().nextInt(alive.size()));
    }

    private Zombie nearestTarget(Plant user, List<Zombie> zombies) {
        Zombie nearest = null;
        double shortest = Double.MAX_VALUE;
        for (Zombie zombie : zombies) {
            if (!TargetFinder.isTargetable(user, zombie, false)) continue;
            double dist = zombie.getPosition().distanceTo(user.getPosition());
            if (dist < shortest) {
                shortest = dist;
                nearest = zombie;
            }
        }
        return nearest;
    }
}
