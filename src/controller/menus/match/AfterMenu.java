package controller.menus.match;

import controller.CollectionManager;
import controller.NewsManager;
import controller.menus.GameMenu;
import controller.menus.Menu;
import model.App;
import model.Regex;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.match.main.levels.Level;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.GameSession;
import model.utils.LevelLoader;
import model.utils.LevelProgression;
import view.GeneralPrinter;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class AfterMenu extends Menu {
    private static final Random RANDOM = new Random();
    private static final CollectionManager MANAGER = new CollectionManager();

    private static boolean won;
    private static boolean rewardGranted = false;
    private static int coinsAwarded;
    private static String seedPacketPlantName = "None";

    public static void reset(boolean matchWon) {
        won = matchWon;
        rewardGranted = false;
    }

    public AfterMenu() {
        if (!rewardGranted) {
            grantReward();
            rewardGranted = true;
            GeneralPrinter.print(showMenu());
        }
    }

    private void grantReward() {
        UserState state = User.currentUser.userState;
        Level level = GameSession.getInstance().getLevel();

        List<Level> allLevels;
        try {
            allLevels = LevelProgression.sorted(LevelLoader.loadLevels());
        } catch (Exception e) {
            allLevels = List.of();
        }
        int previousLastLevel = state.lastLevel;
        Set<String> seenZombiesBefore = MANAGER.getSeenZombieAliases(state);

        coinsAwarded = won ? 100 + RANDOM.nextInt(101) : 20 + RANDOM.nextInt(21);
        state.coins += coinsAwarded;

        List<PlantJsonParser.PlantConfig> unlocked = MANAGER.getUnlockedPlants(state);
        if (!unlocked.isEmpty()) {
            PlantJsonParser.PlantConfig picked = unlocked.get(RANDOM.nextInt(unlocked.size()));
            state.addSeedPackets(picked.id, 1);
            seedPacketPlantName = picked.name;
        } else {
            seedPacketPlantName = "None";
        }

        int completedLevelId = won && level != null ? level.getId() : state.lastLevel;
        state.recordGameResult(completedLevelId, coinsAwarded);

        for (Level candidate : allLevels) {
            boolean unlockedBefore = LevelProgression.isUnlocked(allLevels, previousLastLevel, candidate);
            boolean unlockedAfter = LevelProgression.isUnlocked(allLevels, state.lastLevel, candidate);
            if (!unlockedBefore && unlockedAfter) {
                String chapterName = candidate.getSeason() != null ? candidate.getSeason().getName() : "Unknown";
                String info = "Chapter: " + chapterName + " | Game mode: " + candidate.getGameMode();
                NewsManager.generateNews("LEVEL", candidate.getName(), info);
            }
        }

        Set<String> seenZombiesAfter = MANAGER.getSeenZombieAliases(state);
        Set<String> newlySeenZombies = new HashSet<>(seenZombiesAfter);
        newlySeenZombies.removeAll(seenZombiesBefore);
        for (String alias : newlySeenZombies) {
            Zombie zombie = MANAGER.findZombie(alias);
            String info = zombie != null ? MANAGER.formatZombie(zombie) : "";
            NewsManager.generateNews("ZOMBIE", alias, info);
        }

        User.save();
    }

    @Override
    public String getName() {
        return "After Menu";
    }

    @Override
    public void handleCommand(String text){
        super.handleCommand(text);
        if (isGeneralCmd) return;



        if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new GameMenu();
    }

    @Override
    public String showMenu() {
        return (won ? "YOU WIN!" : "The zombie ate your brain; LOSER !!!") +
                "\nReward: +" + coinsAwarded + " coins, +1 seed packet (" + seedPacketPlantName + ")";
    }
}
