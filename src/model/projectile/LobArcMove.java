package model.projectile;

import model.match_mechanisms.vector.Position;

public class LobArcMove implements MoveStrategy {

    private static final double MIN_SPAN = 0.05;

    private final double startX;
    private final double startY;
    private final double endX;
    private final double endY;
    private final double peakHeight;
    private final double horizontalSpeed;

    private double travelledX;
    private boolean landed;

    public LobArcMove(double startX, double startY, double endX, double endY,
                      double peakHeight, double horizontalSpeed) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.peakHeight = peakHeight;
        this.horizontalSpeed = Math.max(MIN_SPAN, horizontalSpeed);
    }

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }
    public double getPeakHeight() { return peakHeight; }
    public double getHorizontalSpeed() { return horizontalSpeed; }
    public double getTravelledX() { return travelledX; }
    public boolean hasLanded() { return landed; }

    public void setTravelledX(double travelledX) {
        this.travelledX = Math.max(0, travelledX);
    }

    public double getProgress() {
        double span = Math.max(MIN_SPAN, endX - startX);
        return Math.max(0.0, Math.min(1.0, travelledX / span));
    }

    public double getFlightSeconds() {
        return Math.max(MIN_SPAN, endX - startX) / horizontalSpeed;
    }

    @Override
    public void move(Projectile projectile, double deltaSeconds) {
        travelledX += horizontalSpeed * deltaSeconds;

        double span = Math.max(MIN_SPAN, endX - startX);
        double progress = travelledX / span;
        if (progress >= 1.0) {
            progress = 1.0;
            travelledX = span;
            landed = true;
        }

        Position previous = projectile.getPosition();
        Position next = pointAt(progress, Math.min(1.0, travelledX / span));
        projectile.setPosition(next);

        if (previous != null && deltaSeconds > 0) {
            projectile.setSpeed(Position.of((next.x() - previous.x()) / deltaSeconds,
                    (next.y() - previous.y()) / deltaSeconds));
        }
    }

    public void applyTo(Projectile projectile) {
        double progress = getProgress();
        landed = progress >= 1.0;
        projectile.setPosition(pointAt(progress, progress));
    }

    private Position pointAt(double progress, double clampedProgress) {
        double span = Math.max(MIN_SPAN, endX - startX);
        double x = startX + clampedProgress * span;
        double baseY = startY + progress * (endY - startY);
        double arcOffset = -4.0 * peakHeight * progress * (1.0 - progress);
        return Position.of(x, baseY + arcOffset);
    }
}
