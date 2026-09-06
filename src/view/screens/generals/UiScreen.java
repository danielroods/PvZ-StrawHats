package view.screens.generals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import controller.ScreenManager;
import model.App;
import model.game_exceptions.GameException;
import model.resoures.CurrencyType;
import view.GeneralPrinter;

import java.util.function.Consumer;

public abstract class UiScreen extends BaseScreen {

    protected static final float SPACE_XS = 2f;
    protected static final float SPACE_SM = 8f;
    protected static final float SPACE_MD = 10f;
    protected static final float SPACE_LG = 14f;
    protected static final float SPACE_XL = 22f;

    protected static final float CARD_PAD = 12f;
    protected static final float LABEL_WIDTH = 120f;
    protected static final float FIELD_WIDTH = 212f;
    protected static final float BUTTON_WIDTH = 270f;
    protected static final float CARD_MAX_HEIGHT = SCREEN_HEIGHT - 64f;

    protected static String[] particlePaths = new String[]{"assets/images/ui/hocus_crocus_49x28.png"};
    private final Consumer<String> printerListener = this::onMessage;

    protected static TextureRegion whitePixelRegion() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegion(texture);
    }

    private Texture loadLinearTexture(String path) {
        if (!Gdx.files.internal(path).exists()) {
            return null;
        }
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private Drawable loadDrawable(String path) {
        Texture texture = loadLinearTexture(path);
        return texture == null ? null : new TextureRegionDrawable(texture);
    }

    @Override
    public void setSkin() {
        this.skin = new Skin();
        BitmapFont font;
        String fontPath = "assets/fonts/FBUSV8C5EI.TTF";
        if (Gdx.files.internal(fontPath).exists()) {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 22;
            parameter.color = Color.WHITE;
            font = generator.generateFont(parameter);
            generator.dispose();
        } else {
            font = new BitmapFont();
            font.getData().setScale(1.15f);
        }
        skin.add("default-font", font);

        Drawable panel = loadDrawable("assets/images/ui/card_panel.png");
        if (panel == null) {
            panel = roundedPanel(64, 16, 3,
                    new Color(0.16f, 0.13f, 0.06f, 0.93f), new Color(0.62f, 0.47f, 0.16f, 1f));
        }
        skin.add("card-background", panel, Drawable.class);
        skin.add("modal-background", panel, Drawable.class);

        Drawable fieldBg = loadDrawable("assets/images/ui/textfield_bg.png");
        if (fieldBg == null) {
            fieldBg = roundedPanel(48, 10, 2,
                    new Color(0.09f, 0.08f, 0.05f, 0.9f), new Color(0.62f, 0.47f, 0.16f, 1f));
        }
        skin.add("textfield-bg", fieldBg, Drawable.class);

        skin.add("cursor", solidDrawable(3, 26, Color.GOLD), Drawable.class);
        skin.add("selection", solidDrawable(4, 26, new Color(0.85f, 0.65f, 0.15f, 0.45f)), Drawable.class);

        Drawable buttonUp = loadDrawable("assets/images/ui/button_up.png");
        Drawable buttonDown = loadDrawable("assets/images/ui/button_down.png");
        Drawable buttonOver;
        if (buttonUp == null || buttonDown == null) {
            buttonUp = roundedPanel(48, 12, 2,
                    new Color(0.80f, 0.60f, 0.16f, 1f), new Color(0.45f, 0.32f, 0.08f, 1f));
            buttonDown = roundedPanel(48, 12, 2,
                    new Color(0.55f, 0.40f, 0.10f, 1f), new Color(0.35f, 0.25f, 0.06f, 1f));
            buttonOver = roundedPanel(48, 12, 2,
                    new Color(0.90f, 0.70f, 0.22f, 1f), new Color(0.45f, 0.32f, 0.08f, 1f));
        } else {
            buttonOver = buttonUp;
        }
        skin.add("button-up", buttonUp, Drawable.class);
        skin.add("button-down", buttonDown, Drawable.class);
        skin.add("button-over", buttonOver, Drawable.class);

        Drawable checkOn = loadDrawable("assets/images/ui/checkbox_enabled.png");
        Drawable checkOff = loadDrawable("assets/images/ui/checkbox_disabled.png");
        if (checkOn == null || checkOff == null) {
            checkOn = roundedPanel(28, 6, 2,
                    new Color(0.35f, 0.62f, 0.30f, 1f), new Color(0.20f, 0.40f, 0.18f, 1f));
            checkOff = roundedPanel(28, 6, 2,
                    new Color(0.14f, 0.13f, 0.10f, 1f), new Color(0.62f, 0.47f, 0.16f, 1f));
        }
        skin.add("checkbox-on", checkOn, Drawable.class);
        skin.add("checkbox-off", checkOff, Drawable.class);

        skin.add("title", new Label.LabelStyle(font, Color.WHITE));
        skin.add("main", new Label.LabelStyle(font, Color.LIGHT_GRAY));
        skin.add("muted", new Label.LabelStyle(font, Color.GRAY));
        skin.add("default", new Label.LabelStyle(font, Color.LIGHT_GRAY));

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = skin.getDrawable("textfield-bg");
        textFieldStyle.cursor = skin.getDrawable("cursor");
        textFieldStyle.selection = skin.getDrawable("selection");
        textFieldStyle.messageFont = font;
        textFieldStyle.messageFontColor = Color.LIGHT_GRAY;
        skin.add("main", textFieldStyle);

        TextButton.TextButtonStyle mainButtonStyle = new TextButton.TextButtonStyle();
        mainButtonStyle.font = font;
        mainButtonStyle.fontColor = Color.WHITE;
        mainButtonStyle.up = skin.getDrawable("button-up");
        mainButtonStyle.down = skin.getDrawable("button-down");
        mainButtonStyle.over = skin.getDrawable("button-over");
        skin.add("main", mainButtonStyle);
        skin.add("default", mainButtonStyle);

        TextButton.TextButtonStyle secButtonStyle = new TextButton.TextButtonStyle(mainButtonStyle);
        secButtonStyle.fontColor = Color.LIGHT_GRAY;
        skin.add("secondary", secButtonStyle);

        TextButton.TextButtonStyle genderBtnStyle = new TextButton.TextButtonStyle(mainButtonStyle);
        genderBtnStyle.checked = skin.getDrawable("button-down");
        skin.add("gender-button", genderBtnStyle);

        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = Color.WHITE;
        checkBoxStyle.checkboxOn = skin.getDrawable("checkbox-on");
        checkBoxStyle.checkboxOff = skin.getDrawable("checkbox-off");
        skin.add("main", checkBoxStyle);
        skin.add("default", checkBoxStyle);
    }

    protected Texture loadTextureSafe(String path) {
        if (path != null && !path.isEmpty() && Gdx.files.internal(path).exists()) {
            Texture tex = new Texture(Gdx.files.internal(path));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return tex;
        }
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        Texture fallback = new Texture(pixmap);
        pixmap.dispose();
        return fallback;
    }

    protected ImageButton createIconButton(String path, float width, float height, Runnable action) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(loadTextureSafe(path));
        ImageButton button = new ImageButton(drawable);
        button.getImageCell().size(width, height);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    protected Actor createIconButtonWithLabel(String path, float width, float height, String text, Runnable action) {
        Table container = new Table();
        ImageButton btn = createIconButton(path, width, height, action);
        Label label = new Label(text, skin, "title");
        label.setAlignment(Align.center);

        container.add(btn).row();
        container.add(label).padTop(2);

        container.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return container;
    }

    protected Table createResourceWidget(String iconPath, String value) {
        Stack stack = new Stack();
        Table bgTable = new Table();
        bgTable.setBackground(new TextureRegionDrawable(loadTextureSafe(iconPath)));

        Table textTable = new Table();
        textTable.add(new Label(value, skin, "title")).center().expand();

        stack.add(bgTable);
        stack.add(textTable);

        Table outer = new Table();
        outer.add(stack).size(130, 42);
        return outer;
    }

    /**
     * A coin/diamond top-bar widget that also works as the "generic cheat" trigger:
     * touching it opens {@link CurrencyCheatModal}, letting the player type an amount
     * of that currency to add to the current account. Used by every menu that shows
     * a coin or diamond counter (main, game, stages, greenhouse, collection, shop),
     * so the cheat behaviour lives in exactly one place.
     */
    protected Table currencyWidget(CurrencyType currency, int amount) {
        Table widget = createResourceWidget(currency.getIconPath(), String.valueOf(amount));
        widget.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CurrencyCheatModal.open(UiScreen.this::refreshContent);
            }
        });
        return widget;
    }

    /**
     * Called after the currency cheat popup changes the user's coins/diamonds so the
     * screen can redraw itself with the new values. Screens that display a coin or
     * diamond widget (via {@link #currencyWidget}) should override this to call their
     * own rebuild method (usually a private {@code build()}).
     */
    protected void refreshContent() {
    }

    /**
     * Public entry point so code outside this screen (e.g. the local TA offer web server,
     * which updates the account from a background HTTP thread) can ask an already-open
     * screen to redraw itself with fresh model data, instead of the player having to leave
     * and reopen the screen to see the change.
     */
    public void refresh() {
        refreshContent();
    }

    private Drawable roundedPanel(int tile, int radius, int border, Color fill, Color borderColor) {
        Pixmap outer = new Pixmap(tile, tile, Pixmap.Format.RGBA8888);
        outer.setColor(borderColor);
        fillRounded(outer, 0, 0, tile, tile, radius);

        int innerSize = tile - border * 2;
        Pixmap inner = new Pixmap(innerSize, innerSize, Pixmap.Format.RGBA8888);
        inner.setColor(fill);
        fillRounded(inner, 0, 0, innerSize, innerSize, Math.max(0, radius - border));
        outer.drawPixmap(inner, border, border);
        inner.dispose();

        Texture texture = new Texture(outer);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        outer.dispose();

        int inset = Math.max(border + 1, radius);
        NinePatch patch = new NinePatch(texture, inset, inset, inset, inset);
        return new NinePatchDrawable(patch);
    }

    private void fillRounded(Pixmap pixmap, int x, int y, int w, int h, int radius) {
        radius = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        pixmap.fillRectangle(x + radius, y, w - 2 * radius, h);
        pixmap.fillRectangle(x, y + radius, w, h - 2 * radius);
        pixmap.fillCircle(x + radius, y + radius, radius);
        pixmap.fillCircle(x + w - radius - 1, y + radius, radius);
        pixmap.fillCircle(x + radius, y + h - radius - 1, radius);
        pixmap.fillCircle(x + w - radius - 1, y + h - radius - 1, radius);
    }

    private Drawable solidDrawable(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(Math.max(1, width), Math.max(1, height), Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    protected void addTitleImage(Table card) {
        String titlePath = "assets/images/ui/pvz2_logo_horizontal.png";
        if (Gdx.files.internal(titlePath).exists()) {
            Texture titleTexture = loadLinearTexture(titlePath);
            Image titleImg = new Image(titleTexture);
            titleImg.setScaling(Scaling.fit);
            card.add(titleImg).colspan(2).size(150, 55).padBottom(SPACE_MD).row();
        } else {
            card.add(new Label("Plants vs Zombies", skin, "title")).colspan(2).padBottom(SPACE_MD).row();
        }
    }

    protected ScrollPane scrollable(Table content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);
        pane.setOverscroll(false, false);
        return pane;
    }

    protected void showCard(Table card) {
        card.pack();
        float height = Math.min(card.getPrefHeight(), CARD_MAX_HEIGHT);
        ScrollPane outer = scrollable(card);
        rootTable.add(outer).width(card.getPrefWidth()).height(height);
    }

    @Override
    public void initParticles() {
        if (particles != null) {
            particles.dispose();
        }

        particles = new ParticleCreator(particlePaths, 15, 20f, 32f, 1.2f, true);
        Actor particleActor = particles.createActor();
        particleActor.setTouchable(Touchable.disabled);
        rootStack.addActorAt(1, particleActor);
    }

    @Override
    public void show() {
        super.show();
        initParticles();
        GeneralPrinter.addListener(printerListener);
    }

    @Override
    public void hide() {
        GeneralPrinter.removeListener(printerListener);
        super.hide();
    }

    private void onMessage(String message) {
        if (stage != null && notificationsEnabled()) {
            Toast.show(stage, message);
        }
    }

    /** Menus show toast notifications by default; gameplay screens turn them off
     *  (see GameScreen) since they were popping in over match UI, e.g. Beghouled's
     *  upgrade cards, and eating the clicks meant for it. */
    protected boolean notificationsEnabled() {
        return true;
    }

    protected boolean runCommand(String command) {
        try {
            App.currentMenu.handleCommand(command);
        } catch (GameException e) {
            GeneralPrinter.print("[Error] " + e.getMessage());
        } catch (Exception e) {
            GeneralPrinter.print(String.valueOf(e.getMessage()));
        }
        ScreenManager.syncWithCurrentMenu();
        if (ScreenManager.getScreen() == this) {
            onAfterCommand();
        }
        return false;
    }

    protected void onAfterCommand() {
    }

    protected TextField field(boolean masked) {
        TextField textField = new TextField("", skin, "main");
        textField.setTextFieldFilter((tf, c) -> c != ' ');
        if (masked) {
            textField.setPasswordCharacter('*');
            textField.setPasswordMode(true);
        }
        return textField;
    }

    protected void addRow(Table card, String labelText, Actor field) {
        card.add(new Label(labelText, skin, "main")).width(LABEL_WIDTH).left();
        card.add(field).width(FIELD_WIDTH).left().row();
    }

    protected TextButton primaryButton(String text, Runnable action) {
        return styledButton(new TextButton(text, skin, "main"), action);
    }

    protected TextButton secondaryButton(String text, Runnable action) {
        return styledButton(new TextButton(text, skin, "secondary"), action);
    }

    protected TextButton styledButton(TextButton button, Runnable action) {
        button.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                button.addAction(Actions.scaleTo(1.05f, 1.05f, 0.1f));
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                button.addAction(Actions.scaleTo(1.0f, 1.0f, 0.1f));
            }
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    protected Label createLabel(String text, String styleName) {
        return new Label(text, skin, styleName);
    }
}