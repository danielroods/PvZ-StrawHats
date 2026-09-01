package model.collections.zombie.zombie_pushing_item;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match_mechanisms.vector.Position;
import model.pitches.obstacles.PushableType;
import model.utils.GameSession;

public class PushableStructure {
    // How long an ice block spends dropping in from the sky before it settles onto the
    // lawn and can start being pushed / take damage - see startFalling()/updateFall().
    private static final double FALL_DURATION_SECONDS = 0.6;

    private final PushableType type;
    private Position position;
    private int hp;
    private Zombie owner;
    private boolean destructionHandled = false;
    private boolean falling = false;
    private double fallElapsed = 0;

    public PushableStructure(PushableType type, Position position) {
        this.type = type;
        this.position = position;
        this.hp = switch (type) {
            case ICE_BLOCK -> 600;
            case ARCADE_CABINET, BARREL -> 1100;
        };
    }

    /** Begins the "falls smoothly from the sky" drop-in animation for a freshly spawned block. */
    public void startFalling() {
        this.falling = true;
        this.fallElapsed = 0;
    }

    public boolean isFalling() { return falling; }

    public void updateFall(double deltaTime) {
        if (!falling) return;
        fallElapsed += Math.max(0, deltaTime);
        if (fallElapsed >= FALL_DURATION_SECONDS) {
            falling = false;
            fallElapsed = FALL_DURATION_SECONDS;
        }
    }

    /** 0 = still high in the sky, 1 = landed on the lawn. */
    public double getFallProgress() {
        if (!falling) return 1.0;
        return Math.max(0, Math.min(1, fallElapsed / FALL_DURATION_SECONDS));
    }

    public boolean isAlive() { return hp > 0; }
    public PushableType getType() { return type; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = Math.max(0, hp); }
    public Zombie getOwner() { return owner; }
    public void setOwner(Zombie owner) { this.owner = owner; }

    public void takeDamage(int damage, Plant source, GameSession session) {
        if (!isAlive() || damage <= 0) return;
        hp = Math.max(0, hp - damage);
        if (hp == 0) onDestroyed(session);
    }

    private void onDestroyed(GameSession session) {
        if (destructionHandled) return;
        destructionHandled = true;
        if (session == null || position == null) return;

        int row = (int) Math.round(position.y());
        int col = Math.max(0, Math.min(session.getCols() - 1, (int) Math.round(position.x())));

        if (type == PushableType.BARREL) {
            for (int i = 0; i < 2; i++) {
                Zombie imp = ZombieFactory.create("ZombieImp", row, Math.min(session.getCols() - 1, col + i));
                session.spawnZombie(imp);
            }
        } else if (type == PushableType.ICE_BLOCK) {
            // The imp frozen inside the ice block is only revealed once the block
            // itself is pushed into something and shatters.
            Zombie imp = ZombieFactory.create("ZombieImp", row, col);
            session.spawnZombie(imp);
        }
    }
}