package model.match.endless;

import model.match.main.levels.Level;
import model.match.waves.WaveDirector;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Lottery node of a chapter, played as a true endless survival level.
 * <p>
 * It is a real {@link Level} - it carries its own chapter's season, board size, plant
 * list and zombie roster, so it loads the same map, hazards and assets every other stage
 * of that chapter does - but it has no authored wave list and no win condition. Waves
 * come from an {@link EndlessWaveDirector} that is asked for wave N when wave N is due,
 * so the schedule has no end to run out of.
 */
public class EndlessLevel extends Level {

    /**
     * Sky sun falls a little more often than in a normal match (a shorter interval means
     * more sun), because an endless run has to keep paying for replacements forever. It
     * scales the season's own rate rather than replacing it, so a night chapter that
     * drops no sky sun at all still drops none.
     */
    public static final double SKY_SUN_INTERVAL_MULTIPLIER = 0.8;

    private final EndlessChapter chapter;
    private final EndlessWaveDirector director;
    private EndlessRun run = new EndlessRun();

    public EndlessLevel(EndlessChapter chapter, List<String> roster, int cols, Random random) {
        this.chapter = chapter;
        this.director = new EndlessWaveDirector(roster, cols, random);
        setId(chapter.levelId());
        setName(chapter.levelName());
        setGameMode("Lottery - Endless");
        setZombiePool(new ArrayList<>(director.getRoster()));
        setWaves(new ArrayList<>());
    }

    public EndlessChapter getChapter() {
        return chapter;
    }

    public EndlessRun getRun() {
        return run;
    }

    @Override
    public WaveDirector getWaveDirector() {
        return director;
    }

    @Override
    public void initSpecial(GameSession session) {
        run = new EndlessRun();
        if (session == null) return;
        session.setSkySunIntervalMultiplier(SKY_SUN_INTERVAL_MULTIPLIER);
    }

    
    @Override
    public boolean checkWinCondition(GameSession session) {
        return false;
    }
}
