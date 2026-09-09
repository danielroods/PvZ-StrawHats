package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import controller.CollectionManager;
import controller.ScreenManager;
import controller.match.mini_games.MiniGameEndMenu;
import model.App;
import model.collections.animations.AnimationFactory;
import model.collections.plant.PlantJsonParser;
import model.user_data.User;
import model.user_data.UserState;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.UiScreen;

import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * End-of-mini-game screen. {@link MiniGameEndMenu} still owns the win/loss result and any
 * restart action; this class is presentation only.
 * <p>
 * On a loss, behaviour is unchanged from before this feature existed:
 * {@link #buildResultPanel()} is shown immediately.
 * <p>
 * On a win, a bonus reward pinata plays first ({@link #buildPinataStage()}) - exactly the
 * same mechanism used at the end of a normal match ({@code view.screens.match.after.
 * AfterMatchScreen}): it fades/scales in idle, the player clicks it to explode it, then taps
 * the resulting pile four times - once per {@link PinataReward} - each tap playing the
 * "tap_pile" clip once and popping a reward (two seed packets with a random 1-5 seed count
 * each, a coin stack worth 100/200/500/1000, and a diamond reward worth 1/2/5). Once all four
 * are collected the pinata fades out and an "outcome scroll" summarises everything collected;
 * only after the player continues past that scroll does the normal {@link #buildResultPanel()}
 * (title / game name / details / Play Again-Back buttons) appear, exactly as it did before
 * this feature existed. Each mini game uses its own pinata skin (see {@link #resolvePinataPamPath()}).
 */
public class MiniGameEndScreen extends UiScreen {

    private static final String BACKGROUND = "assets/images/backg/mainmenu_background.png";

    
    private static final String PINATA_VASEBREAKER = "768/FULL/EFFECTS/PRIZE_PINATA_GREENS/PRIZE_PINATA_GREENS.PAM";
    private static final String PINATA_WALLNUT_BOWLING = "768/FULL/EFFECTS/PRIZE_PINATA_COWBOY/PRIZE_PINATA_COWBOY.PAM";
    private static final String PINATA_IZOMBIE = "768/FULL/EFFECTS/PRIZE_PINATA_ICE/PRIZE_PINATA_ICE.PAM";
    private static final String PINATA_BEGHOULED = "768/FULL/EFFECTS/PRIZE_PINATA_FRUIT/PRIZE_PINATA_FRUIT.PAM";
    private static final String PINATA_ZOMBOTANY = "768/FULL/EFFECTS/PRIZE_PINATA_MUSHROOMS/PRIZE_PINATA_MUSHROOMS.PAM";
    
    private static final String PINATA_DEFAULT = PINATA_ZOMBOTANY;

    private static final String COIN_DIAMOND_PAM = "768/INITIAL/EFFECTS/COIN_DIAMOND/COIN_DIAMOND.PAM";
    private static final String COIN_STACK_PAM = "768/INITIAL/EFFECTS/COIN_STACK/COIN_STACK.PAM";

    private static final int[] COIN_REWARD_VALUES = {100, 200, 500, 1000};
    private static final int[] DIAMOND_REWARD_VALUES = {1, 2, 5};
    private static final int REWARD_COUNT = 4;

    private static final float PINATA_INTRO_DURATION = 0.45f;
    private static final float PINATA_EXIT_DURATION = 0.35f;
    
    private static final float PINATA_ANCHOR_HEIGHT_RATIO = 0.35f;
    
    
    
    private static final float DEFAULT_EXPLODE_DURATION = 1.0f;
    private static final float DEFAULT_TAP_DURATION = 0.55f;

    private final Random random = new Random();
    private final SeedPacketCardFactory cardFactory = new SeedPacketCardFactory();
    private final CollectionManager collectionManager = new CollectionManager();

    private TextureBank textureBank;
    private PamPlayer pamPlayer;

    private MiniGameEndMenu menu;
    private boolean wonMatch = false;
    private boolean pinataSequenceStarted = false;

    private PinataActor pinataActor;
    private Label pinataHintLabel;
    private final List<PinataReward> pinataRewardQueue = new ArrayList<>();
    private final List<PinataReward> collectedRewards = new ArrayList<>();

    @Override
    public void show() {
        if (textureBank == null) {
            try {
                FileHandle rootHandle = Gdx.files.internal("assets/pvz-assets");
                textureBank = new TextureBank("atlases", rootHandle);
                pamPlayer = new PamPlayer(textureBank, rootHandle);
            } catch (Throwable t) {
                Gdx.app.error("MiniGameEndScreen", "Failed to initialize PAM system", t);
            }
        }
        setBackground(BACKGROUND);
        AudioManager.get().playMusic(AudioEnum.MINI_GAME_MUSIC, true);
        super.show();
        build();
    }

    @Override
    public void render(float delta) {
        if (textureBank != null) {
            try {
                textureBank.update();
            } catch (Throwable t) {
                Gdx.app.error("MiniGameEndScreen", "textureBank.update() failed", t);
            }
        }
        super.render(delta);
    }

    private void build() {
        rootTable.clear();

        menu = App.currentMenu instanceof MiniGameEndMenu m ? m : null;
        wonMatch = menu != null && menu.isWon();
        AudioManager.get().playSound(wonMatch ? AudioEnum.SFX_MATCH_WIN : AudioEnum.SFX_MATCH_LOSE);

        if (wonMatch && !pinataSequenceStarted) {
            
            
            
            pinataSequenceStarted = true;
            buildPinataStage();
        } else {
            buildResultPanel();
        }
    }

    
    
    

    private void buildPinataStage() {
        rootTable.clear();

        pinataRewardQueue.clear();
        pinataRewardQueue.addAll(generatePinataRewards());
        collectedRewards.clear();

        Table layer = new Table();

        pinataActor = new PinataActor(resolvePinataPamPath());
        Table pinataCell = new Table();
        
        
        
        
        pinataCell.add(pinataActor).size(360f * 0.7f, 420f * 0.7f);
        layer.add(pinataCell).padBottom(SPACE_SM + 85f).row();

        pinataHintLabel = new Label("Tap the pinata!", skin, "main");
        pinataHintLabel.setAlignment(Align.center);
        layer.add(pinataHintLabel);

        rootTable.add(layer).expand().center();
    }

    
    private List<PinataReward> generatePinataRewards() {
        List<PinataReward> rewards = new ArrayList<>();

        UserState state = User.currentUser != null ? User.currentUser.userState : null;
        List<PlantJsonParser.PlantConfig> pool = new ArrayList<>(collectionManager.getUnlockedPlants(state));
        Collections.shuffle(pool, random);

        for (int i = 0; i < 2; i++) {
            PinataReward reward = new PinataReward();
            reward.kind = RewardKind.SEED_PACKET;
            if (!pool.isEmpty()) {
                
                
                PlantJsonParser.PlantConfig config = pool.get(i % pool.size());
                reward.plantId = config.id;
                reward.plantName = config.name;
            } else {
                reward.plantId = -1;
                reward.plantName = "Peashooter";
            }
            reward.amount = 1 + random.nextInt(5); 
            rewards.add(reward);
        }

        PinataReward coin = new PinataReward();
        coin.kind = RewardKind.COIN;
        coin.amount = COIN_REWARD_VALUES[random.nextInt(COIN_REWARD_VALUES.length)];
        rewards.add(coin);

        PinataReward diamond = new PinataReward();
        diamond.kind = RewardKind.DIAMOND;
        diamond.amount = DIAMOND_REWARD_VALUES[random.nextInt(DIAMOND_REWARD_VALUES.length)];
        rewards.add(diamond);

        Collections.shuffle(rewards, random);
        return rewards;
    }

    private void onPinataExploded() {
        if (pinataHintLabel != null) {
            pinataHintLabel.setText("Tap the pile for your rewards! (0/" + REWARD_COUNT + ")");
        }
    }

    
    private void onPinataTapResolved() {
        if (pinataRewardQueue.isEmpty()) return;

        PinataReward reward = pinataRewardQueue.remove(0);
        applyReward(reward);
        collectedRewards.add(reward);
        showRewardPopup(reward);

        if (pinataHintLabel != null) {
            pinataHintLabel.setText(pinataRewardQueue.isEmpty()
                    ? "Nice haul!"
                    : "Tap the pile for your rewards! (" + collectedRewards.size() + "/" + REWARD_COUNT + ")");
        }

        if (pinataRewardQueue.isEmpty()) {
            User.save();
            if (pinataActor != null) {
                pinataActor.tapsEnabled = false;
                pinataActor.beginExit();
            }
        }
    }

    
    private void onPinataFullyGone() {
        showOutcomeScroll();
    }

    private void applyReward(PinataReward reward) {
        UserState state = User.currentUser != null ? User.currentUser.userState : null;
        if (state == null) return;

        switch (reward.kind) {
            case SEED_PACKET -> {
                if (reward.plantId >= 0) {
                    state.addSeedPackets(reward.plantId, reward.amount);
                }
            }
            case COIN -> state.coins += reward.amount;
            case DIAMOND -> state.diamonds += reward.amount;
        }
    }

    
    private void showRewardPopup(PinataReward reward) {
        if (pinataActor == null) return;

        Vector2 center = pinataActor.localToStageCoordinates(
                new Vector2(pinataActor.getWidth() / 2f, pinataActor.getHeight() / 2f + 50f));

        Table popup = new Table();
        popup.add(buildRewardIcon(reward, 0.50f)).size(64f * 1.38f * 3f, 64f * 3f).row();
        Label label = new Label(rewardLabel(reward), skin, "title");
        label.setFontScale(0.55f);
        popup.add(label).padTop(2f);
        popup.pack();
        popup.setPosition(center.x - popup.getWidth() / 2f, center.y - 200f);
        popup.getColor().a = 0f;

        
        
        stage.addActor(popup);
        popup.addAction(Actions.sequence(
                Actions.parallel(Actions.fadeIn(0.2f), Actions.moveBy(0f, 30f, 0.9f)),
                Actions.delay(0.4f),
                Actions.fadeOut(0.35f),
                Actions.removeActor()
        ));
    }

    /** Seed packets render via the real {@link SeedPacketCard} class, at a small scale; coins
     *  and diamonds render as their own looping "idle" PAM clip. */
    private Actor buildRewardIcon(PinataReward reward, float seedPacketScale) {
        if (reward.kind == RewardKind.SEED_PACKET) {
            SeedPacketCard card = cardFactory.buildCardForDisplayName(reward.plantName);
            if (card != null) {
                card.setTransform(true);
                card.setScale(seedPacketScale);

                float scale = 3f;
                Container<SeedPacketCard> container = new Container<>(card);
                container.size(card.getWidth() * seedPacketScale * scale, card.getHeight() * seedPacketScale * scale);
                return container;
            }
            return new Image(solidColorDrawable(new Color(0f, 0f, 0f, 0f)));
        }
        String pamPath = reward.kind == RewardKind.COIN ? COIN_STACK_PAM : COIN_DIAMOND_PAM;
        return new PamIconActor(pamPath, "idle");
    }

    private String rewardLabel(PinataReward reward) {
        return switch (reward.kind) {
            case SEED_PACKET -> "+" + reward.amount + " " + reward.plantName + " Seeds";
            case COIN -> "+" + reward.amount + " Coins";
            case DIAMOND -> "+" + reward.amount + " Diamonds";
        };
    }

    /** Rewards summary modal shown once the pinata is gone; "Continue" reveals the normal
     *  win/lose result panel, unchanged from before this feature existed. */
    private void showOutcomeScroll() {
        Table overlay = new Table();
        overlay.setFillParent(true);

        Image dim = new Image(solidColorDrawable(new Color(0f, 0f, 0f, 0.7f)));
        dim.setFillParent(true);

        Stack overlayStack = new Stack();
        overlayStack.add(dim);

        Table scroll = new Table();
        scroll.setBackground(skin.getDrawable("modal-background"));
        scroll.pad(20);

        Label title = new Label("PINATA REWARDS!", skin, "title");
        title.setColor(Color.GOLD);
        scroll.add(title).padBottom(12).row();

        Table rewardsRow = new Table();

        for (PinataReward reward : collectedRewards) {
            Table itemCell = new Table();

            Actor icon = buildRewardIcon(reward, 0.23f);

            itemCell.add(icon).size(280f, 150f).padBottom(6f).row();

            Label rowLabel = new Label(rewardLabel(reward), skin, "main");
            rowLabel.setFontScale(0.7f);
            rowLabel.setAlignment(Align.center);
            itemCell.add(rowLabel).center();

            rewardsRow.add(itemCell).top().pad(0f, 10f, 0f, 10f);
        }

        scroll.add(rewardsRow).center().padBottom(14f).row();

        TextButton continueBtn = primaryButton("Continue", () -> {
            overlay.remove();
            buildResultPanel();
        });
        scroll.add(continueBtn).width(190f).height(46f).padTop(8f);

        Container<Table> scrollContainer = new Container<>(scroll);
        scrollContainer.center();
        overlayStack.add(scrollContainer);

        overlay.add(overlayStack).expand().fill();
        overlay.getColor().a = 0f;

        getModalStack().add(overlay);
        overlay.addAction(Actions.fadeIn(0.3f));
    }

    /** Picks the pinata skin for the mini game that just ended, keyed off {@link
     *  MiniGameEndMenu#getGameName()}. Every mini game that can reach this screen gets its
     *  own skin; anything unrecognised falls back to {@link #PINATA_DEFAULT}. */
    private String resolvePinataPamPath() {
        String gameName = menu == null ? null : menu.getGameName();
        if (gameName == null) return PINATA_DEFAULT;

        String normalized = gameName.toLowerCase();
        if (normalized.contains("vasebreaker")) return PINATA_VASEBREAKER;
        if (normalized.contains("wall-nut bowling") || normalized.contains("wallnut bowling")) {
            return PINATA_WALLNUT_BOWLING;
        }
        if (normalized.contains("zombie")) return PINATA_IZOMBIE; 
        if (normalized.contains("beghouled")) return PINATA_BEGHOULED;
        if (normalized.contains("zombotany")) return PINATA_ZOMBOTANY;
        return PINATA_DEFAULT;
    }

    
    
    
    

    private void buildResultPanel() {
        rootTable.clear();

        MiniGameEndMenu currentMenu = menu != null ? menu : (App.currentMenu instanceof MiniGameEndMenu m ? m : null);
        boolean won = currentMenu != null && currentMenu.isWon();

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("modal-background"));
        panel.pad(34);

        Label title = new Label(won ? "YOU WIN!" : "GAME OVER", skin, "title");
        title.setColor(won ? Color.GREEN : Color.RED);
        panel.add(title).center().padBottom(10).row();

        if (currentMenu != null) {
            panel.add(new Label(currentMenu.getGameName(), skin, "main")).center().padBottom(16).row();
        }

        String details = currentMenu == null ? "" : currentMenu.getDetails();
        if (!details.isBlank()) {
            Label summary = new Label(details, skin, "main");
            summary.setWrap(true);
            summary.setAlignment(1);
            panel.add(summary).width(560).padBottom(22).row();
        }

        Table buttons = new Table();

        if (currentMenu != null && currentMenu.canRestart()) {
            TextButton playAgain = new TextButton("Play Again", skin);
            playAgain.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    currentMenu.restart();
                    ScreenManager.syncWithCurrentMenu();
                }
            });
            buttons.add(playAgain).width(200).height(54).pad(6);
        }

        TextButton exit = new TextButton("Back to Travel Log", skin);
        exit.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                runCommand("menu exit");
            }
        });
        buttons.add(exit).width(240).height(54).pad(6);

        panel.add(buttons);

        
        panel.setTransform(true);
        panel.setOrigin(Align.center);
        panel.getColor().a = 0f;
        panel.setScale(0.9f);
        panel.addAction(Actions.parallel(
                Actions.fadeIn(0.25f),
                Actions.scaleTo(1f, 1f, 0.25f, Interpolation.swingOut)));

        rootTable.add(panel).center();
    }

    private Drawable solidColorDrawable(Color color) {
        Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    @Override
    public void dispose() {
        cardFactory.dispose();
        super.dispose();
    }

    
    
    

    private enum RewardKind { SEED_PACKET, COIN, DIAMOND }

    private static final class PinataReward {
        RewardKind kind;
        int plantId;
        String plantName;
        int amount;
    }

    
    
    
    

    private enum PinataAnim { IDLE, EXPLODE, IDLE_PILE, TAP_PILE }

    private class PinataActor extends Actor {
        private final String pamPath;
        private PinataAnim state = PinataAnim.IDLE;
        private float stateTime = 0f;
        private float introTime = 0f;
        private float exitTime = -1f;
        private boolean exitReported = false;
        boolean tapsEnabled = true;

        PinataActor(String pamPath) {
            this.pamPath = pamPath;
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    handleClick();
                }
            });
        }

        private void handleClick() {
            if (exitTime >= 0f) return; 

            if (state == PinataAnim.IDLE) {
                state = PinataAnim.EXPLODE;
                stateTime = 0f;
                AudioManager.get().playSound(AudioEnum.SFX_CLICK);
                onPinataExploded();
            } else if (state == PinataAnim.IDLE_PILE && tapsEnabled) {
                state = PinataAnim.TAP_PILE;
                stateTime = 0f;
                AudioManager.get().playSound(AudioEnum.SFX_CLICK);
            }
            
            
        }

        void beginExit() {
            if (exitTime < 0f) {
                exitTime = 0f;
            }
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += delta;
            introTime = Math.min(PINATA_INTRO_DURATION, introTime + delta);

            if (state == PinataAnim.EXPLODE && stateTime >= explodeDuration()) {
                state = PinataAnim.IDLE_PILE;
                stateTime = 0f;
            } else if (state == PinataAnim.TAP_PILE && stateTime >= tapDuration()) {
                state = PinataAnim.IDLE_PILE;
                stateTime = 0f;
                onPinataTapResolved();
            }

            if (exitTime >= 0f) {
                exitTime += delta;
                if (exitTime >= PINATA_EXIT_DURATION && !exitReported) {
                    exitReported = true;
                    onPinataFullyGone();
                }
            }
        }

        private float explodeDuration() {
            float d = AnimationFactory.clipDurationForPath(pamPath, "explode");
            return d > 0f ? d : DEFAULT_EXPLODE_DURATION;
        }

        private float tapDuration() {
            float d = AnimationFactory.clipDurationForPath(pamPath, "tap_pile");
            return d > 0f ? d : DEFAULT_TAP_DURATION;
        }

        private String clipNameFor(PinataAnim anim) {
            return switch (anim) {
                case IDLE -> "idle";
                case EXPLODE -> "explode";
                case IDLE_PILE -> "idle_pile";
                case TAP_PILE -> "tap_pile";
            };
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (pamPlayer == null || pamPath == null) return;
            try {
                String preferred = clipNameFor(state);
                String resolved = AnimationFactory.resolveClipNameForPath(pamPath, preferred);
                String clipName = resolved != null ? resolved : preferred;

                ClipRef clip = pamPlayer.getClip(pamPath, clipName);
                if (clip == null) return;

                float introProgress = Math.min(1f, introTime / PINATA_INTRO_DURATION);
                float exitProgress = exitTime < 0f ? 0f : Math.min(1f, exitTime / PINATA_EXIT_DURATION);
                float alpha = (0.4f + 0.6f * introProgress) * (1f - exitProgress) * parentAlpha;
                if (alpha <= 0f) return;
                
                float visualScale = (0.6f + 0.4f * introProgress) * (1f - 0.25f * exitProgress) * 0.8f;

                
                
                
                
                
                
                
                
                
                float anchorX = getX() + getWidth() / 2f;
                float anchorY = getY() + getHeight() * PINATA_ANCHOR_HEIGHT_RATIO;

                Matrix4 original = batch.getTransformMatrix().cpy();
                Matrix4 scaled = new Matrix4(original)
                        .translate(anchorX, anchorY, 0f)
                        .scale(visualScale, visualScale, 1f)
                        .translate(-anchorX, -anchorY, 0f);
                batch.setTransformMatrix(scaled);

                Color prevColor = batch.getColor().cpy();
                batch.setColor(1f, 1f, 1f, alpha);
                pamPlayer.draw(batch, clip, stateTime, anchorX, anchorY, false);
                batch.setColor(prevColor);

                batch.setTransformMatrix(original);
            } catch (Throwable t) {
                Gdx.app.error("MiniGameEndScreen", "Failed to draw pinata (" + pamPath + ")", t);
            }
        }
    }

    
    private class PamIconActor extends Actor {
        private final String pamPath;
        private final String clipName;
        private float stateTime = 0f;

        PamIconActor(String pamPath, String clipName) {
            this.pamPath = pamPath;
            this.clipName = clipName;
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (pamPlayer == null || pamPath == null) return;
            try {
                String resolved = AnimationFactory.resolveClipNameForPath(pamPath, clipName);
                String finalClip = resolved != null ? resolved : clipName;

                ClipRef clip = pamPlayer.getClip(pamPath, finalClip);
                if (clip == null) return;

                float centerX = getX() + getWidth() / 2f;
                float centerY = getY() + getHeight() / 2f;
                float visualScale = 0.5f;

                Matrix4 original = batch.getTransformMatrix().cpy();
                Matrix4 scaled = new Matrix4(original)
                        .translate(centerX, centerY, 0f)
                        .scale(visualScale, visualScale, 1f)
                        .translate(-centerX, -centerY, 0f);
                batch.setTransformMatrix(scaled);

                pamPlayer.draw(batch, clip, stateTime, centerX, centerY, false);

                batch.setTransformMatrix(original);
            } catch (Throwable t) {
                Gdx.app.error("MiniGameEndScreen", "Failed to draw pam icon (" + pamPath + ")", t);
            }
        }
    }
}