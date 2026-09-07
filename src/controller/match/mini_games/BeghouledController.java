package controller.match.mini_games;

import controller.cheat.CheatAccess;
import controller.ui_menus.Menu;
import controller.ui_menus.TravelLogMenu;
import model.App;
import model.Regex;
import model.match.mini_games.Beghouled;
import model.user_data.User;

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

    public Beghouled getGame() {
        return game;
    }


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
        } else if (Regex.SHOW_TILE_STATUS.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.SHOW_TILE_STATUS.getMatcherRaw(text);
            matcher.matches();
            int x = Integer.parseInt(matcher.group("x"));
            int y = Integer.parseInt(matcher.group("y"));

        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
            return;
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

    }

    private void handleSwap(String text) {
        int[] coords = parseTwoCoords(text);
        boolean swapped = game.trySwap(coords[1] - 1, coords[0] - 1,
                coords[3] - 1, coords[2] - 1);

    }

    private void handleUpgrade(String text) {
        int marker = text.toLowerCase().indexOf("-t");
        String plantName = marker < 0 ? "" : text.substring(marker + 2).trim();
        game.upgrade(plantName);
    }

    private void handleCollect(String text) {
        Matcher matcher = Regex.COLLECT_ITEM.getMatcherRaw(text).matches()
                ? Regex.COLLECT_ITEM.getMatcherRaw(text)
                : COIN_COLLECTION.matcher(text);
        matcher.matches();
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        List<?> collected = game.collectItemsAt(x - 1, y - 1);


    }

    private void handleSunCheat(String text) {
        if (!CheatAccess.allow()) return;
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
        if (App.currentMenu != this) return;
        if (game.isWon()) {
            MiniGameResults.recordWin("beghouled", game.getDifficulty());


            App.currentMenu = new MiniGameEndMenu("Beghouled", true,
                    "The required number of combinations was completed.");
        } else if (game.isLost()) {

            App.currentMenu = new MiniGameEndMenu("Beghouled", false,
                    "A zombie reached the house.");
        }
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new TravelLogMenu();
    }

    @Override
    public String showMenu() {
        String cheatHelp = CheatAccess.isEnabled()
                ? "  cheat add -n <count> suns\n" : "";
        return "[ Beghouled Menu ]\n" + game.getStageDetails()
                + " | Zombie pool: " + game.getZombiePool() + "\nCommands:\n"
                + "  swap -l (x,y) -l (x,y)\n"
                + "  upgrade -t <plant>\n"
                + cheatHelp
                + "  show map | show state | show status | show plants | show sun amount | show zombies\n"
                + "  show tile status -l (x,y) | collect (x,y) | collect coin -l (x,y)\n"
                + "  advance time -t <n> ticks\n"
                + "  menu exit | menu show current";
    }
}