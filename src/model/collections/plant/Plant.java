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

    // Melee/Chomper visual state. These fields are presentation hints only and
    // do not alter the plant's gameplay state.
    private boolean meleeFacingLeft = false;
    private String visualAnimationState;
    private double visualAnimationRemaining = 0.0;
    private double visualAnimationElapsed = 0.0;
    private boolean chomperSpecialActive = false;
    private boolean chomperSpecialPending = false;
    private boolean chomperDigestIdlePending = false;
    private int kiwibeastHitCounter = 0;

    private Position squashVisualPosition;
    private Position squashVisualOrigin;
    private Position squashVisualTarget;
    private boolean squashActionState = false;
    private boolean specialInvulnerable = false;

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

        tickVisualAnimation(deltaTimeSeconds);

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
            double previousPlantFoodTimer = plantFoodTimer;
            plantFoodTimer = GameClock.countDown(plantFoodTimer, deltaTimeSeconds);
            if (previousPlantFoodTimer > 0.0 && plantFoodTimer <= 0.0) {
                finishPlantFoodVisualState();
            }
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
        if (specialInvulnerable) return;
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
            advanceKiwibeastGrowthOnDamage();
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
        if (this.growthTracker != null && !"Kiwibeast".equalsIgnoreCase(name)) this.growthTracker.skipToMaxStage();
        for (Plant sibling : session.getPlants()) {
            if (sibling != null && sibling.isAlive() && sibling.getId() == this.id
                    && sibling.getLifespanSeconds() > 0) {
                sibling.resetLifespan();
            }
        }
        this.plantFoodEffect.applyStatusModifiers(this);
        this.plantFoodEffect.triggerSuperpower(this, session);
        this.plantFoodTimer = Math.max(0.0, this.plantFoodEffect.getDurationSeconds());

        // Puff-shroom: feeding any one of them applies the full Plant Food boost (burst +
        // "plantfood" animation), not just the lifespan reset every shroom already gets
        // above, to every other live Puff-shroom currently on the field.
        if ("Puff-shroom".equalsIgnoreCase(name)) {
            for (Plant sibling : session.getPlants()) {
                if (sibling == this || sibling == null || !sibling.isAlive()) continue;
                if (sibling.getId() != this.id || sibling.plantFoodEffect == null) continue;
                if (sibling.plantFoodTimer > 0) continue;
                sibling.plantFoodEffect.reset();
                sibling.plantFoodEffect.applyStatusModifiers(sibling);
                sibling.plantFoodEffect.triggerSuperpower(sibling, session);
                sibling.plantFoodTimer = Math.max(0.0, sibling.plantFoodEffect.getDurationSeconds());
            }
        }
        return true;
    }
    public boolean canUsePlantFood() {
        return this.plantFoodEffect != null && this.plantFoodTimer <= 0 && isAlive();
    }

    /** Internal Plant Food transfer used by Pumpkin. It intentionally bypasses UI/card checks. */
    public boolean activatePlantFoodFromPumpkin(GameSession session) {
        if (!canUsePlantFood() || session == null) return false;
        return activatePlant(session);
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

    public boolean isPumpkin() {
        return name != null && name.equalsIgnoreCase("Pumpkin");
    }

    public boolean isWallNut() {
        return name != null && name.equalsIgnoreCase("Wall-nut");
    }

    public boolean isTallNut() {
        return name != null && name.equalsIgnoreCase("Tall-nut");
    }

    public String getTallNutHealthAnimationState() {
        if (!isTallNut()) return "idle";
        double ratio = getHealthRatio();
        if (ratio > 0.60) return "idle";
        if (ratio >= 0.25) return "damage";
        return "damage2";
    }

    public int getTallNutPlantFoodArmorStage() {
        if (!isTallNut() || armor == null || armor.getHP() <= 0) return 0;
        int max = Math.max(1, armor.getMaxHP());
        double ratio = armor.getHP() / (double) max;
        if (ratio > 0.60) return 1;
        if (ratio > 0.25) return 2;
        return 3;
    }

    public String getWallNutHealthAnimationState() {
        if (!isWallNut()) return "idle";
        double ratio = getHealthRatio();
        if (ratio > 0.80) return "idle";
        if (ratio > 0.50) return "damage";
        if (ratio > 0.20) return "damage2";
        return "damage3";
    }

    public int getWallNutPlantFoodArmorStage() {
        if (!isWallNut() || armor == null || armor.getHP() <= 0) return 0;
        int max = Math.max(1, armor.getMaxHP());
        double ratio = armor.getHP() / (double) max;
        if (ratio > 0.60) return 1;
        if (ratio > 0.25) return 2;
        return 3;
    }

    public double getHealthRatio() {
        int max = Math.max(1, getMaxHp());
        return Math.max(0.0, Math.min(1.0, getHP() / (double) max));
    }

    public int getPumpkinArmorVisualStage() {
        if (!isPumpkin() || armor == null || armor.getHP() <= 0) return 0;
        int max = Math.max(1, armor.getMaxHP());
        double ratio = armor.getHP() / (double) max;
        if (ratio > 0.75) return 1;
        if (ratio > 0.50) return 2;
        if (ratio > 0.25) return 3;
        return 4;
    }
    public void setState(PlantState state) {
        this.state = state;
    }


    /// getState()/setState(ItemState) pair with an unrelated return type.
    public PlantState getPlantState() {
        return this.state;
    }

    public boolean isMeleeFacingLeft() { return meleeFacingLeft; }
    public void setMeleeFacingLeft(boolean meleeFacingLeft) { this.meleeFacingLeft = meleeFacingLeft; }

    public String getVisualAnimationState() { return visualAnimationState; }
    public double getVisualAnimationRemaining() { return visualAnimationRemaining; }
    public double getVisualAnimationElapsed() { return visualAnimationElapsed; }
    public boolean isChomperSpecialActive() { return chomperSpecialActive; }

    public Position getSquashVisualPosition() {
        return squashVisualPosition == null ? null
                : new Position(squashVisualPosition.x(), squashVisualPosition.y());
    }

    public void setSquashVisualPosition(Position position) {
        this.squashVisualPosition = position == null ? null
                : new Position(position.x(), position.y());
    }

    public Position getSquashVisualOrigin() {
        return squashVisualOrigin == null ? null
                : new Position(squashVisualOrigin.x(), squashVisualOrigin.y());
    }

    public Position getSquashVisualTarget() {
        return squashVisualTarget == null ? null
                : new Position(squashVisualTarget.x(), squashVisualTarget.y());
    }

    public void setSquashVisualPath(Position origin, Position target) {
        this.squashVisualOrigin = origin == null ? null : new Position(origin.x(), origin.y());
        this.squashVisualTarget = target == null ? null : new Position(target.x(), target.y());
        this.squashVisualPosition = origin == null ? null : new Position(origin.x(), origin.y());
    }

    public void clearSquashVisualPath() {
        this.squashVisualOrigin = null;
        this.squashVisualTarget = null;
        this.squashVisualPosition = null;
    }

    public boolean isSquashActionState() { return squashActionState; }
    public void setSquashActionState(boolean active) { this.squashActionState = active; }

    public boolean isSpecialInvulnerable() { return specialInvulnerable; }
    public void setSpecialInvulnerable(boolean value) { this.specialInvulnerable = value; }

    public void setVisualAnimationState(String state, double durationSeconds) {
        this.visualAnimationState = state;
        this.visualAnimationRemaining = Math.max(0.0, durationSeconds);
        this.visualAnimationElapsed = 0.0;
    }

    public void clearVisualAnimationState() {
        this.visualAnimationState = null;
        this.visualAnimationRemaining = 0.0;
        this.visualAnimationElapsed = 0.0;
    }

    /** Freeze the current visual clip on its final frame. Used by Squash so the
     * landing frame is actually rendered before the plant is removed. */
    public void holdVisualAnimationAtEnd() {
        this.visualAnimationElapsed += this.visualAnimationRemaining;
        this.visualAnimationRemaining = 0.0;
    }

    private void finishPlantFoodVisualState() {
        // Plant Food visual states are temporary. Once the Plant Food timer ends,
        // never leave the plant locked on an intro/loop/outro frame. Clearing the
        // explicit visual state lets PlantRenderer resume its normal idle/attack
        // selection on the very next frame.
        //
        // Chomper is the only exception: if its eating special is still active,
        // return to its looping special idle instead of the normal idle.
        if ("Chomper".equalsIgnoreCase(name) && chomperSpecialActive) {
            setVisualAnimationState("special_idle", 10.0);
        } else {
            clearVisualAnimationState();
        }
    }

    public int getGrowthStage() {
        return growthTracker == null ? 1 : growthTracker.getCurrentStage();
    }

    private void advanceKiwibeastGrowthOnDamage() {
        // Kiwibeast growth is time-based through GrowthTracker.update().
        // Damage must not change its growth stage.
    }

    public int incrementKiwibeastHitCounter() {
        return ++kiwibeastHitCounter;
    }

    public int getKiwibeastHitCounter() {
        return kiwibeastHitCounter;
    }

    public void startChomperBite(boolean killedZombie) {
        if (!"Chomper".equalsIgnoreCase(name)) return;
        // The attack clip is bite_end; the renderer will switch to special/special_idle
        // only when a zombie was actually killed.
        setVisualAnimationState("bite_end", 0.45);
        chomperSpecialPending = killedZombie;
    }

    public void finishChomperSpecial() {
        if (!"Chomper".equalsIgnoreCase(name)) return;
        chomperSpecialActive = false;
        setVisualAnimationState("special_end", 0.6);
    }

    public void tickVisualAnimation(double deltaTimeSeconds) {
        if (visualAnimationRemaining <= 0) return;
        visualAnimationElapsed += deltaTimeSeconds;
        visualAnimationRemaining = Math.max(0.0, visualAnimationRemaining - deltaTimeSeconds);
        if (visualAnimationRemaining <= 0 && "special".equals(visualAnimationState)) {
            if (chomperDigestIdlePending) {
                chomperDigestIdlePending = false;
                setVisualAnimationState("special_idle", 10.0);
            } else {
                finishChomperSpecial();
            }
        } else if (visualAnimationRemaining <= 0 && "special_idle".equals(visualAnimationState)) {
            finishChomperSpecial();
        } else if (visualAnimationRemaining <= 0 && "special_end".equals(visualAnimationState)) {
            clearVisualAnimationState();
        } else if (visualAnimationRemaining <= 0 && "bite_end".equals(visualAnimationState)) {
            clearVisualAnimationState();
            if (chomperSpecialPending) {
                chomperSpecialPending = false;
                chomperSpecialActive = true;
                chomperDigestIdlePending = true;
                setVisualAnimationState("special", 0.8);
            }
        } else if (visualAnimationRemaining <= 0) {
            // Generic one-shot states (e.g. Kernel-pult's butter "attack2" throw)
            // that don't need a special follow-up transition just revert to the
            // normal idle/attack resolution once their window elapses.
            clearVisualAnimationState();
        }
    }

    public int getChillLevel() {
        return this.chillLevel;
    }

    public void setChillLevel(int chillLevel) {
        this.chillLevel = chillLevel;
    }
}