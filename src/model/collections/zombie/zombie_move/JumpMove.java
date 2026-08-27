package model.collections.zombie.zombie_move;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.utils.GameSession;

import java.util.List;

public class JumpMove implements MoveBehavior {
    private final double bonusChancePerCell;
    private final double cooldownDuration;
    private final double resetChanceValue;
    private final List<String> bypassPlants;

    private double currentChance;
    private double jumpCooldownTimer = 0;
    private double distanceAccumulator = 0;
    private boolean flying = false;
    private double flyAnimationTimer = 0;
    private static final double FLY_START_DURATION = 0.22;
    private static final double FLY_DURATION = 0.65;
    private static final double FLY_END_DURATION = 0.22;

    public JumpMove(double bonusChance, double cooldown, double initChance, double resetChance, List<String> bypassPlants) {
        this.bonusChancePerCell = bonusChance;
        this.cooldownDuration = cooldown;
        this.currentChance = initChance;
        this.resetChanceValue = resetChance;
        this.bypassPlants = bypassPlants;
    }

    @Override
    public void move(Zombie zombie, double deltaTime, GameSession session) {
        if (jumpCooldownTimer > 0) {
            jumpCooldownTimer = Math.max(0, jumpCooldownTimer - deltaTime);
        }

        Position pos = zombie.getPosition();
        Position speed = zombie.getSpeed();
        if (pos == null || speed == null) return;

        if (flying) {
            flyAnimationTimer -= deltaTime;
            if (flyAnimationTimer <= 0) {
                zombie.setActionAnimationState("fly_end", FLY_END_DURATION, false);
                flying = false;
            } else if (flyAnimationTimer <= FLY_DURATION) {
                zombie.setActionAnimationState("fly_loop", 0, true);
            }
        }

        double step = Math.abs(speed.x() * deltaTime);
        distanceAccumulator += step;
        if (distanceAccumulator >= 1.0) {
            currentChance += bonusChancePerCell;
            distanceAccumulator -= 1.0;
        }

        int currentRow = (int) pos.y();
        int targetCol = (int) (pos.x() - 0.5);
        Environment lawn = session.getLawn();
        Cell nextCell = (lawn != null) ? lawn.getCell(currentRow, targetCol) : null;

        boolean executeJump = false;
        if (nextCell != null && jumpCooldownTimer <= 0) {
            Plant targetPlant = nextCell.getPlant();
            if (targetPlant != null && targetPlant.isAlive()) {
                String plantName = targetPlant.getName().toLowerCase().replace("-", "").replace(" ", "");
                if (plantName.contains("iceberg")) plantName = "iceburg";

                if (bypassPlants != null && bypassPlants.contains(plantName)
                        && Math.random() <= currentChance) {
                    executeJump = true;
                }
            }

            if (!executeJump && nextCell.getObstacle() != null && Math.random() <= currentChance) {
                executeJump = true;
            }

            if (!executeJump && nextCell.getTile() != null
                    && nextCell.getTile().type() == model.pitches.TileType.Slippery
                    && Math.random() <= currentChance) {
                executeJump = true;
            }
        }

        if (executeJump) {
            double landingX = pos.x() - 1.2;
            zombie.setPosition(new Position(landingX, pos.y()));

            currentChance = resetChanceValue;
            jumpCooldownTimer = cooldownDuration;
            distanceAccumulator = 0;
            if (zombie.getAlias() != null && zombie.getAlias().equals("ZombieIceAgeDodo")) {
                flying = true;
                flyAnimationTimer = FLY_START_DURATION + FLY_DURATION;
                zombie.setActionAnimationState("fly_start", FLY_START_DURATION, false);
            }
        } else {
            Position nextPos = new Position(
                    pos.x() + speed.x() * deltaTime,
                    pos.y() + speed.y() * deltaTime
            );

            int oldCol = (int) pos.x();
            int newCol = (int) nextPos.x();
            if (newCol != oldCol) {
                nextPos = applySliderRedirect(zombie, pos, nextPos, session);
            }
            zombie.setPosition(nextPos);
        }

    }
}
