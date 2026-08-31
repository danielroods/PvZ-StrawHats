package net.client;

import model.collections.Item;
import model.collections.armour.ArmourType;
import model.collections.armour.PlantArmour;
import model.collections.armour.ZombieArmour;
import model.collections.item.GroundItem;
import model.collections.item.GroundPlantFood;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.collections.zombie.ZombieState;
import model.match_mechanisms.vector.Position;
import model.projectile.GrapeshotProjectile;
import model.projectile.LaneShiftMove;
import model.projectile.LobArcMove;
import model.projectile.MoveStrategy;
import model.projectile.Projectile;
import model.projectile.ProjectileImpact;
import model.projectile.StraightMove;
import model.projectile.zombie_projectile.BoneProjectile;
import model.projectile.zombie_projectile.GargantuarImpProjectile;
import model.projectile.zombie_projectile.OctopusProjectile;
import model.projectile.zombie_projectile.SnowballProjectile;
import model.projectile.zombie_projectile.ZombiePeaProjectile;
import model.projectile.zombie_projectile.ZombieProjectile;
import model.utils.GameSession;
import net.dto.MatchSnapshot;
import service.GameClock;
import service.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SnapshotApplier {

    private static final double PLANT_FOOD_DISPLAY_SECONDS = 1.0;
    private static final double MIN_ACTIVE_PLANT_FOOD_SECONDS = 0.001;
    private static final double ENDURIAN_ATTACK_DISPLAY_SECONDS = 0.3;
    private static final double MIN_PROJECTILE_SNAP_DISTANCE = 0.5;
    private static final double PROJECTILE_SNAP_TICKS = 1.5;
    private static final double PROJECTILE_CORRECTION = 0.35;
    private static final double ZOMBIE_SHOT_FLIGHT_SECONDS = 9999.0;

    private final GameSession session;
    private final Map<Integer, Plant> plants = new HashMap<>();
    private final Map<Integer, Zombie> zombies = new HashMap<>();
    private final Map<Integer, GroundItem> groundItems = new HashMap<>();
    private final Map<Integer, Projectile> projectiles = new HashMap<>();
    private final Map<Integer, ZombieProjectile> zombieShots = new HashMap<>();
    private final Map<String, Plant> detachedSources = new HashMap<>();
    private final Map<Integer, Boolean> hotPotatoMeltSeen = new HashMap<>();
    private final List<Zombie> recentlyRemoved = new ArrayList<>();
    private final List<Plant> recentlyRemovedPlants = new ArrayList<>();

    public SnapshotApplier(GameSession session) {
        this.session = session;
    }

    public void apply(MatchSnapshot snapshot) {
        if (snapshot == null) return;
        applyPlants(snapshot);
        applyZombies(snapshot);
        applyProjectiles(snapshot);
        applyZombieProjectiles(snapshot);
        applyItems(snapshot);
        applyImpacts(snapshot);
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
            applyPlantState(plant, dto);
        }

        if (snapshot.removedPlants != null) {
            for (MatchSnapshot.PlantDto dto : snapshot.removedPlants) {
                if (seen.contains(dto.id)) continue;
                Plant plant = plants.remove(dto.id);
                if (plant == null) {
                    plant = createPlant(dto);
                    if (plant == null) continue;
                } else {
                    detachPlant(plant);
                }
                applyPlantState(plant, dto);
                plant.setAlive(false);
                hotPotatoMeltSeen.remove(dto.id);
                recentlyRemovedPlants.add(plant);
            }
        }

        plants.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            Plant plant = entry.getValue();
            detachPlant(plant);
            plant.setAlive(false);
            hotPotatoMeltSeen.remove(entry.getKey());
            recentlyRemovedPlants.add(plant);
            return true;
        });
    }

    private void detachPlant(Plant plant) {
        session.getPlants().remove(plant);
        if (plant.getPosition() == null) return;
        int row = (int) Math.round(plant.getPosition().y());
        int col = (int) Math.round(plant.getPosition().x());
        var cell = session.getEnvironment().getCell(row, col);
        if (cell != null && cell.getPlant() == plant) cell.setPlant(null);
    }

    private Plant createPlant(MatchSnapshot.PlantDto dto) {
        try {
            return PlantFactory.createPlant(dto.plantId, 1, new Position(dto.col, dto.row));
        } catch (Exception e) {
            Log.error("Snapshot", "Unknown plant id " + dto.plantId, e);
            return null;
        }
    }

    private void applyPlantState(Plant plant, MatchSnapshot.PlantDto dto) {
        if (dto.maxHp > 0 && plant.getMaxHp() != dto.maxHp) plant.setMaxHp(dto.maxHp);
        plant.setHP(dto.hp);
        plant.setAlive(dto.hp > 0);
        plant.setInternalTimer(dto.intervalTimer);
        plant.setPlantFoodTimer(dto.plantFoodActive
                ? Math.max(dto.plantFoodTimer, MIN_ACTIVE_PLANT_FOOD_SECONDS) : 0.0);
        plant.setState(plantState(dto.state));
        plant.setVisualAnimationProgress(dto.visualState, dto.visualRemaining, dto.visualElapsed);
        plant.setGrowthStage(Math.max(1, dto.growthStage));
        plant.setStackNumber(Math.max(1, dto.stackNumber));
        plant.setChillLevel(dto.chillLevel);
        if (dto.lifespan > 0 && plant.getLifespanSeconds() != dto.lifespan) {
            plant.setLifespanSeconds(dto.lifespan);
        }
        plant.setRemainingLifeSeconds(dto.remainingLife);
        plant.setMeleeFacingLeft(dto.meleeFacingLeft);
        plant.setCactusPosture(dto.cactusUnderground, dto.cactusStretching);
        plant.setPotatoMineArmed(dto.potatoMineArmed);
        plant.setPotatoMineDetonationPending(dto.potatoMineDetonating);
        plant.setPotatoMineEatenByZombie(dto.potatoMineEaten);
        plant.setExplodeONutDetonated(dto.explodeONutDetonated);
        plant.setMagnetItemVisible(dto.magnetItem);
        plant.setEndurianAttackVisualTimer(
                dto.endurianUnderAttack ? ENDURIAN_ATTACK_DISPLAY_SECONDS : 0.0);
        applySquashPath(plant, dto);
        applyPlantArmour(plant, dto);
        applyHotPotatoMelt(plant, dto);
    }

    private Plant.PlantState plantState(String name) {
        if (name == null) return Plant.PlantState.ACTIVE;
        try {
            return Plant.PlantState.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return Plant.PlantState.ACTIVE;
        }
    }

    private void applySquashPath(Plant plant, MatchSnapshot.PlantDto dto) {
        plant.setSquashActionState(dto.squashActionState);
        Position origin = position(dto.squashOrigin);
        Position target = position(dto.squashTarget);
        if (origin == null || target == null) {
            if (plant.getSquashVisualOrigin() != null || plant.getSquashVisualTarget() != null) {
                plant.clearSquashVisualPath();
            }
            return;
        }
        if (samePosition(plant.getSquashVisualOrigin(), origin)
                && samePosition(plant.getSquashVisualTarget(), target)) {
            return;
        }
        plant.setSquashVisualPath(origin, target);
    }

    private static boolean samePosition(Position a, Position b) {
        if (a == null || b == null) return a == b;
        return Math.abs(a.x() - b.x()) < 0.0001 && Math.abs(a.y() - b.y()) < 0.0001;
    }

    private static Position position(MatchSnapshot.PointDto point) {
        return point == null ? null : new Position(point.x, point.y);
    }

    private void applyPlantArmour(Plant plant, MatchSnapshot.PlantDto dto) {
        if (dto.armourHp <= 0) {
            if (plant.getArmor() != null) plant.setArmor(null);
            return;
        }
        PlantArmour armour = plant.getArmor();
        if (armour == null || armour.getMaxHP() != dto.armourMaxHp) {
            armour = new PlantArmour(Math.max(1, dto.armourMaxHp), 0, dto.armourExplodes);
            plant.setArmor(armour);
        }
        armour.setHP(dto.armourHp);
    }

    private void applyHotPotatoMelt(Plant plant, MatchSnapshot.PlantDto dto) {
        boolean previous = Boolean.TRUE.equals(hotPotatoMeltSeen.put(dto.id, dto.hotPotatoMelt));
        if (dto.hotPotatoMelt && !previous) plant.setHotPotatoMeltEffectPending(true);
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

    private void applyProjectiles(MatchSnapshot snapshot) {
        Set<Integer> seen = new HashSet<>();
        for (MatchSnapshot.ProjectileDto dto : snapshot.projectiles) {
            seen.add(dto.id);
            Projectile projectile = projectiles.get(dto.id);
            if (projectile == null) {
                projectile = createProjectile(dto);
                projectiles.put(dto.id, projectile);
                session.getProjectiles().add(projectile);
            } else {
                reconcile(projectile, dto);
            }
            projectile.setSourcePlant(resolveSource(dto));
            projectile.setSourceDisplay(dto.sourceName, dto.plantFood);
            projectile.setAssetVariant(dto.assetVariant);
            if (dto.displayPath != null) projectile.setDisplay(dto.displayPath, dto.displayState);
        }
        projectiles.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            Projectile gone = entry.getValue();
            gone.setAlive(false);
            session.getProjectiles().remove(gone);
            return true;
        });
    }

    private void reconcile(Projectile projectile, MatchSnapshot.ProjectileDto dto) {
        if (projectile.getMoveStrategy() instanceof LobArcMove arc
                && dto.motion != null && dto.motion.length >= 7) {
            double local = arc.getTravelledX();
            double authoritative = dto.motion[6];
            arc.setTravelledX(local + (authoritative - local) * PROJECTILE_CORRECTION);
            arc.applyTo(projectile);
            projectile.setSpeed(new Position(dto.vx, dto.vy));
            return;
        }

        projectile.setPosition(correctedPosition(projectile.getPosition(), dto));
        projectile.setSpeed(new Position(dto.vx, dto.vy));
    }

    private Position correctedPosition(Position rendered, MatchSnapshot.ProjectileDto dto) {
        if (rendered == null) return new Position(dto.x, dto.y);
        double dx = dto.x - rendered.x();
        double dy = dto.y - rendered.y();
        double tolerance = snapToleranceFor(dto);
        if (Math.abs(dx) > tolerance || Math.abs(dy) > tolerance) {
            return new Position(dto.x, dto.y);
        }
        return new Position(rendered.x() + dx * PROJECTILE_CORRECTION,
                rendered.y() + dy * PROJECTILE_CORRECTION);
    }

    private double snapToleranceFor(MatchSnapshot.ProjectileDto dto) {
        double speed = Math.hypot(dto.vx, dto.vy);
        return Math.max(MIN_PROJECTILE_SNAP_DISTANCE,
                speed * GameClock.SECONDS_PER_TICK * PROJECTILE_SNAP_TICKS);
    }

    private Projectile createProjectile(MatchSnapshot.ProjectileDto dto) {
        Position at = new Position(dto.x, dto.y);
        Position velocity = new Position(dto.vx, dto.vy);
        Projectile projectile = "GRAPESHOT".equals(dto.kind)
                ? new GrapeshotProjectile(null, at, velocity, 0, 1, 1.0)
                : new Projectile((Item) null, at, velocity, 0, rebuildMotion(dto), null);
        projectile.setSpawnDelaySeconds(0);
        projectile.setPosition(at);
        projectile.setSpeed(velocity);
        return projectile;
    }

    private MoveStrategy rebuildMotion(MatchSnapshot.ProjectileDto dto) {
        String kind = dto.kind == null ? "" : dto.kind;
        if ("LOB".equals(kind) && dto.motion != null && dto.motion.length >= 7) {
            LobArcMove arc = new LobArcMove(dto.motion[0], dto.motion[1], dto.motion[2],
                    dto.motion[3], dto.motion[4], dto.motion[5]);
            arc.setTravelledX(dto.motion[6]);
            return arc;
        }
        if ("LANE".equals(kind) && dto.motion != null && dto.motion.length >= 2) {
            return new LaneShiftMove(dto.motion[0], dto.motion[1]);
        }
        return new StraightMove();
    }

    private Plant resolveSource(MatchSnapshot.ProjectileDto dto) {
        if (dto.sourceId > 0) {
            Plant live = plants.get(dto.sourceId);
            if (live != null) return live;
        }
        if (dto.sourcePlantId <= 0) return null;
        String key = dto.sourcePlantId + (dto.plantFood ? ":pf" : ":normal");
        Plant detached = detachedSources.get(key);
        if (detached == null) {
            try {
                detached = PlantFactory.createPlant(dto.sourcePlantId, 1, new Position(0, 0));
            } catch (Exception e) {
                return null;
            }
            if (detached == null) return null;
            detached.setPlantFoodTimer(dto.plantFood ? PLANT_FOOD_DISPLAY_SECONDS : 0.0);
            detachedSources.put(key, detached);
        }
        return detached;
    }

    private void applyImpacts(MatchSnapshot snapshot) {
        if (snapshot.impacts == null) return;
        for (MatchSnapshot.ImpactDto dto : snapshot.impacts) {
            if (dto.plantName == null) continue;
            session.recordProjectileImpact(new ProjectileImpact(dto.plantName, dto.plantFood,
                    dto.assetVariant, new Position(dto.x, dto.y)));
        }
    }

    private void applyZombieProjectiles(MatchSnapshot snapshot) {
        if (snapshot.removedZombieProjectiles != null) {
            for (MatchSnapshot.ZombieProjectileDto dto : snapshot.removedZombieProjectiles) {
                ZombieProjectile gone = zombieShots.get(dto.id);
                if (gone instanceof ZombiePeaProjectile pea) pea.setSplatted(dto.splatted);
            }
        }

        Set<Integer> seen = new HashSet<>();
        for (MatchSnapshot.ZombieProjectileDto dto : snapshot.zombieProjectiles) {
            seen.add(dto.id);
            ZombieProjectile shot = zombieShots.get(dto.id);
            if (shot == null) {
                shot = createZombieProjectile(dto);
                if (shot == null) continue;
                zombieShots.put(dto.id, shot);
                session.getZombieProjectiles().add(shot);
            }
            shot.setPosition(new Position(dto.x, dto.y));
        }
        zombieShots.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            ZombieProjectile gone = entry.getValue();
            gone.setAlive(false);
            session.getZombieProjectiles().remove(gone);
            return true;
        });
    }

    private ZombieProjectile createZombieProjectile(MatchSnapshot.ZombieProjectileDto dto) {
        Position at = new Position(dto.x, dto.y);
        try {
            return switch (dto.kind == null ? "" : dto.kind) {
                case "IMP" -> new GargantuarImpProjectile(at, at, ZOMBIE_SHOT_FLIGHT_SECONDS, 0,
                        (int) Math.round(dto.y), dto.impAlias, dto.facingRight, session);
                case "PEA" -> new ZombiePeaProjectile(at, 0, session);
                case "OCTOPUS" -> new OctopusProjectile(at, at, ZOMBIE_SHOT_FLIGHT_SECONDS, session);
                case "SNOWBALL" -> new SnowballProjectile(at, at, ZOMBIE_SHOT_FLIGHT_SECONDS, session);
                case "BONE" -> new BoneProjectile(at, at, ZOMBIE_SHOT_FLIGHT_SECONDS, session);
                default -> null;
            };
        } catch (Exception e) {
            Log.error("Snapshot", "Unknown zombie projectile " + dto.kind, e);
            return null;
        }
    }

    private void applyItems(MatchSnapshot snapshot) {
        Set<Integer> seen = new HashSet<>();
        for (MatchSnapshot.GroundItemDto dto : snapshot.items) {
            GroundItem item = groundItems.get(dto.id);
            if (item == null) {
                item = createGroundItem(dto);
                if (item == null) continue;
                groundItems.put(dto.id, item);
                session.getItems().add(item);
            }
            seen.add(dto.id);
            item.setPosition(new Position(dto.x, dto.y));
        }
        groundItems.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            entry.getValue().setAlive(false);
            session.getItems().remove(entry.getValue());
            return true;
        });
    }

    private GroundItem createGroundItem(MatchSnapshot.GroundItemDto dto) {
        Position at = new Position(dto.x, dto.y);
        if ("SUN".equals(dto.type)) return new GroundSun(at, dto.value);
        if ("PLANT_FOOD".equals(dto.type)) return new GroundPlantFood(at);
        return null;
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

    public void advanceProjectiles(float delta) {
        if (delta <= 0f) return;
        for (Projectile projectile : projectiles.values()) {
            projectile.advanceVisual(delta);
        }
    }
    public void advancePlantVisuals(float delta) {
        if (delta <= 0f) return;
        for (Plant plant : plants.values()) {
            if (plant == null || !plant.isAlive()) continue;
            double remaining = plant.getVisualAnimationRemaining();
            if (remaining <= 0) continue;
            plant.setVisualAnimationProgress(plant.getVisualAnimationState(),
                    Math.max(0.0, remaining - delta),
                    plant.getVisualAnimationElapsed() + delta);
        }
    }

    public List<Zombie> drainRemovedZombies() {
        List<Zombie> drained = new ArrayList<>(recentlyRemoved);
        recentlyRemoved.clear();
        return drained;
    }

    public List<Plant> drainRemovedPlants() {
        List<Plant> drained = new ArrayList<>(recentlyRemovedPlants);
        recentlyRemovedPlants.clear();
        return drained;
    }

    public void clear() {
        plants.clear();
        zombies.clear();
        groundItems.clear();
        projectiles.clear();
        zombieShots.clear();
        detachedSources.clear();
        hotPotatoMeltSeen.clear();
        recentlyRemoved.clear();
        recentlyRemovedPlants.clear();
    }
}
