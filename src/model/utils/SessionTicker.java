package model.utils;

import model.collections.Item;
import model.collections.item.GroundPlantFood;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match.main.season.travellog.cave.IceWind;
import model.match_mechanisms.vector.Position;
import model.pitches.LawnMower;
import model.projectile.Projectile;
import model.projectile.zombie_projectile.ZombieProjectile;
import service.GameClock;

import java.util.List;

/**
 * Drives one fixed simulation tick of a match: entity updates, hazards, wave scheduling,
 * sky sun drops, corpse/grid cleanup, and the win/loss evaluation at the end of the tick.
 */
class SessionTicker {

    private final GameSession session;

    SessionTicker(GameSession session) {
        this.session = session;
    }

    void tick() {
        if (session.gameOver || session.gameWon) return;

        SessionHazards hazards = session.hazards();
        SessionEconomy economy = session.economy();
        SessionBoard board = session.board();

        session.clock().tick();
        double deltaTimeSeconds = GameClock.SECONDS_PER_TICK;

        model.match.boss.ZombossFight bossFight = session.getZombossFight();
        if (bossFight != null) {
            bossFight.tick(deltaTimeSeconds);
            if (bossFight.isCutscene()) return;
        }

        economy.tickPlantCooldowns(deltaTimeSeconds);

        if (session.getLevel() != null) {
            session.getLevel().updateTide(deltaTimeSeconds, session);
            if (session.getLevel().getSeason() != null) {
                session.getLevel().getSeason().applyPerTickEffect(session, deltaTimeSeconds);
            }
        }

        hazards.updateSandStorm(deltaTimeSeconds);
        IceWind.tick(session, deltaTimeSeconds);
        hazards.updateBeachBigWave(deltaTimeSeconds);

        List<Plant> plants = session.getPlants();
        for (int i = plants.size() - 1; i >= 0; i--) {
            Plant plant = plants.get(i);
            if (plant.isAlive()) plant.tick(deltaTimeSeconds, session);
        }
        List<Zombie> zombies = session.getZombies();
        for (int i = zombies.size() - 1; i >= 0; i--) {
            Zombie zombie = zombies.get(i);
            if (zombie.isAlive() && !hazards.isEnteringWithHazard(zombie)
                    && !session.isFrozenInIceBlock(zombie)) {
                zombie.tick(deltaTimeSeconds, session);
            }
        }
        List<Projectile> projectiles = session.getProjectiles();
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            if (projectile.isAlive()) {
                projectile.tick();
            }
        }
        List<ZombieProjectile> zombieProjectiles = session.getZombieProjectiles();
        for (int i = zombieProjectiles.size() - 1; i >= 0; i--) {
            ZombieProjectile zombieProjectile = zombieProjectiles.get(i);
            if (zombieProjectile.isAlive()) zombieProjectile.tick();
        }
        List<Item> items = session.getItems();
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            if (item.isAlive()) item.tick();
        }
        LawnMower[] lawnMowers = session.getLawnMowers();
        for (int i = 0; i < lawnMowers.length; i++) {
            if (lawnMowers[i] != null) {
                lawnMowers[i].tick(deltaTimeSeconds, session);
            }
        }

        if (session.isWavesStarted()) session.waves().tickWaveScheduler(deltaTimeSeconds);

        if (session.isWavesStarted() && session.isSkySunEnabledForSession()) {
            double skySunTimer = economy.advanceSkySunTimer(deltaTimeSeconds);
            if (GameClock.hasReached(skySunTimer, economy.getEffectiveSkySunInterval())) {
                economy.resetSkySunTimer();
                int col = GameSession.ITEM_RANDOM.nextInt(session.getEnvironment().getCols());
                int row = GameSession.ITEM_RANDOM.nextInt(session.getEnvironment().getRows());
                GroundSun sun = GroundSun.fallFromSky(new Position(col, row));
                session.getItems().add(sun);
                view.GeneralPrinter.print("New " + sun.getDropType().name().toLowerCase()
                        + " sun is dropping at position (" + (col + 1) + ", " + (row + 1) + ").");
            }
        }

        for (int i = session.getZombies().size() - 1; i >= 0; i--) {
            Zombie zombie = session.getZombies().get(i);
            if (!zombie.isAlive() && zombie.isPlantFoodPending()) {
                session.getItems().add(new GroundPlantFood(zombie.getPosition()));
                zombie.clearPlantFoodPending();
            }
        }

        hazards.pruneSandStormEntries(session.getZombies());
        board.clearDeadPlantsFromGrid();
        board.clearDeadStructuresFromGrid();
        board.refreshZombieOccupancy();

        recordLevelSpecificDeaths();
        tickLevelSpecificLogic(deltaTimeSeconds);

        economy.countPlantsLost(session.getPlants());
        session.getPlants().removeIf(p -> !p.isAlive());
        session.getZombies().removeIf(z -> !z.isAlive());
        session.getItems().removeIf(i -> !i.isAlive());
        session.getProjectiles().removeIf(p -> !p.isAlive());
        session.getZombieProjectiles().removeIf(p -> !p.isAlive());

        if (session.isZombieBreachesEnabled()) board.checkZombieBreaches();

        if (session.getLevel() != null && session.getLevel().checkLossCondition(session)) {
            session.gameOver = true;
        }

        boolean levelWon = session.getLevel() != null
                ? session.getLevel().checkWinCondition(session)
                : session.isWavesStarted() && session.allWavesSpawned() && session.getZombies().isEmpty();
        if (levelWon && !session.gameOver) {
            if (!session.gameWon) {
                session.gameWon = true;
                controller.QuestManager.notifyLevelWon(session);
            }
        }
    }

    private void recordLevelSpecificDeaths() {
        if (session.getLevel() instanceof model.match.main.levels.special_levels.LoveYourPlantsLevel loveLevel) {
            session.getPlants().stream().filter(p -> !p.isAlive()).forEach(p -> loveLevel.recordPlantLoss());
        }
        if (session.getLevel() instanceof model.match.main.levels.special_levels.TimedWarLevel timedWarLevel) {
            session.getZombies().stream().filter(z -> !z.isAlive()).forEach(z -> timedWarLevel.recordZombieKill());
            timedWarLevel.tickTimer(GameClock.SECONDS_PER_TICK);
        }
    }

    private void tickLevelSpecificLogic(double deltaTimeSeconds) {
        if (session.getLevel() instanceof model.match.main.levels.special_levels.ConveyorBeltLevel conveyorLevel) {
            conveyorLevel.tickConveyor(deltaTimeSeconds);
        }
    }
}
