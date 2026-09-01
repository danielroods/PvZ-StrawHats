package model.match.main.season.travellog.cave;

import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.main.season.Season;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.Tile;
import model.pitches.TileType;
import model.pitches.obstacles.SlipperyDirection;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Cave extends Season {
    private static final int ICE_TILE_COUNT = 4;
    private static final String FROSTED_ZOMBIE_ALIAS = "ZombieIceAgeTroglobite";
    private static final int FROSTED_ZOMBIE_BACK_COLUMNS = 3;
    private static final int FROSTED_ZOMBIE_COUNT_MULTIPLIER = 1;
    private static final int MIN_FROSTED_ZOMBIES = 2;
    private static final double ICE_WIND_CHANCE_PER_WAVE = 0.60;
    private static final double PERIODIC_WIND_MIN_SECONDS = 12.0;
    private static final double PERIODIC_WIND_MAX_SECONDS = 17.0;
    private static final Random RANDOM = new Random();
    private double windCooldown = 6.0;

    public Cave() {
        super("Frostbite Caves");
    }

    @Override
    public void placeSeasonObstacles(GameSession session) {
        if (session == null || session.getEnvironment() == null) return;
        Environment env = session.getEnvironment();

        int placed = 0;
        int attempts = 0;
        while (placed < ICE_TILE_COUNT && attempts < 100) {
            attempts++;
            int row = RANDOM.nextInt(env.getRows());
            int col = RANDOM.nextInt(env.getCols());
            Cell cell = env.getCell(row, col);
            if (cell != null && cell.getTile() == null) {
                SlipperyDirection direction = RANDOM.nextBoolean() ? SlipperyDirection.UP : SlipperyDirection.DOWN;
                cell.setTile(new Tile(TileType.Slippery, direction));
                placed++;
            }
        }

        // A Zomboss level opens on an empty lawn (five silent seconds), so the season's
        // usual pre-frozen Troglobites are left out of it.
        if (session.getZombossFight() == null) placeFrostedZombies(session);
    }

    private static void placeFrostedZombies(GameSession session) {
        int scheduled = countScheduledFrostedZombies(session);
        int count = Math.max(scheduled * FROSTED_ZOMBIE_COUNT_MULTIPLIER, MIN_FROSTED_ZOMBIES);

        Environment env = session.getEnvironment();
        int backColumnStart = Math.max(0, env.getCols() - FROSTED_ZOMBIE_BACK_COLUMNS);
        List<Cell> candidates = new ArrayList<>();
        for (int row = 0; row < env.getRows(); row++) {
            for (int col = backColumnStart; col < env.getCols(); col++) {
                Cell cell = env.getCell(row, col);
                if (cell != null && cell.getObstacle() == null
                        && cell.getStructure() == null && !cell.hasPlant()) {
                    candidates.add(cell);
                }
            }
        }
        java.util.Collections.shuffle(candidates, RANDOM);

        int placed = 0;
        for (Cell cell : candidates) {
            if (placed >= count) break;
            Zombie zombie = ZombieFactory.create(FROSTED_ZOMBIE_ALIAS, cell.getRow(), cell.getCol());
            session.spawnZombie(zombie);
            if (FrostbiteFreezing.freezeZombieInIce(session, zombie, cell.getRow(), cell.getCol())) {
                placed++;
            }
        }
    }

    private static int countScheduledFrostedZombies(GameSession session) {
        if (session.getLevel() == null || session.getLevel().getWaves() == null) return 1;
        int count = 0;
        for (var wave : session.getLevel().getWaves()) {
            if (wave.getWaveZombies() == null) continue;
            for (Zombie template : wave.getWaveZombies()) {
                if (ZombieFactory.shouldSpawnFrosted(template.getAlias())) count++;
            }
        }
        return Math.max(count, 1);
    }

    @Override
    public void onWaveStart(GameSession session, int waveIndex) {
        windCooldown = 5.0 + RANDOM.nextDouble() * 4.0;
        if (RANDOM.nextDouble() < ICE_WIND_CHANCE_PER_WAVE) {
            IceWind.blow(session);
            windCooldown = PERIODIC_WIND_MIN_SECONDS + RANDOM.nextDouble()
                    * (PERIODIC_WIND_MAX_SECONDS - PERIODIC_WIND_MIN_SECONDS);
        }
    }

    @Override
    public void applyPerTickEffect(GameSession session, double deltaSeconds) {
        FrostbiteFreezing.meltFromAdjacentFirePlants(session, deltaSeconds);
        windCooldown -= Math.max(0, deltaSeconds);
        if (session != null && session.isWavesStarted() && !IceWind.isActive(session) && windCooldown <= 0) {
            IceWind.blow(session);
            windCooldown = PERIODIC_WIND_MIN_SECONDS + RANDOM.nextDouble()
                    * (PERIODIC_WIND_MAX_SECONDS - PERIODIC_WIND_MIN_SECONDS);
        }
    }

    /**
     * Melts a zombie free of its ice directly (as opposed to the ice block's HP being
     * chipped away to zero). Routes through the same IceBlock still sitting on the
     * zombie's cell so the obstacle is cleared and the "ice falls from the sky as a
     * pushable block" hand-off in IceBlock.release() still happens - keeping this in
     * sync with the normal damage-based melt path in FrostbiteFreezing.
     */
    public static void meltIce(Zombie zombie) {
        if (zombie == null) return;
        if (zombie.getStatus() != Zombie.Status.FREEZE && zombie.getStatus() != Zombie.Status.FROZEN) return;

        GameSession session = GameSession.peekInstance();
        Cell cell = null;
        if (session != null && session.getEnvironment() != null && zombie.getPosition() != null) {
            int row = (int) Math.round(zombie.getPosition().y());
            int col = (int) Math.round(zombie.getPosition().x());
            cell = session.getEnvironment().getCell(row, col);
        }

        if (cell != null && cell.getObstacle() instanceof model.pitches.obstacles.IceBlock iceBlock
                && iceBlock.getFrozenZombie() == zombie) {
            cell.setObstacle(null);
            iceBlock.release();
        } else {
            zombie.setStatus(Zombie.Status.NORMAL);
        }
    }
}