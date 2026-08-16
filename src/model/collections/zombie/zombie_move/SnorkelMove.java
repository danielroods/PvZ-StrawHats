package model.collections.zombie.zombie_move;

import model.collections.zombie.VulnerabilityType;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieState;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class SnorkelMove implements MoveBehavior {

    @Override
    public void move(Zombie zombie, double deltaTime, GameSession session) {
        Position pos = zombie.getPosition();
        if (pos == null || zombie.getSpeed() == null) return;

        double deltaX = zombie.getSpeed().x() * deltaTime;
        Position nextPos = new Position(pos.x() + deltaX, pos.y());
        nextPos = applySliderRedirect(zombie, pos, nextPos, session);
        zombie.setPosition(nextPos);
        double nextX = nextPos.x();

        var level = session.getLevel();
        boolean inWaterSection = level != null
                && level.getCurrentTideColumn() > 0
                && nextX >= session.getEnvironment().getCols() - level.getCurrentTideColumn();
        boolean isEating = zombie.getZombieState() == ZombieState.EATING;

        if (inWaterSection && !isEating) {
            zombie.setVulnerabilityState(VulnerabilityType.SUBMERGED);
        } else {
            zombie.setVulnerabilityState(VulnerabilityType.FULLY_VULNERABLE);
        }

    }
}
