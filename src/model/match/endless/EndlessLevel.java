package model.match.endless;

import model.match.main.levels.Level;
import model.match.waves.WaveDirector;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class EndlessLevel extends Level {

    
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
