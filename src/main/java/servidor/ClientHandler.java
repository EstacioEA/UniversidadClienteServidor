package servidor;

import java.io.*;
import java.net.Socket;

/**
 * Clase para manejar cada cliente TCP en un hilo separado
 * Permite que el servidor atienda múltiples clientes simultáneamente
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private DatabaseManager dbManager;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String direccionCliente;

    /**
     * Constructor
     * @param socket Socket del cliente conectado
     * @param dbManager Instancia del gestor de base de datos
     */
    public ClientHandler(Socket socket, DatabaseManager dbManager) {
        this.socket = socket;
        this.dbManager = dbManager;
        this.direccionCliente = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    /**
     * Método principal que se ejecuta en el hilo
     */
    @Override
    public void run() {
        try {
            // Inicializar streams de entrada y salida
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("✓ Manejador iniciado para cliente: " + direccionCliente);

            String mensajeCliente;

            // Bucle principal de comunicación con el cliente
            while ((mensajeCliente = entrada.readLine()) != null) {
                System.out.println("📨 [" + direccionCliente + "] Comando recibido: " + mensajeCliente);

                // Si el cliente envía SALIR, terminar la conexión
                if (mensajeCliente.equals("SALIR")) {
                    salida.println("SUCCESS: Conexión cerrada por el servidor. ¡Hasta pronto!");
                    System.out.println("👋 [" + direccionCliente + "] Cliente solicitó desconexión");
                    break;
                }

                // Procesar el comando y obtener respuesta
                String respuesta = procesarComando(mensajeCliente);

                // Enviar respuesta al cliente
                salida.println(respuesta);
                System.out.println("📤 [" + direccionCliente + "] Respuesta enviada\n");
            }

        } catch (IOException e) {
            System.err.println("✗ [" + direccionCliente + "] Error en comunicación: " + e.getMessage());
        } finally {
            cerrarConexion();
        }
    }

    /**
     * Procesa los comandos recibidos del cliente
     * @param comando Comando en formato: OPERACION|param1|param2|...
     * @return Respuesta del servidor
     */
    private String procesarComando(String comando) {
        try {
            // Validar que el comando no esté vacío
            if (comando == null || comando.trim().isEmpty()) {
                return "ERROR: Comando vacío";
            }

            // Delegar el procesamiento al DatabaseManager
            String respuesta = dbManager.procesarComando(comando);

            return respuesta;

        } catch (Exception e) {
            return "ERROR: Error al procesar comando - " + e.getMessage();
        }
    }

    /**
     * Cierra todos los recursos asociados a este cliente
     */
    private void cerrarConexion() {
        try {
            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("← [" + direccionCliente + "] Cliente desconectado\n");

        } catch (IOException e) {
            System.err.println("✗ [" + direccionCliente + "] Error al cerrar conexión: " + e.getMessage());
        }
    }

    /**
     * Obtiene la dirección del cliente
     * @return String con la dirección IP y puerto del cliente
     */
    public String getDireccionCliente() {
        return direccionCliente;
    }
}