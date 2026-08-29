package model.collections.zombie.zombie_effect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EffectStatusRegistry {

    public static ZombieEffectStatus createOrNull(Object spec, Map<String, Object> zombieData) {
        if (spec == null) return null;

        String type;
        Map<String, Object> params;

        if (spec instanceof String s) {
            type = s;
            params = Map.of();
        } else if (spec instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) map.put(String.valueOf(entry.getKey()), entry.getValue());
            Object typeValue = map.get("type");
            type = typeValue != null ? typeValue.toString() : null;
            params = map;
        } else {
            return null;
        }

        if (type == null) return null;
        Map<String, Object> data = zombieData == null ? Map.of() : zombieData;

        return switch (type) {
            case "FireEffect" -> new FireEffect(getDouble(params, data, "blastRadius", 1.0));

            case "GargantuarImpThrowEffect", "GigantorImpChucker" -> new GigantorImpChucker(
                    gargThreshold(params, data),
                    getDouble(params, data, "arcApex", getDouble(params, data, "ImpApex", 150)),
                    getDouble(params, data, "flightDuration", getDouble(params, data, "ImpFlightTime", 1.0)),
                    getInt(params, data, "targetCol", getInt(params, data, "ImpTargetColumn", 2)),
                    getDouble(params, data, "minSpawnX", getDouble(params, data, "MinPosXThrowImp", 0)),
                    resolveImpAlias(getString(params, data, "impAlias", getString(params, data, "ImpType", "ZombieImp"))));

            case "TombRaiserEffect", "GraveErectorStatus" -> new GraveErectorStatus(
                    getDouble(params, data, "cooldown", 15.0),
                    getInt(params, data, "spawnCount", 2));

            case "IceAgeHunterEffect" -> new IceAgeHunterEffect(getDouble(params, data, "snowballDelay", 4.0));
            case "FishermanEffect", "ReelingTackleStatus" -> new ReelingTackleStatus(getDouble(params, data, "reelCooldown", 6.0));
            case "OctopusThrowEffect", "OctopusThrow" -> new OctopusThrow(getDouble(params, data, "snareCooldown", 5.0));
            case "SpinEffect", "RotationalTurbulenceState" -> new RotationalTurbulenceState();
            case "WizardEffect", "MageState" -> new MageState(getDouble(params, data, "hexCooldown", 4.0));
            case "KingBuffEffect" -> new KingBuffEffect(getDouble(params, data, "coronationCooldown", 10.0));
            case "PianistMusicEffect" -> new PianistMusicEffect(getDouble(params, data, "tempoDelay", 5.0));

            case "SunThief" -> new SunThief(
                    getBoolean(params, data, "isBankThief", false),
                    getInt(params, data, "maxSunsToSteal", 50),
                    getDouble(params, data, "dropRatioOnDeath", 0.5),
                    getDouble(params, data, "chargingTime", 5.0),
                    getInt(params, data, "laserDamage", 4001));

            case "CrystalSkullBeamEffect" -> new CrystalSkullBeamEffect(
                    getInt(params, data, "laserDamage", getInt(params, data, "LaserBeamDamage", 4001)),
                    getDouble(params, data, "chargingTime", getDouble(params, data, "ChargingTime", 5.0)),
                    getDouble(params, data, "laserCooldownTime", getDouble(params, data, "LaserCooldownTime", 5.0)));

            case "PeashooterZombieEffect" -> new PeashooterZombieEffect(
                    getInt(params, data, "damage", 20),
                    getDouble(params, data, "fireRate", 1.5),
                    getInt(params, data, "shotsPerVolley", 1),
                    getDouble(params, data, "volleyGap", 0.2));
            case "JalapenoZombieEffect", "ThermiteExplosion" -> new ThermiteExplosion();
            case "SunProducer" -> new SunProducer();
            default -> null;
        };
    }

    private static double gargThreshold(Map<String, Object> params, Map<String, Object> data) {
        double direct = getDouble(params, data, "launchThreshold", -1);
        if (direct >= 0) return direct;
        Object layers = data.get("HealthThresholdToImpAmmoLayers");
        if (layers instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object value = first.get("HealthPercentThrowImp");
            if (value instanceof Number number) return number.doubleValue();
        }
        return 0.5;
    }

    private static String resolveImpAlias(String raw) {
        if (raw == null) return "ZombieImp";
        String normalized = raw.toLowerCase().replace("-", "").replace("_", "").replace(" ", "");
        return normalized.contains("imp") ? "ZombieImp" : raw;
    }

    private static Object value(Map<String, Object> params, Map<String, Object> data, String key) {
        return params.containsKey(key) ? params.get(key) : data.get(key);
    }

    private static double getDouble(Map<String, Object> params, Map<String, Object> data, String key, double defaultValue) {
        Object value = value(params, data, key);
        return value instanceof Number number ? number.doubleValue() : defaultValue;
    }

    private static int getInt(Map<String, Object> params, Map<String, Object> data, String key, int defaultValue) {
        Object value = value(params, data, key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    private static boolean getBoolean(Map<String, Object> params, Map<String, Object> data, String key, boolean defaultValue) {
        Object value = value(params, data, key);
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    private static String getString(Map<String, Object> params, Map<String, Object> data, String key, String defaultValue) {
        Object value = value(params, data, key);
        return value instanceof String str ? str : defaultValue;
    }
}
