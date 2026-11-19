import javafx.application.Application;

/**
 * Launcher class kept separate from the JavaFX Application subclass so the
 * IDE can start the app with JavaFX supplied as Maven dependencies.
 */
public class MapApp {
    public static void main(String[] args) {
        Application.launch(MapWindow.class, args);
    }
}
