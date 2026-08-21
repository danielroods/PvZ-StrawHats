package model.projectile.zombie_projectile;

import model.collections.Item;
import model.collections.plant.Plant;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import service.GameClock;

import java.lang.reflect.Field;

public abstract class ZombieProjectile extends Item {
    protected Position startPosition;
    protected Position targetPosition;
    protected double flightTime;
    protected double elapsedTimer = 0.0;
    protected String sourceZombieAlias;
    protected GameSession session;

    public ZombieProjectile(Position startPosition, Position targetPosition, double flightTime, String sourceZombieAlias, GameSession session) {
        super(startPosition, 1);
        this.startPosition = startPosition;
        this.targetPosition = targetPosition;
        this.setPosition(startPosition);
        this.flightTime = flightTime;
        this.sourceZombieAlias = sourceZombieAlias;
        this.session = session;
    }

    @Override
    public void tick() {
        if (!isAlive()) return;

        elapsedTimer += GameClock.SECONDS_PER_TICK;
        double progress = elapsedTimer / flightTime;

        if (progress >= 1.0) {
            this.setPosition(targetPosition);
            onDestinationReached(session);
            this.setAlive(false);
        } else {
            updateFlightPath(progress);
        }
    }

    public String getSourceZombieAlias() {
        return sourceZombieAlias;
    }

    protected abstract void updateFlightPath(double progress);

    protected abstract void onDestinationReached(GameSession session);

    protected boolean isPlantIncapacitated(Plant plant) {
        if (plant == null) return false;
        try {
            Field stateField = Plant.class.getDeclaredField("state");
            stateField.setAccessible(true);
            Object stateValue = stateField.get(plant);
            return stateValue != null && "INCAPACITATED".equals(stateValue.toString());
        } catch (Exception e) {
            return false;
        }
    }

    protected int getChillLevel(Plant plant) {
        if (plant == null) return 0;
        try {
            Field chillField = Plant.class.getDeclaredField("chillLevel");
            chillField.setAccessible(true);
            return chillField.getInt(plant);
        } catch (Exception e) {
            return 0;
        }
    }

    protected void setChillLevel(Plant plant, int level) {
        if (plant == null) return;
        try {
            Field chillField = Plant.class.getDeclaredField("chillLevel");
            chillField.setAccessible(true);
            chillField.setInt(plant, level);
        } catch (Exception e) {
        }
    }
}