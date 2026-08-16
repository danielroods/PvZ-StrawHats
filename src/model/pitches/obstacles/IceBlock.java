package model.pitches.obstacles;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;

public class IceBlock implements Obstacle {
    public static final int BASE_HP = 600;

    private final Plant frozenPlant;
    private final Zombie frozenZombie;
    private int hp;

    public IceBlock(Plant frozenPlant, int hp) {
        this.frozenPlant = frozenPlant;
        this.frozenZombie = null;
        this.hp = Math.max(1, hp);
    }

    public IceBlock(Zombie frozenZombie, int hp) {
        this.frozenPlant = null;
        this.frozenZombie = frozenZombie;
        this.hp = Math.max(1, hp);
    }

    public boolean takeDamage(int amount) {
        if (hp <= 0) return false;
        hp = Math.max(0, hp - Math.max(0, amount));
        if (hp == 0) {
            release();
            return true;
        }
        return false;
    }

    public void release() {
        if (frozenPlant != null && frozenPlant.isAlive()) {
            frozenPlant.setState(Plant.PlantState.ACTIVE);
            frozenPlant.setChillLevel(0);
        }
        if (frozenZombie != null && frozenZombie.isAlive()) {
            frozenZombie.setStatus(Zombie.Status.NORMAL);
        }
    }

    public Plant getFrozenPlant() { return frozenPlant; }
    public Zombie getFrozenZombie() { return frozenZombie; }
    public boolean contains(Object entity) { return entity == frozenPlant || entity == frozenZombie; }
    public int getHp() { return hp; }

    @Override public boolean blocksPlanting() { return true; }
    @Override public String getName() { return "Ice Block"; }
}
