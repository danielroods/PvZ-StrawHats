package model.projectile.zombie_projectile;

import model.collections.plant.Plant;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.utils.GameSession;

public class SnowballProjectile extends ZombieProjectile {

    public SnowballProjectile(Position startPosition, Position targetPosition, double flightTime, GameSession session) {
        super(startPosition, targetPosition, flightTime, "IceAgeHunter", session);
    }

    @Override
    protected void updateFlightPath(double progress) {
        double currentX = startPosition.x() + (targetPosition.x() - startPosition.x()) * progress;
        double currentY = startPosition.y() + (targetPosition.y() - startPosition.y()) * progress;
        this.setPosition(new Position(currentX, currentY));
    }

    @Override
    protected void onDestinationReached(GameSession session) {
        if (session == null || session.getEnvironment() == null) return;
        int targetRow = (int) Math.round(targetPosition.y());
        int targetCol = (int) Math.round(targetPosition.x());
        Cell targetCell = session.getEnvironment().getCell(targetRow, targetCol);
        if (targetCell == null) return;

        Plant targetPlant = targetCell.getPlant();
        if (targetPlant != null && targetPlant.isAlive()) {
            FrostbiteFreezing.addChillLevel(session, targetPlant);
        }
    }
}
