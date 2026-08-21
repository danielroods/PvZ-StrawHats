package controller.ui_menus;


import controller.CollectionManager;
import model.App;
import model.Regex;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.game_exceptions.GameException;
import model.user_data.User;
import model.user_data.UserState;
import view.GeneralPrinter;

import java.util.regex.Matcher;

public class CollectionMenu extends Menu {
    private final CollectionManager manager = new CollectionManager();

    @Override
    public String getName() {
        return "Collection Menu";
    }

    @Override
    public void handleCommand(String text){
        super.handleCommand(text);
        if (isGeneralCmd) return;


        UserState state = User.currentUser.userState;

        if (Regex.MENU_COLLECTION_SHOW_ALL_PLANTS.getMatcherRaw(text).matches()) {
            showAllPlants(state);
        } else if (Regex.MENU_COLLECTION_SHOW_PLANTS.getMatcherRaw(text).matches()) {
            showUnlockedPlants(state);
        } else if (Regex.MENU_COLLECTION_SHOW_PLANT.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.MENU_COLLECTION_SHOW_PLANT.getMatcherRaw(text);
            matcher.matches();
            showOnePlant(state, matcher.group("plantname"));
        } else if (Regex.MENU_COLLECTION_UPGRADE_PLANT.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.MENU_COLLECTION_UPGRADE_PLANT.getMatcherRaw(text);
            matcher.matches();
            upgradePlant(state, matcher.group("plantname"));
        } else if (Regex.MENU_COLLECTION_SHOW_ALL_ZOMBIES.getMatcherRaw(text).matches()) {
            showAllZombies();
        } else if (Regex.MENU_COLLECTION_SHOW_ZOMBIES.getMatcherRaw(text).matches()) {
            showSeenZombies(state);
        } else if (Regex.MENU_COLLECTION_SHOW_ZOMBIE.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.MENU_COLLECTION_SHOW_ZOMBIE.getMatcherRaw(text);
            matcher.matches();
            showOneZombie(state, matcher.group("zombiename"));
        } else if (Regex.MENU_COLLECTION_PURCHASE_PLANT.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.MENU_COLLECTION_PURCHASE_PLANT.getMatcherRaw(text);
            matcher.matches();
            purchasePlant(state, matcher.group("plantname"));
        }  else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    private void showAllPlants(UserState state) {
        for (PlantJsonParser.PlantConfig config : manager.getAllPlants()) {
            boolean unlocked = state.isPlantUnlocked(config.id);
            GeneralPrinter.print(manager.formatPlant(config, unlocked, state.getPlantLevel(config.id)));
        }
    }

    private void showUnlockedPlants(UserState state) {
        for (PlantJsonParser.PlantConfig config : manager.getUnlockedPlants(state)) {
            GeneralPrinter.print(manager.formatPlant(config, true, state.getPlantLevel(config.id)));
        }
    }

    private void showOnePlant(UserState state, String plantName) {
        PlantJsonParser.PlantConfig config = manager.findPlant(plantName);
        if (config == null) {
            throw new GameException("no such plant.");
        }
        boolean unlocked = state.isPlantUnlocked(config.id);
        GeneralPrinter.print(manager.formatPlant(config, unlocked, state.getPlantLevel(config.id)));
    }

    private void showAllZombies() {
        for (String alias : manager.getAllZombieAliases()) {
            GeneralPrinter.print(manager.formatZombie(manager.findZombie(alias)));
        }
    }

    private void showSeenZombies(UserState state) {
        for (String alias : manager.getSeenZombieAliases(state)) {
            GeneralPrinter.print(manager.formatZombie(manager.findZombie(alias)));
        }
    }

    private void showOneZombie(UserState state, String zombieName) {
        if (!manager.getSeenZombieAliases(state).contains(zombieName)) {
            throw new GameException("zombie not found or not yet observed.");
        }
        Zombie zombie = manager.findZombie(zombieName);
        GeneralPrinter.print(manager.formatZombie(zombie));
    }

    private void purchasePlant(UserState state, String plantName) {
        PlantJsonParser.PlantConfig config = manager.findPlant(plantName);
        if (config == null) {
            throw new GameException("no such plant.");
        } else if (state.isPlantUnlocked(config.id)) {
            throw new GameException("plant already unlocked.");
        } else if (!manager.purchasePlant(state, config)) {
            throw new GameException("not enough coins.");
        } else {
            GeneralPrinter.print("Plant purchased: " + config.name);
        }
    }

    private void upgradePlant(UserState state, String plantName) {
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

    @Override
    public void exitMenu() {
        App.currentMenu = new GameMenu();
    }

    @Override
    public String showMenu() {
        return "[ Collection Menu ]\nCommands:\n"
                + "  menu collection show-plants\n"
                + "  menu collection show-all-plants\n"
                + "  menu collection show-zombies\n"
                + "  menu collection show-all-zombies\n"
                + "  menu collection show-plant -p <plant_name>\n"
                + "  menu collection show-zombie -z <zombie_name>\n"
                + "  menu collection upgrade-plant -p <plant_name>\n"
                + "  menu collection purchase-plant -p <plant_name>\n"
                + "  menu exit | menu show current";
    }


}
