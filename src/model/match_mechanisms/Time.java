package model.match_mechanisms;

import service.GameClock;

public class Time {
    // ---- Global game tick (static) ----
    private static int tick;


    public static int getTick() {
        return tick;
    }

    public static void setTick(int tick) {
        Time.tick = tick;
    }

    // ---- Instance timer (for level timers, e.g., TimedWar) ----
    private double secondsRemaining;
    private boolean running = false;

    public Time(double seconds) {
        this.secondsRemaining = seconds;
        this.running = true;
    }

    /// Decrease the timer by delta seconds.
    public void tick(double delta) {
        if (running && !GameClock.isZero(secondsRemaining))
            secondsRemaining = GameClock.countDown(secondsRemaining, delta);

    }

    public double getSecondsRemaining() {
        return secondsRemaining;
    }

    public boolean isZero() {
        return GameClock.isZero(secondsRemaining);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void reset(double seconds) {
        this.secondsRemaining = seconds;
        this.running = true;
    }
}
