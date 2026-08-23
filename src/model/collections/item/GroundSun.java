package model.collections.item;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.user_data.UserState;
import model.utils.GameSession;
import service.GameClock;
import view.GeneralPrinter;

import java.util.Random;

public class GroundSun extends GroundItem {
    public static final String NORMAL_SUN_PAM_PATH = "768/INITIAL/EFFECTS/SUN/SUN.PAM";
    public static final String RADIOACTIVE_SUN_PAM_PATH = "768/FULL/EFFECTS/SUN_BOMB/SUN_BOMB.PAM";

    public static String getPamAnimationPath(SunDropType type) {
        if (type == null) return NORMAL_SUN_PAM_PATH;

        return switch (type) {
            case REGULAR, SPECIAL -> NORMAL_SUN_PAM_PATH;
            case RADIOACTIVE -> RADIOACTIVE_SUN_PAM_PATH;
        };
    }

    public static String getPamAnimationClip(SunDropType type) {
        if (type == null) return "YOUR_NORMAL_SUN_CLIP";

        return switch (type) {
            case REGULAR -> "animation";
            case SPECIAL -> "blue";
            case RADIOACTIVE -> "animation2";
        };
    }

    private static final Random RANDOM = new Random();

    public enum SunDropType {
        REGULAR(80, 25),
        SPECIAL(15, 100),
        RADIOACTIVE(5, 25);

        private final int probability;
        private final int value;

        SunDropType(int probability, int value) {
            this.probability = probability;
            this.value = value;
        }

        public int getProbability() {
            return probability;
        }

        public int getValue() {
            return value;
        }

        public static SunDropType rollRandom() {
            int roll = RANDOM.nextInt(100);
            int cumulative = 0;
            for (SunDropType type : values()) {
                cumulative += type.probability;
                if (roll < cumulative) return type;
            }
            return REGULAR;
        }
    }

    private static final double FALL_SPEED_PX_PER_SECOND = 45.0;
    private static final double FALL_START_OFFSET_PX = 35.0;
    private static final double FALL_SPRITE_OFFSET_TILES = 0.75;
    private static final double GROUND_LIFETIME_SECONDS = 20.0;

    private SunDropType dropType;
    private final int sunValue;
    private final boolean plantProduced;
    private double fallDurationSeconds;
    private double fallSecondsRemaining;

    public GroundSun(Position position, int sunValue) {
        this(position, sunValue, false);
    }

    public GroundSun(Position position, int sunValue, boolean plantProduced) {
        super(ItemType.SUN, position, 0, 0.6);
        this.dropType = SunDropType.REGULAR;
        this.sunValue = sunValue;
        this.plantProduced = plantProduced;
        this.fallSecondsRemaining = 0;
    }

    private GroundSun(Position position, SunDropType dropType) {
        super(ItemType.SUN, position, GROUND_LIFETIME_SECONDS + getFallDurationSeconds(position), 0.6);
        this.dropType = dropType;
        this.sunValue = dropType.getValue();
        this.plantProduced = false;
        this.fallDurationSeconds = getFallDurationSeconds(position);
        this.fallSecondsRemaining = fallDurationSeconds;
    }

    private static double getFallDurationSeconds(Position position) {
        double targetRow = Math.max(0.0, position == null ? 0.0 : position.y());
        double distancePx = targetRow * 96.0
                + FALL_SPRITE_OFFSET_TILES * 96.0
                + FALL_START_OFFSET_PX;
        return Math.max(0.1, distancePx / FALL_SPEED_PX_PER_SECOND);
    }

    public static GroundSun fallFromSky(Position position) {
        return new GroundSun(position, SunDropType.rollRandom());
    }

    @Override
    public void tick() {
        boolean wasFalling = isFalling();
        super.tick();
        if (wasFalling) {
            fallSecondsRemaining = GameClock.countDown(fallSecondsRemaining, GameClock.SECONDS_PER_TICK);
            if (!isFalling() && isAlive()) {
                if (dropType == SunDropType.RADIOACTIVE) {
                    dropType = SunDropType.REGULAR;
                }
                Position position = getPosition();
                if (position != null) {
                    GeneralPrinter.print("Sun reached the ground at position ("
                            + ((int) position.x() + 1) + ", " + ((int) position.y() + 1) + ").");
                }
            }
        }
    }

    public boolean isFalling() {
        return fallSecondsRemaining > 0.0 && isAlive();
    }

    public float getFallProgress() {
        if (fallDurationSeconds <= 0.0) return 1f;
        return (float) Math.max(0.0, Math.min(1.0,
                (fallDurationSeconds - fallSecondsRemaining) / fallDurationSeconds));
    }

    @Override
    public void applyRewards(GameSession session, UserState state) {
        if (dropType == SunDropType.RADIOACTIVE && isFalling()) {
            explodeRadioactive(session);
            return;
        }

        session.addSun(sunValue);
    }

    private void explodeRadioactive(GameSession session) {
        Position center = getPosition();
        if (center == null) return;

        for (Zombie zombie : session.getZombies()) {
            if (!zombie.isAlive() || zombie.getPosition() == null) continue;
            if (Math.abs(zombie.getPosition().y() - center.y()) <= 2
                    && Math.abs(zombie.getPosition().x() - center.x()) <= 2) {
                zombie.takeDamage(150, null);
            }
        }

        for (Plant plant : session.getPlants()) {
            if (!plant.isAlive() || plant.getPosition() == null) continue;
            if (Math.abs(plant.getPosition().y() - center.y()) <= 1
                    && Math.abs(plant.getPosition().x() - center.x()) <= 1) {
                plant.takeDamage(80, null);
            }
        }
    }

    public SunDropType getDropType() {
        return dropType;
    }

    public int getSunValue() {
        return sunValue;
    }

    public boolean isPlantProduced() {
        return plantProduced;
    }
}