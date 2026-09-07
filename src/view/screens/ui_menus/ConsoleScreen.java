package view.screens.ui_menus;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.UiScreen;

/** Minimal Console hub. Visual polish is intentionally deferred; gameplay is the focus. */
public class ConsoleScreen extends UiScreen {
    @Override public void show() {
        setBackground("assets/images/backg/mainmenu_background.png");
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        super.show();
        build();
    }

    private void build() {
        rootTable.clear();
        Table top = new Table();
        top.add(createIconButton("assets/images/ui/buttons_hud_back_normal.png", 54, 54,
                () -> runCommand("menu exit")).left());
        top.add(new Label("CONSOLE", skin, "title")).center();
        top.add().expandX();
        rootTable.add(top).fillX().row();

        Stack card = new Stack();
        Texture tex = loadTextureSafe("assets/images/ui/calendar_card_7day_mgpwinterevent.png");
        ImageButton image = new ImageButton(new TextureRegionDrawable(tex));
        Label label = new Label("Zombie Packman", skin, "title");
        label.setAlignment(Align.center);
        Table text = new Table();
        text.add(label).expand().bottom().padBottom(20);
        card.add(image);
        card.add(text);
        card.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                runCommand("menu enter zombie packman");
            }
        });
        rootTable.add(card).size(420, 260).padTop(35).center();
    }
}
