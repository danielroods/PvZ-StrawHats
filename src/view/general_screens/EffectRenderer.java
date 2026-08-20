package view.general_screens;

import com.badlogic.gdx.graphics.Color;

import controller.assets.ProjectileEffectAssets;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.plant.Plant;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.projectile.zombie_projectile.GargantuarImpProjectile;
import model.projectile.zombie_projectile.ZombieProjectile;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

class EffectRenderer {

    static final float PROJECTILE_PAM_SCALE = 0.35f;

    private static final float IMPACT_EFFECT_DURATION = 0.35f;
    private static final float STATIC_PROJECTILE_SCALE = 0.25f;

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
    private final Map<Projectile, ProjectileTrace> projectileTraces = new IdentityHashMap<>();
    private final Map<Projectile, Float> projectileAnimTimes = new IdentityHashMap<>();
    private final Map<ZombieProjectile, Float> zombieProjectileAnimTimes = new IdentityHashMap<>();

    EffectRenderer(GameScreen screen) {
        this.screen = screen;
    }

    ProjectileEffectAssets.AssetEntry resolveExplosionEntry(String plantName) {
        ProjectileEffectAssets.Variant normal = ProjectileEffectAssets.Variant.NORMAL;
        List<ProjectileEffectAssets.AssetEntry> hitEntries =
                ProjectileEffectAssets.get(plantName, ProjectileEffectAssets.Kind.HIT, normal);
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

    void drawExplodingPlantEffects(float delta) {
        drawTimedEffects(explodingPlantEffects, delta);
        drawTimedEffects(impactEffects, delta);
        drawPlantFoodEffects();
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
                if (entry.isStaticImage()) {
                    screen.assets().drawStaticEffect(entry.path(), x, y, PROJECTILE_PAM_SCALE);
                } else {
                    screen.drawPam(entry.path(), entry.state(), time, x, y, PROJECTILE_PAM_SCALE, loop);
                }
            }
        }
    }

    private void drawTimedEffects(List<TimedPamEffect> effects, float delta) {
        if (effects.isEmpty()) return;
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        for (TimedPamEffect effect : effects) {
            effect.time += delta;
            float x = GameScreen.BOARD_X + (float) effect.position.x() * boardTileWidth
                    + boardTileWidth * 0.3f;
            float y = screen.cellY((int) effect.position.y()) + boardTileHeight * 0.3f;
            if (effect.staticImage) {
                screen.assets().drawStaticEffect(effect.path, x, y, effect.scale);
            } else {
                screen.drawPam(effect.path, effect.state, effect.time, x, y, effect.scale, effect.loop);
            }
        }
        effects.removeIf(e -> e.time > e.duration);
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
            if (!drawProjectilePam(projectile, age)) {
                drawSmallDot(projectile.getPosition(), new Color(0.95f, 0.9f, 0.18f, 1f));
            }
        }
        spawnImpactEffectsForSpentProjectiles();
        for (ZombieProjectile projectile : screen.session.getZombieProjectiles()) {
            if (!drawZombieProjectilePam(projectile, delta)) {
                drawSmallDot(projectile.getPosition(), new Color(0.8f, 0.18f, 0.18f, 1f));
            }
        }
        projectileAnimTimes.keySet().removeIf(p -> !screen.session.getProjectiles().contains(p));
        projectileTraces.keySet().removeIf(p -> !screen.session.getProjectiles().contains(p));
        zombieProjectileAnimTimes.keySet().removeIf(p -> !screen.session.getZombieProjectiles().contains(p));
    }

    // While a Gargantuar-thrown imp is airborne it isn't a Zombie yet (it
    // only spawns as one on landing), so it's rendered here directly using
    // its own PAM in the "fly" animation state.
    private boolean drawZombieProjectilePam(ZombieProjectile projectile, float delta) {
        if (!(projectile instanceof GargantuarImpProjectile impProjectile)) return false;
        Position position = projectile.getPosition();
        if (position == null) return false;

        String path = ZombieAnimationRegistry.pathFor(impProjectile.getImpAlias(), screen.seasonFolder);
        if (path == null) return false;

        float age = zombieProjectileAnimTimes.getOrDefault(projectile, 0f) + delta;
        zombieProjectileAnimTimes.put(projectile, age);

        float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth() - 10f;
        float y = screen.cellY(position.y()) + 40f;
        return screen.drawPam(path, "fly", age, x, y, 0.52f, impProjectile.isFacingRight());
    }

    private void spawnImpactEffectsForSpentProjectiles() {
        if (projectileTraces.isEmpty()) return;
        for (Map.Entry<Projectile, ProjectileTrace> tracked : projectileTraces.entrySet()) {
            if (screen.session.getProjectiles().contains(tracked.getKey())) continue;
            ProjectileTrace trace = tracked.getValue();
            if (trace.position == null || isOffBoard(trace.position)) continue;

            ProjectileEffectAssets.AssetEntry entry = resolveImpactEntry(trace);
            if (entry == null) continue;
            impactEffects.add(new TimedPamEffect(entry.path(), entry.state(),
                    entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP,
                    entry.isStaticImage(), trace.position, IMPACT_EFFECT_DURATION,
                    PROJECTILE_PAM_SCALE));
        }
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

        ProjectileEffectAssets.Variant variant = source.isPlantFoodActive()
                ? ProjectileEffectAssets.Variant.PLANT_FOOD
                : ProjectileEffectAssets.Variant.NORMAL;
        List<ProjectileEffectAssets.AssetEntry> entries = ProjectileEffectAssets.get(
                source.getName(), ProjectileEffectAssets.Kind.PROJECTILE, variant);
        if (entries.isEmpty() && variant == ProjectileEffectAssets.Variant.PLANT_FOOD) {
            entries = ProjectileEffectAssets.get(source.getName(),
                    ProjectileEffectAssets.Kind.PROJECTILE, ProjectileEffectAssets.Variant.NORMAL);
        }
        if (entries.isEmpty()) return false;

        ProjectileEffectAssets.AssetEntry entry =
                entries.get(Math.min(projectile.getAssetVariant(), entries.size() - 1));
        boolean loop = entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP;

        float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                + screen.getBoardTileWidth() * 0.41f;
        float y = screen.cellY((int) position.y()) + screen.getBoardTileHeight() * 0.42f;
        float scaleFactor = 2.0f;

        if (entry.isStaticImage()) {
            return screen.assets().drawStaticEffect(entry.path(), x, y, STATIC_PROJECTILE_SCALE);
        }
        return screen.drawPam(entry.path(), entry.state(), age, x, y, PROJECTILE_PAM_SCALE * scaleFactor, loop);
    }

    private void drawSmallDot(Position p, Color color) {
        if (p == null) return;
        float x = GameScreen.BOARD_X + (float) p.x() * screen.getBoardTileWidth()
                + screen.getBoardTileWidth() * 0.41f;
        float y = screen.cellY((int) p.y()) + screen.getBoardTileHeight() * 0.42f;
        screen.drawFallback(x, y, 18f, 18f, color);
    }
}