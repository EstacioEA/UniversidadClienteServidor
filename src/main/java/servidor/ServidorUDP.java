package servidor;

import java.io.IOException;
import java.net.*;

/**
 * Servidor UDP para manejar peticiones de clientes
 */
public class ServidorUDP {
    private static final int PUERTO = 5001;
    private static final int BUFFER_SIZE = 65535;
    private DatagramSocket socket;
    private DatabaseManager dbManager;
    private boolean ejecutando;

    public ServidorUDP() {
        dbManager = new DatabaseManager();
        ejecutando = true;
    }

    /**
     * Inicia el servidor UDP
     */
    public void iniciar() {
        try {
            socket = new DatagramSocket(PUERTO);
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║\tSERVIDOR UDP INICIADO\t\t║");
            System.out.println("║\tPuerto: " + PUERTO + "\t║");
            System.out.println("║\tEsperando peticiones...\t\t║");
            System.out.println("╚════════════════════════════════════════╝\n");

            byte[] buffer = new byte[BUFFER_SIZE];

            while (ejecutando) {
                try {
                    // Recibir paquete del cliente
                    DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length);
                    socket.receive(paqueteRecibido);

                    // Extraer datos del paquete
                    String comando = new String(paqueteRecibido.getData(), 0,
                            paqueteRecibido.getLength());

                    InetAddress direccionCliente = paqueteRecibido.getAddress();
                    int puertoCliente = paqueteRecibido.getPort();

                    System.out.println("→ Petición UDP recibida desde: " +
                            direccionCliente.getHostAddress() + ":" + puertoCliente);
                    System.out.println("📨 Comando: " + comando);

                    // Procesar comando
                    String respuesta = dbManager.procesarComando(comando);

                    // Enviar respuesta al cliente
                    byte[] datosRespuesta = respuesta.getBytes();
                    DatagramPacket paqueteRespuesta = new DatagramPacket(
                            datosRespuesta,
                            datosRespuesta.length,
                            direccionCliente,
                            puertoCliente
                    );

                    socket.send(paqueteRespuesta);
                    System.out.println("📤 Respuesta enviada al cliente\n");

                    // Limpiar buffer
                    buffer = new byte[BUFFER_SIZE];

                } catch (IOException e) {
                    if (ejecutando) {
                        System.err.println("✗ Error al procesar petición: " + e.getMessage());
                    }
                }
            }

        } catch (SocketException e) {
            System.err.println("✗ Error al crear socket UDP: " + e.getMessage());
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
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        dbManager.cerrarConexion();
        System.out.println("\n✓ Servidor UDP detenido");
    }

    /**
     * Método main para ejecutar el servidor
     */
    public static void main(String[] args) {
        ServidorUDP servidor = new ServidorUDP();

        // Agregar shutdown hook para cerrar limpiamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚠ Cerrando servidor...");
            servidor.detener();
        }));

        servidor.iniciar();
    }
}