package model.match.boss;

import java.util.List;

public enum ZombossChapter {

    EGYPT(1.0, "Egypt", "ZombieEgyptZomboss",
            "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_ZOMBOSS/ZOMBIE_EGYPT_ZOMBOSS.PAM",
            "stun_start", "stun_loop", "stun_end",
            List.of("die", "die_idle", "die_talk", "die_exit"),
            List.of("So the little sprouts dug their way into MY pyramid.",
                    "Ten thousand years I spent building this machine.",
                    "Let us see how your garden holds up against a god-king!")),

    ICE_AGE(1.0, "Frostbite Caves", "ZombieIceAgeZomboss",
            "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_ZOMBOSS/ZOMBIE_ICEAGE_ZOMBOSS.PAM",
            "reveal", "stun", "cover_up",
            List.of("die", "die_talk", "die_exit"),
            List.of("You followed me all the way into the deep freeze?",
                    "My glacier walls will hold. Yours will not.",
                    "Enjoy the cold, plants. You will be here a while.")),

    BEACH(1.05, "Big Wave Beach", "ZombieBeachZomboss",
            "768/FULL/ZOMBIE/ZOMBIE_BEACH_ZOMBOSS/ZOMBIE_BEACH_ZOMBOSS.PAM",
            "stun_start", "stun_loop", "stun_end",
            List.of("die", "die_talk", "die_exit"),
            List.of("Welcome to my beach. Mind the undertow.",
                    "This machine drinks the whole ocean if I ask it to.",
                    "Let us find out how well a garden swims!")),

    DARK_AGES(1.3, "Dark Ages", "ZombieDarkZomboss",
            "768/FULL/ZOMBIE/ZOMBIE_DARK_ZOMBOSS/ZOMBIE_DARK_ZOMBOSS.PAM",
            "stun_start", "stun_loop", "stun_end",
            List.of("die", "die_talk", "die_exit"),
            List.of("A dragon. I built myself a DRAGON.",
                    "Every grave on this lawn answers to me tonight.",
                    "Burn, little garden. Burn brightly.")),

    PIRATES(1.0, "Pirates", "ZombieEgyptZomboss",
            "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_ZOMBOSS/ZOMBIE_EGYPT_ZOMBOSS.PAM",
            "stun_start", "stun_loop", "stun_end",
            List.of("die", "die_idle", "die_talk", "die_exit"),
            List.of("So the little sprouts dug their way into MY pyramid.",
                    "Ten thousand years I spent building this machine.",
                    "Let us see how your garden holds up against a god-king!")),

    // Placeholder assets (reusing the Dark Ages Zomboss, the same way Pirates
    // reuses Egypt's) until dedicated Future Zomboss art/PAM clips exist.
    FUTURE(1.35, "Future", "ZombieDarkZomboss",
            "768/FULL/ZOMBIE/ZOMBIE_DARK_ZOMBOSS/ZOMBIE_DARK_ZOMBOSS.PAM",
            "stun_start", "stun_loop", "stun_end",
            List.of("die", "die_talk", "die_exit"),
            List.of("You really thought you could out-tech ME?",
                    "Every gadget on this lawn answers to my signal now.",
                    "Let us see your garden survive the future!"));

    public static final String NPC_PAM = "768/FULL/NPC/ZOMBOSS/ZOMBOSS.PAM";
    public static final String NPC_ENTER_CLIP = "zomboss_enter";
    public static final String NPC_IDLE_CLIP = "zomboss_idle";
    public static final String NPC_TALK_CLIP = "zomboss_talk";
    public static final String NPC_SHOUT_CLIP = "zomboss_shout";
    public static final String NPC_EXIT_CLIP = "zomboss_exit";

    public static final String INTRO_CLIP = "intro";
    public static final String IDLE_CLIP = "idle";

    private final double spawnPacing;
    private final String seasonName;
    private final String alias;
    private final String bossPam;
    private final String stunStartClip;
    private final String stunLoopClip;
    private final String stunEndClip;
    private final List<String> deathClips;
    private final List<String> dialogue;

    ZombossChapter(double spawnPacing, String seasonName, String alias, String bossPam,
                   String stunStartClip, String stunLoopClip, String stunEndClip,
                   List<String> deathClips, List<String> dialogue) {
        this.spawnPacing = spawnPacing;
        this.seasonName = seasonName;
        this.alias = alias;
        this.bossPam = bossPam;
        this.stunStartClip = stunStartClip;
        this.stunLoopClip = stunLoopClip;
        this.stunEndClip = stunEndClip;
        this.deathClips = deathClips;
        this.dialogue = dialogue;
    }

    public double getSpawnPacing() { return spawnPacing; }

    public String getSeasonName() { return seasonName; }

    public String getAlias() { return alias; }

    public String getBossPam() { return bossPam; }

    public String getStunStartClip() { return stunStartClip; }

    public String getStunLoopClip() { return stunLoopClip; }

    public String getStunEndClip() { return stunEndClip; }

    public List<String> getDeathClips() { return deathClips; }

    public List<String> getDialogue() { return dialogue; }

    public static ZombossChapter fromSeason(String name) {
        if (name == null) return null;
        String key = name.trim().toLowerCase().replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "egypt", "ancient_egypt" -> EGYPT;
            case "cave", "ice_age", "iceage", "frostbite_caves" -> ICE_AGE;
            case "beach", "big_wave_beach" -> BEACH;
            case "darkage", "dark_age", "dark_ages" -> DARK_AGES;
            case "pirate", "pirates" -> PIRATES;
            case "future" -> FUTURE;
            default -> null;
        };
    }
}