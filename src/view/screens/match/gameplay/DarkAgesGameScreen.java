package view.screens.match.gameplay;

import model.utils.GameSession;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;


public class DarkAgesGameScreen extends GameScreen {
    public DarkAgesGameScreen() {
        seasonFolder = "darkage";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.DARK_AGES_MUSIC, true);
        GameSession session = GameSession.peekInstance();
        if (session != null && session.getLevel() != null) {
        }
    }
}
