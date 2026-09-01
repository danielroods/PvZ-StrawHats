package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.pitches.Cell;
import model.pitches.TileType;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import model.collections.plant.PlantFactory;

public class ExplodeStrategy implements ActStrategy {
    private static final double TRAP_ACTIVATION_RADIUS = 0.3;
    private static final double BASE_FREEZE_SECONDS = 5.0;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getPosition() == null) return;

        if (user.isPotatoMine()) {
            actPotatoMine(user, session);
            return;
        }

        if (user.getIntervalTimer() > 0) return;

        if (user.getName().equalsIgnoreCase("Doom-shroom")) {
            explodeDoomShroom(user, session);
            return;
        }

        ArrayList<Zombie> targets;
        switch ((int) user.getAbilityValue()) {
            case 1 -> {
                if (actsWithoutTouch(user)) targets = new ArrayList<>();
                else if (!isZombieTouch(user, session)) return;
                else targets = touchDetect(user, session);
            }
            case 2 -> targets = areaDetect(user, session);
            case 3 -> targets = lineDetect(user, session);
            case 4 -> targets = wholePitchDetect(session);
            default -> targets = new ArrayList<>();
        }

        userAct(user, targets);
        damageStructures(user,session);
        if (user.getTags().contains(PlantTag.FIRE)) {
            int mode = (int) user.getAbilityValue();
            FrostbiteFreezing.damageAdjacentIceBlocks(session, user.getPosition(), mode, user.getDamage(), true);
        }
        user.setAlive(false);
    }

    private void actPotatoMine(Plant user, GameSession session) {
        if (user.isPotatoMineDetonationPending()) {
            if (user.getIntervalTimer() > 0.0001) return;

            Position center = user.getPosition();
            int row = (int) Math.round(center.y());
            int col = (int) Math.round(center.x());
            int damage = Math.max(1, user.getDamage());
            int tileRadius = "Primal Potato Mine".equalsIgnoreCase(user.getName()) ? 1 : 0;

            for (Zombie zombie : session.getZombies()) {
                if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
                int zr = (int) Math.round(zombie.getPosition().y());
                int zc = (int) Math.round(zombie.getPosition().x());
                if (Math.abs(zr - row) <= tileRadius && Math.abs(zc - col) <= tileRadius) {
                    zombie.takeDamageWithAsh(damage, user);
                }
            }

            damageStructures(user, session);
            user.finishPotatoMineAttack();
            user.setAlive(false);
            return;
        }

        if (!user.isPotatoMineArmed()) return;
        if ("recover".equals(user.getVisualAnimationState())
                || "plantfood2".equals(user.getVisualAnimationState())) return;
        Position center = user.getPosition();
        int row = (int) Math.round(center.y());
        int col = (int) Math.round(center.x());
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            int zr = (int) Math.round(zombie.getPosition().y());
            int zc = (int) Math.round(zombie.getPosition().x());
            if (zr == row && zc == col) {
                user.startPotatoMineAttack();
                return;
            }
        }
    }

    private void explodeDoomShroom(Plant user, GameSession session) {
        Position center = user.getPosition();
        if (center == null || session.getEnvironment() == null) return;

        int stage = Math.max(1, Math.min(3, user.getGrowthStage()));
        int radius = stage;
        int damage = Math.max(1, user.getDamage());

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            double dx = Math.abs(zombie.getPosition().x() - center.x());
            double dy = Math.abs(zombie.getPosition().y() - center.y());
            if (Math.max(dx, dy) <= radius) {
                zombie.takeDamage(damage, user);
            }
        }

        for (model.collections.zombie.zombie_pushing_item.PushableStructure structure
                : session.getPushableStructures()) {
            if (structure == null || !structure.isAlive() || structure.getPosition() == null) continue;
            double dx = Math.abs(structure.getPosition().x() - center.x());
            double dy = Math.abs(structure.getPosition().y() - center.y());
            if (Math.max(dx, dy) <= radius) {
                structure.takeDamage(damage, user, session);
            }
        }

        int row = (int) Math.round(center.y());
        int col = (int) Math.round(center.x());
        session.scorchTile(row, col, 10.0);

        if (stage > 1) {
            List<Position> candidates = new ArrayList<>();
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nr = row + dy;
                    int nc = col + dx;
                    if (nr < 0 || nr >= session.getEnvironment().getRows()
                            || nc < 0 || nc >= session.getEnvironment().getCols()) continue;
                    if (session.isScorchedTile(nr, nc)) continue;
                    Cell neighbor = session.getEnvironment().getCell(nr, nc);
                    if (neighbor == null || neighbor.hasPlant() || neighbor.getObstacle() != null) continue;
                    if (neighbor.getTile() != null && neighbor.getTile().type() == TileType.Slippery) continue;
                    candidates.add(new Position(nc, nr));
                }
            }
            java.util.Collections.shuffle(candidates, ThreadLocalRandom.current());
            int spawnCount = Math.min(candidates.size(), ThreadLocalRandom.current().nextInt(1, 3));
            for (int i = 0; i < spawnCount; i++) {
                Position spawn = candidates.get(i);
                Plant clone = PlantFactory.createPlantByName("Doom-shroom", user.getLevel(), spawn);
                if (!session.plantAt((int) spawn.y(), (int) spawn.x(), clone)) {
                    clone.setAlive(false);
                }
            }
        }

        user.setAlive(false);
    }
    private boolean actsWithoutTouch(Plant user) {
        return user.getName().equalsIgnoreCase("Hot Potato");
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
        boolean requireWater = user.getName().equalsIgnoreCase("Tangle Kelp");
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            double distance = zombie.getPosition().distanceTo(user.getPosition());
            if (distance >= TRAP_ACTIVATION_RADIUS) continue;
            if (requireWater && !isOnWaterTile(zombie.getPosition(), session)) continue;
            return true;
        }
        return false;
    }

    private ArrayList<Zombie> touchDetect(Plant user, GameSession session) {
        ArrayList<Zombie> targets = new ArrayList<>();
        boolean requireWater = user.getName().equalsIgnoreCase("Tangle Kelp");
        Zombie firstTouch = null;
        double shortest = Double.MAX_VALUE;
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            if (requireWater && !isOnWaterTile(zombie.getPosition(), session)) continue;
            double distance = zombie.getPosition().distanceTo(user.getPosition());
            if (distance >= TRAP_ACTIVATION_RADIUS) continue;
            if (distance < shortest) {
                shortest = distance;
                firstTouch = zombie;
            }
        }
        if (firstTouch != null) targets.add(firstTouch);
        return targets;
    }

    private boolean isOnWaterTile(Position position, GameSession session) {
        if (position == null || session.getEnvironment() == null) return false;
        int row = (int) Math.round(position.y());
        int col = (int) Math.round(position.x());
        Cell cell = session.getEnvironment().getCell(row, col);
        return cell != null && cell.getTile() != null && cell.getTile().type() == TileType.Water;
    }

    private ArrayList<Zombie> areaDetect(Plant user, GameSession session) {
        ArrayList<Zombie> targets = new ArrayList<>();
        Position center = user.getPosition();
        boolean cherryBomb = "Cherry Bomb".equalsIgnoreCase(user.getName());

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position pos = zombie.getPosition();

            if (cherryBomb) {
                 if (Math.abs(pos.y() - center.y()) <= 1
                        && pos.x() >= center.x() - 1
                        && pos.x() <= center.x() + 2) {
                    targets.add(zombie);
                }
            } else if (Math.abs(pos.y() - center.y()) <= 1
                    && Math.abs(pos.x() - center.x()) <= 1) {
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
            if (user.getTags().contains(PlantTag.ICE)) {
                zombie.applyStatus(Zombie.Status.FROZEN,
                        BASE_FREEZE_SECONDS + user.getSpecialUpgrade("FREEZE_DURATION_EXT", 0));
            } else if (user.getTags().contains(PlantTag.FIRE)) {
                zombie.setStatus(Zombie.Status.FIRED);
            }
            if ("Cherry Bomb".equalsIgnoreCase(user.getName())
                    || "Jalapeno".equalsIgnoreCase(user.getName())) {
                zombie.takeDamageWithAsh(damage, user);
            } else {
                zombie.takeDamage(damage, user);
            }
        }
    }
}
