package controller.match.mini_games;

import model.user_data.User;
import model.user_data.UserState;

/**
 * The one place a mini-game win is written to the account.
 * <p>
 * Every controller used to bump {@code miniGamesWon} inline and leave it there, so the
 * count only ever reached disk if something unrelated happened to save afterwards - a
 * player who won a mini-game and closed the game lost the win, and so did the
 * leaderboard. Recording it here saves immediately, which is also what pushes it to the
 * server when the account is signed in.
 */
public final class MiniGameResults {

    private MiniGameResults() {
    }

    public static void recordWin(String miniGameKey, int difficulty) {
        UserState state = User.currentUser == null ? null : User.currentUser.userState;
        if (state == null) return;
        state.miniGamesWon++;
        if (difficulty > 0) state.recordMiniGameWin(miniGameKey, difficulty);
        User.save();
    }

    /**
     * Takes the server's authoritative total for an online match instead of adding one
     * locally, so a win credited on both sides still lands on the same number.
     */
    public static void adoptOnlineTotal(int total) {
        UserState state = User.currentUser == null ? null : User.currentUser.userState;
        if (state == null) return;
        state.miniGamesWon = Math.max(state.miniGamesWon, total);
        User.save();
    }
}
