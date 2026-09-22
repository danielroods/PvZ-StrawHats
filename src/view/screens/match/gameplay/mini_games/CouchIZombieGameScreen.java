package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
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

import controller.match.mini_games.CouchIZombieController;
import controller.match.mini_games.MiniGameEndMenu;
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
import service.card_factory.ZombieIconCard;
import service.card_factory.ZombieIconCardFactory;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.MiniGameLawnArt;
import view.screens.generals.GameScreen;
import view.screens.generals.Toast;

import java.util.ArrayList;
import java.util.List;

public class CouchIZombieGameScreen extends GameScreen {

    private static final float BRAIN_WIDTH_FACTOR = 0.72f;
    
    private static final float PLANT_CARD_W = 95f;
    private static final float PLANT_CARD_H = 60f;
    private static final float ZOMBIE_CARD_W = 72f;
    private static final float ZOMBIE_CARD_H = 92f;
    private static final Color RED_LINE_COLOR = new Color(0.88f, 0.16f, 0.14f, 0.8f);
    private static final Color PLANT_ZONE = new Color(0.45f, 0.95f, 0.45f, 0.10f);
    private static final Color ZOMBIE_CURSOR = new Color(0.95f, 0.45f, 0.25f, 0.35f);
    
    private static final float ZOMBIE_TRAY_AREA_WIDTH = 150f;
    
    private static final String TEXTURE_RIGHT = "assets/images/ui/texture_right.png";

    {
        seasonFolder = "izombie";
    }

    private final SeedPacketCardFactory seedCards = new SeedPacketCardFactory();
    private final ZombieIconCardFactory zombieCards = new ZombieIconCardFactory();
    private final List<Group> seedViews = new ArrayList<>();
    private final List<Image> seedDim = new ArrayList<>();
    private final List<Image> seedSel = new ArrayList<>();
    private final List<Label> seedCooldownLabels = new ArrayList<>();

    private Texture brainTexture;
    private Texture textureRight;
    private Table plantTray;
    private Label plantTrayTitle;
    private Table zombieTray;
    private Label zombieTrayTitle;
    private Image zombieSunIcon;
    private Label zombieSunAmountLabel;
    private final List<Group> zombieViews = new ArrayList<>();
    private final List<Image> zombieDim = new ArrayList<>();
    private final List<Image> zombieSel = new ArrayList<>();
    private final List<Label> zombieCostLabels = new ArrayList<>();
    private final List<Label> zombieCooldownLabels = new ArrayList<>();
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
        return MiniGameLawnArt.backgroundPath(getSeasonGameplayFolder());
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
        
        return ZOMBIE_TRAY_AREA_WIDTH;
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
        AudioManager.get().playMusic(AudioEnum.MINI_GAME_MUSIC, true);

        brainTexture = loadBrainTexture();
        textureRight = loadTextureRight();
        if (hud != null) {
            hud.setLoadoutBankVisible(false);
            hud.setShovelVisible(false);
            hud.setCheatButtonsVisible(false, false, false);
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

    private Texture loadTextureRight() {
        if (!Gdx.files.internal(TEXTURE_RIGHT).exists()) {
            return null;
        }
        try {
            Texture texture = new Texture(Gdx.files.internal(TEXTURE_RIGHT));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return texture;
        } catch (Throwable t) {
            Gdx.app.error("CouchIZombieGameScreen", "Failed to load " + TEXTURE_RIGHT, t);
            return null;
        }
    }

    @Override
    public void dispose() {
        seedCards.dispose();
        zombieCards.dispose();
        if (brainTexture != null) brainTexture.dispose();
        if (textureRight != null) textureRight.dispose();
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
        CouchIZombieController activeController = controller();
        if (activeController == null) {
            
            
            
            
            
            Gdx.app.error("CouchIZombieGameScreen", "handleZombiePlayerKeys: controller() is null, "
                    + "App.currentMenu is " + (App.currentMenu == null ? "null" : App.currentMenu.getClass()));
            return;
        }
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            
            
            
            
            Gdx.app.log("CouchIZombieGameScreen", "SPACE pressed: packetIndex=" + packetIndex
                    + " rosterSize=" + roster.size() + " cursor=(" + cursorRow + "," + cursorCol + ")");
            if (packetIndex < roster.size()) {
                String rejection = activeController.apply(Role.ZOMBIES, "PLACE_ZOMBIE",
                        roster.get(packetIndex).getAlias(), cursorRow, cursorCol);
                Gdx.app.log("CouchIZombieGameScreen", "apply() returned: " + rejection);
                if (rejection != null) {
                    Toast.show(stage, "P2: " + rejection);
                } else {
                    AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.6f);
                }
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
        if (matchFinished || !isMatchEndSequenceIdle() || endHandled) return;

        if (App.currentMenu instanceof MiniGameEndMenu end) {
            endHandled = true;
            startMatchEndSequence(end.isWon());
            return;
        }

        IZombieMatch match = match();
        if (match == null || !match.isFinished()) return;
    }

    @Override
    protected void refreshHud(float delta) {
        IZombieMatch match = match();
        if (hud != null && match != null) {
            double remaining = match.getRemainingSeconds();
            int eaten = match.getBrainsEaten();
            int brains = Math.max(1, match.getBrainCount());
            hud.setProgressOverride(String.format("BRAINZ %d/%d   %02d:%02d", eaten, brains,
                    (int) (remaining / 60), (int) (remaining % 60)), eaten / (float) brains);
            
            
            
            hud.setSunOverride(match.getPlantSun());
        }
        super.refreshHud(delta);
        if (hud == null || match == null) return;
        updateTrays(match);
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        drawRightTexture();
        IZombieMatch match = match();
        if (match == null) return;
        brainAnimTime += delta;

        for (int col = IZombieMatch.BRAIN_COLUMN + 1;
             col <= IZombieMatch.REDLINE_COLUMN; col++) {
            drawFallback(getCellX(col), getBoardBottom(), getBoardTileWidth(), bh, PLANT_ZONE);
        }
        drawDeadlineFlowerLine(IZombieMatch.REDLINE_COLUMN + 1, session.getRows());
        drawFallback(getCellX(cursorCol), getCellY(cursorRow), getBoardTileWidth(),
                getBoardTileHeight(), ZOMBIE_CURSOR);
        drawBrains(match);
    }

    
    private void drawRightTexture() {
        float viewW = stage.getViewport().getWorldWidth();
        float viewH = stage.getViewport().getWorldHeight();
        float x = viewW - ZOMBIE_TRAY_AREA_WIDTH;

        batch.setColor(Color.WHITE);
        if (textureRight != null) {
            batch.draw(textureRight, x, 0f, ZOMBIE_TRAY_AREA_WIDTH, viewH);
        } else {
            batch.setColor(0.08f, 0.07f, 0.05f, 0.94f);
            batch.draw(whitePixel, x, 0f, ZOMBIE_TRAY_AREA_WIDTH, viewH);
            batch.setColor(Color.WHITE);
        }
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
        tray.pad(2f).top();

        seedViews.clear();
        seedDim.clear();
        seedSel.clear();
        seedCooldownLabels.clear();

        for (SeedCard card : match.getSeeds()) {
            Group stack = new Group();
            stack.setTouchable(Touchable.enabled);
            stack.setSize(PLANT_CARD_W, PLANT_CARD_H);
            SeedPacketCard art = null;
            try {
                art = seedCards.buildCardByPlantName(card.name());
            } catch (Throwable ignored) {
                art = null;
            }
            if (art != null) {
                art.setSize(PLANT_CARD_W, PLANT_CARD_H);
                art.setTouchable(Touchable.disabled);
                art.setBounds(0f, 0f, PLANT_CARD_W, PLANT_CARD_H);
                stack.addActor(art);
            } else {
                Table fallback = new Table();
                fallback.setBackground(skin.getDrawable("card-background"));
                Label label = new Label(card.name(), skin, "main");
                label.setAlignment(Align.center);
                label.setFontScale(0.7f);
                label.setWrap(true);
                fallback.add(label).width(PLANT_CARD_W).center();
                fallback.setBounds(0f, 0f, PLANT_CARD_W, PLANT_CARD_H);
                stack.addActor(fallback);
            }

            Image dim = new Image(new TextureRegionDrawable(whitePixelRegion()));
            dim.setColor(0f, 0f, 0f, 0.6f);
            dim.setFillParent(true);
            dim.setTouchable(Touchable.disabled);
            dim.setBounds(0f, 0f, PLANT_CARD_W, PLANT_CARD_H);
            stack.addActor(dim);

            Image sel = new Image(new TextureRegionDrawable(whitePixelRegion()));
            sel.setColor(0.25f, 1f, 0.25f, 0.30f);
            sel.setFillParent(true);
            sel.setTouchable(Touchable.disabled);
            sel.setBounds(0f, 0f, PLANT_CARD_W, PLANT_CARD_H);
            stack.addActor(sel);

            Label cost = new Label(String.valueOf(card.cost()), skin, "main");
            cost.setFontScale(0.72f);
            cost.setAlignment(Align.bottomRight);
            cost.setTouchable(Touchable.disabled);
            cost.setBounds(0f, 0f, PLANT_CARD_W - 2f, PLANT_CARD_H - 2f);
            stack.addActor(cost);

            Label cooldown = new Label("", skin, "title");
            cooldown.setFontScale(0.78f);
            cooldown.setAlignment(Align.center);
            cooldown.setTouchable(Touchable.disabled);
            cooldown.setBounds(0f, 0f, PLANT_CARD_W, PLANT_CARD_H);
            stack.addActor(cooldown);

            seedCooldownLabels.add(cooldown);

            stack.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    selectedSeed = card.name().equalsIgnoreCase(selectedSeed) ? null : card.name();
                    AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.5f);
                }
            });

            seedViews.add(stack);
            seedDim.add(dim);
            seedSel.add(sel);
            tray.add(wrapWithCardFrame(stack, PLANT_CARD_W, PLANT_CARD_H)).size(PLANT_CARD_W, PLANT_CARD_H).pad(1f).row();
        }

        plantTrayTitle = new Label("P1 MOUSE", skin, "main");
        plantTrayTitle.setFontScale(0.7f);
        plantTrayTitle.setAlignment(Align.center);
        plantTrayTitle.setWrap(true);
        tray.add(plantTrayTitle).width(PLANT_CARD_W).padTop(2f).row();
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
        tray.pad(2f).top();

        zombieViews.clear();
        zombieDim.clear();
        zombieSel.clear();
        zombieCostLabels.clear();
        zombieCooldownLabels.clear();

        zombieTrayTitle = new Label("P2 KEYBOARD", skin, "main");
        zombieTrayTitle.setFontScale(0.7f);
        zombieTrayTitle.setAlignment(Align.center);
        tray.add(zombieTrayTitle).width(ZOMBIE_CARD_W).padBottom(2f).row();

        
        
        Table sunRow = new Table();
        zombieSunIcon = new Image(brainTexture);
        zombieSunAmountLabel = new Label(String.valueOf(match.getZombieSun()), skin, "main");
        zombieSunAmountLabel.setFontScale(0.7f);
        sunRow.add(zombieSunIcon).size(20f, 20f).padRight(3f);
        sunRow.add(zombieSunAmountLabel);
        tray.add(sunRow).padBottom(4f).row();

        List<ZombiePacket> roster = match.getRoster();
        for (int i = 0; i < roster.size(); i++) {
            ZombiePacket packet = roster.get(i);
            final int slotIndex = i;

            Group stack = new Group();
            stack.setTouchable(Touchable.enabled);
            stack.setSize(ZOMBIE_CARD_W, ZOMBIE_CARD_H);
            try {
                ZombieIconCard art = zombieCards.buildCardForAlias(packet.getAlias(), ZOMBIE_CARD_W, ZOMBIE_CARD_H);
                if (art != null) {
                    art.setTouchable(Touchable.disabled);
                    art.setBounds(0f, 0f, ZOMBIE_CARD_W, ZOMBIE_CARD_H);
                    stack.addActor(art);
                }
            } catch (Throwable ignored) {
            }
            if (stack.getChildren().isEmpty()) {
                Table fallback = new Table();
                fallback.setBackground(skin.getDrawable("card-background"));
                Label label = new Label(packet.getDisplayName(), skin, "main");
                label.setAlignment(Align.center);
                label.setFontScale(0.6f);
                label.setWrap(true);
                fallback.add(label).width(ZOMBIE_CARD_W).center();
                fallback.setBounds(0f, 0f, ZOMBIE_CARD_W, ZOMBIE_CARD_H);
                stack.addActor(fallback);
            }

            Image dim = new Image(new TextureRegionDrawable(whitePixelRegion()));
            dim.setColor(0f, 0f, 0f, 0.6f);
            dim.setFillParent(true);
            dim.setTouchable(Touchable.disabled);
            dim.setBounds(0f, 0f, ZOMBIE_CARD_W, ZOMBIE_CARD_H);
            stack.addActor(dim);

            Image sel = new Image(new TextureRegionDrawable(whitePixelRegion()));
            sel.setColor(0.95f, 0.55f, 0.25f, 0.30f);
            sel.setFillParent(true);
            sel.setTouchable(Touchable.disabled);
            sel.setBounds(0f, 0f, ZOMBIE_CARD_W, ZOMBIE_CARD_H);
            stack.addActor(sel);

            Label cost = new Label(String.valueOf(packet.getCost()), skin, "main");
            cost.setFontScale(0.6f);
            cost.setAlignment(Align.bottomRight);
            cost.setTouchable(Touchable.disabled);
            cost.setBounds(0f, 0f, ZOMBIE_CARD_W - 2f, ZOMBIE_CARD_H - 2f);
            stack.addActor(cost);

            Label cooldown = new Label("", skin, "title");
            cooldown.setFontScale(0.78f);
            cooldown.setAlignment(Align.center);
            cooldown.setTouchable(Touchable.disabled);
            cooldown.setBounds(0f, 0f, ZOMBIE_CARD_W, ZOMBIE_CARD_H);
            stack.addActor(cooldown);

            stack.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    packetIndex = slotIndex;
                    AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.5f);
                }
            });

            zombieViews.add(stack);
            zombieDim.add(dim);
            zombieSel.add(sel);
            zombieCostLabels.add(cost);
            zombieCooldownLabels.add(cooldown);
            tray.add(stack).size(ZOMBIE_CARD_W, ZOMBIE_CARD_H).pad(1f).row();
        }

        Label help = new Label("W/S lane  A/D col  SPACE drop", skin, "muted");
        help.setFontScale(0.55f);
        help.setAlignment(Align.center);
        help.setWrap(true);
        tray.add(help).width(ZOMBIE_CARD_W).padTop(4f).row();

        tray.pack();
        
        
        tray.setPosition(stage.getWidth() - ZOMBIE_TRAY_AREA_WIDTH
                + (ZOMBIE_TRAY_AREA_WIDTH - tray.getWidth()) * 0.5f, 26f);
        zombieTray = tray;
        stage.addActor(zombieTray);
    }

    private static String index1(int zeroBasedIndex) {
        return String.valueOf(zeroBasedIndex + 1);
    }

    
    private com.badlogic.gdx.scenes.scene2d.Actor wrapWithCardFrame(
            com.badlogic.gdx.scenes.scene2d.Actor content, float outerW, float outerH) {
        Table framed = new Table();
        framed.setBackground(skin.getDrawable("card-background"));
        framed.pad(3f);
        framed.add(content).size(outerW - 6f, outerH - 6f);
        return framed;
    }

    private void updateTrays(IZombieMatch match) {
        List<SeedCard> seeds = match.getSeeds();
        for (int i = 0; i < seedViews.size() && i < seeds.size(); i++) {
            SeedCard card = seeds.get(i);
            boolean ready = match.getSession().isPlantReady(card.plantId());
            boolean affordable = match.getPlantSun() >= card.cost();
            seedDim.get(i).setVisible(!ready || !affordable);
            seedSel.get(i).setVisible(card.name().equalsIgnoreCase(selectedSeed));
            Label cooldown = seedCooldownLabels.get(i);
            if (!ready) {
                cooldown.setText(String.format("%.1f", match.getSession().getPlantCooldown(card.plantId())));
                cooldown.setVisible(true);
            } else {
                cooldown.setVisible(false);
            }
        }

        List<ZombiePacket> roster = match.getRoster();
        for (int i = 0; i < zombieViews.size() && i < roster.size(); i++) {
            ZombiePacket packet = roster.get(i);
            boolean ready = packet.isReady();
            boolean affordable = match.getZombieSun() >= packet.getCost();
            zombieDim.get(i).setVisible(!ready || !affordable);
            zombieSel.get(i).setVisible(i == packetIndex);
            zombieCostLabels.get(i).setText(String.valueOf(packet.getCost()));
            zombieCostLabels.get(i).setColor(affordable ? Color.WHITE : Color.RED);
            Label cooldown = zombieCooldownLabels.get(i);
            if (!ready) {
                cooldown.setText(String.format("%.1f", packet.getCooldown()));
                cooldown.setVisible(true);
            } else {
                cooldown.setVisible(false);
            }
        }
        if (zombieSunAmountLabel != null) {
            zombieSunAmountLabel.setText(String.valueOf(match.getZombieSun()));
        }
        if (plantTrayTitle != null) {
            plantTrayTitle.setText("P1  sun " + match.getPlantSun());
        }
    }

    @Override
    public void onMatchEndSequenceFinished(boolean won) {
        
    }
}