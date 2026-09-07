import controller.cheat.CheatAccess;
import controller.cheat.NukeCheatController;
import controller.match.GameplayMenu;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import model.utils.GameSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugModeCheatGateTest {

    private static final int ROWS = 5;
    private static final int COLS = 9;

    private GameSession session;
    private boolean originalDebugMode;
    private boolean originalShowGrid;

    @BeforeEach
    void setUp() {
        originalDebugMode = GameSettings.get().isDebugMode();
        originalShowGrid = GameSettings.get().isShowGrid();
        session = new GameSession(ROWS, COLS);
        GameSession.setCurrent(session);
        session.setZombieBreachesEnabled(false);
        session.setLawnMowersEnabled(false);
        session.setSkySunEnabled(false);
    }

    @AfterEach
    void tearDown() {
        GameSettings.get().setDebugMode(originalDebugMode);
        GameSettings.get().setShowGrid(originalShowGrid);
    }

    @Test
    void aFreshInstallHasBothDebugTogglesOff() {
        assertNull(com.badlogic.gdx.Gdx.app, "the defaults must hold without any stored preferences");
        GameSettings settings = GameSettings.get();
        settings.load();
        assertFalse(settings.isDebugMode(), "debug mode must default to off");
        assertFalse(settings.isShowGrid(), "lawn grid lines must default to off");
    }

    @Test
    void cheatAccessFollowsTheDebugModeSetting() {
        GameSettings.get().setDebugMode(false);
        assertFalse(CheatAccess.isEnabled());
        assertFalse(CheatAccess.allow());

        GameSettings.get().setDebugMode(true);
        assertTrue(CheatAccess.isEnabled());
        assertTrue(CheatAccess.allow());
    }

    @Test
    void gameplayCheatCommandsDoNothingWhileDebugModeIsOff() {
        GameSettings.get().setDebugMode(false);
        GameplayMenu menu = new GameplayMenu();
        int sunBefore = session.getSunCount();
        int foodBefore = session.getPlantFoodCount();
        zombie(0, 5);

        menu.handleCommand("cheat add -n 250 suns");
        menu.handleCommand("cheat add-plant-food");
        menu.handleCommand("release the nuke");

        assertEquals(sunBefore, session.getSunCount(), "the sun cheat must be inert");
        assertEquals(foodBefore, session.getPlantFoodCount(), "the plant food cheat must be inert");
        assertEquals(1, aliveZombies(), "the nuke cheat must be inert");
    }

    @Test
    void gameplayCheatCommandsWorkWhileDebugModeIsOn() {
        GameSettings.get().setDebugMode(true);
        GameplayMenu menu = new GameplayMenu();
        int sunBefore = session.getSunCount();
        int foodBefore = session.getPlantFoodCount();
        zombie(0, 5);

        menu.handleCommand("cheat add -n 250 suns");
        menu.handleCommand("cheat add-plant-food");
        menu.handleCommand("release the nuke");

        assertEquals(sunBefore + 250, session.getSunCount());
        assertEquals(foodBefore + 1, session.getPlantFoodCount());
        assertEquals(0, aliveZombies(), "the nuke cheat must clear the lawn");
    }

    @Test
    void theNukeControllerIsGatedOnTheSameSetting() {
        zombie(1, 4);
        zombie(2, 6);
        NukeCheatController controller = new NukeCheatController();

        GameSettings.get().setDebugMode(false);
        assertTrue(controller.detonate().isEmpty(), "no zombie may die while debug mode is off");
        assertEquals(2, aliveZombies());

        GameSettings.get().setDebugMode(true);
        List<Zombie> killed = controller.detonate();
        assertEquals(2, killed.size());
        assertEquals(0, aliveZombies());
    }

    private void zombie(int row, double col) {
        Zombie zombie = ZombieFactory.create("ZombieDefault", row, (int) Math.round(col));
        zombie.setPosition(new Position(col, row));
        zombie.setSpeed(Position.ShowZero());
        session.getZombies().add(zombie);
    }

    private long aliveZombies() {
        return session.getZombies().stream().filter(Zombie::isAlive).count();
    }
}
