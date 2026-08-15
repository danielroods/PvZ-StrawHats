package view.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;

import model.collections.plant.PlantJsonParser;
import model.match.main.levels.Level;
import model.match.main.levels.special_levels.ConveyorBeltLevel;
import model.utils.GameSession;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Single in-match HUD layer. Gameplay mutation stays in GameScreen. */
public final class MatchHud extends Table implements Disposable {
    private static final float CARD_W = 100f;
    private static final float CARD_H = 60f;

    private final Button shovelButton;
    private final Skin skin;
    private final SeedPacketCardFactory cardFactory = new SeedPacketCardFactory();
    private final Table loadoutRow = new Table();
    private final Table conveyorBox = new Table();
    private final Label sunLabel;
    private final Label foodLabel;
    private final Label coinLabel;
    private final Label waveLabel;
    private final Label objectiveLabel;
    private final TextButton pauseButton;
    private final TextButton foodButton;
    private final TextButton startButton;
    private final TextButton debugAddSunButton;
    private final TextButton debugAddFoodButton;
    private final Table debugRow = new Table();

    private Consumer<String> plantSelection;
    private Consumer<Vector2> plantDragRelease;
    private Runnable shovelAction;
    private Runnable foodAction;
    private Runnable pauseAction;
    private Runnable startWavesAction;
    private Runnable debugAddSunAction;
    private Runnable debugAddFoodAction;
    private String selectedPlant;
    private String lastLoadoutKey = "";
    private String lastConveyorPlant = null;
    private final List<SlotView> slotViews = new ArrayList<>();
    private boolean shovelActive;
    private boolean foodActive;

    private static final class SlotView {
        final String name;
        final int id;
        final int cost;
        final Stack stack;
        final Image unavailable;
        final Image selected;
        final Label costLabel;
        final Label cooldownLabel;
        SlotView(String name, int id, int cost, Stack stack, Image unavailable, Image selected,
                 Label costLabel, Label cooldownLabel) {
            this.name = name; this.id = id; this.cost = cost; this.stack = stack;
            this.unavailable = unavailable; this.selected = selected;
            this.costLabel = costLabel; this.cooldownLabel = cooldownLabel;
        }
    }

    public MatchHud(Skin skin) {
        this.skin = skin;
        setFillParent(true);
        setTouchable(Touchable.childrenOnly);
        top().left();
        pad(8f);

        sunLabel = new Label("0", skin, "title");
        foodLabel = new Label("0", skin, "title");
        coinLabel = new Label("0", skin, "title");
        waveLabel = new Label("WAVES 0/0", skin, "main");
        objectiveLabel = new Label("", skin, "main");
        objectiveLabel.setAlignment(Align.center);
        objectiveLabel.setWrap(true);

        pauseButton = new TextButton("II", skin);
        Texture shovelBtnTex = loadTexture("assets/images/chapters/egypt/gameplay/shovel_button.png");
        shovelButton = new ImageButton(new TextureRegionDrawable(new TextureRegion(shovelBtnTex)));
        foodButton = new TextButton("Food", skin);
        startButton = new TextButton("START", skin);
        debugAddSunButton = new TextButton("+25 Sun", skin);
        debugAddFoodButton = new TextButton("+1 Food", skin);

        pauseButton.addListener(click(() -> { if (pauseAction != null) pauseAction.run(); }));
        shovelButton.addListener(click(() -> { if (shovelAction != null) shovelAction.run(); }));
        foodButton.addListener(click(() -> { if (foodAction != null) foodAction.run(); }));
        startButton.addListener(click(() -> { if (startWavesAction != null) startWavesAction.run(); }));
        debugAddSunButton.addListener(click(() -> { if (debugAddSunAction != null) debugAddSunAction.run(); }));
        debugAddFoodButton.addListener(click(() -> { if (debugAddFoodAction != null) debugAddFoodAction.run(); }));

        Table resources = new Table();
        resources.add(resource(sunLabel, "images/chapters/egypt/gameplay/sun.png")).size(92, 40).padRight(3);
        resources.add(resource(foodLabel, "images/chapters/egypt/gameplay/plantfood.png")).size(92, 40).padRight(3);
        resources.add(resource(coinLabel, "assets/images/ui/buttons_coin_buy_normal.png")).size(92, 40).padRight(6);

        Table topRow = new Table();
        topRow.add(resources).left().padRight(6);
        topRow.add(waveLabel).width(120).center().padRight(6);
        topRow.add(objectiveLabel).width(210).center().expandX().fillX().padRight(6);
        topRow.add(startButton).size(100, 42).padRight(5);
        topRow.add(pauseButton).size(54, 42);

        Table bankFrame = new Table();
        bankFrame.setBackground(skin.getDrawable("card-background"));
        bankFrame.pad(5f);
        loadoutRow.left();
        bankFrame.add(loadoutRow).left().expandX().fillX();

        conveyorBox.setBackground(skin.getDrawable("card-background"));
        conveyorBox.pad(4f);
        conveyorBox.setVisible(false);

        Table tools = new Table();
        tools.add(shovelButton).size(60, 60).padRight(5);
        tools.add(foodButton).size(100, 40);

        debugRow.left();
        debugRow.add(debugAddSunButton).size(100, 36).padRight(5);
        debugRow.add(debugAddFoodButton).size(100, 36);
        debugRow.setVisible(false);

        add(topRow).growX().row();
        add(bankFrame).growX().padTop(4).row();
        add(conveyorBox).left().padTop(3).row();
        add(tools).left().padTop(4).row();
        add(debugRow).left().padTop(4);
    }

    private ClickListener click(Runnable action) {
        return new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { action.run(); }
        };
    }

    private Table resource(Label value, String path) {
        Stack stack = new Stack();
        Image image = new Image(loadTexture(path));
        image.setScaling(Scaling.fit);
        stack.add(image);
        Table text = new Table();
        text.add(value).center().expand();
        stack.add(text);
        Table out = new Table();
        out.add(stack).grow();
        return out;
    }

    private Texture loadTexture(String path) {
        String resolved = path;
        if (!Gdx.files.internal(resolved).exists() && resolved.startsWith("assets/")) {
            resolved = resolved.substring("assets/".length());
        }
        if (Gdx.files.internal(resolved).exists()) {
            Texture t = new Texture(Gdx.files.internal(resolved));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return t;
        }
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private Texture solid(Color color) {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(color);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    public void setPlantSelection(Consumer<String> callback) { plantSelection = callback; }
    public void setPlantDragRelease(Consumer<Vector2> callback) { plantDragRelease = callback; }
    public void setShovelAction(Runnable action) { shovelAction = action; }
    public void setFoodAction(Runnable action) { foodAction = action; }
    public void setPauseAction(Runnable action) { pauseAction = action; }
    public void setStartWavesAction(Runnable action) { startWavesAction = action; }
    public void setDebugAddSunAction(Runnable action) { debugAddSunAction = action; }
    public void setDebugAddFoodAction(Runnable action) { debugAddFoodAction = action; }
    public void setSelectedPlant(String name) { selectedPlant = name; }
    public void setTools(boolean shovel, boolean food) { shovelActive = shovel; foodActive = food; }

    public void update(GameSession session, List<String> selectedPlants) {
        if (session == null) return;
        sunLabel.setText(String.valueOf(session.getSunCount()));
        foodLabel.setText(String.valueOf(session.getPlantFoodCount()));
        int coins = 0;
        if (model.user_data.User.currentUser != null && model.user_data.User.currentUser.userState != null) {
            coins = model.user_data.User.currentUser.userState.coins;
        }
        coinLabel.setText(String.valueOf(coins));
        waveLabel.setText("WAVES " + session.getWavesSpawnedCount() + "/" + Math.max(1, session.getTotalWaveCount()));
        objectiveLabel.setText(objectiveFor(session.getLevel()));
        startButton.setVisible(!session.isWavesStarted() && !(session.getLevel() instanceof ConveyorBeltLevel));
        shovelButton.setChecked(shovelActive);
        foodButton.setChecked(foodActive);
        foodButton.setDisabled(session.getPlantFoodCount() <= 0);
        debugRow.setVisible(model.utils.GameSettings.get().isDebugMode());
        updateLoadout(session, selectedPlants);
        updateConveyor(session);
    }

    private String objectiveFor(Level level) {
        if (level == null) return "";
        if (level instanceof model.match.main.levels.special_levels.IntroductionLevel) return "LEARN THE BASICS";
        if (level instanceof ConveyorBeltLevel) return "CONVEYOR BELT";
        if (level instanceof model.match.main.levels.special_levels.LockedPlantsLevel) return "LOCKED PLANTS";
        if (level instanceof model.match.main.levels.special_levels.BossLevel) return "BOSS BATTLE";
        if (level instanceof model.match.main.levels.special_levels.SaveOurSeedsLevel) return "PROTECT YOUR PLANTS";
        if (level instanceof model.match.main.levels.special_levels.DeadLineLevel) return "DO NOT CROSS THE LINE";
        if (level instanceof model.match.main.levels.special_levels.TimedWarLevel) return "SURVIVE THE TIMER";
        return "SURVIVE THE WAVES";
    }

    private void updateLoadout(GameSession session, List<String> selectedPlants) {
        String key = String.join("\u0001", selectedPlants);
        if (!key.equals(lastLoadoutKey)) {
            lastLoadoutKey = key;
            rebuildLoadout(session, selectedPlants);
        }

        for (SlotView slot : slotViews) {
            boolean ready = slot.id < 0 || session.isPlantReady(slot.id);
            boolean affordable = session.getSunCount() >= slot.cost;
            slot.unavailable.setVisible(!ready || !affordable);
            slot.selected.setVisible(selectedPlant != null && selectedPlant.equalsIgnoreCase(slot.name));
            slot.costLabel.setColor(affordable ? Color.WHITE : Color.RED);
            if (!ready && slot.id >= 0) {
                slot.cooldownLabel.setText(String.format("%.1f", session.getPlantCooldown(slot.id)));
                slot.cooldownLabel.setVisible(true);
            } else {
                slot.cooldownLabel.setVisible(false);
            }
        }
    }

    private void rebuildLoadout(GameSession session, List<String> selectedPlants) {
        loadoutRow.clearChildren();
        slotViews.clear();
        if (session.getLevel() instanceof ConveyorBeltLevel) return;

        for (String name : selectedPlants) {
            SlotView slot = createPlantSlot(session, name);
            slotViews.add(slot);
            loadoutRow.add(slot.stack).size(CARD_W, CARD_H).pad(2f);
        }
        for (int i = selectedPlants.size(); i < 8; i++) {
            Table empty = new Table();
            empty.setBackground(skin.getDrawable("card-background"));
            loadoutRow.add(empty).size(CARD_W, CARD_H).pad(2f);
        }
    }

    private SlotView createPlantSlot(GameSession session, String plantName) {
        Stack stack = new Stack();
        stack.setTouchable(Touchable.enabled);

        SeedPacketCard card = null;
        try { card = cardFactory.buildCardForDisplayName(plantName); } catch (Throwable ignored) {}
        if (card != null) {
            card.setSize(CARD_W, CARD_H);
            card.setTouchable(Touchable.disabled);
            stack.add(card);
        } else {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            fallback.add(new Label(plantName, skin, "main")).center();
            stack.add(fallback);
        }

        PlantJsonParser.PlantConfig config = findConfig(plantName);
        int cost = config == null ? 0 : config.cost;
        int id = config == null ? -1 : config.id;

        Image unavailable = new Image(new TextureRegionDrawable(solid(new Color(0f, 0f, 0f, 0.60f))));
        unavailable.setFillParent(true);
        unavailable.setTouchable(Touchable.disabled);
        stack.add(unavailable);

        Image selected = new Image(new TextureRegionDrawable(solid(new Color(0.25f, 1f, 0.25f, 0.30f))));
        selected.setFillParent(true);
        selected.setTouchable(Touchable.disabled);
        stack.add(selected);

        Label costLabel = new Label(String.valueOf(cost), skin, "main");
        Table costTable = new Table();
        costTable.bottom().right();
        costTable.add(costLabel).pad(2);
        costTable.setTouchable(Touchable.disabled);
        stack.add(costTable);

        Label cooldownLabel = new Label("", skin, "title");
        cooldownLabel.setAlignment(Align.center);
        Table cd = new Table();
        cd.setFillParent(true);
        cd.setTouchable(Touchable.disabled);
        cd.add(cooldownLabel).center().expand();
        stack.add(cd);

        final int slotId = id;
        final int slotCost = cost;
        stack.addListener(new InputListener() {
            private boolean dragging;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                boolean ready = slotId < 0 || session.isPlantReady(slotId);
                boolean affordable = session.getSunCount() >= slotCost;
                if (!ready || !affordable || plantSelection == null) return false;
                dragging = true;
                plantSelection.accept(plantName);
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                // Selection stays armed while the pointer travels from the card to the lawn.
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (!dragging) return;
                dragging = false;
                if (plantDragRelease != null) {
                    Vector2 stagePos = new Vector2(event.getStageX(), event.getStageY());
                    plantDragRelease.accept(stagePos);
                }
            }
        });
        return new SlotView(plantName, id, cost, stack, unavailable, selected, costLabel, cooldownLabel);
    }

    private PlantJsonParser.PlantConfig findConfig(String name) {
        for (PlantJsonParser.PlantConfig config : model.collections.plant.PlantFactory.getBlueprints().values()) {
            if (config.name.equalsIgnoreCase(name)) return config;
        }
        return null;
    }

    private void updateConveyor(GameSession session) {
        if (!(session.getLevel() instanceof ConveyorBeltLevel conveyor)) {
            conveyorBox.setVisible(false);
            lastConveyorPlant = null;
            return;
        }
        String current = conveyor.getCurrentPlant() == null ? null : conveyor.getCurrentPlant().getName();
        if (java.util.Objects.equals(current, lastConveyorPlant)) {
            conveyorBox.setVisible(true);
            return;
        }
        lastConveyorPlant = current;
        conveyorBox.setVisible(true);
        conveyorBox.clearChildren();
        conveyorBox.add(new Label("CONVEYOR", skin, "title")).padRight(10);
        if (current != null) {
            SlotView slot = createPlantSlot(session, current);
            conveyorBox.add(slot.stack).size(CARD_W, CARD_H);
            slotViews.remove(slot);
        } else {
            conveyorBox.add(new Label("Waiting for next plant...", skin, "main"));
        }
    }

    @Override public void dispose() { cardFactory.dispose(); }
}