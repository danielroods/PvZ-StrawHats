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
    private static final double ENDURIAN_ATTACK_VISUAL_HOLD = 0.3;
    private static final double ENDURIAN_CONTACT_RANGE_X = 1.0;
    private static final double ENDURIAN_CONTACT_RANGE_Y = 0.75;
    private static final double EXPLODE_O_NUT_BLAST_RADIUS = 1.0;

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

    private boolean meleeFacingLeft = false;
    private String visualAnimationState;
    private double visualAnimationRemaining = 0.0;
    private double visualAnimationElapsed = 0.0;

    private double endurianSpikeCooldown = 0.0;
    private double endurianAttackVisualTimer = 0.0;

    // Cactus: true while ducked underground because a zombie is standing on its own tile
    // (down/down_idle/down_attack), and true while popped up on tiptoe to shoot a
    // Gargantuar (up_stretch/attack_stretch) - see tickCactusPosture(). The two are
    // mutually exclusive.
    private boolean cactusUnderground = false;
    private boolean cactusStretching = false;

    private boolean explodeONutDetonated = false;

    private boolean potatoMineArmed = false;
    private boolean potatoMineDetonationPending = false;
    // Set only when a zombie's eating/chomping attack actually kills a potato mine.
    // Such a death must not trigger the mine's explosion effect.
    private boolean potatoMineEatenByZombie = false;
    private boolean chomperSpecialActive = false;
    private boolean chomperSpecialPending = false;
    private boolean chomperDigestIdlePending = false;
    private int kiwibeastHitCounter = 0;

    // Imitater keeps the loadout-selected target until its planting animation
    // completes, then becomes that plant.
    private String imitaterTargetName;
    private boolean imitaterTransformationStarted = false;
    private int imitaterAnimationPhase = 0; // 0=idle, 1=attack, 2=transformed

    private Position squashVisualPosition;
    private Position squashVisualOrigin;
    private Position squashVisualTarget;
    private boolean squashActionState = false;
    private boolean specialInvulnerable = false;

    // Magnet-shroom: whether it is currently holding a caught metal item (the PAM's
    // "Magnet_Item" element). False = nothing caught yet, element must stay hidden.
    // Set true once a "catch" animation completes, and set false again once Plant Food
    // throws the held items at zombies - see ModifyStrategy and DisarmBlast.
    private boolean magnetItemVisible = false;
    // Set when Hot Potato is planted on an IceBlock so the renderer can play the
    // ice-melting puddle effect once, without coupling the model to EffectRenderer.
    private boolean hotPotatoMeltEffectPending = false;
    private boolean graveBusterConsumedGrave = false;

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
    private boolean farewellBlastFired = false;
    private int damageUpgradeBonus = 0;
    private double abilityUpgradeBonus = 0.0;
    private List<Position> shootingVectors = new ArrayList<>();

    public Plant(String name, Position position, int HP) {
        super(position, HP);
        this.name = name;
        this.hpStat = new ModifiableStat(HP);
    }

    public double getIntervalTimer() { return this.internalTimer; }
    public void setInternalTimer(double internalTimer) { this.internalTimer = internalTimer; }
    public int getMaxHp() { return (int) hpStat.getValue(); }
    public void setMaxHp(int maxHp) { hpStat.setBaseValue(Math.max(1, maxHp)); }

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
        tickEndurianTimers(deltaTimeSeconds);
        tickCactusPosture(session);

        if (lifespanSeconds > 0) {
            remainingLifeSeconds = GameClock.countDown(remainingLifeSeconds, deltaTimeSeconds);
            if (GameClock.isZero(remainingLifeSeconds)) {
                setState(PlantState.DYING);
                if (name != null && name.equalsIgnoreCase("Torchwood")) {
                    executeTorchwoodDeathExplosion();
                }
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
            boolean cactusBurstFinished = isCactus()
                    && plantFoodEffect instanceof model.collections.plant.plantfood.TimedProjectileBurst burst
                    && burst.isBurstFinished();
            // Cactus's Plant Food timer runs forever (see activatePlant), so once its initial
            // burst has fired, fall through and let the normal ActStrategy cadence keep it
            // shooting for the rest of its life instead of freezing on the burst forever.
            if (!cactusBurstFinished && (plantFoodEffect == null || plantFoodEffect.drivesActStrategy())) return;
        }

        if (actStrategy == null) return;
        internalTimer = GameClock.countDown(internalTimer, deltaTimeSeconds);
        if (state == PlantState.PREPPING && GameClock.isZero(internalTimer)) {
            state = PlantState.ACTIVE;
            if (isPotatoMine() && !potatoMineArmed && !potatoMineDetonationPending) {
                potatoMineArmed = true;
                float recoverDuration = model.collections.animations.AnimationFactory
                        .clipDurationForDisplayName(name, "recover");
                setVisualAnimationState("recover", recoverDuration > 0f ? recoverDuration : 0.4);
            }
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
        if (isEndurian()) reflectEndurianSpikes(dealer);
        if (dealer != null && name.equalsIgnoreCase("Sun Bean") && abilityValue > 0) {
            int sunValue = (int) abilityValue;
            if (isPlantFoodActive()) sunValue *= 2;
            dealer.markSunBeanCarrier(sunValue);
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
                if (name.equalsIgnoreCase("Doom-shroom")) {
                    // A Doom-shroom can also be killed by a zombie before its fuse ends;
                    // that death must detonate it at its current growth stage.
                    this.internalTimer = 0.0;
                    if (this.actStrategy != null) this.actStrategy.act(this, frostSession);
                }
                if (isExplodeONut()) detonateExplodeONut();
                if (name.equalsIgnoreCase("Torchwood")) executeTorchwoodDeathExplosion();
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

    private void executeTorchwoodDeathExplosion() {
        GameSession session = GameSession.peekInstance();
        Position center = getPosition();
        if (session == null || center == null) return;

        // Torchwood's death explosion only affects zombies in/around its tile.
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position zp = zombie.getPosition();
            if (Math.abs(zp.x() - center.x()) <= 1
                    && Math.abs(zp.y() - center.y()) <= 1) {
                zombie.takeDamage(Math.max(1, zombie.getHP()), this);
            }
        }
    }

    public boolean isExplodeONut() {
        return name != null && name.equalsIgnoreCase("Explode-o-nut");
    }

    public boolean isExplodeONutDetonated() {
        return explodeONutDetonated;
    }

    public void setExplodeONutDetonated(boolean detonated) {
        this.explodeONutDetonated = detonated;
    }

    private void detonateExplodeONut() {
        if (explodeONutDetonated) return;
        explodeONutDetonated = true;

        GameSession session = GameSession.peekInstance();
        Position center = getPosition();
        if (session == null || center == null) return;

        int damage = Math.max(1, getDamage());
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position zp = zombie.getPosition();
            if (Math.abs(zp.x() - center.x()) <= EXPLODE_O_NUT_BLAST_RADIUS
                    && Math.abs(zp.y() - center.y()) <= EXPLODE_O_NUT_BLAST_RADIUS) {
                zombie.takeDamageWithAsh(damage, this);
            }
        }

        for (model.collections.zombie.zombie_pushing_item.PushableStructure structure
                : session.getPushableStructures()) {
            if (structure == null || !structure.isAlive()) continue;
            Position sp = structure.getPosition();
            if (sp == null) continue;
            if (Math.abs(sp.x() - center.x()) <= EXPLODE_O_NUT_BLAST_RADIUS
                    && Math.abs(sp.y() - center.y()) <= EXPLODE_O_NUT_BLAST_RADIUS) {
                structure.takeDamage(damage, this, session);
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
    public boolean isPotatoMine() {
        return name != null && (name.equalsIgnoreCase("Potato Mine")
                || name.equalsIgnoreCase("Primal Potato Mine"));
    }

    public boolean isPotatoMineArmed() {
        return potatoMineArmed;
    }

    public void armPotatoMine() {
        if (!isPotatoMine() || !isAlive()) return;
        potatoMineArmed = true;
        potatoMineDetonationPending = false;
        internalTimer = 0.0;
        state = PlantState.ACTIVE;
        clearVisualAnimationState();
    }

    public boolean startPotatoMineAttack() {
        if (!isPotatoMine() || !potatoMineArmed || potatoMineDetonationPending || !isAlive()) return false;
        potatoMineArmed = false;
        potatoMineDetonationPending = true;
        internalTimer = 0.67;
        state = PlantState.PREPPING;
        setVisualAnimationState("attack", 0.67);
        return true;
    }

    public boolean isPotatoMineDetonationPending() {
        return potatoMineDetonationPending;
    }

    public void markPotatoMineEatenByZombie() {
        if (isPotatoMine()) {
            potatoMineEatenByZombie = true;
            potatoMineDetonationPending = false;
            potatoMineArmed = false;
            visualAnimationState = null;
            visualAnimationRemaining = 0.0;
            visualAnimationElapsed = 0.0;
        }
    }

    public boolean wasPotatoMineEatenByZombie() {
        return potatoMineEatenByZombie;
    }

    public void finishPotatoMineAttack() {
        potatoMineDetonationPending = false;
    }

    public void setPotatoMineArmed(boolean armed) {
        this.potatoMineArmed = armed;
    }

    public void setPotatoMineDetonationPending(boolean pending) {
        this.potatoMineDetonationPending = pending;
    }

    public void setPotatoMineEatenByZombie(boolean eaten) {
        this.potatoMineEatenByZombie = eaten;
    }

    public boolean activatePlant(GameSession session) {
        if (this.plantFoodEffect == null || this.plantFoodTimer > 0 || session == null || !isAlive()) return false;
        this.plantFoodEffect.reset();
        if (this.growthTracker != null) this.growthTracker.skipToMaxStage();
        for (Plant sibling : new ArrayList<>(session.getPlants())) {
            if (sibling != null && sibling.isAlive() && sibling.getId() == this.id
                    && sibling.getLifespanSeconds() > 0) {
                sibling.resetLifespan();
            }
        }
        this.plantFoodEffect.applyStatusModifiers(this);
        this.plantFoodTimer = Double.POSITIVE_INFINITY;
        try {
            this.plantFoodEffect.triggerSuperpower(this, session);
        } finally {
            this.plantFoodTimer = 0.0;
        }
        if (isPotatoMine()) {
            this.plantFoodTimer = 0.0;
            setVisualAnimationState("plantfood2", 0.67);
        } else {
            this.plantFoodTimer = ("Torchwood".equalsIgnoreCase(name) || isCactus())
                    ? Double.POSITIVE_INFINITY
                    : Math.max(0.0, this.plantFoodEffect.getDurationSeconds());
        }

        if ("Torchwood".equalsIgnoreCase(name)) {
            setVisualAnimationState("plantfood", Double.POSITIVE_INFINITY);
        }

        if ("Sweet Potato".equalsIgnoreCase(name) && this.plantFoodTimer > 0.0) {
            this.specialInvulnerable = true;
            float clipDuration = model.collections.animations.AnimationFactory
                    .exactClipDurationForPath(
                            model.collections.animations.AnimationFactory.pathForDisplayName(name),
                            "plantfood");
            double visualDuration = clipDuration > 0f
                    ? Math.min(this.plantFoodTimer, Math.max(clipDuration, 0.1f))
                    : this.plantFoodTimer;
            setVisualAnimationState("plantfood", visualDuration);
        }

        if ("Puff-shroom".equalsIgnoreCase(name)) {
            for (Plant sibling : new ArrayList<>(session.getPlants())) {
                if (sibling == this || sibling == null || !sibling.isAlive()) continue;
                if (sibling.getId() != this.id || sibling.plantFoodEffect == null) continue;
                if (sibling.plantFoodTimer > 0) continue;
                sibling.plantFoodEffect.reset();
                sibling.plantFoodEffect.applyStatusModifiers(sibling);
                sibling.plantFoodTimer = Double.POSITIVE_INFINITY;
                try {
                    sibling.plantFoodEffect.triggerSuperpower(sibling, session);
                } finally {
                    sibling.plantFoodTimer = 0.0;
                }
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

    public double getPlantFoodTimer() { return this.plantFoodTimer; }

    public void setPlantFoodTimer(double seconds) { this.plantFoodTimer = Math.max(0.0, seconds); }
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
        int base = this.damage;
        if (growthTracker != null) {
            Double staged = growthTracker.getStageValue("damage");
            if (staged != null) base = staged.intValue() + damageUpgradeBonus;
        }
        // Cactus deals reduced damage while ducked underground hiding from a zombie
        // standing on its tile - see tickCactusPosture().
        if (isCactus() && cactusUnderground) {
            base = Math.max(1, (int) Math.round(base * 0.5));
        }
        return base;
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
    public void setStackNumber(int stackNumber) {
        this.stackNumber = Math.max(1, Math.min(this.maxStackNumber, stackNumber));
    }
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
    public boolean hasFiredFarewellBlast() { return farewellBlastFired; }
    public void markFarewellBlastFired() { farewellBlastFired = true; }
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
    public void setRemainingLifeSeconds(double seconds) {
        this.remainingLifeSeconds = Math.max(0, seconds);
    }
    public void resetLifespan() { this.remainingLifeSeconds = this.lifespanSeconds; }
    public double getAbilityValue() {
        if (growthTracker != null) {
            Double staged = growthTracker.getStageValue("abilityValue");
            if (staged != null) return staged + abilityUpgradeBonus;
        }
        return this.abilityValue;
    }

    public void setUpgradeStatBonuses(int damageBonus, double abilityBonus) {
        this.damageUpgradeBonus = damageBonus;
        this.abilityUpgradeBonus = abilityBonus;
    }

    public int getDamageUpgradeBonus() { return damageUpgradeBonus; }

    public double getAbilityUpgradeBonus() { return abilityUpgradeBonus; }
    public void setWrampUp(List<Map<String, Object>> wrampUp) { setWrampUp(wrampUp, 0.0, 0); }
    public void setWrampUp(List<Map<String, Object>> wrampUp, double stageTimeShift) {
        setWrampUp(wrampUp, stageTimeShift, 0);
    }
    public void setWrampUp(List<Map<String, Object>> wrampUp, double stageTimeShift, int extraStages) {
        this.growthTracker = (wrampUp != null && !wrampUp.isEmpty())
                ? new GrowthTracker(wrampUp, stageTimeShift, extraStages) : null;
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

    public boolean isGarlic() {
        return name != null && name.equalsIgnoreCase("Garlic");
    }

    public boolean isSweetPotato() {
        return name != null && name.equalsIgnoreCase("Sweet Potato");
    }

    public String getSweetPotatoHealthAnimationState() {
        if (!isSweetPotato()) return "idle";
        double ratio = getHealthRatio();
        if (ratio > 0.70) return "idle";
        if (ratio > 0.45) return "idle_damage";
        if (ratio >= 0.20) return "idle_damage2";
        return "idle2_damage3";
    }

    public String getGarlicHealthAnimationState() {
        if (!isGarlic()) return "idle";
        double ratio = getHealthRatio();
        if (ratio > 0.65) return "idle";
        if (ratio >= 0.30) return "idle_damage";
        return "idle-damage2";
    }

    public boolean handleGarlicBite(Zombie zombie, GameSession session) {
        if (!isGarlic() || zombie == null || session == null || zombie.getPosition() == null) return false;
        if (isGarlicRedirectImmune(zombie)) return false;

        int rows = session.getRows();
        if (rows <= 1) return false;

        int currentRow = (int) Math.round(zombie.getPosition().y());
        if (currentRow < 0 || currentRow >= rows) return false;

        int targetRow;
        if (currentRow <= 0) targetRow = 1;
        else if (currentRow >= rows - 1) targetRow = rows - 2;
        else targetRow = Math.random() < 0.5 ? currentRow - 1 : currentRow + 1;

        double originalX = zombie.getPosition().x();
        double directionX = zombie.getSpeed() == null ? -1.0
                : Math.signum(zombie.getSpeed().x());
        if (Math.abs(directionX) < 0.0001) directionX = -1.0;
        double redirectX = originalX + (0.06 * directionX);
        // Glide the zombie into its new row over a short duration instead of
        // snapping it there instantly — the same smooth row-shift used by
        // Frostbite Caves tile sliders (see GameSession#beginSliderRide).
        session.beginSliderRide(zombie, redirectX, currentRow, targetRow);
        zombie.startKnockback(0.03 * directionX, 0.12);
        zombie.applyStatus(Zombie.Status.BUTTER, 0.65);
        zombie.clearActionAnimationState();
        return true;
    }

    private boolean isGarlicRedirectImmune(Zombie zombie) {
        String alias = zombie.getAlias() == null ? "" : zombie.getAlias().toLowerCase();
        return alias.contains("piano")
                || alias.contains("arcade")
                || alias.contains("robot")
                || alias.contains("robo");
    }

    public void executeGarlicPlantFood(GameSession session) {
        if (!isGarlic() || session == null || getPosition() == null) return;
        int row = (int) Math.round(getPosition().y());
        double garlicX = getPosition().x();

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()
                    || zombie.getPosition() == null) continue;
            if (isGarlicRedirectImmune(zombie)) continue;
            Position zp = zombie.getPosition();
            if (Math.abs(zp.y() - row) > 0.5 || zp.x() <= garlicX) continue;

            int targetRow;
            if (row <= 0) targetRow = 1;
            else if (row >= session.getRows() - 1) targetRow = session.getRows() - 2;
            else targetRow = Math.random() < 0.5 ? row - 1 : row + 1;

            double directionX = zombie.getSpeed() == null ? -1.0
                    : Math.signum(zombie.getSpeed().x());
            if (Math.abs(directionX) < 0.0001) directionX = -1.0;
            double redirectX = zp.x() + (0.06 * directionX);
            // Same smooth row glide as the single-bite redirect above, rather
            // than snapping every zombie on the row instantly.
            session.beginSliderRide(zombie, redirectX, row, targetRow);
            zombie.startKnockback(0.03 * directionX, 0.12);
            zombie.applyStatus(Zombie.Status.BUTTER, 7.5);
            zombie.clearActionAnimationState();
        }
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

    public boolean isEndurian() {
        return name != null && name.equalsIgnoreCase("Endurian");
    }

    public boolean isCactus() {
        return name != null && name.equalsIgnoreCase("Cactus");
    }

    public boolean isCactusUnderground() {
        return cactusUnderground;
    }

    public boolean isCactusStretching() {
        return cactusStretching;
    }

    public void setCactusPosture(boolean underground, boolean stretching) {
        this.cactusUnderground = underground;
        this.cactusStretching = stretching;
    }

    /**
     * Cactus has two situational postures on top of its normal idle/attack:
     * - it ducks underground (down -> down_idle/down_attack loop -> up) for as long as a
     *   zombie is standing on its own tile, dealing reduced damage while hidden (see
     *   getDamage()) instead of eating a melee hit;
     * - lacking a Balloon Zombie to justify the pose, it instead pops up on its "stretch"
     *   pose (up_stretch -> attack_stretch -> down_stretch) whenever it's shooting at a
     *   Gargantuar, so that clip still gets used.
     * Both transitions are one-shot clips driven through the existing
     * visualAnimationState mechanism; the looping down/up-stretch clip choice itself is
     * resolved by PlantRenderer from the booleans this method maintains.
     */
    private void tickCactusPosture(GameSession session) {
        if (!isCactus() || session == null || !isAlive()) return;

        boolean zombieOnTile = isZombieOnCactusTile(session);
        if (zombieOnTile != cactusUnderground) {
            cactusUnderground = zombieOnTile;
            if (zombieOnTile) cactusStretching = false;
            boolean pf = isPlantFoodActive();
            String introState = zombieOnTile
                    ? (pf ? "down_plantfood" : "down")
                    : (pf ? "up_plantfood" : "up");
            float duration = model.collections.animations.AnimationFactory
                    .clipDurationForDisplayName(name, introState);
            setVisualAnimationState(introState, duration > 0f ? duration : 0.4);
        }

        if (!cactusUnderground) {
            boolean targetingGargantuar = isGargantuarInCactusRange(session);
            if (targetingGargantuar != cactusStretching) {
                cactusStretching = targetingGargantuar;
                String stretchState = targetingGargantuar ? "up_stretch" : "down_stretch";
                float duration = model.collections.animations.AnimationFactory
                        .clipDurationForDisplayName(name, stretchState);
                setVisualAnimationState(stretchState, duration > 0f ? duration : 0.4);
            }
        }
    }

    private boolean isZombieOnCactusTile(GameSession session) {
        Position self = getPosition();
        if (self == null) return false;
        long row = Math.round(self.y());
        long col = Math.round(self.x());
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position zp = zombie.getPosition();
            if (Math.round(zp.y()) == row && Math.round(zp.x()) == col) return true;
        }
        return false;
    }

    private boolean isGargantuarInCactusRange(GameSession session) {
        Position self = getPosition();
        if (self == null) return false;
        long row = Math.round(self.y());
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            if (zombie.getRace() != model.collections.zombie.ZombieRace.GARGANTUAR) continue;
            Position zp = zombie.getPosition();
            if (Math.round(zp.y()) != row || zp.x() <= self.x()) continue;
            if (!isWithinAttackRange(zp)) continue;
            return true;
        }
        return false;
    }

    public boolean isExplodeONutArmored() {
        return isExplodeONut() && armor != null && armor.getHP() > 0;
    }

    public int getExplodeONutDamageTier() {
        if (!isExplodeONut()) return 0;
        double ratio = getHealthRatio();
        if (ratio > 0.80) return 0;
        if (ratio > 0.50) return 1;
        if (ratio > 0.20) return 2;
        return 3;
    }

    public String getExplodeONutHealthAnimationState() {
        return switch (getExplodeONutDamageTier()) {
            case 1 -> "damage";
            case 2 -> "damage2";
            case 3 -> "damage3";
            default -> "idle";
        };
    }

    public int getExplodeONutPlantFoodArmorStage() {
        if (!isExplodeONutArmored()) return 0;
        int max = Math.max(1, armor.getMaxHP());
        double ratio = armor.getHP() / (double) max;
        if (ratio > 0.60) return 1;
        if (ratio > 0.25) return 2;
        return 3;
    }

    public boolean isEndurianPlantFoodArmored() {
        return isEndurian() && armor != null && armor.getHP() > 0;
    }

    public int getEndurianSpikeDamage() {
        if (!isEndurian()) return 0;
        int base = getDamage() + (int) getSpecialUpgrade("REFLECT_DAMAGE_BUFF", 0);
        if (base <= 0) return 0;
        return (isPlantFoodActive() || isEndurianPlantFoodArmored()) ? base * 2 : base;
    }

    public boolean isEndurianUnderAttack() {
        return isEndurian() && endurianAttackVisualTimer > 0;
    }

    public void setEndurianAttackVisualTimer(double seconds) {
        this.endurianAttackVisualTimer = Math.max(0, seconds);
    }

    public int getEndurianDamageTier() {
        if (!isEndurian()) return 0;
        double ratio = getHealthRatio();
        if (ratio > 0.80) return 0;
        if (ratio > 0.50) return 1;
        if (ratio > 0.20) return 2;
        return 3;
    }

    public String getEndurianHealthAnimationState() {
        return switch (getEndurianDamageTier()) {
            case 1 -> "damage";
            case 2 -> "damage2";
            case 3 -> "damage3";
            default -> "idle";
        };
    }

    public int getEndurianPlantFoodArmorStage() {
        if (!isEndurianPlantFoodArmored()) return 0;
        int max = Math.max(1, armor.getMaxHP());
        double ratio = armor.getHP() / (double) max;
        if (ratio > 0.60) return 1;
        if (ratio > 0.25) return 2;
        return 3;
    }

    private void tickEndurianTimers(double deltaTimeSeconds) {
        if (!isEndurian()) return;
        if (endurianSpikeCooldown > 0) {
            endurianSpikeCooldown = GameClock.countDown(endurianSpikeCooldown, deltaTimeSeconds);
        }
        if (endurianAttackVisualTimer > 0) {
            endurianAttackVisualTimer = GameClock.countDown(endurianAttackVisualTimer, deltaTimeSeconds);
        }
    }

    private void reflectEndurianSpikes(Zombie dealer) {
        if (dealer == null || !dealer.isAlive() || !isEndurianSpikeContact(dealer)) return;
        endurianAttackVisualTimer = ENDURIAN_ATTACK_VISUAL_HOLD;
        if (endurianSpikeCooldown > 0) return;
        int spikeDamage = getEndurianSpikeDamage();
        if (spikeDamage <= 0) return;
        endurianSpikeCooldown = Math.max(GameClock.SECONDS_PER_TICK, getActionInterval());
        dealer.takeDamage(spikeDamage, this);
    }

    private boolean isEndurianSpikeContact(Zombie dealer) {
        Position self = getLocation();
        Position other = dealer.getPosition();
        if (self == null || other == null) return false;
        return Math.abs(other.x() - self.x()) <= ENDURIAN_CONTACT_RANGE_X
                && Math.abs(other.y() - self.y()) <= ENDURIAN_CONTACT_RANGE_Y;
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

    public String getImitaterTargetName() { return imitaterTargetName; }
    public void setImitaterTargetName(String name) { this.imitaterTargetName = name; }
    public boolean isImitaterTransformationStarted() { return imitaterTransformationStarted; }
    public void setImitaterTransformationStarted(boolean started) { this.imitaterTransformationStarted = started; }
    public int getImitaterAnimationPhase() { return imitaterAnimationPhase; }
    public void setImitaterAnimationPhase(int phase) { this.imitaterAnimationPhase = phase; }

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

    public boolean isMagnetItemVisible() { return magnetItemVisible; }
    public void setMagnetItemVisible(boolean visible) { this.magnetItemVisible = visible; }

    public boolean isHotPotatoMeltEffectPending() { return hotPotatoMeltEffectPending; }
    public void setHotPotatoMeltEffectPending(boolean pending) { this.hotPotatoMeltEffectPending = pending; }

    public boolean isGraveBuster() {
        return name != null && name.equalsIgnoreCase("Grave Buster");
    }

    public boolean hasGraveBusterConsumedGrave() { return graveBusterConsumedGrave; }
    public void setGraveBusterConsumedGrave(boolean eaten) { graveBusterConsumedGrave = eaten; }

    public boolean isSpecialInvulnerable() { return specialInvulnerable; }
    public void setSpecialInvulnerable(boolean value) { this.specialInvulnerable = value; }

    public void setVisualAnimationState(String state, double durationSeconds) {
        this.visualAnimationState = state;
        this.visualAnimationRemaining = Math.max(0.0, durationSeconds);
        this.visualAnimationElapsed = 0.0;
    }

    public void setVisualAnimationProgress(String state, double remainingSeconds,
                                           double elapsedSeconds) {
        this.visualAnimationState = state;
        this.visualAnimationRemaining = Math.max(0.0, remainingSeconds);
        this.visualAnimationElapsed = Math.max(0.0, elapsedSeconds);
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
        if ("Sweet Potato".equalsIgnoreCase(name)) {
            specialInvulnerable = false;
            clearVisualAnimationState();
            return;
        }
        if ("Chomper".equalsIgnoreCase(name) && chomperSpecialActive) {
            setVisualAnimationState("special_idle", 10.0);
        } else {
            clearVisualAnimationState();
        }
    }

    public int getGrowthStage() {
        return growthTracker == null ? 1 : growthTracker.getCurrentStage();
    }

    public void setGrowthStage(int stage) {
        if (growthTracker != null) growthTracker.setCurrentStage(stage);
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
        if (visualAnimationRemaining <= 0 && "Magnet-shroom".equalsIgnoreCase(name)
                && "special".equals(visualAnimationState)) {
            // The metal item has finished travelling to the plant - switch to the
            // "catch" clip that shows it actually grabbing hold of it.
            float catchDuration = model.collections.animations.AnimationFactory
                    .clipDurationForDisplayName(name, "catch");
            setVisualAnimationState("catch", catchDuration > 0f ? catchDuration : 0.5);
        } else if (visualAnimationRemaining <= 0 && "Magnet-shroom".equalsIgnoreCase(name)
                && "catch".equals(visualAnimationState)) {
            // Caught for good - the Magnet_Item element stays visible from here on,
            // through idle, until Plant Food throws it away (see DisarmBlast).
            magnetItemVisible = true;
            clearVisualAnimationState();
        } else if (visualAnimationRemaining <= 0 && "special".equals(visualAnimationState)) {
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