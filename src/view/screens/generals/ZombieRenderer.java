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
import model.collections.zombie.ZombieStunProfile;
import model.collections.zombie.zombie_effect.ProtectorShield;
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
    
    private static final String FUTURE_SPAWN_EFFECT_PAM =
            "768/FULL/EFFECTS/DIRT_SPAWN_FUTURE/DIRT_SPAWN_FUTURE.PAM";
    private static final String FUTURE_SPAWN_EFFECT_STATE = "tomb_dirt_anim";
    
    
    private static final String FUTURE_GARGANTUAR_BASE_PAM =
            "768/FULL/EFFECTS/ZOMBIE_FUTURE_GARGANTUAR_BASE/ZOMBIE_FUTURE_GARGANTUAR_BASE.PAM";
    private static final float FUTURE_GARGANTUAR_BASE_SCALE = 0.52f;
    private static final float FUTURE_GARGANTUAR_BASE_OFFSET_X = 4f;
    private static final float FUTURE_GARGANTUAR_BASE_OFFSET_Y = 0f;
    private static final float DEATH_ANIM_DURATION = 1.0f;
    private static final float HYPNO_OVERLAY_SCALE = 0.55f;
    private static final float ZOMBIE_SCALE = 0.52f;
    private static final float HEAD_BOB = 3.5f;
    private static final String HYPNO_ZOMBIE_EFFECT_PAM =
            "768/INITIAL/EFFECTS/HYPNO_ZOMBIE_EFFECT/HYPNO_ZOMBIE_EFFECT.PAM";

    private static final String ZOMBIE_BEACH_FISHERMAN_ALIAS = "ZombieBeachFisherman";
    private static final String ZOMBIE_PIANO_ALIAS = "ZombiePiano";
    
    private static final String PIANO_PROP_PAM = "768/FULL/ZOMBIE/PIANO/PIANO.PAM";
    private static final float DEFAULT_PIANO_DAMAGE_DURATION = 0.4f;
    
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
    private static final String ZOMBIE_PIRATE_CAPTAIN_ALIAS = "ZombiePirateCaptain";
    private static final String CAPTAIN_PARROT_PAM =
            "768/FULL/ZOMBIE/ZOMBIE_PIRATE_CAPTAIN_PARROT/ZOMBIE_PIRATE_CAPTAIN_PARROT.PAM";
    
    
    private static final float PARROT_OFFSET_X = 30f;
    private static final float PARROT_OFFSET_Y = 60f;
    private static final float PARROT_SCALE = 0.52f;
    private static final String ZOMBIE_TROGLOBITE_ALIAS = "ZombieIceAgeTroglobite";
    
    
    
    private static final float ICE_BLOCK_PUSH_SCALE = 1.8f;
    private static final float ICE_BLOCK_FALL_HEIGHT_TILES = 5.5f;
    
    
    
    private static final float ICE_BLOCK_OFFSET_X = -95f;
    private static final float ICE_BLOCK_OFFSET_Y = 30f;
    
    
    
    private static final String ICE_BLOCK_IMP_PAM = "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_IMP/ZOMBIE_ICEAGE_IMP.PAM";
    private static final String ZOMBIE_MODERN_ALLSTAR_ALIAS = "ZombieModernAllStar";
    private static final String ZOMBIE_NEWSPAPER_ALIAS = "ZombieNewspaper";
    
    private static final String ARCADE_PROP_PAM = "768/FULL/EFFECTS/80S_ARCADE_CABINET/80S_ARCADE_CABINET.PAM";
    private static final float DEFAULT_ARCADE_DEATH_DURATION = 0.6f;
    private static final float ARCADE_SCALE = 0.6f;
    
    
    private static final String BUTTER_ELEMENT_NAME = "butter";
    
    
    private static final float SHIELD_SCALE = 0.52f;
    private static final float SHIELD_OFFSET_X = 22f;
    private static final float SHIELD_OFFSET_Y = 4f;
    
    private static final float SHIELD_ON_DURATION = 0.5f;
    private static final float WATER_RIPPLE_SCALE = 0.70f;
    private static final float DEFAULT_RIPPLE_EXIT_DURATION = 0.6f;
    private static final String SWASHBUCKLER_WATER_SPLASH_PAM =
            "768/FULL/EFFECTS/WATER_SPLASH/WATER_SPLASH.PAM";
    private static final String SWASHBUCKLER_WATER_SPLASH_STATE = "water_splash_01";
    private static final float SWASHBUCKLER_FAILURE_DURATION = 1.0f;
    private static final float SWASHBUCKLER_SPLASH_DURATION = 0.8f;
    private static final float SWASHBUCKLER_SPLASH_OFFSET_X = 215f;
    private static final float SWASHBUCKLER_SPLASH_OFFSET_Y = -300f;
    private static final String WATER_GARGANTUAR_RIPPLE_PAM =
            "768/FULL/BACKGROUNDS/WATER_GARGANTUAR_RIPPLE/WATER_GARGANTUAR_RIPPLE.PAM";
    private static final String WATER_IMP_RIPPLE_PAM =
            "768/FULL/BACKGROUNDS/WATER_IMP_RIPPLE/WATER_IMP_RIPPLE.PAM";
    private static final String WATER_ZOMBIE_RIPPLE_PAM =
            "768/FULL/BACKGROUNDS/WATER_ZOMBIE_RIPPLE/WATER_ZOMBIE_RIPPLE.PAM";

    private static final float RIPPLE_OFFSET_X = -50f;
    private static final float RIPPLE_OFFSET_Y = -18f;

    
    
    
    
    
    private static final String ZOMBIE_BEACH_SNORKEL_ALIAS = "ZombieBeachSnorkel";
    private static final float SWIMMER_SUBMERGE_OFFSET_Y = 58f;
    private static final float DEFAULT_SUBMERGE_OFFSET_Y = 20f;

    
    
    
    
    private static final Color FREEZE_TINT = new Color(0.55f, 0.78f, 1f, 1f);
    private static final Color FROZEN_TINT = new Color(0.42f, 0.68f, 1f, 1f);
    private static final Color FREEZE_FALLBACK_TINT = new Color(0.35f, 0.55f, 0.85f, 1f);

    
    
    private static final Color HYPNO_TINT = new Color(0.85f, 0.55f, 1f, 1f);
    private static final Color HYPNO_FALLBACK_TINT = new Color(0.65f, 0.4f, 0.85f, 1f);

    
    
    
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
        
        final String shockPath;
        final float shockDuration;
        
        final String ashPath;
        final float ashDuration;
        final boolean barrelBroken;
        final boolean swashbucklerWaterDeath;
        
        
        final float hoverLift;
        
        
        
        final PushableStructure barrelStructure;
        float time;
        float barrelRollTime;
        float barrelDeathTime;

        DyingZombie(String alias, Position position, boolean facingRight,
                    float duration, String shockPath, float shockDuration,
                    String ashPath, float ashDuration, boolean barrelBroken,
                    boolean swashbucklerWaterDeath, float hoverLift,
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
            this.swashbucklerWaterDeath = swashbucklerWaterDeath;
            this.hoverLift = hoverLift;
            this.barrelStructure = barrelStructure;
            this.barrelRollTime = 0f;
            this.barrelDeathTime = -1f;
        }
    }

    private final GameScreen screen;
    private final ZombieArmorMask armorMask;

    private final Map<Zombie, Float> zombieSpawnEffects = new IdentityHashMap<>();
    private final Map<Zombie, Boolean> gargantuarShakeTriggered = new IdentityHashMap<>();
    private final Map<Zombie, Float> zombieAnimTimes = new IdentityHashMap<>();
    private final Map<Zombie, ZombieWaterRipple> zombieWaterRipples = new IdentityHashMap<>();
    private final Map<Zombie, Boolean> zombieGyratingLast = new IdentityHashMap<>();
    
    
    
    private final Map<Zombie, Boolean> zombieEatingLast = new IdentityHashMap<>();
    private final Map<Zombie, Boolean> zombieHypnotizedLast = new IdentityHashMap<>();
    private final Map<Zombie, String> zombieActionStateLast = new IdentityHashMap<>();
    private final Map<Zombie, Integer> zombieLastHp = new IdentityHashMap<>();
    private final Map<Zombie, Float> pianoDamageAnimTimes = new IdentityHashMap<>();
    private final Map<Zombie, Float> arcadeDeathAnimTimes = new IdentityHashMap<>();
    private final Map<Zombie, ZombieWaterRipple> arcadeWaterRipples = new IdentityHashMap<>();
    private final Map<Zombie, Float> barrelDeathAnimTimes = new IdentityHashMap<>();
    private final Map<Zombie, Float> shieldAnimTimes = new IdentityHashMap<>();
    private final List<DyingZombie> dyingZombies = new ArrayList<>();

    ZombieRenderer(GameScreen screen) {
        this.screen = screen;
        this.armorMask = new ZombieArmorMask();
    }

    private static boolean isGargantuarAlias(String alias) {
        return alias != null && alias.toLowerCase().contains("gargantuar");
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

            
            
            if (isGargantuarAlias(zombie.getAlias())
                    && !gargantuarShakeTriggered.getOrDefault(zombie, false)) {
                gargantuarShakeTriggered.put(zombie, true);
                screen.triggerScreenShake(2.2f, 0.16f);
            }
            boolean frozenInIce = FrostbiteFreezing.isFrozenInIce(screen.session, zombie);
            float t = zombieAnimTimes.getOrDefault(zombie, 0f);
            if (!frozenInIce) {
                t += delta;
                zombieAnimTimes.put(zombie, t);
            }
            Position p = zombie.getPosition();
            float x = GameScreen.BOARD_X + (float) p.x() * boardTileWidth;
            float y = screen.cellY(p.y());
            float hoverLift = (float) zombie.getHoverHeight() * boardTileHeight;
            float zombieOffsetY = y + 40f + hoverLift;

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
                boolean futureSpawn = isFutureSeason();
                String spawnPam = futureSpawn ? FUTURE_SPAWN_EFFECT_PAM : ZOMBIE_SPAWN_EFFECT_PAM;
                String spawnState = futureSpawn ? FUTURE_SPAWN_EFFECT_STATE : "animation";
                float spawnDuration = AnimationFactory.exactClipDurationForPath(spawnPam, spawnState);
                if (spawnDuration <= 0f) spawnDuration = ZOMBIE_SPAWN_EFFECT_DURATION;
                float effectTime = zombieSpawnEffects.get(zombie) + delta;
                if (effectTime < spawnDuration) {
                    zombieSpawnEffects.put(zombie, effectTime);
                    int spawnRow = (int) Math.round(p.y());
                    screen.queueRowDraw(spawnRow, () -> screen.drawPam(
                            spawnPam,
                            spawnState,
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
                
                
                
                preferred = zombie.getActionAnimationState();
                if (!preferred.equals(zombieActionStateLast.get(zombie))) {
                    if ("power_up".equals(preferred) || "laser_start".equals(preferred)) {
                        AudioManager.get().playSound(AudioEnum.SFX_LASER_SHOT);
                    } else if (ZombieStunProfile.CLIP_START.equals(preferred)) {
                        AudioManager.get().playSound(AudioEnum.SFX_ELECTRIC_SHOCK);
                    }
                }
                zombieActionStateLast.put(zombie, preferred);
            } else if (isBarrelPusher) {
                
                
                
                
                preferred = barrelBroken ? "walk2" : "walk";
            } else if (zombie.getZombieState() == ZombieState.EATING) {
                
                
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

            
            
            
            Map<String, Boolean> elementVisibility = armorVisibility != null
                    ? armorVisibility : new java.util.HashMap<>();
            elementVisibility.put(BUTTER_ELEMENT_NAME, zombie.getStatus() == Zombie.Status.BUTTER);

            
            
            
            boolean chilled = zombie.getStatus() == Zombie.Status.FREEZE
                    || zombie.getStatus() == Zombie.Status.FROZEN;
            ZombieVisualArgs visualArgs = new ZombieVisualArgs(t, x, zombieDrawY,
                    zombieOffsetY,
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
        shieldAnimTimes.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
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
            drawZombieParrot(zombie, t, delta, x, zombieDrawY, zombie.isFacingRight());
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

        drawFutureGargantuarLaserBase(zombie, x, zombieDrawY, zombie.isFacingRight());
        drawZombieShield(zombie, delta, x, zombieDrawY, zombie.isFacingRight());

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
            boolean swashbucklerWaterDeath = zombie.diedFromSwashbucklerWater();
            String deathState = barrelBroken ? "die2" : "die";
            float normalDeathDuration = screen.pam().resolveClipDuration(zombie.getAlias(), deathState);
            if (normalDeathDuration <= 0f) normalDeathDuration = DEATH_ANIM_DURATION;

            if (swashbucklerWaterDeath) {
                
                
                
                normalDeathDuration = SWASHBUCKLER_FAILURE_DURATION + SWASHBUCKLER_SPLASH_DURATION;
            }

            float deathSequenceDuration = shockDuration;
            if (shockPath != null) {
                deathSequenceDuration += ashPath != null ? ashDuration : normalDeathDuration;
            } else {
                deathSequenceDuration = ashPath != null ? ashDuration : normalDeathDuration;
            }

            dyingZombies.add(new DyingZombie(
                    zombie.getAlias(), zombie.getPosition(), zombie.isFacingRight(),
                    deathSequenceDuration, shockPath, shockDuration,
                    ashPath, ashDuration, barrelBroken, swashbucklerWaterDeath,
                    (float) zombie.getHoverHeight() * screen.getBoardTileHeight(),
                    barrelStructure));
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
            float zombieOffsetY = y + 40f + dz.hoverLift;
            int row = (int) dz.position.y();

            if (dz.swashbucklerWaterDeath) {
                float failureTime = Math.min(dz.time, SWASHBUCKLER_FAILURE_DURATION);
                if (dz.time < SWASHBUCKLER_FAILURE_DURATION) {
                    screen.queueRowDraw(row, () -> screen.drawPam(
                            ZombieAnimationRegistry.pathFor(dz.alias, screen.seasonFolder),
                            "swing failure", failureTime,
                            x - 10f, zombieOffsetY, 0.52f, dz.facingRight));
                } else if (dz.time < SWASHBUCKLER_FAILURE_DURATION + SWASHBUCKLER_SPLASH_DURATION) {
                    float splashTime = Math.min(
                            dz.time - SWASHBUCKLER_FAILURE_DURATION,
                            SWASHBUCKLER_SPLASH_DURATION);
                    screen.queueRowDraw(row, () -> screen.drawPam(
                            SWASHBUCKLER_WATER_SPLASH_PAM,
                            SWASHBUCKLER_WATER_SPLASH_STATE,
                            splashTime,
                            x + SWASHBUCKLER_SPLASH_OFFSET_X,
                            zombieOffsetY + SWASHBUCKLER_SPLASH_OFFSET_Y,
                            1.0f,
                            false));
                }
                continue;
            }

            boolean inShockPhase = dz.shockPath != null && dz.time < dz.shockDuration;
            if (inShockPhase) {
                float shockTime = Math.min(dz.time, dz.shockDuration);
                screen.queueRowDraw(row, () -> screen.drawPam(dz.shockPath, ZombieShockAnimationRegistry.SHOCK_STATE,
                        shockTime, x - 10f, zombieOffsetY, 0.52f, dz.facingRight));
                continue;
            }

            float postShockTime = dz.shockPath == null ? dz.time : dz.time - dz.shockDuration;

            if (dz.ashPath != null) {
                
                float ashTime = Math.min(postShockTime, dz.ashDuration);
                screen.queueRowDraw(row, () -> screen.drawPam(dz.ashPath, ZombieAshAnimationRegistry.ASH_STATE,
                        ashTime, x - 10f, zombieOffsetY, 0.52f, dz.facingRight));
                continue;
            }

            
            
            
            
            
            
            boolean deathAnimComplete = dz.time >= dz.duration;
            if (!deathAnimComplete) {
                String path = ZombieAnimationRegistry.pathFor(dz.alias, screen.seasonFolder);

                
                
                float normalDeathTime = Math.max(0f, postShockTime);
                float normalDeathDuration = Math.max(0.001f, dz.duration - dz.shockDuration);
                float fallProgress = Math.min(1f, normalDeathTime / (normalDeathDuration * 0.5f));
                float fallEase = 1f - (1f - fallProgress) * (1f - fallProgress);
                float particleDrop = 24f * fallEase;

                ZombotanyArt.Head plantHead = ZombotanyArt.headFor(dz.alias);
                if (plantHead == null && AnimationFactory.hasExactClip(path, "particles")) {
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

    private final Map<Zombie, String> parrotLastState = new IdentityHashMap<>();
    private final Map<Zombie, Float> parrotStateElapsed = new IdentityHashMap<>();
    private static final java.util.Set<String> PARROT_RAID_STATES = java.util.Set.of(
            "parrot_releas", "fly", "carry", "fly back", "die");
    private static final java.util.Set<String> PARROT_OBJECT_STATES = java.util.Set.of(
            "fly", "carry", "fly back", "die");
    private static final float PARROT_ACTION_SPEED_FACTOR = 1.8f;
    
    
    private static final float PARROT_RAID_DISTANCE = 220f;

    
    private void drawZombieParrot(Zombie zombie, float t, float delta, float x, float zombieDrawY, boolean facingRight) {
        if (!ZOMBIE_PIRATE_CAPTAIN_ALIAS.equals(zombie.getAlias())) return;

        String state = zombie.getActionAnimationState();
        if (state == null || !PARROT_RAID_STATES.contains(state)) {
            parrotLastState.remove(zombie);
            parrotStateElapsed.remove(zombie);
            return;
        }

        boolean sameState = state.equals(parrotLastState.put(zombie, state));
        float elapsed = (sameState ? parrotStateElapsed.getOrDefault(zombie, 0f) : 0f) + delta;
        parrotStateElapsed.put(zombie, elapsed);

        if (!PARROT_OBJECT_STATES.contains(state)) return;

        float duration = AnimationFactory.exactClipDurationForPath(CAPTAIN_PARROT_PAM, state);
        float actionElapsed = elapsed / PARROT_ACTION_SPEED_FACTOR;
        float parrotTime = duration > 0f ? Math.min(actionElapsed, duration) : actionElapsed;
        float progress = duration > 0f
                ? Math.min(1f, elapsed / (duration * PARROT_ACTION_SPEED_FACTOR))
                : Math.min(1f, elapsed / 1.0f);

        
        
        float travel = switch (state) {
            case "fly" -> PARROT_RAID_DISTANCE * progress;
            case "carry" -> PARROT_RAID_DISTANCE;
            case "fly back" -> PARROT_RAID_DISTANCE * (1f - progress);
            default -> 0f;
        };

        
        
        
        boolean drawFacingRight = "carry".equals(state) != facingRight;

        float direction = facingRight ? 1f : -1f;
        float parrotX = x + direction * (PARROT_OFFSET_X + travel);
        float parrotY = zombieDrawY + PARROT_OFFSET_Y;
        screen.pam().drawPamExact(CAPTAIN_PARROT_PAM, state, parrotTime, parrotX, parrotY, PARROT_SCALE, drawFacingRight);
    }

    
    private void drawZombieBarrel(Zombie zombie, float delta, float boardTileWidth) {
        if (!ZOMBIE_BARREL_ROLLER_ALIAS.equals(zombie.getAlias())) return;

        
        
        
        
    }

    
    
    
    
    
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

    private boolean isFutureSeason() {
        return screen.session != null && screen.session.getLevel() != null
                && screen.session.getLevel().getSeason() != null
                && "Future".equalsIgnoreCase(screen.session.getLevel().getSeason().getName());
    }

    private void drawFutureGargantuarLaserBase(Zombie zombie, float x, float zombieDrawY,
                                               boolean facingRight) {
        String state = zombie.getActionAnimationState();
        if (state == null || !state.startsWith("laser_")) return;
        if (!AnimationFactory.hasExactClip(FUTURE_GARGANTUAR_BASE_PAM, state)) return;

        float duration = AnimationFactory.exactClipDurationForPath(FUTURE_GARGANTUAR_BASE_PAM, state);
        float time = (float) zombie.getActionAnimationElapsed();
        if (duration > 0f) {
            time = zombie.isActionAnimationLoop() ? time % duration : Math.min(time, duration);
        }
        screen.pam().drawPamExact(FUTURE_GARGANTUAR_BASE_PAM, state, time,
                x + FUTURE_GARGANTUAR_BASE_OFFSET_X, zombieDrawY + FUTURE_GARGANTUAR_BASE_OFFSET_Y,
                FUTURE_GARGANTUAR_BASE_SCALE, facingRight);
    }

    private void drawZombieShield(Zombie zombie, float delta, float x, float zombieDrawY,
                                  boolean facingRight) {
        if (!zombie.hasShield()) {
            shieldAnimTimes.remove(zombie);
            return;
        }

        float elapsed = shieldAnimTimes.getOrDefault(zombie, 0f) + delta;
        shieldAnimTimes.put(zombie, elapsed);

        String state;
        float time;
        if (elapsed < SHIELD_ON_DURATION) {
            state = ProtectorShield.CLIP_ON;
            time = elapsed;
        } else if (zombie.getShieldHitFlash() > 0) {
            state = zombie.getShieldFraction() <= ProtectorShield.HEAVY_DAMAGE_FRACTION
                    ? ProtectorShield.CLIP_DAMAGE_2 : ProtectorShield.CLIP_DAMAGE_1;
            time = elapsed;
        } else {
            state = ProtectorShield.CLIP_IDLE;
            time = elapsed;
        }

        float duration = AnimationFactory.exactClipDurationForPath(ProtectorShield.PAM, state);
        if (duration > 0f) time %= duration;

        screen.pam().drawPamExact(ProtectorShield.PAM, state, time,
                x + SHIELD_OFFSET_X, zombieDrawY + SHIELD_OFFSET_Y, SHIELD_SCALE, facingRight);
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