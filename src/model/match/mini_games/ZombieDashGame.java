package model.match.mini_games;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public final class ZombieDashGame {
    public static final int LANE_COUNT = 3;
    public static final int MAX_LIVES = 3;

    private static final float BASE_SPEED = 6.5f;
    private static final float MAX_SPEED = 14f;
    private static final float SPEED_RAMP_TIME = 150f;
    private static final float LANE_SWITCH_TIME = 0.14f;
    private static final float SPAWN_AHEAD_DISTANCE = 26f;
    private static final float DESPAWN_BEHIND_DISTANCE = 4f;
    private static final float INVULNERABLE_TIME = 1.2f;
    private static final float SHOOTER_START_TIME = 10f;
    private static final float SHOOTER_FIRE_RANGE = 7f;
    private static final float PEA_SPAWN_OFFSET = 1.2f;
    private static final float PEA_EXTRA_SPEED = 5f;
    
    public static final float SHOOTER_FLASH_TIME = 0.4f;

    public enum EntityType { PLANT, GRAVE, PEASHOOTER, SNOWPEA, PEA, SUN, COIN, BRAIN }

    public enum Kind { OBSTACLE, COLLECTIBLE }

    public static final class SpawnEntity {
        public final EntityType type;
        public final Kind kind;
        public final int lane;
        public float distance;
        public boolean collected;
        public boolean hit;
        
        public boolean hasFired;
        
        public float fireFlashTimer;
        
        public boolean icy;

        SpawnEntity(EntityType type, int lane, float distance) {
            this.type = type;
            this.lane = lane;
            this.distance = distance;
            this.kind = (type == EntityType.SUN || type == EntityType.COIN || type == EntityType.BRAIN)
                    ? Kind.COLLECTIBLE : Kind.OBSTACLE;
        }
    }

    private final Random random;
    private final List<SpawnEntity> entities = new ArrayList<>();

    private int lane = 1;
    private int targetLane = 1;
    private float laneSwitchTimer;
    private float distanceTraveled;
    private float elapsedTime;
    private float nextSpawnDistance;
    private int lives = MAX_LIVES;
    private int score;
    private int coinsCollected;
    private int sunsCollected;
    private int brainsCollected;
    private float invulnerableTimer;
    private boolean paused;
    private boolean gameOver;

    public ZombieDashGame() { this(new Random()); }

    public ZombieDashGame(Random random) {
        this.random = random;
        nextSpawnDistance = SPAWN_AHEAD_DISTANCE * 0.6f;
    }

    public int getLane() { return lane; }
    public float getLaneSwitchProgress() {
        return LANE_SWITCH_TIME <= 0 ? 1f : 1f - Math.max(0, laneSwitchTimer) / LANE_SWITCH_TIME;
    }
    public float getDistanceTraveled() { return distanceTraveled; }
    public float getSpeed() {
        float ramp = Math.min(1f, elapsedTime / SPEED_RAMP_TIME);
        return BASE_SPEED + (MAX_SPEED - BASE_SPEED) * ramp;
    }
    public int getLives() { return lives; }
    public int getScore() { return score; }
    public int getCoinsCollected() { return coinsCollected; }
    public int getSunsCollected() { return sunsCollected; }
    public int getBrainsCollected() { return brainsCollected; }
    public boolean isInvulnerable() { return invulnerableTimer > 0; }
    public boolean isPaused() { return paused; }
    public boolean isGameOver() { return gameOver; }
    public List<SpawnEntity> getEntities() { return entities; }

    public void setPaused(boolean value) { if (!gameOver) paused = value; }
    public void togglePaused() { setPaused(!paused); }

    public void moveUp() { if (!gameOver && !paused) targetLane = Math.max(0, targetLane - 1); }
    public void moveDown() { if (!gameOver && !paused) targetLane = Math.min(LANE_COUNT - 1, targetLane + 1); }

    public void update(float delta) {
        if (gameOver || paused) return;
        delta = Math.min(delta, 0.05f);
        elapsedTime += delta;
        invulnerableTimer = Math.max(0, invulnerableTimer - delta);

        if (targetLane != lane) {
            laneSwitchTimer += delta;
            if (laneSwitchTimer >= LANE_SWITCH_TIME) {
                lane = targetLane;
                laneSwitchTimer = 0;
            }
        } else {
            laneSwitchTimer = 0;
        }

        float speed = getSpeed();
        distanceTraveled += speed * delta;
        score += Math.round(speed * delta * 2f);

        spawnAhead();
        updateShooters(delta);
        checkCollisions();
        cleanupBehind();
    }

    
    private void updateShooters(float delta) {
        List<SpawnEntity> newPeas = null;
        for (SpawnEntity e : entities) {
            if (e.type == EntityType.PEA) {
                e.distance -= PEA_EXTRA_SPEED * delta;
                continue;
            }
            if (e.type != EntityType.PEASHOOTER && e.type != EntityType.SNOWPEA) continue;
            if (e.fireFlashTimer > 0) e.fireFlashTimer = Math.max(0, e.fireFlashTimer - delta);
            if (e.hit || e.collected || e.hasFired) continue;
            float aheadOfPlayer = e.distance - distanceTraveled;
            if (aheadOfPlayer > 0 && aheadOfPlayer < SHOOTER_FIRE_RANGE) {
                e.hasFired = true;
                e.fireFlashTimer = SHOOTER_FLASH_TIME;
                SpawnEntity pea = new SpawnEntity(EntityType.PEA, e.lane, e.distance - PEA_SPAWN_OFFSET);
                pea.icy = e.type == EntityType.SNOWPEA;
                if (newPeas == null) newPeas = new ArrayList<>();
                newPeas.add(pea);
            }
        }
        if (newPeas != null) entities.addAll(newPeas);
    }

    private void spawnAhead() {
        while (nextSpawnDistance < distanceTraveled + SPAWN_AHEAD_DISTANCE) {
            spawnWave(nextSpawnDistance);
            nextSpawnDistance += spawnGap();
        }
    }

    
    private float spawnGap() {
        float minGap = 5.5f - Math.min(2.5f, elapsedTime * 0.02f);
        float maxGap = 8.5f - Math.min(3.0f, elapsedTime * 0.025f);
        return minGap + random.nextFloat() * (maxGap - minGap);
    }

    private void spawnWave(float distance) {
        int obstacleLane = random.nextInt(LANE_COUNT);
        entities.add(new SpawnEntity(rollObstacle(), obstacleLane, distance));

        boolean secondObstacle = elapsedTime > 25f && random.nextFloat() < difficultyRamp();
        if (secondObstacle) {
            int secondLane;
            do { secondLane = random.nextInt(LANE_COUNT); } while (secondLane == obstacleLane);
            entities.add(new SpawnEntity(rollObstacle(), secondLane, distance));
        }

        for (int l = 0; l < LANE_COUNT; l++) {
            if (l == obstacleLane) continue;
            if (secondObstacle) continue;
            if (random.nextFloat() < 0.55f) {
                EntityType collectible = rollCollectible();
                entities.add(new SpawnEntity(collectible, l, distance + 1.2f));
            }
        }
    }

    
    private float difficultyRamp() {
        return Math.min(0.35f, elapsedTime / 400f);
    }

    
    private EntityType rollObstacle() {
        if (elapsedTime < SHOOTER_START_TIME) {
            return random.nextBoolean() ? EntityType.PLANT : EntityType.GRAVE;
        }
        float r = random.nextFloat();
        if (r < 0.18f) return EntityType.PEASHOOTER;
        if (r < 0.30f) return EntityType.SNOWPEA;
        if (r < 0.65f) return EntityType.PLANT;
        return EntityType.GRAVE;
    }

    private EntityType rollCollectible() {
        float r = random.nextFloat();
        if (r < 0.08f) return EntityType.BRAIN;
        if (r < 0.45f) return EntityType.SUN;
        return EntityType.COIN;
    }

    private void checkCollisions() {
        for (SpawnEntity e : entities) {
            if (e.collected || e.hit) continue;
            if (e.lane != lane) continue;
            if (Math.abs(e.distance - distanceTraveled) > 0.6f) continue;

            if (e.kind == Kind.COLLECTIBLE) {
                e.collected = true;
                applyPickup(e.type);
            } else if (invulnerableTimer <= 0) {
                e.hit = true;
                hurtPlayer();
            }
        }
    }

    private void applyPickup(EntityType type) {
        switch (type) {
            case COIN -> { coinsCollected++; score += 25; }
            case SUN -> { sunsCollected++; score += 15; }
            case BRAIN -> { brainsCollected++; score += 100; }
            default -> { }
        }
    }

    private void hurtPlayer() {
        lives--;
        invulnerableTimer = INVULNERABLE_TIME;
        if (lives <= 0) { lives = 0; gameOver = true; }
    }

    private void cleanupBehind() {
        Iterator<SpawnEntity> it = entities.iterator();
        while (it.hasNext()) {
            SpawnEntity e = it.next();
            if (e.distance < distanceTraveled - DESPAWN_BEHIND_DISTANCE) it.remove();
        }
    }
}
