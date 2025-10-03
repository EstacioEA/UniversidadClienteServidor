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
            System.out.println("║\tSERVIDOR TCP INICIADO\t\t║");
            System.out.println("║\tPuerto: " + PUERTO + "\t║");
            System.out.println("║\tEsperando conexiones...\t\t║");
            System.out.println("╚════════════════════════════════════════╝\n");

            while (ejecutando) {
                try {
                    Socket clienteSocket = serverSocket.accept();
                    System.out.println("→ Cliente conectado desde: " +
                            clienteSocket.getInetAddress().getHostAddress());

                    // Crear un hilo para manejar cada cliente
                    Thread hiloCliente = new Thread(new ManejadorCliente(clienteSocket, dbManager));
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

/**
 * Clase interna para manejar cada cliente en un hilo separado
 */
class ManejadorCliente implements Runnable {
    private Socket socket;
    private DatabaseManager dbManager;

    public ManejadorCliente(Socket socket, DatabaseManager dbManager) {
        this.socket = socket;
        this.dbManager = dbManager;
    }

    @Override
    public void run() {
        try (
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String mensajeCliente;

            while ((mensajeCliente = entrada.readLine()) != null) {
                System.out.println("📨 Comando recibido: " + mensajeCliente);

                if (mensajeCliente.equals("SALIR")) {
                    salida.println("Conexión cerrada por el servidor");
                    break;
                }

                // Procesar el comando y enviar respuesta
                String respuesta = dbManager.procesarComando(mensajeCliente);
                salida.println(respuesta);
                System.out.println("📤 Respuesta enviada al cliente\n");
            }

        } catch (IOException e) {
            System.err.println("✗ Error en comunicación con cliente: " + e.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("← Cliente desconectado\n");
            } catch (IOException e) {
                System.err.println("✗ Error al cerrar socket: " + e.getMessage());
            }
        }
    }
}