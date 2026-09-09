package model.match_mechanisms;

import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;

import java.util.List;

/// Represents a single wave of zombies in a level,and a list of zombies that spawn together.
public class ZombieWave {
    private double delay;          // seconds after previous wave
    private List<Zombie> waveZombies;
    private boolean isFinalWave;   // optional flag for the last wave

    public ZombieWave(double delay, List<Zombie> waveZombies) {
        this.delay = delay;
        this.waveZombies = waveZombies;
        this.isFinalWave = false;
    }

    public ZombieWave(double delay, List<Zombie> waveZombies, boolean isFinalWave) {
        this.delay = delay;
        this.waveZombies = waveZombies;
        this.isFinalWave = isFinalWave;
    }

    public double getDelay() {
        return delay;
    }

    public void setDelay(double delay) {
        this.delay = delay;
    }

    public List<Zombie> getWaveZombies() {
        return waveZombies;
    }

    public void setWaveZombies(List<Zombie> waveZombies) {
        this.waveZombies = waveZombies;
    }

    public boolean isFinalWave() {
        return isFinalWave;
    }

    public void setFinalWave(boolean isFinalWave) {
        this.isFinalWave = isFinalWave;
    }

    // Returns the total "cost" or difficulty of this wave, for wave difficulty calculations.
    public int getWaveCost() {
        if (waveZombies == null) return 0;
        return waveZombies.stream().mapToInt(zombie -> ZombieFactory.getZombieCost(zombie.getAlias())).sum();
    }

    @Override
    public String toString() {
        return "ZombieWave{" +
                "delay=" + delay +
                ", zombieCount=" + (waveZombies != null ? waveZombies.size() : 0) +
                ", final=" + isFinalWave +
                '}';
    }
}
