package view.screens.match.gameplay;

import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;

/** Future currently reuses the Dark Ages gameplay visuals and map assets. */
public class FutureGameScreen extends GameScreen {
    public FutureGameScreen() {
        // Deliberately use Dark Ages' folder so the temporary Future chapter
        // gets the exact same gameplay map/background/visual assets.
        seasonFolder = "darkage";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.DARK_AGES_MUSIC, true);
    }
}
