package controller.match;

import controller.CollectionManager;
import controller.ui_menus.Menu;
import model.App;
import model.Regex;
import model.collections.plant.PlantJsonParser;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantFoodType;
import model.game_exceptions.GameException;
import model.match.main.levels.Level;
import model.match.main.levels.special_levels.ConveyorBeltLevel;
import model.match.main.levels.special_levels.LockedPlantsLevel;
import model.match.main.levels.special_levels.PlantWhatYouGetLevel;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.GameSession;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class BeforeMenu extends Menu {
    private static final int PLANT_SLOTS = 8;
    public static List<String> selectedPlants = new ArrayList<>();

    private final CollectionManager manager = new CollectionManager();

    @Override
    public String getName() {
        return "Before Menu";
    }

    @Override
    public void handleCommand(String text) {
        if (Regex.SHOW_ALL_PLANTS.getMatcherRaw(text).matches()) {
            showAllPlants();
        } else if (Regex.SHOW_AVAILABLE_PLANTS.getMatcherRaw(text).matches()) {
            showAvailablePlants();
        } else if (Regex.ADD_PLANT.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.ADD_PLANT.getMatcherRaw(text);
            matcher.matches();
            addPlant(matcher.group("type"));
        } else if (Regex.REMOVE_PLANT.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.REMOVE_PLANT.getMatcherRaw(text);
            matcher.matches();
            removePlant(matcher.group("type"));
        } else if (Regex.BOOST_PLANT.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.BOOST_PLANT.getMatcherRaw(text);
            matcher.matches();
            boostPlant(matcher.group("type"));
        } else if (Regex.UPGRADE_PLANT.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.UPGRADE_PLANT.getMatcherRaw(text);
            matcher.matches();
            upgradePlant(matcher.group("type"));
        } else if (Regex.START_GAME.getMatcherRaw(text).matches()) {
            startMatch();
        } else if (Regex.MENU_ENTER.getMatcherRaw(text).matches()) {
            changeMenu(text);
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else if (Regex.MENU_SHOW_CURRENT.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(showMenu());
        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    private Level currentLevel() {
        return GameSession.getInstance().getLevel();
    }

    private void showAllPlants() {
        UserState state = User.currentUser.userState;
        for (PlantJsonParser.PlantConfig config : manager.getUnlockedPlants(state)) {
            GeneralPrinter.print(manager.formatPlant(config, true, state.getPlantLevel(config.id)));
        }
    }

    private void showAvailablePlants() {
        Level level = currentLevel();
        if (level == null) {
            throw new GameException("no level loaded.");
        }
        if (level instanceof ConveyorBeltLevel conveyor) {
            if (conveyor.getConveyorPlants() == null || conveyor.getConveyorPlants().isEmpty()) {
                GeneralPrinter.print("No conveyor plants configured.");
                return;
            }
            conveyor.getConveyorPlants().stream().map(plant -> plant.getName()).distinct()
                    .forEach(GeneralPrinter::print);
            return;
        }

        UserState state = User.currentUser.userState;
        for (PlantJsonParser.PlantConfig config : manager.getUnlockedPlants(state)) {
            if (isAllowedInCurrentLevel(config.name)) {
                GeneralPrinter.print(manager.formatPlant(config, true, state.getPlantLevel(config.id)));
            }
        }
    }

    private void addPlant(String plantName) {
        UserState state = User.currentUser.userState;
        PlantJsonParser.PlantConfig config = manager.findPlant(plantName);
        if (config == null || !state.isPlantUnlocked(config.id)) {
            throw new GameException("plant not available.");
        } else if (!isAllowedInCurrentLevel(config.name)) {
            throw new GameException("plant is locked for this stage.");
        } else if (selectedPlants.contains(config.name)) {
            throw new GameException("plant already selected.");
        } else if (selectedPlants.size() >= PLANT_SLOTS) {
            throw new GameException("no free plant slots.");
        } else {
            selectedPlants.add(config.name);
            GeneralPrinter.print(config.name + " added to loadout.");
        }
    }

    private void removePlant(String plantName) {
        if (selectedPlants.removeIf(name -> name.equalsIgnoreCase(plantName))) {
            GeneralPrinter.print(plantName + " removed from loadout.");
        } else {
            throw new GameException("plant not in loadout.");
        }
    }

    private static final int BOOST_COST_DIAMONDS = 2;

    private void boostPlant(String plantName) {
        UserState state = User.currentUser.userState;
        String imitaterTarget = selectedPlants.stream()
                .filter(name -> name != null && !name.equalsIgnoreCase("Imitater"))
                .findFirst()
                .orElse(null);
        PlantFactory.setImitaterTargetName(imitaterTarget);

        GameSession session = GameSession.getInstance();
        PlantJsonParser.PlantConfig config = manager.findPlant(plantName);
        if (config == null || !state.isPlantUnlocked(config.id)) {
            throw new GameException("plant not available.");
        } else if (!isAllowedInCurrentLevel(config.name)) {
            throw new GameException("plant is locked for this stage.");
        } else if (config.plantFoodType == null || config.plantFoodType == PlantFoodType.NONE) {
            throw new GameException("this plant has no plant food effect.");
        } else if (session.hasMatchBoost(config.id)) {
            throw new GameException("plant already boosted for this match.");
        } else if (state.diamonds < BOOST_COST_DIAMONDS) {
            throw new GameException("not enough diamonds.");
        } else {
            state.diamonds -= BOOST_COST_DIAMONDS;
            session.grantMatchBoost(config.id);
            User.save();
            GeneralPrinter.print(config.name + " boosted for this match.");
        }
    }

    private void upgradePlant(String plantName) {
        UserState state = User.currentUser.userState;
        PlantJsonParser.PlantConfig config = manager.findPlant(plantName);
        if (config == null) {
            throw new GameException("no such plant.");
        } else if (!state.isPlantUnlocked(config.id)) {
            throw new GameException("plant is locked.");
        }

        int currentLevel = state.getPlantLevel(config.id);
        int coinCost = currentLevel * 500;
        int packetsNeeded = currentLevel;
        int packetsOwned = state.seedPacketInventory.getOrDefault(config.id, 0);

        if (state.coins < coinCost) {
            throw new GameException("not enough coins (" + state.coins + "/" + coinCost + ").");
        } else if (packetsOwned < packetsNeeded) {
            throw new GameException("not enough " + config.name + " seed packets (" + packetsOwned + "/" + packetsNeeded + "). Buy some from the store.");
        } else if (!manager.upgradePlant(state, config)) {
            throw new GameException("upgrade failed.");
        } else {
            GeneralPrinter.print("Plant upgraded: " + config.name + " -> level " + state.getPlantLevel(config.id));
        }
    }

    private void startMatch() {
        Level level = currentLevel();
        if (level == null) {
            throw new GameException("no level loaded.");

        }
        if (level.getForcedPlants() != null) {
            for (String forced : level.getForcedPlants()) {
                if (selectedPlants.stream().noneMatch(name -> name.equalsIgnoreCase(forced))) {
                    if (selectedPlants.size() >= PLANT_SLOTS) {
                        throw new GameException("forced plants do not fit in the loadout.");
                    }
                    selectedPlants.add(forced);
                }
            }
        }
        if (!(level instanceof ConveyorBeltLevel) && selectedPlants.isEmpty()) {
            throw new GameException("select at least one plant before starting.");
        }
        GameSession session = GameSession.getInstance();
        session.setDifficultyLevel(User.currentUser.userState.difficultyLevel);
        if (!(level instanceof PlantWhatYouGetLevel)) session.startWaves();
        App.currentMenu = new GameplayMenu();
    }

    private boolean isAllowedInCurrentLevel(String plantName) {
        Level level = currentLevel();
        if (level == null || level instanceof ConveyorBeltLevel) return false;
        if (level instanceof LockedPlantsLevel lockedLevel) {
            return !lockedLevel.isPlantLocked(plantName);
        }
        if (level instanceof PlantWhatYouGetLevel) {
            return !plantName.toLowerCase().contains("sunflower");
        }
        return true;
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new MatchMenu();
    }

    @Override
    public String showMenu() {
        Level level = currentLevel();
        String levelName = level != null ? level.getName() : "unknown";
        return "[ Plant Loadout ]\n"
                + "Stage: " + levelName + " | Game mode: "
                + (level == null ? "unknown" : level.getGameMode())
                + " | Selected: " + selectedPlants + " (" + selectedPlants.size() + "/" + PLANT_SLOTS + ")\n"
                + "Commands:\n"
                + "  show all plants\n"
                + "  show available plants\n"
                + "  add plant -t <type>\n"
                + "  remove plant -t <type>\n"
                + "  boost plant -t <type>\n"
                + "  upgrade plant -t <type>\n"
                + "  start game\n"
                + "  menu exit | menu show current";
    }
}