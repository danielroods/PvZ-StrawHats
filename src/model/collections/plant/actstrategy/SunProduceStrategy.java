package model.collections.plant.actstrategy;

import model.collections.item.GroundSun;
import model.collections.plant.AbilityType;
import model.collections.plant.Plant;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import service.GameClock;
import view.GeneralPrinter;

public class SunProduceStrategy implements ActStrategy {
    private static final double DOUBLE_SUN_PROBABILITY = 0.5;

    @Override
    public void act(Plant user, GameSession session) {
        if (!GameClock.isZero(user.getIntervalTimer())) return;

        Position location = user.getLocation();
        if (location == null) return;

        int sunValue = Math.max(0, (int) user.getAbilityValue());
        if (user.hasSpecialUpgrade("DOUBLE_SUN_CHANCE")
                && Math.random() < DOUBLE_SUN_PROBABILITY) {
            sunValue *= 2;
        }
        if (user.getAbilityType() == AbilityType.INSTANT_SUN_BURST) {
            session.addSun(sunValue);
            GeneralPrinter.print("plant " + user.getName() + " produced " + sunValue + " suns.");
            user.setAlive(false);
            return;
        }

        boolean sunAlreadyExists = session.getItems().stream()
                .anyMatch(item -> item instanceof GroundSun sun
                        && !sun.isCollected()
                        && item.isAlive()
                        && item.getPosition() != null
                        && item.getPosition().x() == location.x()
                        && item.getPosition().y() == location.y());

        if (sunAlreadyExists) return;

        session.getItems().add(new GroundSun(location, sunValue));
        GeneralPrinter.print("plant " + user.getName() + " produced a sun at ("
                + ((int) location.x() + 1) + ", " + ((int) location.y() + 1) + ").");

        user.setInternalTimer(user.getActionInterval());
    }
}
