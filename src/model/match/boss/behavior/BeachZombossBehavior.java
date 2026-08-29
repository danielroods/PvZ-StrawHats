package model.match.boss.behavior;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match.boss.ZombossActionSequence;
import model.match.boss.ZombossFight;
import model.match.boss.ZombossLawn;
import model.match.boss.ZombossShark;
import model.match_mechanisms.vector.Position;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BeachZombossBehavior extends ZombossBehavior {

    public static final String TURBINE_WIND_PAM =
            "768/FULL/EFFECTS/ZOMBOSS_TURBINE_WIND/ZOMBOSS_TURBINE_WIND.PAM";
    public static final String PLANT_PULLED_PAM =
            "768/FULL/EFFECTS/ZOMBOSS_PLANT_PULLED/ZOMBOSS_PLANT_PULLED.PAM";

    private static final double SUCTION_LOOP_SECONDS = 4.5;
    private static final double SUCTION_PULL_COLUMNS_PER_SECOND = 1.6;
    private static final double SUCTION_KILL_DISTANCE = 0.45;
    private static final int SUCTION_LETHAL_DAMAGE = 99999;
    private static final double TANGLE_KELP_DAMAGE_FRACTION = 0.10;
    private static final double PLANT_ARRIVAL_SECONDS = 0.85;

    private static final double SHARK_INTERVAL_MIN = 12.0;
    private static final double SHARK_INTERVAL_SPREAD = 7.0;
    private static final double SHARK_FIRST_DELAY = 14.0;
    private static final double WATER_ATTACK_REACH = 1.4;
    private static final double WATER_ATTACK_CHANCE = 0.35;
    private static final double SUCTION_CHANCE = 0.4;
    private static final int SUCTION_MAX_PLANTS = 2;
    private static final double COOLDOWN_MIN = 3.5;
    private static final double COOLDOWN_SPREAD = 2.5;

    public static final class PulledPlant {
        private final int row;
        private double column;
        private final boolean overWater;
        private final boolean tangleKelp;
        private boolean arrived;
        private double arrivedElapsed;

        PulledPlant(int row, double column, boolean overWater, boolean tangleKelp) {
            this.row = row;
            this.column = column;
            this.overWater = overWater;
            this.tangleKelp = tangleKelp;
        }

        public boolean isTangleKelp() { return tangleKelp; }

        public int getRow() { return row; }

        public double getColumn() { return column; }

        public boolean isOverWater() { return overWater; }

        public boolean hasArrived() { return arrived; }

        public double getArrivedElapsed() { return arrivedElapsed; }
    }

    private final List<ZombossShark> sharks = new ArrayList<>();
    private final List<PulledPlant> pulledPlants = new ArrayList<>();

    private double sharkTimer = SHARK_FIRST_DELAY;
    private int suctionRow = -1;
    private boolean tangleKelpPending;
    private boolean waterAttackApplied;
    private Position pendingMoveTarget;

    public BeachZombossBehavior(ZombossFight fight) {
        super(fight);
    }

    public List<ZombossShark> getSharks() { return Collections.unmodifiableList(sharks); }

    public List<PulledPlant> getPulledPlants() {
        return Collections.unmodifiableList(pulledPlants);
    }

    public int getSuctionRow() {
        ZombossActionSequence action = fight.getAction();
        boolean active = action != null && !action.isFinished()
                && "suction".equals(action.getName());
        return active ? suctionRow : -1;
    }

    @Override
    public void onBattleStart() {
        snapToWater();
    }

    @Override
    public void onDefeated() {
        sharks.clear();
        pulledPlants.clear();
    }

    @Override
    public void update(double deltaSeconds) {
        tickSharks(deltaSeconds);
        tickPulledPlants(deltaSeconds);

        ZombossActionSequence action = fight.getAction();
        if (action != null && !action.isFinished()) {
            driveAction(action, deltaSeconds);
            return;
        }
        if (!fight.isIdleForAction()) return;
        chooseAbility();
    }

    private void tickSharks(double deltaSeconds) {
        for (int i = sharks.size() - 1; i >= 0; i--) {
            ZombossShark shark = sharks.get(i);
            shark.tick(deltaSeconds, session());
            if (shark.isDone()) sharks.remove(i);
        }
        sharkTimer -= deltaSeconds;
        if (sharkTimer > 0) return;
        sharkTimer = SHARK_INTERVAL_MIN + random().nextDouble() * SHARK_INTERVAL_SPREAD;
        releaseShark();
    }

    private void releaseShark() {
        List<Position> water = ZombossLawn.waterTiles(session(), 0);
        if (water.isEmpty()) return;
        Position tile = water.get(random().nextInt(water.size()));
        sharks.add(new ZombossShark((int) tile.y(), tile.x(), session().getCols() + 1.5));
    }


    private void chooseAbility() {
        if (waterPlantWithinReach() != null && random().nextDouble() < WATER_ATTACK_CHANCE) {
            startWaterAttack();
            return;
        }
        if (random().nextDouble() < SUCTION_CHANCE || !startMove()) {
            startSuction();
        }
    }

    private void beginCooldown() {
        fight.setActionCooldown(COOLDOWN_MIN + random().nextDouble() * COOLDOWN_SPREAD);
    }

    private Plant waterPlantWithinReach() {
        Plant plant = ZombossLawn.plantWithinReach(session(), fight.getBossPosition(),
                WATER_ATTACK_REACH);
        if (plant == null) return null;
        return ZombossLawn.isWater(session(), (int) Math.round(plant.getPosition().y()),
                (int) Math.round(plant.getPosition().x())) ? plant : null;
    }

    private void startWaterAttack() {
        waterAttackApplied = false;
        fight.startAction(fight.newAction("spawn").then("spawn"));
        beginCooldown();
    }

    private void applyWaterAttack() {
        Plant victim = waterPlantWithinReach();
        if (victim != null) ZombossLawn.destroyPlant(session(), victim);
    }

    private boolean startMove() {
        Position target = pickMoveTarget();
        if (target == null) return false;
        pendingMoveTarget = target;
        fight.startAction(fight.newAction("move").then("submerge").then("emerge"));
        beginCooldown();
        return true;
    }

    private Position pickMoveTarget() {
        List<Position> water = ZombossLawn.waterTiles(session(), 0);
        if (water.isEmpty()) return null;
        Position current = fight.getBossPosition();
        List<Position> hunting = new ArrayList<>();
        for (Position tile : water) {
            if ((int) tile.x() == (int) Math.round(current.x())
                    && (int) tile.y() == (int) Math.round(current.y())) {
                continue;
            }
            Plant neighbour = session().getPlantAt((int) tile.y(), (int) tile.x() - 1);
            if (neighbour != null && neighbour.isAlive()) hunting.add(tile);
        }
        List<Position> pool = hunting.isEmpty() ? water : hunting;
        return pool.get(random().nextInt(pool.size()));
    }

    private void snapToWater() {
        Position current = fight.getBossPosition();
        int row = (int) Math.round(current.y());
        int col = (int) Math.round(current.x());
        if (ZombossLawn.isWater(session(), row, col)) return;
        List<Position> water = ZombossLawn.waterTiles(session(), 0);
        if (water.isEmpty()) return;
        Position best = water.get(0);
        double bestDistance = Double.MAX_VALUE;
        for (Position tile : water) {
            double distance = Math.abs(tile.x() - col) + Math.abs(tile.y() - row);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = tile;
            }
        }
        fight.moveBossTo(best.x(), best.y());
    }

    private void startSuction() {
        suctionRow = random().nextInt(session().getRows());
        tangleKelpPending = false;
        pulledPlants.clear();
        fight.startAction(fight.newAction("suction")
                .then("suction_on")
                .loop("suction_loop", SUCTION_LOOP_SECONDS)
                .then("suction_off"));
        beginCooldown();
    }

    private int pickSuctionRow() {
        List<Integer> occupied = new ArrayList<>();
        for (int row = 0; row < session().getRows(); row++) {
            if (!ZombossLawn.plantsInRow(session(), row).isEmpty()) occupied.add(row);
        }
        if (occupied.isEmpty()) return random().nextInt(session().getRows());
        return occupied.get(random().nextInt(occupied.size()));
    }

    private void tearPlantsIntoVortex() {
        if (suctionRow < 0) return;
       List<Plant> row = new ArrayList<>(ZombossLawn.plantsInRow(session(), suctionRow));
        double bossColumn = fight.getBossPosition().x();
        row.sort(java.util.Comparator.comparingDouble(
                p -> Math.abs(bossColumn - p.getPosition().x())));
        int taken = 0;
        for (Plant plant : row) {
            boolean kelp = ZombossLawn.isTangleKelp(plant);
            if (taken >= SUCTION_MAX_PLANTS && !kelp) continue;
            int col = (int) Math.round(plant.getPosition().x());
            boolean overWater = ZombossLawn.isWater(session(), suctionRow, col);
            if (kelp) tangleKelpPending = true;
            pulledPlants.add(new PulledPlant(suctionRow, plant.getPosition().x(),
                    overWater, kelp));
            ZombossLawn.destroyPlant(session(), plant);
            taken++;
            if (kelp) break;
        }
    }

    private void dragRow(double deltaSeconds) {
        if (suctionRow < 0) return;
        double bossColumn = fight.getBossPosition().x();
        double step = SUCTION_PULL_COLUMNS_PER_SECOND * deltaSeconds;
        for (Zombie zombie : new ArrayList<>(session().getZombies())) {
            if (zombie == null || !zombie.isAlive() || zombie == fight.getBoss()) continue;
            if (zombie.getPosition() == null) continue;
            if ((int) Math.round(zombie.getPosition().y()) != suctionRow) continue;
            double dx = bossColumn - zombie.getPosition().x();
            if (Math.abs(dx) <= SUCTION_KILL_DISTANCE) {
                zombie.takeDamage(SUCTION_LETHAL_DAMAGE, fight.getBoss());
                continue;
            }
            double moved = Math.signum(dx) * Math.min(step, Math.abs(dx));
            zombie.setPosition(new Position(zombie.getPosition().x() + moved,
                    zombie.getPosition().y()));
        }
    }

    private void tickPulledPlants(double deltaSeconds) {
        if (pulledPlants.isEmpty()) return;
        double bossColumn = fight.getBossPosition().x();
        double step = SUCTION_PULL_COLUMNS_PER_SECOND * 1.8 * deltaSeconds;
        for (int i = pulledPlants.size() - 1; i >= 0; i--) {
            PulledPlant pulled = pulledPlants.get(i);
            if (pulled.arrived) {
                pulled.arrivedElapsed += deltaSeconds;
                if (pulled.arrivedElapsed >= PLANT_ARRIVAL_SECONDS) pulledPlants.remove(i);
                continue;
            }
            double dx = bossColumn - pulled.column;
            if (Math.abs(dx) <= SUCTION_KILL_DISTANCE) {
                pulled.arrived = true;
                pulled.arrivedElapsed = 0.0;
                continue;
            }
            pulled.column += Math.signum(dx) * Math.min(step, Math.abs(dx));
        }
    }

    private void breakOnTangleKelp() {
        Zombie boss = fight.getBoss();
        if (boss != null) {
            int damage = (int) Math.max(1, boss.getMaxHp() * TANGLE_KELP_DAMAGE_FRACTION);
            boss.takeDamage(damage, boss);
        }
        pulledPlants.clear();
        suctionRow = -1;
        tangleKelpPending = false;
        fight.startAction(fight.newAction("tangled")
                .then("tangled_on")
                .loop("tangled_loop", 2.0)
                .then("tangled_off"));
        fight.setActionCooldown(2.0);
        view.GeneralPrinter.print("Tangle Kelp jammed the Zomboss turbine!");
    }

    private void driveAction(ZombossActionSequence action, double deltaSeconds) {
        int step = consumeStepChange(action);
        switch (action.getName()) {
            case "spawn" -> {
                if (!waterAttackApplied && action.getStepProgress() >= 0.6) {
                    waterAttackApplied = true;
                    applyWaterAttack();
                }
            }
            case "move" -> {
                if (step == 1 && pendingMoveTarget != null) {
                    fight.moveBossTo(pendingMoveTarget.x(), pendingMoveTarget.y());
                    pendingMoveTarget = null;
                }
            }
            case "suction" -> driveSuction(action, step, deltaSeconds);
            default -> { }
        }
    }

    private void driveSuction(ZombossActionSequence action, int step, double deltaSeconds) {
        if (step == 1) tearPlantsIntoVortex();
        if (action.getStepIndex() == 1) {
            dragRow(deltaSeconds);
            if (tangleKelpPending && tangleKelpReachedMouth()) breakOnTangleKelp();
        }
    }

    private boolean tangleKelpReachedMouth() {
        for (PulledPlant pulled : pulledPlants) {
            if (pulled.tangleKelp && pulled.arrived) return true;
        }
        return false;
    }
}
