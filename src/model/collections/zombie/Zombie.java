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
import model.projectile.ArcMove;
import model.projectile.Projectile;
import model.projectile.hit.HitEffectStrategy;
import model.utils.GameSession;

import java.util.List;
import java.util.Random;

public class Zombie extends Item implements Attack {
    private static final Random RAND = new Random();

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
    private VulnerabilityType vulnerabilityState = VulnerabilityType.FULLY_VULNERABLE;
    private Faction faction = Faction.ZOMBIES;
    private boolean fromNecromancy;

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

        if (this.vulnerabilityState == VulnerabilityType.SUBMERGED) {
            boolean allowDamage = false;

            if (damageSource instanceof Plant plant) {
                String plantName = normalizePlantName(plant.getName());

                if (damageWhileSubmerged != null && damageWhileSubmerged.contains(plantName)) {
                    allowDamage = true;
                }

            } else if (damageSource instanceof Projectile p) {
                if (p.getMoveStrategy() instanceof ArcMove) {
                    allowDamage = true;
                } else if (p.getSourcePlant() != null) {
                    String plantName = normalizePlantName(p.getSourcePlant().getName());
                    allowDamage = damageWhileSubmerged != null && damageWhileSubmerged.contains(plantName);
                }
            }

            if (!allowDamage) return;
        }

        int actualDamage = damage;
        if (this.defenseBehavior != null) {
            actualDamage = this.defenseBehavior.handleDamage(this, damage, damageSource, GameSession.getInstance());
        }

        if (actualDamage > 0) {
            applyDamageCalculations(actualDamage, damageSource);
        }
    }

    private void applyDamageCalculations(int damage, Object damageSource) {
        int remaining = (armour != null && armour.getHP() > 0) ? armour.absorbDamage(damage) : damage;
        if (remaining <= 0) return;

        int newHp = Math.max(0, getHP() - remaining);
        setHP(newHp);

        if (newHp <= 0) {
            boolean diedFromFire = isFireDamageSource(damageSource) || status == Status.FIRED;
            handleDeath(GameSession.peekInstance(), resolveKillerName(damageSource), diedFromFire);
        }
    }

    private void handleDeath(GameSession session, String killerName, boolean firedDeath) {
        if (deathHandled) return;
        deathHandled = true;
        zombieState = ZombieState.DEAD;
        this.firedDeath = firedDeath;
        setHP(0);
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

        ZombieFactory.respawnPushedStructureIfNeeded(this);

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

        Item target = acquireTarget(session);
        if (target != null && target.isAlive()) {
            zombieState = ZombieState.EATING;
            if (moveBehavior instanceof model.collections.zombie.zombie_move.SnorkelMove) {
                vulnerabilityState = VulnerabilityType.FULLY_VULNERABLE;
            }
            // Butter-stunned zombies hold whatever they were doing (here,
            // about to eat) but don't actually act until it wears off.
            if (status != Status.BUTTER) {
                if (attackBehavior != null) {
                    attackBehavior.attack(this, session);
                } else {
                    dealDamage(target);
                }
            }
        } else {
            zombieState = ZombieState.WALKING;
            if (status != Status.BUTTER) {
                if (moveBehavior != null) {
                    moveBehavior.move(this, deltaTimeSeconds, session);
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
        if (durationSeconds <= 0.0 || Math.abs(distance) < 0.0001) return;
        this.knockbackVelocityX = distance / durationSeconds;
        this.knockbackRemaining = durationSeconds;
    }

    public boolean isBeingKnockedBack() {
        return knockbackRemaining > 0.0;
    }

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
        if (faction == Faction.PLANTS || !isAlive()) return;
        this.faction = Faction.PLANTS;
        this.status = Status.HYPNOTIZED;
        this.statusTimer = 0;

        Position speed = getSpeed();
        if (speed != null) {
            setSpeed(new Position(-speed.x(), -speed.y()));
        }

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
    /** True when this zombie's death was caused by fire (fire pea hit, or dying while ablaze). */
    public boolean diedFromFire() { return firedDeath; }
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
        if (status == Status.HYPNOTIZED) {
            hypnotize();
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