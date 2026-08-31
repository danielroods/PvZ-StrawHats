package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.utils.GameSession;

public class TimedProjectileBurst implements PlantFoodEffect {
    public static final double DEFAULT_FIRE_INTERVAL = 0.25;
    private static final double DEFAULT_TAIL_SECONDS = 0.4;

    private final int burstCount;
    private final double fireInterval;
    private final double tailSeconds;
    private double elapsed = 0;
    private int fired = 0;
    private boolean finished = false;

    public TimedProjectileBurst(int burstCount) {
        this(burstCount, DEFAULT_FIRE_INTERVAL, DEFAULT_TAIL_SECONDS);
    }

    public TimedProjectileBurst(int burstCount, double fireInterval) {
        this(burstCount, fireInterval, DEFAULT_TAIL_SECONDS);
    }

    public TimedProjectileBurst(int burstCount, double fireInterval, double tailSeconds) {
        this.burstCount = Math.max(1, burstCount);
        this.fireInterval = fireInterval > 0 ? fireInterval : DEFAULT_FIRE_INTERVAL;
        this.tailSeconds = Math.max(0.0, tailSeconds);
    }

    public int getBurstCount() {
        return burstCount;
    }

    public double getFireInterval() {
        return fireInterval;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        onBurstStart(plant, session);
        fireOnce(plant, session);
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        GameSession session = GameSession.peekInstance();
        onBurstTick(plant, session, deltaTimeSeconds);
        while (elapsed >= fireInterval && fired < burstCount) {
            elapsed -= fireInterval;
            fireOnce(plant, session);
        }
        if (fired >= burstCount && !finished) {
            finished = true;
            onBurstEnd(plant, session);
        }
    }

    private void fireOnce(Plant plant, GameSession session) {
        if (plant.getActStrategy() == null || session == null || fired >= burstCount) return;
        plant.setInternalTimer(0);
        plant.getActStrategy().act(plant, session);
        fired++;
    }

    protected void onBurstStart(Plant plant, GameSession session) {
    }

    protected void onBurstTick(Plant plant, GameSession session, double deltaTimeSeconds) {
    }

    protected void onBurstEnd(Plant plant, GameSession session) {
    }

    @Override
    public double getDurationSeconds() {
        return burstCount * fireInterval + tailSeconds;
    }

    @Override
    public boolean drivesActStrategy() {
        return true;
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }

    @Override
    public void reset() {
        elapsed = 0;
        fired = 0;
        finished = false;
    }

    /** True once every shot of the burst has fired. Used by Cactus, whose Plant Food
     * timer runs forever, to know when to stop waiting on this burst and let its normal
     * ActStrategy cadence take back over - see Plant#tick. */
    public boolean isBurstFinished() {
        return fired >= burstCount;
    }
}
