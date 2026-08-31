package model.user_data;

import model.greenhouse.PotData;
import model.news.News;
import model.quests.GameQuest;

import java.util.*;

public class UserState {

    public List<News> news;
    public int lastLevel, diamonds, coins;
    public int difficultyLevel;
    public int gamesPlayed = 0;
    public int highScore = 0;
    public Integer bonusHighScore;
    public int miniGamesWon = 0;
    public int questsCompleted = 0;

    public Set<Integer> unlockedPlantIds = new HashSet<>();
    public Map<Integer, Integer> plantLevels = new HashMap<>();
    public Map<Integer, Integer> seedPacketInventory = new HashMap<>();
    public Map<Integer, Boolean> plantBoosts = new HashMap<>();
    public List<List<PotData>> greenhousePots;
    public int plantFoodCount = 0;

    public List<GameQuest> activeQuests = new ArrayList<>();

    public Map<String, Integer> miniGameHighestLevelWon = new HashMap<>();

    public String dailyOfferDate;
    public Integer dailyOfferPlantId;
    public boolean dailyOfferPurchased;

    public UserState(List<News> news, int lastLevel, int diamonds, int coins) {
        this.news = news;
        this.lastLevel = lastLevel;
        this.diamonds = diamonds;
        this.coins = coins;
        this.difficultyLevel = 3;
        this.unlockedPlantIds.add(1);
    }

    public boolean isPlantUnlocked(int plantId) {
        return unlockedPlantIds.contains(plantId);
    }

    public boolean unlockPlant(int plantId) {
        return unlockedPlantIds.add(plantId);
    }

    public int getPlantLevel(int plantId) {
        return plantLevels.getOrDefault(plantId, 1);
    }

    public void setPlantLevel(int plantId, int level) {
        plantLevels.put(plantId, level);
    }

    public boolean hasBoost(int plantId) {
        if (plantBoosts == null) plantBoosts = new HashMap<>();
        return plantBoosts.getOrDefault(plantId, false);
    }

    public boolean grantBoost(int plantId) {
        if (plantBoosts == null) plantBoosts = new HashMap<>();
        if (hasBoost(plantId)) return false;
        plantBoosts.put(plantId, true);
        return true;
    }

    public boolean consumeBoost(int plantId) {
        if (plantBoosts == null) plantBoosts = new HashMap<>();
        if (!hasBoost(plantId)) return false;
        plantBoosts.remove(plantId);
        return true;
    }

    public void addSeedPackets(int plantId, int count) {
        seedPacketInventory.merge(plantId, count, Integer::sum);
    }

    public void addNews(News item) {
        if (item != null) news.add(item);
    }

    public void recordGameResult(int levelReached) {
        gamesPlayed++;
        if (levelReached > lastLevel) lastLevel = levelReached;
    }

    public boolean recordBonusScore(int score) {
        if (bonusHighScore != null && score <= bonusHighScore) return false;
        bonusHighScore = score;
        return true;
    }

    public boolean hasBonusScore() {
        return bonusHighScore != null;
    }

    public boolean hasUnreadNews() {
        for (News item : news) {
            if (!item.isRead()) return true;
        }
        return false;
    }

    /** Highest difficulty (1-3) the player has beaten for the given mini-game key
     *  (e.g. "beghouled", "zombotany"), or 0 if none has been won yet. */
    public int getMiniGameHighestLevelWon(String miniGameKey) {
        if (miniGameHighestLevelWon == null) miniGameHighestLevelWon = new HashMap<>();
        return miniGameHighestLevelWon.getOrDefault(normaliseMiniGameKey(miniGameKey), 0);
    }

    public void recordMiniGameWin(String miniGameKey, int level) {
        if (miniGameHighestLevelWon == null) miniGameHighestLevelWon = new HashMap<>();
        String key = normaliseMiniGameKey(miniGameKey);
        int current = miniGameHighestLevelWon.getOrDefault(key, 0);
        if (level > current) miniGameHighestLevelWon.put(key, level);
    }

    /** Level 1 is always playable; any later level requires the previous one to
     *  have been won at least once. */
    public boolean isMiniGameLevelUnlocked(String miniGameKey, int level) {
        if (level <= 1) return true;
        return getMiniGameHighestLevelWon(miniGameKey) >= level - 1;
    }

    private String normaliseMiniGameKey(String miniGameKey) {
        return miniGameKey == null ? "" : miniGameKey.toLowerCase().replace("-", "").replace(" ", "").replace("_", "").trim();
    }
}