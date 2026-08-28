package net.dto;

import java.util.ArrayList;
import java.util.List;

public class MatchSnapshot {

    public int tick;
    public double clock;
    public double remaining;
    public int zombieSun;
    public int plantSun;
    public int plantFood;
    public int brainsEaten;
    public int[] brains;
    public List<PacketDto> packets = new ArrayList<>();
    public List<SeedDto> seeds = new ArrayList<>();
    public List<PlantDto> plants = new ArrayList<>();
    public List<ZombieDto> zombies = new ArrayList<>();
    public List<ProjectileDto> projectiles = new ArrayList<>();
    public List<GroundItemDto> items = new ArrayList<>();

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
        public String type;
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
