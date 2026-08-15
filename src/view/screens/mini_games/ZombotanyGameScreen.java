package view.screens.mini_games;

import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.general_screens.GameScreen;

/**
 * ZombotanyGameScreen mini-game gameplay entry point. The actual gameplay machinery
 * lives in GameScreen, exactly like the regular chapter stages; this class
 * only points it at the mini-game's own background. No mini-game-specific
 * rules are wired in yet.
 */
public class ZombotanyGameScreen extends GameScreen {

    @Override
    protected String getGameplayBackgroundPath() {
        return "assets/images/backg/mini_games/zombotany/texture.png";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
    }
}
