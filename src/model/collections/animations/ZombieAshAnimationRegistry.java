package model.collections.animations;

import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieRace;

import java.util.Set;


public final class ZombieAshAnimationRegistry {

    private static final String IMP_ASH =
            "768/INITIAL/EFFECTS/ZOMBIE_IMP_ASH/ZOMBIE_IMP_ASH.PAM";
    private static final String GARGANTUAR_ASH =
            "768/INITIAL/EFFECTS/ZOMBIE_GARGANTUAR_ASH/ZOMBIE_GARGANTUAR_ASH.PAM";
    private static final String LOSTCITY_JANE_ASH =
            "768/FULL/EFFECTS/ZOMBIE_LOSTCITY_JANE_ASH/ZOMBIE_LOSTCITY_JANE_ASH.PAM";
    private static final String BIG_ASH =
            "768/INITIAL/EFFECTS/ZOMBIE_BIG_ASH/ZOMBIE_BIG_ASH.PAM";
    private static final String DEFAULT_ASH =
            "768/INITIAL/EFFECTS/ZOMBIE_ASH/ZOMBIE_ASH.PAM";

    
    public static final String ASH_STATE = "animation";

    private static final Set<String> NO_ASH_DEATH_ALIASES = Set.of("ZombieDarkKing", "ZombiePiano", "ZombieIceAgeDodo");
    private static final Set<String> BIG_ASH_ALIASES =
            Set.of("ZombieArcade", "ZombieIceAgeTroglobite", "ZombieBeachOctopus");
    private static final String LOST_CITY_JANE_ALIAS = "ZombieLostCityJane";

    private ZombieAshAnimationRegistry() {
    }

    
    public static String pathFor(Zombie zombie) {
        if (zombie == null) return DEFAULT_ASH;

        String alias = zombie.getAlias();
        if (alias != null && NO_ASH_DEATH_ALIASES.contains(alias)) return null;

        ZombieRace race = zombie.getRace();
        if (race == ZombieRace.IMP) return IMP_ASH;
        if (race == ZombieRace.GARGANTUAR) return GARGANTUAR_ASH;

        if (LOST_CITY_JANE_ALIAS.equals(alias)) return LOSTCITY_JANE_ASH;
        if (alias != null && BIG_ASH_ALIASES.contains(alias)) return BIG_ASH;

        return DEFAULT_ASH;
    }
}
