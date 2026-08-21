package controller.mini_games;

import controller.menus.Menu;
import controller.menus.TravelLogMenu;
import model.App;
import model.Regex;
import model.collections.plant.Plant;
import model.game_exceptions.GameException;
import model.match.mini_games.Zombotany;
import model.user_data.User;
import service.GameClock;
import view.GeneralPrinter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZombotanyController extends Menu {

    private static final Pattern COIN_COLLECTION = Pattern.compile(
            "^\\s*collect\\s+coin\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SUN_CHEAT = Pattern.compile(
            "^\\s*cheat\\s+(?:sun\\s+|add\\s+-n\\s+)(?<count>\\d+)\\s*(?:suns?)?\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RESTART = Pattern.compile(
            "^\\s*restart\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern END_GAME = Pattern.compile(
            "^\\s*end\\s+game(?:\\s+-r\\s+(?<result>win|lose))?\\s*$", Pattern.CASE_INSENSITIVE);

    private Zombotany game;

    public ZombotanyController(Zombotany game) {
        this.game = game;
    }

    public Zombotany getGame() {
        return game;
    }

    public void tick(double deltaSeconds) {
        game.tick(deltaSeconds);
        reportOutcome();
    }

    public void restartGame() {
        game = new Zombotany(game.getDifficulty());
        GeneralPrinter.print("Zombotany level " + game.getDifficulty() + " restarted.");
    }

    @Override
    public String getName() {
        return "Zombotany Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.PLANT_AT.getMatcherRaw(text).matches()
                || Regex.PLANT_ON_FIELD.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.PLANT_AT.getMatcherRaw(text).matches()
                    ? Regex.PLANT_AT.getMatcherRaw(text)
                    : Regex.PLANT_ON_FIELD.getMatcherRaw(text);
            matcher.matches();
            game.plantAt(matcher.group("type"), Integer.parseInt(matcher.group("y")) - 1,
                    Integer.parseInt(matcher.group("x")) - 1);
        } else if (Regex.DIG_PLANT_AT.getMatcherRaw(text).matches()
                || Regex.REMOVE_PLANT_AT.getMatcherRaw(text).matches()
                || Regex.PLUCK_PLANT_FIELD.getMatcherRaw(text).matches()) {
            handleDig(text);
        } else if (Regex.FEED_PLANT_FIELD.getMatcherRaw(text).matches()
                || Regex.USE_PLANT_FOOD.getMatcherRaw(text).matches()) {
            handleFeed(text);
        } else if (Regex.COLLECT_ITEM.getMatcherRaw(text).matches()
                || Regex.COLLECT_SUN.getMatcherRaw(text).matches()
                || COIN_COLLECTION.matcher(text).matches()) {
            handleCollect(text);
        } else if (Regex.CHEAT_ADD_SUNS.getMatcherRaw(text).matches()
                || SUN_CHEAT.matcher(text).matches()) {
            handleSunCheat(text);
        } else if (Regex.CHEAT_ADD_PLANT_FOOD.getMatcherRaw(text).matches()) {
            game.addPlantFoodCheat();
        } else if (Regex.CHEAT_REMOVE_COOLDOWN.getMatcherRaw(text).matches()) {
            game.getSession().removeAllCooldowns();
            GeneralPrinter.print("All seed packets are ready again.");
        } else if (Regex.CHEAT_SPAWN_ZOMBIE.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.CHEAT_SPAWN_ZOMBIE.getMatcherRaw(text);
            matcher.matches();
            game.spawnZombie(matcher.group("type"), Integer.parseInt(matcher.group("y")) - 1);
        } else if (Regex.RELEASE_THE_NUKE.getMatcherRaw(text).matches()) {
            game.getSession().killAllZombies();
            GeneralPrinter.print("Every zombie on the lawn was wiped out.");
        } else if (Regex.START_ZOMBIE_WAVES.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Zombotany waves are already rolling.");
        } else if (RESTART.matcher(text).matches()) {
            restartGame();
            return;
        } else if (END_GAME.matcher(text).matches()) {
            handleEndGame(text);
            return;
        } else if (Regex.MINIGAME_ADVANCE_TIME.getMatcherRaw(text).matches()) {
            advanceTime(text);
        } else if (Regex.SHOW_MAP.getMatcherRaw(text).matches()
                || text.trim().equalsIgnoreCase("show state")
                || text.trim().equalsIgnoreCase("show status")) {
            GeneralPrinter.print(game.renderState());
        } else if (text.trim().equalsIgnoreCase("show plants")
                || Regex.SHOW_PLANT_STATUS.getMatcherRaw(text).matches()
                || text.trim().equalsIgnoreCase("plants info")) {
            GeneralPrinter.print(game.renderPlantsInfo());
        } else if (text.trim().equalsIgnoreCase("show seeds")
                || text.trim().equalsIgnoreCase("show seed packets")) {
            GeneralPrinter.print("Seed packets: " + game.getAvailablePlants());
        } else if (text.trim().equalsIgnoreCase("show roster")
                || text.trim().equalsIgnoreCase("show zombie roster")) {
            GeneralPrinter.print("Zombie roster:\n  " + game.renderRoster());
        } else if (Regex.SHOW_SUN_AMOUNT.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Sun: " + game.getSession().getSunCount());
        } else if (Regex.SHOW_PLANT_FOOD_AMOUNT.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Plant food: " + game.getSession().getPlantFoodCount());
        } else if (Regex.ZOMBIES_INFO.getMatcherRaw(text).matches()
                || text.trim().equalsIgnoreCase("show zombies")) {
            GeneralPrinter.print(game.renderZombiesInfo());
        } else if (Regex.SHOW_TILE_STATUS.getMatcherRaw(text).matches()
                || Regex.SHOW_TILE.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.SHOW_TILE_STATUS.getMatcherRaw(text).matches()
                    ? Regex.SHOW_TILE_STATUS.getMatcherRaw(text)
                    : Regex.SHOW_TILE.getMatcherRaw(text);
            matcher.matches();
            int x = Integer.parseInt(matcher.group("x"));
            int y = Integer.parseInt(matcher.group("y"));
            GeneralPrinter.print(game.getSession().renderTileStatus(y - 1, x - 1));
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
            return;
        } else {
            GeneralPrinter.print("Unknown command in Zombotany.");
        }
        reportOutcome();
    }

    private void advanceTime(String text) {
        Matcher matcher = Regex.MINIGAME_ADVANCE_TIME.getMatcherRaw(text);
        matcher.matches();
        int ticks = Math.min(6000, Integer.parseInt(matcher.group("ticks")));
        int elapsed = 0;
        for (; elapsed < ticks && !game.isFinished(); elapsed++) {
            game.tick(GameClock.SECONDS_PER_TICK);
        }
        GeneralPrinter.print("Time passes... (" + elapsed + " ticks).");
    }

    private void handleDig(String text) {
        Matcher matcher = Regex.DIG_PLANT_AT.getMatcherRaw(text);
        if (!matcher.matches()) {
            matcher = Regex.REMOVE_PLANT_AT.getMatcherRaw(text);
            if (!matcher.matches()) matcher = Regex.PLUCK_PLANT_FIELD.getMatcherRaw(text);
        }
        matcher.matches();
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        Plant dug = game.digPlantAt(y - 1, x - 1);
        if (dug == null) throw new GameException("there is no plant to dig up there.");
        GeneralPrinter.print("Dug up " + dug.getName() + " at (" + x + ", " + y + ").");
    }

    private void handleFeed(String text) {
        Matcher matcher = Regex.FEED_PLANT_FIELD.getMatcherRaw(text);
        if (!matcher.matches()) matcher = Regex.USE_PLANT_FOOD.getMatcherRaw(text);
        matcher.matches();
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        game.feedPlantAt(y - 1, x - 1);
    }

    private void handleCollect(String text) {
        Matcher matcher;
        if (Regex.COLLECT_ITEM.getMatcherRaw(text).matches()) {
            matcher = Regex.COLLECT_ITEM.getMatcherRaw(text);
        } else if (Regex.COLLECT_SUN.getMatcherRaw(text).matches()) {
            matcher = Regex.COLLECT_SUN.getMatcherRaw(text);
        } else {
            matcher = COIN_COLLECTION.matcher(text);
        }
        matcher.matches();
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        List<?> collected = game.collectItemsAt(x - 1, y - 1);
        GeneralPrinter.print(collected.isEmpty()
                ? "Nothing to collect there."
                : "Collected " + collected.size() + " item(s).");
    }

    private void handleSunCheat(String text) {
        Matcher matcher = Regex.CHEAT_ADD_SUNS.getMatcherRaw(text).matches()
                ? Regex.CHEAT_ADD_SUNS.getMatcherRaw(text)
                : SUN_CHEAT.matcher(text);
        matcher.matches();
        game.addSunCheat(Integer.parseInt(matcher.group("count")));
    }

    private void handleEndGame(String text) {
        Matcher matcher = END_GAME.matcher(text);
        matcher.matches();
        String result = matcher.group("result");
        boolean winner = game.isWon() || "win".equalsIgnoreCase(result);
        finish(winner);
    }

    private void reportOutcome() {
        if (App.currentMenu != this) return;
        if (game.isWon()) {
            finish(true);
        } else if (game.isLost()) {
            finish(false);
        }
    }

    private void finish(boolean winner) {
        int difficulty = game.getDifficulty();
        Runnable restart = () ->
                App.currentMenu = new ZombotanyController(new Zombotany(difficulty));

        if (winner) {
            if (User.currentUser != null && User.currentUser.userState != null) {
                User.currentUser.userState.miniGamesWon++;
            }
            GeneralPrinter.print("All Zombotany waves cleared. You win!");
            App.currentMenu = new MiniGameEndMenu("Zombotany", true, summary(), restart);
        } else {
            GeneralPrinter.print("The plant zombies got through. You lose!");
            App.currentMenu = new MiniGameEndMenu("Zombotany", false, summary(), restart);
        }
    }

    private String summary() {
        return "Waves survived: " + game.getWavesSurvived() + "/" + game.getTotalWaves()
                + "   |   Plant zombies destroyed: " + game.getZombiesKilled()
                + "   |   Plants lost: " + game.getPlantsLost()
                + "\nTime on the lawn: " + String.format("%.0f", game.getElapsedSeconds()) + "s";
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new TravelLogMenu();
    }

    @Override
    public String showMenu() {
        return "[ Zombotany Menu ]\n" + game.getStageDetails()
                + " | Sun: " + game.getSession().getSunCount()
                + " | Waves: " + game.getWavesSurvived() + "/" + game.getTotalWaves()
                + "\nSeed packets: " + game.getAvailablePlants()
                + "\nZombie roster:\n  " + game.renderRoster() + "\nCommands:\n"
                + "  plant plant -t <type> -l (<x>, <y>)\n"
                + "  dig plant at (<x>, <y>) | feed plant -l (<x>, <y>)\n"
                + "  show map | show state | show plants | show seeds | show roster\n"
                + "  show zombies | zombies info | show tile status -l (x,y)\n"
                + "  show sun amount | show plant food amount\n"
                + "  collect (x,y) | collect sun -l (x,y) | collect coin -l (x,y)\n"
                + "  cheat add -n <count> suns | cheat add-plant-food | cheat remove-cooldown\n"
                + "  cheat spawn-zombie -t <alias> -l (x,y)\n"
                + "  advance time -t <n> ticks | restart\n"
                + "  menu exit | menu show current";
    }
}
