package model.match.mini_games.izombie;

public class ZombiePacket {

    private final String alias;
    private final String displayName;
    private final int cost;
    private final double recharge;

    private double cooldown;

    public ZombiePacket(String alias, String displayName, int cost, double recharge) {
        this.alias = alias;
        this.displayName = displayName;
        this.cost = cost;
        this.recharge = recharge;
    }

    public String getAlias() { return alias; }

    public String getDisplayName() { return displayName; }

    public int getCost() { return cost; }

    public double getRecharge() { return recharge; }

    public double getCooldown() { return cooldown; }

    public boolean isReady() { return cooldown <= 1e-6; }

    public void startCooldown() { cooldown = recharge; }

    public void tick(double deltaSeconds) {
        if (cooldown > 0) cooldown = Math.max(0, cooldown - deltaSeconds);
    }

    public double getCooldownProgress() {
        if (recharge <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, 1.0 - cooldown / recharge));
    }
}
