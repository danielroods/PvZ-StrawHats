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

                if (bypassPlants != null && bypassPlants.contains(plantName)) {
                    if (Math.random() <= currentChance) {
                        executeJump = true;
                    }
                }
            }
        }

        if (executeJump) {
            double landingX = pos.x() - 1.2;
            zombie.setPosition(new Position(landingX, pos.y()));

            currentChance = resetChanceValue;
            jumpCooldownTimer = cooldownDuration;
            distanceAccumulator = 0;
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
