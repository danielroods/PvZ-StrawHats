package view.screens.generals;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/** The in-match pause dialog: resume, restart the level, or save and leave. */
class PauseModal extends Modal {

    PauseModal(GameScreen screen) {
        content.add(new Label("PAUSED", skin, "title")).colspan(2).padBottom(15).row();
        TextButton resume = new TextButton("Resume", skin);
        resume.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { screen.paused = false; hide(); } });
        content.add(resume).size(220, 50).pad(5).row();
        TextButton restart = new TextButton("Restart", skin);
        restart.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                // "restart" (see GameplayMenu.restartMatch()) sets App.currentMenu to a
                // *new* BeforeMenu or GameplayMenu instance rather than mutating this
                // screen's session in place. For BeforeMenu that's a different class from
                // whatever menu we're restarting from, so plain syncWithCurrentMenu() (used
                // by "menu exit" below) swaps the screen correctly on its own. But
                // conveyor-belt levels restart straight back into a new GameplayMenu - the
                // *same* class we were just in - and syncWithCurrentMenu() treats "same
                // menu class as before" as "nothing changed" and leaves the old GameScreen
                // on screen, still wired to the GameSession that "restart" just replaced
                // (paused stuck true, board frozen, nothing responds to clicks). Restart
                // always needs a fresh screen, so use forceResync() here instead, which
                // skips that same-class shortcut.
                if (screen.runCommand("restart")) {
                    hide();
                    controller.ScreenManager.forceResync();
                }
            }
        });
        content.add(restart).size(220, 50).pad(5).row();
        TextButton exit = new TextButton("Save & Exit", skin);
        exit.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                screen.runCommand("menu exit");
                hide();
                controller.ScreenManager.syncWithCurrentMenu();
            }
        });
        content.add(exit).size(220, 50).pad(5);
    }
}