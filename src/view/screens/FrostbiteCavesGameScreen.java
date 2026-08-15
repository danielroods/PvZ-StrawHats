package view.screens;

import model.utils.GameSession;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.general_screens.GameScreen;

/**
 * Frostbite Caves gameplay entry point. The actual gameplay machinery lives
 * in GameScreen so the model/input/rendering path is identical to the other
 * seasons; this class only selects the Frostbite Caves map/visual context by
 * setting seasonFolder - GameScreen derives map.png/texture_left.png/
 * texture_right.png/grave.png from "chapters/<seasonFolder>/gameplay/" itself.
 */
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
