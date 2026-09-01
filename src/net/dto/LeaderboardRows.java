package net.dto;

import model.match.endless.EndlessChapter;
import model.match.main.levels.Level;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.LevelProgression;

import java.util.LinkedHashMap;
import java.util.List;

public final class LeaderboardRows {

    private LeaderboardRows() {
    }

    public static LeaderboardRowDto of(User user, List<Level> allLevels) {
        LeaderboardRowDto row = new LeaderboardRowDto();
        if (user == null) return row;
        row.username = user.username;
        row.nickname = user.nickname;
        row.profilePicture = user.profilePicture;
        if (user.userState == null) return row;

        UserState state = user.userState;
        row.miniGamesWon = state.miniGamesWon;
        row.questsCompleted = state.questsCompleted;
        row.myPoint = state.bonusHighScore;
        row.lotteryScores = lotteryScores(state);

        applyChapterProgress(row, state, allLevels);
        return row;
    }

    private static LinkedHashMap<String, Long> lotteryScores(UserState state) {
        LinkedHashMap<String, Long> scores = new LinkedHashMap<>();
        for (EndlessChapter chapter : EndlessChapter.values()) {
            if (state.hasLotteryScore(chapter.key())) {
                scores.put(chapter.key(), state.getLotteryHighScore(chapter.key()));
            }
        }
        return scores;
    }

    private static void applyChapterProgress(LeaderboardRowDto row, UserState state,
                                             List<Level> allLevels) {
        if (allLevels == null || allLevels.isEmpty()) return;
        List<Level> ordered = LevelProgression.sorted(allLevels);
        int clearedIndex = LevelProgression.completedIndex(ordered, state.lastLevel);
        row.levelsCleared = clearedIndex + 1;

        int currentIndex = Math.min(ordered.size() - 1, Math.max(0, clearedIndex + 1));
        Level current = ordered.get(currentIndex);
        if (current.getSeason() == null) return;

        String seasonName = current.getSeason().getName();
        EndlessChapter chapter = EndlessChapter.forSeason(seasonName);
        row.chapter = chapter == null ? seasonName : chapter.seasonName();

        for (int index = 0; index < ordered.size(); index++) {
            Level level = ordered.get(index);
            if (level.getSeason() == null
                    || !seasonName.equalsIgnoreCase(level.getSeason().getName())) {
                continue;
            }
            row.chapterStageCount++;
            if (index <= clearedIndex) row.chapterStagesCleared++;
        }
    }
}
