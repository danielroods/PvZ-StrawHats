package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.projectile.hit.FireHit;
import model.projectile.hit.IceHit;
import model.projectile.hit.PoisonHit;
import model.utils.GameSession;

import java.util.ArrayList;

public class ModifyStrategy implements ActStrategy {
    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        if (user.getName().equalsIgnoreCase("Imitater")) {
            imitateNearestPlant(user, session);
            return;
        }
        if (user.getName().equalsIgnoreCase("Magnet-shroom")) {
            disarmNearestZombie(user, session);
            return;
        }
        if ((int) user.getAbilityValue() == 3 || user.getName().equalsIgnoreCase("Hypno-shroom")) {
            hypnotizeTouchingZombie(user, session);
            return;
        }
        if ((int) user.getAbilityValue() == 2) {
            modifyTargets(user, projectileThroughDetect(user, session));
        }
    }

    private void imitateNearestPlant(Plant user, GameSession session) {
        Plant source = null;
        double shortest = Double.MAX_VALUE;
        for (Plant plant : session.getPlants()) {
            if (plant == null || plant == user || !plant.isAlive() || plant.getPosition() == null) continue;
            double distance = plant.getPosition().distanceTo(user.getPosition());
            if (distance < shortest) {
                shortest = distance;
                source = plant;
            }
        }
        if (source == null) return;
        user.setHP(source.getHP());
        user.setType(source.getType());
        user.setAbilityType(source.getAbilityType());
        user.setAbilityValue(source.getAbilityValue());
        user.setDamage(source.getDamage());
        user.setActionInterval(source.getActionInterval());
        user.getTags().clear();
        user.getTags().addAll(source.getTags());
        user.setShootingVectors(new ArrayList<>(source.getShootingVectors()));
        user.setActStrategy(source.getActStrategy());
        user.setPlantFoodEffect(source.getPlantFoodEffect());
    }

    private void disarmNearestZombie(Plant user, GameSession session) {
        Zombie nearest = null;
        double shortest = Double.MAX_VALUE;
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()
                    || zombie.getPosition() == null || zombie.getArmour() == null || zombie.getArmour().getHP() <= 0) continue;
            double distance = zombie.getPosition().distanceTo(user.getPosition());
            if (distance < shortest) {
                shortest = distance;
                nearest = zombie;
            }
        }
        if (nearest != null) {
            nearest.setArmour(null);
            user.setInternalTimer(user.getActionInterval());
        }
    }

    private void hypnotizeTouchingZombie(Plant user, GameSession session) {
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized() || zombie.getPosition() == null) continue;
            if (zombie.getPosition().distanceTo(user.getPosition()) <= 0.6) {
                zombie.hypnotize();
                user.setAlive(false);
                return;
            }
        }
    }

    private ArrayList<Projectile> projectileThroughDetect(Plant user, GameSession session) {
        Position userPos = user.getPosition();
        ArrayList<Projectile> targets = new ArrayList<>();
        for (Projectile projectile : session.getProjectiles()) {
            Position projPos = projectile.getPosition();
            if (projectile.isAlive() && projPos != null && userPos.distanceTo(projPos) < 0.7) targets.add(projectile);
        }
        return targets;
    }

    private void modifyTargets(Plant user, ArrayList<Projectile> targets) {
        if (user.getTags().contains(PlantTag.FIRE)) {
            double multiplier = user.isPlantFoodActive() ? 3.0 : 2.0;
            for (Projectile projectile : targets) if (!(projectile.getHitEffectStrategy() instanceof FireHit)) projectile.setHitEffectStrategy(new FireHit(1, multiplier));
        } else if (user.getTags().contains(PlantTag.ICE)) {
            for (Projectile projectile : targets) if (!(projectile.getHitEffectStrategy() instanceof IceHit)) projectile.setHitEffectStrategy(new IceHit(1));
        } else if (user.getTags().contains(PlantTag.POISON)) {
            for (Projectile projectile : targets) if (!(projectile.getHitEffectStrategy() instanceof PoisonHit)) projectile.setHitEffectStrategy(new PoisonHit(1));
        }
    }
}
