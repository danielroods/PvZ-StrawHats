package model.match.main.levels.special_levels;

import model.collections.zombie.Zombie;
import model.match.boss.ZombossChapter;
import model.match.boss.ZombossFight;
import model.match.main.levels.Level;
import model.utils.GameSession;

public class BossLevel extends Level {

    private Zombie bossZombie;
    private ZombossChapter chapter;
    private ZombossFight fight;

    public Zombie getBossZombie() { return bossZombie; }

    public void setBossZombie(Zombie bossZombie) { this.bossZombie = bossZombie; }

    public ZombossChapter getChapter() { return chapter; }

    public void setChapter(ZombossChapter chapter) { this.chapter = chapter; }

    public ZombossFight getFight() { return fight; }

    @Override
    public void initSpecial(GameSession session) {
        if (chapter == null && season != null) {
            chapter = ZombossChapter.fromSeason(season.getName());
        }
        fight = chapter == null ? null : new ZombossFight(session, chapter);
    }

    @Override
    public boolean checkWinCondition(GameSession session) {
        return fight != null && fight.isFinished();
    }
}
