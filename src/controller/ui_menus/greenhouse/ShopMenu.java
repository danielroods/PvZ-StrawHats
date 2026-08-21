package controller.ui_menus.greenhouse;

import controller.ui_menus.Menu;
import model.App;
import model.Regex;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.game_exceptions.GameException;
import model.greenhouse.shop.Shop;
import model.user_data.User;
import model.user_data.UserState;
import view.GeneralPrinter;

import java.util.Map;
import java.util.regex.Matcher;

public class ShopMenu extends Menu {

    private static final Shop STORE = new Shop();

    @Override
    public String getName() {
        return "Store Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;
        handleStoreCommand(text);
    }

    private void handleStoreCommand(String text) {
        UserState state = User.currentUser.userState;

        if (Regex.SHOPPING_LIST.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(STORE.renderPermanentGoods());
        } else if (Regex.SHOP_DAILY.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(STORE.renderDailyOffer(state));
        } else if (Regex.SHOP_BUY.getMatcherRaw(text).matches()) {
            Matcher m = Regex.SHOP_BUY.getMatcherRaw(text);
            m.matches();
            String itemId = m.group("itemid");
            int count = Integer.parseInt(m.group("count"));
            String plantTypeName = m.group("planttype");
            Integer plantTypeId = plantTypeName == null ? null : resolvePlantId(plantTypeName);
            if (plantTypeName != null && plantTypeId == null) {
                throw new GameException("unknown plant type '" + plantTypeName + "'.");
            } else {
                GeneralPrinter.print(STORE.buy(state, itemId, count, plantTypeId));
            }
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    private Integer resolvePlantId(String name) {
        String normalized = name.replace("-", "").replace("_", "").replace(" ", "").toLowerCase();
        for (Map.Entry<Integer, PlantJsonParser.PlantConfig> entry : PlantFactory.getBlueprints().entrySet()) {
            String candidate = entry.getValue().name.replace(" ", "").toLowerCase();
            if (candidate.equals(normalized)) return entry.getKey();
        }
        return null;
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new GreenhouseMenu();
    }

    @Override
    public String showMenu() {
        return "[ Store Menu ]\n"
                + STORE.renderPermanentGoods() + "\n"
                + "Commands:\n"
                + "  shopping list\n"
                + "  shop daily\n"
                + "  shop buy -i <item_id> -n <count> [-t <plant_type>]\n"
                + "  menu exit | menu show current";
    }
}
