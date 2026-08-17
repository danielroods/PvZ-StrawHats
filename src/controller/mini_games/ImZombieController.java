package controller.mini_games;

import controller.menus.Menu;
import controller.menus.TravelLogMenu;
import model.App;
import model.Regex;
import model.match.mini_games.izombie.IZombie;
import model.user_data.User;
import view.GeneralPrinter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImZombieController extends Menu {
    private static final Pattern PLACE_ZOMBIE_AT = Pattern.compile(
            "^\\s*place\\s+zombie\\s+-t\\s+(?<type>\\S+)\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACE_ZOMBIE_ROW = Pattern.compile(
            "^\\s*place\\s+zombie\\s+-t\\s+(?<type>\\S+)\\s+-r\\s+(?<row>\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COIN_COLLECTION = Pattern.compile(
            "^\\s*collect\\s+coin\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SUN_CHEAT = Pattern.compile(
            "^\\s*cheat\\s+(?:sun\\s+|add\\s+-n\\s+)(?<count>\\d+)\\s*(?:suns?)?\\s*$",
            Pattern.CASE_INSENSITIVE);
    private final IZombie game;

    public ImZombieController(IZombie game) {
        this.game = game;
    }

    public IZombie getGame() { return game; }

    @Override
    public String getName() {
        return "I, Zombie Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (PLACE_ZOMBIE_AT.matcher(text).matches()
                || Regex.IZOMBIE_PLACE_ZOMBIE.getMatcherRaw(text).matches()) {
            handlePlace(text);
        } else if (Regex.COLLECT_ITEM.getMatcherRaw(text).matches()
                || COIN_COLLECTION.matcher(text).matches()) {
            handleCollect(text);
        } else if (Regex.COLLECT_SUN.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Sun Producer Zombies generate sun automatically. Current sun: "
                    + game.getSession().getSunCount() + ".");
        } else if (Regex.CHEAT_ADD_SUNS.getMatcherRaw(text).matches()
                || SUN_CHEAT.matcher(text).matches()) {
            handleSunCheat(text);
        } else if (Regex.MINIGAME_ADVANCE_TIME.getMatcherRaw(text).matches()) {
            advanceTime(text);
        } else if (text.trim().equalsIgnoreCase("show map")
                || text.trim().equalsIgnoreCase("show state")
                || text.trim().equalsIgnoreCase("show status")) {
            GeneralPrinter.print(game.renderState());
        } else if (text.trim().equalsIgnoreCase("show available zombies")) {
            GeneralPrinter.print("Available zombies and costs: " + game.getRoster());
        } else if (text.trim().equalsIgnoreCase("show plants")
                || text.trim().equalsIgnoreCase("plants info")) {
            GeneralPrinter.print(game.renderDefendingPlants());
        } else if (text.trim().equalsIgnoreCase("show zombies")
                || Regex.ZOMBIES_INFO.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Available zombies and costs: " + game.getRoster()
                    + "\nOn-field zombies:\n" + game.renderZombiesInfo());
        } else if (Regex.SHOW_TILE_STATUS.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.SHOW_TILE_STATUS.getMatcherRaw(text);
            matcher.matches();
            int x = Integer.parseInt(matcher.group("x"));
            int y = Integer.parseInt(matcher.group("y"));
            GeneralPrinter.print(game.renderPlantAt(y - 1, x - 1));
        } else if (Regex.SHOW_SUN_AMOUNT.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Sun: " + game.getSession().getSunCount());
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
            return;
        } else {
            GeneralPrinter.print("Unknown command in I, Zombie.");
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

    private void handlePlace(String text) {
        if (PLACE_ZOMBIE_AT.matcher(text).matches()) {
            Matcher matcher = PLACE_ZOMBIE_AT.matcher(text);
            matcher.matches();
            String alias = matcher.group("type");
            int x = Integer.parseInt(matcher.group("x"));
            int y = Integer.parseInt(matcher.group("y"));
            if (!game.placeZombie(alias, y - 1, x - 1)) {
                GeneralPrinter.print("Can't place " + alias + " there. Zombies must be "
                        + "to the right of the red line and cost available sun.");
            }
            return;
        }

        Matcher matcher = PLACE_ZOMBIE_ROW.matcher(text);
        matcher.matches();
        String alias = matcher.group("type");
        int row = Integer.parseInt(matcher.group("row")) - 1;
        if (!game.placeZombie(alias, row)) {
            GeneralPrinter.print("Can't place " + alias + " right now (not enough sun, "
                    + "not one of this level's five zombies, or invalid lane).");
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

    private void reportOutcome() {
        if (game.isWon()) {
            if (User.currentUser != null) User.currentUser.userState.miniGamesWon++;
            GeneralPrinter.print("Every brain has been eaten. You win!");
            App.currentMenu = new MiniGameEndMenu("I, Zombie", true,
                    "All five brains were eaten.");
        } else if (game.isLost()) {
            GeneralPrinter.print("Out of sun and out of playable zombies. You lose!");
            App.currentMenu = new MiniGameEndMenu("I, Zombie", false,
                    "There was no affordable playable zombie left.");
        }
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new TravelLogMenu();
    }

    @Override
    public String showMenu() {
        return "[ I, Zombie Menu ]\n" + game.getStageDetails()
                + " | Red line after column " + (game.getRedLineColumn() + 1)
                + "\nAvailable zombies and costs: " + game.getRoster() + "\nCommands:\n"
                + "  place zombie -t <alias> -r <row>\n"
                + "  place zombie -t <alias> -l (<x>, <y>)\n"
                + "  show available zombies | show plants | show map | show state\n"
                + "  show tile status -l (x,y) | show sun amount | collect (x,y) | collect coin -l (x,y)\n"
                + "  collect sun -l (x,y) | zombies info\n"
                + "  cheat add -n <count> suns | cheat sun <count>\n"
                + "  advance time -t <n> ticks\n"
                + "  menu exit | menu show current";
    }
}