package view;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class GeneralPrinter {

    
    
    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public static void print(String message) {
        System.out.println(message);
        for (Consumer<String> listener : listeners) {
            listener.accept(message);
        }
    }

    
    public static void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public static void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }
}
