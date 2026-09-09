package model.match.boss.behavior;

import model.collections.plant.Plant;
import model.match.boss.ZombossActionSequence;
import model.match.boss.ZombossFight;
import model.match.boss.ZombossLawn;
import model.match.boss.ZombossSkyStrike;
import model.match_mechanisms.vector.Position;

/**
 * Ancient Egypt Zomboss. The mobile one: it walks the back half of the lawn a tile at a time,
 * hops onto a plant to crush it and hops straight back, stomps whatever gets close, opens a
 * portal on its own tile to let minions through, and lobs a guided rocket at the back line.
 */
public class EgyptZombossBehavior extends ZombossBehavior {

    private static final String MISSILE_PAM =
            "768/INITIAL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_EGYPT/"
                    + "ZOMBOSS_MISSILE_EXPLOSION_EGYPT.PAM";

    private static final ZombossSkyStrike.Config MISSILE = new ZombossSkyStrike.Config(
            MISSILE_PAM, "missile_lock_reticle",
            MISSILE_PAM, "missile",
            MISSILE_PAM, "missile_explosion",
            1.15, false);

    private static final double STOMP_REACH = 1.35;
    private static final double STOMP_CHANCE = 0.35;
    private static final double PORTAL_LOOP_SECONDS = 4.0;
    private static final int PORTAL_SPAWN_COUNT = 2;
    private static final double MIN_COLUMN = 5.0;
    private static final double MAX_COLUMN = 8.0;
    private static final double COOLDOWN_MIN = 3.0;
    private static final double COOLDOWN_SPREAD = 2.2;

    private Position moveFrom;
    private Position moveTo;
    private Position jumpHome;
    private Plant jumpVictim;

    private double portalSpawnTimer;
    private int portalSpawned;
    private boolean stompApplied;

    public EgyptZombossBehavior(ZombossFight fight) {
        super(fight);
    }

    @Override
    public void update(double deltaSeconds) {
        ZombossActionSequence action = fight.getAction();
        if (action != null && !action.isFinished()) {
            driveAction(action, deltaSeconds);
            return;
        }
        if (!fight.isIdleForAction()) return;
        chooseAbility();
    }

    private void chooseAbility() {
        
        
        Plant reachable = ZombossLawn.plantWithinReach(session(), fight.getBossPosition(),
                STOMP_REACH);
        if (reachable != null && random().nextDouble() < STOMP_CHANCE) {
            startStomp();
            return;
        }
        double roll = random().nextDouble();
        if (roll < 0.28) {
            startMissile();
        } else if (roll < 0.50) {
            startPortal();
        } else if (roll < 0.66 && startJump()) {
            return;
        } else {
            startWalk();
        }
    }

    private void beginCooldown() {
        fight.queueRecovery(COOLDOWN_MIN + random().nextDouble() * COOLDOWN_SPREAD);
    }

    

    private void startStomp() {
        stompApplied = false;
        fight.startAction(fight.newAction("stomp").then("stomp"));
        beginCooldown();
    }

    private void applyStomp() {
        Position position = fight.getBossPosition();
        Plant underfoot = session().getPlantAt((int) Math.round(position.y()),
                (int) Math.round(position.x()));
        Plant victim = underfoot != null ? underfoot
                : ZombossLawn.plantWithinReach(session(), position, STOMP_REACH);
        if (victim != null) ZombossLawn.destroyPlant(session(), victim);
    }

    

    private boolean startJump() {
        Plant victim = ZombossLawn.pickBombardTarget(session(), random());
        if (victim == null) return false;
        jumpVictim = victim;
        jumpHome = fight.getBossPosition();
        moveFrom = jumpHome;
        moveTo = new Position(Math.round(victim.getPosition().x()),
                Math.round(victim.getPosition().y()));
        fight.startAction(fight.newAction("jump")
                .then("jump_start").then("jump_mid").then("jump_land")
                .then("jump_start").then("jump_mid").then("jump_land"));
        beginCooldown();
        return true;
    }

    

    private void startPortal() {
        portalSpawned = 0;
        portalSpawnTimer = 0.0;
        fight.startAction(fight.newAction("portal")
                .then("zombie_portal_start")
                .loop("zombie_portal_loop", PORTAL_LOOP_SECONDS)
                .then("zombie_portal_end"));
        beginCooldown();
    }

    

    private void startMissile() {
        fight.startAction(fight.newAction("missile")
                .then("missile_start").then("rocket_launch"));
        beginCooldown();
    }

    private void launchMissile() {
        Plant target = ZombossLawn.pickBombardTarget(session(), random());
        if (target == null) return;
        fight.addSkyStrike(new ZombossSkyStrike(MISSILE,
                (int) Math.round(target.getPosition().y()),
                (int) Math.round(target.getPosition().x())));
    }

    

    private void startWalk() {
        Position from = fight.getBossPosition();
        int rows = session().getRows();
        String clip;
        double targetCol = from.x();
        double targetRow = from.y();

        int direction = random().nextInt(4);
        switch (direction) {
            case 0 -> {
                targetCol = from.x() - 1;
                clip = "walk_forward";
            }
            case 1 -> {
                targetCol = from.x() + 1;
                clip = "walk_backwards";
            }
            case 2 -> {
                targetRow = from.y() - 1;
                clip = "walk_up";
            }
            default -> {
                targetRow = from.y() + 1;
                clip = "walk_down";
            }
        }
        if (targetCol < MIN_COLUMN || targetCol > MAX_COLUMN
                || targetRow < 0 || targetRow > rows - 1) {
            
            fight.setActionCooldown(1.5);
            return;
        }
        moveFrom = from;
        moveTo = new Position(targetCol, targetRow);
        fight.startAction(fight.newAction("walk").then(clip));
        beginCooldown();
    }

    

    @Override
    public void onStunStart() {
        
        
        if (jumpHome != null) fight.moveBossTo(jumpHome.x(), jumpHome.y());
        jumpVictim = null;
        moveFrom = null;
        moveTo = null;
    }

    @Override
    public void onActionFinished(ZombossActionSequence sequence) {
        
        
        switch (sequence.getName()) {
            case "walk" -> {
                if (moveTo != null) fight.moveBossTo(moveTo.x(), moveTo.y());
            }
            case "jump" -> {
                if (jumpHome != null) fight.moveBossTo(jumpHome.x(), jumpHome.y());
            }
            default -> { }
        }
    }

    private void driveAction(ZombossActionSequence action, double deltaSeconds) {
        int step = consumeStepChange(action);
        switch (action.getName()) {
            case "stomp" -> {
                if (!stompApplied && action.getStepProgress() >= 0.55) {
                    stompApplied = true;
                    applyStomp();
                }
            }
            case "walk" -> lerpBoss(action.getStepProgress());
            case "jump" -> driveJump(action, step);
            case "portal" -> drivePortal(action, deltaSeconds);
            case "missile" -> {
                if (step == 1) launchMissile();
            }
            default -> { }
        }
    }

    private void driveJump(ZombossActionSequence action, int step) {
        switch (step) {
            case 1 -> {
                moveFrom = fight.getBossPosition();
                
            }
            case 2 -> {
                fight.moveBossTo(moveTo.x(), moveTo.y());
                if (jumpVictim != null && jumpVictim.isAlive()) {
                    ZombossLawn.destroyPlant(session(), jumpVictim);
                }
                jumpVictim = null;
            }
            case 4 -> {
                moveFrom = fight.getBossPosition();
                moveTo = jumpHome;
            }
            case 5 -> fight.moveBossTo(jumpHome.x(), jumpHome.y());
            default -> { }
        }
        if (action.getStepIndex() == 1 || action.getStepIndex() == 4) {
            lerpBoss(action.getStepProgress());
        }
    }

    private void drivePortal(ZombossActionSequence action, double deltaSeconds) {
        if (action.getStepIndex() != 1) return;
        portalSpawnTimer -= deltaSeconds;
        if (portalSpawnTimer > 0 || portalSpawned >= PORTAL_SPAWN_COUNT) return;
        fight.spawnMinionAtBoss();
        portalSpawned++;
        portalSpawnTimer = PORTAL_LOOP_SECONDS / (PORTAL_SPAWN_COUNT + 1.0);
    }

    private void lerpBoss(double progress) {
        if (moveFrom == null || moveTo == null) return;
        double eased = progress * progress * (3.0 - 2.0 * progress);
        fight.moveBossTo(moveFrom.x() + (moveTo.x() - moveFrom.x()) * eased,
                moveFrom.y() + (moveTo.y() - moveFrom.y()) * eased);
    }
}
