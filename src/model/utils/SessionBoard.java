package model.utils;

import controller.QuestManager;
import model.collections.armour.ArmourFactory;
import model.collections.armour.ArmourType;
import model.collections.armour.PlantArmour;
import model.collections.item.GroundPlantFood;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.LawnMower;
import model.pitches.TileType;
import model.pitches.obstacles.Grave;
import model.pitches.obstacles.IceBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Everything that reads or mutates the match grid itself: placing/removing plants,
 * graves, pushable structures, per-tick occupancy refresh, and the lawn-mower breach
 * check at the left edge of the lawn.
 */
class SessionBoard {

    private final GameSession session;

    SessionBoard(GameSession session) {
        this.session = session;
    }

    private Environment environment() {
        return session.getEnvironment();
    }

    void damageGrave(Cell cell, int damage) {
        if (cell == null || !(cell.getObstacle() instanceof Grave grave)) return;
        if (!grave.takeDamage(damage)) return;

        int row = cell.getRow();
        int col = cell.getCol();
        destroyGrave(row, col, grave);
    }

    void destroyGrave(int row, int col, Grave grave) {
        Cell cell = environment().getCell(row, col);
        if (cell == null || cell.getObstacle() != grave) return;

        cell.setObstacle(null);

        Position position = new Position(col, row);

        if (grave.getReward() == Grave.Reward.SUN) {
            session.getItems().add(new GroundSun(position, 50));
        } else if (grave.getReward() == Grave.Reward.PLANT_FOOD) {
            session.getItems().add(new GroundPlantFood(position));
        }
    }

    void clearDeadPlantsFromGrid() {
        Environment environment = environment();

        for (int r = 0; r < environment.getRows(); r++) {
            for (int c = 0; c < environment.getCols(); c++) {
                Cell cell = environment.getCell(r, c);

                if (cell.getPlant() != null && !cell.getPlant().isAlive()) {
                    Plant bottom = cell.getPlant().getBottom();
                    cell.setPlant(bottom != null && bottom.isAlive() ? bottom : null);
                }
            }
        }
    }

    void clearDeadStructuresFromGrid() {
        Environment environment = environment();

        for (int r = 0; r < environment.getRows(); r++) {
            for (int c = 0; c < environment.getCols(); c++) {
                Cell cell = environment.getCell(r, c);

                if (cell.getStructure() != null && !cell.getStructure().isAlive()) {
                    cell.setStructure(null);
                }
            }
        }
    }

    void refreshZombieOccupancy() {
        Environment environment = environment();

        for (int r = 0; r < environment.getRows(); r++) {
            for (int c = 0; c < environment.getCols(); c++) {
                environment.getCell(r, c).clearZombies();
            }
        }

        for (Zombie zombie : session.getZombies()) {
            if (!zombie.isAlive() || zombie.getPosition() == null) continue;

            int col = (int) Math.round(zombie.getPosition().x());
            int row = (int) Math.round(zombie.getPosition().y());

            if (row < 0 || row >= environment.getRows()
                    || col < 0 || col >= environment.getCols()) {
                continue;
            }

            Cell cell = environment.getCell(row, col);

            if (cell != null) {
                cell.addZombie(zombie);
            }
        }
    }

    void checkZombieBreaches() {
        boolean mowersEnabled = session.areLawnMowersEnabled();
        LawnMower[] lawnMowers = session.getLawnMowers();

        for (Zombie zombie : session.getZombies()) {
            if (!zombie.isAlive() || zombie.getPosition() == null) continue;
            if (zombie.getPosition().x() >= 0.0) continue;

            if (!mowersEnabled) {
                session.onZombieReachedEnd();
                return;
            }

            int row = (int) Math.round(zombie.getPosition().y());

            if (row < 0 || row >= lawnMowers.length) continue;

            LawnMower mower = lawnMowers[row];

            if (mower.getState() == LawnMower.MowerState.DEAD) {
                if (zombie.getPosition().x() < -0.8) {
                    session.onZombieReachedEnd();
                    return;
                }
            } else if (mower.getState() == LawnMower.MowerState.IDLE) {
                boolean survived = mower.killZombiesInRow(zombiesInRow(row));

                if (!survived) {
                    session.onZombieReachedEnd();
                    return;
                }
            } else {
                if (zombie.getPosition().x() < -2.0) {
                    session.onZombieReachedEnd();
                    return;
                }
            }
        }
    }

    private List<Zombie> zombiesInRow(int row) {
        List<Zombie> result = new ArrayList<>();

        for (Zombie zombie : session.getZombies()) {
            if (zombie.isAlive()
                    && zombie.getPosition() != null
                    && (int) Math.round(zombie.getPosition().y()) == row) {
                result.add(zombie);
            }
        }

        return result;
    }

    boolean isFrozenInIceBlock(Zombie zombie) {
        Environment environment = session.getEnvironment();

        if (zombie == null
                || environment == null
                || zombie.getPosition() == null) {
            return false;
        }

        int row = (int) Math.round(zombie.getPosition().y());
        int col = (int) Math.round(zombie.getPosition().x());

        Cell cell = environment.getCell(row, col);

        return cell != null
                && cell.getObstacle() instanceof IceBlock iceBlock
                && iceBlock.getFrozenZombie() == zombie;
    }

    boolean plantAt(int row, int col, Plant plant) {
        List<Plant> plants = session.getPlants();
        SessionEconomy economy = session.economy();

        Cell cell = environment().getCell(row, col);

        if (cell == null || plant == null) return false;

        if (session.isScorchedTile(row, col)) return false;

        boolean isHotPotato =
                plant.getName().equalsIgnoreCase("Hot Potato");

        boolean handlesIceBlock =
                isHotPotato && cell.getObstacle() instanceof IceBlock;

        boolean handlesGrave =
                plant.getName().equalsIgnoreCase("Grave Buster")
                        && cell.getObstacle() instanceof Grave;

        boolean handlesObstacle = handlesIceBlock || handlesGrave;

        if (cell.getObstacle() != null && !handlesObstacle) return false;

        if (cell.getTile() != null
                && cell.getTile().type() == TileType.Slippery) {
            return false;
        }

        if (isBlockedByZombossGlacier(col)) return false;

        boolean flooded = cell.getTile() != null
                && cell.getTile().type() == TileType.Water;

        if (plant.getName().equalsIgnoreCase("Sea-shroom") && !flooded) {
            return false;
        }

        Plant existing = cell.hasPlant() ? cell.getPlant() : null;

        /*
         * HOT POTATO + FROZEN PLANT
         *
         * The frozen plant is stored inside the IceBlock:
         *
         *     iceBlock.getFrozenPlant() == existing
         *
         * We must NOT replace the frozen plant in the Cell with Hot Potato.
         *
         * However, Hot Potato still needs to exist in session.getPlants(), because
         * PlantRenderer/normal plant processing uses that collection to render its
         * animation and to detect the Hot Potato explosion.
         *
         * Therefore:
         *
         *   1. Melt the IceBlock.
         *   2. Keep the original plant in the Cell.
         *   3. Put Hot Potato into the session plant list.
         *   4. Give Hot Potato the same position.
         *   5. Do NOT put Hot Potato into the Cell.
         *
         * This makes it behave exactly like a normally planted Hot Potato from the
         * renderer's point of view, while preserving the original frozen plant.
         */
        boolean hotPotatoMeltsFrozenPlant =
                isHotPotato
                        && existing != null
                        && cell.getObstacle() instanceof IceBlock iceBlock
                        && iceBlock.getFrozenPlant() == existing;

        if (hotPotatoMeltsFrozenPlant) {

            // Remove the ice immediately.
            FrostbiteFreezing.damageFrozenPlantIfInIce(
                    session,
                    existing,
                    IceBlock.BASE_HP,
                    true
            );

            /*
             * IMPORTANT:
             * Do not call cell.setPlant(plant).
             *
             * The frozen plant must remain the plant occupying this tile.
             * Hot Potato only exists logically/render-wise for its normal
             * animation/explosion lifecycle.
             */
            plant.setPosition(new Position(col, row));

            if (!plants.contains(plant)) {
                plants.add(plant);
            }

            economy.markPlantedAnyPlant();

            return true;
        }

        boolean lilySupport = existing != null
                && existing.getTags().contains(PlantTag.WATER)
                && existing.getTags().contains(PlantTag.STACK);

        boolean incomingIsShell =
                plant.getName().equalsIgnoreCase("Pumpkin");

        boolean existingIsShell =
                existing != null
                        && !lilySupport
                        && existing.getName().equalsIgnoreCase("Pumpkin");

        if (existing != null
                && !lilySupport
                && incomingIsShell
                && !existingIsShell) {

            plant.setBottom(existing);
            cell.setPlant(plant);
            plant.setPosition(new Position(col, row));
            plants.add(plant);

            economy.markPlantedAnyPlant();
            return true;
        }

        if (existing != null
                && !lilySupport
                && existing.getId() == plant.getId()
                && plant.getTags().contains(PlantTag.STACK)
                && plant.getMaxStackNumber() > 1) {

            if (!existing.addStack()) return false;

            plant.setAlive(false);
            economy.markPlantedAnyPlant();
            return true;
        }

        if (existingIsShell && !incomingIsShell) {
            if (existing.getBottom() != null
                    && existing.getBottom().isAlive()) {
                return false;
            }

            plant.setPosition(new Position(col, row));
            existing.setBottom(plant);

            int pumpkinIndex = plants.indexOf(existing);

            if (pumpkinIndex >= 0) {
                plants.add(pumpkinIndex, plant);
            } else {
                plants.add(plant);
            }

            economy.markPlantedAnyPlant();
            return true;
        }

        if (existing != null && !lilySupport) return false;

        if (flooded
                && !plant.getTags().contains(PlantTag.WATER)
                && !lilySupport) {
            return false;
        }

        if (lilySupport) {
            plant.setBottom(existing);
        }

        boolean destroysGrave =
                plant.getName().equalsIgnoreCase("Grave Buster")
                        && cell.getObstacle() instanceof Grave grave;

        cell.setPlant(plant);
        plant.setPosition(new Position(col, row));
        plants.add(plant);

        if (handlesIceBlock) {
            FrostbiteFreezing.damageIce(
                    cell,
                    IceBlock.BASE_HP,
                    true
            );
        }

        if (destroysGrave) {
            destroyGrave(row, col, (Grave) cell.getObstacle());
        }

        economy.markPlantedAnyPlant();

        boolean nightPlant = false;

        if (plant.getTags() != null) {
            for (PlantTag tag : plant.getTags()) {
                economy.recordPlantFamily(tag.getName().toLowerCase());
                economy.recordPlantFamily(tag.name().toLowerCase());
            }

            nightPlant = plant.getTags().contains(PlantTag.NIGHT)
                    || plant.getTags().contains(PlantTag.SHROOM);
        }

        if (!nightPlant) {
            economy.markUsedNonNightPlant();
        }

        if (plant.getTags() != null
                && plant.getTags().contains(PlantTag.EXPLOSIVE)) {

            QuestManager.updateProgress(
                    "USE_EXPLOSIVE_PLANTS",
                    1,
                    Collections.emptyMap()
            );
        }

        return true;
    }

    boolean isBlockedByZombossGlacier(int col) {
        model.match.boss.ZombossFight fight =
                session.getZombossFight();

        if (fight == null) return false;

        if (!(fight.getBehavior()
                instanceof model.match.boss.behavior.IceAgeZombossBehavior ice)) {
            return false;
        }

        return col >= ice.getBlockedColumnStart();
    }

    boolean removePlantAt(int row, int col) {
        Cell cell = environment().getCell(row, col);

        if (cell == null || !cell.hasPlant()) return false;

        Plant plant = cell.getPlant();
        Plant bottom = plant.getBottom();

        plant.setAlive(false);
        plant.setBottom(null);

        if (bottom != null && bottom.isAlive()) {
            cell.setPlant(bottom);
        } else {
            cell.setPlant(null);
        }

        session.getPlants().remove(plant);
        return true;
    }

    Plant digPlantAt(int row, int col) {
        Cell cell = environment().getCell(row, col);

        if (cell == null || !cell.hasPlant()) return null;

        Plant plant = cell.getPlant();
        Plant bottom = plant.getBottom();

        cell.setPlant(bottom != null && bottom.isAlive() ? bottom : null);
        session.getPlants().remove(plant);

        return plant;
    }

    Plant findPlantAt(int row, int col) {
        Cell cell = environment().getCell(row, col);

        return (cell != null && cell.hasPlant())
                ? cell.getPlant()
                : null;
    }

    List<PushableStructure> getPushableStructures() {
        Environment environment = session.getEnvironment();

        Set<PushableStructure> structures =
                Collections.newSetFromMap(new IdentityHashMap<>());

        if (environment == null) return new ArrayList<>();

        for (int r = 0; r < environment.getRows(); r++) {
            for (int c = 0; c < environment.getCols(); c++) {
                PushableStructure structure =
                        environment.getCell(r, c).getStructure();

                if (structure != null && structure.isAlive()) {
                    structures.add(structure);
                }
            }
        }

        return new ArrayList<>(structures);
    }

    void registerStructure(PushableStructure structure) {
        Environment environment = session.getEnvironment();

        if (structure == null
                || environment == null
                || structure.getPosition() == null) {
            return;
        }

        for (int r = 0; r < environment.getRows(); r++) {
            for (int c = 0; c < environment.getCols(); c++) {
                PushableStructure existing =
                        environment.getCell(r, c).getStructure();

                if (existing == structure) {
                    environment.getCell(r, c).setStructure(null);
                }
            }
        }

        if (!structure.isAlive()) return;

        int row = (int) Math.round(structure.getPosition().y());
        int col = (int) Math.round(structure.getPosition().x());

        Cell cell = environment.getCell(row, col);

        if (cell != null
                && (cell.getStructure() == null
                || cell.getStructure() == structure)) {
            cell.setStructure(structure);
        }
    }
}