package net.server;

import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import net.Protocol;
import service.Log;

import java.io.File;

public final class ServerLauncher {

    private ServerLauncher() {
    }

    public static void main(String[] args) throws Exception {
        int port = Protocol.DEFAULT_PORT;
        String dataDirectory = "server-data";

        for (int i = 0; i + 1 < args.length; i++) {
            if ("--port".equals(args[i])) port = Integer.parseInt(args[i + 1]);
            if ("--data".equals(args[i])) dataDirectory = args[i + 1];
        }

        PlantFactory.autoInit();
        ZombieFactory.init();
        Log.info("Server", "Plant and zombie datasets loaded.");

        GameServer server = new GameServer(port, new File(dataDirectory));
        server.start();
    }
}
