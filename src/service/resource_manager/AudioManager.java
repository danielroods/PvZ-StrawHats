package service.resource_manager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import controller.assets.GameAssetManager;


import java.util.EnumMap;
import java.util.Map;

public class AudioManager implements Disposable {

    private static AudioManager instance;
    private final GameAssetManager gameAssetManager;
    private final Map<AudioEnum, Sound> soundCache;
    private final Map<AudioEnum, Music> musicCache;

    private Music currentMusic;
    private AudioEnum currentMusicEnum;

    private float soundVolume = 1.0f;
    private float musicVolume = 1.0f;
    private boolean soundMuted = false;
    private boolean musicMuted = false;

    public AudioManager() {
        this.gameAssetManager = GameAssetManager.get();
        this.soundCache = new EnumMap<>(AudioEnum.class);
        this.musicCache = new EnumMap<>(AudioEnum.class);

        Preferences prefs = Gdx.app.getPreferences("GamePreferences");
        this.musicMuted = prefs.getBoolean("music_muted", false);
        this.soundMuted = prefs.getBoolean("sound_muted", false);
    }

    public void loadAll() {
        for (AudioEnum audio : AudioEnum.values()) {
            loadAudio(audio);
        }
    }

    public static AudioManager get() {
        if (instance == null)
            instance = new AudioManager();

        return instance;
    }

    public void loadAudio(AudioEnum audio) {
        String path = audio.getFilePath();
        if (path == null || path.isEmpty()) return;

        if (isMusicPath(path)) {
            gameAssetManager.loadMusic(path);
        } else {
            gameAssetManager.loadSound(path);
        }
    }

    public void playSound(AudioEnum audio) {
        playSound(audio, 1.0f, 1.0f, 0.0f);
    }

    public void playSound(AudioEnum audio, float volumeMultiplier) {
        playSound(audio, volumeMultiplier, 1.0f, 0.0f);
    }

    public void playSound(AudioEnum audio, float volumeMultiplier, float pitch, float pan) {
        if (soundMuted || audio == null) return;

        Sound sound = getSound(audio);
        if (sound != null) {
            float finalVolume = Math.max(0.0f, Math.min(1.0f, soundVolume * volumeMultiplier));
            sound.play(finalVolume, pitch, pan);
        }
    }

    public Sound getSound(AudioEnum audio) {
        if (audio == null) return null;
        if (soundCache.containsKey(audio)) {
            return soundCache.get(audio);
        }

        String path = audio.getFilePath();
        if (path == null || path.isEmpty()) return null; // e.g. SFX_CLICK("") before a real clip is wired up

        if (!gameAssetManager.isLoaded(path)) {
            gameAssetManager.loadSound(path);
            gameAssetManager.finishLoading();
        }

        Sound sound = gameAssetManager.getSound(path);
        if (sound != null) {
            soundCache.put(audio, sound);
        }
        return sound;
    }

    public void playMusic(AudioEnum audio, boolean looping) {
        if (audio == null) return;
        if (currentMusicEnum == audio && currentMusic != null && currentMusic.isPlaying()) {
            return;
        }

        stopMusic();

        Music music = getMusic(audio);
        if (music != null) {
            this.currentMusic = music;
            this.currentMusicEnum = audio;
            currentMusic.setLooping(looping);
            currentMusic.setVolume(musicMuted ? 0.0f : musicVolume);
            currentMusic.play();
        }
    }

    public Music getMusic(AudioEnum audio) {
        if (audio == null) return null;
        if (musicCache.containsKey(audio)) {
            return musicCache.get(audio);
        }

        String path = audio.getFilePath();
        if (path == null || path.isEmpty()) return null;

        if (!gameAssetManager.isLoaded(path)) {
            gameAssetManager.loadMusic(path);
            gameAssetManager.finishLoading();
        }

        Music music = gameAssetManager.getMusic(path);
        if (music != null) {
            musicCache.put(audio, music);
        }
        return music;
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentMusicEnum = null;
        }
    }

    public void pauseMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    public void resumeMusic() {
        if (currentMusic != null && !currentMusic.isPlaying() && !musicMuted) {
            currentMusic.play();
        }
    }

    private boolean isMusicPath(String path) {
        return path != null && (path.endsWith(".mp3") || path.contains("/music/"));
    }

    public void setSoundVolume(float volume) {
        this.soundVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0.0f, Math.min(1.0f, volume));
        if (currentMusic != null && !musicMuted) {
            currentMusic.setVolume(this.musicVolume);
        }
    }

    public void setSoundMuted(boolean muted) {
        this.soundMuted = muted;
    }

    public void setMusicMuted(boolean muted) {
        this.musicMuted = muted;
        if (currentMusic != null) {
            currentMusic.setVolume(muted ? 0.0f : musicVolume);
        }
    }

    public float getSoundVolume() { return soundVolume; }
    public float getMusicVolume() { return musicVolume; }
    public boolean isSoundMuted() { return soundMuted; }
    public boolean isMusicMuted() { return musicMuted; }

    public void update(float delta) {
    }

    @Override
    public void dispose() {
        stopMusic();
        soundCache.clear();
        musicCache.clear();
    }
}