package model.collections.zombie;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.collections.armour.Armour;
import model.collections.armour.ArmourType;
import model.collections.armour.ZombieArmour;
import model.collections.zombie.zombie_attack.AttackBehaviorRegistry;
import model.collections.zombie.zombie_defense.DefenseBehaviorRegistry;
import model.collections.zombie.zombie_effect.EffectStatusRegistry;
import model.collections.zombie.zombie_move.MoveBehaviorRegistry;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.pitches.obstacles.PushableType;
import model.utils.GameSession;
import model.utils.ResourceResolver;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZombieFactory {
    private static final Map<String, Map<String, Object>> blueprints = new HashMap<>();
    private static final Map<String, Integer> armorBaseHp = new HashMap<>();
    private static final int DEFAULT_WAVE_POINT_COST = 100;
    private static final double DEFAULT_SPEED = 0.185;
    private static boolean loaded = false;

    public static void init() {
        if (loaded) return;
        loadZombies();
        loadArmorData();
        loaded = true;
    }

    @SuppressWarnings("unchecked")
    private static void loadZombies() {
        try (var is = ResourceResolver.open("Zombie.json")) {
            if (is == null) throw new IOException("Zombie.json was not found");
            Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> entries = new Gson().fromJson(new java.io.InputStreamReader(is), listType);
            if (entries == null) return;

            for (Map<String, Object> entry : entries) {
                List<String> aliases = (List<String>) entry.get("aliases");
                Map<String, Object> objdata = (Map<String, Object>) entry.get("objdata");
                if (aliases == null || objdata == null) continue;
                for (String alias : aliases) {
                    blueprints.put(alias, objdata);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load Zombie.json: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadArmorData() {
        try (var is = ResourceResolver.open("ArmorTypeData.json")) {
            if (is == null) throw new IOException("ArmorTypeData.json was not found");
            Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> entries = new Gson().fromJson(new java.io.InputStreamReader(is), listType);
            if (entries == null) return;

            for (Map<String, Object> entry : entries) {
                List<String> aliases = (List<String>) entry.get("aliases");
                Map<String, Object> objdata = (Map<String, Object>) entry.get("objdata");
                if (aliases == null || objdata == null || !objdata.containsKey("BaseHealth")) continue;
                int baseHp = ((Number) objdata.get("BaseHealth")).intValue();
                for (String alias : aliases) {
                    armorBaseHp.put(alias, baseHp);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load ArmorTypeData.json: " + e.getMessage());
        }
    }

    public static boolean shouldSpawnFrosted(String alias) {
        init();
        Map<String, Object> data = blueprints.get(alias);
        if (data == null) return false;
        Object value = data.get("Frosted");
        if (value == null) value = data.get("StartsFrozenInIce");
        if (value == null) value = data.get("SpawnInIceBlock");
        return Boolean.TRUE.equals(value);
    }

    public static int getZombieCost(String alias) {
        init();
        Map<String, Object> data = blueprints.get(alias);
        if (data == null) return DEFAULT_WAVE_POINT_COST;
        return ((Number) data.getOrDefault("WavePointCost", DEFAULT_WAVE_POINT_COST)).intValue();
    }

    /**
     * True for zombies whose configured movement is airborne/flying. Pirate Seas uses
     * this when choosing spawn lanes: flyers may enter any lane, while ground zombies
     * must enter through a bridge lane so they never appear in the open-water section.
     */
    public static boolean isFlying(String alias) {
        init();
        Map<String, Object> data = blueprints.get(alias);
        if (data == null) return false;
        Object move = data.get("move");
        Object type = move instanceof Map<?, ?> spec ? ((Map<?, ?>) spec).get("type") : move;
        if (!(type instanceof String moveType)) return false;
        return "SeagullFlyMove".equals(moveType) || "PelicanFlyMove".equals(moveType);
    }

    @SuppressWarnings("unchecked")
    private static ZombieStunProfile resolveStunProfile(Map<String, Object> data) {
        Object raw = data.get("Stun");
        if (!(raw instanceof Map<?, ?>)) return null;
        Map<String, Object> stun = (Map<String, Object>) raw;
        return new ZombieStunProfile(
                BehaviorSpec.getDouble(stun, "HealthPercent", 0.5),
                BehaviorSpec.getDouble(stun, "Seconds", 3.0),
                BehaviorSpec.getBoolean(stun, "Immobilizes", false));
    }

    public static boolean entersOnBoard(String alias) {
        init();
        Map<String, Object> data = blueprints.get(alias);
        return data != null && Boolean.TRUE.equals(data.get("EntersOnBoard"));
    }

    public static boolean isStationaryMover(String alias) {
        init();
        Map<String, Object> data = blueprints.get(alias);
        if (data == null) return false;
        Object move = data.get("move");
        Object type = move instanceof Map<?, ?> spec ? ((Map<?, ?>) spec).get("type") : move;
        return "StationaryMove".equals(type);
    }

    public static double getZombieSpeed(String alias) {
        init();
        Map<String, Object> data = blueprints.get(alias);
        if (data == null) return DEFAULT_SPEED;
        return ((Number) data.getOrDefault("Speed", DEFAULT_SPEED)).doubleValue();
    }

    public static ZombieRace getZombieRace(String alias) {
        init();
        Map<String, Object> data = blueprints.get(alias);
        Object size = data == null ? null : data.get("Size");
        return raceFor(size instanceof String ? (String) size : "default");
    }

    private static ZombieRace raceFor(String sizeStr) {
        return switch (sizeStr.toLowerCase()) {
            case "imp" -> ZombieRace.IMP;
            case "large" -> ZombieRace.GARGANTUAR;
            default -> ZombieRace.DEFAULT;
        };
    }

    @SuppressWarnings("unchecked")
    public static Zombie create(String alias, int row, int col) {
        init();
        Map<String, Object> blueprint = blueprints.get(alias);
        if (blueprint == null) {
            throw new IllegalArgumentException("Unknown zombie alias: " + alias);
        }

        Map<String, Object> data = new java.util.HashMap<>(blueprint);
        Zombie zombie = buildBaseZombie(alias, data, row, col);

        Object moveSpec = data.getOrDefault("move", "NormalWalk");

        // Generic armor aliases reuse the chapter's basic-zombie art. In Pirate
        // Seas they must also use the pirate ground movement so they respect
        // bridges/open water exactly like ZombiePirateBasic.
        if (isPirateSeason() && isGenericArmorAlias(alias)) {
            moveSpec = "PirateGroundWalk";
        }

        Object attackSpec = data.getOrDefault("attack", "ChompAttack");
        if ("ZombiePiano".equals(alias) && attackSpec instanceof String) {
            attackSpec = Map.of("type", "CrushAttack", "crushDamage", 99999);
        }
        Object defenseSpec = data.getOrDefault("defense", "NormalDefense");
        Object effectSpec = data.get("effect");
        if (effectSpec == null && "ZombiePiano".equals(alias)) effectSpec = "PianistMusicEffect";

        zombie.setMoveBehavior(MoveBehaviorRegistry.create(moveSpec, data));
        zombie.setAttackBehavior(AttackBehaviorRegistry.create(attackSpec, data));
        zombie.setDefenseBehavior(DefenseBehaviorRegistry.create(defenseSpec));
        zombie.setEffectStatus(EffectStatusRegistry.createOrNull(effectSpec, data));
        zombie.setStunProfile(resolveStunProfile(data));

        attachPushedStructureIfNeeded(zombie, data);

        return zombie;
    }

    private static boolean isGenericArmorAlias(String alias) {
        return "ZombieArmor1".equals(alias)
                || "ZombieArmor2".equals(alias)
                || "ZombieArmor4".equals(alias);
    }

    private static boolean isPirateSeason() {
        GameSession session = GameSession.peekInstance();
        if (session == null || session.getLevel() == null || session.getLevel().getSeason() == null) {
            return false;
        }
        String season = session.getLevel().getSeason().getName();
        if (season == null) return false;
        String normalized = season.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("pirate");
    }

    @SuppressWarnings("unchecked")
    private static Zombie buildBaseZombie(String alias, Map<String, Object> data, int row, int col) {
        int hp = ((Number) data.getOrDefault("Hitpoints", 270)).intValue();
        double speed = ((Number) data.getOrDefault("Speed", 1)).doubleValue();
        double eatDps = ((Number) data.getOrDefault("EatDPS", 60)).doubleValue();

        String sizeStr = data.containsKey("Size") ? (String) data.get("Size") : "default";
        ZombieRace race = switch (sizeStr.toLowerCase()) {
            case "imp" -> ZombieRace.IMP;
            case "large" -> ZombieRace.GARGANTUAR;
            default -> ZombieRace.DEFAULT;
        };

        boolean canSpawnPlantFood = !data.containsKey("CanSpawnPlantFood") || (Boolean) data.get("CanSpawnPlantFood");
        Armour armour = resolveArmor(data);

        Zombie zombie = new Zombie(alias, armour, canSpawnPlantFood);
        zombie.setMaxHp(hp);
        zombie.setHp(hp);
        zombie.setEatDps(eatDps);
        zombie.setRace(race);

        if (data.containsKey("DamageWhileSubmerged")) {
            Map<String, Object> map = (Map<String, Object>) data.get("DamageWhileSubmerged");
            zombie.setDamageWhileSubmerged((List<String>) map.get("List"));
        }
        if (data.containsKey("DamageWhileSubmergedPlantfoodOnly")) {
            Map<String, Object> map = (Map<String, Object>) data.get("DamageWhileSubmergedPlantfoodOnly");
            zombie.setDamageWhileSubmergedPlantfoodOnly((List<String>) map.get("List"));
        }

        Position spawnPos = Position.of(col, row);
        zombie.setPosition(spawnPos);
        double runningSpeedScale = ((Number) data.getOrDefault("RunningSpeedScale", 1)).doubleValue();
        zombie.setSpeed(Position.of(-speed * runningSpeedScale, 0));

        applyDifficultyScaling(zombie, data);

        return zombie;
    }

    @SuppressWarnings("unchecked")
    private static void applyDifficultyScaling(Zombie zombie, Map<String, Object> data) {
        int diff = 1;
        GameSession session = GameSession.peekInstance();
        if (session != null && session.getDifficultyLevel() > 0) {
            diff = session.getDifficultyLevel();
        }

        List<Map<String, Object>> scaledProps = (List<Map<String, Object>>) data.get("ScaledProps");
        if (scaledProps == null) return;

        for (Map<String, Object> prop : scaledProps) {
            if (!"standard".equals(prop.get("Formula"))) continue;

            double arg1 = ((Number) prop.get("Arg1")).doubleValue();
            double arg2 = ((Number) prop.get("Arg2")).doubleValue();
            double scale = arg1 + ((diff - 1) * arg2);

            if ("Hitpoints".equals(prop.get("Key"))) {
                zombie.setMaxHp((int) (zombie.getMaxHp() * scale));
                zombie.setHp(zombie.getMaxHp());
            } else if ("EatDPS".equals(prop.get("Key"))) {
                zombie.setEatDps(zombie.getEatDps() * scale);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void attachPushedStructureIfNeeded(Zombie zombie, Map<String, Object> data) {
        PushableType type = switch (zombie.getAlias()) {
            case "ZombieArcade" -> PushableType.ARCADE_CABINET;
            case "ZombieIceAgeTroglobite" -> PushableType.ICE_BLOCK;
            case "ZombieBarrelRoller", "ZombieBarrel" -> PushableType.BARREL;
            default -> null;
        };

        if (type == null) return;

        if (type == PushableType.ICE_BLOCK) {
            // Troglobites spawn already frozen solid inside their own ice (see
            // Cave.placeFrostedZombies / FrostbiteFreezing.freezeZombieInIce). The
            // pushable ice block - the same block, now containing a frozen imp - only
            // drops onto the lawn once that ice melts and the zombie is released;
            // see spawnFallingIceBlockOnRelease(), called from IceBlock.release().
            // Only ever the one block - it is not replaced once destroyed.
            zombie.setPushableRespawnsRemaining(1);
            return;
        }

        Position position = zombie.getPosition();
        PushableStructure structure = new PushableStructure(type,
                new Position(position.x() - 0.6, position.y()));
        structure.setOwner(zombie);
        zombie.setPushedStructure(structure);
        placeOnLawnIfPossible(zombie, structure);
    }

    /**
     * Called once a Troglobite's surrounding ice block melts away and frees it. Drops a
     * fresh pushable ice block - with a frozen imp waiting inside - onto the cell just
     * ahead of the zombie, the same spot an arcade cabinet would be placed for a
     * ZombieArcade, and starts its "falls smoothly from the sky" animation.
     */
    public static void spawnFallingIceBlockOnRelease(Zombie zombie) {
        if (zombie == null || !"ZombieIceAgeTroglobite".equals(zombie.getAlias())) return;
        if (zombie.getPushedStructure() != null) return;
        if (zombie.getPushableRespawnsRemaining() <= 0) zombie.setPushableRespawnsRemaining(1);
        spawnNextIceBlock(zombie);
    }

    private static void spawnNextIceBlock(Zombie zombie) {
        Position position = zombie.getPosition();
        if (position == null) return;
        PushableStructure structure = new PushableStructure(PushableType.ICE_BLOCK,
                new Position(position.x() - 0.6, position.y()));
        structure.setOwner(zombie);
        structure.startFalling();
        zombie.setPushedStructure(structure);
        zombie.setPushableRespawnsRemaining(Math.max(0, zombie.getPushableRespawnsRemaining() - 1));
        placeOnLawnIfPossible(zombie, structure);
    }

    public static void respawnPushedStructureIfNeeded(Zombie zombie) {
        PushableStructure current = zombie.getPushedStructure();
        if (current == null || current.isAlive() || zombie.getPushableRespawnsRemaining() <= 0) {
            return;
        }

        GameSession session = GameSession.peekInstance();
        if (session != null) session.registerStructure(current);

        if (current.getType() == PushableType.ICE_BLOCK) {
            // Troglobites only ever get the one ice block (see attachPushedStructureIfNeeded);
            // it is not replaced once destroyed.
            return;
        }

        Position position = zombie.getPosition();
        PushableStructure fresh = new PushableStructure(current.getType(),
                new Position(position.x() - 0.6, position.y()));
        fresh.setOwner(zombie);
        zombie.setPushedStructure(fresh);
        zombie.setPushableRespawnsRemaining(zombie.getPushableRespawnsRemaining() - 1);
        placeOnLawnIfPossible(zombie, fresh);
    }

    private static void placeOnLawnIfPossible(Zombie zombie, PushableStructure structure) {
        GameSession session = GameSession.getInstance();
        if (session == null || session.getLawn() == null) return;

        session.registerStructure(structure);
    }

    @SuppressWarnings("unchecked")
    private static Armour resolveArmor(Map<String, Object> data) {
        List<String> armorProps = (List<String>) data.get("ZombieArmorProps");
        if (armorProps == null || armorProps.isEmpty()) return null;

        int accumulatedArmorHp = 0;
        ArmourType primaryType = null;

        for (String rtid : armorProps) {
            String armorAlias = parseRtidAlias(rtid);
            ArmourType resolvedType = resolveArmorType(armorAlias);
            if (resolvedType != null) {
                primaryType = resolvedType;
                int hpValue = "NewspaperDefault".equals(armorAlias)
                        ? ordinaryZombieHp()
                        : armorBaseHp.getOrDefault(armorAlias, resolvedType.getArmorHp());
                accumulatedArmorHp += hpValue;
            }
        }

        if (primaryType == null || accumulatedArmorHp <= 0) return null;
        return new ZombieArmour(primaryType, accumulatedArmorHp);
    }

    private static int ordinaryZombieHp() {
        Map<String, Object> defaultBlueprint = blueprints.get("ZombieDefault");
        if (defaultBlueprint != null && defaultBlueprint.get("Hitpoints") instanceof Number hp) {
            return hp.intValue();
        }
        return ArmourType.NEWSPAPER.getArmorHp();
    }

    public static Armour createKnightArmor() {
        int crownHp = armorBaseHp.getOrDefault("CrownDefault", ArmourType.CROWN.getArmorHp());
        return new ZombieArmour(ArmourType.CROWN, crownHp);
    }

    private static String parseRtidAlias(String rtid) {
        int start = rtid.indexOf('(');
        int at = rtid.indexOf('@');
        if (start < 0 || at < 0) return rtid;
        return rtid.substring(start + 1, at);
    }

    private static ArmourType resolveArmorType(String alias) {
        return switch (alias) {
            case "ConeDefault" -> ArmourType.CONE;
            case "BucketDefault" -> ArmourType.BUCKET;
            case "BrickDefault" -> ArmourType.BRICK;
            case "CrownDefault" -> ArmourType.CROWN;
            case "ShoulderArmorDefault" -> ArmourType.SHOULDER_ARMOR;
            case "NewspaperDefault" -> ArmourType.NEWSPAPER;
            default -> null;
        };
    }

    public static Set<String> getAllZombieAliases() {
        init();
        return blueprints.keySet();
    }
}