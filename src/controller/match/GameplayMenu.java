package controller.match;

import controller.CollectionManager;
import controller.ui_menus.GameMenu;
import controller.ui_menus.Menu;
import model.App;
import model.Regex;
import model.collections.item.GroundItem;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.game_exceptions.GameException;
import model.match.main.levels.special_levels.ConveyorBeltLevel;
import model.match.main.levels.special_levels.PlantWhatYouGetLevel;
import model.match_mechanisms.vector.Position;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.GameSession;
import model.utils.LevelLoader;
import view.GeneralPrinter;

import java.util.List;
import java.util.regex.Matcher;

public class GameplayMenu extends Menu {

    private final CollectionManager manager = new CollectionManager();
    private boolean paused = false;

    @Override
    public String getName() {
        return "Meanwhile Menu";
    }

    @Override
    public void handleCommand(String text) {
        String normalized = text.trim().toLowerCase();
        if (normalized.equals("pause")) {
            if (paused) {
                GeneralPrinter.print("The match is already paused.");
            } else {
                paused = true;
                GeneralPrinter.print("Game paused. Time will not advance until you use 'resume'.");
            }
        } else if (normalized.equals("resume")) {
            if (!paused) {
                GeneralPrinter.print("The match is already running.");
            } else {
                paused = false;
                GeneralPrinter.print("Resuming match...");
            }
        } else if (paused && !isAllowedWhilePaused(text)) {
            throw new GameException("the match is paused; use 'resume' before taking that action.");
        } else if (normalized.equals("restart")) {
            paused = false;
            restartMatch();
        } else if (text.toLowerCase().startsWith("end game")) {
            requestEndGame(text);
        } else if (Regex.PLANT_AT.getMatcherRaw(text).matches()
                || Regex.PLANT_ON_FIELD.getMatcherRaw(text).matches()) {
            Matcher m = Regex.PLANT_AT.getMatcherRaw(text).matches()
                    ? Regex.PLANT_AT.getMatcherRaw(text)
                    : Regex.PLANT_ON_FIELD.getMatcherRaw(text);
            m.matches();
            plantAt(m.group("type"), Integer.parseInt(m.group("x")), Integer.parseInt(m.group("y")));
        } else if (Regex.REMOVE_PLANT_AT.getMatcherRaw(text).matches()
                || Regex.PLUCK_PLANT_FIELD.getMatcherRaw(text).matches()) {
            Matcher m = Regex.REMOVE_PLANT_AT.getMatcherRaw(text).matches()
                    ? Regex.REMOVE_PLANT_AT.getMatcherRaw(text)
                    : Regex.PLUCK_PLANT_FIELD.getMatcherRaw(text);
            m.matches();
            removePlantAt(Integer.parseInt(m.group("x")), Integer.parseInt(m.group("y")));
        } else if (Regex.DIG_PLANT_AT.getMatcherRaw(text).matches()) {
            Matcher m = Regex.DIG_PLANT_AT.getMatcherRaw(text);
            m.matches();
            digPlantAt(Integer.parseInt(m.group("x")), Integer.parseInt(m.group("y")));
        } else if (Regex.COLLECT_ITEM.getMatcherRaw(text).matches()) {
            Matcher m = Regex.COLLECT_ITEM.getMatcherRaw(text);
            m.matches();
            collectAt(Integer.parseInt(m.group("x")), Integer.parseInt(m.group("y")));
        } else if (Regex.USE_PLANT_FOOD.getMatcherRaw(text).matches()) {
            Matcher m = Regex.USE_PLANT_FOOD.getMatcherRaw(text);
            m.matches();
            useFoodAt(Integer.parseInt(m.group("x")), Integer.parseInt(m.group("y")));
        } else if (Regex.FEED_PLANT_FIELD.getMatcherRaw(text).matches()) {
            Matcher m = Regex.FEED_PLANT_FIELD.getMatcherRaw(text);
            m.matches();
            useFoodAt(Integer.parseInt(m.group("x")), Integer.parseInt(m.group("y")));
        } else if (Regex.SHOW_SUN_AMOUNT.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Sun: " + GameSession.getInstance().getSunCount());
        } else if (Regex.SHOW_PLANT_FOOD_AMOUNT.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Plant food: " + GameSession.getInstance().getPlantFoodCount());
        } else if (Regex.COLLECT_SUN.getMatcherRaw(text).matches()) {
            Matcher m = Regex.COLLECT_SUN.getMatcherRaw(text);
            m.matches();
            collectAt(Integer.parseInt(m.group("x")), Integer.parseInt(m.group("y")));
        } else if (Regex.CHEAT_ADD_SUNS.getMatcherRaw(text).matches()) {
            Matcher m = Regex.CHEAT_ADD_SUNS.getMatcherRaw(text);
            m.matches();
            GameSession.getInstance().addSun(Integer.parseInt(m.group("count")));
            GeneralPrinter.print("Cheated in " + m.group("count") + " sun. Sun: " + GameSession.getInstance().getSunCount());
        } else if (Regex.CHEAT_ADD_PLANT_FOOD.getMatcherRaw(text).matches()) {
            boolean added = GameSession.getInstance().addPlantFood();
            GeneralPrinter.print(added
                    ? "Cheated in 1 plant food. Plant food: " + GameSession.getInstance().getPlantFoodCount()
                    : "Plant food storage is already full (3).");
        } else if (Regex.CHEAT_REMOVE_COOLDOWN.getMatcherRaw(text).matches()) {
            GameSession.getInstance().removeAllCooldowns();
            GeneralPrinter.print("All plant cooldowns were removed.");
        } else if (Regex.CHEAT_SPAWN_ZOMBIE.getMatcherRaw(text).matches()) {
            Matcher m = Regex.CHEAT_SPAWN_ZOMBIE.getMatcherRaw(text);
            m.matches();
            spawnZombie(m.group("type"), Integer.parseInt(m.group("x")), Integer.parseInt(m.group("y")));
        } else if (Regex.RELEASE_THE_NUKE.getMatcherRaw(text).matches()) {
            GameSession.getInstance().killAllZombies();
            GeneralPrinter.print("All zombies were removed.");
        } else if (Regex.START_ZOMBIE_WAVES.getMatcherRaw(text).matches()) {
            startZombieWaves();
        } else if (Regex.SHOW_GARDEN.getMatcherRaw(text).matches()
                || Regex.SHOW_MAP.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(GameSession.getInstance().renderMap());
            GeneralPrinter.print(GameSession.getInstance().renderPlantsStatus());
        } else if (Regex.SHOW_PLANT_STATUS.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(GameSession.getInstance().renderPlantsStatus());
        } else if (Regex.ZOMBIES_INFO.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(GameSession.getInstance().renderZombiesInfo());
        } else if (Regex.SHOW_TILE.getMatcherRaw(text).matches()
                || Regex.SHOW_TILE_STATUS.getMatcherRaw(text).matches()) {
            Matcher m = Regex.SHOW_TILE.getMatcherRaw(text).matches()
                    ? Regex.SHOW_TILE.getMatcherRaw(text)
                    : Regex.SHOW_TILE_STATUS.getMatcherRaw(text);
            m.matches();
            int x = Integer.parseInt(m.group("x"));
            int y = Integer.parseInt(m.group("y"));
            GeneralPrinter.print(GameSession.getInstance().renderTileStatus(y - 1, x - 1));
        } else if (Regex.WAIT_SECONDS.getMatcherRaw(text).matches()) {
            Matcher m = Regex.WAIT_SECONDS.getMatcherRaw(text);
            m.matches();
            waitSeconds(Integer.parseInt(m.group("seconds")));
        } else if (Regex.ADVANCE_TIME.getMatcherRaw(text).matches()) {
            Matcher m = Regex.ADVANCE_TIME.getMatcherRaw(text);
            m.matches();
            advanceTicks(Integer.parseInt(m.group("ticks")));
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else if (Regex.MENU_SHOW_CURRENT.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(showMenu());
        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    private void plantAt(String plantName, int x, int y) {
        GameSession session = GameSession.getInstance();
        UserState state = User.currentUser.userState;

        if (session.getLevel() instanceof ConveyorBeltLevel conveyor) {
            Plant offered = conveyor.getCurrentPlant();
            if (offered == null) throw new GameException("the conveyor is empty; advance time for the next plant.");
            if (!offered.getName().equalsIgnoreCase(plantName)) {
                throw new GameException("the conveyor currently offers " + offered.getName() + ".");
            }
            if (!session.plantAt(y - 1, x - 1, offered)) {
                throw new GameException("that tile is occupied, blocked, or out of bounds.");
            }
            conveyor.takeCurrentPlant();
            GeneralPrinter.print(offered.getName() + " planted from the conveyor at (" + x + "," + y + ").");
            return;
        }

        PlantJsonParser.PlantConfig config = manager.findPlant(plantName);
        if (config == null || !state.isPlantUnlocked(config.id)) {
            throw new GameException("plant not available.");
        }
        if (BeforeMenu.selectedPlants.stream().noneMatch(name -> name.equalsIgnoreCase(config.name))) {
            throw new GameException("plant not in this match's loadout.");
        }

        int row = y - 1;
        int col = x - 1;
        int level = state.getPlantLevel(config.id);
        Plant plant = PlantFactory.createPlant(config.id, level, new Position(col, row));

        boolean freePlantingTime = session.getLevel() instanceof PlantWhatYouGetLevel
                && !session.isWavesStarted();
        if (!freePlantingTime && !session.isPlantReady(config.id)) {
            throw new GameException(config.name + " is recharging for "
                    + String.format("%.1f", session.getPlantCooldown(config.id)) + " more seconds.");
        }
        if (session.getSunCount() < plant.getCost()) {
            throw new GameException("not enough sun; " + config.name + " costs " + plant.getCost() + ".");
        }

        if (!session.plantAt(row, col, plant)) {
            throw new GameException("that tile is occupied, blocked, or out of bounds.");
        } else {
            session.spendSun(plant.getCost());
            if (!freePlantingTime) session.startPlantCooldown(config.id, plant.getRecharge());
            boolean matchBoost = session.hasMatchBoost(config.id);
            boolean reservedBoost = !matchBoost && state.hasBoost(config.id);
            if ((matchBoost || reservedBoost) && plant.activatePlant(session) && reservedBoost) {
                state.consumeBoost(config.id);
                User.save();
            }
            GeneralPrinter.print(config.name + " planted at (" + x + "," + y + "). Sun: "
                    + session.getSunCount() + ".");
        }
    }

    private void removePlantAt(int x, int y) {
        boolean removed = GameSession.getInstance().removePlantAt(y - 1, x - 1);
        GeneralPrinter.print(removed ? "Plant removed." : "Error: no plant there.");
    }

    private void digPlantAt(int x, int y) {
        Plant dug = GameSession.getInstance().digPlantAt(y - 1, x - 1);
        if (dug == null) {
            GeneralPrinter.print("Error: no plant there.");
        } else {
            User.currentUser.userState.addSeedPackets(dug.getId(), 1);
            GeneralPrinter.print("Dug up " + dug.getName() + " at (" + x + "," + y + "); seed packet returned to inventory.");
        }
    }

    private void collectAt(int x, int y) {
        List<GroundItem> collected = GameSession.getInstance()
                .collectItemsNear(new Position(x - 1, y - 1));
        if (collected.isEmpty()) {
            GeneralPrinter.print("Nothing to collect there.");
        } else {
            GeneralPrinter.print("Collected " + collected.size() + " item(s).");
        }
    }

    private void useFoodAt(int x, int y) {
        GameSession session = GameSession.getInstance();
        Plant plant = null;
        for (Plant p : session.getPlants()) {
            if (p.isAlive() && p.getPosition() != null
                    && (int) p.getPosition().x() == x - 1
                    && (int) p.getPosition().y() == y - 1) {
                plant = p;
                break;
            }
        }
        if (plant == null) {
            throw new GameException("no plant there.");
        } else if (plant.getPlantFoodEffect() == null) {
            throw new GameException("this plant has no plant food effect.");
        } else if (plant.isPlantFoodActive()) {
            throw new GameException("this plant's plant food effect is already active.");
        } else if (!session.spendPlantFood()) {
            throw new GameException("no plant food available.");
        } else if (!plant.activatePlant(session)) {
            session.addPlantFood();
            throw new GameException("plant food could not be used on this plant.");
        } else {
            GeneralPrinter.print("Plant food used on " + plant.getName() + ".");
        }
    }

    private void waitSeconds(int seconds) {
        long requestedTicks = (long) seconds * 10L;
        advanceTicks((int) Math.min(requestedTicks, 6000L));
    }

    private void advanceTicks(int ticks) {
        GameSession session = GameSession.getInstance();
        int safeTicks = Math.min(Math.max(ticks, 0), 6000);
        int elapsedTicks = 0;
        for (int i = 0; i < safeTicks; i++) {
            session.tick();
            elapsedTicks++;
            if (session.isGameOver() || session.isGameWon()) break;
        }
        if (session.isGameOver()) {
            finishMatch("lose");
        } else if (session.isGameWon()) {
            finishMatch("win");
        } else {
            GeneralPrinter.print("Time passes... (" + elapsedTicks + " ticks)");
        }
    }

    private boolean isAllowedWhilePaused(String text) {
        return Regex.MENU_SHOW_CURRENT.getMatcherRaw(text).matches()
                || Regex.SHOW_GARDEN.getMatcherRaw(text).matches()
                || Regex.SHOW_MAP.getMatcherRaw(text).matches()
                || Regex.SHOW_PLANT_STATUS.getMatcherRaw(text).matches()
                || Regex.SHOW_SUN_AMOUNT.getMatcherRaw(text).matches()
                || Regex.SHOW_PLANT_FOOD_AMOUNT.getMatcherRaw(text).matches()
                || Regex.SHOW_TILE.getMatcherRaw(text).matches()
                || Regex.SHOW_TILE_STATUS.getMatcherRaw(text).matches()
                || Regex.ZOMBIES_INFO.getMatcherRaw(text).matches()
                || Regex.MENU_EXIT.getMatcherRaw(text).matches()
                || text.trim().equalsIgnoreCase("restart")
                || text.trim().toLowerCase().startsWith("end game");
    }

    private void spawnZombie(String alias, int x, int y) {
        GameSession session = GameSession.getInstance();
        int row = y - 1;
        int col = x - 1;
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) {
            throw new GameException("zombie position is out of bounds.");
        }
        try {
            Zombie zombie = ZombieFactory.create(alias, row, col);
            session.spawnZombie(zombie);
            GeneralPrinter.print(zombie.getName() + " spawned at (" + x + "," + y + ").");
        } catch (IllegalArgumentException e) {
            throw new GameException("no such zombie.");
        }
    }

    private void restartMatch() {
        GameSession session = GameSession.getInstance();
        if (session.getLevel() == null) {
            throw new GameException("no active match.");

        }
        try {
            var matchBoosts = session.getMatchBoostedPlantIds();
            var freshLevel = LevelLoader.loadLevelById(session.getLevel().getId());
            MatchMenu.selectedLevel = freshLevel;
            GameSession fresh = new GameSession(freshLevel.getRows(), freshLevel.getCols());
            fresh.setDifficultyLevel(User.currentUser.userState.difficultyLevel);
            fresh.setLevel(freshLevel);
            fresh.restoreMatchBoosts(matchBoosts);
            if (!(freshLevel instanceof PlantWhatYouGetLevel)) fresh.startWaves();
            GeneralPrinter.print("Match restarted.");
        } catch (Exception e) {
            throw new GameException("could not restart the level.");
        }
    }

    private void requestEndGame(String text) {
        GameSession session = GameSession.getInstance();
        String result = text.replaceFirst("(?i)^\\s*end\\s+game", "")
                .replaceFirst("(?i)^\\s*-r\\s*", "").trim();
        if (result.equalsIgnoreCase("win") && !session.isGameWon()) {
            throw new GameException("the match has not been won yet.");
        }
        finishMatch(result.equalsIgnoreCase("win") ? "win" : "lose");
    }

    private void finishMatch(String result) {
        boolean won = result.equalsIgnoreCase("win");
        if (!won) controller.QuestManager.notifyLevelLost();
        AfterMenu.reset(won);
        App.currentMenu = new AfterMenu();
    }

    private void startZombieWaves() {
        GameSession session = GameSession.getInstance();
        if (!(session.getLevel() instanceof PlantWhatYouGetLevel)) {
            throw new GameException("this command is only used in Plant What You Get stages.");
        }
        if (session.isWavesStarted()) {
            throw new GameException("zombie waves have already started.");
        }
        session.startWaves();
        GeneralPrinter.print("Zombie waves started.");
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new GameMenu();
    }

    @Override
    public String showMenu() {
        var level = GameSession.getInstance().getLevel();
        String stageDetails = level == null ? ""
                : "\nStage: " + level.getName() + " | Game mode: " + level.getGameMode();
        String conveyorOffer = "";
        if (GameSession.getInstance().getLevel() instanceof ConveyorBeltLevel conveyor) {
            conveyorOffer = " | conveyor: "
                    + (conveyor.getCurrentPlant() == null ? "waiting" : conveyor.getCurrentPlant().getName());
        }
        String startWaves = GameSession.getInstance().getLevel() instanceof PlantWhatYouGetLevel
                && !GameSession.getInstance().isWavesStarted()
                ? "\n  start zombie waves" : "";
        return (paused ? "[ Match Paused ]" : "[ Match in Progress ]")
                + stageDetails + conveyorOffer + "\nCommands:\n"
                + "  plant plant -t <type> -l (<x>, <y>)\n"
                + "  pluck plant -l (<x>, <y>) | dig plant at (<x>, <y>)\n"
                + "  collect (<x>, <y>) | collect sun -l (<x>, <y>)\n"
                + "  feed plant -l (<x>, <y>)\n"
                + "  show map | show sun amount | show plant-food amount\n"
                + "  show plant status | show tile status -l (<x>, <y>) | zombies info\n"
                + "  advance time -t <count> ticks | wait <seconds>"
                + startWaves + "\n"
                + "  cheat add -n <count> suns | cheat add-plant-food | cheat remove-cooldown\n"
                + "  cheat spawn-zombie -t <type> -l (<x>, <y>) | release the nuke\n"
                + "  pause | resume | restart | end game -r <win/lose>\n"
                + "  menu exit | menu show current";
    }
}
