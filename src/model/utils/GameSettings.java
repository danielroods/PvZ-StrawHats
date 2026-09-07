package model.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public final class GameSettings {

    private static GameSettings instance;

    private static final String PREF_NAME = "game_settings";

    private final Preferences preferences;

    private boolean debugMode = false;
    private boolean showGrid = false;
    private int difficulty = 3;
    private int gameSpeed = 1;
    private float musicVolume = 0.6f;
    private float sfxVolume = 0.8f;
    private boolean sfxEnabled = true;

    private GameSettings() {
        preferences = Gdx.app == null ? null : Gdx.app.getPreferences(PREF_NAME);
        load();
    }

    public static GameSettings get() {
        if (instance == null) {
            instance = new GameSettings();
        }
        return instance;
    }

    public void load() {
        if (preferences == null) {
            return;
        }
        debugMode = preferences.getBoolean("debugMode", false);
        showGrid = preferences.getBoolean("showGrid", false);
        difficulty = preferences.getInteger("difficulty", 3);
        gameSpeed = preferences.getInteger("gameSpeed", 1);
        musicVolume = preferences.getFloat("musicVolume", 0.6f);
        sfxVolume = preferences.getFloat("sfxVolume", 0.8f);
        sfxEnabled = preferences.getBoolean("sfxEnabled", true);
    }

    public void save() {
        if (preferences == null) {
            return;
        }
        preferences.putBoolean("debugMode", debugMode);
        preferences.putBoolean("showGrid", showGrid);
        preferences.putInteger("difficulty", difficulty);
        preferences.putInteger("gameSpeed", gameSpeed);
        preferences.putFloat("musicVolume", musicVolume);
        preferences.putFloat("sfxVolume", sfxVolume);
        preferences.putBoolean("sfxEnabled", sfxEnabled);
        preferences.flush();
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = Math.max(1, Math.min(5, difficulty));
    }

    public int getGameSpeed() {
        return gameSpeed;
    }

    public void setGameSpeed(int gameSpeed) {
        this.gameSpeed = Math.max(1, Math.min(3, gameSpeed));
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = Math.max(0f, Math.min(1f, musicVolume));
    }

    public float getSfxVolume() {
        return sfxVolume;
    }

    public void setSfxVolume(float sfxVolume) {
        this.sfxVolume = Math.max(0f, Math.min(1f, sfxVolume));
    }

    public boolean isSfxEnabled() {
        return sfxEnabled;
    }

    public void setSfxEnabled(boolean sfxEnabled) {
        this.sfxEnabled = sfxEnabled;
    }
}