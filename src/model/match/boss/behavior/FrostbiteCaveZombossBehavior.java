package model.match.boss.behavior;

import model.collections.plant.Plant;
import model.match.boss.ZombossActionSequence;
import model.match.boss.ZombossFight;
import model.match.boss.ZombossLawn;
import model.match.boss.ZombossRowEffect;
import model.match.boss.ZombossSkyStrike;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.match.main.season.travellog.cave.IceWind;

/**
 * Frostbite Caves Zomboss. It is bolted into a glacier at the back of the lawn and never
 * moves: instead it breathes a freezing wind straight down one row (the {@code wind_1..4}
 * clips cover rows 2 to 5) and slingshots blocks of ice onto whatever is out of reach.
 * <p>
 * The glacier it sits in walls off the last two columns for the whole match - see
 * {@link #getBlockedColumnStart()}, which {@code SessionBoard} consults before it lets a
 * plant go down.
 */
public class FrostbiteCaveZombossBehavior extends ZombossBehavior {

    private static final String ICE_PAM =
            "768/FULL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_ICEAGE/"
                    + "ZOMBOSS_MISSILE_EXPLOSION_ICEAGE.PAM";

    private static final ZombossSkyStrike.Config ICE_BLOCK = new ZombossSkyStrike.Config(
            ICE_PAM, "missile_lock_reticle",
            ICE_PAM, "missile",
            ICE_PAM, "missile_explosion",
            1.0, false);

    /** The glacier is two columns deep, measured back from the right-hand edge. */
    private static final int BLOCKED_COLUMN_COUNT = 2;

    private static final double CHILL_TICK_SECONDS = 0.8;
    private static final double COOLDOWN_MIN = 2.0;
    private static final double COOLDOWN_SPREAD = 2.0;

    private int windRow = -1;
    private double chillTimer;
    private boolean slingshotFired;

    public FrostbiteCaveZombossBehavior(ZombossFight fight) {
        super(fight);
    }

    /** First lawn column the glacier covers; planting is refused from here to the right edge. */
    public int getBlockedColumnStart() {
        return Math.max(0, session().getCols() - BLOCKED_COLUMN_COUNT);
    }

    @Override
    public void update(double deltaSeconds) {
        ZombossActionSequence action = fight.getAction();
        if (action != null && !action.isFinished()) {
            driveAction(action, deltaSeconds);
            return;
        }
        if (!fight.isIdleForAction()) return;
        if (random().nextDouble() < 0.55) {
            startWind();
        } else {
            startSlingshot();
        }
    }

    private void beginCooldown() {
        fight.setActionCooldown(COOLDOWN_MIN + random().nextDouble() * COOLDOWN_SPREAD);
    }

    /**
     * {@code wind_1} through {@code wind_4} are authored for lawn rows 2 to 5, so row index 0
     * has no matching clip and is never chosen.
     */
    private void startWind() {
        int rows = session().getRows();
        int maxRow = Math.min(4, rows - 1);
        if (maxRow < 1) {
            startSlingshot();
            return;
        }
        windRow = 1 + random().nextInt(maxRow);
        chillTimer = 0.0;
        fight.startAction(fight.newAction("wind").then("wind_" + windRow));
        fight.addRowEffect(new ZombossRowEffect(IceWind.PAM_PATH_PLACEHOLDER, IceWind.PAM_CLIP,
                windRow, 2.9, false));
        beginCooldown();
    }

    private void startSlingshot() {
        slingshotFired = false;
        fight.startAction(fight.newAction("slingshot").then("slingshot"));
        beginCooldown();
    }

    private void driveAction(ZombossActionSequence action, double deltaSeconds) {
        consumeStepChange(action);
        if ("wind".equals(action.getName())) {
            chillTimer -= deltaSeconds;
            if (chillTimer <= 0) {
                chillTimer = CHILL_TICK_SECONDS;
                chillRow();
            }
        } else if ("slingshot".equals(action.getName())
                && !slingshotFired && action.getStepProgress() >= 0.6) {
            // The block leaves the sling about two thirds of the way through the wind-up.
            slingshotFired = true;
            dropIceBlock();
        }
    }

    private void chillRow() {
        if (windRow < 0) return;
        for (Plant plant : ZombossLawn.plantsInRow(session(), windRow)) {
            FrostbiteFreezing.addChillLevel(session(), plant);
        }
    }

    private void dropIceBlock() {
        Plant target = ZombossLawn.pickBombardTarget(session(), random());
        if (target == null) return;
        fight.addSkyStrike(new ZombossSkyStrike(ICE_BLOCK,
                (int) Math.round(target.getPosition().y()),
                (int) Math.round(target.getPosition().x())));
    }
}
