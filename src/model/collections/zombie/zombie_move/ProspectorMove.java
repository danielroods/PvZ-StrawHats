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
    // Tiles per second while airborne - a constant speed rather than a fixed
    // total flight time, so the zombie always actually reaches the landing
    // tile no matter how far away it started the blast from.
    private static final double FLY_SPEED = 5.0;

    private DynamiteState phase = DynamiteState.WALKING_LEFT;
    private double dynamiteTimer = 10.0;
    private double landingX = 0.0;
    private boolean dynamiteExtinguished = false;

    // The leap plays its own "fly" clip, flipped relative to the zombie's normal
    // walking sprite - same idea as the flip a hypnotized zombie's sprite gets -
    // then flips back once it lands and resumes walking.
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
                // Sailing to the landing tile: no slider redirects, no target
                // acquisition (handled via ignoreTargetAcquisition), just a
                // straight flight all the way to the first column.
                double nextX = Math.max(landingX, pos.x() - FLY_SPEED * deltaTime);
                zombie.setPosition(new Position(nextX, pos.y()));

                if (nextX <= landingX) {
                    land(zombie);
                }
            }

            case GROUND_WALK -> {
                // Having landed at the first column, it now walks back the
                // other way (rightward), eating every plant it runs into
                // along the way, until it walks off the far edge of the lawn.
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

        // Lands on the first column on the left - the last tile of the row
        // before the house (never the house/"brain" tile itself).
        landingX = 0.0;

        // Sail clean over any plants in the way instead of stopping to eat one
        // partway through the flight - that was cutting the flight short.
        zombie.setIgnoreTargetAcquisition(true);

        zombie.setFacingRight(!zombie.isFacingRight());
        facingFlippedForFlight = true;

        // Loops for as long as the flight actually takes (duration 0 = persist
        // until explicitly cleared) instead of a fixed one-shot beat that could
        // run out and freeze before the zombie has actually landed.
        zombie.setActionAnimationState(FLY_ANIMATION_STATE, 0, true);

        if (pos.x() <= landingX) {
            land(zombie);
        }
    }

    private void land(Zombie zombie) {
        zombie.setPosition(new Position(landingX, zombie.getPosition().y()));
        zombie.clearActionAnimationState();
        zombie.setIgnoreTargetAcquisition(false);

        // No need to flip back: it keeps moving in the same direction it was
        // just flying in (rightward, back out through the lawn), so the
        // flipped-during-flight facing is already the correct one to keep.
        facingFlippedForFlight = false;

        Position currentSpeed = zombie.getSpeed();
        double speedMagnitude = (currentSpeed != null) ? Math.abs(currentSpeed.x()) : 1.0;
        // Now walks the other way: rightward, back out through the lawn,
        // eating whatever it passes.
        zombie.setSpeed(new Position(speedMagnitude, 0));

        phase = DynamiteState.GROUND_WALK;
    }
}