package view.screens.ui_menus;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.greenhouse.Greenhouse;
import model.greenhouse.Pot;
import model.greenhouse.shop.Product;
import model.greenhouse.shop.Shop;
import model.user_data.User;
import model.user_data.UserState;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.UiScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static view.screens.ui_menus.GreenhouseScreen.SEED_PACKET_BG;
import static view.screens.ui_menus.GreenhouseScreen.SEED_PACKET_ICON;

public class ShopScreen extends UiScreen {

    private static final Shop STORE = new Shop();

    private static final String TAB_ICON_PLANTS = "assets/images/shop/event_icon_potw_down.png";
    private static final String TAB_ICON_CURRENCY = "assets/images/shop/moneybag.png";
    private static final String INFO_ICON = "assets/images/shop/info_on.png";
    private static final String INFO_ICON_ACTIVE = "assets/images/shop/info_off.png";

    private static final float TAB_ICON_SIZE = 46f;

    private static final float CARD_W = 200f;
    private static final float CARD_H = 300f;
    private static final float CARD_IMAGE_SIZE = 130f;
    private static final float DETAILS_H = 60f;

    private static final float DAILY_BOX_W = 260f;
    private static final float ITEMS_BOX_W = 700f;
    private static final float PANEL_H = 380f;

    private enum ShopTab { PLANTS, CURRENCY }

    private ShopTab currentTab = ShopTab.PLANTS;
    private final java.util.Set<String> expandedDetails = new java.util.HashSet<>();

    @Override
    public void show() {
        setBackground("assets/images/backg/mainmenu_background.png");
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        super.show();
        build();
    }

    private void build() {
        rootTable.clear();

        UserState state = User.currentUser.userState;
        STORE.refreshDailyOffer(state);

        rootTable.add(buildTopBar()).fillX().padTop(5).padLeft(15).padRight(15).row();

        Table centerTable = new Table();
        Label title = new Label("Shop", skin, "title");
        title.setFontScale(1.6f);
        centerTable.add(title).padBottom(SPACE_MD).row();

        centerTable.add(buildCategoryTabs()).left().padBottom(SPACE_SM).row();

        Table board = new Table();
        board.setBackground(skin.getDrawable("card-background"));
        board.pad(SPACE_MD);
        board.add(buildDailyOfferBox(state)).width(DAILY_BOX_W).height(PANEL_H).top().padRight(SPACE_MD);
        board.add(buildItemsBox(state)).width(ITEMS_BOX_W).height(PANEL_H).top();
        centerTable.add(board);

        rootTable.add(centerTable).expand().center().row();
    }

    private Table buildCategoryTabs() {
        Table tabs = new Table();
        tabs.add(buildCategoryTab(TAB_ICON_PLANTS, "Plants", ShopTab.PLANTS)).padRight(SPACE_MD);
        tabs.add(buildCategoryTab(TAB_ICON_CURRENCY, "Coins & Gems", ShopTab.CURRENCY));
        return tabs;
    }

    private Table buildCategoryTab(String iconPath, String label, ShopTab tab) {
        Table container = new Table();
        if (tab == currentTab) {
            container.setBackground(skin.getDrawable("card-background"));
        }
        container.pad(SPACE_XS, SPACE_SM, SPACE_XS, SPACE_SM);

        Image icon = new Image(loadTextureSafe(iconPath));
        container.add(icon).size(TAB_ICON_SIZE, TAB_ICON_SIZE).row();

        Label tabLabel = new Label(label, skin, tab == currentTab ? "title" : "muted");
        tabLabel.setFontScale(0.8f);
        container.add(tabLabel).padTop(2);

        container.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentTab = tab;
                build();
            }
        });
        return container;
    }

    private Table buildDailyOfferBox(UserState state) {
        Table box = new Table();
        box.top();

        Label header = new Label("Daily Offer", skin, "title");
        header.setFontScale(0.9f);
        box.add(header).left().padBottom(SPACE_SM).row();

        Table list = new Table();
        list.top();
        list.add(buildDailyOfferCard(state)).width(CARD_W).height(CARD_H).row();

        ScrollPane scrollPane = scrollable(list);
        box.add(scrollPane).expand().fill();
        return box;
    }

    private Table buildItemsBox(UserState state) {
        Table box = new Table();
        box.top();

        String header = currentTab == ShopTab.PLANTS ? "Plants" : "Coins & Gems";
        Label headerLabel = new Label(header, skin, "title");
        headerLabel.setFontScale(0.9f);
        box.add(headerLabel).left().padBottom(SPACE_SM).row();

        Table row = new Table();
        row.left();
        for (Product product : productsForTab(currentTab)) {
            row.add(buildItemCard(product, state)).width(CARD_W).height(CARD_H).padRight(SPACE_MD);
        }

        ScrollPane scrollPane = new ScrollPane(row);
        scrollPane.setScrollingDisabled(false, true);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        box.add(scrollPane).expand().fill();
        return box;
    }

    private List<Product> productsForTab(ShopTab tab) {
        return switch (tab) {
            case PLANTS -> List.of(Product.POT, Product.PLANT_FOOD, Product.SEED_RANDOM, Product.SEED_CHOICE);
            case CURRENCY -> List.of(Product.CURRENCY_EXCHANGE);
        };
    }

    private Table buildDailyOfferCard(UserState state) {
        String cardId = "daily-offer";
        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));
        card.pad(SPACE_SM);
        card.top();

        String plantName = plantName(state.dailyOfferPlantId);
        String detail = state.dailyOfferPurchased
                ? "Already purchased today - come back tomorrow."
                : "10 seed packs, once a day.";

        Table detailsSlot = new Table();
        detailsSlot.top();

        Table headerRow = new Table();
        Label nameLabel = new Label(plantName, skin, "main");
        nameLabel.setFontScale(0.85f);
        nameLabel.setWrap(true);
        headerRow.add(nameLabel).expandX().left().width(CARD_W - 60f);
        headerRow.add(buildInfoButton(cardId, detailsSlot, detail)).size(22, 22).right();
        card.add(headerRow).fillX().row();

        Image icon = new Image(loadTextureSafe(iconFor(Product.DAILY_OFFER)));
        card.add(icon).size(CARD_IMAGE_SIZE, CARD_IMAGE_SIZE).padTop(SPACE_SM).padBottom(SPACE_SM).row();

        fillDetailsSlot(detailsSlot, cardId, detail);
        card.add(detailsSlot).width(CARD_W - 40f).height(DETAILS_H).padBottom(SPACE_SM).row();
        card.add().expandY().row();

        boolean canBuy = !state.dailyOfferPurchased && state.dailyOfferPlantId != null;
        TextButton buyBtn = primaryButton(state.dailyOfferPurchased
                        ? "Bought" : Product.DAILY_OFFER.getCoinCost() + " coins",
                () -> new ConfirmBuyModal(Product.DAILY_OFFER, () -> buy("daily-offer", 1, null)).show());
        buyBtn.setDisabled(!canBuy);
        card.add(buyBtn).width(150).height(44);

        return card;
    }

    private Table buildItemCard(Product product, UserState state) {
        String cardId = product.getItemId();
        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));
        card.pad(SPACE_SM);
        card.top();

        String detail = extraNote(product, state);

        Table detailsSlot = new Table();
        detailsSlot.top();

        Table headerRow = new Table();
        Label nameLabel = new Label(product.getDisplayName(), skin, "main");
        nameLabel.setFontScale(0.85f);
        nameLabel.setWrap(true);
        headerRow.add(nameLabel).expandX().left().width(CARD_W - 60f);
        headerRow.add(buildInfoButton(cardId, detailsSlot, detail)).size(22, 22).right();
        card.add(headerRow).fillX().row();

        Image icon = new Image(loadTextureSafe(iconFor(product)));
        card.add(icon).size(CARD_IMAGE_SIZE, CARD_IMAGE_SIZE).padTop(SPACE_SM).padBottom(SPACE_SM).row();

        fillDetailsSlot(detailsSlot, cardId, detail);
        card.add(detailsSlot).width(CARD_W - 40f).height(DETAILS_H).padBottom(SPACE_SM).row();
        card.add().expandY().row();

        TextButton buyBtn = primaryButton(costLabel(product), () -> onBuyClicked(product, state));
        card.add(buyBtn).width(150).height(44);

        return card;
    }

    private ImageButton buildInfoButton(String cardId, Table detailsSlot, String detail) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(loadTextureSafe(infoIconFor(cardId)));
        ImageButton infoBtn = new ImageButton(drawable);
        infoBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (expandedDetails.contains(cardId)) {
                    expandedDetails.remove(cardId);
                } else {
                    expandedDetails.add(cardId);
                }
                fillDetailsSlot(detailsSlot, cardId, detail);
                infoBtn.getStyle().imageUp = new TextureRegionDrawable(loadTextureSafe(infoIconFor(cardId)));
                infoBtn.getStyle().imageDown = infoBtn.getStyle().imageUp;
            }
        });
        return infoBtn;
    }

    private String infoIconFor(String cardId) {
        return expandedDetails.contains(cardId) ? INFO_ICON_ACTIVE : INFO_ICON;
    }

    private void fillDetailsSlot(Table detailsSlot, String cardId, String detail) {
        detailsSlot.clear();
        if (detail != null && expandedDetails.contains(cardId)) {
            Label detailLabel = new Label(detail, skin, "muted");
            detailLabel.setFontScale(0.85f);
            detailLabel.setWrap(true);
            detailsSlot.add(detailLabel).width(CARD_W - 40f).top();
        }
    }

    private void onBuyClicked(Product product, UserState state) {
        if (product == Product.SEED_CHOICE) {
            new SeedChoiceModal(state, plantId -> new ConfirmBuyModal(product,
                    () -> buy(product.getItemId(), 1, plantId)).show()).show();
        } else {
            new ConfirmBuyModal(product, () -> buy(product.getItemId(), 1, null)).show();
        }
    }

    private void buy(String itemId, int count, Integer plantTypeId) {
        String cmd = "shop buy -i " + itemId + " -n " + count
                + (plantTypeId != null ? " -t " + plantName(plantTypeId) : "");
        runCommand(cmd);
    }

    private String costLabel(Product product) {
        if (product.getCoinCost() > 0) return product.getCoinCost() + " coins";
        if (product.getDiamondCost() > 0) return product.getDiamondCost() + " diamonds";
        return "Free";
    }

    private String extraNote(Product product, UserState state) {
        if (product == Product.POT) {
            int total = Greenhouse.getRowCount() * Greenhouse.getColCount();
            return "Pots unlocked: " + countUnlockedPots() + " / " + total;
        }
        if (product == Product.PLANT_FOOD) {
            return "Holding: " + state.plantFoodCount + " / 3";
        }
        if (product == Product.SEED_RANDOM) {
            return "Gives seed packets for a random unlocked plant.";
        }
        if (product == Product.SEED_CHOICE) {
            return "Choose which unlocked plant to buy seeds for.";
        }
        if (product == Product.CURRENCY_EXCHANGE) {
            return "Exchange diamonds for 500 coins.";
        }
        return null;
    }

    private int countUnlockedPots() {
        int count = 0;
        Pot[][] pots = Greenhouse.getInstance().getPots();
        for (Pot[] row : pots) {
            for (Pot pot : row) {
                if (pot != null && !pot.isLocked()) count++;
            }
        }
        return count;
    }

    private String iconFor(Product product) {
        return switch (product) {
            case POT -> "assets/images/shop/pot.png";
            case PLANT_FOOD -> "assets/images/shop/plantfood.png";
            case SEED_RANDOM -> "assets/images/shop/random_seed.png";
            case SEED_CHOICE -> "assets/images/shop/choice_seed.png";
            case CURRENCY_EXCHANGE -> "assets/images/shop/exchange.png";
            case DAILY_OFFER -> "assets/images/shop/daily.png";
        };
    }

    private String plantName(Integer plantId) {
        if (plantId == null) return "none available";
        Map<Integer, PlantJsonParser.PlantConfig> blueprints = PlantFactory.getBlueprints();
        PlantJsonParser.PlantConfig config = blueprints.get(plantId);
        return config == null ? ("#" + plantId) : config.name;
    }

    private Table buildTopBar() {
        ImageButton backBtn = createIconButton("assets/images/ui/buttons_hud_back_normal.png", 54, 54,
                () -> runCommand("menu exit"));

        Table topLeft = new Table();
        topLeft.add(backBtn).padRight(16);
        topLeft.add(createIconButtonWithLabel("assets/images/ui/collection.png", 54, 54,
                "Collection", () -> runCommand("menu enter collection")));

        User user = User.currentUser;
        int coins = (user != null && user.userState != null) ? user.userState.coins : 0;
        int diamonds = (user != null && user.userState != null) ? user.userState.diamonds : 0;
        int seedPackets = totalSeedPackets(user);

        Table currencyRow = new Table();
        currencyRow.add(createResourceWidget("assets/images/ui/buttons_coin_buy_normal.png",
                String.valueOf(coins))).padRight(15);
        currencyRow.add(createResourceWidget("assets/images/ui/buttons_premium_normal.png",
                String.valueOf(diamonds)));

        Table topRight = new Table();
        topRight.add(currencyRow).right().row();
        topRight.add(createResourceWidget(SEED_PACKET_BG, SEED_PACKET_ICON, String.valueOf(seedPackets), 130, 55))
                .padTop(25).padBottom(-45).padRight(8).right();

        Table topBar = new Table();
        topBar.add(topLeft).left().expandX().padBottom(-110);
        topBar.add(topRight).right().padBottom(-120);
        return topBar;
    }

    private int totalSeedPackets(User user) {
        if (user == null || user.userState == null || user.userState.seedPacketInventory == null) {
            return 0;
        }
        return user.userState.seedPacketInventory.values().stream().mapToInt(Integer::intValue).sum();
    }

    private ImageButton createIconButton(String path, float width, float height, Runnable action) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(loadTextureSafe(path));
        ImageButton button = new ImageButton(drawable);
        button.getImageCell().size(width, height);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    private Table createIconButtonWithLabel(String path, float width, float height, String text, Runnable action) {
        Table container = new Table();
        ImageButton btn = createIconButton(path, width, height, action);
        Label label = new Label(text, skin, "title");

        container.add(btn).row();
        container.add(label).padTop(2);

        container.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return container;
    }

    private Table createResourceWidget(String bgPath, String iconPath, String value, float width, float height) {
        float bgWidth = 100;
        float bgHeight = 30;
        float groupWidth = Math.max(width, bgWidth);
        float groupHeight = Math.max(height, bgHeight);

        Group group = new Group();
        group.setSize(groupWidth, groupHeight);

        Image bgImage = new Image(loadTextureSafe(bgPath));
        bgImage.setSize(bgWidth, bgHeight);
        bgImage.setPosition(((groupWidth - bgWidth) / 2f) + 20, (groupHeight - bgHeight) / 2f);
        group.addActor(bgImage);

        Table textTable = new Table();
        Image iconImage = new Image(loadTextureSafe(iconPath));
        iconImage.setSize(55, 85);
        textTable.add(iconImage).left().expand();
        textTable.setSize(width, height);
        textTable.setPosition((groupWidth - width) / 2f, (groupHeight - height) / 2f);
        textTable.add(new Label(value, skin, "title")).center().expand();
        group.addActor(textTable);

        Table outer = new Table();
        outer.add(group).size(groupWidth, groupHeight);
        return outer;
    }

    @Override
    protected void onAfterCommand() {
        build();
    }

    private class ConfirmBuyModal extends view.screens.generals.Modal {

        ConfirmBuyModal(Product product, Runnable onConfirm) {
            content.add(new Label("Confirm Purchase", skin, "title")).padBottom(SPACE_SM).row();

            Label message = new Label("Buy " + product.getDisplayName() + " for " + costLabel(product) + "?",
                    skin, "main");
            message.setWrap(true);
            content.add(message).width(280).padBottom(SPACE_SM).row();

            Table buttons = new Table();
            buttons.add(secondaryButton("Cancel", this::hide)).width(130).height(44).padRight(SPACE_SM);
            buttons.add(primaryButton("Confirm", () -> {
                hide();
                onConfirm.run();
            })).width(130).height(44);
            content.add(buttons).padTop(SPACE_SM);
        }
    }

    private class SeedChoiceModal extends view.screens.generals.Modal {

        private final SeedPacketCardFactory cardFactory = new SeedPacketCardFactory();

        SeedChoiceModal(UserState state, java.util.function.Consumer<Integer> onPick) {
            content.pad(24);

            Label titleLabel = new Label("Choose a Plant", skin, "title");
            titleLabel.setFontScale(1.1f);
            content.add(titleLabel).padBottom(SPACE_SM).row();

            List<Integer> unlockedIds = new ArrayList<>(state.unlockedPlantIds);
            Table grid = new Table();
            grid.top().padTop(10f);

            if (unlockedIds.isEmpty()) {
                grid.add(new Label("No unlocked plants yet.", skin, "muted"));
            } else {
                int col = 0;
                int plantsPerRow = 4;

                for (Integer plantId : unlockedIds) {
                    String pName = plantName(plantId);
                    Table cell = new Table();

                    try {
                        SeedPacketCard card = cardFactory.buildCardForDisplayName(pName);
                        if (card != null) {
                            card.addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                    hide();
                                    cardFactory.dispose();
                                    onPick.accept(plantId);
                                }
                            });
                            cell.add(card).size(card.getWidth(), card.getHeight());
                        }
                    } catch (Exception e) {
                        TextButton pick = secondaryButton(pName, () -> {
                            hide();
                            cardFactory.dispose();
                            onPick.accept(plantId);
                        });
                        cell.add(pick).width(130).height(42);
                    }

                    grid.add(cell).pad(SPACE_XS);
                    col++;
                    if (col % plantsPerRow == 0) {
                        grid.row();
                    }
                }
            }

            ScrollPane scrollPane = new ScrollPane(grid);
            scrollPane.setScrollingDisabled(true, false);
            scrollPane.setFadeScrollBars(false);

            content.add(scrollPane).width(780).height(400).padBottom(SPACE_SM).row();

            TextButton cancel = secondaryButton("Cancel", () -> {
                cardFactory.dispose();
                hide();
            });
            content.add(cancel).width(160).padTop(SPACE_XS);
        }
    }
}