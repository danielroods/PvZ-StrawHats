package model.collections.zombie.zombie_effect;

import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieSequence;
import model.match_mechanisms.vector.Position;
import model.projectile.zombie_projectile.GargantuarImpProjectile;
import model.utils.GameSession;

public class GigantorImpChucker implements ZombieEffectStatus {
    private final double launchThresholdPercent;
    private final double projectileArcApex;
    private final double airTravelDuration;
    private final int landingGridCol;
    private final double thresholdSpawnX;
    private final String impCharacterAlias;

    private boolean impLaunched = false;

    public GigantorImpChucker(double launchThreshold, double arcApex, double flightDuration,
                              int targetCol, double limitMinX, String impName) {
        this.launchThresholdPercent = launchThreshold;
        this.projectileArcApex = arcApex;
        this.airTravelDuration = flightDuration;
        this.landingGridCol = targetCol;
        this.thresholdSpawnX = limitMinX;
        this.impCharacterAlias = impName;
    }


    public static final String CLIP_FIRE = "fire";
    public static final String CLIP_CANNON_FIRE = "cannon_fire";

    private static final double FALLBACK_FIRE_SECONDS = 0.7;
    private static final double FALLBACK_CANNON_FIRE_SECONDS = 0.7;

    @Override
    public void applyTickEffect(Zombie host, GameSession session) {
        if (impLaunched || !host.isAlive() || host.getMaxHp() <= 0 || host.getPosition() == null) {
            return;
        }
        if (host.isSequenceActive()) return;

        double healthRatio = (double) host.getHp() / host.getMaxHp();
        if (healthRatio > launchThresholdPercent) return;

        impLaunched = true;
        String pam = ZombieAnimationRegistry.pathForCurrentSeason(host.getAlias());
        host.playSequence(new ZombieSequence()
                .add(CLIP_FIRE, clipSeconds(pam, CLIP_FIRE, FALLBACK_FIRE_SECONDS))
                .add(CLIP_CANNON_FIRE,
                        clipSeconds(pam, CLIP_CANNON_FIRE, FALLBACK_CANNON_FIRE_SECONDS), false,
                        this::executeImpThrow));
    }

    private static double clipSeconds(String pamPath, String clip, double fallback) {
        float duration = AnimationFactory.exactClipDurationForPath(pamPath, clip);
        return duration > 0f ? duration : fallback;
    }

    private void executeImpThrow(Zombie launcher, GameSession session) {
        int trackRow = (int) launcher.getPosition().y();
        if (session.getEnvironment() == null) return;

        int totalGridCols = session.getEnvironment().getCols();
        int correctedCol = Math.min(landingGridCol, totalGridCols - 1);

        Position origin = launcher.getPosition();
        Position destination = new Position(correctedCol, trackRow);

        session.addZombieProjectile(new GargantuarImpProjectile(
                origin, destination, airTravelDuration, projectileArcApex, trackRow,
                impCharacterAlias, launcher.isFacingRight(), session
        ));
        view.GeneralPrinter.print("ZombieGargantuar threw an Imp to column " + (correctedCol + 1)
                + " in lane " + (trackRow + 1) + ".");
    }
}