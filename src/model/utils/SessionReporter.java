package model.utils;

import model.collections.Item;
import model.collections.item.GroundItem;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.match.main.levels.Level;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.LawnMower;
import model.pitches.Tile;
import model.pitches.TileType;
import service.GameClock;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the text-adventure views of a live match - the ASCII lawn, the loadout/plant
 * status listing, a single tile's contents, and the zombie roster - for the command layer.
 */
class SessionReporter {

    private final GameSession session;

    SessionReporter(GameSession session) {
        this.session = session;
    }

    String renderMap() {
        Environment environment = session.getEnvironment();
        Level level = session.getLevel();
        LawnMower[] lawnMowers = session.getLawnMowers();
        StringBuilder sb = new StringBuilder();
        if (level != null) {
            sb.append("Stage: ").append(level.getName())
                    .append(" | Game mode: ").append(level.getGameMode()).append("\n");
        }
        sb.append("Wave: ").append(session.getWavesSpawnedCount()).append("/").append(session.getTotalWaveCount())
                .append(" | Sun: ").append(session.getSunCount())
                .append(" | Plant food: ").append(session.getPlantFoodCount());
        if (!session.allWavesSpawned()) {
            sb.append(" | Next wave: ")
                    .append(String.format("%.1fs", session.getSecondsUntilNextWave()));
        }
        sb.append("\n");
        for (int r = 0; r < environment.getRows(); r++) {
            sb.append(r + 1);
            if (!session.isZombieBreachesEnabled()) {
                sb.append(" [B] ");
            } else {
                sb.append(lawnMowers[r].isUsed() ? " [ ] " : " [M] ");
            }
            for (int c = 0; c < environment.getCols(); c++) {
                sb.append(mapSymbolFor(environment.getCell(r, c)));
            }
            sb.append("\n");
        }
        sb.append("(M=lawn mower, B=brain mode, P=plant, Z=zombie, E=zombie eating a plant, X=obstacle, ~=ice, W=water, .=empty)");
        List<GroundItem> visibleItems = session.getItems().stream()
                .filter(GroundItem.class::isInstance)
                .map(GroundItem.class::cast)
                .filter(Item::isAlive)
                .toList();
        if (!visibleItems.isEmpty()) {
            sb.append("\nGround items:");
            for (GroundItem item : visibleItems) {
                Position position = item.getPosition();
                if (position == null) continue;
                sb.append("\n  ").append(item.getItemType().name().toLowerCase())
                        .append(" at (").append((int) Math.round(position.x()) + 1)
                        .append(", ").append((int) Math.round(position.y()) + 1).append(")");
                if (item instanceof GroundSun sun && sun.isFalling()) sb.append(" [falling]");
            }
        }
        return sb.toString().trim();
    }

    private char mapSymbolFor(Cell cell) {
        if (cell == null) return '?';
        boolean hasZombie = !cell.getZombies().isEmpty();
        boolean hasPlant = cell.hasPlant();
        if (hasZombie && hasPlant) return 'E';
        if (hasZombie) return 'Z';
        if (hasPlant) return 'P';
        if (cell.getObstacle() != null) return 'X';
        if (cell.getTile() != null && cell.getTile().type() == TileType.Slippery) return '~';
        if (cell.getTile() != null && cell.getTile().type() == TileType.Water) return 'W';
        return '.';
    }

    String renderPlantsStatus() {
        List<Plant> plants = session.getPlants();
        StringBuilder sb = new StringBuilder("Loadout planting status:");
        List<String> selected = controller.menus.match.BeforeMenu.selectedPlants;
        if (selected.isEmpty()) {
            sb.append("\n  no plants selected");
        }
        for (String selectedName : selected) {
            PlantJsonParser.PlantConfig config = PlantFactory.getBlueprints().values().stream()
                    .filter(candidate -> candidate.name.equalsIgnoreCase(selectedName))
                    .findFirst().orElse(null);
            if (config == null) continue;
            double cooldown = session.getPlantCooldown(config.id);
            sb.append("\n  ").append(config.name)
                    .append(" | cost: ").append(config.cost)
                    .append(" | ").append(GameClock.isZero(cooldown)
                            ? "ready" : String.format("recharging: %.1fs", cooldown));
        }
        sb.append("\nPlants on the field:");
        if (plants.isEmpty()) {
            sb.append("\n  none");
            return sb.toString();
        }
        for (Plant plant : plants) {
            Position position = plant.getPosition();
            sb.append("\n  ").append(plant.getName())
                    .append(" | hp: ").append(plant.getHP())
                    .append(" | level: ").append(plant.getLevel());
            if (position != null) {
                sb.append(" | position: (").append((int) position.x() + 1)
                        .append(", ").append((int) position.y() + 1).append(")");
            }
        }
        return sb.toString();
    }

    String renderTileStatus(int row, int col) {
        Environment environment = session.getEnvironment();
        Level level = session.getLevel();
        Cell cell = environment.getCell(row, col);
        StringBuilder sb = new StringBuilder();
        sb.append("tile (").append(col + 1).append(", ").append(row + 1).append("): ");

        if (cell == null) {
            sb.append("out of bounds");
            return sb.toString();
        }

        List<String> parts = new ArrayList<>();

        if (cell.hasPlant()) {
            Plant plant = cell.getPlant();
            parts.add("plant=" + plant.getName() + " hp=" + plant.getHP() + " level=" + plant.getLevel());
        } else {
            parts.add("no plant");
        }

        if (cell.getObstacle() != null) {
            parts.add("obstacle=" + cell.getObstacle().getName());
        }

        if (cell.getTile() != null) {
            Tile tile = cell.getTile();
            String terrain = tile.type().toString();
            if (tile.slipperyDirection() != null) {
                terrain += " (" + tile.slipperyDirection() + ")";
            }
            parts.add("terrain=" + terrain);
        }

        if (cell.getStructure() != null) {
            parts.add("structure=" + cell.getStructure().getClass().getSimpleName());
        }

        if (level != null && level.getSeason() != null && level.getSeason().hasTide()
                && col >= environment.getCols() - level.getCurrentTideColumn()) {
            parts.add("flooded (tide)");
        }

        if (!cell.getZombies().isEmpty()) {
            parts.add("zombies=" + cell.getZombies().size());
        }

        sb.append(String.join(", ", parts));
        return sb.toString();
    }

    String renderZombiesInfo() {
        List<Zombie> zombies = session.getZombies();
        if (zombies.isEmpty()) return "no zombies on the field";
        StringBuilder sb = new StringBuilder();
        for (Zombie zombie : zombies) {
            Position position = zombie.getPosition();
            sb.append(zombie.getName())
                    .append(" | hp: ").append(zombie.getHp())
                    .append("/").append(zombie.getMaxHp());
            if (position != null) {
                sb.append(" | position: (").append(String.format("%.2f", position.x() + 1))
                        .append(", ").append((int) Math.round(position.y()) + 1).append(")");
            }
            if (zombie.getArmor() != null && zombie.getArmor().getHP() > 0) {
                sb.append(" | armor: ").append(zombie.getArmor().getHP());
            }
            sb.append(" | state: ").append(zombie.getZombieState())
                    .append("\n");
        }
        return sb.toString().trim();
    }
}
