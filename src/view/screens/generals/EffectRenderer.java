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
import model.projectile.ProjectileImpact;
import model.projectile.zombie_projectile.GargantuarImpProjectile;
import model.projectile.zombie_projectile.OctopusProjectile;
import model.projectile.zombie_projectile.SnowballProjectile;
import model.projectile.zombie_projectile.BoneProjectile;
import model.projectile.zombie_projectile.CrystalSkullBeamProjectile;
import model.projectile.zombie_projectile.FutureGargantuarBeamProjectile;
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

    private static final String RADIOACTIVE_SUN_EXPLOSION_PAM =
            "768/INITIAL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_EGYPT/ZOMBOSS_MISSILE_EXPLOSION_EGYPT.PAM";
    private static final String RADIOACTIVE_SUN_EXPLOSION_CLIP = "missile_explosion";
    private static final float RADIOACTIVE_SUN_EXPLOSION_SCALE = 0.65f;
    private static final float RADIOACTIVE_SUN_EXPLOSION_FALLBACK_DURATION = 1.0f;

    private static final float IMPACT_EFFECT_DURATION = 0.35f;

    private static final String GRAVE_BUSTER_DIRT_PAM =
            "768/INITIAL/EFFECTS/GRAVEBUSTER_DIRT/GRAVEBUSTER_DIRT.PAM";
    static final String GRAVE_BUSTER_DIRT_ENTRY_STATE = "gravebuster_dirt_anim";
    static final String GRAVE_BUSTER_DIRT_FADE_STATE = "gravebuster_dirt_fade";
    private static final float GRAVE_BUSTER_DIRT_FALLBACK_DURATION = 0.5f;
    private static final float GRAVE_BUSTER_DIRT_SCALE = 0.65f;

    /**
     * One scale for a shot and for the splat it leaves behind. They used to differ by a
     * factor of two, so every impact popped at half the size of the projectile that
     * caused it.
     */
    static final float PROJECTILE_DRAW_SCALE = PROJECTILE_PAM_SCALE * 2.0f;

    /**
     * Where a shot sits inside its tile, as a fraction of tile width/height. The default
     * is the whole-lawn calibration every projectile used to share; the per-plant entries
     * lift a shot to the muzzle it actually leaves from instead of drawing it at the
     * plant's base. The splat a shot leaves reuses the same offset, so the impact lands
     * exactly where the projectile was drawn.
     */
    private static final float[] DEFAULT_MUZZLE_OFFSET = {0.41f, 0.42f};
    private static final float[] PEA_MUZZLE_OFFSET = {0.47f, 0.55f};
    private static final float[] PULT_MUZZLE_OFFSET = {0.45f, 0.50f};
    private static final float[] LOW_MUZZLE_OFFSET = {0.41f, 0.30f};
    private static final Map<String, float[]> MUZZLE_OFFSETS = buildMuzzleOffsets();

    private static Map<String, float[]> buildMuzzleOffsets() {
        Map<String, float[]> offsets = new java.util.HashMap<>();
        for (String pea : new String[] {"Peashooter", "Repeater", "Threepeater", "Split Pea",
                "Pea Pod", "Mega Gatling Pea", "Snow Pea", "Fire Peashooter", "Goo Peashooter"}) {
            offsets.put(pea.toLowerCase(), PEA_MUZZLE_OFFSET);
        }
        for (String pult : new String[] {"Cabbage-pult", "Kernel-pult", "Melon-pult",
                "Winter Melon", "Pepper-pult"}) {
            offsets.put(pult.toLowerCase(), PULT_MUZZLE_OFFSET);
        }
        offsets.put("bowling bulb", LOW_MUZZLE_OFFSET);
        return offsets;
    }

    private static float[] muzzleOffsetFor(String plantName) {
        if (plantName == null) return DEFAULT_MUZZLE_OFFSET;
        return MUZZLE_OFFSETS.getOrDefault(plantName.toLowerCase(), DEFAULT_MUZZLE_OFFSET);
    }

    private static final float[] NO_MUZZLE_NUDGE = {0f, 0f};
    private static final float[] PEA_MUZZLE_NUDGE = {30f, 0f};
    private static final float[] PULT_MUZZLE_NUDGE = {-30f, 50f};
    private static final Map<String, float[]> MUZZLE_NUDGES = buildMuzzleNudges();

    private static Map<String, float[]> buildMuzzleNudges() {
        Map<String, float[]> nudges = new java.util.HashMap<>();
        for (String pea : new String[] {"Peashooter", "Repeater", "Threepeater", "Split Pea",
                "Pea Pod", "Mega Gatling Pea", "Snow Pea", "Fire Peashooter", "Goo Peashooter"}) {
            nudges.put(pea.toLowerCase(), PEA_MUZZLE_NUDGE);
        }
        for (String pult : new String[] {"Cabbage-pult", "Kernel-pult", "Melon-pult",
                "Winter Melon", "Pepper-pult"}) {
            nudges.put(pult.toLowerCase(), PULT_MUZZLE_NUDGE);
        }
        return nudges;
    }

    private static float[] muzzleNudgeFor(String plantName) {
        if (plantName == null) return NO_MUZZLE_NUDGE;
        return MUZZLE_NUDGES.getOrDefault(plantName.toLowerCase(), NO_MUZZLE_NUDGE);
    }

    private static float muzzleX(float[] offset, boolean backwards) {
        return backwards ? 1f - offset[0] : offset[0];
    }

    private static final float GRAPE_PROJECTILE_SCALE_FACTOR = 1.6f;
    private static final float DEFAULT_PROJECTILE_SCALE_FACTOR = 1f;
    private static final Map<String, Float> PROJECTILE_SCALE_FACTORS =
            Map.of("cat-tail", 0.5f);

    private static float projectileScaleFactor(String plantName) {
        if (plantName == null) return DEFAULT_PROJECTILE_SCALE_FACTOR;
        return PROJECTILE_SCALE_FACTORS.getOrDefault(plantName.toLowerCase(),
                DEFAULT_PROJECTILE_SCALE_FACTOR);
    }
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
    private static final String CRYSTALSKULL_BEAM_PAM =
            "768/FULL/EFFECTS/CRYSTALSKULL_BEAM/CRYSTALSKULL_BEAM.PAM";
    private static final String CRYSTALSKULL_BEAM_STATE = "laser_baem";
    // The beam clip is authored one tile wide; stretch it across however many
    // tiles separate the zombie from the plant it's hitting.
    private static final float CRYSTALSKULL_BEAM_HEIGHT_SCALE = PROJECTILE_PAM_SCALE * 2.0f;
    private static final String FUTURE_GARGANTUAR_BEAM_PAM =
            "768/FULL/EFFECTS/ZOMBIE_FUTURE_GARGANTUAR_BEAM/ZOMBIE_FUTURE_GARGANTUAR_BEAM.PAM";
    private static final String FUTURE_GARGANTUAR_BEAM_STATE = "laser_beam";
    private static final String FUTURE_GARGANTUAR_SCORCH_PAM =
            "768/FULL/EFFECTS/ZOMBIE_FUTURE_GARGANTUAR_SCORCH/ZOMBIE_FUTURE_GARGANTUAR_SCORCH.PAM";
    private static final String FUTURE_GARGANTUAR_SCORCH_STATE = "laser_hit";
    private static final float FUTURE_GARGANTUAR_BEAM_HEIGHT_SCALE = PROJECTILE_PAM_SCALE * 2.4f;
    private static final float FUTURE_GARGANTUAR_SCORCH_SCALE = PROJECTILE_PAM_SCALE * 2.0f;
    // The beam leaves the gargantuar's chest, noticeably higher than a normal muzzle.
    private static final float FUTURE_GARGANTUAR_BEAM_MUZZLE_Y = 0.62f;

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

    /**
     * A splat queued by the model when a shot actually connected. Unlike the old
     * "the projectile vanished from the list, so draw a splat" guess, a shot that simply
     * ran out of range or left the lawn queues nothing, and the position is the exact
     * point of impact rather than wherever the shot happened to be a tick earlier.
     */
    private static final class ProjectileImpactEffect {
        final String path;
        final String state;
        final boolean staticImage;
        final Position position;
        final float duration;
        final float scale;
        final float offsetX;
        final float offsetY;
        final float nudgeX;
        final float nudgeY;
        float time;

        ProjectileImpactEffect(String path, String state, boolean staticImage, Position position,
                               float duration, float scale, float offsetX, float offsetY,
                               float nudgeX, float nudgeY) {
            this.path = path;
            this.state = state;
            this.staticImage = staticImage;
            this.position = position;
            this.duration = duration;
            this.scale = scale;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.nudgeX = nudgeX;
            this.nudgeY = nudgeY;
        }
    }

    private final GameScreen screen;

    private final List<TimedPamEffect> explodingPlantEffects = new ArrayList<>();
    private final List<TimedPamEffect> impactEffects = new ArrayList<>();
    private final List<PlacedPamEffect> behindZombieEffects = new ArrayList<>();
    private final List<PlacedPamEffect> foregroundEffects = new ArrayList<>();
    private final List<PlacedPamEffect> radioactiveSunExplosionEffects = new ArrayList<>();
    private final List<ScorchedTileEffect> scorchedTileEffects = new ArrayList<>();
    private final List<HotPotatoMeltEffect> hotPotatoMeltEffects = new ArrayList<>();
    private final List<ProjectileImpactEffect> projectileImpactEffects = new ArrayList<>();
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

    void addRadioactiveSunExplosion(Position position) {
        if (position == null) return;

        Position at = new Position(
                Math.round(position.x()),
                Math.round(position.y()));
        float duration = AnimationFactory.exactClipDurationForPath(
                RADIOACTIVE_SUN_EXPLOSION_PAM, RADIOACTIVE_SUN_EXPLOSION_CLIP);
        if (duration <= 0f) duration = RADIOACTIVE_SUN_EXPLOSION_FALLBACK_DURATION;
        radioactiveSunExplosionEffects.add(new PlacedPamEffect(
                RADIOACTIVE_SUN_EXPLOSION_PAM,
                RADIOACTIVE_SUN_EXPLOSION_CLIP,
                at,
                duration,
                RADIOACTIVE_SUN_EXPLOSION_SCALE,
                screen.getBoardTileWidth() * 0.5f,
                screen.getBoardTileHeight() * 0.5f));
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

    void addGraveBusterDirtEffect(Position position, String state) {
        if (position == null || state == null) return;
        float duration = AnimationFactory.exactClipDurationForPath(GRAVE_BUSTER_DIRT_PAM, state);
        if (duration <= 0f) duration = GRAVE_BUSTER_DIRT_FALLBACK_DURATION;
        explodingPlantEffects.add(new TimedPamEffect(GRAVE_BUSTER_DIRT_PAM, state, false, false,
                new Position(position.x(), position.y()), duration, GRAVE_BUSTER_DIRT_SCALE));
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
        drawPlacedEffects(radioactiveSunExplosionEffects, delta);
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
        float alpha = screen.tickAlpha();
        for (Projectile projectile : screen.session.getProjectiles()) {
            if (!projectile.isVisible()) continue;
            float age = projectileAnimTimes.getOrDefault(projectile, 0f) + delta;
            projectileAnimTimes.put(projectile, age);
            Position drawAt = interpolated(projectile.getPreviousPosition(),
                    projectile.getPosition(), alpha);
            if (drawAt == null) continue;
            int row = (int) Math.round(drawAt.y());
            int layer = drawsBehindPlants(projectile, drawAt)
                    ? GameScreen.ROW_LAYER_BEHIND_PLANTS : GameScreen.ROW_LAYER_DEFAULT;
            screen.queueRowDraw(row, layer, () -> {
                if (!drawProjectilePam(projectile, drawAt, age)) {
                    drawSmallDot(drawAt, new Color(0.95f, 0.9f, 0.18f, 1f));
                }
            });
        }
        queueProjectileImpacts();
        drawProjectileImpactEffects(delta);
        drawMeleePlantProjectiles(delta);
        drawZombieProjectiles(delta);
        projectileAnimTimes.keySet().removeIf(p -> !screen.session.getProjectiles().contains(p));
    }

    private static Position interpolated(Position previous, Position current, float alpha) {
        if (current == null) return null;
        if (previous == null) return current;
        return Position.of(previous.x() + (current.x() - previous.x()) * alpha,
                previous.y() + (current.y() - previous.y()) * alpha);
    }

    private void queueProjectileImpacts() {
        List<ProjectileImpact> impacts = screen.session.drainProjectileImpacts();
        if (impacts.isEmpty()) return;

        boolean playedSound = false;
        for (ProjectileImpact impact : impacts) {
            Position at = impact.position();
            if (at == null || isOffBoard(at)) continue;
            if (!playedSound) {
                AudioManager.get().playSound(AudioEnum.SFX_ZOMBIE_HIT);
                playedSound = true;
            }

            ProjectileEffectAssets.AssetEntry entry = resolveImpactEntry(impact);
            if (entry == null) continue;

            float duration = IMPACT_EFFECT_DURATION;
            if (GRAPESHOT.equalsIgnoreCase(impact.plantName())) {
                float exact = AnimationFactory.exactClipDurationForPath(entry.path(),
                        entry.state());
                if (exact > 0f) duration = exact;
            }
            float[] offset = muzzleOffsetFor(impact.plantName());
            float[] nudge = muzzleNudgeFor(impact.plantName());
            projectileImpactEffects.add(new ProjectileImpactEffect(entry.path(), entry.state(),
                    entry.isStaticImage(), at, duration, PROJECTILE_DRAW_SCALE,
                    offset[0], offset[1], nudge[0], nudge[1]));
        }
    }

    private void drawProjectileImpactEffects(float delta) {
        if (projectileImpactEffects.isEmpty()) return;
        float tileWidth = screen.getBoardTileWidth();
        float tileHeight = screen.getBoardTileHeight();
        for (ProjectileImpactEffect effect : projectileImpactEffects) {
            effect.time += delta;
            float x = GameScreen.BOARD_X + (float) effect.position.x() * tileWidth
                    + tileWidth * effect.offsetX + effect.nudgeX;
            float y = screen.cellY(effect.position.y()) + tileHeight * effect.offsetY
                    + effect.nudgeY;
            float time = effect.time;
            int row = (int) Math.round(effect.position.y());
            screen.queueRowDraw(row, () -> {
                if (effect.staticImage) {
                    screen.assets().drawStaticEffect(effect.path, x, y, effect.scale);
                } else {
                    screen.drawPam(effect.path, effect.state, time, x, y, effect.scale, false);
                }
            });
        }
        projectileImpactEffects.removeIf(e -> e.time > e.duration);
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
                    float pulseSeconds = Math.max(1.0f, (float) plant.getPlantFoodTimer());
                    impactEffects.add(new TimedPamEffect(entry.path(), entry.state(),
                            entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP,
                            entry.isStaticImage(), plant.getPosition(), pulseSeconds,
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
        float alpha = screen.tickAlpha();
        List<ZombieProjectile> live = screen.session.getZombieProjectiles();
        for (ZombieProjectile projectile : live) {
            Position position = interpolated(projectile.getPreviousPosition(),
                    projectile.getPosition(), alpha);
            if (position == null) continue;
            boolean justSpawned = !zombieProjectileAnimTimes.containsKey(projectile);
            zombieProjectileTraces.put(projectile, projectile.getPosition());
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
            projectileImpactEffects.add(new ProjectileImpactEffect(ZOMBIE_PEA_SPLAT_PAM,
                    "animation", false, position, IMPACT_EFFECT_DURATION, PROJECTILE_DRAW_SCALE,
                    DEFAULT_MUZZLE_OFFSET[0], DEFAULT_MUZZLE_OFFSET[1],
                    NO_MUZZLE_NUDGE[0], NO_MUZZLE_NUDGE[1]));
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
               String state = "fly";
                if (AnimationFactory.hasExactClip(path, GargantuarImpProjectile.CLIP_RISING)) {
                    state = impProjectile.isRising()
                            ? GargantuarImpProjectile.CLIP_RISING
                            : GargantuarImpProjectile.CLIP_FALLING;
                }
                pamDrawn = screen.drawPam(path, state, age, x, y, 0.52f, impProjectile.isFacingRight());
            }
            if (!pamDrawn) {
                drawSmallDot(position, new Color(0.8f, 0.18f, 0.18f, 1f));
            }
        } else if (projectile instanceof ZombiePeaProjectile) {
            float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                    + screen.getBoardTileWidth() * DEFAULT_MUZZLE_OFFSET[0];
            float y = screen.cellY(position.y())
                    + screen.getBoardTileHeight() * DEFAULT_MUZZLE_OFFSET[1];
            if (!screen.drawPam(ZOMBIE_PEA_PAM, "animation", age, x, y,
                    PROJECTILE_DRAW_SCALE, true)) {
                drawSmallDot(position, new Color(0.55f, 0.85f, 0.25f, 1f));
            }
        } else if (projectile instanceof OctopusProjectile) {
            float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                    + screen.getBoardTileWidth() * DEFAULT_MUZZLE_OFFSET[0];
            float y = screen.cellY(position.y())
                    + screen.getBoardTileHeight() * DEFAULT_MUZZLE_OFFSET[1];
            if (!screen.drawPam(OCTOPUS_PROJECTILE_PAM, "toss", age, x, y,
                    PROJECTILE_DRAW_SCALE, true)) {
                drawSmallDot(position, new Color(0.55f, 0.2f, 0.55f, 1f));
            }
        } else if (projectile instanceof SnowballProjectile) {
            drawZombieProjectileTexture(SNOWBALL_PROJECTILE_TEXTURE, position, 0.34f, 0f);
        } else if (projectile instanceof BoneProjectile) {
            drawZombieProjectileTexture(BONE_PROJECTILE_TEXTURE, position, 0.34f, 90f);
        } else if (projectile instanceof CrystalSkullBeamProjectile beam) {
            drawCrystalSkullBeam(beam, position, age);
        } else if (projectile instanceof FutureGargantuarBeamProjectile beam) {
            drawFutureGargantuarBeam(beam, position, age);
        } else {
            drawSmallDot(position, new Color(0.8f, 0.18f, 0.18f, 1f));
        }
    }

    /**
     * Scratches the beam art from the zombie's own position straight across to
     * the plant it's hitting, stretching the clip's width to match the actual
     * distance instead of always drawing it at a fixed length.
     */
    private void drawCrystalSkullBeam(CrystalSkullBeamProjectile beam, Position sourcePosition, float age) {
        Position targetPosition = beam.getBeamTargetPosition();
        if (targetPosition == null) {
            drawSmallDot(sourcePosition, new Color(0.55f, 0.85f, 0.95f, 1f));
            return;
        }

        float boardTileWidth = screen.getBoardTileWidth();
        float sourceX = GameScreen.BOARD_X + (float) sourcePosition.x() * boardTileWidth
                + boardTileWidth * 0.41f;
        float targetX = GameScreen.BOARD_X + (float) targetPosition.x() * boardTileWidth
                + boardTileWidth * 0.41f;
        float y = screen.cellY(sourcePosition.y())
                + screen.getBoardTileHeight() * DEFAULT_MUZZLE_OFFSET[1];

        // The clip is drawn anchored at the source (zombie) and stretched toward the
        // target (plant); a negative X scale both flips and stretches leftward since
        // the target is toward the house (lower x) from the zombie.
        float distancePixels = targetX - sourceX;
        float scaleX = distancePixels / boardTileWidth * PROJECTILE_PAM_SCALE;
        if (Math.abs(scaleX) < 0.01f) scaleX = scaleX < 0 ? -0.01f : 0.01f;

        if (!screen.drawPamStretched(CRYSTALSKULL_BEAM_PAM, CRYSTALSKULL_BEAM_STATE, age,
                sourceX, y, scaleX, CRYSTALSKULL_BEAM_HEIGHT_SCALE, false)) {
            drawSmallDot(sourcePosition, new Color(0.55f, 0.85f, 0.95f, 1f));
        }
    }

    private void drawFutureGargantuarBeam(FutureGargantuarBeamProjectile beam,
                                          Position sourcePosition, float age) {
        Position targetPosition = beam.getBeamTargetPosition();
        if (targetPosition == null) {
            drawSmallDot(sourcePosition, new Color(1f, 0.35f, 0.2f, 1f));
            return;
        }

        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        float sourceX = GameScreen.BOARD_X + (float) sourcePosition.x() * boardTileWidth
                + boardTileWidth * 0.41f;
        float targetX = GameScreen.BOARD_X + (float) targetPosition.x() * boardTileWidth
                + boardTileWidth * 0.41f;
        float beamY = screen.cellY(sourcePosition.y())
                + boardTileHeight * FUTURE_GARGANTUAR_BEAM_MUZZLE_Y;

        float distancePixels = targetX - sourceX;
        float scaleX = distancePixels / boardTileWidth * PROJECTILE_PAM_SCALE;
        if (Math.abs(scaleX) < 0.01f) scaleX = scaleX < 0 ? -0.01f : 0.01f;

        boolean beamDrawn = screen.drawPamStretched(FUTURE_GARGANTUAR_BEAM_PAM,
                FUTURE_GARGANTUAR_BEAM_STATE, age, sourceX, beamY, scaleX,
                FUTURE_GARGANTUAR_BEAM_HEIGHT_SCALE, false);
        if (!beamDrawn) drawSmallDot(sourcePosition, new Color(1f, 0.35f, 0.2f, 1f));

        float scorchY = screen.cellY(targetPosition.y())
                + boardTileHeight * FUTURE_GARGANTUAR_BEAM_MUZZLE_Y;
        screen.drawPam(FUTURE_GARGANTUAR_SCORCH_PAM, FUTURE_GARGANTUAR_SCORCH_STATE, age,
                targetX, scorchY, FUTURE_GARGANTUAR_SCORCH_SCALE, false);
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

    private ProjectileEffectAssets.AssetEntry resolveImpactEntry(ProjectileImpact impact) {
        List<ProjectileEffectAssets.AssetEntry> entries = impact.plantFood()
                ? ProjectileEffectAssets.get(impact.plantName(), ProjectileEffectAssets.Kind.HIT,
                ProjectileEffectAssets.Variant.PLANT_FOOD)
                : List.of();
        if (entries.isEmpty()) {
            entries = ProjectileEffectAssets.get(impact.plantName(),
                    ProjectileEffectAssets.Kind.HIT, ProjectileEffectAssets.Variant.NORMAL);
        }
        return entries.isEmpty()
                ? null : entries.get(Math.min(impact.assetVariant(), entries.size() - 1));
    }

    private boolean drawProjectilePam(Projectile projectile, Position position, float age) {
        String sourceName = projectile.getSourcePlantName();
        if (position == null || sourceName == null) return false;

        // Projectiles normally fly left-to-right (positive x speed), which is the
        // orientation the artwork is drawn in. Once something (e.g. a Jester Zombie)
        // deflects a projectile back the other way, its speed.x() goes negative, so
        // mirror the animation to match the direction it's actually travelling in.
        boolean flip = isTravellingLeft(projectile);

        // Some shots (Rotobaga's four diagonal launches, Starfruit's diagonal shots,
        // etc.) actually travel at an angle rather than along a single row. A plain
        // left/right mirror can't represent that - the sprite needs to rotate to point
        // along the real direction of travel. Only kicks in once the vertical speed is
        // non-negligible, so ordinary straight shots keep their existing mirror-only
        // behaviour untouched.
        Position projectileSpeed = projectile.getSpeed();
        boolean diagonal = projectileSpeed != null && Math.abs(projectileSpeed.y()) > 1.0e-3;
        float rotationDegrees = 0f;
        if (diagonal) {
            // Row coordinates increase downward while screen Y increases upward
            // (see BoardLayout.cellY), so the vertical component has to be negated
            // to land the rotation in the direction that's actually drawn on screen.
            rotationDegrees = (float) Math.toDegrees(
                    Math.atan2(-projectileSpeed.y(), projectileSpeed.x()));
        }

        // The variant is the one the shot was fired with, not the source plant's current
        // state, so a pea keeps its look when the plant's Plant Food starts or expires
        // while the pea is still in the air.
        ProjectileEffectAssets.Variant variant = projectile.isPlantFoodShot()
                ? ProjectileEffectAssets.Variant.PLANT_FOOD
                : ProjectileEffectAssets.Variant.NORMAL;

        float[] muzzle = muzzleOffsetFor(sourceName);
        float[] nudge = muzzleNudgeFor(sourceName);
        boolean backwards = projectile.isFiredBackwards();
        float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                + screen.getBoardTileWidth() * muzzleX(muzzle, backwards)
                + (backwards ? -nudge[0] : nudge[0]);
        // cellY takes the fractional row, so a lobbed shot's arc and a lane-shifting
        // shot's slide render as the smooth curves the model computes rather than being
        // truncated onto whole rows.
        float y = screen.cellY(position.y()) + screen.getBoardTileHeight() * muzzle[1] + nudge[1];

        float scaleFactor = projectileScaleFactor(sourceName);

        if (projectile.getDisplayPath() != null && projectile.getDisplayState() != null) {
            float displayScale = PROJECTILE_DRAW_SCALE * scaleFactor;
            if (diagonal) {
                return screen.drawPamRotated(projectile.getDisplayPath(), projectile.getDisplayState(), age,
                        x, y, displayScale, rotationDegrees);
            }
            if (flip) {
                screen.drawPamMirrored(projectile.getDisplayPath(), projectile.getDisplayState(), age,
                        x, y, displayScale);
            } else {
                screen.drawPam(projectile.getDisplayPath(), projectile.getDisplayState(), age,
                        x, y, displayScale, false);
            }
            return true;
        }
        List<ProjectileEffectAssets.AssetEntry> entries = ProjectileEffectAssets.get(
                sourceName, ProjectileEffectAssets.Kind.PROJECTILE, variant);
        if (entries.isEmpty() && variant == ProjectileEffectAssets.Variant.PLANT_FOOD) {
            entries = ProjectileEffectAssets.get(sourceName,
                    ProjectileEffectAssets.Kind.PROJECTILE, ProjectileEffectAssets.Variant.NORMAL);
        }
        if (entries.isEmpty()) return false;

        ProjectileEffectAssets.AssetEntry entry =
                entries.get(Math.min(projectile.getAssetVariant(), entries.size() - 1));

        boolean freeFlying = projectile instanceof GrapeshotProjectile;
        float drawScale = scaleFactor * (freeFlying
                ? PROJECTILE_PAM_SCALE * GRAPE_PROJECTILE_SCALE_FACTOR : PROJECTILE_DRAW_SCALE);

        if (entry.isStaticImage()) {
            return screen.assets().drawStaticEffect(entry.path(), x, y,
                    STATIC_PROJECTILE_SCALE * scaleFactor, flip);
        }
        // Rotate to the real travel angle for diagonal shots (see above); otherwise fall
        // back to the existing mirror-only handling for plain left/right travel.
        //
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
        if (diagonal) {
            return screen.drawPamRotated(entry.path(), entry.state(), age, x, y, drawScale, rotationDegrees);
        }
        if (flip) {
            return screen.drawPamMirrored(entry.path(), entry.state(), age, x, y, drawScale);
        }
        return screen.drawPam(entry.path(), entry.state(), age, x, y, drawScale, false);
    }

    private boolean drawsBehindPlants(Projectile projectile, Position drawAt) {
        if (!"Cat-tail".equalsIgnoreCase(projectile.getSourcePlantName())) return false;
        Plant source = projectile.getSourcePlant();
        if (source == null || source.getPosition() == null || drawAt == null) return true;
        return drawAt.distanceTo(source.getPosition()) <= CAT_TAIL_BEHIND_RADIUS;
    }

    private static final float CAT_TAIL_BEHIND_RADIUS = 0.8f;

    /** True once a projectile's horizontal speed has gone negative (e.g. after a Jester deflection). */
    private boolean isTravellingLeft(Projectile projectile) {
        Position speed = projectile.getSpeed();
        return speed != null && speed.x() < 0;
    }

    private void drawSmallDot(Position p, Color color) {
        if (p == null) return;
        float x = GameScreen.BOARD_X + (float) p.x() * screen.getBoardTileWidth()
                + screen.getBoardTileWidth() * DEFAULT_MUZZLE_OFFSET[0];
        float y = screen.cellY(p.y()) + screen.getBoardTileHeight() * DEFAULT_MUZZLE_OFFSET[1];
        screen.drawFallback(x, y, 18f, 18f, color);
    }
}