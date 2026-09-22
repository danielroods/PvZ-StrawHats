package model.match.boss.behavior;

import model.collections.plant.Plant;
import model.match.boss.ZombossFight;
import model.match.boss.ZombossLawn;
import model.match.boss.ZombossSkyStrike;
import model.utils.GameSession;


public class FutureZombossBehavior extends EgyptZombossBehavior {

    private static final String MISSILE_PAM =
            "768/FULL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_FUTURE/"
                    + "ZOMBOSS_MISSILE_EXPLOSION_FUTURE.PAM";

    private static final ZombossSkyStrike.Config MISSILE = new ZombossSkyStrike.Config(
            MISSILE_PAM, "missile_lock_reticle",
            MISSILE_PAM, "missile",
            MISSILE_PAM, "missile_explosion",
            1.15, false);

    private static final int LINK_PAIR_COUNT = 3;

    private int chosenPair = -1;

    public FutureZombossBehavior(ZombossFight fight) {
        super(fight);
    }

    @Override
    protected void startMissile() {
        chosenPair = random().nextInt(LINK_PAIR_COUNT);
        fight.startAction(fight.newAction("missile")
                .then("linktile" + (chosenPair + 1) + "_start")
                .then("rocket_launch"));
        beginCooldown();
    }

    @Override
    protected void launchMissile() {
        GameSession session = session();
        if (session == null || chosenPair < 0) return;
        Plant target = ZombossLawn.pickBombardTarget(session, random());
        int column = target != null
                ? (int) Math.round(target.getPosition().x())
                : session.getCols() - 1;
        for (int row : rowsForPair(session, chosenPair)) {
            fight.addSkyStrike(new ZombossSkyStrike(MISSILE, row, column));
        }
    }

    
    private int[] rowsForPair(GameSession session, int pairIndex) {
        int rows = session.getRows();
        java.util.List<Integer> result = new java.util.ArrayList<>();
        for (int row = 0; row < rows; row++) {
            if (row % LINK_PAIR_COUNT == pairIndex) result.add(row);
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }
}