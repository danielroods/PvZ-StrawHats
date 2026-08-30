package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.item.GroundItem;
import model.match.mini_games.izombie.Brain;
import model.match.mini_games.izombie.IZombieMatch;
import model.utils.GameSession;
import net.Protocol;
import net.client.NetMatchState;
import net.client.NetworkClient;
import net.dto.MatchSnapshot;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;
import service.card_factory.ZombieIconCard;
import service.card_factory.ZombieIconCardFactory;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;
import view.screens.generals.Toast;

import java.util.ArrayList;
import java.util.List;

public class NetIZombieGameScreen extends GameScreen {

    private static final float BRAIN_WIDTH_FACTOR = 0.72f;
    private static final float PLANT_CARD_W = 95f;
    private static final float PLANT_CARD_H = 60f;
    private static final float ZOMBIE_CARD_W = 72f;
    private static final float ZOMBIE_CARD_H = 92f;
    private static final float TRAY_LEFT = 8f;
    private static final float TRAY_BOTTOM = 26f;
    private static final float SIDE_AREA_WIDTH = 150f;
    private static final String TEXTURE_RIGHT = "assets/images/ui/texture_right.png";
    private static final Color RED_LINE_COLOR = new Color(0.88f, 0.16f, 0.14f, 0.8f);
    private static final Color PLANT_ZONE = new Color(0.45f, 0.95f, 0.45f, 0.10f);
    private static final Color HOVER_VALID_TINT = new Color(0.55f, 1f, 0.55f, 0.30f);
    private static final Color HOVER_INVALID_TINT = new Color(1f, 0.35f, 0.30f, 0.25f);

    {
        seasonFolder = "izombie";
    }

    private static final class CardView {
        final String key;
        final Group stack;
        final Image unavailable;
        final Image selected;
        final Label costLabel;
        final Label cooldownLabel;

        CardView(String key, Group stack, Image unavailable, Image selected, Label costLabel, Label cooldownLabel) {
            this.key = key;
            this.stack = stack;
            this.unavailable = unavailable;
            this.selected = selected;
            this.costLabel = costLabel;
            this.cooldownLabel = cooldownLabel;
        }
    }

    private final ZombieIconCardFactory zombieCards = new ZombieIconCardFactory();
    private final SeedPacketCardFactory seedCards = new SeedPacketCardFactory();
    private final List<CardView> cardViews = new ArrayList<>();

    private NetMatchState state;
    private Table tray;
    private Label trayTitle;
    private Label opponentLabel;
    private Table reactionBar;
    private ReactionOverlay reactionOverlay;
    private Texture brainTexture;
    private Texture textureRight;
    private String selectedKey;
    private int builtCardCount;
    private float brainAnimTime;
    private float ghostAnimTime;
    private boolean shovelArmed;
    private boolean foodArmed;
    private boolean endHandled;
    private MatchStartOverlay matchStartOverlay;

    @Override
    protected String getSeasonGameplayFolder() {
        return "assets/images/backg/mini_games/izombie/";
    }

    @Override
    protected String getGameplayBackgroundPath() {
        return getSeasonGameplayFolder() + "texture.png";
    }

    @Override
    protected String getGraveIconPath() {
        return "";
    }

    @Override
    protected boolean areLawnMowersVisible() {
        return false;
    }

    @Override
    protected float reservedRightAreaWidth() {
        return SIDE_AREA_WIDTH;
    }

    private boolean isPlantSide() {
        return state == null || state.isPlantSide();
    }

    @Override
    public void show() {
        state = NetworkClient.get().getMatchState();
        if (state != null) GameSession.setCurrent(state.getShadowSession());
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);

        brainTexture = loadBrainTexture();
        textureRight = loadTextureRight();
        if (hud != null) {
            hud.setLoadoutBankVisible(false);
            hud.setShovelVisible(isPlantSide());
            hud.setCheatButtonsVisible(false, false, false);
            hud.setFoodVisible(isPlantSide());
            hud.setStartButtonAvailable(false);
            hud.setObjectiveOverride(isPlantSide() ? "PROTECT THE BRAINZ" : "EAT ALL THE BRAINZ");
            hud.setShovelAction(() -> {
                shovelArmed = !shovelArmed;
                foodArmed = false;
                if (shovelArmed) selectedKey = null;
            });
            hud.setFoodAction(() -> {
                foodArmed = !foodArmed;
                shovelArmed = false;
                if (foodArmed) selectedKey = null;
            });
        }
        buildTray();
        buildOpponentPanel();
        buildReactionBar();
        reactionOverlay = new ReactionOverlay(skin, this::loadTextureSafe);
        stage.addActor(reactionOverlay);

        matchStartOverlay = new MatchStartOverlay(this);
        matchStartOverlay.start();
    }

    private Texture loadBrainTexture() {
        String path = getSeasonGameplayFolder() + "brain.png";
        if (!Gdx.files.internal(path).exists() && path.startsWith("assets/")) {
            path = path.substring("assets/".length());
        }
        return loadTextureSafe(path);
    }

    private Texture loadTextureRight() {
        if (!Gdx.files.internal(TEXTURE_RIGHT).exists()) {
            return null;
        }
        try {
            Texture texture = new Texture(Gdx.files.internal(TEXTURE_RIGHT));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return texture;
        } catch (Throwable t) {
            Gdx.app.error("NetIZombieGameScreen", "Failed to load " + TEXTURE_RIGHT, t);
            return null;
        }
    }

    @Override
    public void dispose() {
        zombieCards.dispose();
        seedCards.dispose();
        if (brainTexture != null) brainTexture.dispose();
        if (textureRight != null) textureRight.dispose();
        super.dispose();
    }

    @Override
    protected void tickSession() {
        // The server owns the simulation; this client only renders what it is told.
    }

    @Override
    public void render(float delta) {
        boolean starting = matchStartOverlay != null && matchStartOverlay.isActive();
        if (starting) matchStartOverlay.advance(delta);

        if (state != null) {
            GameSession.setCurrent(state.getShadowSession());
            List<model.collections.zombie.Zombie> removed = state.drainRemovedZombies();
            if (!removed.isEmpty()) trackZombieDeaths(removed);
            List<model.collections.plant.Plant> uprooted = state.drainRemovedPlants();
            if (!uprooted.isEmpty()) trackPlantDeaths(uprooted);
            // Freeze the board's own visible progress while the VS splash plays;
            // the server-side match keeps running underneath regardless.
            if (!starting) state.advance(delta);

            String rejection = state.pollRejection();
            if (rejection != null && stage != null) Toast.show(stage, rejection);

            if (reactionOverlay != null) {
                reactionOverlay.update(state.getIncomingReaction(), state.getOpponentNickname());
            }
        }
        super.render(delta);
    }

    @Override
    protected void drawMatchStartOverlay() {
        if (matchStartOverlay != null) matchStartOverlay.draw();
    }

    @Override
    protected void checkMatchEnd() {
        if (endHandled || state == null || !state.isEnded()) return;
        endHandled = true;
        matchFinished = true;
        boolean won = state.isWon();
        String reason = state.getEndReason();
        NetworkClient.get().clearMatchState();
        model.App.currentMenu = new controller.match.mini_games.MiniGameEndMenu(
                "Online I, Zombie", won, reason);
    }

    @Override
    protected void refreshHud(float delta) {
        super.refreshHud(delta);
        if (hud == null || state == null) return;
        MatchSnapshot snapshot = state.getSnapshot();
        double remaining = state.getRemainingSeconds();
        double total = Math.max(1.0, state.getMatchSeconds());
        float progress = (float) Math.max(0, Math.min(1, 1.0 - remaining / total));
        hud.setProgressOverride(String.format("BRAINZ %d/5   %02d:%02d",
                state.getBrainsEaten(), (int) (remaining / 60), (int) (remaining % 60)), progress);
        hud.setTools(shovelArmed, foodArmed);
        if (snapshot != null) updateTray(snapshot);
        if (opponentLabel != null) {
            opponentLabel.setText(state.getOpponentNickname() + "\n"
                    + (isPlantSide() ? "ZOMBIES" : "PLANTS"));
        }
    }

    @Override
    protected void onCellClicked(int row, int col) {
        if (state == null) return;
        NetworkClient client = NetworkClient.get();

        if (isPlantSide()) {
            if (shovelArmed) {
                client.sendIntent(Protocol.INTENT_DIG, null, row, col);
                shovelArmed = false;
                AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.6f);
                return;
            }
            if (foodArmed) {
                client.sendIntent(Protocol.INTENT_USE_PLANT_FOOD, null, row, col);
                foodArmed = false;
                AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.6f);
                return;
            }
            if (selectedKey == null) {
                Toast.show(stage, "Pick a seed packet from the tray first.");
                return;
            }
            if (col <= IZombieMatch.BRAIN_COLUMN || col > IZombieMatch.REDLINE_COLUMN) {
                Toast.show(stage, "Plant between the brains and the red line.");
                return;
            }
            client.sendIntent(Protocol.INTENT_PLANT, selectedKey, row, col);
            selectedKey = null;
            AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.6f);
            return;
        }

        if (selectedKey == null) {
            Toast.show(stage, "Pick a zombie from the tray first.");
            return;
        }
        if (col <= IZombieMatch.REDLINE_COLUMN) {
            Toast.show(stage, "Zombies drop in to the right of the red line.");
            return;
        }
        client.sendIntent(Protocol.INTENT_PLACE_ZOMBIE, selectedKey, row, col);
        selectedKey = null;
        AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.6f);
    }

    @Override
    protected boolean collectUnderMouse(Vector2 click) {
        if (state == null || !isPlantSide()) return false;
        GroundItem item = itemUnderMouse(click);
        if (item == null || item.getPosition() == null) return false;
        int row = (int) Math.round(item.getPosition().y());
        int col = (int) Math.round(item.getPosition().x());
        NetworkClient.get().sendIntent(Protocol.INTENT_COLLECT_SUN, null, row, col);
        AudioManager.get().playSound(AudioEnum.SFX_ITEM_COLLECT);
        return true;
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        drawSideTexture();
        if (state == null) return;
        brainAnimTime += delta;
        drawPlacementZone(bh);
        for (int col = IZombieMatch.BRAIN_COLUMN + 1;
             col <= IZombieMatch.REDLINE_COLUMN; col++) {
            drawFallback(getCellX(col), getBoardBottom(), getBoardTileWidth(), bh, PLANT_ZONE);
        }
        drawRedLine(bh);
        drawBrains();
    }

    private void drawSideTexture() {
        float viewW = stage.getViewport().getWorldWidth();
        float viewH = stage.getViewport().getWorldHeight();
        float x = viewW - SIDE_AREA_WIDTH;

        batch.setColor(Color.WHITE);
        if (textureRight != null) {
            batch.draw(textureRight, x, 0f, SIDE_AREA_WIDTH, viewH);
        } else {
            batch.setColor(0.08f, 0.07f, 0.05f, 0.94f);
            batch.draw(whitePixel, x, 0f, SIDE_AREA_WIDTH, viewH);
            batch.setColor(Color.WHITE);
        }
    }

    @Override
    protected void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        super.drawSeasonForegroundEffects(delta, bw, bh);
        if (state == null || selectedKey == null) return;

        ghostAnimTime += delta;
        Vector2 mouse = mouseWorld();
        int col = (int) ((mouse.x - getCellX(0)) / getBoardTileWidth());
        int row = session.getRows() - 1
                - (int) ((mouse.y - getBoardBottom()) / getBoardTileHeight());
        boolean onBoard = row >= 0 && row < session.getRows()
                && col >= 0 && col < session.getCols();
        if (onBoard) {
            boolean placeable = isPlantSide()
                    ? col > IZombieMatch.BRAIN_COLUMN && col <= IZombieMatch.REDLINE_COLUMN
                    : col > IZombieMatch.REDLINE_COLUMN;
            drawFallback(getCellX(col), getCellY(row), getBoardTileWidth(), getBoardTileHeight(),
                    placeable ? HOVER_VALID_TINT : HOVER_INVALID_TINT);
        }

        String path = isPlantSide()
                ? AnimationFactory.pathForDisplayName(selectedKey)
                : ZombieAnimationRegistry.pathFor(selectedKey, seasonFolder);
        if (path == null) return;
        batch.setColor(1f, 1f, 1f, isPlantSide() ? 0.85f : 0.7f);
        if (isPlantSide()) {
            float size = getBoardTileWidth() * 0.8f;
            drawPam(path, "idle", ghostAnimTime, mouse.x - size * 0.35f, mouse.y - size * 0.5f,
                    0.5f, false);
        } else {
            drawPam(path, "idle", ghostAnimTime,
                    mouse.x - 10f, mouse.y - getBoardTileHeight() * 0.35f, 0.52f, false);
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Highlights the drop zone while a card is held. Only the zombie player needs it -
     * the plant player's own columns are already tinted for the whole match below, and
     * painting them twice just doubled the tint.
     */
    private void drawPlacementZone(float bh) {
        if (selectedKey == null || isPlantSide()) return;
        for (int col = IZombieMatch.REDLINE_COLUMN + 1; col < session.getCols(); col++) {
            drawFallback(getCellX(col), getBoardBottom(), getBoardTileWidth(), bh,
                    PLANT_ZONE);
        }
    }

    private void drawRedLine(float bh) {
        float redLineX = getCellX(IZombieMatch.REDLINE_COLUMN + 1);
        drawFallback(redLineX - 2f, getBoardBottom(), 4f, bh, RED_LINE_COLOR);
    }

    private void drawBrains() {
        MatchSnapshot snapshot = state.getSnapshot();
        if (snapshot == null || snapshot.brains == null) return;
        float tileW = getBoardTileWidth();
        float tileH = getBoardTileHeight();
        float width = tileW * BRAIN_WIDTH_FACTOR;
        float height = brainTexture == null || brainTexture.getWidth() == 0
                ? width * 0.74f : width * brainTexture.getHeight() / brainTexture.getWidth();

        for (int row = 0; row < snapshot.brains.length; row++) {
            int hp = snapshot.brains[row];
            if (hp <= 0) continue;
            float bite = Math.max(0f, Math.min(1f, hp / (float) Brain.BRAIN_HP));
            float scale = 0.72f + 0.28f * bite;
            float centerX = getCellX(IZombieMatch.BRAIN_COLUMN) + tileW * 0.5f;
            float centerY = getCellY(row) + tileH * 0.42f;
            float bob = (float) Math.sin(brainAnimTime * 2.2f + row) * tileH * 0.02f;
            batch.setColor(1f, 0.6f + 0.4f * bite, 0.6f + 0.4f * bite, 1f);
            if (brainTexture != null && brainTexture.getWidth() > 1) {
                batch.draw(brainTexture, centerX - width * scale * 0.5f,
                        centerY + bob - height * scale * 0.5f, width * scale, height * scale);
            } else {
                drawFallback(centerX - width * scale * 0.5f, centerY + bob - height * scale * 0.5f,
                        width * scale, height * scale, new Color(0.94f, 0.45f, 0.55f, 1f));
            }
            batch.setColor(Color.WHITE);
        }
    }

    private Vector2 mouseWorld() {
        Vector3 coordinates = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        stage.getViewport().unproject(coordinates);
        return new Vector2(coordinates.x, coordinates.y);
    }

    private void buildOpponentPanel() {
        Table panel = new Table();
        panel.setBackground(skin.getDrawable("card-background"));
        panel.pad(6f);
        Label youAre = new Label(state == null ? "?" : "YOU: " + state.getRole().name(),
                skin, "main");
        youAre.setFontScale(0.7f);
        youAre.setAlignment(Align.center);
        opponentLabel = new Label("", skin, "muted");
        opponentLabel.setFontScale(0.65f);
        opponentLabel.setAlignment(Align.center);
        panel.add(youAre).width(126f).center().row();
        panel.add(opponentLabel).width(126f).center();
        panel.pack();
        panel.setPosition(stage.getWidth() - SIDE_AREA_WIDTH
                        + (SIDE_AREA_WIDTH - panel.getWidth()) * 0.5f,
                stage.getHeight() - panel.getHeight() - 96f);
        stage.addActor(panel);
    }

    private void buildTray() {
        if (tray != null) {
            tray.remove();
            tray = null;
        }
        cardViews.clear();
        builtCardCount = 0;
        if (state == null) return;

        Table built = new Table();
        built.setBackground(skin.getDrawable("card-background"));
        built.pad(2f).top();

        MatchSnapshot snapshot = state.getSnapshot();
        float cardWidth = isPlantSide() ? PLANT_CARD_W : ZOMBIE_CARD_W;

        trayTitle = new Label(isPlantSide() ? "PLANTS" : "ZOMBIES", skin, "main");
        trayTitle.setFontScale(0.7f);
        trayTitle.setAlignment(Align.center);
        trayTitle.setWrap(true);
        if (!isPlantSide()) {
            built.add(trayTitle).width(cardWidth).padBottom(2f).row();
        }

        if (snapshot != null) {
            if (isPlantSide()) {
                for (MatchSnapshot.SeedDto seed : snapshot.seeds) {
                    CardView view = buildSeedCard(seed);
                    cardViews.add(view);
                    built.add(wrapWithCardFrame(view.stack, PLANT_CARD_W, PLANT_CARD_H))
                            .size(PLANT_CARD_W, PLANT_CARD_H).pad(1f).row();
                }
                builtCardCount = snapshot.seeds.size();
            } else {
                for (MatchSnapshot.PacketDto packet : snapshot.packets) {
                    CardView view = buildPacketCard(packet);
                    cardViews.add(view);
                    built.add(view.stack).size(ZOMBIE_CARD_W, ZOMBIE_CARD_H).pad(1f).row();
                }
                builtCardCount = snapshot.packets.size();
            }
        }

        if (isPlantSide()) {
            built.add(trayTitle).width(cardWidth).padTop(2f).row();
        }

        built.pack();
        if (isPlantSide()) {
            built.setPosition(TRAY_LEFT, TRAY_BOTTOM);
        } else {
            built.setPosition(stage.getWidth() - SIDE_AREA_WIDTH
                    + (SIDE_AREA_WIDTH - built.getWidth()) * 0.5f, TRAY_BOTTOM);
        }
        tray = built;
        stage.addActor(tray);
        if (!cardViews.isEmpty()) tray.toFront();
    }

    private CardView buildPacketCard(MatchSnapshot.PacketDto packet) {
        Group stack = new Group();
        stack.setTouchable(Touchable.enabled);
        stack.setSize(ZOMBIE_CARD_W, ZOMBIE_CARD_H);
        try {
            ZombieIconCard card = zombieCards.buildCardForAlias(packet.alias,
                    ZOMBIE_CARD_W, ZOMBIE_CARD_H);
            if (card != null) {
                card.setTouchable(Touchable.disabled);
                card.setBounds(0f, 0f, ZOMBIE_CARD_W, ZOMBIE_CARD_H);
                stack.addActor(card);
            }
        } catch (Throwable ignored) {
            // Falls through to the text placeholder below.
        }
        if (stack.getChildren().isEmpty()) {
            Actor fallback = placeholderCard(packet.label, ZOMBIE_CARD_W);
            fallback.setBounds(0f, 0f, ZOMBIE_CARD_W, ZOMBIE_CARD_H);
            stack.addActor(fallback);
        }
        return finishCard(stack, packet.alias, String.valueOf(packet.cost), 0.6f);
    }

    private CardView buildSeedCard(MatchSnapshot.SeedDto seed) {
        Group stack = new Group();
        stack.setTouchable(Touchable.enabled);
        stack.setSize(PLANT_CARD_W, PLANT_CARD_H);
        try {
            SeedPacketCard card = seedCards.buildCardByPlantName(seed.name);
            if (card != null) {
                card.setSize(PLANT_CARD_W, PLANT_CARD_H);
                card.setTouchable(Touchable.disabled);
                card.setBounds(0f, 0f, PLANT_CARD_W, PLANT_CARD_H);
                stack.addActor(card);
            }
        } catch (Throwable ignored) {
        }
        if (stack.getChildren().isEmpty()) {
            Actor fallback = placeholderCard(seed.name, PLANT_CARD_W);
            fallback.setBounds(0f, 0f, PLANT_CARD_W, PLANT_CARD_H);
            stack.addActor(fallback);
        }
        return finishCard(stack, seed.name, String.valueOf(seed.cost), 0.7f);
    }

    private Table placeholderCard(String text, float width) {
        Table fallback = new Table();
        fallback.setBackground(skin.getDrawable("card-background"));
        Label label = new Label(text == null ? "?" : text, skin, "main");
        label.setAlignment(Align.center);
        label.setFontScale(0.65f);
        label.setWrap(true);
        fallback.add(label).width(width).center();
        return fallback;
    }

    private CardView finishCard(Group stack, String key, String cost, float costScale) {
        Image unavailable = new Image(new TextureRegionDrawable(whitePixelRegion()));
        unavailable.setColor(0f, 0f, 0f, 0.6f);
        unavailable.setFillParent(true);
        unavailable.setTouchable(Touchable.disabled);
        unavailable.setBounds(0f, 0f, stack.getWidth(), stack.getHeight());
        stack.addActor(unavailable);

        Image selected = new Image(new TextureRegionDrawable(whitePixelRegion()));
        selected.setColor(isPlantSide() ? 0.25f : 0.95f, isPlantSide() ? 1f : 0.55f, 0.25f, 0.30f);
        selected.setFillParent(true);
        selected.setTouchable(Touchable.disabled);
        selected.setBounds(0f, 0f, stack.getWidth(), stack.getHeight());
        stack.addActor(selected);

        Label costLabel = new Label(cost, skin, "main");
        costLabel.setFontScale(costScale);
        costLabel.setAlignment(Align.bottomRight);
        costLabel.setTouchable(Touchable.disabled);
        costLabel.setBounds(0f, 0f, stack.getWidth() - 2f, stack.getHeight() - 2f);
        stack.addActor(costLabel);

        Label cooldownLabel = new Label("", skin, "title");
        cooldownLabel.setFontScale(0.85f);
        cooldownLabel.setAlignment(Align.center);
        cooldownLabel.setTouchable(Touchable.disabled);
        cooldownLabel.setBounds(0f, 0f, stack.getWidth(), stack.getHeight());
        stack.addActor(cooldownLabel);

        stack.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedKey = key.equalsIgnoreCase(selectedKey) ? null : key;
                shovelArmed = false;
                foodArmed = false;
                AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.5f);
            }
        });

        return new CardView(key, stack, unavailable, selected, costLabel, cooldownLabel);
    }

    private Actor wrapWithCardFrame(Actor content, float outerW, float outerH) {
        Table framed = new Table();
        framed.setBackground(skin.getDrawable("card-background"));
        framed.pad(3f);
        framed.add(content).size(outerW - 6f, outerH - 6f);
        return framed;
    }

    private void updateTray(MatchSnapshot snapshot) {
        int available = isPlantSide() ? snapshot.seeds.size() : snapshot.packets.size();
        if (tray == null || available != builtCardCount) {
            buildTray();
            return;
        }
        int sun = state.getMySun();
        for (CardView view : cardViews) {
            double cooldown = 0;
            int cost = 0;
            if (isPlantSide()) {
                for (MatchSnapshot.SeedDto seed : snapshot.seeds) {
                    if (seed.name.equals(view.key)) {
                        cooldown = seed.cooldown;
                        cost = seed.cost;
                    }
                }
            } else {
                for (MatchSnapshot.PacketDto packet : snapshot.packets) {
                    if (packet.alias.equals(view.key)) {
                        cooldown = packet.cooldown;
                        cost = packet.cost;
                    }
                }
            }
            boolean ready = cooldown <= 0.0001;
            boolean affordable = sun >= cost;
            view.unavailable.setVisible(!ready || !affordable);
            view.selected.setVisible(view.key.equalsIgnoreCase(selectedKey));
            view.costLabel.setText(String.valueOf(cost));
            view.costLabel.setVisible(true);
            if (!ready) {
                view.cooldownLabel.setText(String.format("%.1f", cooldown));
                view.cooldownLabel.setVisible(true);
            } else {
                view.cooldownLabel.setVisible(false);
            }
        }
        if (trayTitle != null) {
            trayTitle.setText("SUN " + sun);
        }
    }

    private void buildReactionBar() {
        Table bar = new Table();
        bar.setBackground(skin.getDrawable("card-background"));
        bar.pad(6f);

        Label title = new Label("SAY SOMETHING", skin, "main");
        title.setFontScale(0.6f);
        bar.add(title).colspan(3).padBottom(4f).row();

        for (int i = 0; i < Protocol.REACTION_TEXTS.length; i++) {
            int index = i;
            bar.add(reactionButton(Protocol.REACTION_TEXTS[i], 104f,
                            () -> NetworkClient.get().sendReaction(Protocol.REACTION_TEXT, index)))
                    .padRight(3f);
        }
        for (int i = 0; i < 3; i++) {
            int index = i;
            bar.add(reactionButton(ReactionOverlay.EMOJI_LABELS[i], 34f,
                            () -> NetworkClient.get().sendReaction(Protocol.REACTION_EMOJI, index)))
                    .padRight(3f);
        }
        for (int i = 0; i < 3; i++) {
            int index = i;
            bar.add(reactionButton(ReactionOverlay.STICKER_LABELS[i], 34f,
                            () -> NetworkClient.get().sendReaction(Protocol.REACTION_STICKER, index)))
                    .padRight(3f);
        }

        bar.pack();
        bar.setPosition(stage.getWidth() - SIDE_AREA_WIDTH - bar.getWidth() - 10f, 6f);
        reactionBar = bar;
        stage.addActor(reactionBar);
    }

    private Table reactionButton(String label, float width, Runnable action) {
        Table button = new Table();
        button.setBackground(skin.getDrawable("button-up"));
        Label text = new Label(label, skin, "main");
        text.setFontScale(0.58f);
        text.setAlignment(Align.center);
        button.add(text).center().expand();
        button.setTouchable(Touchable.enabled);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                action.run();
                AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.5f);
            }
        });
        Table wrapper = new Table();
        wrapper.add(button).size(width, 26f);
        return wrapper;
    }

    @Override
    public void onMatchEndSequenceFinished(boolean won) {
        // MATCH_END from the server is the only thing that ends a networked match.
    }
}
