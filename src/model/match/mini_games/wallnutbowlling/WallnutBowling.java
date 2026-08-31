package model.match.mini_games.wallnutbowlling;

import model.collections.item.GroundItem;
import model.collections.zombie.Zombie;
import model.match.mini_games.MiniGameMode;
import model.match.mini_games.MiniGameWaves;
import model.match.mini_games.wallnutbowlling.nut.BigNut;
import model.match.mini_games.wallnutbowlling.nut.BowlingWallnut;
import model.match.mini_games.wallnutbowlling.nut.ExplodeONut;
import model.match.mini_games.wallnutbowlling.nut.Nut;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import view.GeneralPrinter;

import java.util.*;

public class WallnutBowling extends MiniGameMode {
    private static final Random RAND = new Random();
    private static final double HIT_RADIUS_COLUMNS = 0.5;
    private static final double HIT_RADIUS_ROWS = 0.55;
    private static final double CONVEYOR_INTERVAL_SECONDS = 6.0;
    private static final int CONVEYOR_CAPACITY = 4;
    private static final int BOWLING_SHARE_PERCENT = 70;
    private static final int EXPLODE_SHARE_PERCENT = 20;

    public enum NutKind {
        BOWLING,
        EXPLODE,
        BIG
    }

    private final GameSession session;
    private final int redLineColumn;
    private final List<Nut> activeNuts = new ArrayList<>();
    private final Queue<NutKind> conveyorBelt = new ArrayDeque<>();
    private final Map<Nut, Set<Zombie>> hitHistory = new IdentityHashMap<>();
    private final List<String> eventLog = new ArrayList<>();
    private final List<String> zombiePool;
    private NutKind nextNutKind;
    private double conveyorTimer;

    public WallnutBowling(int difficulty) {
        setDifficulty(difficulty);
        this.session = new GameSession(5, 9);
        configureSession(session);
        session.setSkySunEnabled(false);
        this.redLineColumn = 5 - getDifficulty();
        this.zombiePool = zombiePoolFor(getDifficulty());
        session.setWaves(wavesFor(getDifficulty()));
        session.startWaves();
        initialiseConveyor();
        log("Wall-nut Bowling level " + getDifficulty()
                + " started. The conveyor belt is moving.");
    }

    private void initialiseConveyor() {
        conveyorBelt.add(NutKind.BOWLING);
        conveyorBelt.add(NutKind.EXPLODE);
        while (conveyorBelt.size() < CONVEYOR_CAPACITY) {
            conveyorBelt.add(randomNutKind());
        }
        updateNextNut();
    }

    private NutKind randomNutKind() {
        int roll = RAND.nextInt(100);
        if (roll < BOWLING_SHARE_PERCENT) return NutKind.BOWLING;
        if (roll < BOWLING_SHARE_PERCENT + EXPLODE_SHARE_PERCENT) return NutKind.EXPLODE;
        return NutKind.BIG;
    }

    private void updateNextNut() {
        nextNutKind = conveyorBelt.peek();
    }

    public NutKind getNextNutKind() { return nextNutKind; }

    public List<NutKind> getConveyorBelt() {
        return List.copyOf(conveyorBelt);
    }

    public Set<NutKind> getAvailableNutKinds() {
        return conveyorBelt.isEmpty()
                ? EnumSet.noneOf(NutKind.class)
                : EnumSet.copyOf(conveyorBelt);
    }

    public double getConveyorSecondsUntilNextNut() {
        if (conveyorBelt.size() >= CONVEYOR_CAPACITY) return -1;
        return Math.max(0, CONVEYOR_INTERVAL_SECONDS - conveyorTimer);
    }

    public boolean plantNut(int row, int col) {
        return plantNut(row, col, null);
    }

    public boolean plantNut(int row, int col, String requestedKind) {
        if (isWon() || isLost() || row < 0 || row >= session.getRows()
                || col < 0 || col > redLineColumn || conveyorBelt.isEmpty()) {
            return false;
        }

        NutKind kind = requestedKind == null
                ? nextNutKind
                : findRequestedKind(requestedKind);
        if (kind == null) return false;

        for (Nut active : activeNuts) {
            if (active.isAlive() && active.getPosition().distanceTo(new Position(col, row)) < 0.6) {
                return false;
            }
        }

        Position position = new Position(col, row);
        Position direction = new Position(1, 0);
        Nut nut = switch (kind) {
            case BOWLING -> new BowlingWallnut(position, direction);
            case EXPLODE -> new ExplodeONut(position, direction);
            case BIG -> new BigNut(position, direction);
        };

        activeNuts.add(nut);
        hitHistory.put(nut, Collections.newSetFromMap(new IdentityHashMap<>()));
        conveyorBelt.remove(kind);
        updateNextNut();
        log(nut.getKindName() + " launched from (" + (col + 1) + ", " + (row + 1)
                + "). Conveyor next: " + (nextNutKind == null ? "waiting" : nextNutKind) + ".");
        return true;
    }

    private boolean kindMatches(String requestedKind, NutKind kind) {
        String normalized = requestedKind.toLowerCase()
                .replace("-", "").replace("_", "").replace(" ", "");
        return switch (kind) {
            case BOWLING -> normalized.equals("bowling")
                    || normalized.equals("wallnut") || normalized.equals("bowlingwallnut");
            case EXPLODE -> normalized.equals("explode")
                    || normalized.equals("explodeonut") || normalized.equals("explodenut");
            case BIG -> normalized.equals("big") || normalized.equals("bignut")
                    || normalized.equals("bigwallnut");
        };
    }

    private NutKind findRequestedKind(String requestedKind) {
        return conveyorBelt.stream()
                .filter(kind -> kindMatches(requestedKind, kind))
                .findFirst()
                .orElse(null);
    }

    public void tick(double deltaSeconds) {
        if (isWon() || isLost()) return;
        session.tick();
        double safeDelta = Math.max(0, deltaSeconds);
        conveyorTimer += safeDelta;
        if (conveyorBelt.size() < CONVEYOR_CAPACITY
                && conveyorTimer >= CONVEYOR_INTERVAL_SECONDS) {
            conveyorTimer = 0;
            conveyorBelt.add(randomNutKind());
            updateNextNut();
            log("The conveyor delivered a " + nextNutKind + " nut. Belt: " + conveyorBelt + ".");
        }
        moveNuts(safeDelta);
        resolveCollisions();
        activeNuts.removeIf(nut -> {
            if (!nut.isAlive()) hitHistory.remove(nut);
            return !nut.isAlive();
        });
    }

    private void moveNuts(double deltaSeconds) {
        for (Nut nut : activeNuts) {
            if (!nut.isAlive()) continue;
            nut.move(deltaSeconds);
            Position pos = nut.getPosition();
            if (pos.x() > session.getCols() || pos.x() < -1) {
                nut.kill();
                log(nut.getKindName() + " left the lawn.");
                continue;
            }
            double lastRow = session.getRows() - 1;
            if (pos.y() < 0 || pos.y() > lastRow) {
                nut.setPosition(new Position(pos.x(), Math.max(0, Math.min(lastRow, pos.y()))));
                nut.bounceOffLaneEdge();
                log(nut.getKindName() + " ricocheted off the lawn edge.");
            }
        }
    }

    private void resolveCollisions() {
        for (Nut nut : activeNuts) {
            if (!nut.isAlive()) continue;
            Set<Zombie> alreadyHit = hitHistory.computeIfAbsent(nut,
                    key -> Collections.newSetFromMap(new IdentityHashMap<>()));
            session.getZombies().stream()
                    .filter(zombie -> zombie.isAlive() && !alreadyHit.contains(zombie))
                    .filter(zombie -> isTouching(nut, zombie))
                    .min(Comparator.comparingDouble(
                            zombie -> nut.getPosition().distanceTo(zombie.getPosition())))
                    .ifPresent(zombie -> {
                        alreadyHit.add(zombie);
                        int oldHp = zombie.getHp();
                        Position hitPosition = zombie.getPosition();
                        log(nut.getKindName() + " collided with " + zombie.getName()
                                + " at (" + ((int) Math.round(hitPosition.x()) + 1)
                                + ", " + ((int) Math.round(hitPosition.y()) + 1) + ").");
                        boolean consumed = nut.onHitZombie(zombie, session);
                        if (!zombie.isAlive() || zombie.getHp() < oldHp) {
                            log(zombie.getName() + " was hit by " + nut.getKindName()
                                    + " (" + oldHp + " -> " + zombie.getHp() + " hp).");
                        }
                        if (!zombie.isAlive()) {
                            log(zombie.getName() + " was killed by " + nut.getKindName() + ".");
                        }
                        if (consumed || !nut.isAlive()) {
                            log(nut.getKindName() + " was consumed in the collision.");
                        }
                    });
        }
    }

    private boolean isTouching(Nut nut, Zombie zombie) {
        Position zombiePosition = zombie.getPosition();
        if (zombiePosition == null) return false;
        Position nutPosition = nut.getPosition();
        return Math.abs(nutPosition.x() - zombiePosition.x()) <= HIT_RADIUS_COLUMNS
                && Math.abs(nutPosition.y() - zombiePosition.y()) <= HIT_RADIUS_ROWS;
    }

    public boolean isWon() {
        return session.isWavesStarted() && session.areWavesDone() && !session.isGameOver();
    }

    public boolean isLost() {
        return session.isGameOver();
    }

    public GameSession getSession() { return session; }
    public int getRedLineColumn() { return redLineColumn; }
    public List<Nut> getActiveNuts() { return activeNuts; }
    public List<String> getZombiePool() { return List.copyOf(zombiePool); }

    public List<GroundItem> collectItemsAt(int x, int y) {
        return session.collectItemsNear(new Position(x, y));
    }

    public String renderState() {
        StringBuilder result = new StringBuilder(getStageDetails())
                .append(" | Red line: column ").append(redLineColumn + 1)
                .append(" | Conveyor: ").append(conveyorBelt)
                .append(" | Next nut: ").append(nextNutKind == null ? "waiting" : nextNutKind)
                .append(" | Active nuts: ").append(activeNuts.size())
                .append("\n").append(session.renderMap());
        result.append("\nConveyor belt: ").append(conveyorBelt);
        if (conveyorBelt.size() < CONVEYOR_CAPACITY) {
            result.append(" (next delivery in ")
                    .append(String.format("%.1fs", getConveyorSecondsUntilNextNut())).append(")");
        }
        if (!activeNuts.isEmpty()) {
            result.append("\nNuts:");
            for (Nut nut : activeNuts) {
                result.append("\n  ").append(nut.getKindName()).append(" at (")
                        .append(String.format("%.1f", nut.getPosition().x() + 1))
                        .append(", ").append(String.format("%.1f", nut.getPosition().y() + 1))
                        .append(")");
            }
        }
        result.append("\nZombies:\n  ")
                .append(session.renderZombiesInfo().replace("\n", "\n  "));
        if (!eventLog.isEmpty()) {
            result.append("\nRecent events:\n  ")
                    .append(String.join("\n  ", eventLog));
        }
        return result.toString();
    }

    public String renderZombiesInfo() {
        return session.renderZombiesInfo();
    }

    private void log(String message) {
        eventLog.add(message);
        if (eventLog.size() > 12) eventLog.remove(0);
        GeneralPrinter.print(message);
    }

    private List<String> zombiePoolFor(int level) {
        return switch (level) {
            case 2 -> List.of("ZombieDefault", "ZombieArmor1", "ZombieArmor2");
            case 3 -> List.of("ZombieDefault", "ZombieImp", "ZombieArmor1", "ZombieArmor2",
                    "ZombieNewspaper", "ZombieModernAllStar", "ZombieGargantuar");
            default -> List.of("ZombieDefault", "ZombieImp", "ZombieArmor1");
        };
    }

    private List<model.match_mechanisms.ZombieWave> wavesFor(int level) {
        return switch (level) {
            case 2 -> MiniGameWaves.create(session,
                    new double[] {4, 7, 7, 7, 7},
                    new String[][] {
                            {"ZombieDefault", "ZombieArmor1", "ZombieDefault"},
                            {"ZombieArmor2", "ZombieArmor1", "ZombieDefault"},
                            {"ZombieArmor2", "ZombieArmor1", "ZombieArmor1", "ZombieDefault"},
                            {"ZombieArmor2", "ZombieArmor2", "ZombieArmor1", "ZombieDefault", "ZombieDefault"},
                            {"ZombieArmor2", "ZombieArmor2", "ZombieArmor1", "ZombieArmor1",
                                    "ZombieDefault", "ZombieDefault"}
                    });
            case 3 -> MiniGameWaves.create(session,
                    new double[] {3, 6, 6, 6, 6},
                    new String[][] {
                            {"ZombieArmor1", "ZombieArmor2", "ZombieImp"},
                            {"ZombieNewspaper", "ZombieImp", "ZombieArmor1", "ZombieArmor2"},
                            {"ZombieModernAllStar", "ZombieDefault", "ZombieNewspaper", "ZombieImp"},
                            {"ZombieGargantuar", "ZombieArmor2", "ZombieArmor1", "ZombieDefault", "ZombieImp"},
                            {"ZombieGargantuar", "ZombieModernAllStar", "ZombieArmor2", "ZombieArmor2",
                                    "ZombieArmor1", "ZombieDefault", "ZombieDefault"}
                    });
            default -> MiniGameWaves.create(session,
                    new double[] {5, 9, 9, 9},
                    new String[][] {
                            {"ZombieDefault", "ZombieDefault"},
                            {"ZombieDefault", "ZombieImp", "ZombieDefault"},
                            {"ZombieArmor1", "ZombieDefault", "ZombieDefault", "ZombieImp"},
                            {"ZombieArmor1", "ZombieArmor1", "ZombieDefault", "ZombieDefault", "ZombieImp"}
                    });
        };
    }
}