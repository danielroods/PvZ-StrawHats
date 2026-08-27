package model.collections.animations;

import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieRace;

import java.util.Set;

/**
 * Resolves the shock-death effect caused by Electric Blueberry.
 *
 * Only zombies with a dedicated shock PAM get the shock animation. Zombies
 * without a matching entry return null and keep their normal death behavior.
 */
public final class ZombieShockAnimationRegistry {

    private static final String IMP_SHOCK =
            "768/INITIAL/EFFECTS/ZOMBIE_IMP_SHOCK/ZOMBIE_IMP_SHOCK.PAM";
    private static final String BIG_SHOCK =
            "768/INITIAL/EFFECTS/ZOMBIE_BIG_SHOCK/ZOMBIE_BIG_SHOCK.PAM";
    private static final String GARGANTUAR_SHOCK =
            "768/INITIAL/EFFECTS/ZOMBIE_GARGANTUAR_SHOCK/ZOMBIE_GARGANTUAR_SHOCK.PAM";
    private static final String DODO_SHOCK =
            "768/FULL/EFFECTS/ZOMBIE_DODO_SHOCK/ZOMBIE_DODO_SHOCK.PAM";
    private static final String DEFAULT_SHOCK =
            "768/INITIAL/EFFECTS/ZOMBIE_SHOCK/ZOMBIE_SHOCK.PAM";

    public static final String SHOCK_STATE = "animation";

    private static final Set<String> BIG_SHOCK_ALIASES =
            Set.of("ZombieArcade", "ZombieIceAgeTroglobite", "ZombieBeachOctopus");
    private static final String DODO_ALIAS = "ZombieIceAgeDodo";

    private ZombieShockAnimationRegistry() {
    }

    public static String pathFor(Zombie zombie) {
        if (zombie == null) return null;

        String alias = zombie.getAlias();
        if (DODO_ALIAS.equals(alias)) return DODO_SHOCK;

        ZombieRace race = zombie.getRace();
        if (race == ZombieRace.IMP) return IMP_SHOCK;
        if (race == ZombieRace.GARGANTUAR) return GARGANTUAR_SHOCK;

        if (alias != null && BIG_SHOCK_ALIASES.contains(alias)) return BIG_SHOCK;

        // The generic shock asset is used for ordinary zombies. The explicit
        // special cases above prevent larger/special zombie bodies from using it.
        return DEFAULT_SHOCK;
    }
}
