package model.collections.zombie.zombie_move;

import model.collections.zombie.BehaviorSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MoveBehaviorRegistry {
    private static final Map<String, MoveBehaviorFactory> REGISTRY = new HashMap<>();

    static {
        register("NormalWalk", (params, data) -> new NormalWalk());

        register("SprintMove", (params, data) -> {
            double initialScale = BehaviorSpec.getDouble(params, "baseSprintMultiplier", 1.0);
            double rageScale = BehaviorSpec.getDouble(params, "enrageMultiplier",
                    BehaviorSpec.getDouble(data, "EnragedSpeedScale", 4.0));
            boolean armorEnrage = BehaviorSpec.getBoolean(params, "enragesOnArmorLoss",
                    data != null && data.containsKey("EnragedSpeedScale"));
            return new SprintMove(initialScale, rageScale, armorEnrage);
        });

        register("PusherMove", (params, data) -> new PusherMove());
        register("ProspectorMove", (params, data) -> new ProspectorMove());

        register("JumpMove", (params, data) -> {
            double addChance = BehaviorSpec.getDouble(data, "AddRandomChanceForJumpPerGridWalked", 0.0);
            double cd = BehaviorSpec.getDouble(data, "CooldownSecondsUntilNextJumpAvailable", 0.0);
            double startChance = BehaviorSpec.getDouble(data, "InitialSetRandomChanceForJump", 0.0);
            double landedChance = BehaviorSpec.getDouble(data, "LandedResetRandomChanceForJump", 0.0);

            List<String> flyList = new ArrayList<>();
            if (data != null && data.containsKey("PlantsToFlyOver")) {
                Object outer = data.get("PlantsToFlyOver");
                if (outer instanceof Map<?, ?> outerMap) {
                    Object listObj = outerMap.get("List");
                    if (listObj instanceof List<?> list) {
                        for (Object o : list) {
                            if (o != null) flyList.add(o.toString());
                        }
                    }
                }
            }

            return new JumpMove(addChance, cd, startChance, landedChance, flyList);
        });

        register("SnorkelMove", (params, data) -> new SnorkelMove());
        register("StationaryMove", (params, data) -> new StationaryMove());

        
        register("PirateGroundWalk", (params, data) -> new PirateGroundWalk());
        register("SwashbucklerSwingMove", (params, data) -> new SwashbucklerSwingMove());
        register("SeagullFlyMove", (params, data) -> new SeagullFlyMove());
        register("PelicanFlyMove", (params, data) -> {
            double glideSpeed = BehaviorSpec.getDouble(params, "rowGlideSpeed",
                    BehaviorSpec.getDouble(data, "RowGlideSpeed", 3.0));
            return new PelicanFlyMove(glideSpeed);
        });

        register("JetpackFlyMove", (params, data) -> {
            double cooldown = BehaviorSpec.getDouble(params, "takeoffCooldown",
                    BehaviorSpec.getDouble(data, "TakeoffCooldownSeconds", 4.0));
            double chance = BehaviorSpec.getDouble(params, "takeoffChancePerSecond",
                    BehaviorSpec.getDouble(data, "TakeoffChancePerSecond", 0.35));
            double flight = BehaviorSpec.getDouble(params, "flightSeconds",
                    BehaviorSpec.getDouble(data, "FlightSeconds", 5.0));
            return new JetpackFlyMove(cooldown, chance, flight);
        });
    }

    private MoveBehaviorRegistry() {}

    public static void register(String name, MoveBehaviorFactory factory) {
        REGISTRY.put(name, factory);
    }

    public static MoveBehavior create(Object rawConfig, Map<String, Object> zombieData) {
        if (rawConfig == null) {
            return new NormalWalk();
        }
        BehaviorSpec spec = BehaviorSpec.parse(rawConfig);
        MoveBehaviorFactory factory = REGISTRY.get(spec.getType());
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported movement style: " + spec.getType());
        }
        return factory.create(spec.params(), zombieData);
    }
}