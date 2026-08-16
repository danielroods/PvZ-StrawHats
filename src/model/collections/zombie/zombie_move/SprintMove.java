package model.collections.zombie.zombie_move;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class SprintMove implements MoveBehavior {
    private final double sprintMultiplier;
    private final double angerMultiplier;
    private final boolean enrageOnArmorLoss;

    public SprintMove() {
        this(1.0, 1.0, false);
    }

    public SprintMove(double baseSprintMultiplier) {
        this(baseSprintMultiplier, 1.0, false);
    }

    public SprintMove(double baseSprintMultiplier, double enrageMultiplier, boolean enragesOnArmorLoss) {
        this.sprintMultiplier = baseSprintMultiplier;
        this.angerMultiplier = enrageMultiplier;
        this.enrageOnArmorLoss = enragesOnArmorLoss;
    }

    @Override
    public void move(Zombie zombie, double deltaTime, GameSession session) {
        Position pos = zombie.getPosition();
        if (pos == null) return;

        double speedX = getActiveSpeedX(zombie) * deltaTime;
        Position nextPos = new Position(pos.x() + speedX, pos.y());

        int oldCol = (int) pos.x();
        int newCol = (int) nextPos.x();
        if (newCol != oldCol) {
            nextPos = applySliderRedirect(zombie, pos, nextPos, session);
        }

        zombie.setPosition(nextPos);

    }

    protected double getActiveSpeedX(Zombie zombie) {
        Position speed = zombie.getSpeed();
        if (speed == null) return 0.0;

        boolean armorDestroyed = (zombie.getArmour() == null || zombie.getArmour().getHP() <= 0);
        if (enrageOnArmorLoss && armorDestroyed) {
            return speed.x() * angerMultiplier;
        }
        return speed.x() * sprintMultiplier;
    }
}
