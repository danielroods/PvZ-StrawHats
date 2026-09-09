package model.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.main.levels.Level;
import model.match.main.levels.normal_levels.NormalLevel;
import model.match.boss.ZombossChapter;
import model.match.main.levels.special_levels.*;
import model.match.main.season.SeasonFactory;
import model.match_mechanisms.Time;
import model.match_mechanisms.ZombieWave;
import model.match_mechanisms.vector.Position;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelLoader {
    private static final Gson gson = new Gson();
    private static final List<String> MINI_GAME_ONLY_ZOMBIES = List.of(
            "ZombiePeashooter", "ZombieWallnut", "ZombieJalapeno", "ZombieSquash",
            "ZombieGatlingPea", "ZombieTallnut"
    );

    public static List<Level> loadLevels() throws java.io.IOException {
        return loadLevels("Levels.json");
    }

    public static List<Level> loadLevels(String resourcePath) throws java.io.IOException {
        try (var is = ResourceResolver.open(resourcePath)) {
            if (is == null) {
                throw new java.io.FileNotFoundException("Could not find " + resourcePath);
            }
            Type listType = new TypeToken<List<JsonObject>>() {}.getType();
            List<JsonObject> rawLevels = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), listType);
            if (rawLevels == null) return new ArrayList<>();
            List<Level> levels = new ArrayList<>();
            for (JsonObject raw : rawLevels) {
                levels.add(parseLevel(raw));
            }
            return levels;
        }
    }

    private static Level parseLevel(JsonObject raw) {
        String type = raw.get("type").getAsString();
        Level level;

        switch (type) {
            case "normal":          level = new NormalLevel(); break;
            case "conveyor":        level = new ConveyorBeltLevel(); break;
            case "locked":          level = new LockedPlantsLevel(); break;
            case "saveSeeds":       level = new SaveOurSeedsLevel(); break;
            case "moldBlock":       level = new NotEveryWhereYouCanPlantLevel(); break;
            case "timedWar":        level = new TimedWarLevel(); break;
            case "nightOps":        level = new NightOpsLevel(); break;
            case "deadLine":        level = new DeadLineLevel(); break;
            case "lovePlants":      level = new LoveYourPlantsLevel(); break;
            case "plantWhatYouGet": level = new PlantWhatYouGetLevel(); break;
            case "boss":            level = new BossLevel(); break;
            case "intro":           level = new IntroductionLevel(); break;
            default: throw new IllegalArgumentException("Unknown level type: " + type);
        }

        level.setId(raw.get("id").getAsInt());
        level.setName(raw.get("name").getAsString());
        level.setGameMode(raw.has("gameMode")
                ? raw.get("gameMode").getAsString()
                : defaultGameMode(type));

        String seasonName = raw.get("season").getAsString();
        level.setSeason(SeasonFactory.create(seasonName));

        level.setRows(raw.has("rows") ? raw.get("rows").getAsInt() : 5);
        level.setCols(raw.has("cols") ? raw.get("cols").getAsInt() : 9);
        int initialSun = raw.has("initialSun")
                ? raw.get("initialSun").getAsInt()
                : raw.has("primarySun") ? raw.get("primarySun").getAsInt() : 150;
        level.setInitialSun(initialSun);

        List<String> availablePlants = new ArrayList<>();
        if (raw.has("availablePlants")) {
            raw.get("availablePlants").getAsJsonArray().forEach(e -> availablePlants.add(e.getAsString()));
        }
        level.setAvailablePlants(availablePlants);

        List<String> forcedPlants = new ArrayList<>();
        if (raw.has("forcedPlants")) {
            raw.get("forcedPlants").getAsJsonArray().forEach(e -> forcedPlants.add(e.getAsString()));
        }
        level.setForcedPlants(forcedPlants);

        List<String> zombiePool = new ArrayList<>();
        if (raw.has("zombiePool")) {
            raw.get("zombiePool").getAsJsonArray().forEach(e -> zombiePool.add(e.getAsString()));
        }
        level.setZombiePool(zombiePool);

        if (raw.has("waves")) {
            JsonArray waveArray = raw.get("waves").getAsJsonArray();
            List<ZombieWave> waves = new ArrayList<>();
            for (var wElem : waveArray) {
                JsonObject w = wElem.getAsJsonObject();
                double delay = w.get("delay").getAsDouble();
                JsonArray zombieTypes = w.get("zombies").getAsJsonArray();
                List<Zombie> zombies = new ArrayList<>();
                for (var zt : zombieTypes) {
                    String zombieType = zt.getAsString();
                    validateAdventureZombie(level, zombieType);
                    Zombie z = ZombieFactory.create(zombieType, 0, level.getCols());
                    z.setPosition(new Position(level.getCols(), 0));
                    zombies.add(z);
                }
                waves.add(new ZombieWave(delay, zombies));
            }
            level.setWaves(waves);
        } else {
            level.setWaves(new ArrayList<>());
        }

        if (level instanceof ConveyorBeltLevel) {
            List<Plant> conveyorPlants = new ArrayList<>();
            if (raw.has("conveyorPlants")) {
                raw.get("conveyorPlants").getAsJsonArray().forEach(e -> {
                    String plantType = e.getAsString();
                    Integer plantId = null;
                    for (var config : PlantFactory.getBlueprints().values()) {
                        if (config.name.equalsIgnoreCase(plantType)) {
                            plantId = config.id;
                            break;
                        }
                    }
                    if (plantId == null) return;
                    Plant p = PlantFactory.createPlant(plantId,
                            model.collections.plant.PlantProgression.currentLevelOf(plantId),
                            new Position(9, 0));
                    conveyorPlants.add(p);
                });
            }
            ((ConveyorBeltLevel) level).setConveyorPlants(conveyorPlants);
            if (raw.has("maxConveyorSize")) {
                ((ConveyorBeltLevel) level).setMaxConveyorSize(raw.get("maxConveyorSize").getAsInt());
            }
        } else if (level instanceof LockedPlantsLevel) {
            List<String> locked = new ArrayList<>();
            if (raw.has("lockedPlants")) {
                raw.get("lockedPlants").getAsJsonArray().forEach(e -> locked.add(e.getAsString()));
            }
            ((LockedPlantsLevel) level).setLockedPlants(locked);

            List<String> always = new ArrayList<>();
            if (raw.has("alwaysAvailable")) {
                raw.get("alwaysAvailable").getAsJsonArray().forEach(e -> always.add(e.getAsString()));
            }
            ((LockedPlantsLevel) level).setAlwaysAvailable(always);
        } else if (level instanceof SaveOurSeedsLevel) {
            if (raw.has("seedPositions")) {
                JsonObject seedObj = raw.get("seedPositions").getAsJsonObject();
                Map<Position, String> seedMap = new HashMap<>();
                for (var entry : seedObj.entrySet()) {
                    String key = entry.getKey(); // "row,col"
                    String plantType = entry.getValue().getAsString();
                    String[] parts = key.split(",");
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);
                    seedMap.put(new Position(col, row), plantType);
                }
                ((SaveOurSeedsLevel) level).setSeedPositions(seedMap);
            }
        } else if (level instanceof NotEveryWhereYouCanPlantLevel) {
            if (raw.has("blockedColumns")) {
                List<Integer> blocked = new ArrayList<>();
                raw.get("blockedColumns").getAsJsonArray().forEach(e -> blocked.add(e.getAsInt()));
                ((NotEveryWhereYouCanPlantLevel) level).setBlockedColumns(blocked);
            }
        } else if (level instanceof TimedWarLevel) {
            double timeSec = raw.get("timeLimitSeconds").getAsDouble();
            ((TimedWarLevel) level).setTimeLimit(new Time(timeSec));
            // zombiesToKill is no longer part of the win/loss logic (this is a pure survival
            // level now), but the field may still linger in older level JSON - ignore it if
            // present instead of requiring it.
        } else if (level instanceof DeadLineLevel) {
            int col = raw.get("deadLineColumn").getAsInt();
            ((DeadLineLevel) level).setDeadLine(new Position(col, 0));
        } else if (level instanceof LoveYourPlantsLevel) {
            int maxLoss = raw.has("maxPlantLoss") ? raw.get("maxPlantLoss").getAsInt() : 3;
            ((LoveYourPlantsLevel) level).setMaxPlantLoss(maxLoss);
        } else if (level instanceof PlantWhatYouGetLevel) {
            ((PlantWhatYouGetLevel) level).setPrimarySun(initialSun);
        } else if (level instanceof BossLevel bossLevel) {
            String chapterKey = raw.has("chapter")
                    ? raw.get("chapter").getAsString() : seasonName;
            ZombossChapter chapter = ZombossChapter.fromSeason(chapterKey);
            if (chapter == null) {
                throw new IllegalArgumentException(
                        "No Zomboss chapter is defined for season " + seasonName + ".");
            }
            bossLevel.setChapter(chapter);
            bossLevel.setBossZombie(ZombieFactory.create(chapter.getAlias(), 0, level.getCols()));
            level.setWaves(new ArrayList<>());
        }

        for (ZombieWave wave : level.getWaves()) wave.setFinalWave(false);
        if (!level.getWaves().isEmpty()) {
            level.getWaves().get(level.getWaves().size() - 1).setFinalWave(true);
        }

        validateWaveDifficulty(level);

        return level;
    }

    private static String defaultGameMode(String type) {
        return switch (type) {
            case "normal" -> "Adventure - Normal";
            case "intro" -> "Adventure - Introduction";
            case "boss" -> "Adventure - Boss";
            case "conveyor" -> "Adventure - Special: Conveyor Belt";
            case "locked" -> "Adventure - Special: Locked Plants";
            case "saveSeeds" -> "Adventure - Special: Save Your Seeds";
            case "moldBlock" -> "Adventure - Special: Not Every Where You Can Plant!";
            case "timedWar" -> "Adventure - Special: Timed War";
            case "nightOps" -> "Adventure - Special: Night Ops";
            case "deadLine" -> "Adventure - Special: Dead Line";
            case "lovePlants" -> "Adventure - Special: Love Your Plants";
            case "plantWhatYouGet" -> "Adventure - Special: Plant What You Get";
            default -> "Adventure";
        };
    }

    private static void validateAdventureZombie(Level level, String zombieType) {
        if (MINI_GAME_ONLY_ZOMBIES.stream().anyMatch(alias -> alias.equalsIgnoreCase(zombieType))) {
            throw new IllegalArgumentException(zombieType + " is reserved for Zombotany and cannot be used in Adventure.");
        }
        if (level.getZombiePool() != null && !level.getZombiePool().isEmpty()
                && level.getZombiePool().stream().noneMatch(alias -> alias.equalsIgnoreCase(zombieType))) {
            throw new IllegalArgumentException(zombieType + " is not in the zombie pool for " + level.getName() + ".");
        }
    }

    private static void validateWaveDifficulty(Level level) {
        List<ZombieWave> waves = level.getWaves();
        if (waves == null || waves.isEmpty()) return;

        for (int i = 1; i < waves.size(); i++) {
            int previousCost = waves.get(i - 1).getWaveCost();
            int currentCost = waves.get(i).getWaveCost();
            double requiredMultiplier = waves.get(i).isFinalWave() ? 2.0 : 1.25;
            if (currentCost + 0.001 < previousCost * requiredMultiplier) {
                throw new IllegalArgumentException("Unbalanced waves in " + level.getName()
                        + ": wave " + (i + 1) + " costs " + currentCost
                        + " but must cost at least " + requiredMultiplier + "x wave " + i
                        + " (" + previousCost + ").");
            }
        }
    }

    public static Level loadLevelById(int levelId) throws java.io.IOException {
        return loadLevels().stream()
                .filter(level -> level.getId() == levelId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown level id: " + levelId));
    }
}