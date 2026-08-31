package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.projectile.GrapeshotProjectile;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.List;

public class GrapeshotStrategy implements ActStrategy {

    public static final int GRAPE_COUNT = 8;
    public static final double GRAPE_SPEED = 6.6;
    public static final int SHRAPNEL_DAMAGE_DIVISOR = 6;
    public static final int BASE_BOUNCES = 3;
    public static final int EXTENDED_BOUNCES = 5;
    public static final double BASE_LIFETIME_SECONDS = 3.0;
    public static final double EXTENDED_LIFETIME_SECONDS = 4.2;
    public static final String BOUNCE_UPGRADE_TAG = "GRAPE_BOUNCE_EXT";

    private static final double BLAST_RADIUS_TILES = 1.0;

    @Override
    public void act(Plant user, GameSession session) {
        if (user == null || session == null || user.getPosition() == null) return;
        if (user.getIntervalTimer() > 0) return;

        Position center = user.getPosition();
        int damage = Math.max(1, user.getDamage());

        List<Zombie> caught = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            if (inBlast(zombie.getPosition(), center)) caught.add(zombie);
        }
        for (Zombie zombie : caught) {
            if (zombie.isAlive()) zombie.takeDamage(damage, user);
        }

        for (PushableStructure structure : session.getPushableStructures()) {
            if (structure == null || !structure.isAlive() || structure.getPosition() == null) continue;
            if (inBlast(structure.getPosition(), center)) structure.takeDamage(damage, user, session);
        }

        spawnGrapes(user, session, center, damage);
        user.setAlive(false);
    }

    private boolean inBlast(Position position, Position center) {
        return Math.abs(position.x() - center.x()) <= BLAST_RADIUS_TILES
                && Math.abs(position.y() - center.y()) <= BLAST_RADIUS_TILES;
    }

    private void spawnGrapes(Plant user, GameSession session, Position center, int damage) {
        boolean extended = user.getRawUpgrades().contains(BOUNCE_UPGRADE_TAG);
        int bounces = extended ? EXTENDED_BOUNCES : BASE_BOUNCES;
        double lifetime = extended ? EXTENDED_LIFETIME_SECONDS : BASE_LIFETIME_SECONDS;
        int grapeDamage = Math.max(1, damage / SHRAPNEL_DAMAGE_DIVISOR);

        double grapeSpeed = session.projectileSpeed(GRAPE_SPEED);
        for (int i = 0; i < GRAPE_COUNT; i++) {
            double angle = (2.0 * Math.PI * i) / GRAPE_COUNT;
            Position velocity = Position.of(Math.cos(angle) * grapeSpeed,
                    Math.sin(angle) * grapeSpeed);
            GrapeshotProjectile grape = new GrapeshotProjectile(user, center, velocity,
                    grapeDamage, bounces, lifetime);
            grape.setAssetVariant(i % 3);
            session.getProjectiles().add(grape);
        }
    }
}
