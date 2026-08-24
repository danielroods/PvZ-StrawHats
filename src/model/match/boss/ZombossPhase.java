package model.match.boss;


public enum ZombossPhase {
    
    SILENCE,
    
    BOSS_INTRO,
    
    NPC_ENTER,
    
    NPC_TALK,
    
    NPC_EXIT,
    
    BATTLE,
    
    DEFEATED,
    
    FINISHED;

    public boolean isCutscene() {
        return this == BOSS_INTRO || this == NPC_ENTER || this == NPC_TALK
                || this == NPC_EXIT || this == DEFEATED;
    }

    public boolean isBeforeBattle() {
        return ordinal() < BATTLE.ordinal();
    }
}
