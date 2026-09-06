package model.collections.zombie;

import model.collections.Faction;
import model.collections.Item;
import model.collections.armour.Armour;
import model.collections.plant.Plant;
import model.collections.zombie.zombie_attack.AttackBehavior;
import model.collections.zombie.zombie_attack.ZombieTargeting;
import model.collections.zombie.zombie_defense.DefenseBehavior;
import model.collections.zombie.zombie_effect.FireEffect;
import model.collections.zombie.zombie_effect.ZombieEffectStatus;
import model.collections.zombie.zombie_move.HypnotizedMoveBehavior;
import model.collections.zombie.zombie_move.MoveBehavior;
import model.collections.zombie.zombie_move.ProspectorMove;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.Attack;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.projectile.hit.HitEffectStrategy;
import model.utils.GameSession;

import java.util.List;
import java.util.Random;

public class Zombie extends Item implements Attack {
    private static final Random RAND = new Random();
    private static final double BOSS_MAX_SINGLE_HIT_FRACTION = 0.10;

    private String name;
    private Armour armour;
    private boolean isFacingRight;
    private boolean hasPlantFood;
    private boolean plantFoodPending;

    private int maxHp;
    private double eatDps;
    private ZombieRace race;
    private ZombieState zombieState = ZombieState.WALKING;
    private final boolean isGlowing;

    private MoveBehavior moveBehavior;

    // Temporary smooth knockback used by melee plants. The knockback is applied
    // over time in tick(), so the zombie does not teleport to the final tile.
    private double knockbackVelocityX = 0.0;
    private double knockbackRemaining = 0.0;
    private AttackBehavior attackBehavior;
    private DefenseBehavior defenseBehavior;
    private ZombieEffectStatus zombieEffectStatus;

    private PushableStructure pushedStructure;
    private int pushableRespawnsRemaining = 0;

    private List<String> damageWhileSubmerged;
    private List<String> damageWhileSubmergedPlantfoodOnly;

    // Tangle Kelp drag-under-water visual state (cosmetic only - the water ripple itself
    // never moves; the zombie's draw position is pushed further down under the fixed
    // clip line as this climbs from 0 to 1). Driven by TangleKelpStrategy/TangleKelpPlantFood
    // over the grab's duration, then the zombie is killed once fully submerged.
    private double dragUnderWaterProgress = 0.0;
    private boolean dragUnderWaterDeath = false;
    private boolean swashbucklerWaterDeath = false;
    private ZombieSequence sequence;
    private boolean immobilized;
    private ZombieStunProfile stunProfile;
    private int shieldHp;
    private int shieldMaxHp;
    private double shieldHitFlash;
    private double hoverHeight;

    // Optional visual-only animation override (e.g. "toss", "push", "cast",
    // "cast_loop", "reel") on top of the coarse WALKING/EATING/DEAD state.
    // Does not affect gameplay logic, only what the renderer prefers to show.
    private String actionAnimationState;
    private double actionAnimationElapsed;
    private double actionAnimationDuration;
    private boolean actionAnimationLoop;

    public enum Status { NORMAL, FREEZE, FROZEN, FIRED, POISONED, BUTTER, HYPNOTIZED }
    private Status status = Status.NORMAL;
    private double statusTimer = 0;
    private double statusDamageAccumulator = 0;
    private boolean deathHandled = false;
    private boolean firedDeath = false;
    private boolean ashDeath = false;
    private boolean shockDeath = false;
    private VulnerabilityType vulnerabilityState = VulnerabilityType.FULLY_VULNERABLE;
    private Faction faction = Faction.ZOMBIES;
    private boolean fromNecromancy;

    // While true (e.g. Prospector Zombie sailing through the air after its
    // dynamite blast), the zombie sails over plants instead of stopping to eat
    // them the moment one shows up in its path.
    private boolean ignoreTargetAcquisition = false;
    private int sunBeanCarrierValue = 0;
    private double sunBeanTimer = 0.0;

    private boolean boss;
    private double damageTakenMultiplier = 1.0;

    public Zombie(String name, Position position, int HP, boolean isFacingRight, Armour armour, int speed) {
        super(position, HP);
        this.name = name;
        this.armour = armour;
        this.isFacingRight = isFacingRight;
        this.maxHp = HP;
        setPosition(position);
        setSpeed(new Position(isFacingRight ? speed : -speed, 0));
        this.hasPlantFood = chanceToHavePlantFood();
        this.isGlowing = this.hasPlantFood;
    }

    public Zombie(String name, Armour armour, boolean canSpawnPlantFood) {
        super(null, 1);
        this.name = name;
        this.armour = armour;
        this.isGlowing = canSpawnPlantFood && RAND.nextInt(100) < 5;
    }

    public Zombie(String name, Armour armour) {
        this(name, armour, true);
    }

    public boolean chanceToHavePlantFood() {
        return RAND.nextInt(100) < 5;
    }

    public static final double HIT_RADIUS = 0.5;
    public static final double BOSS_HIT_RADIUS = 1.55;

    public void markAsBoss() {
        this.boss = true;
    }

    public double getHitRadius() {
        return boss ? BOSS_HIT_RADIUS : HIT_RADIUS;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setDamageTakenMultiplier(double multiplier) {
        this.damageTakenMultiplier = Math.max(0.0, multiplier);
    }

    public double getDamageTakenMultiplier() {
        return damageTakenMultiplier;
    }

    @Override
    public void dealDamage(Item target) {
        if (target == null || !target.isAlive()) return;
        int damage = (int) Math.round(getEatDps() * service.GameClock.SECONDS_PER_TICK);
        if (damage <= 0) return;

        if (target instanceof Plant plant) {
            plant.takeDamage(damage, this);
        } else if (target instanceof Zombie zombie) {
            zombie.takeDamage(damage, this);
        } else {
            target.takeDamage(damage);
        }
    }

    @Override
    public void takeDamage(int damage) {
        takeDamage(damage, null);
    }

    public void takeDamage(int damage, boolean isPoisonous) {
        if (!isAlive() || this.vulnerabilityState == VulnerabilityType.INVULNERABLE) return;

        if (isPoisonous) {
            int newHp = Math.max(0, getHP() - damage);
            setHP(newHp);

            if (newHp <= 0) handleDeath(GameSession.peekInstance(), "Poison", false);
        } else {
            takeDamage(damage, null);
        }
    }

    public void takeDamage(int damage, Object damageSource) {
        if (!isAlive() || this.vulnerabilityState == VulnerabilityType.INVULNERABLE) return;

        GameSession frostSession = GameSession.peekInstance();
        if (frostSession != null && FrostbiteFreezing.isFrozenInIce(frostSession, this)) {
            FrostbiteFreezing.damageFrozenZombieIfInIce(
                    frostSession, this, Math.max(0, damage), FrostbiteFreezing.isFireDamageSource(damageSource));
            return;
        }

        // The Pirate Barrel Pusher is protected by its barrel while the barrel
        // is still intact. Lobbed projectiles can hit the zombie from above, and
        // direct projectiles are allowed only when they reach it from behind.
        // Once the barrel is destroyed, the zombie uses its normal vulnerability.
        if ("ZombieBarrelRoller".equals(name)
                && pushedStructure != null
                && pushedStructure.isAlive()
                && pushedStructure.getType() == model.pitches.obstacles.PushableType.BARREL) {
            boolean allowDamage = false;
            if (damageSource instanceof Projectile projectile) {
                if (projectile.isLobbed()) {
                    allowDamage = true;
                } else {
                    Position previous = projectile.getPreviousPosition();
                    Position current = projectile.getPosition();
                    Position zombiePosition = getPosition();
                    if (previous != null && current != null && zombiePosition != null) {
                        double approachX = current.x() - previous.x();
                        // Zombies normally face left. Their back is therefore to
                        // the right; hypnotized/reversed zombies have the opposite back.
                        allowDamage = isFacingRight ? approachX > 0
                                && previous.x() < zombiePosition.x()
                                : approachX < 0
                                && previous.x() > zombiePosition.x();
                    }
                }
            }
            if (!allowDamage) return;
        }

        if (!acceptsAttackFrom(damageSource)) return;

        int actualDamage = damage;
        if (this.defenseBehavior != null) {
            actualDamage = this.defenseBehavior.handleDamage(this, damage, damageSource, GameSession.getInstance());
        }

        if (actualDamage > 0) {
            applyDamageCalculations(actualDamage, damageSource);
        }
    }

    private void applyDamageCalculations(int damage, Object damageSource) {
        int scaled = damageTakenMultiplier == 1.0
                ? damage : (int) Math.round(damage * damageTakenMultiplier);
        if (boss) scaled = capBossHit(scaled);
        scaled = absorbWithShield(scaled);
        if (scaled <= 0) return;
        int remaining = (armour != null && armour.getHP() > 0) ? armour.absorbDamage(scaled) : scaled;
        if (remaining <= 0) return;

        int newHp = Math.max(0, getHP() - remaining);
        setHP(newHp);

        if (newHp <= 0) {
            boolean diedFromFire = isFireDamageSource(damageSource) || status == Status.FIRED;
            boolean diedFromShock = isShockDamageSource(damageSource);
            handleDeath(GameSession.peekInstance(), resolveKillerName(damageSource), diedFromFire, ashDeath, diedFromShock);
        }
    }

    private int capBossHit(int damage) {
        int cap = Math.max(1, (int) Math.round(maxHp * BOSS_MAX_SINGLE_HIT_FRACTION));
        return Math.min(damage, cap);
    }

    public void takeDamageWithAsh(int damage, Object damageSource) {
        if (!isAlive() || vulnerabilityState == VulnerabilityType.INVULNERABLE) return;
        ashDeath = true;
        try {
            takeDamage(damage, damageSource);
        } finally {
            if (isAlive()) ashDeath = false;
        }
    }

    private void handleDeath(GameSession session, String killerName, boolean firedDeath) {
        handleDeath(session, killerName, firedDeath, false, false);
    }

    private void handleDeath(GameSession session, String killerName, boolean firedDeath, boolean ashDeath) {
        handleDeath(session, killerName, firedDeath, ashDeath, false);
    }

    private void handleDeath(GameSession session, String killerName, boolean firedDeath,
                             boolean ashDeath, boolean shockDeath) {
        if (deathHandled) return;
        deathHandled = true;
        zombieState = ZombieState.DEAD;
        this.firedDeath = firedDeath;
        this.ashDeath = this.ashDeath || ashDeath;
        this.shockDeath = shockDeath;
        setHP(0);
        if (sunBeanCarrierValue > 0 && session != null && getPosition() != null) {
            session.getItems().add(new model.collections.item.GroundSun(getPosition(), sunBeanCarrierValue));
            sunBeanCarrierValue = 0;
        }
        if (isGlowing) plantFoodPending = true;
        if (zombieEffectStatus != null) zombieEffectStatus.onDeath(this, session);
        if (session != null) session.notifyZombieDied(this, killerName);
    }

    /**
     * Whether the given damage source was fire-based (e.g. a Fire Peashooter
     * pea), used to decide if a zombie's death should play the ash-death
     * animation instead of its normal die animation. Mirrors the same
     * fire-source check used elsewhere for ice/obstacle fire damage.
     */
    private boolean isFireDamageSource(Object damageSource) {
        if (FrostbiteFreezing.isFireDamageSource(damageSource)) return true;
        if (damageSource instanceof Projectile projectile) {
            HitEffectStrategy strategy = projectile.getHitEffectStrategy();
            return strategy != null && strategy.isFireDamage();
        }
        return false;
    }

    /**
     * Whether the killing hit came from Electric Blueberry's electric projectile.
     * The renderer uses this flag to play the zombie-specific shock death before
     * falling back to the normal/ash death sequence.
     */
    private boolean isShockDamageSource(Object damageSource) {
        if (!(damageSource instanceof Projectile projectile)) return false;
        Plant sourcePlant = projectile.getSourcePlant();
        return sourcePlant != null
                && "Electric Blueberry".equalsIgnoreCase(sourcePlant.getName());
    }

    private void updateStatus(double deltaTimeSeconds) {
        if (status == Status.NORMAL || status == Status.HYPNOTIZED) return;
        statusTimer = Math.max(0, statusTimer - deltaTimeSeconds);
        if (status == Status.POISONED) {
            statusDamageAccumulator += deltaTimeSeconds;
            while (statusDamageAccumulator >= 1.0 && isAlive()) {
                statusDamageAccumulator -= 1.0;
                takeDamage(15, true);
            }
        }
        if (statusTimer <= 0 && isAlive()) {
            status = Status.NORMAL;
            statusDamageAccumulator = 0;
        }
    }

    private String normalizePlantName(String plantName) {
        return plantName == null ? "" : plantName.toLowerCase().replace("-", "").replace("_", "").replace(" ", "");
    }

    private String resolveKillerName(Object damageSource) {
        if (damageSource instanceof Plant plant) {
            return plant.getName();
        }
        if (damageSource instanceof Projectile projectile && projectile.getSourcePlant() != null) {
            return projectile.getSourcePlant().getName();
        }
        return "Unknown";
    }

    @Override
    public void tick() {

    }

    public void tick(double deltaTimeSeconds, GameSession session) {
        if (!isAlive()) {
            handleDeath(session, "Unknown", status == Status.FIRED);
            return;
        }

        if (GameSession.peekInstance() != null && FrostbiteFreezing.isFrozenInIce(GameSession.peekInstance(), this)) return;

        updateStatus(deltaTimeSeconds);
        if (!isAlive()) {
            handleDeath(session, status == Status.POISONED ? "Poison" : "Fire", status == Status.FIRED);
            return;
        }

        updateActionAnimation(deltaTimeSeconds);
        updateShieldFlash(deltaTimeSeconds);
        updateSunBeanCarrier(deltaTimeSeconds, session);

        ZombieFactory.respawnPushedStructureIfNeeded(this);

        if (stunProfile != null) stunProfile.update(this);

        if (zombieEffectStatus != null) {
            zombieEffectStatus.applyTickEffect(this, session);
        }

        if (knockbackRemaining > 0.0) {
            double stepTime = Math.min(deltaTimeSeconds, knockbackRemaining);
            Position pos = getPosition();
            if (pos != null) {
                setPosition(new Position(
                        pos.x() + knockbackVelocityX * stepTime,
                        pos.y()));
            }
            knockbackRemaining = Math.max(0.0, knockbackRemaining - stepTime);
            if (knockbackRemaining <= 0.0) {
                knockbackVelocityX = 0.0;
            }
            return;
        }

        // A boss never eats or walks on its own: ZombossFight owns its position and its
        // whole moveset, so the ordinary target/attack/move pass is skipped for it.
        if (boss) return;

        // A scripted beat (stun, shield cast, laser, imp cannon, imp landing) owns the
        // zombie while it plays: it holds its ground and does not bite.
        if (sequence != null) {
            if (sequence.tick(this, session, deltaTimeSeconds)) {
                zombieState = ZombieState.WALKING;
                return;
            }
            sequence = null;
        }

        Item target = ignoreTargetAcquisition ? null : acquireTarget(session);
        if (target != null && target.isAlive()) {
            zombieState = ZombieState.EATING;
            if (moveBehavior instanceof model.collections.zombie.zombie_move.SnorkelMove) {
                vulnerabilityState = VulnerabilityType.FULLY_VULNERABLE;
            }

            if (status != Status.BUTTER && status != Status.FROZEN) {
                if (attackBehavior != null) {
                    attackBehavior.attack(this, session);
                } else {
                    dealDamage(target);
                }
            }
        } else {
            zombieState = ZombieState.WALKING;
            // FROZEN (and BUTTER) halt movement entirely; FREEZE only slows it. This has to
            // be handled here rather than inside move(double) below, since that method is a
            // dead-code fallback - moveBehavior is set for effectively every zombie, so
            // NormalWalk/PusherMove/etc. are what actually run, and none of them look at
            // status on their own.
            if (status != Status.BUTTER && status != Status.FROZEN && !immobilized) {
                if (moveBehavior != null) {
                    double scaledDeltaTime = status == Status.FREEZE ? deltaTimeSeconds * 0.5 : deltaTimeSeconds;
                    moveBehavior.move(this, scaledDeltaTime, session);
                } else {
                    move(deltaTimeSeconds);
                }
            }
        }
    }

    private void updateActionAnimation(double deltaTimeSeconds) {
        if (actionAnimationState == null) return;
        actionAnimationElapsed += deltaTimeSeconds;
        if (actionAnimationDuration > 0 && actionAnimationElapsed >= actionAnimationDuration) {
            clearActionAnimationState();
        }
    }

    /**
     * Plays a one-off or looping visual-only animation state (e.g. "toss",
     * "push", "cast", "cast_loop", "reel") on top of walk/eat/die.
     *
     * @param state    the animation clip name to prefer, or null to clear it.
     * @param duration how long (seconds) to keep showing it before automatically
     *                 reverting to the default walk/eat resolution; pass 0 (or
     *                 less) for a state that should persist until explicitly
     *                 cleared with {@link #clearActionAnimationState()}.
     * @param loop     whether the clip should loop (e.g. a continuous "push"
     *                 while shoving a structure) or play once and hold its
     *                 last frame (e.g. a single "toss"/"cast"/"reel" beat).
     */
    public void setActionAnimationState(String state, double duration, boolean loop) {
        if (state == null) {
            clearActionAnimationState();
            return;
        }
        if (!state.equals(actionAnimationState)) {
            actionAnimationState = state;
            actionAnimationElapsed = 0;
        }
        actionAnimationDuration = duration;
        actionAnimationLoop = loop;
    }

    public boolean acceptsAttackFrom(Object damageSource) {
        if (vulnerabilityState != VulnerabilityType.SUBMERGED
                && vulnerabilityState != VulnerabilityType.AIRBORNE) {
            return true;
        }
        if (damageSource instanceof Plant plant) {
            return isListedReacher(plant.getName());
        }
        if (damageSource instanceof Projectile projectile) {
            if (projectile.isLobbed()) return true;
            Plant source = projectile.getSourcePlant();
            return source != null && isListedReacher(source.getName());
        }
        return false;
    }

    private boolean isListedReacher(String plantName) {
        return damageWhileSubmerged != null
                && damageWhileSubmerged.contains(normalizePlantName(plantName));
    }

    public boolean isAirborne() {
        return vulnerabilityState == VulnerabilityType.AIRBORNE;
    }

    public void playSequence(ZombieSequence next) {
        this.sequence = next != null && !next.isEmpty() ? next : null;
        if (this.sequence == null) {
            clearActionAnimationState();
            return;
        }
        this.sequence.tick(this, GameSession.peekInstance(), 0);
    }

    public boolean isSequenceActive() {
        return sequence != null && !sequence.isFinished();
    }

    public ZombieStunProfile getStunProfile() { return stunProfile; }
    public void setStunProfile(ZombieStunProfile stunProfile) { this.stunProfile = stunProfile; }

    public boolean isStunTriggered() {
        return stunProfile != null && stunProfile.isTriggered();
    }

    public boolean isStunCompleted() {
        return stunProfile != null && stunProfile.isCompleted();
    }

    public boolean isImmobilized() { return immobilized; }
    public void setImmobilized(boolean immobilized) { this.immobilized = immobilized; }

    public void applyShield(int amount) {
        if (amount <= 0 || !isAlive()) return;
        shieldHp += amount;
        shieldMaxHp = Math.max(shieldMaxHp, shieldHp);
    }

    public boolean hasShield() { return shieldHp > 0 && isAlive(); }

    public int getShieldHp() { return shieldHp; }

    public double getShieldFraction() {
        return shieldMaxHp <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, shieldHp / (double) shieldMaxHp));
    }

    public double getShieldHitFlash() { return shieldHitFlash; }

    private static final double SHIELD_HIT_FLASH_SECONDS = 0.35;

    private int absorbWithShield(int damage) {
        if (shieldHp <= 0 || damage <= 0) return damage;
        shieldHitFlash = SHIELD_HIT_FLASH_SECONDS;
        if (damage < shieldHp) {
            shieldHp -= damage;
            return 0;
        }
        int leftover = damage - shieldHp;
        shieldHp = 0;
        shieldMaxHp = 0;
        return leftover;
    }

    private void updateShieldFlash(double deltaTimeSeconds) {
        if (shieldHitFlash > 0) shieldHitFlash = Math.max(0, shieldHitFlash - deltaTimeSeconds);
    }

    public double getHoverHeight() { return hoverHeight; }

    public void setHoverHeight(double hoverHeight) {
        this.hoverHeight = Math.max(0.0, hoverHeight);
    }

    public void clearActionAnimationState() {
        actionAnimationState = null;
        actionAnimationElapsed = 0;
        actionAnimationDuration = 0;
        actionAnimationLoop = false;
    }

    public String getActionAnimationState() { return actionAnimationState; }
    public double getActionAnimationElapsed() { return actionAnimationElapsed; }
    public boolean isActionAnimationLoop() { return actionAnimationLoop; }

    public void startKnockback(double distance, double durationSeconds) {
        if (boss || durationSeconds <= 0.0 || Math.abs(distance) < 0.0001) return;
        this.knockbackVelocityX = distance / durationSeconds;
        this.knockbackRemaining = durationSeconds;
    }

    public boolean isBeingKnockedBack() {
        return knockbackRemaining > 0.0;
    }

    /** 0 = not being dragged under, 1 = fully sunk beneath the water ripple. */
    public double getDragUnderWaterProgress() { return dragUnderWaterProgress; }

    /** Cosmetic only: pushes the renderer's draw position further under the fixed water
     * clip line without moving the zombie's actual grid position (or the ripple, which is
     * drawn off that grid position and so stays put). */
    public void setDragUnderWaterProgress(double progress) {
        this.dragUnderWaterProgress = Math.max(0.0, Math.min(1.0, progress));
    }

    /** Marks that this zombie's upcoming death was a Tangle Kelp drag-under-water kill, so
     * the renderer skips the normal splash/particle "die" playback - the zombie has already
     * visually vanished beneath the ripple by the time it actually dies. */
    public void markDragUnderWaterDeath() { this.dragUnderWaterDeath = true; }

    public boolean diedFromDragUnderWater() { return dragUnderWaterDeath; }

    /** Marks a Swashbuckler that failed its rope swing and fell into open water. */
    public void markSwashbucklerWaterDeath() { this.swashbucklerWaterDeath = true; }

    /** True when this zombie's death should use the Swashbuckler water-fall sequence. */
    public boolean diedFromSwashbucklerWater() { return swashbucklerWaterDeath; }


    public void move(double deltaTimeSeconds) {
        Position pos = getPosition();
        Position vel = getSpeed();
        if (pos == null || vel == null) return;

        double speedMultiplier = (status == Status.FREEZE) ? 0.5
                : (status == Status.BUTTER || status == Status.FROZEN) ? 0 : 1.0;
        setPosition(new Position(
                pos.x() + vel.x() * deltaTimeSeconds * speedMultiplier,
                pos.y() + vel.y() * deltaTimeSeconds * speedMultiplier
        ));
    }

    public Item acquireTarget(GameSession session) {
        return ZombieTargeting.findTarget(this, session);
    }

    public void hypnotize() {
        if (boss || faction == Faction.PLANTS || !isAlive()) return;
        this.faction = Faction.PLANTS;
        this.status = Status.HYPNOTIZED;
        this.statusTimer = 0;

        Position speed = getSpeed();
        if (speed != null) {
            setSpeed(new Position(-speed.x(), -speed.y()));
        }
        // Hypnotized zombies turn around and walk back the other way, so their
        // sprite needs to face the opposite direction too.
        this.isFacingRight = !this.isFacingRight;

        if (this.moveBehavior != null) {
            this.moveBehavior = new HypnotizedMoveBehavior(this.moveBehavior);
        }
    }

    public MoveBehavior getMoveBehavior() { return moveBehavior; }
    public void setMoveBehavior(MoveBehavior moveBehavior) { this.moveBehavior = moveBehavior; }

    public AttackBehavior getAttackBehavior() { return attackBehavior; }
    public void setAttackBehavior(AttackBehavior attackBehavior) { this.attackBehavior = attackBehavior; }

    public DefenseBehavior getDefenseBehavior() { return defenseBehavior; }
    public void setDefenseBehavior(DefenseBehavior defenseBehavior) { this.defenseBehavior = defenseBehavior; }

    public void markSunBeanCarrier(int sunValue) {
        if (sunValue > 0 && sunBeanCarrierValue <= 0) {
            sunBeanCarrierValue = sunValue;
            sunBeanTimer = 0.0;
        }
    }

    private static final double SUN_BEAN_INTERVAL_SECONDS = 5.0;

    private void updateSunBeanCarrier(double deltaTimeSeconds, GameSession session) {
        if (sunBeanCarrierValue <= 0 || session == null || getPosition() == null) return;
        sunBeanTimer += deltaTimeSeconds;
        while (sunBeanTimer >= SUN_BEAN_INTERVAL_SECONDS) {
            sunBeanTimer -= SUN_BEAN_INTERVAL_SECONDS;
            session.getItems().add(new model.collections.item.GroundSun(
                    getPosition(), sunBeanCarrierValue, true));
        }
    }

    public boolean isSunBeanCarrier() {
        return sunBeanCarrierValue > 0 && isAlive();
    }

    public int getSunBeanCarrierValue() {
        return sunBeanCarrierValue;
    }

    public ZombieEffectStatus getEffectStatus() { return zombieEffectStatus; }
    public void setEffectStatus(ZombieEffectStatus zombieEffectStatus) { this.zombieEffectStatus = zombieEffectStatus; }

    public Faction getFaction() { return faction; }
    public void setFaction(Faction faction) { this.faction = faction; }
    public boolean isHypnotized() { return faction == Faction.PLANTS; }
    public boolean isPlantFoodPending() { return plantFoodPending; }
    public void clearPlantFoodPending() { plantFoodPending = false; }
    public VulnerabilityType getVulnerabilityState() { return vulnerabilityState; }
    public void setVulnerabilityState(VulnerabilityType state) { this.vulnerabilityState = state; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getHp() { return getHP(); }
    public void setHp(int hp) {
        setHP(hp);
        if (this.maxHp <= 0) {
            this.maxHp = hp;
        }
    }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public double getEatDps() {
        if ("ZombieNewspaper".equals(name) && (armour == null || armour.getHP() <= 0)) return eatDps * 4.0;
        return eatDps;
    }
    public void setEatDps(double eatDps) { this.eatDps = eatDps; }
    public ZombieRace getRace() { return race; }
    public void setRace(ZombieRace race) { this.race = race; }
    public ZombieState getZombieState() { return zombieState; }
    public void setZombieState(ZombieState zombieState) { this.zombieState = zombieState; }
    public void setStatusWithoutEffects(Status status) { this.status = status; }
    /** True when this zombie's death was caused by fire (fire pea hit, or dying while ablaze). */
    public boolean diedFromFire() { return firedDeath; }
    public boolean diedFromAsh() { return ashDeath; }
    public boolean diedFromShock() { return shockDeath; }
    public Armour getArmor() { return armour; }
    public void setArmor(Armour armour) { this.armour = armour; }
    public Armour getArmour() { return armour; }
    public void setArmour(Armour armour) { this.armour = armour; }
    public boolean isGlowing() { return isGlowing; }
    public String getAlias() { return name; }
    public Status getStatus() { return this.status; }
    public void setStatus(Status status) {
        double duration = switch (status) {
            case FREEZE, FROZEN -> 5.0;
            case FIRED -> 3.0;
            case POISONED -> 6.0;
            case BUTTER -> 4.0;
            default -> 0.0;
        };
        applyStatus(status, duration);
    }
    public void applyStatus(Status status, double duration) {
        if (status == null || !isAlive()) return;
        if (boss && (status == Status.HYPNOTIZED || status == Status.BUTTER
                || status == Status.FREEZE || status == Status.FROZEN)) {
            return;
        }
        if (status == Status.HYPNOTIZED) {
            hypnotize();
            return;
        }
        if (status == Status.FREEZE && this.status == Status.FROZEN && statusTimer > 0) {
            return;
        }
        if (status == Status.FIRED) {
            if (zombieEffectStatus instanceof FireEffect fireEffect) fireEffect.setActiveFlame(true);
            if (moveBehavior instanceof ProspectorMove prospectorMove) prospectorMove.litDynamite();
        } else if (status == Status.FREEZE || status == Status.FROZEN) {
            if (zombieEffectStatus instanceof FireEffect fireEffect) fireEffect.setActiveFlame(false);
            if (moveBehavior instanceof ProspectorMove prospectorMove) prospectorMove.extinguishDynamite();
        }
        this.status = status;
        this.statusTimer = Math.max(0, duration);
        this.statusDamageAccumulator = 0;
    }
    public boolean isFromNecromancy() {
        return fromNecromancy;
    }

    public void setFromNecromancy(boolean fromNecromancy) {
        this.fromNecromancy = fromNecromancy;
    }
    public boolean isFacingRight() { return isFacingRight; }
    public void setIgnoreTargetAcquisition(boolean ignore) { this.ignoreTargetAcquisition = ignore; }
    public boolean isIgnoringTargetAcquisition() { return ignoreTargetAcquisition; }
    public void setFacingRight(boolean facingRight) { isFacingRight = facingRight; }
    public boolean hasPlantFood() { return hasPlantFood; }
    public void setHasPlantFood(boolean hasPlantFood) { this.hasPlantFood = hasPlantFood; }

    public PushableStructure getPushedStructure() { return pushedStructure; }
    public void setPushedStructure(PushableStructure pushedStructure) { this.pushedStructure = pushedStructure; }
    public int getPushableRespawnsRemaining() { return pushableRespawnsRemaining; }
    public void setPushableRespawnsRemaining(int remaining) { this.pushableRespawnsRemaining = remaining; }

    public void setDamageWhileSubmerged(List<String> list) { this.damageWhileSubmerged = list; }
    public void setDamageWhileSubmergedPlantfoodOnly(List<String> list) { this.damageWhileSubmergedPlantfoodOnly = list; }
}