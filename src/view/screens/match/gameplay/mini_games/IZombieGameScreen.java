package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
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

import controller.ScreenManager;
import controller.match.mini_games.ImZombieController;
import controller.match.mini_games.MiniGameEndMenu;
import model.App;
import model.collections.animations.ZombieAnimationRegistry;
import model.match.mini_games.izombie.Brain;
import model.match.mini_games.izombie.IZombie;
import model.match.mini_games.izombie.ZombiePacket;
import service.GameClock;
import service.card_factory.ZombieIconCard;
import service.card_factory.ZombieIconCardFactory;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;
import view.screens.generals.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * IZombieGameScreen mini-game gameplay entry point. The actual gameplay machinery
 * lives in GameScreen, exactly like the regular chapter stages; this class only
 * points it at the mini-game's own art folder (background and left/right border
 * textures all come from the same folder - see GameScreen.getSeasonGameplayFolder())
 * and wires up I-Zombie-specific interaction: clicking a lane right of the red line
 * places the currently selected roster zombie there, same as
 * "place zombie -t <alias> -l (x,y)" in the terminal engine.
 * The defending plants are real Plant entities the same as any other level, so
 * GameScreen already draws them; what this screen adds on top is the brain in each
 * lane, the red line, the placement highlight, and the zombie-packet tray, since
 * MatchHud's plant tray is loadout-based and has no concept of I Zombie's
 * purchasable zombie roster.
 */
public class IZombieGameScreen extends GameScreen {

    private static final float BRAIN_WIDTH_FACTOR = 0.72f;
    private static final float BRAIN_EATEN_BURST_DURATION = 0.9f;
    // Card + zombie icon shrunk down from the old 88x112, and built directly at this
    // size (see buildPacketCard) rather than resized afterward - the previous size
    // made the zombie icon inside each card look oversized next to the tray/HUD.
    private static final float PACKET_CARD_W = 72f;
    private static final float PACKET_CARD_H = 92f;
    private static final float PACKET_TRAY_LEFT = 10f;
    private static final float PACKET_TRAY_BOTTOM = 26f;
    private static final Color RED_LINE_COLOR = new Color(0.88f, 0.16f, 0.14f, 0.8f);
    private static final Color PLACEABLE_TINT = new Color(0.45f, 0.95f, 0.45f, 0.12f);
    private static final Color HOVER_VALID_TINT = new Color(0.55f, 1f, 0.55f, 0.30f);
    private static final Color HOVER_INVALID_TINT = new Color(1f, 0.35f, 0.30f, 0.25f);

    {
        // Mini-games have no Level/Season, so this doubles as the lawn mower art
        // key - see GameScreen.getLawnMowerSeasonKey() and SEASON_LAWN_MOWER_PAM_PATHS.
        seasonFolder = "izombie";
    }

    private static final class PacketView {
        final ZombiePacket packet;
        final Group stack;
        final Image unavailable;
        final Image selected;
        final Label costLabel;
        final Label cooldownLabel;

        PacketView(ZombiePacket packet, Group stack, Image unavailable, Image selected,
                   Label costLabel, Label cooldownLabel) {
            this.packet = packet;
            this.stack = stack;
            this.unavailable = unavailable;
            this.selected = selected;
            this.costLabel = costLabel;
            this.cooldownLabel = cooldownLabel;
        }
    }

    private final ZombieIconCardFactory cardFactory = new ZombieIconCardFactory();
    private final List<PacketView> packetViews = new ArrayList<>();
    private final boolean[] brainWasEaten = new boolean[16];
    private final float[] brainBurstTimers = new float[16];

    private Table packetTray;
    private Label trayTitle;
    private Texture brainTexture;
    private IZombie activeGame;
    private String selectedAlias;
    private float brainAnimTime;
    private float ghostAnimTime;

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
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        selectedAlias = null;
        activeGame = currentGame();
        brainTexture = loadBrainTexture();
        if (hud != null) {
            hud.setLoadoutBankVisible(false);
            hud.setShovelVisible(false);
            hud.setCheatButtonsVisible(false, false, false);
            hud.setStartButtonAvailable(false);
            hud.setObjectiveOverride("EAT ALL THE BRAINZ");
        }
        buildPacketTray();
        syncBrainState();
    }

    @Override
    public void dispose() {
        cardFactory.dispose();
        if (brainTexture != null) brainTexture.dispose();
        super.dispose();
    }

    private Texture loadBrainTexture() {
        String path = getSeasonGameplayFolder() + "brain.png";
        if (!Gdx.files.internal(path).exists() && path.startsWith("assets/")) {
            path = path.substring("assets/".length());
        }
        return loadTextureSafe(path);
    }

    private IZombie currentGame() {
        return App.currentMenu instanceof ImZombieController controller
                ? controller.getGame() : null;
    }

    private IZombie game() {
        IZombie live = currentGame();
        if (live != null && live != activeGame) {
            activeGame = live;
            selectedAlias = null;
            buildPacketTray();
            syncBrainState();
        }
        return activeGame;
    }

    @Override
    protected void tickSession() {
        if (App.currentMenu instanceof ImZombieController controller) {
            controller.tick(GameClock.SECONDS_PER_TICK);
        }
    }

    @Override
    protected void checkMatchEnd() {
        if (matchFinished || !isMatchEndSequenceIdle()) return;
        if (App.currentMenu instanceof ImZombieController) return;

        if (App.currentMenu instanceof MiniGameEndMenu end) {
            if (packetTray != null) packetTray.setVisible(false);
            startMatchEndSequence(end.isWon());
            return;
        }
        matchFinished = true;
        ScreenManager.syncWithCurrentMenu();
    }

    @Override
    public void onMatchEndSequenceFinished(boolean won) {
        // The controller already swapped App.currentMenu to the mini-game end menu the
        // moment the outcome was decided; MatchEndSequence syncs the screen right after
        // this call, so there is no "end game" command to run here.
    }

    @Override
    protected void refreshHud(float delta) {
        super.refreshHud(delta);
        IZombie game = game();
        if (hud == null || game == null) return;
        int eaten = game.getBrainsEaten();
        int total = game.getBrainCount();
        hud.setProgressOverride("BRAINZ EATEN " + eaten + "/" + total,
                total == 0 ? 0f : eaten / (float) total);
        updatePacketTray(delta);
    }

    @Override
    protected void onCellClicked(int row, int col) {
        IZombie game = game();
        if (game == null) {
            super.onCellClicked(row, col);
            return;
        }
        if (col <= game.getRedLineColumn()) {
            Toast.show(stage, "Zombies drop in to the right of the red line, past the plants.");
            return;
        }
        if (selectedAlias == null) {
            Toast.show(stage, "Pick a zombie from the tray on the left first.");
            return;
        }
        ZombiePacket packet = game.findPacket(selectedAlias);
        if (packet == null) {
            selectedAlias = null;
            return;
        }
        if (!packet.isReady()) {
            Toast.show(stage, packet.getDisplayName() + " is still recharging.");
            return;
        }
        if (packet.getCost() > game.getSession().getSunCount()) {
            Toast.show(stage, "Not enough sun for " + packet.getDisplayName() + ".");
            return;
        }
        int sunBefore = game.getSession().getSunCount();
        runCommand("place zombie -t " + packet.getAlias()
                + " -l (" + (col + 1) + ", " + (row + 1) + ")");
        if (game.getSession().getSunCount() != sunBefore) {
            AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.6f);
            selectedAlias = null;
        }
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        IZombie game = game();
        if (game == null) return;

        brainAnimTime += delta;
        drawPlacementZone(game, bh);
        drawRedLine(game, bh);
        drawBrains(game, delta);
    }

    @Override
    protected void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        super.drawSeasonForegroundEffects(delta, bw, bh);
        IZombie game = game();
        if (game == null || selectedAlias == null) return;

        ghostAnimTime += delta;
        Vector2 mouse = mouseWorld();
        int col = (int) ((mouse.x - boardLeft()) / getBoardTileWidth());
        int row = session.getRows() - 1
                - (int) ((mouse.y - getBoardBottom()) / getBoardTileHeight());
        boolean onBoard = row >= 0 && row < session.getRows()
                && col >= 0 && col < session.getCols();
        if (onBoard) {
            boolean placeable = col > game.getRedLineColumn();
            drawFallback(getCellX(col), getCellY(row), getBoardTileWidth(), getBoardTileHeight(),
                    placeable ? HOVER_VALID_TINT : HOVER_INVALID_TINT);
        }

        String path = ZombieAnimationRegistry.pathFor(selectedAlias, seasonFolder);
        batch.setColor(1f, 1f, 1f, 0.7f);
        boolean drawn = drawPam(path, "idle", ghostAnimTime,
                mouse.x - 10f, mouse.y - getBoardTileHeight() * 0.35f, 0.52f, false);
        batch.setColor(Color.WHITE);
        if (!drawn) {
            drawFallback(mouse.x - 18f, mouse.y - 18f, 36f, 36f,
                    new Color(0.55f, 0.5f, 0.45f, 0.7f));
        }
    }

    private void drawPlacementZone(IZombie game, float bh) {
        if (selectedAlias == null) return;
        for (int col = game.getRedLineColumn() + 1; col < session.getCols(); col++) {
            drawFallback(getCellX(col), getBoardBottom(), getBoardTileWidth(), bh, PLACEABLE_TINT);
        }
    }

    private void drawRedLine(IZombie game, float bh) {
        float redLineX = getCellX(game.getRedLineColumn() + 1);
        drawFallback(redLineX - 2f, getBoardBottom(), 4f, bh, RED_LINE_COLOR);
    }

    private void drawBrains(IZombie game, float delta) {
        float tileW = getBoardTileWidth();
        float tileH = getBoardTileHeight();
        float width = tileW * BRAIN_WIDTH_FACTOR;
        float height = brainTexture == null || brainTexture.getWidth() == 0
                ? width * 0.74f : width * brainTexture.getHeight() / brainTexture.getWidth();

        for (int row = 0; row < game.getBrainCount(); row++) {
            Brain brain = game.getBrain(row);
            if (brain == null) continue;

            float centerX = getCellX(game.getBrainColumn()) + tileW * 0.5f;
            float centerY = getCellY(row) + tileH * 0.42f;

            if (!brain.isEaten()) {
                float bite = (float) brain.getRemainingRatio();
                float scale = 0.72f + 0.28f * bite;
                float bob = (float) Math.sin(brainAnimTime * 2.2f + row) * tileH * 0.02f;
                float shake = bite < 1f ? (float) Math.sin(brainAnimTime * 26f + row) * 2.5f : 0f;
                batch.setColor(1f, 0.6f + 0.4f * bite, 0.6f + 0.4f * bite, 1f);
                drawBrainQuad(centerX + shake, centerY + bob, width * scale, height * scale);
                batch.setColor(Color.WHITE);
            }

            if (row < brainBurstTimers.length && brainBurstTimers[row] > 0f) {
                brainBurstTimers[row] = Math.max(0f, brainBurstTimers[row] - delta);
                float progress = 1f - brainBurstTimers[row] / BRAIN_EATEN_BURST_DURATION;
                float scale = 1f + progress * 1.4f;
                batch.setColor(1f, 1f, 1f, Math.max(0f, 1f - progress));
                drawBrainQuad(centerX, centerY + progress * tileH * 0.4f,
                        width * scale, height * scale);
                batch.setColor(Color.WHITE);
            }
        }
        syncBrainState();
    }

    private void drawBrainQuad(float centerX, float centerY, float width, float height) {
        if (brainTexture != null && brainTexture.getWidth() > 1) {
            batch.draw(brainTexture, centerX - width * 0.5f, centerY - height * 0.5f,
                    width, height);
            return;
        }
        drawFallback(centerX - width * 0.5f, centerY - height * 0.5f, width, height,
                new Color(0.94f, 0.45f, 0.55f, batch.getColor().a));
    }

    private void syncBrainState() {
        IZombie game = activeGame;
        if (game == null) return;
        for (int row = 0; row < game.getBrainCount() && row < brainWasEaten.length; row++) {
            Brain brain = game.getBrain(row);
            boolean eaten = brain != null && brain.isEaten();
            if (eaten && !brainWasEaten[row]) {
                brainBurstTimers[row] = BRAIN_EATEN_BURST_DURATION;
            }
            brainWasEaten[row] = eaten;
        }
    }

    private float boardLeft() {
        return getCellX(0);
    }

    private Vector2 mouseWorld() {
        Vector3 coordinates = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        stage.getViewport().unproject(coordinates);
        return new Vector2(coordinates.x, coordinates.y);
    }

    private void buildPacketTray() {
        if (packetTray != null) {
            packetTray.remove();
            packetTray = null;
        }
        packetViews.clear();
        trayTitle = null;
        IZombie game = activeGame;
        if (game == null) return;

        Table tray = new Table();
        tray.setBackground(skin.getDrawable("card-background"));
        tray.pad(5f);
        tray.top();

        for (ZombiePacket packet : game.getRoster()) {
            PacketView view = buildPacketCard(packet);
            packetViews.add(view);
            tray.add(view.stack).size(PACKET_CARD_W, PACKET_CARD_H).padBottom(3f).row();
        }

        trayTitle = new Label("ZOMBIES", skin, "main");
        trayTitle.setFontScale(0.75f);
        trayTitle.setAlignment(Align.center);
        trayTitle.setWrap(true);
        tray.add(trayTitle).width(PACKET_CARD_W).padTop(2f).row();
        tray.pack();
        tray.setPosition(PACKET_TRAY_LEFT, PACKET_TRAY_BOTTOM);

        packetTray = tray;
        stage.addActor(packetTray);
    }

    private PacketView buildPacketCard(ZombiePacket packet) {
        Group stack = new Group();
        stack.setTouchable(Touchable.enabled);
        stack.setSize(PACKET_CARD_W, PACKET_CARD_H);

        ZombieIconCard card = null;
        try {
            // Built directly at the smaller tray size (frame + icon together), rather
            // than building at the default size and resizing afterward - ZombieIconCard
            // fixes its icon inset at construction time, so a later setSize() would
            // leave the icon at its original pixel size and get clipped.
            card = cardFactory.buildCardForAlias(packet.getAlias(), PACKET_CARD_W, PACKET_CARD_H);
        } catch (Throwable ignored) {
            card = null;
        }
        if (card != null) {
            card.setTouchable(Touchable.disabled);
            card.setBounds(0f, 0f, PACKET_CARD_W, PACKET_CARD_H);
            stack.addActor(card);
        } else {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            fallback.add(new Label(packet.getDisplayName(), skin, "main")).center();
            fallback.setBounds(0f, 0f, PACKET_CARD_W, PACKET_CARD_H);
            stack.addActor(fallback);
        }

        Image unavailable = new Image(new TextureRegionDrawable(whitePixelRegion()));
        unavailable.setColor(0f, 0f, 0f, 0.6f);
        unavailable.setFillParent(true);
        unavailable.setTouchable(Touchable.disabled);
        unavailable.setBounds(0f, 0f, PACKET_CARD_W, PACKET_CARD_H);
        stack.addActor(unavailable);

        Image selected = new Image(new TextureRegionDrawable(whitePixelRegion()));
        selected.setColor(0.25f, 1f, 0.25f, 0.30f);
        selected.setFillParent(true);
        selected.setTouchable(Touchable.disabled);
        selected.setBounds(0f, 0f, PACKET_CARD_W, PACKET_CARD_H);
        stack.addActor(selected);

        Label costLabel = new Label(String.valueOf(packet.getCost()), skin, "main");
        costLabel.setFontScale(0.85f);
        costLabel.setAlignment(Align.bottomRight);
        costLabel.setTouchable(Touchable.disabled);
        costLabel.setBounds(0f, 0f, PACKET_CARD_W - 2f, PACKET_CARD_H - 2f);
        stack.addActor(costLabel);

        Label cooldownLabel = new Label("", skin, "title");
        cooldownLabel.setFontScale(0.85f);
        cooldownLabel.setAlignment(Align.center);
        cooldownLabel.setTouchable(Touchable.disabled);
        cooldownLabel.setBounds(0f, 0f, PACKET_CARD_W, PACKET_CARD_H);
        stack.addActor(cooldownLabel);

        stack.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                IZombie game = game();
                if (game == null) return;
                if (!packet.isReady()) {
                    Toast.show(stage, packet.getDisplayName() + " is still recharging.");
                    return;
                }
                if (packet.getCost() > game.getSession().getSunCount()) {
                    Toast.show(stage, "Not enough sun for " + packet.getDisplayName()
                            + " (" + packet.getCost() + ").");
                    return;
                }
                selectedAlias = packet.getAlias().equalsIgnoreCase(selectedAlias)
                        ? null : packet.getAlias();
                AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.5f);
            }
        });

        return new PacketView(packet, stack, unavailable, selected, costLabel, cooldownLabel);
    }

    private void updatePacketTray(float delta) {
        IZombie game = activeGame;
        if (game == null || packetTray == null) return;

        int sun = game.getSession().getSunCount();
        for (PacketView view : packetViews) {
            boolean ready = view.packet.isReady();
            boolean affordable = sun >= view.packet.getCost();
            view.unavailable.setVisible(!ready || !affordable);
            view.selected.setVisible(view.packet.getAlias().equalsIgnoreCase(selectedAlias));
            view.costLabel.setColor(affordable ? Color.WHITE : Color.RED);
            if (!ready) {
                view.cooldownLabel.setText(String.format("%.1f", view.packet.getCooldown()));
                view.cooldownLabel.setVisible(true);
            } else {
                view.cooldownLabel.setVisible(false);
            }
        }
        if (selectedAlias != null) {
            ZombiePacket packet = game.findPacket(selectedAlias);
            if (packet == null || !packet.isReady() || packet.getCost() > sun) selectedAlias = null;
        }
        if (trayTitle != null) {
            ZombiePacket packet = selectedAlias == null ? null : game.findPacket(selectedAlias);
            trayTitle.setText(packet == null ? "ZOMBIES" : packet.getDisplayName().toUpperCase());
        }
    }
}