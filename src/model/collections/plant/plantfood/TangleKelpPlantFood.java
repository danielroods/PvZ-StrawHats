package model.collections.plant.plantfood;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieRace;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.TileType;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tangle Kelp's Plant Food.
 *
 * Grabs whichever zombie is standing on the plant's own tile (if any) plus up to
 * {@code maxRemoteTargets} more zombies anywhere else in the water (nearest first),
 * dragging every one of them smoothly under the (stationary) water ripple before killing
 * them. Gargantuars are immune, same as the normal attack in
 * {@link model.collections.plant.actstrategy.TangleKelpStrategy}.
 *
 * Visual timeline: "plantfood_on" (short wind-up) -> "plantfood" (the long drag-under,
 * where {@link Zombie#setDragUnderWaterProgress(double)} climbs from 0 to 1 for every
 * grabbed zombie) -> "plantfood_off" (short settle). That three-stage clip plays on the
 * plant's own tile. Every *other* tile holding a grabbed zombie has no real plant on it,
 * so it instead shows a borrowed Tangle Kelp "attack" clip for the same window - see
 * {@link #remoteAttackTiles()} and {@link #remoteAttackElapsed()}, read back by
 * PlantRenderer.
 */
public class TangleKelpPlantFood implements PlantFoodEffect {
    private static final double ON_DURATION_FALLBACK = 0.35;
    private static final double MAIN_DURATION_FALLBACK = 1.5;
    private static final double OFF_DURATION_FALLBACK = 0.35;
    private static final int LETHAL_DAMAGE = 9999;

    private final int maxRemoteTargets;

    private double onDuration = ON_DURATION_FALLBACK;
    private double mainDuration = MAIN_DURATION_FALLBACK;
    private double offDuration = OFF_DURATION_FALLBACK;
    private double elapsed;
    private boolean killed;
    private final List<Zombie> grabbed = new ArrayList<>();
    private final Map<Zombie, Position> remoteTiles = new IdentityHashMap<>();

    public TangleKelpPlantFood(int maxRemoteTargets) {
        this.maxRemoteTargets = Math.max(0, maxRemoteTargets);
    }

    @Override
    public double getDurationSeconds() {
        return onDuration + mainDuration + offDuration;
    }

    @Override
    public boolean drivesActStrategy() {
        // While boosted, the grab/drag/kill sequence is driven entirely by
        // tickDurationEffect below - the normal single-target TangleKelpStrategy sits out.
        return true;
    }

    @Override
    public void reset() {
        elapsed = 0.0;
        killed = false;
        grabbed.clear();
        remoteTiles.clear();
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        reset();

        float onClip = AnimationFactory.clipDurationForDisplayName(plant.getName(), "plantfood_on");
        float mainClip = AnimationFactory.clipDurationForDisplayName(plant.getName(), "plantfood");
        float offClip = AnimationFactory.clipDurationForDisplayName(plant.getName(), "plantfood_off");
        onDuration = onClip > 0f ? onClip : ON_DURATION_FALLBACK;
        mainDuration = mainClip > 0f ? mainClip : MAIN_DURATION_FALLBACK;
        offDuration = offClip > 0f ? offClip : OFF_DURATION_FALLBACK;

        plant.setVisualAnimationState("plantfood_on", onDuration);

        Position center = plant.getPosition();
        if (center == null || session == null) return;

        int maxGrabs = 1 + maxRemoteTargets;

        // The plant's own tile grabs first, if a zombie happens to be standing right on it.
        for (Zombie zombie : session.getZombies()) {
            if (grabbed.size() >= maxGrabs) break;
            if (!isGrabbable(zombie, session)) continue;
            if (isSameTile(zombie.getPosition(), center)) {
                grabbed.add(zombie);
            }
        }

        // Up to maxRemoteTargets more, anywhere else in the water, nearest first.
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (grabbed.contains(zombie) || !isGrabbable(zombie, session)) continue;
            candidates.add(zombie);
        }
        candidates.sort((a, b) -> Double.compare(
                a.getPosition().distanceTo(center), b.getPosition().distanceTo(center)));
        for (Zombie zombie : candidates) {
            if (grabbed.size() >= maxGrabs) break;
            grabbed.add(zombie);
        }

        for (Zombie zombie : grabbed) {
            if (isSameTile(zombie.getPosition(), center)) continue;
            remoteTiles.put(zombie, new Position(zombie.getPosition().x(), zombie.getPosition().y()));
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;

        if (elapsed < onDuration) {
            if (!"plantfood_on".equals(plant.getVisualAnimationState())) {
                plant.setVisualAnimationState("plantfood_on", onDuration - elapsed);
            }
            return;
        }

        if (elapsed < onDuration + mainDuration) {
            if (!"plantfood".equals(plant.getVisualAnimationState())) {
                plant.setVisualAnimationState("plantfood", onDuration + mainDuration - elapsed);
            }
            double dragElapsed = elapsed - onDuration;
            double progress = mainDuration > 0 ? Math.min(1.0, dragElapsed / mainDuration) : 1.0;
            for (Zombie zombie : grabbed) {
                if (zombie == null || !zombie.isAlive()) continue;
                zombie.applyStatus(Zombie.Status.BUTTER, Math.max(0.1, deltaTimeSeconds * 2.0));
                zombie.setDragUnderWaterProgress(progress);
            }
            return;
        }

        if (!killed) {
            killed = true;
            for (Zombie zombie : grabbed) {
                if (zombie == null || !zombie.isAlive()) continue;
                zombie.setDragUnderWaterProgress(1.0);
                zombie.markDragUnderWaterDeath();
                zombie.takeDamage(LETHAL_DAMAGE, plant);
            }
        }
        if (!"plantfood_off".equals(plant.getVisualAnimationState())) {
            plant.setVisualAnimationState("plantfood_off", Math.max(0.05, getDurationSeconds() - elapsed));
        }
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }

    /**
     * Tile positions (excluding the plant's own tile) that should currently show a borrowed
     * Tangle Kelp "attack" clip because a remote zombie is being dragged under there. Empty
     * once the grab/drag window (plantfood_on + plantfood) has passed.
     */
    public List<Position> remoteAttackTiles() {
        if (elapsed >= onDuration + mainDuration) return List.of();
        return new ArrayList<>(remoteTiles.values());
    }

    /** Animation clock for the borrowed "attack" clip drawn at {@link #remoteAttackTiles()}. */
    public double remoteAttackElapsed() {
        return elapsed;
    }

    private boolean isGrabbable(Zombie zombie, GameSession session) {
        if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) return false;
        if (zombie.getRace() == ZombieRace.GARGANTUAR) return false;
        return isOnWaterTile(zombie.getPosition(), session);
    }

    private boolean isSameTile(Position a, Position b) {
        if (a == null || b == null) return false;
        return Math.abs(a.x() - b.x()) < 0.5 && Math.abs(a.y() - b.y()) < 0.5;
    }

    private boolean isOnWaterTile(Position position, GameSession session) {
        if (position == null || session.getEnvironment() == null) return false;
        int row = (int) Math.round(position.y());
        int col = (int) Math.round(position.x());
        Cell cell = session.getEnvironment().getCell(row, col);
        return cell != null && cell.getTile() != null && cell.getTile().type() == TileType.Water;
    }
}