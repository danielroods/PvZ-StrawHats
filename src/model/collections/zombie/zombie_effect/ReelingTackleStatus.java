package model.collections.zombie.zombie_effect;

import model.collections.Faction;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.utils.GameSession;
import service.GameClock;

public class ReelingTackleStatus implements ZombieEffectStatus {

    // The hook sequence plays as three animation beats, one after another:
    //   "cast"      - the line goes out and hooks the plant/zombie.
    //   "cast_loop" - a one-second hold while the line stays taut.
    //   "reel"      - the actual pull happens, PULL_DELAY seconds after the
    //                 cast started (i.e. once cast + cast_loop have finished),
    //                 not at the moment the cast begins.
    private static final double CAST_DURATION = 1.0;
    private static final double CAST_LOOP_DURATION = 1.0;
    private static final double PULL_DELAY = CAST_DURATION + CAST_LOOP_DURATION; // 2 seconds after cast
    private static final double REEL_DURATION = 1.0;

    private final double reelCooldown;
    private double cooldownTimer;

    private boolean sequenceActive = false;
    private double sequenceTimer = 0;
    private boolean pullApplied = false;
    private int sequenceRow;
    private int sequenceCol;

    public ReelingTackleStatus(double reelCooldown) {
        this.reelCooldown = reelCooldown;
        this.cooldownTimer = reelCooldown;
    }

    @Override
    public void applyTickEffect(Zombie target, GameSession session) {
        if (!target.isAlive() || target.getPosition() == null) return;

        if (target.getFaction() == Faction.ZOMBIES) {
            int lastGridCol = session.getEnvironment().getCols() - 1;
            if (target.getPosition().x() < lastGridCol) {
                target.setPosition(new Position(lastGridCol, target.getPosition().y()));
            }
        }

        if (sequenceActive) {
            advanceSequence(target, session);
            return;
        }

        cooldownTimer += GameClock.SECONDS_PER_TICK;
        if (cooldownTimer >= reelCooldown) {
            if (hasReelTarget(target, session)) {
                startSequence(target);
                cooldownTimer = 0;
            }
        }
    }

    private void startSequence(Zombie caster) {
        sequenceActive = true;
        sequenceTimer = 0;
        pullApplied = false;
        sequenceRow = (int) caster.getPosition().y();
        sequenceCol = (int) caster.getPosition().x();
        caster.setActionAnimationState("cast", CAST_DURATION, false);
    }

    private void advanceSequence(Zombie caster, GameSession session) {
        sequenceTimer += GameClock.SECONDS_PER_TICK;

        if (sequenceTimer >= CAST_DURATION && sequenceTimer < PULL_DELAY
                && !"cast_loop".equals(caster.getActionAnimationState())) {
            caster.setActionAnimationState("cast_loop", CAST_LOOP_DURATION, false);
        }

        if (sequenceTimer >= PULL_DELAY && !pullApplied) {
            pullApplied = true;
            // The plant (or hostile zombie) only actually moves here, two
            // seconds after the cast began - not back when the cast fired.
            caster.setActionAnimationState("reel", REEL_DURATION, false);
            performPull(caster, session);
        }

        if (sequenceTimer >= PULL_DELAY + REEL_DURATION) {
            sequenceActive = false;
        }
    }

    private boolean hasReelTarget(Zombie caster, GameSession session) {
        int r = (int) caster.getPosition().y();
        int c = (int) caster.getPosition().x();
        if (caster.getFaction() == Faction.ZOMBIES) {
            return findDraggablePlantCell(session, r, c) != null;
        } else {
            return findHostileZombie(session, r, c) != null;
        }
    }

    private void performPull(Zombie caster, GameSession session) {
        if (caster.getFaction() == Faction.ZOMBIES) {
            dragPlantTowardZombie(session, sequenceRow, sequenceCol, caster);
        } else {
            pullHostileZombie(session, sequenceRow, sequenceCol);
        }
    }

    private Cell findDraggablePlantCell(GameSession session, int r, int c) {
        for (int colIter = c - 1; colIter >= 0; colIter--) {
            Cell activeCell = session.getEnvironment().getCell(r, colIter);
            if (activeCell != null && activeCell.getPlant() != null && activeCell.getPlant().isAlive()) {
                return activeCell;
            }
        }
        return null;
    }

    private boolean dragPlantTowardZombie(GameSession session, int r, int c, Zombie caster) {
        Cell activeCell = findDraggablePlantCell(session, r, c);
        if (activeCell == null) return false;

        Plant targetPlant = activeCell.getPlant();
        int currentCol = (int) targetPlant.getLocation().x();
        int nextCol = currentCol + 1;

        if (nextCol >= c) {
            targetPlant.takeDamage(targetPlant.getHP(), caster);
            return true;
        }

        Cell targetCell = session.getEnvironment().getCell(r, nextCol);
        if (targetCell != null && targetCell.getPlant() == null && targetCell.getStructure() == null) {
            activeCell.setPlant(null);
            targetCell.setPlant(targetPlant);
            targetPlant.setPosition(new Position(nextCol, r));
            return true;
        }
        return false;
    }

    private Zombie findHostileZombie(GameSession session, int r, int c) {
        for (Zombie enemy : session.getZombies()) {
            if (enemy.isAlive() && enemy.getFaction() == Faction.ZOMBIES
                    && (int) enemy.getPosition().y() == r && enemy.getPosition().x() > c) {
                return enemy;
            }
        }
        return null;
    }

    private boolean pullHostileZombie(GameSession session, int r, int c) {
        Zombie enemy = findHostileZombie(session, r, c);
        if (enemy == null) return false;
        enemy.takeDamage(enemy.getHp());
        return true;
    }
}