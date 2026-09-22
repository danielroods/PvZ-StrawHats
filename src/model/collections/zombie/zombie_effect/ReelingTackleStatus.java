package model.collections.zombie.zombie_effect;

import model.collections.Faction;
import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match.waves.SpawnPlacement;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.TileType;
import model.utils.GameSession;
import service.GameClock;

import java.util.ArrayList;
import java.util.List;

public class ReelingTackleStatus implements ZombieEffectStatus {

    
    
    
    
    
    
    private static final double CAST_DURATION = 1.0;
    private static final double CAST_LOOP_DURATION = 1.0;
    private static final double PULL_DELAY = CAST_DURATION + CAST_LOOP_DURATION; 
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

    private static double holdColumnFor(Zombie target, GameSession session) {
        double lastGridCol = session.getEnvironment().getCols() - 1;
        List<Zombie> otherFishermen = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (zombie != target && zombie.isAlive() && target.getAlias().equals(zombie.getAlias())) {
                otherFishermen.add(zombie);
            }
        }
        if (otherFishermen.isEmpty()) return lastGridCol;
        return SpawnPlacement.clearSpot(otherFishermen, target,
                (int) Math.round(target.getPosition().y()), lastGridCol, 0.5, lastGridCol);
    }

    @Override
    public void applyTickEffect(Zombie target, GameSession session) {
        if (!target.isAlive() || target.getPosition() == null) return;

        if (target.getFaction() == Faction.ZOMBIES) {
            double holdColumn = holdColumnFor(target, session);
            if (target.getPosition().x() < holdColumn) {
                target.setPosition(new Position(holdColumn, target.getPosition().y()));
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

    
    private static List<Plant> plantStack(Plant top) {
        List<Plant> stack = new ArrayList<>();
        for (Plant cursor = top; cursor != null; cursor = cursor.getBottom()) {
            stack.add(cursor);
        }
        return stack;
    }

    private boolean dragPlantTowardZombie(GameSession session, int r, int c, Zombie caster) {
        Cell activeCell = findDraggablePlantCell(session, r, c);
        if (activeCell == null) return false;

        Plant targetPlant = activeCell.getPlant();
        List<Plant> stack = plantStack(targetPlant);
        int currentCol = (int) targetPlant.getLocation().x();
        int nextCol = currentCol + 1;

        if (nextCol >= c) {
            
            
            for (Plant plant : stack) {
                if (plant.isAlive()) plant.takeDamage(plant.getHP(), caster);
            }
            return true;
        }

        Cell targetCell = session.getEnvironment().getCell(r, nextCol);
        if (targetCell == null || targetCell.getPlant() != null || targetCell.getStructure() != null) {
            return false;
        }

        boolean destinationIsWater = targetCell.getTile() != null
                && targetCell.getTile().type() == TileType.Water;
        boolean stackHasLivingWaterSupport = stack.stream()
                .anyMatch(plant -> plant.isAlive() && plant.getTags().contains(PlantTag.WATER));

        if (destinationIsWater && !stackHasLivingWaterSupport) {
            
            
            
            for (Plant plant : stack) {
                if (plant.isAlive()) plant.takeDamage(plant.getHP(), caster);
            }
            return true;
        }

        activeCell.setPlant(null);
        targetCell.setPlant(targetPlant);
        
        
        
        for (Plant plant : stack) {
            plant.setPosition(new Position(nextCol, r));
        }
        return true;
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