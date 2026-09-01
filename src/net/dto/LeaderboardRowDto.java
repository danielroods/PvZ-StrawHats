package net.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class LeaderboardRowDto {

    public String username;
    public String nickname;
    public String profilePicture;

    public String chapter = "-";
    public int chapterStagesCleared;
    public int chapterStageCount;
    public int levelsCleared;

    public int miniGamesWon;
    public int questsCompleted;
    public Integer myPoint;

    public Map<String, Long> lotteryScores = new LinkedHashMap<>();

    public Long lotteryScore(String chapterKey) {
        return lotteryScores == null ? null : lotteryScores.get(chapterKey);
    }

    public long lotteryScoreOrZero(String chapterKey) {
        Long score = lotteryScore(chapterKey);
        return score == null ? 0L : score;
    }

    public long myPointOrZero() {
        return myPoint == null ? 0L : myPoint;
    }

    public String usernameOrEmpty() {
        return username == null ? "" : username;
    }
}
