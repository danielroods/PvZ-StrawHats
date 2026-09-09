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

    
    public Map<Integer, Set<String>> ownedPlantCostumes = new HashMap<>();

    
    public Map<Integer, String> selectedPlantCostumes = new HashMap<>();
    public List<List<PotData>> greenhousePots;
    public int plantFoodCount = 0;

    public List<GameQuest> activeQuests = new ArrayList<>();

    public Map<String, Integer> miniGameHighestLevelWon = new HashMap<>();

    
    public Map<String, Long> lotteryHighScores = new HashMap<>();

    public long stateRevision;
    public long stateUpdatedAt;

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

    public void markSaved() {
        stateRevision++;
        stateUpdatedAt = System.currentTimeMillis();
    }

    public boolean isNewerThan(UserState other) {
        if (other == null) return true;
        if (stateRevision != other.stateRevision) return stateRevision > other.stateRevision;
        return stateUpdatedAt > other.stateUpdatedAt;
    }

    public void adoptRevisionOf(UserState other) {
        if (other == null) return;
        stateRevision = other.stateRevision;
        stateUpdatedAt = other.stateUpdatedAt;
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

    public Map<Integer, Set<String>> ownedPlantCostumes() {
        if (ownedPlantCostumes == null) ownedPlantCostumes = new HashMap<>();
        return ownedPlantCostumes;
    }

    public boolean hasOwnedPlantCostume(int plantId, String costumeId) {
        if (costumeId == null || costumeId.isBlank()) return false;
        return ownedPlantCostumes().getOrDefault(plantId, Collections.emptySet()).contains(costumeId);
    }

    public void addOwnedPlantCostume(int plantId, String costumeId) {
        if (costumeId == null || costumeId.isBlank()) return;
        ownedPlantCostumes().computeIfAbsent(plantId, ignored -> new HashSet<>()).add(costumeId);
    }

    public String getSelectedPlantCostume(int plantId) {
        if (selectedPlantCostumes == null) selectedPlantCostumes = new HashMap<>();
        String value = selectedPlantCostumes.get(plantId);
        return value == null || value.isBlank() ? null : value;
    }

    public void setSelectedPlantCostume(int plantId, String costumeId) {
        if (selectedPlantCostumes == null) selectedPlantCostumes = new HashMap<>();
        if (costumeId == null || costumeId.isBlank()) selectedPlantCostumes.remove(plantId);
        else selectedPlantCostumes.put(plantId, costumeId);
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

    /**
     * Adventure progress only ever moves forward along the authored ladder. Ids at or
     * below zero are synthetic levels (the Lottery nodes), which are deliberately not part
     * of that ladder - letting one in would set lastLevel to a level that
     * LevelProgression cannot find and lock every stage behind it.
     */
    public void recordGameResult(int levelReached) {
        gamesPlayed++;
        if (levelReached > 0 && levelReached > lastLevel) lastLevel = levelReached;
    }

    /**
     * Brings a state loaded from disk (or from an older build) back to something every
     * reader can trust: Gson skips field initialisers, so the collections can come back
     * null, and a save written before recordGameResult() rejected synthetic level ids can
     * carry a negative lastLevel that reads as "no chapter progress at all".
     */
    public void repair() {
        if (news == null) news = new ArrayList<>();
        if (unlockedPlantIds == null) unlockedPlantIds = new HashSet<>();
        if (plantLevels == null) plantLevels = new HashMap<>();
        if (seedPacketInventory == null) seedPacketInventory = new HashMap<>();
        if (plantBoosts == null) plantBoosts = new HashMap<>();
        if (ownedPlantCostumes == null) ownedPlantCostumes = new HashMap<>();
        if (selectedPlantCostumes == null) selectedPlantCostumes = new HashMap<>();
        if (activeQuests == null) activeQuests = new ArrayList<>();
        if (miniGameHighestLevelWon == null) miniGameHighestLevelWon = new HashMap<>();
        if (lotteryHighScores == null) lotteryHighScores = new HashMap<>();
        if (lastLevel < 0) lastLevel = 0;
        if (gamesPlayed < 0) gamesPlayed = 0;
        if (miniGamesWon < 0) miniGamesWon = 0;
        if (questsCompleted < 0) questsCompleted = 0;
    }

    public boolean recordBonusScore(int score) {
        if (bonusHighScore != null && score <= bonusHighScore) return false;
        bonusHighScore = score;
        return true;
    }

    public long getLotteryHighScore(String chapterKey) {
        if (lotteryHighScores == null) lotteryHighScores = new HashMap<>();
        Long best = lotteryHighScores.get(normaliseChapterKey(chapterKey));
        return best == null ? 0L : best;
    }

    public boolean hasLotteryScore(String chapterKey) {
        if (lotteryHighScores == null) lotteryHighScores = new HashMap<>();
        return lotteryHighScores.containsKey(normaliseChapterKey(chapterKey));
    }

    /**
     * Keeps the better of the stored and given score for one chapter. My Point stays the
     * best endless run across every chapter, which is the only thing that ever wrote it.
     */
    public boolean recordLotteryScore(String chapterKey, long score) {
        if (lotteryHighScores == null) lotteryHighScores = new HashMap<>();
        String key = normaliseChapterKey(chapterKey);
        if (key.isEmpty()) return false;
        Long best = lotteryHighScores.get(key);
        boolean improved = best == null || score > best;
        if (improved) lotteryHighScores.put(key, score);
        recordBonusScore(bestLotteryScoreAsPoints());
        return improved;
    }

    public long bestLotteryScore() {
        if (lotteryHighScores == null) return 0L;
        long best = 0L;
        for (Long score : lotteryHighScores.values()) {
            if (score != null && score > best) best = score;
        }
        return best;
    }

    private int bestLotteryScoreAsPoints() {
        return (int) Math.min(Integer.MAX_VALUE, bestLotteryScore());
    }

    private String normaliseChapterKey(String chapterKey) {
        return chapterKey == null ? ""
                : chapterKey.toLowerCase().replace(" ", "_").replace("-", "_").trim();
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