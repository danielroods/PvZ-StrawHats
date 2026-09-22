package view.screens.match.gameplay;

import model.utils.GameSession;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;


public class FrostbiteCavesGameScreen extends GameScreen {
    public FrostbiteCavesGameScreen() {
        seasonFolder = "frostbite_cave";
    }
    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.FROSTBITE_MUSIC, true);
        GameSession session = GameSession.peekInstance();
        if (session != null && session.getLevel() != null) {
        }
    }
}
