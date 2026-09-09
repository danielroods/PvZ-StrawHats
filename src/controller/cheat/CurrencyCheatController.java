package controller.cheat;

import model.resoures.CurrencyType;
import model.user_data.User;
import model.user_data.UserState;
import view.GeneralPrinter;


public class CurrencyCheatController {

    
    public boolean grant(CurrencyType currency, int amount) {
        if (!CheatAccess.allow()) {
            return false;
        }
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
