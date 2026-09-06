package controller;

import controller.assets.AssetPaths;
import model.match.boss.ZombossChapter;
import model.match.main.levels.Level;
import model.match.main.levels.special_levels.BossLevel;
import model.user_data.UserState;
import model.utils.LevelLoader;
import model.utils.LevelProgression;

import java.util.ArrayList;
import java.util.List;

/**
 * Derives the trophy/key shelf shown in {@code TrophiesMenu} from data that already
 * exists elsewhere - there is no new persisted state here. A chapter's trophy and key
 * are both considered "earned" once that chapter's last level (its {@link BossLevel},
 * i.e. its Zomboss fight) is completed, the same way the real game awards one trophy
 * per world for beating that world's Zombot.
 */
public class TrophyManager {

    public record TrophyEntry(String chapterName, String trophyImagePath, String keyImagePath,
                               boolean earned) {}

    // Ordered the same way the chapters unlock in Adventure Mode.
    private static final String[] CHAPTER_ORDER = {
            "Egypt", "Pirates", "Big Wave Beach", "Frostbite Caves", "Dark Ages", "Future"
    };

    public List<TrophyEntry> getTrophyShelf(UserState state) {
        List<Level> levels;
        try {
            levels = LevelLoader.loadLevels();
        } catch (Exception e) {
            levels = new ArrayList<>();
        }
        List<Level> sorted = LevelProgression.sorted(levels);

        List<TrophyEntry> shelf = new ArrayList<>();
        for (String chapterName : CHAPTER_ORDER) {
            boolean earned = isChapterTrophyEarned(sorted, state, chapterName);
            shelf.add(new TrophyEntry(chapterName,
                    trophyImagePathFor(chapterName), keyImagePathFor(chapterName), earned));
        }
        return shelf;
    }

    private boolean isChapterTrophyEarned(List<Level> sortedLevels, UserState state, String chapterName) {
        for (Level level : sortedLevels) {
            if (!(level instanceof BossLevel)) continue;
            if (level.getSeason() == null || !chapterName.equalsIgnoreCase(level.getSeason().getName())) continue;
            return LevelProgression.isCompleted(sortedLevels, state.lastLevel, level);
        }
        return false;
    }

    private String trophyImagePathFor(String chapterName) {
        ZombossChapter chapter = ZombossChapter.fromSeason(chapterName);
        if (chapter == null) return AssetPaths.TROPHY_LOCKED_SILHOUETTE;
        return switch (chapter) {
            case EGYPT -> AssetPaths.TROPHY_EGYPT;
            case ICE_AGE -> AssetPaths.TROPHY_FROSTBITE_CAVES;
            case BEACH -> AssetPaths.TROPHY_BIG_WAVE_BEACH;
            case DARK_AGES -> AssetPaths.TROPHY_DARK_AGES;
            case PIRATES -> AssetPaths.TROPHY_PIRATE_SEAS;
            case FUTURE -> AssetPaths.TROPHY_FAR_FUTURE;
        };
    }

    private String keyImagePathFor(String chapterName) {
        ZombossChapter chapter = ZombossChapter.fromSeason(chapterName);
        if (chapter == null) return AssetPaths.KEY_LOCKED_SILHOUETTE;
        return switch (chapter) {
            case EGYPT -> AssetPaths.KEY_EGYPT;
            case ICE_AGE -> AssetPaths.KEY_FROSTBITE_CAVES;
            case BEACH -> AssetPaths.KEY_BIG_WAVE_BEACH;
            case DARK_AGES -> AssetPaths.KEY_DARK_AGES;
            case PIRATES -> AssetPaths.KEY_PIRATE_SEAS;
            case FUTURE -> AssetPaths.KEY_FAR_FUTURE;
        };
    }
}
