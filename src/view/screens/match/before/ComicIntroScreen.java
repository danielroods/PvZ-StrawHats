package view.screens.match.before;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import controller.ScreenManager;
import view.screens.generals.ParticleCreator;
import view.screens.generals.UiScreen;

import java.util.ArrayList;
import java.util.List;

public class ComicIntroScreen extends UiScreen {

    private static final int ROWS = 3;
    private static final int COLS = 3;
    private static final int TOTAL_PANELS = ROWS * COLS;

    public static String COMIC_SHEET_PATH;
    protected static String[] particlePaths = new String[]{"assets/images/chapters/egypt/strawburst_plantfood_projectile_8x9.png"};
    private final List<Image> panelImages = new ArrayList<>();
    private Texture comicTexture;
    private int currentRevealedIndex = 0;
    private boolean isTransitioning = false;
    private final Runnable onCompleteAction;

    public ComicIntroScreen(Runnable onCompleteAction) {
        this.onCompleteAction = onCompleteAction;
    }

    @Override
    public void initParticles() {
        if (particles != null) {
            particles.dispose();
        }

        particles = new ParticleCreator(particlePaths, 30, 20f, 20f, 1.2f, true);
        Actor particleActor = particles.createActor();
        particleActor.setTouchable(Touchable.disabled);
        rootStack.addActorAt(1, particleActor);
    }

    @Override
    public void show() {
        super.show();
        buildComicGrid();
        stage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleNextStep();
            }
        });
        revealNextPanel();
    }

    private void buildComicGrid() {
        rootTable.clear();
        rootTable.setFillParent(true);
        rootTable.center();

        Table comicTable = new Table();
        comicTable.pad(15f);

        comicTexture = loadTextureSafe(COMIC_SHEET_PATH);

        int tileWidth = comicTexture.getWidth() / COLS;
        int tileHeight = comicTexture.getHeight() / ROWS;

        int inset = 2;

        float displayPanelWidth = 360f;
        float displayPanelHeight = 200f;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                TextureRegion panelRegion = new TextureRegion(
                        comicTexture,
                        c * tileWidth + inset,
                        r * tileHeight + inset,
                        tileWidth - (2 * inset),
                        tileHeight - (2 * inset)
                );

                Image panelImage = new Image(new TextureRegionDrawable(panelRegion));

                panelImage.setOrigin(displayPanelWidth / 2f, displayPanelHeight / 2f);

                panelImage.setColor(1f, 1f, 1f, 0f);
                panelImage.setScale(0.85f);
                panelImage.setTouchable(Touchable.disabled);

                panelImages.add(panelImage);
                comicTable.add(panelImage).size(displayPanelWidth, displayPanelHeight).pad(8f);
            }
            comicTable.row();
        }

        rootTable.add(comicTable).center();
    }

    private void handleNextStep() {
        if (isTransitioning) return;

        if (currentRevealedIndex < TOTAL_PANELS) {
            revealNextPanel();
        } else {
            finishComic();
        }
    }

    private void revealNextPanel() {
        if (currentRevealedIndex >= TOTAL_PANELS) return;

        Image panel = panelImages.get(currentRevealedIndex);

        panel.addAction(Actions.parallel(
                Actions.fadeIn(0.35f, Interpolation.smooth),
                Actions.scaleTo(1f, 1f, 0.35f, Interpolation.swingOut)
        ));
        currentRevealedIndex++;
    }

    private void finishComic() {
        isTransitioning = true;

        stage.getRoot().addAction(Actions.sequence(
                Actions.fadeOut(0.4f, Interpolation.fade),
                Actions.run(() -> {
                    if (onCompleteAction != null) {
                        onCompleteAction.run();
                    } else {
                        ScreenManager.setScreen(new BeforeMatchScreen());
                    }
                })
        ));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        if (comicTexture != null) {
            comicTexture.dispose();
        }
        super.dispose();
    }
}