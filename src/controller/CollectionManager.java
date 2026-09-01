package controller;

import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.main.levels.Level;
import model.match_mechanisms.ZombieWave;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.LevelLoader;
import model.utils.LevelProgression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionManager {
    private static final int PURCHASE_COST = 2000;

    public static int getPurchaseCost() {
        return PURCHASE_COST;
    }

    public List<PlantJsonParser.PlantConfig> getAllPlants() {
        return new ArrayList<>(PlantFactory.getBlueprints().values());
    }

    public List<PlantJsonParser.PlantConfig> getUnlockedPlants(UserState state) {
        return getAllPlants().stream()
                .filter(p -> state.isPlantUnlocked(p.id))
                .collect(Collectors.toList());
    }

    public PlantJsonParser.PlantConfig findPlant(String name) {
        return getAllPlants().stream()
                .filter(p -> p.name.equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public Set<String> getAllZombieAliases() {
        return ZombieFactory.getAllZombieAliases();
    }

    /**
     * All zombie aliases that actually appear somewhere in the adventure levels
     * (seen or not). Excludes minigame-only zombies (e.g. Zombotany's plant-zombies,
     * I-Zombie test entries) since those never show up in a Level's waves - they
     * belong only in their own minigame, not the Collection roster.
     */
    public Set<String> getAdventureZombieAliases() {
        Set<String> aliases = new HashSet<>();
        List<Level> levels;
        try {
            levels = LevelLoader.loadLevels();
        } catch (Exception e) {
            return aliases;
        }
        for (Level level : levels) {
            if (level.getWaves() == null) continue;
            for (ZombieWave wave : level.getWaves()) {
                if (wave.getWaveZombies() == null) continue;
                for (Zombie zombie : wave.getWaveZombies()) {
                    aliases.add(zombie.getName());
                }
            }
        }
        return aliases;
    }

    public Set<String> getSeenZombieAliases(UserState state) {
        Set<String> seen = new HashSet<>();
        List<Level> levels;
        try {
            levels = LevelLoader.loadLevels();
        } catch (Exception e) {
            return seen;
        }
        List<Level> sorted = LevelProgression.sorted(levels);
        for (Level level : sorted) {
            if (!LevelProgression.isCompleted(sorted, state.lastLevel, level) || level.getWaves() == null) continue;
            for (ZombieWave wave : level.getWaves()) {
                if (wave.getWaveZombies() == null) continue;
                for (Zombie zombie : wave.getWaveZombies()) {
                    seen.add(zombie.getName());
                }
            }
        }
        return seen;
    }

    public Zombie findZombie(String alias) {
        for (String known : getAllZombieAliases()) {
            if (known.equalsIgnoreCase(alias)) return ZombieFactory.create(known, 0, 0);
        }
        return null;
    }

    public String formatPlant(PlantJsonParser.PlantConfig config, boolean unlocked, int level) {
        String tags = config.tags == null || config.tags.isEmpty()
                ? "None" : config.tags.stream().map(Enum::name).collect(Collectors.joining(", "));
        String family = config.tags == null || config.tags.isEmpty() ? "General" : config.tags.get(0).name();
        return "Name: " + config.name +
                " | Type: " + config.category +
                " | Family: " + family +
                " | Tags: " + tags +
                " | Level: " + (unlocked ? level : "-") +
                " | Status: " + (unlocked ? "Unlocked" : "Locked");
    }

    public String formatZombie(Zombie zombie) {
        String attackType = zombie.getAttackBehavior() != null
                ? zombie.getAttackBehavior().getClass().getSimpleName() : "None";
        String defenseType = zombie.getDefenseBehavior() != null
                ? zombie.getDefenseBehavior().getClass().getSimpleName() : "None";
        double speed = zombie.getSpeed() != null ? Math.abs(zombie.getSpeed().x()) : 0;

        return "Name: " + zombie.getName() +
                " | Race: " + zombie.getRace() +
                " | Attack: " + attackType +
                " | Defense: " + defenseType +
                " | HP: " + zombie.getMaxHp() +
                " | Speed: " + String.format("%.2f", speed) +
                " | Eat DPS: " + zombie.getEatDps();
    }

    public boolean purchasePlant(UserState state, PlantJsonParser.PlantConfig config) {
        if (state.isPlantUnlocked(config.id) || state.coins < PURCHASE_COST) return false;
        state.coins -= PURCHASE_COST;
        state.unlockPlant(config.id);
        NewsManager.generateNews("PLANT", config.name,
                formatPlant(config, true, state.getPlantLevel(config.id)));

        // Persist the modified UserState immediately so coins/unlocks survive
        // leaving the collection screen or restarting the application.
        User.save();
        return true;
    }

    public boolean upgradePlant(UserState state, PlantJsonParser.PlantConfig config) {
        if (!state.isPlantUnlocked(config.id)) return false;
        int currentLevel = state.getPlantLevel(config.id);
        int coinCost = currentLevel * 500;
        int packetsNeeded = currentLevel;
        if (state.coins < coinCost || state.seedPacketInventory.getOrDefault(config.id, 0) < packetsNeeded) {
            return false;
        }
        state.coins -= coinCost;
        state.seedPacketInventory.merge(config.id, -packetsNeeded, Integer::sum);
        state.setPlantLevel(config.id, currentLevel + 1);

        // Persist the modified UserState immediately. Without this, the in-memory
        // upgrade disappears when the application is restarted.
        User.save();
        return true;
    }
}