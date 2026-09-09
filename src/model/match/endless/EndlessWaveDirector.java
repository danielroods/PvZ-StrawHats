package model.match.endless;

import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.waves.WaveDirector;
import model.match.waves.WaveType;
import model.match_mechanisms.ZombieWave;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

/**
 * Generates the endless Lottery schedule one wave at a time. Nothing here is bounded by a
 * wave count: every quantity that grows with the wave number is either clamped to a
 * ceiling or grows in a way that stays inside its type, so wave 10 and wave 10,000 are
 * both well defined.
 * <p>
 * Three ramps run at once, which is what makes a run get harder the longer it survives:
 * the point budget of a wave (more zombies), the slice of the chapter roster that is
 * allowed to show up and how hard it is weighted towards the expensive end (nastier
 * zombies), and the lull between waves (harder pacing). Past the point where the budget
 * ceiling and the per-wave zombie cap both bite, {@link #empower} keeps the pressure
 * climbing by scaling the zombies themselves instead of adding more of them.
 */
public final class EndlessWaveDirector implements WaveDirector {

    private static final int BASE_WAVE_BUDGET = 260;
    private static final int LINEAR_BUDGET_STEP = 90;
    private static final double QUADRATIC_BUDGET_STEP = 3.0;
    private static final int MAX_WAVE_BUDGET = 30_000;

    private static final int MIN_ZOMBIES_PER_WAVE = 2;
    private static final int MAX_ZOMBIES_PER_WAVE = 45;

    private static final int STARTING_ROSTER_TIERS = 2;
    private static final double ROSTER_TIERS_PER_WAVE = 0.5;
    private static final double MAX_STRENGTH_BIAS = 3.5;
    private static final double STRENGTH_BIAS_PER_WAVE = 0.075;
    private static final double FLAG_WAVE_BIAS_BONUS = 1.5;

    private static final double START_DELAY_SECONDS = 26.0;
    private static final double MIN_DELAY_SECONDS = 8.0;
    private static final double DELAY_DECAY_PER_WAVE = 0.45;

    private static final int FLAG_WAVE_PERIOD = 5;
    private static final int RAMP_WAVES = 40;

    private static final double POWER_PER_WAVE = 0.03;
    private static final double MAX_POWER_MULTIPLIER = 10.0;
    private static final double EAT_DPS_SHARE_OF_POWER = 0.35;
    private static final int MAX_ZOMBIE_HP = 20_000_000;

    private final List<String> roster;
    private final int columns;
    private final Random random;

    public EndlessWaveDirector(List<String> chapterRoster, int columns, Random random) {
        this.roster = orderByThreat(chapterRoster);
        this.columns = Math.max(1, columns);
        this.random = random == null ? new Random() : random;
    }

    public List<String> getRoster() {
        return List.copyOf(roster);
    }

    private static List<String> orderByThreat(List<String> chapterRoster) {
        List<String> ordered = new ArrayList<>(new LinkedHashSet<>(
                chapterRoster == null ? List.of() : chapterRoster));
        ordered.removeIf(alias -> alias == null || alias.isBlank());
        ordered.sort(Comparator.comparingInt(ZombieFactory::getZombieCost)
                .thenComparing(Comparator.naturalOrder()));
        return ordered;
    }

    @Override
    public ZombieWave waveAt(int waveIndex) {
        int index = Math.max(0, waveIndex);
        List<Zombie> zombies = new ArrayList<>();
        if (roster.isEmpty()) return new ZombieWave(delaySeconds(index), zombies);

        List<String> unlocked = unlockedRoster(index);
        double bias = strengthBias(index, typeOf(index));
        int budget = budgetFor(index);
        int spent = 0;

        while (zombies.size() < MAX_ZOMBIES_PER_WAVE
                && (spent < budget || zombies.size() < MIN_ZOMBIES_PER_WAVE)) {
            String alias = pick(unlocked, bias);
            zombies.add(ZombieFactory.create(alias, 0, columns - 1));
            spent += ZombieFactory.getZombieCost(alias);
        }
        return new ZombieWave(delaySeconds(index), zombies);
    }

    @Override
    public WaveType typeOf(int waveIndex) {
        return (waveIndex + 1) % FLAG_WAVE_PERIOD == 0 ? WaveType.FLAG : WaveType.NORMAL;
    }

    @Override
    public double delaySeconds(int waveIndex) {
        double decayed = START_DELAY_SECONDS - DELAY_DECAY_PER_WAVE * Math.max(0, waveIndex);
        return Math.max(MIN_DELAY_SECONDS, decayed);
    }

    @Override
    public double rampProgress(int waveIndex) {
        return Math.min(1.0, Math.max(0, waveIndex) / (double) RAMP_WAVES);
    }

    @Override
    public void empower(Zombie zombie, int waveIndex) {
        if (zombie == null) return;
        double power = powerMultiplier(waveIndex);
        if (power <= 1.0) return;

        long scaledHp = Math.min(MAX_ZOMBIE_HP, Math.round(zombie.getMaxHp() * power));
        zombie.setMaxHp((int) scaledHp);
        zombie.setHp((int) scaledHp);
        zombie.setEatDps(zombie.getEatDps() * (1.0 + (power - 1.0) * EAT_DPS_SHARE_OF_POWER));
    }

    public int budgetFor(int waveIndex) {
        int index = Math.max(0, waveIndex);
        double raw = BASE_WAVE_BUDGET
                + (double) LINEAR_BUDGET_STEP * index
                + QUADRATIC_BUDGET_STEP * (double) index * index;
        return (int) Math.min(MAX_WAVE_BUDGET, raw);
    }

    public double powerMultiplier(int waveIndex) {
        return Math.min(MAX_POWER_MULTIPLIER,
                1.0 + POWER_PER_WAVE * Math.max(0, waveIndex));
    }

    private List<String> unlockedRoster(int waveIndex) {
        int tiers = (int) Math.min(roster.size(),
                STARTING_ROSTER_TIERS + ROSTER_TIERS_PER_WAVE * waveIndex);
        return roster.subList(0, Math.max(1, tiers));
    }

    private double strengthBias(int waveIndex, WaveType type) {
        double bias = Math.min(MAX_STRENGTH_BIAS, STRENGTH_BIAS_PER_WAVE * waveIndex);
        return type.isHuge() ? bias + FLAG_WAVE_BIAS_BONUS : bias;
    }

    private String pick(List<String> unlocked, double bias) {
        int size = unlocked.size();
        if (size == 1) return unlocked.get(0);

        double total = 0;
        double[] weights = new double[size];
        for (int i = 0; i < size; i++) {
            weights[i] = 1.0 + bias * (i / (double) (size - 1));
            total += weights[i];
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < size; i++) {
            roll -= weights[i];
            if (roll <= 0) return unlocked.get(i);
        }
        return unlocked.get(size - 1);
    }
}
