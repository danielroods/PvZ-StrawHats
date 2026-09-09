package model.match.waves;

import model.collections.zombie.Zombie;
import model.match_mechanisms.ZombieWave;

/**
 * Supplies waves to {@code WaveScheduler} on demand instead of from a fixed, authored
 * list. A level that hands the session a director has no wave count at all: the
 * scheduler asks for wave N the moment it needs it, so the schedule can run forever.
 */
public interface WaveDirector {

    ZombieWave waveAt(int waveIndex);

    WaveType typeOf(int waveIndex);

    /** Seconds of lull the scheduler should aim for before wave {@code waveIndex} starts. */
    double delaySeconds(int waveIndex);

    /**
     * How far along the ramp wave {@code waveIndex} sits, 0..1. Feeds the same pacing
     * curve authored levels get from {@code waveIndex / (totalWaves - 1)}.
     */
    double rampProgress(int waveIndex);

    /** Last chance to scale a freshly created zombie before it lands on the lawn. */
    void empower(Zombie zombie, int waveIndex);
}
