package model.match.main.levels.special_levels;

import model.match.main.levels.Level;
import model.match_mechanisms.Time;
import model.utils.GameSession;

/**
 * Survival mode: huge, near-continuous zombie waves, and the player wins simply by lasting
 * until the clock runs out - there is no kill quota. Losing still works exactly like every
 * other level (a zombie reaching the house / lawnmower line, handled by the normal
 * {@code checkZombieBreaches()} path in {@code SessionTicker}), so this level intentionally
 * never reports its own loss condition.
 */
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

    /** Kept for the HUD / end-of-match stats only - kills no longer gate win or loss. */
    public void recordZombieKill() {
        zombiesKilledSoFar++;
    }

    /**
     * Survival levels never lose on their own account - only running out of lawnmowers /
     * a zombie reaching the house (handled generically for every level) can end the match
     * early. Previously this also failed the level whenever the timer hit zero, which made
     * "survive the timer" impossible to actually win.
     */
    @Override
    public boolean checkLossCondition(GameSession session) {
        return false;
    }

    /** Survived the full duration: the timer has run out and the player is still standing. */
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

    /** Seconds left on the survival clock, for the HUD countdown. 0 if there is no timer. */
    public double getSecondsRemaining() {
        return timeLimit == null ? 0 : Math.max(0, timeLimit.getSecondsRemaining());
    }

    /** Full survival duration in seconds (e.g. 120 for a 2-minute survive level), for the HUD
     *  countdown's progress fraction. */
    public double getTotalDurationSeconds() {
        return configuredTimeLimitSeconds;
    }
}