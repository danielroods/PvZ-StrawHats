package controller.ui_menus.network;

import controller.ui_menus.MainMenu;
import controller.ui_menus.Menu;
import model.App;
import model.Regex;
import net.Protocol;
import net.client.NetworkClient;
import view.GeneralPrinter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkMenu extends Menu {

    private static final Pattern CONNECT = Pattern.compile(
            "^\\s*connect(?:\\s+-h\\s+(?<host>\\S+))?(?:\\s+-p\\s+(?<port>\\d+))?\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NET_LOGIN = Pattern.compile(
            "^\\s*net\\s+login\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NET_REGISTER = Pattern.compile(
            "^\\s*net\\s+register\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)"
                    + "\\s+-n\\s+(?<nickname>\\S+)\\s+-e\\s+(?<email>\\S+)"
                    + "\\s+-g\\s+(?<gender>\\S+)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CHALLENGE = Pattern.compile(
            "^\\s*challenge\\s+-u\\s+(?<username>\\S+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVITE_RESPOND = Pattern.compile(
            "^\\s*invite\\s+(?<answer>accept|reject)\\s*$", Pattern.CASE_INSENSITIVE);

    @Override
    public String getName() {
        return "Network Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        NetworkClient client = NetworkClient.get();
        if (CONNECT.matcher(text).matches()) {
            Matcher matcher = CONNECT.matcher(text);
            matcher.matches();
            String host = matcher.group("host") == null
                    ? Protocol.DEFAULT_HOST : matcher.group("host");
            int port = matcher.group("port") == null
                    ? Protocol.DEFAULT_PORT : Integer.parseInt(matcher.group("port"));
            client.connect(host, port);
            GeneralPrinter.print(client.getStatusMessage());
        } else if (text.trim().equalsIgnoreCase("disconnect")) {
            client.disconnect();
            GeneralPrinter.print("Disconnected. You are playing offline again.");
        } else if (NET_LOGIN.matcher(text).matches()) {
            Matcher matcher = NET_LOGIN.matcher(text);
            matcher.matches();
            client.login(matcher.group("username"), matcher.group("password"),
                    envelope -> GeneralPrinter.print(envelope.isType(Protocol.OK)
                            ? "Signed in as " + matcher.group("username")
                            : envelope.getString("message", "Sign-in failed.")));
        } else if (NET_REGISTER.matcher(text).matches()) {
            Matcher matcher = NET_REGISTER.matcher(text);
            matcher.matches();
            client.register(matcher.group("username"), matcher.group("password"),
                    matcher.group("nickname"), matcher.group("email"), matcher.group("gender"),
                    "1. What is the name of your first pet?", "pvz",
                    envelope -> GeneralPrinter.print(envelope.isType(Protocol.OK)
                            ? "Account created. Sign in with net login."
                            : envelope.getString("message", "Registration failed.")));
        } else if (CHALLENGE.matcher(text).matches()) {
            Matcher matcher = CHALLENGE.matcher(text);
            matcher.matches();
            client.challenge(matcher.group("username"),
                    envelope -> GeneralPrinter.print(envelope.isType(Protocol.OK)
                            ? "Invite sent to " + matcher.group("username") + "."
                            : envelope.getString("message", "Could not send the invite.")));
        } else if (INVITE_RESPOND.matcher(text).matches()) {
            Matcher matcher = INVITE_RESPOND.matcher(text);
            matcher.matches();
            NetworkClient.Invite invite = client.getPendingInvite();
            if (invite == null) {
                GeneralPrinter.print("There is no invite waiting.");
                return;
            }
            client.respondToInvite(invite.inviteId(),
                    matcher.group("answer").equalsIgnoreCase("accept"), null);
        } else if (text.trim().equalsIgnoreCase("find match")) {
            client.joinQueue(envelope -> GeneralPrinter.print(envelope.isType(Protocol.OK)
                    ? "Waiting for an opponent..."
                    : envelope.getString("message", "Could not join the queue.")));
        } else if (text.trim().equalsIgnoreCase("cancel match")) {
            client.leaveQueue();
            GeneralPrinter.print("Left the queue.");
        } else if (text.trim().equalsIgnoreCase("who")) {
            client.refreshOnlinePlayers();
            GeneralPrinter.print("Refreshing the player list...");
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Unknown command in the Network menu.");
        }
    }

    @Override
    public void exitMenu() {
        NetworkClient.get().leaveQueue();
        App.currentMenu = new MainMenu();
    }

    @Override
    public String showMenu() {
        NetworkClient client = NetworkClient.get();
        StringBuilder builder = new StringBuilder("[ Network Menu ]\n");
        builder.append("Status: ").append(client.isConnected() ? "connected" : "offline");
        if (client.isSignedIn()) builder.append(" as ").append(client.getSignedInUsername());
        builder.append('\n');
        if (client.getPendingInvite() != null) {
            builder.append("Invite from ").append(client.getPendingInvite().fromNickname())
                    .append(" - use 'invite accept' or 'invite reject'.\n");
        }
        builder.append("Connecting signs in with the account you made in Authentication - ")
                .append("no separate online account needed.\n");
        builder.append("Commands:\n")
                .append("  connect [-h <host>] [-p <port>] | disconnect\n")
                .append("  net register -u <u> -p <p> -n <n> -e <e> -g <g>  (only for a separate, online-only account)\n")
                .append("  net login -u <username> -p <password>  (only for a separate, online-only account)\n")
                .append("  challenge -u <username> | invite accept | invite reject\n")
                .append("  find match | cancel match | who\n")
                .append("  menu exit | menu show current");
        return builder.toString();
    }
}
