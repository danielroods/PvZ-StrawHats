package controller.match.mini_games;

import controller.ui_menus.MainMenu;
import controller.ui_menus.Menu;
import model.App;
import model.Regex;
import model.match.mini_games.izombie.IZombieMatch;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.user_data.User;
import service.GameClock;
import view.GeneralPrinter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CouchIZombieController extends Menu {

    private static final Pattern PLACE_ZOMBIE = Pattern.compile(
            "^\\s*place\\s+zombie\\s+-t\\s+(?<type>\\S+)\\s+-l\\s*"
                    + "\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private IZombieMatch match = new IZombieMatch(IZombieMatch.COUCH_MATCH_SECONDS);
    private boolean outcomeReported;

    public IZombieMatch getMatch() {
        return match;
    }

    public void restart() {
        match = new IZombieMatch(IZombieMatch.COUCH_MATCH_SECONDS);
        outcomeReported = false;
    }

    public void tick(double deltaSeconds) {
        match.tick();
        reportOutcome();
    }

    public String apply(Role role, String action, String target, int row, int col) {
        return match.applyIntent(role, action, target, row, col);
    }

    @Override
    public String getName() {
        return "Couch I, Zombie";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (PLACE_ZOMBIE.matcher(text).matches()) {
            Matcher matcher = PLACE_ZOMBIE.matcher(text);
            matcher.matches();
            announce(apply(Role.ZOMBIES, "PLACE_ZOMBIE", matcher.group("type"),
                    Integer.parseInt(matcher.group("y")) - 1,
                    Integer.parseInt(matcher.group("x")) - 1));
        } else if (Regex.PLANT_ON_FIELD.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.PLANT_ON_FIELD.getMatcherRaw(text);
            matcher.matches();
            announce(apply(Role.PLANTS, "PLANT", matcher.group("type"),
                    Integer.parseInt(matcher.group("y")) - 1,
                    Integer.parseInt(matcher.group("x")) - 1));
        } else if (Regex.COLLECT_ITEM.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.COLLECT_ITEM.getMatcherRaw(text);
            matcher.matches();
            announce(apply(Role.PLANTS, "COLLECT_SUN", null,
                    Integer.parseInt(matcher.group("y")) - 1,
                    Integer.parseInt(matcher.group("x")) - 1));
        } else if (Regex.MINIGAME_ADVANCE_TIME.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.MINIGAME_ADVANCE_TIME.getMatcherRaw(text);
            matcher.matches();
            int ticks = Math.min(6000, Integer.parseInt(matcher.group("ticks")));
            for (int i = 0; i < ticks && !match.isFinished(); i++) {
                match.tick();
            }
            reportOutcome();
        } else if (text.trim().equalsIgnoreCase("restart")) {
            restart();
            GeneralPrinter.print("Couch I, Zombie restarted.");
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Unknown command in Couch I, Zombie.");
        }
    }

    private void announce(String rejection) {
        if (rejection != null) GeneralPrinter.print(rejection);
    }

    private void reportOutcome() {
        if (!match.isFinished() || outcomeReported) return;
        if (App.currentMenu != this) return;
        outcomeReported = true;

        Role winner = match.getWinner();
        boolean plantsWon = winner == Role.PLANTS;
        if (User.currentUser != null) User.currentUser.userState.miniGamesWon++;
        GeneralPrinter.print(match.getEndReason());
        App.currentMenu = new MiniGameEndMenu("Couch I, Zombie", true,
                (plantsWon ? "Player 1 (plants) wins!" : "Player 2 (zombies) wins!")
                        + "  " + match.getEndReason(),
                () -> App.currentMenu = new CouchIZombieController()) {
            @Override
            public void exitMenu() {
                App.currentMenu = new MainMenu();
            }
        };
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new MainMenu();
    }

    @Override
    public String showMenu() {
        return "[ Couch I, Zombie ]\n"
                + "Player 1 (mouse) defends the plants, Player 2 (keyboard) sends the zombies.\n"
                + "Keyboard: W/S pick a lane, A/D pick a column, 1-6 pick a zombie, "
                + "SPACE to drop it.\n"
                + "Commands:\n"
                + "  plant plant -t <type> -l (<x>, <y>)\n"
                + "  place zombie -t <alias> -l (<x>, <y>)\n"
                + "  collect (<x>, <y>) | advance time -t <n> ticks | restart\n"
                + "  menu exit | menu show current";
    }
}
