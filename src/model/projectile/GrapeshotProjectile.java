package model.projectile;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import service.GameClock;

public class GrapeshotProjectile extends Projectile {

    private static final double HIT_RADIUS = 0.5;
    private static final double SEEK_RADIUS = 4.5;
    private static final int MAX_EDGE_BOUNCES = 6;
    private static final double EPSILON = 1.0e-6;

    private final double lifetimeSeconds;
    private int remainingZombieBounces;
    private int remainingEdgeBounces = MAX_EDGE_BOUNCES;
    private double elapsedSeconds;
    private Zombie lastHitZombie;

    public GrapeshotProjectile(Plant source, Position origin, Position velocity, int damage,
                               int bounces, double lifetimeSeconds) {
        super(source, origin, velocity, (Zombie) null, damage, null, null);
        this.remainingZombieBounces = Math.max(1, bounces);
        this.lifetimeSeconds = Math.max(0.1, lifetimeSeconds);
        setSpawnDelaySeconds(0.0);
    }

    public int getRemainingBounces() {
        return remainingZombieBounces;
    }

    public double getRemainingLifetime() {
        return Math.max(0.0, lifetimeSeconds - elapsedSeconds);
    }

    @Override
    public void tick() {
        if (!isAlive()) return;

        GameSession session = GameSession.peekInstance();
        if (session == null || session.getEnvironment() == null) {
            setAlive(false);
            return;
        }

        elapsedSeconds += GameClock.SECONDS_PER_TICK;
        if (elapsedSeconds >= lifetimeSeconds) {
            setAlive(false);
            return;
        }

        Position start = getPosition();
        Position velocity = getSpeed();
        if (start == null || velocity == null) {
            setAlive(false);
            return;
        }
        setPreviousPosition(start);

        Position end = advance(session, start, velocity, GameClock.SECONDS_PER_TICK);
        setPosition(end);
        if (!isAlive()) return;

        Zombie hit = firstZombieAlong(session, start, end);
        if (hit == null) return;

        hit.takeDamage(getDamage(), this);
        recordShrapnelImpact(session, hit.getPosition());
        lastHitZombie = hit;
        remainingZombieBounces--;
        if (remainingZombieBounces <= 0) {
            setAlive(false);
            return;
        }
        setSpeed(bounceOffZombie(session, hit, end, getSpeed()));
    }

    @Override
    public void advanceVisual(double deltaSeconds) {
        if (!isAlive() || deltaSeconds <= 0) return;
        Position position = getPosition();
        Position velocity = getSpeed();
        if (position == null || velocity == null) return;
        setPosition(position.add(velocity.scale(deltaSeconds)));
        setPreviousPosition(getPosition());
    }

    private void recordShrapnelImpact(GameSession session, Position at) {
        if (at == null || getSourcePlantName() == null) return;
        session.recordProjectileImpact(new ProjectileImpact(getSourcePlantName(),
                isPlantFoodShot(), getAssetVariant(), at));
    }

    private Position advance(GameSession session, Position start, Position velocity, double delta) {
        double x = start.x() + velocity.x() * delta;
        double y = start.y() + velocity.y() * delta;
        double vx = velocity.x();
        double vy = velocity.y();
        double maxX = session.getEnvironment().getCols() - 1.0;
        double maxY = session.getEnvironment().getRows() - 1.0;
        boolean bounced = false;

        if (x < 0.0) {
            x = -x;
            vx = -vx;
            bounced = true;
        } else if (x > maxX) {
            x = maxX - (x - maxX);
            vx = -vx;
            bounced = true;
        }
        if (y < 0.0) {
            y = -y;
            vy = -vy;
            bounced = true;
        } else if (y > maxY) {
            y = maxY - (y - maxY);
            vy = -vy;
            bounced = true;
        }

        if (bounced) {
            setSpeed(Position.of(vx, vy));
            remainingEdgeBounces--;
            if (remainingEdgeBounces < 0) setAlive(false);
        }
        return Position.of(Math.max(0.0, Math.min(maxX, x)), Math.max(0.0, Math.min(maxY, y)));
    }

    private Zombie firstZombieAlong(GameSession session, Position start, Position end) {
        Zombie best = null;
        double bestProjection = Double.MAX_VALUE;
        for (Zombie zombie : session.getZombies()) {
            if (!isTargetable(zombie) || zombie == lastHitZombie) continue;
            double projection = segmentProjection(zombie.getPosition(), start, end);
            if (projection >= 0 && projection < bestProjection) {
                bestProjection = projection;
                best = zombie;
            }
        }
        return best;
    }

    private Position bounceOffZombie(GameSession session, Zombie hit, Position at, Position velocity) {
        double speed = velocity.length();
        if (speed < EPSILON) return velocity;

        Zombie next = nearestOtherZombie(session, at);
        if (next != null) {
            Position toNext = next.getPosition().sub(at);
            if (toNext.length() > EPSILON) return toNext.normalize().scale(speed);
        }

        Position normal = at.sub(hit.getPosition());
        if (normal.length() < EPSILON) return velocity.negate();
        normal = normal.normalize();
        double dot = velocity.dot(normal);
        if (dot >= 0) return velocity.negate();
        return velocity.sub(normal.scale(2.0 * dot));
    }

    private Zombie nearestOtherZombie(GameSession session, Position from) {
        Zombie best = null;
        double shortest = SEEK_RADIUS;
        for (Zombie zombie : session.getZombies()) {
            if (!isTargetable(zombie) || zombie == lastHitZombie) continue;
            double distance = zombie.getPosition().distanceTo(from);
            if (distance < shortest) {
                shortest = distance;
                best = zombie;
            }
        }
        return best;
    }

    private boolean isTargetable(Zombie zombie) {
        return zombie != null && zombie.isAlive() && !zombie.isHypnotized()
                && zombie.getPosition() != null;
    }

    private double segmentProjection(Position target, Position start, Position end) {
        if (target == null || start == null || end == null) return -1;
        Position movement = end.sub(start);
        double lengthSquared = movement.dot(movement);
        if (lengthSquared < EPSILON) {
            return end.distanceTo(target) <= HIT_RADIUS ? 0 : -1;
        }
        double projection = target.sub(start).dot(movement) / lengthSquared;
        double clamped = Math.max(0, Math.min(1, projection));
        Position closest = start.add(movement.scale(clamped));
        return closest.distanceTo(target) <= HIT_RADIUS ? clamped : -1;
    }
}
