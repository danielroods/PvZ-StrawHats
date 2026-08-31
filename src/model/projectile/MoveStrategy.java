package model.projectile;

public interface MoveStrategy {
    void move(Projectile projectile, double deltaSeconds);

    default void onHit(Projectile projectile) {
    }
}
