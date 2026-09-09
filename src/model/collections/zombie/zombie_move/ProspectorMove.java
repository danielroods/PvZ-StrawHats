package model.collections.zombie.zombie_move;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class ProspectorMove implements MoveBehavior {
    private enum DynamiteState {
        WALKING_LEFT,
        AIRBORNE_LAUNCH,
        GROUND_WALK
    }

    private static final String FLY_ANIMATION_STATE = "fly";
    
    
    
    private static final double FLY_SPEED = 5.0;

    private DynamiteState phase = DynamiteState.WALKING_LEFT;
    private double dynamiteTimer = 10.0;
    private double landingX = 0.0;
    private boolean dynamiteExtinguished = false;

    
    
    
    private boolean facingFlippedForFlight = false;

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
                    beginBlastLaunch(zombie, pos);
                }
            }

            case AIRBORNE_LAUNCH -> {
                
                
                
                double nextX = Math.max(landingX, pos.x() - FLY_SPEED * deltaTime);
                zombie.setPosition(new Position(nextX, pos.y()));

                if (nextX <= landingX) {
                    land(zombie);
                }
            }

            case GROUND_WALK -> {
                
                
                
                Position speed = zombie.getSpeed();
                if (speed == null) return;
                Position nextPos = new Position(
                        pos.x() + speed.x() * deltaTime,
                        pos.y() + speed.y() * deltaTime
                );
                nextPos = applySliderRedirect(zombie, pos, nextPos, session);
                zombie.setPosition(nextPos);

                if (session != null && session.getLawn() != null
                        && nextPos.x() >= session.getLawn().getCols()) {
                    zombie.setHp(0);
                }
            }
        }
    }

    private void beginBlastLaunch(Zombie zombie, Position pos) {
        phase = DynamiteState.AIRBORNE_LAUNCH;

        
        
        landingX = 0.0;

        
        
        zombie.setIgnoreTargetAcquisition(true);

        zombie.setFacingRight(!zombie.isFacingRight());
        facingFlippedForFlight = true;

        
        
        
        zombie.setActionAnimationState(FLY_ANIMATION_STATE, 0, true);

        if (pos.x() <= landingX) {
            land(zombie);
        }
    }

    private void land(Zombie zombie) {
        zombie.setPosition(new Position(landingX, zombie.getPosition().y()));
        zombie.clearActionAnimationState();
        zombie.setIgnoreTargetAcquisition(false);

        
        
        
        facingFlippedForFlight = false;

        Position currentSpeed = zombie.getSpeed();
        double speedMagnitude = (currentSpeed != null) ? Math.abs(currentSpeed.x()) : 1.0;
        
        
        zombie.setSpeed(new Position(speedMagnitude, 0));

        phase = DynamiteState.GROUND_WALK;
    }
}