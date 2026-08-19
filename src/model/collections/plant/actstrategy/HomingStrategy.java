package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.projectile.hit.HypnotizeHit;
import model.projectile.hit.NormalHit;
import model.utils.GameSession;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class HomingStrategy implements ActStrategy {

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        List<Zombie> zombies = session.getZombies();
        if (zombies.isEmpty()) return;

        boolean isMagic = user.getTags().contains(PlantTag.MAGIC);
        boolean randomTargeting = isMagic || user.getName().equalsIgnoreCase("Electric Blueberry");
        Zombie target = randomTargeting ? randomTarget(zombies) : nearestTarget(user, zombies);
        if (target == null) return;

        session.getProjectiles().add(buildProjectile(user, target, isMagic));
        user.setInternalTimer(user.getActionInterval());
    }

    private Projectile buildProjectile(Plant user, Zombie target, boolean isMagic) {
        Position direction = target.getPosition().sub(user.getPosition()).normalize();
        Position velocity = direction.scale(20.0);

        if (isMagic) {
            int pierceCount = (int) user.getAbilityValue();
            return new Projectile(user,
                    user.getPosition(), velocity, target,
                    user.getDamage(), new StraightMove(), new HypnotizeHit(pierceCount)
            );
        }

        return new Projectile(user,
                user.getPosition(), velocity, target,
                user.getDamage(), new StraightMove(), new NormalHit(1)
        );
    }

    private Zombie randomTarget(List<Zombie> zombies) {
        List<Zombie> alive = zombies.stream().filter(z -> z != null && z.isAlive()).toList();
        if (alive.isEmpty()) return null;
        return alive.get(ThreadLocalRandom.current().nextInt(alive.size()));
    }

    private Zombie nearestTarget(Plant user, List<Zombie> zombies) {
        Zombie nearest = null;
        double shortest = Double.MAX_VALUE;
        for (Zombie z : zombies) {
            if (z == null || !z.isAlive()) continue;
            double dist = z.getPosition().distanceTo(user.getPosition());
            if (dist < shortest) {
                shortest = dist;
                nearest = z;
            }
        }
        return nearest;
    }
}
