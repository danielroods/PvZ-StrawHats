package view.screens.generals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

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
    private static final float GAMING_X = 490f;
    private static final float GAMING_Y = 585f;
    private static final float GAMING_W = 265f;
    private static final float GAMING_H = 100f;


    /*
     * COLLECTION
     *
     * Collection / decoration area.
     */
    private static final float COLLECTION_X = 205f;
    private static final float COLLECTION_Y = 470f;
    private static final float COLLECTION_W = 235f;
    private static final float COLLECTION_H = 70f;


    /*
     * GREENHOUSE
     *
     * Glass greenhouse on the upper-right.
     */
    private static final float GREENHOUSE_X = 850f;
    private static final float GREENHOUSE_Y = 470f;
    private static final float GREENHOUSE_W = 225f;
    private static final float GREENHOUSE_H = 70f;


    /*
     * ADVENTURE
     *
     * Large adventure/world-map area in the lower-left.
     */
    private static final float ADVENTURE_X = 65f;
    private static final float ADVENTURE_Y = 45f;
    private static final float ADVENTURE_W = 300f;
    private static final float ADVENTURE_H = 120f;


    /*
     * ACHIEVEMENTS / TROPHIES
     *
     * Trophy/library area in the middle-right.
     */
    private static final float ACHIEVEMENTS_X = 735f;
    private static final float ACHIEVEMENTS_Y = 275f;
    private static final float ACHIEVEMENTS_W = 350f;
    private static final float ACHIEVEMENTS_H = 65f;


    /*
     * LEADERBOARD
     *
     * Board/display in the lower-right.
     *
     * This is intentionally separate from achievements.
     */
    private static final float LEADERBOARD_X = 950f;
    private static final float LEADERBOARD_Y = 120f;
    private static final float LEADERBOARD_W = 275f;
    private static final float LEADERBOARD_H = 150f;


    @Override
    public void show() {
        super.show();

        setBackground(
                "assets/images/backg/gamemenu.png"
        );

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

        style.up = null;
        style.down = null;
        style.over = null;
        style.checked = null;

        if (skin.has(
                "default-font",
                com.badlogic.gdx.graphics.g2d.BitmapFont.class
        )) {
            style.font = skin.getFont("default-font");
        } else {
            style.font =
                    new com.badlogic.gdx.graphics.g2d.BitmapFont();
        }

        style.fontColor = Color.WHITE;
        style.overFontColor = Color.WHITE;
        style.downFontColor = Color.WHITE;

        TextButton button = new TextButton(text, style);

        button.setBounds(
                x,
                y,
                width,
                height
        );

        button.setTouchable(Touchable.enabled);

        return button;
    }
}