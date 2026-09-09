package model.collections.zombie.zombie_effect;

/**
 * Art/clip constants for the Pirate Captain's parrot. The parrot rides the
 * captain (rendered as part of {@code ZOMBIE_PIRATE_CAPTAIN.PAM}, no extra
 * object) until it is released to go steal a plant, at which point it becomes
 * its own object using this PAM file.
 */
public final class CaptainParrot {
    private CaptainParrot() {}

    public static final String PAM =
            "768/FULL/ZOMBIE/ZOMBIE_PIRATE_CAPTAIN_PARROT/ZOMBIE_PIRATE_CAPTAIN_PARROT.PAM";

    /** Played once as the parrot leaves the captain's shoulder. */
    public static final String CLIP_RELEASE = "parrot_releas";
    /** Looping clip while the parrot is in transit (out and back). */
    public static final String CLIP_FLY = "fly";
    /**
     * Played while carrying a snatched plant to the water. Authored moving to
     * the right, so the renderer must horizontally flip it since the parrot
     * always carries leftward toward the nearest un-bridged water tile.
     */
    public static final String CLIP_CARRY = "carry";
    /** Played as the parrot returns to re-land on the captain. */
    public static final String CLIP_FLYBACK = "fly back";
    /** Parrot's own death animation. */
    public static final String CLIP_DEATH = "feather_burst";
    /** Played once the parrot has re-landed on the captain's shoulder. */
    public static final String CLIP_LAND = "parrot_land";
}