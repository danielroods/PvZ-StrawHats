package net.server.ta;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import model.user_data.User;
import net.server.AccountStore;
import service.EmailSender;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public final class TaPortalServer {
    private static final Gson GSON = new Gson();
    private final File dataDirectory;
    private final TaPortalStore store;
    private final AccountStore accounts;
    private final EmailSender emailSender;
    private final int requestedPort;
    private final Map<String, TaPortalStore.TaCode> sessions = new ConcurrentHashMap<>();
    private HttpServer http;

    public TaPortalServer(File dataDirectory, int port) {
        this.dataDirectory = dataDirectory;
        this.requestedPort = port;
        this.store = new TaPortalStore(dataDirectory);
        this.accounts = new AccountStore(dataDirectory);
        this.emailSender = new EmailSender();
    }

    public synchronized void start() throws IOException {
        if (http != null) return;
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", requestedPort), 0);
        http.createContext("/", this::handleStatic);
        http.createContext("/api/ta/login", this::handleLogin);
        http.createContext("/api/ta/logout", this::handleLogout);
        http.createContext("/api/ta/whoami", this::handleWhoAmI);
        http.createContext("/api/ta/group", this::handleGroup);
        http.createContext("/api/ta/submit", this::handleSubmit);
        http.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        http.start();
    }

    public synchronized void stop() {
        if (http != null) { http.stop(0); http = null; }
    }

    public boolean isRunning() { return http != null; }
    public int getPort() { return http == null ? requestedPort : http.getAddress().getPort(); }
    public String getUrl() { return "http:

    
    private static final String[] HTML_CANDIDATE_PATHS = {
            "src/web/ta/index.html"
    };

    private void handleStatic(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { send(ex, 405, "Method Not Allowed", "text/plain"); return; }
        File webRoot = resolveWebRoot();
        if (webRoot == null) {
            send(ex, 404, "TA portal files not found. Looked for "
                    + String.join(" or ", HTML_CANDIDATE_PATHS)
                    + " starting from the working directory (" + new File("").getAbsolutePath()
                    + ") and from the game's own install directory. Make sure that folder "
                    + "(index.html, style.css, and its images) is committed to the repo.", "text/plain");
            return;
        }

        String requestPath = ex.getRequestURI().getPath();
        if (requestPath == null || requestPath.isEmpty() || requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        File requested;
        try {
            requested = new File(webRoot, requestPath).getCanonicalFile();
        } catch (IOException e) {
            send(ex, 400, "Bad request.", "text/plain");
            return;
        }
        String webRootCanonical = webRoot.getCanonicalPath();
        if (!requested.getPath().equals(webRootCanonical)
                && !requested.getPath().startsWith(webRootCanonical + File.separator)) {
            send(ex, 403, "Forbidden", "text/plain");
            return;
        }
        if (!requested.isFile()) {
            send(ex, 404, "Not found: " + requestPath, "text/plain");
            return;
        }
        send(ex, 200, Files.readAllBytes(requested.toPath()), contentTypeFor(requested.getName()));
    }

    
    private static File resolveWebRoot() {
        File direct = firstExistingCandidate(new File(""));
        if (direct != null) return direct.getParentFile();

        File anchor = codeSourceDirectory();
        for (int i = 0; anchor != null && i < 8; i++, anchor = anchor.getParentFile()) {
            File found = firstExistingCandidate(anchor);
            if (found != null) return found.getParentFile();
        }
        return null;
    }

    private static File firstExistingCandidate(File base) {
        for (String candidate : HTML_CANDIDATE_PATHS) {
            File f = new File(base, candidate);
            if (f.isFile()) return f;
        }
        return null;
    }

    private static String contentTypeFor(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".woff")) return "font/woff";
        return "application/octet-stream";
    }

    private static File codeSourceDirectory() {
        try {
            var location = TaPortalServer.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(location.toURI());
            return file.isDirectory() ? file : file.getParentFile();
        } catch (Exception e) {
            return null;
        }
    }

    private void handleLogin(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 405, error("Method Not Allowed")); return; }
        TaPortalStore.TaCode ta = store.authenticateCode(form(ex).get("code"));
        if (ta == null) { sendJson(ex, 401, error("Invalid TA code.")); return; }
        String token = UUID.randomUUID().toString();
        sessions.put(token, ta);
        ex.getResponseHeaders().add("Set-Cookie", "TA_SESSION=" + token + "; Path=/; HttpOnly; SameSite=Lax");
        sendJson(ex, 200, Map.of("ok", true, "taName", ta.name(), "taEmail", ta.email()));
    }

    private void handleLogout(HttpExchange ex) throws IOException {
        String token = cookie(ex, "TA_SESSION");
        if (token != null) sessions.remove(token);
        ex.getResponseHeaders().add("Set-Cookie", "TA_SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        sendJson(ex, 200, Map.of("ok", true));
    }

    
    private void handleWhoAmI(HttpExchange ex) throws IOException {
        if (session(ex) == null) { sendJson(ex, 401, error("Sign in as a TA first.")); return; }
        User current = User.currentUser;
        if (current == null) {
            sendJson(ex, 200, Map.of("ok", true, "signedIn", false));
            return;
        }
        int coins = current.userState == null ? 0 : current.userState.coins;
        sendJson(ex, 200, Map.of("ok", true, "signedIn", true,
                "username", current.username, "coins", coins));
    }

    private void handleGroup(HttpExchange ex) throws IOException {
        if (session(ex) == null) { sendJson(ex, 401, error("Sign in as a TA first.")); return; }
        String group = query(ex.getRequestURI(), "group");
        sendJson(ex, 200, Map.of("ok", true, "groupId", group == null ? "" : group,
                "groupScore", group == null ? 0.0 : store.groupScore(group)));
    }

    private void handleSubmit(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 405, error("Method Not Allowed")); return; }
        TaPortalStore.TaCode ta = session(ex);
        if (ta == null) { sendJson(ex, 401, error("Sign in as a TA first.")); return; }
        Map<String,String> form = form(ex);
        String groupId = form.get("groupId");
        double added;
        try { added = Double.parseDouble(form.getOrDefault("addedScore", "0")); }
        catch (NumberFormatException e) { sendJson(ex, 400, error("Added score must be a number.")); return; }

        
        
        User current = User.currentUser;
        if (current == null || current.username == null || current.username.isBlank()) {
            sendJson(ex, 400, error("No game account is signed in right now. Log into your account in the game, then reopen this offer.")); return;
        }
        String username = current.username;

        TaPortalStore.RewardResult result = store.grant(groupId, username, added, ta.name());
        if (!result.success()) { sendJson(ex, 400, error(result.error())); return; }
        int coins = result.rewardCoins();
        if (coins <= 0) { sendJson(ex, 400, error("No 0.25 score unit was completed.")); return; }

        
        
        
        if (current.userState == null) {
            current.userState = new model.user_data.UserState(new ArrayList<>(), 0, 0, 0);
        }
        current.userState.coins += coins;
        User.save();
        int totalCoins = current.userState.coins;

        
        
        
        if (com.badlogic.gdx.Gdx.app != null) {
            com.badlogic.gdx.Gdx.app.postRunnable(controller.ScreenManager::refreshCurrentScreen);
        }

        store.recordTransaction(groupId, username, current.email == null ? "" : current.email, added, coins, ta.name(), ta.email());

        String subject = "Thank You!";
        boolean emailSent = false;
        String emailError;
        String targetEmail = (ta.email() == null || ta.email().isBlank())
                ? form.getOrDefault("fallbackEmail", "").trim()
                : ta.email();
        if (targetEmail.isBlank()) {
            emailError = "This TA code has no email address configured.";
        } else {
            try {
                String template = Files.readString(new File("src/service/thanks.html").toPath());
                Map<String, String> vars = Map.of(
                        "taName", ta.name(),
                        "studentId", username,
                        "teamPhotoUrl", "https:
                );
                emailSent = emailSender.send(targetEmail, subject, template, vars);
                emailError = emailSent ? null : "Could not send the email. Check the sender logs.";
            } catch (IOException e) {
                emailError = "Could not read the email template: " + e.getMessage();
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("username", username);
        response.put("coinsAdded", coins);
        response.put("totalCoins", totalCoins);
        response.put("newGroupScore", result.newScore());
        response.put("emailSent", emailSent);
        response.put("emailUsed", targetEmail);
        if (emailError != null) response.put("emailError", emailError);
        sendJson(ex, 200, response);
    }

    private TaPortalStore.TaCode session(HttpExchange ex) {
        String token = cookie(ex, "TA_SESSION");
        return token == null ? null : sessions.get(token);
    }

    private static Map<String,String> form(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String,String> out = new HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) continue;
            String[] p = pair.split("=", 2);
            out.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8), p.length == 2 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "");
        }
        return out;
    }

    private static String cookie(HttpExchange ex, String name) {
        String raw = ex.getRequestHeaders().getFirst("Cookie");
        if (raw == null) return null;
        for (String part : raw.split(";")) {
            String[] p = part.trim().split("=", 2);
            if (p.length == 2 && name.equals(p[0])) return p[1];
        }
        return null;
    }

    private static String query(URI uri, String name) {
        String q = uri.getRawQuery(); if (q == null) return null;
        for (String pair : q.split("&")) {
            String[] p = pair.split("=", 2);
            if (p.length == 2 && name.equals(URLDecoder.decode(p[0], StandardCharsets.UTF_8))) return URLDecoder.decode(p[1], StandardCharsets.UTF_8);
        }
        return null;
    }

    private static Map<String,String> error(String message) { return Map.of("ok", "false", "error", message); }
    private static void sendJson(HttpExchange ex, int status, Object value) throws IOException { send(ex, status, GSON.toJson(value), "application/json; charset=UTF-8"); }
    private static void send(HttpExchange ex, int status, String body, String contentType) throws IOException {
        send(ex, status, body.getBytes(StandardCharsets.UTF_8), contentType);
    }
    private static void send(HttpExchange ex, int status, byte[] bytes, String contentType) throws IOException {
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) { out.write(bytes); }
    }
}