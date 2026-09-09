package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.Comparator;

public class WallNutStrategy implements ActStrategy {
    private static final double CONTACT_RADIUS = 0.7;
    private static final double SWEET_POTATO_FRONT_RANGE = 4.05;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.isGarlic()) return;
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
        Position center = user.getPosition();
        if (center == null) return;

        int redirected = 0;
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()
                    || session.isZombieInSandStorm(zombie)
                    || !isInSweetPotatoFrontStrip(zombie, center)) continue;

            Position zombiePos = zombie.getPosition();
            int fromRow = (int) Math.round(zombiePos.y());
            int toRow = (int) Math.round(center.y());
            if (fromRow != toRow) {
                
                
                session.beginSliderRide(zombie, zombiePos.x(), fromRow, toRow);
            } else {
                zombie.setPosition(new Position(zombiePos.x(), center.y()));
            }
            zombie.setFacingRight(false);
            if (zombie.getSpeed() != null && zombie.getSpeed().x() > 0) {
                zombie.setSpeed(new Position(-Math.abs(zombie.getSpeed().x()), zombie.getSpeed().y()));
            }
            zombie.clearActionAnimationState();
            redirected++;
        }

        if (redirected > 0) {
            user.setInternalTimer(Math.max(0.35, user.getActionInterval()));
        }
    }

    private boolean isInSweetPotatoFrontStrip(Zombie zombie, Position center) {
        Position pos = zombie.getPosition();
        if (pos == null) return false;

        double dx = pos.x() - center.x();
        double dy = pos.y() - center.y();

        return dx >= 0.0 && dx <= SWEET_POTATO_FRONT_RANGE
                && Math.abs(dy) >= 0.5
                && Math.abs(dy) <= 1.5;
    }
}