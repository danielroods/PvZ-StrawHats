package controller.match;

import model.match.endless.EndlessChapter;
import model.match.endless.EndlessLevel;
import model.match.endless.EndlessRun;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.GameSession;
import net.client.NetworkClient;
import view.GeneralPrinter;

/**
 * Books an endless (Lottery) run into the account that played it, once, when the match
 * ends. The account's own {@link UserState} is the single source of truth: the record is
 * written there and saved through {@link User#save()}, which is what mirrors it to
 * client-data/Data.json offline and pushes it to the server (newest revision wins) when
 * signed in - so the leaderboard sees the same number either way.
 */
public final class EndlessScoreboard {

    public record Result(EndlessChapter chapter, long score, long best, boolean improved) {
    }

    private static Result lastResult;

    private EndlessScoreboard() {
    }

    public static Result lastResult() {
        return lastResult;
    }

    public static Result recordRun(GameSession session) {
        lastResult = null;
        if (session == null || !(session.getLevel() instanceof EndlessLevel level)) return null;
        if (User.currentUser == null || User.currentUser.userState == null) return null;

        EndlessRun run = level.getRun();
        EndlessChapter chapter = level.getChapter();
        long score = run == null ? 0L : run.getScore();

        UserState state = User.currentUser.userState;
        boolean improved = state.recordLotteryScore(chapter.key(), score);
        long best = state.getLotteryHighScore(chapter.key());
        User.save();

        NetworkClient client = NetworkClient.get();
        if (client.isSignedIn()) client.submitLotteryScore(chapter.key(), score, null);

        lastResult = new Result(chapter, score, best, improved);
        GeneralPrinter.print(improved
                ? "New " + chapter.levelName() + " record: " + score + " Meow Points."
                : "Scored " + score + " Meow Points in " + chapter.levelName()
                        + ". Your record stays " + best + ".");
        return lastResult;
    }
}
