package net.client;

import model.collections.armour.ArmourType;
import model.collections.armour.ZombieArmour;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.collections.zombie.ZombieState;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import net.dto.MatchSnapshot;
import service.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SnapshotApplier {

    private final GameSession session;
    private final Map<Integer, Plant> plants = new HashMap<>();
    private final Map<Integer, Zombie> zombies = new HashMap<>();
    private final Map<Integer, GroundSun> suns = new HashMap<>();
    private final java.util.List<Zombie> recentlyRemoved = new java.util.ArrayList<>();

    public SnapshotApplier(GameSession session) {
        this.session = session;
    }

    public void apply(MatchSnapshot snapshot) {
        if (snapshot == null) return;
        applyPlants(snapshot);
        applyZombies(snapshot);
        applyItems(snapshot);
    }

    private void applyPlants(MatchSnapshot snapshot) {
        Set<Integer> seen = new HashSet<>();
        for (MatchSnapshot.PlantDto dto : snapshot.plants) {
            seen.add(dto.id);
            Plant plant = plants.get(dto.id);
            if (plant == null) {
                plant = createPlant(dto);
                if (plant == null) continue;
                plants.put(dto.id, plant);
                session.getPlants().add(plant);
                var cell = session.getEnvironment().getCell(dto.row, dto.col);
                if (cell != null) cell.setPlant(plant);
            }
            plant.setHP(dto.hp);
            plant.setAlive(dto.hp > 0);
        }
        plants.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            Plant plant = entry.getValue();
            session.getPlants().remove(plant);
            if (plant.getPosition() != null) {
                int row = (int) Math.round(plant.getPosition().y());
                int col = (int) Math.round(plant.getPosition().x());
                var cell = session.getEnvironment().getCell(row, col);
                if (cell != null && cell.getPlant() == plant) cell.setPlant(null);
            }
            return true;
        });
    }

    private Plant createPlant(MatchSnapshot.PlantDto dto) {
        try {
            return PlantFactory.createPlant(dto.plantId, 1, new Position(dto.col, dto.row));
        } catch (Exception e) {
            Log.error("Snapshot", "Unknown plant id " + dto.plantId, e);
            return null;
        }
    }

    private void applyZombies(MatchSnapshot snapshot) {
        Set<Integer> seen = new HashSet<>();
        for (MatchSnapshot.ZombieDto dto : snapshot.zombies) {
            seen.add(dto.id);
            Zombie zombie = zombies.get(dto.id);
            if (zombie == null) {
                zombie = createZombie(dto);
                if (zombie == null) continue;
                zombies.put(dto.id, zombie);
                session.getZombies().add(zombie);
            }
            zombie.setMaxHp(dto.maxHp);
            zombie.setHp(dto.hp);
            zombie.setAlive(dto.hp > 0);
            zombie.setFacingRight(dto.facingRight);
            zombie.setPosition(new Position(dto.x, dto.row));
            applyZombieState(zombie, dto);
            applyZombieArmour(zombie, dto);
        }
        zombies.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            Zombie gone = entry.getValue();
            gone.setZombieState(ZombieState.DEAD);
            session.getZombies().remove(gone);
            recentlyRemoved.add(gone);
            return true;
        });
    }

    private Zombie createZombie(MatchSnapshot.ZombieDto dto) {
        try {
            Zombie zombie = ZombieFactory.create(dto.alias, dto.row, (int) Math.round(dto.x));
            zombie.setPosition(new Position(dto.x, dto.row));
            return zombie;
        } catch (Exception e) {
            Log.error("Snapshot", "Unknown zombie alias " + dto.alias, e);
            return null;
        }
    }

    private void applyZombieState(Zombie zombie, MatchSnapshot.ZombieDto dto) {
        try {
            zombie.setZombieState(ZombieState.valueOf(dto.state));
        } catch (IllegalArgumentException ignored) {
            zombie.setZombieState(ZombieState.WALKING);
        }
        try {
            zombie.setStatusWithoutEffects(Zombie.Status.valueOf(dto.status));
        } catch (IllegalArgumentException ignored) {
            zombie.setStatusWithoutEffects(Zombie.Status.NORMAL);
        }
    }

    private void applyZombieArmour(Zombie zombie, MatchSnapshot.ZombieDto dto) {
        if (dto.armourType == null || dto.armourHp <= 0) {
            if (zombie.getArmour() != null) zombie.setArmour(null);
            return;
        }
        if (zombie.getArmour() == null) {
            ArmourType type = ArmourType.getByName(dto.armourType);
            if (type == null) return;
            zombie.setArmour(new ZombieArmour(type, dto.armourMaxHp));
        }
        zombie.getArmour().setHP(dto.armourHp);
    }

    private void applyItems(MatchSnapshot snapshot) {
        Set<Integer> seen = new HashSet<>();
        for (MatchSnapshot.GroundItemDto dto : snapshot.items) {
            if (!"SUN".equals(dto.type)) continue;
            seen.add(dto.id);
            GroundSun sun = suns.get(dto.id);
            if (sun == null) {
                sun = new GroundSun(new Position(dto.x, dto.y), dto.value);
                suns.put(dto.id, sun);
                session.getItems().add(sun);
            }
            sun.setPosition(new Position(dto.x, dto.y));
        }
        suns.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            entry.getValue().setAlive(false);
            session.getItems().remove(entry.getValue());
            return true;
        });
    }

    public void interpolate(MatchSnapshot from, MatchSnapshot to, float alpha) {
        if (from == null || to == null) return;
        Map<Integer, Double> previousX = new HashMap<>();
        for (MatchSnapshot.ZombieDto dto : from.zombies) {
            previousX.put(dto.id, dto.x);
        }
        for (MatchSnapshot.ZombieDto dto : to.zombies) {
            Zombie zombie = zombies.get(dto.id);
            if (zombie == null) continue;
            Double before = previousX.get(dto.id);
            double x = before == null ? dto.x : before + (dto.x - before) * alpha;
            zombie.setPosition(new Position(x, dto.row));
        }
    }

    public java.util.List<Zombie> drainRemovedZombies() {
        java.util.List<Zombie> drained = new java.util.ArrayList<>(recentlyRemoved);
        recentlyRemoved.clear();
        return drained;
    }

    public void clear() {
        plants.clear();
        zombies.clear();
        suns.clear();
        recentlyRemoved.clear();
    }
}
