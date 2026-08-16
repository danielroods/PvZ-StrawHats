package view.screens.mini_games;

import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.general_screens.GameScreen;

/**
 * WallnutBowlingGameScreen mini-game gameplay entry point. The actual gameplay machinery
 * lives in GameScreen, exactly like the regular chapter stages; this class only
 * points it at the mini-game's own art folder (background, left/right border
 * textures, and grave icon all come from the same folder - see
 * GameScreen.getSeasonGameplayFolder()). No mini-game-specific rules are wired in yet.
 */
public class WallnutBowlingGameScreen extends GameScreen {

    @Override
    protected String getSeasonGameplayFolder() {
        return "assets/images/backg/mini_games/wallnut/";
    }

    @Override
    protected String getGameplayBackgroundPath() {
        return getSeasonGameplayFolder() + "texture.png";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
    }
}
