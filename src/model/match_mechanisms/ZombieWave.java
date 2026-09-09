package model.match_mechanisms;

import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;

import java.util.List;


public class ZombieWave {
    private double delay;          
    private List<Zombie> waveZombies;
    private boolean isFinalWave;   

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
