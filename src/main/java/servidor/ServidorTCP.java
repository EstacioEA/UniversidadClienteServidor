package servidor;

import java.io.*;
import java.net.*;

/**
 * Servidor TCP para manejar conexiones de clientes
 */
public class ServidorTCP {
    private static final int PUERTO = 5000;
    private ServerSocket serverSocket;
    private DatabaseManager dbManager;
    private boolean ejecutando;

    public ServidorTCP() {
        dbManager = new DatabaseManager();
        ejecutando = true;
    }

    /**
     * Inicia el servidor TCP
     */
    public void iniciar() {
        try {
            serverSocket = new ServerSocket(PUERTO);
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║   SERVIDOR TCP INICIADO                ║");
            System.out.println("║   Puerto: " + PUERTO + "                        ║");
            System.out.println("║   Esperando conexiones...              ║");
            System.out.println("╚════════════════════════════════════════╝\n");

            while (ejecutando) {
                try {
                    Socket clienteSocket = serverSocket.accept();
                    System.out.println("→ Cliente conectado desde: " +
                            clienteSocket.getInetAddress().getHostAddress());

                    // Crear un hilo para manejar cada cliente
                    Thread hiloCliente = new Thread(new ClientHandler(clienteSocket, dbManager));
                    hiloCliente.start();

                } catch (IOException e) {
                    if (ejecutando) {
                        System.err.println("✗ Error al aceptar conexión: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("✗ Error al iniciar servidor TCP: " + e.getMessage());
            e.printStackTrace();
        } finally {
            detener();
        }
    }

    /**
     * Detiene el servidor
     */
    public void detener() {
        ejecutando = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            dbManager.cerrarConexion();
            System.out.println("\n✓ Servidor TCP detenido");
        } catch (IOException e) {
            System.err.println("✗ Error al cerrar servidor: " + e.getMessage());
        }
    }

    /**
     * Método main para ejecutar el servidor
     */
    public static void main(String[] args) {
        ServidorTCP servidor = new ServidorTCP();

        // Agregar shutdown hook para cerrar limpiamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚠ Cerrando servidor...");
            servidor.detener();
        }));

        servidor.iniciar();
    }
}