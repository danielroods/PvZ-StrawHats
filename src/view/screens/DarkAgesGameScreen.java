package view.screens;

import view.general_screens.GameScreen;

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
}
