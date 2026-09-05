package view.screens.match.gameplay;

import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;

/**
 * Pirate Seas gameplay entry point. The actual gameplay machinery lives in
 * GameScreen so the model/input/rendering path is identical to the other
 * seasons; this class only selects the Pirates visual context by setting
 * seasonFolder - ZombieAnimationRegistry.pathFor(alias, seasonFolder) uses
 * this to pick the ZOMBIE_PIRATE_* animations (basic/flag/imp/gargantuar)
 * instead of falling back to GameScreen's default "egypt" seasonFolder.
 * <p>
 * There's no dedicated pirate map/tileset yet (see model.match.main.season.
 * travellog.pirate.Pirate), so this reuses the Egyptian music track as a
 * placeholder the same way BeforeMatchScreen intentionally reuses the
 * Egyptian map background for the pirate stage-select preview.
 */
public class PirateGameScreen extends GameScreen {
    public PirateGameScreen() {
        seasonFolder = "pirate";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.EGYPT_MUSIC, true);
    }
}