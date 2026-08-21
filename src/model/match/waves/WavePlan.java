package model.match.waves;

import java.util.ArrayList;
import java.util.List;

public final class WavePlan {

    private final int waveIndex;
    private final WaveType type;
    private final List<ScheduledSpawn> spawns;

    public WavePlan(int waveIndex, WaveType type, List<ScheduledSpawn> spawns) {
        this.waveIndex = waveIndex;
        this.type = type;
        this.spawns = List.copyOf(spawns);
    }

    public int getWaveIndex() {
        return waveIndex;
    }

    public int getWaveNumber() {
        return waveIndex + 1;
    }

    public WaveType getType() {
        return type;
    }

    public List<ScheduledSpawn> getSpawns() {
        return spawns;
    }

    public int size() {
        return spawns.size();
    }

    public boolean isEmpty() {
        return spawns.isEmpty();
    }

    public double getSpawnWindowSeconds() {
        if (spawns.isEmpty()) return 0;
        return spawns.get(spawns.size() - 1).offsetSeconds();
    }

    public WavePlan compressed(double maxWindowSeconds) {
        double window = getSpawnWindowSeconds();
        if (window <= maxWindowSeconds || window <= 0) return this;
        double scale = maxWindowSeconds / window;
        List<ScheduledSpawn> scaled = new ArrayList<>(spawns.size());
        for (ScheduledSpawn spawn : spawns) {
            scaled.add(new ScheduledSpawn(spawn.alias(), spawn.offsetSeconds() * scale,
                    spawn.preferredLane()));
        }
        return new WavePlan(waveIndex, type, scaled);
    }

    @Override
    public String toString() {
        return "WavePlan{wave=" + getWaveNumber() + ", type=" + type
                + ", zombies=" + spawns.size()
                + ", window=" + String.format("%.1fs", getSpawnWindowSeconds()) + '}';
    }
}
