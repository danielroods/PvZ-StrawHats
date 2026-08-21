package view.screens.ui_menus;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import controller.ui_menus.Menu;
import view.screens.generals.UiScreen;

public class PlaceholderScreen extends UiScreen {

    private final Menu menu;

    public PlaceholderScreen(Menu menu) {
        this.menu = menu;
    }

    @Override
    public void show() {
        super.show();
        build();
    }

    private void build() {
        rootTable.clear();

        TextField commandField = field(false);
        commandField.setTextFieldFilter(null);

        Label info = new Label(menu.showMenu(), skin, "muted");
        info.setWrap(true);

        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));
        card.pad(40).defaults().pad(8);
        card.add(new Label("PvZ", skin, "title")).colspan(2).padBottom(10).row();
        card.add(new Label(menu.getName(), skin, "title")).colspan(2).padBottom(12).row();
        card.add(new Label("Graphics for this menu haven't been built yet - the same commands from phase 1 still work here.", skin, "main"))
                .colspan(2).width(480).padBottom(8).row();
        card.add(new ScrollPane(info)).colspan(2).width(480).height(220).padBottom(12).row();
        card.add(new Label("Command", skin, "main")).left();
        card.add(commandField).width(320).row();
        card.add(primaryButton("Run", () -> runCommand(commandField.getText())))
                .colspan(2).padTop(12).width(220).row();

        showCard(card);
    }
}