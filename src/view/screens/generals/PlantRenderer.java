package view.screens.generals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import controller.assets.GameAssetManager;
import controller.assets.ProjectileEffectAssets;
import model.collections.animations.AnimationFactory;
import model.collections.plant.AbilityType;
import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.plant.PlantType;
import model.collections.zombie.Zombie;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the plants on the lawn and owns their animation state: the idle/attack clip
 * choice, the fire-event detection that opens an attack window, stack- and Split-Pea-aware
 * clip names, the chill frost overlay, and the plant-food glow.
 */
class PlantRenderer {

    private static final float DEFAULT_PLANT_ATTACK_DURATION = 0.4f;
    private static final float EXPLODING_PLANT_EFFECT_DURATION = 0.7f;

    private final GameScreen screen;

    private final Map<Plant, Float> plantAnimTimes = new IdentityHashMap<>();
    // Fire-event detection + one-shot "attack" clip playback for plants (see drawPlants).
    private final Map<Plant, Double> plantLastCooldown = new IdentityHashMap<>();
    private final Map<Plant, Float> plantAttackAnimTimes = new IdentityHashMap<>();
    private final Map<Plant, Float> plantAttackWindow = new IdentityHashMap<>();
    // Tracks whether the fire event currently playing out in plantAttackAnimTimes was
    // triggered while the plant was plant-food-boosted, so it plays "plantfood" instead
    // of the normal "attack" clip - see drawPlants.
    private final Map<Plant, Boolean> plantAttackIsBoosted = new IdentityHashMap<>();
    // Split Pea shoots both forward (right) and backward (left) in the same volley, but only
    // toward sides that actually have a target in range. Its PAM has three attack clips for
    // this - "attack" (right only), "attack3" (left only), "attack2" (both sides) - so the
    // side(s) that fired are captured once when the fire event is detected and held here for
    // the rest of the attack window, so the clip choice doesn't flicker mid-animation.
    private final Map<Plant, String> plantAttackBaseState = new IdentityHashMap<>();

    PlantRenderer(GameScreen screen) {
        this.screen = screen;
    }

    float animTimeFor(Plant plant) {
        return plantAnimTimes.getOrDefault(plant, 0f);
    }

    void drawPlants(float delta, float bw, float bh) {
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        for (Plant plant : new ArrayList<>(screen.session.getPlants())) {
            if (plant == null || plant.getPosition() == null) continue;
            boolean frozenInIce = FrostbiteFreezing.isFrozenInIce(screen.session, plant);
            // One-shot plants (bombs, mints, Gold Bloom) hold a fuse in PREPPING before their
            // payload resolves; that window exists so their own explode/intro clip can play,
            // so it must run forward once instead of looping the idle clip.
            boolean prepping = plant.getPlantState() == Plant.PlantState.PREPPING;
            float t = plantAnimTimes.getOrDefault(plant, 0f);
            if (!frozenInIce) {
                t += delta;
                if (!prepping) {
                    float idleDuration = screen.pam().resolvePlantClipDuration(plant.getName(),
                            plantStackState(plant, "idle"));
                    if (idleDuration > 0f) t %= idleDuration;
                }
                plantAnimTimes.put(plant, t);
            }
            Position p = screen.visualPositionFor(plant);
            float x = GameScreen.BOARD_X + (float) p.x() * boardTileWidth;
            float y = screen.cellY((int) p.y());

            float plantOffsetX = x + 30f;
            float plantOffsetY = y + 40f;

            String path = AnimationFactory.pathForDisplayName(plant.getName());

            // The model has no "attacking" state - ActStrategy.act() fires a shot the
            // instant internalTimer hits 0 and immediately resets it to actionInterval.
            // So a fire event is detected here by watching that reset happen (cooldown
            // was <= 0 last frame, is > 0 now), and a short "attack" window is opened
            // for plantAttackAnimTimes/plantAttackWindow to ride out.
            double cooldown = plant.getIntervalTimer();
            Double lastCooldown = plantLastCooldown.put(plant, cooldown);
            if (!frozenInIce && lastCooldown != null && cooldown > lastCooldown + 0.05) {
                boolean boosted = plant.isPlantFoodActive();
                String baseAttackState = resolveAttackBaseState(plant);
                String durationState = boosted ? "plantfood" : baseAttackState;
                float attackDuration = screen.pam().resolvePlantClipDuration(plant.getName(), durationState);
                if (attackDuration <= 0f) attackDuration = DEFAULT_PLANT_ATTACK_DURATION;
                plantAttackAnimTimes.put(plant, 0f);
                plantAttackWindow.put(plant, attackDuration);
                plantAttackIsBoosted.put(plant, boosted);
                plantAttackBaseState.put(plant, baseAttackState);
            }

            Float attackTime = plantAttackAnimTimes.get(plant);
            if (attackTime != null && !frozenInIce) {
                attackTime += delta;
                float window = plantAttackWindow.getOrDefault(plant, DEFAULT_PLANT_ATTACK_DURATION);
                if (attackTime >= window) {
                    plantAttackAnimTimes.remove(plant);
                    plantAttackWindow.remove(plant);
                    plantAttackIsBoosted.remove(plant);
                    plantAttackBaseState.remove(plant);
                } else {
                    plantAttackAnimTimes.put(plant, attackTime);
                }
            }

            boolean attacking = plantAttackAnimTimes.containsKey(plant);
            boolean attackIsBoosted = attacking && Boolean.TRUE.equals(plantAttackIsBoosted.get(plant));
            String preferredState;
            float animTime;
            if (prepping) {
                preferredState = plant.getAbilityType() == AbilityType.MINT_FAMILY_BOOST
                        ? "intro"
                        : screen.pam().resolveFuseClipState(plant.getName());
                animTime = t;
            } else if (plant.getVisualAnimationState() != null) {
                // Explicit special/melee Plant Food animation states have priority over the
                // generic cooldown-driven attack detector.
                preferredState = plant.getVisualAnimationState();
                animTime = (float) plant.getVisualAnimationElapsed();
            } else if (attacking) {
                preferredState = attackIsBoosted ? "plantfood"
                        : plantAttackBaseState.getOrDefault(plant, plantStackState(plant, "attack"));
                animTime = plantAttackAnimTimes.get(plant);
            } else {
                preferredState = resolveIdleState(plant);
                animTime = t;
            }

            boolean meleePlant = "Bonk Choy".equalsIgnoreCase(plant.getName())
                    || "Wasabi Whip".equalsIgnoreCase(plant.getName())
                    || "Chomper".equalsIgnoreCase(plant.getName());

            boolean mirror = meleePlant && plant.isMeleeFacingLeft()
                    && (attacking
                    || preferredState.startsWith("attack")
                    || "bite_end".equals(preferredState)
                    || "special".equals(preferredState)
                    || "special_idle".equals(preferredState));

            // special_idle is a looping chew animation. The visual-state timer itself
            // is finite (10 seconds), so wrap only the clip time, not the state lifetime.
            if ("special_idle".equals(preferredState)) {
                float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
                if (clipDuration > 0f) animTime %= clipDuration;
            }

            if (!screen.drawPam(path, preferredState, animTime, plantOffsetX , plantOffsetY, 0.55f, mirror)) {
                TextureRegion region = GameAssetManager.get().getPlantRegion(plant.getName());
                screen.drawEntity(region, plantOffsetX, plantOffsetY, boardTileWidth, boardTileHeight,
                        new Color(0.2f, 0.65f, 0.22f, 1f), GameScreenGraphics.initials(plant.getName()));
            }
            int chill = plant.getChillLevel();
            if (chill > 0 && chill < 3) {
                GameScreenAssets assets = screen.assets();
                Texture chillTexture = chill == 1 ? assets.plantIceBlockTexture1() : assets.plantIceBlockTexture2();
                if (chillTexture == null) {
                    chillTexture = assets.plantIceBlockTexture1() != null
                            ? assets.plantIceBlockTexture1() : assets.plantIceBlockTexture2();
                }
                if (chillTexture == null) chillTexture = assets.plantIceBlockTexture3();

                if (chillTexture != null) {
                    float alpha = chill == 1 ? FrostbiteRenderer.PLANT_ICE_ALPHA_1 : FrostbiteRenderer.PLANT_ICE_ALPHA_2;
                    float levelScale = FrostbiteRenderer.plantIceScaleFor(chill);
                    float offsetX = FrostbiteRenderer.plantIceOffsetXFor(chill);
                    float offsetY = FrostbiteRenderer.plantIceOffsetYFor(chill);
                    float drawW = chillTexture.getWidth() * screen.boardFitScale() * levelScale;
                    float drawH = chillTexture.getHeight() * screen.boardFitScale() * levelScale;
                    float drawX = x + (boardTileWidth - drawW) * 0.5f + offsetX;
                    float drawY = y + (boardTileHeight - drawH) * 0.5f + offsetY;
                    screen.batch.setColor(1f, 1f, 1f, alpha);
                    screen.batch.draw(chillTexture, drawX, drawY, drawW, drawH);
                    screen.batch.setColor(Color.WHITE);
                } else {
                    float alpha = chill == 1 ? 0.35f : 0.55f;
                    screen.batch.setColor(0.75f, 0.93f, 1f, alpha);
                    screen.batch.draw(screen.whitePixel, x + 9f, y + 7f, boardTileWidth - 18f, boardTileHeight - 12f);
                    screen.batch.setColor(Color.WHITE);
                }
            }
        }
        plantAnimTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantLastCooldown.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackAnimTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackWindow.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackIsBoosted.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackBaseState.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
    }

    /**
     * Stackable plants (e.g. Pea Pod, tagged {@link PlantTag#STACK}) have a separate clip per
     * number of peas: "idle"/"attack" for 1 pea, "idle2".."idle5"/"attack2".."attack5" for
     * 2-5 peas. Appends the current stack count to {@code baseState} for those plants only;
     * everything else keeps using the plain base state name.
     */
    private String plantStackState(Plant plant, String baseState) {
        if (plant.getTags().contains(PlantTag.STACK)) {
            int stackNumber = plant.getStackNumber();
            if (stackNumber > 1) {
                String separator = "attack".equals(baseState) ? " " : "";
                return baseState + separator + stackNumber;
            }
        }
        return baseState;
    }

    /**
     * Picks the base "attack" clip name for a plant's just-fired volley. Every plant except
     * Split Pea keeps using {@link #plantStackState}'s plain "attack" (or "attack2".."attack5"
     * for STACK-tagged plants); Split Pea gets its own side-aware resolution since it can fire
     * right, left, or both in the same volley - see {@link #splitPeaAttackBaseState}.
     */
    private String resolveIdleState(Plant plant) {
        if (plant != null && "Kiwibeast".equalsIgnoreCase(plant.getName())) {
            return switch (plant.getGrowthStage()) {
                case 2 -> "idle_stage2_2";
                case 3 -> "idle_stage3_3";
                default -> "idle";
            };
        }
        return plantStackState(plant, "idle");
    }

    private String resolveAttackBaseState(Plant plant) {
        if (plant != null && "Kiwibeast".equalsIgnoreCase(plant.getName())) {
            return switch (plant.getGrowthStage()) {
                case 2 -> "attack_stage2";
                case 3 -> "attack_stage3";
                default -> "attack";
            };
        }
        if (plant != null && "Split Pea".equalsIgnoreCase(plant.getName())) {
            return splitPeaAttackBaseState(plant);
        }
        if (plant != null && "Chomper".equalsIgnoreCase(plant.getName())) {
            return "bite_end";
        }
        return plantStackState(plant, "attack");
    }

    /**
     * Split Pea shoots both forward (right, toward the zombies) and backward (left) in the
     * same volley, but {@link model.collections.plant.actstrategy.ShootStrategy} only actually
     * launches a projectile toward a side that has a target (zombie or grave) in range. Its
     * PAM mirrors that with three clips: "attack" (right side only), "attack3" (left side
     * only), "attack2" (both sides). This picks the matching clip by re-checking, purely for
     * display, which side(s) have a target right now using the same same-row / in-range rule
     * ShootStrategy.act() uses - it doesn't change what actually gets fired.
     */
    private String splitPeaAttackBaseState(Plant plant) {
        boolean rightHasTarget = splitPeaSideHasTarget(plant, 1.0);
        boolean leftHasTarget = splitPeaSideHasTarget(plant, -1.0);
        if (rightHasTarget && leftHasTarget) return "attack2";
        if (leftHasTarget) return "attack3";
        return "attack";
    }

    /** Whether Split Pea has a zombie or grave target on the given side (dxSign > 0 = right, < 0 = left). */
    private boolean splitPeaSideHasTarget(Plant plant, double dxSign) {
        if (plant == null || screen.session == null) return false;
        Position origin = plant.getPosition();
        if (origin == null) return false;

        for (Zombie zombie : screen.session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position zp = zombie.getPosition();
            double relX = zp.x() - origin.x();
            double relY = zp.y() - origin.y();
            if (Math.abs(relY) >= 0.75 || Math.signum(relX) != Math.signum(dxSign)) continue;
            if (plant.isWithinAttackRange(zp)) return true;
        }

        if (screen.session.getEnvironment() != null) {
            for (int row = 0; row < screen.session.getEnvironment().getRows(); row++) {
                for (int col = 0; col < screen.session.getEnvironment().getCols(); col++) {
                    Cell cell = screen.session.getEnvironment().getCell(row, col);
                    if (cell == null || !(cell.getObstacle() instanceof model.pitches.obstacles.Grave)) continue;
                    double relX = col - origin.x();
                    double relY = row - origin.y();
                    if (Math.abs(relY) >= 0.75 || Math.signum(relX) != Math.signum(dxSign)) continue;
                    if (plant.isWithinAttackRange(new Position(col, row))) return true;
                }
            }
        }
        return false;
    }

    void trackExplodedPlants(List<Plant> alivePlantsBeforeTick) {
        List<Plant> stillAlive = screen.session.getPlants();
        for (Plant plant : alivePlantsBeforeTick) {
            if (stillAlive.contains(plant)) continue;
            if (plant.getPosition() == null || plant.getHP() <= 0) continue;
            if (plant.getType() != PlantType.EXPLOSIVE) continue;

            ProjectileEffectAssets.AssetEntry entry = screen.effects().resolveExplosionEntry(plant.getName());
            if (entry == null) continue;

            boolean loop = entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP;
            Position position = plant.getPosition();
            screen.effects().addExplodingPlantEffect(entry, loop, position, EXPLODING_PLANT_EFFECT_DURATION);
            plantAnimTimes.remove(plant);
            plantAttackAnimTimes.remove(plant);
        }
    }
}