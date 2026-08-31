package net.server;

import model.collections.armour.Armour;
import model.collections.armour.PlantArmour;
import model.collections.armour.ZombieArmour;
import model.collections.item.GroundItem;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match.mini_games.izombie.Brain;
import model.match.mini_games.izombie.IZombieMatch;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.match.mini_games.izombie.IZombieMatch.SeedCard;
import model.match.mini_games.izombie.ZombiePacket;
import model.match_mechanisms.vector.Position;
import model.projectile.GrapeshotProjectile;
import model.projectile.LaneShiftMove;
import model.projectile.LobArcMove;
import model.projectile.Projectile;
import model.projectile.ProjectileImpact;
import model.projectile.zombie_projectile.BoneProjectile;
import model.projectile.zombie_projectile.GargantuarImpProjectile;
import model.projectile.zombie_projectile.OctopusProjectile;
import model.projectile.zombie_projectile.SnowballProjectile;
import model.projectile.zombie_projectile.ZombiePeaProjectile;
import model.projectile.zombie_projectile.ZombieProjectile;
import model.utils.GameSession;
import net.dto.MatchSnapshot;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class MatchSnapshotBuilder {

    private final Map<Object, Integer> entityIds = new IdentityHashMap<>();
    private final List<MatchSnapshot.PlantDto> removedPlants = new ArrayList<>();
    private final List<MatchSnapshot.ZombieProjectileDto> removedZombieShots = new ArrayList<>();
    private final List<MatchSnapshot.ImpactDto> impacts = new ArrayList<>();

    private int nextEntityId = 1;

    private int idOf(Object entity) {
        Integer id = entityIds.get(entity);
        if (id == null) {
            id = nextEntityId++;
            entityIds.put(entity, id);
        }
        return id;
    }

    public void captureRemovals(IZombieMatch match, List<Plant> plantsBefore,
                                List<ZombieProjectile> shotsBefore) {
        if (match == null) return;
        GameSession session = match.getSession();
        for (ProjectileImpact impact : session.drainProjectileImpacts()) {
            impacts.add(impactDto(impact));
        }
        if (plantsBefore != null) {
            List<Plant> stillAlive = session.getPlants();
            for (Plant plant : plantsBefore) {
                if (plant == null || plant.getPosition() == null) continue;
                if (stillAlive.contains(plant)) continue;
                removedPlants.add(plantDto(plant));
            }
        }
        if (shotsBefore != null) {
            List<ZombieProjectile> stillFlying = session.getZombieProjectiles();
            for (ZombieProjectile shot : shotsBefore) {
                if (shot == null || shot.getPosition() == null) continue;
                if (stillFlying.contains(shot)) continue;
                removedZombieShots.add(zombieProjectileDto(shot));
            }
        }
    }

    public void clearRemovals() {
        removedPlants.clear();
        removedZombieShots.clear();
        impacts.clear();
    }

    public void forgetDeadEntities(IZombieMatch match) {
        if (match == null) return;
        GameSession session = match.getSession();
        entityIds.keySet().removeIf(entity -> {
            if (entity instanceof Plant plant) return !session.getPlants().contains(plant);
            if (entity instanceof Zombie zombie) return !session.getZombies().contains(zombie);
            if (entity instanceof Projectile projectile) {
                return !session.getProjectiles().contains(projectile);
            }
            if (entity instanceof ZombieProjectile shot) {
                return !session.getZombieProjectiles().contains(shot);
            }
            if (entity instanceof GroundItem item) return !session.getItems().contains(item);
            return false;
        });
    }

    public MatchSnapshot build(IZombieMatch match, Role viewer, int tickCount) {
        if (match == null) return null;
        GameSession session = match.getSession();
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.tick = tickCount;
        snapshot.clock = match.getElapsedSeconds();
        snapshot.remaining = match.getRemainingSeconds();
        snapshot.matchSeconds = match.getMatchSeconds();
        snapshot.zombieSun = match.getZombieSun();
        snapshot.plantSun = match.getPlantSun();
        snapshot.plantFood = session.getPlantFoodCount();
        snapshot.brainsEaten = match.getBrainsEaten();

        Brain[] brains = match.getBrains();
        snapshot.brains = new int[brains.length];
        for (int i = 0; i < brains.length; i++) {
            snapshot.brains[i] = brains[i] == null || brains[i].isEaten() ? 0 : brains[i].getHP();
        }

        if (viewer != Role.PLANTS) {
            for (ZombiePacket packet : match.getRoster()) {
                MatchSnapshot.PacketDto dto = new MatchSnapshot.PacketDto();
                dto.alias = packet.getAlias();
                dto.label = packet.getDisplayName();
                dto.cost = packet.getCost();
                dto.cooldown = packet.getCooldown();
                dto.recharge = packet.getRecharge();
                snapshot.packets.add(dto);
            }
        }

        if (viewer != Role.ZOMBIES) {
            for (SeedCard card : match.getSeeds()) {
                MatchSnapshot.SeedDto dto = new MatchSnapshot.SeedDto();
                dto.plantId = card.plantId();
                dto.name = card.name();
                dto.cost = card.cost();
                dto.cooldown = session.getPlantCooldown(card.plantId());
                dto.recharge = card.recharge();
                snapshot.seeds.add(dto);
            }
        }

        for (Plant plant : new ArrayList<>(session.getPlants())) {
            if (!plant.isAlive() || plant.getPosition() == null) continue;
            snapshot.plants.add(plantDto(plant));
        }

        snapshot.removedPlants.addAll(removedPlants);

        for (Zombie zombie : new ArrayList<>(session.getZombies())) {
            if (!zombie.isAlive() || zombie.getPosition() == null) continue;
            snapshot.zombies.add(zombieDto(zombie));
        }

        for (Projectile projectile : new ArrayList<>(session.getProjectiles())) {
            if (!projectile.isAlive() || projectile.getPosition() == null) continue;
            if (!projectile.isVisible()) continue;
            snapshot.projectiles.add(projectileDto(projectile));
        }

        for (ZombieProjectile shot : new ArrayList<>(session.getZombieProjectiles())) {
            if (!shot.isAlive() || shot.getPosition() == null) continue;
            snapshot.zombieProjectiles.add(zombieProjectileDto(shot));
        }

        snapshot.removedZombieProjectiles.addAll(removedZombieShots);
        snapshot.impacts.addAll(impacts);

        for (Object raw : new ArrayList<>(session.getItems())) {
            if (!(raw instanceof GroundItem item)) continue;
            if (!item.isAlive() || item.isCollected() || item.getPosition() == null) continue;
            MatchSnapshot.GroundItemDto dto = new MatchSnapshot.GroundItemDto();
            dto.id = idOf(item);
            dto.type = item.getItemType().name();
            dto.x = item.getPosition().x();
            dto.y = item.getPosition().y();
            dto.value = item instanceof GroundSun sun ? sun.getSunValue() : 0;
            dto.falling = item instanceof GroundSun sun2 && sun2.isFalling();
            snapshot.items.add(dto);
        }

        return snapshot;
    }

    private MatchSnapshot.PlantDto plantDto(Plant plant) {
        MatchSnapshot.PlantDto dto = new MatchSnapshot.PlantDto();
        dto.id = idOf(plant);
        dto.name = plant.getName();
        dto.plantId = plant.getId();
        dto.col = (int) Math.round(plant.getPosition().x());
        dto.row = (int) Math.round(plant.getPosition().y());
        dto.hp = plant.getHP();
        dto.maxHp = plant.getMaxHp();
        dto.plantFoodActive = plant.isPlantFoodActive();
        dto.plantFoodTimer = finite(plant.getPlantFoodTimer());
        dto.intervalTimer = finite(plant.getIntervalTimer());
        dto.state = plant.getPlantState() == null ? null : plant.getPlantState().name();
        dto.visualState = plant.getVisualAnimationState();
        dto.visualRemaining = finite(plant.getVisualAnimationRemaining());
        dto.visualElapsed = finite(plant.getVisualAnimationElapsed());
        dto.growthStage = plant.getGrowthStage();
        dto.stackNumber = plant.getStackNumber();
        dto.chillLevel = plant.getChillLevel();
        dto.lifespan = finite(plant.getLifespanSeconds());
        dto.remainingLife = finite(plant.getRemainingLifeSeconds());
        dto.meleeFacingLeft = plant.isMeleeFacingLeft();
        dto.cactusUnderground = plant.isCactusUnderground();
        dto.cactusStretching = plant.isCactusStretching();
        dto.potatoMineArmed = plant.isPotatoMineArmed();
        dto.potatoMineDetonating = plant.isPotatoMineDetonationPending();
        dto.potatoMineEaten = plant.wasPotatoMineEatenByZombie();
        dto.explodeONutDetonated = plant.isExplodeONutDetonated();
        dto.magnetItem = plant.isMagnetItemVisible();
        dto.hotPotatoMelt = plant.isHotPotatoMeltEffectPending();
        dto.endurianUnderAttack = plant.isEndurianUnderAttack();
        dto.squashActionState = plant.isSquashActionState();
        dto.squashOrigin = point(plant.getSquashVisualOrigin());
        dto.squashTarget = point(plant.getSquashVisualTarget());
        PlantArmour armour = plant.getArmor();
        if (armour != null && armour.getHP() > 0) {
            dto.armourHp = armour.getHP();
            dto.armourMaxHp = armour.getMaxHP();
            dto.armourExplodes = armour.isExplodeOnBreak();
        }
        return dto;
    }

    private MatchSnapshot.ProjectileDto projectileDto(Projectile projectile) {
        MatchSnapshot.ProjectileDto dto = new MatchSnapshot.ProjectileDto();
        dto.id = idOf(projectile);
        Plant source = projectile.getSourcePlant();
        if (source != null) {
            dto.sourceId = idOf(source);
            dto.sourceName = source.getName();
            dto.sourcePlantId = source.getId();
            dto.plantFood = source.isPlantFoodActive();
        }
        if (source == null) {
            dto.sourceName = projectile.getSourcePlantName();
            dto.plantFood = projectile.isPlantFoodShot();
        }
        dto.kind = kindOf(projectile);
        dto.motion = motionOf(projectile);
        dto.assetVariant = projectile.getAssetVariant();
        dto.displayPath = projectile.getDisplayPath();
        dto.displayState = projectile.getDisplayState();
        Position position = projectile.getPosition();
        dto.x = finite(position.x());
        dto.y = finite(position.y());
        Position speed = projectile.getSpeed();
        dto.vx = speed == null ? 0 : finite(speed.x());
        dto.vy = speed == null ? 0 : finite(speed.y());
        return dto;
    }

    private String kindOf(Projectile projectile) {
        if (projectile instanceof GrapeshotProjectile) return "GRAPESHOT";
        if (projectile.getMoveStrategy() instanceof LobArcMove) return "LOB";
        if (projectile.getMoveStrategy() instanceof LaneShiftMove) return "LANE";
        return "STRAIGHT";
    }

    private double[] motionOf(Projectile projectile) {
        if (projectile.getMoveStrategy() instanceof LobArcMove arc) {
            return new double[] {finite(arc.getStartX()), finite(arc.getStartY()),
                    finite(arc.getEndX()), finite(arc.getEndY()), finite(arc.getPeakHeight()),
                    finite(arc.getHorizontalSpeed()), finite(arc.getTravelledX())};
        }
        if (projectile.getMoveStrategy() instanceof LaneShiftMove lane) {
            return new double[] {finite(lane.getTargetY()), finite(lane.getForwardSpeed())};
        }
        return null;
    }

    private MatchSnapshot.ImpactDto impactDto(ProjectileImpact impact) {
        MatchSnapshot.ImpactDto dto = new MatchSnapshot.ImpactDto();
        dto.plantName = impact.plantName();
        dto.plantFood = impact.plantFood();
        dto.assetVariant = impact.assetVariant();
        dto.x = finite(impact.position().x());
        dto.y = finite(impact.position().y());
        return dto;
    }

    private MatchSnapshot.ZombieProjectileDto zombieProjectileDto(ZombieProjectile shot) {
        MatchSnapshot.ZombieProjectileDto dto = new MatchSnapshot.ZombieProjectileDto();
        dto.id = idOf(shot);
        Position position = shot.getPosition();
        dto.x = finite(position.x());
        dto.y = finite(position.y());
        if (shot instanceof GargantuarImpProjectile imp) {
            dto.kind = "IMP";
            dto.impAlias = imp.getImpAlias();
            dto.facingRight = imp.isFacingRight();
        } else if (shot instanceof ZombiePeaProjectile pea) {
            dto.kind = "PEA";
            dto.splatted = pea.hasSplatted();
        } else if (shot instanceof OctopusProjectile) {
            dto.kind = "OCTOPUS";
        } else if (shot instanceof SnowballProjectile) {
            dto.kind = "SNOWBALL";
        } else if (shot instanceof BoneProjectile) {
            dto.kind = "BONE";
        } else {
            dto.kind = "OTHER";
        }
        return dto;
    }

    private MatchSnapshot.ZombieDto zombieDto(Zombie zombie) {
        MatchSnapshot.ZombieDto dto = new MatchSnapshot.ZombieDto();
        dto.id = idOf(zombie);
        dto.alias = zombie.getAlias();
        dto.x = zombie.getPosition().x();
        dto.row = (int) Math.round(zombie.getPosition().y());
        dto.hp = zombie.getHp();
        dto.maxHp = zombie.getMaxHp();
        dto.state = zombie.getZombieState() == null ? "WALKING" : zombie.getZombieState().name();
        dto.status = zombie.getStatus() == null ? "NORMAL" : zombie.getStatus().name();
        dto.facingRight = zombie.isFacingRight();
        dto.glowing = zombie.isGlowing();
        Armour armour = zombie.getArmour();
        if (armour instanceof ZombieArmour zombieArmour && armour.getHP() > 0) {
            dto.armourType = zombieArmour.getArmorType().getName();
            dto.armourHp = armour.getHP();
            dto.armourMaxHp = armour.getMaxHP();
        }
        return dto;
    }

    private static MatchSnapshot.PointDto point(Position position) {
        return position == null ? null
                : new MatchSnapshot.PointDto(position.x(), position.y());
    }

    private static double finite(double value) {
        if (Double.isNaN(value)) return 0.0;
        return Math.max(-1.0e9, Math.min(1.0e9, value));
    }
}
