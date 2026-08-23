package model.collections.plant.actstrategy;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieRace;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.TileType;
import model.utils.GameSession;
import service.GameClock;

/**
 * Tangle Kelp's normal (non-Plant-Food) attack.
 *
 * Timeline: "idle" (lurking, nothing in reach) -> a zombie steps onto its water tile ->
 * "attack_submerge" (prepping the grab) -> "attack" (the grab itself - the target is
 * dragged smoothly under the fixed water ripple over this window and dies once fully
 * submerged) -> the plant is consumed, same as the real game.
 *
 * Gargantuars can't be dragged under and are ignored entirely, same as
 * {@link model.collections.plant.plantfood.TangleKelpPlantFood}.
 *
 * There is no deltaTime parameter on {@link ActStrategy#act}, but every simulation tick is
 * a fixed {@link GameClock#SECONDS_PER_TICK} (see SessionTicker), so that constant is used
 * directly to advance the phase timers here.
 */
public class TangleKelpStrategy implements ActStrategy {
    private static final double TRAP_ACTIVATION_RADIUS = 0.3;
    private static final double SUBMERGE_DURATION_FALLBACK = 0.4;
    private static final double ATTACK_DURATION_FALLBACK = 0.6;
    private static final int LETHAL_DAMAGE = 9999;

    private enum Phase { WAITING, SUBMERGING, ATTACKING, DONE }

    private Phase phase = Phase.WAITING;
    private double phaseTimer;
    private double submergeDuration = SUBMERGE_DURATION_FALLBACK;
    private double attackDuration = ATTACK_DURATION_FALLBACK;
    private Zombie grabbedZombie;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getPosition() == null || phase == Phase.DONE) return;
        double delta = GameClock.SECONDS_PER_TICK;

        switch (phase) {
            case WAITING -> tryGrab(user, session);
            case SUBMERGING -> {
                phaseTimer += delta;
                if (grabbedZombie == null || !grabbedZombie.isAlive()) {
                    // The target died or was otherwise removed before the grab landed;
                    // go back to lurking instead of grabbing an empty tile.
                    resetToWaiting();
                    return;
                }
                if (phaseTimer >= submergeDuration) {
                    beginAttack(user);
                }
            }
            case ATTACKING -> {
                phaseTimer += delta;
                if (grabbedZombie != null && grabbedZombie.isAlive()) {
                    grabbedZombie.applyStatus(Zombie.Status.BUTTER, Math.max(0.1, delta * 2.0));
                    double progress = attackDuration > 0
                            ? Math.min(1.0, phaseTimer / attackDuration) : 1.0;
                    grabbedZombie.setDragUnderWaterProgress(progress);
                    if (phaseTimer >= attackDuration) {
                        grabbedZombie.markDragUnderWaterDeath();
                        grabbedZombie.takeDamage(LETHAL_DAMAGE, user);
                    }
                }
                if (phaseTimer >= attackDuration) {
                    phase = Phase.DONE;
                    user.setAlive(false);
                }
            }
            case DONE -> { }
        }
    }

    private void resetToWaiting() {
        phase = Phase.WAITING;
        phaseTimer = 0.0;
        grabbedZombie = null;
    }

    private void tryGrab(Plant user, GameSession session) {
        Zombie target = findTarget(user, session);
        if (target == null) return;

        grabbedZombie = target;
        phase = Phase.SUBMERGING;
        phaseTimer = 0.0;

        float submergeClip = AnimationFactory.clipDurationForDisplayName(user.getName(), "attack_submerge");
        submergeDuration = submergeClip > 0f ? submergeClip : SUBMERGE_DURATION_FALLBACK;
        user.setVisualAnimationState("attack_submerge", submergeDuration);
    }

    private void beginAttack(Plant user) {
        phase = Phase.ATTACKING;
        phaseTimer = 0.0;

        float attackClip = AnimationFactory.clipDurationForDisplayName(user.getName(), "attack");
        attackDuration = attackClip > 0f ? attackClip : ATTACK_DURATION_FALLBACK;
        user.setVisualAnimationState("attack", attackDuration);
    }

    private Zombie findTarget(Plant user, GameSession session) {
        Zombie closest = null;
        double shortest = Double.MAX_VALUE;
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            if (zombie.getRace() == ZombieRace.GARGANTUAR) continue;
            if (!isOnWaterTile(zombie.getPosition(), session)) continue;
            double distance = zombie.getPosition().distanceTo(user.getPosition());
            if (distance >= TRAP_ACTIVATION_RADIUS) continue;
            if (distance < shortest) {
                shortest = distance;
                closest = zombie;
            }
        }
        return closest;
    }

    private boolean isOnWaterTile(Position position, GameSession session) {
        if (position == null || session.getEnvironment() == null) return false;
        int row = (int) Math.round(position.y());
        int col = (int) Math.round(position.x());
        Cell cell = session.getEnvironment().getCell(row, col);
        return cell != null && cell.getTile() != null && cell.getTile().type() == TileType.Water;
    }
}