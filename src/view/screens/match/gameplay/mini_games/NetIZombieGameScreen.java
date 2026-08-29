package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import controller.match.mini_games.NetIZombieController;
import model.App;
import model.collections.animations.ZombieAnimationRegistry;
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
    private static final float CARD_W = 88f;
    private static final float CARD_H = 112f;
    private static final float TRAY_LEFT = 10f;
    private static final float TRAY_BOTTOM = 26f;
    private static final Color RED_LINE_COLOR = new Color(0.88f, 0.16f, 0.14f, 0.8f);
    private static final Color PLACEABLE_TINT = new Color(0.45f, 0.95f, 0.45f, 0.12f);
    private static final Color HOVER_VALID_TINT = new Color(0.55f, 1f, 0.55f, 0.30f);
    private static final Color HOVER_INVALID_TINT = new Color(1f, 0.35f, 0.30f, 0.25f);

    {
        seasonFolder = "izombie";
    }

    private static final class CardView {
        final String key;
        final Stack stack;
        final Image unavailable;
        final Image selected;
        final Label cooldownLabel;

        CardView(String key, Stack stack, Image unavailable, Image selected, Label cooldownLabel) {
            this.key = key;
            this.stack = stack;
            this.unavailable = unavailable;
            this.selected = selected;
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
    private String selectedKey;
    private float brainAnimTime;
    private float ghostAnimTime;
    private boolean shovelArmed;
    private boolean readySent;
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
    public void show() {
        state = NetworkClient.get().getMatchState();
        if (state != null) GameSession.setCurrent(state.getShadowSession());
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);

        brainTexture = loadBrainTexture();
        if (hud != null) {
            hud.setLoadoutBankVisible(false);
            hud.setShovelVisible(state != null && state.isPlantSide());
            hud.setStartButtonAvailable(false);
            hud.setObjectiveOverride(state != null && state.isPlantSide()
                    ? "PROTECT THE BRAINZ" : "EAT ALL THE BRAINZ");
            hud.setShovelAction(() -> {
                shovelArmed = !shovelArmed;
                if (shovelArmed) selectedKey = null;
            });
        }
        buildTray();
        buildOpponentPanel();
        buildReactionBar();
        reactionOverlay = new ReactionOverlay(skin, this::loadTextureSafe);
        stage.addActor(reactionOverlay);

        matchStartOverlay = new MatchStartOverlay(this);
        matchStartOverlay.start();

        if (!readySent && App.currentMenu instanceof NetIZombieController controller) {
            controller.sendReady();
            readySent = true;
        }
    }

    private Texture loadBrainTexture() {
        String path = getSeasonGameplayFolder() + "brain.png";
        if (!Gdx.files.internal(path).exists() && path.startsWith("assets/")) {
            path = path.substring("assets/".length());
        }
        return loadTextureSafe(path);
    }

    @Override
    public void dispose() {
        zombieCards.dispose();
        seedCards.dispose();
        if (brainTexture != null) brainTexture.dispose();
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
        App.currentMenu = new controller.match.mini_games.MiniGameEndMenu(
                "Online I, Zombie", won, reason);
    }

    @Override
    protected void refreshHud(float delta) {
        super.refreshHud(delta);
        if (hud == null || state == null) return;
        MatchSnapshot snapshot = state.getSnapshot();
        double remaining = state.getRemainingSeconds();
        float progress = (float) Math.max(0, Math.min(1,
                1.0 - remaining / IZombieMatch.MATCH_SECONDS));
        int eaten = state.getBrainsEaten();
        hud.setProgressOverride(String.format("BRAINZ %d/5   %02d:%02d",
                eaten, (int) (remaining / 60), (int) (remaining % 60)), progress);
        hud.setTools(shovelArmed, false);
        if (snapshot != null) updateTray(snapshot);
        if (opponentLabel != null) {
            opponentLabel.setText(state.getOpponentNickname() + "  -  "
                    + (state.isPlantSide() ? "ZOMBIES" : "PLANTS"));
        }
    }

    @Override
    protected void onCellClicked(int row, int col) {
        if (state == null) return;
        NetworkClient client = NetworkClient.get();

        if (state.isPlantSide()) {
            if (shovelArmed) {
                client.sendIntent(Protocol.INTENT_DIG, null, row, col);
                shovelArmed = false;
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
            Toast.show(stage, "Pick a zombie from the tray on the left first.");
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
        if (state == null || !state.isPlantSide()) return false;
        int col = (int) ((click.x - getCellX(0)) / getBoardTileWidth());
        int row = session.getRows() - 1
                - (int) ((click.y - getBoardBottom()) / getBoardTileHeight());
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) {
            return false;
        }
        MatchSnapshot snapshot = state.getSnapshot();
        if (snapshot == null) return false;
        for (MatchSnapshot.GroundItemDto item : snapshot.items) {
            if (Math.abs(item.x - col) <= 0.75 && Math.abs(item.y - row) <= 0.75) {
                NetworkClient.get().sendIntent(Protocol.INTENT_COLLECT_SUN, null, row, col);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        if (state == null) return;
        brainAnimTime += delta;
        drawPlacementZone(bh);
        drawRedLine(bh);
        drawBrains();
    }

    @Override
    protected void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        super.drawSeasonForegroundEffects(delta, bw, bh);
        drawNetworkProjectiles();
        if (state == null || selectedKey == null) return;

        ghostAnimTime += delta;
        Vector2 mouse = mouseWorld();
        int col = (int) ((mouse.x - getCellX(0)) / getBoardTileWidth());
        int row = session.getRows() - 1
                - (int) ((mouse.y - getBoardBottom()) / getBoardTileHeight());
        boolean onBoard = row >= 0 && row < session.getRows()
                && col >= 0 && col < session.getCols();
        if (onBoard) {
            boolean placeable = state.isPlantSide()
                    ? col > IZombieMatch.BRAIN_COLUMN && col <= IZombieMatch.REDLINE_COLUMN
                    : col > IZombieMatch.REDLINE_COLUMN;
            drawFallback(getCellX(col), getCellY(row), getBoardTileWidth(), getBoardTileHeight(),
                    placeable ? HOVER_VALID_TINT : HOVER_INVALID_TINT);
        }

        if (!state.isPlantSide()) {
            String path = ZombieAnimationRegistry.pathFor(selectedKey, seasonFolder);
            batch.setColor(1f, 1f, 1f, 0.7f);
            drawPam(path, "idle", ghostAnimTime,
                    mouse.x - 10f, mouse.y - getBoardTileHeight() * 0.35f, 0.52f, false);
            batch.setColor(Color.WHITE);
        }
    }

    private void drawNetworkProjectiles() {
        if (state == null) return;
        MatchSnapshot snapshot = state.getSnapshot();
        if (snapshot == null) return;
        for (MatchSnapshot.ProjectileDto projectile : snapshot.projectiles) {
            float x = getCellX(0) + (float) projectile.x * getBoardTileWidth()
                    + getBoardTileWidth() * 0.45f;
            float y = getCellY((int) Math.round(projectile.y)) + getBoardTileHeight() * 0.45f;
            boolean drawn = projectile.type != null
                    && drawPam(projectile.type, "idle", brainAnimTime, x, y, 0.5f, false);
            if (!drawn) {
                drawFallback(x, y, 14f, 14f, new Color(0.55f, 0.85f, 0.3f, 0.95f));
            }
        }
    }

    private void drawPlacementZone(float bh) {
        if (selectedKey == null) return;
        int from = state.isPlantSide() ? IZombieMatch.BRAIN_COLUMN + 1
                : IZombieMatch.REDLINE_COLUMN + 1;
        int to = state.isPlantSide() ? IZombieMatch.REDLINE_COLUMN + 1 : session.getCols();
        for (int col = from; col < to; col++) {
            drawFallback(getCellX(col), getBoardBottom(), getBoardTileWidth(), bh, PLACEABLE_TINT);
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
            float bite = Math.max(0f, Math.min(1f, hp / 300f));
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
        panel.pad(8f);
        Label youAre = new Label(state == null ? "?" : "YOU: " + state.getRole().name(),
                skin, "title");
        youAre.setFontScale(0.8f);
        opponentLabel = new Label("", skin, "main");
        opponentLabel.setFontScale(0.75f);
        panel.add(youAre).left().row();
        panel.add(opponentLabel).left();
        panel.pack();
        panel.setPosition(stage.getWidth() - panel.getWidth() - 16f,
                stage.getHeight() - panel.getHeight() - 90f);
        stage.addActor(panel);
    }

    private void buildTray() {
        if (tray != null) {
            tray.remove();
            tray = null;
        }
        cardViews.clear();
        if (state == null) return;

        Table built = new Table();
        built.setBackground(skin.getDrawable("card-background"));
        built.pad(5f);
        built.top();

        trayTitle = new Label(state.isPlantSide() ? "PLANTS" : "ZOMBIES", skin, "main");
        trayTitle.setFontScale(0.75f);
        trayTitle.setAlignment(Align.center);
        trayTitle.setWrap(true);

        MatchSnapshot snapshot = state.getSnapshot();
        if (snapshot != null) {
            if (state.isPlantSide()) {
                for (MatchSnapshot.SeedDto seed : snapshot.seeds) {
                    CardView view = buildSeedCard(seed);
                    cardViews.add(view);
                    built.add(view.stack).size(CARD_W, CARD_H).padBottom(3f).row();
                }
            } else {
                for (MatchSnapshot.PacketDto packet : snapshot.packets) {
                    CardView view = buildPacketCard(packet);
                    cardViews.add(view);
                    built.add(view.stack).size(CARD_W, CARD_H).padBottom(3f).row();
                }
            }
        }

        built.add(trayTitle).width(CARD_W).padTop(2f).row();
        built.pack();
        built.setPosition(TRAY_LEFT, TRAY_BOTTOM);
        tray = built;
        stage.addActor(tray);
    }

    private CardView buildPacketCard(MatchSnapshot.PacketDto packet) {
        Stack stack = new Stack();
        stack.setTouchable(Touchable.enabled);
        ZombieIconCard card = null;
        try {
            card = zombieCards.buildCardForAlias(packet.alias);
        } catch (Throwable ignored) {
            card = null;
        }
        if (card != null) {
            card.setSize(CARD_W, CARD_H);
            card.setTouchable(Touchable.disabled);
            stack.add(card);
        } else {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            fallback.add(new Label(packet.label, skin, "main")).center();
            stack.add(fallback);
        }
        return finishCard(stack, packet.alias, String.valueOf(packet.cost));
    }

    private CardView buildSeedCard(MatchSnapshot.SeedDto seed) {
        Stack stack = new Stack();
        stack.setTouchable(Touchable.enabled);
        SeedPacketCard card = null;
        try {
            card = seedCards.buildCardByPlantName(seed.name);
        } catch (Throwable ignored) {
            card = null;
        }
        if (card != null) {
            card.setSize(CARD_W, CARD_H);
            card.setTouchable(Touchable.disabled);
            stack.add(card);
        } else {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            fallback.add(new Label(seed.name, skin, "main")).center();
            stack.add(fallback);
        }
        return finishCard(stack, seed.name, String.valueOf(seed.cost));
    }

    private CardView finishCard(Stack stack, String key, String cost) {
        Image unavailable = new Image(new TextureRegionDrawable(whitePixelRegion()));
        unavailable.setColor(0f, 0f, 0f, 0.6f);
        unavailable.setFillParent(true);
        unavailable.setTouchable(Touchable.disabled);
        stack.add(unavailable);

        Image selected = new Image(new TextureRegionDrawable(whitePixelRegion()));
        selected.setColor(0.25f, 1f, 0.25f, 0.30f);
        selected.setFillParent(true);
        selected.setTouchable(Touchable.disabled);
        stack.add(selected);

        Label costLabel = new Label(cost, skin, "main");
        costLabel.setFontScale(0.85f);
        Table costTable = new Table();
        costTable.bottom().right();
        costTable.add(costLabel).padRight(4f).padBottom(2f);
        costTable.setTouchable(Touchable.disabled);
        stack.add(costTable);

        Label cooldownLabel = new Label("", skin, "title");
        cooldownLabel.setFontScale(0.85f);
        cooldownLabel.setAlignment(Align.center);
        Table cooldownTable = new Table();
        cooldownTable.setFillParent(true);
        cooldownTable.setTouchable(Touchable.disabled);
        cooldownTable.add(cooldownLabel).center().expand();
        stack.add(cooldownTable);

        stack.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedKey = key.equalsIgnoreCase(selectedKey) ? null : key;
                AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.5f);
            }
        });

        return new CardView(key, stack, unavailable, selected, cooldownLabel);
    }

    private void updateTray(MatchSnapshot snapshot) {
        if (tray == null) return;
        if (cardViews.isEmpty()
                && (!snapshot.seeds.isEmpty() || !snapshot.packets.isEmpty())) {
            buildTray();
            return;
        }
        int sun = state.getMySun();
        for (CardView view : cardViews) {
            double cooldown = 0;
            int cost = 0;
            if (state.isPlantSide()) {
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
            if (!ready) {
                view.cooldownLabel.setText(String.format("%.1f", cooldown));
                view.cooldownLabel.setVisible(true);
            } else {
                view.cooldownLabel.setVisible(false);
            }
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
            bar.add(reactionButton(Protocol.REACTION_TEXTS[i], 150f,
                            () -> NetworkClient.get().sendReaction(Protocol.REACTION_TEXT, index)))
                    .padRight(4f);
        }
        bar.row();

        Table quick = new Table();
        for (int i = 0; i < 3; i++) {
            int index = i;
            quick.add(reactionButton(ReactionOverlay.EMOJI_LABELS[i], 46f,
                            () -> NetworkClient.get().sendReaction(Protocol.REACTION_EMOJI, index)))
                    .padRight(4f);
        }
        for (int i = 0; i < 3; i++) {
            int index = i;
            quick.add(reactionButton(ReactionOverlay.STICKER_LABELS[i], 46f,
                            () -> NetworkClient.get().sendReaction(Protocol.REACTION_STICKER, index)))
                    .padRight(4f);
        }
        bar.add(quick).colspan(3).padTop(4f);

        bar.pack();
        bar.setPosition(stage.getWidth() - bar.getWidth() - 14f, 14f);
        reactionBar = bar;
        stage.addActor(reactionBar);
    }

    private Table reactionButton(String label, float width, Runnable action) {
        Table button = new Table();
        button.setBackground(skin.getDrawable("button-up"));
        Label text = new Label(label, skin, "main");
        text.setFontScale(0.62f);
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
        wrapper.add(button).size(width, 30f);
        return wrapper;
    }

    @Override
    public void onMatchEndSequenceFinished(boolean won) {
        // MATCH_END from the server is the only thing that ends a networked match.
    }
}