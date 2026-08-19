package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.Comparator;

public class WallNutStrategy implements ActStrategy {
    private static final double CONTACT_RADIUS = 0.7;
    private static final double ATTRACT_RANGE = 4.0;

    @Override
    public void act(Plant user, GameSession session) {
        if (!user.getTags().contains(PlantTag.MOVE_ZOMBIES) || user.getIntervalTimer() > 0) return;

        if (user.getName().equalsIgnoreCase("Sweet Potato")) attractZombie(user, session);
        else divertTouchingZombie(user, session);
    }

    private void divertTouchingZombie(Plant user, GameSession session) {
        Zombie target = session.getZombies().stream()
                .filter(zombie -> zombie != null && zombie.isAlive() && !zombie.isHypnotized()
                        && zombie.getPosition() != null
                        && zombie.getPosition().distanceTo(user.getPosition()) <= CONTACT_RADIUS)
                .min(Comparator.comparingDouble(zombie -> zombie.getPosition().distanceTo(user.getPosition())))
                .orElse(null);
        if (target == null) return;

        int rows = session.getRows();
        int currentRow = (int) Math.round(target.getPosition().y());
        int targetRow;
        if (currentRow <= 0) targetRow = 1;
        else if (currentRow >= rows - 1) targetRow = rows - 2;
        else targetRow = Math.random() < 0.5 ? currentRow - 1 : currentRow + 1;

        target.setPosition(new Position(target.getPosition().x(), targetRow));
        user.setInternalTimer(user.getActionInterval());
    }

    private void attractZombie(Plant user, GameSession session) {
        Zombie target = session.getZombies().stream()
                .filter(zombie -> zombie != null && zombie.isAlive() && !zombie.isHypnotized()
                        && zombie.getPosition() != null
                        && Math.abs(zombie.getPosition().x() - user.getPosition().x()) <= ATTRACT_RANGE
                        && Math.abs(Math.abs(zombie.getPosition().y() - user.getPosition().y()) - 1) < 0.5)
                .min(Comparator.comparingDouble(zombie -> zombie.getPosition().distanceTo(user.getPosition())))
                .orElse(null);
        if (target == null) return;

        target.setPosition(new Position(target.getPosition().x(), user.getPosition().y()));
        user.setInternalTimer(Math.max(0.5, user.getActionInterval()));
    }
}
