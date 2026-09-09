package model.match_mechanisms;

import service.GameClock;

public class Time {
    
    private static int tick;


    public static int getTick() {
        return tick;
    }

    public static void setTick(int tick) {
        Time.tick = tick;
    }

    
    private double secondsRemaining;
    private boolean running = false;

    public Time(double seconds) {
        this.secondsRemaining = seconds;
        this.running = true;
    }

    
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
