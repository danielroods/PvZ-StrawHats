package view.screens.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import controller.ScreenManager;
import controller.mini_games.WallnutBowlingController;
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
import view.general_screens.GameScreen;
import view.general_screens.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WallnutBowlingGameScreen extends GameScreen {

    private static final float BURST_DURATION = 0.4f;
    private static final String PLANTS_UI_DIR = "assets/images/ui/plants_ui/";
    private static final float NUT_SIZE_FACTOR = 0.62f;
    private static final float BIG_NUT_SIZE_FACTOR = 1.05f;

    private static final float CARD_W = 95f;
    private static final float CARD_H = 60f;
    private static final float CARD_GAP = 4f;
    private static final float BELT_PAD = 6f;
    private static final int BELT_CAPACITY = 4;
    private static final float CONVEYOR_LEFT_X = 20f;
    private static final float CONVEYOR_TOP_Y = SCREEN_HEIGHT - 90f;
    private static final float SLIDE_DURATION = 0.35f;

    {
        seasonFolder = "wallnutbowlling";
    }

    private Group conveyorPanel;
    private Group beltTrack;
    private final List<CardView> cardViews = new ArrayList<>();
    private List<WallnutBowling.NutKind> lastConveyorSnapshot = List.of();
    private WallnutBowling.NutKind selectedKind;

    private static final class CardView {
        final WallnutBowling.NutKind kind;
        final Actor actor;
        float targetY = Float.NaN;
        boolean active;
        CardView(WallnutBowling.NutKind kind, Actor actor) {
            this.kind = kind;
            this.actor = actor;
        }
    }

    private static final class ClippedGroup extends Group {
        @Override
        public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
            batch.flush();
            if (clipBegin(getX(), getY(), getWidth(), getHeight())) {
                super.draw(batch, parentAlpha);
                batch.flush();
                clipEnd();
            }
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
        return getSeasonGameplayFolder() + "texture.png";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        if (hud != null) hud.setLoadoutBankVisible(false);
        buildConveyorPanel();
        syncConveyorCards();
    }

    @Override
    public void dispose() {
        conveyorCardFactory.dispose();
        for (Texture texture : nutTextures) {
            texture.dispose();
        }
        nutTextures.clear();
        nutRegions.clear();
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
        if (matchFinished) return;
        if (!(App.currentMenu instanceof WallnutBowlingController)) {
            matchFinished = true;
            ScreenManager.syncWithCurrentMenu();
        }
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
        syncConveyorCards();
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

        float redLineX = getCellX(game.getRedLineColumn() + 1);
        drawFallback(redLineX - 2f, getBoardBottom(), 4f, bh, new Color(0.85f, 0.2f, 0.2f, 0.85f));

        if (!game.getConveyorBelt().equals(lastConveyorSnapshot)) {
            syncConveyorCards();
        }

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
        String file = kind == WallnutBowling.NutKind.EXPLODE ? "explodeonut.png" : "wallnut.png";
        return nutRegions.computeIfAbsent(file, name -> {
            Texture texture = loadTextureSafe(PLANTS_UI_DIR + name);
            nutTextures.add(texture);
            return new TextureRegion(texture);
        });
    }

    private float sizeFactorFor(WallnutBowling.NutKind kind) {
        return kind == WallnutBowling.NutKind.BIG ? BIG_NUT_SIZE_FACTOR : NUT_SIZE_FACTOR;
    }

    // TODO: no dedicated Wall-nut Bowling explosion/impact VFX asset was found in
    private void drawBurst(Burst burst) {
        float progress = burst.elapsed / BURST_DURATION;
        float x = getCellX(0) + (float) burst.position.x() * getBoardTileWidth();
        float y = getCellY((int) Math.round(burst.position.y()));
        float size = getBoardTileWidth() * (0.9f + progress * 1.6f);
        float alpha = Math.max(0f, 1f - progress);
        float boxX = x + (getBoardTileWidth() - size) * 0.5f;
        float boxY = y + (getBoardTileHeight() - size) * 0.5f;
        drawFallback(boxX, boxY, size, size, new Color(1f, 0.55f, 0.15f, alpha * 0.8f));
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

        float trackH = BELT_CAPACITY * CARD_H + (BELT_CAPACITY - 1) * CARD_GAP;
        float titleH = 22f;
        float panelW = CARD_W + BELT_PAD * 2f;
        float panelH = trackH + titleH + BELT_PAD * 2f;

        Group panel = new Group();
        panel.setSize(panelW, panelH);
        panel.setPosition(CONVEYOR_LEFT_X, CONVEYOR_TOP_Y - panelH);

        Image background = new Image(skin.getDrawable("card-background"));
        background.setSize(panelW, panelH);
        background.setTouchable(Touchable.disabled);
        panel.addActor(background);

        Label title = new Label("CONVEYOR", skin, "main");
        title.setPosition(BELT_PAD, panelH - titleH);
        title.setTouchable(Touchable.disabled);
        panel.addActor(title);

        beltTrack = new ClippedGroup();
        beltTrack.setBounds(BELT_PAD, BELT_PAD, CARD_W, trackH);
        panel.addActor(beltTrack);

        conveyorPanel = panel;
        stage.addActor(conveyorPanel);
    }

    private float slotY(int index) {
        float trackH = beltTrack.getHeight();
        return trackH - CARD_H - index * (CARD_H + CARD_GAP);
    }

    private void syncConveyorCards() {
        WallnutBowling game = currentGame();
        if (game == null || beltTrack == null) return;

        if (selectedKind != null && !game.getAvailableNutKinds().contains(selectedKind)) {
            selectedKind = null;
        }
        WallnutBowling.NutKind activeKind = selectedKind != null ? selectedKind : game.getNextNutKind();

        List<WallnutBowling.NutKind> belt = game.getConveyorBelt();
        lastConveyorSnapshot = belt;

        List<CardView> pool = new ArrayList<>(cardViews);
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
                match.actor.setPosition(0f, -CARD_H - CARD_GAP);
                beltTrack.addActor(match.actor);
            }
            ordered.add(match);
        }

        for (CardView launched : pool) {
            launched.actor.clearActions();
            launched.actor.addAction(Actions.sequence(
                    Actions.fadeOut(0.15f), Actions.removeActor()));
        }

        cardViews.clear();
        cardViews.addAll(ordered);

        for (int i = 0; i < cardViews.size(); i++) {
            CardView view = cardViews.get(i);
            float targetY = slotY(i);
            if (view.targetY != targetY) {
                view.targetY = targetY;
                view.actor.clearActions();
                view.actor.addAction(Actions.moveTo(0f, targetY, SLIDE_DURATION, Interpolation.pow2Out));
            }
            boolean active = view.kind == activeKind;
            if (view.active != active) {
                view.active = active;
                view.actor.setColor(1f, 1f, 1f, active ? 1f : 0.55f);
            }
        }
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
                syncConveyorCards();
            }
        });
        return new CardView(kind, visual);
    }
}
