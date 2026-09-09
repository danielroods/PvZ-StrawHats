import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import model.match.endless.EndlessChapter;
import controller.ui_menus.LeaderboardMenu;
import model.match.main.levels.Level;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.LevelLoader;
import net.dto.LeaderboardRowDto;
import net.dto.LeaderboardRows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every leaderboard column, checked against the account data it claims to report.
 */
class LeaderboardColumnsTest {

    private static List<Level> allLevels;

    @BeforeAll
    static void loadGameData() throws Exception {
        PlantFactory.autoInit();
        ZombieFactory.init();
        allLevels = LevelLoader.loadLevels();
    }

    private static User account(String username) {
        User user = new User(username, "Passw0rd!", username + "-nick",
                username + "@example.com", "male");
        user.userState = new UserState(new ArrayList<>(), 0, 0, 0);
        return user;
    }

    private static LeaderboardRowDto rowOf(User user) {
        return LeaderboardRows.of(user, allLevels);
    }

    @Test
    void aBrandNewAccountReportsNoProgressAtAll() {
        LeaderboardRowDto row = rowOf(account("rookie"));

        assertEquals("rookie", row.username);
        assertEquals(0, row.levelsCleared);
        assertEquals("Egypt", row.chapter, "a new player starts on the first chapter");
        assertEquals(0, row.chapterStagesCleared);
        assertEquals(4, row.chapterStageCount);
        assertEquals(0, row.miniGamesWon);
        assertEquals(0, row.questsCompleted);
        assertNull(row.myPoint, "a player who never scored has no My Point");
        for (EndlessChapter chapter : EndlessChapter.values()) {
            assertNull(row.lotteryScore(chapter.key()),
                    "a Lottery never played has no record");
        }
    }

    @Test
    void chapterProgressFollowsTheLastClearedStage() {
        User user = account("climber");

        user.userState.recordGameResult(101);
        LeaderboardRowDto afterFirstStage = rowOf(user);
        assertEquals("Egypt", afterFirstStage.chapter);
        assertEquals(1, afterFirstStage.chapterStagesCleared);
        assertEquals(1, afterFirstStage.levelsCleared);

        user.userState.recordGameResult(104);
        LeaderboardRowDto afterEgypt = rowOf(user);
        assertEquals("Frostbite Caves", afterEgypt.chapter,
                "clearing a chapter moves the player on to the next one");
        assertEquals(0, afterEgypt.chapterStagesCleared);
        assertEquals(4, afterEgypt.levelsCleared);

        user.userState.recordGameResult(404);
        LeaderboardRowDto finished = rowOf(user);
        assertEquals("Dark Ages", finished.chapter);
        assertEquals(4, finished.chapterStagesCleared);
        assertEquals(allLevels.size(), finished.levelsCleared);
    }

    @Test
    void aSyntheticLotteryLevelNeverPoisonsChapterProgress() {
        User user = account("gambler");
        user.userState.recordGameResult(103);
        user.userState.recordGameResult(EndlessChapter.EGYPT.levelId());

        assertEquals(103, user.userState.lastLevel,
                "a Lottery node is not part of the adventure ladder");
        assertEquals("Egypt", rowOf(user).chapter);
        assertEquals(3, rowOf(user).chapterStagesCleared);
    }

    @Test
    void aStateSavedWithABrokenLastLevelHealsWhenItIsRepaired() {
        UserState state = new UserState(new ArrayList<>(), 0, 0, 0);
        state.lastLevel = -1_000_101;
        state.repair();
        assertEquals(0, state.lastLevel, "a synthetic id left over from an old save is cleared");

        User user = account("healed");
        user.userState = state;
        assertEquals("Egypt", rowOf(user).chapter);
    }

    @Test
    void miniGameAndQuestColumnsReportTheAccountCounters() {
        User user = account("hobbyist");
        user.userState.miniGamesWon = 5;
        user.userState.questsCompleted = 12;

        LeaderboardRowDto row = rowOf(user);
        assertEquals(5, row.miniGamesWon);
        assertEquals(12, row.questsCompleted);
    }

    @Test
    void eachLotteryColumnReportsItsOwnChapterRecord() {
        User user = account("survivor");
        user.userState.recordLotteryScore(EndlessChapter.EGYPT.key(), 4200);
        user.userState.recordLotteryScore(EndlessChapter.BIG_WAVE_BEACH.key(), 900);

        LeaderboardRowDto row = rowOf(user);
        assertEquals(4200L, row.lotteryScore(EndlessChapter.EGYPT.key()));
        assertEquals(900L, row.lotteryScore(EndlessChapter.BIG_WAVE_BEACH.key()));
        assertNull(row.lotteryScore(EndlessChapter.DARK_AGES.key()));
        assertNull(row.lotteryScore(EndlessChapter.FROSTBITE_CAVES.key()));
        assertEquals(4200, row.myPoint, "My Point is the best Lottery run of any chapter");
    }

    @Test
    void aRowAlwaysCarriesTheProfileItBelongsTo() {
        User user = account("shanks");
        user.profilePicture = "assets/images/ui/avatar_shanks.png";

        LeaderboardRowDto row = rowOf(user);
        assertEquals("shanks", row.username);
        assertEquals("shanks-nick", row.nickname);
        assertEquals("assets/images/ui/avatar_shanks.png", row.profilePicture);
    }

    @Test
    void twoAccountsNeverShareARow() {
        User first = account("nami");
        first.userState.recordLotteryScore(EndlessChapter.EGYPT.key(), 700);
        first.userState.miniGamesWon = 3;

        User second = account("zoro");
        second.userState.recordLotteryScore(EndlessChapter.EGYPT.key(), 1500);

        LeaderboardRowDto firstRow = rowOf(first);
        LeaderboardRowDto secondRow = rowOf(second);

        assertEquals(700L, firstRow.lotteryScore(EndlessChapter.EGYPT.key()));
        assertEquals(1500L, secondRow.lotteryScore(EndlessChapter.EGYPT.key()));
        assertEquals(3, firstRow.miniGamesWon);
        assertEquals(0, secondRow.miniGamesWon);
    }

    @Test
    void aChapterNeverPlayedReadsAsZeroWithoutBeingSavedAsOne() {
        User user = account("rookie");
        user.userState.recordLotteryScore(EndlessChapter.EGYPT.key(), 640);

        LeaderboardRowDto row = rowOf(user);
        assertEquals(0L, row.lotteryScoreOrZero(EndlessChapter.DARK_AGES.key()),
                "an unplayed chapter shows as a plain 0");
        assertEquals(640L, row.lotteryScoreOrZero(EndlessChapter.EGYPT.key()));
        assertNull(row.lotteryScore(EndlessChapter.DARK_AGES.key()),
                "and it is still absent in the data behind the row");
        assertFalse(user.userState.hasLotteryScore(EndlessChapter.DARK_AGES.key()),
                "showing a 0 must not write a 0 into the account");
        assertTrue(user.userState.lotteryHighScores.size() == 1,
                "only the chapter actually played is stored");
    }

    @Test
    void myPointAlsoReadsAsZeroBeforeTheFirstRun() {
        LeaderboardRowDto row = rowOf(account("rookie"));
        assertEquals(0L, row.myPointOrZero());
        assertNull(row.myPoint, "the saved value is still unset");
    }

    @Test
    void theTextLeaderboardListsTheNewColumnsAndDropsTheOldOnes() {
        java.util.ArrayList<User> previous = User.users;
        try {
            User.users = new java.util.ArrayList<>();
            User bigScore = account("usopp");
            bigScore.userState.recordGameResult(102);
            bigScore.userState.recordLotteryScore(EndlessChapter.EGYPT.key(), 8888);
            User noScore = account("kaya");
            User.users.add(bigScore);
            User.users.add(noScore);

            LeaderboardMenu menu = new LeaderboardMenu();
            String board = menu.showMenu();

            assertTrue(board.contains("Egypt Meow"), board);
            assertTrue(board.contains("Frostbite Caves Meow"), board);
            assertTrue(board.contains("Big Wave Beach Meow"), board);
            assertTrue(board.contains("Dark Ages Meow"), board);
            assertTrue(board.contains("8888"), board);
            assertFalse(board.contains("Lottery"), "the columns are Meow columns now");
            assertFalse(board.contains("Rank"), "the Rank column is gone");
            assertFalse(board.contains("Season"), "the Season column is gone");
            assertFalse(board.contains("Stage"), "the Stage column is gone");

            String kayaRow = lineFor(board, "kaya");
            assertFalse(kayaRow.contains("-"), "a player with no runs shows zeros, not dashes");
            assertTrue(kayaRow.contains("0"), kayaRow);

            assertEquals(List.of("usopp", "kaya"), orderIn(board, "usopp", "kaya"),
                    "descending puts the player with a record above the one without");

            menu.handleCommand("leaderboard sort -c egypt -o asc");
            String ascending = menu.showMenu();
            assertEquals(List.of("kaya", "usopp"), orderIn(ascending, "usopp", "kaya"),
                    "and ascending sorts that missing score as a numeric 0, so it comes first");

            menu.handleCommand("leaderboard sort -c stage -o asc");
            assertTrue(menu.showMenu().contains("sorted by egypt"),
                    "a column that no longer exists is refused, keeping the previous sort");
        } finally {
            User.users = previous;
        }
    }

    /**
     * Three accounts whose values disagree on every column, sorted both ways down each
     * column in turn - including the three Meow columns nobody has played, where every row
     * is a 0 and only the username tie-break is left to order them.
     */
    @Test
    void everyColumnSortsBothWaysWithUnplayedChaptersCountingAsZero() {
        java.util.ArrayList<User> previous = User.users;
        try {
            User.users = new java.util.ArrayList<>();

            User alpha = account("alpha");
            alpha.userState.recordGameResult(101);
            alpha.userState.miniGamesWon = 1;
            alpha.userState.questsCompleted = 3;
            alpha.userState.recordLotteryScore(EndlessChapter.EGYPT.key(), 500);

            User bravo = account("bravo");
            bravo.userState.recordGameResult(204);
            bravo.userState.miniGamesWon = 9;
            bravo.userState.questsCompleted = 1;

            User charlie = account("charlie");
            charlie.userState.miniGamesWon = 4;
            charlie.userState.questsCompleted = 7;
            charlie.userState.recordLotteryScore(EndlessChapter.BIG_WAVE_BEACH.key(), 900);

            User.users.add(alpha);
            User.users.add(bravo);
            User.users.add(charlie);

            assertSorted("username", "desc", "charlie", "bravo", "alpha");
            assertSorted("username", "asc", "alpha", "bravo", "charlie");

            assertSorted("chapter", "desc", "bravo", "alpha", "charlie");
            assertSorted("chapter", "asc", "charlie", "alpha", "bravo");

            assertSorted("minigames", "desc", "bravo", "charlie", "alpha");
            assertSorted("minigames", "asc", "alpha", "charlie", "bravo");

            assertSorted("quests", "desc", "charlie", "alpha", "bravo");
            assertSorted("quests", "asc", "bravo", "alpha", "charlie");

            assertSorted("score", "desc", "charlie", "alpha", "bravo");
            assertSorted("score", "asc", "bravo", "alpha", "charlie");

            assertSorted("egypt", "desc", "alpha", "bravo", "charlie");
            assertSorted("egypt", "asc", "bravo", "charlie", "alpha");

            assertSorted("beach", "desc", "charlie", "alpha", "bravo");
            assertSorted("beach", "asc", "alpha", "bravo", "charlie");

            
            assertSorted("frostbite", "desc", "alpha", "bravo", "charlie");
            assertSorted("frostbite", "asc", "alpha", "bravo", "charlie");
            assertSorted("darkages", "desc", "alpha", "bravo", "charlie");
            assertSorted("darkages", "asc", "alpha", "bravo", "charlie");
        } finally {
            User.users = previous;
        }
    }

    private static void assertSorted(String column, String order, String... expected) {
        LeaderboardMenu menu = new LeaderboardMenu();
        menu.handleCommand("leaderboard sort -c " + column + " -o " + order);
        String board = menu.showMenu();
        assertEquals(List.of(expected), orderIn(board, expected),
                "sorting by " + column + " " + order + "\n" + board);
    }

    
    private static List<String> orderIn(String board, String... usernames) {
        List<String> found = new ArrayList<>(List.of(usernames));
        found.sort(java.util.Comparator.comparingInt(name -> lineIndexOf(board, name)));
        return found;
    }

    private static int lineIndexOf(String board, String username) {
        String[] lines = board.split("\r?\n");
        for (int index = 0; index < lines.length; index++) {
            if (lines[index].startsWith(username + " ")) return index;
        }
        throw new AssertionError(username + " is missing from the board:\n" + board);
    }

    private static String lineFor(String board, String username) {
        for (String line : board.split("\r?\n")) {
            if (line.startsWith(username + " ")) return line;
        }
        throw new AssertionError(username + " is missing from the board:\n" + board);
    }

    @Test
    void aRowSurvivesAnAccountWithNoStateAtAll() {
        User user = account("ghost");
        user.userState = null;

        LeaderboardRowDto row = rowOf(user);
        assertNotNull(row);
        assertEquals("ghost", row.username);
        assertEquals("-", row.chapter);
        assertTrue(row.lotteryScores.isEmpty());
    }
}
