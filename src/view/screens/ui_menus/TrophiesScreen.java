package view.screens.ui_menus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import controller.TrophyManager;
import controller.assets.AssetPaths;
import model.user_data.User;
import model.resoures.CurrencyType;
import model.user_data.UserState;
import view.screens.generals.Modal;
import view.screens.generals.UiScreen;

import java.util.List;


public class TrophiesScreen extends UiScreen {

    private static final float SHELF_WIDTH = 900f;
    private static final float TROPHY_SIZE_X = 135f;
    private static final float TROPHY_SIZE_Y = 240f;


    private final TrophyManager manager = new TrophyManager();

    @Override
    public void show() {
        setBackground(AssetPaths.TROPHIES_CABINET_BG);
        super.show();
        build();
    }

    @Override
    protected void refreshContent() {
        build();
    }

    private void build() {

        rootTable.clear();
        rootTable.setBackground(new TextureRegionDrawable(loadTextureSafe(AssetPaths.TROPHIES_CABINET_BG)));

        Table topBar = buildTopBar();

        Table shelves = new Table();
        shelves.top();
        List<TrophyManager.TrophyEntry> entries = manager.getTrophyShelf(currentUserState());
        for (TrophyManager.TrophyEntry entry : entries) {
            shelves.add(buildShelf(entry)).width(SHELF_WIDTH).padBottom(SPACE_LG).row();
        }

        ScrollPane scrollPane = scrollable(shelves);

        rootTable.add(topBar).fillX().padTop(5).padLeft(15).padRight(15).row();
        rootTable.add(scrollPane).expand().fill().padTop(SPACE_MD).row();
    }

    private UserState currentUserState() {
        User user = User.currentUser;
        return (user != null) ? user.userState : new UserState(new java.util.ArrayList<>(), 0, 0, 0);
    }

    private Table buildTopBar() {
        ImageButton backBtn = createIconButton("assets/images/ui/buttons_hud_back_normal.png", 54, 54,
                () -> runCommand("menu exit"));


        Table topLeft = new Table();
        topLeft.left();
        topLeft.add(backBtn).width(120).height(48).padRight(20);
        topLeft.add(new Label("Trophy Cabinet", skin, "title")).left();

        User user = User.currentUser;
        int coins = (user != null && user.userState != null) ? user.userState.coins : 0;
        int diamonds = (user != null && user.userState != null) ? user.userState.diamonds : 0;

        Table topRight = new Table();
        topRight.right();
        topRight.add(currencyWidget(CurrencyType.COIN, coins)).padRight(15);
        topRight.add(currencyWidget(CurrencyType.DIAMOND, diamonds));

        Table topBar = new Table();
        topBar.add(topLeft).left().expandX();
        topBar.add(topRight).right();
        return topBar;
    }

    
    private Table buildShelf(TrophyManager.TrophyEntry entry) {
        Table shelf = new Table();
        Drawable shelfBg = loadDrawableSafe(AssetPaths.TROPHIES_SHELF_TILE);
        if (shelfBg != null) {
            shelf.setBackground(shelfBg);
        } else {
            shelf.setBackground(solidColorDrawable(new Color(0.24f, 0.16f, 0.08f, 0.9f)));
        }
        shelf.pad(SPACE_LG);

        Label chapterLabel = new Label(entry.chapterName(), skin, "title");
        chapterLabel.setAlignment(Align.center);
        shelf.add(chapterLabel).colspan(2).padBottom(SPACE_SM).row();

        Table slotsRow = new Table();
        slotsRow.add(buildTrophySlot(entry));
        shelf.add(slotsRow).colspan(2);

        return shelf;
    }

    private Table buildTrophySlot(TrophyManager.TrophyEntry entry) {
        Table slot = new Table();
        String imagePath = entry.earned() ? entry.trophyImagePath() : AssetPaths.TROPHY_LOCKED_SILHOUETTE;
        Image trophyImage = new Image(loadTextureSafe(imagePath));
        trophyImage.setColor(1f, 1f, 1f, entry.earned() ? 1f : 0.35f);

        Table imageBox = new Table();
        imageBox.setTransform(true);
        imageBox.add(trophyImage).size(TROPHY_SIZE_X, TROPHY_SIZE_Y);

        if (entry.earned()) {
            imageBox.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    new TrophyDetailModal(entry).show();
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    imageBox.addAction(Actions.scaleTo(1.1f, 1.1f, 0.12f));
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    imageBox.addAction(Actions.scaleTo(1f, 1f, 0.12f));
                }
            });
            imageBox.setOrigin(TROPHY_SIZE_X / 2f, TROPHY_SIZE_Y / 2f);
        }

        slot.add(imageBox).size(TROPHY_SIZE_X, TROPHY_SIZE_Y).row();
        slot.add(new Label(entry.earned() ? "Trophy" : "???", skin, "muted")).padTop(4);
        return slot;
    }



    private Drawable loadDrawableSafe(String path) {
        Texture texture = loadTextureSafe(path);
        return texture == null ? null : new TextureRegionDrawable(texture);
    }

    private Drawable solidColorDrawable(Color color) {
        Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    
    private class TrophyDetailModal extends Modal {
        TrophyDetailModal(TrophyManager.TrophyEntry entry) {
            Image bigTrophy = new Image(loadTextureSafe(entry.trophyImagePath()));
            content.add(bigTrophy).size(TROPHY_SIZE_X * 2f, TROPHY_SIZE_Y * 2f).padBottom(SPACE_MD).row();

            Label title = new Label(entry.chapterName() + " Trophy", skin, "title");
            title.setAlignment(Align.center);
            title.setWrap(true);
            content.add(title).width(360).padBottom(SPACE_SM).row();

            Label subtitle = new Label(entry.trophyDescription(), skin, "muted");
            subtitle.setWrap(true);
            subtitle.setAlignment(Align.center);
            content.add(subtitle).width(360f).padBottom(SPACE_MD).row();

            content.add(secondaryButton("Close", this::hide)).width(160).height(48);
        }
    }
}
