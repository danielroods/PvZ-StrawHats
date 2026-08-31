package net.server.ta;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/** Minimal SMTP sender. Configure server-data/ta.properties; leave enabled=false for offline demo mode. */
public final class SmtpEmailSender {
    private final Properties props;

    public SmtpEmailSender(File propertiesFile) {
        props = new Properties();
        if (propertiesFile.isFile()) {
            try (Reader reader = new FileReader(propertiesFile)) {
                props.load(reader);
            } catch (IOException ignored) { }
        }
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(props.getProperty("smtp.enabled", "false"));
    }

    /**
     * Null if email is fully configured and ready to send; otherwise a human-readable reason
     * it isn't, so a caller can show the real problem instead of a silent "email not sent".
     */
    public String describeProblem() {
        if (!isEnabled()) {
            return "Email is turned off (set smtp.enabled=true in server-data/ta.properties).";
        }
        String host = props.getProperty("smtp.host", "");
        String username = props.getProperty("smtp.username", "");
        String password = props.getProperty("smtp.password", "");
        if (host.isBlank() || username.isBlank() || password.isBlank()) {
            return "smtp.host, smtp.username, and smtp.password must all be set in "
                    + "server-data/ta.properties.";
        }
        return null;
    }

    public void send(String recipient, String subject, String body) throws IOException {
        if (!isEnabled()) return;
        String host = props.getProperty("smtp.host", "");
        int port = Integer.parseInt(props.getProperty("smtp.port", "465"));
        String username = props.getProperty("smtp.username", "");
        String password = props.getProperty("smtp.password", "");
        String from = props.getProperty("smtp.from", username);
        if (host.isBlank() || username.isBlank() || password.isBlank() || recipient == null || recipient.isBlank()) {
            throw new IOException("SMTP is enabled but its configuration is incomplete.");
        }

        try (Socket socket = javax.net.ssl.SSLSocketFactory.getDefault().createSocket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            expect(in, 220);
            command(out, in, "EHLO localhost", 250);
            command(out, in, "AUTH PLAIN " + Base64.getEncoder().encodeToString(
                    ("\0" + username + "\0" + password).getBytes(StandardCharsets.UTF_8)), 235);
            command(out, in, "MAIL FROM:<" + from + ">", 250);
            command(out, in, "RCPT TO:<" + recipient + ">", 250, 251);
            command(out, in, "DATA", 354);
            out.write("From: " + from + "\r\n");
            out.write("To: " + recipient + "\r\n");
            out.write("Subject: " + subject + "\r\n");
            out.write("Content-Type: text/plain; charset=UTF-8\r\n");
            out.write("\r\n");
            out.write(body.replace("\r", "").replace("\n", "\r\n"));
            out.write("\r\n.\r\n");
            out.flush();
            expect(in, 250);
            command(out, in, "QUIT", 221);
        }
    }

    private static void command(BufferedWriter out, BufferedReader in, String command, int... accepted) throws IOException {
        out.write(command + "\r\n");
        out.flush();
        int code = readCode(in);
        for (int expected : accepted) if (code == expected) return;
        throw new IOException("SMTP command failed: " + command + " (" + code + ")");
    }

    private static void expect(BufferedReader in, int expected) throws IOException {
        int code = readCode(in);
        if (code != expected) throw new IOException("SMTP server returned " + code + ", expected " + expected);
    }

    private static int readCode(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null || line.length() < 3) throw new IOException("SMTP connection closed.");
        int code = Integer.parseInt(line.substring(0, 3));
        while (line.length() > 3 && line.charAt(3) == '-') {
            line = in.readLine();
            if (line == null) throw new IOException("SMTP connection closed.");
        }
        return code;
    }
}