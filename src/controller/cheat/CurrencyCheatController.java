package controller.cheat;

import model.resoures.CurrencyType;
import model.user_data.User;
import model.user_data.UserState;
import view.GeneralPrinter;

/**
 * Applies the "add currency" cheat to the currently logged in user and persists it.
 * Kept separate from the view so any screen (or a future debug console/command)
 * can reuse the same logic instead of poking {@link UserState} directly.
 */
public class CurrencyCheatController {

    /**
     * Grants {@code amount} of {@code currency} to the current user, saves the
     * profile and reports the result through {@link GeneralPrinter}.
     *
     * @return true if the amount was applied.
     */
    public boolean grant(CurrencyType currency, int amount) {
        if (currency == null || amount <= 0) {
            GeneralPrinter.print("[Cheat] Enter a positive amount first.");
            return false;
        }

        UserState state = currentUserState();
        if (state == null) {
            GeneralPrinter.print("[Cheat] No active user to apply the cheat to.");
            return false;
        }

        currency.addAmount(state, amount);
        User.save();
        GeneralPrinter.print("[Cheat] Added " + amount + " " + currency.getDisplayName().toLowerCase() + ".");
        return true;
    }

    public int currentAmount(CurrencyType currency) {
        UserState state = currentUserState();
        return (state == null || currency == null) ? 0 : currency.getAmount(state);
    }

    private UserState currentUserState() {
        User user = User.currentUser;
        return user != null ? user.userState : null;
    }
}
