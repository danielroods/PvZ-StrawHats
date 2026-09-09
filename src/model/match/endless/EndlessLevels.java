package model.match.endless;

import model.collections.zombie.ZombieFactory;
import model.match.main.levels.Level;
import model.match.main.levels.special_levels.BossLevel;
import model.match.main.season.Season;
import model.match.main.season.SeasonFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;


public final class EndlessLevels {

    private static final int ENDLESS_INITIAL_SUN = 250;
    private static final int DEFAULT_ROWS = 5;
    private static final int DEFAULT_COLS = 9;
    private static final String COSMETIC_FLAG_ALIAS = "ZombieFlag";

    private EndlessLevels() {
    }

    public static EndlessLevel forChapter(EndlessChapter chapter, List<Level> chapterLevels) {
        if (chapter == null) return null;
        List<Level> levels = chapterLevels == null ? List.of() : chapterLevels;

        int rows = DEFAULT_ROWS;
        int cols = DEFAULT_COLS;
        List<String> plants = new ArrayList<>();
        List<String> roster = new ArrayList<>();
        for (Level level : levels) {
            if (level == null) continue;
            rows = level.getRows();
            cols = level.getCols();
            if (level.getZombiePool() != null) roster.addAll(level.getZombiePool());
            
            
            
            
            if (level.getAvailablePlants() != null && !(level instanceof BossLevel)) {
                plants.addAll(level.getAvailablePlants());
            }
        }

        EndlessLevel endless = new EndlessLevel(chapter, playableRoster(chapter, roster),
                cols, new Random());
        endless.setSeason(seasonOf(chapter, levels));
        endless.setRows(rows);
        endless.setCols(cols);
        endless.setInitialSun(ENDLESS_INITIAL_SUN);
        endless.setAvailablePlants(new ArrayList<>(new LinkedHashSet<>(plants)));
        endless.setForcedPlants(new ArrayList<>());
        return endless;
    }

    public static List<Level> levelsOf(EndlessChapter chapter, List<Level> allLevels) {
        List<Level> chapterLevels = new ArrayList<>();
        if (chapter == null || allLevels == null) return chapterLevels;
        for (Level level : allLevels) {
            if (level == null || level.getSeason() == null) continue;
            if (chapter == EndlessChapter.forSeason(level.getSeason().getName())) {
                chapterLevels.add(level);
            }
        }
        return chapterLevels;
    }

    
    private static List<String> playableRoster(EndlessChapter chapter, List<String> roster) {
        List<String> playable = new ArrayList<>(new LinkedHashSet<>(roster));
        playable.removeIf(alias -> alias == null
                || alias.isBlank()
                || COSMETIC_FLAG_ALIAS.equals(alias)
                || !isKnown(alias)
                || (chapter == EndlessChapter.FROSTBITE_CAVES
                        && ZombieFactory.shouldSpawnFrosted(alias)));
        return playable;
    }

    private static boolean isKnown(String alias) {
        try {
            ZombieFactory.create(alias, 0, 0);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Season seasonOf(EndlessChapter chapter, List<Level> chapterLevels) {
        for (Level level : chapterLevels) {
            if (level != null && level.getSeason() != null) {
                return SeasonFactory.create(level.getSeason().getName());
            }
        }
        return SeasonFactory.create(chapter.seasonName());
    }
}
