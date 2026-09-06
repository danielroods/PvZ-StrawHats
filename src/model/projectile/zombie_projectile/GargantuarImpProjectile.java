package model.projectile.zombie_projectile;

import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.collections.zombie.ZombieSequence;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.utils.GameSession;

public class GargantuarImpProjectile extends ZombieProjectile {

    public static final String CLIP_RISING = "fall";
    public static final String CLIP_FALLING = "drop";
    public static final String CLIP_IMPACT = "impact";
    public static final String CLIP_BALL_IDLE = "idle_ball2";
    public static final String CLIP_TRANSITION = "transition";

    private static final double FALLBACK_IMPACT_SECONDS = 0.5;
    private static final double BALL_IDLE_SECONDS = 1.6;
    private static final double FALLBACK_TRANSITION_SECONDS = 0.8;

    private final double apex;
    private final int targetRow;
    private final String impAlias;
    private final boolean facingRight;
    private double flightProgress;

    public GargantuarImpProjectile(Position startPosition, Position targetPosition, double flightTime,
                                   double apex, int targetRow, String impAlias, GameSession session) {
        this(startPosition, targetPosition, flightTime, apex, targetRow, impAlias, true, session);
    }

    public GargantuarImpProjectile(Position startPosition, Position targetPosition, double flightTime,
                                   double apex, int targetRow, String impAlias, boolean facingRight, GameSession session) {
        super(startPosition, targetPosition, flightTime, "Gargantuar", session);
        this.apex = apex;
        this.targetRow = targetRow;
        this.impAlias = impAlias;
        this.facingRight = facingRight;
    }

    public String getImpAlias() { return impAlias; }
    public boolean isFacingRight() { return facingRight; }

    public double getFlightProgress() { return flightProgress; }

    public boolean isRising() { return flightProgress < 0.5; }

    @Override
    protected void updateFlightPath(double progress) {
        this.flightProgress = progress;
        double currentX = startPosition.x() + (targetPosition.x() - startPosition.x()) * progress;
        double currentY = startPosition.y() + (targetPosition.y() - startPosition.y()) * progress;

        double visualY = currentY - (apex / 100.0) * Math.sin(progress * Math.PI);

        this.setPosition(new Position(currentX, visualY));
    }

    @Override
    protected void onDestinationReached(GameSession session) {
        if (session == null || session.getEnvironment() == null) return;

        int targetCol = (int) (targetPosition.x());

        Cell targetCell = session.getEnvironment().getCell(targetRow, targetCol);
        if (targetCell == null) return;

        Zombie imp = ZombieFactory.create(impAlias, targetRow, targetCol);
        playLandingSequence(imp);
        session.spawnZombie(imp);
    }

    private void playLandingSequence(Zombie imp) {
        String pam = ZombieAnimationRegistry.pathForCurrentSeason(imp.getAlias());
        if (AnimationFactory.exactClipDurationForPath(pam, CLIP_IMPACT) <= 0f) return;

        imp.playSequence(new ZombieSequence()
                .add(CLIP_IMPACT, clipSeconds(pam, CLIP_IMPACT, FALLBACK_IMPACT_SECONDS))
                .add(CLIP_BALL_IDLE, BALL_IDLE_SECONDS, true)
                .add(CLIP_TRANSITION, clipSeconds(pam, CLIP_TRANSITION, FALLBACK_TRANSITION_SECONDS)));
    }

    private static double clipSeconds(String pamPath, String clip, double fallback) {
        float duration = AnimationFactory.exactClipDurationForPath(pamPath, clip);
        return duration > 0f ? duration : fallback;
    }
}