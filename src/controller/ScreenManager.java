package controller;

import controller.ui_menus.*;
import controller.ui_menus.authentication.LoginMenu;
import controller.ui_menus.authentication.SignupMenu;
import controller.match.MatchMenu;
import controller.match.BeforeMenu;
import controller.match.GameplayMenu;
import controller.match.AfterMenu;
import controller.ui_menus.greenhouse.GreenhouseMenu;
import controller.ui_menus.greenhouse.ShopMenu;
import model.App;
import model.match.main.levels.Level;
import view.screens.generals.BaseScreen;
import view.screens.generals.GameMenuScreen;
import view.screens.match.after.AfterMatchScreen;
import view.screens.stages.*;
import view.screens.ui_menus.*;

import view.screens.match.before.BeforeMatchScreen;
import view.screens.match.gameplay.*;
import view.screens.stages.BigWaveBeachStagesScreen;
import view.screens.stages.DarkAgesStagesScreen;
import view.screens.stages.EgyptStagesScreen;
import view.screens.stages.FrostbiteCavesStagesScreen;
import view.screens.generals.GameScreen;
import controller.match.mini_games.*;
import view.screens.match.gameplay.mini_games.*;

public final class ScreenManager {

    private static BaseScreen currentScreen;
    private static Class<? extends Menu> currentMenuClass;

    private ScreenManager() {
    }

    public static void setScreen(BaseScreen next) {
        if (currentScreen != null) {
            currentScreen.hide();
            currentScreen.dispose();
        }
        currentScreen = next;
        if (currentScreen != null) {
            currentScreen.show();
        }
    }

    /**
     * Refreshes whatever screen is currently on-screen with the latest model data, without
     * changing which screen it is. Used by things that update the account from outside the
     * normal menu flow (e.g. the local TA offer web server), so the player sees the change
     * immediately instead of needing to leave and reopen the screen.
     */
    public static void refreshCurrentScreen() {
        if (currentScreen instanceof view.screens.generals.UiScreen uiScreen) {
            uiScreen.refresh();
        }
    }

    public static void syncWithCurrentMenu() {
        // Don't swap screens out from under an in-flight win/lose sequence: some menus (mini
        // games in particular) flip App.currentMenu to their end-of-game menu as soon as the
        // outcome is known, well before the board-hold/fade/title animation on the current
        // GameScreen finishes playing. This is polled every frame (see Main#render), so
        // without this guard the very next frame would tear the animation down after a
        // single frame. Once the sequence itself finishes it triggers a sync, so this never
        // gets permanently stuck.
        if (currentScreen instanceof GameScreen gameScreen && gameScreen.isMatchEndSequenceActive()) {
            return;
        }

        Menu menu = App.currentMenu;
        Class<? extends Menu> menuClass = menu == null ? null : menu.getClass();
        if (currentScreen != null && menuClass == currentMenuClass) {
            return;
        }
        currentMenuClass = menuClass;
        setScreen(resolveScreen(menu));
    }

    /**
     * Same as {@link #syncWithCurrentMenu()}, but always rebuilds the screen even when
     * {@link App#currentMenu} is still the same *class* it was before (e.g. restarting a
     * match sets App.currentMenu to a brand new GameplayMenu instance, but it's still a
     * GameplayMenu - syncWithCurrentMenu()'s same-class check would treat that as "nothing
     * to do" and leave the old GameScreen on screen, still wired to the GameSession that
     * "restart" just replaced, which is why nothing on it responded to input anymore).
     * Used by restart flows, which always need a fresh screen instance regardless of
     * whether the destination menu's class happens to match the one just left.
     */
    public static void forceResync() {
        if (currentScreen instanceof GameScreen gameScreen && gameScreen.isMatchEndSequenceActive()) {
            return;
        }

        Menu menu = App.currentMenu;
        currentMenuClass = menu == null ? null : menu.getClass();
        setScreen(resolveScreen(menu));
    }

    private static BaseScreen resolveScreen(Menu menu) {
        if (menu instanceof SignupMenu) {
            return new SignupScreen();
        }
        if (menu instanceof LoginMenu) {
            return new LoginScreen();
        }
        if (menu instanceof MainMenu) {
            return new MainMenuScreen();
        }
        if (menu instanceof GameMenu) {
            return new GameMenuScreen();
        }
        if (menu instanceof AdventureMenu) {
            return new AdventureScreen();
        }
        if (menu instanceof ConsoleMenu) {
            return new ConsoleScreen();
        }
        if (menu instanceof ZombiePackmanMenu) {
            return new view.screens.generals.LoadingScreen(
                    ZombiePackmanGameScreen::new);
        }
        if (menu instanceof ProfileMenu) {
            return new ProfileMenuScreen();
        }
        if (menu instanceof NewsMenu) {
            return new NewsScreen();
        }
        if (menu instanceof TravelLogMenu) {
            return new TravelLogScreen();
        }
        if (menu instanceof SettingMenu) {
            return new SettingsScreen();
        }
        if (menu instanceof LeaderboardMenu) {
            return new LeaderboardScreen();
        }
        if (menu instanceof TrophiesMenu) {
            return new TrophiesScreen();
        }
        if (menu instanceof ShopMenu) {
            return new ShopScreen();
        }
        if (menu instanceof GreenhouseMenu) {
            return new GreenhouseScreen();
        }
        if (menu instanceof CollectionMenu) {
            return new CollectionScreen();
        }
        if (menu instanceof TrophiesMenu) {
            return new TrophiesScreen();
        }
        if (menu instanceof controller.ui_menus.network.NetworkMenu) {
            return new NetworkScreen();
        }
        if (menu instanceof CouchIZombieController) {
            return new view.screens.generals.LoadingScreen(
                    CouchIZombieGameScreen::new);
        }
        if (menu instanceof NetIZombieController) {
            return new view.screens.generals.LoadingScreen(
                    NetIZombieGameScreen::new);
        }

        if (menu instanceof controller.match.NetBeforeMenu) {
            return new view.screens.generals.LoadingScreen(
                    view.screens.match.before.NetBeforeMatchScreen::new);
        }
        if (menu instanceof controller.match.CoopBeforeMenu) {
            return new view.screens.generals.LoadingScreen(
                    view.screens.match.before.CoopBeforeMatchScreen::new);
        }
        if (menu instanceof BeforeMenu) {
            return new BeforeMatchScreen();
        }
        if (menu instanceof GameplayMenu) {
            Level level = model.utils.GameSession.peekInstance() == null
                    ? null : model.utils.GameSession.peekInstance().getLevel();



            String seasonName = level == null || level.getSeason() == null ? null : level.getSeason().getName();
            if (seasonName != null && seasonName.equalsIgnoreCase("Egypt")) {
                return new EgyptGameScreen();
            }
            if (seasonName != null && seasonName.equalsIgnoreCase("Dark Ages")) {
                return new DarkAgesGameScreen();
            }
            if (seasonName != null && seasonName.equalsIgnoreCase("Big Wave Beach")) {
                return new BigWaveBeachGameScreen();
            }
            if (seasonName != null && seasonName.equalsIgnoreCase("Frostbite Caves")) {
                return new FrostbiteCavesGameScreen();
            }
            if (seasonName != null && seasonName.equalsIgnoreCase("Pirates")) {
                return new PirateGameScreen();
            }
            return new GameScreen();
        }
        if (menu instanceof AfterMenu) {
            return new AfterMatchScreen();
        }

        if (menu instanceof VasebreakerController) {
            return new VasebreakerGameScreen();
        }
        if (menu instanceof WallnutBowlingController) {
            return new WallnutBowlingGameScreen();
        }
        if (menu instanceof ImZombieController) {
            return new IZombieGameScreen();
        }
        if (menu instanceof BeghouledController) {
            return new BeghouledGameScreen();
        }
        if (menu instanceof ZombotanyController) {
            return new ZombotanyGameScreen();
        }
        if (menu instanceof MiniGameEndMenu) {
            return new MiniGameEndScreen();
        }

        if (menu instanceof MatchMenu) {
            Level selected = MatchMenu.selectedLevel;
            String seasonName = selected != null ? selected.getSeason().getName() : null;
            if (seasonName != null && seasonName.equalsIgnoreCase("Egypt")) {
                return new EgyptStagesScreen();
            }
            if (seasonName != null && seasonName.equalsIgnoreCase("Frostbite Caves")) {
                return new FrostbiteCavesStagesScreen();
            }
            if (seasonName != null && seasonName.equalsIgnoreCase("Big Wave Beach")) {
                return new BigWaveBeachStagesScreen();
            }
            if (seasonName != null && seasonName.equalsIgnoreCase("Dark Ages")) {
                return new DarkAgesStagesScreen();
            }
            if (seasonName != null && seasonName.equalsIgnoreCase("Pirates")) {
                return new PirateStagesScreen();
            }
            if (seasonName != null && seasonName.equalsIgnoreCase("Future")) {
                return new FutureStagesScreen();
            }

        }
        return new PlaceholderScreen(menu);
    }

    private static boolean isDangerOrLotteryLevel(Level level) {
        if (level == null) {
            Level selected = MatchMenu.selectedLevel;
            if (selected != null) {
                return isDangerOrLotteryLevel(selected);
            }
            return false;
        }

        String className = level.getClass().getSimpleName().toLowerCase();
        if (className.contains("danger") || className.contains("lottery") || className.contains("pipe")) {
            return true;
        }

        try {
            if (level.getName() != null) {
                String name = level.getName().toLowerCase();
                if (name.contains("danger") || name.contains("lottery") || name.contains("pipe") || name.contains("لوله")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        String[] checkMethods = {"isDangerNode", "isDanger", "isLottery", "isLotteryLevel"};
        for (String methodName : checkMethods) {
            try {
                java.lang.reflect.Method m = level.getClass().getMethod(methodName);
                Object val = m.invoke(level);
                if (Boolean.TRUE.equals(val)) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        return false;
    }

    public static BaseScreen getScreen() {
        return currentScreen;
    }

    public static void render(float delta) {
        if (currentScreen != null) {
            currentScreen.render(delta);
        }
    }

    public static void resize(int width, int height) {
        if (currentScreen != null) {
            currentScreen.resize(width, height);
        }
    }

    public static void dispose() {
        if (currentScreen != null) {
            currentScreen.dispose();
            currentScreen = null;
        }
    }
}