package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.ArrayList;

public class MeleeStrategy implements ActStrategy {
    @Override
    
    
    public void act(Plant user, GameSession session) {
        if(user.getIntervalTimer() > 0) return;
        ArrayList<Zombie> targets = switch ((int) user.getAbilityValue()) {
            case 1 -> frontBackDetect(user, session);
            case 2 -> areaDetect(user, session);
            case 3 -> waveDetect(user, session);
            case 4 -> swallowDetect(user, session);
            default -> null;
        };
        boolean hitStructure = attackStructures(user, session);
        if((targets == null || targets.isEmpty()) && !hitStructure) return;
        if (targets != null && !targets.isEmpty()) userAct(user , targets);
        user.setInternalTimer(user.getActionInterval());

    }

    private boolean attackStructures(Plant user, GameSession session) {
        Position center = user.getPosition();
        if (center == null) return false;
        int mode = (int) user.getAbilityValue();
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

    private boolean isEnemyAround(Plant user , GameSession session) {
        Position userPos = user.getPosition();
        switch ((int)user.getAbilityValue()) {
            case 1 : // front and back
                for(Zombie zombie : session.getZombies()) {
                    if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
                    if(Math.abs(zombie.getPosition().y() - userPos.y()) < 0.5
                            && Math.abs(zombie.getPosition().x() - userPos.x()) < 1)
                        return true;
                }
                break;
            case 2 : // area damage
                for(Zombie zombie : session.getZombies()) {
                    if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
                    Position zomPos = zombie.getPosition();
                    if(Math.abs(zomPos.x() - userPos.x()) <= 1 && Math.abs(zomPos.y() - userPos.y()) <= 1)
                        return true;
                }
                break;
            case 3 : // wave damage
                for(Zombie zombie : session.getZombies()) {
                    if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
                    if(Math.abs(zombie.getPosition().distanceTo(userPos)) < 5)
                        return true;
                }
                break;
            case 4 : // swallow
                for (Zombie zombie : session.getZombies()) {
                    if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
                    double dx = zombie.getPosition().x() - userPos.x();
                    if(Math.abs(zombie.getPosition().y() - userPos.y()) < 0.5 && dx > 0 && dx < 1)
                        return true;
                }
        }
        return false;
    }

    private ArrayList<Zombie> frontBackDetect(Plant user , GameSession session) {
        Zombie nearestFront = null;
        double shortestFront = Double.MAX_VALUE;
        Zombie nearestBack = null;
        double shortestBack = Double.MAX_VALUE;
        ArrayList<Zombie> targets = new ArrayList<>();

        Position userPos = user.getPosition();
        for(Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position zomPos = zombie.getPosition();
            if(Math.abs(zomPos.y() - userPos.y()) >= 0.5) continue;
            if(zomPos.x() - userPos.x() > 0 && zomPos.x() - userPos.x() < 1) {
                double distance = zomPos.distanceTo(userPos);
                if(distance < shortestFront) {
                    shortestFront = distance;
                    nearestFront = zombie;
                }
            }
            else if(zomPos.x() - userPos.x() < 0 && zomPos.x() - userPos.x() > -1) {
                double distance = zomPos.distanceTo(userPos);
                if(distance < shortestBack) {
                    shortestBack = distance;
                    nearestBack = zombie;
                }
            }
        }
        if(nearestFront != null) targets.add(nearestFront);
        if(nearestBack != null) targets.add(nearestBack);
        return targets;
    }

    private ArrayList<Zombie> areaDetect(Plant user , GameSession session) {
        ArrayList<Zombie> targets = new ArrayList<>();
        Position userPos = user.getPosition();
        for(Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position zomPos = zombie.getPosition();
            if(Math.abs(zomPos.y() - userPos.y()) <= 1 && Math.abs(zomPos.x() - userPos.x()) <= 1)
                targets.add(zombie);
        }
        return targets;
    }

    private ArrayList<Zombie> waveDetect(Plant user , GameSession session) {
        ArrayList<Zombie> targets = new ArrayList<>();
        Position userPos = user.getPosition();
        for(Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position zomPos = zombie.getPosition();
            if(zomPos.distanceTo(userPos) < 5)
                targets.add(zombie);
        }
        return targets;
    }

    private ArrayList<Zombie> swallowDetect(Plant user , GameSession session) {
        ArrayList<Zombie> targets = new ArrayList<>();
        Zombie nearest = null;
        double shortest = Double.MAX_VALUE;

        Position userPos = user.getPosition();
        for(Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position zomPos = zombie.getPosition();
            if(Math.abs(zomPos.y() - userPos.y()) >= 0.5) continue;
            if(zomPos.x() - userPos.x() > 0 && zomPos.x() - userPos.x() < 1) {
                double distance = zomPos.distanceTo(userPos);
                if(distance < shortest) {
                    shortest = distance;
                    nearest = zombie;
                }
            }
        }
        if(nearest != null) targets.add(nearest);
        return targets;
    }

    private void userAct(Plant user , ArrayList<Zombie> targets) {
        int userDamage = user.getDamage();
        for(Zombie zombie : targets) {
            if (user.getTags().contains(PlantTag.FIRE)) zombie.applyStatus(Zombie.Status.FIRED, 3.0);
            if (user.getTags().contains(PlantTag.ICE)) zombie.applyStatus(Zombie.Status.FREEZE, 5.0);
            if (user.getTags().contains(PlantTag.POISON)) zombie.takeDamage(userDamage, true);
            else zombie.takeDamage(userDamage, user);
        }
    }
}

