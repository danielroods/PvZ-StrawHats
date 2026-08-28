import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadlessSimulationTest {

    private static final int ROWS = 5;
    private static final int COLS = 9;

    @Test
    void theSimulationRunsWithoutAnyLibGdxRuntime() {
        assertNull(com.badlogic.gdx.Gdx.app, "no libGDX application should be booted in tests");

        PlantFactory.autoInit();
        ZombieFactory.init();

        GameSession session = new GameSession(ROWS, COLS);
        session.setDifficultyLevel(3);
        session.setSkySunEnabled(false);
        session.setZombieBreachesEnabled(false);
        session.addSun(2000);

        for (int row = 0; row < ROWS; row++) {
            Plant peashooter = PlantFactory.createPlantByName("Peashooter", 1, new Position(1, row));
            assertNotNull(peashooter, "Peashooter must resolve from Plants.json");
            session.plantAt(row, 1, peashooter);
        }

        for (int row = 0; row < ROWS; row++) {
            Zombie zombie = ZombieFactory.create("ZombieDefault", row, COLS - 1);
            zombie.setPosition(new Position(COLS - 1, row));
            session.spawnZombie(zombie);
        }

        for (int tick = 0; tick < 600; tick++) {
            session.tick();
        }

        assertTrue(session.getElapsedSeconds() > 0, "the clock must have advanced");
    }

    @Test
    void setCurrentSwitchesWhichSessionTheModelStaticsSee() {
        GameSession first = new GameSession(ROWS, COLS);
        GameSession second = new GameSession(ROWS, COLS);

        assertSame(second, GameSession.peekInstance(), "the newest session wins by construction");

        GameSession.setCurrent(first);
        assertSame(first, GameSession.peekInstance());

        GameSession.setCurrent(second);
        assertSame(second, GameSession.peekInstance());
    }
}
