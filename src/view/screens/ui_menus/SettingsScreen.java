package view.screens.ui_menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import model.user_data.User;
import model.utils.GameSettings;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.GeneralPrinter;
import view.screens.generals.UiScreen;

public class SettingsScreen extends UiScreen {

    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;
    private static final int MIN_GAME_SPEED = 1;
    private static final int MAX_GAME_SPEED = 3;

    private Preferences prefs;
    private Slider.SliderStyle safeSliderStyle;

    @Override
    public void show() {
        setBackground("assets/images/backg/profilemenu_background.png");
        prefs = Gdx.app.getPreferences("GamePreferences");
        safeSliderStyle = createSafeSliderStyle();
        super.show();

        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);

        build();
    }

    private void build() {
        rootTable.clear();

        ImageButton backBtn = createIconButton("assets/images/ui/buttons_hud_back_normal.png", 54, 54, () -> runCommand("menu exit"));

        Table topBar = new Table();
        topBar.add(backBtn).left().expandX();

        Table card = new Table();
        card.setBackground(createTranslucentCardDrawable());
        card.pad(30).defaults().pad(6);

        card.add(new Label("Brightness", skin, "main")).left();
        card.add(brightnessRow()).left().row();

        card.add(new Label("Music Volume", skin, "main")).left();
        card.add(musicVolumeRow()).left().row();

        card.add(musicMuteToggle()).colspan(2).left().padTop(2).row();
        card.add(soundMuteToggle()).colspan(2).left().padTop(2).row();

        card.add(new Label("Difficulty", skin, "main")).left();
        card.add(difficultyRow()).left().row();

        card.add(new Label("Game speed", skin, "main")).left();
        card.add(gameSpeedRow()).left().row();

        card.add(gridToggle()).colspan(2).left().padTop(2).row();
        card.add(debugToggle()).colspan(2).left().padTop(2).row();

        rootTable.add(topBar).fillX().padTop(15).padLeft(20).row();
        rootTable.add(card).expand().center();
    }

    private Table brightnessRow() {
        Table row = new Table();
        float currentBrightness = prefs.getFloat("brightness", 100f);
        Slider slider = new Slider(50f, 150f, 1f, false, safeSliderStyle);
        slider.setValue(currentBrightness);

        Label valueLabel = new Label((int) currentBrightness + "%", skin, "main");

        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int val = (int) slider.getValue();
                valueLabel.setText(val + "%");
                prefs.putFloat("brightness", slider.getValue());
                prefs.flush();
                updateBrightnessOverlay();
            }
        });

        row.add(slider).width(200).padRight(10);
        row.add(valueLabel).width(50);
        return row;
    }

    private Table musicVolumeRow() {
        Table row = new Table();
        float currentVol = prefs.getFloat("music_volume", AudioManager.get().getMusicVolume() * 100f);
        Slider slider = new Slider(0, 100, 1f, false, safeSliderStyle);
        slider.setValue(currentVol);

        Label valueLabel = new Label((int) currentVol + "%", skin, "main");

        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int val = (int) slider.getValue();
                valueLabel.setText(val + "%");
                float volFraction = slider.getValue() / 100;
                AudioManager.get().setMusicVolume(volFraction);
                prefs.putFloat("music_volume", slider.getValue());
                prefs.flush();
            }
        });

        row.add(slider).width(200).padRight(10);
        row.add(valueLabel).width(50);
        return row;
    }

    private CheckBox musicMuteToggle() {
        CheckBox musicToggle = new CheckBox(" Music Enabled", skin, "main");
        boolean isMuted = prefs.getBoolean("music_muted", AudioManager.get().isMusicMuted());
        musicToggle.setChecked(!isMuted);

        musicToggle.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean enable = musicToggle.isChecked();
                AudioManager.get().setMusicMuted(!enable);
                prefs.putBoolean("music_muted", !enable);
                prefs.flush();
            }
        });
        return musicToggle;
    }

    private CheckBox soundMuteToggle() {
        CheckBox soundToggle = new CheckBox(" Sound Effects Enabled", skin, "main");
        boolean isMuted = prefs.getBoolean("sound_muted", AudioManager.get().isSoundMuted());
        soundToggle.setChecked(!isMuted);

        soundToggle.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean enable = soundToggle.isChecked();
                AudioManager.get().setSoundMuted(!enable);
                prefs.putBoolean("sound_muted", !enable);
                prefs.flush();
            }
        });
        return soundToggle;
    }

    private Table difficultyRow() {
        Table row = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);
        int current = (User.currentUser != null && User.currentUser.userState != null)
                ? User.currentUser.userState.difficultyLevel : 3;

        for (int level = MIN_DIFFICULTY; level <= MAX_DIFFICULTY; level++) {
            TextButton button = new TextButton(String.valueOf(level), skin, "gender-button");
            button.setChecked(level == current);
            int chosenLevel = level;
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (User.currentUser != null && User.currentUser.userState != null) {
                        if (chosenLevel != User.currentUser.userState.difficultyLevel) {
                            runCommand("menu settings change-difficulty -l " + chosenLevel);
                        }
                    }
                }
            });
            group.add(button);
            row.add(button).pad(4).width(56);
        }
        return row;
    }

    private Table gameSpeedRow() {
        Table row = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);
        int current = GameSettings.get().getGameSpeed();

        for (int speed = MIN_GAME_SPEED; speed <= MAX_GAME_SPEED; speed++) {
            TextButton button = new TextButton(speed + "x", skin, "gender-button");
            button.setChecked(speed == current);
            int chosenSpeed = speed;
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    GameSettings.get().setGameSpeed(chosenSpeed);
                    GameSettings.get().save();
                    GeneralPrinter.print("Game speed set to " + chosenSpeed + "x.");
                }
            });
            group.add(button);
            row.add(button).pad(4).width(70);
        }
        return row;
    }

    private CheckBox gridToggle() {
        CheckBox showGrid = new CheckBox(" Show lawn grid lines", skin, "main");
        showGrid.setChecked(GameSettings.get().isShowGrid());
        showGrid.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameSettings.get().setShowGrid(showGrid.isChecked());
                GameSettings.get().save();
                GeneralPrinter.print("Lawn grid " + (showGrid.isChecked() ? "shown." : "hidden."));
            }
        });
        return showGrid;
    }

    private CheckBox debugToggle() {
        CheckBox debugMode = new CheckBox(" Debug mode (cheat buttons in-game)", skin, "main");
        debugMode.setChecked(GameSettings.get().isDebugMode());
        debugMode.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameSettings.get().setDebugMode(debugMode.isChecked());
                GameSettings.get().save();
                GeneralPrinter.print("Debug mode " + (debugMode.isChecked() ? "enabled." : "disabled."));
            }
        });
        return debugMode;
    }

    

    private Slider.SliderStyle createSafeSliderStyle() {
        try {
            if (skin != null && skin.has("default-horizontal", Slider.SliderStyle.class)) {
                return skin.get("default-horizontal", Slider.SliderStyle.class);
            }
        } catch (Exception ignored) {}

        Pixmap bgPixmap = new Pixmap(150, 6, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(new Color(0.3f, 0.3f, 0.3f, 0.8f));
        bgPixmap.fill();
        Texture bgTex = new Texture(bgPixmap);
        bgPixmap.dispose();

        Pixmap knobPixmap = new Pixmap(18, 18, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(new Color(0.9f, 0.75f, 0.2f, 1f));
        knobPixmap.fillCircle(9, 9, 8);
        Texture knobTex = new Texture(knobPixmap);
        knobPixmap.dispose();

        Slider.SliderStyle style = new Slider.SliderStyle();
        style.background = new TextureRegionDrawable(bgTex);
        style.knob = new TextureRegionDrawable(knobTex);
        return style;
    }

    

    private Drawable createTranslucentCardDrawable() {
        Pixmap pixmap = new Pixmap(650, 480, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.12f, 0.10f, 0.05f, 0.75f));
        pixmap.fill();
        pixmap.setColor(new Color(0.45f, 0.34f, 0.10f, 0.85f));
        pixmap.drawRectangle(0, 0, 650, 480);
        pixmap.drawRectangle(1, 1, 648, 478);
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }
}