package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.targeting.PlantTarget;
import model.projectile.targeting.TargetFinder;
import model.utils.GameSession;

import java.util.List;
import java.util.function.Predicate;

public class MeleeStrategy implements ActStrategy {
    public static final String TILE_RANGE_TAG = "TILE_RANGE_EXT";

    private static final double BONK_RANGE = 2.0;
    private static final double WASABI_RANGE = 3.0;
    private static final double PHAT_BEET_RANGE = 2.0;
    private static final double KIWI_RANGE = 1.0;
    private static final double CHOMPER_RANGE = 1.0;
    private static final double HEADBUTTER_LETTUCE_RANGE = 2.0;
    private static final double DEFAULT_MELEE_RANGE = 1.0;
    private static final double AREA_MODE_REACH = 1.0;
    private static final double WAVE_MODE_REACH = 5.0;
    private static final double SAME_ROW_TOLERANCE = 0.5;
    private static final double SELF_TILE_EPSILON = 0.01;
    private static final double HEADBUTTER_LETTUCE_BASE_BUTTER_CHANCE = 0.25;
    private static final double HEADBUTTER_LETTUCE_BUTTER_SECONDS = 5.0;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        Position origin = user.getPosition();
        if (origin == null) return;

        List<PlantTarget> targets = TargetFinder.allWithin(user, session, reach(user), false);
        if (targets.isEmpty()) return;

        PlantTarget closest = nearest(targets, origin);
        if (closest != null && closest.getPosition() != null) {
            user.setMeleeFacingLeft(closest.getPosition().x() < origin.x());
        }

        if ("Chomper".equalsIgnoreCase(user.getName())) {
            bite(user, session, closest);
            return;
        }

        strike(user, session, targets);
        user.setInternalTimer(user.getActionInterval());
    }

    private void bite(Plant user, GameSession session, PlantTarget target) {
        if (target == null) return;

        boolean killed = false;
        Zombie zombie = target.getZombie();
        if (zombie != null) {
            applyElementalStatus(user, zombie);
            int beforeHp = zombie.getHP();
            if (user.getDamage() > 0) zombie.takeDamage(user.getDamage(), user);
            killed = beforeHp > 0 && !zombie.isAlive();
        } else {
            target.takeDamage(user.getDamage(), user, session, isFireDamage(user));
        }

        user.startChomperBite(killed);
        user.setInternalTimer(user.getActionInterval());
    }

    private void strike(Plant user, GameSession session, List<PlantTarget> targets) {
        int userDamage = user.getDamage();
        boolean headbutterLettuce = "Iceberg Lettuce".equalsIgnoreCase(user.getName());
        boolean fire = isFireDamage(user);

        for (PlantTarget target : targets) {
            Zombie zombie = target.getZombie();
            if (zombie == null) {
                target.takeDamage(userDamage, user, session, fire);
                continue;
            }

            applyElementalStatus(user, zombie);
            if (user.getTags().contains(PlantTag.POISON)) zombie.takeDamage(userDamage, true);
            else zombie.takeDamage(userDamage, user);

            
            
            
            if (headbutterLettuce && zombie.isAlive()) {
                double butterChance = HEADBUTTER_LETTUCE_BASE_BUTTER_CHANCE
                        + user.getSpecialUpgrade("BUTTER_CHANCE_BUFF", 0);
                if (Math.random() < butterChance) {
                    zombie.applyStatus(Zombie.Status.BUTTER, HEADBUTTER_LETTUCE_BUTTER_SECONDS);
                }
            }

            if ("Kiwibeast".equalsIgnoreCase(user.getName()) && zombie.isAlive()) {
                
                int hitNumber = user.incrementKiwibeastHitCounter();
                if (hitNumber % 2 == 0) {
                    Position zp = zombie.getPosition();
                    Position up = user.getPosition();
                    if (zp != null && up != null) {
                        double dir = Math.signum(zp.x() - up.x());
                        if (dir == 0) dir = 1;
                        zombie.startKnockback(dir * 1.0, 0.30);
                    }
                }
            }
        }
    }

    private static void applyElementalStatus(Plant user, Zombie zombie) {
        if (user.getTags().contains(PlantTag.FIRE)) zombie.applyStatus(Zombie.Status.FIRED, 3.0);
        if (user.getTags().contains(PlantTag.ICE)) zombie.applyStatus(Zombie.Status.FREEZE, 5.0);
    }

    private static boolean isFireDamage(Plant user) {
        return user.getTags().contains(PlantTag.FIRE);
    }

    private static PlantTarget nearest(List<PlantTarget> targets, Position origin) {
        PlantTarget best = null;
        double shortest = Double.MAX_VALUE;
        for (PlantTarget target : targets) {
            Position at = target.getPosition();
            if (at == null) continue;
            double distance = at.distanceTo(origin);
            if (distance < shortest) {
                shortest = distance;
                best = target;
            }
        }
        return best;
    }

    private static Predicate<Position> reach(Plant user) {
        Position origin = user.getPosition();
        if (origin == null) return at -> false;

        Double sameRowRange = sameRowRange(user);
        if (sameRowRange != null) return sameRow(origin, sameRowRange);

        return switch ((int) user.getAbilityValue()) {
            case 1, 4 -> sameRow(origin, DEFAULT_MELEE_RANGE);
            case 2 -> TargetFinder.box(origin, AREA_MODE_REACH, AREA_MODE_REACH);
            case 3 -> TargetFinder.within(origin, WAVE_MODE_REACH);
            default -> at -> false;
        };
    }

    private static Double sameRowRange(Plant user) {
        String name = user.getName() == null ? "" : user.getName();
        if ("Bonk Choy".equalsIgnoreCase(name)) return meleeRange(user, BONK_RANGE);
        if ("Wasabi Whip".equalsIgnoreCase(name)) return meleeRange(user, WASABI_RANGE);
        if ("Phat Beet".equalsIgnoreCase(name)) return meleeRange(user, PHAT_BEET_RANGE);
        if ("Kiwibeast".equalsIgnoreCase(name)) return meleeRange(user, KIWI_RANGE);
        if ("Chomper".equalsIgnoreCase(name)) return meleeRange(user, CHOMPER_RANGE);
        if ("Iceberg Lettuce".equalsIgnoreCase(name)) return meleeRange(user, HEADBUTTER_LETTUCE_RANGE);
        return null;
    }

    private static Predicate<Position> sameRow(Position origin, double range) {
        return TargetFinder.sameRowWithin(origin, SAME_ROW_TOLERANCE, range, SELF_TILE_EPSILON);
    }

    private static double meleeRange(Plant user, double baseRange) {
        return baseRange + Math.max(0.0, user.getSpecialUpgrade(TILE_RANGE_TAG, 0.0));
    }
}
