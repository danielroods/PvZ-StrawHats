package view.screens.ui_menus;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import controller.ui_menus.authentication.SignupMenu;
import view.screens.generals.UiScreen;

import java.util.List;

public class SignupScreen extends UiScreen {

    private enum Step { REGISTER, SECURITY_QUESTION }

    private Step step = Step.REGISTER;

    @Override
    public void show() {
        setBackground("assets/images/backg/PVZIOS_newtitle.png");
        super.show();
        buildRegisterStep();
    }

    private void buildRegisterStep() {
        step = Step.REGISTER;
        rootTable.clear();

        TextField username = field(false);
        TextField password = field(true);
        TextField confirmPassword = field(true);
        TextField nickname = field(false);
        TextField email = field(false);

        TextButton male = new TextButton("Male", skin, "gender-button");
        TextButton female = new TextButton("Female", skin, "gender-button");
        male.setChecked(true);
        ButtonGroup<TextButton> genderGroup = new ButtonGroup<>();
        genderGroup.setMinCheckCount(1);
        genderGroup.setMaxCheckCount(1);
        genderGroup.add(male, female);

        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));

        card.pad(12, 25, 12, 25).defaults().pad(2);

        addTitleImage(card);
        card.add(new Label("Create your account", skin, "title")).colspan(2).padBottom(4).row();

        addRow(card, "Username", username);
        addRow(card, "Password", password);
        addRow(card, "Confirm password", confirmPassword);
        addRow(card, "Nickname", nickname);
        addRow(card, "Email", email);

        Table genderRow = new Table();
        genderRow.add(male).pad(2).width(130).height(30);
        genderRow.add(female).pad(2).width(130).height(30);
        card.add(new Label("Gender", skin, "main")).left();
        card.add(genderRow).left().row();

        card.add(primaryButton("Register", () -> {
            String gender = male.isChecked() ? "male" : "female";
            runCommand("register -u " + username.getText()
                    + " -p " + password.getText() + " " + confirmPassword.getText()
                    + " -n " + nickname.getText()
                    + " -e " + email.getText()
                    + " -g " + gender);
        })).colspan(2).padTop(6).width(350).height(34).row();

        card.add(secondaryButton("Already have an account? Log in", () -> runCommand("menu enter login")))
                .colspan(2).padTop(3).width(350).height(30).row();
        card.add(secondaryButton("Quit", this::confirmQuit)).colspan(2).padTop(3).width(350).height(30).row();

        showCard(card);
    }

    private void buildSecurityQuestionStep() {
        step = Step.SECURITY_QUESTION;
        rootTable.clear();

        List<String> questions = SignupMenu.getSecurityQuestions();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);

        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));
        card.pad(15).defaults().pad(4);
        addTitleImage(card);
        card.add(new Label("Pick a security question", skin, "title")).colspan(2).padBottom(8).row();

        for (String q : questions) {
            TextButton questionButton = new TextButton(q, skin, "main");
            questionButton.getLabel().setWrap(true);
            group.add(questionButton);
            card.add(questionButton).colspan(2).width(420).left().row();
        }
        if (!group.getButtons().isEmpty()) {
            group.getButtons().first().setChecked(true);
        }

        TextField answer = field(false);
        TextField confirmAnswer = field(false);
        addRow(card, "Answer", answer);
        addRow(card, "Confirm answer", confirmAnswer);

        card.add(primaryButton("Submit", () -> {
            int questionNumber = group.getCheckedIndex() + 1;
            runCommand("pick question -q " + questionNumber + " -a " + answer.getText() + " -c " + confirmAnswer.getText());
        })).colspan(2).padTop(8).width(350).height(34).row();

        showCard(card);
    }

    private void confirmQuit() {
        new ConfirmModal("Quit Plants vs. Zombies?", "Any unsaved progress will be lost.", "Quit",
                () -> runCommand("menu exit")).show();
    }

    @Override
    protected void onAfterCommand() {
        boolean pendingQuestion = SignupMenu.isPendingSecurityAnswer();
        if (pendingQuestion && step != Step.SECURITY_QUESTION) {
            buildSecurityQuestionStep();
        } else if (!pendingQuestion && step == Step.SECURITY_QUESTION) {
            buildRegisterStep();
        }
    }
}