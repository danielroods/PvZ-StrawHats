package net.server;

import model.collections.armour.Armour;
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
import model.projectile.Projectile;
import model.utils.GameSession;
import net.dto.MatchSnapshot;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MatchSession {

    public record Intent(Role role, String action, String target, int row, int col,
                         ClientSession from) { }

    private final String matchId;
    private final IZombieMatch match = new IZombieMatch();
    private final ClientSession plantsClient;
    private final ClientSession zombiesClient;
    private final String plantsUsername;
    private final String zombiesUsername;
    private final Queue<Intent> inbox = new ConcurrentLinkedQueue<>();
    private final Map<Object, Integer> entityIds = new IdentityHashMap<>();

    private boolean plantsReady;
    private boolean zombiesReady;
    private boolean started;
    private int nextEntityId = 1;
    private int tickCount;

    public MatchSession(String matchId, ClientSession plantsClient, ClientSession zombiesClient) {
        this.matchId = matchId;
        this.plantsClient = plantsClient;
        this.zombiesClient = zombiesClient;
        this.plantsUsername = plantsClient.getUsername();
        this.zombiesUsername = zombiesClient.getUsername();
    }

    public String getMatchId() {
        return matchId;
    }

    public IZombieMatch getMatch() {
        return match;
    }

    public ClientSession clientFor(Role role) {
        return role == Role.PLANTS ? plantsClient : zombiesClient;
    }

    public String usernameFor(Role role) {
        return role == Role.PLANTS ? plantsUsername : zombiesUsername;
    }

    public Role roleOf(ClientSession session) {
        if (session == plantsClient) return Role.PLANTS;
        if (session == zombiesClient) return Role.ZOMBIES;
        return null;
    }

    public boolean involves(ClientSession session) {
        return session == plantsClient || session == zombiesClient;
    }

    public void markReady(Role role) {
        if (role == Role.PLANTS) plantsReady = true;
        if (role == Role.ZOMBIES) zombiesReady = true;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean canStart() {
        return plantsReady && zombiesReady;
    }

    public void markStarted() {
        started = true;
    }

    public void submit(Intent intent) {
        inbox.offer(intent);
    }

    public Queue<Intent> inbox() {
        return inbox;
    }

    public int tickCount() {
        return tickCount;
    }

    public void countTick() {
        tickCount++;
    }

    private int idOf(Object entity) {
        Integer id = entityIds.get(entity);
        if (id == null) {
            id = nextEntityId++;
            entityIds.put(entity, id);
        }
        return id;
    }

    public void forgetDeadEntities() {
        GameSession session = match.getSession();
        entityIds.keySet().removeIf(entity -> {
            if (entity instanceof Plant plant) return !session.getPlants().contains(plant);
            if (entity instanceof Zombie zombie) return !session.getZombies().contains(zombie);
            if (entity instanceof Projectile projectile) {
                return !session.getProjectiles().contains(projectile);
            }
            if (entity instanceof GroundItem item) return !session.getItems().contains(item);
            return false;
        });
    }

    public MatchSnapshot snapshot() {
        GameSession session = match.getSession();
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.tick = tickCount;
        snapshot.clock = match.getElapsedSeconds();
        snapshot.remaining = match.getRemainingSeconds();
        snapshot.zombieSun = match.getZombieSun();
        snapshot.plantSun = match.getPlantSun();
        snapshot.plantFood = session.getPlantFoodCount();
        snapshot.brainsEaten = match.getBrainsEaten();

        Brain[] brains = match.getBrains();
        snapshot.brains = new int[brains.length];
        for (int i = 0; i < brains.length; i++) {
            snapshot.brains[i] = brains[i] == null || brains[i].isEaten() ? 0 : brains[i].getHP();
        }

        for (ZombiePacket packet : match.getRoster()) {
            MatchSnapshot.PacketDto dto = new MatchSnapshot.PacketDto();
            dto.alias = packet.getAlias();
            dto.label = packet.getDisplayName();
            dto.cost = packet.getCost();
            dto.cooldown = packet.getCooldown();
            dto.recharge = packet.getRecharge();
            snapshot.packets.add(dto);
        }

        for (SeedCard card : match.getSeeds()) {
            MatchSnapshot.SeedDto dto = new MatchSnapshot.SeedDto();
            dto.plantId = card.plantId();
            dto.name = card.name();
            dto.cost = card.cost();
            dto.cooldown = session.getPlantCooldown(card.plantId());
            dto.recharge = card.recharge();
            snapshot.seeds.add(dto);
        }

        for (Plant plant : new ArrayList<>(session.getPlants())) {
            if (!plant.isAlive() || plant.getPosition() == null) continue;
            MatchSnapshot.PlantDto dto = new MatchSnapshot.PlantDto();
            dto.id = idOf(plant);
            dto.name = plant.getName();
            dto.plantId = plant.getId();
            dto.col = (int) Math.round(plant.getPosition().x());
            dto.row = (int) Math.round(plant.getPosition().y());
            dto.hp = plant.getHP();
            dto.maxHp = plant.getMaxHp();
            dto.plantFoodActive = plant.isPlantFoodActive();
            snapshot.plants.add(dto);
        }

        for (Zombie zombie : new ArrayList<>(session.getZombies())) {
            if (!zombie.isAlive() || zombie.getPosition() == null) continue;
            snapshot.zombies.add(zombieDto(zombie));
        }

        for (Projectile projectile : new ArrayList<>(session.getProjectiles())) {
            if (!projectile.isAlive() || projectile.getPosition() == null) continue;
            if (!projectile.isVisible()) continue;
            MatchSnapshot.ProjectileDto dto = new MatchSnapshot.ProjectileDto();
            dto.id = idOf(projectile);
            dto.type = projectile.getDisplayPath();
            dto.x = projectile.getPosition().x();
            dto.y = projectile.getPosition().y();
            snapshot.projectiles.add(dto);
        }

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

    public List<Role> roles() {
        return List.of(Role.PLANTS, Role.ZOMBIES);
    }
}
