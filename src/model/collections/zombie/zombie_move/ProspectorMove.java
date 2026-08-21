package model.collections.zombie.zombie_move;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class ProspectorMove implements MoveBehavior {
    private enum DynamiteState {
        WALKING_LEFT,
        AIRBORNE_LAUNCH,
        REVERSE_WALK
    }

    private DynamiteState phase = DynamiteState.WALKING_LEFT;
    private double dynamiteTimer = 10.0;
    private double flightDuration = 1.5;
    private double flightSpeedX = 0.0;
    private boolean dynamiteExtinguished = false;

    public void extinguishDynamite() {
        if (phase == DynamiteState.WALKING_LEFT) {
            dynamiteExtinguished = true;
        }
    }

    public void litDynamite() {
        if (phase == DynamiteState.WALKING_LEFT) {
            dynamiteExtinguished = false;
        }
    }

    @Override
    public void move(Zombie zombie, double deltaTime, GameSession session) {
        if (dynamiteExtinguished) {
            new NormalWalk().move(zombie, deltaTime, session);
            return;
        }

        Position pos = zombie.getPosition();
        if (pos == null) return;

        switch (phase) {
            case WALKING_LEFT -> {
                Position speed = zombie.getSpeed();
                if (speed == null) return;
                Position nextPos = new Position(
                        pos.x() + speed.x() * deltaTime,
                        pos.y() + speed.y() * deltaTime
                );
                nextPos = applySliderRedirect(zombie, pos, nextPos, session);
                zombie.setPosition(nextPos);

                dynamiteTimer -= deltaTime;
                if (dynamiteTimer <= 0) {
                    phase = DynamiteState.AIRBORNE_LAUNCH;
                    if (pos.x() > 0) {
                        flightSpeedX = pos.x() / flightDuration;
                    }
                }

            }

            case AIRBORNE_LAUNCH -> {
                double nextX = pos.x() - (flightSpeedX * deltaTime);
                zombie.setPosition(new Position(Math.max(0, nextX), pos.y()));

                flightDuration -= deltaTime;
                if (flightDuration <= 0 || zombie.getPosition().x() <= 0) {
                    zombie.setPosition(new Position(0, pos.y()));
                    phase = DynamiteState.REVERSE_WALK;

                    Position currentSpeed = zombie.getSpeed();
                    double speedMagnitude = (currentSpeed != null) ? Math.abs(currentSpeed.x()) : 1.0;
                    zombie.setSpeed(new Position(speedMagnitude, 0));
                }
            }

            case REVERSE_WALK -> {
                Position speed = zombie.getSpeed();
                if (speed == null) return;
                Position nextPos = new Position(
                        pos.x() + speed.x() * deltaTime,
                        pos.y() + speed.y() * deltaTime
                );
                nextPos = applySliderRedirect(zombie, pos, nextPos, session);
                zombie.setPosition(nextPos);
                if (session.getLawn() != null && nextPos.x() >= session.getLawn().getCols()) {
                    zombie.setHp(0);
                }
            }
        }
    }
}
