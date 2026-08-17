package view.screens.mini_games;

import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.general_screens.GameScreen;

/**
 * ZombotanyGameScreen mini-game gameplay entry point. The actual gameplay machinery
 * lives in GameScreen, exactly like the regular chapter stages; this class only
 * points it at the mini-game's own art folder (background, left/right border
 * textures, and grave icon all come from the same folder - see
 * GameScreen.getSeasonGameplayFolder()). No mini-game-specific rules are wired in yet.
 */
public class ZombotanyGameScreen extends GameScreen {

    {
        // Mini-games have no Level/Season, so this doubles as the lawn mower art
        // key - see GameScreen.getLawnMowerSeasonKey() and SEASON_LAWN_MOWER_PAM_PATHS.
        seasonFolder = "zombotany";
    }

    @Override
    protected String getSeasonGameplayFolder() {
        return "assets/images/backg/mini_games/zombotany/";
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
