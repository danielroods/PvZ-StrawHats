package model.resoures;

import model.user_data.UserState;

/**
 * Every currency that the "add currency" cheat popup can grant.
 * Adding a new currency later only means adding one more enum constant here -
 * every screen/menu that already uses {@code CurrencyType} (top bar widgets,
 * the cheat popup, the cheat controller) will pick it up automatically.
 */
public enum CurrencyType {

    COIN("assets/images/ui/buttons_coin_buy_normal.png", "Coins") {
        @Override
        public int getAmount(UserState state) {
            return state.coins;
        }

        @Override
        public void addAmount(UserState state, int amount) {
            state.coins += amount;
        }
    },

    DIAMOND("assets/images/ui/buttons_premium_normal.png", "Diamonds") {
        @Override
        public int getAmount(UserState state) {
            return state.diamonds;
        }

        @Override
        public void addAmount(UserState state, int amount) {
            state.diamonds += amount;
        }
    };

    private final String iconPath;
    private final String displayName;

    CurrencyType(String iconPath, String displayName) {
        this.iconPath = iconPath;
        this.displayName = displayName;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Current amount of this currency the given user state holds. */
    public abstract int getAmount(UserState state);

    /** Adds (never subtracts) amount to this currency on the given user state. */
    public abstract void addAmount(UserState state, int amount);
}
