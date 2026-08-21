package view.screens.ui_menus;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import controller.ui_menus.authentication.LoginMenu;
import view.screens.generals.UiScreen;

public class LoginScreen extends UiScreen {

    private enum Step { LOGIN, FORGOT_PASSWORD, SECURITY_ANSWER, NEW_PASSWORD }

    private Step step = Step.LOGIN;

    @Override
    public void show() {
        setBackground("assets/images/backg/sddefault.jpg");
        super.show();
        buildLoginStep();
    }

    private void buildLoginStep() {
        step = Step.LOGIN;
        rootTable.clear();

        TextField username = field(false);
        TextField password = field(true);
        CheckBox stayLoggedIn = new CheckBox(" Stay logged in", skin, "main");

        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));
        card.pad(CARD_PAD).defaults().space(SPACE_SM);
        addTitleImage(card);
        card.add(new Label("Welcome back", skin, "title")).colspan(2).padBottom(SPACE_LG).row();
        addRow(card, "Username", username);
        addRow(card, "Password", password);
        card.add(stayLoggedIn).colspan(2).left().padTop(SPACE_XS).row();

        card.add(primaryButton("Log in", () -> {
            String cmd = "login -u " + username.getText() + " -p " + password.getText();
            if (stayLoggedIn.isChecked()) {
                cmd += " -stay-logged-in";
            }
            runCommand(cmd);
        })).colspan(2).padTop(SPACE_LG).width(BUTTON_WIDTH).row();

        card.add(secondaryButton("Forgot password?", this::buildForgotPasswordStep)).colspan(2).padTop(SPACE_SM).width(BUTTON_WIDTH).row();
        card.add(secondaryButton("New here? Create an account", () -> runCommand("menu exit")))
                .colspan(2).padTop(SPACE_XS).width(BUTTON_WIDTH).row();

        showCard(card);
    }

    private void buildForgotPasswordStep() {
        step = Step.FORGOT_PASSWORD;
        rootTable.clear();

        TextField username = field(false);
        TextField email = field(false);

        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));
        card.pad(CARD_PAD).defaults().space(SPACE_SM);
        addTitleImage(card);
        card.add(new Label("Reset your password", skin, "title")).colspan(2).padBottom(SPACE_LG).row();
        addRow(card, "Username", username);
        addRow(card, "Email", email);

        card.add(primaryButton("Continue", () ->
                        runCommand("forget password -u " + username.getText() + " -e " + email.getText())))
                .colspan(2).padTop(SPACE_LG).width(BUTTON_WIDTH).row();
        card.add(secondaryButton("Back to login", this::buildLoginStep)).colspan(2).padTop(SPACE_SM).width(BUTTON_WIDTH).row();

        showCard(card);
    }

    private void buildSecurityAnswerStep() {
        step = Step.SECURITY_ANSWER;
        rootTable.clear();

        TextField answer = field(false);

        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));
        card.pad(CARD_PAD).defaults().space(SPACE_SM);
        addTitleImage(card);
        card.add(new Label("Security question", skin, "title")).colspan(2).padBottom(SPACE_MD).row();

        Label question = new Label(String.valueOf(LoginMenu.getPendingSecurityQuestion()), skin, "muted");
        question.setWrap(true);
        card.add(question).colspan(2).width(440).padBottom(SPACE_MD).row();

        addRow(card, "Answer", answer);

        card.add(primaryButton("Submit", () -> runCommand("answer -a " + answer.getText())))
                .colspan(2).padTop(SPACE_LG).width(BUTTON_WIDTH).row();
        card.add(secondaryButton("Back to login", this::buildLoginStep)).colspan(2).padTop(SPACE_SM).width(BUTTON_WIDTH).row();

        showCard(card);
    }

    private void buildNewPasswordStep() {
        step = Step.NEW_PASSWORD;
        rootTable.clear();

        TextField newPassword = field(true);

        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));
        card.pad(CARD_PAD).defaults().space(SPACE_SM);
        addTitleImage(card);
        card.add(new Label("Choose a new password", skin, "title")).colspan(2).padBottom(SPACE_LG).row();
        addRow(card, "New password", newPassword);

        card.add(primaryButton("Save password", () -> runCommand(newPassword.getText())))
                .colspan(2).padTop(SPACE_LG).width(BUTTON_WIDTH).row();

        showCard(card);
    }

    @Override
    protected void onAfterCommand() {
        if (LoginMenu.isAwaitingNewPassword() && step != Step.NEW_PASSWORD) {
            buildNewPasswordStep();
        } else if (LoginMenu.isAwaitingSecurityAnswer() && step != Step.SECURITY_ANSWER) {
            buildSecurityAnswerStep();
        } else if (!LoginMenu.isAwaitingSecurityAnswer() && !LoginMenu.isAwaitingNewPassword()
                && (step == Step.SECURITY_ANSWER || step == Step.NEW_PASSWORD)) {
            buildLoginStep();
        }
    }
}