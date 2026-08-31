package net.dto;

import java.util.ArrayList;
import java.util.List;

public class MatchSnapshot {

    public int tick;
    public double clock;
    public double remaining;
    public double matchSeconds;
    public int zombieSun;
    public int plantSun;
    public int plantFood;
    public int brainsEaten;
    public int[] brains;
    public List<PacketDto> packets = new ArrayList<>();
    public List<SeedDto> seeds = new ArrayList<>();
    public List<PlantDto> plants = new ArrayList<>();
    public List<PlantDto> removedPlants = new ArrayList<>();
    public List<ZombieDto> zombies = new ArrayList<>();
    public List<ProjectileDto> projectiles = new ArrayList<>();
    public List<ZombieProjectileDto> zombieProjectiles = new ArrayList<>();
    public List<ZombieProjectileDto> removedZombieProjectiles = new ArrayList<>();
    public List<ImpactDto> impacts = new ArrayList<>();
    public List<GroundItemDto> items = new ArrayList<>();

    public static class PointDto {
        public double x;
        public double y;

        public PointDto() {
        }

        public PointDto(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class PacketDto {
        public String alias;
        public String label;
        public int cost;
        public double cooldown;
        public double recharge;
    }

    public static class SeedDto {
        public int plantId;
        public String name;
        public int cost;
        public double cooldown;
        public double recharge;
    }

    public static class PlantDto {
        public int id;
        public String name;
        public int plantId;
        public int row;
        public int col;
        public int hp;
        public int maxHp;
        public boolean plantFoodActive;
        public double plantFoodTimer;
        public double intervalTimer;
        public String state;
        public String visualState;
        public double visualRemaining;
        public double visualElapsed;
        public int growthStage;
        public int stackNumber;
        public int chillLevel;
        public double lifespan;
        public double remainingLife;
        public boolean meleeFacingLeft;
        public boolean cactusUnderground;
        public boolean cactusStretching;
        public boolean potatoMineArmed;
        public boolean potatoMineDetonating;
        public boolean potatoMineEaten;
        public boolean explodeONutDetonated;
        public boolean magnetItem;
        public boolean hotPotatoMelt;
        public boolean endurianUnderAttack;
        public boolean squashActionState;
        public PointDto squashOrigin;
        public PointDto squashTarget;
        public int armourHp;
        public int armourMaxHp;
        public int armourReflect;
        public boolean armourExplodes;
    }

    public static class ZombieDto {
        public int id;
        public String alias;
        public double x;
        public int row;
        public int hp;
        public int maxHp;
        public String state;
        public String status;
        public boolean facingRight;
        public boolean glowing;
        public String armourType;
        public int armourHp;
        public int armourMaxHp;
    }

    public static class ProjectileDto {
        public int id;
        public int sourceId;
        public String sourceName;
        public int sourcePlantId;
        public String kind;
        public int assetVariant;
        public boolean plantFood;
        public String displayPath;
        public String displayState;
        public double x;
        public double y;
        public double vx;
        public double vy;

        public double[] motion;
    }

    public static class ImpactDto {
        public String plantName;
        public boolean plantFood;
        public int assetVariant;
        public double x;
        public double y;
    }

    public static class ZombieProjectileDto {
        public int id;
        public String kind;
        public String impAlias;
        public boolean facingRight;
        public boolean splatted;
        public double x;
        public double y;
    }

    public static class GroundItemDto {
        public int id;
        public String type;
        public double x;
        public double y;
        public int value;
        public boolean falling;
    }
}
