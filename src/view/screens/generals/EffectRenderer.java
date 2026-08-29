package view.screens.generals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Gdx;

import controller.assets.ProjectileEffectAssets;
import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.GrapeshotProjectile;
import model.projectile.Projectile;
import model.projectile.zombie_projectile.GargantuarImpProjectile;
import model.projectile.zombie_projectile.OctopusProjectile;
import model.projectile.zombie_projectile.SnowballProjectile;
import model.projectile.zombie_projectile.BoneProjectile;
import model.projectile.zombie_projectile.ZombiePeaProjectile;
import model.projectile.zombie_projectile.ZombieProjectile;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

class EffectRenderer {

    static final float PROJECTILE_PAM_SCALE = 0.35f;

    static final float POTATO_MINE_EXPLOSION_SCALE_MULTIPLIER = 1.8f;
    static final float POTATO_MINE_EXPLOSION_OFFSET_X = 0.43f;
    static final float POTATO_MINE_EXPLOSION_OFFSET_Y = 1f;
    private static final String POTATO_MINE_EXPLOSION_PAM =
            "768/INITIAL/EFFECTS/POTATOMINE_EXPLOSION/POTATOMINE_EXPLOSION.PAM";
    private static final String PRIMAL_POTATO_MINE_EXPLOSION_PAM =
            "768/INITIAL/EFFECTS/PRIMAL_POTATOMINE_EXPLOSION/PRIMAL_POTATOMINE_EXPLOSION.PAM";
    private static final String CHERRY_BOMB_EXPLOSION_PAM =
            "768/FULL/EFFECTS/CHERRYBOMB_EXPLOSION_TOP/CHERRYBOMB_EXPLOSION_TOP.PAM";
    private static final float CHERRY_BOMB_EXPLOSION_SCALE = 0.55f;
    private static final float CHERRY_BOMB_EXPLOSION_OFFSET_X = 0.43f;
    private static final float CHERRY_BOMB_EXPLOSION_OFFSET_Y = 1.3f;

    private static final String GENERIC_EXPLOSION_BACK_PAM =
            "768/INITIAL/EFFECTS/GENERIC_EXPLOSION_BACK/GENERIC_EXPLOSION_BACK.PAM";
    private static final String GENERIC_EXPLOSION_FRONT_PAM =
            "768/INITIAL/EFFECTS/GENERIC_EXPLOSION_FRONT/GENERIC_EXPLOSION_FRONT.PAM";
    private static final String GENERIC_EXPLOSION_STATE = "animation2";
    private static final float GENERIC_EXPLOSION_SCALE = 0.78f;
    private static final float GENERIC_EXPLOSION_OFFSET_X = 30f;
    private static final float GENERIC_EXPLOSION_OFFSET_Y = 40f;

    private static final float IMPACT_EFFECT_DURATION = 0.35f;
    private static final float GRAPE_PROJECTILE_SCALE_FACTOR = 1.6f;
    private static final String GRAPESHOT = "Grapeshot";
    private static final float STATIC_PROJECTILE_SCALE = 0.80f;
    private static final String ZOMBIE_PEA_PAM =
            "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM";
    private static final String ZOMBIE_PEA_SPLAT_PAM =
            "768/INITIAL/EFFECTS/SPLAT_PEA/SPLAT_PEA.PAM";
    private static final String SCORCHED_EARTH_TILE_PAM =
            "768/FULL/EFFECTS/SCORCHED_EARTH_TILE/SCORCHED_EARTH_TILE.PAM";
    private static final float SCORCHED_TILE_SCALE = 0.65f;
    private static final float SCORCHED_TILE_OFFSET_X = 0.45f;
    private static final float SCORCHED_TILE_OFFSET_Y = 0.50f;
    private static final float SCORCHED_TILE_LOCK_SECONDS = 10.0f;
    private static final String SNOWBALL_PROJECTILE_TEXTURE = "assets/images/zombies/zombie_hunter_snowball_projectile.png";
    private static final String BONE_PROJECTILE_TEXTURE = "assets/images/zombies/zombie_egypt_tombraiser_31x62.png";
    private static final String OCTOPUS_PROJECTILE_PAM =
            "768/FULL/EFFECTS/ZOMBIE_OCTOPUS_PROJECTILE/ZOMBIE_OCTOPUS_PROJECTILE.PAM";

    private static final class TimedPamEffect {
        final String path;
        final String state;
        final boolean loop;
        final boolean staticImage;
        final Position position;
        final float duration;
        final float scale;
        float time;

        TimedPamEffect(String path, String state, boolean loop, boolean staticImage,
                       Position position, float duration, float scale) {
            this.path = path;
            this.state = state;
            this.loop = loop;
            this.staticImage = staticImage;
            this.position = position;
            this.duration = duration;
            this.scale = scale;
        }
    }

    private static final class PlacedPamEffect {
        final String path;
        final String state;
        final Position position;
        final float duration;
        final float scale;
        final float offsetX;
        final float offsetY;
        float time;

        PlacedPamEffect(String path, String state, Position position, float duration,
                        float scale, float offsetX, float offsetY) {
            this.path = path;
            this.state = state;
            this.position = position;
            this.duration = duration;
            this.scale = scale;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    // Ice-shroom: persists as a normal plant (idle/attack/plantfood, like any other MELEE
    // plant - see MeleeStrategy's PlantTag.ICE handling and Plants.json), so unlike the
    // exploding plants above it needs its own long-lived, per-plant tracked state: the
    // 3x3 (9-tile) frost patch that stays on the ground the whole time it's alive.
    private static final String ICE_SHROOM = "Ice-shroom";

    /** Tracks one Ice-shroom's 3x3 ground frost patch through spawn -> animation_loop -> end. */
    private static final class ScorchedTileEffect {
        final Position position;
        float age;
        String phase = "animation";
        float phaseTime;

        ScorchedTileEffect(Position position) {
            this.position = position;
        }
    }

    // Hot Potato: melting the ice block/frozen plant it's planted on plays a three-phase
    // ground puddle - see addHotPotatoMeltEffect/drawHotPotatoMeltEffects.
    private static final String HOTPOTATO_ICEBLOCK_PUDDLE_PAM =
            "768/FULL/EFFECTS/HOTPOTATO_ICEBLOCK_PUDDLE/HOTPOTATO_ICEBLOCK_PUDDLE.PAM";
    private static final float HOTPOTATO_PUDDLE_HOLD_SECONDS = 3.0f;
    private static final float HOTPOTATO_PUDDLE_SCALE = 0.65f;
    private static final float HOTPOTATO_PUDDLE_OFFSET_X = 0.45f;
    private static final float HOTPOTATO_PUDDLE_OFFSET_Y = 0.50f;

    /** Tracks one Hot Potato melt puddle through animation (intro) -> animation2 (hold) -> animation3 (outro). */
    private static final class HotPotatoMeltEffect {
        final Position position;
        String phase = "animation";
        float phaseTime;

        HotPotatoMeltEffect(Position position) {
            this.position = position;
        }
    }

    private static final class IceShroomZone {
        final Plant plant;
        final List<Position> tiles;
        String phase = "spawn";
        float phaseTime;

        IceShroomZone(Plant plant, List<Position> tiles) {
            this.plant = plant;
            this.tiles = tiles;
        }
    }

    private static final class ProjectileTrace {
        final String plantName;
        final boolean boosted;
        final int variant;
        final Position position;

        ProjectileTrace(String plantName, boolean boosted, int variant, Position position) {
            this.plantName = plantName;
            this.boosted = boosted;
            this.variant = variant;
            this.position = position;
        }
    }

    private final GameScreen screen;

    private final List<TimedPamEffect> explodingPlantEffects = new ArrayList<>();
    private final List<TimedPamEffect> impactEffects = new ArrayList<>();
    private final List<PlacedPamEffect> behindZombieEffects = new ArrayList<>();
    private final List<PlacedPamEffect> foregroundEffects = new ArrayList<>();
    private final List<ScorchedTileEffect> scorchedTileEffects = new ArrayList<>();
    private final List<HotPotatoMeltEffect> hotPotatoMeltEffects = new ArrayList<>();
    private final Map<Projectile, ProjectileTrace> projectileTraces = new IdentityHashMap<>();
    private final Map<Projectile, Float> projectileAnimTimes = new IdentityHashMap<>();
    private final Map<ZombieProjectile, Float> zombieProjectileAnimTimes = new IdentityHashMap<>();
    private final Map<String, Texture> zombieProjectileTextures = new java.util.HashMap<>();
    private final Map<ZombieProjectile, Position> zombieProjectileTraces = new IdentityHashMap<>();
    private final Map<Plant, Double> meleeLastCooldown = new IdentityHashMap<>();
    private final Map<Plant, Boolean> meleePlantFoodSeen = new IdentityHashMap<>();
    private final Map<Plant, Double> iceShroomLastCooldown = new IdentityHashMap<>();
    private final Map<Plant, Boolean> iceShroomPlantFoodSeen = new IdentityHashMap<>();
    private final List<IceShroomZone> iceShroomZones = new ArrayList<>();

    EffectRenderer(GameScreen screen) {
        this.screen = screen;
    }

    ProjectileEffectAssets.AssetEntry resolveExplosionEntry(String plantName) {
        ProjectileEffectAssets.Variant normal = ProjectileEffectAssets.Variant.NORMAL;
        List<ProjectileEffectAssets.AssetEntry> hitEntries =
                ProjectileEffectAssets.get(plantName, ProjectileEffectAssets.Kind.HIT, normal);
        if (!hitEntries.isEmpty()) {
            ProjectileEffectAssets.AssetEntry entry = hitEntries.get(0);
            if ("Potato Mine".equalsIgnoreCase(plantName)) {
                String exact = AnimationFactory.exactClipNameForPath(entry.path(), "animation");
                if (exact == null) {
                    exact = AnimationFactory.exactClipNameForPath(entry.path(), "animation2");
                }
                if (exact != null && !exact.equals(entry.state())) {
                    return new ProjectileEffectAssets.AssetEntry(
                            entry.path(), exact, entry.playMode(), entry.kind(), entry.variant(),
                            entry.scope(), entry.purpose());
                }
            }
            if ("Primal Potato Mine".equalsIgnoreCase(plantName)) {
                String exact = AnimationFactory.exactClipNameForPath(entry.path(), "animation3");
                if (exact != null && !exact.equals(entry.state())) {
                    return new ProjectileEffectAssets.AssetEntry(
                            entry.path(), exact, entry.playMode(), entry.kind(), entry.variant(),
                            entry.scope(), entry.purpose());
                }
            }
            if ("Cherry Bomb".equalsIgnoreCase(plantName)) {
                String exact = AnimationFactory.exactClipNameForPath(CHERRY_BOMB_EXPLOSION_PAM, "explosion3");
                if (exact != null) {
                    return new ProjectileEffectAssets.AssetEntry(
                            CHERRY_BOMB_EXPLOSION_PAM, exact,
                            ProjectileEffectAssets.PlayMode.ONCE,
                            ProjectileEffectAssets.Kind.HIT,
                            ProjectileEffectAssets.Variant.NORMAL,
                            ProjectileEffectAssets.Scope.SELF,
                            "Cherry Bomb main explosion");
                }
            }
            return entry;
        }
        if (!hitEntries.isEmpty()) return hitEntries.get(0);

        List<ProjectileEffectAssets.AssetEntry> effectEntries =
                ProjectileEffectAssets.get(plantName, ProjectileEffectAssets.Kind.EFFECT, normal);
        return effectEntries.isEmpty() ? null : effectEntries.get(0);
    }

    void addExplodingPlantEffect(ProjectileEffectAssets.AssetEntry entry, boolean loop,
                                 Position position, float duration) {
        explodingPlantEffects.add(new TimedPamEffect(entry.path(), entry.state(), loop,
                entry.isStaticImage(), position, duration,
                PROJECTILE_PAM_SCALE));
    }

    void addDoomExplosion(Position position, int stage) {
        if (position == null) return;
        int safeStage = Math.max(1, Math.min(3, stage));
        String state = "stage" + safeStage + "_explode";
        String path = AnimationFactory.pathForDisplayName("Doom-shroom");
        if (path == null) return;
        float duration = AnimationFactory.clipDurationForPath(path, state);
        if (duration <= 0f) duration = 0.7f;
        explodingPlantEffects.add(new TimedPamEffect(path, state, false, false,
                position, duration, PROJECTILE_PAM_SCALE));
    }

    void addExplodeONutExplosion(Position position) {
        if (position == null) return;
        Position at = new Position(position.x(), position.y());
        float back = AnimationFactory.exactClipDurationForPath(
                GENERIC_EXPLOSION_BACK_PAM, GENERIC_EXPLOSION_STATE);
        float front = AnimationFactory.exactClipDurationForPath(
                GENERIC_EXPLOSION_FRONT_PAM, GENERIC_EXPLOSION_STATE);
        if (back <= 0f) back = 3f;
        if (front <= 0f) front = 3f;
        behindZombieEffects.add(new PlacedPamEffect(
                GENERIC_EXPLOSION_BACK_PAM, GENERIC_EXPLOSION_STATE, at, back,
                GENERIC_EXPLOSION_SCALE, GENERIC_EXPLOSION_OFFSET_X, GENERIC_EXPLOSION_OFFSET_Y));
        foregroundEffects.add(new PlacedPamEffect(
                GENERIC_EXPLOSION_FRONT_PAM, GENERIC_EXPLOSION_STATE, at, front,
                GENERIC_EXPLOSION_SCALE, GENERIC_EXPLOSION_OFFSET_X, GENERIC_EXPLOSION_OFFSET_Y));
    }

    void addScorchedTileEffect(Position position) {
        if (position == null) return;
        scorchedTileEffects.add(new ScorchedTileEffect(
                new Position(position.x(), position.y())));
    }

    /** Starts the melting-ice-puddle sequence on Hot Potato's own tile (its ice block/frozen plant). */
    void addHotPotatoMeltEffect(Position position) {
        if (position == null) return;
        hotPotatoMeltEffects.add(new HotPotatoMeltEffect(
                new Position(position.x(), position.y())));
    }

    void addTorchwoodRowFireEffect(int row, float duration) {
        if (screen.session.getEnvironment() == null) return;
        final String path = "768/INITIAL/EFFECTS/FIREPEASHOOTER_FIRE/FIREPEASHOOTER_FIRE.PAM";
        for (int col = 0; col < screen.session.getEnvironment().getCols(); col++) {
            impactEffects.add(new TimedPamEffect(path, "idle", true, false,
                    new Position(col, row), duration, PROJECTILE_PAM_SCALE));
        }
    }

    void addJalapenoRowFireEffect(int row) {
        if (screen.session.getEnvironment() == null) return;

        final String path = "768/INITIAL/EFFECTS/JALAPENO_FIRE/JALAPENO_FIRE.PAM";
        final float duration = 0.90f;
        for (int col = 0; col < screen.session.getEnvironment().getCols(); col++) {
            impactEffects.add(new TimedPamEffect(
                    path, "idle2", false, false,
                    new Position(col+0.15f, row), duration, 0.565f));
        }
    }

    void addDeflectSparkEffect(Position position) {
        impactEffects.add(new TimedPamEffect(ZOMBIE_PEA_SPLAT_PAM, "animation", false,
                false, position, IMPACT_EFFECT_DURATION, PROJECTILE_PAM_SCALE));
    }

    void drawExplodingPlantEffects(float delta) {
        triggerIceShroomAttacks();
        triggerIceShroomPlantFood();
        updateIceShroomZones(delta);
        drawTimedEffects(explodingPlantEffects, delta);
        drawTimedEffects(impactEffects, delta);
        drawPlacedEffects(behindZombieEffects, delta);
        drawPlantFoodEffects();
        drawGarlicPlantFoodProjectiles();
    }

    void drawForegroundEffects(float delta) {
        drawPlacedEffects(foregroundEffects, delta);
    }

    private void drawPlacedEffects(List<PlacedPamEffect> effects, float delta) {
        if (effects.isEmpty()) return;
        float boardTileWidth = screen.getBoardTileWidth();
        for (PlacedPamEffect effect : effects) {
            effect.time += delta;
            float x = GameScreen.BOARD_X + (float) effect.position.x() * boardTileWidth
                    + effect.offsetX;
            float y = screen.cellY((int) effect.position.y()) + effect.offsetY;
            float time = effect.time;
            int row = (int) effect.position.y();
            screen.queueRowDraw(row, () -> screen.drawPam(effect.path, effect.state, time, x, y, effect.scale, false));
        }
        effects.removeIf(e -> e.time > e.duration);
    }

    private void drawPlantFoodEffects() {
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        for (Plant plant : screen.session.getPlants()) {
            if (plant == null || !plant.isPlantFoodActive() || plant.getPosition() == null) continue;
            List<ProjectileEffectAssets.AssetEntry> entries = ProjectileEffectAssets.get(
                    plant.getName(), ProjectileEffectAssets.Kind.EFFECT,
                    ProjectileEffectAssets.Variant.PLANT_FOOD);
            if (entries.isEmpty()) continue;

            float time = screen.plants().animTimeFor(plant);
            ProjectileEffectAssets.AssetEntry entry = entries.get(0);
            boolean loop = entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP;
            int row = (int) plant.getPosition().y();
            int fromCol = (int) plant.getPosition().x();
            int toCol = entry.scope() == ProjectileEffectAssets.Scope.ROW
                    ? screen.session.getEnvironment().getCols() - 1 : fromCol;

            for (int col = fromCol; col <= toCol; col++) {
                float x = GameScreen.BOARD_X + col * boardTileWidth + boardTileWidth * 0.3f;
                float y = screen.cellY(row) + boardTileHeight * 0.3f;
                screen.queueRowDraw(row, () -> {
                    if (entry.isStaticImage()) {
                        screen.assets().drawStaticEffect(entry.path(), x, y, PROJECTILE_PAM_SCALE);
                    } else {
                        screen.drawPam(entry.path(), entry.state(), time, x, y, PROJECTILE_PAM_SCALE, loop);
                    }
                });
            }
        }
    }

    /**
     * Renders the scorched-earth tile before plants are drawn.
     * GameScreen calls this as part of the board background layer.
     */
    void drawScorchedTileEffects(float delta) {
        if (scorchedTileEffects.isEmpty()) return;

        float tileW = screen.getBoardTileWidth();
        float tileH = screen.getBoardTileHeight();
        float spawnDuration = AnimationFactory.clipDurationForPath(
                SCORCHED_EARTH_TILE_PAM, "animation");
        float endDuration = AnimationFactory.clipDurationForPath(
                SCORCHED_EARTH_TILE_PAM, "animation3");
        if (spawnDuration <= 0f) spawnDuration = 0.35f;
        if (endDuration <= 0f) endDuration = 0.35f;

        for (ScorchedTileEffect effect : scorchedTileEffects) {
            effect.age += delta;
            effect.phaseTime += delta;

            if ("animation".equals(effect.phase) && effect.phaseTime >= spawnDuration) {
                effect.phase = "animation2";
                effect.phaseTime = 0f;
            }
            if (effect.age >= SCORCHED_TILE_LOCK_SECONDS
                    && !"animation3".equals(effect.phase)) {
                effect.phase = "animation3";
                effect.phaseTime = 0f;
            }

            float x = GameScreen.BOARD_X + (float) effect.position.x() * tileW
                    + tileW * SCORCHED_TILE_OFFSET_X;
            float y = screen.cellY((int) effect.position.y())
                    + tileH * SCORCHED_TILE_OFFSET_Y;
            boolean loop = "animation2".equals(effect.phase);
            float time = effect.phaseTime;
            if (loop) {
                float duration = AnimationFactory.clipDurationForPath(
                        SCORCHED_EARTH_TILE_PAM, "animation2");
                if (duration > 0f) time %= duration;
            }
            String phase = effect.phase;
            float finalTime = time;
            int row = (int) effect.position.y();
            screen.queueRowDraw(row, () -> screen.drawPam(SCORCHED_EARTH_TILE_PAM, phase, finalTime,
                    x, y, SCORCHED_TILE_SCALE, loop));
        }

        float finalEndDuration = endDuration;
        scorchedTileEffects.removeIf(e -> "animation3".equals(e.phase)
                && e.phaseTime >= finalEndDuration);
    }

    /**
     * Renders the Hot Potato melting-ice puddle before plants are drawn, so it sits
     * underneath the plant/ice block it's melting rather than on top of it.
     * GameScreen calls this as part of the board background layer, alongside
     * {@link #drawScorchedTileEffects}.
     */
    void drawHotPotatoMeltEffects(float delta) {
        if (hotPotatoMeltEffects.isEmpty()) return;

        float tileW = screen.getBoardTileWidth();
        float tileH = screen.getBoardTileHeight();
        float introDuration = AnimationFactory.clipDurationForPath(
                HOTPOTATO_ICEBLOCK_PUDDLE_PAM, "animation");
        float outroDuration = AnimationFactory.clipDurationForPath(
                HOTPOTATO_ICEBLOCK_PUDDLE_PAM, "animation3");
        if (introDuration <= 0f) introDuration = 0.5f;
        if (outroDuration <= 0f) outroDuration = 0.5f;

        for (HotPotatoMeltEffect effect : hotPotatoMeltEffects) {
            effect.phaseTime += delta;

            if ("animation".equals(effect.phase) && effect.phaseTime >= introDuration) {
                effect.phase = "animation2";
                effect.phaseTime = 0f;
            }
            if ("animation2".equals(effect.phase) && effect.phaseTime >= HOTPOTATO_PUDDLE_HOLD_SECONDS) {
                effect.phase = "animation3";
                effect.phaseTime = 0f;
            }

            float x = GameScreen.BOARD_X + (float) effect.position.x() * tileW
                    + tileW * HOTPOTATO_PUDDLE_OFFSET_X;
            float y = screen.cellY((int) effect.position.y())
                    + tileH * HOTPOTATO_PUDDLE_OFFSET_Y;
            boolean loop = "animation2".equals(effect.phase);
            float time = effect.phaseTime;
            if (loop) {
                float duration = AnimationFactory.clipDurationForPath(
                        HOTPOTATO_ICEBLOCK_PUDDLE_PAM, "animation2");
                if (duration > 0f) time %= duration;
            }
            String phase = effect.phase;
            float finalTime = time;
            int row = (int) effect.position.y();
            screen.queueRowDraw(row, () -> screen.drawPam(HOTPOTATO_ICEBLOCK_PUDDLE_PAM, phase, finalTime,
                    x, y, HOTPOTATO_PUDDLE_SCALE, loop));
        }

        float finalOutroDuration = outroDuration;
        hotPotatoMeltEffects.removeIf(e -> "animation3".equals(e.phase)
                && e.phaseTime >= finalOutroDuration);
    }

    private void drawTimedEffects(List<TimedPamEffect> effects, float delta) {
        if (effects.isEmpty()) return;
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        for (TimedPamEffect effect : effects) {
            effect.time += delta;
            boolean potatoMineExplosion = POTATO_MINE_EXPLOSION_PAM.equals(effect.path)
                    || PRIMAL_POTATO_MINE_EXPLOSION_PAM.equals(effect.path);
            boolean cherryBombExplosion = CHERRY_BOMB_EXPLOSION_PAM.equals(effect.path);
            float offsetX = potatoMineExplosion
                    ? POTATO_MINE_EXPLOSION_OFFSET_X
                    : cherryBombExplosion ? CHERRY_BOMB_EXPLOSION_OFFSET_X : 0.30f;
            float offsetY = potatoMineExplosion
                    ? POTATO_MINE_EXPLOSION_OFFSET_Y
                    : cherryBombExplosion ? CHERRY_BOMB_EXPLOSION_OFFSET_Y : 0.30f;
            float drawScale = potatoMineExplosion
                    ? effect.scale * POTATO_MINE_EXPLOSION_SCALE_MULTIPLIER
                    : cherryBombExplosion ? CHERRY_BOMB_EXPLOSION_SCALE : effect.scale;
            float x = GameScreen.BOARD_X + (float) effect.position.x() * boardTileWidth
                    + boardTileWidth * offsetX;
            float y = screen.cellY((int) effect.position.y()) + boardTileHeight * offsetY
                    + boardTileWidth * 0.3f;
            int row = (int) effect.position.y();
            screen.queueRowDraw(row, () -> drawTimedEffectVisual(effect, x, y, drawScale, potatoMineExplosion));
        }
        effects.removeIf(e -> e.time > e.duration);
    }

    private void drawTimedEffectVisual(TimedPamEffect effect, float x, float y, float drawScale,
                                       boolean potatoMineExplosion) {
            if (effect.staticImage) {
                screen.assets().drawStaticEffect(effect.path, x, y, drawScale);
                screen.assets().drawStaticEffect(effect.path, x, y, effect.scale);
            } else {
                boolean drawn = screen.drawPam(effect.path, effect.state, effect.time,
                        x, y, drawScale, effect.loop);
                if (!drawn && potatoMineExplosion) {
                    String fallback;
                    if (PRIMAL_POTATO_MINE_EXPLOSION_PAM.equals(effect.path)) {
                        fallback = "animation";
                    } else {
                        fallback = "animation2".equals(effect.state) ? "animation" : "animation2";
                    }
                    screen.drawPam(effect.path, fallback, effect.time, x, y, drawScale, effect.loop);
                }
                screen.drawPam(effect.path, effect.state, effect.time, x, y, effect.scale, effect.loop);
            }
    }

    private void drawGarlicPlantFoodProjectiles() {
        final String GARLIC_PF_PAM =
                "768/INITIAL/EFFECTS/GARLIC_PROJECTILE/GARLIC_PROJECTILE.PAM";
        final float TRAVEL_SECONDS = 1.15f;

        float tileW = screen.getBoardTileWidth();
        float tileH = screen.getBoardTileHeight();
        int cols = screen.session.getEnvironment().getCols();

        for (Plant plant : screen.session.getPlants()) {
            if (plant == null || !plant.isAlive() || !plant.isGarlic()
                    || !plant.isPlantFoodActive() || plant.getPosition() == null) continue;

            double elapsed = plant.getVisualAnimationElapsed();
            double local = elapsed % TRAVEL_SECONDS;
            double progress = Math.max(0.0, Math.min(1.0, local / TRAVEL_SECONDS));

            float startCol = (float) plant.getPosition().x() + 0.30f;
            float endCol = cols - 0.15f;
            float col = startCol + (endCol - startCol) * (float) progress;

            float x = GameScreen.BOARD_X + col * tileW;
            int row = (int) Math.round(plant.getPosition().y());
            float y = screen.cellY(row) + tileH * 0.36f;
            float elapsedTime = (float) elapsed;

            screen.queueRowDraw(row, () -> screen.drawPam(GARLIC_PF_PAM, "animation", elapsedTime,
                    x, y, PROJECTILE_PAM_SCALE * 1.35f, false));
        }
    }

    private ProjectileEffectAssets.AssetEntry findIceShroomEntry(ProjectileEffectAssets.Kind kind,
                                                                 ProjectileEffectAssets.Variant variant,
                                                                 String state) {
        for (ProjectileEffectAssets.AssetEntry entry : ProjectileEffectAssets.get(ICE_SHROOM, kind, variant)) {
            if (state.equals(entry.state())) return entry;
        }
        return null;
    }

    /** The 9 board cells (3x3, clipped to the board) centered on an Ice-shroom's own tile. */
    private List<Position> iceShroomTiles(Position center) {
        List<Position> tiles = new ArrayList<>();
        if (screen.session.getEnvironment() == null) return tiles;
        int rows = screen.session.getEnvironment().getRows();
        int cols = screen.session.getEnvironment().getCols();
        int centerRow = (int) Math.round(center.y());
        int centerCol = (int) Math.round(center.x());
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int row = centerRow + dy;
                int col = centerCol + dx;
                if (row < 0 || row >= rows || col < 0 || col >= cols) continue;
                tiles.add(new Position(col, row));
            }
        }
        return tiles;
    }

    private void drawIceShroomTile(ProjectileEffectAssets.AssetEntry entry, float phaseTime, Position tile) {
        if (entry == null) return;
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        float x = GameScreen.BOARD_X + (float) tile.x() * boardTileWidth + boardTileWidth * 0.3f;
        float y = screen.cellY((int) tile.y()) + boardTileHeight * 0.3f;
        float time = phaseTime;
        if (entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP) {
            float clipDuration = AnimationFactory.clipDurationForPath(entry.path(), entry.state());
            if (clipDuration > 0f) time = time % clipDuration;
        }
        float finalTime = time;
        int row = (int) tile.y();
        screen.queueRowDraw(row, () -> screen.drawPam(entry.path(), entry.state(), finalTime, x, y, PROJECTILE_PAM_SCALE, false));
    }

    /**
     * Keeps each living Ice-shroom's 9-tile frost patch alive: "spawn" once when the tiles
     * first appear, then loops "animation_loop" for as long as the plant stays alive, then
     * plays "end" once and drops the zone after the plant is gone (eaten/removed).
     */
    private void updateIceShroomZones(float delta) {
        List<Plant> aliveIceShrooms = new ArrayList<>();
        for (Plant plant : screen.session.getPlants()) {
            if (plant != null && plant.isAlive() && plant.getPosition() != null
                    && ICE_SHROOM.equalsIgnoreCase(plant.getName())) {
                aliveIceShrooms.add(plant);
            }
        }
        for (Plant plant : aliveIceShrooms) {
            boolean tracked = false;
            for (IceShroomZone zone : iceShroomZones) {
                if (zone.plant == plant) {
                    tracked = true;
                    break;
                }
            }
            if (!tracked) {
                iceShroomZones.add(new IceShroomZone(plant, iceShroomTiles(plant.getPosition())));
            }
        }

        ProjectileEffectAssets.AssetEntry spawnEntry =
                findIceShroomEntry(ProjectileEffectAssets.Kind.EFFECT, ProjectileEffectAssets.Variant.NORMAL, "spawn");
        ProjectileEffectAssets.AssetEntry loopEntry = findIceShroomEntry(
                ProjectileEffectAssets.Kind.EFFECT, ProjectileEffectAssets.Variant.NORMAL, "animation_loop");
        ProjectileEffectAssets.AssetEntry endEntry =
                findIceShroomEntry(ProjectileEffectAssets.Kind.EFFECT, ProjectileEffectAssets.Variant.NORMAL, "end");

        float spawnDuration = 0.3f;
        if (spawnEntry != null) {
            float d = AnimationFactory.clipDurationForPath(spawnEntry.path(), spawnEntry.state());
            if (d > 0f) spawnDuration = d;
        }
        float endDuration;
        if (endEntry != null) {
            float d = AnimationFactory.clipDurationForPath(endEntry.path(), endEntry.state());
            if (d > 0f) endDuration = d;
            else {
                endDuration = 0.4f;
            }
        } else {
            endDuration = 0.4f;
        }

        for (IceShroomZone zone : iceShroomZones) {
            if (!aliveIceShrooms.contains(zone.plant) && !"end".equals(zone.phase)) {
                zone.phase = "end";
                zone.phaseTime = 0f;
            }
            zone.phaseTime += delta;
            if ("spawn".equals(zone.phase) && zone.phaseTime >= spawnDuration) {
                zone.phase = "animation_loop";
                zone.phaseTime = 0f;
            }
            ProjectileEffectAssets.AssetEntry entry = switch (zone.phase) {
                case "spawn" -> spawnEntry;
                case "end" -> endEntry;
                default -> loopEntry;
            };
            for (Position tile : zone.tiles) {
                drawIceShroomTile(entry, zone.phaseTime, tile);
            }
        }
        iceShroomZones.removeIf(zone -> "end".equals(zone.phase) && zone.phaseTime >= endDuration);
    }

    /**
     * Detects an Ice-shroom's melee-attack cooldown reset (same idiom as
     * {@link #drawMeleePlantProjectiles}) and plays the ice-swing effect on its own tile
     * plus the freeze fx on every zombie caught in its 3x3 attack zone.
     */
    private void triggerIceShroomAttacks() {
        for (Plant plant : screen.session.getPlants()) {
            if (plant == null || !plant.isAlive() || plant.getPosition() == null) continue;
            if (!ICE_SHROOM.equalsIgnoreCase(plant.getName())) continue;

            double cooldown = plant.getIntervalTimer();
            Double last = iceShroomLastCooldown.put(plant, cooldown);
            if (plant.isPlantFoodActive() || last == null || cooldown <= last + 0.05) continue;

            List<ProjectileEffectAssets.AssetEntry> swingEntries = ProjectileEffectAssets.get(
                    ICE_SHROOM, ProjectileEffectAssets.Kind.PROJECTILE, ProjectileEffectAssets.Variant.NORMAL);
            if (!swingEntries.isEmpty()) {
                ProjectileEffectAssets.AssetEntry swing = swingEntries.get(0);
                impactEffects.add(new TimedPamEffect(swing.path(), swing.state(),
                        swing.playMode() == ProjectileEffectAssets.PlayMode.LOOP,
                        swing.isStaticImage(), plant.getPosition(), 0.5f, PROJECTILE_PAM_SCALE));
            }

            ProjectileEffectAssets.AssetEntry hit = findIceShroomEntry(
                    ProjectileEffectAssets.Kind.EFFECT, ProjectileEffectAssets.Variant.NORMAL, "animation");
            if (hit != null) {
                Position center = plant.getPosition();
                for (Zombie zombie : screen.session.getZombies()) {
                    if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
                    Position zp = zombie.getPosition();
                    if (Math.abs(zp.x() - center.x()) <= 1 && Math.abs(zp.y() - center.y()) <= 1) {
                        impactEffects.add(new TimedPamEffect(hit.path(), hit.state(), false,
                                hit.isStaticImage(), zp, IMPACT_EFFECT_DURATION, PROJECTILE_PAM_SCALE));
                    }
                }
            }
        }
        iceShroomLastCooldown.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
    }

    /**
     * On the frame Ice-shroom's Plant Food activates, drops one falling-icicle projectile
     * onto every zombie inside its 3x3 zone (same edge-detection idiom as
     * {@link #drawMeleePlantProjectiles}'s Kiwibeast/Phat Beet handling).
     */
    private void triggerIceShroomPlantFood() {
        for (Plant plant : screen.session.getPlants()) {
            if (plant == null || !plant.isAlive() || plant.getPosition() == null) continue;
            if (!ICE_SHROOM.equalsIgnoreCase(plant.getName())) continue;

            boolean pf = plant.isPlantFoodActive();
            boolean seen = iceShroomPlantFoodSeen.getOrDefault(plant, false);
            if (pf && !seen) {
                List<ProjectileEffectAssets.AssetEntry> pfEntries = ProjectileEffectAssets.get(
                        ICE_SHROOM, ProjectileEffectAssets.Kind.PROJECTILE, ProjectileEffectAssets.Variant.PLANT_FOOD);
                if (!pfEntries.isEmpty()) {
                    ProjectileEffectAssets.AssetEntry entry = pfEntries.get(0);
                    Position center = plant.getPosition();
                    for (Zombie zombie : screen.session.getZombies()) {
                        if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
                        Position zp = zombie.getPosition();
                        if (Math.abs(zp.x() - center.x()) <= 1 && Math.abs(zp.y() - center.y()) <= 1) {
                            impactEffects.add(new TimedPamEffect(entry.path(), entry.state(),
                                    entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP,
                                    entry.isStaticImage(), zp, 0.6f, PROJECTILE_PAM_SCALE));
                        }
                    }
                }
            }
            iceShroomPlantFoodSeen.put(plant, pf);
        }
        iceShroomPlantFoodSeen.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
    }

    void drawProjectiles(float delta, float bw, float bh) {
        for (Projectile projectile : screen.session.getProjectiles()) {
            Plant source = projectile.getSourcePlant();
            if (source != null && projectile.getPosition() != null) {
                projectileTraces.put(projectile, new ProjectileTrace(source.getName(),
                        source.isPlantFoodActive(), projectile.getAssetVariant(),
                        projectile.getPosition()));
            }
            if (!projectile.isVisible()) continue;
            float age = projectileAnimTimes.getOrDefault(projectile, 0f) + delta;
            projectileAnimTimes.put(projectile, age);
            Position projectilePosition = projectile.getPosition();
            if (projectilePosition == null) continue;
            int row = (int) Math.round(projectilePosition.y());
            screen.queueRowDraw(row, () -> {
                if (!drawProjectilePam(projectile, age)) {
                    drawSmallDot(projectilePosition, new Color(0.95f, 0.9f, 0.18f, 1f));
                }
            });
        }
        spawnImpactEffectsForSpentProjectiles();
        drawMeleePlantProjectiles(delta);
        drawZombieProjectiles(delta);
        projectileAnimTimes.keySet().removeIf(p -> !screen.session.getProjectiles().contains(p));
        projectileTraces.keySet().removeIf(p -> !screen.session.getProjectiles().contains(p));
    }


    private void drawMeleePlantProjectiles(float delta) {
        for (Plant plant : screen.session.getPlants()) {
            if (plant == null || plant.getPosition() == null || !plant.isAlive()) continue;
            String name = plant.getName();
            boolean headbutterLettuce = "Iceberg Lettuce".equalsIgnoreCase(name);
            if (!"Kiwibeast".equalsIgnoreCase(name) && !"Phat Beet".equalsIgnoreCase(name)
                    && !headbutterLettuce) continue;

            boolean pf = plant.isPlantFoodActive();
            boolean seenPf = meleePlantFoodSeen.getOrDefault(plant, false);
            if (pf && !seenPf) {
                List<ProjectileEffectAssets.AssetEntry> pfEntries = ProjectileEffectAssets.get(
                        name, ProjectileEffectAssets.Kind.PROJECTILE,
                        ProjectileEffectAssets.Variant.PLANT_FOOD);
                if (!pfEntries.isEmpty()) {
                    ProjectileEffectAssets.AssetEntry entry = pfEntries.get(0);
                    impactEffects.add(new TimedPamEffect(entry.path(), entry.state(),
                            entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP,
                            entry.isStaticImage(), plant.getPosition(),
                            ("Phat Beet".equalsIgnoreCase(name) ? 5.0f : 1.2f),
                            PROJECTILE_PAM_SCALE));
                }
            }
            meleePlantFoodSeen.put(plant, pf);

            double cooldown = plant.getIntervalTimer();
            Double last = meleeLastCooldown.put(plant, cooldown);
            if (!pf && last != null && cooldown > last + 0.05) {
                List<ProjectileEffectAssets.AssetEntry> entries = ProjectileEffectAssets.get(
                        name, ProjectileEffectAssets.Kind.PROJECTILE,
                        ProjectileEffectAssets.Variant.NORMAL);
                if (!entries.isEmpty()) {
                    ProjectileEffectAssets.AssetEntry entry = headbutterLettuce
                            ? pickMeleeFacingEntry(plant, entries)
                            : entries.get(0);
                    impactEffects.add(new TimedPamEffect(entry.path(), entry.state(),
                            entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP,
                            entry.isStaticImage(), plant.getPosition(), 0.45f,
                            PROJECTILE_PAM_SCALE));
                }
            }
        }
        meleeLastCooldown.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        meleePlantFoodSeen.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
    }

    /**
     * Headbutter Lettuce's HITFX has two clips registered under the same Kind/Variant -
     * "animation" (front/right swing) and "animation2" (back/left swing) - so unlike
     * Kiwibeast/Phat Beet (a single entry, always index 0) the right one has to be picked
     * per-hit from the same facing flag MeleeStrategy set for this attack.
     */
    private ProjectileEffectAssets.AssetEntry pickMeleeFacingEntry(
            Plant plant, List<ProjectileEffectAssets.AssetEntry> entries) {
        if (plant.isMeleeFacingLeft()) {
            for (ProjectileEffectAssets.AssetEntry candidate : entries) {
                if ("animation2".equals(candidate.state())) return candidate;
            }
        }
        for (ProjectileEffectAssets.AssetEntry candidate : entries) {
            if ("animation".equals(candidate.state())) return candidate;
        }
        return entries.get(0);
    }

    private void drawZombieProjectiles(float delta) {
        List<ZombieProjectile> live = screen.session.getZombieProjectiles();
        for (ZombieProjectile projectile : live) {
            Position position = projectile.getPosition();
            if (position == null) continue;
            boolean justSpawned = !zombieProjectileAnimTimes.containsKey(projectile);
            zombieProjectileTraces.put(projectile, position);
            float age = zombieProjectileAnimTimes.getOrDefault(projectile, 0f) + delta;
            zombieProjectileAnimTimes.put(projectile, age);

            if (justSpawned && projectile instanceof GargantuarImpProjectile) {
                AudioManager.get().playSound(AudioEnum.SFX_ZOMBIE_IMP);
            }

            int row = (int) Math.round(position.y());
            screen.queueRowDraw(row, () -> drawZombieProjectileVisual(projectile, position, age));
        }

        for (Map.Entry<ZombieProjectile, Position> spent : zombieProjectileTraces.entrySet()) {
            if (live.contains(spent.getKey())) continue;
            if (!(spent.getKey() instanceof ZombiePeaProjectile pea)) continue;
            if (!pea.hasSplatted()) continue;
            Position position = spent.getValue();
            if (position == null || isOffBoard(position)) continue;
            impactEffects.add(new TimedPamEffect(ZOMBIE_PEA_SPLAT_PAM, "animation", false,
                    false, position, IMPACT_EFFECT_DURATION, PROJECTILE_PAM_SCALE));
            AudioManager.get().playSound(AudioEnum.SFX_BUBBLE_HIT);
        }
        zombieProjectileAnimTimes.keySet().removeIf(p -> !live.contains(p));
        zombieProjectileTraces.keySet().removeIf(p -> !live.contains(p));
    }

    private void drawZombieProjectileVisual(ZombieProjectile projectile, Position position, float age) {
        if (projectile instanceof GargantuarImpProjectile impProjectile) {
            String path = ZombieAnimationRegistry.pathFor(impProjectile.getImpAlias(), screen.seasonFolder);
            boolean pamDrawn = false;
            if (path != null) {
                float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth() - 10f;
                float y = screen.cellY(position.y()) + 40f;
                pamDrawn = screen.drawPam(path, "fly", age, x, y, 0.52f, impProjectile.isFacingRight());
            }
            if (!pamDrawn) {
                drawSmallDot(position, new Color(0.8f, 0.18f, 0.18f, 1f));
            }
        } else if (projectile instanceof ZombiePeaProjectile) {
            float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                    + screen.getBoardTileWidth() * 0.41f;
            float y = screen.cellY((int) position.y()) + screen.getBoardTileHeight() * 0.42f;
            if (!screen.drawPam(ZOMBIE_PEA_PAM, "animation", age, x, y,
                    PROJECTILE_PAM_SCALE * 2.0f, true)) {
                drawSmallDot(position, new Color(0.55f, 0.85f, 0.25f, 1f));
            }
        } else if (projectile instanceof OctopusProjectile) {
            float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                    + screen.getBoardTileWidth() * 0.41f;
            float y = screen.cellY(position.y()) + screen.getBoardTileHeight() * 0.42f;
            if (!screen.drawPam(OCTOPUS_PROJECTILE_PAM, "toss", age, x, y,
                    PROJECTILE_PAM_SCALE * 2.0f, true)) {
                drawSmallDot(position, new Color(0.55f, 0.2f, 0.55f, 1f));
            }
        } else if (projectile instanceof SnowballProjectile) {
            drawZombieProjectileTexture(SNOWBALL_PROJECTILE_TEXTURE, position, 0.34f, 0f);
        } else if (projectile instanceof BoneProjectile) {
            drawZombieProjectileTexture(BONE_PROJECTILE_TEXTURE, position, 0.34f, 90f);
        } else {
            drawSmallDot(position, new Color(0.8f, 0.18f, 0.18f, 1f));
        }
    }

    private void spawnImpactEffectsForSpentProjectiles() {
        if (projectileTraces.isEmpty()) return;
        for (Map.Entry<Projectile, ProjectileTrace> tracked : projectileTraces.entrySet()) {
            if (screen.session.getProjectiles().contains(tracked.getKey())) continue;
            ProjectileTrace trace = tracked.getValue();
            if (trace.position == null || isOffBoard(trace.position)) continue;

            ProjectileEffectAssets.AssetEntry entry = resolveImpactEntry(trace);
            if (entry == null) continue;
            float duration = IMPACT_EFFECT_DURATION;
            if (GRAPESHOT.equalsIgnoreCase(trace.plantName)) {
                float exact = AnimationFactory.exactClipDurationForPath(entry.path(), entry.state());
                if (exact > 0f) duration = exact;
            }
            impactEffects.add(new TimedPamEffect(entry.path(), entry.state(),
                    entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP,
                    entry.isStaticImage(), trace.position, IMPACT_EFFECT_DURATION,
                    PROJECTILE_PAM_SCALE));
            AudioManager.get().playSound(AudioEnum.SFX_ZOMBIE_HIT);
        }
    }

    private void drawZombieProjectileTexture(String path, Position position, float scale, float rotation) {
        Texture texture = zombieProjectileTextures.get(path);
        if (texture == null) {
            if (!Gdx.files.internal(path).exists()) {
                drawSmallDot(position, new Color(0.8f, 0.18f, 0.18f, 1f));
                return;
            }
            texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            zombieProjectileTextures.put(path, texture);
        }

        float base = screen.getBoardTileWidth() * scale;
        float width = base;
        float height = base * ((float) texture.getHeight() / Math.max(1, texture.getWidth()));
        float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                + screen.getBoardTileWidth() * 0.5f - width * 0.5f;
        float y = screen.cellY(position.y()) + screen.getBoardTileHeight() * 0.5f - height * 0.5f;
        screen.batch.draw(texture, x, y, width * 0.5f, height * 0.5f,
                width, height, 1f, 1f, rotation, 0, 0, texture.getWidth(), texture.getHeight(), false, false);
    }

    private boolean isOffBoard(Position position) {
        return position.x() < 0 || position.x() >= screen.session.getEnvironment().getCols()
                || position.y() < 0 || position.y() >= screen.session.getEnvironment().getRows();
    }

    private ProjectileEffectAssets.AssetEntry resolveImpactEntry(ProjectileTrace trace) {
        List<ProjectileEffectAssets.AssetEntry> entries = trace.boosted
                ? ProjectileEffectAssets.get(trace.plantName, ProjectileEffectAssets.Kind.HIT,
                ProjectileEffectAssets.Variant.PLANT_FOOD)
                : List.of();
        if (entries.isEmpty()) {
            entries = ProjectileEffectAssets.get(trace.plantName,
                    ProjectileEffectAssets.Kind.HIT, ProjectileEffectAssets.Variant.NORMAL);
        }
        return entries.isEmpty() ? null : entries.get(Math.min(trace.variant, entries.size() - 1));
    }

    private boolean drawProjectilePam(Projectile projectile, float age) {
        Position position = projectile.getPosition();
        Plant source = projectile.getSourcePlant();
        if (position == null || source == null || source.getName() == null) return false;

        // Projectiles normally fly left-to-right (positive x speed), which is the
        // orientation the artwork is drawn in. Once something (e.g. a Jester Zombie)
        // deflects a projectile back the other way, its speed.x() goes negative, so
        // mirror the animation to match the direction it's actually travelling in.
        boolean flip = isTravellingLeft(projectile);

        ProjectileEffectAssets.Variant variant = source.isPlantFoodActive()
                ? ProjectileEffectAssets.Variant.PLANT_FOOD
                : ProjectileEffectAssets.Variant.NORMAL;
        if (projectile.getDisplayPath() != null && projectile.getDisplayState() != null) {
            float dpx = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                    + screen.getBoardTileWidth() * 0.41f;
            float dpy = screen.cellY((int) position.y()) + screen.getBoardTileHeight() * 0.36f;
            if (flip) {
                screen.drawPamMirrored(projectile.getDisplayPath(), projectile.getDisplayState(), age,
                        dpx, dpy, PROJECTILE_PAM_SCALE);
            } else {
                screen.drawPam(projectile.getDisplayPath(), projectile.getDisplayState(), age,
                        dpx, dpy, PROJECTILE_PAM_SCALE, false);
            }
            return true;
        }
        List<ProjectileEffectAssets.AssetEntry> entries = ProjectileEffectAssets.get(
                source.getName(), ProjectileEffectAssets.Kind.PROJECTILE, variant);
        if (entries.isEmpty() && variant == ProjectileEffectAssets.Variant.PLANT_FOOD) {
            entries = ProjectileEffectAssets.get(source.getName(),
                    ProjectileEffectAssets.Kind.PROJECTILE, ProjectileEffectAssets.Variant.NORMAL);
        }
        if (entries.isEmpty()) return false;

        ProjectileEffectAssets.AssetEntry entry =
                entries.get(Math.min(projectile.getAssetVariant(), entries.size() - 1));

        boolean freeFlying = projectile instanceof GrapeshotProjectile;
        float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                + screen.getBoardTileWidth() * 0.41f;
        float y = (freeFlying ? screen.cellY(position.y()) : screen.cellY((int) position.y()))
                + screen.getBoardTileHeight() * 0.42f;
        float scaleFactor = freeFlying ? GRAPE_PROJECTILE_SCALE_FACTOR : 2.0f;

        if (entry.isStaticImage()) {
            return screen.assets().drawStaticEffect(entry.path(), x, y, STATIC_PROJECTILE_SCALE, flip);
        }
        // Note: this used to (incorrectly) pass entry.playMode()==LOOP into the "flip"
        // slot, which had nothing to do with travel direction — that's why deflected
        // projectiles kept their original orientation instead of mirroring with the
        // reversed movement.
        //
        // Also, unlike zombies' own multi-part PAM rigs, these projectile clips don't
        // reliably mirror through PamPlayer's own flip flag (drawPamMirrored exists in
        // PamRenderer for exactly this reason — it flips the whole draw via a negated
        // transform scale instead, which always works regardless of the clip's internals).
        // Use that guaranteed path whenever the projectile is actually travelling left.
        if (flip) {
            return screen.drawPamMirrored(entry.path(), entry.state(), age, x, y,
                    PROJECTILE_PAM_SCALE * scaleFactor);
        }
        return screen.drawPam(entry.path(), entry.state(), age, x, y, PROJECTILE_PAM_SCALE * scaleFactor, false);
    }

    /** True once a projectile's horizontal speed has gone negative (e.g. after a Jester deflection). */
    private boolean isTravellingLeft(Projectile projectile) {
        Position speed = projectile.getSpeed();
        return speed != null && speed.x() < 0;
    }

    private void drawSmallDot(Position p, Color color) {
        if (p == null) return;
        float x = GameScreen.BOARD_X + (float) p.x() * screen.getBoardTileWidth()
                + screen.getBoardTileWidth() * 0.41f;
        float y = screen.cellY((int) p.y()) + screen.getBoardTileHeight() * 0.42f;
        screen.drawFallback(x, y, 18f, 18f, color);
    }
}