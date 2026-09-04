package model.match.endless;

import java.util.Locale;

/**
 * The four chapters that have a Lottery (endless) node, and the stable identity each
 * one uses everywhere its record is stored: the season it plays in, the key its high
 * score is saved under, and the synthetic level id its match runs with.
 */
public enum EndlessChapter {

    EGYPT("egypt", "Egypt", "Ancient Egypt"),
    FROSTBITE_CAVES("frostbite_caves", "Frostbite Caves", "Frostbite Caves"),
    BIG_WAVE_BEACH("big_wave_beach", "Big Wave Beach", "Big Wave Beach"),
    DARK_AGES("dark_ages", "Dark Ages", "Dark Ages"),
    PIRATES("pirates", "Pirates", "Pirates"),
    FUTURE("future", "Future", "Future");

    /**
     * Lottery levels are synthetic - they are never listed in Levels.json - so they take
     * ids below every authored level. Negative ids keep them out of
     * LevelProgression/UserState.lastLevel, which only ever move forward through the
     * authored ladder.
     */
    private static final int LEVEL_ID_BASE = -1_000_000;

    private final String key;
    private final String seasonName;
    private final String chapterTitle;

    EndlessChapter(String key, String seasonName, String chapterTitle) {
        this.key = key;
        this.seasonName = seasonName;
        this.chapterTitle = chapterTitle;
    }

    public String key() {
        return key;
    }

    public String seasonName() {
        return seasonName;
    }

    public String chapterTitle() {
        return chapterTitle;
    }

    public String levelName() {
        return chapterTitle + " Lottery";
    }

    public int levelId() {
        return LEVEL_ID_BASE - ordinal();
    }

    public static EndlessChapter forSeason(String seasonName) {
        if (seasonName == null) return null;
        String normalised = normalise(seasonName);
        for (EndlessChapter chapter : values()) {
            if (normalise(chapter.seasonName).equals(normalised)
                    || chapter.key.equals(normalised)
                    || normalise(chapter.chapterTitle).equals(normalised)) {
                return chapter;
            }
        }
        return switch (normalised) {
            case "cave" -> FROSTBITE_CAVES;
            case "beach" -> BIG_WAVE_BEACH;
            case "darkage" -> DARK_AGES;
            case "pirate" -> PIRATES;
            default -> null;
        };
    }

    private static String normalise(String value) {
        return value.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_").trim();
    }
}
