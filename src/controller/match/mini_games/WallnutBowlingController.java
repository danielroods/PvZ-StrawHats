package controller.match.mini_games;

import controller.ui_menus.Menu;
import controller.ui_menus.TravelLogMenu;
import model.App;
import model.Regex;
import model.match.mini_games.wallnutbowlling.WallnutBowling;
import model.user_data.User;
import view.GeneralPrinter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WallnutBowlingController extends Menu {
    private static final Pattern PLANT_NUT = Pattern.compile(
            "^\\s*plant\\s+nut(?:\\s+-t\\s+(?<kind>\\S+))?\\s+(?:-l\\s*)?\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COIN_COLLECTION = Pattern.compile(
            "^\\s*collect\\s+coin\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private final WallnutBowling game;

    public WallnutBowlingController(WallnutBowling game) {
        this.game = game;
    }

    public WallnutBowling getGame() { return game; }

    public void tick(double deltaSeconds) {
        game.tick(deltaSeconds);
        reportOutcome();
    }

    @Override
    public String getName() {
        return "Wallnut Bowling Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.WALLNUT_PLANT_NUT.getMatcherRaw(text).matches()
                || PLANT_NUT.matcher(text).matches()) {
            handlePlant(text);
        } else if (Regex.COLLECT_ITEM.getMatcherRaw(text).matches()
                || COIN_COLLECTION.matcher(text).matches()) {
            handleCollect(text);
        } else if (Regex.MINIGAME_ADVANCE_TIME.getMatcherRaw(text).matches()) {
            advanceTime(text);
        } else if (text.trim().equalsIgnoreCase("show map")
                || text.trim().equalsIgnoreCase("show state")
                || text.trim().equalsIgnoreCase("show status")) {
            GeneralPrinter.print(game.renderState());
        } else if (text.trim().equalsIgnoreCase("show conveyor")
                || text.trim().equalsIgnoreCase("show nuts")) {
            GeneralPrinter.print("Conveyor belt: " + game.getConveyorBelt()
                    + " | Next: " + game.getNextNutKind());
        } else if (Regex.ZOMBIES_INFO.getMatcherRaw(text).matches()
                || text.trim().equalsIgnoreCase("show zombies")) {
            GeneralPrinter.print(game.renderZombiesInfo());
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
            return;
        } else {
            GeneralPrinter.print("Unknown command in Wallnut Bowling.");
        }
        reportOutcome();
    }

    private void advanceTime(String text) {
        Matcher matcher = Regex.MINIGAME_ADVANCE_TIME.getMatcherRaw(text);
        matcher.matches();
        int ticks = Math.min(6000, Integer.parseInt(matcher.group("ticks")));
        int elapsed = 0;
        for (; elapsed < ticks && !game.isWon() && !game.isLost(); elapsed++) {
            game.tick(0.1);
        }
        GeneralPrinter.print("Time passes... (" + elapsed + " ticks).");
    }

    private void handlePlant(String text) {
        Matcher matcher = PLANT_NUT.matcher(text);
        matcher.matches();
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        String requestedKind = matcher.group("kind");

        boolean planted = game.plantNut(y - 1, x - 1, requestedKind);
        if (!planted && requestedKind != null
                && game.plantNut(y, x, requestedKind)) {
            planted = true;
        }
        if (!planted) {
            GeneralPrinter.print("Can't launch that nut. Check the conveyor's next nut, "
                    + "the red line (column " + (game.getRedLineColumn() + 1)
                    + "), and whether the tile is free.");
        }
    }

    private void handleCollect(String text) {
        Matcher matcher = Regex.COLLECT_ITEM.getMatcherRaw(text).matches()
                ? Regex.COLLECT_ITEM.getMatcherRaw(text)
                : COIN_COLLECTION.matcher(text);
        matcher.matches();
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        List<?> collected = game.collectItemsAt(x - 1, y - 1);
        GeneralPrinter.print(collected.isEmpty()
                ? "Nothing to collect there."
                : "Collected " + collected.size() + " item(s), including any dropped coins.");
    }

    private void reportOutcome() {
        if (App.currentMenu != this) return;
        int difficulty = game.getDifficulty();
        Runnable restart = () -> App.currentMenu =
                new WallnutBowlingController(new WallnutBowling(difficulty));

        if (game.isWon()) {
            if (User.currentUser != null) {
                User.currentUser.userState.miniGamesWon++;
                User.currentUser.userState.recordMiniGameWin("wallnutbowling", difficulty);
            }
            GeneralPrinter.print("All Wall-nut Bowling waves cleared. You win!");
            App.currentMenu = new MiniGameEndMenu("Wall-nut Bowling", true,
                    "Every zombie wave was cleared.", restart);
        } else if (game.isLost()) {
            App.currentMenu = new MiniGameEndMenu("Wall-nut Bowling", false,
                    "A zombie reached the house.", restart);
        }
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new TravelLogMenu();
    }

    @Override
    public String showMenu() {
        return "[ Wallnut Bowling Menu ]\n" + game.getStageDetails()
                + " | Red line: column " + (game.getRedLineColumn() + 1)
                + " | Zombie pool: " + game.getZombiePool() + "\nCommands:\n"
                + "  plant nut [-t <bowling/explode/big>] -l (x,y)\n"
                + "  show conveyor | show map | show state | show status\n"
                + "  show zombies | zombies info | collect (x,y) | collect coin -l (x,y)\n"
                + "  advance time -t <n> ticks\n"
                + "  menu exit | menu show current";
    }
}