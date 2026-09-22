package model.match.main.levels.special_levels;

import model.match.main.levels.Level;
import model.match_mechanisms.Time;
import model.utils.GameSession;


public class TimedWarLevel extends Level {
    private Time timeLimit;
    private double configuredTimeLimitSeconds;
    private int zombiesKilledSoFar = 0;

    @Override
    public void initSpecial(GameSession session) {
        zombiesKilledSoFar = 0;
        if (timeLimit != null) timeLimit.reset(configuredTimeLimitSeconds);
    }

    public void tickTimer(double deltaSeconds) {
        if (timeLimit != null) {
            timeLimit.tick(deltaSeconds);
        }
    }

    
    public void recordZombieKill() {
        zombiesKilledSoFar++;
    }

    
    @Override
    public boolean checkLossCondition(GameSession session) {
        return false;
    }

    
    @Override
    public boolean checkWinCondition(GameSession session) {
        return timeLimit != null && timeLimit.isZero();
    }

    public int getZombiesKilledSoFar() { return zombiesKilledSoFar; }

    public Time getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Time timeLimit) {
        this.timeLimit = timeLimit;
        this.configuredTimeLimitSeconds = timeLimit == null ? 0 : timeLimit.getSecondsRemaining();
    }

    
    public double getSecondsRemaining() {
        return timeLimit == null ? 0 : Math.max(0, timeLimit.getSecondsRemaining());
    }

    
    public double getTotalDurationSeconds() {
        return configuredTimeLimitSeconds;
    }
}