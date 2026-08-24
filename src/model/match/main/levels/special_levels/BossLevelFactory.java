package model.match.main.levels.special_levels;

import model.collections.zombie.ZombieFactory;
import model.match.boss.ZombossChapter;
import model.match.main.season.Season;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BossLevelFactory {

    public static BossLevel createBossLevel(int id, String name, Season season,
                                            int initialSun, List<String> availablePlants,
                                            List<String> zombiePool,
                                            ZombossChapter chapter) {
        if (chapter == null) {
            throw new IllegalArgumentException("a boss level needs a Zomboss chapter");
        }
        BossLevel level = new BossLevel();
        level.setId(id);
        level.setName(name);
        level.setSeason(season);
        level.setInitialSun(initialSun);
        level.setWaves(new ArrayList<>());
        level.setAvailablePlants(availablePlants == null
                ? Collections.emptyList() : availablePlants);
        level.setForcedPlants(Collections.emptyList());
        level.setZombiePool(zombiePool == null ? Collections.emptyList() : zombiePool);
        level.setChapter(chapter);
        level.setBossZombie(ZombieFactory.create(chapter.getAlias(), 0, level.getCols()));
        return level;
    }
}
