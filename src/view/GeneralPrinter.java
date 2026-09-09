package view;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class GeneralPrinter {

    // CopyOnWriteArrayList: screens add/remove listeners from show()/hide(), which can
    // happen while a print() from another thread is mid-iteration.
    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public static void print(String message) {
        System.out.println(message);
        for (Consumer<String> listener : listeners) {
            listener.accept(message);
        }
    }

    /** Lets a graphical screen observe every message the controller layer emits (e.g. to show it as a Toast), without changing any existing print() call site. */
    public static void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public static void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }
}
