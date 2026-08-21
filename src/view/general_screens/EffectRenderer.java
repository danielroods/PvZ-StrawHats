package view.general_screens;

import com.badlogic.gdx.graphics.Color;

import controller.assets.ProjectileEffectAssets;
import model.collections.plant.Plant;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.projectile.zombie_projectile.ZombiePeaProjectile;
import model.projectile.zombie_projectile.ZombieProjectile;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

class EffectRenderer {

    static final float PROJECTILE_PAM_SCALE = 0.35f;

    private static final float IMPACT_EFFECT_DURATION = 0.35f;
    private static final float STATIC_PROJECTILE_SCALE = 0.25f;
    private static final String ZOMBIE_PEA_PAM =
            "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM";
    private static final String ZOMBIE_PEA_SPLAT_PAM =
            "768/INITIAL/EFFECTS/SPLAT_PEA/SPLAT_PEA.PAM";

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
    private final Map<ZombieProjectile, Position> zombieProjectileTraces = new IdentityHashMap<>();

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
        drawZombieProjectiles(delta);
        projectileAnimTimes.keySet().removeIf(p -> !screen.session.getProjectiles().contains(p));
        projectileTraces.keySet().removeIf(p -> !screen.session.getProjectiles().contains(p));
    }

    private void drawZombieProjectiles(float delta) {
        List<ZombieProjectile> live = screen.session.getZombieProjectiles();
        for (ZombieProjectile projectile : live) {
            Position position = projectile.getPosition();
            if (position == null) continue;
            zombieProjectileTraces.put(projectile, position);
            float age = zombieProjectileAnimTimes.getOrDefault(projectile, 0f) + delta;
            zombieProjectileAnimTimes.put(projectile, age);
            if (!(projectile instanceof ZombiePeaProjectile)) {
                drawSmallDot(position, new Color(0.8f, 0.18f, 0.18f, 1f));
                continue;
            }
            float x = GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                    + screen.getBoardTileWidth() * 0.41f;
            float y = screen.cellY((int) position.y()) + screen.getBoardTileHeight() * 0.42f;
            if (!screen.drawPam(ZOMBIE_PEA_PAM, "animation", age, x, y,
                    PROJECTILE_PAM_SCALE * 2.0f, true)) {
                drawSmallDot(position, new Color(0.55f, 0.85f, 0.25f, 1f));
            }
        }

        for (Map.Entry<ZombieProjectile, Position> spent : zombieProjectileTraces.entrySet()) {
            if (live.contains(spent.getKey())) continue;
            if (!(spent.getKey() instanceof ZombiePeaProjectile pea)) continue;
            if (!pea.hasSplatted()) continue;
            Position position = spent.getValue();
            if (position == null || isOffBoard(position)) continue;
            impactEffects.add(new TimedPamEffect(ZOMBIE_PEA_SPLAT_PAM, "animation", false,
                    false, position, IMPACT_EFFECT_DURATION, PROJECTILE_PAM_SCALE));
        }
        zombieProjectileAnimTimes.keySet().removeIf(p -> !live.contains(p));
        zombieProjectileTraces.keySet().removeIf(p -> !live.contains(p));
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
