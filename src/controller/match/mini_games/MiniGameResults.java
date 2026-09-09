package controller.match.mini_games;

import model.user_data.User;
import model.user_data.UserState;


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

    
    public static void adoptOnlineTotal(int total) {
        UserState state = User.currentUser == null ? null : User.currentUser.userState;
        if (state == null) return;
        state.miniGamesWon = Math.max(state.miniGamesWon, total);
        User.save();
    }
}
