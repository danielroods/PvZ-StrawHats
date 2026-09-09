package model.pitches.obstacles;

import java.util.Random;

public class Grave implements Obstacle {
    public enum Reward { NONE, SUN, PLANT_FOOD }

    public static final int MAX_HP = 700;
    public static final int STAGE_COUNT = 5;

    private static final Random RANDOM = new Random();
    private int hp = MAX_HP;
    private final Reward reward;

    public Grave() {
        this(Reward.NONE);
    }

    public Grave(Reward reward) {
        this.reward = reward == null ? Reward.NONE : reward;
    }

    public static Reward randomDarkAgeReward() {
        int roll = RANDOM.nextInt(Reward.values().length);
        return Reward.values()[roll];
    }

    public boolean takeDamage(int damage) {
        hp = Math.max(0, hp - Math.max(0, damage));
        return hp == 0;
    }

    public int getHp() { return hp; }
    public Reward getReward() { return reward; }

    /**
     * Returns the current visual stage of the grave, from 1 (undamaged) to
     * {@link #STAGE_COUNT} (about to be destroyed), based on remaining hp.
     */
    public int getStage() {
        double damageRatio = 1.0 - ((double) hp / (double) MAX_HP);
        int stage = (int) Math.ceil(damageRatio * STAGE_COUNT);
        if (stage < 1) stage = 1;
        if (stage > STAGE_COUNT) stage = STAGE_COUNT;
        return stage;
    }

    @Override
    public boolean blocksPlanting() { return true; }

    @Override
    public String getName() { return "Grave"; }
}