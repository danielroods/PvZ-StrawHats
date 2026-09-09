package view.screens.ui_menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
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

import controller.CollectionManager;
import model.collections.animations.AnimationFactory;
import model.resoures.CurrencyType;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.armour.Armour;
import model.collections.plant.PlantJsonParser;
import model.collections.plant.PlantProgression;
import model.collections.plant.PlantCostume;
import model.collections.plant.PlantCostumeManager;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.user_data.User;
import model.user_data.UserState;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;
import service.card_factory.ZombieIconCard;
import service.card_factory.ZombieIconCardFactory;
import view.screens.generals.PlantCostumeMask;
import view.screens.generals.UiScreen;
import view.screens.generals.ParticleCreator;

import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionScreen extends UiScreen {

    private static final String BACK_ICON = "assets/images/ui/buttons_hud_back_normal.png";
    private static final String COIN_ICON = "assets/images/ui/buttons_coin_buy_normal.png";
    private static final String GEM_ICON = "assets/images/ui/buttons_premium_normal.png";
    private static final String LOCK_ICON = "assets/images/ui/collection/lock_small_gold.png";
    private static final String UPGRADE_ICON = "assets/images/ui/collection/rift_perk_upgrade_uparrow.png";

    private static final float TAG_ICON_SIZE = 30f;

    private static final String TAG_ICON_DIR = "assets/images/ui/collection/tags/";

    
    private static final String DEFAULT_TAG_ICON = TAG_ICON_DIR + "mintfam_banner.png";

    /**
     * Maps each PlantTag to the closest-themed icon in the real mintfam_* set (only 15
     * distinct family icons exist, vs. 38 tags), so cards get a fitting icon instead of
     * mostly falling back to the default plaque.
     */
    private static final Map<PlantTag, String> TAG_ICON_PATHS = buildTagIconPaths();

    private static Map<PlantTag, String> buildTagIconPaths() {
        Map<PlantTag, String> map = new EnumMap<>(PlantTag.class);
        map.put(PlantTag.DAY, "sun");
        map.put(PlantTag.SHROOM, "shadow");
        map.put(PlantTag.WRAMP_UP, "slow");
        map.put(PlantTag.NIGHT, "shadow");
        map.put(PlantTag.PEA, "peashooter");
        map.put(PlantTag.ICE, "cold");
        map.put(PlantTag.STACK, "defense");
        map.put(PlantTag.CHARGE, "electricity");
        map.put(PlantTag.MAGIC, "magic");
        map.put(PlantTag.FIRE, "fire");
        map.put(PlantTag.POISON, "poison");
        map.put(PlantTag.WATER, "cold");
        map.put(PlantTag.BOUNCE, "lobber");
        map.put(PlantTag.PIERCE, "sharp");
        map.put(PlantTag.DELAYED, "slow");
        map.put(PlantTag.SPLASH, "explosive");
        map.put(PlantTag.INSTANT, "sharp");
        map.put(PlantTag.LANE, "defense");
        map.put(PlantTag.STUN, "electricity");
        map.put(PlantTag.MULTIDIRECTIONAL, "peashooter");
        map.put(PlantTag.GATLING, "peashooter");
        map.put(PlantTag.TIMED, "slow");
        map.put(PlantTag.SHIELD, "defense");
        map.put(PlantTag.TALL, "defense");
        map.put(PlantTag.MELEE, "melee");
        map.put(PlantTag.DIVERT, "defense");
        map.put(PlantTag.ATTRACT, "magic");
        map.put(PlantTag.POCKETS, "trap");
        map.put(PlantTag.DISARM, "trap");
        map.put(PlantTag.HYPNO, "magic");
        map.put(PlantTag.CLONE, "magic");
        map.put(PlantTag.BESTER, "melee");
        map.put(PlantTag.SUN, "sun");
        map.put(PlantTag.AOE, "explosive");
        map.put(PlantTag.MOVE_ZOMBIES, "electricity");
        map.put(PlantTag.BUTTER, "trap");
        map.put(PlantTag.EXPLOSIVE, "explosive");
        map.put(PlantTag.TRAP, "trap");
        for (Map.Entry<PlantTag, String> entry : map.entrySet()) {
            entry.setValue(TAG_ICON_DIR + "mintfam_" + entry.getValue() + ".png");
        }
        return map;
    }

    private static final String PLANTS_TAB_ICON = "assets/images/ui/collection/plants.png";
    private static final String ZOMBIES_TAB_ICON = "assets/images/ui/collection/zombies.png";

    private static final String MAIN_BG = "assets/images/backg/mainmenu_background.png";
    private static final String TAB_ACTIVE_BG = "assets/images/ui/zombies_active.png";
    private static final String TAB_BOARD_BG = "assets/images/backg/wood board.png";

    private static final int PLANTS_PER_ROW = 5;
    private static final int ZOMBIES_PER_ROW = 5;
    private static final float CARD_W = 135f;
    private static final float BAR_H = 14f;

    private static final Color YELLOW = new Color(0.95f, 0.80f, 0.15f, 1f);
    private static final Color GREEN = new Color(0.30f, 0.75f, 0.25f, 1f);
    private static final Color GRAY = new Color(0.45f, 0.45f, 0.45f, 1f);
    private static final Color PURPLE = new Color(0.55f, 0.30f, 0.85f, 1f);
    private static final String COSTUME_NEXT_ICON = "assets/images/ui/stats_screen_nav_arrow_next.png";
    private static final String COSTUME_PREVIOUS_ICON = "assets/images/ui/stats_screen_nav_arrow_previous.png";

    private enum CollectionTab { PLANTS, ZOMBIES }

    private CollectionTab currentTab = CollectionTab.PLANTS;

    private final SeedPacketCardFactory cardFactory = new SeedPacketCardFactory();
    private final ZombieIconCardFactory zombieCardFactory = new ZombieIconCardFactory();
    private final CollectionManager manager = new CollectionManager();

    private Integer openPlantId = null;
    private String openZombieAlias = null;
    private final Map<Integer, String> previewPlantCostumes = new HashMap<>();
    private Actor popupOverlay;
    private Actor currentParticleActor;

    private final TextureRegion whitePixel = whitePixelRegion();
    private Texture upgradeIconTexture;
    private Texture roundedAnimBgTexture;
    private Drawable roundedAnimBgDrawable;

    private TextureBank textureBank;
    private PamPlayer pamPlayer;
    private Map<String,Boolean> visibility = new HashMap<>();

    @Override
    public void show() {
        if (textureBank == null) {
            try {
                FileHandle rootHandle = Gdx.files.internal("assets/pvz-assets");
                textureBank = new TextureBank("atlases", rootHandle);
                pamPlayer = new PamPlayer(textureBank, rootHandle);

                Gdx.app.log("PAM_INIT", "PAM System and TextureBank initialized successfully!");
            } catch (Throwable t) {
                Gdx.app.error("PAM_INIT", "Failed to initialize PAM System", t);
            }
        }

        if (upgradeIconTexture == null) {
            upgradeIconTexture = loadTextureSafe(UPGRADE_ICON);
        }

        super.show();
        build();
    }

    @Override
    public void initParticles() {
        if (particles != null) {
            particles.dispose();
        }
        if (currentParticleActor != null) {
            currentParticleActor.remove();
            currentParticleActor = null;
        }

        if (currentTab == CollectionTab.PLANTS) {
            particlePaths = new String[]{
                    "assets/images/ui/collection/cornfettipopper_70x50.png",
                    "assets/images/ui/collection/leaf_backdrop.png",
                    "assets/images/ui/collection/prize_pinata_mushrooms_123x127.png",
                    "assets/images/ui/collection/prize_pinata_nuts_97x91.png"
            };
            particles = new ParticleCreator(particlePaths, 15, 20f, 32f, 1.2f, true);
        } else {
            particlePaths = new String[]{
                    "assets/images/ui/collection/gargantuar_imp_74x59.png",
                    "assets/images/ui/collection/halloween_zombie_basic_36x45.png",
                    "assets/images/ui/collection/zombie_lostcity_crystalskull_34x31.png",
                    "assets/images/ui/collection/zombie_prospector_19x47.png"
            };
            particles = new ParticleCreator(particlePaths, 15, 45f, 75f, 1.2f, true);
        }

        Actor rawParticleActor = particles.createActor();
        rawParticleActor.setTouchable(Touchable.disabled);

        Group particleWrapper = new Group() {
            @Override
            protected void drawChildren(Batch batch, float parentAlpha) {
                Color c = batch.getColor();
                float oldR = c.r, oldG = c.g, oldB = c.b, oldA = c.a;
                batch.setColor(oldR, oldG, oldB, 1.0f);
                super.drawChildren(batch, 1.0f);
                batch.setColor(oldR, oldG, oldB, oldA);
            }
        };
        particleWrapper.setTouchable(Touchable.disabled);
        particleWrapper.addActor(rawParticleActor);

        currentParticleActor = particleWrapper;
        rootStack.addActorAt(1, currentParticleActor);
    }

    @Override
    public void render(float delta) {
        if (textureBank != null) {
            try {
                textureBank.update();
            } catch (Throwable t) {
                Gdx.app.error("CollectionScreen", "textureBank.update() failed", t);
            }
        }
        super.render(delta);
    }

    @Override
    public void dispose() {
        try {
            cardFactory.dispose();
            zombieCardFactory.dispose();
            if (upgradeIconTexture != null) {
                upgradeIconTexture.dispose();
            }
            if (roundedAnimBgTexture != null) {
                roundedAnimBgTexture.dispose();
            }
        } catch (Throwable t) {
            Gdx.app.error("CollectionScreen", "Failed to dispose card factory", t);
        }
        super.dispose();
    }

    private void build() {
        rootTable.clear();
        rootTable.setBackground(new TextureRegionDrawable(loadTextureSafe(MAIN_BG)));

        Stack mainStack = new Stack();
        Table boardContainer = new Table();
        boardContainer.top();
        Table contentBox = currentTab == CollectionTab.PLANTS ? buildPlantsTabBox() : buildZombiesTabBox();
        boardContainer.add(contentBox).expand().fill().padTop(68f).padLeft(20f).padRight(20f).padBottom(20f);

        Table topBarContainer = new Table();
        topBarContainer.top();
        topBarContainer.add(buildTopBar()).expandX().fillX().padTop(12f).padLeft(20f).padRight(20f);

        mainStack.add(boardContainer);
        mainStack.add(topBarContainer);

        rootTable.add(mainStack).expand().fill();

        if (popupOverlay != null) {
            popupOverlay.remove();
            popupOverlay = null;
        }
        if (openPlantId != null) {
            openPlantInfo(openPlantId);
        } else if (openZombieAlias != null) {
            openZombieInfo(openZombieAlias);
        }

        initParticles();
    }

    @Override
    protected void onAfterCommand() {
        build();
    }

    @Override
    protected void refreshContent() {
        build();
    }

    private Table buildTopBar() {
        ImageButton backBtn = createIconButton(BACK_ICON, 54, 54, () -> runCommand("menu exit"));

        Table topLeft = new Table();
        topLeft.left();
        topLeft.add(backBtn).padRight(20).top();
        topLeft.add(buildTabs()).top();

        User user = User.currentUser;
        int coins = (user != null && user.userState != null) ? user.userState.coins : 0;
        int diamonds = (user != null && user.userState != null) ? user.userState.diamonds : 0;

        Table topRight = new Table();
        topRight.right();
        topRight.add(currencyWidget(CurrencyType.COIN, coins)).padRight(15).padTop(-70);
        topRight.add(currencyWidget(CurrencyType.DIAMOND, diamonds)).padTop(-70);

        Table topBar = new Table();
        topBar.add(topLeft).left().expandX();
        topBar.add(topRight).right();
        return topBar;
    }

    private Table buildTabs() {
        Table tabs = new Table();
        tabs.left();
        tabs.add(buildTab(CollectionTab.PLANTS)).padRight(SPACE_SM);
        tabs.add(buildTab(CollectionTab.ZOMBIES));
        return tabs;
    }

    private Table buildTab(CollectionTab tab) {
        Table container = new Table();
        String iconPath = (tab == CollectionTab.PLANTS) ? PLANTS_TAB_ICON : ZOMBIES_TAB_ICON;

        if (tab == currentTab) {
            container.setBackground(new TextureRegionDrawable(loadTextureSafe(TAB_ACTIVE_BG)));
            container.pad(8, 22, 10, 22);

            Image tabImage = new Image(loadTextureSafe(iconPath));
            container.add(tabImage).center();
        } else {
            TextureRegionDrawable tabDrawable = new TextureRegionDrawable(loadTextureSafe(TAB_ACTIVE_BG));
            Drawable inactiveDrawable = tabDrawable.tint(new Color(0.35f, 0.30f, 0.45f, 0.88f));
            container.setBackground(inactiveDrawable);
            container.pad(8, 22, 14, 22);

            Image tabImage = new Image(loadTextureSafe(iconPath));
            tabImage.setColor(new Color(0.85f, 0.85f, 0.90f, 1f));
            container.add(tabImage).center();
        }

        container.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (currentTab == tab) {
                    return;
                }
                currentTab = tab;
                openPlantId = null;
                openZombieAlias = null;
                build();
            }
        });
        return container;
    }

    private Table buildPlantsTabBox() {
        Table box = new Table();
        box.setBackground(new TextureRegionDrawable(loadTextureSafe(TAB_BOARD_BG)));
        box.top();

        UserState state = User.currentUser.userState;
        List<PlantJsonParser.PlantConfig> plants = sortedPlants(state);

        Table grid = new Table();
        grid.top().padTop(10f);

        for (int i = 0; i < plants.size(); i++) {
            Table cell = buildPlantCardCell(plants.get(i), state);
            grid.add(cell).pad(SPACE_SM);
            if ((i + 1) % PLANTS_PER_ROW == 0) {
                grid.row();
            }
        }

        ScrollPane pane = scrollable(grid);
        box.add(pane).expand().fill().padTop(35f).padBottom(15f).padLeft(20f).padRight(20f);
        return box;
    }

    private List<PlantJsonParser.PlantConfig> sortedPlants(UserState state) {
        List<PlantJsonParser.PlantConfig> all = manager.getAllPlants();
        all.sort((a, b) -> {
            boolean unlockedA = state.isPlantUnlocked(a.id);
            boolean unlockedB = state.isPlantUnlocked(b.id);
            if (unlockedA != unlockedB) {
                return unlockedA ? -1 : 1;
            }
            return Integer.compare(a.id, b.id);
        });
        return all;
    }

    private Table buildPlantCardCell(PlantJsonParser.PlantConfig config, UserState state) {
        boolean unlocked = state.isPlantUnlocked(config.id);

        Table cell = new Table();
        cell.setTransform(true);

        float cardW = CARD_W;
        float cardH = CARD_W * 1.38f;

        Stack cardStack = new Stack();
        try {
            SeedPacketCard card = cardFactory.buildCardForDisplayName(config.name);
            if (card != null) {
                cardW = card.getWidth();
                cardH = card.getHeight();
                cardStack.add(card);
            }
        } catch (Throwable t) {
            Gdx.app.error("CollectionScreen", "Failed to build seed packet card for " + config.name, t);
        }
        cardStack.setSize(cardW, cardH);

        if (config.tags != null && !config.tags.isEmpty()) {
            String tagPath = TAG_ICON_PATHS.get(config.tags.get(0));
            if (tagPath == null || !Gdx.files.internal(tagPath).exists()) {
                tagPath = DEFAULT_TAG_ICON;
            }
            Image tagImage = new Image(loadTextureSafe(tagPath));
            Container<Image> tagContainer = new Container<>(tagImage);
            tagContainer.size(TAG_ICON_SIZE, TAG_ICON_SIZE).top().left().padTop(6f).padLeft(6f);
            cardStack.add(tagContainer);
        }

        if (!unlocked) {
            Image dim = new Image(solidColorDrawable(new Color(0f, 0f, 0f, 0.42f)));
            cardStack.add(dim);

            Image lockImage = new Image(loadTextureSafe(LOCK_ICON));
            Container<Image> lockContainer = new Container<>(lockImage);
            lockContainer.size(38f, 38f).bottom().right().padRight(6f).padBottom(6f);
            cardStack.add(lockContainer);
        }

        cardStack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openPlantId = config.id;
                openPlantInfo(config.id);
            }
        });

        cell.add(cardStack).size(cardW, cardH).top().row();

        if (unlocked) {
            int level = PlantProgression.levelOf(state, config);
            boolean maxed = level >= PlantProgression.maxLevel(config);
            int packetsOwned = Math.max(0, state.seedPacketInventory.getOrDefault(config.id, 0));
            float progress = maxed ? 1f : Math.min(1f, (float) packetsOwned / (float) level);
            int coinCost = PlantProgression.upgradeCoinCost(level);
            boolean canUpgrade = !maxed && state.coins >= coinCost && packetsOwned >= level;

            SeedProgressBar bar = new SeedProgressBar(progress, canUpgrade, upgradeIconTexture);
            cell.add(bar).size(cardW, BAR_H).padTop(5f).top();
        }

        return cell;
    }

    private Table buildZombiesTabBox() {
        Table box = new Table();
        box.setBackground(new TextureRegionDrawable(loadTextureSafe(TAB_BOARD_BG)));
        box.top();

        UserState state = User.currentUser.userState;
        Set<String> seenAliases = manager.getSeenZombieAliases(state);
        List<String> aliases = sortedZombieAliases(seenAliases);

        Table grid = new Table();
        grid.top().padTop(10f);

        for (int i = 0; i < aliases.size(); i++) {
            String alias = aliases.get(i);
            Table cell = buildZombieCardCell(alias, seenAliases.contains(alias));
            grid.add(cell).pad(SPACE_SM);
            if ((i + 1) % ZOMBIES_PER_ROW == 0) {
                grid.row();
            }
        }

        ScrollPane pane = scrollable(grid);
        box.add(pane).expand().fill().padTop(35f).padBottom(15f).padLeft(20f).padRight(20f);
        return box;
    }

    private List<String> sortedZombieAliases(Set<String> seenAliases) {
        List<String> all = new ArrayList<>(manager.getAdventureZombieAliases());
        all.sort((a, b) -> {
            boolean seenA = seenAliases.contains(a);
            boolean seenB = seenAliases.contains(b);
            if (seenA != seenB) {
                return seenA ? -1 : 1;
            }
            return friendlyZombieName(a).compareTo(friendlyZombieName(b));
        });
        return all;
    }

    private Table buildZombieCardCell(String alias, boolean seen) {
        Table cell = new Table();
        cell.setTransform(true);

        Stack cardStack = new Stack();
        float cardW = CARD_W;
        float cardH = CARD_W * 1.38f;

        if (seen) {
            ZombieIconCard card = null;
            try {
                card = zombieCardFactory.buildCardForAlias(alias);
            } catch (Throwable t) {
                Gdx.app.error("CollectionScreen", "Failed to build zombie icon card for " + alias, t);
            }

            boolean hasRealIcon = card != null
                    && !ZombieIconCardFactory.PLACEHOLDER_ICON_FILE.equals(card.getIconFile());
            if (hasRealIcon) {
                cardW = card.getWidth();
                cardH = card.getHeight();
                cardStack.setSize(cardW, cardH);
                cardStack.add(card);
            } else {
                
                
                
                
                
                
                cardStack.setSize(cardW, cardH);
                cardStack.add(new Image(new TextureRegionDrawable(zombieCardFactory.getCardBackground())));
                Actor animatedIcon = buildAnimatedZombieIcon(alias, cardW, cardH);
                if (animatedIcon != null) {
                    cardStack.add(animatedIcon);
                }
            }

            cardStack.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    openZombieAlias = alias;
                    openZombieInfo(alias);
                }
            });
        } else {
            cardStack.setSize(cardW, cardH);
            Image frame = new Image(solidColorDrawable(new Color(0.10f, 0.09f, 0.08f, 0.55f)));
            cardStack.add(frame);

            Label mystery = new Label("?", skin, "title");
            mystery.setFontScale(1.6f);
            mystery.setAlignment(Align.center);
            Container<Label> mysteryContainer = new Container<>(mystery);
            mysteryContainer.fill();
            cardStack.add(mysteryContainer);
        }

        cell.add(cardStack).size(cardW, cardH).row();

        Label nameLabel = new Label(seen ? friendlyZombieName(alias) : "???", skin, "main");
        nameLabel.setFontScale(0.75f);
        nameLabel.setAlignment(Align.center);
        nameLabel.setWrap(true);
        cell.add(nameLabel).width(cardW).padTop(4);

        return cell;
    }

    private String friendlyZombieName(String alias) {
        String withoutPrefix = alias.startsWith("Zombie") ? alias.substring("Zombie".length()) : alias;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < withoutPrefix.length(); i++) {
            char c = withoutPrefix.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.length() == 0 ? alias : sb.toString();
    }

    /**
     * Builds a scaled-down, clipped copy of the same idle-animation box
     * {@link #buildZombieInfoPopup} uses (250x250 outer box, 220x220 actor, actor
     * placed at (0,0) with the same +110/+85 offsets - see {@link PlantIdleAnimationActor}),
     * for use as a card icon when {@link ZombieIconCardFactory} has no flat icon for
     * the alias. {@link pvz.libpvz.pam.PamPlayer#draw} always renders at native pixel
     * size (it takes no scale parameter), so shrinking it requires wrapping it in a
     * transform-enabled {@link Group} and scaling that instead.
     */
    private Actor buildAnimatedZombieIcon(String alias, float cardW, float cardH) {
        String animationPath = null;
        try {
            animationPath = ZombieAnimationRegistry.pathFor(alias);
        } catch (Throwable t) {
            Gdx.app.error("CollectionScreen", "Failed to resolve fallback icon animation for " + alias, t);
        }
        if (animationPath == null) {
            return null;
        }

        final float boxSize = 250f;
        final float actorSize = 220f;

        Group animBox = new Group();
        animBox.setTransform(true);
        animBox.setSize(boxSize, boxSize);
        animBox.setOrigin(boxSize / 2f, boxSize / 2f);

        PlantIdleAnimationActor animActor = new PlantIdleAnimationActor(animationPath, 0, 0);
        animActor.setPosition((boxSize - actorSize) / 2f, (boxSize - actorSize) / 2f);
        animBox.addActor(animActor);

        float boxOnCard = Math.min(cardW, cardH) * 0.95f;
        animBox.setScale(boxOnCard / boxSize);
        animBox.setPosition(cardW / 2f - boxSize / 2f, cardH / 2f - boxSize / 2f);

        ClippedGroup clip = new ClippedGroup(cardW, cardH);
        clip.addActor(animBox);
        return clip;
    }

    
    private static class ClippedGroup extends Group {
        ClippedGroup(float width, float height) {
            setSize(width, height);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            boolean clipped = clipBegin(getX(), getY(), getWidth(), getHeight());
            super.draw(batch, parentAlpha);
            if (clipped) clipEnd();
        }
    }

    private void openZombieInfo(String alias) {
        if (!manager.getAdventureZombieAliases().contains(alias)) {
            openZombieAlias = null;
            return;
        }
        if (popupOverlay != null) {
            popupOverlay.remove();
        }
        popupOverlay = buildZombieInfoPopup(alias);
        getModalStack().add(popupOverlay);
    }

    private void closeZombieInfo() {
        if (popupOverlay != null) {
            popupOverlay.remove();
            popupOverlay = null;
        }
        openZombieAlias = null;
    }

    private Stack buildZombieInfoPopup(String alias) {
        Zombie zombie = manager.findZombie(alias);

        Stack popupStack = new Stack();

        Image scrim = new Image(solidColorDrawable(new Color(0f, 0f, 0f, 0.72f)));
        popupStack.add(scrim);

        Table panel = new Table();
        panel.setBackground(new TextureRegionDrawable(loadTextureSafe("assets/images/backg/wood board.png")));
        panel.pad(SPACE_LG);
        panel.top().left();

        Table header = new Table();
        ImageButton back = createIconButton(BACK_ICON, 50, 50, this::closeZombieInfo);
        header.add(back).left().expandX();
        panel.add(header).fillX().padBottom(SPACE_MD).row();

        Table body = new Table();
        body.top();

        Stack animBox = new Stack();
        animBox.add(new Image(getRoundedAnimBgDrawable()));
        animBox.add(new Image(roundedBorderDrawable(Color.WHITE, 3f, 18f)));
        String animationPath = null;
        try {
            animationPath = ZombieAnimationRegistry.pathFor(alias);
        } catch (Throwable t) {
            Gdx.app.error("CollectionScreen", "Failed to resolve idle animation for " + alias, t);
        }
        PlantIdleAnimationActor animActor = new PlantIdleAnimationActor(animationPath, 0, 0);
        Table animLayer = new Table();
        animLayer.setFillParent(true);
        animLayer.add(animActor).size(220, 220);
        animBox.add(animLayer);
        body.add(animBox).size(250, 250).top().padRight(SPACE_LG);

        Table info = new Table();
        info.top().left();

        Label nameLabel = new Label(friendlyZombieName(alias), skin, "title");
        nameLabel.setFontScale(1.3f);
        info.add(nameLabel).left().padBottom(SPACE_SM).row();

        if (zombie != null) {
            info.add(statLabel("Race: " + zombie.getRace())).left().row();
            info.add(statLabel("HP: " + zombie.getMaxHp())).left().row();
            double speed = zombie.getSpeed() != null ? Math.abs(zombie.getSpeed().x()) : 0;
            info.add(statLabel("Speed: " + String.format("%.2f", speed))).left().row();
            info.add(statLabel("Eat DPS: " + String.format("%.1f", zombie.getEatDps()))).left().row();

            Armour armour = zombie.getArmour();
            String armorText = armour == null ? "None" : armour.getStage() + " (" + armour.getHP() + " HP)";
            info.add(statLabel("Armor: " + armorText)).left().padBottom(SPACE_LG).row();
        } else {
            info.add(statLabel("No further data available.")).left().padBottom(SPACE_LG).row();
        }

        body.add(info).top().left().expandX().padLeft(80f);

        panel.add(body).padTop(80f).padLeft(110f).row();

        Table centered = new Table();
        centered.add(panel).width(SCREEN_WIDTH * 0.82f).height(SCREEN_HEIGHT * 0.8f);
        popupStack.add(centered);

        popupStack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.getTarget() == popupStack) {
                    closeZombieInfo();
                }
            }
        });

        return popupStack;
    }

    private void openPlantInfo(int plantId) {
        PlantJsonParser.PlantConfig config = null;
        for (PlantJsonParser.PlantConfig candidate : manager.getAllPlants()) {
            if (candidate.id == plantId) {
                config = candidate;
                break;
            }
        }
        if (config == null) {
            openPlantId = null;
            return;
        }

        if (popupOverlay != null) {
            popupOverlay.remove();
        }
        popupOverlay = buildPlantInfoPopup(config);
        getModalStack().add(popupOverlay);
    }

    private void closePlantInfo() {
        if (popupOverlay != null) {
            popupOverlay.remove();
            popupOverlay = null;
        }
        openPlantId = null;
    }

    private Stack buildPlantInfoPopup(PlantJsonParser.PlantConfig config) {
        UserState state = User.currentUser.userState;
        boolean unlocked = state.isPlantUnlocked(config.id);

        Stack popupStack = new Stack();

        Image scrim = new Image(solidColorDrawable(new Color(0f, 0f, 0f, 0.72f)));
        popupStack.add(scrim);

        Table panel = new Table();
        panel.setBackground(new TextureRegionDrawable(loadTextureSafe("assets/images/backg/wood board.png")));
        panel.pad(SPACE_LG);
        panel.top().left();

        Table header = new Table();
        ImageButton back = createIconButton(BACK_ICON, 50, 50, this::closePlantInfo);
        header.add(back).left().expandX();
        panel.add(header).fillX().padBottom(SPACE_MD).row();

        Table body = new Table();
        body.top();

        Stack animBox = new Stack();
        animBox.add(new Image(getRoundedAnimBgDrawable()));
        animBox.add(new Image(roundedBorderDrawable(Color.WHITE, 3f, 18f)));
        String animationPath = null;
        try {
            animationPath = AnimationFactory.pathForDisplayName(config.name);
        } catch (Throwable t) {
            Gdx.app.error("CollectionScreen", "Failed to resolve idle animation for " + config.name, t);
        }
        String initialPreview = previewPlantCostumes.get(config.id);
        if (initialPreview == null && !previewPlantCostumes.containsKey(config.id)) {
            initialPreview = PlantCostumeManager.selectedCostume(config.id);
            previewPlantCostumes.put(config.id, initialPreview);
        }
        PlantIdleAnimationActor animActor = new PlantIdleAnimationActor(
                config.id, config.name, animationPath, initialPreview, 0, 0);
        Table animLayer = new Table();
        animLayer.setFillParent(true);
        animLayer.add(animActor).size(220, 220);
        animBox.add(animLayer);

        Table animationColumn = new Table();
        animationColumn.top();
        animationColumn.add(animBox).size(250, 250).row();
        if (PlantCostumeManager.hasCostumes(config.name)) {
            animationColumn.add(buildPlantCostumeControls(config, animActor)).padTop(8f).center();
        }
        body.add(animationColumn).top().padRight(SPACE_LG);

        Table info = new Table();
        info.top().left();

        Label nameLabel = new Label(config.name, skin, "title");
        nameLabel.setFontScale(1.3f);
        info.add(nameLabel).left().padBottom(SPACE_SM).row();

        for (String row : PlantStatRows.of(state, config, unlocked)) {
            info.add(statLabel(row)).left().row();
        }
        String upgradeLine = PlantStatRows.upgradeLine(state, config, unlocked);
        info.add(statLabel(upgradeLine == null ? "" : upgradeLine))
                .left().padBottom(SPACE_LG).row();

        TextButton actionButton = buildActionButton(config, state, unlocked);
        info.add(actionButton).width(320).height(58).left();

        body.add(info).top().left().expandX().padLeft(80f);

        panel.add(body).padTop(80f).padLeft(110f).row();

        Table centered = new Table();
        centered.add(panel).width(SCREEN_WIDTH * 0.82f).height(SCREEN_HEIGHT * 0.8f);
        popupStack.add(centered);

        popupStack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.getTarget() == popupStack) {
                    closePlantInfo();
                }
            }
        });

        return popupStack;
    }

    private Drawable getRoundedAnimBgDrawable() {
        if (roundedAnimBgDrawable == null) {
            roundedAnimBgDrawable = createRoundedTextureDrawable("assets/images/ui/collection/card_plant_bg_modern.png", 18f);
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

            float r = screenRadius * ((float) w / 250f);
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

    private Table buildPlantCostumeControls(PlantJsonParser.PlantConfig config, PlantIdleAnimationActor animActor) {
        Table controls = new Table();
        controls.center();

        ImageButton previous = createIconButton(COSTUME_PREVIOUS_ICON, 46, 46, () -> {
            animActor.movePreview(-1);
            previewPlantCostumes.put(config.id, animActor.getPreviewCostumeId());
            animActor.updateActionButton();
        });
        ImageButton next = createIconButton(COSTUME_NEXT_ICON, 46, 46, () -> {
            animActor.movePreview(1);
            previewPlantCostumes.put(config.id, animActor.getPreviewCostumeId());
            animActor.updateActionButton();
        });

        Label name = new Label("", skin, "main");
        name.setFontScale(0.85f);
        animActor.costumeNameLabel = name;

        TextButton action = coloredButton("", GRAY);
        action.setSize(220, 50);
        animActor.costumeActionButton = action;
        animActor.refreshCostumeUi();

        controls.add(previous).size(46, 46).padRight(8f);
        controls.add(name).width(130).center();
        controls.add(next).size(46, 46).padLeft(8f).row();
        controls.add(action).colspan(3).width(240).height(52).padTop(6f);
        return controls;
    }

    private TextButton buildActionButton(PlantJsonParser.PlantConfig config, UserState state, boolean unlocked) {
        if (!unlocked) {
            int cost = CollectionManager.getPurchaseCost();
            TextButton button = coloredButton("Purchase - " + cost + " coins", YELLOW);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    runCommand("menu collection purchase-plant -p " + config.name);
                }
            });
            return button;
        }

        int level = PlantProgression.levelOf(state, config);
        int maxLevel = PlantProgression.maxLevel(config);
        if (level >= maxLevel) {
            TextButton maxed = coloredButton("Max Level (Lv " + maxLevel + ")", GRAY);
            maxed.setDisabled(true);
            return maxed;
        }
        int coinCost = PlantProgression.upgradeCoinCost(level);
        int packetsNeeded = PlantProgression.upgradePacketsRequired(level);
        int packetsOwned = state.seedPacketInventory.getOrDefault(config.id, 0);
        boolean canUpgrade = state.coins >= coinCost && packetsOwned >= packetsNeeded;

        String label = "Upgrade to Lv " + (level + 1) + " - " + coinCost + " coins, "
                + packetsOwned + "/" + packetsNeeded + " packets";
        TextButton button = coloredButton(label, canUpgrade ? GREEN : GRAY);
        button.setDisabled(!canUpgrade);
        if (canUpgrade) {
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    runCommand("menu collection upgrade-plant -p " + config.name);
                }
            });
        }
        return button;
    }

    private Label statLabel(String text) {
        Label label = new Label(text, skin, "main");
        label.setFontScale(0.9f);
        return label;
    }

    private TextButton coloredButton(String text, Color color) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = skin.getFont("default-font");
        style.fontColor = Color.BLACK;
        style.up = roundedFilledDrawable(color, Color.WHITE, 14f, 2f);
        style.down = roundedFilledDrawable(color.cpy().mul(0.85f, 0.85f, 0.85f, 1f), Color.WHITE, 14f, 2f);
        style.disabled = roundedFilledDrawable(GRAY, Color.WHITE, 14f, 2f);
        style.disabledFontColor = new Color(0.8f, 0.8f, 0.8f, 1f);
        return new TextButton(text, style);
    }

    private class PlantIdleAnimationActor extends Actor {
        private final int plantId;
        private final String plantName;
        private final String animationPath;
        private float stateTime = 0f;
        private float offsetX = 0f;
        private float offsetY = 0f;
        private int previewIndex;
        private String previewCostumeId;
        private Label costumeNameLabel;
        private TextButton costumeActionButton;

        PlantIdleAnimationActor(String animationPath, float ox, float oy) {
            this(-1, null, animationPath, null, ox, oy);
        }

        PlantIdleAnimationActor(int plantId, String plantName, String animationPath,
                                String previewCostumeId, float ox, float oy) {
            this.plantId = plantId;
            this.plantName = plantName;
            this.animationPath = animationPath;
            this.offsetX = ox + 110f;
            this.offsetY = oy + 85f;
            setPreviewCostumeId(previewCostumeId);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += delta;
        }

        String getPreviewCostumeId() {
            return previewCostumeId;
        }

        void setPreviewCostumeId(String costumeId) {
            List<PlantCostume> costumes = PlantCostumeManager.costumesForPlant(plantName);
            previewCostumeId = null;
            previewIndex = 0;
            if (costumeId != null) {
                for (int i = 0; i < costumes.size(); i++) {
                    if (costumeId.equalsIgnoreCase(costumes.get(i).id)) {
                        previewCostumeId = costumes.get(i).id;
                        previewIndex = i + 1;
                        break;
                    }
                }
            }
            refreshCostumeUi();
        }

        void movePreview(int delta) {
            List<PlantCostume> costumes = PlantCostumeManager.costumesForPlant(plantName);
            int total = costumes.size() + 1; 
            previewIndex = (previewIndex + delta) % total;
            if (previewIndex < 0) previewIndex += total;
            previewCostumeId = previewIndex == 0 ? null : costumes.get(previewIndex - 1).id;
            stateTime = 0f;
            refreshCostumeUi();
        }

        void refreshCostumeUi() {
            if (costumeNameLabel != null) {
                costumeNameLabel.setText(previewCostumeId == null ? "Default" : previewCostumeId);
            }
            updateActionButton();
        }

        void updateActionButton() {
            if (costumeActionButton == null) return;
            costumeActionButton.clearListeners();

            UserState currentState = User.currentUser == null ? null : User.currentUser.userState;
            if (currentState == null || !currentState.isPlantUnlocked(plantId)) {
                applyCostumeButton("Unlock plant first", GRAY, true);
                return;
            }

            String selected = PlantCostumeManager.selectedCostume(plantId);
            if (previewCostumeId == null) {
                boolean chosen = selected == null;
                applyCostumeButton(chosen ? "Chosen" : "Choose", chosen ? GRAY : PURPLE, chosen);
                if (!chosen) {
                    costumeActionButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if (PlantCostumeManager.choose(plantId, plantName, null)) {
                                previewPlantCostumes.put(plantId, null);
                                build();
                            }
                        }
                    });
                }
                return;
            }

            if (!PlantCostumeManager.isOwned(plantId, previewCostumeId)) {
                boolean canBuy = User.currentUser != null && User.currentUser.userState != null
                        && User.currentUser.userState.coins >= PlantCostumeManager.COST;
                applyCostumeButton("Purchase - " + PlantCostumeManager.COST + " coins",
                        canBuy ? GREEN : GRAY, false);
                costumeActionButton.setDisabled(!canBuy);
                if (canBuy) {
                    costumeActionButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if (PlantCostumeManager.purchase(plantId, plantName, previewCostumeId)) {
                                build();
                            }
                        }
                    });
                }
                return;
            }

            boolean chosen = previewCostumeId.equalsIgnoreCase(selected == null ? "" : selected);
            applyCostumeButton(chosen ? "Chosen" : "Choose", chosen ? GRAY : PURPLE, chosen);
            if (!chosen) {
                costumeActionButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (PlantCostumeManager.choose(plantId, plantName, previewCostumeId)) {
                            build();
                        }
                    }
                });
            }
        }

        void applyCostumeButton(String text, Color color, boolean disabled) {
            TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
            style.font = skin.getFont("default-font");
            style.fontColor = Color.BLACK;
            style.up = roundedFilledDrawable(color, Color.WHITE, 14f, 2f);
            style.down = roundedFilledDrawable(color.cpy().mul(0.85f, 0.85f, 0.85f, 1f), Color.WHITE, 14f, 2f);
            style.disabled = roundedFilledDrawable(GRAY, Color.WHITE, 14f, 2f);
            style.disabledFontColor = new Color(0.8f, 0.8f, 0.8f, 1f);
            costumeActionButton.setStyle(style);
            costumeActionButton.setText(text);
            costumeActionButton.setDisabled(disabled);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (pamPlayer == null || animationPath == null) return;
            try {
                String clipName = AnimationFactory.resolveClipNameForPath(animationPath, "idle");
                if (clipName == null) return;

                ClipRef clip = pamPlayer.getClip(animationPath, clipName);
                if (clip != null) {
                    Map<String, Boolean> costumeVisibility =
                            PlantCostumeMask.forCostume(plantName, previewCostumeId);
                    costumeVisibility = PlantCostumeMask.expandHierarchy(
                            pamPlayer, animationPath, costumeVisibility);
                    if (animationPath.contains("MAGNETSHROOM")) {
                        if (costumeVisibility == null) costumeVisibility = new HashMap<>();
                        costumeVisibility.put("Magnet_Item", false);
                    }
                    if (costumeVisibility != null) {
                        pamPlayer.draw(batch, clip, stateTime, getX() + offsetX, getY() + offsetY, true, costumeVisibility);
                    } else {
                        pamPlayer.draw(batch, clip, stateTime, getX() + offsetX, getY() + offsetY, true);
                    }
                }
            } catch (Throwable t) {
                Gdx.app.error("CollectionScreen", "Failed to draw idle animation for path " + animationPath, t);
            }
        }
    }

    private class SeedProgressBar extends Widget {
        private final float progress;
        private final boolean canUpgrade;
        private final Texture upgradeTexture;

        SeedProgressBar(float progress, boolean canUpgrade, Texture upgradeTexture) {
            this.progress = Math.max(0f, Math.min(1f, progress));
            this.canUpgrade = canUpgrade;
            this.upgradeTexture = upgradeTexture;
        }

        @Override
        public float getPrefWidth() {
            return CARD_W;
        }

        @Override
        public float getPrefHeight() {
            return BAR_H;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            validate();

            float x = getX();
            float y = getY();
            float w = getWidth();
            float h = getHeight();

            batch.setColor(0.035f, 0.035f, 0.035f, parentAlpha);
            batch.draw(whitePixel, x, y, w, h);

            if (progress > 0f) {
                Color fill = canUpgrade ? GREEN : YELLOW;
                batch.setColor(fill.r, fill.g, fill.b, parentAlpha);
                batch.draw(whitePixel, x + 2f, y + 2f, Math.max(1f, (w - 4f) * progress), Math.max(1f, h - 4f));
            }

            batch.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, 0.85f * parentAlpha);
            batch.draw(whitePixel, x, y + h - 1f, w, 1f);
            batch.draw(whitePixel, x, y, w, 1f);
            batch.draw(whitePixel, x, y, 1f, h);
            batch.draw(whitePixel, x + w - 1f, y, 1f, h);

            if (canUpgrade && upgradeTexture != null) {
                batch.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, parentAlpha);
                float iconSize = h + 6f;
                batch.draw(upgradeTexture, x + 2f, y + (h - iconSize) * 0.5f, iconSize, iconSize);
            }
            batch.setColor(Color.WHITE);
        }
    }

    

    protected Texture loadTextureSafe(String path) {
        if (path != null && !path.isEmpty() && Gdx.files.internal(path).exists()) {
            Texture tex = new Texture(Gdx.files.internal(path));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return tex;
        }
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        Texture fallback = new Texture(pixmap);
        pixmap.dispose();
        return fallback;
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
        NinePatchDrawable drawable = new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(texture, 18, 18, 18, 18));
        return drawable;
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
}