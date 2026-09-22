package model.match.waves;

import model.collections.zombie.Zombie;
import model.match_mechanisms.ZombieWave;


public interface WaveDirector {

    ZombieWave waveAt(int waveIndex);

    WaveType typeOf(int waveIndex);

    
    double delaySeconds(int waveIndex);

    
    double rampProgress(int waveIndex);

    
    void empower(Zombie zombie, int waveIndex);
}
