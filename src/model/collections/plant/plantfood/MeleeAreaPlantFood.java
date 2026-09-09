package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.targeting.PlantTarget;
import model.projectile.targeting.TargetFinder;
import model.utils.GameSession;

import java.util.List;

public class MeleeAreaPlantFood implements PlantFoodEffect {

    /** Per-plant tuning for one melee superpower. */
    public record Profile(double reachX, double reachY, int hits, double activeSeconds,
                          int damagePerHit, Zombie.Status status, double statusSeconds,
                          double knockbackTiles, String onState, String loopState,
                          String offState) {
    }

    private final Profile profile;
    private PlantFoodClipSequence clips;
    private double elapsed = 0.0;
    private int landed = 0;
    private double duration;

    public MeleeAreaPlantFood(Profile profile) {
        this.profile = profile;
        this.duration = profile.activeSeconds();
    }

    @Override
    public double getDurationSeconds() {
        return duration;
    }

    @Override
    public boolean drivesActStrategy() {
        return true;
    }

    @Override
    public void reset() {
        elapsed = 0.0;
        landed = 0;
        if (clips != null) clips.reset();
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        clips = PlantFoodClipSequence.forPlant(plant, profile.onState(), profile.loopState(),
                profile.offState(), profile.activeSeconds());
        duration = clips.totalDuration();
        clips.apply(plant);
       if (clips.introDuration() <= 0) {
            landed = 1;
            strike(plant, session);
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        if (clips != null) clips.advance(plant, deltaTimeSeconds);

        double intro = clips == null ? 0.0 : clips.introDuration();
        double intoActive = elapsed - intro;
        if (intoActive < 0) return;

        int expected = expectedHits(intoActive);
        GameSession session = GameSession.peekInstance();
        while (landed < expected) {
            landed++;
            strike(plant, session);
        }
    }

    private int expectedHits(double intoActive) {
        int hits = Math.max(1, profile.hits());
        double active = Math.max(0.0001, profile.activeSeconds());
        double step = active / hits;
        return Math.min(hits, (int) Math.floor(intoActive / step) + 1);
    }

    private void strike(Plant plant, GameSession session) {
        Position center = plant.getPosition();
        if (center == null || session == null) return;

        boolean fire = profile.status() == Zombie.Status.FIRED;
        for (PlantTarget target : targets(plant, session, center)) {
            Zombie zombie = target.getZombie();
            if (zombie == null) {
                target.takeDamage(profile.damagePerHit(), plant, session, fire);
                continue;
            }

            zombie.takeDamage(profile.damagePerHit(), plant);
            if (!zombie.isAlive()) continue;
            if (profile.status() != null) {
                zombie.applyStatus(profile.status(), profile.statusSeconds());
            }
            if (profile.knockbackTiles() > 0) {
                Position at = zombie.getPosition();
                double direction = Math.signum(at.x() - center.x());
                if (direction == 0) direction = 1;
                zombie.startKnockback(direction * profile.knockbackTiles(), 0.25);
            }
        }
    }

    private List<PlantTarget> targets(Plant plant, GameSession session, Position center) {
        return TargetFinder.allWithin(plant, session,
                TargetFinder.box(center, profile.reachX(), profile.reachY()), false);
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
