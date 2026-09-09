package model.collections.animations;

import java.util.Map;

public class ZombieAnimationRegistry {

    private static final Map<String, String> VERIFIED = Map.ofEntries(
            Map.entry("ZombieDefault", "ZOMBIE_TUTORIAL"),
            Map.entry("ZombieFlag", "ZOMBIE_TUTORIAL"),
            Map.entry("ZombieImp", "ZOMBIE_TUTORIAL_IMP"),
            Map.entry("ZombieNewspaper", "ZOMBIE_MODERN_NEWSPAPER"),
            Map.entry("ZombieGargantuar", "GARGANTUAR"),
            Map.entry("ZombieRa", "ZOMBIE_EGYPT_RA"),
            Map.entry("ZombieExplorer", "ZOMBIE_EXPLORER"),
            Map.entry("ZombieTombRaiser", "ZOMBIE_EGYPT_TOMBRAISER"),
            Map.entry("ZombieIceAgeDodo", "ZOMBIE_ICEAGE_DODORIDER"),
            Map.entry("ZombieIceAgeHunter", "ZOMBIE_ICEAGE_HUNTER"),
            Map.entry("ZombieIceAgeTroglobite", "ZOMBIE_ICEAGE_TROGLOBITE"),
            Map.entry("ZombieBeachFisherman", "ZOMBIE_BEACH_FISHERMAN"),
            Map.entry("ZombieBeachOctopus", "ZOMBIE_BEACH_OCTOPUS"),
            Map.entry("ZombieBeachSnorkel", "ZOMBIE_BEACH_SNORKELER"),
            Map.entry("ZombieDarkKing", "ZOMBIE_DARK_KING"),
            Map.entry("ZombieDarkImpDragon", "ZOMBIE_DARK_IMP_DRAGON"),
            Map.entry("ZombieModernAllStar", "ZOMBIE_MODERN_ALLSTAR"),
            Map.entry("ZombieLostCityJane", "ZOMBIE_LOSTCITY_JANE"),
            Map.entry("ZombieCrystalSkull", "ZOMBIE_LOSTCITY_CRYSTALSKULL"),
            Map.entry("ZombieProspector", "ZOMBIE_PROSPECTOR"),
            Map.entry("ZombiePiano", "ZOMBIE_PIANO"),
            Map.entry("ZombieArcade", "ZOMBIE_80S_ARCADE"),
            Map.entry("ZombieBarrelRoller", "ZOMBIE_PIRATE_BARREL_PUSHER"),
            Map.entry("ZombiePirateBasic", "ZOMBIE_PIRATE_BASIC"),
            Map.entry("ZombiePirateBasicFlag", "ZOMBIE_PIRATE_BASIC_FLAG"),
            Map.entry("ZombiePirateImp", "ZOMBIE_PIRATE_IMP"),
            Map.entry("ZombiePirateSeagull", "ZOMBIE_PIRATE_SEAGULL"),
            Map.entry("ZombiePiratePelican", "ZOMBIE_PIRATE_PELICAN"),
            Map.entry("ZombiePirateGargantuar", "PIRATE_GARGANTUAR"),
            Map.entry("ZombiePirateCaptain", "ZOMBIE_PIRATE_CAPTAIN"),
            
            
            
            Map.entry("ZombieSwashbuckler", "ZOMBIE_SWASHBUCKLER"),

            Map.entry("ZombieFutureBasic", "ZOMBIE_FUTURE_BASIC"),
            Map.entry("ZombieFutureFlag", "ZOMBIE_FUTURE_FLAG"),
            Map.entry("ZombieFutureProtector", "ZOMBIE_FUTURE_PROTECTOR"),
            Map.entry("ZombieFutureJetpack", "ZOMBIE_FUTURE_JETPACK"),
            Map.entry("ZombieFutureJetpackBasic", "ZOMBIE_FUTURE_JETPACK_BASIC"),
            Map.entry("ZombieFutureJetpackVeteran", "ZOMBIE_FUTURE_JETPACK_VETERAN"),
            Map.entry("ZombieMechCone", "ZOMBIE_MECH_CONE"),
            Map.entry("ZombieMechFootball", "ZOMBIE_MECH_FOOTBALL"),
            Map.entry("ZombieFutureGargantuar", "GARGANTUAR"),
            Map.entry("ZombieFutureImp", "GARGANTUAR_IMP"),
            Map.entry("ZombiePeashooter", "ZOMBIE_TUTORIAL"),
            Map.entry("ZombieGatlingPea", "ZOMBIE_TUTORIAL"),
            Map.entry("ZombieWallnut", "ZOMBIE_TUTORIAL"),
            Map.entry("ZombieTallnut", "ZOMBIE_TUTORIAL"),
            Map.entry("ZombieJalapeno", "ZOMBIE_TUTORIAL"),
            Map.entry("ZombieSquash", "ZOMBIE_TUTORIAL"),

            Map.entry("ZombieArmor1", "ZOMBIE_TUTORIAL"),
            Map.entry("ZombieArmor2", "ZOMBIE_TUTORIAL"),
            Map.entry("ZombieArmor4", "ZOMBIE_TUTORIAL"),

            Map.entry("ZombieDarkArmor3", "ZOMBIE_DARK_BASIC"),
            Map.entry("ZombieDarkJuggler", "ZOMBIE_DARK_JESTER"),

            Map.entry("ZombieEgyptZomboss", "ZOMBIE_EGYPT_ZOMBOSS"),
            Map.entry("ZombieIceAgeZomboss", "ZOMBIE_ICEAGE_ZOMBOSS"),
            Map.entry("ZombieBeachZomboss", "ZOMBIE_BEACH_ZOMBOSS"),
            Map.entry("ZombieDarkZomboss", "ZOMBIE_DARK_ZOMBOSS")
    );

    private static final Map<String, String> BEST_GUESS = Map.of(
            "ZombieWizard", "ZOMBIE_DARK_WIZARD"
    );

    private static final java.util.Set<String> NOT_FOUND = java.util.Set.of();

    public static AnimationJsonParser.AnimationConfig resolve(String zombieAlias) {
        if (zombieAlias == null || NOT_FOUND.contains(zombieAlias)) return null;

        String animationName = VERIFIED.get(zombieAlias);
        if (animationName == null) animationName = BEST_GUESS.get(zombieAlias);
        if (animationName == null) return null;

        return AnimationFactory.get(animationName);
    }

    public static String pathFor(String zombieAlias) {
        if ("ZombieBarrelRoller".equalsIgnoreCase(zombieAlias)) {
            return "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER/ZOMBIE_PIRATE_BARREL_PUSHER.PAM";
        }
        if ("ZombieSwashbuckler".equalsIgnoreCase(zombieAlias)) {
            return "768/FULL/ZOMBIE/ZOMBIE_SWASHBUCKLER/ZOMBIE_SWASHBUCKLER.PAM";
        }
        AnimationJsonParser.AnimationConfig config = resolve(zombieAlias);
        return config == null ? null : config.path;
    }

    public static String pathFor(String zombieAlias, String season) {
        if (zombieAlias == null) return null;

        if (season != null && !season.isBlank()) {
            String s = season.toLowerCase().trim().replace("-", "_").replace(" ", "_");

            boolean isEgypt = s.contains("egypt");
            boolean isBeach = s.contains("beach");
            boolean isIce = s.contains("ice") || s.contains("cave") || s.contains("frostbite");
            boolean isDark = s.contains("dark");
            boolean isPirate = s.contains("pirate");
            boolean isFuture = s.contains("future");

            if ("ZombieGargantuar".equalsIgnoreCase(zombieAlias)) {
                if (isEgypt) return "768/INITIAL/ZOMBIE/EGYPT_GARGANTUAR/EGYPT_GARGANTUAR.PAM";
                if (isBeach) return "768/FULL/ZOMBIE/BEACH_GARGANTUAR/BEACH_GARGANTUAR.PAM";
                if (isIce)   return "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_GARGANTUAR/ZOMBIE_ICEAGE_GARGANTUAR.PAM";
                if (isDark)  return "768/FULL/ZOMBIE/DARK_GARGANTUAR/DARK_GARGANTUAR.PAM";
                if (isPirate) return "768/FULL/ZOMBIE/PIRATE_GARGANTUAR/PIRATE_GARGANTUAR.PAM";
                if (isFuture) return "768/FULL/ZOMBIE/GARGANTUAR/GARGANTUAR.PAM";
            }

            if ("ZombieDefault".equalsIgnoreCase(zombieAlias)
                    || "ZombieArmor1".equalsIgnoreCase(zombieAlias)
                    || "ZombieArmor2".equalsIgnoreCase(zombieAlias)
                    || "ZombieArmor4".equalsIgnoreCase(zombieAlias)) {
                if (isEgypt) return "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM";
                if (isBeach) return "768/FULL/ZOMBIE/ZOMBIE_BEACH_BASIC/ZOMBIE_BEACH_BASIC.PAM";
                if (isIce)   return "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_BASIC/ZOMBIE_ICEAGE_BASIC.PAM";
                if (isDark)  return "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM";
                if (isPirate) return "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BASIC/ZOMBIE_PIRATE_BASIC.PAM";
                if (isFuture) {
                    return "ZombieArmor4".equalsIgnoreCase(zombieAlias)
                            ? "768/FULL/ZOMBIE/ZOMBIE_FUTURE_BASIC_BRICK/ZOMBIE_FUTURE_BASIC_BRICK.PAM"
                            : "768/FULL/ZOMBIE/ZOMBIE_FUTURE_BASIC/ZOMBIE_FUTURE_BASIC.PAM";
                }
            }

            if ("ZombieImp".equalsIgnoreCase(zombieAlias)) {
                if (isEgypt) return "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_IMP/ZOMBIE_EGYPT_IMP.PAM";
                if (isBeach) return "768/FULL/ZOMBIE/ZOMBIE_BEACH_IMP_MERMAID/ZOMBIE_BEACH_IMP_MERMAID.PAM";
                if (isIce)   return "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_IMP/ZOMBIE_ICEAGE_IMP.PAM";
                if (isDark)  return "768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_MONK/ZOMBIE_DARK_IMP_MONK.PAM";
                if (isPirate) return "768/FULL/ZOMBIE/ZOMBIE_PIRATE_IMP/ZOMBIE_PIRATE_IMP.PAM";
                if (isFuture) return "768/FULL/ZOMBIE/GARGANTUAR_IMP/GARGANTUAR_IMP.PAM";
            }

            if ("ZombieFlag".equalsIgnoreCase(zombieAlias)) {
                if (isEgypt) return "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_VET_FLAG/ZOMBIE_EGYPT_VET_FLAG.PAM";
                if (isBeach) return "768/FULL/ZOMBIE/ZOMBIE_BEACH_FLAG/ZOMBIE_BEACH_FLAG.PAM";
                if (isIce)   return "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_FLAG/ZOMBIE_ICEAGE_FLAG.PAM";
                if (isDark)  return "768/FULL/ZOMBIE/ZOMBIE_DARK_FLAG/ZOMBIE_DARK_FLAG.PAM";
                if (isPirate) return "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BASIC_FLAG/ZOMBIE_PIRATE_BASIC_FLAG.PAM";
                if (isFuture) return "768/FULL/ZOMBIE/ZOMBIE_FUTURE_FLAG/ZOMBIE_FUTURE_FLAG.PAM";
            }
        }

        return pathFor(zombieAlias);
    }

    public static String pathForCurrentSeason(String zombieAlias) {
        model.utils.GameSession session = model.utils.GameSession.peekInstance();
        String season = session == null || session.getLevel() == null
                || session.getLevel().getSeason() == null
                ? null : session.getLevel().getSeason().getName();
        return pathFor(zombieAlias, season);
    }

    public static boolean isVerified(String zombieAlias) {
        return VERIFIED.containsKey(zombieAlias);
    }
}