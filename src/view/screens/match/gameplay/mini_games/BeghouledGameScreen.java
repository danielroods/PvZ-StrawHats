package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import com.badlogic.gdx.utils.Align;
import controller.ScreenManager;
import controller.match.mini_games.BeghouledController;
import model.App;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.plant.Plant;
import model.match.mini_games.Beghouled;
import model.match_mechanisms.vector.Position;
import service.GameClock;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class BeghouledGameScreen extends GameScreen {

    private static final String FOLDER = "assets/images/backg/mini_games/begh/";

    private static final float SWAP_ANIM_DURATION = 0.15f;
    private static final float FALL_ANIM_BASE_DURATION = 0.10f;
    private static final float FALL_ANIM_PER_ROW = 0.05f;
    private static final float FALL_ANIM_MAX_DURATION = 0.45f;
    private static final float REFILL_DROP_IN_ROWS = 2f;

    private Label sunLabel;
    private Label matchesLabel;
    private Table hudTable;
    private final SeedPacketCardFactory upgradeCardFactory = new SeedPacketCardFactory();
    private final List<UpgradeCardView> upgradeCardViews = new ArrayList<>();

    private static final float UPGRADE_CARD_W = 95f;
    private static final float UPGRADE_CARD_H = 60f;

    private static final class UpgradeCardView {
        final int upgradeCost;
        final int plantCost;
        final double recharge;
        final Image unaffordableOverlay;
        final Label costLabel;
        final Label rechargeLabel;

        UpgradeCardView(int upgradeCost, int plantCost, double recharge,
                        Image unaffordableOverlay, Label costLabel, Label rechargeLabel) {
            this.upgradeCost = upgradeCost;
            this.plantCost = plantCost;
            this.recharge = recharge;
            this.unaffordableOverlay = unaffordableOverlay;
            this.costLabel = costLabel;
            this.rechargeLabel = rechargeLabel;
        }
    }

    private int selectedRow = -1;
    private int selectedCol = -1;
    private Actor boardInputActor;
    private float highlightPulseTime = 0f;

    private static final class VisualAnim {
        final Position from;
        final Position to;
        final float duration;
        float elapsed;

        VisualAnim(Position from, Position to, float duration) {
            this.from = from;
            this.to = to;
            this.duration = duration;
        }
    }

    private final Map<Plant, Position> lastKnownGridPos = new IdentityHashMap<>();
    private final Map<Plant, VisualAnim> activeAnims = new IdentityHashMap<>();

    public BeghouledGameScreen() {
        seasonFolder = "beghouled";
    }

    @Override
    protected String getSeasonGameplayFolder() {
        return FOLDER;
    }

    @Override
    protected String getGameplayBackgroundPath() {
        return getSeasonGameplayFolder() + "texture.png";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
    }

    @Override
    protected void tickSession() {
        if (App.currentMenu instanceof BeghouledController controller) {
            controller.tick(GameClock.SECONDS_PER_TICK);
        }
    }

    @Override
    protected void checkMatchEnd() {
        if (matchFinished) return;
        if (!(App.currentMenu instanceof BeghouledController)) {
            matchFinished = true;
            ScreenManager.syncWithCurrentMenu();
        }
    }

    @Override
    protected void createBoardInput() {
        Actor input = new Actor();
        input.setTouchable(Touchable.enabled);
        input.setBounds(BOARD_X, BOARD_Y, session.getCols() * getBoardTileWidth(),
                session.getRows() * getBoardTileHeight());
        input.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleTileClick(event.getStageX(), event.getStageY());
            }
        });
        addBeforeModal(input);
        boardInputActor = input;
        if (hudTable != null) hudTable.toFront();
    }

    private void handleTileClick(float stageX, float stageY) {
        if (matchFinished || session == null) return;
        if (App.currentMenu instanceof BeghouledController controller && controller.getGame().isResolving()) {
            return;
        }

        Vector2 click = new Vector2(stageX, stageY);
        if (collectUnderMouse(click)) return;

        float tileW = getBoardTileWidth();
        float tileH = getBoardTileHeight();
        int col = (int) ((stageX - BOARD_X) / tileW);
        int row = session.getRows() - 1 - (int) ((stageY - BOARD_Y) / tileH);
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) return;

        if (selectedRow < 0) {
            selectedRow = row;
            selectedCol = col;
            return;
        }
        if (selectedRow == row && selectedCol == col) {
            selectedRow = -1;
            selectedCol = -1;
            return;
        }

        boolean adjacent = Math.abs(selectedRow - row) + Math.abs(selectedCol - col) == 1;
        if (!adjacent) {
            selectedRow = row;
            selectedCol = col;
            return;
        }

        runCommand("swap -l (" + (selectedCol + 1) + ", " + (selectedRow + 1) + ") -l ("
                + (col + 1) + ", " + (row + 1) + ")");
        selectedRow = -1;
        selectedCol = -1;
    }

    @Override
    protected void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        updatePlantAnimations(delta);
        drawMatchHighlight(delta);

        if (selectedRow < 0 || selectedCol < 0 || whitePixel == null) return;

        float tileW = getBoardTileWidth();
        float tileH = getBoardTileHeight();
        float x = BOARD_X + selectedCol * tileW;
        float y = getCellCenterY(selectedRow) - tileH / 2f;
        float thickness = 4f;

        batch.setColor(1f, 0.92f, 0.2f, 0.9f);
        batch.draw(whitePixel, x, y, tileW, thickness);
        batch.draw(whitePixel, x, y + tileH - thickness, tileW, thickness);
        batch.draw(whitePixel, x, y, thickness, tileH);
        batch.draw(whitePixel, x + tileW - thickness, y, thickness, tileH);
        batch.setColor(Color.WHITE);
    }

    /** Brief pulsing highlight over plants about to be cleared, so removal reads as a "pop"
     *  instead of an instant vanish once the model actually removes them. */
    private void drawMatchHighlight(float delta) {
        if (whitePixel == null || !(App.currentMenu instanceof BeghouledController controller)) return;
        var positions = controller.getGame().getMatchHighlightPositions();
        if (positions.isEmpty()) return;

        highlightPulseTime += delta;
        float tileW = getBoardTileWidth();
        float tileH = getBoardTileHeight();
        float pulse = 0.55f + 0.35f * (float) Math.sin(highlightPulseTime * 14f);

        batch.setColor(1f, 1f, 0.85f, pulse);
        for (Position pos : positions) {
            int col = (int) Math.round(pos.x());
            int row = (int) Math.round(pos.y());
            float x = BOARD_X + col * tileW;
            float y = getCellCenterY(row) - tileH / 2f;
            batch.draw(whitePixel, x, y, tileW, tileH);
        }
        batch.setColor(Color.WHITE);
    }

    private boolean boardSeeded = false;

    private void updatePlantAnimations(float delta) {
        if (session == null) return;

        if (!boardSeeded) {
            for (Plant plant : session.getPlants()) {
                if (plant != null && plant.isAlive() && plant.getPosition() != null) {
                    lastKnownGridPos.put(plant, plant.getPosition());
                }
            }
            boardSeeded = true;
            return;
        }

        for (Plant plant : session.getPlants()) {
            if (plant == null || !plant.isAlive() || plant.getPosition() == null) continue;
            Position current = plant.getPosition();
            Position last = lastKnownGridPos.get(plant);

            if (last == null) {
                lastKnownGridPos.put(plant, current);
                Position dropFrom = new Position(current.x(), current.y() - REFILL_DROP_IN_ROWS);
                activeAnims.put(plant, new VisualAnim(dropFrom, current, fallDuration(REFILL_DROP_IN_ROWS)));
                continue;
            }

            if (current.x() != last.x() || current.y() != last.y()) {
                Position from = currentVisualPosition(plant, last);
                float rows = (float) Math.max(Math.abs(current.x() - from.x()), Math.abs(current.y() - from.y()));
                float duration = rows > 1f ? fallDuration(rows) : SWAP_ANIM_DURATION;
                activeAnims.put(plant, new VisualAnim(from, current, duration));
                lastKnownGridPos.put(plant, current);
            }
        }

        lastKnownGridPos.keySet().removeIf(p -> !p.isAlive() || !session.getPlants().contains(p));
        activeAnims.keySet().removeIf(p -> !p.isAlive() || !session.getPlants().contains(p));

        for (VisualAnim anim : activeAnims.values()) {
            anim.elapsed = Math.min(anim.duration, anim.elapsed + delta);
        }
    }

    private float fallDuration(float rows) {
        return Math.min(FALL_ANIM_MAX_DURATION, FALL_ANIM_BASE_DURATION + FALL_ANIM_PER_ROW * rows);
    }

    private Position currentVisualPosition(Plant plant, Position fallback) {
        VisualAnim anim = activeAnims.get(plant);
        if (anim == null) return fallback;
        float t = anim.duration <= 0f ? 1f : Math.min(1f, anim.elapsed / anim.duration);
        float eased = 1f - (1f - t) * (1f - t) * (1f - t);
        return new Position(
                anim.from.x() + (anim.to.x() - anim.from.x()) * eased,
                anim.from.y() + (anim.to.y() - anim.from.y()) * eased);
    }

    @Override
    protected Position visualPositionFor(Plant plant) {
        return currentVisualPosition(plant, plant.getPosition());
    }

    @Override
    protected void createHud() {
        Table root = new Table();
        root.setFillParent(true);
        root.top();
        root.pad(16f);

        Table topBar = new Table();
        topBar.add(secondaryButton("Back", () -> runCommand("menu exit"))).size(110, 46).padRight(20);
        sunLabel = createLabel("Sun: 0", "title");
        topBar.add(sunLabel).padRight(24);
        matchesLabel = createLabel("Matches: 0/0", "title");
        topBar.add(matchesLabel).expandX().left();
        root.add(topBar).fillX().row();

        Table upgradeRow = new Table();
        upgradeCardViews.clear();
        if (App.currentMenu instanceof BeghouledController controller) {
            Beghouled game = controller.getGame();
            for (String plantName : game.getUpgradeablePlantNames()) {
                int cost = game.getUpgradeCost(plantName);
                upgradeRow.add(buildUpgradeCard(plantName, cost)).size(UPGRADE_CARD_W, UPGRADE_CARD_H).padRight(8);
            }
        }
        root.add(upgradeRow).left().padTop(10).row();

        hudTable = root;
        addBeforeModal(hudTable);
    }

    /** Builds one upgrade option as the same seed-packet card used on the loadout/match screens,
     *  with a sun-cost badge and a dim overlay when the player can't currently afford it. */
    private Actor buildUpgradeCard(String plantName, int upgradeCost) {
        Group stack = new Group();
        stack.setTouchable(Touchable.enabled);
        stack.setSize(UPGRADE_CARD_W, UPGRADE_CARD_H);

        SeedPacketCard card = null;
        try { card = upgradeCardFactory.buildCardForDisplayName(plantName); } catch (Throwable ignored) {}
        if (card != null) {
            card.setSize(UPGRADE_CARD_W, UPGRADE_CARD_H);
            card.setTouchable(Touchable.disabled);
            card.setBounds(0f, 0f, UPGRADE_CARD_W, UPGRADE_CARD_H);
            stack.addActor(card);
        } else {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            fallback.add(new Label(plantName, skin, "main")).center();
            fallback.setBounds(0f, 0f, UPGRADE_CARD_W, UPGRADE_CARD_H);
            stack.addActor(fallback);
        }

        int plantCost = 0;
        double recharge = 0.0;
        try {
            int plantId = PlantFactory.findPlantIdByName(plantName);
            PlantJsonParser.PlantConfig config = PlantFactory.getBlueprints().get(plantId);
            if (config != null) {
                plantCost = Math.max(0, config.cost);
                recharge = Math.max(0.0, config.recharge);
            }
        } catch (Throwable ignored) {
        }

        Image unaffordable = new Image(new TextureRegionDrawable(whitePixel));
        unaffordable.setColor(0f, 0f, 0f, 0.60f);
        unaffordable.setFillParent(true);
        unaffordable.setTouchable(Touchable.disabled);
        unaffordable.setVisible(false);
        unaffordable.setBounds(0f, 0f, UPGRADE_CARD_W, UPGRADE_CARD_H);
        stack.addActor(unaffordable);
        Label costLabel = new Label(String.valueOf(plantCost), skin, "main");
        costLabel.setFontScale(0.72f);
        costLabel.setAlignment(Align.bottomRight);
        costLabel.setTouchable(Touchable.disabled);
        costLabel.setBounds(0f, 0f, UPGRADE_CARD_W - 2f, UPGRADE_CARD_H - 2f);
        stack.addActor(costLabel);

        Label rechargeLabel = new Label(recharge > 0.0 ? String.format("%.1f", recharge) : "", skin, "title");
        rechargeLabel.setFontScale(0.72f);
        rechargeLabel.setAlignment(Align.center);
        rechargeLabel.setTouchable(Touchable.disabled);
        rechargeLabel.setBounds(0f, 0f, UPGRADE_CARD_W, UPGRADE_CARD_H);
        stack.addActor(rechargeLabel);

        Label upgradeLabel = new Label("UP " + upgradeCost, skin, "muted");
        upgradeLabel.setFontScale(0.48f);
        upgradeLabel.setAlignment(Align.topLeft);
        upgradeLabel.setTouchable(Touchable.disabled);
        upgradeLabel.setBounds(0f, 0f, UPGRADE_CARD_W, UPGRADE_CARD_H);
        stack.addActor(upgradeLabel);

        stack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                runCommand("upgrade -t " + plantName);
            }
        });

        upgradeCardViews.add(new UpgradeCardView(
                upgradeCost, plantCost, recharge, unaffordable, costLabel, rechargeLabel));
        return stack;
    }

    @Override
    protected void refreshHud(float delta) {
        if (sunLabel == null || matchesLabel == null) return;
        if (!(App.currentMenu instanceof BeghouledController controller)) return;

        Beghouled game = controller.getGame();
        int sun = game.getSession().getSunCount();
        sunLabel.setText("Sun: " + sun);
        matchesLabel.setText("Matches: " + game.getMatchesMade() + "/" + game.getMatchesNeeded());

        for (UpgradeCardView view : upgradeCardViews) {
            boolean affordable = sun >= view.upgradeCost;
            view.unaffordableOverlay.setVisible(!affordable);
            view.costLabel.setColor(affordable ? Color.WHITE : Color.RED);
        }
    }

    @Override
    public void dispose() {
        upgradeCardFactory.dispose();
        super.dispose();
    }
}