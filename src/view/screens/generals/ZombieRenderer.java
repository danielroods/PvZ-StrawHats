package view.screens.generals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

import controller.assets.GameAssetManager;
import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.animations.ZombieAshAnimationRegistry;
import model.collections.animations.ZombieShockAnimationRegistry;
import model.collections.armour.ZombieArmour;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieState;
import model.collections.zombie.zombie_effect.RotationalTurbulenceState;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.collections.zombie.zombie_attack.SmashAttack;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.match.main.season.travellog.egypt.SandStorm;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.TileType;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.match.gameplay.mini_games.ZombotanyArt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

class ZombieRenderer {

    private static final String ZOMBIE_SPAWN_EFFECT_PAM = "768/INITIAL/EFFECTS/ZOMBIE_EGYPT_TOMBRAISER_BONE_HIT/ZOMBIE_EGYPT_TOMBRAISER_BONE_HIT.PAM";
    private static final float ZOMBIE_SPAWN_EFFECT_DURATION = 1.33f;
    private static final float DEATH_ANIM_DURATION = 1.0f;
    private static final float HYPNO_OVERLAY_SCALE = 0.55f;
    private static final float ZOMBIE_SCALE = 0.52f;
    private static final float HEAD_BOB = 3.5f;
    private static final String HYPNO_ZOMBIE_EFFECT_PAM =
            "768/INITIAL/EFFECTS/HYPNO_ZOMBIE_EFFECT/HYPNO_ZOMBIE_EFFECT.PAM";

    private static final String ZOMBIE_BEACH_FISHERMAN_ALIAS = "ZombieBeachFisherman";
    private static final String ZOMBIE_PIANO_ALIAS = "ZombiePiano";
    // Standalone prop PAM (idle/play/damage/die), drawn in front of the pianist's own body.
    private static final String PIANO_PROP_PAM = "768/FULL/ZOMBIE/PIANO/PIANO.PAM";
    private static final float DEFAULT_PIANO_DAMAGE_DURATION = 0.4f;
    // Piano sits just in front of (below/ahead of) the pianist's own body origin.
    private static final float PIANO_OFFSET_X = 46f;
    private static final float PIANO_OFFSET_Y = -6f;
    private static final float PIANO_SCALE = 0.52f;
    private static final String ZOMBIE_ARCADE_ALIAS = "ZombieArcade";
    private static final String ZOMBIE_BARREL_ROLLER_ALIAS = "ZombieBarrelRoller";
    private static final String ZOMBIE_BARREL_PAM =
            "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER_BARREL/ZOMBIE_PIRATE_BARREL_PUSHER_BARREL.PAM";
    private static final float BARREL_SCALE = 0.52f;
    private static final float BARREL_OFFSET_X = -18f;
    private static final float BARREL_OFFSET_Y = 30f;
    private static final float DEFAULT_BARREL_DEATH_DURATION = 0.6f;
    private static final String ZOMBIE_TROGLOBITE_ALIAS = "ZombieIceAgeTroglobite";
    // The pushed ice block reuses the same texture the Troglobite was frozen inside
    // (see FrostbiteRenderer.drawFrostbiteIceBlocks / GameScreenAssets.zombieIceBlockTexture)
    // so it visibly reads as "the same ice, now falling and being pushed".
    private static final float ICE_BLOCK_PUSH_SCALE = 1.8f;
    private static final float ICE_BLOCK_FALL_HEIGHT_TILES = 5.5f;
    // Nudges the pushed ice block up and further left within its cell, matching the
    // height the Troglobite's own body sits at (see FrostbiteRenderer.ICE_BLOCK_OFFSET_Y,
    // which is used for the same block while it's still the stationary frozen obstacle).
    private static final float ICE_BLOCK_OFFSET_X = -95f;
    private static final float ICE_BLOCK_OFFSET_Y = 30f;
    // The frozen imp glimpsed inside the ice block, drawn at the same real scale as any
    // other on-field zombie (ZOMBIE_SCALE) and scissor-clipped to the block's own drawn
    // rectangle so it always reads as fully inside the ice, never spilling outside it.
    private static final String ICE_BLOCK_IMP_PAM = "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_IMP/ZOMBIE_ICEAGE_IMP.PAM";
    private static final String ZOMBIE_MODERN_ALLSTAR_ALIAS = "ZombieModernAllStar";
    private static final String ZOMBIE_NEWSPAPER_ALIAS = "ZombieNewspaper";
    // Standalone prop PAM (idle/active/death), drawn at the pushed structure's own position.
    private static final String ARCADE_PROP_PAM = "768/FULL/EFFECTS/80S_ARCADE_CABINET/80S_ARCADE_CABINET.PAM";
    private static final float DEFAULT_ARCADE_DEATH_DURATION = 0.6f;
    private static final float ARCADE_SCALE = 0.6f;
    // Named element inside every zombie's own PAM for the butter-stun face
    // overlay; toggled via the element visibility mask, same as armor pieces.
    private static final String BUTTER_ELEMENT_NAME = "butter";
    private static final float WATER_RIPPLE_SCALE = 0.70f;
    private static final float DEFAULT_RIPPLE_EXIT_DURATION = 0.6f;
    private static final String WATER_GARGANTUAR_RIPPLE_PAM =
            "768/FULL/BACKGROUNDS/WATER_GARGANTUAR_RIPPLE/WATER_GARGANTUAR_RIPPLE.PAM";
    private static final String WATER_IMP_RIPPLE_PAM =
            "768/FULL/BACKGROUNDS/WATER_IMP_RIPPLE/WATER_IMP_RIPPLE.PAM";
    private static final String WATER_ZOMBIE_RIPPLE_PAM =
            "768/FULL/BACKGROUNDS/WATER_ZOMBIE_RIPPLE/WATER_ZOMBIE_RIPPLE.PAM";

    private static final float RIPPLE_OFFSET_X = -50f;
    private static final float RIPPLE_OFFSET_Y = -18f;

    // Swimmer (snorkel) zombie sits noticeably deeper than other beach zombies while
    // submerged - only its head should poke out above the water clip line. The ripple
    // itself is drawn from the zombie's own board position (see rippleDrawX/rippleDrawY
    // below), which is untouched by this offset, so it stays in the same place as any
    // other zombie's ripple.
    private static final String ZOMBIE_BEACH_SNORKEL_ALIAS = "ZombieBeachSnorkel";
    private static final float SWIMMER_SUBMERGE_OFFSET_Y = 58f;
    private static final float DEFAULT_SUBMERGE_OFFSET_Y = 20f;

    // Tints a zombie's own PAM draw blue while it's chilled (Status.FREEZE), applied via
    // SpriteBatch color multiplication so the tint rides along with the actual animation
    // frames (transparent pixels stay transparent) instead of drawing a flat colored shape
    // over it.
    private static final Color FREEZE_TINT = new Color(0.55f, 0.78f, 1f, 1f);
    private static final Color FROZEN_TINT = new Color(0.42f, 0.68f, 1f, 1f);
    private static final Color FREEZE_FALLBACK_TINT = new Color(0.35f, 0.55f, 0.85f, 1f);

    // Same idea as FREEZE_TINT, but for a hypnotized zombie's body - a purple/pink wash
    // via SpriteBatch color multiplication.
    private static final Color HYPNO_TINT = new Color(0.85f, 0.55f, 1f, 1f);
    private static final Color HYPNO_FALLBACK_TINT = new Color(0.65f, 0.4f, 0.85f, 1f);

    // Rising hypnosis bubbles drawn over a hypnotized zombie's body, purple/pink and
    // semi-transparent - several of them, staggered so they don't all pop in step,
    // climbing from the zombie's feet to over its head, then fading out and looping.
    private static final Color HYPNO_BUBBLE_COLOR = new Color(0.87f, 0.45f, 0.95f, 1f);
    private static final int HYPNO_BUBBLE_COUNT = 6;
    private static final float HYPNO_BUBBLE_CYCLE_SECONDS = 1.8f;
    private static final float HYPNO_BUBBLE_MIN_SIZE = 8f;
    private static final float HYPNO_BUBBLE_MAX_SIZE = 20f;
    private static final float HYPNO_BUBBLE_SWAY = 10f;

    private static final class ZombieWaterRipple {
        boolean inWater;
        float rippleLoopTime;
        boolean exiting;
        float exitElapsed;
    }

    private static final class DyingZombie {
        final String alias;
        final Position position;
        final boolean facingRight;
        final float duration;
        // Shock always plays first when the zombie was killed by Electric Blueberry.
        final String shockPath;
        final float shockDuration;
        // After shock, ash plays when this zombie has an ash-death asset.
        final String ashPath;
        final float ashDuration;
        final boolean barrelBroken;
        // The barrel survives independently of the pusher zombie.  Keep the actual
        // structure reference so it can continue rolling after the zombie's death
        // sequence has finished.
        final PushableStructure barrelStructure;
        float time;
        float barrelRollTime;
        float barrelDeathTime;

        DyingZombie(String alias, Position position, boolean facingRight,
                    float duration, String shockPath, float shockDuration,
                    String ashPath, float ashDuration, boolean barrelBroken,
                    PushableStructure barrelStructure) {
            this.alias = alias;
            this.position = position;
            this.facingRight = facingRight;
            this.duration = duration;
            this.shockPath = shockPath;
            this.shockDuration = shockDuration;
            this.ashPath = ashPath;
            this.ashDuration = ashDuration;
            this.barrelBroken = barrelBroken;
            this.barrelStructure = barrelStructure;
            this.barrelRollTime = 0f;
            this.barrelDeathTime = -1f;
        }
    }

    private final GameScreen screen;
    private final ZombieArmorMask armorMask;

    private final Map<Zombie, Float> zombieSpawnEffects = new IdentityHashMap<>();
    private final Map<Zombie, Float> zombieAnimTimes = new IdentityHashMap<>();
    private final Map<Zombie, ZombieWaterRipple> zombieWaterRipples = new IdentityHashMap<>();
    private final Map<Zombie, Boolean> zombieGyratingLast = new IdentityHashMap<>();
    // Edge-detection for one-shot SFX (same idiom as zombieGyratingLast above):
    // fires SFX_ZOMBIE_EAT the frame a zombie starts biting, SFX_HYPNOTIZE the
    // frame Hypno-shroom flips a zombie to the player's side.
    private final Map<Zombie, Boolean> zombieEatingLast = new IdentityHashMap<>();
    private final Map<Zombie, Boolean> zombieHypnotizedLast = new IdentityHashMap<>();
    private final Map<Zombie, String> zombieActionStateLast = new IdentityHashMap<>();
    private final Map<Zombie, Integer> zombieLastHp = new IdentityHashMap<>();
    private final Map<Zombie, Float> pianoDamageAnimTimes = new IdentityHashMap<>();
    private final Map<Zombie, Float> arcadeDeathAnimTimes = new IdentityHashMap<>();
    private final Map<Zombie, ZombieWaterRipple> arcadeWaterRipples = new IdentityHashMap<>();
    private final Map<Zombie, Float> barrelDeathAnimTimes = new IdentityHashMap<>();
    private final List<DyingZombie> dyingZombies = new ArrayList<>();

    ZombieRenderer(GameScreen screen) {
        this.screen = screen;
        this.armorMask = new ZombieArmorMask();
    }

    void drawZombies(float delta, float bw, float bh) {
        if (screen.isBeforeMatchPreview()) return;

        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        List<Zombie> zombies = new ArrayList<>(screen.session.getZombies());
        zombies.sort(Comparator.comparingDouble(z -> z.getPosition() == null ? 0 : z.getPosition().y()));
        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.getPosition() == null) continue;
            if (zombie.isBoss()) continue;
            boolean frozenInIce = FrostbiteFreezing.isFrozenInIce(screen.session, zombie);
            float t = zombieAnimTimes.getOrDefault(zombie, 0f);
            if (!frozenInIce) {
                t += delta;
                zombieAnimTimes.put(zombie, t);
            }
            Position p = zombie.getPosition();
            float x = GameScreen.BOARD_X + (float) p.x() * boardTileWidth;
            float y = screen.cellY(p.y());
            float zombieOffsetY = y + 40f;

            boolean waterRippleEligible = screen.isBeach()
                    && !ZOMBIE_BEACH_FISHERMAN_ALIAS.equals(zombie.getAlias());
            ZombieWaterRipple waterRipple = waterRippleEligible
                    ? updateZombieWaterRipple(zombie, delta) : null;
            boolean submerged = waterRipple != null && waterRipple.inWater;

            float clipWaterY = y + 15f;
            float rippleDrawX = (x + boardTileWidth * 0.5f) + RIPPLE_OFFSET_X;
            float rippleDrawY = clipWaterY + RIPPLE_OFFSET_Y;

            float submergeOffset = ZOMBIE_BEACH_SNORKEL_ALIAS.equals(zombie.getAlias())
                    ? SWIMMER_SUBMERGE_OFFSET_Y : DEFAULT_SUBMERGE_OFFSET_Y;
            float zombieDrawY = submerged ? zombieOffsetY - submergeOffset : zombieOffsetY;

            if (zombie.isFromNecromancy()) {
                zombieSpawnEffects.put(zombie, 0f);
                zombie.setFromNecromancy(false);
                AudioManager.get().playSound(AudioEnum.SFX_NECROMANCY);
            }

            // Hypno-shroom's registered overlay marks a zombie that now fights for the player;
            // without it a hypnotised zombie is indistinguishable from a hostile one.
            boolean hypnotized = zombie.isHypnotized();
            if (hypnotized && !zombieHypnotizedLast.getOrDefault(zombie, false)) {
                AudioManager.get().playSound(AudioEnum.SFX_HYPNOTIZE);
            }
            zombieHypnotizedLast.put(zombie, hypnotized);
            if (hypnotized) {
                float hypnoT = t;
                int hypnoRow = (int) Math.round(p.y());
                screen.queueRowDraw(hypnoRow, () -> {
                    screen.drawPam(HYPNO_ZOMBIE_EFFECT_PAM, "animation", hypnoT, x + 20f, zombieOffsetY,
                            HYPNO_OVERLAY_SCALE, false);
                    drawHypnoBubbles(zombie, hypnoT, x + boardTileWidth * 0.5f, zombieOffsetY, boardTileHeight);
                });
            }

            if (zombieSpawnEffects.containsKey(zombie)) {
                float effectTime = zombieSpawnEffects.get(zombie) + delta;
                if (effectTime < ZOMBIE_SPAWN_EFFECT_DURATION) {
                    zombieSpawnEffects.put(zombie, effectTime);
                    int spawnRow = (int) Math.round(p.y());
                    screen.queueRowDraw(spawnRow, () -> screen.drawPam(
                            ZOMBIE_SPAWN_EFFECT_PAM,
                            "animation",
                            effectTime,
                            x - 10f,
                            zombieOffsetY,
                            0.52f,
                            zombie.isFacingRight()
                    ));
                } else {
                    zombieSpawnEffects.remove(zombie);
                }
            }

            boolean gyratingNow = zombie.getEffectStatus() instanceof RotationalTurbulenceState spin
                    && spin.isActivelyGyrating();
            if (gyratingNow && !zombieGyratingLast.getOrDefault(zombie, false)) {
                screen.effects().addDeflectSparkEffect(new Position(p.x(), p.y()));
            }
            zombieGyratingLast.put(zombie, gyratingNow);

            // PusherMove leaves the visual "push" state active while the barrel zombie
            // is moving. If the zombie has already acquired a target, that stale push
            // state must not mask its eat animation.
            boolean staleBarrelPushWhileEating =
                    ZOMBIE_BARREL_ROLLER_ALIAS.equals(zombie.getAlias())
                            && zombie.getZombieState() == ZombieState.EATING
                            && "push".equals(zombie.getActionAnimationState());
            boolean isBarrelPusher = ZOMBIE_BARREL_ROLLER_ALIAS.equals(zombie.getAlias());
            boolean hasActionOverride = !isBarrelPusher
                    && zombie.getZombieState() != ZombieState.DEAD
                    && zombie.getActionAnimationState() != null
                    && !staleBarrelPushWhileEating;
            boolean barrelBroken = isBarrelPusher
                    && zombie.getPushedStructure() != null
                    && zombie.getPushedStructure().getType() == model.pitches.obstacles.PushableType.BARREL
                    && !zombie.getPushedStructure().isAlive();
            String preferred;
            if (zombie.getZombieState() == ZombieState.DEAD) {
                preferred = barrelBroken ? "die2" : "die";
            } else if (hasActionOverride) {
                // A special one-off/looping action beat (e.g. "toss", "push",
                // "cast", "cast_loop", "reel") takes priority over the plain
                // eat/spin/walk resolution below.
                preferred = zombie.getActionAnimationState();
                if ("power_up".equals(preferred)
                        && !"power_up".equals(zombieActionStateLast.get(zombie))) {
                    AudioManager.get().playSound(AudioEnum.SFX_LASER_SHOT);
                }
                zombieActionStateLast.put(zombie, preferred);
            } else if (isBarrelPusher) {
                // ZombieBarrelRoller's first walking clip contains the barrel as part of
                // the same PAM.  When the barrel is gone, the corresponding walk2 clip
                // removes the barrel (apart from the remaining particle artwork).
                // Never let PusherMove's "push" action override these clips.
                preferred = barrelBroken ? "walk2" : "walk";
            } else if (zombie.getZombieState() == ZombieState.EATING) {
                // ZombieBeachFisherman's PAM has no dedicated "eat" clip; it
                // reuses its "toss" animation for chomping instead.
                boolean hasNewspaper = ZOMBIE_NEWSPAPER_ALIAS.equals(zombie.getAlias())
                        && zombie.getArmour() instanceof ZombieArmour armour
                        && !armour.isDestroyed();
                preferred = ZOMBIE_BEACH_FISHERMAN_ALIAS.equals(zombie.getAlias()) ? "toss"
                        : hasNewspaper ? "eat_newspaper"
                        : barrelBroken ? "eat2" : "eat";
                if (!zombieEatingLast.getOrDefault(zombie, false)) {
                    AudioManager.get().playSound(AudioEnum.SFX_ZOMBIE_EAT);
                }
                zombieEatingLast.put(zombie, true);
            } else {
                zombieEatingLast.put(zombie, false);
                zombieActionStateLast.remove(zombie);
                boolean stillRunning = ZOMBIE_MODERN_ALLSTAR_ALIAS.equals(zombie.getAlias())
                        && zombie.getAttackBehavior() instanceof SmashAttack;
                boolean hasNewspaper = ZOMBIE_NEWSPAPER_ALIAS.equals(zombie.getAlias())
                        && zombie.getArmour() instanceof ZombieArmour armour
                        && !armour.isDestroyed();
                preferred = gyratingNow ? "spin" : stillRunning ? "run"
                        : hasNewspaper ? "walk_newspaper" : barrelBroken ? "walk2" : "walk";
            }
            String path = ZombieAnimationRegistry.pathFor(zombie.getAlias(), screen.seasonFolder);

            float animationTime = t;
            if (hasActionOverride) {
                float duration = screen.pam().resolveClipDuration(zombie.getAlias(), preferred);
                float elapsed = (float) zombie.getActionAnimationElapsed();
                if (zombie.isActionAnimationLoop()) {
                    animationTime = duration > 0f ? elapsed % duration : elapsed;
                } else {
                    // One-shot beat: play forward and hold the last frame
                    // instead of looping/glitching once it finishes.
                    animationTime = duration > 0f ? Math.min(elapsed, duration) : elapsed;
                }
            } else if (("walk".equals(preferred) || "walk2".equals(preferred)
                    || "eat".equals(preferred) || "eat2".equals(preferred)
                    || "toss".equals(preferred) || "spin".equals(preferred)
                    || "run".equals(preferred) || "walk_newspaper".equals(preferred)
                    || "eat_newspaper".equals(preferred)) && path != null) {
                float duration = screen.pam().resolveClipDuration(zombie.getAlias(), preferred);
                if (duration > 0f) {
                    animationTime = t % duration;
                }
            }
            Map<String, Boolean> armorVisibility = armorMask.basicZombieArmorVisibility(zombie);
            ZombotanyArt.Head plantHead = ZombotanyArt.headFor(zombie.getAlias());
            if (plantHead != null) armorVisibility = mergeHeadlessMask(armorVisibility);

            // The butter-stun face overlay is a named element baked into each
            // zombie's own PAM (same idea as the armor pieces above), so it's
            // toggled the same way rather than drawn as a separate asset.
            Map<String, Boolean> elementVisibility = armorVisibility != null
                    ? armorVisibility : new java.util.HashMap<>();
            elementVisibility.put(BUTTER_ELEMENT_NAME, zombie.getStatus() == Zombie.Status.BUTTER);

            // FROZEN is the solid freeze (Ice-shroom, Iceberg Lettuce's and Snow Pea's
            // Plant Food); FREEZE is the chill a snow projectile leaves. Both read as ice,
            // so both get the blue wash - previously a zombie frozen stiff looked untouched.
            boolean chilled = zombie.getStatus() == Zombie.Status.FREEZE
                    || zombie.getStatus() == Zombie.Status.FROZEN;
            ZombieVisualArgs visualArgs = new ZombieVisualArgs(t, x, zombieDrawY, zombieOffsetY,
                    submerged, hypnotized, chilled, preferred, path, animationTime, elementVisibility,
                    plantHead, clipWaterY, waterRipple, rippleDrawX, rippleDrawY,
                    boardTileWidth, boardTileHeight, delta);
            int zombieRow = (int) Math.round(p.y());
            screen.queueRowDraw(zombieRow, () -> drawZombieVisual(zombie, visualArgs));
        }
        zombieAnimTimes.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        zombieSpawnEffects.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        zombieWaterRipples.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        zombieGyratingLast.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        zombieEatingLast.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        zombieHypnotizedLast.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        zombieActionStateLast.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        zombieLastHp.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        pianoDamageAnimTimes.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        arcadeDeathAnimTimes.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        arcadeWaterRipples.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        barrelDeathAnimTimes.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
    }

    private record ZombieVisualArgs(
            float t, float x, float zombieDrawY, float zombieOffsetY, boolean submerged,
            boolean hypnotized, boolean chilled, String preferred, String path, float animationTime,
            Map<String, Boolean> elementVisibility, ZombotanyArt.Head plantHead, float clipWaterY,
            ZombieWaterRipple waterRipple, float rippleDrawX, float rippleDrawY,
            float boardTileWidth, float boardTileHeight, float delta) { }

    private void drawZombieVisual(Zombie zombie, ZombieVisualArgs a) {
        float t = a.t();
        float x = a.x();
        float zombieDrawY = a.zombieDrawY();
        float zombieOffsetY = a.zombieOffsetY();
        boolean submerged = a.submerged();
        boolean hypnotized = a.hypnotized();
        boolean chilled = a.chilled();
        String preferred = a.preferred();
        String path = a.path();
        float animationTime = a.animationTime();
        Map<String, Boolean> elementVisibility = a.elementVisibility();
        ZombotanyArt.Head plantHead = a.plantHead();
        float clipWaterY = a.clipWaterY();
        ZombieWaterRipple waterRipple = a.waterRipple();
        float rippleDrawX = a.rippleDrawX();
        float rippleDrawY = a.rippleDrawY();
        float boardTileWidth = a.boardTileWidth();
        float boardTileHeight = a.boardTileHeight();
        float delta = a.delta();

        boolean waterClipActive = submerged && pushWaterClip(clipWaterY);
        boolean tinted = hypnotized || chilled;
        boolean frozenSolid = zombie.getStatus() == Zombie.Status.FROZEN;
        Color tint = hypnotized ? HYPNO_TINT : frozenSolid ? FROZEN_TINT : FREEZE_TINT;
        try {
            drawZombiePiano(zombie, preferred, t, delta, x - 7f, zombieDrawY, zombie.isFacingRight());
            drawZombieArcade(zombie, delta, boardTileWidth, zombie.isFacingRight());
            drawZombieIceBlock(zombie, boardTileWidth, boardTileHeight);
            drawZombieBarrel(zombie, delta, boardTileWidth);
            if (tinted) screen.batch.setColor(tint);
            boolean pamDrawn;
            if (zombie.isFacingRight()) {
                pamDrawn = screen.drawPamMirrored(path, preferred, animationTime,
                        x - 10f, zombieDrawY, 0.52f);
            } else {
                pamDrawn = screen.drawPam(path, preferred, animationTime, x - 10f, zombieDrawY,
                        0.52f, false, elementVisibility);
            }
            if (tinted) screen.batch.setColor(Color.WHITE);
            if (pamDrawn && plantHead != null) {
                drawPlantHead(plantHead, t, x - 10f, zombieDrawY, ZOMBIE_SCALE,
                        zombie.isFacingRight(), zombie.getZombieState());
            }

            if (!pamDrawn) {
                TextureRegion region = GameAssetManager.get().getZombieRegion(zombie.getAlias());
                Color fallbackTint = hypnotized ? HYPNO_FALLBACK_TINT
                        : chilled ? FREEZE_FALLBACK_TINT : new Color(0.55f, 0.5f, 0.45f, 1f);
                screen.drawEntity(region, x, zombieDrawY, boardTileWidth, boardTileHeight,
                        fallbackTint, GameScreenGraphics.initials(zombie.getAlias()));
            }
        } finally {
            if (waterClipActive) popWaterClip();
        }

        if (waterRipple != null && (waterRipple.inWater || waterRipple.exiting)) {
            drawWaterRipple(zombie.getAlias(), rippleDrawX, rippleDrawY,
                    zombie.isFacingRight(), waterRipple);
        }

        if (zombie.isSunBeanCarrier()) {
            final String SUN_BEAN_CARRIER_PAM =
                    "768/FULL/EFFECTS/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1.PAM";
            float haloTime = t;
            float haloDuration = AnimationFactory.clipDurationForPath(SUN_BEAN_CARRIER_PAM, "animation");
            if (haloDuration > 0f) haloTime %= haloDuration;
            screen.drawPam(SUN_BEAN_CARRIER_PAM, "animation", haloTime,
                    x - 10f, zombieDrawY, 0.52f, zombie.isFacingRight());
        }

        if (screen.session.isZombieInSandStorm(zombie)) {
            double stormTime = screen.session.getSandStormAnimationTime(zombie);
            if (stormTime >= 0.0) {
                float stormX = x + boardTileWidth * 0.5f - 60f;
                float stormY = zombieOffsetY + boardTileHeight * 0.78f - 40f;
                drawZombieSandStorm(stormX, stormY,
                        boardTileWidth, boardTileHeight, (float) stormTime, System.identityHashCode(zombie), true, zombie.isFacingRight());
            }
        }
    }

    void trackZombieDeaths(List<Zombie> aliveBeforeTick) {
        List<Zombie> stillAlive = screen.session.getZombies();
        for (Zombie zombie : aliveBeforeTick) {
            if (stillAlive.contains(zombie)) continue;
            if (zombie.isBoss()) continue;
            if (zombie.getPosition() == null) continue;
            if (zombie.getZombieState() != ZombieState.DEAD) continue;

            String shockPath = zombie.diedFromShock()
                    ? ZombieShockAnimationRegistry.pathFor(zombie) : null;
            float shockDuration = shockPath == null ? 0f
                    : AnimationFactory.clipDurationForPath(
                    shockPath, ZombieShockAnimationRegistry.SHOCK_STATE);
            if (shockPath != null && shockDuration <= 0f) shockDuration = DEATH_ANIM_DURATION;
            if (shockPath != null) {
                AudioManager.get().playSound(AudioEnum.SFX_ELECTRIC_SHOCK);
            }

            String ashPath = (zombie.diedFromFire() || zombie.diedFromAsh() || zombie.diedFromShock())
                    ? ZombieAshAnimationRegistry.pathFor(zombie) : null;
            float ashDuration = ashPath == null ? 0f
                    : AnimationFactory.clipDurationForPath(
                    ashPath, ZombieAshAnimationRegistry.ASH_STATE);
            if (ashPath != null && ashDuration <= 0f) ashDuration = DEATH_ANIM_DURATION;

            boolean playNormalDeathAfterShock = shockPath != null && ashPath == null;
            PushableStructure barrelStructure =
                    ZOMBIE_BARREL_ROLLER_ALIAS.equals(zombie.getAlias())
                            && zombie.getPushedStructure() != null
                            && zombie.getPushedStructure().getType() == model.pitches.obstacles.PushableType.BARREL
                            && zombie.getPushedStructure().isAlive()
                            ? zombie.getPushedStructure() : null;
            boolean barrelBroken = ZOMBIE_BARREL_ROLLER_ALIAS.equals(zombie.getAlias())
                    && zombie.getPushedStructure() != null
                    && zombie.getPushedStructure().getType() == model.pitches.obstacles.PushableType.BARREL
                    && !zombie.getPushedStructure().isAlive();
            String deathState = barrelBroken ? "die2" : "die";
            float normalDeathDuration = screen.pam().resolveClipDuration(zombie.getAlias(), deathState);
            if (normalDeathDuration <= 0f) normalDeathDuration = DEATH_ANIM_DURATION;

            float deathSequenceDuration = shockDuration;
            if (shockPath != null) {
                deathSequenceDuration += ashPath != null ? ashDuration : normalDeathDuration;
            } else {
                deathSequenceDuration = ashPath != null ? ashDuration : normalDeathDuration;
            }

            dyingZombies.add(new DyingZombie(
                    zombie.getAlias(), zombie.getPosition(), zombie.isFacingRight(),
                    deathSequenceDuration, shockPath, shockDuration,
                    ashPath, ashDuration, barrelBroken, barrelStructure));
            zombieAnimTimes.remove(zombie);
            screen.onZombieDied(zombie);
        }
    }

    void drawDyingZombies(float delta) {
        if (dyingZombies.isEmpty()) return;
        float boardTileWidth = screen.getBoardTileWidth();
        for (DyingZombie dz : dyingZombies) {
            dz.time += delta;
            float x = GameScreen.BOARD_X + (float) dz.position.x() * boardTileWidth;
            float y = screen.cellY((int) dz.position.y());
            float zombieOffsetY = y + 40f;
            int row = (int) dz.position.y();

            boolean inShockPhase = dz.shockPath != null && dz.time < dz.shockDuration;
            if (inShockPhase) {
                float shockTime = Math.min(dz.time, dz.shockDuration);
                screen.queueRowDraw(row, () -> screen.drawPam(dz.shockPath, ZombieShockAnimationRegistry.SHOCK_STATE,
                        shockTime, x - 10f, zombieOffsetY, 0.52f, dz.facingRight));
                continue;
            }

            float postShockTime = dz.shockPath == null ? dz.time : dz.time - dz.shockDuration;

            if (dz.ashPath != null) {
                // Shock is complete; play the ash-death animation next.
                float ashTime = Math.min(postShockTime, dz.ashDuration);
                screen.queueRowDraw(row, () -> screen.drawPam(dz.ashPath, ZombieAshAnimationRegistry.ASH_STATE,
                        ashTime, x - 10f, zombieOffsetY, 0.52f, dz.facingRight));
                continue;
            }

            // Once the death animation has fully played out, the pusher's own corpse
            // pose must stop being drawn - otherwise it stays glued to the screen
            // forever (frozen on its last "die" frame) instead of disappearing, even
            // though the code below has already moved on to animating the surviving
            // barrel independently. Only the barrel logic further down should still
            // run past this point.
            boolean deathAnimComplete = dz.time >= dz.duration;
            if (!deathAnimComplete) {
                String path = ZombieAnimationRegistry.pathFor(dz.alias, screen.seasonFolder);

                // Particles (head + hand) drop off and settle onto the row's ground
                // over roughly the first half of the death animation.
                float normalDeathTime = Math.max(0f, postShockTime);
                float normalDeathDuration = Math.max(0.001f, dz.duration - dz.shockDuration);
                float fallProgress = Math.min(1f, normalDeathTime / (normalDeathDuration * 0.5f));
                float fallEase = 1f - (1f - fallProgress) * (1f - fallProgress);
                float particleDrop = 24f * fallEase;

                ZombotanyArt.Head plantHead = ZombotanyArt.headFor(dz.alias);
                if (plantHead == null) {
                    screen.queueRowDraw(row, () -> screen.drawPam(path, "particles", normalDeathTime,
                            x - 10f, zombieOffsetY - particleDrop, 0.52f, dz.facingRight));
                }
                float dieTime = Math.min(normalDeathTime, normalDeathDuration);
                if (plantHead != null) {
                    float fade = Math.max(0f, 1f - normalDeathTime / normalDeathDuration);
                    screen.queueRowDraw(row, () -> {
                        screen.drawPam(path, dz.barrelBroken ? "die2" : "die", dieTime, x - 10f, zombieOffsetY, 0.52f, dz.facingRight,
                                ZombotanyArt.headlessBodyMask());
                        screen.batch.setColor(1f, 1f, 1f, Math.min(1f, 0.25f + fade));
                        drawPlantHead(plantHead, normalDeathTime, x - 10f, zombieOffsetY - particleDrop,
                                ZOMBIE_SCALE, dz.facingRight, ZombieState.DEAD);
                        screen.batch.setColor(Color.WHITE);
                    });
                } else {
                    screen.queueRowDraw(row, () -> screen.drawPam(path, dz.barrelBroken ? "die2" : "die", dieTime,
                            x - 10f, zombieOffsetY, 0.52f, dz.facingRight));
                }
                if (ZOMBIE_PIANO_ALIAS.equals(dz.alias)) {
                    screen.queueRowDraw(row, () -> drawPianoDying(dieTime, x - 10f, zombieOffsetY, dz.facingRight));
                }
            }

            // The barrel is a separate object, but it must NOT appear during the
            // pusher's death animation: the combined zombie PAM owns the visual during
            // that entire sequence.  Only after the zombie death animation is complete
            // does the surviving barrel take over as a standalone rolling PAM.
            if (dz.barrelStructure != null && dz.time >= dz.duration) {
                PushableStructure barrel = dz.barrelStructure;
                if (barrel.isAlive() && barrel.getPosition() != null) {
                    Position barrelPos = barrel.getPosition();
                    float barrelX = GameScreen.BOARD_X
                            + (float) barrelPos.x() * boardTileWidth + BARREL_OFFSET_X;
                    float barrelY = screen.cellY(barrelPos.y()) + BARREL_OFFSET_Y;

                    float rollTime = dz.barrelRollTime;
                    float rollDuration = AnimationFactory.exactClipDurationForPath(
                            ZOMBIE_BARREL_PAM, "roll");
                    if (rollDuration > 0f) rollTime %= rollDuration;
                    final float rollTimeForDraw = rollTime;

                    screen.queueRowDraw(row, () -> screen.pam().drawPamExact(
                            ZOMBIE_BARREL_PAM, "roll", rollTimeForDraw,
                            barrelX, barrelY, BARREL_SCALE, dz.facingRight));
                    dz.barrelRollTime += delta;
                } else if (dz.barrelDeathTime < 0f) {
                    // If the surviving barrel is destroyed after the pusher has died,
                    // start its own death clip from frame zero.
                    dz.barrelDeathTime = 0f;
                }

                if (dz.barrelDeathTime >= 0f) {
                    float barrelDeathDuration = AnimationFactory.exactClipDurationForPath(
                            ZOMBIE_BARREL_PAM, "die");
                    if (barrelDeathDuration <= 0f) {
                        barrelDeathDuration = DEFAULT_BARREL_DEATH_DURATION;
                    }

                    float barrelDieTime = Math.min(dz.barrelDeathTime, barrelDeathDuration);
                    Position barrelPos = barrel.getPosition();
                    if (barrelPos != null) {
                        float barrelX = GameScreen.BOARD_X
                                + (float) barrelPos.x() * boardTileWidth + BARREL_OFFSET_X;
                        float barrelY = screen.cellY(barrelPos.y()) + BARREL_OFFSET_Y;
                        screen.queueRowDraw(row, () -> screen.pam().drawPamExact(
                                ZOMBIE_BARREL_PAM, "die", barrelDieTime,
                                barrelX, barrelY, BARREL_SCALE, dz.facingRight));
                    }
                    dz.barrelDeathTime += delta;
                }
            }
        }
        dyingZombies.removeIf(dz ->
                dz.time > dz.duration
                        && (dz.barrelStructure == null
                        || !dz.barrelStructure.isAlive()
                        && dz.barrelDeathTime >= DEFAULT_BARREL_DEATH_DURATION));
    }

    /**
     * Several small purple/pink bubbles rising from a hypnotized zombie's feet to over its
     * head, staggered so they don't all pop at once, each fading in, floating up with a
     * slight side-to-side sway, then fading out and looping back to the bottom - same idea
     * as the original game's hypnosis bubble trail.
     */
    private void drawHypnoBubbles(Zombie zombie, float t, float baseX, float baseY, float boardTileHeight) {
        float riseHeight = boardTileHeight * 0.85f;
        float fadeWindow = 0.15f;
        for (int i = 0; i < HYPNO_BUBBLE_COUNT; i++) {
            float phaseOffset = (HYPNO_BUBBLE_CYCLE_SECONDS / HYPNO_BUBBLE_COUNT) * i;
            float localTime = (t + phaseOffset) % HYPNO_BUBBLE_CYCLE_SECONDS;
            float progress = localTime / HYPNO_BUBBLE_CYCLE_SECONDS;

            float fadeIn = Math.min(1f, progress / fadeWindow);
            float fadeOut = Math.min(1f, (1f - progress) / fadeWindow);
            float alpha = Math.max(0f, Math.min(fadeIn, fadeOut));
            if (alpha <= 0f) continue;

            float lateralSeed = i * 2.4f;
            float bubbleX = baseX + (float) Math.sin(progress * Math.PI * 2f + lateralSeed) * HYPNO_BUBBLE_SWAY
                    + ((i % 3) - 1) * 9f;
            float bubbleY = baseY + progress * riseHeight;
            float size = HYPNO_BUBBLE_MIN_SIZE + (HYPNO_BUBBLE_MAX_SIZE - HYPNO_BUBBLE_MIN_SIZE) * progress;

            screen.batch.setColor(HYPNO_BUBBLE_COLOR.r, HYPNO_BUBBLE_COLOR.g, HYPNO_BUBBLE_COLOR.b, alpha);
            screen.batch.draw(screen.bubbleTexture, bubbleX - size * 0.5f, bubbleY - size * 0.5f, size, size);
        }
        screen.batch.setColor(Color.WHITE);
    }

    private void drawPlantHead(ZombotanyArt.Head head, float time, float bodyX, float bodyY,
                               float bodyScale, boolean facingRight, ZombieState state) {
        float bob = state == ZombieState.EATING
                ? (float) Math.sin(time * 9.0f) * HEAD_BOB * 1.6f
                : (float) Math.sin(time * 4.2f) * HEAD_BOB;
        float direction = facingRight ? 1f : -1f;
        float headX = bodyX + head.offsetX() * bodyScale * direction;
        float headY = bodyY + (head.offsetY() + bob) * bodyScale;
        float headTime = time;
        float duration = screen.pam().resolvePlantClipDuration(head.plantName(), "idle");
        if (duration > 0f) headTime = time % duration;
        screen.drawPamMirrored(head.pam(), "idle", headTime, headX, headY,
                bodyScale * head.scale());
    }

    // Pianist's piano prop: same "idle"/"walk"->eat resolution as the zombie body, plus a
    // one-shot "damage" beat (mirrors OctopusThrow's toss trick, but driven off HP deltas
    // since a piano hit isn't its own attack/effect) and its own "die" clip.
    private void drawZombiePiano(Zombie zombie, String preferred, float t, float delta, float x, float zombieDrawY,
                                 boolean facingRight) {
        if (!ZOMBIE_PIANO_ALIAS.equals(zombie.getAlias())) return;

        Integer lastHp = zombieLastHp.put(zombie, zombie.getHP());
        if (lastHp != null && zombie.getHP() < lastHp) {
            pianoDamageAnimTimes.put(zombie, 0f);
            AudioManager.get().playSound(AudioEnum.SFX_PIANO);
        }

        Float damageTime = pianoDamageAnimTimes.get(zombie);
        if (damageTime != null) {
            float damageDuration = AnimationFactory.exactClipDurationForPath(PIANO_PROP_PAM, "damage");
            if (damageDuration <= 0f) damageDuration = DEFAULT_PIANO_DAMAGE_DURATION;
            if (damageTime >= damageDuration) {
                pianoDamageAnimTimes.remove(zombie);
                damageTime = null;
            } else {
                pianoDamageAnimTimes.put(zombie, damageTime + delta);
            }
        }

        String pianoState;
        float pianoTime;
        if (damageTime != null) {
            pianoState = "damage";
            pianoTime = damageTime;
        } else if ("eat".equals(preferred)) {
            pianoState = "play";
            pianoTime = t;
        } else {
            pianoState = "idle";
            pianoTime = t;
        }
        float duration = AnimationFactory.exactClipDurationForPath(PIANO_PROP_PAM, pianoState);
        if (duration > 0f) pianoTime %= duration;

        float direction = facingRight ? 1f : -1f;
        float pianoX = x + PIANO_OFFSET_X * direction;
        float pianoY = zombieDrawY + PIANO_OFFSET_Y;
        screen.pam().drawPamExact(PIANO_PROP_PAM, pianoState, pianoTime, pianoX, pianoY, PIANO_SCALE, facingRight);
    }

    private void drawPianoDying(float dieTime, float x, float zombieDrawY, boolean facingRight) {
        float duration = AnimationFactory.exactClipDurationForPath(PIANO_PROP_PAM, "die");
        float pianoTime = duration > 0f ? Math.min(dieTime, duration) : dieTime;
        float direction = facingRight ? 1f : -1f;
        float pianoX = x + PIANO_OFFSET_X * direction;
        float pianoY = zombieDrawY + PIANO_OFFSET_Y;
        screen.pam().drawPamExact(PIANO_PROP_PAM, "die", pianoTime, pianoX, pianoY, PIANO_SCALE, facingRight);
    }

    // Arcade cabinet the ZombieArcade pushes ahead of itself: idle while intact and not
    // being pushed, active while the zombie's own "push" beat is running (see PusherMove),
    // and a one-shot death clip the moment the structure's HP hits zero. Positioned from the
    // structure's own board Position rather than the zombie's, since the cabinet leads the
    // zombie by PusherMove.PUSH_GAP and can lag behind on destruction.
    private void drawZombieArcade(Zombie zombie, float delta, float boardTileWidth, boolean facingRight) {
        if (!ZOMBIE_ARCADE_ALIAS.equals(zombie.getAlias())) return;
        PushableStructure structure = zombie.getPushedStructure();
        if (structure == null || structure.getPosition() == null) return;

        if (!structure.isAlive() && !arcadeDeathAnimTimes.containsKey(zombie)) {
            arcadeDeathAnimTimes.put(zombie, 0f);
        }

        Float deathTime = arcadeDeathAnimTimes.get(zombie);
        if (deathTime != null) {
            float deathDuration = AnimationFactory.exactClipDurationForPath(ARCADE_PROP_PAM, "death");
            if (deathDuration <= 0f) deathDuration = DEFAULT_ARCADE_DEATH_DURATION;
            if (deathTime >= deathDuration) {
                arcadeDeathAnimTimes.remove(zombie);
                return;
            }
            arcadeDeathAnimTimes.put(zombie, deathTime + delta);
        }

        String arcadeState = deathTime != null ? "death"
                : "push".equals(zombie.getActionAnimationState()) ? "active" : "idle";
        float arcadeTime = deathTime != null ? deathTime : zombieAnimTimes.getOrDefault(zombie, 0f);
        float duration = AnimationFactory.exactClipDurationForPath(ARCADE_PROP_PAM, arcadeState);
        if (duration > 0f && deathTime == null) arcadeTime %= duration;

        Position pos = structure.getPosition();
        float arcadeX = GameScreen.BOARD_X + (float) pos.x() * boardTileWidth - 25;
        float arcadeY = screen.cellY(pos.y()) + 40f;
        screen.pam().drawPamExact(ARCADE_PROP_PAM, arcadeState, arcadeTime, arcadeX, arcadeY, ARCADE_SCALE, facingRight);

        // Same water ripple treatment as any zombie standing in the surf, but driven off
        // the cabinet's own board position (it leads the zombie by PusherMove.PUSH_GAP)
        // rather than the pushing zombie's.
        if (screen.isBeach()) {
            String arcadeRipplePam = ripplePamFor(zombie.getAlias());
            ZombieWaterRipple arcadeRipple = updateWaterRipple(arcadeWaterRipples, zombie, pos, delta, arcadeRipplePam);
            if (arcadeRipple.inWater || arcadeRipple.exiting) {
                float tileX = GameScreen.BOARD_X + (float) pos.x() * boardTileWidth;
                float tileY = screen.cellY(pos.y());
                float rippleX = (tileX + boardTileWidth * 0.5f) + RIPPLE_OFFSET_X;
                float rippleY = (tileY + 15f) + RIPPLE_OFFSET_Y;
                drawWaterRipple(zombie.getAlias(), rippleX, rippleY, facingRight, arcadeRipple);
            }
        }
    }

    /**
     * Renders the Pirate Barrel Pusher's separate barrel after the pusher zombie
     * has died, or its one-shot death animation after the barrel itself is destroyed.
     * While both are alive, the combined pusher PAM already contains the barrel and
     * this method deliberately draws nothing.
     */
    private void drawZombieBarrel(Zombie zombie, float delta, float boardTileWidth) {
        if (!ZOMBIE_BARREL_ROLLER_ALIAS.equals(zombie.getAlias())) return;

        // The first/walk animation of ZombieBarrelRoller already contains the intact
        // barrel.  Drawing the standalone barrel while the zombie is alive creates the
        // visible "two barrels" bug.  The standalone barrel is intentionally rendered
        // only by drawDyingZombies(), after the pusher's death animation has completed.
    }

    // The ice block a Troglobite pushes ahead of itself once freed. Falls smoothly in
    // from above the lawn onto the cell just ahead of the zombie (mirroring the arcade
    // cabinet's placement / ZombossRenderer's sky-strike drop) and, once landed, is drawn
    // at the structure's own board position exactly like the arcade cabinet so it keeps
    // pace with PusherMove while being shoved along - and by plants/tile sliders moving it.
    private void drawZombieIceBlock(Zombie zombie, float boardTileWidth, float boardTileHeight) {
        if (!ZOMBIE_TROGLOBITE_ALIAS.equals(zombie.getAlias())) return;
        PushableStructure structure = zombie.getPushedStructure();
        if (structure == null || structure.getPosition() == null || !structure.isAlive()) return;

        com.badlogic.gdx.graphics.Texture texture = screen.assets().zombieIceBlockTexture();
        if (texture == null) return;

        Position pos = structure.getPosition();
        float baseX = GameScreen.BOARD_X + (float) pos.x() * boardTileWidth;
        float baseY = screen.cellY(pos.y());

        float drawW = boardTileWidth * ICE_BLOCK_PUSH_SCALE;
        float drawH = boardTileHeight * ICE_BLOCK_PUSH_SCALE;
        float drawX = baseX + (boardTileWidth - drawW) * 0.5f + ICE_BLOCK_OFFSET_X;
        float drawY = baseY + (boardTileHeight - drawH) * 0.5f + ICE_BLOCK_OFFSET_Y;

        float alpha = 1f;
        if (structure.isFalling()) {
            float remaining = 1f - (float) structure.getFallProgress();
            drawY += remaining * ICE_BLOCK_FALL_HEIGHT_TILES * boardTileHeight;
            alpha = 0.6f + 0.4f * (float) structure.getFallProgress();
        }

        screen.batch.setColor(1f, 1f, 1f, alpha);
        screen.batch.draw(texture, drawX, drawY, drawW, drawH);
        screen.batch.setColor(Color.WHITE);

        // The frozen imp riding inside the block, at the same real scale as any other
        // on-field zombie, centered over the block and hard-clipped to its bounds so it
        // never spills outside the ice regardless of the PAM's own art anchor/padding.
        boolean clipActive = pushRectClip(drawX, drawY, drawW, drawH);
        try {
            float impCenterX = drawX + drawW * 0.5f;
            float impCenterY = drawY + drawH * 0.42f;
            screen.batch.setColor(FROZEN_TINT.r, FROZEN_TINT.g, FROZEN_TINT.b, alpha);
            boolean impDrawn = screen.pam().drawPamExact(ICE_BLOCK_IMP_PAM, "idle", 0f,
                    impCenterX - 10f, impCenterY, ZOMBIE_SCALE, false);
            screen.batch.setColor(Color.WHITE);
            if (!impDrawn) {
                TextureRegion impRegion = GameAssetManager.get().getZombieRegion("ZombieImp");
                if (impRegion != null) {
                    float impW = drawW * 0.7f;
                    float impH = drawH * 0.7f;
                    screen.batch.setColor(FROZEN_TINT.r, FROZEN_TINT.g, FROZEN_TINT.b, alpha);
                    screen.batch.draw(impRegion, drawX + (drawW - impW) * 0.5f, drawY + (drawH - impH) * 0.3f,
                            impW, impH);
                    screen.batch.setColor(Color.WHITE);
                }
            }
        } finally {
            if (clipActive) popRectClip();
        }
    }

    private Map<String, Boolean> mergeHeadlessMask(Map<String, Boolean> existing) {
        if (existing == null) return ZombotanyArt.headlessBodyMask();
        Map<String, Boolean> merged = new java.util.HashMap<>(existing);
        merged.putAll(ZombotanyArt.headlessBodyMask());
        return merged;
    }

    private void drawZombieSandStorm(float centerX, float centerY, float tileW, float tileH,
                                     float time, int seed, boolean renderPam, boolean flip) {
        boolean pamDrawn = false;
        if (renderPam) {
            String pamPath = SandStorm.PAM_PATH_PLACEHOLDER;
            float elapsed = time;
            String phase;
            float phaseTime;
            float intro = (float) SandStorm.INTRO_DURATION_SECONDS;
            float outroStart = (float) (SandStorm.EVENT_DURATION_SECONDS - SandStorm.OUTRO_DURATION_SECONDS);
            if (elapsed < intro) {
                phase = "intro";
                phaseTime = Math.max(0f, elapsed);
            } else if (elapsed < outroStart) {
                phase = "loop";
                phaseTime = (elapsed - intro) % (float) SandStorm.LOOP_DURATION_SECONDS;
            } else {
                phase = "outro";
                phaseTime = Math.max(0f, elapsed - outroStart);
            }

            float stormScale = tileW / 118f;
            pamDrawn = screen.drawPam(pamPath, phase, phaseTime, centerX, centerY, stormScale, flip);
        }

        if (pamDrawn) return;

        screen.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        int particleCount = 16;
        float baseRadius = tileW * 0.72f;
        for (int i = 0; i < particleCount; i++) {
            float particleSeed = (seed % 360) + i * 137.5f;
            float angularSpeed = 52f + (i % 3) * 18f;
            float angle = (particleSeed + time * angularSpeed) % 360f;
            float rad = (float) Math.toRadians(angle);
            float orbit = baseRadius * (0.35f + 0.65f * ((i % 5) / 4f));
            float px = centerX + (float) Math.cos(rad) * orbit;
            float py = centerY + (float) Math.sin(rad) * orbit * 0.46f;
            float size = tileW * (0.095f + 0.045f * (i % 4));
            float alpha = 0.22f + 0.18f * (float) Math.sin(time * 4.0f + i * 1.31f);
            screen.batch.setColor(0.97f, 0.82f, 0.54f, Math.max(0.10f, alpha));
            screen.batch.draw(screen.whitePixel, px - size * 0.5f, py - size * 0.15f, size * 0.5f, size * 0.15f,
                    size, size * 0.3f, 1f, 1f, angle * 1.5f);
        }
        screen.batch.setColor(Color.WHITE);
        screen.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private ZombieWaterRipple updateZombieWaterRipple(Zombie zombie, float delta) {
        return updateWaterRipple(zombieWaterRipples, zombie, zombie.getPosition(), delta,
                ripplePamFor(zombie.getAlias()));
    }

    // Shared ripple state machine, keyed by whatever owns the ripple (a zombie for its own
    // ripple, or the zombie that pushes an arcade cabinet for the cabinet's ripple) so the
    // arcade box can get the same "in water / exiting" water ripple as any other zombie
    // without duplicating this logic.
    private ZombieWaterRipple updateWaterRipple(Map<Zombie, ZombieWaterRipple> rippleMap, Zombie key,
                                                Position position, float delta, String ripplePam) {
        ZombieWaterRipple ripple = rippleMap.computeIfAbsent(key, z -> new ZombieWaterRipple());
        boolean inWaterNow = isPositionOnFloodedTile(position);
        if (inWaterNow) {
            ripple.inWater = true;
            ripple.exiting = false;
            ripple.exitElapsed = 0f;
            ripple.rippleLoopTime += delta;
        } else if (ripple.inWater) {
            ripple.inWater = false;
            ripple.exiting = true;
            ripple.exitElapsed = 0f;
        } else if (ripple.exiting) {
            ripple.exitElapsed += delta;
            float duration = AnimationFactory.clipDurationForPath(ripplePam, "ripple_exit");
            if (duration <= 0f) duration = DEFAULT_RIPPLE_EXIT_DURATION;
            if (ripple.exitElapsed > duration) {
                ripple.exiting = false;
            }
        }
        return ripple;
    }

    private boolean isZombieOnFloodedTile(Zombie zombie) {
        return zombie.getPosition() != null && isPositionOnFloodedTile(zombie.getPosition());
    }

    private boolean isPositionOnFloodedTile(Position position) {
        if (position == null || screen.session == null) return false;
        Environment environment = screen.session.getEnvironment();
        if (environment == null) return false;
        int row = (int) Math.round(position.y());
        int col = (int) Math.floor(position.x());
        if (col >= environment.getCols()) {
            return true;
        }
        Cell cell = environment.getCell(row, col);
        return cell != null && cell.getTile() != null && cell.getTile().type() == TileType.Water;
    }

    private static String ripplePamFor(String alias) {
        if (alias == null) return WATER_ZOMBIE_RIPPLE_PAM;
        String lower = alias.toLowerCase();
        if (lower.contains("gargantuar")) return WATER_GARGANTUAR_RIPPLE_PAM;
        if (lower.contains("imp")) return WATER_IMP_RIPPLE_PAM;
        return WATER_ZOMBIE_RIPPLE_PAM;
    }

    private void drawWaterRipple(String alias, float centerX, float centerY, boolean flip, ZombieWaterRipple ripple) {
        String ripplePam = ripplePamFor(alias);
        if (ripple.exiting) {
            float duration = AnimationFactory.clipDurationForPath(ripplePam, "ripple_exit");
            if (duration <= 0f) duration = DEFAULT_RIPPLE_EXIT_DURATION;
            screen.drawPam(ripplePam, "ripple_exit", Math.min(ripple.exitElapsed, duration),
                    centerX, centerY, WATER_RIPPLE_SCALE, flip);
        } else if (ripple.inWater) {
            float duration = AnimationFactory.clipDurationForPath(ripplePam, "ripple");
            float loopTime = duration > 0f ? ripple.rippleLoopTime % duration : ripple.rippleLoopTime;
            screen.drawPam(ripplePam, "ripple", loopTime, centerX, centerY, WATER_RIPPLE_SCALE, flip);
        }
    }

    private boolean pushWaterClip(float waterY) {
        screen.batch.flush();
        float worldWidth = screen.stage.getViewport().getWorldWidth();
        float worldHeight = screen.stage.getViewport().getWorldHeight();
        Rectangle clipBounds = new Rectangle(0, waterY, worldWidth, worldHeight - waterY);
        Rectangle scissors = new Rectangle();
        ScissorStack.calculateScissors(
                screen.stage.getCamera(),
                screen.batch.getTransformMatrix(),
                clipBounds,
                scissors
        );
        return ScissorStack.pushScissors(scissors);
    }

    private void popWaterClip() {
        screen.batch.flush();
        ScissorStack.popScissors();
    }

    /** Generic version of pushWaterClip/popWaterClip for clipping to an arbitrary rectangle
     *  (e.g. keeping the frozen imp fully inside the pushed ice block's own drawn bounds). */
    private boolean pushRectClip(float x, float y, float width, float height) {
        screen.batch.flush();
        Rectangle clipBounds = new Rectangle(x, y, width, height);
        Rectangle scissors = new Rectangle();
        ScissorStack.calculateScissors(
                screen.stage.getCamera(),
                screen.batch.getTransformMatrix(),
                clipBounds,
                scissors
        );
        return ScissorStack.pushScissors(scissors);
    }

    private void popRectClip() {
        screen.batch.flush();
        ScissorStack.popScissors();
    }
}