package model.match.main.season;

import model.match.main.season.travellog.TravelLog;
import model.pitches.obstacles.ObstacleInformation;

import java.util.ArrayList;

public abstract class Season {
    protected String name;
    public TravelLog travelLog;
    public ArrayList<ObstacleInformation> obstacles = new ArrayList<>();

    public Season(String name) {
        this.name = name;
    }

    public String getName() { return name; }


    public boolean hasTide() { return false; }
    public boolean isNight() { return false; }

    public void applyPerTickEffect(model.utils.GameSession session, double deltaSeconds) {
        // default no-op
    }

    public void onWaveStart(model.utils.GameSession session, int waveIndex) {
        // default no-op
    }

    public void placeSeasonObstacles(model.utils.GameSession session) {
        // default no-op
    }
}