package view.screens.match.gameplay;

import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;

public class FutureGameScreen extends GameScreen {

    public FutureGameScreen() {
        seasonFolder = "future";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.DARK_AGES_MUSIC, true);
    }
}
