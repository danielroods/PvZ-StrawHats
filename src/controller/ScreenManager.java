package controller;

import controller.menus.*;
import controller.menus.authentication.LoginMenu;
import controller.menus.authentication.SignupMenu;
import controller.menus.match.MatchMenu;
import controller.menus.match.BeforeMenu;
import controller.menus.match.MeanwhileMenu;
import controller.menus.match.AfterMenu;
import controller.menus.greenhouse.GreenhouseMenu;
import controller.menus.greenhouse.ShopMenu;
import model.App;
import model.match.main.levels.Level;
import view.general_screens.BaseScreen;
import view.screens.*;
import view.screens.stages_screens.BigWaveBeachStagesScreen;
import view.screens.stages_screens.DarkAgesStagesScreen;
import view.screens.stages_screens.EgyptStagesScreen;
import view.screens.stages_screens.FrostbiteCavesStagesScreen;
import view.general_screens.GameScreen;
import controller.mini_games.*;
import view.screens.mini_games.*;

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

    public static void syncWithCurrentMenu() {
        Menu menu = App.currentMenu;
        Class<? extends Menu> menuClass = menu == null ? null : menu.getClass();
        if (currentScreen != null && menuClass == currentMenuClass) {
            return;
        }
        currentMenuClass = menuClass;
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
        if (menu instanceof ShopMenu) {
            return new ShopScreen();
        }
        if (menu instanceof GreenhouseMenu) {
            return new GreenhouseScreen();
        }
        if (menu instanceof CollectionMenu) {
            return new CollectionScreen();
        }

        if (menu instanceof BeforeMenu) {
            return new BeforeMatchScreen();
        }
        if (menu instanceof MeanwhileMenu) {
            Level level = model.utils.GameSession.peekInstance() == null
                    ? null : model.utils.GameSession.peekInstance().getLevel();

            if (isDangerOrLotteryLevel(level)) {
                return new LotteryGameScreen(model.utils.GameSession.peekInstance());
            }

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