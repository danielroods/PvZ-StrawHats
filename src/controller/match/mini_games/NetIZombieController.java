package controller.match.mini_games;

import controller.ui_menus.Menu;
import controller.ui_menus.network.NetworkMenu;
import model.App;
import model.Regex;
import net.Protocol;
import net.client.NetMatchState;
import net.client.NetworkClient;
import view.GeneralPrinter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetIZombieController extends Menu {

    private static final Pattern PLACE_ZOMBIE = Pattern.compile(
            "^\\s*place\\s+zombie\\s+-t\\s+(?<type>\\S+)\\s+-l\\s*"
                    + "\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REACT = Pattern.compile(
            "^\\s*react\\s+-k\\s+(?<kind>text|emoji|sticker)\\s+-i\\s+(?<index>[0-2])\\s*$",
            Pattern.CASE_INSENSITIVE);

    private final NetMatchState state;

    public NetIZombieController(NetMatchState state) {
        this.state = state;
    }

    public NetMatchState getState() {
        return state;
    }

    @Override
    public String getName() {
        return "Online I, Zombie";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (PLACE_ZOMBIE.matcher(text).matches()) {
            Matcher matcher = PLACE_ZOMBIE.matcher(text);
            matcher.matches();
            NetworkClient.get().sendIntent(Protocol.INTENT_PLACE_ZOMBIE,
                    matcher.group("type"),
                    Integer.parseInt(matcher.group("y")) - 1,
                    Integer.parseInt(matcher.group("x")) - 1);
        } else if (Regex.PLANT_ON_FIELD.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.PLANT_ON_FIELD.getMatcherRaw(text);
            matcher.matches();
            NetworkClient.get().sendIntent(Protocol.INTENT_PLANT,
                    matcher.group("type"),
                    Integer.parseInt(matcher.group("y")) - 1,
                    Integer.parseInt(matcher.group("x")) - 1);
        } else if (Regex.PLUCK_PLANT_FIELD.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.PLUCK_PLANT_FIELD.getMatcherRaw(text);
            matcher.matches();
            NetworkClient.get().sendIntent(Protocol.INTENT_DIG, null,
                    Integer.parseInt(matcher.group("y")) - 1,
                    Integer.parseInt(matcher.group("x")) - 1);
        } else if (Regex.COLLECT_ITEM.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.COLLECT_ITEM.getMatcherRaw(text);
            matcher.matches();
            NetworkClient.get().sendIntent(Protocol.INTENT_COLLECT_SUN, null,
                    Integer.parseInt(matcher.group("y")) - 1,
                    Integer.parseInt(matcher.group("x")) - 1);
        } else if (Regex.FEED_PLANT_FIELD.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.FEED_PLANT_FIELD.getMatcherRaw(text);
            matcher.matches();
            NetworkClient.get().sendIntent(Protocol.INTENT_USE_PLANT_FOOD, null,
                    Integer.parseInt(matcher.group("y")) - 1,
                    Integer.parseInt(matcher.group("x")) - 1);
        } else if (REACT.matcher(text).matches()) {
            Matcher matcher = REACT.matcher(text);
            matcher.matches();
            NetworkClient.get().sendReaction(matcher.group("kind").toUpperCase(),
                    Integer.parseInt(matcher.group("index")));
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Unknown command in the online match.");
        }
    }

    @Override
    public void exitMenu() {
        NetworkClient.get().leaveMatch();
        NetworkClient.get().clearMatchState();
        App.currentMenu = new NetworkMenu();
    }

    @Override
    public String showMenu() {
        String role = state == null ? "?" : state.getRole().name();
        return "[ Online I, Zombie ]\nYou are the " + role
                + " | Opponent: " + (state == null ? "?" : state.getOpponentNickname())
                + "\nCommands:\n"
                + "  place zombie -t <alias> -l (<x>, <y>)\n"
                + "  plant plant -t <type> -l (<x>, <y>)\n"
                + "  pluck plant -l (<x>, <y>) | collect (<x>, <y>)\n"
                + "  feed plant -l (<x>, <y>)\n"
                + "  react -k <text|emoji|sticker> -i <0-2>\n"
                + "  menu exit";
    }
}
