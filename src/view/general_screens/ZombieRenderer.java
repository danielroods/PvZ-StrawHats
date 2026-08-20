package view.general_screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

import controller.assets.GameAssetManager;
import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieState;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.match.main.season.travellog.egypt.SandStorm;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.TileType;

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
    private static final String HYPNO_ZOMBIE_EFFECT_PAM =
            "768/INITIAL/EFFECTS/HYPNO_ZOMBIE_EFFECT/HYPNO_ZOMBIE_EFFECT.PAM";

    private static final String ZOMBIE_BEACH_FISHERMAN_ALIAS = "ZombieBeachFisherman";
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
        float time;

        DyingZombie(String alias, Position position, boolean facingRight, float duration) {
            this.alias = alias;
            this.position = position;
            this.facingRight = facingRight;
            this.duration = duration;
        }
    }

    private final GameScreen screen;
    private final ZombieArmorMask armorMask;

    private final Map<Zombie, Float> zombieSpawnEffects = new IdentityHashMap<>();
    private final Map<Zombie, Float> zombieAnimTimes = new IdentityHashMap<>();
    private final Map<Zombie, ZombieWaterRipple> zombieWaterRipples = new IdentityHashMap<>();
    private final List<DyingZombie> dyingZombies = new ArrayList<>();

    ZombieRenderer(GameScreen screen) {
        this.screen = screen;
        this.armorMask = new ZombieArmorMask();
    }

    void drawZombies(float delta, float bw, float bh) {
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        List<Zombie> zombies = new ArrayList<>(screen.session.getZombies());
        zombies.sort(Comparator.comparingDouble(z -> z.getPosition() == null ? 0 : z.getPosition().y()));
        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.getPosition() == null) continue;
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
            float rippleCenterX = x + boardTileWidth * 0.5f;
            float rippleCenterY = y + 15f;

            float clipWaterY = y + 15f;

            float rippleDrawX = (x + boardTileWidth * 0.5f) + RIPPLE_OFFSET_X;
            float rippleDrawY = clipWaterY + RIPPLE_OFFSET_Y;

            float zombieDrawY = submerged ? zombieOffsetY - 20f : zombieOffsetY;

            if (zombie.isFromNecromancy()) {
                zombieSpawnEffects.put(zombie, 0f);
                zombie.setFromNecromancy(false);
            }

            if (zombie.isHypnotized()) {
                screen.drawPam(HYPNO_ZOMBIE_EFFECT_PAM, "animation", t, x + 20f, zombieOffsetY,
                        HYPNO_OVERLAY_SCALE, false);
            }

            if (zombieSpawnEffects.containsKey(zombie)) {
                float effectTime = zombieSpawnEffects.get(zombie) + delta;
                if (effectTime < ZOMBIE_SPAWN_EFFECT_DURATION) {
                    zombieSpawnEffects.put(zombie, effectTime);
                    screen.drawPam(
                            ZOMBIE_SPAWN_EFFECT_PAM,
                            "animation",
                            effectTime,
                            x - 10f,
                            zombieOffsetY,
                            0.52f,
                            zombie.isFacingRight()
                    );
                } else {
                    zombieSpawnEffects.remove(zombie);
                }
            }

            boolean hasActionOverride = zombie.getZombieState() != ZombieState.DEAD
                    && zombie.getActionAnimationState() != null;
            String preferred;
            if (zombie.getZombieState() == ZombieState.DEAD) {
                preferred = "die";
            } else if (hasActionOverride) {
                preferred = zombie.getActionAnimationState();
            } else if (zombie.getZombieState() == ZombieState.EATING) {
                preferred = "ZombieBeachFisherman".equals(zombie.getAlias()) ? "toss" : "eat";
            } else {
                preferred = "walk";
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
            } else if (("walk".equals(preferred) || "eat".equals(preferred) || "toss".equals(preferred)) && path != null) {
                float duration = screen.pam().resolveClipDuration(zombie.getAlias(), preferred);
                if (duration > 0f) {
                    animationTime = t % duration;
                }
            }
            Map<String, Boolean> armorVisibility = armorMask.basicZombieArmorVisibility(zombie);
            boolean waterClipActive = submerged && pushWaterClip(clipWaterY);
            try {
                boolean pamDrawn = armorVisibility != null
                        ? screen.drawPam(path, preferred, animationTime, x - 10f, zombieDrawY, 0.52f, zombie.isFacingRight(), armorVisibility)
                        : screen.drawPam(path, preferred, animationTime, x - 10f, zombieDrawY, 0.52f, zombie.isFacingRight());
                if (!pamDrawn) {
                    TextureRegion region = GameAssetManager.get().getZombieRegion(zombie.getAlias());
                    screen.drawEntity(region, x, zombieDrawY, boardTileWidth, boardTileHeight,
                            new Color(0.55f, 0.5f, 0.45f, 1f), GameScreenGraphics.initials(zombie.getAlias()));
                }
            } finally {
                if (waterClipActive) popWaterClip();
            }

            if (waterRipple != null && (waterRipple.inWater || waterRipple.exiting)) {
                drawWaterRipple(zombie.getAlias(), rippleDrawX, rippleDrawY,
                        zombie.isFacingRight(), waterRipple);
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
        zombieAnimTimes.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        zombieSpawnEffects.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
        zombieWaterRipples.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
    }

    void trackZombieDeaths(List<Zombie> aliveBeforeTick) {
        List<Zombie> stillAlive = screen.session.getZombies();
        for (Zombie zombie : aliveBeforeTick) {
            if (stillAlive.contains(zombie)) continue;
            if (zombie.getPosition() == null) continue;
            if (zombie.getZombieState() != ZombieState.DEAD) continue;
            float dieDuration = screen.pam().resolveClipDuration(zombie.getAlias(), "die");
            if (dieDuration <= 0f) dieDuration = DEATH_ANIM_DURATION;
            dyingZombies.add(new DyingZombie(zombie.getAlias(), zombie.getPosition(), zombie.isFacingRight(), dieDuration));
            zombieAnimTimes.remove(zombie);
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

            String path = ZombieAnimationRegistry.pathFor(dz.alias, screen.seasonFolder);

            float fallProgress = Math.min(1f, dz.time / (dz.duration * 0.5f));
            float fallEase = 1f - (1f - fallProgress) * (1f - fallProgress);
            float particleDrop = 24f * fallEase;

            screen.drawPam(path, "particles", dz.time, x - 10f, zombieOffsetY - particleDrop, 0.52f, dz.facingRight);
            screen.drawPam(path, "die", Math.min(dz.time, dz.duration), x - 10f, zombieOffsetY, 0.52f, dz.facingRight);
        }
        dyingZombies.removeIf(dz -> dz.time > dz.duration);
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
        ZombieWaterRipple ripple = zombieWaterRipples.computeIfAbsent(zombie, z -> new ZombieWaterRipple());
        boolean inWaterNow = isZombieOnFloodedTile(zombie);
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
            float duration = AnimationFactory.clipDurationForPath(ripplePamFor(zombie.getAlias()), "ripple_exit");
            if (duration <= 0f) duration = DEFAULT_RIPPLE_EXIT_DURATION;
            if (ripple.exitElapsed > duration) {
                ripple.exiting = false;
            }
        }
        return ripple;
    }

    private boolean isZombieOnFloodedTile(Zombie zombie) {
        if (zombie.getPosition() == null || screen.session == null) return false;
        Environment environment = screen.session.getEnvironment();
        if (environment == null) return false;
        int row = (int) Math.round(zombie.getPosition().y());
        int col = (int) Math.floor(zombie.getPosition().x());
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
}