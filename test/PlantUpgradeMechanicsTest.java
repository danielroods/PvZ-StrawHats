import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantProgression;
import model.collections.plant.PlantStats;
import model.collections.plant.UpgradeEffects;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantUpgradeMechanicsTest {

    private static final int ROWS = 5;
    private static final int COLS = 9;

    private GameSession session;

    @BeforeEach
    void setUp() {
        PlantFactory.autoInit();
        session = new GameSession(ROWS, COLS);
    }

    private Plant plant(String name, int level, int col, int row) {
        Plant plant = PlantFactory.createPlantByName(name, level, new Position(col, row));
        assertTrue(session.plantAt(row, col, plant), name + " should be plantable");
        return plant;
    }

    private Zombie spawn(String alias, int col, int row, int hp) {
        Zombie zombie = ZombieFactory.create(alias, row, col);
        zombie.setPosition(new Position(col, row));
        zombie.setHp(hp);
        session.getZombies().add(zombie);
        return zombie;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) session.tick();
    }

    @Test
    void everySpecialUpgradeTagInTheDatasetReachesThePlantedPlant() {
        for (var config : PlantFactory.getBlueprints().values()) {
            int maxLevel = PlantProgression.maxLevel(config);
            PlantStats stats = PlantStats.of(config, maxLevel);
            if (stats.specialTags().isEmpty()) continue;

            Plant planted = PlantFactory.createPlant(config.id, maxLevel, new Position(1, 1));
            for (String tag : stats.specialTags()) {
                assertTrue(planted.hasSpecialUpgrade(tag),
                        config.name + " should carry its " + tag + " perk at level " + maxLevel);
            }
        }
    }

    @Test
    void aTaggedBuffUpgradeStillRegistersItsPerkName() {
        Plant mine = PlantFactory.createPlant(
                PlantFactory.findPlantIdByName("Potato Mine"), 2, new Position(1, 1));
        assertTrue(mine.hasSpecialUpgrade("ARM_TIME_REDUCTION"),
                "a perk named on a stat buff must still be recorded on the plant");
        assertEquals(-3.0, mine.getSpecialUpgrade("ARM_TIME_REDUCTION", 0), 1e-9);
    }

    @Test
    void anUpgradedKernelPultKeepsItsHigherButterChance() {
        Plant fresh = PlantFactory.createPlantByName("Kernel-pult", 1, new Position(1, 1));
        Plant upgraded = PlantFactory.createPlantByName("Kernel-pult", 2, new Position(1, 1));

        assertEquals(0.0, fresh.getSpecialUpgrade("BUTTER_CHANCE_BUFF", 0), 1e-9);
        assertEquals(0.05, upgraded.getSpecialUpgrade("BUTTER_CHANCE_BUFF", 0), 1e-9);
    }

    @Test
    void anUpgradedSnowPeaChillsForLonger() {
        Plant fresh = PlantFactory.createPlantByName("Snow Pea", 2, new Position(1, 1));
        Plant upgraded = PlantFactory.createPlantByName("Snow Pea", 3, new Position(1, 1));

        assertEquals(0.0, fresh.getSpecialUpgrade("CHILL_DURATION_EXT", 0), 1e-9);
        assertEquals(2.0, upgraded.getSpecialUpgrade("CHILL_DURATION_EXT", 0), 1e-9);
    }

    @Test
    void anUpgradedMelonPultDealsExtraSplashDamage() {
        Plant fresh = PlantFactory.createPlantByName("Melon-pult", 2, new Position(1, 1));
        Plant upgraded = PlantFactory.createPlantByName("Melon-pult", 3, new Position(1, 1));

        assertEquals(0.0, fresh.getSpecialUpgrade("SPLASH_DAMAGE_BUFF", 0), 1e-9);
        assertEquals(15.0, upgraded.getSpecialUpgrade("SPLASH_DAMAGE_BUFF", 0), 1e-9);
    }

    @Test
    void kiwibeastKeepsItsDamageUpgradeAfterItGrows() {
        Plant fresh = PlantFactory.createPlantByName("Kiwibeast", 1, new Position(1, 1));
        Plant upgraded = PlantFactory.createPlantByName("Kiwibeast", 3, new Position(1, 1));

        int freshStageOne = fresh.getDamage();
        int upgradedStageOne = upgraded.getDamage();
        assertTrue(upgradedStageOne > freshStageOne, "the level 3 damage buff applies at stage 1");

        fresh.setGrowthStage(2);
        upgraded.setGrowthStage(2);
        assertTrue(upgraded.getDamage() > fresh.getDamage(),
                "the damage buff must survive the growth stage taking over");
        assertEquals(upgradedStageOne - freshStageOne, upgraded.getDamage() - fresh.getDamage(),
                "the upgrade bonus stays constant across growth stages");
    }

    @Test
    void kiwibeastGainsAnExtraGrowthStageAtMaxLevel() {
        Plant upgraded = PlantFactory.createPlantByName("Kiwibeast", 4, new Position(1, 1));
        Plant lower = PlantFactory.createPlantByName("Kiwibeast", 3, new Position(1, 1));

        lower.setGrowthStage(4);
        upgraded.setGrowthStage(4);

        assertTrue(upgraded.getDamage() > lower.getDamage(),
                "GROWTH_STAGE_MAX_UP must add a stronger final growth stage");
    }

    @Test
    void anUpgradedHypnoShroomProducesAToughAllyZombie() {
        Plant hypno = plant("Hypno-shroom", 4, 3, 2);
        Zombie zombie = spawn("ZombieDefault", 3, 2, 200);
        int startingHp = zombie.getHP();
        double startingEatDps = zombie.getEatDps();

        tick(3);

        assertTrue(zombie.isHypnotized(), "the touching zombie should be hypnotized");
        assertTrue(zombie.getHP() > startingHp, "ZOMBIE_HEALTH_MULTIPLIER should buff the ally");
        assertTrue(zombie.getEatDps() > startingEatDps,
                "ZOMBIE_DAMAGE_MULTIPLIER should buff the ally");
        assertFalse(hypno.isAlive());
    }

    @Test
    void anUnupgradedHypnoShroomLeavesTheAllyUntouched() {
        plant("Hypno-shroom", 1, 3, 2);
        Zombie zombie = spawn("ZombieDefault", 3, 2, 200);
        int startingHp = zombie.getHP();
        double startingEatDps = zombie.getEatDps();

        tick(3);

        assertTrue(zombie.isHypnotized());
        assertEquals(startingHp, zombie.getHP());
        assertEquals(startingEatDps, zombie.getEatDps(), 1e-9);
    }

    @Test
    void aMaxLevelTorchwoodBlastsItsNeighboursWhenItDies() {
        Plant torchwood = plant("Torchwood", 3, 4, 2);
        Zombie neighbour = spawn("ZombieDefault", 5, 2, 400);
        int startingHp = neighbour.getHP();

        torchwood.setAlive(false);
        tick(1);

        assertTrue(neighbour.getHP() < startingHp,
                "DEATH_EXPLOSION_AOE should damage adjacent zombies");
        assertEquals(startingHp - UpgradeEffects.DEFAULT_FAREWELL_DAMAGE, neighbour.getHP());
    }

    @Test
    void aLowLevelTorchwoodDiesQuietly() {
        Plant torchwood = plant("Torchwood", 2, 4, 2);
        Zombie neighbour = spawn("ZombieDefault", 5, 2, 400);
        int startingHp = neighbour.getHP();

        torchwood.setAlive(false);
        tick(1);

        assertEquals(startingHp, neighbour.getHP(),
                "an un-upgraded Torchwood must not explode");
    }

    @Test
    void aMaxLevelGraveBusterExplodesWhenItIsDone() {
        Plant graveBuster = PlantFactory.createPlantByName("Grave Buster", 4, new Position(4, 2));
        session.getPlants().add(graveBuster);
        Zombie neighbour = spawn("ZombieDefault", 5, 2, 400);
        int startingHp = neighbour.getHP();

        assertTrue(graveBuster.hasSpecialUpgrade(UpgradeEffects.EXPLODE_ON_FINISH_TAG));
        graveBuster.setAlive(false);
        tick(1);

        assertTrue(neighbour.getHP() < startingHp,
                "EXPLODE_ON_FINISH should damage nearby zombies");
    }

    @Test
    void theFarewellBlastOnlyFiresOnce() {
        Plant torchwood = plant("Torchwood", 3, 4, 2);
        Zombie neighbour = spawn("ZombieDefault", 5, 2, 900);
        int startingHp = neighbour.getHP();

        torchwood.setAlive(false);
        tick(4);

        assertEquals(startingHp - UpgradeEffects.DEFAULT_FAREWELL_DAMAGE, neighbour.getHP());
    }

    @Test
    void anUpgradedMintResetsItsFamilySeedCooldowns() {
        int peashooterId = PlantFactory.findPlantIdByName("Peashooter");
        session.startPlantCooldown(peashooterId, 20.0);
        assertFalse(session.isPlantReady(peashooterId));

        plant("Peashooter", 1, 1, 2);
        Plant mint = plant("Appease-mint", 4, 3, 2);
        tickUntilConsumed(mint);

        assertTrue(session.isPlantReady(peashooterId),
                "RESET_FAMILY_COOLDOWNS should clear the family's seed cooldowns");
    }

    @Test
    void anUnupgradedMintLeavesSeedCooldownsAlone() {
        int peashooterId = PlantFactory.findPlantIdByName("Peashooter");
        session.startPlantCooldown(peashooterId, 20.0);

        plant("Peashooter", 1, 1, 2);
        Plant mint = plant("Appease-mint", 1, 3, 2);
        tickUntilConsumed(mint);

        assertFalse(session.isPlantReady(peashooterId));
    }

    @Test
    void anUpgradedMintExtendsTheBoostItHandsOut() {
        double base = mintedPlantFoodTimer(1);
        double extended = mintedPlantFoodTimer(2);

        assertTrue(base > 0, "the mint should have boosted the peashooter");
        assertTrue(extended > base,
                "DURATION_EXT should lengthen the plant food the mint grants");
        assertEquals(1.0, extended - base, 1e-6);
    }

    private double mintedPlantFoodTimer(int mintLevel) {
        session = new GameSession(ROWS, COLS);
        Plant boosted = plant("Peashooter", 1, 1, 2);
        Plant mint = plant("Appease-mint", mintLevel, 3, 2);
        double timerWhenMinted = 0.0;
        int guard = 0;
        while (mint.isAlive() && guard++ < 400) {
            session.tick();
            if (!mint.isAlive()) timerWhenMinted = boosted.getPlantFoodTimer();
        }
        return timerWhenMinted;
    }

    private void tickUntilConsumed(Plant mint) {
        int guard = 0;
        while (mint.isAlive() && guard++ < 400) session.tick();
        assertFalse(mint.isAlive(), "the mint should have fired and been consumed");
    }

    @Test
    void anUpgradedElectricBlueberryGoesForTheGargantuar() {
        Plant blueberry = plant("Electric Blueberry", 3, 1, 2);
        assertTrue(blueberry.hasSpecialUpgrade("PRIORITIZE_GARGANTUARS"));

        Zombie basic = spawn("ZombieDefault", 6, 0, 500);
        Zombie gargantuar = spawn("ZombieGargantuar", 7, 4, 3000);

        int guard = 0;
        while (session.getProjectiles().isEmpty() && guard++ < 400) session.tick();

        assertFalse(session.getProjectiles().isEmpty(), "the plant should have fired");
        assertEquals(gargantuar, session.getProjectiles().get(0).getTarget(),
                "the upgraded plant should target the gargantuar over the basic zombie");
        assertNotNull(basic);
    }

    @Test
    void anUpgradedWasabiWhipReachesFurther() {
        Plant fresh = PlantFactory.createPlantByName("Wasabi Whip", 2, new Position(1, 1));
        Plant upgraded = PlantFactory.createPlantByName("Wasabi Whip", 3, new Position(1, 1));

        assertEquals(0.0, fresh.getSpecialUpgrade("TILE_RANGE_EXT", 0), 1e-9);
        assertEquals(1.0, upgraded.getSpecialUpgrade("TILE_RANGE_EXT", 0), 1e-9);
    }

    @Test
    void anUpgradedGooPeashooterPoisonsForLonger() {
        Plant upgraded = PlantFactory.createPlantByName("Goo Peashooter", 2, new Position(1, 1));
        assertEquals(5.0, upgraded.getSpecialUpgrade("POISON_TICK_BUFF", 0), 1e-9);
    }

    @Test
    void anUpgradedHotPotatoMeltsAWiderArea() {
        Plant fresh = PlantFactory.createPlantByName("Hot Potato", 2, new Position(1, 1));
        Plant upgraded = PlantFactory.createPlantByName("Hot Potato", 3, new Position(1, 1));

        assertEquals(1, UpgradeEffects.iceMeltMode(fresh, 1));
        assertEquals(2, UpgradeEffects.iceMeltMode(upgraded, 1));
    }

    @Test
    void anUpgradedPepperPultWarmsAWiderArea() {
        Plant fresh = PlantFactory.createPlantByName("Pepper-pult", 2, new Position(1, 1));
        Plant upgraded = PlantFactory.createPlantByName("Pepper-pult", 3, new Position(1, 1));

        assertEquals(1, UpgradeEffects.warmthRadius(fresh));
        assertEquals(2, UpgradeEffects.warmthRadius(upgraded));
    }

    @Test
    void anUpgradedSunShroomGrowsUpSooner() {
        Plant fresh = plant("Sun-shroom", 1, 1, 1);
        Plant upgraded = plant("Sun-shroom", 2, 2, 1);

        tick((int) Math.round(20.0 / service.GameClock.SECONDS_PER_TICK));

        assertEquals(1, fresh.getGrowthStage(), "a level 1 Sun-shroom is still a baby at 20s");
        assertTrue(upgraded.getGrowthStage() > 1,
                "GROW_TIME_REDUCTION should let the upgraded Sun-shroom grow sooner");
    }
}
