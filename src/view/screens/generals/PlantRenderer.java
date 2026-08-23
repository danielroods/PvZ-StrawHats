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
                    String loopState;
                    if (plant.isWallNut()) loopState = plant.getWallNutHealthAnimationState();
                    else if (plant.isTallNut()) loopState = plant.getTallNutHealthAnimationState();
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
            float animTime = t;
            boolean pumpkinHasArmor = plant.isPumpkin()
                    && plant.getArmor() != null
                    && plant.getArmor().getHP() > 0;
            boolean wallNutHasPlantFoodArmor = plant.isWallNut()
                    && plant.getArmor() != null
                    && plant.getArmor().getHP() > 0;
            boolean tallNutHasPlantFoodArmor = plant.isTallNut()
                    && plant.getArmor() != null
                    && plant.getArmor().getHP() > 0;
            if (pumpkinHasArmor) {
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
            } else if (plant.isWallNut() && plant.getVisualAnimationState() != null) {
                preferredState = plant.getVisualAnimationState();
                animTime = (float) plant.getVisualAnimationElapsed();
                float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
                if (clipDuration > 0f) animTime %= clipDuration;
            } else if (plant.getVisualAnimationState() != null) {
                preferredState = plant.getVisualAnimationState();
                animTime = (float) plant.getVisualAnimationElapsed();
                if (plant.isPumpkin() && "idle_plantfood".equals(preferredState)) {
                    float clipDuration = screen.pam().resolvePlantClipDuration(plant.getName(), preferredState);
                    if (clipDuration > 0f) animTime %= clipDuration;
                }
            } else if (prepping) {
                preferredState = plant.getAbilityType() == AbilityType.MINT_FAMILY_BOOST
                        ? "intro"
                        : screen.pam().resolveFuseClipState(plant.getName());
                animTime = t;
            } else if (attacking) {
                preferredState = attackIsBoosted ? "plantfood"
                        : plantAttackBaseState.getOrDefault(plant, plantStackState(plant, "attack"));
                animTime = plantAttackAnimTimes.get(plant);
            } else if (plant.isPumpkin() && plant.isPlantFoodActive()) {
                preferredState = "idle_plantfood";
                animTime = (float) plant.getVisualAnimationElapsed();
            } else if (plant.isTallNut() && plant.isPlantFoodActive()) {
                preferredState = "idle";
                animTime = t;
            } else {
                preferredState = resolveIdleState(plant);
                animTime = t;
            }

            if (squashJumping) {
                plantOffsetY += squashJumpArcOffset(plant, boardTileHeight);
            }

            boolean meleePlant = "Bonk Choy".equalsIgnoreCase(plant.getName())
                    || "Wasabi Whip".equalsIgnoreCase(plant.getName())
                    || "Chomper".equalsIgnoreCase(plant.getName())
                    || "Squash".equalsIgnoreCase(plant.getName());

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
            if (squashExactState) {
                boolean squashMirror = "turn".equals(preferredState) && plant.isMeleeFacingLeft();
                drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                        plantOffsetX, plantOffsetY, 0.55f, squashMirror);
            } else if (pumpkinPlantFoodState) {
                java.util.Map<String, Boolean> pumpkinPfVisibility = new java.util.HashMap<>();
                pumpkinPfVisibility.put("pumpkin_armor_01", "idle_plantfood".equals(preferredState));
                pumpkinPfVisibility.put("pumpkin_armor_02", "idle_plantfood2".equals(preferredState));
                pumpkinPfVisibility.put("pumpkin_armor_03", "idle_plantfood3".equals(preferredState));
                pumpkinPfVisibility.put("pumpkin_armor_04", "idle_plantfood4".equals(preferredState));
                drawn = screen.drawPam(path, preferredState, animTime,
                        plantOffsetX, plantOffsetY, 0.55f, false, pumpkinPfVisibility);
            } else if (pumpkinExactState) {
                drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                        plantOffsetX, plantOffsetY, 0.55f, false);
            } else if (wallNutPlantFoodState) {
                Map<String, Boolean> wallNutPfVisibility = new java.util.HashMap<>();
                wallNutPfVisibility.put("wallnut_plantfood_armor_01", "plantfood".equals(preferredState));
                wallNutPfVisibility.put("wallnut_plantfood_armor_02", "plantfood2".equals(preferredState));
                wallNutPfVisibility.put("wallnut_plantfood_armor_03", "plantfood3".equals(preferredState));
                drawn = screen.drawPam(path, preferredState, animTime,
                        plantOffsetX, plantOffsetY, 0.55f, false, wallNutPfVisibility);
            } else if (wallNutExactState) {
                drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                        plantOffsetX, plantOffsetY, 0.55f, false);
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
                        plantOffsetX, plantOffsetY, 0.55f, false, tallNutArmorVisibility);
            } else if (tallNutExactState) {
                drawn = screen.pam().drawPamExact(path, preferredState, animTime,
                        plantOffsetX, plantOffsetY, 0.55f, false);
            } else {
                drawn = screen.drawPam(path, preferredState, animTime, plantOffsetX, plantOffsetY, 0.55f, mirror);
            }
            if (!drawn) {
                TextureRegion region = GameAssetManager.get().getPlantRegion(plant.getName());
                screen.drawEntity(region, plantOffsetX, plantOffsetY, boardTileWidth, boardTileHeight,
                        new Color(0.2f, 0.65f, 0.22f, 1f), GameScreenGraphics.initials(plant.getName()));
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
        plantAnimTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantLastCooldown.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackAnimTimes.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackWindow.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackIsBoosted.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
        plantAttackBaseState.keySet().removeIf(p -> !screen.session.getPlants().contains(p));
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
        if (plant != null && plant.isTallNut()) {
            return plant.getTallNutHealthAnimationState();
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
        return plantStackState(plant, "idle");
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

        java.util.Map<String, Boolean> visibility = new java.util.HashMap<>();
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