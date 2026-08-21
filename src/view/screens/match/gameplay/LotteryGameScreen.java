package view.screens.match.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import controller.ScreenManager;
import controller.match.AfterMenu;
import model.App;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieState;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import view.screens.generals.GameScreen;
import view.hud.LotteryMatchHud;
import view.screens.match.after.AfterMatchScreen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LotteryGameScreen extends GameScreen {

    private Stage lotteryStage;
    private LotteryMatchHud lotteryHud;
    private Texture seasonBackgroundTexture;
    private Image bgImageActor;

    private float difficultyMultiplier = 1.0f;
    private int comboCount = 0;
    private int spawnedBatchesCount = 0;

    private final List<Long> recentKillTimestamps = new ArrayList<>();
    private final Map<Zombie, Float> knownZombies = new IdentityHashMap<>();

    private float gameTimeSeconds = 0f;
    private float spawnTimer = 0f;
    private float sunSpawnTimer = 0f;

    public LotteryGameScreen() {
        super();
    }

    public LotteryGameScreen(GameSession session) {
        super();
        this.session = session;
    }

    @Override
    public void show() {
        super.show();
        if (session == null) {
            session = GameSession.getInstance();
        }

        lotteryStage = new Stage(new ScreenViewport(), stage.getBatch());
        lotteryHud = new LotteryMatchHud(skin);
        lotteryStage.addActor(lotteryHud);

        difficultyMultiplier = 1.0f;
        gameTimeSeconds = 0f;
        spawnTimer = 0f;
        sunSpawnTimer = 0f;
        spawnedBatchesCount = 0;

        // تنظیم پس‌زمینه فصل
        setupSeasonBackgroundActor();

        // پاکسازی زامبی‌های قبلی
        if (session != null && session.getZombies() != null) {
            session.getZombies().clear();
        }

        // نامحدود ساختن امواج موتور بازی
        keepEngineWavesInfinite();

        // اسپاون اولین دسته زامبی‌ها
        spawnContinuousZombies();
    }

    private void setupSeasonBackgroundActor() {
        String seasonName = getSeasonName();
        String texturePath = "images/backgrounds/egypt.png";

        if (seasonName != null) {
            String lower = seasonName.toLowerCase();
            if (lower.contains("dark")) {
                texturePath = "images/backgrounds/dark_ages.png";
            } else if (lower.contains("beach")) {
                texturePath = "images/backgrounds/big_wave_beach.png";
            } else if (lower.contains("frost") || lower.contains("ice")) {
                texturePath = "images/backgrounds/frostbite_caves.png";
            } else if (lower.contains("egypt")) {
                texturePath = "images/backgrounds/egypt.png";
            }
        }

        try {
            if (Gdx.files.internal(texturePath).exists()) {
                seasonBackgroundTexture = new Texture(texturePath);
                if (stage != null) {
                    bgImageActor = new Image(seasonBackgroundTexture);
                    bgImageActor.setFillParent(true);
                    stage.addActor(bgImageActor);
                    bgImageActor.toBack();
                }
            }
        } catch (Throwable ignored) {}
    }

    private String getSeasonName() {
        if (session != null && session.getLevel() != null && session.getLevel().getSeason() != null) {
            return session.getLevel().getSeason().getName();
        }
        return null;
    }

    /**
     * نامحدود ساختن امواج موتور بازی با پیمایش کامل کلاس‌های والد (Superclasses)
     * و ریست مداوم پرچم‌ها و اندیس‌های موج
     */
    private void keepEngineWavesInfinite() {
        if (session == null) return;

        // ریست کردن پرچم‌های اتمام در سطح جلسه
        setFieldSilently(session, "isFinished", false);
        setFieldSilently(session, "isWon", false);
        setFieldSilently(session, "gameWon", false);

        Object level = session.getLevel();
        if (level == null) return;

        // تنظیم سقف امواج و ریست پرچم‌ها
        setFieldSilently(level, "totalWaves", 99999);
        setFieldSilently(level, "maxWaves", 99999);
        setFieldSilently(level, "numberOfWaves", 99999);

        setFieldSilently(level, "spawningFinished", false);
        setFieldSilently(level, "allWavesSpawned", false);
        setFieldSilently(level, "isSpawningFinished", false);
        setFieldSilently(level, "isFinished", false);
        setFieldSilently(level, "isWon", false);
        setFieldSilently(level, "gameWon", false);

        // ریست مداوم اندیس موج جاری جهت جلوگیری از سرریز لیست امواج
        setFieldSilently(level, "currentWave", 0);
        setFieldSilently(level, "currentWaveIndex", 0);
        setFieldSilently(level, "waveIndex", 0);

        // پیمایش تمام زنجیره کلاس‌های والد برای گسترش لیست امواج
        Class<?> current = level.getClass();
        while (current != null && current != Object.class) {
            try {
                for (Field f : current.getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        List list = (List) f.get(level);
                        if (list != null && !list.isEmpty()) {
                            String name = f.getName().toLowerCase();
                            if (name.contains("wave") || name.contains("zombie") || name.contains("spawn")) {
                                Object sample = list.get(0);
                                while (list.size() < 2000) {
                                    list.add(sample);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
            current = current.getSuperclass();
        }
    }

    @Override
    protected void tickSession() {
        if (session == null) return;

        keepEngineWavesInfinite();
        try {
            session.tick();
        } catch (Throwable ignored) {
            // جلوگیری از متوقف شدن چرخه بازی در صورت بروز خطای امواج موتور
        }
        keepEngineWavesInfinite();
    }

    @Override
    public void render(float delta) {
        gameTimeSeconds += delta;

        keepEngineWavesInfinite();
        handleContinuousSpawning(delta);
        handleFastSunGeneration(delta);
        trackLotteryGameplay(delta);

        if (bgImageActor != null) {
            bgImageActor.toBack();
        }

        super.render(delta);

        if (lotteryStage != null) {
            lotteryStage.act(delta);
            lotteryStage.draw();
        }
    }

    private void handleContinuousSpawning(float delta) {
        if (session == null) return;

        spawnTimer += delta;
        float currentSpawnInterval = Math.max(0.6f, 3.5f - (spawnedBatchesCount * 0.12f));

        boolean isListEmpty = session.getZombies() != null && session.getZombies().isEmpty();

        if (spawnTimer >= currentSpawnInterval || isListEmpty) {
            spawnTimer = 0f;
            spawnedBatchesCount++;
            spawnContinuousZombies();
        }

        difficultyMultiplier = 1.0f + (gameTimeSeconds / 20.0f);
    }

    private void handleFastSunGeneration(float delta) {
        sunSpawnTimer += delta;
        if (sunSpawnTimer >= 2.0f) {
            sunSpawnTimer = 0f;
            addFastSunToSession(25);
        }
    }

    private void addFastSunToSession(int amount) {
        if (session == null) return;
        try {
            var method = session.getClass().getMethod("addSun", int.class);
            method.invoke(session, amount);
        } catch (Throwable t) {
            try {
                var getSun = session.getClass().getMethod("getSun");
                int current = (int) getSun.invoke(session);
                var setSun = session.getClass().getMethod("setSun", int.class);
                setSun.invoke(session, current + amount);
            } catch (Throwable ignored) {}
        }
    }

    private void spawnContinuousZombies() {
        if (session == null) return;

        int rows = session.getRows() > 0 ? session.getRows() : 5;
        int cols = session.getCols() > 0 ? session.getCols() : 9;

        int zombieCount = 3 + (spawnedBatchesCount / 2);
        zombieCount = Math.min(zombieCount, 12);

        List<String> zombiePool = getSeasonSpecificZombiePool();
        Random random = new Random();
        List<Zombie> sessionZombies = session.getZombies();

        if (sessionZombies == null) return;

        for (int i = 0; i < zombieCount; i++) {
            int row = random.nextInt(rows);
            float startCol = cols + random.nextFloat() * 1.5f + (i * 0.35f);
            Position pos = new Position(startCol, row);

            String selectedClass = zombiePool.get(random.nextInt(zombiePool.size()));
            Zombie z = safeInstantiateZombie(selectedClass, pos);
            if (z != null) {
                sessionZombies.add(z);

            }
        }
    }

    private List<String> getSeasonSpecificZombiePool() {
        List<String> pool = new ArrayList<>();
        String seasonName = getSeasonName();

        if (seasonName != null) {
            String lower = seasonName.toLowerCase();
            if (lower.contains("egypt")) {
                pool.add("MummyZombie");
                pool.add("TombRaiserZombie");
                pool.add("PharaohZombie");
                pool.add("ConeheadZombie");
                pool.add("BucketheadZombie");
                pool.add("FlagZombie");
            } else if (lower.contains("dark")) {
                pool.add("KnightZombie");
                pool.add("ConeheadZombie");
                pool.add("BucketheadZombie");
                pool.add("Zombie");
            } else if (lower.contains("beach")) {
                pool.add("SnorkelZombie");
                pool.add("DolphinRiderZombie");
                pool.add("BucketheadZombie");
                pool.add("Zombie");
            } else if (lower.contains("frost") || lower.contains("ice")) {
                pool.add("YetiZombie");
                pool.add("BucketheadZombie");
                pool.add("Zombie");
            }
        }

        if (pool.isEmpty()) {
            pool.add("Zombie");
            pool.add("ConeheadZombie");
            pool.add("BucketheadZombie");
            pool.add("FlagZombie");
            pool.add("NewspaperZombie");
            pool.add("PoleVaultingZombie");
            pool.add("FootballZombie");
            pool.add("ScreenDoorZombie");
        }
        return pool;
    }

    private void trackLotteryGameplay(float delta) {
        if (session == null) return;

        List<Zombie> activeZombies = session.getZombies();
        if (activeZombies == null) return;

        for (Zombie z : new ArrayList<>(activeZombies)) {
            if (z != null && !knownZombies.containsKey(z) && z.getZombieState() != ZombieState.DEAD) {
                knownZombies.put(z, gameTimeSeconds);
            }
        }

        List<Zombie> deadOrRemoved = new ArrayList<>();
        for (Map.Entry<Zombie, Float> entry : knownZombies.entrySet()) {
            Zombie z = entry.getKey();
            if (!activeZombies.contains(z) || z.getZombieState() == ZombieState.DEAD) {
                deadOrRemoved.add(z);
            }
        }

        for (Zombie z : deadOrRemoved) {
            float spawnTime = knownZombies.remove(z);
            float killTime = gameTimeSeconds;
            onZombieKilled(z, false, spawnTime, killTime);
        }
    }

    public void onZombieKilled(Zombie zombie, boolean isPiercingShot, float spawnTimeSeconds, float killTimeSeconds) {
        comboCount++;
        long now = TimeUtils.millis();
        recentKillTimestamps.add(now);

        int totalMeowPoints = calculate5PatternPoints(zombie, isPiercingShot, spawnTimeSeconds, killTimeSeconds);

        if (lotteryHud != null) {
            lotteryHud.addPoints(totalMeowPoints);
            lotteryHud.updateCombo(comboCount);
            lotteryHud.incrementKills();
        }
    }

    private int calculate5PatternPoints(Zombie zombie, boolean isPiercingShot, float spawnTime, float killTime) {
        int p1 = isPiercingShot ? 25 : 5;
        float timeAlive = killTime - spawnTime;
        int p2 = (timeAlive <= 3.0f) ? 30 : ((timeAlive <= 6.0f) ? 15 : 5);

        long now = TimeUtils.millis();
        recentKillTimestamps.removeIf(time -> (now - time) > 1000);
        int simKills = recentKillTimestamps.size();
        int p3 = (simKills >= 4) ? 50 : ((simKills >= 2) ? 20 : 0);

        int p4 = Math.min(comboCount * 2, 40);

        float rawScore = (p1 * 1.5f) + (p2 * 2.0f) + (p3 * 2.5f) + (p4 * 1.0f);
        return Math.round(rawScore * difficultyMultiplier);
    }

    @Override
    protected void checkMatchEnd() {
        if (matchFinished) return;

        if (session != null && session.isGameOver()) {
            matchFinished = true;
            try {
                App.currentMenu = new AfterMenu();
                ScreenManager.setScreen(new AfterMatchScreen());
            } catch (Throwable t) {
                runCommand("end game -r lose");
                ScreenManager.syncWithCurrentMenu();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (lotteryStage != null) {
            lotteryStage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (lotteryStage != null) {
            lotteryStage.dispose();
            lotteryStage = null;
        }
        if (seasonBackgroundTexture != null) {
            seasonBackgroundTexture.dispose();
            seasonBackgroundTexture = null;
        }
    }

    private Zombie safeInstantiateZombie(String className, Position pos) {
        String[] possiblePackages = {
                "model.collections.zombie.",
                "model.zombie.",
                "model.collections.zombies.",
                "model.match.zombie."
        };

        for (String pkg : possiblePackages) {
            Zombie z = tryCreateZombieClass(pkg + className, pos);
            if (z != null) return z;
        }

        Zombie fallback = tryCreateZombieClass("model.collections.zombie.Zombie", pos);
        if (fallback != null) return fallback;

        return tryCreateZombieClass("model.collections.zombie.ConeheadZombie", pos);
    }

    private Zombie tryCreateZombieClass(String fullClassName, Position pos) {
        try {
            Class<?> clazz = Class.forName(fullClassName);
            for (var cons : clazz.getConstructors()) {
                if (cons.getParameterCount() == 1 && cons.getParameterTypes()[0].equals(Position.class)) {
                    return (Zombie) cons.newInstance(pos);
                }
            }
            Zombie z = (Zombie) clazz.getDeclaredConstructor().newInstance();
            safeSetPosition(z, pos);
            return z;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void safeSetPosition(Zombie z, Position pos) {
        if (z == null || pos == null) return;
        try {
            z.setPosition(pos);
        } catch (Throwable t) {
            try {
                var m = z.getClass().getMethod("setPosition", Position.class);
                m.invoke(z, pos);
            } catch (Throwable ignored) {}
        }
    }

    private static void setFieldSilently(Object obj, String fieldName, Object value) {
        if (obj == null) return;
        Class<?> current = obj.getClass();
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            } catch (Throwable t) {
                current = current.getSuperclass();
            }
        }
    }
}