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


public class TrophyManager {

    public record TrophyEntry(String chapterName, String trophyImagePath,
                               boolean earned, String trophyTitle, String trophyDescription) {}

    
    
    
    
    
    private record TrophyFlavor(String title, String description) {}

    private static final java.util.Map<String, TrophyFlavor> TROPHY_FLAVOR = java.util.Map.of(
            "Egypt", new TrophyFlavor("Golden Zomboss Sarcophagus",
                    "Defeated the pharaoh's finest reanimation engine. It came with a curse, a "
                            + "ten-thousand-year warranty, and surprisingly good cup holders. All three "
                            + "are now yours to ignore."),
            "Pirates", new TrophyFlavor("Barnacled Boss-Wreck Trophy",
                    "Sunk a mechanical menace beneath the waves along with its captain's dignity. "
                            + "Smells faintly of brine, motor oil, and unearned confidence."),
            "Big Wave Beach", new TrophyFlavor("Undertow Zomboss Anchor",
                    "This machine tried to drink the entire ocean to beat you. It did not. You are "
                            + "now legally allowed to say you out-swam a robot."),
            "Frostbite Caves", new TrophyFlavor("Frozen Zomboss Popsicle",
                    "Once a towering ice-age terror, now a chilly little trinket that sweats "
                            + "nervously whenever the heater turns on."),
            "Dark Ages", new TrophyFlavor("Dragon-Bolted Zomboss Relic",
                    "Yes, it built itself a dragon. No, that did not go well for it. Every grave in "
                            + "the graveyard signed a strongly worded apology afterward."),
            "Future", new TrophyFlavor("Short-Circuited Zomboss Core",
                    "Out-teched a machine from the actual future using plants. The future, it turns "
                            + "out, still loses to a well-placed cabbage.")
    );

    
    private static final String[] CHAPTER_ORDER = {
            "Egypt", "Big Wave Beach", "Frostbite Caves", "Dark Ages", "Pirates", "Future"
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
            TrophyFlavor flavor = TROPHY_FLAVOR.getOrDefault(chapterName,
                    new TrophyFlavor(chapterName + " Trophy", "You defeated Dr. Zomboss here."));

            shelf.add(new TrophyEntry(chapterName, trophyImagePathFor(chapterName), earned,
                    flavor.title(), flavor.description()));
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


}
