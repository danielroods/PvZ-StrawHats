package controller.match;

import controller.CollectionManager;
import controller.match.mini_games.CouchIZombieController;
import controller.ui_menus.MainMenu;
import model.App;
import model.game_exceptions.GameException;
import model.match.mini_games.izombie.IZombieMatch;
import view.GeneralPrinter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Before-match loadout menu used for co-op. Plant commands (add/remove/boost/
 * upgrade plant, show plants...) are all inherited unchanged from {@link BeforeMenu} -
 * co-op does not touch that mechanism. The only addition is a mirrored roster
 * command pair for the human zombie player ("add zombie -t X" / "remove zombie -t X"),
 * backed by {@link BeforeMenu#selectedZombies} instead of a level-supplied zombie pool.
 * "start game" is overridden: instead of starting the normal wave-based
 * GameplayMenu, co-op hands off to the existing {@link CouchIZombieController}
 * match, whose own placement mechanism is left exactly as it was.
 */
public class CoopBeforeMenu extends BeforeMenu {

    /**
     * Exactly what the match can actually field. It used to be 7 (mirroring the plant
     * loadout), but {@link IZombieMatch} only ever builds {@link IZombieMatch#MAX_ROSTER_SIZE}
     * packets and the in-match tray only binds hotkeys 1-6, so a 7th pick was accepted in
     * the loadout and then silently dropped - it never reached the tray and could never
     * be placed. Deriving it keeps the picker honest.
     */
    public static final int ZOMBIE_SLOTS = IZombieMatch.MAX_ROSTER_SIZE;

    private static final Pattern ADD_ZOMBIE = Pattern.compile(
            "^\\s*add\\s+zombie\\s+-t\\s+(?<type>\\S+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMOVE_ZOMBIE = Pattern.compile(
            "^\\s*remove\\s+zombie\\s+-t\\s+(?<type>\\S+)\\s*$", Pattern.CASE_INSENSITIVE);

    private final CollectionManager manager = new CollectionManager();

    @Override
    public String getName() {
        return "Co-op Before Menu";
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
        } else if (text.trim().equalsIgnoreCase("start game")) {
            startCoopMatch();
        } else {
            super.handleCommand(text);
        }
    }

    private void addZombie(String alias) {
        boolean known = manager.getAllZombieAliases().stream().anyMatch(a -> a.equalsIgnoreCase(alias));
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
        if (selectedZombies.removeIf(z -> z.equalsIgnoreCase(alias))) {
            GeneralPrinter.print(alias + " removed from zombie loadout.");
        } else {
            throw new GameException("zombie not in loadout.");
        }
    }

    private void startCoopMatch() {
        if (selectedPlants.isEmpty()) {
            throw new GameException("select at least one plant before starting.");
        } else if (selectedZombies.isEmpty()) {
            throw new GameException("select at least one zombie before starting.");
        }
        GeneralPrinter.print("Co-op match starting - " + selectedPlants.size()
                + " plants vs " + selectedZombies.size() + " zombies.");
        
        
        App.currentMenu = new CouchIZombieController(selectedPlants, selectedZombies);
    }

    @Override
    public void exitMenu() {
        selectedZombies.clear();
        App.currentMenu = new MainMenu();
    }

    @Override
    public String showMenu() {
        return "[ Co-op Loadout ]\n"
                + "Plants selected: " + selectedPlants + " (" + selectedPlants.size() + "/8)\n"
                + "Zombies selected: " + selectedZombies + " (" + selectedZombies.size() + "/" + ZOMBIE_SLOTS + ")\n"
                + "Commands:\n"
                + "  show all plants | show available plants\n"
                + "  add plant -t <type> | remove plant -t <type>\n"
                + "  add zombie -t <alias> | remove zombie -t <alias>\n"
                + "  boost plant -t <type> | upgrade plant -t <type>\n"
                + "  start game\n"
                + "  menu exit | menu show current";
    }
}