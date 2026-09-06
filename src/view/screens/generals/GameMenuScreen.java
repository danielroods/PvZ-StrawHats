package view.screens.generals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class GameMenuScreen extends UiScreen {

    /*
     * Background artwork:
     *
     * 1536 x 1024
     *
     * Virtual game coordinates:
     *
     * 1280 x 720
     *
     * The background is stretched to the virtual viewport,
     * therefore all clickable areas below are already expressed
     * in the 1280 x 720 coordinate system.
     */

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private Group buttonLayer;

    // ============================================================
    // MENU AREAS
    // ============================================================
    /*
     * GAMING CLUB / MINI-GAMES
     *
     * Upper-middle gaming room.
     */
    private static final float GAMING_X = VIRTUAL_WIDTH/3-150;
    private static final float GAMING_Y = VIRTUAL_HEIGHT/2+30;
    private static final float GAMING_W = 100f;
    private static final float GAMING_H = 40f;


    /*
     * COLLECTION
     *
     * Collection / decoration area.
     */
    private static final float COLLECTION_X = VIRTUAL_WIDTH/2-50;
    private static final float COLLECTION_Y = VIRTUAL_HEIGHT/4-70;
    private static final float COLLECTION_W = 110f;
    private static final float COLLECTION_H = 40f;


    /*
     * GREENHOUSE
     *
     * Glass greenhouse on the upper-right.
     */
    private static final float GREENHOUSE_X = VIRTUAL_WIDTH*2/3+120;
    private static final float GREENHOUSE_Y = VIRTUAL_HEIGHT/2;
    private static final float GREENHOUSE_W = 110f;
    private static final float GREENHOUSE_H = 40f;


    /*
     * ADVENTURE
     *
     * Large adventure/world-map area in the lower-left.
     */
    private static final float ADVENTURE_X = VIRTUAL_WIDTH/4-50;
    private static final float ADVENTURE_Y = 95f;
    private static final float ADVENTURE_W = 110f;
    private static final float ADVENTURE_H = 40f;


    /*
     * ACHIEVEMENTS / TROPHIES
     *
     * Trophy/library area in the middle-right.
     */
    private static final float ACHIEVEMENTS_X = VIRTUAL_WIDTH/2-40;
    private static final float ACHIEVEMENTS_Y = VIRTUAL_HEIGHT/2;
    private static final float ACHIEVEMENTS_W = 100f;
    private static final float ACHIEVEMENTS_H = 40f;


    /*
     * LEADERBOARD
     *
     * Board/display in the lower-right.
     *
     * This is intentionally separate from achievements.
     */
    private static final float LEADERBOARD_X = VIRTUAL_WIDTH-300;
    private static final float LEADERBOARD_Y = 100f;
    private static final float LEADERBOARD_W = 130f;
    private static final float LEADERBOARD_H = 40f;
    private Texture textBackgroundTexture;


    @Override
    public void show() {
        setBackground(
                "assets/images/backg/gamemenu.png"
        );
        super.show();

        createTextBackground();
        createMenuButtons();
    }


    private void createMenuButtons() {

        buttonLayer = new Group();

        buttonLayer.setSize(
                VIRTUAL_WIDTH,
                VIRTUAL_HEIGHT
        );

        buttonLayer.setTouchable(Touchable.enabled);

        addBeforeModal(buttonLayer);


        // ========================================================
        // CREATE BUTTONS
        // ========================================================




        TextButton gaming = createButton(
                "console",
                GAMING_X,
                GAMING_Y,
                GAMING_W,
                GAMING_H
        );


        TextButton collection = createButton(
                "collection",
                COLLECTION_X,
                COLLECTION_Y,
                COLLECTION_W,
                COLLECTION_H
        );


        TextButton greenhouse = createButton(
                "greenhouse",
                GREENHOUSE_X,
                GREENHOUSE_Y,
                GREENHOUSE_W,
                GREENHOUSE_H
        );


        TextButton adventure = createButton(
                "adventure",
                ADVENTURE_X,
                ADVENTURE_Y,
                ADVENTURE_W,
                ADVENTURE_H
        );


        TextButton achievements = createButton(
                "trophies",
                ACHIEVEMENTS_X,
                ACHIEVEMENTS_Y,
                ACHIEVEMENTS_W,
                ACHIEVEMENTS_H
        );


        TextButton leaderboard = createButton(
                "leaderboard",
                LEADERBOARD_X,
                LEADERBOARD_Y,
                LEADERBOARD_W,
                LEADERBOARD_H
        );


        // ========================================================
        // ADD BUTTONS
        // ========================================================


        buttonLayer.addActor(gaming);
        buttonLayer.addActor(collection);
        buttonLayer.addActor(greenhouse);
        buttonLayer.addActor(adventure);
        buttonLayer.addActor(achievements);
        buttonLayer.addActor(leaderboard);


        // ========================================================
        // FUNCTIONS
        // ========================================================




        gaming.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                runCommand("menu enter console");
            }
        });


        collection.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                runCommand("menu enter collection");
            }
        });


        greenhouse.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                runCommand("menu greenhouse");
            }
        });


        adventure.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                runCommand("menu enter adventure");
            }
        });


        achievements.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                runCommand("menu enter trophies");
            }
        });


        leaderboard.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                runCommand("menu leaderboard");
            }
        });
    }

    private void createTextBackground() {

        Pixmap pixmap = new Pixmap(
                1,
                1,
                Pixmap.Format.RGBA8888
        );

        pixmap.setColor(
                0f,
                0f,
                0f,
                0.50f
        );

        pixmap.fill();

        textBackgroundTexture = new Texture(pixmap);

        pixmap.dispose();
    }


    /**
     * Creates an invisible TextButton.
     *
     * The visual wooden signs / boards are already part of
     * the background artwork.
     */
    private TextButton createButton(
            String text,
            float x,
            float y,
            float width,
            float height
    ) {

        TextButton.TextButtonStyle style =
                new TextButton.TextButtonStyle();


        /*
         * ----------------------------------------------------------
         * BUTTON BACKGROUND
         * ----------------------------------------------------------
         *
         * Instead of putting text directly on the artwork,
         * we give it a dark translucent background.
         */



        /*
         * ----------------------------------------------------------
         * FONT
         * ----------------------------------------------------------
         */

        BitmapFont font;

        if (skin.has(
                "default-font",
                BitmapFont.class
        )) {

            font = skin.getFont("default-font");

        } else {

            font = new BitmapFont();
        }

        style.font = font;

        style.fontColor = Color.WHITE;
        style.overFontColor = Color.WHITE;
        style.downFontColor = Color.WHITE;


        /*
         * Create button with empty text.
         *
         * We'll add our own Label so we can control
         * font size independently.
         */

        TextButton button = new TextButton("", style);

        button.setBounds(
                x,
                y,
                width,
                height
        );

        button.setTouchable(Touchable.enabled);

        TextureRegionDrawable background =
                new TextureRegionDrawable(
                        new TextureRegion(textBackgroundTexture)
                );

        style.up = background;
        style.down = background;
        style.over = background;
        style.checked = background;

        /*
         * ----------------------------------------------------------
         * LARGE TEXT LABEL
         * ----------------------------------------------------------
         */

        Label.LabelStyle labelStyle =
                new Label.LabelStyle();

        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;


        Label label = new Label(
                text,
                labelStyle
        );

        /*
         * Bigger text.
         *
         * 1.35 = 35% larger
         */
        label.setFontScale(1.35f);

        /*
         * Center the text.
         */
        label.setAlignment(
                com.badlogic.gdx.utils.Align.center
        );

        /*
         * Make label fill the button.
         */

        label.setBounds(
                0,
                0,
                width,
                height
        );

        /*
         * The label itself must NOT consume clicks.
         */
        label.setTouchable(Touchable.disabled);

        button.addActor(label);

        return button;
    }
}