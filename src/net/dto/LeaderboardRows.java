package net.dto;

import model.match.main.levels.Level;
import model.user_data.User;

import java.util.List;

public final class LeaderboardRows {

    private LeaderboardRows() {
    }

    public static LeaderboardRowDto of(User user, List<Level> allLevels) {
        LeaderboardRowDto row = new LeaderboardRowDto();
        row.username = user.username;
        row.nickname = user.nickname;
        row.profilePicture = user.profilePicture;
        if (user.userState == null) return row;

        row.miniGamesWon = user.userState.miniGamesWon;
        row.questsCompleted = user.userState.questsCompleted;
        row.myPoint = user.userState.bonusHighScore;

        Level level = null;
        if (user.userState.lastLevel > 0 && allLevels != null) {
            level = allLevels.stream()
                    .filter(candidate -> candidate.getId() == user.userState.lastLevel)
                    .findFirst()
                    .orElse(null);
        }
        if (level == null) return row;

        row.stageLevelId = level.getId();
        row.season = level.getSeason() == null ? "-" : capitalize(level.getSeason().getName());
        String name = level.getName() == null ? "" : level.getName();
        int separator = name.indexOf(" - ");
        if (separator >= 0) {
            row.chapter = name.substring(0, separator);
            row.stage = name.substring(separator + 3);
        } else {
            row.chapter = name;
            row.stage = name;
        }
        return row;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
