package net.server;

import com.google.gson.JsonObject;
import model.user_data.AccountValidation;
import model.user_data.User;
import model.user_data.UserState;
import net.Envelope;
import net.JsonLine;
import net.Protocol;
import net.dto.AccountDto;

public class AccountHandlers {

    private final GameServer server;

    public AccountHandlers(GameServer server) {
        this.server = server;
    }

    public void handle(ClientSession session, Envelope envelope) {
        switch (envelope.t) {
            case Protocol.REGISTER -> register(session, envelope);
            case Protocol.LOGIN -> login(session, envelope);
            case Protocol.LOGOUT -> logout(session, envelope);
            case Protocol.FORGOT_PASSWORD_START -> forgotStart(session, envelope);
            case Protocol.FORGOT_PASSWORD_ANSWER -> forgotAnswer(session, envelope);
            case Protocol.PROFILE_UPDATE -> profileUpdate(session, envelope);
            case Protocol.STATE_PUSH -> statePush(session, envelope);
            case Protocol.STATE_PULL -> statePull(session, envelope);
            case Protocol.BONUS_SCORE_SUBMIT -> bonusScore(session, envelope);
            default -> session.sendError(envelope.id, Protocol.ERR_BAD_REQUEST, "Unhandled.");
        }
    }

    private void register(ClientSession session, Envelope envelope) {
        String error = server.accounts().register(
                envelope.getString("username"),
                envelope.getString("password"),
                envelope.getString("nickname"),
                envelope.getString("email"),
                envelope.getString("gender"),
                envelope.getString("securityQuestion"),
                envelope.getString("securityAnswer"));
        if (error != null) {
            String code = error.startsWith("Username already")
                    ? Protocol.ERR_USERNAME_TAKEN : Protocol.ERR_VALIDATION;
            session.sendError(envelope.id, code, error);
            return;
        }
        session.sendOk(envelope.id, Envelope.obj("username", envelope.getString("username")));
    }

    private void login(ClientSession session, Envelope envelope) {
        String username = envelope.getString("username");
        User user = server.accounts().authenticate(username, envelope.getString("password"));
        if (user == null) {
            session.sendError(envelope.id, Protocol.ERR_BAD_CREDENTIALS,
                    "Wrong username or password.");
            return;
        }
        if (server.sessions().isOnline(user.username)) {
            session.sendError(envelope.id, Protocol.ERR_ALREADY_ONLINE,
                    "That account is already signed in somewhere else.");
            return;
        }
        server.sessions().release(session);
        server.sessions().bind(user.username, session);
        session.setUsername(user.username);
        session.sendOk(envelope.id, accountPayload(user));
    }

    private void logout(ClientSession session, Envelope envelope) {
        server.matchmaking().onSessionClosed(session);
        server.matches().onSessionClosed(session);
        server.sessions().release(session);
        session.setUsername(null);
        session.sendOk(envelope.id, Envelope.obj());
    }

    private void forgotStart(ClientSession session, Envelope envelope) {
        User user = server.accounts().find(envelope.getString("username"));
        if (user == null) {
            session.sendError(envelope.id, Protocol.ERR_NO_SUCH_USER, "Username not found.");
            return;
        }
        String email = envelope.getString("email");
        if (user.email == null || !user.email.equalsIgnoreCase(email)) {
            session.sendError(envelope.id, Protocol.ERR_VALIDATION,
                    "Email does not match our records.");
            return;
        }
        if (user.securityQuestion == null) {
            session.sendError(envelope.id, Protocol.ERR_VALIDATION,
                    "No security question set for this account.");
            return;
        }
        session.setPendingResetUsername(user.username);
        session.sendOk(envelope.id, Envelope.obj("securityQuestion", user.securityQuestion));
    }

    private void forgotAnswer(ClientSession session, Envelope envelope) {
        String pending = session.getPendingResetUsername();
        if (pending == null) {
            session.sendError(envelope.id, Protocol.ERR_BAD_REQUEST,
                    "Start the password reset first.");
            return;
        }
        User user = server.accounts().find(pending);
        if (user == null || !user.checkSecurityAnswer(envelope.getString("answer", ""))) {
            session.setPendingResetUsername(null);
            session.sendError(envelope.id, Protocol.ERR_VALIDATION, "Incorrect answer.");
            return;
        }
        String newPassword = envelope.getString("newPassword");
        String error = AccountValidation.passwordError(newPassword);
        if (error != null) {
            session.sendError(envelope.id, Protocol.ERR_VALIDATION, error);
            return;
        }
        user.setPassword(newPassword);
        session.setPendingResetUsername(null);
        server.accounts().touch();
        server.accounts().saveNow();
        session.sendOk(envelope.id, Envelope.obj());
    }

    private void profileUpdate(ClientSession session, Envelope envelope) {
        if (!requireLogin(session, envelope)) return;
        User user = server.accounts().find(session.getUsername());
        if (user == null) {
            session.sendError(envelope.id, Protocol.ERR_NO_SUCH_USER, "Account is gone.");
            return;
        }
        String field = envelope.getString("field", "");
        String value = envelope.getString("value", "");
        String error = applyProfileChange(session, user, field, value, envelope);
        if (error != null) {
            session.sendError(envelope.id, Protocol.ERR_VALIDATION, error);
            return;
        }
        server.accounts().touch();
        server.accounts().saveNow();
        session.sendOk(envelope.id, accountPayload(user));
    }

    private String applyProfileChange(ClientSession session, User user, String field, String value,
                                      Envelope envelope) {
        switch (field) {
            case "username" -> {
                if (value.equalsIgnoreCase(user.username)) {
                    return "New username must be different from your current username.";
                }
                String error = AccountValidation.usernameError(value);
                if (error != null) return error;
                if (!server.accounts().rename(user.username, value)) {
                    return "Username already exists. Please choose a different one.";
                }
                server.sessions().release(session);
                server.sessions().bind(value, session);
                session.setUsername(value);
                return null;
            }
            case "nickname" -> {
                if (value.equals(user.nickname)) {
                    return "New nickname must be different from your current nickname.";
                }
                String error = AccountValidation.nicknameError(value);
                if (error != null) return error;
                user.nickname = value;
                return null;
            }
            case "email" -> {
                if (value.equalsIgnoreCase(user.email)) {
                    return "New email must be different from your current email.";
                }
                String error = AccountValidation.emailError(value);
                if (error != null) return error;
                user.email = value;
                return null;
            }
            case "password" -> {
                String oldPassword = envelope.getString("oldPassword", "");
                if (!user.checkPassword(oldPassword)) return "Your current password is incorrect.";
                if (user.checkPassword(value)) {
                    return "New password must be different from your current password.";
                }
                String error = AccountValidation.passwordError(value);
                if (error != null) return error;
                user.setPassword(value);
                return null;
            }
            case "profilePicture" -> {
                user.profilePicture = value;
                return null;
            }
            default -> {
                return "Unknown profile field: " + field;
            }
        }
    }

    private void statePush(ClientSession session, Envelope envelope) {
        if (!requireLogin(session, envelope)) return;
        JsonObject json = envelope.getObject("userState");
        UserState state = JsonLine.fromTree(json, UserState.class);
        if (state == null) {
            session.sendError(envelope.id, Protocol.ERR_BAD_REQUEST, "Missing userState.");
            return;
        }
        server.accounts().replaceState(session.getUsername(), state);
        session.sendOk(envelope.id, Envelope.obj());
    }

    private void statePull(ClientSession session, Envelope envelope) {
        if (!requireLogin(session, envelope)) return;
        User user = server.accounts().find(session.getUsername());
        if (user == null) {
            session.sendError(envelope.id, Protocol.ERR_NO_SUCH_USER, "Account is gone.");
            return;
        }
        session.sendOk(envelope.id, accountPayload(user));
    }

    private void bonusScore(ClientSession session, Envelope envelope) {
        if (!requireLogin(session, envelope)) return;
        int score = envelope.getInt("score", 0);
        Integer before = server.accounts().bonusScoreOf(session.getUsername());
        server.accounts().submitBonusScore(session.getUsername(), score);
        Integer best = server.accounts().bonusScoreOf(session.getUsername());
        boolean improved = before == null || (best != null && best > before);
        server.accounts().saveNow();
        session.sendOk(envelope.id, Envelope.obj("bestScore", best, "improved", improved));
    }

    private boolean requireLogin(ClientSession session, Envelope envelope) {
        if (session.isLoggedIn()) return true;
        session.sendError(envelope.id, Protocol.ERR_NOT_LOGGED_IN, "Sign in first.");
        return false;
    }

    private JsonObject accountPayload(User user) {
        JsonObject payload = new JsonObject();
        payload.add("account", JsonLine.toTree(AccountDto.of(user)));
        payload.add("userState", JsonLine.toTree(user.userState));
        return payload;
    }
}
