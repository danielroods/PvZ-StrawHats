package view.screens.match.before;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
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
import controller.assets.GameAssetManager;
import controller.match.BeforeMenu;
import model.App;
import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.plant.PlantJsonParser;
import model.collections.plant.PlantProgression;
import model.collections.plant.PlantStats;
import view.screens.ui_menus.PlantStatRows;
import model.match.main.levels.Level;
import model.match.main.levels.special_levels.ConveyorBeltLevel;
import model.match.main.levels.special_levels.LockedPlantsLevel;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.GameSession;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;
import view.screens.generals.GameScreen;

import pvz.libpvz.pam.ClipRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BeforeMatchScreen extends GameScreen {

    protected static final float PREVIEW_ZOMBIE_SCALE = 0.46f;
    protected static final float PREVIEW_ZOMBIE_GAP_X = 52f;

    protected static final float PREVIEW_GROUP_SHIFT_X = 40f;


    protected static final float LOADOUT_CARD_W = 95f;
    protected static final float LOADOUT_CARD_H = 60f;

    protected static final String LOADOUT_SLOT_BG = "assets/images/ui/cooldown.png";
    protected static final String PREVIEW_PANEL_BG = "assets/images/ui/quest_panel_daily.png";
    protected static final String BACK_ICON = "assets/images/ui/buttons_hud_back_normal.png";
    protected static final String LOCK_ICON = "assets/images/ui/collection/lock_small_gold.png";
    protected static final String COIN_ICON = "assets/images/ui/buttons_coin_buy_normal.png";
    protected static final String GEM_ICON = "assets/images/ui/buttons_premium_normal.png";

    protected static final Color GREEN = new Color(0.30f, 0.75f, 0.25f, 1f);
    protected static final Color GRAY = new Color(0.45f, 0.45f, 0.45f, 1f);
    protected static final Color PURPLE = new Color(0.60f, 0.30f, 0.85f, 1f);

    protected static final int PLANT_GRID_COLUMNS = 6;

    protected final SeedPacketCardFactory cardFactory = new SeedPacketCardFactory();
    protected final CollectionManager collectionManager = new CollectionManager();
    protected final List<Actor> cards = new ArrayList<>();
    protected Table activePopup = null;

    protected String previewPlantName = null;

    protected Map<String, Boolean> visibility = new HashMap<>();
    protected Texture roundedAnimBgTexture;
    protected Drawable roundedAnimBgDrawable;
    protected Texture rightPreviewTexture;
    protected float previewAnimationTime;
    protected Table startOverlay;

    @Override
    public void show() {
        configureSeasonFolder();
        previewAnimationTime = 0f;
        super.show();

        loadRightPreviewTexture();
        build();
    }

    protected void configureSeasonFolder() {
        GameSession gameSession = GameSession.peekInstance();
        Level level = gameSession == null ? null : gameSession.getLevel();
        String season = level == null || level.getSeason() == null
                ? "" : level.getSeason().getName();

        if ("Egypt".equalsIgnoreCase(season)) {
            seasonFolder = "egypt";
        } else if ("Big Wave Beach".equalsIgnoreCase(season)) {
            seasonFolder = "beach";
        } else if ("Frostbite Caves".equalsIgnoreCase(season)
                || "Frostbite Cave".equalsIgnoreCase(season)) {
            seasonFolder = "frostbite_cave";
        } else if ("Dark Ages".equalsIgnoreCase(season)) {
            seasonFolder = "darkage";
        } else if ("Pirates".equalsIgnoreCase(season)) {
            seasonFolder = "pirate";
        } else if ("Future".equalsIgnoreCase(season)
                || "Far Future".equalsIgnoreCase(season)) {
            seasonFolder = "future";
        }
    }

    @Override
    protected void createHud() {
    }

    @Override
    protected void createBoardInput() {
    }

    @Override
    protected void createZombossDialogue() {
    }

    @Override
    protected void tickSession() {
    }

    @Override
    protected boolean isBeforeMatchPreview() {
        return true;
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        previewAnimationTime += Math.max(0f, delta);

        float x = getBoardRight();
        float viewW = stage.getViewport().getWorldWidth();
        float viewH = stage.getViewport().getWorldHeight();
        float width = viewW - x;

        if (width > 0f) {
            batch.setColor(Color.WHITE);
            if (rightPreviewTexture != null) {
                float texW = rightPreviewTexture.getWidth();
                float texH = rightPreviewTexture.getHeight();
                float scale = viewH / texH;
                float drawW = texW * scale;


                float bgOffsetRight = 390f;
                float drawX = viewW - drawW + bgOffsetRight;

                batch.draw(rightPreviewTexture, drawX, 0f, drawW, viewH);
            } else {
                batch.setColor(0.08f, 0.07f, 0.05f, 0.94f);
                batch.draw(whitePixel, x, 0f, width, viewH);
                batch.setColor(Color.WHITE);
            }
        }
    }

    @Override
    protected void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        drawPreviewZombies();
    }

    protected String resolveExistingAssetPath(String path) {
        if (path == null) return "";
        if (Gdx.files.internal(path).exists()) return path;
        if (path.startsWith("assets/")) {
            String noPrefix = path.substring("assets/".length());
            if (Gdx.files.internal(noPrefix).exists()) return noPrefix;
        } else {
            String withPrefix = "assets/" + path;
            if (Gdx.files.internal(withPrefix).exists()) return withPrefix;
        }
        return path;
    }

    protected void loadRightPreviewTexture() {
        if (rightPreviewTexture != null) {
            rightPreviewTexture.dispose();
            rightPreviewTexture = null;
        }

        String[] candidates = {
                "assets/images/chapters/" + seasonFolder + "/gameplay/texture_right.png",
                "assets/images/chapters/" + seasonFolder + "/gameplay/right.png",
                "assets/images/chapters/" + seasonFolder + "/gameplay/right_texture.png",
                "assets/images/chapters/" + seasonFolder + "/gameplay/map_right.png",
                "assets/images/chapters/" + seasonFolder + "/right.png",
                "assets/images/chapters/" + seasonFolder + "/gameplay/zombie_area.png"
        };

        for (String candidate : candidates) {
            String resolved = resolveExistingAssetPath(candidate);
            if (resolved != null && !resolved.isBlank() && Gdx.files.internal(resolved).exists()) {
                try {
                    rightPreviewTexture = new Texture(Gdx.files.internal(resolved));
                    rightPreviewTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    return;
                } catch (Throwable t) {
                    Gdx.app.error("BeforeMatchScreen",
                            "Failed to load right-side preview texture: " + resolved, t);
                }
            }
        }
    }




    protected static final String ZOMBIE_JESTER_ALIAS = "ZombieDarkJuggler";

    protected static final String ZOMBIE_PIANO_ALIAS = "ZombiePiano";
    protected static final String PIANO_PROP_PAM = "768/FULL/ZOMBIE/PIANO/PIANO.PAM";
    protected static final float PIANO_PREVIEW_OFFSET_X = 46f;
    protected static final float PIANO_PREVIEW_OFFSET_Y = -6f;

    protected static final String ZOMBIE_ARCADE_ALIAS = "ZombieArcade";
    protected static final String ARCADE_PROP_PAM = "768/FULL/EFFECTS/80S_ARCADE_CABINET/80S_ARCADE_CABINET.PAM";
    protected static final float ARCADE_PREVIEW_OFFSET_X = -70f;
    protected static final float ARCADE_PREVIEW_OFFSET_Y = 6f;
    protected static final float ARCADE_PREVIEW_SCALE = 0.6f;

    protected void drawPreviewZombies() {
        Level level = session == null ? null : session.getLevel();
        if (level == null || level.getZombiePool() == null || level.getZombiePool().isEmpty()) return;

        Set<String> uniqueAliases = new LinkedHashSet<>();
        for (String alias : level.getZombiePool()) {
            if (alias == null || alias.isBlank()) continue;
            if (isBossPreviewAlias(alias)) continue;
            uniqueAliases.add(alias);
        }

        if (uniqueAliases.isEmpty()) return;

        float rightX = getBoardRight();
        float viewW = stage.getViewport().getWorldWidth();
        float areaW = Math.max(1f, viewW - rightX);

        float centerX = rightX + areaW * 0.5f + PREVIEW_GROUP_SHIFT_X;



        List<String> aliasList = new ArrayList<>(uniqueAliases);
        int totalRows = Math.max(1, session.getRows());
        int[] usableRows = middlePreviewRows(totalRows);
        int rowCount = usableRows.length;

        List<int[]> slots = new ArrayList<>();
        for (int i = 0; i < aliasList.size(); i++) {
            int row = usableRows[i % rowCount];
            int columnCycle = i / rowCount;
            slots.add(new int[]{i, row, columnCycle});
        }





        slots.sort((a, b) -> Integer.compare(a[1], b[1]));

        for (int[] slot : slots) {
            int aliasIndex = slot[0];
            int row = slot[1];
            int columnCycle = slot[2];
            String alias = aliasList.get(aliasIndex);

            float xOffset = columnCycle % 2 == 0 ? -PREVIEW_ZOMBIE_GAP_X : PREVIEW_ZOMBIE_GAP_X;
            if (aliasList.size() <= rowCount) xOffset = 0f;

            float x = centerX + xOffset;
            float y = getCellCenterY(row) + ((aliasIndex % 3) - 1) * 8f;

            drawPreviewZombie(alias, x, y, aliasIndex);
        }
    }

    protected boolean isBossPreviewAlias(String alias) {
        String normalized = alias.toLowerCase();
        return normalized.contains("zomboss");
    }

    /**
     * Middle rows only (rows 2/3/4 in 1-indexed terms), so the preview avoids the very
     * top/bottom board rows. For the standard 5-row board that's indices {1, 2, 3}; for
     * odd row counts other than 5 it takes the middle three (or fewer if the board is
     * smaller), keeping the selection centered.
     */
    protected int[] middlePreviewRows(int totalRows) {
        if (totalRows <= 3) {
            int[] all = new int[totalRows];
            for (int i = 0; i < totalRows; i++) all[i] = i;
            return all;
        }
        int start = (totalRows - 3) / 2;
        return new int[]{start, start + 1, start + 2};
    }

    protected void drawPreviewZombie(String alias, float x, float y, int index) {
        String path = ZombieAnimationRegistry.pathFor(alias, seasonFolder);
        if (path == null) return;



        boolean useWalkInstead = ZOMBIE_JESTER_ALIAS.equalsIgnoreCase(alias);
        String preferredState = useWalkInstead ? "walk" : "idle";

        String clip = AnimationFactory.exactClipNameForPath(path, preferredState);
        if (clip == null) {
            clip = AnimationFactory.resolveClipNameForPath(path, preferredState);
        }
        if (clip == null) return;

        float time = previewAnimationTime;
        float duration = AnimationFactory.clipDurationForPath(path, clip);
        if (duration > 0f) time %= duration;

        float scale = alias.toLowerCase().contains("gargantuar")
                ? PREVIEW_ZOMBIE_SCALE * 0.92f : PREVIEW_ZOMBIE_SCALE;

        float drawX = x - 42f;
        float drawY = y - 5f;

        batch.setColor(Color.WHITE);



        if (ZOMBIE_PIANO_ALIAS.equalsIgnoreCase(alias)) {
            drawPreviewProp(PIANO_PROP_PAM, "idle", drawX + PIANO_PREVIEW_OFFSET_X, drawY + PIANO_PREVIEW_OFFSET_Y, scale, false);
        } else if (ZOMBIE_ARCADE_ALIAS.equalsIgnoreCase(alias)) {
            drawPreviewProp(ARCADE_PROP_PAM, "idle", drawX + ARCADE_PREVIEW_OFFSET_X, drawY + ARCADE_PREVIEW_OFFSET_Y, ARCADE_PREVIEW_SCALE, false);
        }

        boolean drawn = drawPam(path, clip, time, drawX, drawY, scale, false);
        if (!drawn) {
            try {
                var region = GameAssetManager.get().getZombieRegion(alias);
                if (region != null) {
                    drawEntity(region, drawX, drawY,
                            getBoardTileWidth(), getBoardTileHeight(),
                            new Color(0.55f, 0.5f, 0.45f, 1f),
                            alias);
                }
            } catch (Throwable ignored) {
            }
        }
        batch.setColor(Color.WHITE);
    }

    protected void drawPreviewProp(String propPath, String preferredState, float x, float y, float scale, boolean flip) {
        String clip = AnimationFactory.exactClipNameForPath(propPath, preferredState);
        if (clip == null) {
            clip = AnimationFactory.resolveClipNameForPath(propPath, preferredState);
        }
        if (clip == null) return;

        float time = previewAnimationTime;
        float duration = AnimationFactory.clipDurationForPath(propPath, clip);
        if (duration > 0f) time %= duration;

        drawPam(propPath, clip, time, x, y, scale, flip);
    }

    @Override
    public boolean runCommand(String command) {
        boolean result = super.runCommand(command);

        controller.ScreenManager.syncWithCurrentMenu();

        if (App.currentMenu instanceof BeforeMenu
                && controller.ScreenManager.getScreen() == this) {
            scheduleBuild();
        }

        return result;
    }

    protected void scheduleBuild() {
        if (stage == null) return;

        Gdx.app.postRunnable(() -> {
            if (stage != null
                    && controller.ScreenManager.getScreen() == this
                    && App.currentMenu instanceof BeforeMenu) {
                build();
            }
        });
    }

    protected void build() {
        rootTable.clear();
        cards.clear();

        if (startOverlay != null) {
            startOverlay.remove();
            startOverlay = null;
        }

        Level level = GameSession.peekInstance() == null ? null : GameSession.peekInstance().getLevel();
        if (level == null) {
            rootTable.add(new Label("No level loaded.", skin, "title")).center();
            return;
        }

        rootTable.top().left();

        Table topBar = buildTopBar(level);
        Table middle = buildMiddleSection(level);

        rootTable.add(topBar).expandX().fillX().top().left().padTop(8f).padLeft(12f).padRight(16f).row();
        rootTable.add(middle).expand().fill().top().left().padTop(6f).padLeft(12f);

        topBar.toFront();

        startOverlay = buildBottomBar();
        startOverlay.setFillParent(true);
        addBeforeModal(startOverlay);
    }

    protected Table buildTopBar(Level level) {
        Table topBar = new Table();
        topBar.top().left();

        ImageButton back = createIconButton(BACK_ICON, 46, 46, () -> runCommand("menu exit"));

        Table topLeft = new Table();
        topLeft.add(back).padRight(14f);
        Label nameLabel = new Label(level.getName(), skin, "title");
        nameLabel.setFontScale(0.85f);
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

    protected Table buildMiddleSection(Level level) {
        Table board = new Table();
        board.top().left();

        board.add(buildLoadoutPanel(level)).width(105f).expandY().fillY().top().left().padRight(10f);
        board.add(buildRightPanel(level)).expand().fill().top().left();

        return board;
    }

    protected Table buildRightPanel(Level level) {
        Table rightPanel = new Table();
        rightPanel.top().left();

        rightPanel.add(buildPreviewPanel()).expandX().fillX().padRight(170f).padBottom(SPACE_SM).row();
        rightPanel.add(buildPlantGrid(level)).expand().fill().top().left();

        return rightPanel;
    }

    protected Table buildBottomBar() {
        Table bottom = new Table();
        bottom.bottom().right();
        bottom.pad(12f).padRight(16f);
        TextButton start = primaryButton("Let's Rock!", () -> runCommand("start game"));
        bottom.add(start).width(180).height(48);
        return bottom;
    }

    protected ImageButton createIconButton(String path, float width, float height, Runnable action) {
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

    protected Table buildPreviewPanel() {
        Table box = new Table();
        box.setBackground(previewPanelBackground());
        box.pad(8).padTop(3f).top().left();

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
        int level = PlantProgression.levelOf(state, config);
        int maxLevel = PlantProgression.maxLevel(config);
        PlantStats stats = PlantStats.of(config, unlocked ? level : PlantStats.MIN_LEVEL);

        Label nameLabel = new Label(config.name + " (Lv. " + level + "/" + maxLevel + ")",
                skin, "title");
        nameLabel.setFontScale(0.9f);
        box.add(nameLabel).colspan(3).left().padBottom(6).row();

        Stack animBox = new Stack();
        animBox.add(new Image(getRoundedAnimBgDrawable()));
        animBox.add(new Image(roundedBorderDrawable(Color.WHITE, 2f, 12f)));

        String animationPath = null;
        try {
            animationPath = AnimationFactory.pathForDisplayName(config.name);
        } catch (Throwable ignored) {}

        PlantIdleAnimationActor animActor = new PlantIdleAnimationActor(animationPath, -10f, -8f);

        Table animLayer = new Table();
        animLayer.setTransform(true);
        animLayer.setScale(0.85f);
        animLayer.setOrigin(Align.center);
        animLayer.setFillParent(true);
        animLayer.add(animActor).size(120, 120);
        animBox.add(animLayer);

        box.add(animBox).size(165, 165).padRight(12).top();

        Table infoTable = new Table();
        infoTable.top().left();

        Label catLabel = new Label("Type: " + config.category, skin, "main");
        catLabel.setFontScale(0.75f);
        infoTable.add(catLabel).left().row();

        Label statsLabel = new Label("HP: " + stats.hp() + " | Dmg: " + stats.damage()
                + " | Rec: " + stats.recharge() + "s", skin, "main");
        statsLabel.setFontScale(0.75f);
        infoTable.add(statsLabel).left().row();

        Label economyLabel = new Label("Sun: " + stats.cost() + " | Interval: "
                + String.format("%.2f", stats.actionInterval()) + "s", skin, "main");
        economyLabel.setFontScale(0.75f);
        infoTable.add(economyLabel).left().row();

        if (!stats.specialTags().isEmpty()) {
            Label perkLabel = new Label("Perks: "
                    + String.join(", ", stats.specialTags()).replace('_', ' ').toLowerCase(),
                    skin, "muted");
            perkLabel.setFontScale(0.7f);
            infoTable.add(perkLabel).left().row();
        }

        String tagsStr = (config.tags == null || config.tags.isEmpty())
                ? "None"
                : config.tags.stream().map(Enum::name).collect(Collectors.joining(", "));
        Label tagsLabel = new Label("Tags: " + tagsStr, skin, "muted");
        tagsLabel.setFontScale(0.75f);
        infoTable.add(tagsLabel).left().row();

        String upgradeLine = PlantStatRows.upgradeLine(state, config, unlocked);
        if (upgradeLine != null) {
            Label upgradeLabel = new Label(upgradeLine, skin, "muted");
            upgradeLabel.setFontScale(0.7f);
            infoTable.add(upgradeLabel).left().row();
        }

        box.add(infoTable).expandX().fillX().top().padRight(12);

        Table actionsTable = new Table();
        actionsTable.bottom().right();

        if (unlocked && state != null) {
            boolean maxed = level >= maxLevel;
            int coinCost = PlantProgression.upgradeCoinCost(level);
            int packetsNeeded = PlantProgression.upgradePacketsRequired(level);
            int packetsOwned = state.seedPacketInventory.getOrDefault(config.id, 0);
            boolean canUpgrade = !maxed && state.coins >= coinCost && packetsOwned >= packetsNeeded;

            String upgText = maxed
                    ? "Max Level\nLv." + maxLevel
                    : "Upgrade Lv." + (level + 1) + "\n" + coinCost + " Coins (" + packetsOwned + "/" + packetsNeeded + ")";
            TextButton upgBtn = coloredButton(upgText, canUpgrade ? GREEN : GRAY, 0.7f);
            upgBtn.setDisabled(!canUpgrade);
            final String plantName = config.name;
            if (canUpgrade) {
                upgBtn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        runCommand("upgrade plant -t " + plantName);
                        scheduleBuild();
                    }
                });
            }
            actionsTable.add(upgBtn).width(145).height(44).padRight(10);
        }

        int diamonds = state != null ? state.diamonds : 0;
        boolean canBoost = diamonds >= 10;
        TextButton boostBtn = coloredButton("Boost Plant\n(10 Diamonds)", canBoost ? PURPLE : GRAY, 0.7f);
        boostBtn.setDisabled(!canBoost);
        final String pName = config.name;
        boostBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                int plantId = findId(collectionManager.getAllPlants(), pName);
                if (state != null && plantId >= 0 && state.diamonds >= 10
                        && session != null && !session.hasMatchBoost(plantId)) {
                    state.diamonds -= 10;
                    session.grantMatchBoost(plantId);
                    User.save();
                    showNoticePopup("Plant Boosted!", pName + " is boosted for this match!");
                    scheduleBuild();
                } else if (session != null && plantId >= 0 && session.hasMatchBoost(plantId)) {
                    showNoticePopup("Already Boosted", pName + " is already boosted for this match.");
                } else {
                    showNoticePopup("Insufficient Diamonds", "You need 10 diamonds to boost this plant.");
                }
            }
        });
        actionsTable.add(boostBtn).width(145).height(44).padRight(10);

        box.add(actionsTable).bottom().right();

        return box;
    }

    protected boolean allCollectionsUnlocked() {
        return false;
    }

    protected int loadoutSlots() {
        return 7;
    }

    protected Actor buildPlantGrid(Level level) {
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

        java.util.function.Predicate<String> locked = name -> !allCollectionsUnlocked()
                && state != null && !state.isPlantUnlocked(findId(allPlants, name));
        return buildGridContainer(names, locked, locked);
    }

    protected Actor buildLockedPlantsGrid(List<String> names, LockedPlantsLevel lockedLevel) {
        Table grid = new Table();
        grid.top().left();

        Table currentRow = null;
        for (int i = 0; i < names.size(); i++) {
            if (i % PLANT_GRID_COLUMNS == 0) {
                currentRow = new Table();
                grid.add(currentRow).left().padBottom(SPACE_XS).row();
            }
            String name = names.get(i);
            boolean locked = isPlantLockedInLevel(lockedLevel, name);
            Actor card = buildCard(name, locked, locked);
            currentRow.add(card).padLeft(2f).padRight(2f);
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);

        Table viewport = new Table();
        viewport.top().left();
        viewport.add(scrollPane).expand().fill().top().left();
        return viewport;
    }

    protected boolean isPlantLockedInLevel(LockedPlantsLevel lockedLevel, String plantName) {
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

    protected int findId(List<PlantJsonParser.PlantConfig> plants, String name) {
        for (PlantJsonParser.PlantConfig config : plants) {
            if (config.name.equalsIgnoreCase(name)) return config.id;
        }
        return -1;
    }

    protected Actor buildGridContainer(List<String> names, java.util.function.Predicate<String> darkened,
                                       java.util.function.Predicate<String> showLockIcon) {
        Table grid = new Table();
        grid.top().left();

        Table currentRow = null;
        for (int i = 0; i < names.size(); i++) {
            if (i % PLANT_GRID_COLUMNS == 0) {
                currentRow = new Table();
                grid.add(currentRow).left().padBottom(SPACE_XS).row();
            }
            String name = names.get(i);
            boolean isDarkened = darkened.test(name);
            Actor card = buildCard(name, isDarkened, showLockIcon.test(name));
            currentRow.add(card).padLeft(2f).padRight(2f);
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);

        Table gridContainer = new Table();
        gridContainer.top().left();
        gridContainer.add(scrollPane).expand().fill().top().left();
        return gridContainer;
    }

    protected Actor buildCard(String name, boolean darkened, boolean showLockIcon) {
        Stack cardStack = new Stack();

        float cardW = 104f;
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
                    scheduleBuild();
                }
            }
        });

        return cell;
    }

    protected Table buildLoadoutPanel(Level level) {
        Table panel = new Table();
        panel.pad(2f).top();

        Table slots = new Table();
        slots.top();

        int slotCount = loadoutSlots();
        for (int i = 0; i < slotCount; i++) {
            String name = i < BeforeMenu.selectedPlants.size()
                    ? BeforeMenu.selectedPlants.get(i) : "Empty";

            final String selected = name;

            if ("Empty".equals(name)) {
                Actor emptySlot = createEmptyLoadoutSlot("Empty", false, null);
                slots.add(emptySlot).size(LOADOUT_CARD_W, LOADOUT_CARD_H).pad(1f).row();
            } else {
                Actor cardActor = createLoadoutCard(name, new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        previewPlantName = selected;
                        removePlant(selected);
                    }
                });
                slots.add(cardActor).size(LOADOUT_CARD_W, LOADOUT_CARD_H).pad(1f).row();
            }
        }

        if (slotCount >= 8) {

        } else if (BeforeMenu.selectedPlants.size() == 8) {
            String rentedPlantName = BeforeMenu.selectedPlants.get(7);
            Actor rentedCardActor = createLoadoutCard(rentedPlantName, new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    previewPlantName = rentedPlantName;
                    removePlant(rentedPlantName);
                }
            });
            slots.add(rentedCardActor).size(LOADOUT_CARD_W, LOADOUT_CARD_H).pad(1f).row();
        } else {
            Actor rentSlot = createEmptyLoadoutSlot("Rent", true, new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (BeforeMenu.selectedPlants.size() < 7) {
                        showNoticePopup("Loadout Not Full", "You must fill all 7 loadout slots before renting a plant!");
                    } else {
                        showRentPlantPopup();
                    }
                }
            });
            slots.add(rentSlot).size(LOADOUT_CARD_W, LOADOUT_CARD_H).pad(1f).row();
        }

        panel.add(slots).expand().fill().row();

        int currentSelected = Math.min(BeforeMenu.selectedPlants.size(), slotCount);
        Label info = new Label(currentSelected + "/" + slotCount, skin, "main");
        info.setAlignment(Align.center);
        info.setFontScale(0.8f);
        panel.add(info).padTop(2).padBottom(2);

        return panel;
    }

    private TextureRegionDrawable loadoutSlotDrawable;
    private NinePatchDrawable previewPanelDrawable;
    private static final int PREVIEW_PANEL_HEADER_PX = 29;

    protected TextureRegionDrawable loadoutSlotBackground() {
        if (loadoutSlotDrawable == null) {
            loadoutSlotDrawable = new TextureRegionDrawable(new TextureRegion(loadTextureSafe(LOADOUT_SLOT_BG)));
        }
        return loadoutSlotDrawable;
    }

    protected NinePatchDrawable previewPanelBackground() {
        if (previewPanelDrawable == null) {
            Texture tex = loadTextureSafe(PREVIEW_PANEL_BG);
            NinePatch patch = new NinePatch(tex, 10, 10, PREVIEW_PANEL_HEADER_PX, 10);
            previewPanelDrawable = new NinePatchDrawable(patch);
        }
        return previewPanelDrawable;
    }

    protected Actor createLoadoutCard(String plantName, ClickListener clickListener) {
        Stack stack = new Stack();
        SeedPacketCard card = null;
        try {
            int plantId = findId(collectionManager.getAllPlants(), plantName);
            boolean boosted = session != null && plantId >= 0 && session.hasMatchBoost(plantId);
            card = boosted
                    ? cardFactory.buildCardForDisplayName(plantName, "boost.png")
                    : cardFactory.buildCardForDisplayName(plantName);
        } catch (Throwable ignored) {}

        if (card != null) {
            card.setSize(LOADOUT_CARD_W, LOADOUT_CARD_H);
            card.setTouchable(Touchable.disabled);
            stack.add(card);
        } else {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            Label label = new Label(plantName, skin, "main");
            label.setAlignment(Align.center);
            label.setFontScale(0.7f);
            label.setWrap(true);
            fallback.add(label).width(LOADOUT_CARD_W).center();
            stack.add(fallback);
        }

        if (clickListener != null) {
            stack.addListener(clickListener);
        }



        return wrapWithCardFrame(stack, LOADOUT_CARD_W, LOADOUT_CARD_H);
    }

    /**
     * Wraps a card actor with a thin card-background "frame" behind it, so plant cards
     * read as a bordered slot the same way zombie cards do (their frame.png border is
     * baked into the icon texture itself). Returned actor keeps the requested outer size.
     */
    protected Actor wrapWithCardFrame(Actor content, float outerW, float outerH) {
        Stack framed = new Stack();

        Image glass = new Image(loadoutSlotBackground());
        glass.setScaling(Scaling.stretch);
        glass.setColor(1f, 1f, 1f, 0.18f);
        framed.add(glass);

        Table inner = new Table();
        inner.pad(3f);
        inner.add(content).size(outerW - 6f, outerH - 6f);
        framed.add(inner);

        framed.setSize(outerW, outerH);
        return framed;
    }

    protected Actor createEmptyLoadoutSlot(String labelText, boolean isRent, ClickListener clickListener) {
        Table slot = new Table();

        Stack stack = new Stack();

        Image glass = new Image(loadoutSlotBackground());
        glass.setScaling(Scaling.stretch);
        glass.setColor(1f, 1f, 1f, 0.18f);
        stack.add(glass);

        if (isRent) {
            Image lockImg = new Image(loadTextureSafe(LOCK_ICON));
            lockImg.setScaling(Scaling.fit);
            stack.add(lockImg);
        }

        slot.add(stack).expand().fill();

        if (clickListener != null) {
            slot.addListener(clickListener);
        }
        return slot;
    }

    protected void showRentPlantPopup() {
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
                        scheduleBuild();
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

    protected void showNoticePopup(String titleText, String msgText) {
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

    protected int getUserCoins() {
        if (User.currentUser != null && User.currentUser.userState != null) {
            return User.currentUser.userState.coins;
        }
        return 0;
    }

    protected boolean tryDeductCoins(int amount) {
        if (User.currentUser != null && User.currentUser.userState != null) {
            if (User.currentUser.userState.coins >= amount) {
                User.currentUser.userState.coins -= amount;
                return true;
            }
        }
        return false;
    }

    protected void togglePlant(String name) {
        Level level = GameSession.peekInstance() == null ? null : GameSession.peekInstance().getLevel();
        if (BeforeMenu.selectedPlants.stream().anyMatch(p -> p.equalsIgnoreCase(name))) {
            removePlant(name);
            return;
        }
        if (BeforeMenu.selectedPlants.size() >= loadoutSlots()) {
            scheduleBuild();
            return;
        }
        if (level instanceof LockedPlantsLevel) {
            BeforeMenu.selectedPlants.add(name);
            scheduleBuild();
            return;
        }
        runCommand("add plant -t " + name);
        scheduleBuild();
    }

    protected void removePlant(String name) {
        Level level = GameSession.peekInstance() == null ? null : GameSession.peekInstance().getLevel();
        if (level instanceof LockedPlantsLevel) {
            BeforeMenu.selectedPlants.removeIf(p -> p.equalsIgnoreCase(name));
            scheduleBuild();
            return;
        }
        runCommand("remove plant -t " + name);
        scheduleBuild();
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

    protected TextButton coloredButton(String text, Color color, float fontScale) {
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

    protected Drawable getRoundedAnimBgDrawable() {
        if (roundedAnimBgDrawable == null) {
            roundedAnimBgDrawable = createRoundedTextureDrawable("assets/images/ui/collection/card_plant_bg_modern.png", 14f);
        }
        return roundedAnimBgDrawable;
    }

    protected Drawable createRoundedTextureDrawable(String path, float screenRadius) {
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

    protected Drawable roundedFilledDrawable(Color fill, Color border, float radius, float thickness) {
        return roundedDrawable(fill, border, radius, thickness, true);
    }

    protected Drawable roundedBorderDrawable(Color border, float thickness, float radius) {
        return roundedDrawable(new Color(1f, 1f, 1f, 0f), border, radius, thickness, false);
    }

    protected Drawable roundedDrawable(Color fill, Color border, float radius, float thickness, boolean filled) {
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

    protected float roundedRectDistance(float x, float y, float w, float h, float radius) {
        float cx = Math.max(radius, Math.min(x, w - radius));
        float cy = Math.max(radius, Math.min(y, h - radius));
        float dx = x - cx;
        float dy = y - cy;
        return (float) Math.sqrt(dx * dx + dy * dy) - radius;
    }

    protected Drawable solidColorDrawable(Color color) {
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

    protected class PlantIdleAnimationActor extends Actor {
        private final String animationPath;
        private float stateTime = 0f;
        private final float offsetX;
        private final float offsetY;

        PlantIdleAnimationActor(String animationPath, float ox, float oy) {
            this.animationPath = animationPath;
            this.offsetX = ox + 60f;
            this.offsetY = oy + 53f;
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
    public void dispose() {
        cardFactory.dispose();
        if (roundedAnimBgTexture != null) {
            roundedAnimBgTexture.dispose();
        }
        if (rightPreviewTexture != null) {
            rightPreviewTexture.dispose();
            rightPreviewTexture = null;
        }
        super.dispose();
    }
}