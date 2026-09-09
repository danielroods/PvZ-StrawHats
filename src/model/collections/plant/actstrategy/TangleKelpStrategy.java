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

public class TangleKelpStrategy implements ActStrategy {
    private static final double SUBMERGE_DURATION_FALLBACK = 0.4;
    private static final double ATTACK_DURATION_FALLBACK = 0.6;
    private static final int LETHAL_DAMAGE = 9999;

    private static int lethalDamage(Plant user) {
        return Math.max(LETHAL_DAMAGE, user.getDamage());
    }

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
                    
                    
                    resetToWaiting(user);
                    return;
                }
                holdVictim(delta);
                if (phaseTimer >= submergeDuration) {
                    beginAttack(user);
                }
            }
            case ATTACKING -> {
                phaseTimer += delta;
                if (grabbedZombie != null && grabbedZombie.isAlive()) {
                    holdVictim(delta);
                    double progress = attackDuration > 0
                            ? Math.min(1.0, phaseTimer / attackDuration) : 1.0;
                    grabbedZombie.setDragUnderWaterProgress(progress);
                    if (phaseTimer >= attackDuration) {
                        grabbedZombie.markDragUnderWaterDeath();
                        grabbedZombie.takeDamage(lethalDamage(user), user);
                    }
                }
                if (phaseTimer >= attackDuration) {
                    phase = Phase.DONE;
                    user.setSpecialInvulnerable(false);
                    user.setAlive(false);
                }
            }
            case DONE -> { }
        }
    }

    private void holdVictim(double delta) {
        if (grabbedZombie == null || !grabbedZombie.isAlive()) return;
        grabbedZombie.applyStatus(Zombie.Status.BUTTER, Math.max(0.1, delta * 2.0));
    }

    private void resetToWaiting(Plant user) {
        phase = Phase.WAITING;
        phaseTimer = 0.0;
        grabbedZombie = null;
        user.setSpecialInvulnerable(false);
        user.clearVisualAnimationState();
    }

    private void tryGrab(Plant user, GameSession session) {
        Zombie target = findTarget(user, session);
        if (target == null) return;

        grabbedZombie = target;
        phase = Phase.SUBMERGING;
        phaseTimer = 0.0;
        user.setSpecialInvulnerable(true);

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
            if (zombie.isHypnotized()) continue;
            if (zombie.getRace() == ZombieRace.GARGANTUAR) continue;
            if (!isOnWaterTile(zombie.getPosition(), session)) continue;
            if (!isOnTile(zombie.getPosition(), user.getPosition())) continue;
            double distance = zombie.getPosition().distanceTo(user.getPosition());
            if (distance < shortest) {
                shortest = distance;
                closest = zombie;
            }
        }
        return closest;
    }

    private boolean isOnTile(Position zombie, Position plant) {
        return Math.round(zombie.x()) == Math.round(plant.x())
                && Math.round(zombie.y()) == Math.round(plant.y());
    }

    private boolean isOnWaterTile(Position position, GameSession session) {
        if (position == null || session.getEnvironment() == null) return false;
        int row = (int) Math.round(position.y());
        int col = (int) Math.round(position.x());
        Cell cell = session.getEnvironment().getCell(row, col);
        return cell != null && cell.getTile() != null && cell.getTile().type() == TileType.Water;
    }
}
