import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import model.match.mini_games.izombie.IZombieMatch;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IZombieMatchRulesTest {

    private IZombieMatch match;

    @BeforeEach
    void setUp() {
        PlantFactory.autoInit();
        ZombieFactory.init();
        match = new IZombieMatch();
        GameSession.setCurrent(match.getSession());
    }

    @Test
    void theBoardOpensWithBrainsAndDefenders() {
        assertEquals(5, match.getBrainCount());
        assertEquals(0, match.getBrainsEaten());
        assertEquals(400, match.getZombieSun());
        assertEquals(150, match.getPlantSun());
        assertEquals(6, match.getRoster().size());
        assertEquals(6, match.getSeeds().size());
        assertTrue(match.getSession().getPlants().size() >= 10,
                "the lawn starts defended so the plant player is not naked at t=0");
        for (var brain : match.getBrains()) {
            assertNotNull(brain);
            assertFalse(brain.isEaten());
        }
    }

    @Test
    void eachSideCanOnlyDoItsOwnJob() {
        assertNotNull(match.applyIntent(Role.PLANTS, "PLACE_ZOMBIE", "ZombieImp", 0, 8),
                "the plant player cannot place zombies");
        assertNotNull(match.applyIntent(Role.ZOMBIES, "PLANT", "Peashooter", 0, 3),
                "the zombie player cannot plant");
        assertNotNull(match.applyIntent(Role.ZOMBIES, "COLLECT_SUN", null, 0, 3),
                "only the plant player collects sun");
    }

    @Test
    void placementZonesAreMirrored() {
        assertNotNull(match.applyIntent(Role.ZOMBIES, "PLACE_ZOMBIE", "ZombieImp", 0, 3),
                "zombies cannot spawn left of the red line");
        assertNull(match.applyIntent(Role.ZOMBIES, "PLACE_ZOMBIE", "ZombieImp", 0, 7),
                "zombies drop right of the red line");

        assertNotNull(match.applyIntent(Role.PLANTS, "PLANT", "Peashooter", 0, 7),
                "plants cannot be planted past the red line");
        assertNotNull(match.applyIntent(Role.PLANTS, "PLANT", "Peashooter", 0, 0),
                "plants cannot be planted on the brain column");
    }

    @Test
    void spendingAndRechargeAreEnforced() {
        int before = match.getZombieSun();
        assertNull(match.applyIntent(Role.ZOMBIES, "PLACE_ZOMBIE", "ZombieDefault", 1, 8));
        assertEquals(before - 50, match.getZombieSun(), "the browncoat costs 50");

        assertNotNull(match.applyIntent(Role.ZOMBIES, "PLACE_ZOMBIE", "ZombieDefault", 1, 8),
                "the packet is recharging right after use");

        assertNull(match.applyIntent(Role.PLANTS, "PLANT", "Wall-nut", 3, 4));
        assertEquals(100, match.getPlantSun(), "the wall-nut costs 50 of the 150 opening sun");
        assertNotNull(match.applyIntent(Role.PLANTS, "PLANT", "Wall-nut", 3, 5),
                "the seed card is recharging");
    }

    @Test
    void thePlantPlayerCanShovelItsOwnHalfOnly() {
        assertNull(match.applyIntent(Role.PLANTS, "PLANT", "Wall-nut", 3, 4),
                "row 3 column 4 is empty in the opening layout");
        assertNotNull(match.getSession().getPlantAt(3, 4));

        assertNull(match.applyIntent(Role.PLANTS, "DIG", null, 3, 4),
                "the plant player can shovel a plant back up");
        assertNull(match.getSession().getPlantAt(3, 4));

        assertNotNull(match.applyIntent(Role.PLANTS, "DIG", null, 3, 7),
                "there is nothing of theirs past the red line");
        assertNotNull(match.applyIntent(Role.ZOMBIES, "DIG", null, 3, 3),
                "the zombie player cannot shovel");
    }

    @Test
    void thePlantBankPaysOutOverTime() {
        int before = match.getPlantSun();
        for (int tick = 0; tick < 61; tick++) {
            match.tick();
        }
        assertEquals(before + 25, match.getPlantSun(),
                "the plant player banks 25 sun every six seconds");
    }

    @Test
    void plantsWinWhenTheClockRunsOut() {
        for (int tick = 0; tick < 1300 && !match.isFinished(); tick++) {
            match.tick();
        }
        assertTrue(match.isFinished(), "the two minute clock must end the match");
        assertEquals(Role.PLANTS, match.getWinner(),
                "surviving the clock with a brain intact is a plant win");
        assertTrue(match.getRemainingSeconds() <= 0.0001);
    }


    @Test
    void aPeaPodStacksOntoAnotherPeaPodJustAsItDoesOffline() {
        IZombieMatch peaPodMatch = new IZombieMatch(IZombieMatch.ONLINE_MATCH_SECONDS,
                List.of("Pea Pod"), List.of());
        GameSession.setCurrent(peaPodMatch.getSession());

        int row = 1;
        int col = 4;
        assertNull(peaPodMatch.getSession().getPlantAt(row, col),
                "the test needs an empty tile inside the plant zone");

        bankSunAndRecharge(peaPodMatch);
        assertNull(peaPodMatch.applyIntent(Role.PLANTS, "PLANT", "Pea Pod", row, col));
        Plant peaPod = peaPodMatch.getSession().getPlantAt(row, col);
        assertNotNull(peaPod, "the first Pea Pod went down");
        assertEquals(1, peaPod.getStackNumber());

        bankSunAndRecharge(peaPodMatch);
        assertNull(peaPodMatch.applyIntent(Role.PLANTS, "PLANT", "Pea Pod", row, col),
                "a second Pea Pod on the same tile stacks instead of being refused");
        assertSame(peaPod, peaPodMatch.getSession().getPlantAt(row, col),
                "stacking upgrades the plant that is already there");
        assertEquals(2, peaPod.getStackNumber(), "and the stack grew by one head");
    }

    @Test
    void somethingElseStillCannotBeDroppedOnAPlantedTile() {
        IZombieMatch peaPodMatch = new IZombieMatch(IZombieMatch.ONLINE_MATCH_SECONDS,
                List.of("Pea Pod", "Peashooter"), List.of());
        GameSession.setCurrent(peaPodMatch.getSession());

        bankSunAndRecharge(peaPodMatch);
        assertNull(peaPodMatch.applyIntent(Role.PLANTS, "PLANT", "Pea Pod", 1, 4));
        bankSunAndRecharge(peaPodMatch);
        assertEquals("There is already a plant there.",
                peaPodMatch.applyIntent(Role.PLANTS, "PLANT", "Peashooter", 1, 4));
    }

    private static void bankSunAndRecharge(IZombieMatch match) {
        for (int tick = 0; tick < 400 && match.getPlantSun() < 400; tick++) {
            match.tick();
        }
    }

    @Test
    void aForfeitHandsTheWinToTheOtherSide() {
        match.forfeit(Role.ZOMBIES, "left");
        assertTrue(match.isFinished());
        assertEquals(Role.PLANTS, match.getWinner());
    }

    @Test
    void anEatenBrainIsReportedOnceAsAnEvent() {
        for (int row = 0; row < IZombieMatch.ROWS; row++) {
            match.getBrains()[row].setHP(0);
        }
        match.tick();

        var events = match.drainEvents();
        assertEquals(5, events.size(), "one event per brain");
        assertTrue(events.stream().allMatch(event -> "BRAIN_EATEN".equals(event.kind())));
        assertEquals(5, match.getBrainsEaten());
        assertTrue(match.isFinished());
        assertEquals(Role.ZOMBIES, match.getWinner());

        assertTrue(match.drainEvents().isEmpty(), "events are drained, not repeated");
    }
}
