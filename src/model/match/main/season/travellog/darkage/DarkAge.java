package model.match.main.season.travellog.darkage;

import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.main.season.Season;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.obstacles.Grave;
import model.utils.GameSession;

import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DarkAge extends Season {
    private static final int INITIAL_GRAVE_COUNT = 3;
    private static final int GRAVES_ADDED_PER_WAVE = 2;
    private static final int MAX_GRAVES = 14;
    private static final double NECROMANCY_CHANCE = 0.50;
    private static final double FRONT_GRAVE_WEIGHT = 0.20;  // columns 1-4
    private static final double BACK_GRAVE_WEIGHT = 1.00;   // columns 5-9
    private static final Random RANDOM = new Random();

    public DarkAge() {
        super("Dark Ages");
    }

    @Override
    public boolean isNight() { return true; }

    @Override
    public void placeSeasonObstacles(GameSession session) {
        if (session == null || session.getEnvironment() == null) return;
        Environment env = session.getEnvironment();

        placeGraves(session, INITIAL_GRAVE_COUNT);
    }

    @Override
    public void onWaveStart(GameSession session, int waveIndex) {
        if (session == null || session.getEnvironment() == null) return;

        if (waveIndex > 0) {
            int targetTotal = Math.min(MAX_GRAVES, INITIAL_GRAVE_COUNT + waveIndex * GRAVES_ADDED_PER_WAVE);
            placeGraves(session, targetTotal - countGraves(session));
        }

        necromancy(session, waveIndex);
    }

    private static void placeGraves(GameSession session, int count) {
        if (session == null || session.getEnvironment() == null || count <= 0) return;
        Environment env = session.getEnvironment();

        List<Cell> candidates = new ArrayList<>();
        for (int row = 0; row < env.getRows(); row++) {
            for (int col = 0; col < env.getCols(); col++) {
                Cell cell = env.getCell(row, col);
                if (cell != null && cell.getObstacle() == null && cell.getStructure() == null && !cell.hasPlant()) {
                    candidates.add(cell);
                }
            }
        }

        int placed = 0;
        while (placed < count && countGraves(session) < MAX_GRAVES && !candidates.isEmpty()) {
            double totalWeight = 0.0;
            for (Cell cell : candidates) {
                int col = cell.getCol();
                totalWeight += (col >= 5 && col <= 9) ? BACK_GRAVE_WEIGHT : FRONT_GRAVE_WEIGHT;
            }

            double roll = RANDOM.nextDouble() * totalWeight;
            Cell selected = candidates.get(candidates.size() - 1);
            for (Cell cell : candidates) {
                int col = cell.getCol();
                roll -= (col >= 5 && col <= 9) ? BACK_GRAVE_WEIGHT : FRONT_GRAVE_WEIGHT;
                if (roll <= 0.0) {
                    selected = cell;
                    break;
                }
            }

            selected.setObstacle(new Grave(Grave.randomDarkAgeReward()));
            candidates.remove(selected);
            placed++;
        }
    }

    private static int countGraves(GameSession session) {
        Environment env = session.getEnvironment();
        int count = 0;
        for (int row = 0; row < env.getRows(); row++) {
            for (int col = 0; col < env.getCols(); col++) {
                Cell cell = env.getCell(row, col);
                if (cell != null && cell.getObstacle() instanceof Grave) count++;
            }
        }
        return count;
    }

    public static void necromancy(GameSession session, int waveIndex) {
        if (session == null || session.getEnvironment() == null) return;
        Environment env = session.getEnvironment();

        List<Cell> graves = new ArrayList<>();
        for (int row = 0; row < env.getRows(); row++) {
            for (int col = 0; col < env.getCols(); col++) {
                Cell cell = env.getCell(row, col);
                if (cell != null
                        && col >= 5 && col <= 9
                        && cell.getObstacle() instanceof Grave
                        && !cell.hasPlant()
                        && cell.getStructure() == null) {
                    graves.add(cell);
                }
            }
        }

        Collections.shuffle(graves, RANDOM);
        int raised = 0;
        for (Cell cell : graves) {
            if (RANDOM.nextDouble() >= NECROMANCY_CHANCE) continue;

            int row = cell.getRow();
            int col = cell.getCol();

            Zombie risen = ZombieFactory.create("ZombieDefault", row, col);
            risen.setPosition(new Position(col, row));
            risen.setFromNecromancy(true);
            session.spawnZombieForCurrentWave(risen);
            raised++;
            view.GeneralPrinter.print("Dark Ages necromancy spawned " + risen.getName()
                    + " at grave cell (row=" + row + ", col=" + col + ").");
        }

        if (raised > 0) {
            view.GeneralPrinter.print("Dark Ages necromancy raised " + raised
                    + " zombie(s) from graves on wave " + (waveIndex + 1) + ".");
        }
    }
}