package controller.match.mini_games;

import controller.ui_menus.Menu;
import controller.ui_menus.TravelLogMenu;
import model.App;
import model.Regex;
import model.match.mini_games.Beghouled;
import model.user_data.User;
import view.GeneralPrinter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BeghouledController extends Menu {
    private static final Pattern SWAP = Pattern.compile(
            "^\\s*swap\\s+-l\\s*\\(\\s*\\d+\\s*,\\s*\\d+\\s*\\)\\s+-l\\s*\\(\\s*\\d+\\s*,\\s*\\d+\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COIN_COLLECTION = Pattern.compile(
            "^\\s*collect\\s+coin\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SUN_CHEAT = Pattern.compile(
            "^\\s*cheat\\s+(?:sun\\s+|add\\s+-n\\s+)(?<count>\\d+)\\s*(?:suns?)?\\s*$",
            Pattern.CASE_INSENSITIVE);
    private final Beghouled game;

    public BeghouledController(Beghouled game) {
        this.game = game;
    }

    /** The underlying model, for the graphical screen to read board/sun/upgrade state from. */
    public Beghouled getGame() {
        return game;
    }

    /**
     * Advances the simulation by deltaSeconds and runs the same win/loss check every
     * text command already goes through (see reportOutcome()). Called once per fixed
     * tick by BeghouledGameScreen instead of ticking the raw GameSession directly, so
     * this stays the single place that decides when the match is over.
     */
    public void tick(double deltaSeconds) {
        game.tick(deltaSeconds);
        reportOutcome();
    }

    @Override
    public String getName() {
        return "Beghouled Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.BEGHOULED_SWAP.getMatcherRaw(text).matches()
                || SWAP.matcher(text).matches()) {
            handleSwap(text);
        } else if (Regex.BEGHOULED_UPGRADE.getMatcherRaw(text).matches()) {
            handleUpgrade(text);
        } else if (Regex.COLLECT_ITEM.getMatcherRaw(text).matches()
                || COIN_COLLECTION.matcher(text).matches()) {
            handleCollect(text);
        } else if (Regex.CHEAT_ADD_SUNS.getMatcherRaw(text).matches()
                || SUN_CHEAT.matcher(text).matches()) {
            handleSunCheat(text);
        } else if (Regex.MINIGAME_ADVANCE_TIME.getMatcherRaw(text).matches()) {
            advanceTime(text);
        } else if (text.trim().equalsIgnoreCase("show map")
                || text.trim().equalsIgnoreCase("show state")
                || text.trim().equalsIgnoreCase("show status")) {
            GeneralPrinter.print(game.renderState());
        } else if (text.trim().equalsIgnoreCase("show plants")) {
            GeneralPrinter.print(game.renderPlantsInfo());
        } else if (Regex.SHOW_SUN_AMOUNT.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Sun: " + game.getSession().getSunCount());
        } else if (text.trim().equalsIgnoreCase("show zombies")
                || Regex.ZOMBIES_INFO.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(game.renderZombiesInfo());
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
            GeneralPrinter.print("Unknown command in Beghouled.");
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

    private void handleSwap(String text) {
        int[] coords = parseTwoCoords(text);
        boolean swapped = game.trySwap(coords[1] - 1, coords[0] - 1,
                coords[3] - 1, coords[2] - 1);
        if (!swapped) {
            GeneralPrinter.print("That swap is invalid or doesn't create a match; "
                    + "the board was left unchanged.");
        }
    }

    private void handleUpgrade(String text) {
        int marker = text.toLowerCase().indexOf("-t");
        String plantName = marker < 0 ? "" : text.substring(marker + 2).trim();
        if (!game.upgrade(plantName)) {
            GeneralPrinter.print("Can't upgrade " + plantName
                    + " (not enough sun, no matching plant, or no such upgrade).");
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
                : "Collected " + collected.size() + " item(s).");
    }

    private void handleSunCheat(String text) {
        Matcher matcher = Regex.CHEAT_ADD_SUNS.getMatcherRaw(text).matches()
                ? Regex.CHEAT_ADD_SUNS.getMatcherRaw(text)
                : SUN_CHEAT.matcher(text);
        matcher.matches();
        game.addSunCheat(Integer.parseInt(matcher.group("count")));
    }

    private int[] parseTwoCoords(String text) {
        String[] tokens = text.replace("(", "")
                .replace(")", "")
                .replace(",", " ")
                .split("\\s+");
        return new int[] {
                Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]),
                Integer.parseInt(tokens[5]), Integer.parseInt(tokens[6])
        };
    }

    private void reportOutcome() {
        if (game.isWon()) {
            if (User.currentUser != null) User.currentUser.userState.miniGamesWon++;
            GeneralPrinter.print("Reached " + game.getMatchesNeeded()
                    + " matches — every zombie is cleared. You win!");
            App.currentMenu = new MiniGameEndMenu("Beghouled", true,
                    "The required number of combinations was completed.");
        } else if (game.isLost()) {
            GeneralPrinter.print("The zombies got through. You lose!");
            App.currentMenu = new MiniGameEndMenu("Beghouled", false,
                    "A zombie reached the house.");
        } else {
            GeneralPrinter.print("Matches: " + game.getMatchesMade() + "/"
                    + game.getMatchesNeeded());
        }
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new TravelLogMenu();
    }

    @Override
    public String showMenu() {
        return "[ Beghouled Menu ]\n" + game.getStageDetails()
                + " | Zombie pool: " + game.getZombiePool() + "\nCommands:\n"
                + "  swap -l (x,y) -l (x,y)\n"
                + "  upgrade -t <plant> | cheat add -n <count> suns\n"
                + "  show map | show state | show status | show plants | show sun amount | show zombies\n"
                + "  show tile status -l (x,y) | collect (x,y) | collect coin -l (x,y)\n"
                + "  advance time -t <n> ticks\n"
                + "  menu exit | menu show current";
    }
}