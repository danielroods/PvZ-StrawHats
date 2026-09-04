package model.match.main.season.travellog.pirate;

import model.match.main.season.Season;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.obstacles.Grave;
import model.utils.GameSession;

import java.util.Random;

public class Pirate extends Season {
    private static final int GRAVE_COUNT = 3;
    private static final Random RANDOM = new Random();

    public Pirate() {
        super("Pirates");
    }



    
    @Override
    public void placeSeasonObstacles(GameSession session) {
        if (session == null || session.getEnvironment() == null) return;
        Environment env = session.getEnvironment();

        int placed = 0;
        int attempts = 0;
        while (placed < GRAVE_COUNT && attempts < 100) {
            attempts++;
            int row = RANDOM.nextInt(env.getRows());
            int col = RANDOM.nextInt(env.getCols());
            Cell cell = env.getCell(row, col);
            if (cell != null && cell.getObstacle() == null && !cell.hasPlant()) {
                cell.setObstacle(new Grave());
                placed++;
            }
        }
    }
}