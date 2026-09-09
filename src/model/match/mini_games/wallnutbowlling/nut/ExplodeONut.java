package model.match.mini_games.wallnutbowlling.nut;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class ExplodeONut extends Nut {
    private static final int EXPLOSION_DAMAGE = 1800; // matches Cherry Bomb's damage
    private static final int BLAST_RADIUS_TILES = 1;

    public ExplodeONut(Position position, Position direction) {
        super(position, direction);
    }

    @Override
    public boolean onHitZombie(Zombie zombie, GameSession session) {
        int centerRow = (int) zombie.getPosition().y();
        int centerCol = (int) zombie.getPosition().x();

        session.getZombies().stream()
                .filter(z -> z.isAlive() && withinBlast(z, centerRow, centerCol))
                .forEach(z -> z.takeDamage(EXPLOSION_DAMAGE, this));

        kill();
        return true; // the nut is consumed by the explosion
    }

    @Override
    public String getKindName() {
        return "Explode-O-Nut";
    }

    private boolean withinBlast(Zombie zombie, int centerRow, int centerCol) {
        Position pos = zombie.getPosition();
        if (pos == null) return false;
        int row = (int) pos.y();
        int col = (int) pos.x();
        return Math.abs(row - centerRow) <= BLAST_RADIUS_TILES && Math.abs(col - centerCol) <= BLAST_RADIUS_TILES;
    }
}
