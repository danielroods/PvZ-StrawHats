package controller.match;

import model.match_mechanisms.ZombieWave;

import java.util.List;

public class ZombieWaveController {
    private List<ZombieWave> waves;

    public List<ZombieWave> getWaves() {
        return waves;
    }

    public void setWaves(List<ZombieWave> waves) {
        this.waves = waves;
    }
}
