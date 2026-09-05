package view.screens.generals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import controller.assets.GameAssetManager;
import controller.assets.ProjectileEffectAssets;
import model.collections.animations.AnimationFactory;
import model.collections.plant.AbilityType;
import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.plant.PlantTag;
import model.collections.plant.PlantType;
import model.collections.plant.plantfood.TangleKelpPlantFood;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_effect.MageState;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.obstacles.OctopusWrap;
import model.pitches.obstacles.MoldBlock;
import model.collections.animations.ZombieAnimationRegistry;
import model.pitches.TileType;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;

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
    private static final float SHROOM_DEATH_HOLD_SECONDS = 1.2f;
    // Dark Wizard's hex (sheepening): transform once, then hold the sheep idle.
    private static final String SHEEP_PAM = "768/FULL/EFFECTS/DARK_WIZARD_SHEEPENING/DARK_WIZARD_SHEEPENING.PAM";
    private static final float DEFAULT_SHEEP_TRANSFORM_DURATION = 0.6f;
    private static final float SHEEP_SCALE = 0.5f;
    private static final String GRAPESHOT_ATTACK_STATE = "attack_t2";
    private static final String BOWLING_BULB_NAME = "Bowling Bulb";
    private static final String EXPLODE_O_NUT_BLINK_PAM =
            "768/INITIAL/EFFECTS/EXPLODEONUT_BLINK/EXPLODEONUT_BLINK.PAM";
    private static final float EXPLODE_O_NUT_BLINK_SCALE = 0.55f;
    private static final Color EXPLODE_O_NUT_BLINK_TINT = new Color(1f, 0.55f, 0.30f, 0.55f);
    private static final float EXPLODE_O_NUT_PLANTFOOD_OFF_HOLD = 0.2667f;

    private final GameScreen screen;

    /**
     * A short-lived, position-only playback of a plant's own "death" (Sea-shroom) or
     * "idle_stage4" (Puff-shroom lifespan-expiry death) clip. The plant itself is already
     * gone from session.getPlants() by the time this plays - see trackExplodedPlants -
     * so this is tracked independently of any live Plant instance, the same way
     * EffectRenderer's exploding-plant effects are.
     */
    private static final class DyingShroomEffect {
        final String path;
        final String state;
        final Position position;
        float elapsed;

        DyingShroomEffect(String path, String state, Position position) {
            this.path = path;
            this.state = state;
            this.position = position;
        }
    }

    private final List<DyingShroomEffect> dyingShroomEffects = new ArrayList<>();

    private final Map<Plant, Float> plantAnimTimes = new IdentityHashMap<>();
    private final Map<Plant, Float> sheepAnimTimes = new IdentityHashMap<>();
    private final Map<Cell, Float> octopusWrapAnimTimes = new IdentityHashMap<>();
    private final Map<Cell, Float> moldBlockAnimTimes = new IdentityHashMap<>();
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
    // Sun-shroom growth-stage detection: a short one-shot "growth_stageN" clip plays once
    // when GrowthTracker advances its stage (1->2 plays "growth_stage1", 2->3 plays
    // "growth_stage2"), then playback falls back to the new stage's idle - see drawPlants.
    private final Map<Plant, Integer> plantLastGrowthStage = new IdentityHashMap<>();
    private final Map<Plant, Float> plantGrowthAnimTimes = new IdentityHashMap<>();
    private final Map<Plant, Float> plantGrowthWindow = new IdentityHashMap<>();

    // Bowling Bulb cycles through 3 different balls (light/medium/heavy) each shot, both on
    // a normal attack and on every shot of its Plant Food burst - see BowlingBulbStrategy's
    // own ammoIndex. This mirrors that same 0/1/2 cycle here, advanced once per fire event
    // (the same cooldown-reset detection used for plantAttackAnimTimes above), so the right
    // "special"/"specialN" (attack), "plantfoodN" (Plant Food) and "reloadN" (post-attack
    // cooldown, non-Plant-Food only) clip can be picked for whichever ball just fired.
    private final Map<Plant, Integer> bowlingBulbShotIndex = new IdentityHashMap<>();

    // Doom-shroom has its own three-stage visual lifecycle: spawn -> stage idle ->
    // stage transform -> next stage idle. These are tracked per instance because the
    // plant can create new stage-1 Doom-shrooms when a higher-stage Doom explodes.
    private final Map<Plant, Float> doomSpawnAnimTimes = new IdentityHashMap<>();
    private final Map<Plant, Float> doomSpawnWindows = new IdentityHashMap<>();
    private final Map<Plant, Float> doomTransformAnimTimes = new IdentityHashMap<>();
    private final Map<Plant, Float> doomTransformWindows = new IdentityHashMap<>();
    private final Map<Plant, Integer> doomLastGrowthStage = new IdentityHashMap<>();
    private final Map<Plant, Boolean> plantFoodLastActive = new IdentityHashMap<>();

    private final Map<Plant, Integer> endurianAttackPhases = new IdentityHashMap<>();
    private final Map<Plant, Float> endurianAttackTimes = new IdentityHashMap<>();

    private final Map<Plant, Float> explodeONutBlinkTimes = new IdentityHashMap<>();
    private final Map<Plant, Boolean> explodeONutHadArmor = new IdentityHashMap<>();
    private final Map<Plant, Float> explodeONutArmorOffTimes = new IdentityHashMap<>();

    private final Map<Plant, Boolean> graveBusterEntrySeen = new IdentityHashMap<>();
    private final Map<Plant, Boolean> headbutterLettucePfWasActive = new IdentityHashMap<>();
    private final Map<Plant, Float> headbutterLettucePfOnTime = new IdentityHashMap<>();
    private final Map<Plant, Float> headbutterLettucePfOffTime = new IdentityHashMap<>();

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
            if (plant == null || !plant.isAlive() || plant.getPosition() == null) continue;
            boolean frozenInIce = FrostbiteFreezing.isFrozenInIce(screen.session, plant);
            boolean prepping = plant.getPlantState() == Plant.PlantState.PREPPING;
            if (plant.isGraveBuster() && graveBusterEntrySeen.put(plant, Boolean.TRUE) == null) {
                screen.effects().addGraveBusterDirtEffect(plant.getPosition(),
                        EffectRenderer.GRAVE_BUSTER_DIRT_ENTRY_STATE);
            }
            float t = plantAnimTimes.getOrDefault(plant, 0f);
            if (!frozenInIce) {
                t += delta;
                if (!prepping) {
                    String loopState;
                    if (plant.isWallNut()) loopState = plant.getWallNutHealthAnimationState();
                    else if (plant.isExplodeONut()) loopState = resolveExplodeONutIdleState(plant);
                    else if (plant.isTallNut()) loopState = plant.getTallNutHealthAnimationState();
                    else if (plant.isGarlic()) loopState = plant.getGarlicHealthAnimationState();
                    else if (plant.isEndurian()) loopState = resolveEndurianIdleState(plant);
                    else loopState = plantStackState(plant, "idle");
                    float idleDuration = screen.pam().resolvePlantClipDuration(plant.getName(), loopState);
                    if (idleDuration > 0f) t %= idleDuration;
                }
                plantAnimTimes.put(plant, t);
            }
            Position p = plant.getSquashVisualPosition();
            boolean squashJumping = "Squash".equalsIgnoreCase(plant.getName())
                    && plant.isSquashActionState()
                    && plant.getVisualAnimationState() != null
                    && plant.getSquashVisualOrigin() != null
                    && plant.getSquashVisualTarget() != null;
            if (squashJumping) {
                p = smoothSquashVisualPosition(plant);
                plant.setSquashVisualPosition(p);
            }
            if (p == null) p = screen.visualPositionFor(plant);
            float x = GameScreen.BOARD_X + (float) p.x() * boardTileWidth;
            float y = screen.cellY((int) p.y());

            float plantOffsetX = x + 30f;
            float plantOffsetY = y + 40f;

            if (findHexer(plant) != null) {
                if (!sheepAnimTimes.containsKey(plant)) {
                    AudioManager.get().playSound(AudioEnum.SFX_BLEAT);
                }
                SheepFrame sheepFrame = advanceSheepState(plant, delta);
                float sheepDrawX = plantOffsetX;
                float sheepDrawY = plantOffsetY;
                int sheepRow = (int) plant.getPosition().y();
                screen.queueRowDraw(sheepRow, () -> screen.pam().drawPamExact(
                        SHEEP_PAM, sheepFrame.state(), sheepFrame.time(),
                        sheepDrawX, sheepDrawY, SHEEP_SCALE, false));
                continue;
            } else {
                sheepAnimTimes.remove(plant);
            }

            String path = AnimationFactory.pathForDisplayName(plant.getName());

            if ("Hot Potato".equalsIgnoreCase(plant.getName()) && plant.isHotPotatoMeltEffectPending()) {
                screen.effects().addHotPotatoMeltEffect(plant.getPosition());
                plant.setHotPotatoMeltEffectPending(false);
            }

            boolean plantFoodActiveNow = plant.isPlantFoodActive();
            Boolean plantFoodWasActive = plantFoodLastActive.put(plant, plantFoodActiveNow);
            if (plantFoodActiveNow && !Boolean.TRUE.equals(plantFoodWasActive)) {
                AudioManager.get().playSound(AudioEnum.SFX_PLANT_FOOD);
            }

            // The model has no "attacking" state - ActStrategy.act() fires a shot the
            // instant internalTimer hits 0 and immediately resets it to actionInterval.
            // So a fire event is detected here by watching that reset happen (cooldown
            // was <= 0 last frame, is > 0 now), and a short "attack" window is opened
            // for plantAttackAnimTimes/plantAttackWindow to ride out.
            double cooldown = plant.getIntervalTimer();
            Double lastCooldown = plantLastCooldown.put(plant, cooldown);
            if (!frozenInIce && !"Doom-shroom".equalsIgnoreCase(plant.getName())
                    && lastCooldown != null && cooldown > lastCooldown + 0.05) {
                if (isBowlingBulb(plant)) {
                    int nextShotIndex = (bowlingBulbShotIndex.getOrDefault(plant, -1) + 1) % 3;
                    bowlingBulbShotIndex.put(plant, nextShotIndex);
                }
                boolean boosted = plant.isPlantFoodActive();
                String baseAttackState = resolveAttackBaseState(plant);
                String durationState = boosted ? plantFoodClipState(plant) : baseAttackState;
                float attackDuration = screen.pam().resolvePlantClipDuration(plant.getName(), durationState);
                if (attackDuration <= 0f) attackDuration = DEFAULT_PLANT_ATTACK_DURATION;
                plantAttackAnimTimes.put(plant, 0f);
                plantAttackWindow.put(plant, attackDuration);
                plantAttackIsBoosted.put(plant, boosted);
                plantAttackBaseState.put(plant, baseAttackState);
                playFireEventSound(plant);
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

            // Doom-shroom has explicit stage clips supplied by its PAM. A newly planted
            // Doom starts with stage1_spawn; each GrowthTracker transition plays the
            // corresponding transform clip once before settling into the new stage idle.
            if ("Doom-shroom".equalsIgnoreCase(plant.getName())) {
                int stage = Math.max(1, Math.min(3, plant.getGrowthStage()));
                Integer lastStage = doomLastGrowthStage.put(plant, stage);
                if (!frozenInIce && lastStage == null) {
                    float duration = screen.pam().resolvePlantClipDuration(plant.getName(), "stage1_spawn");
                    if (duration <= 0f) duration = DEFAULT_PLANT_ATTACK_DURATION;
                    doomSpawnAnimTimes.put(plant, 0f);
                    doomSpawnWindows.put(plant, duration);
                } else if (!frozenInIce && lastStage != null && stage > lastStage) {
                    String transformState = "stage" + lastStage + "_transform";
                    float duration = screen.pam().resolvePlantClipDuration(plant.getName(), transformState);
                    if (duration <= 0f) duration = DEFAULT_PLANT_ATTACK_DURATION;
                    doomTransformAnimTimes.put(plant, 0f);
                    doomTransformWindows.put(plant, duration);
                }
            }

            Float doomSpawnTime = doomSpawnAnimTimes.get(plant);
            if (doomSpawnTime != null && !frozenInIce) {
                doomSpawnTime += delta;
                float window = doomSpawnWindows.getOrDefault(plant, DEFAULT_PLANT_ATTACK_DURATION);
                if (doomSpawnTime >= window) {
                    doomSpawnAnimTimes.remove(plant);
                    doomSpawnWindows.remove(plant);
                } else {
                    doomSpawnAnimTimes.put(plant, doomSpawnTime);
                }
            }

            Float doomTransformTime = doomTransformAnimTimes.get(plant);
            if (doomTransformTime != null && !frozenInIce) {
                doomTransformTime += delta;
                float window = doomTransformWindows.getOrDefault(plant, DEFAULT_PLANT_ATTACK_DURATION);
                if (doomTransformTime >= window) {
                    doomTransformAnimTimes.remove(plant);
                    doomTransformWindows.remove(plant);
                } else {
                    doomTransformAnimTimes.put(plant, doomTransformTime);
                }
            }

            // Sun-shroom grows through 3 stages over time (GrowthTracker). Every time it
            // steps up a stage, briefly play the matching one-shot "growth_stageN" clip
            // before settling back into that stage's idle/plantfood loop.
            if (isSunShroom(plant)) {
                int stage = plant.getGrowthStage();
                Integer lastStage = plantLastGrowthStage.put(plant, stage);
                if (!frozenInIce && lastStage != null && stage > lastStage) {
                    String growthState = "growth_stage" + (stage - 1);
                    float growthDuration = screen.pam().resolvePlantClipDuration(plant.getName(), growthState);
                    if (growthDuration <= 0f) growthDuration = DEFAULT_PLANT_ATTACK_DURATION;
                    plantGrowthAnimTimes.put(plant, 0f);
                    plantGrowthWindow.put(plant, growthDuration);
                }
            }
            Float growthTime = plantGrowthAnimTimes.get(plant);
            if (growthTime != null && !frozenInIce) {
                growthTime += delta;
                float growthWindow = plantGrowthWindow.getOrDefault(plant, DEFAULT_PLANT_ATTACK_DURATION);
                if (growthTime >= growthWindow) {
                    plantGrowthAnimTimes.remove(plant);
                    plantGrowthWindow.remove(plant);
                } else {
                    plantGrowthAnimTimes.put(plant, growthTime);
                }
            }
            boolean growing = plantGrowthAnimTimes.containsKey(plant) && !plant.isPlantFoodActive();

            boolean endurian = plant.isEndurian();
            if (endurian && !frozenInIce) advanceEndurianAttack(plant, delta);
            int endurianPhase = endurian ? endurianAttackPhases.getOrDefault(plant, 0) : 0;

            boolean explodeONut = plant.isExplodeONut();
            if (explodeONut && !frozenInIce) advanceExplodeONutTimers(plant, delta);

            boolean headbutterLettuce = isHeadbutterLettuce(plant);
            if (headbutterLettuce && !frozenInIce) advanceHeadbutterLettuceTimers(plant, delta);

            float capturedGrowthTime = growthTime == null ? 0f : growthTime;
            PlantVisualArgs visualArgs = new PlantVisualArgs(frozenInIce, prepping, t, attacking,
                    attackIsBoosted, endurian, endurianPhase, explodeONut, headbutterLettuce, growing,
                    capturedGrowthTime, path, x, y, plantOffsetX, plantOffsetY, squashJumping,
                    boardTileWidth, boardTileHeight);
            int plantRow = (int) plant.getPosition().y();
            screen.queueRowDraw(plantRow, () -> drawPlantVisual(plant, visualArgs));

            if (plant.getPlantFoodEffect() instanceof TangleKelpPlantFood tangleKelpPlantFood) {
                drawTangleKelpRemoteAttacks(tangleKelpPlantFood, boardTileWidth);
            }
        }
        plantAnimTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantLastCooldown.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackAnimTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackWindow.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackIsBoosted.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackBaseState.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantLastGrowthStage.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantGrowthAnimTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantGrowthWindow.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        sheepAnimTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantFoodLastActive.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        graveBusterEntrySeen.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        bowlingBulbShotIndex.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        endurianAttackPhases.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        endurianAttackTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        explodeONutBlinkTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        explodeONutHadArmor.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        explodeONutArmorOffTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        headbutterLettucePfWasActive.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        headbutterLettucePfOnTime.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        headbutterLettucePfOffTime.keySet().removeIf(p -> !screen.session.getPlants().contains(p));

        drawDyingShroomEffects(delta, boardTileWidth, boardTileHeight);
        drawOctopusWraps(delta, boardTileWidth, boardTileHeight);
        drawMoldBlocks(delta, boardTileWidth, boardTileHeight);
    }

    private record PlantVisualArgs(
            boolean frozenInIce, boolean prepping, float t, boolean attacking, boolean attackIsBoosted,
            boolean endurian, int endurianPhase, boolean explodeONut, boolean headbutterLettuce,
            boolean growing, float growthTime, String path, float x, float y, float plantOffsetX,
            float plantOffsetY, boolean squashJumping, float boardTileWidth, float boardTileHeight) { }

    private Map<String, Boolean> plantCostumeVisibility(Plant plant) {
        if (plant == null) return null;
        return PlantCostumeMask.forPlant(plant.getId(), plant.getName());
    }

    private void drawPlantVisual(Plant plant, PlantVisualArgs a) {
        boolean frozenInIce = a.frozenInIce();
        boolean prepping = a.prepping();
        float t = a.t();
        boolean attacking = a.attacking();
        boolean attackIsBoosted = a.attackIsBoosted();
        boolean endurian = a.endurian();
        int endurianPhase = a.endurianPhase();
        boolean explodeONut = a.explodeONut();
        boolean headbutterLettuce = a.headbutterLettuce();
        boolean growing = a.growing();
        float growthTime = a.growthTime();
        String path = a.path();
        float x = a.x();
        float y = a.y();
        float plantOffsetX = a.plantOffsetX();
        float plantOffsetY = a.plantOffsetY();
        boolean squashJumping = a.squashJumping();
        float boardTileWidth = a.boardTileWidth();
        float boardTileHeight = a.boardTileHeight();

        String preferredState;
        float animTime = t;
        boolean potatoMine = plant.isPotatoMine();
        boolean pumpkinHasArmor = plant.isPumpkin()
                && plant.getArmor() != null
                && plant.getArmor().getHP() > 0;
        boolean wallNutHasPlantFoodArmor = plant.isWallNut()
                && plant.getArmor() != null
                && plant.getArmor().getHP() > 0;
        boolean tallNutHasPlantFoodArmor = plant.isTallNut()
                && plant.getArmor() != null
                && plant.getArmor().getHP() > 0;
        if (explodeONut) {
            preferredState = resolveExplodeONutState(plant);
            animTime = explodeONutAnimTime(plant, preferredState, t);
        } else if (endurian) {
            if ("plantfood_on".equals(plant.getVisualAnimationState())) {
                preferredState = "plantfood_on";
                animTime = (float) plant.getVisualAnimationElapsed();
            } else if (endurianPhase > 0) {
                preferredState = endurianAttackClip(plant, endurianPhase);
                animTime = endurianAttackTimes.getOrDefault(plant, 0f);
            } else {
                preferredState = resolveEndurianIdleState(plant);
                animTime = t;
            }
        } else if (pumpkinHasArmor) {
            preferredState = resolvePumpkinPlantFoodState(plant);
            animTime = plant.isPlantFoodActive()
                    ? (float) plant.getVisualAnimationElapsed()
                    : t;
            float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
            if (clipDuration > 0f) animTime %= clipDuration;
        } else if (wallNutHasPlantFoodArmor) {
            preferredState = resolveWallNutPlantFoodState(plant);
            animTime = t;
        } else if (tallNutHasPlantFoodArmor) {
            preferredState = "idle";
            float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
            animTime = t;
            if (clipDuration > 0f) animTime %= clipDuration;
        } else if (sunBeanHasPlantFoodShield(plant) && plant.getVisualAnimationState() == null) {
            // Sun Bean's Plant Food shell is the "plantfood" clip, and it stays up for as
            // long as the armour it granted survives - not just for the boost window. The
            // "plantfood_on" intro GrantArmor sets runs first through the visual-state
            // branch below, which is why this only takes over once that has finished.
            preferredState = "plantfood";
            animTime = t;
        } else if (plant.isWallNut() && plant.getVisualAnimationState() != null) {
            preferredState = plant.getVisualAnimationState();
            animTime = (float) plant.getVisualAnimationElapsed();
        } else if (plant.isSweetPotato() && plant.isPlantFoodActive()) {

            preferredState = "plantfood";
            animTime = (float) plant.getVisualAnimationElapsed();
            float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
            if (clipDuration > 0f) animTime %= clipDuration;
        } else if (potatoMine && plant.getVisualAnimationState() != null) {
            preferredState = plant.getVisualAnimationState();
            animTime = (float) plant.getVisualAnimationElapsed();
        } else if (plant.getVisualAnimationState() != null) {
            preferredState = plant.getVisualAnimationState();
            animTime = (float) plant.getVisualAnimationElapsed();
            if (plant.isPumpkin() && "idle_plantfood".equals(preferredState)) {
                float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
                if (clipDuration > 0f) animTime %= clipDuration;
            }
        } else if (potatoMine && !plant.isPotatoMineArmed()) {
            preferredState = "plant_idle";
            float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
            animTime = clipDuration > 0f ? (t % clipDuration) : t;
        } else if (potatoMine) {
            preferredState = "idle";
            float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
            animTime = clipDuration > 0f ? (t % clipDuration) : t;
        } else if ("Doom-shroom".equalsIgnoreCase(plant.getName())
                && doomSpawnAnimTimes.containsKey(plant)) {
            preferredState = "stage1_spawn";
            animTime = doomSpawnAnimTimes.get(plant);
        } else if ("Doom-shroom".equalsIgnoreCase(plant.getName())
                && doomTransformAnimTimes.containsKey(plant)) {
            int transformFrom = Math.max(1, plant.getGrowthStage() - 1);
            preferredState = "stage" + transformFrom + "_transform";
            animTime = doomTransformAnimTimes.get(plant);
        } else if ("Doom-shroom".equalsIgnoreCase(plant.getName())) {
            preferredState = "stage" + Math.max(1, Math.min(3, plant.getGrowthStage())) + "_idle";
            animTime = t;
        } else if (prepping) {
            if ("Cherry Bomb".equalsIgnoreCase(plant.getName())) {
                preferredState = "attack";
            } else if ("Grapeshot".equalsIgnoreCase(plant.getName())) {
                preferredState = GRAPESHOT_ATTACK_STATE;
            } else if (plant.getAbilityType() == AbilityType.MINT_FAMILY_BOOST) {
                preferredState = "intro";
            } else {
                preferredState = screen.pam().resolveFuseClipState(plant.getName());
            }
            animTime = t;
        } else if (attacking) {
            preferredState = attackIsBoosted ? plantFoodClipState(plant)
                    : plantAttackBaseState.getOrDefault(plant, plantStackState(plant, "attack"));
            animTime = plantAttackAnimTimes.get(plant);
        } else if (isBowlingBulb(plant) && plant.isPlantFoodActive()) {
            preferredState = "plantfood_idle";
            float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
            animTime = clipDuration > 0f ? (t % clipDuration) : t;
        } else if (isBowlingBulb(plant) && plant.getIntervalTimer() > 0.001) {
            preferredState = bowlingBulbReloadState(plant);
            float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
            animTime = clipDuration > 0f ? (t % clipDuration) : t;
        } else if ("Torchwood".equalsIgnoreCase(plant.getName()) && plant.isPlantFoodActive()) {
            preferredState = "plantfood";
            animTime = t;
        } else if (headbutterLettuce
                && (plant.isPlantFoodActive() || headbutterLettucePfOffTime.containsKey(plant))) {
            preferredState = headbutterLettuceState(plant);
            animTime = headbutterLettuceAnimTime(plant, preferredState, t);
        } else if (plant.isPumpkin() && plant.isPlantFoodActive()) {
            preferredState = "idle_plantfood";
            animTime = (float) plant.getVisualAnimationElapsed();
        } else if (plant.isTallNut() && plant.isPlantFoodActive()) {
            preferredState = "idle";
            animTime = t;
        } else if (isSunProducingPlant(plant)
                && plant.isPlantFoodActive()
                && plant.getVisualAnimationState() != null) {
            // Sun producers use their Plant Food clip as a true one-shot. The model
            // starts the clip when Plant Food is activated and keeps it alive only for
            // the exact clip duration; do not modulo the time here, otherwise the last
            // frames would loop/hold before the suns are dropped.
            preferredState = plant.getVisualAnimationState();
            float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
            float elapsed = (float) plant.getVisualAnimationElapsed();
            animTime = clipDuration > 0f
                    ? Math.min(elapsed, Math.max(0f, clipDuration - 0.0001f))
                    : elapsed;
        } else if (showsPlantFoodLoopForFullDuration(plant) && plant.isPlantFoodActive()) {
            preferredState = plantFoodClipState(plant);
            animTime = t;
        } else if (growing) {
            preferredState = "growth_stage" + (plant.getGrowthStage() - 1);
            animTime = growthTime;
        } else {
            preferredState = resolveIdleState(plant);
            animTime = t;
        }

        if (squashJumping) {
            plantOffsetY += squashJumpArcOffset(plant, boardTileHeight);
        }

        boolean meleePlant = "Wasabi Whip".equalsIgnoreCase(plant.getName())
                || "Chomper".equalsIgnoreCase(plant.getName())
                || "Squash".equalsIgnoreCase(plant.getName());

        boolean mirror = meleePlant && plant.isMeleeFacingLeft()
                && (attacking
                || preferredState.startsWith("attack")
                || "bite_end".equals(preferredState)
                || "special".equals(preferredState)
                || "special_idle".equals(preferredState));
        if ("special_idle".equals(preferredState)) {
            float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
            if (clipDuration > 0f) animTime %= clipDuration;
        }

        boolean drawn;
        boolean squashExactState = "Squash".equalsIgnoreCase(plant.getName())
                && plant.getVisualAnimationState() != null;
        boolean pumpkinPlantFoodState = plant.isPumpkin()
                && preferredState != null
                && (preferredState.equals("idle_plantfood")
                || preferredState.equals("idle_plantfood2")
                || preferredState.equals("idle_plantfood3")
                || preferredState.equals("idle_plantfood4"));
        boolean pumpkinExactState = plant.isPumpkin()
                && ("idle".equals(preferredState)
                || "idle2".equals(preferredState)
                || "idle3".equals(preferredState));
        boolean explodeONutArmorState = explodeONut
                && ("plantfood".equals(preferredState)
                || "plantfood2".equals(preferredState)
                || "plantfood3".equals(preferredState)
                || "plantfood_on".equals(preferredState));
        boolean explodeONutExactState = explodeONut && !explodeONutArmorState;
        boolean wallNutPlantFoodState = plant.isWallNut()
                && ("plantfood".equals(preferredState)
                || "plantfood2".equals(preferredState)
                || "plantfood3".equals(preferredState));
        boolean wallNutExactState = plant.isWallNut()
                && ("idle".equals(preferredState)
                || "damage".equals(preferredState)
                || "damage2".equals(preferredState)
                || "damage3".equals(preferredState));
        boolean tallNutArmorState = plant.isTallNut()
                && tallNutHasPlantFoodArmor;
        boolean tallNutExactState = plant.isTallNut()
                && ("idle".equals(preferredState)
                || "damage".equals(preferredState)
                || "damage2".equals(preferredState));
        boolean sunProducerPlantFoodExactState = isSunProducingPlant(plant)
                && plant.isPlantFoodActive()
                && plant.getVisualAnimationState() != null;
        boolean garlicExactState = plant.isGarlic()
                && ("idle".equals(preferredState)
                || "idle_damage".equals(preferredState)
                || "idle-damage2".equals(preferredState)
                || "plantfood".equals(preferredState));
        boolean sweetPotatoExactState = plant.isSweetPotato()
                && ("idle".equals(preferredState)
                || "idle_damage".equals(preferredState)
                || "idle_damage2".equals(preferredState)
                || "idle_damage3".equals(preferredState)
                || "idle2_damage3".equals(preferredState)
                || "plantfood".equals(preferredState));
        boolean potatoMineExactState = potatoMine
                && ("plant_idle".equals(preferredState)
                || "recover".equals(preferredState)
                || "idle".equals(preferredState)
                || "attack".equals(preferredState)
                || "plantfood2".equals(preferredState));
        if (explodeONutArmorState) {
            drawn = screen.drawPam(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false,
                    PlantCostumeMask.merge(explodeONutArmorVisibility(preferredState), plantCostumeVisibility(plant)));
        } else if (explodeONutExactState) {
            drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, plantCostumeVisibility(plant));
        } else if (potatoMineExactState) {
            drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, plantCostumeVisibility(plant));
        } else if (squashExactState) {
            boolean squashMirror = "turn".equals(preferredState) && plant.isMeleeFacingLeft();
            drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, squashMirror, plantCostumeVisibility(plant));
        } else if (pumpkinPlantFoodState) {
            Map<String, Boolean> pumpkinPfVisibility = new java.util.HashMap<>();
            pumpkinPfVisibility.put("pumpkin_armor_01", "idle_plantfood".equals(preferredState));
            pumpkinPfVisibility.put("pumpkin_armor_02", "idle_plantfood2".equals(preferredState));
            pumpkinPfVisibility.put("pumpkin_armor_03", "idle_plantfood3".equals(preferredState));
            pumpkinPfVisibility.put("pumpkin_armor_04", "idle_plantfood4".equals(preferredState));
            drawn = screen.drawPam(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, PlantCostumeMask.merge(pumpkinPfVisibility, plantCostumeVisibility(plant)));
        } else if (pumpkinExactState) {
            drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, plantCostumeVisibility(plant));
        } else if (wallNutPlantFoodState) {
            Map<String, Boolean> wallNutPfVisibility = new java.util.HashMap<>();
            wallNutPfVisibility.put("wallnut_plantfood_armor_01", "plantfood".equals(preferredState));
            wallNutPfVisibility.put("wallnut_plantfood_armor_02", "plantfood2".equals(preferredState));
            wallNutPfVisibility.put("wallnut_plantfood_armor_03", "plantfood3".equals(preferredState));
            drawn = screen.drawPam(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, PlantCostumeMask.merge(wallNutPfVisibility, plantCostumeVisibility(plant)));
        } else if (wallNutExactState) {
            drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, plantCostumeVisibility(plant));
        } else if (garlicExactState) {
            drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, plantCostumeVisibility(plant));
        } else if (sweetPotatoExactState) {
            drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, plantCostumeVisibility(plant));
        } else if (tallNutArmorState) {
            Map<String, Boolean> tallNutArmorVisibility = new java.util.HashMap<>();
            tallNutArmorVisibility.put("_tallnut_plantfood_armor", true);
            tallNutArmorVisibility.put("tallnut_plantfood_armor_norm", false);
            tallNutArmorVisibility.put("tallnut_plantfood_armor_damage_01", false);
            tallNutArmorVisibility.put("tallnut_plantfood_armor_damage_02", false);
            int stage = plant.getTallNutPlantFoodArmorStage();
            switch (stage) {
                case 1 -> tallNutArmorVisibility.put("tallnut_plantfood_armor_norm", true);
                case 2 -> tallNutArmorVisibility.put("tallnut_plantfood_armor_damage_01", true);
                case 3 -> tallNutArmorVisibility.put("tallnut_plantfood_armor_damage_02", true);
                default -> { }
            }
            drawn = screen.drawPam(path, "idle", animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, PlantCostumeMask.merge(tallNutArmorVisibility, plantCostumeVisibility(plant)));
        } else if (tallNutExactState) {
            drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, plantCostumeVisibility(plant));
        } else if (sunProducerPlantFoodExactState) {
            drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, false, plantCostumeVisibility(plant));
        } else if (endurian) {
            drawn = screen.drawPam(path, preferredState, animTime, plantOffsetX, plantOffsetY,
                    0.55f, false, PlantCostumeMask.merge(endurianVisibility(plant, preferredState), plantCostumeVisibility(plant)));
        } else if (isMagnetShroom(plant)) {
            // The "Magnet_Item" slot is where the real game swaps in whatever metal object
            // the magnet just pulled off a zombie. The shipped atlas has no such artwork
            // for it - MAGNETSHROOM_67X67, the image the PAM points that slot at, is a flat
            // purple placeholder square - so showing it drew a purple block over the plant.
            // Keep it hidden; the catch still reads through the plant's own catch/plantfood
            // clips and the metal the zombie loses. Plant#isMagnetItemVisible stays as the
            // model's record of "holding something" (it is synced to online clients), it
            // just has no artwork to draw for it.
            Map<String, Boolean> magnetVisibility = new java.util.HashMap<>();
            magnetVisibility.put(MAGNET_ITEM_ELEMENT, false);
            drawn = screen.drawPam(path, preferredState, animTime,
                    plantOffsetX, plantOffsetY, 0.55f, mirror, PlantCostumeMask.merge(magnetVisibility, plantCostumeVisibility(plant)));
        } else {
            drawn = screen.drawPam(path, preferredState, animTime, plantOffsetX, plantOffsetY, 0.55f, mirror, plantCostumeVisibility(plant));
        }
        if (!drawn) {
            TextureRegion region = GameAssetManager.get().getPlantRegion(plant.getName());
            screen.drawEntity(region, plantOffsetX, plantOffsetY, boardTileWidth, boardTileHeight,
                    new Color(0.2f, 0.65f, 0.22f, 1f), GameScreenGraphics.initials(plant.getName()));
        }

        if (explodeONut && !frozenInIce) {
            drawExplodeONutBlink(plant, plantOffsetX, plantOffsetY);
        }

        if (plant.isPumpkin() && plant.getArmor() != null && plant.getArmor().getHP() > 0
                && !pumpkinPlantFoodState) {
            drawPumpkinArmorOverlay(plant, plantOffsetX, plantOffsetY);
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

    private void drawOctopusWraps(float delta, float boardTileWidth, float boardTileHeight) {
        if (screen.session.getEnvironment() == null) return;

        String path = "768/FULL/EFFECTS/ZOMBIE_OCTOPUS_PROJECTILE/ZOMBIE_OCTOPUS_PROJECTILE.PAM";
        if (path == null) return;

        for (int row = 0; row < screen.session.getEnvironment().getRows(); row++) {
            for (int col = 0; col < screen.session.getEnvironment().getCols(); col++) {
                Cell cell = screen.session.getEnvironment().getCell(row, col);
                if (cell == null || !(cell.getObstacle() instanceof OctopusWrap wrap)) continue;

                float time = octopusWrapAnimTimes.getOrDefault(cell, 0f) + delta;
                octopusWrapAnimTimes.put(cell, time);

                Position pos = wrap.getWrappedPlant() != null
                        ? wrap.getWrappedPlant().getPosition()
                        : new Position(col, row);
                if (pos == null) pos = new Position(col, row);

                float x = GameScreen.BOARD_X + (float) pos.x() * boardTileWidth + 20f;
                float y = screen.cellY(pos.y()) + 40f;

                if (!wrap.isDead()) {
                    float duration = screen.pam().resolveClipDuration("ZombieBeachOctopus", "animation3");
                    float animTime = duration > 0f ? time % duration : time;
                    screen.queueRowDraw(row, () -> screen.drawPam(path, "animation3", animTime, x, y, 0.52f, false));
                } else {
                    float duration = screen.pam().resolveClipDuration("ZombieBeachOctopus", "die");
                    float animTime = duration > 0f ? Math.min(time, duration) : time;
                    screen.queueRowDraw(row, () -> screen.drawPam(path, "die", animTime, x, y, 0.52f, false));
                    if (duration <= 0f || time >= duration) {
                        cell.setObstacle(null);
                        octopusWrapAnimTimes.remove(cell);
                    }
                }
            }
        }
        octopusWrapAnimTimes.keySet().removeIf(cell -> cell == null || cell.getObstacle() == null);
    }

    /**
     * Draws the idle mold animation over any tile blocked by {@link MoldBlock}
     * (e.g. the unplantable column in the "Not Every Where You Can Plant!" level).
     */
    private void drawMoldBlocks(float delta, float boardTileWidth, float boardTileHeight) {
        if (screen.session.getEnvironment() == null) return;

        for (int row = 0; row < screen.session.getEnvironment().getRows(); row++) {
            for (int col = 0; col < screen.session.getEnvironment().getCols(); col++) {
                Cell cell = screen.session.getEnvironment().getCell(row, col);
                if (cell == null || !(cell.getObstacle() instanceof MoldBlock)) continue;

                float time = moldBlockAnimTimes.getOrDefault(cell, 0f) + delta;
                moldBlockAnimTimes.put(cell, time);

                int finalRow = row;
                float x = GameScreen.BOARD_X + col * boardTileWidth + 20f;
                float y = screen.cellY(row) + 40f;
                screen.queueRowDraw(finalRow, () -> screen.drawPam(
                        MoldBlock.PAM_PATH, MoldBlock.PAM_CLIP, time, x, y, 0.55f, false));
            }
        }
        moldBlockAnimTimes.keySet().removeIf(cell -> cell == null || !(cell.getObstacle() instanceof MoldBlock));
    }

    /**
     * Tangle Kelp's Plant Food can drag zombies under on tiles other than its own - there's
     * no real plant standing there, so each such tile borrows a plain "attack" clip of the
     * same PAM for as long as {@link TangleKelpPlantFood#remoteAttackTiles()} reports it.
     */
    private void drawTangleKelpRemoteAttacks(TangleKelpPlantFood effect, float boardTileWidth) {
        List<Position> tiles = effect.remoteAttackTiles();
        if (tiles.isEmpty()) return;

        String path = AnimationFactory.pathForDisplayName("Tangle Kelp");
        float clipDuration = screen.pam().resolvePlantClipDuration("Tangle Kelp", "attack");
        float rawTime = (float) effect.remoteAttackElapsed();
        float time = clipDuration > 0f ? rawTime % clipDuration : rawTime;

        for (Position tile : tiles) {
            float tileX = GameScreen.BOARD_X + (float) tile.x() * boardTileWidth + 30f;
            float tileY = screen.cellY((int) tile.y()) + 40f;
            int tileRow = (int) tile.y();
            screen.queueRowDraw(tileRow, () -> screen.drawPam(path, "attack", time, tileX, tileY, 0.55f, false));
        }
    }

    /** Plays the brief "death"/"idle_stage4" clip queued up by trackExplodedPlants. */
    private void drawDyingShroomEffects(float delta, float boardTileWidth, float boardTileHeight) {
        if (dyingShroomEffects.isEmpty()) return;
        for (DyingShroomEffect effect : dyingShroomEffects) {
            effect.elapsed += delta;
            float x = GameScreen.BOARD_X + (float) effect.position.x() * boardTileWidth + 30f;
            float y = screen.cellY((int) effect.position.y()) + 40f;
            float elapsed = effect.elapsed;
            String effectPath = effect.path;
            String effectState = effect.state;
            int row = (int) effect.position.y();
            screen.queueRowDraw(row, () -> screen.drawPam(effectPath, effectState, elapsed, x, y, 0.55f, false));
        }
        dyingShroomEffects.removeIf(e -> e.elapsed >= SHROOM_DEATH_HOLD_SECONDS);
    }

    private float squashJumpArcOffset(Plant plant, float boardTileHeight) {
        String state = plant.getVisualAnimationState();
        if (state == null) return 0f;

        double elapsed = Math.max(0.0, plant.getVisualAnimationElapsed());
        double total = elapsed + Math.max(0.0, plant.getVisualAnimationRemaining());
        double arc;

        if (plant.getVisualAnimationRemaining() <= 0.0001) {
            if (state.startsWith("jump_up_")) return boardTileHeight * 0.9f;
            return 0f;
        }
        if (total <= 0.0001) return 0f;

        double t = Math.max(0.0, Math.min(1.0, elapsed / total));

        if (state.startsWith("jump_up_")) {
            arc = Math.sin(t * Math.PI * 0.5);
        } else if (state.startsWith("jump_down_")) {
            double fallT = Math.max(0.0, Math.min(1.0, elapsed / 0.20));
            arc = Math.cos(fallT * Math.PI * 0.5);
        } else if (state.startsWith("plantfood_jump_down_")) {
            arc = Math.sin(t * Math.PI);
        } else {
            return 0f;
        }

        return boardTileHeight * 0.9f * (float) Math.max(0.0, arc);
    }

    private Position smoothSquashVisualPosition(Plant plant) {
        Position origin = plant.getSquashVisualOrigin();
        Position target = plant.getSquashVisualTarget();
        if (origin == null || target == null) return plant.getSquashVisualPosition();

        String state = plant.getVisualAnimationState();
        double duration = plant.getVisualAnimationRemaining() + plant.getVisualAnimationElapsed();
        if (duration <= 0.0001) return new Position(origin.x(), origin.y());
        double t = Math.max(0.0, Math.min(1.0, plant.getVisualAnimationElapsed() / duration));

        if (state != null && state.startsWith("jump_up_")) {
            double eased = t * t * (3.0 - 2.0 * t);
            return new Position(
                    origin.x() + (target.x() - origin.x()) * 0.45 * eased,
                    origin.y() + (target.y() - origin.y()) * 0.45 * eased);
        }
        if (state != null && state.startsWith("jump_down_")) {
            final double fallDuration = 0.20;
            double fallT = Math.max(0.0, Math.min(1.0, plant.getVisualAnimationElapsed() / fallDuration));
            double eased = fallT * fallT * (3.0 - 2.0 * fallT);
            double startX = origin.x() + (target.x() - origin.x()) * 0.45;
            double startY = origin.y() + (target.y() - origin.y()) * 0.45;
            return new Position(
                    startX + (target.x() - startX) * eased,
                    startY + (target.y() - startY) * eased);
        }
        if (state != null && state.startsWith("plantfood_jump_down_")) {
            double eased = t * t * (3.0 - 2.0 * t);
            return new Position(
                    origin.x() + (target.x() - origin.x()) * eased,
                    origin.y() + (target.y() - origin.y()) * eased);
        }
        return new Position(origin.x(), origin.y());
    }

    /** Finds the Dark Wizard effect currently hexing this plant, if any. */
    private MageState findHexer(Plant plant) {
        if (plant.getPlantState() != Plant.PlantState.INCAPACITATED) return null;
        for (Zombie zombie : screen.session.getZombies()) {
            if (zombie == null) continue;
            if (zombie.getEffectStatus() instanceof MageState mage && mage.isHexed(plant)) return mage;
        }
        return null;
    }

    private record SheepFrame(String state, float time) { }

    /** Advances the sheep transform's state (in place of the plant while it is hexed). */
    private SheepFrame advanceSheepState(Plant plant, float delta) {
        float t = sheepAnimTimes.getOrDefault(plant, 0f) + delta;
        float transformDuration = AnimationFactory.exactClipDurationForPath(SHEEP_PAM, "animation");
        if (transformDuration <= 0f) transformDuration = DEFAULT_SHEEP_TRANSFORM_DURATION;
        String state;
        float time;
        if (t < transformDuration) {
            state = "animation";
            time = t;
        } else {
            state = "idle";
            float idleDuration = AnimationFactory.exactClipDurationForPath(SHEEP_PAM, "idle");
            time = idleDuration > 0f ? (t - transformDuration) % idleDuration : (t - transformDuration);
        }
        sheepAnimTimes.put(plant, t);
        return new SheepFrame(state, time);
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
        if (plant != null && plant.isWallNut()) {
            return plant.getWallNutHealthAnimationState();
        }
        if (plant != null && plant.isExplodeONut()) {
            return resolveExplodeONutIdleState(plant);
        }
        if (plant != null && plant.isTallNut()) {
            return plant.getTallNutHealthAnimationState();
        }
        if (plant != null && plant.isGarlic()) {
            return plant.getGarlicHealthAnimationState();
        }
        if (plant != null && plant.isSweetPotato()) {
            return plant.getSweetPotatoHealthAnimationState();
        }
        if (plant != null && plant.isEndurian()) {
            return resolveEndurianIdleState(plant);
        }
        if (plant != null && plant.isCactus()) {
            return cactusIdleState(plant);
        }
        if (plant != null && plant.isPumpkin()) {
            double ratio = plant.getHealthRatio();
            if (ratio > 0.60) return "idle";
            if (ratio >= 0.25) return "idle2";
            return "idle3";
        }
        if (plant != null && "Kiwibeast".equalsIgnoreCase(plant.getName())) {
            return switch (plant.getGrowthStage()) {
                case 2 -> "idle_stage2_2";
                case 3 -> "idle_stage3_3";
                default -> "idle";
            };
        }
        if (isSunShroom(plant)) {
            return switch (plant.getGrowthStage()) {
                case 2 -> "idle_stage2";
                case 3 -> "idle_stage3";
                default -> "idle_stage1";
            };
        }
        if (isPuffShroom(plant)) {
            return switch (puffShroomStage(plant)) {
                case 2 -> "idle_stage2";
                case 3 -> "idle_stage3";
                default -> "idle_stage1";
            };
        }
        return plantStackState(plant, "idle");
    }

    private boolean isSunShroom(Plant plant) {
        return plant != null && "Sun-shroom".equalsIgnoreCase(plant.getName());
    }

    private boolean isSeaShroom(Plant plant) {
        return plant != null && "Sea-shroom".equalsIgnoreCase(plant.getName());
    }

    private boolean isPuffShroom(Plant plant) {
        return plant != null && "Puff-shroom".equalsIgnoreCase(plant.getName());
    }

    private boolean isFumeShroom(Plant plant) {
        return plant != null && "Fume-shroom".equalsIgnoreCase(plant.getName());
    }

    /**
     * Puff-shroom counts up through 3 stages over its lifespan (30s each, {@link
     * #puffShroomStage} derives the stage purely from elapsed lifespan time so it needs no
     * extra state). Plant Food resets its lifespan back to 0 elapsed (see {@link
     * Plant#activatePlant}), which naturally drops this back to stage 1 too.
     */
    private int puffShroomStage(Plant plant) {
        if (!isPuffShroom(plant)) return 1;
        double lifespan = plant.getLifespanSeconds();
        if (lifespan <= 0) return 1;
        double elapsed = lifespan - plant.getRemainingLifeSeconds();
        int stage = 1 + (int) Math.floor(elapsed / 30.0);
        return Math.max(1, Math.min(3, stage));
    }

    /**
     * Sunflower, Twin Sunflower, Primal Sunflower and Sun-shroom all use the "special"
     * clip (Sun-shroom's staged "special_stageN" variant) while they're actively producing
     * a sun. Sun Bean doesn't produce sun this way (biting it marks the zombie as a sun-bean
     * carrier - halo overlay until that zombie dies, then it drops sun - see Plant#takeDamage
     * and Zombie#markSunBeanCarrier), so it's excluded here even though it's still part of
     * {@link #isSunProducerFamily}
     * for Plant Food purposes.
     */
    private boolean isSunProducingPlant(Plant plant) {
        if (plant == null) return false;
        String name = plant.getName();
        return "Sunflower".equalsIgnoreCase(name)
                || "Twin Sunflower".equalsIgnoreCase(name)
                || "Primal Sunflower".equalsIgnoreCase(name)
                || isSunShroom(plant);
    }

    /** The full family that shows the "plantfood" clip for its whole Plant Food duration. */
    private boolean isSunProducerFamily(Plant plant) {
        return isSunProducingPlant(plant) || (plant != null && "Sun Bean".equalsIgnoreCase(plant.getName()));
    }

    private String sunProducerSpecialState(Plant plant) {
        if (isSunShroom(plant)) {
            return switch (plant.getGrowthStage()) {
                case 2 -> "special_stage2";
                case 3 -> "special_stage3";
                default -> "special_stage1";
            };
        }
        return "special";
    }

    /**
     * Stage-aware "plantfood" clip name; Sun-shroom has per-stage variants, Sea-shroom uses
     * its own "pf" clip name, and everything else (including Puff-shroom and Fume-shroom)
     * just uses the plain "plantfood" clip for its whole Plant Food duration.
     */
    private String plantFoodClipState(Plant plant) {
        if (isHeadbutterLettuce(plant)) {
            return "plantfood_loop";
        }
        // Cat-tail borrows Homing Thistle's rig, whose sustained-fire clip is the one
        // meant to be repeated for the length of the volley.
        if (isCatTail(plant)) {
            return "plantfood_loop";
        }
        if (isSunShroom(plant)) {
            return switch (plant.getGrowthStage()) {
                case 2 -> "plantfood_stage2";
                case 3 -> "plantfood_stage3";
                default -> "plantfood_stage1";
            };
        }
        if (isSeaShroom(plant)) {
            return "pf";
        }
        if (isBowlingBulb(plant)) {
            return bowlingBulbPlantFoodState(plant);
        }
        if (plant.isCactus()) {
            return plant.isCactusUnderground() ? "down_attack_plantfood" : "attack_plantfood";
        }
        return "plantfood";
    }

    private boolean showsPlantFoodLoopForFullDuration(Plant plant) {
        if (plant == null) return false;
        if (plant.isCactus() || "Torchwood".equalsIgnoreCase(plant.getName())) return false;
        if (isSunProducerFamily(plant) || isSeaShroom(plant) || isPuffShroom(plant)
                || isFumeShroom(plant) || isMagnetShroom(plant) || isCatTail(plant)
                || isElectricBlueberry(plant)) {
            return true;
        }
        PlantFoodEffect effect = plant.getPlantFoodEffect();
        return effect != null && effect.drivesActStrategy();
    }

    private boolean isCatTail(Plant plant) {
        return plant != null && "Cat-tail".equalsIgnoreCase(plant.getName());
    }

    private boolean isElectricBlueberry(Plant plant) {
        return plant != null && "Electric Blueberry".equalsIgnoreCase(plant.getName());
    }

    private boolean sunBeanHasPlantFoodShield(Plant plant) {
        return plant != null && "Sun Bean".equalsIgnoreCase(plant.getName())
                && plant.getArmor() != null && plant.getArmor().getHP() > 0;
    }

    private static final String MAGNET_ITEM_ELEMENT = "Magnet_Item";

    private boolean isMagnetShroom(Plant plant) {
        return plant != null && "Magnet-shroom".equalsIgnoreCase(plant.getName());
    }

    private String resolvePumpkinPlantFoodState(Plant plant) {
        int stage = plant.getPumpkinArmorVisualStage();
        return switch (stage) {
            case 1 -> "idle_plantfood";
            case 2 -> "idle_plantfood2";
            case 3 -> "idle_plantfood3";
            case 4 -> "idle_plantfood4";
            default -> resolveIdleState(plant);
        };
    }

    /**
     * Cactus's idle loop while it isn't mid-transition and isn't in its brief fire-event
     * attack window (see resolveAttackBaseState/plantFoodClipState for the attack clips,
     * and Plant#tickCactusPosture for the down/up one-shot transitions handled generically
     * through the visualAnimationState catch-all above this in the draw-loop if-chain).
     */
    private String cactusIdleState(Plant plant) {
        boolean plantFood = plant.isPlantFoodActive();
        if (plant.isCactusUnderground()) {
            return plantFood ? "down_idle_plantfood" : "down_idle";
        }
        return plantFood ? "idle_plantfood" : "idle";
    }

    private String resolveEndurianIdleState(Plant plant) {
        if (plant.getEndurianDamageTier() == 0 && isOnWaterTile(plant)) return "water";
        return plant.getEndurianHealthAnimationState();
    }

    private boolean isOnWaterTile(Plant plant) {
        Position position = plant.getPosition();
        if (position == null || screen.session == null || screen.session.getEnvironment() == null) {
            return false;
        }
        Cell cell = screen.session.getEnvironment().getCell(
                (int) Math.round(position.y()), (int) Math.round(position.x()));
        return cell != null && cell.getTile() != null && cell.getTile().type() == TileType.Water;
    }

    private String endurianTierSuffix(Plant plant) {
        return switch (plant.getEndurianDamageTier()) {
            case 1 -> "_damage";
            case 2 -> "_damage2";
            case 3 -> "_damage3";
            default -> "";
        };
    }

    private String endurianAttackClip(Plant plant, int phase) {
        String base = switch (phase) {
            case 1 -> "attack_start";
            case 2 -> "attack_loop";
            default -> "attack_end";
        };
        return base + endurianTierSuffix(plant);
    }

    private float endurianClipDuration(Plant plant, String base) {
        float duration = screen.pam().resolvePlantClipDuration(plant.getName(),
                base + endurianTierSuffix(plant));
        return duration > 0f ? duration : DEFAULT_PLANT_ATTACK_DURATION;
    }

    private void advanceEndurianAttack(Plant plant, float delta) {
        int phase = endurianAttackPhases.getOrDefault(plant, 0);
        float time = endurianAttackTimes.getOrDefault(plant, 0f);
        boolean underAttack = plant.isEndurianUnderAttack();

        if (phase != 0) time += delta;
        if (underAttack && (phase == 0 || phase == 3)) {
            phase = 1;
            time = 0f;
        }
        if (phase == 0) return;

        if (phase == 1 && time >= endurianClipDuration(plant, "attack_start")) {
            phase = underAttack ? 2 : 3;
            time = 0f;
        }
        if (phase == 2) {
            if (!underAttack) {
                phase = 3;
                time = 0f;
            } else {
                float loop = endurianClipDuration(plant, "attack_loop");
                if (loop > 0f && time >= loop) time %= loop;
            }
        }
        if (phase == 3 && time >= endurianClipDuration(plant, "attack_end")) {
            endurianAttackPhases.remove(plant);
            endurianAttackTimes.remove(plant);
            return;
        }
        endurianAttackPhases.put(plant, phase);
        endurianAttackTimes.put(plant, time);
    }

    private Map<String, Boolean> endurianVisibility(Plant plant, String state) {
        Map<String, Boolean> visibility = new java.util.HashMap<>();
        for (int i = 1; i <= 8; i++) {
            visibility.put("PF_spike" + i, false);
        }
        visibility.put("PF_armor_1", false);
        visibility.put("armor2", false);
        visibility.put("armor_3", false);

        int stage = plant.getEndurianPlantFoodArmorStage();
        boolean armored = stage > 0;
        boolean attackState = state != null && state.startsWith("attack_");
        for (int i = 1; i <= 3; i++) {
            visibility.put("armor_damage_" + i, armored && !attackState && stage == i);
            visibility.put("armor_damage_" + i + "_attack", armored && attackState && stage == i);
        }
        visibility.put("endurian_plantfood_armor", armored);
        return visibility;
    }

    private void advanceExplodeONutTimers(Plant plant, float delta) {
        explodeONutBlinkTimes.put(plant, explodeONutBlinkTimes.getOrDefault(plant, 0f) + delta);

        boolean armored = plant.isExplodeONutArmored();
        boolean hadArmor = Boolean.TRUE.equals(explodeONutHadArmor.put(plant, armored));
        if (hadArmor && !armored) {
            explodeONutArmorOffTimes.put(plant, 0f);
        } else if (armored) {
            explodeONutArmorOffTimes.remove(plant);
        }

        Float offTime = explodeONutArmorOffTimes.get(plant);
        if (offTime != null) {
            offTime += delta;
            if (offTime >= EXPLODE_O_NUT_PLANTFOOD_OFF_HOLD) explodeONutArmorOffTimes.remove(plant);
            else explodeONutArmorOffTimes.put(plant, offTime);
        }
    }

    private float explodeONutAnimTime(Plant plant, String state, float loopTime) {
        if ("plantfood_on".equals(state)) return (float) plant.getVisualAnimationElapsed();
        if ("plantfood_off".equals(state)) return explodeONutArmorOffTimes.getOrDefault(plant, 0f);
        float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), state);
        return clipDuration > 0f ? loopTime % clipDuration : loopTime;
    }

    private boolean isHeadbutterLettuce(Plant plant) {
        return plant != null && "Iceberg Lettuce".equalsIgnoreCase(plant.getName());
    }

    /**
     * Drives Headbutter Lettuce's three-phase Plant Food sequence: "plantfood_on" (intro,
     * played once as soon as Plant Food activates), "plantfood_loop" (holds for whatever
     * remains of the Plant Food window), then "plantfood_off" (outro, played once - note
     * this runs *after* isPlantFoodActive() has already gone false, the same idiom
     * advanceExplodeONutTimers uses for its own "plantfood_off").
     */
    private void advanceHeadbutterLettuceTimers(Plant plant, float delta) {
        boolean active = plant.isPlantFoodActive();
        boolean wasActive = Boolean.TRUE.equals(headbutterLettucePfWasActive.put(plant, active));

        if (active && !wasActive) {
            headbutterLettucePfOnTime.put(plant, 0f);
            headbutterLettucePfOffTime.remove(plant);
        } else if (!active && wasActive) {
            headbutterLettucePfOffTime.put(plant, 0f);
            headbutterLettucePfOnTime.remove(plant);
        }

        Float onTime = headbutterLettucePfOnTime.get(plant);
        if (active && onTime != null) {
            headbutterLettucePfOnTime.put(plant, onTime + delta);
        }

        Float offTime = headbutterLettucePfOffTime.get(plant);
        if (offTime != null) {
            float offDuration = headbutterLettuceClipDuration(plant, "plantfood_off");
            offTime += delta;
            if (offTime >= offDuration) headbutterLettucePfOffTime.remove(plant);
            else headbutterLettucePfOffTime.put(plant, offTime);
        }
    }

    /** "plantfood_on" while its intro clip is still playing, then "plantfood_loop" for the
     *  rest of the Plant Food window, then "plantfood_off" once Plant Food has ended. */
    private String headbutterLettuceState(Plant plant) {
        if (plant.isPlantFoodActive()) {
            float onDuration = headbutterLettuceClipDuration(plant, "plantfood_on");
            Float onTime = headbutterLettucePfOnTime.get(plant);
            if (onTime != null && onTime < onDuration) return "plantfood_on";
            return "plantfood_loop";
        }
        return "plantfood_off";
    }

    private float headbutterLettuceAnimTime(Plant plant, String state, float loopTime) {
        if ("plantfood_on".equals(state)) return headbutterLettucePfOnTime.getOrDefault(plant, 0f);
        if ("plantfood_off".equals(state)) return headbutterLettucePfOffTime.getOrDefault(plant, 0f);
        float clipDuration = headbutterLettuceClipDuration(plant, state);
        return clipDuration > 0f ? (loopTime % clipDuration) : loopTime;
    }

    private float headbutterLettuceClipDuration(Plant plant, String state) {
        float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), state);
        return clipDuration > 0f ? clipDuration : DEFAULT_PLANT_ATTACK_DURATION;
    }

    private String resolveExplodeONutIdleState(Plant plant) {
        if (plant.getExplodeONutDamageTier() == 0 && isOnWaterTile(plant)) return "water";
        return plant.getExplodeONutHealthAnimationState();
    }

    private String resolveExplodeONutState(Plant plant) {
        if (plant.isExplodeONutArmored()) {
            if ("plantfood_on".equals(plant.getVisualAnimationState())) return "plantfood_on";
            return switch (plant.getExplodeONutPlantFoodArmorStage()) {
                case 2 -> "plantfood2";
                case 3 -> "plantfood3";
                default -> "plantfood";
            };
        }
        if (explodeONutArmorOffTimes.containsKey(plant)) return "plantfood_off";
        return resolveExplodeONutIdleState(plant);
    }

    private Map<String, Boolean> explodeONutArmorVisibility(String state) {
        Map<String, Boolean> visibility = new java.util.HashMap<>();
        visibility.put("wallnut_plantfood_armor_01",
                "plantfood".equals(state) || "plantfood_on".equals(state));
        visibility.put("wallnut_plantfood_armor_02", "plantfood2".equals(state));
        visibility.put("wallnut_plantfood_armor_03", "plantfood3".equals(state));
        return visibility;
    }

    private void drawExplodeONutBlink(Plant plant, float plantOffsetX, float plantOffsetY) {
        float clip = AnimationFactory.exactClipDurationForPath(
                EXPLODE_O_NUT_BLINK_PAM, "animation");
        if (clip <= 0f) clip = 0.3f;

        float period = switch (plant.getExplodeONutDamageTier()) {
            case 1 -> 1.5f;
            case 2 -> 1.0f;
            case 3 -> 0.55f;
            default -> 2.2f;
        };
        if (plant.isExplodeONutArmored()) period = 1.2f;
        period = Math.max(period, clip);

        float phase = explodeONutBlinkTimes.getOrDefault(plant, 0f) % period;
        if (phase > clip) return;

        screen.batch.setColor(EXPLODE_O_NUT_BLINK_TINT);
        screen.drawPam(EXPLODE_O_NUT_BLINK_PAM, "animation", phase,
                plantOffsetX, plantOffsetY, EXPLODE_O_NUT_BLINK_SCALE, false);
        screen.batch.setColor(Color.WHITE);
    }

    private String resolveWallNutPlantFoodState(Plant plant) {
        int stage = plant.getWallNutPlantFoodArmorStage();
        return switch (stage) {
            case 1 -> "plantfood";
            case 2 -> "plantfood2";
            case 3 -> "plantfood3";
            default -> plant.getWallNutHealthAnimationState();
        };
    }

    private void drawPumpkinArmorOverlay(Plant plant, float x, float y) {
        int stage = plant.getPumpkinArmorVisualStage();
        if (stage <= 0) return;

        String selected = switch (stage) {
            case 1 -> "pumpkin_armor_01";
            case 2 -> "pumpkin_armor_02";
            case 3 -> "pumpkin_armor_03";
            default -> "pumpkin_armor_04";
        };

        Map<String, Boolean> visibility = new java.util.HashMap<>();
        visibility.put("pumpkin_body", false);
        visibility.put("pumpkin_body_2", false);
        visibility.put("pumpkin_body_3", false);
        visibility.put("pumpkin_armor_01", false);
        visibility.put("pumpkin_armor_02", false);
        visibility.put("pumpkin_armor_03", false);
        visibility.put("pumpkin_armor_04", false);
        visibility.put(selected, true);

        String path = AnimationFactory.pathForDisplayName(plant.getName());
        if (path == null) return;
        String clip = AnimationFactory.firstAvailableClipState(plant.getName(), "idle");
        if (clip == null) return;
        screen.drawPam(path, clip, 0f, x, y, 0.55f, false, visibility);
    }

    private String resolveAttackBaseState(Plant plant) {
        if (isHeadbutterLettuce(plant)) {
            // Front (right, toward oncoming zombies) uses "attack"; a target caught on the
            // back tile mirrors MeleeStrategy's facing flag and uses the separate "attack2"
            // clip instead of a mirrored sprite (unlike Wasabi Whip/Chomper/Squash - see the
            // meleePlant mirror check in the main draw loop, which this plant is deliberately
            // not part of).
            return plant.isMeleeFacingLeft() ? "attack2" : "attack";
        }
        if (plant != null && "Bonk Choy".equalsIgnoreCase(plant.getName())) {
            // Same idea as Headbutter Lettuce above: Bonk Choy has its own real "attack2"
            // clip for a back-tile hit, so it's also excluded from the meleePlant mirror
            // check rather than mirroring the plain "attack" sprite.
            return plant.isMeleeFacingLeft() ? "attack2" : "attack";
        }
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
        if (plant != null && plant.isCactus()) {
            if (plant.isCactusUnderground()) return "down_attack";
            if (plant.isCactusStretching()) return "attack_stretch";
            return "attack";
        }
        if (isFumeShroom(plant)) {
            return "special";
        }
        if (isPuffShroom(plant)) {
            return switch (puffShroomStage(plant)) {
                case 2 -> "special_stage2";
                case 3 -> "special_stage3";
                default -> "special_stage1";
            };
        }
        if (isSunProducingPlant(plant)) {
            return sunProducerSpecialState(plant);
        }
        if (isBowlingBulb(plant)) {
            return bowlingBulbSpecialState(plant);
        }
        return plantStackState(plant, "attack");
    }

    private boolean isBowlingBulb(Plant plant) {
        return plant != null && BOWLING_BULB_NAME.equalsIgnoreCase(plant.getName());
    }

    private int bowlingBulbAmmoIndex(Plant plant) {
        return bowlingBulbShotIndex.getOrDefault(plant, 0);
    }

    private String bowlingBulbSpecialState(Plant plant) {
        return switch (bowlingBulbAmmoIndex(plant)) {
            case 1 -> "special2";
            case 2 -> "special3";
            default -> "special";
        };
    }

    private String bowlingBulbReloadState(Plant plant) {
        return switch (bowlingBulbAmmoIndex(plant)) {
            case 1 -> "reload2";
            case 2 -> "reload3";
            default -> "reload";
        };
    }

    private String bowlingBulbPlantFoodState(Plant plant) {
        return switch (bowlingBulbAmmoIndex(plant)) {
            case 1 -> "plantfood2";
            case 2 -> "plantfood3";
            default -> "plantfood";
        };
    }

    /** Plays the SFX matching a detected plant fire event. */
    private void playFireEventSound(Plant plant) {
        if (plant == null) return;
        if (isSunProducingPlant(plant)) {
            AudioManager.get().playSound(AudioEnum.SFX_SUN_PRODUCE);
        } else if (plant.getType() == PlantType.MELEE) {
            AudioManager.get().playSound(AudioEnum.SFX_MELEE_HIT);
        } else if (plant.getTags().contains(PlantTag.FIRE)) {
            AudioManager.get().playSound(AudioEnum.SFX_SHOOT_FIRE);
        } else if (plant.getTags().contains(PlantTag.ICE)) {
            AudioManager.get().playSound(AudioEnum.SFX_SHOOT_ICE);
        } else if (plant.getType() == PlantType.SHOOTER || plant.getType() == PlantType.LOBBER
                || plant.getType() == PlantType.HOMING || plant.getType() == PlantType.STRIKE_THROUGH) {
            AudioManager.get().playSound(AudioEnum.SFX_SHOOT_NORMAL);
        }
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
            if (plant.getPosition() == null) continue;

            if ("Doom-shroom".equalsIgnoreCase(plant.getName())) {
                int stage = Math.max(1, Math.min(3, plant.getGrowthStage()));
                screen.effects().addDoomExplosion(plant.getPosition(), stage);
                screen.effects().addScorchedTileEffect(plant.getPosition());
                AudioManager.get().playSound(AudioEnum.SFX_PLANT_EXPLODE);
                plantAnimTimes.remove(plant);
                plantAttackAnimTimes.remove(plant);
                doomSpawnAnimTimes.remove(plant);
                doomSpawnWindows.remove(plant);
                doomTransformAnimTimes.remove(plant);
                doomTransformWindows.remove(plant);
                doomLastGrowthStage.remove(plant);
                continue;
            }

            if ("Torchwood".equalsIgnoreCase(plant.getName())) {
                String path = AnimationFactory.pathForDisplayName(plant.getName());
                if (path != null) {
                    screen.effects().addExplodingPlantEffect(
                            new ProjectileEffectAssets.AssetEntry(path, "explosion",
                                    ProjectileEffectAssets.PlayMode.ONCE,
                                    ProjectileEffectAssets.Kind.EFFECT,
                                    ProjectileEffectAssets.Variant.NORMAL,
                                    ProjectileEffectAssets.Scope.SELF,
                                    "Torchwood death explosion"),
                            false, plant.getPosition(), EXPLODING_PLANT_EFFECT_DURATION);
                }
                screen.effects().addTorchwoodRowFireEffect((int) Math.round(plant.getPosition().y()),
                        EXPLODING_PLANT_EFFECT_DURATION);
                AudioManager.get().playSound(AudioEnum.SFX_PLANT_EXPLODE);
                plantAnimTimes.remove(plant);
                plantAttackAnimTimes.remove(plant);
                continue;
            }

            if (plant.isExplodeONut()) {
                if (plant.isExplodeONutDetonated()) {
                    screen.effects().addExplodeONutExplosion(plant.getPosition());
                    AudioManager.get().playSound(AudioEnum.SFX_PLANT_EXPLODE);
                }
                plantAnimTimes.remove(plant);
                plantAttackAnimTimes.remove(plant);
                explodeONutBlinkTimes.remove(plant);
                explodeONutHadArmor.remove(plant);
                explodeONutArmorOffTimes.remove(plant);
                continue;
            }

            if ("Jalapeno".equalsIgnoreCase(plant.getName())) {
                screen.effects().addJalapenoRowFireEffect(
                        (int) Math.round(plant.getPosition().y()));
                AudioManager.get().playSound(AudioEnum.SFX_PLANT_EXPLODE);
                plantAnimTimes.remove(plant);
                plantAttackAnimTimes.remove(plant);
                continue;
            }

            if ("Grapeshot".equalsIgnoreCase(plant.getName())) {
                AudioManager.get().playSound(AudioEnum.SFX_PLANT_EXPLODE);
                plantAnimTimes.remove(plant);
                plantAttackAnimTimes.remove(plant);
                continue;
            }

            if (plant.isGraveBuster()) {
                // Only a finished chew earns the closing puff - a Grave Buster that was
                // eaten off the grave, or shovelled, just disappears and leaves the grave.
                if (plant.hasGraveBusterConsumedGrave()) {
                    screen.effects().addGraveBusterDirtEffect(plant.getPosition(),
                            EffectRenderer.GRAVE_BUSTER_DIRT_FADE_STATE);
                }
                plantAnimTimes.remove(plant);
                plantAttackAnimTimes.remove(plant);
                graveBusterEntrySeen.remove(plant);
                continue;
            }

            boolean delayedExplosiveDeath = plant.isPotatoMine()
                    || "Cherry Bomb".equalsIgnoreCase(plant.getName());
            if (plant.isPotatoMine() && plant.wasPotatoMineEatenByZombie()) continue;
            if (!delayedExplosiveDeath && plant.getHP() <= 0) continue;
            if (plant.getType() != PlantType.EXPLOSIVE) continue;

            ProjectileEffectAssets.AssetEntry entry = screen.effects().resolveExplosionEntry(plant.getName());
            if (entry == null) continue;

            boolean loop = entry.playMode() == ProjectileEffectAssets.PlayMode.LOOP;
            Position position = plant.getPosition();
            float effectDuration = EXPLODING_PLANT_EFFECT_DURATION;
            if ("Potato Mine".equalsIgnoreCase(plant.getName())
                    || "Primal Potato Mine".equalsIgnoreCase(plant.getName())
                    || "Cherry Bomb".equalsIgnoreCase(plant.getName())) {
                float resolved = AnimationFactory.exactClipDurationForPath(entry.path(), entry.state());
                if (resolved > 0f) effectDuration = resolved;
            }
            screen.effects().addExplodingPlantEffect(entry, loop, position, effectDuration);
            AudioManager.get().playSound(AudioEnum.SFX_PLANT_EXPLODE);
            plantAnimTimes.remove(plant);
            plantAttackAnimTimes.remove(plant);
        }
        trackDyingShrooms(alivePlantsBeforeTick, stillAlive);
    }

    /**
     * Sea-shroom plays "death" however it dies (eaten by a zombie or washed away by the
     * tide - both zero its HP, the latter via Flood.removeAquaticPlant). Puff-shroom plays
     * "idle_stage4" only when its own 30s-per-stage countdown runs out without Plant Food
     * (Plant.tick sets PlantState.DYING for that case specifically, without touching HP;
     * a zombie kill also sets DYING but zeroes HP first, so the HP check tells them apart).
     * Both plants are already gone from stillAlive by the time this runs, so the clip is
     * queued as a position-only overlay - see DyingShroomEffect/drawDyingShroomEffects.
     */
    private void trackDyingShrooms(List<Plant> alivePlantsBeforeTick, List<Plant> stillAlive) {
        for (Plant plant : alivePlantsBeforeTick) {
            if (stillAlive.contains(plant) || plant.getPosition() == null) continue;

            boolean seaShroomDeath = isSeaShroom(plant)
                    && (plant.getHP() <= 0 || plant.getPlantState() == Plant.PlantState.DYING);
            boolean puffShroomDeath = isPuffShroom(plant)
                    && plant.getHP() > 0
                    && plant.getPlantState() == Plant.PlantState.DYING;
            if (!seaShroomDeath && !puffShroomDeath) continue;

            String path = AnimationFactory.pathForDisplayName(plant.getName());
            if (path == null) continue;
            String state = seaShroomDeath ? "death" : "idle_stage4";
            dyingShroomEffects.add(new DyingShroomEffect(path, state, plant.getPosition()));
        }
    }
}