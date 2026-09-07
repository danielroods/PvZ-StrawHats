package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.HomingMove;
import model.projectile.Projectile;
import model.projectile.hit.NormalHit;
import model.projectile.targeting.PlantTarget;
import model.projectile.targeting.TargetFinder;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HomingBarrage implements PlantFoodEffect {
    private static final double SPIKE_SPEED = 6.0;
    private static final double TURN_RATE_PER_SECOND = 9.0;
    private static final double TAIL_SECONDS = 0.6;

    private final int spikes;
    private final double fireInterval;
    private final double damageMultiplier;

    private double elapsed = 0.0;
    private int fired = 0;
    private int nextTarget = 0;

    public HomingBarrage(int spikes, double fireInterval, double damageMultiplier) {
        this.spikes = Math.max(1, spikes);
        this.fireInterval = fireInterval > 0 ? fireInterval : 0.12;
        this.damageMultiplier = damageMultiplier > 0 ? damageMultiplier : 1.0;
    }

    @Override
    public double getDurationSeconds() {
        return spikes * fireInterval + TAIL_SECONDS;
    }

    @Override
    public boolean drivesActStrategy() {
        return true;
    }

    @Override
    public void reset() {
        elapsed = 0.0;
        fired = 0;
        nextTarget = 0;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        fireOne(plant, session);
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        GameSession session = GameSession.peekInstance();
        while (elapsed >= fireInterval && fired < spikes) {
            elapsed -= fireInterval;
            fireOne(plant, session);
        }
    }

    private void fireOne(Plant plant, GameSession session) {
        if (session == null || fired >= spikes || plant.getPosition() == null) return;
        fired++;

        List<PlantTarget> targets = liveTargets(plant, session);
        if (targets.isEmpty()) return;

        PlantTarget target = targets.get(nextTarget % targets.size());
        nextTarget++;

        double speed = session.projectileSpeed(SPIKE_SPEED);
        Position toTarget = target.getPosition().sub(plant.getPosition());
        Position direction = toTarget.length() > 0 ? toTarget.normalize() : Position.of(1, 0);

        int damage = Math.max(1, (int) Math.round(plant.getDamage() * damageMultiplier));
        Projectile spike = new Projectile(plant,
                plant.getPosition(),
                direction.scale(speed),
                target.getZombie(),
                damage,
                new HomingMove(target, speed, TURN_RATE_PER_SECOND),
                new NormalHit(1)
        );
        spike.setSpawnDelaySeconds(0.0);
        session.getProjectiles().add(spike);

        if (fired >= spikes) plant.setInternalTimer(plant.getActionInterval());
    }

    private List<PlantTarget> liveTargets(Plant plant, GameSession session) {
        List<PlantTarget> targets = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (!TargetFinder.isTargetable(plant, zombie, false)) continue;
            targets.add(PlantTarget.ofZombie(zombie));
        }
        if (targets.isEmpty()) {
            PlantTarget object = TargetFinder.anywhere(plant, session, false).nearest();
            if (object != null && object.getPosition() != null) targets.add(object);
        }
        targets.sort(Comparator.comparingDouble(
                target -> target.getPosition().distanceTo(plant.getPosition())));
        return targets;
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
