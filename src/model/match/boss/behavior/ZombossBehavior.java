package model.match.boss.behavior;

import model.match.boss.ZombossActionSequence;
import model.match.boss.ZombossChapter;
import model.match.boss.ZombossFight;
import model.utils.GameSession;

import java.util.Random;

public abstract class ZombossBehavior {

    protected final ZombossFight fight;

    private ZombossActionSequence trackedAction;
    private int lastStepIndex = -1;

    protected ZombossBehavior(ZombossFight fight) {
        this.fight = fight;
    }

    public abstract void update(double deltaSeconds);

    public String idleClip() {
        return ZombossChapter.IDLE_CLIP;
    }

    public void onBossSpawned() {
    }

    public void onBattleStart() {
    }

    public void onActionFinished(ZombossActionSequence sequence) {
    }

    public void onStunStart() {
    }

    public void onStunEnd() {
    }

    public void onDefeated() {
    }

    public void spawnPoolMinion() {
        fight.spawnMinionAtEdge();
    }

    protected GameSession session() {
        return fight.getSession();
    }

    protected Random random() {
        return fight.getRandom();
    }

    protected int consumeStepChange(ZombossActionSequence sequence) {
        if (sequence != trackedAction) {
            trackedAction = sequence;
            lastStepIndex = -1;
        }
        if (sequence == null) return -1;
        int index = sequence.getStepIndex();
        if (index != lastStepIndex) {
            lastStepIndex = index;
            return index;
        }
        return -1;
    }
}
