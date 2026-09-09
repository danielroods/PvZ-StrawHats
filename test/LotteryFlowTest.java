import controller.match.AfterMenu;
import controller.match.BeforeMenu;
import controller.match.EndlessScoreboard;
import controller.match.GameplayMenu;
import controller.match.MatchMenu;
import model.App;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import model.match.endless.EndlessChapter;
import model.match.endless.EndlessLevel;
import model.match.endless.EndlessLevels;
import model.match.main.levels.Level;
import model.match_mechanisms.vector.Position;
import model.user_data.User;
import model.user_data.UserState;
import model.user_data.UserStore;
import model.utils.GameSession;
import model.utils.LevelLoader;
import net.dto.LeaderboardRowDto;
import net.dto.LeaderboardRows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole Lottery flow driven through the real menu state machine - pick the node,
 * choose a loadout, play until the lawn is overrun, then land on the after-match menu -
 * checking that the run is scored, saved against the right chapter of the right account,
 * and shows up on that account's leaderboard row.
 */
class LotteryFlowTest {

    
    private static final class MemoryStore implements UserStore {
        private int saves;

        @Override public void load() { }

        @Override public void save() {
            if (User.currentUser != null && User.currentUser.userState != null) {
                User.currentUser.userState.markSaved();
            }
            saves++;
        }

        @Override public void addUser(User user) { User.users.add(user); }

        @Override public User findByUsername(String username) {
            for (User user : User.users) {
                if (user.username.equalsIgnoreCase(username)) return user;
            }
            return null;
        }

        @Override public boolean usernameExists(String username) {
            return findByUsername(username) != null;
        }

        @Override public void setUser(User user) {
            User.currentUser = user;
            App.currentUser = user;
        }
    }

    private static List<Level> allLevels;

    private MemoryStore store;
    private User player;
    private java.util.ArrayList<User> previousUsers;
    private User previousCurrentUser;

    @BeforeAll
    static void loadGameData() throws Exception {
        PlantFactory.autoInit();
        ZombieFactory.init();
        allLevels = LevelLoader.loadLevels();
    }

    @BeforeEach
    void signIn() {
        previousUsers = User.users;
        previousCurrentUser = User.currentUser;
        User.users = new ArrayList<>();

        store = new MemoryStore();
        User.useStore(store);

        player = new User("luffy", "Passw0rd!", "luffy-nick", "luffy@example.com", "male");
        player.userState = new UserState(new ArrayList<>(), 0, 0, 0);
        User.users.add(player);
        store.setUser(player);
    }

    @AfterEach
    void signOut() {
        User.useLocalStore();
        User.users = previousUsers;
        User.currentUser = previousCurrentUser;
        MatchMenu.selectedLevel = null;
        BeforeMenu.selectedPlants.clear();
        App.currentMenu = null;
    }

    private GameSession playLottery(EndlessChapter chapter, int ticks) {
        MatchMenu.selectedLevel = EndlessLevels.forChapter(chapter,
                EndlessLevels.levelsOf(chapter, allLevels));
        App.currentMenu = new MatchMenu();
        App.currentMenu.handleCommand("start game");

        assertInstanceOf(BeforeMenu.class, App.currentMenu,
                "the Lottery goes through the loadout step like any other stage");
        App.currentMenu.handleCommand("add plant -t Sunflower");
        App.currentMenu.handleCommand("start game");
        assertInstanceOf(GameplayMenu.class, App.currentMenu, "and then into the match");

        GameSession session = GameSession.getInstance();
        assertTrue(session.isWavesStarted());
        defendTheLawn(session);

        for (int tick = 0; tick < ticks && !session.isGameOver(); tick++) {
            session.tick();
        }
        return session;
    }

    
    private void defendTheLawn(GameSession session) {
        for (int column = 1; column <= 2; column++) {
            for (int row = 0; row < session.getRows(); row++) {
                Plant peashooter = PlantFactory.createPlantByName("Peashooter", 1,
                        new Position(column, row));
                if (peashooter != null) session.plantAt(row, column, peashooter);
            }
        }
    }

    private LeaderboardRowDto row() {
        return LeaderboardRows.of(player, allLevels);
    }

    @Test
    void aLotteryRunIsScoredSavedAndShownOnTheLeaderboard() {
        assertNull(row().lotteryScore(EndlessChapter.EGYPT.key()));

        GameSession session = playLottery(EndlessChapter.EGYPT, 4000);
        assertInstanceOf(EndlessLevel.class, session.getLevel());
        assertFalse(session.isGameWon(), "an endless run is never won");
        long score = session.getEndlessRun().getScore();
        assertTrue(score > 0, "surviving waves with plants must be worth something");

        int savesBefore = store.saves;
        App.currentMenu.handleCommand("end game -r lose");
        assertInstanceOf(AfterMenu.class, App.currentMenu);
        assertTrue(store.saves > savesBefore, "the run is written to the account, not just held");

        assertEquals(score, player.userState.getLotteryHighScore(EndlessChapter.EGYPT.key()));
        assertEquals((int) Math.min(Integer.MAX_VALUE, score),
                (int) player.userState.bonusHighScore);

        EndlessScoreboard.Result result = EndlessScoreboard.lastResult();
        assertNotNull(result);
        assertEquals(EndlessChapter.EGYPT, result.chapter());
        assertEquals(score, result.score());
        assertTrue(result.improved(), "a first run is always a record");

        assertEquals(score, row().lotteryScore(EndlessChapter.EGYPT.key()));
        assertNull(row().lotteryScore(EndlessChapter.DARK_AGES.key()),
                "the other chapters stay untouched");
    }

    @Test
    void playingTheLotteryNeverMovesAdventureProgress() {
        playLottery(EndlessChapter.BIG_WAVE_BEACH, 1500);
        App.currentMenu.handleCommand("end game -r lose");

        assertEquals(0, player.userState.lastLevel,
                "a Lottery node is not a stage on the ladder");
        assertEquals("Egypt", row().chapter, "so chapter progress is where it was");
        assertEquals(0, row().chapterStagesCleared);
    }

    @Test
    void eachChapterKeepsItsOwnRecordForTheSameAccount() {
        long egypt = playLottery(EndlessChapter.EGYPT, 2500).getEndlessRun().getScore();
        App.currentMenu.handleCommand("end game -r lose");
        assertEquals(egypt, player.userState.getLotteryHighScore(EndlessChapter.EGYPT.key()));

        long frostbite = playLottery(EndlessChapter.FROSTBITE_CAVES, 2500)
                .getEndlessRun().getScore();
        App.currentMenu.handleCommand("end game -r lose");
        assertEquals(frostbite,
                player.userState.getLotteryHighScore(EndlessChapter.FROSTBITE_CAVES.key()));

        assertEquals(egypt, player.userState.getLotteryHighScore(EndlessChapter.EGYPT.key()),
                "playing another chapter never disturbs the first one's record");
        assertEquals((int) Math.max(egypt, frostbite), (int) player.userState.bonusHighScore,
                "My Point is the best of them");

        LeaderboardRowDto row = row();
        assertEquals(egypt, row.lotteryScore(EndlessChapter.EGYPT.key()));
        assertEquals(frostbite, row.lotteryScore(EndlessChapter.FROSTBITE_CAVES.key()));
        assertNull(row.lotteryScore(EndlessChapter.BIG_WAVE_BEACH.key()));
        assertNull(row.lotteryScore(EndlessChapter.DARK_AGES.key()));
    }

    @Test
    void aShorterSecondRunNeverLowersTheRecord() {
        long first = playLottery(EndlessChapter.DARK_AGES, 3000).getEndlessRun().getScore();
        App.currentMenu.handleCommand("end game -r lose");
        long best = player.userState.getLotteryHighScore(EndlessChapter.DARK_AGES.key());
        assertEquals(first, best, "the first run sets the record");

        playLottery(EndlessChapter.DARK_AGES, 250);
        App.currentMenu.handleCommand("end game -r lose");

        assertEquals(best, player.userState.getLotteryHighScore(EndlessChapter.DARK_AGES.key()));
        assertFalse(EndlessScoreboard.lastResult().improved(),
                "a shorter run is reported as not a record");
        assertEquals(best, EndlessScoreboard.lastResult().best());
    }

    @Test
    void leavingARunPartWayThroughStillBanksIt() {
        GameSession session = playLottery(EndlessChapter.EGYPT, 2500);
        long score = session.getEndlessRun().getScore();

        App.currentMenu.handleCommand("menu exit");

        assertEquals(score, player.userState.getLotteryHighScore(EndlessChapter.EGYPT.key()),
                "the pause dialog's Save & Exit has to keep what the run was worth");
        assertEquals(score, row().lotteryScore(EndlessChapter.EGYPT.key()));
    }

    @Test
    void restartingBanksTheRunBeforeThrowingItAway() {
        GameSession session = playLottery(EndlessChapter.EGYPT, 2500);
        long score = session.getEndlessRun().getScore();

        App.currentMenu.handleCommand("restart");

        assertEquals(score, player.userState.getLotteryHighScore(EndlessChapter.EGYPT.key()),
                "restarting keeps the record the abandoned run had earned");
        assertEquals(0, GameSession.getInstance().getEndlessRun().getScore(),
                "and the new run starts from zero");
    }

    @Test
    void aNormalStageStillEndsWithoutAnyLotteryReport() throws Exception {
        MatchMenu.selectedLevel = LevelLoader.loadLevelById(101);
        App.currentMenu = new MatchMenu();
        App.currentMenu.handleCommand("start game");
        App.currentMenu.handleCommand("add plant -t Sunflower");
        App.currentMenu.handleCommand("start game");

        GameSession session = GameSession.getInstance();
        assertFalse(session.isEndless(), "an authored stage is not endless");
        for (int tick = 0; tick < 600 && !session.isGameOver(); tick++) {
            session.tick();
        }
        App.currentMenu.handleCommand("end game -r lose");

        assertNull(EndlessScoreboard.lastResult(),
                "a plain adventure loss must not report a Lottery score");
        assertNull(player.userState.bonusHighScore,
                "and it must not fake a My Point either");
    }
}
