package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import controller.ScreenManager;
import controller.match.mini_games.MiniGameEndMenu;
import controller.match.mini_games.ZombotanyController;
import model.App;
import model.collections.zombie.Zombie;
import model.match.mini_games.Zombotany;
import service.GameClock;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;
import view.screens.generals.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ZombotanyGameScreen extends GameScreen {

    private static final String JALAPENO_FIRE_PAM =
            "768/INITIAL/EFFECTS/JALAPENO_FIRE/JALAPENO_FIRE.PAM";
    private static final String ZOMBIE_PEA_PAM =
            "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM";
    private static final String ZOMBIE_PEA_SPLAT_PAM =
            "768/INITIAL/EFFECTS/SPLAT_PEA/SPLAT_PEA.PAM";
    private static final float LANE_FIRE_DURATION = 1.6f;
    private static final float LANE_FIRE_SCALE = 0.5f;
    private static final float FINAL_WAVE_ALERT_LEAD = 5.0f;
    private static final float ALERT_DURATION = 3.2f;

    {
        seasonFolder = "zombotany";
    }

    private static final class LaneFire {
        final int row;
        final int cols;
        float time;

        LaneFire(int row, int cols) {
            this.row = row;
            this.cols = cols;
        }
    }

    private final List<LaneFire> laneFires = new ArrayList<>();

    private Zombotany activeGame;
    private Label alertLabel;
    private Table alertBox;
    private float alertTimer;
    private int lastWaveCount = -1;
    private boolean finalWaveAnnounced;

    @Override
    protected String getSeasonGameplayFolder() {
        return "assets/images/backg/mini_games/zombotany/";
    }

    @Override
    protected String getGameplayBackgroundPath() {
        return getSeasonGameplayFolder() + "map.png";
    }

    @Override
    protected String getGraveIconPath() {
        return "";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MINI_GAME_MUSIC, true);
        activeGame = currentGame();
        laneFires.clear();
        lastWaveCount = -1;
        finalWaveAnnounced = false;
        if (hud != null) {
            hud.setLoadoutBankVisible(true);
            hud.setShovelVisible(true);
            hud.setStartButtonAvailable(false);
            hud.setObjectiveOverride("STOP THE PLANT-HEADED ZOMBIES");
        }
        buildAlertBox();
        preloadZombotanyArt();
        Toast.show(stage, "These zombies wear the plants they ate - and use their abilities.");
    }

    private void preloadZombotanyArt() {
        List<String> paths = new ArrayList<>(ZombotanyArt.allPamPaths());
        paths.add(JALAPENO_FIRE_PAM);
        paths.add(ZOMBIE_PEA_PAM);
        paths.add(ZOMBIE_PEA_SPLAT_PAM);
        preloadPam(paths.toArray(new String[0]));
    }

    private Zombotany currentGame() {
        return App.currentMenu instanceof ZombotanyController controller
                ? controller.getGame() : null;
    }

    private Zombotany game() {
        Zombotany live = currentGame();
        if (live != null && live != activeGame) {
            activeGame = live;
            session = live.getSession();
            laneFires.clear();
            lastWaveCount = -1;
            finalWaveAnnounced = false;
            alertTimer = 0f;
        }
        return activeGame;
    }

    @Override
    protected void tickSession() {
        if (App.currentMenu instanceof ZombotanyController controller) {
            game();
            controller.tick(GameClock.SECONDS_PER_TICK);
        }
    }

    @Override
    protected List<String> loadoutPlants() {
        Zombotany game = game();
        return game == null ? super.loadoutPlants() : new ArrayList<>(game.getAvailablePlants());
    }

    @Override
    protected void checkMatchEnd() {
        if (matchFinished || !isMatchEndSequenceIdle()) return;
        if (App.currentMenu instanceof ZombotanyController) return;

        if (App.currentMenu instanceof MiniGameEndMenu end) {
            startMatchEndSequence(end.isWon());
            return;
        }
        matchFinished = true;
        ScreenManager.syncWithCurrentMenu();
    }

    @Override
    public void onMatchEndSequenceFinished(boolean won) {
    }

    @Override
    protected void onZombieDied(Zombie zombie) {
        Zombotany game = game();
        if (game == null || zombie == null || zombie.getPosition() == null) return;
        if (!Zombotany.JALAPENO_ZOMBIE.equals(zombie.getAlias())) return;
        int row = (int) Math.round(zombie.getPosition().y());
        laneFires.add(new LaneFire(row, session.getCols()));
        Toast.show(stage, "The Jalapeno Zombie torched lane " + (row + 1) + "!");
    }

    @Override
    protected void refreshHud(float delta) {
        Zombotany game = game();
        int spawned = game == null ? 0 : game.getWavesSurvived();
        int total = game == null ? 1 : Math.max(1, game.getTotalWaves());
        super.refreshHud(delta);
        if (hud == null || game == null) return;
        updateWaveAlerts(delta, game, spawned, total);
    }

    private void updateWaveAlerts(float delta, Zombotany game, int spawned, int total) {
        if (lastWaveCount < 0) {
            lastWaveCount = spawned;
        } else if (spawned > lastWaveCount) {
            lastWaveCount = spawned;
            showAlert(spawned >= total ? "FINAL WAVE!" : "WAVE " + spawned);
        }

        double untilNext = game.getSession().getSecondsUntilNextWave();
        if (!finalWaveAnnounced && spawned == total - 1 && untilNext >= 0
                && untilNext <= FINAL_WAVE_ALERT_LEAD) {
            finalWaveAnnounced = true;
            showAlert("A HUGE WAVE OF ZOMBOTANY IS APPROACHING!");
        }

        if (alertTimer > 0f) {
            alertTimer = Math.max(0f, alertTimer - delta);
            if (alertBox != null) {
                alertBox.setVisible(true);
                alertBox.getColor().a = Math.min(1f, alertTimer / 0.6f);
            }
        } else if (alertBox != null) {
            alertBox.setVisible(false);
        }
    }

    private void buildAlertBox() {
        if (alertBox != null) {
            alertBox.remove();
            alertBox = null;
        }
        Table box = new Table();
        box.setBackground(skin.getDrawable("modal-background"));
        box.pad(10f, 26f, 10f, 26f);
        alertLabel = new Label("", skin, "title");
        alertLabel.setAlignment(Align.center);
        alertLabel.setColor(new Color(1f, 0.86f, 0.35f, 1f));
        box.add(alertLabel).center();
        box.pack();
        box.setVisible(false);
        alertBox = box;
        stage.addActor(alertBox);
    }

    private void showAlert(String text) {
        if (alertLabel == null || alertBox == null) return;
        alertLabel.setText(text);
        alertBox.pack();
        alertBox.setPosition((stage.getViewport().getWorldWidth() - alertBox.getWidth()) * 0.5f,
                stage.getViewport().getWorldHeight() - alertBox.getHeight() - 96f);
        alertTimer = ALERT_DURATION;
        alertBox.setVisible(true);
    }

    @Override
    protected void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        super.drawSeasonForegroundEffects(delta, bw, bh);
        drawLaneFires(delta);
    }

    private void drawLaneFires(float delta) {
        if (laneFires.isEmpty()) return;
        float tileW = getBoardTileWidth();
        float tileH = getBoardTileHeight();
        Iterator<LaneFire> iterator = laneFires.iterator();
        while (iterator.hasNext()) {
            LaneFire fire = iterator.next();
            fire.time += delta;
            if (fire.time > LANE_FIRE_DURATION) {
                iterator.remove();
                continue;
            }
            float fade = 1f - fire.time / LANE_FIRE_DURATION;
            batch.setColor(1f, 1f, 1f, Math.min(1f, 0.35f + fade));
            for (int col = 0; col < fire.cols; col++) {
                float x = getCellX(col) + tileW * 0.3f;
                float y = getCellY(fire.row) + tileH * 0.34f;
                if (!drawPam(JALAPENO_FIRE_PAM, "idle", fire.time, x, y, LANE_FIRE_SCALE, false)) {
                    drawFallback(getCellX(col), getCellY(fire.row), tileW, tileH,
                            new Color(1f, 0.45f, 0.12f, 0.45f * fade));
                }
            }
            batch.setColor(Color.WHITE);
        }
    }
}
