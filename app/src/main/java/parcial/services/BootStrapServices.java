package parcial.services;

import org.h2.tools.Server;

import java.sql.SQLException;

public class BootStrapServices {

    private static BootStrapServices instance;

    private BootStrapServices() {

    }

    public static BootStrapServices getInstance() {
        if (instance == null) {
            instance = new BootStrapServices();
        }
        return instance;
    }

    public void startDb() {
        try {
            // Modo servidor H2.
            Server.createTcpServer("-tcpPort",
                    "9092",
                    "-tcpAllowOthers",
                    "-tcpDaemon",
                    "-ifNotExists").start();
            // Abriendo el cliente web. El valor 0 representa puerto aleatorio.
            String status = Server.createWebServer("-trace", "-webPort", "0").start().getStatus();
            //
            System.out.println("Status Web: " + status);
        } catch (SQLException ex) {
            System.out.println("Problema con la base de datos: " + ex.getMessage());
        }
    }

    public void init() {
        startDb();
    }
}
