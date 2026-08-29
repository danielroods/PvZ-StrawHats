package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import controller.match.mini_games.CouchIZombieController;
import model.App;
import model.match.mini_games.izombie.Brain;
import model.match.mini_games.izombie.IZombieMatch;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.match.mini_games.izombie.IZombieMatch.SeedCard;
import model.match.mini_games.izombie.ZombiePacket;
import model.utils.GameSession;
import service.GameClock;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;
import view.screens.generals.Toast;

import java.util.ArrayList;
import java.util.List;

public class CouchIZombieGameScreen extends GameScreen {

    private static final float BRAIN_WIDTH_FACTOR = 0.72f;
    private static final float CARD_W = 84f;
    private static final float CARD_H = 106f;
    private static final Color RED_LINE_COLOR = new Color(0.88f, 0.16f, 0.14f, 0.8f);
    private static final Color PLANT_ZONE = new Color(0.45f, 0.95f, 0.45f, 0.10f);
    private static final Color ZOMBIE_CURSOR = new Color(0.95f, 0.45f, 0.25f, 0.35f);

    {
        seasonFolder = "izombie";
    }

    private final SeedPacketCardFactory seedCards = new SeedPacketCardFactory();
    private final List<Stack> seedViews = new ArrayList<>();
    private final List<Image> seedDim = new ArrayList<>();
    private final List<Image> seedSel = new ArrayList<>();

    private Texture brainTexture;
    private Table plantTray;
    private Label plantTrayTitle;
    private Table zombieTray;
    private Label zombieTrayTitle;
    private List<Label> packetLabels = new ArrayList<>();
    private String selectedSeed;
    private int packetIndex;
    private int cursorRow = 2;
    private int cursorCol = IZombieMatch.COLS - 1;
    private float brainAnimTime;
    private boolean endHandled;

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

    private CouchIZombieController controller() {
        return App.currentMenu instanceof CouchIZombieController couch ? couch : null;
    }

    private IZombieMatch match() {
        CouchIZombieController controller = controller();
        return controller == null ? null : controller.getMatch();
    }

    @Override
    public void show() {
        IZombieMatch match = match();
        if (match != null) GameSession.setCurrent(match.getSession());
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);

        brainTexture = loadBrainTexture();
        if (hud != null) {
            hud.setLoadoutBankVisible(false);
            hud.setShovelVisible(false);
            hud.setStartButtonAvailable(false);
            hud.setObjectiveOverride("P1 PLANTS  vs  P2 ZOMBIES");
        }
        buildPlantTray();
        buildZombieTray();
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
        seedCards.dispose();
        if (brainTexture != null) brainTexture.dispose();
        super.dispose();
    }

    @Override
    protected void tickSession() {
        CouchIZombieController controller = controller();
        if (controller != null) controller.tick(GameClock.SECONDS_PER_TICK);
    }

    @Override
    public void render(float delta) {
        IZombieMatch match = match();
        if (match != null) {
            GameSession.setCurrent(match.getSession());
            handleZombiePlayerKeys(match);
        }
        super.render(delta);
    }

    private void handleZombiePlayerKeys(IZombieMatch match) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            cursorRow = Math.max(0, cursorRow - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            cursorRow = Math.min(IZombieMatch.ROWS - 1, cursorRow + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            cursorCol = Math.max(IZombieMatch.REDLINE_COLUMN + 1, cursorCol - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            cursorCol = Math.min(IZombieMatch.COLS - 1, cursorCol + 1);
        }

        List<ZombiePacket> roster = match.getRoster();
        for (int i = 0; i < roster.size() && i < 6; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) packetIndex = i;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && packetIndex < roster.size()) {
            String rejection = controller().apply(Role.ZOMBIES, "PLACE_ZOMBIE",
                    roster.get(packetIndex).getAlias(), cursorRow, cursorCol);
            if (rejection != null) {
                Toast.show(stage, "P2: " + rejection);
            } else {
                AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.6f);
            }
        }
    }

    @Override
    protected void onCellClicked(int row, int col) {
        CouchIZombieController controller = controller();
        if (controller == null) return;
        if (selectedSeed == null) {
            Toast.show(stage, "P1: pick a seed packet first.");
            return;
        }
        String rejection = controller.apply(Role.PLANTS, "PLANT", selectedSeed, row, col);
        if (rejection != null) {
            Toast.show(stage, "P1: " + rejection);
            return;
        }
        selectedSeed = null;
        AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.6f);
    }

    @Override
    protected boolean collectUnderMouse(Vector2 click) {
        CouchIZombieController controller = controller();
        if (controller == null) return false;
        int col = (int) ((click.x - getCellX(0)) / getBoardTileWidth());
        int row = session.getRows() - 1
                - (int) ((click.y - getBoardBottom()) / getBoardTileHeight());
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) {
            return false;
        }
        return controller.apply(Role.PLANTS, "COLLECT_SUN", null, row, col) == null;
    }

    @Override
    protected void checkMatchEnd() {
        IZombieMatch match = match();
        if (endHandled || match == null || !match.isFinished()) return;
        endHandled = true;
        matchFinished = true;
    }

    @Override
    protected void refreshHud(float delta) {
        super.refreshHud(delta);
        IZombieMatch match = match();
        if (hud == null || match == null) return;
        double remaining = match.getRemainingSeconds();
        float progress = (float) Math.max(0, Math.min(1,
                1.0 - remaining / IZombieMatch.MATCH_SECONDS));
        hud.setProgressOverride(String.format("BRAINZ %d/5   %02d:%02d",
                match.getBrainsEaten(), (int) (remaining / 60), (int) (remaining % 60)),
                progress);
        updateTrays(match);
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        IZombieMatch match = match();
        if (match == null) return;
        brainAnimTime += delta;

        for (int col = IZombieMatch.BRAIN_COLUMN + 1;
                col <= IZombieMatch.REDLINE_COLUMN; col++) {
            drawFallback(getCellX(col), getBoardBottom(), getBoardTileWidth(), bh, PLANT_ZONE);
        }
        drawFallback(getCellX(IZombieMatch.REDLINE_COLUMN + 1) - 2f, getBoardBottom(),
                4f, bh, RED_LINE_COLOR);
        drawFallback(getCellX(cursorCol), getCellY(cursorRow), getBoardTileWidth(),
                getBoardTileHeight(), ZOMBIE_CURSOR);
        drawBrains(match);
    }

    private void drawBrains(IZombieMatch match) {
        float tileW = getBoardTileWidth();
        float tileH = getBoardTileHeight();
        float width = tileW * BRAIN_WIDTH_FACTOR;
        float height = brainTexture == null || brainTexture.getWidth() == 0
                ? width * 0.74f : width * brainTexture.getHeight() / brainTexture.getWidth();

        Brain[] brains = match.getBrains();
        for (int row = 0; row < brains.length; row++) {
            Brain brain = brains[row];
            if (brain == null || brain.isEaten()) continue;
            float bite = (float) brain.getRemainingRatio();
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

    private void buildPlantTray() {
        IZombieMatch match = match();
        if (match == null) return;
        Table tray = new Table();
        tray.setBackground(skin.getDrawable("card-background"));
        tray.pad(5f);
        tray.top();

        for (SeedCard card : match.getSeeds()) {
            Stack stack = new Stack();
            stack.setTouchable(Touchable.enabled);
            SeedPacketCard art = null;
            try {
                art = seedCards.buildCardByPlantName(card.name());
            } catch (Throwable ignored) {
                art = null;
            }
            if (art != null) {
                art.setSize(CARD_W, CARD_H);
                art.setTouchable(Touchable.disabled);
                stack.add(art);
            } else {
                Table fallback = new Table();
                fallback.setBackground(skin.getDrawable("card-background"));
                fallback.add(new Label(card.name(), skin, "main")).center();
                stack.add(fallback);
            }

            Image dim = new Image(new TextureRegionDrawable(whitePixelRegion()));
            dim.setColor(0f, 0f, 0f, 0.6f);
            dim.setFillParent(true);
            dim.setTouchable(Touchable.disabled);
            stack.add(dim);

            Image sel = new Image(new TextureRegionDrawable(whitePixelRegion()));
            sel.setColor(0.25f, 1f, 0.25f, 0.30f);
            sel.setFillParent(true);
            sel.setTouchable(Touchable.disabled);
            stack.add(sel);

            Label cost = new Label(String.valueOf(card.cost()), skin, "main");
            cost.setFontScale(0.8f);
            Table costTable = new Table();
            costTable.bottom().right();
            costTable.add(cost).padRight(4f).padBottom(2f);
            costTable.setTouchable(Touchable.disabled);
            stack.add(costTable);

            stack.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    selectedSeed = card.name().equalsIgnoreCase(selectedSeed) ? null : card.name();
                    AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.5f);
                }
            });

            seedViews.add(stack);
            seedDim.add(dim);
            seedSel.add(sel);
            tray.add(stack).size(CARD_W, CARD_H).padBottom(3f).row();
        }

        plantTrayTitle = new Label("P1 MOUSE", skin, "main");
        plantTrayTitle.setFontScale(0.7f);
        plantTrayTitle.setAlignment(Align.center);
        plantTrayTitle.setWrap(true);
        tray.add(plantTrayTitle).width(CARD_W).padTop(2f).row();
        tray.pack();
        tray.setPosition(8f, 26f);
        plantTray = tray;
        stage.addActor(plantTray);
    }

    private void buildZombieTray() {
        IZombieMatch match = match();
        if (match == null) return;
        Table tray = new Table();
        tray.setBackground(skin.getDrawable("card-background"));
        tray.pad(6f);
        tray.top();

        zombieTrayTitle = new Label("P2 KEYBOARD", skin, "main");
        zombieTrayTitle.setFontScale(0.7f);
        tray.add(zombieTrayTitle).left().padBottom(4f).row();

        packetLabels = new ArrayList<>();
        int index = 1;
        for (ZombiePacket packet : match.getRoster()) {
            Label label = new Label(index + "  " + packet.getDisplayName()
                    + "  " + packet.getCost(), skin, "main");
            label.setFontScale(0.66f);
            packetLabels.add(label);
            tray.add(label).left().row();
            index++;
        }

        Label help = new Label("W/S lane   A/D column   SPACE drop", skin, "muted");
        help.setFontScale(0.6f);
        tray.add(help).left().padTop(6f).row();

        tray.pack();
        tray.setPosition(stage.getWidth() - tray.getWidth() - 10f, 26f);
        zombieTray = tray;
        stage.addActor(zombieTray);
    }

    private void updateTrays(IZombieMatch match) {
        List<SeedCard> seeds = match.getSeeds();
        for (int i = 0; i < seedViews.size() && i < seeds.size(); i++) {
            SeedCard card = seeds.get(i);
            boolean ready = match.getSession().isPlantReady(card.plantId());
            boolean affordable = match.getPlantSun() >= card.cost();
            seedDim.get(i).setVisible(!ready || !affordable);
            seedSel.get(i).setVisible(card.name().equalsIgnoreCase(selectedSeed));
        }

        List<ZombiePacket> roster = match.getRoster();
        for (int i = 0; i < packetLabels.size() && i < roster.size(); i++) {
            ZombiePacket packet = roster.get(i);
            boolean ready = packet.isReady() && match.getZombieSun() >= packet.getCost();
            Label label = packetLabels.get(i);
            label.setText((i + 1) + (i == packetIndex ? " > " : "  ")
                    + packet.getDisplayName() + "  " + packet.getCost()
                    + (packet.isReady() ? "" : String.format(" (%.1fs)", packet.getCooldown())));
            label.setColor(ready ? Color.WHITE : Color.GRAY);
        }
        if (zombieTrayTitle != null) {
            zombieTrayTitle.setText("P2 KEYBOARD   sun " + match.getZombieSun());
        }
        if (plantTrayTitle != null) {
            plantTrayTitle.setText("P1 SUN " + match.getPlantSun());
        }
    }

    @Override
    public void onMatchEndSequenceFinished(boolean won) {
        // The controller decides the winner as soon as the shared match reports it.
    }
}
