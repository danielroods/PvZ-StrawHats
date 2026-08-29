package model.match.boss.behavior;

import model.collections.plant.Plant;
import model.match.boss.ZombossActionSequence;
import model.match.boss.ZombossFight;
import model.match.boss.ZombossLawn;
import model.match.boss.ZombossRowEffect;
import model.match.boss.ZombossSkyStrike;

public class DarkAgeZombossBehavior extends ZombossBehavior {

    private static final String FIREBALL_PAM =
            "768/FULL/EFFECTS/ZOMBOSS_DARK_FIREBALL/ZOMBOSS_DARK_FIREBALL.PAM";
    public static final String FIRE_TILE_PAM = "768/FULL/EFFECTS/SNAPDRAGON_PLANTFOOD_FIREBALLS/SNAPDRAGON_PLANTFOOD_FIREBALLS.PAM";
    public static final String FIRE_TILE_CLIP = "animation";

    private static final ZombossSkyStrike.Config FIRE_BOMB = new ZombossSkyStrike.Config(
            null, null,
            FIREBALL_PAM, "fall",
            FIREBALL_PAM, "impact",
            2.6, true);

    private static final double ROW_SHIFT_SECONDS = 1.1;
    private static final double FIRE_ATTACK_LOOP_SECONDS = 3.0;
    private static final double FIRE_ROW_BURN_SECONDS = 3.0;
    private static final double FIRE_ROW_WARNING_SECONDS = 1.3;
    private static final int FIRE_BREATH_RANGE = 5;
    private static final double FIRE_BOMB_LOOP_SECONDS = 1.2;
    private static final int SUMMON_COUNT = 1;
    private static final double VULNERABLE_LOOP_SECONDS = 5.0;
    private static final double VULNERABLE_CHANCE = 0.28;
    private static final double COOLDOWN_MIN = 3.0;
    private static final double COOLDOWN_SPREAD = 2.2;

    private double shiftFromRow;
    private double shiftToRow;
    private int summoned;
    private double summonTimer;
    private boolean bombLaunched;
    private boolean rowLit;
    private boolean vulnerableOpen;

    public DarkAgeZombossBehavior(ZombossFight fight) {
        super(fight);
    }

    @Override
    public void onBattleStart() {
        fight.moveBossTo(Math.max(0, session().getCols() - 2), fight.getBossPosition().y());
    }

    @Override
    public void update(double deltaSeconds) {
        ZombossActionSequence action = fight.getAction();
        if (action != null && !action.isFinished()) {
            driveAction(action, deltaSeconds);
            return;
        }
        if (vulnerableOpen) closeVulnerable();
        if (!fight.isIdleForAction()) return;
        chooseAbility();
    }

    private void chooseAbility() {
        if (random().nextDouble() < VULNERABLE_CHANCE) {
            startVulnerable();
            return;
        }
        double roll = random().nextDouble();
        if (roll < 0.26) {
            startSummon();
        } else if (roll < 0.52) {
            startFireBomb();
        } else if (roll < 0.72) {
            startFireAttack();
        } else if (!startRowShift()) {
            startFireAttack();
        }
    }

    private void beginCooldown() {
        fight.queueRecovery(COOLDOWN_MIN + random().nextDouble() * COOLDOWN_SPREAD);
    }

    private void startSummon() {
        summoned = 0;
        summonTimer = 0.0;
        fight.startAction(fight.newAction("summon").then("summoning"));
        beginCooldown();
    }

    private void startFireBomb() {
        bombLaunched = false;
        fight.startAction(fight.newAction("firebomb")
                .then("fire_bomb")
                .loop("fire_bomb_loop", FIRE_BOMB_LOOP_SECONDS)
                .then("fire_bomb_end"));
        beginCooldown();
    }

    private void launchFireBomb() {
        Plant target = ZombossLawn.pickBombardTarget(session(), random());
        if (target == null) return;
        fight.addSkyStrike(new ZombossSkyStrike(FIRE_BOMB,
                (int) Math.round(target.getPosition().y()),
                (int) Math.round(target.getPosition().x())));
    }

    private void startFireAttack() {
        rowLit = false;
        fight.startAction(fight.newAction("fireattack")
                .then("fire_attack")
                .loop("fire_attack_idle", FIRE_ATTACK_LOOP_SECONDS)
                .then("fire_attack_end"));
        beginCooldown();
    }

    private void igniteRow() {
        int row = (int) Math.round(fight.getBossPosition().y());
        int reach = (int) Math.round(fight.getBossPosition().x()) - FIRE_BREATH_RANGE + 1;
        fight.addRowEffect(new ZombossRowEffect(FIRE_TILE_PAM, FIRE_TILE_CLIP, row,
                FIRE_ROW_BURN_SECONDS, true, FIRE_ROW_WARNING_SECONDS, Math.max(0, reach)));
    }

    private boolean startRowShift() {
        int rows = session().getRows();
        if (rows <= 1) return false;
        int current = (int) Math.round(fight.getBossPosition().y());
        int target = random().nextInt(rows);
        if (target == current) target = (current + 1) % rows;
        shiftFromRow = fight.getBossPosition().y();
        shiftToRow = target;
        fight.startAction(fight.newAction("shift").then(idleClip(), ROW_SHIFT_SECONDS));
        beginCooldown();
        return true;
    }

    private void startVulnerable() {
        vulnerableOpen = true;
        fight.setVulnerable(true);
        fight.startAction(fight.newAction("vulnerable")
                .then("vulnerable")
                .loop("vulnerable_loop", VULNERABLE_LOOP_SECONDS)
                .then("vulnerable_end"));
        beginCooldown();
        view.GeneralPrinter.print("The dragon machine glitches - strike it now!");
    }

    private void closeVulnerable() {
        vulnerableOpen = false;
        fight.setVulnerable(false);
    }

    @Override
    public void onStunStart() {
        if (vulnerableOpen) closeVulnerable();
    }

    @Override
    public void onDefeated() {
        if (vulnerableOpen) closeVulnerable();
    }

    @Override
    public void onActionFinished(ZombossActionSequence sequence) {
        if ("vulnerable".equals(sequence.getName())) closeVulnerable();
        if ("shift".equals(sequence.getName())) {
            fight.moveBossTo(fight.getBossPosition().x(), shiftToRow);
        }
    }

    private void driveAction(ZombossActionSequence action, double deltaSeconds) {
        int step = consumeStepChange(action);
        switch (action.getName()) {
            case "summon" -> driveSummon(action, deltaSeconds);
            case "firebomb" -> {
                if (step == 1 && !bombLaunched) {
                    bombLaunched = true;
                    launchFireBomb();
                }
            }
            case "fireattack" -> {
                if (step == 1 && !rowLit) {
                    rowLit = true;
                    igniteRow();
                }
            }
            case "shift" -> {
                double eased = ease(action.getStepProgress());
                fight.moveBossTo(fight.getBossPosition().x(),
                        shiftFromRow + (shiftToRow - shiftFromRow) * eased);
            }
            default -> { }
        }
    }

    private void driveSummon(ZombossActionSequence action, double deltaSeconds) {
        summonTimer -= deltaSeconds;
        if (summonTimer > 0 || summoned >= SUMMON_COUNT) return;
        fight.spawnMinionAtBoss();
        summoned++;
        summonTimer = action.totalSeconds() / (SUMMON_COUNT + 1.0);
    }

    private static double ease(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }
}
