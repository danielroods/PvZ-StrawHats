package model.match.main.season.travellog.cave;

import model.collections.plant.Plant;
import model.pitches.Cell;
import model.pitches.Environment;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class IceWind {
    public static final String PAM_PATH_PLACEHOLDER = "768/FULL/EFFECTS/FROSTBITE_CHILL_WIND/FROSTBITE_CHILL_WIND.PAM";
    public static final String PAM_CLIP = "animation";
    public static final double EVENT_DURATION_SECONDS = 6.5;

    private static final Random RANDOM = new Random();
    private static final Map<GameSession, State> STATES = new IdentityHashMap<>();

    private static final class State {
        final List<Integer> rows;
        double elapsed;
        State(List<Integer> rows) { this.rows = rows; }
    }

    private IceWind() { }

    public static int blow(GameSession session) {
        if (session == null || session.getEnvironment() == null) return 0;
        Environment environment = session.getEnvironment();
        int maxRows = Math.max(1, Math.min(2, environment.getRows()));
        int count = 1 + RANDOM.nextInt(maxRows);
        List<Integer> rows = new ArrayList<>();
        for (int row = 0; row < environment.getRows(); row++) rows.add(row);
        Collections.shuffle(rows, RANDOM);
        rows = new ArrayList<>(rows.subList(0, Math.min(count, rows.size())));

        STATES.put(session, new State(rows));

        int affectedPlants = 0;
        for (int row : rows) {
            for (int col = 0; col < environment.getCols(); col++) {
                Cell cell = environment.getCell(row, col);
                Plant plant = cell == null ? null : cell.getPlant();
                if (FrostbiteFreezing.addChillLevel(session, plant)) affectedPlants++;
            }
        }
        return affectedPlants;
    }

    public static void tick(GameSession session, double deltaSeconds) {
        State state = STATES.get(session);
        if (state == null) return;
        state.elapsed += Math.max(0, deltaSeconds);
        if (state.elapsed >= EVENT_DURATION_SECONDS) STATES.remove(session);
    }

    public static void reset(GameSession session) {
        if (session != null) STATES.remove(session);
    }

    public static boolean isActive(GameSession session) { return STATES.containsKey(session); }
    public static double animationTime(GameSession session) {
        State state = STATES.get(session);
        return state == null ? -1.0 : state.elapsed;
    }
    public static List<Integer> activeRows(GameSession session) {
        State state = STATES.get(session);
        return state == null ? List.of() : List.copyOf(state.rows);
    }
}
