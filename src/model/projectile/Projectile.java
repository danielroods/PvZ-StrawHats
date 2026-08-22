package model.projectile;

import model.collections.Item;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.obstacles.IceBlock;
import model.pitches.obstacles.Grave;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.projectile.hit.HitEffectStrategy;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class Projectile extends Item {
    private final int damage;
    private final Item target;
    private Plant sourcePlant;
    private boolean isStunning;
    private int assetVariant;
    private double maxTravelDistance;
    private double travelledDistance;
    private Position previousPosition;

    private final MoveStrategy moveStrategy;
    private HitEffectStrategy hitEffectStrategy;
    private final Set<Zombie> hitZombies = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private int remainingHits = Integer.MIN_VALUE;

    private static double SPEED_MULTIPLIER = 1.0;

    private double spawnDelayTicks = 3.9;

    private boolean lobberTargetOnly = false;

    public void setLobberTargetOnly(boolean lobberTargetOnly) {
        this.lobberTargetOnly = lobberTargetOnly;
    }

    public void deflectTowardsPlant(Zombie deflector) {
        if (deflector != null) hitZombies.add(deflector);
        remainingHits = 2;
        setAlive(true);
    }

    public Projectile(Position position, Position velocity, Zombie zombie, int damage, MoveStrategy moveStrategy, HitEffectStrategy hitEffectStrategy) {
        this(zombie, position, velocity, damage, moveStrategy, hitEffectStrategy);
    }

    public Projectile(Plant sourcePlant, Position position, Position velocity, Zombie zombie, int damage,
                      MoveStrategy moveStrategy, HitEffectStrategy hitEffectStrategy) {
        this(position, velocity, zombie, damage, moveStrategy, hitEffectStrategy);
        this.sourcePlant = sourcePlant;
    }

    public Projectile(Item target, Position position, Position velocity, int damage, MoveStrategy moveStrategy, HitEffectStrategy hitEffectStrategy) {
        super(position, 1);
        this.setPosition(position);

        Position scaledVelocity = scaleVelocity(velocity, SPEED_MULTIPLIER);
        this.setSpeed(scaledVelocity);

        this.target = target;
        this.damage = damage;
        this.moveStrategy = moveStrategy;
        this.hitEffectStrategy = hitEffectStrategy;
        this.isStunning = false;

        this.spawnDelayTicks = 3.9;
    }


    public static void setGlobalSpeedMultiplier(double multiplier) {
        SPEED_MULTIPLIER = multiplier;
    }

    public void setSpawnDelayTicks(double ticks) {
        this.spawnDelayTicks = Math.max(0.0, ticks);
    }

    public double getSpawnDelayTicks() {
        return spawnDelayTicks;
    }

    public boolean isSpawning() {
        return spawnDelayTicks > 0;
    }

    public boolean isVisible() {
        return isAlive && spawnDelayTicks <= 0;
    }

    private static Position scaleVelocity(Position v, double factor) {
        if (v == null) return null;
        return new Position(v.x() * factor, v.y() * factor);
    }

    public void setStunning(boolean isStunning) {
        this.isStunning = isStunning;
    }

    public Position getPreviousPosition() {
        return previousPosition == null ? getPosition() : previousPosition;
    }

    public double distanceFromPathTo(Position point) {
        Position end = getPosition();
        Position start = getPreviousPosition();
        if (point == null || end == null || start == null) return Double.MAX_VALUE;

        Position movement = end.sub(start);
        double lengthSquared = movement.dot(movement);
        if (lengthSquared == 0) return end.distanceTo(point);

        double projection = point.sub(start).dot(movement) / lengthSquared;
        double clamped = Math.max(0, Math.min(1, projection));
        return start.add(movement.scale(clamped)).distanceTo(point);
    }

    public double getMaxTravelDistance() {
        return maxTravelDistance;
    }

    public void setMaxTravelDistance(double maxTravelDistance) {
        this.maxTravelDistance = Math.max(0, maxTravelDistance);
    }

    public int getAssetVariant() {
        return assetVariant;
    }

    public void setAssetVariant(int assetVariant) {
        this.assetVariant = Math.max(0, assetVariant);
    }

    @Override
    public void tick() {
        if (!isAlive) return;

        if (spawnDelayTicks > 0) {
            spawnDelayTicks -= 1.0;
            return;
        }

        Position previousPosition = getPosition();
        if (previousPosition == null) {
            setAlive(false);
            return;
        }
        this.previousPosition = previousPosition;

        if (moveStrategy != null) {
            moveStrategy.move(this);
        }

        Position currentPosition = getPosition();
        if (currentPosition == null) {
            setAlive(false);
            return;
        }
        if (maxTravelDistance > 0) {
            travelledDistance += currentPosition.distanceTo(previousPosition);
            if (travelledDistance > maxTravelDistance) {
                setAlive(false);
                return;
            }
        }

        GameSession session = GameSession.peekInstance();
        if (session == null || session.getEnvironment() == null) {
            hitOriginalTarget(previousPosition);
            return;
        }

        if (lobberTargetOnly) {
            // Normal Lobber shots are target-only.
            // Zombie target: ignore every other collision while flying.
            if (target != null) {
                hitOriginalTarget(previousPosition);

                if (!isAlive) return;
                if (target == null || !target.isAlive()) {
                    setAlive(false);
                    return;
                }
            } else {
                // When the normal Lobber has no Zombie target, the existing
                // game logic targets an obstacle (for example a Grave) by
                // passing null as the Item target. Keep the old obstacle
                // damage behaviour, but ONLY on the plant's own row so the
                // projectile cannot hit obstacles on other rows through its arc.
                GraveCollision graveCollision = findFirstGraveCollisionOnRow(
                        session, previousPosition, currentPosition);
                if (graveCollision != null) {
                    session.damageGrave(graveCollision.cell(), getEffectiveDamage());
                    setAlive(false);
                    return;
                }

                IceCollision iceCollision = findFirstIceBlockCollisionOnRow(
                        session, previousPosition, currentPosition);
                if (iceCollision != null) {
                    boolean fireDamage = (hitEffectStrategy != null && hitEffectStrategy.isFireDamage())
                            || (sourcePlant != null && sourcePlant.getTags() != null
                            && sourcePlant.getTags().contains(model.collections.plant.PlantTag.FIRE));
                    FrostbiteFreezing.damageIce(iceCollision.cell(), getEffectiveDamage(), fireDamage);
                    setAlive(false);
                    return;
                }

                PushableStructure structure = findFirstStructureCollisionOnRow(
                        session, previousPosition, currentPosition);
                if (structure != null) {
                    structure.takeDamage(getEffectiveDamage(), sourcePlant, session);
                    setAlive(false);
                    return;
                }
            }

            if (isOutsideLawnForLobber(session, currentPosition)) {
                setAlive(false);
            }
            return;
        }

        GraveCollision graveCollision = findFirstGraveCollision(session, previousPosition, currentPosition);
        if (graveCollision != null) {
            session.damageGrave(graveCollision.cell(), getEffectiveDamage());
            setAlive(false);
            return;
        }

        IceCollision iceCollision = findFirstIceBlockCollision(session, previousPosition, currentPosition);
        if (iceCollision != null) {
            boolean fireDamage = (hitEffectStrategy != null && hitEffectStrategy.isFireDamage())
                    || (sourcePlant != null && sourcePlant.getTags() != null
                    && sourcePlant.getTags().contains(model.collections.plant.PlantTag.FIRE));
            FrostbiteFreezing.damageIce(iceCollision.cell(), getEffectiveDamage(), fireDamage);
            setAlive(false);
            return;
        }

        PushableStructure structure = findFirstStructureCollision(session, previousPosition, currentPosition);
        if (structure != null) {
            structure.takeDamage(getEffectiveDamage(), sourcePlant, session);
            setAlive(false);
            return;
        }

        List<ZombieHit> collisions = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (!isValidTarget(zombie) || hitZombies.contains(zombie)) continue;
            double projection = collisionProjection(zombie.getPosition(), previousPosition, currentPosition);
            if (projection >= 0) collisions.add(new ZombieHit(zombie, projection));
        }
        collisions.sort(Comparator.comparingDouble(ZombieHit::projection));

        if (remainingHits == Integer.MIN_VALUE) {
            remainingHits = hitEffectStrategy == null ? 1 : hitEffectStrategy.getPierceCount();
            if (remainingHits == 0) remainingHits = 1;
        }

        for (ZombieHit collision : collisions) {
            hitZombie(collision.zombie(), session);
            hitZombies.add(collision.zombie());
            if (remainingHits > 0) remainingHits--;
            if (remainingHits == 0) {
                setAlive(false);
                break;
            }
        }

        if (isOutsideLawn(session, currentPosition)) setAlive(false);
    }

    private void hitOriginalTarget(Position previousPosition) {
        if (target == null || !target.isAlive()) return;
        Position targetPosition = resolveTargetPosition(target);
        Position currentPosition = getPosition();
        if (targetPosition == null || currentPosition == null) return;
        if (collisionProjection(targetPosition, previousPosition, currentPosition) < 0) return;

        if (target instanceof Zombie zombie) {
            zombie.takeDamage(getEffectiveDamage(), this);
            if (hitEffectStrategy != null) hitEffectStrategy.apply(zombie);
        } else {
            target.takeDamage(getEffectiveDamage());
        }
        setAlive(false);
    }

    private void hitZombie(Zombie primary, GameSession session) {
        applyDamageAndEffect(primary);

        int areaLength = hitEffectStrategy == null ? 1 : hitEffectStrategy.getAreaLength();
        double radius = Math.max(0, (areaLength - 1) / 2.0);
        if (radius <= 0 || primary.getPosition() == null) return;

        Position center = primary.getPosition();
        for (Zombie zombie : session.getZombies()) {
            if (zombie == primary || !isValidTarget(zombie) || zombie.getPosition() == null) continue;
            if (Math.abs(zombie.getPosition().x() - center.x()) <= radius
                    && Math.abs(zombie.getPosition().y() - center.y()) <= radius) {
                applyDamageAndEffect(zombie);
            }
        }
    }

    private void applyDamageAndEffect(Zombie zombie) {
        int effectiveDamage = getEffectiveDamage();
        if (hitEffectStrategy != null && hitEffectStrategy.bypassesArmor()) {
            zombie.takeDamage(effectiveDamage, true);
        } else {
            zombie.takeDamage(effectiveDamage, this);
        }
        if (hitEffectStrategy != null && zombie.isAlive()) hitEffectStrategy.apply(zombie);
        if (isStunning && zombie.isAlive()) zombie.applyStatus(Zombie.Status.BUTTER, 1.0);

        if (hitEffectStrategy != null && hitEffectStrategy.getKnockbackDistance() != 0 && zombie.getPosition() != null) {
            zombie.setPosition(new Position(
                    zombie.getPosition().x() + hitEffectStrategy.getKnockbackDistance(),
                    zombie.getPosition().y()
            ));
        }
    }

    private int getEffectiveDamage() {
        double multiplier = hitEffectStrategy == null ? 1.0 : hitEffectStrategy.getDamageMultiplier();
        return Math.max(0, (int) Math.round(damage * multiplier));
    }

    private boolean isValidTarget(Zombie zombie) {
        return zombie != null && zombie.isAlive() && !zombie.isHypnotized() && zombie.getPosition() != null;
    }

    private record GraveCollision(Cell cell, double projection) {}

    private GraveCollision findFirstGraveCollision(GameSession session, Position start, Position end) {
        GraveCollision best = null;
        double bestProjection = Double.MAX_VALUE;
        for (int row = 0; row < session.getEnvironment().getRows(); row++) {
            for (int col = 0; col < session.getEnvironment().getCols(); col++) {
                Cell cell = session.getEnvironment().getCell(row, col);
                if (cell == null || !(cell.getObstacle() instanceof Grave)) continue;
                double projection = collisionProjection(new Position(col, row), start, end);
                if (projection >= 0 && projection < bestProjection) {
                    bestProjection = projection;
                    best = new GraveCollision(cell, projection);
                }
            }
        }
        return best;
    }

    private record IceCollision(Cell cell, double projection) {}

    private IceCollision findFirstIceBlockCollision(GameSession session, Position start, Position end) {
        IceCollision best = null;
        double bestProjection = Double.MAX_VALUE;
        for (int row = 0; row < session.getEnvironment().getRows(); row++) {
            for (int col = 0; col < session.getEnvironment().getCols(); col++) {
                Cell cell = session.getEnvironment().getCell(row, col);
                if (cell == null || !(cell.getObstacle() instanceof IceBlock)) continue;
                double projection = collisionProjection(new Position(col, row), start, end);
                if (projection >= 0 && projection < bestProjection) {
                    bestProjection = projection;
                    best = new IceCollision(cell, projection);
                }
            }
        }
        return best;
    }

    private PushableStructure findFirstStructureCollision(GameSession session, Position start, Position end) {
        PushableStructure best = null;
        double bestProjection = Double.MAX_VALUE;
        Set<PushableStructure> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        for (int row = 0; row < session.getEnvironment().getRows(); row++) {
            for (int col = 0; col < session.getEnvironment().getCols(); col++) {
                Cell cell = session.getEnvironment().getCell(row, col);
                PushableStructure structure = cell == null ? null : cell.getStructure();
                if (structure == null || !structure.isAlive() || !seen.add(structure)) continue;
                double projection = collisionProjection(structure.getPosition(), start, end);
                if (projection >= 0 && projection < bestProjection) {
                    bestProjection = projection;
                    best = structure;
                }
            }
        }
        return best;
    }

    private double collisionProjection(Position targetPosition, Position start, Position end) {
        if (targetPosition == null || start == null || end == null) return -1;
        Position movement = end.sub(start);
        double lengthSquared = movement.dot(movement);
        if (lengthSquared == 0) return end.distanceTo(targetPosition) <= 0.5 ? 0 : -1;

        double projection = targetPosition.sub(start).dot(movement) / lengthSquared;
        if (projection < 0 || projection > 1) return -1;
        Position closestPoint = start.add(movement.scale(projection));
        return closestPoint.distanceTo(targetPosition) <= 0.5 ? projection : -1;
    }

    private int getLobberSourceRow() {
        if (sourcePlant == null || sourcePlant.getPosition() == null) return Integer.MIN_VALUE;
        return (int) Math.round(sourcePlant.getPosition().y());
    }

    private GraveCollision findFirstGraveCollisionOnRow(GameSession session, Position start, Position end) {
        int row = getLobberSourceRow();
        if (row == Integer.MIN_VALUE) return null;
        GraveCollision best = null;
        double bestProjection = Double.MAX_VALUE;
        for (int col = 0; col < session.getEnvironment().getCols(); col++) {
            Cell cell = session.getEnvironment().getCell(row, col);
            if (cell == null || !(cell.getObstacle() instanceof Grave)) continue;
            double projection = collisionProjection(new Position(col, row), start, end);
            if (projection >= 0 && projection < bestProjection) {
                bestProjection = projection;
                best = new GraveCollision(cell, projection);
            }
        }
        return best;
    }

    private IceCollision findFirstIceBlockCollisionOnRow(GameSession session, Position start, Position end) {
        int row = getLobberSourceRow();
        if (row == Integer.MIN_VALUE) return null;
        IceCollision best = null;
        double bestProjection = Double.MAX_VALUE;
        for (int col = 0; col < session.getEnvironment().getCols(); col++) {
            Cell cell = session.getEnvironment().getCell(row, col);
            if (cell == null || !(cell.getObstacle() instanceof IceBlock)) continue;
            double projection = collisionProjection(new Position(col, row), start, end);
            if (projection >= 0 && projection < bestProjection) {
                bestProjection = projection;
                best = new IceCollision(cell, projection);
            }
        }
        return best;
    }

    private PushableStructure findFirstStructureCollisionOnRow(GameSession session, Position start, Position end) {
        int row = getLobberSourceRow();
        if (row == Integer.MIN_VALUE) return null;
        PushableStructure best = null;
        double bestProjection = Double.MAX_VALUE;
        Set<PushableStructure> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (int col = 0; col < session.getEnvironment().getCols(); col++) {
            Cell cell = session.getEnvironment().getCell(row, col);
            PushableStructure structure = cell == null ? null : cell.getStructure();
            if (structure == null || !structure.isAlive() || !seen.add(structure)) continue;
            double projection = collisionProjection(structure.getPosition(), start, end);
            if (projection >= 0 && projection < bestProjection) {
                bestProjection = projection;
                best = structure;
            }
        }
        return best;
    }

    private boolean isOutsideLawn(GameSession session, Position position) {
        return position.x() < -1 || position.x() > session.getEnvironment().getCols()
                || position.y() < -1 || position.y() > session.getEnvironment().getRows();
    }

    private Position resolveTargetPosition(Item target) {
        if (target instanceof Zombie zombie) return zombie.getPosition();
        if (target instanceof Plant plant) {
            var loc = plant.getLocation();
            return loc == null ? null : Position.of(loc.x(), loc.y());
        }
        return target.getPosition();
    }

    public void setHitEffectStrategy(HitEffectStrategy strategy) {
        this.hitEffectStrategy = strategy;
        this.remainingHits = Integer.MIN_VALUE;
    }

    public HitEffectStrategy getHitEffectStrategy() { return this.hitEffectStrategy; }
    public Object getMoveStrategy() { return moveStrategy; }
    public int getDamage() { return damage; }
    public Plant getSourcePlant() { return sourcePlant; }

    private record ZombieHit(Zombie zombie, double projection) {}

    private boolean isOutsideLawnForLobber(GameSession session, Position position) {
        return position.x() < -1
                || position.x() > session.getEnvironment().getCols()
                || position.y() < -2.5
                || position.y() > session.getEnvironment().getRows() + 1;
    }
}