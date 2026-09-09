package controller.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectileEffectAssets {

    public enum PlayMode {
        LOOP,
        ONCE
    }

    public enum Kind {
        PROJECTILE,
        HIT,
        EFFECT
    }

    public enum Variant {
        NORMAL,
        PLANT_FOOD
    }

    /** Where an EFFECT entry belongs: on the plant's own tile, or repeated along its whole row. */
    public enum Scope {
        SELF,
        ROW
    }

    public record AssetEntry(String path, String state, PlayMode playMode, Kind kind,
                             Variant variant, Scope scope, String purpose) {

        public String fullPath() {
            return path.startsWith(AssetPaths.ROOT) ? path : AssetPaths.PVZ_ASSETS + path;
        }

        public boolean isStaticImage() {
            String lower = path.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg");
        }
    }

    private static final Map<String, List<AssetEntry>> REGISTRY = new LinkedHashMap<>();

    private static final String SPLAT_PEA =
            "768/INITIAL/EFFECTS/SPLAT_PEA/SPLAT_PEA.PAM";

    static {
        registerPeaFamily();
        registerSunProducers();
        registerRotobaga();
        registerCitron();
        registerCauliflower();
        registerElectricBlueberry();
        registerBowlingBulb();
        registerCactus();
        registerStarfruit();
        registerSeaShroom();
        registerPuffShroom();
        registerFumeShroom();
        registerCabbagePult();
        registerKernelPult();
        registerMelonPult();
        registerWinterMelon();
        registerPepperPult();
        registerPotatoMine();
        registerPrimalPotatoMine();
        registerCherryBomb();
        registerGrapeshot();
        registerJalapeno();
        registerPhatBeet();
        registerKiwibeast();
        registerGarlic();
        registerExplodeOnut();
        registerTorchwood();
        registerHypnoShroom();
        registerCatTail();
        registerIceShroom();
        registerHotPotato();
        registerGravebuster();
        registerMintFamily();
        registerHeadbutterLettuce();
    }

    private ProjectileEffectAssets() {
    }

    private static AssetEntry entry(String path, String state, PlayMode playMode, Kind kind,
                                    Variant variant, String purpose) {
        return new AssetEntry(path, state, playMode, kind, variant, Scope.SELF, purpose);
    }

    private static AssetEntry rowEntry(String path, String state, PlayMode playMode, Kind kind,
                                       Variant variant, String purpose) {
        return new AssetEntry(path, state, playMode, kind, variant, Scope.ROW, purpose);
    }

    private static void register(String plantName, AssetEntry... entries) {
        REGISTRY.put(plantName, List.of(entries));
    }

    public static List<AssetEntry> get(String plantName) {
        return REGISTRY.getOrDefault(plantName, Collections.emptyList());
    }

    public static List<AssetEntry> get(String plantName, Kind kind, Variant variant) {
        List<AssetEntry> result = new ArrayList<>();
        for (AssetEntry candidate : get(plantName)) {
            if (candidate.kind() == kind && candidate.variant() == variant) {
                result.add(candidate);
            }
        }
        return result;
    }

    private static void registerPeaFamily() {
        register("Peashooter",
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "plain pea shot"),
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation2", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "plant food pea shot (shared pea-family PF pea)"),
                entry(SPLAT_PEA, "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "pea impact splat")
        );

        register("Repeater",
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "plain pea shot (fires twice)"),
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation2", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "plant food pea shot (giant pea finisher sets its own display)"),
                entry(SPLAT_PEA, "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "pea impact splat")
        );

        register("Threepeater",
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "plain pea shot (3 lanes)"),
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation2", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "plant food pea shot"),
                entry(SPLAT_PEA, "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "pea impact splat")
        );

        register("Pea Pod",
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "plain pea shot"),
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation2", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "plant food pea shot (giant pea finisher sets its own display)"),
                entry(SPLAT_PEA, "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "pea impact splat")
        );

        register("Split Pea",
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "plain pea shot (front+back)"),
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation2", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "plant food pea shot"),
                entry(SPLAT_PEA, "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "pea impact splat")
        );

        register("Mega Gatling Pea",
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "plain pea shot (4x volley)"),
                entry("768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                        "animation2", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "plant food pea shot"),
                entry(SPLAT_PEA, "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "pea impact splat")
        );

        register("Snow Pea",
                entry("768/INITIAL/EFFECTS/T_SNOW_PEA/T_SNOW_PEA.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "snow pea shot"),
                entry("768/INITIAL/EFFECTS/T_SNOW_PEA/T_SNOW_PEA.PAM",
                        "animation4", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF snow pea shot"),
                entry("768/INITIAL/EFFECTS/SPLAT_SNOW_PEA/SPLAT_SNOW_PEA.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "snow pea impact splat"),
                rowEntry("768/INITIAL/EFFECTS/SNOWPEA_PLANTFOOD/SNOWPEA_PLANTFOOD.PAM",
                        "plantfood_on", PlayMode.ONCE, Kind.EFFECT, Variant.PLANT_FOOD,
                        "ice line across the row, start (no idle/loop state given)"),
                rowEntry("768/INITIAL/EFFECTS/SNOWPEA_PLANTFOOD/SNOWPEA_PLANTFOOD.PAM",
                        "plantfood_off", PlayMode.ONCE, Kind.EFFECT, Variant.PLANT_FOOD,
                        "ice line across the row, end"),
                entry("768/INITIAL/EFFECTS/SNOWPEA_PLANTFOOD_SLOW/"
                                + "SNOWPEA_PLANTFOOD_SLOW.PAM",
                        "plantfood_on", PlayMode.ONCE, Kind.EFFECT, Variant.PLANT_FOOD,
                        "frozen-tile snow fx, start"),
                entry("768/INITIAL/EFFECTS/SNOWPEA_PLANTFOOD_SLOW/"
                                + "SNOWPEA_PLANTFOOD_SLOW.PAM",
                        "plantfood_idle", PlayMode.LOOP, Kind.EFFECT, Variant.PLANT_FOOD,
                        "frozen-tile snow fx, active loop"),
                entry("768/INITIAL/EFFECTS/SNOWPEA_PLANTFOOD_SLOW/"
                                + "SNOWPEA_PLANTFOOD_SLOW.PAM",
                        "plantfood_off", PlayMode.ONCE, Kind.EFFECT, Variant.PLANT_FOOD,
                        "frozen-tile snow fx, end")
        );

        register("Fire Peashooter",
                entry("768/INITIAL/EFFECTS/T_FIRE_PEA/T_FIRE_PEA.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "fire pea shot"),
                entry("768/INITIAL/EFFECTS/T_SPLAT_FIRE_PEA/T_SPLAT_FIRE_PEA.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "fire pea impact splat"),
                rowEntry("768/INITIAL/EFFECTS/FIREPEASHOOTER_FIRE/FIREPEASHOOTER_FIRE.PAM",
                        "idle", PlayMode.LOOP, Kind.EFFECT, Variant.PLANT_FOOD,
                        "PF: sets the whole lane on fire (single-tile fire line asset)")
        );

        register("Goo Peashooter",
                entry("768/INITIAL/EFFECTS/GOOPEASHOOTER_PROJECTILES/"
                                + "GOOPEASHOOTER_PROJECTILES.PAM",
                        "projectile_t1", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "goo pea shot"),
                entry("768/INITIAL/EFFECTS/SHADOWPEASHOOTER_PROJECTILE_HIT/"
                                + "SHADOWPEASHOOTER_PROJECTILE_HIT.PAM",
                        "animation2", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "goo pea hit effect (internal name mismatch: SHADOWPEASHOOTER)"),
                entry("768/INITIAL/EFFECTS/GOOPEASHOOTER_PLANTFOOD/"
                                + "GOOPEASHOOTER_PLANTFOOD.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "goo pea PF shot"),
                entry("768/INITIAL/EFFECTS/SHADOWPEASHOOTER_PLANTFOOD_PROJECTILE/"
                                + "SHADOWPEASHOOTER_PLANTFOOD_PROJECTILE.PAM",
                        "tier2_hit", PlayMode.ONCE, Kind.HIT, Variant.PLANT_FOOD,
                        "big goo ball ending explosion")
        );
    }

    private static void registerSunProducers() {
        register("Sun Bean",
                entry("768/FULL/EFFECTS/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1/"
                                + "SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1.PAM",
                        "animation", PlayMode.ONCE, Kind.EFFECT, Variant.PLANT_FOOD,
                        "PF overlay effect on the sun burst")
        );
    }

    private static void registerRotobaga() {
        register("Rotobaga",
                entry("768/FULL/EFFECTS/T_ROTORUTABAGA_PROJECTILE1/"
                                + "T_ROTORUTABAGA_PROJECTILE1.PAM",
                        "animation2", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "normal thrown rutabaga (internal name: RotoRutabaga)"),
                entry("768/FULL/EFFECTS/T_ROTORUTABAGA_PROJECTILE1/"
                                + "T_ROTORUTABAGA_PROJECTILE1.PAM",
                        "animation3", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "plant food thrown rutabaga"),
                entry("768/FULL/EFFECTS/ROTORUTABAGA_PROJECTILE_HIT/"
                                + "ROTORUTABAGA_PROJECTILE_HIT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "impact splat")
        );
    }

    private static void registerCitron() {
        register("Citron",
                entry("768/FULL/EFFECTS/CITRON_CITRUS_ORB/CITRON_CITRUS_ORB.PAM",
                        "Citron_Citrus_Orb", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "citrus orb shot"),
                entry("768/FULL/EFFECTS/CITRON_PLANTFOOD_LIGHTNING_CHARGE/"
                                + "CITRON_PLANTFOOD_LIGHTNING_CHARGE.PAM",
                        "Citron_Plantfood_Lightning_Charge", PlayMode.ONCE, Kind.EFFECT,
                        Variant.PLANT_FOOD, "PF charge-up before firing"),
                entry("768/FULL/EFFECTS/CITRON_PLANTFOOD_ORB/CITRON_PLANTFOOD_ORB.PAM",
                        "Plantfood_Citron_Plasma_Orb", PlayMode.LOOP, Kind.PROJECTILE,
                        Variant.PLANT_FOOD, "PF plasma orb shot"),
                entry("768/FULL/EFFECTS/CITRON_CITRUS_ORB_HIT/CITRON_CITRUS_ORB_HIT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "citrus orb impact"),
                entry("768/FULL/EFFECTS/CITRON_PLANTFOOD_SHOCK/CITRON_PLANTFOOD_SHOCK.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.PLANT_FOOD,
                        "PF orb shock impact")
        );
    }

    private static void registerCauliflower() {
        register("Caulipower",
                entry("768/INITIAL/EFFECTS/CAULIPOWER_PROJECTILE/CAULIPOWER_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "smash projectile"),
                entry("768/INITIAL/EFFECTS/CAULIPOWER_PROJECTILE/CAULIPOWER_PROJECTILE.PAM",
                        "animation2", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF smash projectile")
        );
    }

    private static void registerElectricBlueberry() {
        String path = "768/INITIAL/EFFECTS/ELECTRICBLUEBERRY_CLOUD_PROJECTILE/"
                + "ELECTRICBLUEBERRY_CLOUD_PROJECTILE.PAM";
        register("Electric Blueberry",
                entry(path, "start", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "cloud spawn"),
                entry(path, "idle", PlayMode.LOOP, Kind.EFFECT, Variant.NORMAL,
                        "cloud hovering loop while not attacking"),
                entry(path, "attack", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "cloud zapping loop while attacking (no PF variant given)"),
                entry(path, "death", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "cloud despawn")
        );
    }

    private static void registerBowlingBulb() {
        register("Bowling Bulb",
                entry("768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE1/BOWLINGBULB_PROJECTILE1.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "green ball variant"),
                entry("768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE2/BOWLINGBULB_PROJECTILE2.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "blue ball variant"),
                entry("768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE3/BOWLINGBULB_PROJECTILE3.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "orange ball variant"),
                entry("768/FULL/EFFECTS/BOWLINGBULB_PLANTFOOD_PROJECTILE/"
                                + "BOWLINGBULB_PLANTFOOD_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF ball rolling"),
                entry("768/FULL/EFFECTS/BOWLINGBULB_PLANTFOOD_PROJECTILE/"
                                + "BOWLINGBULB_PLANTFOOD_PROJECTILE.PAM",
                        "explosion", PlayMode.ONCE, Kind.HIT, Variant.PLANT_FOOD,
                        "PF ball explosion")
        );
    }

    private static void registerCactus() {
        register("Cactus",
                entry("768/INITIAL/EFFECTS/T_CACTUS_PROJECTILE/T_CACTUS_PROJECTILE.PAM",
                        "idle2", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "ground spike shot"),
                entry("768/INITIAL/EFFECTS/CACTUS_AIRATTACK/CACTUS_AIRATTACK.PAM",
                        "idle2", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "anti-air spike shot"),
                entry("768/INITIAL/EFFECTS/CACTUS_PROJECTILE_PLANTFOOD/"
                                + "CACTUS_PROJECTILE_PLANTFOOD.PAM",
                        "idle", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF spike shot"),
                entry("768/INITIAL/EFFECTS/CACTUS_PROJECTILE_HIT/CACTUS_PROJECTILE_HIT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "spike impact")
        );
    }

    private static void registerStarfruit() {
        register("Starfruit",
                entry("768/INITIAL/EFFECTS/T_STARFRUIT_PROJECTILE/T_STARFRUIT_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "star shot"),
                entry("768/INITIAL/EFFECTS/T_STARFRUIT_PROJECTILE/T_STARFRUIT_PROJECTILE.PAM",
                        "animation3", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF star shot (no hit-effect asset given)")
        );
    }

    private static void registerSeaShroom() {
        register("Sea-shroom",
                entry("768/FULL/EFFECTS/SEASHROOM_PROJECTILE/SEASHROOM_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "underwater shot"),
                entry("768/FULL/EFFECTS/SEASHROOM_PROJECTILE/SEASHROOM_PROJECTILE.PAM",
                        "animation2", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF underwater shot"),
                entry("768/FULL/EFFECTS/SEASHOOTER_FX/SEASHOOTER_FX.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "impact fx (internal name mismatch: SEASHOOTER)")
        );
    }

    private static void registerPuffShroom() {
        register("Puff-shroom",
                entry("768/INITIAL/EFFECTS/T_PUFFSHROOM_PROJECTILE/"
                                + "T_PUFFSHROOM_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "spore shot"),
                entry("768/INITIAL/EFFECTS/T_PUFFSHROOM_PROJECTILE/"
                                + "T_PUFFSHROOM_PROJECTILE.PAM",
                        "animation3", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF spore shot"),
                entry("768/INITIAL/EFFECTS/T_PUFFSHROOM_HIT/T_PUFFSHROOM_HIT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "spore impact")
        );
    }

    private static void registerFumeShroom() {
        register("Fume-shroom",
                entry("768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES/FUMESHROOM_BUBBLES.PAM",
                        "special", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "piercing gas cloud (no hit-effect asset given)"),
                entry("768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES/FUMESHROOM_BUBBLES.PAM",
                        "plantfood", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF piercing gas cloud"),
                entry("768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES_HIT/FUMESHROOM_BUBBLES_HIT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "gas impact puff, used by both the normal fume and the PF cloud")
        );
    }

    private static void registerCabbagePult() {
        register("Cabbage-pult",
                entry("768/INITIAL/EFFECTS/T_CABBAGEPULT_PROJECTILE/"
                                + "T_CABBAGEPULT_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "lobbed cabbage"),
                entry("768/INITIAL/EFFECTS/SPLAT_CABBAGEPULT/SPLAT_CABBAGEPULT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "cabbage impact splat"),
                entry("768/INITIAL/EFFECTS/CABBAGEPULT_PLANTFOOD_PROJECTILE/"
                                + "CABBAGEPULT_PLANTFOOD_PROJECTILE.PAM",
                        "plantfood_cabbage", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF lobbed cabbage"),
                entry("768/INITIAL/EFFECTS/CABBAGEPULT_PLANTFOOD_PROJECTILE/"
                                + "CABBAGEPULT_PLANTFOOD_PROJECTILE.PAM",
                        "plantfood_cabbageExplode", PlayMode.ONCE, Kind.HIT, Variant.PLANT_FOOD,
                        "PF cabbage explosion")
        );
    }

    private static void registerKernelPult() {
        register("Kernel-pult",
                entry("768/INITIAL/EFFECTS/T_KERNALPULT_PROJECTILE/"
                                + "T_KERNALPULT_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "lobbed kernel"),
                // NOTE: this is a plain PNG living at assets/projectiles/..., NOT
                // under the 768/.../*.PAM tree the other entries use. AssetEntry
                // .isStaticImage()/.fullPath() already handle that (the path
                // already starts with AssetPaths.ROOT, so it's used as-is rather
                // than getting the pvz-assets PAM root prepended) - don't try to
                // "fix" this into a PAM-style path.
                entry("assets/projectiles/kernelpult_projectile_butter.png",
                        "static", PlayMode.ONCE, Kind.PROJECTILE, Variant.NORMAL,
                        "butter projectile - static image, not a PAM animation; also used for PF"),
                entry("assets/projectiles/kernelpult_projectile_butter.png",
                        "static", PlayMode.ONCE, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF butter projectile - same static image as normal butter"),
                entry("768/INITIAL/EFFECTS/SPLAT_KERNALPULT_KERNAL/"
                                + "SPLAT_KERNALPULT_KERNAL.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "kernel impact"),
                entry("768/INITIAL/EFFECTS/SPLAT_KERNALPULT_BUTTER/"
                                + "SPLAT_KERNALPULT_BUTTER.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "butter impact (shared by normal and PF butter shots)")
                // The on-face butter-stun overlay is not a separate asset: it's a
                // "butter" element baked into each zombie's own PAM (same idea as
                // the armor pieces), toggled on/off via the per-zombie element
                // visibility mask - see ZombieArmorMask/ZombieRenderer.
        );
    }

    private static void registerMelonPult() {
        register("Melon-pult",
                entry("768/INITIAL/EFFECTS/T_MELON_PROJECTILE/T_MELON_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "lobbed melon"),
                entry("768/INITIAL/EFFECTS/T_MELON_PROJECTILE/T_MELON_PROJECTILE.PAM",
                        "animation3", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF lobbed melon"),
                entry("768/INITIAL/EFFECTS/T_SPLAT_MELONPULT/T_SPLAT_MELONPULT.PAM",
                        "animation3", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "melon impact explosion"),
                entry("768/INITIAL/EFFECTS/MELON_EXPLODE/MELON_EXPLODE.PAM",
                        "plantfood_MelonExplode", PlayMode.ONCE, Kind.HIT, Variant.PLANT_FOOD,
                        "PF melon explosion")
        );
    }

    private static void registerWinterMelon() {
        register("Winter Melon",
                entry("768/FULL/EFFECTS/T_WINTERMELON_PROJECTILE/"
                                + "T_WINTERMELON_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "lobbed winter melon"),
                entry("768/FULL/EFFECTS/T_WINTERMELON_PROJECTILE/"
                                + "T_WINTERMELON_PROJECTILE.PAM",
                        "animation3", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF lobbed winter melon"),
                entry("768/FULL/EFFECTS/T_SPLAT_WINTERMELON/T_SPLAT_WINTERMELON.PAM",
                        "animation3", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "winter melon impact + freeze explosion"),
                entry("768/FULL/EFFECTS/WINTERMELON_EXPLODE/WINTERMELON_EXPLODE.PAM",
                        "plantfood_WintermelonExplode", PlayMode.ONCE, Kind.HIT,
                        Variant.PLANT_FOOD, "PF winter melon explosion")
        );
    }

    private static void registerPepperPult() {
        register("Pepper-pult",
                entry("768/FULL/EFFECTS/T_PEPPERPULT_PROJECTILE/"
                                + "T_PEPPERPULT_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "main lobbed pepper"),
                entry("768/FULL/EFFECTS/PEPPERPULT_PROJECTILE_SMALL/"
                                + "PEPPERPULT_PROJECTILE_SMALL.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "small pepper variant (possible duplicate of T_-prefixed asset below)"),
                entry("768/FULL/EFFECTS/T_PEPPERPULT_PROJECTILE_SMALL/"
                                + "T_PEPPERPULT_PROJECTILE_SMALL.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "small pepper variant (T_-prefixed; verify vs. entry above)"),
                entry("768/FULL/EFFECTS/PEPPERPULT_PROJECTILE_SPLAT/"
                                + "PEPPERPULT_PROJECTILE_SPLAT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "normal impact splat"),
                entry("768/FULL/EFFECTS/PEPPERPULT_PROJECTILE_PF_SPLAT/"
                                + "PEPPERPULT_PROJECTILE_PF_SPLAT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.PLANT_FOOD,
                        "PF impact splat")
        );
    }

    private static void registerPotatoMine() {
        register("Potato Mine",
                entry("768/INITIAL/EFFECTS/POTATOMINE_EXPLOSION/POTATOMINE_EXPLOSION.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "preferred mine explosion clip; renderer falls back to animation2")
        );
    }

    private static void registerPrimalPotatoMine() {
        register("Primal Potato Mine",
                entry("768/INITIAL/EFFECTS/PRIMAL_POTATOMINE_EXPLOSION/"
                                + "PRIMAL_POTATOMINE_EXPLOSION.PAM",
                        "animation3", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "mine explosion; animation3 is the actual Primal Potato Mine explosion state")
        );
    }

    private static void registerCherryBomb() {
        register("Cherry Bomb",
                entry("768/FULL/EFFECTS/CHERRYBOMB_EXPLOSION_TOP/"
                                + "CHERRYBOMB_EXPLOSION_TOP.PAM",
                        "explosion3", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "Cherry Bomb's main explosion layer")
        );
    }

    private static void registerGrapeshot() {
        register("Grapeshot",
                entry("768/INITIAL/EFFECTS/GRAPESHOT_PROJECTILE/GRAPESHOT_PROJECTILE.PAM",
                        "animation_forward", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "grape shrapnel in flight; loops for the projectile's whole lifetime"),
                entry("768/INITIAL/EFFECTS/GRAPESHOT_HIT/GRAPESHOT_HIT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "grape impact splat"),
                entry("768/INITIAL/EFFECTS/GRAPESHOT_HIT/GRAPESHOT_HIT.PAM",
                        "animation2", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "grape impact splat variant 2"),
                entry("768/INITIAL/EFFECTS/GRAPESHOT_HIT/GRAPESHOT_HIT.PAM",
                        "animation3", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "grape impact splat variant 3")
        );
    }

    private static void registerJalapeno() {
        register("Jalapeno",
                rowEntry("768/INITIAL/EFFECTS/JALAPENO_FIRE/JALAPENO_FIRE.PAM",
                        "idle2", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "Jalapeno lane fire tile effect; idle2 is the PvZ2 burn clip")
        );
    }

    private static void registerPhatBeet() {
        register("Phat Beet",
                entry("768/FULL/EFFECTS/PHATBEETS_ATTACK_PULSE/PHATBEETS_ATTACK_PULSE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "attack pulse (normal attack)"),
                entry("768/FULL/EFFECTS/PHATBEETS_TILE_HIT/PHATBEETS_TILE_HIT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "tile hit effect"),
                entry("768/FULL/EFFECTS/PHATBEETS_PF_PULSE/PHATBEETS_PF_PULSE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF pulse"),
                entry("768/FULL/EFFECTS/PHATBEETS_PF_PULSE/PHATBEETS_PF_PULSE.PAM",
                        "animation", PlayMode.LOOP, Kind.EFFECT, Variant.PLANT_FOOD,
                        "PF pulse effect on the plant tile")
        );
    }

    private static void registerKiwibeast() {
        register("Kiwibeast",
                entry("768/INITIAL/EFFECTS/KIWIBEAST_ATTACK_PULSE/"
                                + "KIWIBEAST_ATTACK_PULSE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "attack pulse (normal attack)"),
                entry("768/INITIAL/EFFECTS/KIWIBEAST_TILE_HIT/KIWIBEAST_TILE_HIT.PAM",
                        "animation", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "tile hit effect"),
                entry("768/INITIAL/EFFECTS/KIWIBEAST_PF_PULSE/KIWIBEAST_PF_PULSE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF pulse"),
                entry("768/INITIAL/EFFECTS/KIWIBEAST_PF_PULSE/KIWIBEAST_PF_PULSE.PAM",
                        "animation", PlayMode.LOOP, Kind.EFFECT, Variant.PLANT_FOOD,
                        "PF pulse effect on the plant tile")
        );
    }

    private static void registerGarlic() {
        register("Garlic",
                entry("768/INITIAL/EFFECTS/GARLIC_PROJECTILE/GARLIC_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "thrown garlic clove"),
                entry("768/INITIAL/EFFECTS/GARLIC_PROJECTILE/GARLIC_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "Plant Food bad-smell projectile"),
                entry("768/INITIAL/EFFECTS/GARLIC_STINK_LINES/GARLIC_STINK_LINES.PAM",
                        "stink", PlayMode.LOOP, Kind.EFFECT, Variant.NORMAL,
                        "trailing stink lines on the projectile")
        );
    }

    private static void registerExplodeOnut() {
        register("Explode-o-nut",
                entry("768/INITIAL/EFFECTS/GENERIC_EXPLOSION_BACK/GENERIC_EXPLOSION_BACK.PAM",
                        "animation2", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "death explosion, rear layer (cloud + ground scorch)"),
                entry("768/INITIAL/EFFECTS/GENERIC_EXPLOSION_FRONT/GENERIC_EXPLOSION_FRONT.PAM",
                        "animation2", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "death explosion, front layer (blast + debris)"),
                entry("768/INITIAL/EFFECTS/EXPLODEONUT_BLINK/EXPLODEONUT_BLINK.PAM",
                        "animation", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "warning-light blink glow on the fuse")
        );
    }

    private static void registerTorchwood() {
        register("Torchwood",
                entry("768/INITIAL/EFFECTS/TORCHWOOD_HIT_EFFECTS/"
                                + "TORCHWOOD_HIT_EFFECTS.PAM",
                        "hit_normal", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "pea passing through fire, normal hit"),
                entry("768/INITIAL/EFFECTS/TORCHWOOD_HIT_EFFECTS/"
                                + "TORCHWOOD_HIT_EFFECTS.PAM",
                        "hit_power", PlayMode.ONCE, Kind.HIT, Variant.PLANT_FOOD,
                        "pea passing through fire, plant-food hit"),
                rowEntry("768/INITIAL/EFFECTS/FIREPEASHOOTER_FIRE/FIREPEASHOOTER_FIRE.PAM",
                        "idle", PlayMode.LOOP, Kind.EFFECT, Variant.PLANT_FOOD,
                        "plant-food fire across the whole row")
        );
    }

    private static void registerHypnoShroom() {
        register("Hypno-shroom",
                entry("768/INITIAL/EFFECTS/HYPNO_ZOMBIE_EFFECT/HYPNO_ZOMBIE_EFFECT.PAM",
                        "animation", PlayMode.LOOP, Kind.EFFECT, Variant.NORMAL,
                        "overlay effect on the hypnotized zombie, persists while hypnotized")
        );
    }

    private static void registerCatTail() {
        register("Cat-tail",
                entry("768/INITIAL/EFFECTS/T_HOMING_THISTLE_PROJECTILE/"
                                + "T_HOMING_THISTLE_PROJECTILE.PAM",
                        "animation2", PlayMode.LOOP, Kind.PROJECTILE, Variant.NORMAL,
                        "homing projectile (reuses Homing Thistle's assets, per author's note)"),
                entry("768/INITIAL/EFFECTS/T_HOMING_THISTLE_PROJECTILE_HIT/"
                                + "T_HOMING_THISTLE_PROJECTILE_HIT.PAM",
                        "animation3", PlayMode.ONCE, Kind.HIT, Variant.NORMAL,
                        "homing projectile impact"),
                entry("768/INITIAL/EFFECTS/HOMING_THISTLE_PLANTFOOD_PROJECTILE/"
                                + "HOMING_THISTLE_PLANTFOOD_PROJECTILE.PAM",
                        "animation", PlayMode.LOOP, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "PF homing projectile")
        );
    }

    private static void registerIceShroom() {
        register("Ice-shroom",
                // Melee ice-swing effect, played on Ice-shroom's own tile each time its
                // periodic 3x3 attack fires - see EffectRenderer.triggerIceShroomAttacks.
                entry("768/FULL/EFFECTS/ICESHROOM_MELEE_ATTACK/ICESHROOM_MELEE_ATTACK.PAM",
                        "animation", PlayMode.ONCE, Kind.PROJECTILE, Variant.NORMAL,
                        "melee ice-swing attack effect"),
                // Freeze fx played once on every zombie caught in the 3x3 attack zone -
                // see EffectRenderer.triggerIceShroomAttacks.
                entry("768/FULL/EFFECTS/ICESHROOM_FX/ICESHROOM_FX.PAM",
                        "animation", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "freeze fx played on each targeted zombie's tile"),
                // The 9-tile (3x3) frost ground patch, alive for as long as the plant is -
                // see EffectRenderer.updateIceShroomZones.
                entry("768/FULL/EFFECTS/ICESHROOM_TILE_FX/ICESHROOM_TILE_FX.PAM",
                        "spawn", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "frost tile patch, spawn"),
                entry("768/FULL/EFFECTS/ICESHROOM_TILE_FX/ICESHROOM_TILE_FX.PAM",
                        "animation_loop", PlayMode.LOOP, Kind.EFFECT, Variant.NORMAL,
                        "frost tile patch, active loop while the plant is alive"),
                entry("768/FULL/EFFECTS/ICESHROOM_TILE_FX/ICESHROOM_TILE_FX.PAM",
                        "end", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "frost tile patch, end (once the plant is gone)"),
                // Plant Food: icicles dropping vertically onto every zombie in the 3x3 zone -
                // see EffectRenderer.triggerIceShroomPlantFood.
                entry("768/FULL/EFFECTS/ICESHROOM_PROJECTILE/ICESHROOM_PROJECTILE.PAM",
                        "animation", PlayMode.ONCE, Kind.PROJECTILE, Variant.PLANT_FOOD,
                        "Plant Food: icicle dropping vertically onto a targeted zombie")
        );
    }

    private static void registerHotPotato() {
        register("Hot Potato",
                // Melting ice puddle synergy fx, played on the ice block/frozen plant's own
                // tile - see EffectRenderer.addHotPotatoMeltEffect/drawHotPotatoMeltEffects.
                // Three-phase clip: "animation" intro (ice cracking/starting to melt),
                // "animation2" the puddle sitting there for a few seconds, "animation3" the
                // outro as the puddle fades/dries up.
                entry("768/FULL/EFFECTS/HOTPOTATO_ICEBLOCK_PUDDLE/"
                                + "HOTPOTATO_ICEBLOCK_PUDDLE.PAM",
                        "animation", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "melting ice puddle, intro (synergy fx when melting an ice block)"),
                entry("768/FULL/EFFECTS/HOTPOTATO_ICEBLOCK_PUDDLE/"
                                + "HOTPOTATO_ICEBLOCK_PUDDLE.PAM",
                        "animation2", PlayMode.LOOP, Kind.EFFECT, Variant.NORMAL,
                        "melting ice puddle, holds for a few seconds"),
                entry("768/FULL/EFFECTS/HOTPOTATO_ICEBLOCK_PUDDLE/"
                                + "HOTPOTATO_ICEBLOCK_PUDDLE.PAM",
                        "animation3", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "melting ice puddle, outro"),
                entry("768/FULL/EFFECTS/HOTPOTATO_ICEBLOCK_STEAMFX/"
                                + "HOTPOTATO_ICEBLOCK_STEAMFX.PAM",
                        "animation", PlayMode.LOOP, Kind.EFFECT, Variant.NORMAL,
                        "ice block steam (synergy fx)"),
                entry("768/FULL/EFFECTS/HOTPOTATO_STEAMFX/HOTPOTATO_STEAMFX.PAM",
                        "animation", PlayMode.LOOP, Kind.EFFECT, Variant.NORMAL,
                        "hot potato's own steam trail")
        );
    }

    private static void registerGravebuster() {
        register("Grave Buster",
                entry("768/INITIAL/EFFECTS/GRAVEBUSTER_DIRT/GRAVEBUSTER_DIRT.PAM",
                        "gravebuster_dirt_anim", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "dirt burst on entrance"),
                entry("768/INITIAL/EFFECTS/GRAVEBUSTER_DIRT/GRAVEBUSTER_DIRT.PAM",
                        "idle", PlayMode.LOOP, Kind.EFFECT, Variant.NORMAL,
                        "chewing loop while active"),
                entry("768/INITIAL/EFFECTS/GRAVEBUSTER_DIRT/GRAVEBUSTER_DIRT.PAM",
                        "gravebuster_dirt_fade", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                        "fade out once finished")
        );
    }

    private static void registerMintFamily() {
        String[] mintPlantNames = {
                "Appease-mint", "Arma-mint", "Bombard-mint", "Enchant-mint", "Enforce-mint",
                "Enlighten-mint", "Pierce-mint", "Reinforce-mint", "catTail-mint",
        };
        String path = "768/INITIAL/EMPOWERMINTS/PLANT/MINT_FX/MINT_FX.PAM";
        for (String mintName : mintPlantNames) {
            register(mintName,
                    entry(path, "intro", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                            "shared empowermint activation intro"),
                    entry(path, "loop", PlayMode.LOOP, Kind.EFFECT, Variant.NORMAL,
                            "shared empowermint active loop"),
                    entry(path, "outro", PlayMode.ONCE, Kind.EFFECT, Variant.NORMAL,
                            "shared empowermint end fx")
            );
        }
    }

    private static void registerHeadbutterLettuce() {
        register("Iceberg Lettuce",
                // Headbutt impact effect, played on the plant's own tile each time its melee
                // attack lands - see EffectRenderer.drawMeleePlantProjectiles. "animation" is
                // the front-facing (right) swing, "animation2" the back-facing (left) swing -
                // picked per-hit from MeleeStrategy's isMeleeFacingLeft() flag.
                entry("768/INITIAL/EFFECTS/HEADBUTTERLETTUCE_HITFX/HEADBUTTERLETTUCE_HITFX.PAM",
                        "animation", PlayMode.ONCE, Kind.PROJECTILE, Variant.NORMAL,
                        "headbutt impact effect, attack to the right (front)"),
                entry("768/INITIAL/EFFECTS/HEADBUTTERLETTUCE_HITFX/HEADBUTTERLETTUCE_HITFX.PAM",
                        "animation2", PlayMode.ONCE, Kind.PROJECTILE, Variant.NORMAL,
                        "headbutt impact effect, attack to the left (back)")
        );
    }
}