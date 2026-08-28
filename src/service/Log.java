package service;

import view.GeneralPrinter;

public final class Log {

    private Log() {
    }

    public static void error(String tag, String message) {
        error(tag, message, null);
    }

    public static void error(String tag, String message, Throwable cause) {
        StringBuilder line = new StringBuilder("[").append(tag).append("] ").append(message);
        if (cause != null) {
            line.append(" - ").append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null) line.append(": ").append(cause.getMessage());
        }
        GeneralPrinter.print(line.toString());
    }

    public static void info(String tag, String message) {
        GeneralPrinter.print("[" + tag + "] " + message);
    }
}
