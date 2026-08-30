package controller.match;

import controller.CollectionManager;
import controller.ui_menus.network.NetworkMenu;
import model.App;
import model.game_exceptions.GameException;
import model.match.main.levels.normal_levels.NormalLevel;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.utils.GameSession;
import net.client.NetMatchState;
import net.client.NetworkClient;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetBeforeMenu extends BeforeMenu {

    public static final int ZOMBIE_SLOTS = CoopBeforeMenu.ZOMBIE_SLOTS;

    private static final Pattern ADD_ZOMBIE = Pattern.compile(
            "^\\s*add\\s+zombie\\s+-t\\s+(?<type>\\S+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMOVE_ZOMBIE = Pattern.compile(
            "^\\s*remove\\s+zombie\\s+-t\\s+(?<type>\\S+)\\s*$", Pattern.CASE_INSENSITIVE);

    private final CollectionManager manager = new CollectionManager();
    private final NetMatchState state;

    public NetBeforeMenu(NetMatchState state) {
        this.state = state;
    }

    public static NetBeforeMenu open(NetMatchState state) {
        NormalLevel level = new NormalLevel();
        level.setName("Online I, Zombie");
        level.setZombiePool(new ArrayList<>());

        GameSession lobbySession = new GameSession();
        lobbySession.setLevel(level);

        BeforeMenu.selectedPlants.clear();
        BeforeMenu.selectedZombies.clear();
        return new NetBeforeMenu(state);
    }

    public NetMatchState getState() {
        return state;
    }

    public boolean isPlantSide() {
        return state == null || state.getRole() != Role.ZOMBIES;
    }

    public boolean isReadySent() {
        return state != null && state.isMyReady();
    }

    public List<String> myLoadout() {
        return isPlantSide() ? BeforeMenu.selectedPlants : BeforeMenu.selectedZombies;
    }

    @Override
    public String getName() {
        return "Online Loadout";
    }

    @Override
    protected boolean allCollectionsUnlocked() {
        return true;
    }

    @Override
    public void handleCommand(String text) {
        if (text == null) {
            super.handleCommand(text);
            return;
        }

        Matcher addMatcher = ADD_ZOMBIE.matcher(text);
        Matcher removeMatcher = REMOVE_ZOMBIE.matcher(text);

        if (addMatcher.matches()) {
            addZombie(addMatcher.group("type"));
        } else if (removeMatcher.matches()) {
            removeZombie(removeMatcher.group("type"));
        } else if (text.trim().equalsIgnoreCase("ready")
                || text.trim().equalsIgnoreCase("start game")) {
            sendReady();
        } else {
            super.handleCommand(text);
        }
    }

    private void addZombie(String alias) {
        requireZombieSide();
        boolean known = manager.getAllZombieAliases().stream()
                .anyMatch(a -> a.equalsIgnoreCase(alias));
        if (!known) {
            throw new GameException("zombie not available.");
        } else if (selectedZombies.stream().anyMatch(z -> z.equalsIgnoreCase(alias))) {
            throw new GameException("zombie already selected.");
        } else if (selectedZombies.size() >= ZOMBIE_SLOTS) {
            throw new GameException("no free zombie slots.");
        } else {
            selectedZombies.add(alias);
            GeneralPrinter.print(alias + " added to zombie loadout.");
        }
    }

    private void removeZombie(String alias) {
        requireZombieSide();
        if (selectedZombies.removeIf(z -> z.equalsIgnoreCase(alias))) {
            GeneralPrinter.print(alias + " removed from zombie loadout.");
        } else {
            throw new GameException("zombie not in loadout.");
        }
    }

    private void requireZombieSide() {
        if (isPlantSide()) {
            throw new GameException("you are playing the plants this match.");
        }
    }

    public void sendReady() {
        if (state == null) {
            throw new GameException("that match is over.");
        }
        if (state.isMyReady()) {
            throw new GameException("you are already ready.");
        }
        List<String> picks = myLoadout();
        if (picks.isEmpty()) {
            throw new GameException(isPlantSide()
                    ? "select at least one plant before you are ready."
                    : "select at least one zombie before you are ready.");
        }
        state.setLoadout(picks);
        NetworkClient.get().sendMatchReady(picks);
        GeneralPrinter.print("Ready - waiting for your opponent.");
    }

    @Override
    public void exitMenu() {
        selectedPlants.clear();
        selectedZombies.clear();
        NetworkClient.get().leaveMatch();
        NetworkClient.get().clearMatchState();
        App.currentMenu = new NetworkMenu();
    }

    @Override
    public String showMenu() {
        String side = isPlantSide() ? "PLANTS" : "ZOMBIES";
        String picks = isPlantSide()
                ? selectedPlants + " (" + selectedPlants.size() + "/8)"
                : selectedZombies + " (" + selectedZombies.size() + "/" + ZOMBIE_SLOTS + ")";
        return "[ Online Loadout ]\n"
                + "You are the " + side
                + " | Opponent: " + (state == null ? "?" : state.getOpponentNickname()) + "\n"
                + "Selected: " + picks + "\n"
                + (state != null && state.isMyReady() ? "You are READY.\n" : "")
                + (state != null && state.isOpponentReady()
                        ? "Your opponent is READY.\n" : "Your opponent is still picking.\n")
                + "Commands:\n"
                + (isPlantSide()
                        ? "  show all plants | add plant -t <type> | remove plant -t <type>\n"
                        : "  add zombie -t <alias> | remove zombie -t <alias>\n")
                + "  ready\n"
                + "  menu exit | menu show current";
    }
}
