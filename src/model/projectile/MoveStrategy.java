package model.projectile;

public interface MoveStrategy {
    void move(Projectile projectile);

    /**
     * Called whenever the projectile registers a hit on a zombie. Most
     * strategies don't care, but strategies like {@link RollingBounceMove}
     * use it to change the travel direction after impact.
     */
    default void onHit(Projectile projectile) {
    }
}
