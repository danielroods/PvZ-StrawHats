package controller.match;

import controller.CollectionManager;
import controller.NewsManager;
import controller.ui_menus.GameMenu;
import controller.ui_menus.LeaderboardMenu;
import controller.ui_menus.Menu;
import controller.ui_menus.greenhouse.GreenhouseMenu;
import model.App;
import model.Regex;
import model.collections.plant.Plant;
import model.collections.plant.PlantJsonParser;
import model.match.main.levels.Level;
import model.match.main.levels.special_levels.ConveyorBeltLevel;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.GameSession;
import model.utils.LevelProgression;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.List;

public class MatchMenu extends Menu {
    public static Level selectedLevel;
    private static List<Level> allLevels = new ArrayList<>();
    private static List<Level> chapterLevels = new ArrayList<>();

    public static void configureChapter(List<Level> everyLevel, List<Level> unlockedChapterLevels,
                                        Level recommendedLevel) {
        allLevels = new ArrayList<>(everyLevel);
        chapterLevels = new ArrayList<>(unlockedChapterLevels);
        selectedLevel = recommendedLevel;
    }

    @Override
    public String getName() {
        return "Match Menu";
    }

    @Override
    public void handleCommand(String text){
        super.handleCommand(text);
        if (isGeneralCmd) return;





        if (Regex.MENU_SHOW_STAGES.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(formatStageList());
        } else if (Regex.MENU_SELECT_STAGE.getMatcherRaw(text).matches()) {
            var matcher = Regex.MENU_SELECT_STAGE.getMatcherRaw(text);
            matcher.matches();
            selectStage(Integer.parseInt(matcher.group("stage")));
        } else if (Regex.START_GAME.getMatcherRaw(text).matches()) {
            if (selectedLevel == null) {
                throw new model.game_exceptions.GameException("select a stage first.");
            }
            GameSession session = new GameSession(selectedLevel.getRows(), selectedLevel.getCols());
            session.setDifficultyLevel(User.currentUser.userState.difficultyLevel);
            session.setLevel(selectedLevel);
            unlockStagePlants(selectedLevel);
            BeforeMenu.selectedPlants.clear();
            if (selectedLevel instanceof ConveyorBeltLevel) {
                session.startWaves();
                App.currentMenu = new GameplayMenu();
                GeneralPrinter.print("Conveyor stage started. Your first plant is ready on the belt.");
            } else {
                App.currentMenu = new BeforeMenu();
            }
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else if (Regex.MENU_GREENHOUSE.getMatcherRaw(text).matches()) {
            App.currentMenu = new GreenhouseMenu();
        } else if (Regex.MENU_LEADERBOARD.getMatcherRaw(text).matches()) {
            App.currentMenu = new LeaderboardMenu();
        } else {
            GeneralPrinter.print("Not Valid. Type 'menu show current' to see the commands available here.");
        }
    }

    private void selectStage(int value) {
        Level chosen = chapterLevels.stream().filter(level -> level.getId() == value).findFirst().orElse(null);
        if (chosen == null && value >= 1 && value <= chapterLevels.size()) {
            chosen = chapterLevels.get(value - 1);
        }
        if (chosen == null) {
            throw new model.game_exceptions.GameException("stage is locked or does not exist.");
        }
        selectedLevel = chosen;
        GeneralPrinter.print("Selected " + chosen.getName()
                + " (Game mode: " + chosen.getGameMode() + "). Use 'start game' to enter this stage.");
    }

    private void unlockStagePlants(Level level) {
        List<String> plantNames = new ArrayList<>();
        if (level.getAvailablePlants() != null) plantNames.addAll(level.getAvailablePlants());
        if (level.getForcedPlants() != null) plantNames.addAll(level.getForcedPlants());
        if (level instanceof ConveyorBeltLevel conveyor && conveyor.getConveyorPlants() != null) {
            for (Plant plant : conveyor.getConveyorPlants()) plantNames.add(plant.getName());
        }

        for (PlantJsonParser.PlantConfig config : model.collections.plant.PlantFactory.getBlueprints().values()) {
            if (plantNames.stream().anyMatch(name -> name.equalsIgnoreCase(config.name))) {
                UserState state = User.currentUser.userState;
                if (state.unlockPlant(config.id)) {
                    NewsManager.generateNews("PLANT", config.name,
                            new CollectionManager().formatPlant(config, true,
                                    model.collections.plant.PlantProgression.levelOf(state, config)));
                }
            }
        }
    }

    private static String formatStageList() {
        if (chapterLevels.isEmpty()) return "No unlocked stages in this chapter.";
        StringBuilder result = new StringBuilder("Unlocked stages:");
        for (int i = 0; i < chapterLevels.size(); i++) {
            Level level = chapterLevels.get(i);
            boolean completed = LevelProgression.isCompleted(allLevels,
                    User.currentUser.userState.lastLevel, level);
            result.append("\n  ").append(i + 1).append(") ")
                    .append(level.getName()).append(" [id ").append(level.getId()).append("] - ")
                    .append(completed ? "Completed" : "Available");
            if (selectedLevel != null && selectedLevel.getId() == level.getId()) result.append(" (selected)");
            result.append("\n     Game mode: ").append(level.getGameMode())
                    .append(" | Waves: ").append(level.getWaves() == null ? 0 : level.getWaves().size())
                    .append(" | Zombie pool: ").append(level.getZombiePool());
        }
        return result.toString();
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new GameMenu();
    }

    @Override
    public String showMenu() {
        StringBuilder sb = new StringBuilder();
        if (selectedLevel == null) {
            sb.append("[ Match Menu ]\nNo stage selected.\n");
        } else {
            sb.append("Chapter: ").append(selectedLevel.getSeason().getName())
                    .append(" | Stage: ").append(selectedLevel.getName()).append("\n")
                    .append("Game mode: ").append(selectedLevel.getGameMode())
                    .append(" | Waves: ").append(selectedLevel.getWaves() == null ? 0 : selectedLevel.getWaves().size())
                    .append("\nZombie pool: ").append(selectedLevel.getZombiePool()).append("\n");
        }
        sb.append("Commands:\n");
        sb.append("  show stages\n");
        sb.append("  select stage -s <stage_number_or_id>\n");
        sb.append("  start game   (choose your plant loadout for this stage)\n");
        sb.append("  menu exit\n");
        sb.append("  menu show current");
        return sb.toString();
    }
}
