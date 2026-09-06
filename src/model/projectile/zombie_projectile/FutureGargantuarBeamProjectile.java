package model.projectile.zombie_projectile;

import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class FutureGargantuarBeamProjectile extends ZombieProjectile {

    public FutureGargantuarBeamProjectile(Position sourcePosition, Position targetPosition,
                                          double visibleDuration, GameSession session) {
        super(sourcePosition, targetPosition, Math.max(0.05, visibleDuration),
                "ZombieFutureGargantuar", session);
    }

    public Position getBeamTargetPosition() {
        return targetPosition;
    }

    @Override
    protected void updateFlightPath(double progress) {
        setPosition(startPosition);
    }

    @Override
    protected void onDestinationReached(GameSession session) {
    }
}
