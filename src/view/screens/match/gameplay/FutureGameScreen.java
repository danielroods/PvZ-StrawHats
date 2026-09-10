package view.screens.match.gameplay;

import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;

public class FutureGameScreen extends GameScreen {

    private static final String FUTURE_TEXTURE_LEFT =
            "assets/images/chapters/future/gameplay/texture_left.png";
    private static final String FUTURE_TEXTURE =
            "assets/images/chapters/future/gameplay/texture.png";

    public FutureGameScreen() {
        seasonFolder = "future";
    }

    @Override
    protected String[] getGameplayBackgroundLayers() {
        return new String[]{
                FUTURE_TEXTURE_LEFT,
                FUTURE_TEXTURE
        };
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.FUTURE_MUSIC, true);
    }
}
