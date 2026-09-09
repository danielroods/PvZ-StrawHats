package model.match.main.season.travellog.beach;

import model.match.main.levels.Level;
import model.match.main.season.Season;
import model.match_mechanisms.ZombieWave;
import model.utils.GameSession;


public class Beach extends Season {
    public static final int DEFAULT_MAX_TIDE_COLUMNS = 4;

    public Beach() {
        super("Big Wave Beach");
    }

    @Override
    public boolean hasTide() { return true; }

    
    @Override
    public void onWaveStart(GameSession session, int waveIndex) {
        if (session == null || session.getLevel() == null) return;
        Level level = session.getLevel();
        level.setMaxTideColumn(Math.min(DEFAULT_MAX_TIDE_COLUMNS, Math.max(0, level.getCols() - 1)));

        if ((waveIndex & 1) == 0) {
            Flood.riselevel(level, session);
        } else {
            Flood.falllevel(level, session);
        }
    }

    
    public boolean isBigWave(ZombieWave wave) {
        return wave != null && wave.isFinalWave();
    }
}
