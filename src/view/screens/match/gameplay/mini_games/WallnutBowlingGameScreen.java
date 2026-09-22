package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Disposable;

import controller.ScreenManager;
import controller.match.mini_games.MiniGameEndMenu;
import controller.match.mini_games.WallnutBowlingController;
import model.App;
import model.match.mini_games.wallnutbowlling.WallnutBowling;
import model.match.mini_games.wallnutbowlling.nut.BigNut;
import model.match.mini_games.wallnutbowlling.nut.ExplodeONut;
import model.match.mini_games.wallnutbowlling.nut.Nut;
import model.match_mechanisms.vector.Position;
import service.GameClock;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.MiniGameLawnArt;
import view.screens.generals.GameScreen;
import view.screens.generals.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WallnutBowlingGameScreen extends GameScreen {

    private static final float BURST_DURATION = 0.4f;
    private static final String NUT_ASSET_DIR = "assets/images/chapters/mini_games/vasebreaker/gameplay/";
    private static final String EGYPT_CONVEYOR_DIR = "assets/images/chapters/egypt/gameplay/";
    private static final float NUT_SIZE_FACTOR = 0.62f *1.5f;
    private static final float BIG_NUT_SIZE_FACTOR = 1.05f *1.5f;

    private static final String EXPLODE_NUT_BURST_PAM =
            "768/INITIAL/EFFECTS/VINE_BLASTBERRY_PROJECTILE_GRENADE_EXPLOSION/VINE_BLASTBERRY_PROJECTILE_GRENADE_EXPLOSION.PAM";
    private static final String EXPLODE_NUT_BURST_STATE = "attack";
    private static final float EXPLODE_NUT_BURST_SCALE = 0.35f;

    private static final float CARD_W = 95f;
    private static final float CARD_H = 60f;
    private static final int BELT_CAPACITY = 4;
    private static final float CONVEYOR_LEFT_X = 20f;
    private static final float CONVEYOR_TOP_Y = SCREEN_HEIGHT - 90f;

    {
        seasonFolder = "wallnutbowlling";
    }

    private Group conveyorPanel;
    private ConveyorBeltWidget conveyorWidget;
    private WallnutBowling.NutKind selectedKind;

    private static final class CardView {
        final WallnutBowling.NutKind kind;
        final Actor actor;
        boolean active;
        CardView(WallnutBowling.NutKind kind, Actor actor) {
            this.kind = kind;
            this.actor = actor;
        }
    }

    private final SeedPacketCardFactory conveyorCardFactory = new SeedPacketCardFactory();
    private final Map<String, TextureRegion> nutRegions = new HashMap<>();
    private final List<Texture> nutTextures = new ArrayList<>();
    private final Map<Nut, Position> lastExplodeNutPositions = new HashMap<>();
    private final List<Burst> bursts = new ArrayList<>();

    private static final class Burst {
        final Position position;
        float elapsed;
        Burst(Position position) { this.position = position; }
    }

    @Override
    protected String getSeasonGameplayFolder() {
        return "assets/images/backg/mini_games/wallnut/";
    }

    @Override
    protected String getGameplayBackgroundPath() {
        return MiniGameLawnArt.backgroundPath(getSeasonGameplayFolder());
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MINI_GAME_MUSIC, true);
        if (hud != null) {
            hud.setLoadoutBankVisible(false);
            hud.setCheatButtonsVisible(false, false, true);
        }
        buildConveyorPanel();
    }

    @Override
    public void dispose() {
        conveyorCardFactory.dispose();
        for (Texture texture : nutTextures) {
            texture.dispose();
        }
        nutTextures.clear();
        nutRegions.clear();
        if (conveyorWidget != null) {
            conveyorWidget.dispose();
        }
        super.dispose();
    }

    private WallnutBowling currentGame() {
        return App.currentMenu instanceof WallnutBowlingController controller ? controller.getGame() : null;
    }

    @Override
    protected void tickSession() {
        if (App.currentMenu instanceof WallnutBowlingController controller) {
            controller.tick(GameClock.SECONDS_PER_TICK);
        }
    }

    @Override
    protected void checkMatchEnd() {
        if (matchFinished || !isMatchEndSequenceIdle()) return;
        if (App.currentMenu instanceof WallnutBowlingController) return;

        if (App.currentMenu instanceof MiniGameEndMenu end) {
            startMatchEndSequence(end.isWon());
            return;
        }
        matchFinished = true;
        ScreenManager.syncWithCurrentMenu();
    }

    @Override
    public void onMatchEndSequenceFinished(boolean won) {
    }

    @Override
    protected void onCellClicked(int row, int col) {
        WallnutBowling game = currentGame();
        if (game == null) {
            super.onCellClicked(row, col);
            return;
        }

        if (col > game.getRedLineColumn()) {
            Toast.show(stage, "Nuts launch from behind the red line and roll the rest of the way.");
            return;
        }

        WallnutBowling.NutKind kindToPlant;
        if (selectedKind != null) {
            if (!game.getAvailableNutKinds().contains(selectedKind)) {
                Toast.show(stage, "No " + labelFor(selectedKind) + " on the belt right now.");
                return;
            }
            kindToPlant = selectedKind;
        } else {
            kindToPlant = game.getNextNutKind();
        }
        if (kindToPlant == null) {
            Toast.show(stage, "The conveyor belt is empty.");
            return;
        }
        runCommand("plant nut -t " + kindToPlant + " -l (" + (col + 1) + ", " + (row + 1) + ")");
    }

    private String labelFor(WallnutBowling.NutKind kind) {
        return switch (kind) {
            case EXPLODE -> "Explode-o-Nut";
            case BIG -> "Big Wall-nut";
            default -> "Wall-nut";
        };
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        WallnutBowling game = currentGame();
        if (game == null) return;

        drawDeadlineFlowerLine(game.getRedLineColumn() + 1, session.getRows());

        Map<Nut, Position> aliveExplodeNuts = new HashMap<>();
        for (Nut nut : game.getActiveNuts()) {
            if (!nut.isAlive()) continue;
            drawNut(nut);
            if (nut instanceof ExplodeONut) aliveExplodeNuts.put(nut, nut.getPosition());
        }

        Iterator<Map.Entry<Nut, Position>> tracked = lastExplodeNutPositions.entrySet().iterator();
        while (tracked.hasNext()) {
            Map.Entry<Nut, Position> entry = tracked.next();
            if (!aliveExplodeNuts.containsKey(entry.getKey())) {
                bursts.add(new Burst(entry.getValue()));
                tracked.remove();
            }
        }
        lastExplodeNutPositions.putAll(aliveExplodeNuts);

        Iterator<Burst> burstIt = bursts.iterator();
        while (burstIt.hasNext()) {
            Burst burst = burstIt.next();
            burst.elapsed += delta;
            if (burst.elapsed >= BURST_DURATION) {
                burstIt.remove();
                continue;
            }
            drawBurst(burst);
        }
    }

    private void drawNut(Nut nut) {
        WallnutBowling.NutKind kind = kindOf(nut);
        float diameter = getBoardTileWidth() * sizeFactorFor(kind);
        float degreesPerTile = (float) (360.0 / (Math.PI * sizeFactorFor(kind)));
        float rotation = -(float) nut.getRolledDistance() * degreesPerTile;

        float centreX = getCellX(0) + (float) nut.getPosition().x() * getBoardTileWidth()
                + getBoardTileWidth() * 0.5f;
        float centreY = getBoardBottom()
                + (float) (session.getRows() - 1 - nut.getPosition().y()) * getBoardTileHeight()
                + getBoardTileHeight() * 0.5f;

        TextureRegion region = nutRegion(kind);
        if (region == null) {
            drawFallback(centreX - diameter * 0.5f, centreY - diameter * 0.5f,
                    diameter, diameter, nutColor(nut));
            return;
        }
        batch.draw(region, centreX - diameter * 0.5f, centreY - diameter * 0.5f,
                diameter * 0.5f, diameter * 0.5f, diameter, diameter, 1f, 1f, rotation);
    }

    private TextureRegion nutRegion(WallnutBowling.NutKind kind) {
        String file = switch (kind) {
            case EXPLODE -> "explosivenut.png";
            case BIG -> "Bignut.png";
            default -> "bowlingnut.png";
        };
        return nutRegions.computeIfAbsent(file, name -> {
            Texture texture = loadTextureSafe(NUT_ASSET_DIR + name);
            nutTextures.add(texture);
            return new TextureRegion(texture);
        });
    }

    private float sizeFactorFor(WallnutBowling.NutKind kind) {
        return kind == WallnutBowling.NutKind.BIG ? BIG_NUT_SIZE_FACTOR : NUT_SIZE_FACTOR;
    }

    private void drawBurst(Burst burst) {
        float x = getCellX(0) + (float) burst.position.x() * getBoardTileWidth()
                + getBoardTileWidth() * 0.3f;
        float y = getCellY((int) Math.round(burst.position.y())) + getBoardTileHeight() * 0.3f;

        boolean drawn = drawPam(EXPLODE_NUT_BURST_PAM, EXPLODE_NUT_BURST_STATE, burst.elapsed,
                x, y, EXPLODE_NUT_BURST_SCALE, false);
        if (!drawn) {
            float progress = burst.elapsed / BURST_DURATION;
            float boardX = getCellX(0) + (float) burst.position.x() * getBoardTileWidth();
            float boardY = getCellY((int) Math.round(burst.position.y()));
            float size = getBoardTileWidth() * (0.9f + progress * 1.6f);
            float alpha = Math.max(0f, 1f - progress);
            float boxX = boardX + (getBoardTileWidth() - size) * 0.5f;
            float boxY = boardY + (getBoardTileHeight() - size) * 0.5f;
            drawFallback(boxX, boxY, size, size, new Color(1f, 0.55f, 0.15f, alpha * 0.8f));
        }
    }

    private WallnutBowling.NutKind kindOf(Nut nut) {
        if (nut instanceof ExplodeONut) return WallnutBowling.NutKind.EXPLODE;
        if (nut instanceof BigNut) return WallnutBowling.NutKind.BIG;
        return WallnutBowling.NutKind.BOWLING;
    }

    private String iconNameFor(WallnutBowling.NutKind kind) {
        return switch (kind) {
            case EXPLODE -> "explodeonut";
            case BIG -> "tallnut";
            default -> "wallnut";
        };
    }

    private Color nutColor(Nut nut) {
        return switch (kindOf(nut)) {
            case EXPLODE -> new Color(0.9f, 0.35f, 0.1f, 1f);
            case BIG -> new Color(0.6f, 0.42f, 0.2f, 1f);
            default -> new Color(0.82f, 0.65f, 0.35f, 1f);
        };
    }

    private void buildConveyorPanel() {
        if (conveyorPanel != null) return;
        if (currentGame() == null) return;

        conveyorWidget = new ConveyorBeltWidget();

        float titleH = 22f;
        float panelW = conveyorWidget.getWidth();
        float panelH = conveyorWidget.getHeight() + titleH;

        Group panel = new Group();
        panel.setSize(panelW, panelH);
        panel.setPosition(CONVEYOR_LEFT_X, CONVEYOR_TOP_Y - panelH);

        Label title = new Label("CONVEYOR", skin, "main");
        title.setPosition(0f, panelH - titleH);
        title.setTouchable(Touchable.disabled);
        panel.addActor(title);

        conveyorWidget.setPosition(0f, 0f);
        panel.addActor(conveyorWidget);

        conveyorPanel = panel;
        stage.addActor(conveyorPanel);
    }

    private CardView buildConveyorCard(WallnutBowling.NutKind kind) {
        SeedPacketCard card = null;
        try {
            card = conveyorCardFactory.buildCardByPlantName(iconNameFor(kind));
        } catch (Throwable ignored) {
        }

        Actor visual;
        if (card != null) {
            card.setSize(CARD_W, CARD_H);
            card.setTouchable(Touchable.disabled);
            visual = card;
        } else {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            fallback.add(new Label(kind.name(), skin, "main")).center();
            fallback.setSize(CARD_W, CARD_H);
            visual = fallback;
        }

        visual.setSize(CARD_W, CARD_H);
        visual.setTouchable(Touchable.enabled);
        visual.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedKind = kind;
            }
        });
        return new CardView(kind, visual);
    }

    
    private final class ConveyorBeltWidget extends Group implements Disposable {
        private static final float FRAME_WIDTH = 127f;
        private static final float SIDE_WIDTH = 18f;
        private static final float TOP_HEIGHT = 15f;
        private static final float CARD_GAP = 3f;
        private static final float BELT_SPEED = 85f;
        private static final float CARD_SPEED = BELT_SPEED;

        private final Texture topTexture = loadTextureSafe(EGYPT_CONVEYOR_DIR + "conveyor_top.png");
        private final Texture sideTexture = loadTextureSafe(EGYPT_CONVEYOR_DIR + "conveyor_side.png");
        private final Texture beltTexture = loadTextureSafe(EGYPT_CONVEYOR_DIR + "conveyor_belt.png");
        private final BeltActor beltActor = new BeltActor();
        private final ClippedCardLayer cardLayer = new ClippedCardLayer();
        private final FrameActor frameActor = new FrameActor();
        private final List<CardView> activeCards = new ArrayList<>();

        private float beltOffset;
        private final float frameHeight;

        ConveyorBeltWidget() {
            setTouchable(Touchable.childrenOnly);
            frameHeight = BELT_CAPACITY * CARD_H + (BELT_CAPACITY - 1) * CARD_GAP + TOP_HEIGHT * 2f + 6f;
            setSize(FRAME_WIDTH, frameHeight);
            addActor(beltActor);
            addActor(cardLayer);
            addActor(frameActor);
            updateChildBounds();
        }

        private void updateChildBounds() {
            beltActor.setBounds(0f, 0f, FRAME_WIDTH, frameHeight);
            cardLayer.setBounds(0f, 0f, FRAME_WIDTH, frameHeight);
            frameActor.setBounds(0f, 0f, FRAME_WIDTH, frameHeight);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            beltOffset += delta * BELT_SPEED;
            if (beltTexture.getHeight() > 0) beltOffset %= beltTexture.getHeight();
            syncCards(delta);
        }

        private float cardX() {
            return (FRAME_WIDTH - CARD_W) * 0.5f;
        }

        private void syncCards(float delta) {
            WallnutBowling game = currentGame();
            if (game == null) return;

            if (selectedKind != null && !game.getAvailableNutKinds().contains(selectedKind)) {
                selectedKind = null;
            }
            WallnutBowling.NutKind activeKind = selectedKind != null ? selectedKind : game.getNextNutKind();

            List<WallnutBowling.NutKind> belt = game.getConveyorBelt();

            List<CardView> pool = new ArrayList<>(activeCards);
            List<CardView> ordered = new ArrayList<>();
            for (WallnutBowling.NutKind kind : belt) {
                CardView match = null;
                for (Iterator<CardView> it = pool.iterator(); it.hasNext();) {
                    CardView candidate = it.next();
                    if (candidate.kind == kind) {
                        match = candidate;
                        it.remove();
                        break;
                    }
                }
                if (match == null) {
                    match = buildConveyorCard(kind);
                    match.actor.setPosition(cardX(), -CARD_H - 8f);
                    cardLayer.addActor(match.actor);
                }
                ordered.add(match);
            }

            for (CardView launched : pool) {
                launched.actor.remove();
            }

            activeCards.clear();
            activeCards.addAll(ordered);

            float topY = frameHeight - TOP_HEIGHT - CARD_H - 3f;
            float step = CARD_H + CARD_GAP;
            float x = cardX();

            for (int i = 0; i < activeCards.size(); i++) {
                CardView view = activeCards.get(i);
                float targetY = topY - i * step;
                float currentY = view.actor.getY();
                float nextY = currentY;
                if (delta > 0f) {
                    float distance = targetY - currentY;
                    float stepY = CARD_SPEED * delta;
                    nextY = Math.abs(distance) <= stepY
                            ? targetY
                            : currentY + Math.signum(distance) * stepY;
                }
                view.actor.setPosition(x, nextY);

                boolean active = view.kind == activeKind;
                if (view.active != active) {
                    view.active = active;
                    view.actor.setColor(1f, 1f, 1f, active ? 1f : 0.55f);
                }
            }
        }

        private final class BeltActor extends Actor {
            BeltActor() { setTouchable(Touchable.disabled); }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                batch.setColor(1f, 1f, 1f, parentAlpha);
                float tileH = beltTexture.getHeight();
                if (tileH <= 0f) return;

                float beltY = TOP_HEIGHT - tileH + beltOffset;
                while (beltY < getHeight() - TOP_HEIGHT) {
                    batch.draw(beltTexture, 0f, beltY, getWidth(), tileH);
                    beltY += tileH;
                }
            }
        }

        private final class ClippedCardLayer extends Group {
            ClippedCardLayer() { setTouchable(Touchable.childrenOnly); }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                batch.flush();
                Rectangle clip = new Rectangle(
                        SIDE_WIDTH,
                        TOP_HEIGHT,
                        getWidth() - SIDE_WIDTH * 2f,
                        getHeight() - TOP_HEIGHT * 2f
                );
                Rectangle scissors = new Rectangle();
                ScissorStack.calculateScissors(
                        getStage().getCamera(),
                        batch.getTransformMatrix(),
                        clip,
                        scissors
                );

                if (ScissorStack.pushScissors(scissors)) {
                    super.draw(batch, parentAlpha);
                    batch.flush();
                    ScissorStack.popScissors();
                }
            }
        }

        private final class FrameActor extends Actor {
            FrameActor() { setTouchable(Touchable.disabled); }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                batch.setColor(1f, 1f, 1f, parentAlpha);
                batch.draw(sideTexture, 0f, 0f, SIDE_WIDTH, getHeight());
                batch.draw(sideTexture, getWidth() - SIDE_WIDTH + 9f, 0f, SIDE_WIDTH, getHeight());
                batch.draw(topTexture, 0f, -5f, getWidth(), TOP_HEIGHT);
                batch.draw(topTexture, 0f, getHeight() - TOP_HEIGHT + 1f, getWidth(), TOP_HEIGHT);
            }
        }

        @Override
        public void dispose() {
            topTexture.dispose();
            sideTexture.dispose();
            beltTexture.dispose();
        }
    }
}