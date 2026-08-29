package view.screens.generals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import controller.cheat.CurrencyCheatController;
import model.resoures.CurrencyType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Generic "add coins/diamonds" cheat popup.
 * <p>
 * It is a {@link Modal} (so it is never full-screen), but its panel uses the same
 * small wood texture as the collection menu boards, its close button is the same
 * back-button asset used by every other popup, and each currency gets an
 * item-card styled box (same "card-background" drawable used for shop items)
 * with a field to type how much to add.
 * <p>
 * One instance handles every {@link CurrencyType} - adding a new currency later
 * needs no change here.
 */
public class CurrencyCheatModal extends Modal {

    private static final String WOOD_BACKGROUND_PATH = "assets/images/backg/wood board.png";
    private static final String BACK_ICON_PATH = "assets/images/ui/buttons_hud_back_normal.png";

    private static final float PANEL_WIDTH = 420f;
    private static final float FIELD_WIDTH = 140f;

    private final CurrencyCheatController cheatController = new CurrencyCheatController();
    private final Map<CurrencyType, Label> currentAmountLabels = new EnumMap<>(CurrencyType.class);
    private final Runnable onCurrencyChanged;

    private CurrencyCheatModal(Runnable onCurrencyChanged) {
        this.onCurrencyChanged = onCurrencyChanged;

        content.setBackground(loadDrawable(WOOD_BACKGROUND_PATH));
        content.pad(24f).defaults().pad(6f);

        content.add(buildHeader()).width(PANEL_WIDTH).padBottom(14f).row();
        for (CurrencyType currency : CurrencyType.values()) {
            content.add(buildCurrencyBox(currency)).width(PANEL_WIDTH).padBottom(12f).row();
        }
    }

    /** Opens the popup on top of whatever screen is currently active. */
    public static void open(Runnable onCurrencyChanged) {
        new CurrencyCheatModal(onCurrencyChanged).show();
    }

    private Table buildHeader() {
        Table header = new Table();

        ImageButton back = new ImageButton(new TextureRegionDrawable(loadTexture(BACK_ICON_PATH)));
        back.getImageCell().size(50f, 50f);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
            }
        });

        Label title = new Label("Add Coins / Diamonds", skin, "title");
        title.setFontScale(1.05f);

        header.add(back).left();
        header.add(title).expandX().center().padLeft(-50f);
        return header;
    }

    private Table buildCurrencyBox(CurrencyType currency) {
        Table box = new Table();
        box.setBackground(skin.getDrawable("card-background"));
        box.pad(10f);

        Image icon = new Image(loadTexture(currency.getIconPath()));

        Label currentLabel = new Label(currentAmountText(currency), skin, "muted");
        currentAmountLabels.put(currency, currentLabel);

        Table info = new Table();
        info.add(new Label(currency.getDisplayName(), skin, "main")).left().row();
        info.add(currentLabel).left();

        TextField amountField = new TextField("", skin, "main");
        amountField.setMessageText("Amount");
        amountField.setTextFieldFilter((field, c) -> Character.isDigit(c));

        Label addLabel = new Label("Add", skin, "default");
        Table addButton = new Table();
        addButton.setBackground(skin.getDrawable("button-up"));
        addButton.add(addLabel).pad(10f, 10f, 10f, 10f);
        addButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                applyAmount(currency, amountField);
            }
        });

       // box.add(icon).size(90f, 46f).padRight(10f);
        box.add(info).left().expandX();
        box.add(amountField).width(FIELD_WIDTH).padRight(5f);
        box.add(addButton);
        return box;
    }

    private void applyAmount(CurrencyType currency, TextField amountField) {
        int amount = parseAmount(amountField.getText());
        if (cheatController.grant(currency, amount)) {
            amountField.setText("");
            Label label = currentAmountLabels.get(currency);
            if (label != null) {
                label.setText(currentAmountText(currency));
            }
            if (onCurrencyChanged != null) {
                onCurrencyChanged.run();
            }
        }
    }

    private String currentAmountText(CurrencyType currency) {
        return "Current: " + cheatController.currentAmount(currency);
    }

    private int parseAmount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private TextureRegionDrawable loadDrawable(String path) {
        return new TextureRegionDrawable(loadTexture(path));
    }

    private Texture loadTexture(String path) {
        if (path != null && !path.isEmpty() && Gdx.files.internal(path).exists()) {
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return texture;
        }
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        Texture fallback = new Texture(pixmap);
        pixmap.dispose();
        return fallback;
    }
}
