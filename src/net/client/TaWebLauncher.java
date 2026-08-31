package net.client;

import com.badlogic.gdx.Gdx;
import service.Log;
import view.GeneralPrinter;
import net.server.ta.TaPortalServer;

import java.io.File;

/** Starts the local TA web portal on demand. It is independent of GameServer. */
public final class TaWebLauncher {
    private static TaPortalServer portal;

    private TaWebLauncher() { }

    public static synchronized void open() {
        try {
            if (portal == null || !portal.isRunning()) {
                portal = new TaPortalServer(new File("server-data"), 0);
                portal.start();
            }
            String url = portal.getUrl();
            if (!Gdx.net.openURI(url)) {
                GeneralPrinter.print("Could not open the TA offer in the browser: " + url);
            }
        } catch (Exception e) {
            GeneralPrinter.print("Could not start the TA offer web page: " + e.getMessage());
            Log.error("TA Web", "Failed to start TA web page.", e);
        }
    }
}
