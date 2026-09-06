package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MeleeStrategy implements ActStrategy {
    public static final String TILE_RANGE_TAG = "TILE_RANGE_EXT";

    private static final double BONK_RANGE = 2.0;
    private static final double WASABI_RANGE = 3.0;
    private static final double PHAT_BEET_RANGE = 2.0;
    private static final double KIWI_RANGE = 1.0;
    private static final double CHOMPER_RANGE = 1.0;
    // Headbutter Lettuce (Plants.json entry "Iceberg Lettuce" - see AnimationFactory's
    // ICEBERG_LETTUCE -> HEADBUTTER_LETTUCE override): a plain 2-tile melee headbutt, both
    // in front of and behind the plant (sameRowTargets doesn't care about direction, only
    // distance - see act()/attackStructures() below).
    private static final double HEADBUTTER_LETTUCE_RANGE = 2.0;
    // Same random chance (plus the same BUTTER_CHANCE_BUFF upgrade) Kernel-pult uses for its
    // normal (non-Plant-Food) butter shots - see LobberStrategy.BASE_BUTTER_CHANCE - and the
    // same 5s stun length ButterHit applies.
    private static final double HEADBUTTER_LETTUCE_BASE_BUTTER_CHANCE = 0.25;
    private static final double HEADBUTTER_LETTUCE_BUTTER_SECONDS = 5.0;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        String name = user.getName() == null ? "" : user.getName();

        if ("Chomper".equalsIgnoreCase(name)) {
            actChomper(user, session);
            return;
        }

        ArrayList<Zombie> targets;
        if ("Bonk Choy".equalsIgnoreCase(name) || "Wasabi Whip".equalsIgnoreCase(name)) {
            targets = sameRowTargets(user, session, meleeRange(user,
                    "Bonk Choy".equalsIgnoreCase(name) ? BONK_RANGE : WASABI_RANGE));
        } else if ("Phat Beet".equalsIgnoreCase(name)) {
            targets = sameRowTargets(user, session, meleeRange(user, PHAT_BEET_RANGE));
        } else if ("Kiwibeast".equalsIgnoreCase(name)) {
            targets = sameRowTargets(user, session, meleeRange(user, KIWI_RANGE));
        } else if ("Iceberg Lettuce".equalsIgnoreCase(name)) {
            targets = sameRowTargets(user, session, meleeRange(user, HEADBUTTER_LETTUCE_RANGE));
        } else {
            targets = switch ((int) user.getAbilityValue()) {
                case 1 -> frontBackDetect(user, session);
                case 2 -> areaDetect(user, session);
                case 3 -> waveDetect(user, session);
                case 4 -> swallowDetect(user, session);
                default -> null;
            };
        }

        boolean hitStructure = attackStructures(user, session);
        if ((targets == null || targets.isEmpty()) && !hitStructure) return;

        if (targets != null && !targets.isEmpty()) {
            // Direction is determined by the actual hit target. For a target behind
            // the plant, the renderer mirrors the attack animation.
            Zombie closest = targets.stream()
                    .min(Comparator.comparingDouble(z -> z.getPosition().distanceTo(user.getPosition())))
                    .orElse(null);
            if (closest != null) {
                user.setMeleeFacingLeft(closest.getPosition().x() < user.getPosition().x());
            }
            userAct(user, targets);
        }

        user.setInternalTimer(user.getActionInterval());
    }

    private void actChomper(Plant user, GameSession session) {
        ArrayList<Zombie> targets = sameRowTargets(user, session, CHOMPER_RANGE);
        if (targets.isEmpty()) return;

        Zombie target = targets.stream()
                .filter(z -> z.getPosition() != null)
                .min(Comparator.comparingDouble(z -> z.getPosition().distanceTo(user.getPosition())))
                .orElse(null);
        if (target == null) return;

        user.setMeleeFacingLeft(target.getPosition().x() < user.getPosition().x());

        boolean killed = false;
        if (user.getTags().contains(PlantTag.FIRE)) target.applyStatus(Zombie.Status.FIRED, 3.0);
        if (user.getTags().contains(PlantTag.ICE)) target.applyStatus(Zombie.Status.FREEZE, 5.0);

        int beforeHp = target.getHP();
        if (user.getDamage() > 0) target.takeDamage(user.getDamage(), user);
        killed = beforeHp > 0 && !target.isAlive();

        // The normal Chomper bite has its own terminal clip. If it killed a zombie,
        // immediately enter the 10-second chewing/special state.
        user.startChomperBite(killed);
        user.setInternalTimer(user.getActionInterval());
    }

    private boolean attackStructures(Plant user, GameSession session) {
        Position center = user.getPosition();
        if (center == null) return false;
        String name = user.getName() == null ? "" : user.getName();

        double range;
        if ("Bonk Choy".equalsIgnoreCase(name) || "Wasabi Whip".equalsIgnoreCase(name)) {
            range = meleeRange(user, "Bonk Choy".equalsIgnoreCase(name) ? BONK_RANGE : WASABI_RANGE);
        } else if ("Phat Beet".equalsIgnoreCase(name)) {
            range = meleeRange(user, PHAT_BEET_RANGE);
        } else if ("Kiwibeast".equalsIgnoreCase(name)) {
            range = meleeRange(user, KIWI_RANGE);
        } else if ("Chomper".equalsIgnoreCase(name)) {
            range = meleeRange(user, CHOMPER_RANGE);
        } else if ("Iceberg Lettuce".equalsIgnoreCase(name)) {
            range = meleeRange(user, HEADBUTTER_LETTUCE_RANGE);
        } else {
            int mode = (int) user.getAbilityValue();
            return attackStructuresLegacy(user, session, mode);
        }

        boolean hit = false;
        for (model.collections.zombie.zombie_pushing_item.PushableStructure structure : session.getPushableStructures()) {
            Position position = structure.getPosition();
            if (position == null) continue;
            if (Math.abs(position.y() - center.y()) < 0.5
                    && Math.abs(position.x() - center.x()) <= range) {
                structure.takeDamage(Math.max(0, user.getDamage()), user, session);
                hit = true;
            }
        }
        return hit;
    }

    private boolean attackStructuresLegacy(Plant user, GameSession session, int mode) {
        Position center = user.getPosition();
        if (center == null) return false;
        boolean hit = false;
        for (model.collections.zombie.zombie_pushing_item.PushableStructure structure : session.getPushableStructures()) {
            Position position = structure.getPosition();
            if (position == null) continue;
            double dx = position.x() - center.x();
            double dy = position.y() - center.y();
            boolean inRange = switch (mode) {
                case 1 -> Math.abs(dy) < 0.5 && Math.abs(dx) < 1;
                case 2 -> Math.abs(dx) <= 1 && Math.abs(dy) <= 1;
                case 3 -> position.distanceTo(center) < 5;
                case 4 -> Math.abs(dy) < 0.5 && dx > 0 && dx < 1;
                default -> false;
            };
            if (inRange) {
                structure.takeDamage(Math.max(0, user.getDamage()), user, session);
                hit = true;
            }
        }
        return hit;
    }

    private static double meleeRange(Plant user, double baseRange) {
        return baseRange + Math.max(0.0, user.getSpecialUpgrade(TILE_RANGE_TAG, 0.0));
    }

    private ArrayList<Zombie> sameRowTargets(Plant user, GameSession session, double range) {
        ArrayList<Zombie> targets = new ArrayList<>();
        Position origin = user.getPosition();
        if (origin == null) return targets;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position p = zombie.getPosition();
            if (Math.abs(p.y() - origin.y()) < 0.5
                    && Math.abs(p.x() - origin.x()) <= range
                    && Math.abs(p.x() - origin.x()) > 0.01) {
                targets.add(zombie);
            }
        }
        return targets;
    }

    private ArrayList<Zombie> frontBackDetect(Plant user, GameSession session) {
        return sameRowTargets(user, session, 1.0);
    }

    private ArrayList<Zombie> areaDetect(Plant user, GameSession session) {
        ArrayList<Zombie> targets = new ArrayList<>();
        Position userPos = user.getPosition();
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position zomPos = zombie.getPosition();
            if (Math.abs(zomPos.x() - userPos.x()) <= 1 && Math.abs(zomPos.y() - userPos.y()) <= 1)
                targets.add(zombie);
        }
        return targets;
    }

    private ArrayList<Zombie> waveDetect(Plant user, GameSession session) {
        ArrayList<Zombie> targets = new ArrayList<>();
        Position userPos = user.getPosition();
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            if (zombie.getPosition().distanceTo(userPos) < 5) targets.add(zombie);
        }
        return targets;
    }

    private ArrayList<Zombie> swallowDetect(Plant user, GameSession session) {
        return sameRowTargets(user, session, 1.0);
    }

    private void userAct(Plant user, ArrayList<Zombie> targets) {
        int userDamage = user.getDamage();
        boolean headbutterLettuce = "Iceberg Lettuce".equalsIgnoreCase(user.getName());
        for (Zombie zombie : targets) {
            if (user.getTags().contains(PlantTag.FIRE)) zombie.applyStatus(Zombie.Status.FIRED, 3.0);
            if (user.getTags().contains(PlantTag.ICE)) zombie.applyStatus(Zombie.Status.FREEZE, 5.0);
            if (user.getTags().contains(PlantTag.POISON)) zombie.takeDamage(userDamage, true);
            else zombie.takeDamage(userDamage, user);

            // Headbutter Lettuce sometimes butters the zombie it just hit - same chance
            // (base + BUTTER_CHANCE_BUFF upgrade) and stun length as Kernel-pult's normal
            // (non-Plant-Food) butter shots.
            if (headbutterLettuce && zombie.isAlive()) {
                double butterChance = HEADBUTTER_LETTUCE_BASE_BUTTER_CHANCE
                        + user.getSpecialUpgrade("BUTTER_CHANCE_BUFF", 0);
                if (Math.random() < butterChance) {
                    zombie.applyStatus(Zombie.Status.BUTTER, HEADBUTTER_LETTUCE_BUTTER_SECONDS);
                }
            }

            if ("Kiwibeast".equalsIgnoreCase(user.getName()) && zombie.isAlive()) {
                // Not every Kiwi attack knocks back. Every second successful hit does.
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
}