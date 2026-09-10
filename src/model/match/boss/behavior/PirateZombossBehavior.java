package model.match.boss.behavior;

import model.match.boss.ZombossFight;
import model.match.boss.ZombossSkyStrike;

/**
 * Pirates Zomboss. Same action set as the Ancient Egypt Zomboss (walk, jump-crush, stomp,
 * portal, missile) but fires its own Pirate-themed missile explosion PAM instead of Egypt's.
 */
public class PirateZombossBehavior extends EgyptZombossBehavior {

    private static final String MISSILE_PAM =
            "768/FULL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_PIRATE/"
                    + "ZOMBOSS_MISSILE_EXPLOSION_PIRATE.PAM";

    private static final ZombossSkyStrike.Config MISSILE = new ZombossSkyStrike.Config(
            MISSILE_PAM, "missile_lock_reticle",
            MISSILE_PAM, "missile",
            MISSILE_PAM, "missile_explosion",
            1.15, false);

    public PirateZombossBehavior(ZombossFight fight) {
        super(fight);
    }

    @Override
    protected void launchMissile() {
        model.collections.plant.Plant target =
                model.match.boss.ZombossLawn.pickBombardTarget(session(), random());
        if (target == null) return;
        fight.addSkyStrike(new ZombossSkyStrike(MISSILE,
                (int) Math.round(target.getPosition().y()),
                (int) Math.round(target.getPosition().x())));
    }
}