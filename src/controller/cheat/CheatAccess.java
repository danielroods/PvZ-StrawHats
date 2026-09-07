package controller.cheat;

import model.utils.GameSettings;
import view.GeneralPrinter;

public final class CheatAccess {

    private static final String DISABLED_MESSAGE =
            "[Cheat] Debug mode is off - enable it in Settings to use cheats.";

    private CheatAccess() {
    }

    public static boolean isEnabled() {
        return GameSettings.get().isDebugMode();
    }

    public static boolean allow() {
        if (isEnabled()) {
            return true;
        }
        GeneralPrinter.print(DISABLED_MESSAGE);
        return false;
    }
}
