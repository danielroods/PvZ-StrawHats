package model.match.endless;

import java.util.Locale;


public enum EndlessChapter {

    EGYPT("egypt", "Egypt", "Ancient Egypt"),
    FROSTBITE_CAVES("frostbite_caves", "Frostbite Caves", "Frostbite Caves"),
    BIG_WAVE_BEACH("big_wave_beach", "Big Wave Beach", "Big Wave Beach"),
    DARK_AGES("dark_ages", "Dark Ages", "Dark Ages"),
    PIRATES("pirates", "Pirates", "Pirates"),
    FUTURE("future", "Future", "Future");

    
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
