package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class PullAndHeal implements PlantFoodEffect {
    private static final double SWEET_POTATO_SIDE_ROW_RADIUS = 1.0;

    private final double configuredRange;

    public PullAndHeal(double range) {
        this.configuredRange = range;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        Position center = plant.getPosition();
        if (center == null) return;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()
                    || session.isZombieInSandStorm(zombie)
                    || zombie.getPosition() == null) continue;

            Position zombiePos = zombie.getPosition();
            double dy = Math.abs(zombiePos.y() - center.y());

            boolean inArea;
            if (plant.isSweetPotato()) {
                inArea = dy <= SWEET_POTATO_SIDE_ROW_RADIUS;
            } else {
                double width = Math.max(0.5, configuredRange);
                inArea = Math.abs(zombiePos.x() - center.x()) <= width && dy <= 1.0;
            }

            if (!inArea) continue;

            int fromRow = (int) Math.round(zombiePos.y());
            int toRow = (int) Math.round(center.y());
            if (fromRow != toRow) {
                
                
                session.beginSliderRide(zombie, zombiePos.x(), fromRow, toRow);
            } else {
                zombie.setPosition(new Position(zombiePos.x(), center.y()));
            }
            boolean cameFromLeft = zombiePos.x() < center.x();
            zombie.setFacingRight(cameFromLeft);
            if (zombie.getSpeed() != null) {
                double speedX = zombie.getSpeed().x();
                if (Math.abs(speedX) > 0.0001) {
                    double towardSweetPotato = zombiePos.x() < center.x()
                            ? Math.abs(speedX)
                            : -Math.abs(speedX);
                    zombie.setSpeed(new Position(towardSweetPotato, zombie.getSpeed().y()));
                }
            }
            zombie.clearActionAnimationState();
        }

        plant.setHP(plant.getMaxHp());
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
