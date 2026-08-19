package model.collections.plant;

import model.collections.Item;
import model.collections.armour.PlantArmour;
import model.collections.plant.actstrategy.ActStrategy;
import model.collections.zombie.Zombie;
import model.match_mechanisms.Attack;
import model.match_mechanisms.Pluck;
import model.match_mechanisms.vector.Position;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.utils.GameSession;
import service.GameClock;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Plant extends Item implements Pluck, Attack {
    private int id;
    private String name;
    private int level = 1;
    private int recharge;
    private double actionInterval;
    private int cost;
    private final ArrayList<PlantTag> tags = new ArrayList<>();
    private int damage;
    private PlantType type;
    private AbilityType abilityType;
    private Plant bottom;
    private int stackNumber = 1;
    private int maxStackNumber = 1;

    private final ModifiableStat hpStat;
    private ModifiableStat actionIntervalStat;
    private ActStrategy actStrategy;
    private PlantFoodEffect plantFoodEffect;
    private PlantFoodType plantFoodType;

    private double internalTimer = 0.0;
    private double abilityValue;
    private double attackRange;
    private double lifespanSeconds;
    private double remainingLifeSeconds;
    private int chillLevel = 0;
    private GrowthTracker growthTracker;
    private double plantFoodTimer = 0.0;

    private PlantArmour armor;

    public enum PlantState {
        ACTIVE,
        INCAPACITATED,
        PREPPING,
        DYING
    }
    private PlantState state = PlantState.ACTIVE;
    private final List<String> rawUpgrades = new ArrayList<>();
    private final Map<String, Double> specialUpgrades = new HashMap<>();
    private List<Position> shootingVectors = new ArrayList<>();

    public Plant(String name, Position position, int HP) {
        super(position, HP);
        this.name = name;
        this.hpStat = new ModifiableStat(HP);
    }

    public double getIntervalTimer() { return this.internalTimer; }
    public void setInternalTimer(double internalTimer) { this.internalTimer = internalTimer; }
    public int getMaxHp() { return (int) hpStat.getValue(); }

    public Position getPosition() {
        return new Position(getLocation().x(), getLocation().y());
    }

    public Position getLocation() {
        return super.getPosition();
    }

    @Override
    public void tick() {
    }

    public void tick(double deltaTimeSeconds, GameSession session) {
        if (state == PlantState.INCAPACITATED) return;

        if (lifespanSeconds > 0) {
            remainingLifeSeconds = GameClock.countDown(remainingLifeSeconds, deltaTimeSeconds);
            if (GameClock.isZero(remainingLifeSeconds)) {
                setState(PlantState.DYING);
                setAlive(false);
                return;
            }
        }

        if (hpStat != null) hpStat.update((float) deltaTimeSeconds);
        if (actionIntervalStat != null) actionIntervalStat.update((float) deltaTimeSeconds);
        if (growthTracker != null) growthTracker.update(deltaTimeSeconds);

        if (plantFoodTimer > 0) {
            if (plantFoodEffect != null) {
                plantFoodEffect.tickDurationEffect(this, deltaTimeSeconds);
            }
            plantFoodTimer = GameClock.countDown(plantFoodTimer, deltaTimeSeconds);
            if (plantFoodEffect == null || plantFoodEffect.drivesActStrategy()) return;
        }

        if (actStrategy == null) return;
        internalTimer = GameClock.countDown(internalTimer, deltaTimeSeconds);
        if (state == PlantState.PREPPING && GameClock.isZero(internalTimer)) {
            state = PlantState.ACTIVE;
        }

        actStrategy.act(this, session);
    }

    public void takeDamage(int damageAmount, Zombie dealer) {
        if (!isAlive() || damageAmount <= 0) return;
        GameSession frostSession = GameSession.peekInstance();
        if (frostSession != null && FrostbiteFreezing.isFrozenInIce(frostSession, this)) {
            FrostbiteFreezing.damageFrozenPlantIfInIce(frostSession, this, damageAmount, false);
            return;
        }
        if (dealer != null && name.equalsIgnoreCase("Endurian") && getDamage() > 0) {
            int baseReflect = getDamage() + (int) getSpecialUpgrade("REFLECT_DAMAGE_BUFF", 0);
            int reflectDamage = isPlantFoodActive() ? baseReflect * 2 : baseReflect;
            dealer.takeDamage(reflectDamage, this);
        }
        if (name.equalsIgnoreCase("Sun Bean") && abilityValue > 0) {
            GameSession sunSession = GameSession.peekInstance();
            if (sunSession != null) sunSession.addSun((int) abilityValue);
        }
        int remainingDamage = damageAmount;

        if (this.armor != null && !this.armor.isDestroyed()) {
            remainingDamage = this.armor.absorbDamage(remainingDamage);
            this.armor.handleReflection(dealer, this);

            if (this.armor.isDestroyed()) {
                if (this.armor.isExplodeOnBreak()) {
                    executeArmorExplosion();
                }
                this.armor = null;
            }
        }

        if (remainingDamage > 0) {
            int newHp = getHP() - remainingDamage;
            if (newHp <= 0) {
                setHP(0);
                this.state = PlantState.DYING;
                if (name.equalsIgnoreCase("Explode-o-nut")) executeArmorExplosion();
                Position position = getLocation();
                if (position != null) {
                    GeneralPrinter.print("Plant " + name + " at (" + ((int) position.x() + 1)
                            + ", " + ((int) position.y() + 1) + ") is destroyed.");
                }
            } else {
                setHP(newHp);
            }
        }
    }

    private void executeArmorExplosion() {
        GameSession session = GameSession.peekInstance();
        Position center = getPosition();
        if (session == null || center == null) return;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            if (Math.abs(zombie.getPosition().x() - center.x()) <= 1
                    && Math.abs(zombie.getPosition().y() - center.y()) <= 1) {
                zombie.takeDamage(Math.max(1, getDamage()), this);
            }
        }
    }

    @Override public void dealDamage(Item target) { if (target != null) target.setHP(target.getHP() - getDamage()); }
    public boolean activatePlant(GameSession session) {
        if (this.plantFoodEffect == null || this.plantFoodTimer > 0 || session == null || !isAlive()) return false;
        this.plantFoodEffect.reset();
        if (this.growthTracker != null) this.growthTracker.skipToMaxStage();
        for (Plant sibling : session.getPlants()) {
            if (sibling != null && sibling.isAlive() && sibling.getId() == this.id
                    && sibling.getLifespanSeconds() > 0) {
                sibling.resetLifespan();
            }
        }
        this.plantFoodEffect.applyStatusModifiers(this);
        this.plantFoodEffect.triggerSuperpower(this, session);
        this.plantFoodTimer = Math.max(0.0, this.plantFoodEffect.getDurationSeconds());
        return true;
    }
    public boolean canUsePlantFood() {
        return this.plantFoodEffect != null && this.plantFoodTimer <= 0 && isAlive();
    }
    public boolean isPlantFoodActive() { return this.plantFoodTimer > 0; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getRecharge() { return recharge; }
    public void setRecharge(int recharge) { this.recharge = recharge; }
    public double getActionInterval() { return actionIntervalStat != null ? actionIntervalStat.getValue() : actionInterval; }
    public void setActionInterval(double actionInterval) {
        this.actionInterval = actionInterval;
        if (this.actionIntervalStat == null) this.actionIntervalStat = new ModifiableStat((float) actionInterval);
        else this.actionIntervalStat.setBaseValue((float) actionInterval);
    }
    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
    public int getDamage() {
        if (growthTracker != null) {
            Double staged = growthTracker.getStageValue("damage");
            if (staged != null) return staged.intValue();
        }
        return this.damage;
    }
    public void setDamage(int damage) { this.damage = damage; }
    public PlantType getType() { return type; }
    public void setType(PlantType type) { this.type = type; }
    public AbilityType getAbilityType() { return abilityType; }
    public void setAbilityType(AbilityType abilityType) { this.abilityType = abilityType; }
    public Plant getBottom() { return bottom; }
    public void setBottom(Plant bottom) { this.bottom = bottom; }
    public ArrayList<PlantTag> getTags() { return tags; }
    public int getStackNumber() { return stackNumber; }
    public int getMaxStackNumber() { return maxStackNumber; }
    public void setMaxStackNumber(int maxStackNumber) {
        this.maxStackNumber = Math.max(1, maxStackNumber);
        this.stackNumber = Math.min(this.stackNumber, this.maxStackNumber);
    }
    public boolean addStack() {
        if (stackNumber >= maxStackNumber) return false;
        stackNumber++;
        return true;
    }
    public List<String> getRawUpgrades() { return rawUpgrades; }
    public void addSpecialUpgrade(String tag, double value) {
        if (tag != null && !tag.isBlank()) specialUpgrades.merge(tag, value, Double::sum);
    }
    public boolean hasSpecialUpgrade(String tag) { return specialUpgrades.containsKey(tag); }
    public double getSpecialUpgrade(String tag, double fallback) {
        return specialUpgrades.getOrDefault(tag, fallback);
    }
    public void setActStrategy(ActStrategy actStrategy) { this.actStrategy = actStrategy; }
    public ActStrategy getActStrategy() { return this.actStrategy; }
    public PlantFoodEffect getPlantFoodEffect() { return plantFoodEffect; }
    public void setPlantFoodEffect(PlantFoodEffect plantFoodEffect) { this.plantFoodEffect = plantFoodEffect; }
    public void setPlantFoodType(PlantFoodType plantFoodType) { this.plantFoodType = plantFoodType; }
    public void setAbilityValue(double value) { this.abilityValue = value; }
    public double getAttackRange() { return attackRange; }
    public void setAttackRange(double attackRange) { this.attackRange = Math.max(0, attackRange); }
    public boolean isWithinAttackRange(Position target) {
        if (attackRange <= 0 || target == null) return true;
        Position origin = getPosition();
        return origin != null && origin.distanceTo(target) <= attackRange;
    }
    public double getLifespanSeconds() { return lifespanSeconds; }
    public void setLifespanSeconds(double lifespanSeconds) {
        this.lifespanSeconds = Math.max(0, lifespanSeconds);
        this.remainingLifeSeconds = this.lifespanSeconds;
    }
    public double getRemainingLifeSeconds() { return remainingLifeSeconds; }
    public void resetLifespan() { this.remainingLifeSeconds = this.lifespanSeconds; }
    public double getAbilityValue() {
        if (growthTracker != null) {
            Double staged = growthTracker.getStageValue("abilityValue");
            if (staged != null) return staged;
        }
        return this.abilityValue;
    }
    public void setWrampUp(List<Map<String, Object>> wrampUp) { setWrampUp(wrampUp, 0.0); }
    public void setWrampUp(List<Map<String, Object>> wrampUp, double stageTimeShift) {
        this.growthTracker = (wrampUp != null && !wrampUp.isEmpty())
                ? new GrowthTracker(wrampUp, stageTimeShift) : null;
    }
    public List<Position> getShootingVectors() { return shootingVectors; }
    public void setShootingVectors(List<Position> shootingVectors) { this.shootingVectors = shootingVectors; }
    public PlantArmour getArmor() { return armor; }
    public void setArmor(PlantArmour armor) { this.armor = armor; }
    public void setState(PlantState state) {
        this.state = state;
    }


    /// getState()/setState(ItemState) pair with an unrelated return type.
    public PlantState getPlantState() {
        return this.state;
    }

    public int getChillLevel() {
        return this.chillLevel;
    }

    public void setChillLevel(int chillLevel) {
        this.chillLevel = chillLevel;
    }
}
