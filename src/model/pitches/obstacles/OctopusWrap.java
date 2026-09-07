package model.pitches.obstacles;

import model.collections.plant.Plant;

public class OctopusWrap implements Obstacle {
    private final Plant wrappedPlant;
    private int hp;

    public OctopusWrap(Plant wrappedPlant, int hp) {
        this.wrappedPlant = wrappedPlant;
        this.hp = hp;
    }


    public boolean isDead() {
        return hp <= 0;
    }

    public boolean takeDamage(int amount) {
        if (hp <= 0) return false;
        hp -= amount;
        if (hp <= 0) {
            hp = 0;
            release();
            return true;
        }
        return false;
    }

    public void release() {
        if (wrappedPlant != null) {
            wrappedPlant.setState(Plant.PlantState.ACTIVE);
        }
    }

    public Plant getWrappedPlant() {
        return wrappedPlant;
    }

    public int getHp() {
        return hp;
    }

    @Override
    public boolean blocksPlanting() {
        return hp > 0;
    }

    @Override
    public String getName() {
        return "Octopus Wrap";
    }
}
