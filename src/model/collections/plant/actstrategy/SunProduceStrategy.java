package model.collections.plant.actstrategy;

import model.collections.item.GroundSun;
import model.collections.plant.AbilityType;
import model.collections.plant.Plant;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import service.GameClock;
import view.GeneralPrinter;

import java.util.Random;

public class SunProduceStrategy implements ActStrategy {
    private static final double DOUBLE_SUN_PROBABILITY = 0.5;
    private static final double BOSS_LEVEL_INTERVAL_SCALE = 0.5;

    
    
    
    
    private static final Position[] SUN_DROP_OFFSETS = {
            new Position(0.0, 0.0),
            new Position(0.22, 0.0),
            new Position(-0.22, 0.0),
            new Position(0.18, 0.0)
    };
    private static final double DROP_JITTER = 0.06;
    
    
    private static final double NEARBY_SUN_RADIUS = 0.35;

    private static final Random RANDOM = new Random();

    @Override
    public void act(Plant user, GameSession session) {
        if (!GameClock.isZero(user.getIntervalTimer())) return;

        Position location = user.getLocation();
        if (location == null) return;

        int sunValue = Math.max(0, (int) user.getAbilityValue());
        if (user.hasSpecialUpgrade("DOUBLE_SUN_CHANCE")
                && Math.random() < DOUBLE_SUN_PROBABILITY) {
            sunValue *= 2;
        }
        if (user.getAbilityType() == AbilityType.INSTANT_SUN_BURST) {
            session.addSun(sunValue);
            GeneralPrinter.print("plant " + user.getName() + " produced " + sunValue + " suns.");
            user.setAlive(false);
            return;
        }

        boolean sunAlreadyExists = session.getItems().stream()
                .anyMatch(item -> item instanceof GroundSun sun
                        && !sun.isCollected()
                        && item.isAlive()
                        && item.getPosition() != null
                        && item.getPosition().distanceTo(location) < NEARBY_SUN_RADIUS);

        if (sunAlreadyExists) return;

        int sunCount = sunCountFor(user);
        int[] shares = splitSunValue(sunValue, sunCount);
        for (int i = 0; i < sunCount; i++) {
            Position dropPosition = dropOffsetPosition(location, i);
            session.getItems().add(new GroundSun(dropPosition, shares[i], true));
        }
        GeneralPrinter.print("plant " + user.getName() + " produced " + sunCount
                + " sun(s) near (" + ((int) location.x() + 1) + ", " + ((int) location.y() + 1) + ").");

        user.setInternalTimer(user.getActionInterval()
                * (session.isDoubleSunRate() ? BOSS_LEVEL_INTERVAL_SCALE : 1.0));
    }

    /**
     * How many separate sun drops a single production cycle spawns, based on the plant's
     * type. Twin Sunflower represents two flowers at once, so it drops two suns per cycle;
     * every other sun producer (Sunflower, Primal Sunflower, Sun-shroom) drops one.
     */
    private int sunCountFor(Plant plant) {
        if ("Twin Sunflower".equalsIgnoreCase(plant.getName())) return 2;
        return 1;
    }

    
    private int[] splitSunValue(int totalValue, int count) {
        int[] shares = new int[count];
        int base = totalValue / count;
        int remainder = totalValue - base * count;
        for (int i = 0; i < count; i++) {
            shares[i] = base + (i < remainder ? 1 : 0);
        }
        return shares;
    }

    private Position dropOffsetPosition(Position location, int index) {
        Position offset = SUN_DROP_OFFSETS[index % SUN_DROP_OFFSETS.length];
        double jitterX = (RANDOM.nextDouble() * 2 - 1) * DROP_JITTER;
        
        
        
        
        double x = location.x() + offset.x() + jitterX;
        
        
        double maxSpread = 0.45;
        x = Math.max(location.x() - maxSpread, Math.min(location.x() + maxSpread, x));
        return new Position(x, location.y());
    }
}