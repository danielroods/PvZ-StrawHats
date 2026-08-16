package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ExplodeStrategy implements ActStrategy {
    private static final double TRAP_ACTIVATION_RADIUS = 0.3;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0 || user.getPosition() == null) return;

        ArrayList<Zombie> targets;
        switch ((int) user.getAbilityValue()) {
            case 1 -> {
                if (!isZombieTouch(user, session)) return;
                targets = touchDetect(user, session);
            }
            case 2 -> targets = areaDetect(user, session);
            case 3 -> targets = lineDetect(user, session);
            case 4 -> targets = wholePitchDetect(session);
            default -> targets = new ArrayList<>();
        }

        userAct(user, targets);
        if (user.getName().equalsIgnoreCase("Grapeshot")) {
            applyGrapeBounces(user, session, targets);
        }
        damageStructures(user,session);
        if (user.getTags().contains(PlantTag.FIRE)) {
            int mode = (int) user.getAbilityValue();
            FrostbiteFreezing.damageAdjacentIceBlocks(session, user.getPosition(), mode, user.getDamage(), true);
        }
        user.setAlive(false);
    }
    private void damageStructures(Plant user, GameSession session) {
        Position center = user.getPosition();
        if (center == null) return;
        int mode = (int) user.getAbilityValue();
        for (model.collections.zombie.zombie_pushing_item.PushableStructure structure : session.getPushableStructures()) {
            Position position = structure.getPosition();
            if (position == null) continue;
            boolean inRange = switch (mode) {
                case 1 -> position.distanceTo(center) <= TRAP_ACTIVATION_RADIUS;
                case 2 -> Math.abs(position.x() - center.x()) <= 1 && Math.abs(position.y() - center.y()) <= 1;
                case 3 -> Math.abs(position.y() - center.y()) < 0.5;
                case 4 -> true;
                default -> false;
            };
            if (inRange) structure.takeDamage(Math.max(0, user.getDamage()), user, session);
        }
    }

    private boolean isZombieTouch(Plant user, GameSession session) {
        for (Zombie zombie : session.getZombies()) {
            if (zombie != null && zombie.isAlive() && zombie.getPosition() != null
                    && zombie.getPosition().distanceTo(user.getPosition()) < TRAP_ACTIVATION_RADIUS) return true;
        }
        return false;
    }

    private ArrayList<Zombie> touchDetect(Plant user, GameSession session) {
        ArrayList<Zombie> targets = new ArrayList<>();
        Zombie firstTouch = null;
        double shortest = Double.MAX_VALUE;
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            double distance = zombie.getPosition().distanceTo(user.getPosition());
            if (distance < shortest) {
                shortest = distance;
                firstTouch = zombie;
            }
        }
        if (firstTouch != null) targets.add(firstTouch);
        return targets;
    }

    private ArrayList<Zombie> areaDetect(Plant user, GameSession session) {
        ArrayList<Zombie> targets = new ArrayList<>();
        Position center = user.getPosition();
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position pos = zombie.getPosition();
            if (Math.abs(pos.y() - center.y()) <= 1 && Math.abs(pos.x() - center.x()) <= 1) {
                targets.add(zombie);
            }
        }
        return targets;
    }

    private ArrayList<Zombie> lineDetect(Plant user, GameSession session) {
        ArrayList<Zombie> targets = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (zombie != null && zombie.isAlive() && zombie.getPosition() != null
                    && Math.abs(user.getPosition().y() - zombie.getPosition().y()) < 0.5) targets.add(zombie);
        }
        return targets;
    }

    private ArrayList<Zombie> wholePitchDetect(GameSession session) {
        return new ArrayList<>(session.getZombies());
    }

    private void userAct(Plant user, ArrayList<Zombie> targets) {
        int damage = user.getDamage();
        for (Zombie zombie : targets) {
            if (zombie == null || !zombie.isAlive()) continue;
            if (user.getTags().contains(PlantTag.ICE)) zombie.setStatus(Zombie.Status.FREEZE);
            else if (user.getTags().contains(PlantTag.FIRE)) zombie.setStatus(Zombie.Status.FIRED);
            zombie.takeDamage(damage, user);
        }
    }

    private void applyGrapeBounces(Plant user, GameSession session, ArrayList<Zombie> primaryTargets) {
        Position center = user.getPosition();
        int range = user.getRawUpgrades().contains("GRAPE_BOUNCE_EXT") ? 6 : 4;
        int bounceDamage = Math.max(1, user.getDamage() / 4);
        Set<Zombie> primary = new HashSet<>(primaryTargets);
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || primary.contains(zombie) || zombie.getPosition() == null) continue;
            double dx = zombie.getPosition().x() - center.x();
            double dy = zombie.getPosition().y() - center.y();
            boolean onGrapePath = Math.abs(dy) < 0.5 || Math.abs(dx) < 0.5
                    || Math.abs(Math.abs(dx) - Math.abs(dy)) < 0.5;
            if (onGrapePath && Math.max(Math.abs(dx), Math.abs(dy)) <= range) {
                zombie.takeDamage(bounceDamage, user);
            }
        }
    }
}