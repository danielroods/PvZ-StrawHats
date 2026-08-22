package model.collections.animations;

import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieRace;

import java.util.Set;

/**
 * Resolves the "ash" death effect a zombie should play when it is killed by
 * fire (e.g. a Fire Peashooter shot) instead of its normal die/particles
 * animation.
 * <p>
 * Grouping mirrors the game's classic ash-death sets:
 * <ul>
 *     <li>Imps (any {@link ZombieRace#IMP}) -> ZOMBIE_IMP_ASH</li>
 *     <li>Gargantuars (any {@link ZombieRace#GARGANTUAR}) -> ZOMBIE_GARGANTUAR_ASH</li>
 *     <li>Lost City Jane -> ZOMBIE_LOSTCITY_JANE_ASH</li>
 *     <li>Arcade Zombie, Troglobite, Beach Octopus Zombie -> ZOMBIE_BIG_ASH</li>
 *     <li>Everyone else -> ZOMBIE_ASH</li>
 *     <li>King (ZombieDarkKing) and Piano Zombie (ZombiePiano) never get an
 *     ash death; their death animation is always the normal one.</li>
 * </ul>
 */
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

    /** The animation clip/state name every ash PAM uses. */
    public static final String ASH_STATE = "animation";

    private static final Set<String> NO_ASH_DEATH_ALIASES = Set.of("ZombieDarkKing", "ZombiePiano");
    private static final Set<String> BIG_ASH_ALIASES =
            Set.of("ZombieArcade", "ZombieIceAgeTroglobite", "ZombieBeachOctopus");
    private static final String LOST_CITY_JANE_ALIAS = "ZombieLostCityJane";

    private ZombieAshAnimationRegistry() {
    }

    /**
     * @return the ash PAM path for this zombie, or {@code null} if this zombie
     * should never play an ash death (its death animation is always the
     * normal one, e.g. King and Piano Zombie).
     */
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
