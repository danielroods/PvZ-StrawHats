package view.screens.match.gameplay;

import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;

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