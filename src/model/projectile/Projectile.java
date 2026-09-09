package model.projectile;

import model.collections.Item;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.obstacles.Grave;
import model.projectile.hit.HitEffectStrategy;
import model.projectile.targeting.PlantTarget;
import model.utils.GameSession;
import service.GameClock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class Projectile extends Item {

    public static final double DEFAULT_SPAWN_DELAY_SECONDS = 0.39;

    private static final double TILE_HALF_EXTENT = 0.5;
    private static final double STRUCTURE_HIT_RADIUS = 0.6;
    private static final double BOARD_TILE_WIDTH_PIXELS = 76.35;
    private static final double PEA_IMPACT_ALIGNMENT_PIXELS = 85.0;
    public static final double PEA_IMPACT_ALIGNMENT_TILES =
            PEA_IMPACT_ALIGNMENT_PIXELS / BOARD_TILE_WIDTH_PIXELS;
    private static final double LOB_IMPACT_ALIGNMENT_PIXELS = 30.0;
    public static final double LOB_IMPACT_ALIGNMENT_TILES =
            LOB_IMPACT_ALIGNMENT_PIXELS / BOARD_TILE_WIDTH_PIXELS;
    private static final double LOB_GRAVE_IMPACT_ALIGNMENT_PIXELS = 30.0;
    public static final double LOB_GRAVE_IMPACT_ALIGNMENT_TILES =
            LOB_GRAVE_IMPACT_ALIGNMENT_PIXELS / BOARD_TILE_WIDTH_PIXELS;

    private final int damage;
    private final Item target;
    private Plant sourcePlant;
    private String sourcePlantName;
    private boolean plantFoodShot;
    private boolean isStunning;
    private int assetVariant;
    private double damageOverride = Double.NaN;
    private String displayPath;
    private String displayState;
    private boolean torchwoodTransformed;
    private double maxTravelDistance;
    private double travelledDistance;
    private Position previousPosition;
    private boolean groundBounced;
    private boolean deflectedThisTick;

    private MoveStrategy moveStrategy;
    private HitEffectStrategy hitEffectStrategy;
    private final Set<Zombie> hitZombies = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private int remainingHits = Integer.MIN_VALUE;

    private double spawnDelaySeconds = DEFAULT_SPAWN_DELAY_SECONDS;
    private final boolean firedBackwards;

    public void deflectTowardsPlant(Zombie deflector) {
        if (deflector != null) hitZombies.add(deflector);
        remainingHits = 2;
        
        
        moveStrategy = new StraightMove();
        setAlive(true);
    }

    public Projectile(Position position, Position velocity, Zombie zombie, int damage, MoveStrategy moveStrategy, HitEffectStrategy hitEffectStrategy) {
        this(zombie, position, velocity, damage, moveStrategy, hitEffectStrategy);
    }

    public Projectile(Plant sourcePlant, Position position, Position velocity, Zombie zombie, int damage,
                      MoveStrategy moveStrategy, HitEffectStrategy hitEffectStrategy) {
        this(position, velocity, zombie, damage, moveStrategy, hitEffectStrategy);
        setSourcePlant(sourcePlant);
    }

    public Projectile(Item target, Position position, Position velocity, int damage, MoveStrategy moveStrategy, HitEffectStrategy hitEffectStrategy) {
        super(position, 1);
        this.setPosition(position);
        this.previousPosition = position;
        this.setSpeed(velocity);

        this.target = target;
        this.damage = damage;
        this.moveStrategy = moveStrategy;
        this.hitEffectStrategy = hitEffectStrategy;
        this.isStunning = false;
        this.firedBackwards = velocity != null && velocity.x() < 0;
    }

    public boolean isFiredBackwards() {
        return firedBackwards;
    }

    public void setSpawnDelaySeconds(double seconds) {
        this.spawnDelaySeconds = Math.max(0.0, seconds);
    }

    public double getSpawnDelaySeconds() {
        return spawnDelaySeconds;
    }

    public boolean isSpawning() {
        return spawnDelaySeconds > 0;
    }

    public boolean isVisible() {
        return isAlive && spawnDelaySeconds <= 0;
    }

    public boolean isLobbed() {
        return moveStrategy instanceof LobArcMove;
    }

    public void setStunning(boolean isStunning) {
        this.isStunning = isStunning;
    }

    public Position getPreviousPosition() {
        return previousPosition == null ? getPosition() : previousPosition;
    }

    protected void setPreviousPosition(Position previousPosition) {
        this.previousPosition = previousPosition;
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

    public String getDisplayPath() { return displayPath; }
    public String getDisplayState() { return displayState; }

    public void setAssetVariant(int assetVariant) {
        this.assetVariant = Math.max(0, assetVariant);
    }

    public void setSourcePlant(Plant sourcePlant) {
        this.sourcePlant = sourcePlant;
        if (sourcePlant != null) {
            this.sourcePlantName = sourcePlant.getName();
            this.plantFoodShot = sourcePlant.isPlantFoodActive();
        }
    }

    public void setSourceDisplay(String plantName, boolean plantFood) {
        this.sourcePlantName = plantName;
        this.plantFoodShot = plantFood;
    }

    public String getSourcePlantName() {
        return sourcePlantName;
    }

    public boolean isPlantFoodShot() {
        return plantFoodShot;
    }

    public void setDisplay(String displayPath, String displayState) {
        this.displayPath = displayPath;
        this.displayState = displayState;
    }

    @Override
    public void tick() {
        if (!isAlive) return;

        if (spawnDelaySeconds > 0) {
            spawnDelaySeconds = Math.max(0.0, spawnDelaySeconds - GameClock.SECONDS_PER_TICK);
            previousPosition = getPosition();
            return;
        }
        step(GameClock.SECONDS_PER_TICK);
    }


    public void advanceVisual(double deltaSeconds) {
        if (!isAlive || deltaSeconds <= 0 || moveStrategy == null) return;
        moveStrategy.move(this, deltaSeconds);
        previousPosition = getPosition();
    }

    protected void step(double deltaSeconds) {
        Position start = getPosition();
        if (start == null) {
            setAlive(false);
            return;
        }
        this.previousPosition = start;

        if (moveStrategy != null) moveStrategy.move(this, deltaSeconds);

        Position end = getPosition();
        if (end == null) {
            setAlive(false);
            return;
        }

        if (maxTravelDistance > 0) {
            travelledDistance += end.distanceTo(start);
            if (travelledDistance > maxTravelDistance) {
                setAlive(false);
                return;
            }
        }

        GameSession session = GameSession.peekInstance();
        if (session == null || session.getEnvironment() == null) return;

        applyTorchwoodTransform(session, start, end);

        if (isLobbed()) {
            resolveLobbed(session, start, end);
            return;
        }
        resolveDirect(session, start, end);
    }

    private void resolveLobbed(GameSession session, Position start, Position end) {
        LobArcMove arc = (LobArcMove) moveStrategy;

        if (target instanceof Zombie targetZombie) {
            if (!hitZombies.contains(targetZombie) && isValidTarget(targetZombie)
                    && collisionProjection(lobImpactCenter(targetZombie.getPosition()),
                    start, end, targetZombie.getHitRadius()) >= 0) {
                resolveLobImpact(session, targetZombie.getPosition(), targetZombie, 0);
                return;
            }
        } else if (target != null && target.isAlive()) {
            Position targetPosition = resolveTargetPosition(target);
            if (targetPosition != null && collisionProjection(targetPosition, start, end,
                    Zombie.HIT_RADIUS) >= 0) {
                target.takeDamage(getEffectiveDamage());
                recordImpact(session, targetPosition);
                setAlive(false);
                return;
            }
        }

        if (target == null) {
            Blocker blocker = findFirstBlocker(session, start, end,
                    (int) Math.round(arc.getEndY()));
            if (blocker != null) {
                Position impactAt = blockerImpact(blocker, start, end);
                blocker.damage(this, session);
                recordImpact(session, impactAt);
                setAlive(false);
                return;
            }
        }

        if (arc.hasLanded()) {
            resolveLobImpact(session, end, null, Zombie.HIT_RADIUS);
            return;
        }

        if (isOutsideLawnForLobber(session, end)
                && !bounceToNextZombie(session, end)
                && !bounceAlongGround(session, end)) {
            setAlive(false);
        }
    }

    private Position lobImpactCenter(Position at) {
        if (at == null) return null;
        return Position.of(at.x() - LOB_IMPACT_ALIGNMENT_TILES, at.y());
    }

    private void resolveLobImpact(GameSession session, Position center, Zombie primary,
                                  double minimumRadius) {
        deflectedThisTick = false;
        Position impactAt = lobImpactCenter(center);

        boolean connected = false;
        if (primary != null && isValidTarget(primary) && !hitZombies.contains(primary)) {
            applyDamageAndEffect(primary);
            hitZombies.add(primary);
            if (deflectedThisTick) {
                
                
                
                recordImpact(session, impactAt);
                return;
            }
            connected = true;
        }
        if (splashAround(session, impactAt, minimumRadius)) connected = true;

        if (connected) {
            recordImpact(session, impactAt);
            setAlive(false);
            return;
        }

        if (damageObstacleUnder(session, center)) return;

        
        
        
        
        
        
        if (bounceToNextZombie(session, center)) return;
        if (bounceAlongGround(session, center)) return;

        recordImpact(session, impactAt);
        setAlive(false);
    }

    private boolean damageObstacleUnder(GameSession session, Position center) {
        if (center == null) return false;
        Blocker blocker = findFirstBlocker(session, getPreviousPosition(), center,
                (int) Math.round(center.y()));
        if (blocker == null) return false;

        Position impactAt = blockerImpact(blocker, getPreviousPosition(), center);
        blocker.damage(this, session);
        recordImpact(session, impactAt);
        setAlive(false);
        return true;
    }

    private static final double BOUNCE_PEAK_HEIGHT = 0.6;
    private static final double GROUND_BOUNCE_DISTANCE = 2.5;

    /**
     * Called by a zombie's defense (e.g. the parasol zombie) when it absorbs a
     * lobbed hit instead of taking damage from it: redirects this projectile onto
     * the next un-hit zombie in the lane, or a short hop along the ground if
     * nobody else is left, so the shot visibly bounces off instead of just being
     * cancelled. Sets deflectedThisTick so the caller in resolveLobImpact knows
     * not to kill the projectile after this returns.
     */
    public void bounceOverZombie(Zombie deflector, GameSession session) {
        if (deflector != null) hitZombies.add(deflector);
        Position from = (deflector != null && deflector.getPosition() != null)
                ? deflector.getPosition() : getPosition();

        deflectedThisTick = bounceToNextZombie(session, from);
        if (!deflectedThisTick) {
            deflectedThisTick = bounceAlongGround(session, from);
        }
    }

    /**
     * Redirects this lobbed projectile toward the nearest zombie it hasn't already
     * hit in the same row, so a shot that reaches the end of the lane (or lands
     * somewhere empty) bounces onward instead of dealing no damage. Returns false
     * (and leaves the projectile untouched) if there's nobody left in the row to
     * bounce to.
     */
    private boolean bounceToNextZombie(GameSession session, Position from) {
        if (from == null || !(moveStrategy instanceof LobArcMove currentArc)) return false;

        int row = (int) Math.round(from.y());
        Zombie next = null;
        double bestDistance = Double.MAX_VALUE;
        for (Zombie zombie : session.getZombies()) {
            if (!isValidTarget(zombie) || hitZombies.contains(zombie)) continue;
            Position zp = zombie.getPosition();
            if (zp == null || Math.abs(zp.y() - row) >= 0.5) continue;

            double distance = Math.abs(zp.x() - from.x());
            if (distance < bestDistance) {
                bestDistance = distance;
                next = zombie;
            }
        }
        if (next == null) return false;

        this.moveStrategy = new LobArcMove(from.x(), row, next.getPosition().x(), row,
                BOUNCE_PEAK_HEIGHT, currentArc.getHorizontalSpeed());
        this.previousPosition = from;
        setPosition(from);
        return true;
    }

    /**
     * Fallback for when there's no zombie left anywhere in the lane to bounce to:
     * hops the shot a couple of tiles further along the ground so it visibly lands
     * somewhere instead of silently despawning. Only happens once per projectile
     * (via groundBounced) so it can't bounce along the ground forever.
     */
    private boolean bounceAlongGround(GameSession session, Position from) {
        if (from == null || groundBounced || session.getEnvironment() == null
                || !(moveStrategy instanceof LobArcMove currentArc)) return false;

        int row = (int) Math.round(from.y());
        double maxCol = session.getEnvironment().getCols() - 1;
        double targetX = Math.min(maxCol, from.x() + GROUND_BOUNCE_DISTANCE);
        if (targetX <= from.x() + 0.1) return false;

        groundBounced = true;
        this.moveStrategy = new LobArcMove(from.x(), row, targetX, row,
                BOUNCE_PEAK_HEIGHT, currentArc.getHorizontalSpeed());
        this.previousPosition = from;
        setPosition(from);
        return true;
    }

    private boolean splashAround(GameSession session, Position center, double minimumRadius) {
        if (center == null) return false;
        int areaLength = hitEffectStrategy == null ? 1 : hitEffectStrategy.getAreaLength();
        double radius = Math.max(minimumRadius, (areaLength - 1) / 2.0);
        if (radius <= 0) return false;

        boolean any = false;
        for (Zombie zombie : session.getZombies()) {
            if (!isValidTarget(zombie) || hitZombies.contains(zombie)) continue;
            Position at = zombie.getPosition();
            if (Math.abs(at.x() - center.x()) <= radius
                    && Math.abs(at.y() - center.y()) <= radius) {
                applyDamageAndEffect(zombie, true);
                hitZombies.add(zombie);
                any = true;
            }
        }
        return any;
    }

    public boolean isPeaShot() {
        if (sourcePlant != null && sourcePlant.getTags() != null
                && sourcePlant.getTags().contains(model.collections.plant.PlantTag.PEA)) {
            return true;
        }
        return sourcePlantName != null
                && sourcePlantName.toLowerCase().contains("peashooter");
    }

    private Position zombieHitCenter(Zombie zombie) {
        Position at = zombie == null ? null : zombie.getPosition();
        if (at == null || !isPeaShot()) return at;
        return Position.of(at.x() - PEA_IMPACT_ALIGNMENT_TILES, at.y());
    }

    private void resolveDirect(GameSession session, Position start, Position end) {
        Blocker blocker = findFirstBlocker(session, start, end, -1);
        double blockerProjection = blocker == null ? Double.MAX_VALUE : blocker.projection();

        List<ZombieHit> collisions = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (!isValidTarget(zombie) || hitZombies.contains(zombie)) continue;
            Position hitCenter = zombieHitCenter(zombie);
            double projection = collisionProjection(hitCenter, start, end, zombie.getHitRadius());
            if (projection >= 0 && projection <= blockerProjection) {
                collisions.add(new ZombieHit(zombie, projection, isPeaShot() ? hitCenter
                        : onRow(pointAlong(start, end, projection), zombie.getPosition().y())));
            }
        }
        collisions.sort(Comparator.comparingDouble(ZombieHit::projection));

        if (remainingHits == Integer.MIN_VALUE) {
            remainingHits = hitEffectStrategy == null ? 1 : hitEffectStrategy.getPierceCount();
            if (remainingHits == 0) remainingHits = 1;
        }

        for (ZombieHit collision : collisions) {
            hitZombie(collision.zombie(), session);
            hitZombies.add(collision.zombie());
            recordImpact(session, collision.impactAt());
            if (remainingHits > 0) remainingHits--;
            if (remainingHits == 0) {
                setAlive(false);
                return;
            }
        }

        if (blocker != null) {
            Position impactAt = blockerImpact(blocker, start, end);
            blocker.damage(this, session);
            recordImpact(session, impactAt);
            setAlive(false);
            return;
        }

        if (isOutsideLawn(session, end)) setAlive(false);
    }

    private void recordImpact(GameSession session, Position at) {
        if (session == null || at == null || sourcePlantName == null) return;
        session.recordProjectileImpact(
                new ProjectileImpact(sourcePlantName, plantFoodShot, assetVariant, at));
    }

    private static Position onRow(Position along, double row) {
        if (along == null || Double.isNaN(row)) return along;
        return Position.of(along.x(), row);
    }

    private Position blockerImpact(Blocker blocker, Position start, Position end) {
        if (blocker == null) return null;
        Position at = onRow(pointAlong(start, end, blocker.projection()), blocker.targetRow());
        if (at == null || !isLobbed() || !blocker.isGrave()) return at;
        return Position.of(at.x() + LOB_GRAVE_IMPACT_ALIGNMENT_TILES, at.y());
    }

    private static Position pointAlong(Position start, Position end, double projection) {
        if (start == null || end == null) return end;
        double clamped = Math.max(0, Math.min(1, projection));
        return start.add(end.sub(start).scale(clamped));
    }

    private void hitZombie(Zombie primary, GameSession session) {
        applyDamageAndEffect(primary, false);
        if (moveStrategy != null) moveStrategy.onHit(this);

        int areaLength = hitEffectStrategy == null ? 1 : hitEffectStrategy.getAreaLength();
        double radius = Math.max(0, (areaLength - 1) / 2.0);
        if (radius <= 0 || primary.getPosition() == null) return;

        Position center = primary.getPosition();
        for (Zombie zombie : session.getZombies()) {
            if (zombie == primary || !isValidTarget(zombie) || zombie.getPosition() == null) continue;
            if (hitZombies.contains(zombie)) continue;
            if (Math.abs(zombie.getPosition().x() - center.x()) <= radius
                    && Math.abs(zombie.getPosition().y() - center.y()) <= radius) {
                applyDamageAndEffect(zombie, true);
                hitZombies.add(zombie);
            }
        }
    }

    private void applyDamageAndEffect(Zombie zombie) {
        applyDamageAndEffect(zombie, false);
    }

    private void applyDamageAndEffect(Zombie zombie, boolean splashed) {
        if (hitEffectStrategy != null) hitEffectStrategy.beforeDamage(zombie);
        int effectiveDamage = getEffectiveDamage();
        if (splashed && hitEffectStrategy != null) {
            effectiveDamage += hitEffectStrategy.getSplashDamageBonus();
        }
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

    private void applyTorchwoodTransform(GameSession session, Position start, Position end) {
        if (torchwoodTransformed || sourcePlant == null || sourcePlant.getPosition() == null
                || sourcePlant.getTags() == null || !sourcePlant.getTags().contains(model.collections.plant.PlantTag.PEA)
                || start == null || end == null) return;

        Position best = null;
        double bestProjection = Double.MAX_VALUE;
        for (Plant plant : session.getPlants()) {
            if (plant == null || !plant.isAlive() || !"Torchwood".equalsIgnoreCase(plant.getName())
                    || plant.getPosition() == null) continue;
            Position torch = plant.getPosition();
            double projection = collisionProjection(torch, start, end);
            if (projection >= 0 && projection < bestProjection) {
                bestProjection = projection;
                best = torch;
            }
        }
        if (best == null) return;

        torchwoodTransformed = true;
        boolean torchwoodPlantFood = false;
        for (Plant plant : session.getPlants()) {
            if (plant == null || !plant.isAlive() || plant.getPosition() == null) continue;
            if (!"Torchwood".equalsIgnoreCase(plant.getName())) continue;
            if (plant.getPosition().distanceTo(best) <= 0.01) {
                torchwoodPlantFood = plant.isPlantFoodActive();
                break;
            }
        }

        if (torchwoodPlantFood) {
            displayPath = "768/INITIAL/EFFECTS/T_FIRE_PEA_BLUE/T_FIRE_PEA_BLUE.PAM";
            displayState = "animation2";
            damageOverride = 40.0;
            hitEffectStrategy = new model.projectile.hit.FireHit(1, 1.0);
            remainingHits = Integer.MIN_VALUE;
            return;
        }

        if ("Snow Pea".equalsIgnoreCase(sourcePlant.getName())) {
            displayPath = "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM";
            displayState = "animation";
            damageOverride = 20.0;
            hitEffectStrategy = new model.projectile.hit.NormalHit(1);
            remainingHits = Integer.MIN_VALUE;
        } else if (sourcePlant.getTags().contains(model.collections.plant.PlantTag.PEA)
                && !"Fire Peashooter".equalsIgnoreCase(sourcePlant.getName())) {
            displayPath = "768/INITIAL/EFFECTS/T_FIRE_PEA/T_FIRE_PEA.PAM";
            displayState = "animation";
            damageOverride = 40.0;
            hitEffectStrategy = new model.projectile.hit.FireHit(1, 1.0);
            remainingHits = Integer.MIN_VALUE;
        }
    }

    public boolean isTorchwoodTransformed() {
        return torchwoodTransformed;
    }

    int getEffectiveDamage() {
        if (!Double.isNaN(damageOverride)) return Math.max(0, (int) Math.round(damageOverride));
        double multiplier = hitEffectStrategy == null ? 1.0 : hitEffectStrategy.getDamageMultiplier();
        return Math.max(0, (int) Math.round(damage * multiplier));
    }

    private boolean isValidTarget(Zombie zombie) {
        if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()
                || zombie.getPosition() == null) {
            return false;
        }
        return !zombie.isAirborne() || zombie.acceptsAttackFrom(this);
    }

    private record Blocker(Cell cell, PushableStructure structure, double projection) {
        boolean isGrave() {
            return cell != null && cell.getObstacle() instanceof Grave;
        }

        double targetRow() {
            if (structure != null && structure.getPosition() != null) {
                return structure.getPosition().y();
            }
            return cell == null ? Double.NaN : cell.getRow();
        }

        void damage(Projectile projectile, GameSession session) {
            int amount = projectile.getEffectiveDamage();
            if (structure != null) {
                structure.takeDamage(amount, projectile.getSourcePlant(), session);
                return;
            }
            PlantTarget.damageObstacle(cell, amount, session, projectile.isFireShot());
        }
    }

    boolean isFireShot() {
        return (hitEffectStrategy != null && hitEffectStrategy.isFireDamage())
                || (sourcePlant != null && sourcePlant.getTags() != null
                && sourcePlant.getTags().contains(model.collections.plant.PlantTag.FIRE));
    }

    private Blocker findFirstBlocker(GameSession session, Position start, Position end,
                                     int onlyRow) {
        int rows = session.getEnvironment().getRows();
        int cols = session.getEnvironment().getCols();
        int firstRow = onlyRow >= 0 ? onlyRow : 0;
        int lastRow = onlyRow >= 0 ? onlyRow : rows - 1;
        if (firstRow < 0 || lastRow >= rows) return null;

        Blocker best = null;
        Set<PushableStructure> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = 0; col < cols; col++) {
                Cell cell = session.getEnvironment().getCell(row, col);
                if (cell == null) continue;

                if (PlantTarget.isDestructible(cell.getObstacle())) {
                    double projection = tileProjection(col, row, start, end);
                    if (projection >= 0 && (best == null || projection < best.projection())) {
                        best = new Blocker(cell, null, projection);
                    }
                }

                PushableStructure structure = cell.getStructure();
                if (structure == null || !structure.isAlive() || !seen.add(structure)) continue;
                double projection = collisionProjection(structure.getPosition(), start, end,
                        STRUCTURE_HIT_RADIUS);
                if (projection >= 0 && (best == null || projection < best.projection())) {
                    best = new Blocker(null, structure, projection);
                }
            }
        }
        return best;
    }

    private double collisionProjection(Position targetPosition, Position start, Position end) {
        return collisionProjection(targetPosition, start, end, Zombie.HIT_RADIUS);
    }

    private double collisionProjection(Position targetPosition, Position start, Position end,
                                       double radius) {
        if (targetPosition == null || start == null || end == null) return -1;
        Position movement = end.sub(start);
        double lengthSquared = movement.dot(movement);
        if (lengthSquared == 0) return end.distanceTo(targetPosition) <= radius ? 0 : -1;

        double projection = targetPosition.sub(start).dot(movement) / lengthSquared;
        double clamped = Math.max(0, Math.min(1, projection));
        Position closestPoint = start.add(movement.scale(clamped));
        return closestPoint.distanceTo(targetPosition) <= radius ? clamped : -1;
    }

    private double tileProjection(int col, int row, Position start, Position end) {
        if (start == null || end == null) return -1;
        double[][] axes = {
                {start.x(), end.x() - start.x(), col - TILE_HALF_EXTENT, col + TILE_HALF_EXTENT},
                {start.y(), end.y() - start.y(), row - TILE_HALF_EXTENT, row + TILE_HALF_EXTENT},
        };

        double entry = 0.0;
        double exit = 1.0;
        for (double[] axis : axes) {
            double origin = axis[0];
            double delta = axis[1];
            double min = axis[2];
            double max = axis[3];
            if (Math.abs(delta) < 1.0e-9) {
                if (origin < min || origin > max) return -1;
                continue;
            }
            double first = (min - origin) / delta;
            double second = (max - origin) / delta;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            entry = Math.max(entry, first);
            exit = Math.min(exit, second);
            if (entry > exit) return -1;
        }
        return entry;
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
    public MoveStrategy getMoveStrategy() { return moveStrategy; }
    public void setMoveStrategy(MoveStrategy moveStrategy) { this.moveStrategy = moveStrategy; }
    public Item getTarget() { return target; }
    public int getDamage() { return damage; }
    public Plant getSourcePlant() { return sourcePlant; }

    private record ZombieHit(Zombie zombie, double projection, Position impactAt) {}

    private boolean isOutsideLawnForLobber(GameSession session, Position position) {
        return position.x() < -1
                || position.x() > session.getEnvironment().getCols()
                || position.y() < -2.5
                || position.y() > session.getEnvironment().getRows() + 1;
    }
}