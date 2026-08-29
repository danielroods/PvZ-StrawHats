package view.screens.match.before;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import controller.match.BeforeMenu;
import controller.match.CoopBeforeMenu;
import model.match.main.levels.Level;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.GameSession;
import service.card_factory.SeedPacketCard;
import service.card_factory.ZombieIconCard;
import service.card_factory.ZombieIconCardFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Co-op variant of {@link BeforeMatchScreen}. Co-op has no level-supplied zombie
 * pool - instead the human zombie player builds their own roster here, on a
 * screen shifted a bit to the right so there is room for two loadout columns:
 * the normal plant loadout on the left (unchanged from BeforeMatchScreen) and a
 * new, I-Zombie-card-scaled zombie loadout on the right. Between them sit two
 * stacked selection grids - the plant collection on top (identical to
 * BeforeMatchScreen's grid) and an equivalent zombie collection below it, built
 * the same way CollectionScreen builds its zombie tab (via ZombieIconCardFactory).
 * <p>
 * The actual add/remove-plant and add/remove-zombie mechanism is untouched: this
 * class only builds the screen and still drives everything through
 * {@code runCommand(...)} / {@link BeforeMenu#selectedZombies}, exactly like the
 * normal screen does for plants.
 */
public class CoopBeforeMatchScreen extends BeforeMatchScreen {

    private static final float ZOMBIE_LOADOUT_CARD_W = 72f;
    private static final float ZOMBIE_LOADOUT_CARD_H = 92f;

    // Scaled up (keeping the original 92x130 aspect ratio) so 6 columns of zombie cards
    // add up to the exact same row width as the plant grid's 6 columns of 104-wide cards
    // (104 + 2*2px padding = 108 per cell, x6 = 648 either way).
    private static final float ZOMBIE_GRID_CARD_W = 104f;
    private static final float ZOMBIE_GRID_CARD_H = 147f;
    private static final int ZOMBIE_GRID_COLUMNS = 6;

    /** Nudges the whole before-match layout a bit to the right for co-op. */
    private static final float SCREEN_SHIFT_RIGHT = 60f;

    /** Same background co-op's actual match uses (CouchIZombieGameScreen), not a level's season. */
    private static final String COOP_GAMEPLAY_FOLDER = "assets/images/backg/mini_games/izombie/";
    /** The dedicated right-side loadout texture for co-op's before-match screen. */
    private static final String TEXTURE_RIGHT = "assets/images/ui/texture_right.png";

    private final ZombieIconCardFactory zombieCardFactory = new ZombieIconCardFactory();
    private String previewZombieAlias = null;

    @Override
    protected void configureSeasonFolder() {
        // Co-op is not tied to any level's season - it always plays on its own map,
        // the same one the actual co-op match (CouchIZombieGameScreen) uses.
        seasonFolder = "izombie";
    }

    @Override
    protected String getSeasonGameplayFolder() {
        return COOP_GAMEPLAY_FOLDER;
    }

    @Override
    protected String getGameplayBackgroundPath() {
        return getSeasonGameplayFolder() + "texture.png";
    }

    @Override
    protected void loadRightPreviewTexture() {
        if (rightPreviewTexture != null) {
            rightPreviewTexture.dispose();
            rightPreviewTexture = null;
        }

        if (!Gdx.files.internal(TEXTURE_RIGHT).exists()) {
            return;
        }
        try {
            rightPreviewTexture = new Texture(Gdx.files.internal(TEXTURE_RIGHT));
            rightPreviewTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Throwable t) {
            Gdx.app.error("CoopBeforeMatchScreen", "Failed to load " + TEXTURE_RIGHT, t);
        }
    }

    @Override
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
        Table middle = buildCoopMiddleSection(level);

        rootTable.add(topBar).expandX().fillX().top().left()
                .padTop(8f).padLeft(12f + SCREEN_SHIFT_RIGHT).padRight(16f).row();
        rootTable.add(middle).expand().fill().top().left()
                .padTop(6f).padLeft(12f + SCREEN_SHIFT_RIGHT);

        topBar.toFront();

        startOverlay = buildBottomBar();
        startOverlay.setFillParent(true);
        addBeforeModal(startOverlay);
    }

    /** Plant loadout (left) | plant grid + zombie grid stacked (middle) | zombie loadout (right). */
    private Table buildCoopMiddleSection(Level level) {
        Table board = new Table();
        board.top().left();

        board.add(buildLoadoutPanel(level)).width(105f).expandY().fillY().top().left().padRight(10f);
        board.add(buildCoopSelectionColumn(level)).expand().fill().top().left().padRight(10f);
        board.add(buildZombieLoadoutPanel()).width(90f).expandY().fillY().top().left();

        return board;
    }

    /** First the plant selection (same as the normal screen), then the zombie selection below it. */
    private Table buildCoopSelectionColumn(Level level) {
        Table column = new Table();
        column.top().left();

        column.add(buildPreviewPanel()).expandX().fillX().padBottom(SPACE_SM).row();

        Label plantsHeader = new Label("PLANTS", skin, "title");
        plantsHeader.setFontScale(0.7f);
        column.add(plantsHeader).left().padBottom(2f).row();
        column.add(buildPlantGrid(level)).expandX().fillX().height(190f).top().left().padBottom(SPACE_SM).row();

        Label zombiesHeader = new Label("ZOMBIES", skin, "title");
        zombiesHeader.setFontScale(0.7f);
        column.add(zombiesHeader).left().padBottom(2f).row();
        column.add(buildZombieGrid()).expandX().fillX().expandY().fillY().top().left(); // اضافه شدن expandX و fillX

        return column;
    }

    private Actor buildZombieGrid() {
        UserState state = User.currentUser != null ? User.currentUser.userState : null;
        Set<String> seen = state != null ? collectionManager.getSeenZombieAliases(state) : Collections.emptySet();

        List<String> aliases = new ArrayList<>(collectionManager.getAllZombieAliases());
        Collections.sort(aliases);

        Table grid = new Table();
        grid.top().left();

        Table currentRow = null;
        for (int i = 0; i < aliases.size(); i++) {
            if (i % ZOMBIE_GRID_COLUMNS == 0) {
                currentRow = new Table();
                grid.add(currentRow).left().padBottom(SPACE_XS).row();
            }
            String alias = aliases.get(i);
            boolean locked = !seen.contains(alias);
            Actor card = buildZombieCard(alias, locked);
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
    private float getPlantCardWidth() {
        try {
            SeedPacketCard sample = cardFactory.buildCardForDisplayName("Peashooter");
            if (sample != null && sample.getWidth() > 0) {
                return sample.getWidth();
            }
        } catch (Throwable ignored) {}
        return 104f;
    }

    private Actor buildZombieCard(String alias, boolean locked) {
        Stack cardStack = new Stack();

        float cardW = getPlantCardWidth();
        float cardH = cardW * (147f / 104f);

        try {
            ZombieIconCard card = zombieCardFactory.buildCardForAlias(alias, cardW, cardH);
            if (card != null) {
                cardStack.add(card);
            }
        } catch (Throwable ignored) {
        }

        if (cardStack.getChildren().isEmpty()) {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            Label label = new Label(alias, skin, "main");
            label.setAlignment(Align.center);
            label.setFontScale(0.7f);
            label.setWrap(true);
            fallback.add(label).size(cardW, cardH);
            cardStack.add(fallback);
        }

        cardStack.setSize(cardW, cardH);

        if (locked) {
            Image dim = new Image(solidColorDrawable(new Color(0f, 0f, 0f, 0.65f)));
            dim.setScaling(Scaling.stretch);
            cardStack.add(dim);

            Image lockImage = new Image(loadTextureSafe(LOCK_ICON));
            lockImage.setScaling(Scaling.stretch);
            Container<Image> lockContainer = new Container<>(lockImage);
            lockContainer.size(22f, 22f);
            lockContainer.center();
            cardStack.add(lockContainer);
        }

        Table cell = new Table();
        cell.add(cardStack).size(cardW, cardH);

        cardStack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                previewZombieAlias = alias;
                if (!locked) {
                    toggleZombie(alias);
                } else {
                    scheduleBuild();
                }
            }
        });

        return cell;
    }

    private Table buildZombieLoadoutPanel() {
        Table panel = new Table();
        panel.setBackground(skin.getDrawable("card-background"));
        panel.pad(2f).top();

        Table slots = new Table();
        slots.top();

        for (int i = 0; i < CoopBeforeMenu.ZOMBIE_SLOTS; i++) {
            String alias = i < BeforeMenu.selectedZombies.size() ? BeforeMenu.selectedZombies.get(i) : null;

            if (alias == null) {
                Actor emptySlot = createEmptyLoadoutSlot("Empty", false, null);
                slots.add(emptySlot).size(ZOMBIE_LOADOUT_CARD_W, ZOMBIE_LOADOUT_CARD_H).pad(1f).row();
                continue;
            }

            final String selected = alias;
            Stack stack = new Stack();
            try {
                ZombieIconCard card = zombieCardFactory.buildCardForAlias(alias, ZOMBIE_LOADOUT_CARD_W, ZOMBIE_LOADOUT_CARD_H);
                if (card != null) {
                    card.setTouchable(Touchable.disabled);
                    stack.add(card);
                }
            } catch (Throwable ignored) {
            }

            if (stack.getChildren().isEmpty()) {
                Table fallback = new Table();
                fallback.setBackground(skin.getDrawable("card-background"));
                Label label = new Label(alias, skin, "main");
                label.setAlignment(Align.center);
                label.setFontScale(0.6f);
                label.setWrap(true);
                fallback.add(label).width(ZOMBIE_LOADOUT_CARD_W).center();
                stack.add(fallback);
            }

            stack.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    previewZombieAlias = selected;
                    removeZombie(selected);
                }
            });

            slots.add(stack).size(ZOMBIE_LOADOUT_CARD_W, ZOMBIE_LOADOUT_CARD_H).pad(1f).row();
        }

        ScrollPane scrollPane = new ScrollPane(slots);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);

        panel.add(scrollPane).expand().fill().row();

        Label info = new Label(BeforeMenu.selectedZombies.size() + "/" + CoopBeforeMenu.ZOMBIE_SLOTS, skin, "main");
        info.setAlignment(Align.center);
        info.setFontScale(0.8f);
        panel.add(info).padTop(2).padBottom(2);

        return panel;
    }

    private void toggleZombie(String alias) {
        if (BeforeMenu.selectedZombies.stream().anyMatch(z -> z.equalsIgnoreCase(alias))) {
            removeZombie(alias);
            return;
        }
        if (BeforeMenu.selectedZombies.size() >= CoopBeforeMenu.ZOMBIE_SLOTS) {
            scheduleBuild();
            return;
        }
        runCommand("add zombie -t " + alias);
        scheduleBuild();
    }

    private void removeZombie(String alias) {
        runCommand("remove zombie -t " + alias);
        scheduleBuild();
    }

    @Override
    public void dispose() {
        zombieCardFactory.dispose();
        super.dispose();
    }
}