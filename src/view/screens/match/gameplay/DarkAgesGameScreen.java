package view.screens.match.gameplay;

import model.utils.GameSession;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;

/**
 * Dark Ages gameplay entry point. The actual gameplay machinery lives in
 * GameScreen so the model/input/rendering path is identical to the other
 * seasons; this class only selects the Dark Ages map/visual context by
 * setting seasonFolder - GameScreen derives map.png/texture_left.png/
 * texture_right.png/grave.png from "chapters/<seasonFolder>/gameplay/" itself.
 */
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
