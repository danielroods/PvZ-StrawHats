import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.plant.PlantStats;
import model.collections.plant.PlantType;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.pitches.obstacles.Grave;
import model.pitches.obstacles.IceBlock;
import model.pitches.obstacles.OctopusWrap;
import model.pitches.obstacles.PushableType;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeleeTargetingTest {

    private static final int ROWS = 5;
    private static final int COLS = 9;

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession(ROWS, COLS);
        GameSession.setCurrent(session);
        session.setZombieBreachesEnabled(false);
        session.setLawnMowersEnabled(false);
    }

    private Plant plant(String name, int row, int col) {
        Plant plant = PlantFactory.createPlantByName(name, 1, new Position(col, row));
        assertTrue(session.plantAt(row, col, plant), name + " should be plantable");
        return plant;
    }

    private Grave grave(int row, int col) {
        Grave grave = new Grave();
        session.getEnvironment().getCell(row, col).setObstacle(grave);
        return grave;
    }

    private Zombie zombie(int row, double col, int hp) {
        Zombie zombie = ZombieFactory.create("ZombieDefault", row, (int) Math.round(col));
        zombie.setPosition(new Position(col, row));
        zombie.setSpeed(Position.ShowZero());
        zombie.setHP(hp);
        session.getZombies().add(zombie);
        return zombie;
    }

    private void tickUntil(java.util.function.BooleanSupplier done, int maxTicks, String message) {
        for (int i = 0; i < maxTicks && !done.getAsBoolean(); i++) session.tick();
        assertTrue(done.getAsBoolean(), message);
    }

    private static PlantJsonParser.PlantConfig config(String name) {
        for (PlantJsonParser.PlantConfig candidate : PlantFactory.getBlueprints().values()) {
            if (candidate.name.equalsIgnoreCase(name)) return candidate;
        }
        throw new AssertionError("no plant config named " + name);
    }

    @Test
    void bonkChoyPunchesAGraveWithNoZombiesOnTheLawn() {
        plant("Bonk Choy", 2, 2);
        Grave grave = grave(2, 4);

        assertTrue(session.getZombies().isEmpty());
        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 60,
                "Bonk Choy stayed idle with a grave inside its reach");
    }

    @Test
    void bonkChoyStillIgnoresAGraveBeyondItsReach() {
        plant("Bonk Choy", 2, 2);
        Grave grave = grave(2, 5);

        for (int i = 0; i < 60; i++) session.tick();
        assertEquals(Grave.MAX_HP, grave.getHp(),
                "a grave three tiles away is outside Bonk Choy's two-tile reach");
    }

    @Test
    void wasabiWhipReachesThreeTilesBehindItself() {
        Plant wasabi = plant("Wasabi Whip", 2, 5);
        Grave grave = grave(2, 2);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 90,
                "the whip never reached the grave three tiles behind it");
        assertTrue(wasabi.isMeleeFacingLeft(), "the plant turns toward the target it hits");
    }

    @Test
    void phatBeetHitsAGraveWithNoZombiesOnTheLawn() {
        plant("Phat Beet", 2, 2);
        Grave grave = grave(2, 4);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 90,
                "Phat Beet stayed idle with a grave inside its reach");
    }

    @Test
    void kiwibeastHitsTheGraveNextToIt() {
        plant("Kiwibeast", 2, 2);
        Grave grave = grave(2, 3);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 90,
                "Kiwibeast stayed idle with a grave right beside it");
    }

    @Test
    void headbutterLettuceHitsAGraveBehindIt() {
        plant("Iceberg Lettuce", 2, 4);
        Grave grave = grave(2, 2);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 90,
                "Headbutter Lettuce never turned around for the grave behind it");
    }

    @Test
    void iceShroomHitsAGraveOnTheDiagonalOfItsThreeByThreeFootprint() {
        plant("Ice-shroom", 2, 2);
        Grave grave = grave(1, 3);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 90,
                "Ice-shroom's area attack skipped the grave on its own footprint");
    }

    @Test
    void chomperBitesAGraveWithNoZombiesOnTheLawn() {
        plant("Chomper", 2, 2);
        Grave grave = grave(2, 3);

        tickUntil(() -> session.getEnvironment().getCell(2, 3).getObstacle() == null, 60,
                "Chomper stayed idle with a grave right beside it");
        assertEquals(0, grave.getHp());
    }

    @Test
    void aMeleePlantBreaksAnOctopusWrapAndFreesTheWrappedPlant() {
        plant("Bonk Choy", 2, 2);
        Plant victim = plant("Wall-nut", 2, 3);
        OctopusWrap wrap = new OctopusWrap(victim, 60);
        session.getEnvironment().getCell(2, 3).setObstacle(wrap);
        victim.setState(Plant.PlantState.INCAPACITATED);

        tickUntil(wrap::isDead, 90, "Bonk Choy never punched through the octopus wrap");
        assertEquals(Plant.PlantState.ACTIVE, victim.getPlantState(),
                "breaking the wrap must release the plant it was holding");
    }

    @Test
    void aFireMeleePlantShattersAnIceBlockInOneHit() {
        plant("Wasabi Whip", 2, 2);
        Plant frozen = plant("Wall-nut", 2, 3);
        frozen.setState(Plant.PlantState.INCAPACITATED);
        session.getEnvironment().getCell(2, 3).setObstacle(new IceBlock(frozen, IceBlock.BASE_HP));

        tickUntil(() -> session.getEnvironment().getCell(2, 3).getObstacle() == null, 90,
                "the burning whip never thawed the ice block beside it");
        assertEquals(Plant.PlantState.ACTIVE, frozen.getPlantState(),
                "shattering the ice releases the plant frozen inside it");
    }

    @Test
    void aMeleePlantHitsAStructureLeftBehindByItsZombie() {
        plant("Bonk Choy", 2, 2);
        PushableStructure barrel = new PushableStructure(PushableType.BARREL, new Position(3, 2));
        session.registerStructure(barrel);
        int before = barrel.getHp();

        tickUntil(() -> barrel.getHp() < before, 60,
                "Bonk Choy ignored the barrel standing right next to it");
    }

    @Test
    void attackingAnObstacleStartsTheNormalAttackCooldown() {
        Plant beet = plant("Phat Beet", 2, 2);
        Grave grave = grave(2, 3);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 90, "Phat Beet never attacked");
        assertTrue(beet.getIntervalTimer() > 0,
                "hitting an obstacle must open the same attack window a zombie hit does");
    }

    @Test
    void aMeleePlantWithNothingInReachStaysIdle() {
        Plant bonk = plant("Bonk Choy", 2, 2);

        for (int i = 0; i < 60; i++) session.tick();
        assertEquals(0.0, bonk.getIntervalTimer(), 1.0e-9,
                "an empty lawn must not make a melee plant swing at nothing");
    }

    @Test
    void aMeleePlantStillBeatsUpZombiesInReach() {
        plant("Bonk Choy", 2, 2);
        Zombie zombie = zombie(2, 3.0, 1_000_000);

        tickUntil(() -> zombie.getHP() < 1_000_000, 60, "Bonk Choy never hit the zombie in reach");
    }

    @Test
    void aMeleePlantLeavesHypnotizedZombiesAlone() {
        plant("Bonk Choy", 2, 2);
        Zombie ally = zombie(2, 3.0, 1_000_000);
        ally.setFaction(model.collections.Faction.PLANTS);
        assertTrue(ally.isHypnotized());

        for (int i = 0; i < 60; i++) session.tick();
        assertEquals(1_000_000, ally.getHP(), "a melee plant must not punch its own ally");
    }

    @Test
    void aMeleePlantPrefersNothingOverAnEmptyRowButTakesTheGraveWhenTheZombieDies() {
        plant("Bonk Choy", 2, 2);
        Zombie zombie = zombie(2, 3.0, 1);
        Grave grave = grave(2, 4);

        tickUntil(() -> !zombie.isAlive(), 60, "the zombie never went down");
        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 60,
                "with the zombie gone the grave becomes the target");
    }

    @Test
    void everyMeleePlantGetsFiftyPercentMoreBaseDamage() {
        for (PlantJsonParser.PlantConfig config : PlantFactory.getBlueprints().values()) {
            if (config.category != PlantType.MELEE) continue;
            int expected = (int) Math.round(config.damage * 1.5);
            assertEquals(expected, PlantStats.of(config, 1).damage(),
                    config.name + " should carry the boosted melee base damage");
            Plant planted = PlantFactory.createPlantByName(config.name, 1, new Position(1, 1));
            assertEquals(expected, planted.getDamage(),
                    config.name + " should hit for its boosted base damage");
        }
    }

    @Test
    void upgradeScalingSitsOnTopOfTheBoostedMeleeBase() {
        PlantJsonParser.PlantConfig bonkChoy = config("Bonk Choy");
        int base = PlantStats.of(bonkChoy, 1).damage();
        assertEquals((int) Math.round(bonkChoy.damage * 1.5), base);
        assertEquals(base + 5, PlantStats.of(bonkChoy, 2).damage(),
                "the level 2 +5 damage upgrade still adds on top of the new base");

        Plant upgraded = PlantFactory.createPlantByName("Bonk Choy", 2, new Position(1, 1));
        assertEquals(base + 5, upgraded.getDamage());
    }

    @Test
    void kiwibeastGrowthStagesUseTheBoostedBaseToo() {
        PlantJsonParser.PlantConfig kiwibeast = config("Kiwibeast");
        Map<String, Object> stageTwo = PlantStats.scaledGrowthStages(kiwibeast).get(0);
        assertEquals(45.0, ((Number) stageTwo.get("damage")).doubleValue(), 1.0e-9,
                "stage 2 is the boosted version of the json's 30 damage");

        Plant fresh = PlantFactory.createPlantByName("Kiwibeast", 1, new Position(1, 1));
        Plant upgraded = PlantFactory.createPlantByName("Kiwibeast", 3, new Position(1, 1));
        int bonus = upgraded.getDamage() - fresh.getDamage();

        fresh.setGrowthStage(2);
        upgraded.setGrowthStage(2);
        assertEquals(45, fresh.getDamage(), "the growth stage takes over with the boosted value");
        assertEquals(bonus, upgraded.getDamage() - fresh.getDamage(),
                "the upgrade bonus stays constant across growth stages");
    }

    @Test
    void shooterBaseDamageIsUntouchedByTheMeleeBoost() {
        assertEquals(config("Peashooter").damage, PlantStats.of(config("Peashooter"), 1).damage());
        assertEquals(config("Cabbage-pult").damage,
                PlantStats.of(config("Cabbage-pult"), 1).damage());
        assertFalse(PlantStats.baseDamageMultiplier(config("Peashooter")) != 1.0,
                "only melee plants get the boost");
    }
}
