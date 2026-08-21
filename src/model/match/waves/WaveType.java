package model.match.waves;

public enum WaveType {
    NORMAL,
    FLAG,
    FINAL;

    public boolean isHuge() {
        return this != NORMAL;
    }
}
