package model.match.boss;

import model.collections.animations.AnimationFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.boss.behavior.BeachZombossBehavior;
import model.match.boss.behavior.DarkAgeZombossBehavior;
import model.match.boss.behavior.EgyptZombossBehavior;
import model.match.boss.behavior.IceAgeZombossBehavior;
import model.match.boss.behavior.ZombossBehavior;
import model.match.main.levels.Level;
import model.match.waves.LaneBag;
import model.match.waves.SpawnPlacement;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ZombossFight {

    public static final double SILENCE_SECONDS = 5.0;

    private static final int BOSS_SPAWN_ROW = 2;
    private static final int BOSS_SPAWN_COL = 7;

    private static final double[] STUN_HEALTH_FRACTIONS = {0.75, 0.5, 0.25};
    private static final double STUN_LOOP_SECONDS = 8.0;
    private static final double STUN_DAMAGE_MULTIPLIER = 2.5;
    private static final double VULNERABLE_DAMAGE_MULTIPLIER = 2.0;

    private static final double SPAWN_INTERVAL_START = 14.0;
    private static final double SPAWN_INTERVAL_MIN = 7.5;
    private static final double SPAWN_RAMP_SECONDS = 120.0;
    private static final double SPAWN_FIRST_DELAY = 10.0;
    private static final double SPAWN_DOUBLE_AFTER_SECONDS = 150.0;
    private static final int MAX_ACTIVE_MINIONS = 9;
    private static final int SPAWN_DOUBLE_HEADROOM = 3;
    private static final double SUMMON_SPAWN_PAUSE = 7.0;

    private static final double OPENING_GRACE_SECONDS = 6.0;

    private final GameSession session;
    private final ZombossChapter chapter;
    private final ZombossBehavior behavior;
    private final Random random = new Random();
    private final LaneBag laneBag;
    private final List<String> minionPool = new ArrayList<>();
    private final List<String> weightedPool = new ArrayList<>();
    private final List<String> minionDeck = new ArrayList<>();
    private int deckIndex;
    private boolean firstDeckDealt;

    private final List<ZombossSkyStrike> skyStrikes = new ArrayList<>();
    private final List<ZombossRowEffect> rowEffects = new ArrayList<>();

    private Zombie boss;
    private ZombossPhase phase = ZombossPhase.SILENCE;
    private double phaseElapsed;

    private int dialogueIndex;

    private ZombossActionSequence action;
    private double actionCooldown;
    private double battleElapsed;

    private int stunsTriggered;
    private boolean stunned;
    private boolean vulnerable;
    private double queuedRecovery;

    private double spawnTimer;

    private ZombossActionSequence deathSequence;

    public ZombossFight(GameSession session, ZombossChapter chapter) {
        this.session = session;
        this.chapter = chapter;
        this.laneBag = new LaneBag(session == null ? 5 : session.getRows(), random);
        this.behavior = createBehavior(chapter);
        this.spawnTimer = SPAWN_FIRST_DELAY * chapter.getSpawnPacing();
        buildMinionPool();
    }

    private ZombossBehavior createBehavior(ZombossChapter forChapter) {
        return switch (forChapter) {
            case EGYPT -> new EgyptZombossBehavior(this);
            case ICE_AGE -> new IceAgeZombossBehavior(this);
            case BEACH -> new BeachZombossBehavior(this);
            case DARK_AGES -> new DarkAgeZombossBehavior(this);
        };
    }

    private void buildMinionPool() {
        Level level = session == null ? null : session.getLevel();
        if (level == null || level.getZombiePool() == null) return;
        for (String alias : level.getZombiePool()) {
            if (alias == null || alias.equalsIgnoreCase(chapter.getAlias())) continue;
            minionPool.add(alias);
            for (int i = 0; i < minionWeight(alias); i++) weightedPool.add(alias);
        }
    }

    private int minionWeight(String alias) {
        int threat;
        try {
            if (ZombieFactory.isStationaryMover(alias)) return 1;
            threat = ZombieFactory.getZombieCost(alias);
        } catch (Exception e) {
            return 1;
        }
        if (threat <= 300) return 3;
        if (threat <= 800) return 2;
        return 1;
    }

    private String drawMinionAlias() {
        if (minionPool.isEmpty()) return null;
        if (deckIndex >= minionDeck.size()) {
            minionDeck.clear();
            if (firstDeckDealt) {
                minionDeck.addAll(weightedPool);
                Collections.shuffle(minionDeck, random);
            } else {
                minionDeck.addAll(minionPool);
                dealOpeningDeckByThreat();
                firstDeckDealt = true;
            }
            deckIndex = 0;
        }
        return minionDeck.get(deckIndex++);
    }

    private void dealOpeningDeckByThreat() {
        minionDeck.sort(java.util.Comparator.comparingDouble(this::rampScore));
    }

    private double rampScore(String alias) {
        int threat;
        try {
            threat = ZombieFactory.getZombieCost(alias);
        } catch (Exception e) {
            threat = 100;
        }
        return threat * (0.75 + random.nextDouble() * 0.5);
    }

    public GameSession getSession() { return session; }

    public ZombossChapter getChapter() { return chapter; }

    public ZombossBehavior getBehavior() { return behavior; }

    public ZombossPhase getPhase() { return phase; }

    public double getPhaseElapsed() { return phaseElapsed; }

    public Zombie getBoss() { return boss; }

    public Random getRandom() { return random; }

    public boolean isCutscene() { return phase.isCutscene(); }

    public boolean isFinished() { return phase == ZombossPhase.FINISHED; }

    public boolean isBattleActive() { return phase == ZombossPhase.BATTLE; }

    public boolean isStunned() { return stunned; }

    public List<ZombossSkyStrike> getSkyStrikes() {
        return Collections.unmodifiableList(skyStrikes);
    }

    public List<ZombossRowEffect> getRowEffects() {
        return Collections.unmodifiableList(rowEffects);
    }

    public ZombossActionSequence getAction() { return action; }

    public double getBattleElapsed() { return battleElapsed; }

    public Position getBossPosition() {
        return boss == null ? new Position(BOSS_SPAWN_COL, BOSS_SPAWN_ROW) : boss.getPosition();
    }

    public double getBossHealthFraction() {
        if (boss == null || boss.getMaxHp() <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, boss.getHP() / (double) boss.getMaxHp()));
    }

    public String getDialogueLine() {
        if (phase != ZombossPhase.NPC_TALK) return null;
        List<String> lines = chapter.getDialogue();
        if (dialogueIndex < 0 || dialogueIndex >= lines.size()) return null;
        return lines.get(dialogueIndex);
    }

    public int getDialogueIndex() { return dialogueIndex; }

    public int getDialogueCount() { return chapter.getDialogue().size(); }

    public String getNpcClip() {
        return switch (phase) {
            case NPC_ENTER -> ZombossChapter.NPC_ENTER_CLIP;
            case NPC_TALK -> ZombossChapter.NPC_TALK_CLIP;
            case NPC_EXIT -> ZombossChapter.NPC_EXIT_CLIP;
            default -> null;
        };
    }

    public double getNpcClipTime() {
        String clip = getNpcClip();
        if (clip == null) return 0.0;
        double length = AnimationFactory.exactClipDurationForPath(ZombossChapter.NPC_PAM, clip);
        if (ZombossChapter.NPC_TALK_CLIP.equals(clip) && length > 0) return phaseElapsed % length;
        return length > 0 ? Math.min(phaseElapsed, length) : phaseElapsed;
    }

    public String getBossClip() {
        if (phase == ZombossPhase.SILENCE) return null;
        if (phase == ZombossPhase.BOSS_INTRO) return ZombossChapter.INTRO_CLIP;
        // Once die_exit has played out the boss has left; nothing more is drawn for it.
        if (phase == ZombossPhase.FINISHED) return null;
        if (phase == ZombossPhase.DEFEATED) {
            return deathSequence == null ? null : deathSequence.getCurrentClip();
        }
        if (action != null && !action.isFinished()) return action.getCurrentClip();
        return behavior.idleClip();
    }

    public double getBossClipTime() {
        if (phase == ZombossPhase.BOSS_INTRO) {
            double length = AnimationFactory.exactClipDurationForPath(
                    chapter.getBossPam(), ZombossChapter.INTRO_CLIP);
            return length > 0 ? Math.min(phaseElapsed, length) : phaseElapsed;
        }
        if (phase == ZombossPhase.DEFEATED || phase == ZombossPhase.FINISHED) {
            return deathSequence == null ? 0.0 : deathSequence.getCurrentClipTime();
        }
        if (action != null && !action.isFinished()) return action.getCurrentClipTime();
        double length = AnimationFactory.exactClipDurationForPath(
                chapter.getBossPam(), behavior.idleClip());
        return length > 0 ? battleElapsed % length : battleElapsed;
    }

    public ZombossActionSequence newAction(String name) {
        return new ZombossActionSequence(name, chapter.getBossPam());
    }

    public void startAction(ZombossActionSequence sequence) {
        this.action = sequence;
    }

    public boolean isIdleForAction() {
        return phase == ZombossPhase.BATTLE && !stunned
                && (action == null || action.isFinished()) && actionCooldown <= 0;
    }

    public void setActionCooldown(double seconds) {
        this.actionCooldown = Math.max(0.0, seconds);
        this.queuedRecovery = 0.0;
    }

    public void queueRecovery(double seconds) {
        this.queuedRecovery = Math.max(0.0, seconds);
    }

    public boolean isVulnerable() { return vulnerable; }

    public void setVulnerable(boolean open) {
        this.vulnerable = open;
        refreshDamageMultiplier();
    }

    private void refreshDamageMultiplier() {
        if (boss == null) return;
        double multiplier = 1.0;
        if (stunned) multiplier *= STUN_DAMAGE_MULTIPLIER;
        if (vulnerable) multiplier *= VULNERABLE_DAMAGE_MULTIPLIER;
        boss.setDamageTakenMultiplier(multiplier);
    }

    public void addSkyStrike(ZombossSkyStrike strike) {
        if (strike != null) skyStrikes.add(strike);
    }

    public void addRowEffect(ZombossRowEffect effect) {
        if (effect != null) rowEffects.add(effect);
    }

    public void moveBossTo(double col, double row) {
        if (boss != null) boss.setPosition(new Position(col, row));
    }

    public boolean hasMinionRoom() {
        return countMinions() < MAX_ACTIVE_MINIONS;
    }

    public Zombie spawnMinionAtBoss() {
        if (minionPool.isEmpty() || boss == null || boss.getPosition() == null) return null;
        if (!hasMinionRoom()) return null;
        String alias = drawMinionAlias();
        int row = (int) Math.round(boss.getPosition().y());
        int col = (int) Math.round(boss.getPosition().x());
        Zombie minion = createMinion(alias, row, col);
        if (minion == null) return null;
        minion.setPosition(new Position(boss.getPosition().x(), row));
        minion.setFromNecromancy(true);
        session.spawnZombie(minion);
        spawnTimer = Math.max(spawnTimer, SUMMON_SPAWN_PAUSE);
        return minion;
    }

    public Zombie spawnMinionAtEdge() {
        if (minionPool.isEmpty() || session == null) return null;
        if (!hasMinionRoom()) return null;
        String alias = drawMinionAlias();
        int cols = session.getCols();
        SpawnPlacement.Placement placement = SpawnPlacement.resolve(session.getZombies(), alias,
                cols, laneBag.preferenceOrder(laneBag.draw()), SpawnPlacement.entryX(cols));
        int lane = Math.max(0, Math.min(session.getRows() - 1, placement.lane()));
        Zombie minion = createMinion(alias, lane, Math.max(0, cols - 1));
        if (minion == null) return null;
        minion.setPosition(new Position(placement.x(), lane));
        session.spawnZombie(minion);
        return minion;
    }

    private Zombie createMinion(String alias, int row, int col) {
        try {
            return ZombieFactory.create(alias, row, col);
        } catch (Exception e) {
            GeneralPrinter.print("Zomboss could not summon " + alias + ": " + e.getMessage());
            return null;
        }
    }

    public int countMinions() {
        if (session == null) return 0;
        int count = 0;
        for (Zombie zombie : session.getZombies()) {
            if (zombie != null && zombie.isAlive() && zombie != boss) count++;
        }
        return count;
    }

    public void tick(double deltaSeconds) {
        phaseElapsed += Math.max(0.0, deltaSeconds);
        switch (phase) {
            case SILENCE -> {
                if (phaseElapsed >= SILENCE_SECONDS) {
                    spawnBoss();
                    enter(ZombossPhase.BOSS_INTRO);
                }
            }
            case BOSS_INTRO -> {
                double length = AnimationFactory.exactClipDurationForPath(
                        chapter.getBossPam(), ZombossChapter.INTRO_CLIP);
                if (phaseElapsed >= (length > 0 ? length : 3.0)) enter(ZombossPhase.NPC_ENTER);
            }
            case NPC_ENTER -> {
                double length = AnimationFactory.exactClipDurationForPath(
                        ZombossChapter.NPC_PAM, ZombossChapter.NPC_ENTER_CLIP);
                if (phaseElapsed >= (length > 0 ? length : 1.7)) {
                    dialogueIndex = 0;
                    enter(ZombossPhase.NPC_TALK);
                }
            }
            case NPC_TALK -> {
            }
            case NPC_EXIT -> {
                double length = AnimationFactory.exactClipDurationForPath(
                        ZombossChapter.NPC_PAM, ZombossChapter.NPC_EXIT_CLIP);
                if (phaseElapsed >= (length > 0 ? length : 0.7)) {
                    enter(ZombossPhase.BATTLE);
                    setActionCooldown(OPENING_GRACE_SECONDS);
                    behavior.onBattleStart();
                    GeneralPrinter.print("The battle against " + chapter.getSeasonName()
                            + " Zomboss has begun!");
                }
            }
            case BATTLE -> tickBattle(deltaSeconds);
            case DEFEATED -> tickDeath(deltaSeconds);
            default -> { }
        }
    }

    public void advanceDialogue() {
        if (phase != ZombossPhase.NPC_TALK) return;
        dialogueIndex++;
        if (dialogueIndex >= chapter.getDialogue().size()) {
            enter(ZombossPhase.NPC_EXIT);
        }
    }

    private void enter(ZombossPhase next) {
        phase = next;
        phaseElapsed = 0.0;
    }

    private void spawnBoss() {
        try {
            boss = ZombieFactory.create(chapter.getAlias(), BOSS_SPAWN_ROW, BOSS_SPAWN_COL);
        } catch (Exception e) {
            GeneralPrinter.print("Could not create Zomboss (" + chapter.getAlias() + "): "
                    + e.getMessage());
            enter(ZombossPhase.FINISHED);
            return;
        }
        boss.markAsBoss();
        boss.setFacingRight(false);
        boss.setPosition(new Position(BOSS_SPAWN_COL, BOSS_SPAWN_ROW));
        session.spawnZombie(boss);
        behavior.onBossSpawned();
        GeneralPrinter.print("Dr. Zomboss has arrived on the lawn!");
    }

    private void tickBattle(double deltaSeconds) {
        if (boss == null || !boss.isAlive()) {
            beginDeath();
            return;
        }
        battleElapsed += deltaSeconds;
        actionCooldown = Math.max(0.0, actionCooldown - deltaSeconds);

        tickSkyStrikes(deltaSeconds);
        tickRowEffects(deltaSeconds);

        if (action != null) {
            boolean wasFinished = action.isFinished();
            action.advance(deltaSeconds);
            if (!wasFinished && action.isFinished()) {
                behavior.onActionFinished(action);
                if ("stun".equals(action.getName())) {
                    endStun();
                } else if (queuedRecovery > 0) {
                    actionCooldown = Math.max(actionCooldown, queuedRecovery);
                    queuedRecovery = 0.0;
                }
            }
        }

        maybeTriggerStun();
        behavior.update(deltaSeconds);

        if (!stunned) tickMinionSpawning(deltaSeconds);
    }

    private void tickSkyStrikes(double deltaSeconds) {
        for (int i = skyStrikes.size() - 1; i >= 0; i--) {
            ZombossSkyStrike strike = skyStrikes.get(i);
            strike.tick(deltaSeconds, session);
            if (strike.isDone()) skyStrikes.remove(i);
        }
    }

    private void tickRowEffects(double deltaSeconds) {
        for (int i = rowEffects.size() - 1; i >= 0; i--) {
            ZombossRowEffect effect = rowEffects.get(i);
            effect.tick(deltaSeconds, session);
            if (effect.isDone()) rowEffects.remove(i);
        }
    }

    private void maybeTriggerStun() {
        if (stunned || stunsTriggered >= STUN_HEALTH_FRACTIONS.length) return;
        double health = getBossHealthFraction();
        if (health > STUN_HEALTH_FRACTIONS[stunsTriggered]) return;
        while (stunsTriggered < STUN_HEALTH_FRACTIONS.length
                && health <= STUN_HEALTH_FRACTIONS[stunsTriggered]) {
            stunsTriggered++;
        }
        beginStun();
    }

    public void beginStun() {
        stunned = true;
        skyStrikes.clear();
        rowEffects.clear();
        queuedRecovery = 0.0;
        ZombossActionSequence stun = newAction("stun")
                .then(chapter.getStunStartClip())
                .loop(chapter.getStunLoopClip(), STUN_LOOP_SECONDS)
                .then(chapter.getStunEndClip());
        startAction(stun);
        refreshDamageMultiplier();
        behavior.onStunStart();
        GeneralPrinter.print("Zomboss is stunned - hit it while the machine is open!");
    }

    private void endStun() {
        stunned = false;
        refreshDamageMultiplier();
        setActionCooldown(3.0);
        behavior.onStunEnd();
    }

    private void tickMinionSpawning(double deltaSeconds) {
        if (minionPool.isEmpty()) return;
        spawnTimer -= deltaSeconds;
        if (spawnTimer > 0) return;
        int active = countMinions();
        if (active < MAX_ACTIVE_MINIONS) {
            int count = battleElapsed > SPAWN_DOUBLE_AFTER_SECONDS
                    && active <= MAX_ACTIVE_MINIONS - SPAWN_DOUBLE_HEADROOM ? 2 : 1;
            for (int i = 0; i < count; i++) behavior.spawnPoolMinion();
        }
        double ramp = Math.min(1.0, battleElapsed / SPAWN_RAMP_SECONDS);
        spawnTimer = (SPAWN_INTERVAL_START
                + (SPAWN_INTERVAL_MIN - SPAWN_INTERVAL_START) * ramp)
                * chapter.getSpawnPacing();
    }

    private void beginDeath() {
        skyStrikes.clear();
        rowEffects.clear();
        stunned = false;
        vulnerable = false;
        behavior.onDefeated();
        deathSequence = newAction("death");
        for (String clip : chapter.getDeathClips()) deathSequence.then(clip);
        enter(ZombossPhase.DEFEATED);
        GeneralPrinter.print("Zomboss has been destroyed!");
    }

    private void tickDeath(double deltaSeconds) {
        if (deathSequence == null) {
            enter(ZombossPhase.FINISHED);
            return;
        }
        if (deathSequence.advance(deltaSeconds) || deathSequence.isFinished()) {
            enter(ZombossPhase.FINISHED);
        }
    }
}
