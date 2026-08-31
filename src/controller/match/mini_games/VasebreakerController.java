package controller.match.mini_games;

import controller.ui_menus.Menu;
import controller.ui_menus.TravelLogMenu;
import model.App;
import model.Regex;
import model.match.mini_games.vasebreaker.Vasebreaker;
import model.user_data.User;
import view.GeneralPrinter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VasebreakerController extends Menu {
    private static final Pattern COLLECT_SEED_AT = Pattern.compile(
            "^\\s*collect\\s+seed\\s+(?:-l\\s*)?\\(\\s*\\d+\\s*,\\s*\\d+\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLANT_SEED = Pattern.compile(
            "^\\s*plant(?:\\s+seed)?\\s+-t\\s+(?<type>.+?)\\s+at\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COIN_COLLECTION = Pattern.compile(
            "^\\s*collect\\s+coin\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private final Vasebreaker game;

    public VasebreakerController(Vasebreaker game) {
        this.game = game;
    }

    public Vasebreaker getGame() { return game; }

    public void tick(double deltaSeconds) {
        game.tick(deltaSeconds);
        reportOutcome();
    }

    @Override
    public String getName() {
        return "Vasebreaker Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.VASEBREAKER_BREAK_VASE.getMatcherRaw(text).matches()) {
            int[] coords = parseCoords(text);
            if (!breakAtPlayerCoordinates(coords[0], coords[1])) {
                GeneralPrinter.print("There's no unbroken vase at (" + coords[0] + ", "
                        + coords[1] + ").");
            }
        } else if (Regex.VASEBREAKER_COLLECT_SEED.getMatcherRaw(text).matches()
                || COLLECT_SEED_AT.matcher(text).matches()) {
            int[] coords = parseCoords(text);
            if (!collectAtPlayerCoordinates(coords[0], coords[1])) {
                GeneralPrinter.print("No seed packet to collect at (" + coords[0] + ", "
                        + coords[1] + ").");
            }
        } else if (Regex.PLANT_AT.getMatcherRaw(text).matches()
                || Regex.PLANT_ON_FIELD.getMatcherRaw(text).matches()
                || PLANT_SEED.matcher(text).matches()) {
            Matcher matcher;
            if (Regex.PLANT_AT.getMatcherRaw(text).matches()) {
                matcher = Regex.PLANT_AT.getMatcherRaw(text);
            } else if (Regex.PLANT_ON_FIELD.getMatcherRaw(text).matches()) {
                matcher = Regex.PLANT_ON_FIELD.getMatcherRaw(text);
            } else {
                matcher = PLANT_SEED.matcher(text);
            }
            matcher.matches();
            int x = Integer.parseInt(matcher.group("x"));
            int y = Integer.parseInt(matcher.group("y"));
            if (!game.plantSeed(matcher.group("type").trim(), y - 1, x - 1)) {
                GeneralPrinter.print("That seed is not in your Vasebreaker inventory, "
                        + "or the tile is occupied/out of bounds.");
            } else {
                GeneralPrinter.print("Seed planted successfully.");
            }
        } else if (Regex.MINIGAME_ADVANCE_TIME.getMatcherRaw(text).matches()) {
            advanceTime(text);
        } else if (text.trim().equalsIgnoreCase("show map")
                || text.trim().equalsIgnoreCase("show state")
                || text.trim().equalsIgnoreCase("show status")) {
            GeneralPrinter.print(game.renderState());
        } else if (text.trim().equalsIgnoreCase("show seeds")) {
            GeneralPrinter.print(game.renderSeeds());
        } else if (Regex.COLLECT_ITEM.getMatcherRaw(text).matches()
                || COIN_COLLECTION.matcher(text).matches()) {
            handleCollectItem(text);
        } else if (Regex.ZOMBIES_INFO.getMatcherRaw(text).matches()
                || text.trim().equalsIgnoreCase("show zombies")) {
            GeneralPrinter.print(game.renderZombiesInfo());
        } else if (Regex.SHOW_PLANT_STATUS.getMatcherRaw(text).matches()
                || text.trim().equalsIgnoreCase("show plants")) {
            GeneralPrinter.print(game.renderPlantsInfo());
        } else if (Regex.SHOW_TILE_STATUS.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.SHOW_TILE_STATUS.getMatcherRaw(text);
            matcher.matches();
            int x = Integer.parseInt(matcher.group("x"));
            int y = Integer.parseInt(matcher.group("y"));
            GeneralPrinter.print(game.getSession().renderTileStatus(y - 1, x - 1));
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
            return;
        } else {
            GeneralPrinter.print("Unknown command in Vasebreaker.");
        }
        reportOutcome();
    }

    private void advanceTime(String text) {
        Matcher matcher = Regex.MINIGAME_ADVANCE_TIME.getMatcherRaw(text);
        matcher.matches();
        int ticks = Math.min(6000, Integer.parseInt(matcher.group("ticks")));
        for (int i = 0; i < ticks && !game.isWon() && !game.isLost(); i++) {
            game.tick(0.1);
        }
        GeneralPrinter.print("Time passes... (" + ticks + " ticks).");
    }

    private boolean breakAtPlayerCoordinates(int x, int y) {
        if (game.breakVaseAt(y - 1, x - 1)) return true;
        return game.breakVaseAt(y, x);
    }

    private boolean collectAtPlayerCoordinates(int x, int y) {
        if (game.collectSeedPacket(y - 1, x - 1)) return true;
        return game.collectSeedPacket(y, x);
    }

    private void handleCollectItem(String text) {
        Matcher matcher = Regex.COLLECT_ITEM.getMatcherRaw(text).matches()
                ? Regex.COLLECT_ITEM.getMatcherRaw(text)
                : COIN_COLLECTION.matcher(text);
        matcher.matches();
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        var collected = game.getSession().collectItemsNear(
                new model.match_mechanisms.vector.Position(x - 1, y - 1));
        GeneralPrinter.print(collected.isEmpty()
                ? "Nothing to collect there."
                : "Collected " + collected.size() + " item(s), including any coin.");
    }

    private int[] parseCoords(String text) {
        String inner = text.substring(text.indexOf('(') + 1, text.indexOf(')'));
        String[] parts = inner.split(",");
        return new int[] {
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim())
        };
    }

    private void reportOutcome() {
        if (game.isWon()) {
            if (User.currentUser != null) User.currentUser.userState.miniGamesWon++;
            GeneralPrinter.print("All vases broken and all released zombies defeated. You win!");
            App.currentMenu = new MiniGameEndMenu("Vasebreaker", true,
                    "Every vase was opened safely.");
        } else if (game.isLost()) {
            App.currentMenu = new MiniGameEndMenu("Vasebreaker", false,
                    "A zombie reached the house.");
        }
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new TravelLogMenu();
    }

    @Override
    public String showMenu() {
        return "[ Vasebreaker Menu ]\n" + game.getStageDetails()
                + " | Zombie pool: " + game.getZombiePool() + "\nCommands:\n"
                + "  break vase -l (x,y)\n"
                + "  collect seed -l (x,y)\n"
                + "  plant -t <seed> at (x,y) | plant plant -t <seed> -l (x,y)\n"
                + "  collect (x,y) | collect coin -l (x,y)\n"
                + "  show seeds | show map | show state | show status\n"
                + "  show plants | zombies info | show tile status -l (x,y)\n"
                + "  advance time -t <n> ticks\n"
                + "  menu exit | menu show current";
    }
}