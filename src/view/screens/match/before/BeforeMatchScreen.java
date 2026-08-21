package view.screens.match.before;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import controller.CollectionManager;
import controller.match.BeforeMenu;
import model.collections.animations.AnimationFactory;
import model.collections.plant.PlantJsonParser;
import model.match.main.levels.Level;
import model.match.main.levels.special_levels.ConveyorBeltLevel;
import model.match.main.levels.special_levels.LockedPlantsLevel;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.GameSession;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;
import service.resource_manager.AudioManager;
import view.screens.generals.UiScreen;

import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BeforeMatchScreen extends UiScreen {

    private static final String BACKGROUND = "assets/images/backg/mainmenu_background.png";
    private static final String BOARD = "assets/images/backg/wood board.png";
    private static final String BACK_ICON = "assets/images/ui/buttons_hud_back_normal.png";
    private static final String LOCK_ICON = "assets/images/ui/collection/lock_small_gold.png";
    private static final String COIN_ICON = "assets/images/ui/buttons_coin_buy_normal.png";
    private static final String GEM_ICON = "assets/images/ui/buttons_premium_normal.png";

    private static final Color YELLOW = new Color(0.95f, 0.80f, 0.15f, 1f);
    private static final Color GREEN = new Color(0.30f, 0.75f, 0.25f, 1f);
    private static final Color GRAY = new Color(0.45f, 0.45f, 0.45f, 1f);
    private static final Color PURPLE = new Color(0.60f, 0.30f, 0.85f, 1f);

    private static final int PLANT_GRID_COLUMNS = 6;

    private final SeedPacketCardFactory cardFactory = new SeedPacketCardFactory();
    private final CollectionManager collectionManager = new CollectionManager();
    private final List<Actor> cards = new ArrayList<>();
    private Table activePopup = null;

    private String previewPlantName = null;

    private TextureBank textureBank;
    private PamPlayer pamPlayer;
    private Map<String, Boolean> visibility = new HashMap<>();
    private Texture roundedAnimBgTexture;
    private Drawable roundedAnimBgDrawable;
    private final TextureRegion whitePixel = whitePixelRegion();

    @Override
    public void show() {
        AudioManager.get().stopMusic();
        setBackground(BACKGROUND);
        if (textureBank == null) {
            try {
                FileHandle rootHandle = Gdx.files.internal("assets/pvz-assets");
                textureBank = new TextureBank("atlases", rootHandle);
                pamPlayer = new PamPlayer(textureBank, rootHandle);
            } catch (Throwable t) {
                Gdx.app.error("BeforeMatchScreen", "Failed to initialize PAM System", t);
            }
        }
        super.show();
        build();
    }

    @Override
    public void render(float delta) {
        if (textureBank != null) {
            try {
                textureBank.update();
            } catch (Throwable t) {
                Gdx.app.error("BeforeMatchScreen", "textureBank.update() failed", t);
            }
        }
        super.render(delta);
    }

    private void build() {
        rootTable.clear();
        cards.clear();

        Level level = GameSession.peekInstance() == null ? null : GameSession.peekInstance().getLevel();
        if (level == null) {
            rootTable.add(new Label("No level loaded.", skin, "title")).center();
            return;
        }

        Table topBar = buildTopBar(level);
        Table middle = buildMiddleSection(level);
        Table bottom = buildBottomBar();

        // Same three-row shape as every other screen (e.g. EgyptStagesScreen): a fillX
        // header, a middle row that actually receives the leftover vertical space via
        // expand(), and a fillX footer - added straight to rootTable (already fixed at
        // SCREEN_WIDTH x SCREEN_HEIGHT via setFillParent(true) in BaseScreen), not nested
        // inside a self-padded wrapper table that has to guess its own height budget.
        rootTable.add(topBar).fillX().padTop(5).padLeft(15).padRight(15).row();
        rootTable.add(middle).expand().center().padTop(SPACE_SM).row();
        rootTable.add(bottom).fillX().padBottom(SPACE_SM);

        // Same as EgyptStagesScreen: the middle row's ScrollPane content can otherwise
        // paint over these two in the actor draw order even though the Table cells
        // themselves are correctly bounded - push them to the front explicitly.
        topBar.toFront();
        bottom.toFront();
    }

    private Table buildTopBar(Level level) {
        Table topBar = new Table();

        ImageButton back = createIconButton(BACK_ICON, 44, 44, () -> runCommand("menu exit"));

        Table topLeft = new Table();
        topLeft.add(back).padRight(SPACE_MD);
        Label nameLabel = new Label(level.getName(), skin, "title");
        nameLabel.setFontScale(0.9f);
        topLeft.add(nameLabel);

        User user = User.currentUser;
        int coins = (user != null && user.userState != null) ? user.userState.coins : 0;
        int diamonds = (user != null && user.userState != null) ? user.userState.diamonds : 0;

        Table topRight = new Table();
        topRight.add(createResourceWidget(COIN_ICON, String.valueOf(coins))).padRight(SPACE_SM);
        topRight.add(createResourceWidget(GEM_ICON, String.valueOf(diamonds)));

        topBar.add(topLeft).left().expandX();
        topBar.add(topRight).right();
        return topBar;
    }

    private Table buildMiddleSection(Level level) {
        Table board = new Table();
        board.setBackground(new TextureRegionDrawable(loadTextureSafe(BOARD)));
        board.pad(SPACE_MD);
        board.top();

        String mode = level.getGameMode() == null ? "Adventure" : level.getGameMode();
        Label description = new Label(mode + " - Select up to 7 plants (+1 rented). Forced plants are added automatically.",
                skin, "main");
        description.setAlignment(Align.center);
        description.setFontScale(0.8f);
        description.setWrap(true);
        board.add(description).colspan(2).fillX().padBottom(SPACE_XS).row();

        if (level instanceof ConveyorBeltLevel) {
            board.add(buildConveyorNotice()).colspan(2).fillX().padBottom(SPACE_XS).row();
        }

        // Both columns get expandY().fillY() so they share whatever height 'board' is
        // actually given by its own cell in build() (bounded via rootTable's expand()
        // row), instead of each guessing its own preferred height independently -
        // that mismatch (grid taller than its slot) was what pushed the header and
        // Start button off-screen before.
        board.add(buildLoadoutPanel(level)).expandY().fillY().top().left().padRight(SPACE_MD);
        board.add(buildRightPanel(level)).expand().fill().top().left();

        Table sized = new Table();
        sized.add(board).width(1200f).height(600f);
        return sized;
    }

    private Table buildRightPanel(Level level) {
        Table rightPanel = new Table();
        rightPanel.top().left();
        rightPanel.add(buildPreviewPanel()).fillX().padBottom(SPACE_SM).row();
        rightPanel.add(buildPlantGrid(level)).expand().fill();
        return rightPanel;
    }

    private Table buildBottomBar() {
        Table bottom = new Table();
        TextButton start = primaryButton("Let's Rock!", () -> runCommand("start game"));
        bottom.add(start).right().expandX().width(200).height(48);
        return bottom;
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

    private Table buildConveyorNotice() {
        Table t = new Table();
        t.setBackground(skin.getDrawable("card-background"));
        Label l = new Label("CONVEYOR BELT\nPlants arrive during the match; manual loadout is not required.",
                skin, "main");
        l.setAlignment(Align.center);
        l.setWrap(true);
        t.add(l).expandX().fillX().pad(4);
        return t;
    }

    private Table buildPreviewPanel() {
        Table box = new Table();
        box.setBackground(skin.getDrawable("card-background"));
        box.pad(8).top().left();


        List<PlantJsonParser.PlantConfig> allPlants = collectionManager.getAllPlants();
        PlantJsonParser.PlantConfig config = null;
        if (previewPlantName != null) {
            for (PlantJsonParser.PlantConfig c : allPlants) {
                if (c.name.equalsIgnoreCase(previewPlantName)) {
                    config = c;
                    break;
                }
            }
        }
        if (config == null && !allPlants.isEmpty()) {
            config = allPlants.get(0);
            previewPlantName = config.name;
        }

        if (config == null) {
            box.add(new Label("Select a plant to preview", skin, "main"));
            return box;
        }

        UserState state = User.currentUser != null ? User.currentUser.userState : null;
        boolean unlocked = state != null && state.isPlantUnlocked(config.id);
        int level = state != null ? Math.max(1, state.getPlantLevel(config.id)) : 1;

        Stack animBox = new Stack();
        animBox.add(new Image(getRoundedAnimBgDrawable()));
        animBox.add(new Image(roundedBorderDrawable(Color.WHITE, 2f, 12f)));

        String animationPath = null;
        try {
            animationPath = AnimationFactory.pathForDisplayName(config.name);
        } catch (Throwable ignored) {}

        PlantIdleAnimationActor animActor = new PlantIdleAnimationActor(animationPath, -10f, -15f);
        Table animLayer = new Table();
        animLayer.setFillParent(true);
        animLayer.add(animActor).size(120, 120);
        animBox.add(animLayer);

        box.add(animBox).size(200, 200).padRight(12).top();

        Table infoTable = new Table();
        infoTable.top().left();

        Label nameLabel = new Label(config.name + " (Lv. " + level + ")", skin, "title");
        nameLabel.setFontScale(0.9f);
        infoTable.add(nameLabel).left().padBottom(2).row();

        Label catLabel = new Label("Type: " + config.category, skin, "main");
        catLabel.setFontScale(0.75f);
        infoTable.add(catLabel).left().row();

        Label statsLabel = new Label("HP: " + config.baseHp + " | Dmg: " + config.damage + " | Rec: " + config.recharge + "s", skin, "main");
        statsLabel.setFontScale(0.75f);
        infoTable.add(statsLabel).left().row();

        String tagsStr = (config.tags == null || config.tags.isEmpty())
                ? "None"
                : config.tags.stream().map(Enum::name).collect(Collectors.joining(", "));
        Label tagsLabel = new Label("Tags: " + tagsStr, skin, "muted");
        tagsLabel.setFontScale(0.75f);
        infoTable.add(tagsLabel).left().row();

        box.add(infoTable).expandX().fillX().top().padRight(12);

        Table actionsTable = new Table();
        actionsTable.bottom();

        if (unlocked && state != null) {
            int coinCost = level * 500;
            int packetsNeeded = level;
            int packetsOwned = state.seedPacketInventory.getOrDefault(config.id, 0);
            boolean canUpgrade = state.coins >= coinCost && packetsOwned >= packetsNeeded;

            String upgText = "Upgrade Lv." + (level + 1) + "\n" + coinCost + " Coins (" + packetsOwned + "/" + packetsNeeded + ")";
            TextButton upgBtn = coloredButton(upgText, canUpgrade ? GREEN : GRAY, 0.7f);
            upgBtn.setDisabled(!canUpgrade);
            final String plantName = config.name;
            if (canUpgrade) {
                upgBtn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        runCommand("upgrade plant -t " + plantName);
                        build();
                    }
                });
            }
            actionsTable.add(upgBtn).width(155).height(44);
        }

        int diamonds = state != null ? state.diamonds : 0;
        boolean canBoost = diamonds >= 10;
        TextButton boostBtn = coloredButton("Boost Plant\n(10 Diamonds)", canBoost ? PURPLE : GRAY, 0.7f);
        boostBtn.setDisabled(!canBoost);
        final String pName = config.name;
        boostBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (state != null && state.diamonds >= 10) {
                    state.diamonds -= 10;
                    showNoticePopup("Plant Boosted!", pName + " is boosted for this match!");
                    build();
                } else {
                    showNoticePopup("Insufficient Diamonds", "You need 10 diamonds to boost this plant.");
                }
            }
        });
        actionsTable.add(boostBtn).width(155).height(44).right().padLeft(10);

        box.add(actionsTable).left();

        return box;
    }

    private Actor buildPlantGrid(Level level) {
        UserState state = User.currentUser != null ? User.currentUser.userState : null;

        if (level instanceof ConveyorBeltLevel conveyor) {
            List<String> names = new ArrayList<>();
            if (conveyor.getConveyorPlants() != null) {
                for (var plant : conveyor.getConveyorPlants()) {
                    if (plant != null && plant.getName() != null) {
                        names.add(plant.getName());
                    }
                }
            }
            return buildGridContainer(names, name -> false, name -> false);
        }

        List<PlantJsonParser.PlantConfig> allPlants = collectionManager.getAllPlants();
        List<String> names = new ArrayList<>();
        for (PlantJsonParser.PlantConfig config : allPlants) {
            names.add(config.name);
        }

        if (level instanceof LockedPlantsLevel lockedLevel) {
            return buildLockedPlantsGrid(names, lockedLevel);
        }

        return buildGridContainer(names,
                name -> state != null && !state.isPlantUnlocked(findId(allPlants, name)),
                name -> state != null && !state.isPlantUnlocked(findId(allPlants, name)));
    }

    private Actor buildLockedPlantsGrid(List<String> names, LockedPlantsLevel lockedLevel) {
        Table grid = new Table();
        grid.top();

        Table currentRow = null;
        for (int i = 0; i < names.size(); i++) {
            if (i % PLANT_GRID_COLUMNS == 0) {
                currentRow = new Table();
                grid.add(currentRow).center().padBottom(SPACE_SM).row();
            }
            String name = names.get(i);
            boolean locked = isPlantLockedInLevel(lockedLevel, name);
            Actor card = buildCard(name, locked, locked);
            currentRow.add(card).padLeft(SPACE_XS).padRight(SPACE_XS);
        }

        Table viewport = new Table();
        viewport.top().left();
        viewport.add(scrollable(grid)).expand().fill().top().left();
        return viewport;
    }

    private boolean isPlantLockedInLevel(LockedPlantsLevel lockedLevel, String plantName) {
        if (lockedLevel == null || lockedLevel.getLockedPlants() == null) {
            return false;
        }
        for (Object item : lockedLevel.getLockedPlants()) {
            if (item != null && item.toString().equalsIgnoreCase(plantName)) {
                return true;
            }
        }
        return false;
    }

    private int findId(List<PlantJsonParser.PlantConfig> plants, String name) {
        for (PlantJsonParser.PlantConfig config : plants) {
            if (config.name.equalsIgnoreCase(name)) return config.id;
        }
        return -1;
    }

    private Actor buildGridContainer(List<String> names, java.util.function.Predicate<String> darkened,
                                     java.util.function.Predicate<String> showLockIcon) {
        Table grid = new Table();
        grid.top();

        Table currentRow = null;
        for (int i = 0; i < names.size(); i++) {
            if (i % PLANT_GRID_COLUMNS == 0) {
                currentRow = new Table();
                grid.add(currentRow).center().padBottom(SPACE_SM).row();
            }
            String name = names.get(i);
            boolean isDarkened = darkened.test(name);
            Actor card = buildCard(name, isDarkened, showLockIcon.test(name));
            currentRow.add(card).padLeft(SPACE_XS).padRight(SPACE_XS);
        }

        Table gridContainer = new Table();
        gridContainer.top().left();
        gridContainer.add(scrollable(grid)).expand().fill().top().left();
        return gridContainer;
    }

    private Actor buildCard(String name, boolean darkened, boolean showLockIcon) {
        Stack cardStack = new Stack();

        float cardW = 108f;
        float cardH = cardW * 1.38f;

        try {
            SeedPacketCard card = cardFactory.buildCardForDisplayName(name);
            if (card != null) {
                cardW = card.getWidth();
                cardH = card.getHeight();
                cardStack.add(card);
            }
        } catch (Throwable ignored) {
        }

        cardStack.setSize(cardW, cardH);

        if (cardStack.getChildren().isEmpty()) {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            Label label = new Label(name, skin, "main");
            label.setAlignment(Align.center);
            label.setFontScale(0.8f);
            label.setWrap(true);
            fallback.add(label).size(cardW, cardH);
            cardStack.add(fallback);
        }

        if (darkened) {
            Image dim = new Image(solidColorDrawable(new Color(0f, 0f, 0f, 0.65f)));
            dim.setScaling(Scaling.stretch);
            cardStack.add(dim);
        }

        if (showLockIcon) {
            Image lockImage = new Image(loadTextureSafe(LOCK_ICON));
            lockImage.setScaling(Scaling.stretch);
            Container<Image> lockContainer = new Container<>(lockImage);
            lockContainer.size(22f, 22f);
            lockContainer.center();
            cardStack.add(lockContainer);
        }

        Table cell = new Table();
        cell.add(cardStack).size(cardW, cardH);
        cards.add(cell);

        cardStack.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                previewPlantName = name;
                if (!darkened) {
                    togglePlant(name);
                } else {
                    build();
                }
            }
        });

        return cell;
    }

    private Table buildLoadoutPanel(Level level) {
        Table panel = new Table();
        panel.setBackground(skin.getDrawable("card-background"));
        panel.pad(8).top();

        float sampleW = 100f;
        float sampleH = 138f;
        try {
            SeedPacketCard sampleCard = cardFactory.buildCardForDisplayName("Peashooter");
            if (sampleCard != null && sampleCard.getWidth() > 0 && sampleCard.getHeight() > 0) {
                sampleW = sampleCard.getWidth();
                sampleH = sampleCard.getHeight();
            }
        } catch (Throwable ignored) {}

        // Sized to comfortably fit all 8 slots (7 loadout + 1 rent) without needing to
        // scroll on the board's current 600px height budget - was 76f before, which
        // needed 684px total (8 slots + panel padding + counter label) and silently
        // overflowed the board's actual ~500px available height for this column.
        float targetH = 58f;
        float scale = targetH / sampleH;
        float targetW = sampleW * scale;

        Table slots = new Table();
        slots.top();

        for (int i = 0; i < 7; i++) {
            String name = i < BeforeMenu.selectedPlants.size()
                    ? BeforeMenu.selectedPlants.get(i) : "Empty";

            final String selected = name;

            if ("Empty".equals(name)) {
                Actor emptySlot = createEmptyLoadoutSlot("Empty", targetW, targetH, false, null);
                slots.add(emptySlot).size(targetW, targetH).pad(2f).row();
            } else {
                Actor cardActor = createScaledLoadoutCard(name, scale, targetW, targetH, new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        previewPlantName = selected;
                        removePlant(selected);
                    }
                });
                slots.add(cardActor).size(targetW, targetH).pad(2f).row();
            }
        }

        if (BeforeMenu.selectedPlants.size() == 8) {
            String rentedPlantName = BeforeMenu.selectedPlants.get(7);
            Actor rentedCardActor = createScaledLoadoutCard(rentedPlantName, scale, targetW, targetH, new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    previewPlantName = rentedPlantName;
                    removePlant(rentedPlantName);
                }
            });
            slots.add(rentedCardActor).size(targetW, targetH).padTop(4f).padBottom(2f).row();
        } else {
            Actor rentSlot = createEmptyLoadoutSlot("Rent", targetW, targetH, true, new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (BeforeMenu.selectedPlants.size() < 7) {
                        showNoticePopup("Loadout Not Full", "You must fill all 7 loadout slots before renting a plant!");
                    } else {
                        showRentPlantPopup();
                    }
                }
            });
            slots.add(rentSlot).size(targetW, targetH).padTop(4f).padBottom(2f).row();
        }

        panel.add(scrollable(slots)).expand().fill().row();

        int currentSelected = Math.min(BeforeMenu.selectedPlants.size(), 7);
        Label info = new Label(currentSelected + "/7", skin, "main");
        info.setAlignment(Align.center);
        info.setFontScale(0.8f);
        panel.add(info).padTop(6);

        return panel;
    }

    private Actor createScaledLoadoutCard(String plantName, float scale, float targetW, float targetH, ClickListener clickListener) {
        SeedPacketCard card = null;
        try {
            card = cardFactory.buildCardForDisplayName(plantName);
        } catch (Throwable ignored) {}

        Container<Actor> wrapper = new Container<>();
        wrapper.size(targetW, targetH);

        if (card != null) {
            card.setTransform(true);
            card.setOrigin(0, 0);
            card.setScale(scale);
            wrapper.setActor(card);
            wrapper.top().left();
        } else {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            Label label = new Label(plantName, skin, "main");
            label.setAlignment(Align.center);
            label.setFontScale(0.65f);
            label.setWrap(true);
            fallback.add(label).width(targetW).center();
            wrapper.setActor(fallback);
        }

        if (clickListener != null) {
            wrapper.addListener(clickListener);
        }

        return wrapper;
    }

    private Actor createEmptyLoadoutSlot(String labelText, float targetW, float targetH, boolean isRent, ClickListener clickListener) {
        Table slot = new Table();
        slot.setBackground(skin.getDrawable("card-background"));

        Stack stack = new Stack();
        Label label = new Label(labelText, skin, "main");
        label.setAlignment(Align.center);
        label.setColor(isRent ? Color.WHITE : Color.LIGHT_GRAY);
        label.setFontScale(0.7f);
        stack.add(label);

        if (isRent) {
            Image lockImg = new Image(loadTextureSafe(LOCK_ICON));
            lockImg.setScaling(Scaling.fit);
            Container<Image> lockCont = new Container<>(lockImg);
            lockCont.size(16f, 16f);
            lockCont.right().bottom().pad(1);
            stack.add(lockCont);
        }

        slot.add(stack).expand().fill();

        Container<Table> wrapper = new Container<>(slot);
        wrapper.size(targetW, targetH);
        if (clickListener != null) {
            wrapper.addListener(clickListener);
        }
        return wrapper;
    }

    private void showRentPlantPopup() {
        if (activePopup != null) {
            activePopup.remove();
            activePopup = null;
        }

        List<PlantJsonParser.PlantConfig> allPlants = collectionManager.getAllPlants();
        List<String> availableToRent = new ArrayList<>();
        for (PlantJsonParser.PlantConfig config : allPlants) {
            if (BeforeMenu.selectedPlants.stream().noneMatch(p -> p.equalsIgnoreCase(config.name))) {
                availableToRent.add(config.name);
            }
        }

        if (availableToRent.isEmpty()) {
            showNoticePopup("No Plants Available", "All plants are already selected.");
            return;
        }

        Collections.shuffle(availableToRent);
        List<String> rentOptions = availableToRent.subList(0, Math.min(3, availableToRent.size()));

        Table popupOverlay = new Table();
        popupOverlay.setFillParent(true);

        Image bgDim = new Image(solidColorDrawable(new Color(0f, 0f, 0f, 0.75f)));
        bgDim.setFillParent(true);

        Stack overlayStack = new Stack();
        overlayStack.add(bgDim);

        Table window = new Table();
        window.setBackground(skin.getDrawable("card-background"));
        window.pad(16);

        Label title = new Label("RENT A PLANT (600 Coins)", skin, "title");
        title.setAlignment(Align.center);
        window.add(title).padBottom(8).row();

        Label coinLabel = new Label("Your Coins: " + getUserCoins(), skin, "main");
        coinLabel.setColor(Color.GOLD);
        window.add(coinLabel).padBottom(12).row();

        Table optionsTable = new Table();
        for (String plantName : rentOptions) {
            Table cardBox = new Table();
            cardBox.setBackground(skin.getDrawable("card-background"));
            cardBox.pad(8);

            Actor plantCard = buildCard(plantName, false, false);
            cardBox.add(plantCard).size(102f, 140f).row();

            TextButton rentBtn = new TextButton("Rent (600)", skin);
            rentBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (getUserCoins() < 600) {
                        coinLabel.setText("Not enough coins! Need 600.");
                        coinLabel.setColor(Color.RED);
                        return;
                    }
                    if (tryDeductCoins(600)) {
                        BeforeMenu.selectedPlants.add(plantName);
                        popupOverlay.remove();
                        activePopup = null;
                        build();
                    }
                }
            });
            cardBox.add(rentBtn).width(110).height(38).padTop(6);

            optionsTable.add(cardBox).pad(6);
        }
        window.add(optionsTable).padBottom(12).row();

        TextButton cancelBtn = new TextButton("Cancel", skin);
        cancelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                popupOverlay.remove();
                activePopup = null;
            }
        });
        window.add(cancelBtn).width(130).height(40);

        Container<Table> windowContainer = new Container<>(window);
        windowContainer.center();
        overlayStack.add(windowContainer);

        popupOverlay.add(overlayStack).expand().fill();

        getModalStack().add(popupOverlay);
        activePopup = popupOverlay;
    }

    private void showNoticePopup(String titleText, String msgText) {
        Table popupOverlay = new Table();
        popupOverlay.setFillParent(true);

        Image bgDim = new Image(solidColorDrawable(new Color(0f, 0f, 0f, 0.75f)));
        bgDim.setFillParent(true);

        Stack overlayStack = new Stack();
        overlayStack.add(bgDim);

        Table window = new Table();
        window.setBackground(skin.getDrawable("card-background"));
        window.pad(16);

        Label title = new Label(titleText, skin, "title");
        window.add(title).padBottom(8).row();

        Label msg = new Label(msgText, skin, "main");
        msg.setWrap(true);
        msg.setAlignment(Align.center);
        window.add(msg).width(300).padBottom(12).row();

        TextButton okBtn = new TextButton("OK", skin);
        okBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                popupOverlay.remove();
            }
        });
        window.add(okBtn).width(100).height(40);

        Container<Table> windowContainer = new Container<>(window);
        windowContainer.center();
        overlayStack.add(windowContainer);

        popupOverlay.add(overlayStack).expand().fill();
        getModalStack().add(popupOverlay);
    }

    private int getUserCoins() {
        if (User.currentUser != null && User.currentUser.userState != null) {
            return User.currentUser.userState.coins;
        }
        return 0;
    }

    private boolean tryDeductCoins(int amount) {
        if (User.currentUser != null && User.currentUser.userState != null) {
            if (User.currentUser.userState.coins >= amount) {
                User.currentUser.userState.coins -= amount;
                return true;
            }
        }
        return false;
    }

    private void togglePlant(String name) {
        Level level = GameSession.peekInstance() == null ? null : GameSession.peekInstance().getLevel();
        if (BeforeMenu.selectedPlants.stream().anyMatch(p -> p.equalsIgnoreCase(name))) {
            removePlant(name);
            return;
        }
        if (BeforeMenu.selectedPlants.size() >= 7) {
            build();
            return;
        }
        if (level instanceof LockedPlantsLevel) {
            BeforeMenu.selectedPlants.add(name);
            build();
            return;
        }
        runCommand("add plant -t " + name);
        build();
    }

    private void removePlant(String name) {
        Level level = GameSession.peekInstance() == null ? null : GameSession.peekInstance().getLevel();
        if (level instanceof LockedPlantsLevel) {
            BeforeMenu.selectedPlants.removeIf(p -> p.equalsIgnoreCase(name));
            build();
            return;
        }
        runCommand("remove plant -t " + name);
        build();
    }

    protected Table createResourceWidget(String iconPath, String value) {
        Stack stack = new Stack();
        Table bgTable = new Table();
        bgTable.setBackground(new TextureRegionDrawable(loadTextureSafe(iconPath)));

        Table textTable = new Table();
        textTable.add(new Label(value, skin, "title")).center().expand();

        stack.add(bgTable);
        stack.add(textTable);

        Table outer = new Table();
        outer.add(stack).size(120, 38);
        return outer;
    }

    private TextButton coloredButton(String text, Color color, float fontScale) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = skin.getFont("default-font");
        style.fontColor = Color.BLACK;
        style.up = roundedFilledDrawable(color, Color.WHITE, 12f, 2f);
        style.down = roundedFilledDrawable(color.cpy().mul(0.85f, 0.85f, 0.85f, 1f), Color.WHITE, 12f, 2f);
        style.disabled = roundedFilledDrawable(GRAY, Color.WHITE, 12f, 2f);
        style.disabledFontColor = new Color(0.8f, 0.8f, 0.8f, 1f);
        TextButton btn = new TextButton(text, style);
        btn.getLabel().setFontScale(fontScale);
        btn.getLabel().setAlignment(Align.center);
        return btn;
    }

    private Drawable getRoundedAnimBgDrawable() {
        if (roundedAnimBgDrawable == null) {
            roundedAnimBgDrawable = createRoundedTextureDrawable("assets/images/ui/collection/card_plant_bg_modern.png", 14f);
        }
        return roundedAnimBgDrawable;
    }

    private Drawable createRoundedTextureDrawable(String path, float screenRadius) {
        if (path != null && !path.isEmpty() && Gdx.files.internal(path).exists()) {
            Pixmap src = new Pixmap(Gdx.files.internal(path));
            int w = src.getWidth();
            int h = src.getHeight();
            Pixmap dst = new Pixmap(w, h, Pixmap.Format.RGBA8888);
            dst.setBlending(Pixmap.Blending.None);

            float r = screenRadius * ((float) w / 140f);
            r = Math.min(r, Math.min(w, h) / 2f);

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float d = roundedRectDistance(x + 0.5f, y + 0.5f, w, h, r);
                    if (d <= 0f) {
                        dst.drawPixel(x, y, src.getPixel(x, y));
                    } else {
                        dst.drawPixel(x, y, 0);
                    }
                }
            }
            src.dispose();
            if (roundedAnimBgTexture != null) {
                roundedAnimBgTexture.dispose();
            }
            roundedAnimBgTexture = new Texture(dst);
            roundedAnimBgTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            dst.dispose();
            return new TextureRegionDrawable(new TextureRegion(roundedAnimBgTexture));
        }
        return solidColorDrawable(new Color(0f, 0f, 0f, 0f));
    }

    private Drawable roundedFilledDrawable(Color fill, Color border, float radius, float thickness) {
        return roundedDrawable(fill, border, radius, thickness, true);
    }

    private Drawable roundedBorderDrawable(Color border, float thickness, float radius) {
        return roundedDrawable(new Color(1f, 1f, 1f, 0f), border, radius, thickness, false);
    }

    private Drawable roundedDrawable(Color fill, Color border, float radius, float thickness, boolean filled) {
        int size = 64;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float r = Math.min(radius, size / 2f - 1f);
        float t = Math.max(1f, thickness);
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                float d = roundedRectDistance(px + 0.5f, py + 0.5f, size, size, r);
                boolean inside = d <= 0f;
                boolean borderPixel = d <= 0f && d >= -t;
                if (inside && filled) {
                    pixmap.setColor(borderPixel ? border : fill);
                    pixmap.drawPixel(px, py);
                } else if (borderPixel) {
                    pixmap.setColor(border);
                    pixmap.drawPixel(px, py);
                } else {
                    pixmap.setColor(0f, 0f, 0f, 0f);
                    pixmap.drawPixel(px, py);
                }
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(texture, 18, 18, 18, 18));
    }

    private float roundedRectDistance(float x, float y, float w, float h, float radius) {
        float cx = Math.max(radius, Math.min(x, w - radius));
        float cy = Math.max(radius, Math.min(y, h - radius));
        float dx = x - cx;
        float dy = y - cy;
        return (float) Math.sqrt(dx * dx + dy * dy) - radius;
    }

    private Drawable solidColorDrawable(Color color) {
        Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    protected static TextureRegion whitePixelRegion() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegion(texture);
    }

    private class PlantIdleAnimationActor extends Actor {
        private final String animationPath;
        private float stateTime = 0f;
        private final float offsetX;
        private final float offsetY;

        PlantIdleAnimationActor(String animationPath, float ox, float oy) {
            this.animationPath = animationPath;
            this.offsetX = ox + 60f;
            this.offsetY = oy + 45f;
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (pamPlayer == null || animationPath == null) {
                return;
            }
            try {
                String clipName = AnimationFactory.resolveClipNameForPath(animationPath, "idle");
                if (clipName == null) {
                    return;
                }

                ClipRef clip = pamPlayer.getClip(animationPath, clipName);
                if (!animationPath.contains("MAGNETSHROOM")) {
                    if (clip != null) {
                        pamPlayer.draw(batch, clip, stateTime, getX() + offsetX, getY() + offsetY, true);
                    }
                } else {
                    visibility.put("Magnet_Item", false);
                    if (clip != null) {
                        pamPlayer.draw(batch, clip, stateTime, getX() + offsetX, getY() + offsetY, true, visibility);
                    }
                }
            } catch (Throwable t) {
                Gdx.app.error("BeforeMatchScreen", "Failed to draw idle animation for " + animationPath, t);
            }
        }
    }

    @Override
    protected void onAfterCommand() {
        build();
    }

    @Override
    public void dispose() {
        cardFactory.dispose();
        if (roundedAnimBgTexture != null) {
            roundedAnimBgTexture.dispose();
        }
        super.dispose();
    }
}