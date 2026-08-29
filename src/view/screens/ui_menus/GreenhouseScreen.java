package view.screens.ui_menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import controller.ui_menus.greenhouse.PotController;
import model.collections.animations.AnimationFactory;
import model.collections.animations.AnimationJsonParser;
import model.greenhouse.Greenhouse;
import model.greenhouse.Pot;
import model.greenhouse.PotPlant;
import model.resoures.CurrencyType;
import model.user_data.User;
import model.user_data.UserState;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.Toast;
import view.screens.generals.UiScreen;

import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GreenhouseScreen extends UiScreen {

    private static final String BACK_ICON = "assets/images/ui/buttons_hud_back_normal.png";
    private static final String COLLECTION_ICON = "assets/images/ui/collection.png";
    private static final String SHOP_ICON = "assets/images/greenhouse/shop_button.png";

    private static final String COIN_ICON = "assets/images/ui/buttons_coin_buy_normal.png";
    private static final String GEM_ICON = "assets/images/ui/buttons_premium_normal.png";
    static final String SEED_PACKET_BG = "assets/images/ui/seedpacket_bg.png";
    static final String SEED_PACKET_ICON = "assets/images/ui/seedpacket.png";

    private static final String GREENHOUSE_BACKGROUND = "assets/images/backg/greenhouse.png";
    private static final String POT_EMPTY_ICON = "assets/images/greenhouse/pot_empty.png";
    private static final String POT_LOCKED_ICON = "assets/images/greenhouse/pot_empty.png";
    private static final String POT_READY_ICON = "assets/images/greenhouse/pot_growing.png";
    private static final String SPARKLE_ICON = "assets/images/greenhouse/sparkles.png";
    private static final String LOCK_ICON = "assets/images/greenhouse/lock_medium.png";

    private static final float POT_SIZE = 92f;
    private static final float POT_CELL_EXTRA_WIDTH = 50f;
    private static final float POT_CELL_EXTRA_HEIGHT = 40f;
    private static final float POT_CELL_GAP = 8f;

    private static final String WATERING_PAM_PATH = "768/INITIAL/ZEN_GARDEN/ZENGARDEN_WATER_POURING/ZENGARDEN_WATER_POURING.PAM";
    private static final float WATERING_OFFSET_X = 23f;
    private static final float WATERING_OFFSET_Y = 40f;

    private TextureBank textureBank;
    private PamPlayer pamPlayer;

    private final Map<Pot, Table> potCellMap = new HashMap<>();
    private final Map<Pot, PotStatus> lastKnownStatusMap = new HashMap<>();
    private final Map<Pot, Label> potCaptionLabels = new HashMap<>();
    private float statusCheckTimer = 0f;

    private BeeActor beeActor;

    @Override
    public void show() {
        setBackground(GREENHOUSE_BACKGROUND);
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);

        if (textureBank == null) {
            try {
                FileHandle rootHandle = Gdx.files.internal("assets/pvz-assets");
                textureBank = new TextureBank("atlases", rootHandle);
                pamPlayer = new PamPlayer(textureBank, rootHandle);
            } catch (Throwable t) {
                Gdx.app.error("PAM_INIT", "Failed to initialize PAM System", t);
            }
        }

        super.show();

        if (beeActor == null) {
            beeActor = new BeeActor();
        } else if (beeActor.getStage() != null && beeActor.getStage() != stage) {
            beeActor.remove();
        }

        build();

        if (effectStack != null && beeActor != null && beeActor.getParent() != effectStack) {
            beeActor.remove();
            effectStack.addActor(beeActor);
        }

        if (beeActor != null) {
            beeActor.toFront();
        }
    }

    @Override
    public void render(float delta) {
        if (textureBank != null) {
            try {
                textureBank.update();
            } catch (Throwable t) {
                Gdx.app.error("GreenhouseScreen", "textureBank.update() failed", t);
            }
        }

        statusCheckTimer += delta;
        if (statusCheckTimer >= 0.2f) {
            statusCheckTimer = 0f;
            updatePotsRealtime();
        }

        super.render(delta);
    }

    private ClipRef getSafeClip(String pamPath, String requestedClip) {
        if (pamPlayer == null) return null;

        String resolvedName = AnimationFactory.resolveClipNameForPath(pamPath, requestedClip);
        if (resolvedName != null) {
            try {
                ClipRef clip = pamPlayer.getClip(pamPath, resolvedName);
                if (clip != null) return clip;
            } catch (Exception ignored) {}
        }

        try {
            ClipRef clip = pamPlayer.getClip(pamPath, requestedClip);
            if (clip != null) return clip;
        } catch (Exception ignored) {}

        String baseName = pamPath.substring(pamPath.lastIndexOf('/') + 1).replace(".PAM", "").replace(".pam", "");
        String[] fallbacks = {
                "anim_idle", "anim_fly", "anim_work", "anim_bee", "anim_water", "anim_pouring",
                "idle", "fly", "work", "animation", "action", "main", "default",
                baseName, baseName.toLowerCase(), baseName.toUpperCase()
        };

        for (String f : fallbacks) {
            try {
                ClipRef clip = pamPlayer.getClip(pamPath, f);
                if (clip != null) return clip;
            } catch (Exception ignored) {}
        }

        return null;
    }

    private void updatePotsRealtime() {
        boolean needsRebuild = false;
        for (Map.Entry<Pot, Table> entry : potCellMap.entrySet()) {
            Pot pot = entry.getKey();
            PotStatus currentStatus = statusOf(pot);
            PotStatus lastStatus = lastKnownStatusMap.get(pot);

            if (lastStatus != currentStatus) {
                needsRebuild = true;
                break;
            }

            if (currentStatus == PotStatus.GROWING) {
                Label captionLabel = potCaptionLabels.get(pot);
                if (captionLabel != null && pot.getPotPlant() != null) {
                    captionLabel.setText(Greenhouse.formatDuration(pot.getPotPlant().getRemainingSeconds()));
                }
            }
        }

        if (needsRebuild) {
            build();
        }
    }

    @Override
    protected void refreshContent() {
        build();
    }

    private void build() {
        rootTable.clear();

        rootTable.add(buildTopBar()).fillX().padTop(5).padLeft(15).padRight(15).row();
        Label label = new Label("Greenhouse", skin, "title");
        label.setFontScale(1.8f);
        rootTable.add(label).padTop(SPACE_MD).padBottom(-160).row();

        Greenhouse greenhouse = Greenhouse.getInstance();

        rootTable.add(buildPotGrid(greenhouse)).expand().padTop(SPACE_MD).padBottom(-145).row();

        if (rootTable != null) {
            rootTable.validate();
        }


    }

    private Table buildPotGrid(Greenhouse greenhouse) {
        potCellMap.clear();
        lastKnownStatusMap.clear();
        potCaptionLabels.clear();

        Table grid = new Table();
        for (int row = 1; row <= Greenhouse.getRowCount(); row++) {
            for (int col = 1; col <= Greenhouse.getColCount(); col++) {
                Pot pot = greenhouse.getPot(col, row);
                Table cell = buildPotCell(pot, col, row);
                potCellMap.put(pot, cell);
                grid.add(cell)
                        .size(POT_SIZE + POT_CELL_EXTRA_WIDTH, POT_SIZE + POT_CELL_EXTRA_HEIGHT)
                        .pad(POT_CELL_GAP);
            }
            grid.row();
        }
        return grid;
    }

    private enum PotStatus { LOCKED, EMPTY, GROWING, READY }

    private PotStatus statusOf(Pot pot) {
        if (pot.isLocked()) return PotStatus.LOCKED;
        PotPlant plant = pot.getPotPlant();
        if (plant == null) return PotStatus.EMPTY;
        return plant.isCollectAble() ? PotStatus.READY : PotStatus.GROWING;
    }

    private Table buildPotCell(Pot pot, int x, int y) {
        PotStatus status = statusOf(pot);
        lastKnownStatusMap.put(pot, status);

        PotPlant plant = pot.getPotPlant();

        Stack stack = new Stack();
        stack.setSize(POT_SIZE, POT_SIZE);

        String iconPath = switch (status) {
            case LOCKED -> POT_LOCKED_ICON;
            case EMPTY, GROWING -> POT_EMPTY_ICON;
            case READY -> POT_READY_ICON;
        };

        if (!iconPath.isEmpty() && Gdx.files.internal(iconPath).exists()) {
            stack.add(new Image(loadTextureSafe(iconPath)));
        } else {
            stack.add(new Label(fallbackGlyph(status, plant), skin, "title"));
        }

        if ((status == PotStatus.GROWING || status == PotStatus.READY) && plant != null && pamPlayer != null) {
            AnimationJsonParser.AnimationConfig animConfig = AnimationFactory.resolveByDisplayName(plant.getPlantName());
            if (animConfig != null && animConfig.path != null && !animConfig.path.isEmpty()) {
                Actor plantVisual = new PlantIdleAnimationActor(animConfig.path,
                        animConfig.canvasWidth(), animConfig.canvasHeight(), POT_SIZE);
                stack.add(plantVisual);
            }
        }

        if (status == PotStatus.LOCKED && !LOCK_ICON.isEmpty() && Gdx.files.internal(LOCK_ICON).exists()) {
            Table lockBadge = new Table();
            lockBadge.add(new Image(loadTextureSafe(LOCK_ICON))).size(44, 64);
            stack.add(lockBadge);
        }

        if (status == PotStatus.READY) {
            Actor readyGlow;
            if (!SPARKLE_ICON.isEmpty() && Gdx.files.internal(SPARKLE_ICON).exists()) {
                readyGlow = new Image(loadTextureSafe(SPARKLE_ICON));
            } else {
                Label ready = new Label("READY", skin, "title");
                ready.setFontScale(0.75f);
                ready.setAlignment(Align.bottom);
                readyGlow = ready;
            }
            stack.add(readyGlow);
            readyGlow.addAction(Actions.forever(Actions.sequence(
                    Actions.scaleTo(1.12f, 1.12f, 0.5f),
                    Actions.scaleTo(1f, 1f, 0.5f))));
        }

        Table column = new Table();
        column.add(stack).size(POT_SIZE, POT_SIZE).row();

        Label captionLabel = new Label(captionFor(status, plant), skin, "title");
        captionLabel.setAlignment(Align.center);
        captionLabel.setWrap(true);
        potCaptionLabels.put(pot, captionLabel);

        column.add(captionLabel).width(POT_SIZE + 26f).padTop(4);

        column.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float clickX, float clickY) {
                onPotClicked(status, pot, x, y);
            }
        });

        return column;
    }

    private void onPotClicked(PotStatus status, Pot pot, int x, int y) {
        switch (status) {
            case LOCKED -> Toast.show(stage, "Locked - buy a Vase in the Shop, or find one during a level.");
            case EMPTY -> {
                runCommand("plant pot at (" + x + ", " + y + ")");
                build();
            }
            case READY -> {
                runCommand("collect (" + x + ", " + y + ")");
                build();
            }
            case GROWING -> confirmGrow(pot, x, y);
        }
    }

    private void confirmGrow(Pot pot, int x, int y) {
        PotController controller = pot.getPotController();
        int cost = controller.calculateGrowCost();
        UserState state = User.currentUser.userState;
        String message = "Spend " + cost + " diamond(s) to finish growing " + pot.getPotPlant().getPlantName()
                + " right now? You have " + state.diamonds + ".";
        new ConfirmModal("Speed up growth?", message, "Grow now",
                () -> {
                    runCommand("grow (" + x + ", " + y + ")");
                    build();
                    triggerWateringEffect(pot);
                }).show();
    }

    private void triggerWateringEffect(Pot pot) {
        if (rootTable != null) {
            rootTable.validate();
        }
        Table targetCell = potCellMap.get(pot);
        if (targetCell != null && stage != null) {
            targetCell.validate();

            Vector2 pos = targetCell.localToStageCoordinates(new Vector2(targetCell.getWidth() / 2f, targetCell.getHeight() / 2f));
            WateringEffectActor effect = new WateringEffectActor(pos.x + WATERING_OFFSET_X, pos.y + WATERING_OFFSET_Y);
            stage.addActor(effect);
            effect.toFront();
        }
    }

    private String captionFor(PotStatus status, PotPlant plant) {
        return switch (status) {
            case LOCKED -> "Locked";
            case EMPTY -> "Tap to plant";
            case READY -> "Ready";
            case GROWING -> Greenhouse.formatDuration(plant.getRemainingSeconds());
        };
    }

    private String fallbackGlyph(PotStatus status, PotPlant plant) {
        return switch (status) {
            case LOCKED -> "";
            case EMPTY -> "+";
            case READY -> "!";
            case GROWING -> (plant != null && plant.isMarigold()) ? "M" : "P";
        };
    }

    private Table buildTopBar() {
        ImageButton backBtn = createIconButton(BACK_ICON, 54, 54, () -> runCommand("menu exit"));

        Table topLeft = new Table();
        topLeft.add(backBtn).padRight(35);
        topLeft.add(createIconButtonWithLabel(COLLECTION_ICON, 54, 54,
                "Collection", () -> runCommand("menu enter collection"))).padRight(35);
        topLeft.add(createIconButtonWithLabel(SHOP_ICON, 54, 54,
                "Shop", () -> runCommand("enter shop")));

        User user = User.currentUser;
        int coins = (user != null && user.userState != null) ? user.userState.coins : 0;
        int diamonds = (user != null && user.userState != null) ? user.userState.diamonds : 0;
        int seedPackets = totalSeedPackets(user);

        Table currencyRow = new Table();
        currencyRow.add(currencyWidget(CurrencyType.COIN, coins)).padRight(15);
        currencyRow.add(currencyWidget(CurrencyType.DIAMOND, diamonds));

        Table topRight = new Table();
        topRight.add(currencyRow).right().row();
        topRight.add(createResourceWidget(SEED_PACKET_BG, SEED_PACKET_ICON, String.valueOf(seedPackets), 130, 55)).padTop(25).padBottom(-45).padRight(8).right();

        Table topBar = new Table();
        topBar.add(topLeft).left().expandX().padBottom(-110);
        topBar.add(topRight).right().padBottom(-120);
        return topBar;
    }

    private int totalSeedPackets(User user) {
        if (user == null || user.userState == null || user.userState.seedPacketInventory == null) {
            return 0;
        }
        return user.userState.seedPacketInventory.values().stream().mapToInt(Integer::intValue).sum();
    }

    private ImageButton createIconButton(String path, float width, float height, Runnable action) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(loadTextureSafe(path));
        ImageButton button = new ImageButton(drawable);
        button.getImageCell().size(width, height);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    private Actor createIconButtonWithLabel(String path, float width, float height, String text, Runnable action) {
        Table container = new Table();
        ImageButton btn = createIconButton(path, width, height, action);
        Label label = new Label(text, skin, "title");

        container.add(btn).row();
        container.add(label).padTop(2);

        container.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return container;
    }

    private Table createResourceWidget(String bgPath, String value, float width, float height) {
        Stack stack = new Stack();
        Table bgTable = new Table();
        bgTable.setBackground(new TextureRegionDrawable(loadTextureSafe(bgPath)));

        Table textTable = new Table();
        textTable.add(new Label(value, skin, "title")).center().expand();

        stack.add(bgTable);
        stack.add(textTable);

        Table outer = new Table();
        outer.add(stack).size(width, height);
        return outer;
    }

    private Table createResourceWidget(String bgPath, String iconPath, String value, float width, float height) {
        float bgWidth = 100;
        float bgHeight = 30;
        float groupWidth = Math.max(width, bgWidth);
        float groupHeight = Math.max(height, bgHeight);

        Group group = new Group();
        group.setSize(groupWidth, groupHeight);

        Image bgImage = new Image(loadTextureSafe(bgPath));
        bgImage.setSize(bgWidth, bgHeight);
        bgImage.setPosition(((groupWidth - bgWidth) / 2f) + 20, (groupHeight - bgHeight) / 2f);
        group.addActor(bgImage);

        Table textTable = new Table();
        Image iconImage = new Image(loadTextureSafe(iconPath));
        iconImage.setSize(55,85);
        textTable.add(iconImage).left().expand();
        textTable.setSize(width, height);
        textTable.setPosition((groupWidth - width) / 2f, (groupHeight - height) / 2f);
        textTable.add(new Label(value, skin, "title")).center().expand();
        group.addActor(textTable);

        Table outer = new Table();
        outer.add(group).size(groupWidth, groupHeight);
        return outer;
    }

    private class WateringEffectActor extends Actor {
        private static final float WATERING_SCALE = 0.65f;
        private float stateTime = 0f;

        WateringEffectActor(float x, float y) {
            setPosition(x, y);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += delta;

            if (pamPlayer != null) {
                try {
                    ClipRef clip = getSafeClip(WATERING_PAM_PATH, "animation");
                    if (clip != null && stateTime >= 1.90f) {
                        remove();
                    }
                } catch (Throwable t) {
                    remove();
                }
            } else {
                remove();
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (pamPlayer == null) return;
            try {
                ClipRef clip = getSafeClip(WATERING_PAM_PATH, "animation");
                if (clip == null) return;

                float px = getX();
                float py = getY();

                Matrix4 original = batch.getTransformMatrix().cpy();
                Matrix4 scaled = new Matrix4(original)
                        .translate(px, py, 0)
                        .scale(WATERING_SCALE, WATERING_SCALE, 1f)
                        .translate(-px, -py, 0);

                batch.setTransformMatrix(scaled);

                pamPlayer.draw(batch, clip, stateTime, px, py, false);

                batch.setTransformMatrix(original);
            } catch (Throwable t) {
                Gdx.app.error("WateringEffectActor", "Failed to draw watering animation", t);
            }
        }
    }

    private class PlantIdleAnimationActor extends Actor {
        private final String animationPath;
        private final float canvasWidth;
        private final float canvasHeight;
        private final float targetSize;
        private float stateTime = 0f;

        PlantIdleAnimationActor(String animationPath, float canvasWidth, float canvasHeight, float targetSize) {
            this.animationPath = animationPath;
            this.canvasWidth = canvasWidth > 0 ? canvasWidth : targetSize;
            this.canvasHeight = canvasHeight > 0 ? canvasHeight : targetSize;
            this.targetSize = targetSize;
            setSize(targetSize, targetSize);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (pamPlayer == null || animationPath == null) return;

            try {
                ClipRef clip = getSafeClip(animationPath, "idle");
                if (clip == null) return;

                float scale = (targetSize / Math.max(canvasWidth, canvasHeight)) * 2.35f;
                float px = getX();
                float py = getY();

                float pivotX = px + (targetSize / 2f) + 57;
                float pivotY = py + (targetSize * 0.25f) + 125;

                Matrix4 original = batch.getTransformMatrix().cpy();
                Matrix4 scaled = new Matrix4(original)
                        .translate(pivotX, pivotY, 0)
                        .scale(scale, scale, 1f)
                        .translate(-pivotX, -pivotY, 0);
                batch.setTransformMatrix(scaled);

                pamPlayer.draw(batch, clip, stateTime, px, py, true);

                batch.setTransformMatrix(original);
            } catch (Throwable t) {}
        }
    }

    private class BeeActor extends Actor {
        private static final String BEE_PAM_PATH = "768/INITIAL/ZEN_GARDEN/BEE/BEE.PAM";
        private static final float BEE_SCALE = 0.45f;

        private float targetX, targetY;
        private float vx = 0f, vy = 0f;
        private float maxSpeed = 130f;

        private float globalTime = 0f;
        private float clipTime = 0f;

        private float cooldownTimer = 0f;
        private float plantActionTimer = 0f;
        private float actionTimer = 0f;
        private float wanderActionInterval = 3.5f;
        private static final float BEE_PLANT_WORK_TIME = 3.0f;
        private static final float BEE_WORK_COOLDOWN = 4.0f;
        private float turnTimer = 0f;

        private String currentClipName = "idle";
        private boolean facingLeft = true;
        private boolean nextFacingLeft = true;

        private enum State { WANDERING, MOVING_TO_PLANT, AT_PLANT, TURNING }
        private State state = State.WANDERING;
        private State stateAfterTurn = State.WANDERING;

        public BeeActor() {
            float sw = getStageWidth();
            float sh = getStageHeight();
            setPosition(sw / 2f, sh / 2f);
            pickRandomTarget();
        }

        private float getStageWidth() {
            return getStage() != null ? getStage().getWidth() : 1152f;
        }

        private float getStageHeight() {
            return getStage() != null ? getStage().getHeight() : 648f;
        }

        private void changeClip(String newClip) {
            if (!newClip.equals(currentClipName)) {
                currentClipName = newClip;
                clipTime = 0f;
            }
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            globalTime += delta;
            clipTime += delta;

            float sw = getStageWidth();
            float sh = getStageHeight();

            if (cooldownTimer > 0) {
                cooldownTimer -= delta;
            }

            switch (state) {
                case TURNING -> {
                    vx *= 0.88f;
                    vy *= 0.88f;
                    setPosition(getX() + vx * delta, getY() + vy * delta);

                    turnTimer -= delta;
                    if (turnTimer <= 0) {
                        facingLeft = nextFacingLeft;
                        state = stateAfterTurn;
                        if (state == State.AT_PLANT) {
                            changeClip("action3");
                            plantActionTimer = BEE_PLANT_WORK_TIME;
                        } else {
                            changeClip("idle");
                        }
                    }
                }

                case AT_PLANT -> {
                    vx = 0f;
                    vy = 0f;
                    plantActionTimer -= delta;
                    if (plantActionTimer <= 0) {
                        cooldownTimer = BEE_WORK_COOLDOWN;
                        pickRandomTarget();
                    }
                }

                case MOVING_TO_PLANT, WANDERING -> {
                    if (state == State.WANDERING && cooldownTimer <= 0) {
                        Vector2 readyPlantTarget = getRandomReadyPlantPosition();
                        if (readyPlantTarget != null) {
                            setMovementTarget(readyPlantTarget.x, readyPlantTarget.y, State.MOVING_TO_PLANT);
                            break;
                        }
                    }

                    float dx = targetX - getX();
                    float dy = targetY - getY();
                    float dist = (float) Math.hypot(dx, dy);

                    if (dist < 15f) {
                        if (state == State.MOVING_TO_PLANT) {
                            state = State.AT_PLANT;
                            changeClip("action3");
                            plantActionTimer = BEE_PLANT_WORK_TIME;
                        } else {
                            pickRandomTarget();
                        }
                    } else {
                        float targetVx = (dx / dist) * maxSpeed;
                        float targetVy = (dy / dist) * maxSpeed;

                        vx += (targetVx - vx) * Math.min(1f, delta * 5f);
                        vy += (targetVy - vy) * Math.min(1f, delta * 5f);

                        float floatBobbing = (float) Math.sin(globalTime * 6.5f) * 12f * delta;

                        setPosition(getX() + vx * delta, getY() + vy * delta + floatBobbing);

                        setX(Math.max(40f, Math.min(sw - 40f, getX())));
                        setY(Math.max(60f, Math.min(sh - 60f, getY())));
                    }

                    if (state == State.WANDERING) {
                        if (actionTimer > 0) {
                            actionTimer -= delta;
                            if (actionTimer <= 0) {
                                changeClip("idle");
                            }
                        } else {
                            wanderActionInterval -= delta;
                            if (wanderActionInterval <= 0) {
                                triggerRandomWanderAction();
                                wanderActionInterval = 3f + (float) Math.random() * 3f;
                            }
                        }
                    }
                }
            }
        }

        private void triggerRandomWanderAction() {
            if (state == State.WANDERING && !"turn".equals(currentClipName)) {
                String[] actions = {"action2", "idle2", "actionCritical"};
                String chosen = actions[(int) (Math.random() * actions.length)];
                changeClip(chosen);

                if ("actionCritical".equals(chosen)) {
                    actionTimer = 1.77f;
                } else if ("action2".equals(chosen)) {
                    actionTimer = 2.20f;
                } else {
                    actionTimer = 1.2f;
                }
            }
        }

        private void setMovementTarget(float tx, float ty, State nextState) {
            float sw = getStageWidth();
            float sh = getStageHeight();

            this.targetX = Math.max(40f, Math.min(sw - 40f, tx));
            this.targetY = Math.max(60f, Math.min(sh - 60f, ty));

            boolean targetIsLeft = (this.targetX < getX());

            if (Math.abs(this.targetX - getX()) > 30f && targetIsLeft != facingLeft) {
                nextFacingLeft = targetIsLeft;
                state = State.TURNING;
                stateAfterTurn = nextState;
                changeClip("turn");
                turnTimer = 0.45f;
            } else {
                state = nextState;
                if (state == State.WANDERING && actionTimer <= 0) {
                    changeClip("idle");
                }
            }
        }

        private void pickRandomTarget() {
            float sw = getStageWidth();
            float sh = getStageHeight();

            float rx = 60f + (float) Math.random() * (sw - 120f);
            float ry = 90f + (float) Math.random() * (sh - 180f);

            setMovementTarget(rx, ry, State.WANDERING);
        }

        private Vector2 getRandomReadyPlantPosition() {
            if (rootTable != null) {
                rootTable.validate();
            }
            List<Vector2> readyPositions = new ArrayList<>();
            for (Map.Entry<Pot, Table> entry : potCellMap.entrySet()) {
                Pot pot = entry.getKey();
                if (statusOf(pot) == PotStatus.READY) {
                    Table cell = entry.getValue();
                    cell.validate();
                    Vector2 stagePos = cell.localToStageCoordinates(new Vector2(POT_SIZE / 2f, POT_SIZE + 80f));
                    readyPositions.add(stagePos);
                }
            }
            if (readyPositions.isEmpty()) return null;
            int idx = (int) (Math.random() * readyPositions.size());
            return readyPositions.get(idx);
        }

        private ClipRef getBeeClipSafely(String preferredClip) {
            if (pamPlayer == null) return null;

            try {
                ClipRef clip = pamPlayer.getClip(BEE_PAM_PATH, preferredClip);
                if (clip != null) return clip;
            } catch (Throwable ignored) {}

            try {
                String resolved = AnimationFactory.resolveClipNameForPath(BEE_PAM_PATH, preferredClip);
                if (resolved != null) {
                    ClipRef clip = pamPlayer.getClip(BEE_PAM_PATH, resolved);
                    if (clip != null) return clip;
                }
            } catch (Throwable ignored) {}

            String[] fallbacks = {"anim_idle", "anim_fly", "idle", "action3", "turn", "Action3"};
            for (String fallback : fallbacks) {
                try {
                    ClipRef clip = pamPlayer.getClip(BEE_PAM_PATH, fallback);
                    if (clip != null) return clip;
                } catch (Throwable ignored) {}
            }

            return null;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (pamPlayer == null) return;

            try {
                ClipRef clip = getBeeClipSafely(currentClipName);
                if (clip == null) return;

                float px = getX();
                float py = getY();

                float scaleX = facingLeft ? BEE_SCALE : -BEE_SCALE;
                float scaleY = BEE_SCALE;

                Matrix4 original = batch.getTransformMatrix().cpy();
                Matrix4 scaled = new Matrix4(original)
                        .translate(px, py, 0)
                        .scale(scaleX, scaleY, 1f)
                        .translate(-px, -py, 0);

                batch.setTransformMatrix(scaled);

                pamPlayer.draw(batch, clip, clipTime, px, py, true);

                batch.setTransformMatrix(original);
            } catch (Throwable t) {
                Gdx.app.error("BeeActor", "Error rendering bee", t);
            }
        }
    }
}