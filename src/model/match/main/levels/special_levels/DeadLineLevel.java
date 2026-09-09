package model.match.main.levels.special_levels;

import model.match.main.levels.Level;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class DeadLineLevel extends Level {
    private Position deadLine; // e.g., column x = 3

    
    @Override
    public boolean checkLossCondition(GameSession session) {
        if (deadLine == null) return false;

        return session.getZombies().stream()
                .anyMatch(zombie -> zombie.isAlive()
                        && zombie.getPosition() != null
                        && zombie.getPosition().x() <= deadLine.x());
    }

    public Position getDeadLine() { return deadLine; }
    public void setDeadLine(Position deadLine) { this.deadLine = deadLine; }
}
