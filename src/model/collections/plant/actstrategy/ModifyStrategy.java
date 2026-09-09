package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import controller.match.BeforeMenu;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.projectile.hit.FireHit;
import model.projectile.hit.IceHit;
import model.projectile.hit.PoisonHit;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

public class ModifyStrategy implements ActStrategy {
    public static final String AUTO_PLANT_FOOD_TAG = "AUTO_PLANTFOOD_ON_ENTER";

    private static final double MODIFY_RADIUS = 0.7;
    private static final double IMITATER_IDLE_SECONDS = 1.0;

    private enum ImitaterPhase { IDLE, ATTACK }

    private static final class ImitaterAction {
        final String targetName;
        ImitaterPhase phase = ImitaterPhase.IDLE;

        ImitaterAction(String targetName) {
            this.targetName = targetName;
        }
    }

    private final Map<Plant, ImitaterAction> imitaterActions = new IdentityHashMap<>();

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
        ImitaterAction action = imitaterActions.get(user);
        if (action == null) {
            String targetName = resolveImitaterTargetName();
            if (targetName == null) return;
            action = new ImitaterAction(targetName);
            imitaterActions.put(user, action);
            user.setState(Plant.PlantState.PREPPING);
            user.setVisualAnimationState("idle", IMITATER_IDLE_SECONDS);
            return;
        }

        if (user.getVisualAnimationRemaining() > 0.0) return;

        if (action.phase == ImitaterPhase.IDLE) {
            action.phase = ImitaterPhase.ATTACK;
            double attackDuration = 0.8;
            float resolved = model.collections.animations.AnimationFactory
                    .clipDurationForDisplayName("Imitater", "attack");
            if (resolved > 0f) attackDuration = resolved;
            user.setVisualAnimationState("attack", attackDuration);
            return;
        }

        replaceImitaterWithTarget(user, session, action.targetName);
        imitaterActions.remove(user);
    }

    /** Select exactly one loadout plant at match start and keep that target for the
     * entire lifetime of each Imitater. The Imitater is never copied from a nearby
     * plant, so planting it later cannot make it switch targets dynamically. */
    private String resolveImitaterTargetName() {
        for (String selected : BeforeMenu.selectedPlants) {
            if (selected != null && !selected.equalsIgnoreCase("Imitater")) return selected;
        }
        return null;
    }

    private void replaceImitaterWithTarget(Plant imitater, GameSession session, String targetName) {
        if (imitater == null || session == null || targetName == null || !imitater.isAlive()) return;
        PlantJsonParser.PlantConfig targetConfig = null;
        for (PlantJsonParser.PlantConfig config : PlantFactory.getBlueprints().values()) {
            if (config != null && config.name != null && config.name.equalsIgnoreCase(targetName)) {
                targetConfig = config;
                break;
            }
        }
        if (targetConfig == null) return;

        Position pos = imitater.getPosition();
        if (pos == null) return;
        int row = (int) Math.round(pos.y());
        int col = (int) Math.round(pos.x());
        Plant replacement = PlantFactory.createPlant(targetConfig.id, imitater.getLevel(),
                new Position(col, row));

        // Replace the board occupant itself. We do not mutate the Imitater into
        // another runtime plant: the selected target becomes a real Plant object
        // occupying the exact same tile.
        if (!session.removePlantAt(row, col)) return;
        if (!session.plantAt(row, col, replacement)) {
            session.plantAt(row, col, imitater);
            return;
        }
        if (imitater.hasSpecialUpgrade(AUTO_PLANT_FOOD_TAG)) {
            replacement.activatePlant(session);
        }
        imitater.setAlive(false);
    }

    private void disarmNearestZombie(Plant user, GameSession session) {
        Zombie nearest = null;
        double shortest = Double.MAX_VALUE;
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()
                    || zombie.getPosition() == null || !hasMetalArmour(zombie)) continue;
            double distance = zombie.getPosition().distanceTo(user.getPosition());
            if (distance < shortest) {
                shortest = distance;
                nearest = zombie;
            }
        }
        if (nearest != null) {
            // Strips the armor object outright (not just zeroing its HP) so the zombie
            // is left exactly like a basic zombie - see ZombieArmorMask, which now
            // explicitly hides every armor element whenever a zombie has no live
            // Armour, instead of leaving it to whatever the PAM's clip defaults to.
            nearest.setArmour(null);
            user.setInternalTimer(user.getActionInterval());
            startMagnetPullAnimation(user);
        }
    }

    /** Only bucket and crown armor (Dark Ages basic zombie's crown+shoulder set) are
     * flagged metallic in the armor data - anything else (cone, brick, newspaper,
     * shoulder armor on its own) is not something Magnet-shroom can pull off. */
    private boolean hasMetalArmour(Zombie zombie) {
        return zombie.getArmour() instanceof model.collections.armour.ZombieArmour armour
                && armour.getHP() > 0 && armour.isMetal();
    }

    /** Kicks off the "special" clip: the caught item travels to the plant over that
     * clip's own duration. Plant#tickVisualAnimation carries it on to "catch" and then
     * back to idle (with the Magnet_Item element left visible) once it lands. */
    private void startMagnetPullAnimation(Plant user) {
        float specialDuration = model.collections.animations.AnimationFactory
                .clipDurationForDisplayName(user.getName(), "special");
        user.setVisualAnimationState("special", specialDuration > 0f ? specialDuration : 0.9);
    }

    private void hypnotizeTouchingZombie(Plant user, GameSession session) {
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized() || zombie.getPosition() == null) continue;
            if (zombie.getPosition().distanceTo(user.getPosition()) <= 0.6) {
                zombie.hypnotize(user.getSpecialUpgrade("ZOMBIE_HEALTH_MULTIPLIER", 1.0),
                        user.getSpecialUpgrade("ZOMBIE_DAMAGE_MULTIPLIER", 1.0));
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
            if (!projectile.isAlive() || projPos == null || projectile.isLobbed()) continue;
            if (projectile.isTorchwoodTransformed() || !carriesPeaPhysics(projectile)) continue;
            if (projectile.distanceFromPathTo(userPos) < MODIFY_RADIUS) {
                targets.add(projectile);
            }
        }
        return targets;
    }

    private boolean carriesPeaPhysics(Projectile projectile) {
        Plant source = projectile.getSourcePlant();
        return source != null && source.getTags() != null
                && source.getTags().contains(PlantTag.PEA);
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