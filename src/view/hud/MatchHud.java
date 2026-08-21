package view.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
import pvz.libpvz.pam.PamPlayer;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Single in-match HUD layer. Gameplay mutation stays in GameScreen. */
public final class MatchHud extends Table implements Disposable {
    private static final float CARD_W = 95f;

    private static final float CARD_H = 60;

    private final Button shovelButton;
    private final Button foodButton;
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
    private final TextButton startButton;
    private final TextButton debugAddSunButton;
    private final TextButton debugAddFoodButton;
    private final Table debugRow = new Table();
    private final ProgressBar waveProgressBar;

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
    private PamPlayer pamPlayer;
    private Table leftColumn;
    private Table rightArea;
    private String objectiveOverride;
    private String progressLabelOverride;
    private Float progressValueOverride;
    private boolean startButtonAvailable = true;

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
        pad(10f);

        sunLabel = new Label("0", skin, "title");
        foodLabel = new Label("0", skin, "title");
        coinLabel = new Label("0", skin, "title");
        waveLabel = new Label("WAVES 0/0", skin, "main");
        objectiveLabel = new Label("", skin, "main");
        objectiveLabel.setAlignment(Align.center);
        objectiveLabel.setWrap(true);

        pauseButton = new TextButton("II", skin);
        startButton = new TextButton("START", skin);

        Texture shovelBtnTex = loadTexture("assets/images/chapters/egypt/gameplay/shovel_button.png");
        shovelButton = new ImageButton(new TextureRegionDrawable(new TextureRegion(shovelBtnTex)));
        Texture foodBtnTex = loadTexture("images/chapters/egypt/gameplay/plantfood.png");
        ImageButton.ImageButtonStyle foodStyle = new ImageButton.ImageButtonStyle();
        foodStyle.imageUp = new TextureRegionDrawable(new TextureRegion(foodBtnTex));
        foodStyle.imageDown = foodStyle.imageUp;
        foodStyle.imageChecked = foodStyle.imageUp;
        foodButton = new ImageButton(foodStyle);

        debugAddSunButton = new TextButton("+25 Sun", skin);
        debugAddFoodButton = new TextButton("+1 Food", skin);

        pauseButton.addListener(click(() -> { if (pauseAction != null) pauseAction.run(); }));
        shovelButton.addListener(click(() -> { if (shovelAction != null) shovelAction.run(); }));
        foodButton.addListener(click(() -> { if (foodAction != null) foodAction.run(); }));
        startButton.addListener(click(() -> { if (startWavesAction != null) startWavesAction.run(); }));
        debugAddSunButton.addListener(click(() -> { if (debugAddSunAction != null) debugAddSunAction.run(); }));
        debugAddFoodButton.addListener(click(() -> { if (debugAddFoodAction != null) debugAddFoodAction.run(); }));

        Table sunWidget = resource(sunLabel, "images/chapters/egypt/gameplay/sun.png");

        ProgressBar.ProgressBarStyle waveStyle = new ProgressBar.ProgressBarStyle();
        waveStyle.background = new TextureRegionDrawable(new TextureRegion(solid(new Color(0f, 0f, 0f, 0.35f))));
        waveStyle.background.setMinHeight(26f);
        waveStyle.knobBefore = new TextureRegionDrawable(new TextureRegion(solid(new Color(0.30f, 0.80f, 0.25f, 1f))));
        waveStyle.knobBefore.setMinHeight(26f);
        waveProgressBar = new ProgressBar(0f, 1f, 0.001f, false, waveStyle);
        waveProgressBar.setAnimateDuration(0.25f);

        Stack waveBarStack = new Stack();
        waveBarStack.add(waveProgressBar);
        Table waveLabelOverlay = new Table();
        waveLabelOverlay.add(waveLabel).center().expand();
        waveBarStack.add(waveLabelOverlay);

        Table difficultyOverlay = new Table();
        difficultyOverlay.right();
        difficultyOverlay.add(new DifficultyMeterActor()).size(26f, 26f).padRight(-5f);
        waveBarStack.add(difficultyOverlay);

        Table centerColumn = new Table();
        centerColumn.add(waveBarStack).growX().height(26f).row();
        centerColumn.add(objectiveLabel).growX().padTop(3f).row();
        centerColumn.add(startButton).size(130, 38).padTop(5f);


        Table coinGroup = new Table();
        coinGroup.add(resource(coinLabel, "assets/images/ui/buttons_coin_buy_normal.png")).size(96, 44).padRight(4);
        coinGroup.add(pauseButton).size(50, 42);

        Table sunArea = new Table();
        sunArea.top().left();
        sunArea.add(sunWidget).row();

        debugRow.left();
        debugRow.add(debugAddSunButton).size(100, 36).padRight(5);
        debugRow.add(debugAddFoodButton).size(100, 36);
        debugRow.setVisible(false);
        sunArea.add(debugRow).left().padTop(4f);

        add(sunArea).top().left();
        add(centerColumn).expandX().fillX().top().padLeft(10).padRight(10);
        add(coinGroup).top().right();
        row();

        Table bankFrame = new Table();
        bankFrame.setBackground(skin.getDrawable("card-background"));
        bankFrame.pad(5f);
        loadoutRow.top();
        ScrollPane loadoutScroll = new ScrollPane(loadoutRow);
        loadoutScroll.setScrollingDisabled(true, false);
        loadoutScroll.setFadeScrollBars(false);
        loadoutScroll.setOverscroll(false, false);
        bankFrame.add(loadoutScroll).top().grow();

        Stack foodStack = new Stack();
        foodStack.add(foodButton);
        Table foodBadge = new Table();
        foodBadge.bottom().right();
        foodBadge.add(foodLabel).pad(2f);
        foodBadge.setTouchable(Touchable.disabled);
        foodStack.add(foodBadge);

        conveyorBox.setBackground(skin.getDrawable("card-background"));
        conveyorBox.pad(4f);
        conveyorBox.setVisible(false);

        leftColumn = new Table();
        leftColumn.top();
        leftColumn.add(bankFrame).top().expand().fill().row();
        leftColumn.add(foodStack).size(64, 64).padTop(8f).row();
        leftColumn.add(conveyorBox).top().padTop(8f);

        rightArea = new Table();
        rightArea.add().expand().fill().row();
        rightArea.add(shovelButton).size(64, 64).bottom().right().pad(10f);

        add(leftColumn).top().left().expandY().fillY();
        add(rightArea).colspan(2).expand().fill();
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
        int spawned = session.getWavesSpawnedCount();
        int total = Math.max(1, session.getTotalWaveCount());
        if (progressLabelOverride != null) {
            waveLabel.setText(progressLabelOverride);
            waveProgressBar.setValue(progressValueOverride == null ? 0f : progressValueOverride);
        } else {
            waveLabel.setText("WAVES " + spawned + "/" + total);
            waveProgressBar.setValue(Math.min(1f, (float) session.getWaveProgress()));
        }
        objectiveLabel.setText(objectiveOverride != null
                ? objectiveOverride : objectiveFor(session.getLevel()));
        startButton.setVisible(startButtonAvailable && !session.isWavesStarted()
                && !(session.getLevel() instanceof ConveyorBeltLevel));
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
            loadoutRow.add(slot.stack).size(CARD_W, CARD_H).pad(2f).row();
        }
        for (int i = selectedPlants.size(); i < 8; i++) {
            Table empty = new Table();
            empty.setBackground(skin.getDrawable("card-background"));
            loadoutRow.add(empty).size(CARD_W, CARD_H).pad(2f).row();
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
    public void setPamPlayer(PamPlayer pamPlayer) {
        this.pamPlayer = pamPlayer;
    }

    public void setLoadoutBankVisible(boolean visible) {
        leftColumn.setVisible(visible);
    }

    public void setShovelVisible(boolean visible) {
        if (rightArea != null) rightArea.setVisible(visible);
    }

    public void setStartButtonAvailable(boolean available) {
        this.startButtonAvailable = available;
    }

    public void setObjectiveOverride(String objective) {
        this.objectiveOverride = objective;
    }

    public void setProgressOverride(String label, float value) {
        this.progressLabelOverride = label;
        this.progressValueOverride = label == null ? null : value;
    }

    private final class DifficultyMeterActor extends Actor {
        private float stateTime = 0f;

        public DifficultyMeterActor() {
            setTouchable(Touchable.disabled);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
            if (pamPlayer == null) return;

            int diff = 3;
            if (model.user_data.User.currentUser != null && model.user_data.User.currentUser.userState != null) {
                diff = model.user_data.User.currentUser.userState.difficultyLevel;
            }

            String clipName = switch (diff) {
                case 1 -> "animation";
                case 2 -> "animation2";
                case 3 -> "animation3";
                case 4 -> "animation4";
                case 5 -> "animation5";
                default -> "animation";
            };

            String pamPath = "768/DEV/UI/QUESTS/DIFFICULTY_METER/DIFFICULTY_METER.PAM";
            pvz.libpvz.pam.ClipRef clip = pamPlayer.getClip(pamPath, clipName);
            if (clip == null) return;

            float duration = 0f;
            duration = switch (diff) {
                case 1 -> 0.50f;
                case 2 -> 3.33f;
                case 3 -> 3.33f;
                case 4 -> 3.33f;
                case 5 -> 3.33f;
                default -> 3.33f;
            };

            float animTime = (duration > 0f) ? (stateTime % duration) : stateTime;

            float offsetY = 50f;
            float x = getX() + getWidth() / 2f;
            float y = getY() + getHeight() / 2f - offsetY;

            float scale = 0.35f;

            batch.flush();
            com.badlogic.gdx.math.Matrix4 old = batch.getTransformMatrix().cpy();
            batch.getTransformMatrix().translate(x, y, 0f).scale(scale, scale, 1f);
            batch.setTransformMatrix(batch.getTransformMatrix());

            pamPlayer.draw(batch, clip, animTime, 0f, 0f, true);

            batch.flush();
            batch.setTransformMatrix(old);
        }
    }
    @Override public void dispose() { cardFactory.dispose(); }
}