package net.server;

import model.match.main.levels.Level;
import model.user_data.User;
import model.utils.LevelLoader;
import net.dto.LeaderboardRowDto;
import net.dto.LeaderboardRows;
import service.Log;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardService {

    private final AccountStore accounts;
    private List<Level> levelCache;

    public LeaderboardService(AccountStore accounts) {
        this.accounts = accounts;
    }

    public List<LeaderboardRowDto> rows() {
        List<Level> levels = levels();
        List<LeaderboardRowDto> rows = new ArrayList<>();
        for (User user : accounts.snapshot()) {
            rows.add(LeaderboardRows.of(user, levels));
        }
        return rows;
    }

    private List<Level> levels() {
        if (levelCache != null) return levelCache;
        try {
            levelCache = LevelLoader.loadLevels();
        } catch (Exception e) {
            Log.error("Leaderboard", "Could not load levels", e);
            levelCache = List.of();
        }
        return levelCache;
    }
}
