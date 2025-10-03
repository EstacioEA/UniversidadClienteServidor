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

    public void iniciar() {
        try {
            socket = new DatagramSocket(PUERTO);
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║   SERVIDOR UDP INICIADO                ║");
            System.out.println("║   Puerto: " + PUERTO + "                        ║");
            System.out.println("║   Esperando peticiones...              ║");
            System.out.println("╚════════════════════════════════════════╝\n");

            byte[] buffer = new byte[BUFFER_SIZE];

            while (ejecutando) {
                try {
                    DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length);
                    socket.receive(paqueteRecibido);

                    String comando = new String(paqueteRecibido.getData(), 0,
                            paqueteRecibido.getLength());

                    InetAddress direccionCliente = paqueteRecibido.getAddress();
                    int puertoCliente = paqueteRecibido.getPort();

                    System.out.println("→ Petición UDP recibida desde: " +
                            direccionCliente.getHostAddress() + ":" + puertoCliente);
                    System.out.println("📨 Comando: " + comando);

                    String respuesta = procesarComando(comando);

                    byte[] datosRespuesta = respuesta.getBytes();
                    DatagramPacket paqueteRespuesta = new DatagramPacket(
                            datosRespuesta,
                            datosRespuesta.length,
                            direccionCliente,
                            puertoCliente
                    );

                    socket.send(paqueteRespuesta);
                    System.out.println("📤 Respuesta enviada al cliente\n");

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

    private String procesarComando(String comando) {
        try {
            if (comando == null || comando.trim().isEmpty()) {
                return "ERROR: Comando vacío";
            }

            String[] partes = comando.split("\\|");

            if (partes.length == 0) {
                return "ERROR: Comando inválido";
            }

            String operacion = partes[0].toUpperCase();

            switch (operacion) {
                // ========== OPERACIONES UNIVERSIDADES ==========
                case "INSERTAR_UNIVERSIDAD":
                    if (partes.length == 4) {
                        return dbManager.insertarUniversidad(
                                partes[1],  // nombre
                                partes[2],  // ciudad
                                partes[3]   // pais
                        );
                    }
                    return "ERROR: Formato incorrecto. Use: INSERTAR_UNIVERSIDAD|nombre|ciudad|pais";

                case "CONSULTAR_UNIVERSIDADES":
                    return dbManager.consultarUniversidades();

                case "ACTUALIZAR_UNIVERSIDAD":
                    if (partes.length == 5) {
                        return dbManager.actualizarUniversidad(
                                Integer.parseInt(partes[1]),  // id
                                partes[2],  // nombre
                                partes[3],  // ciudad
                                partes[4]   // pais
                        );
                    }
                    return "ERROR: Formato incorrecto. Use: ACTUALIZAR_UNIVERSIDAD|id|nombre|ciudad|pais";

                case "ELIMINAR_UNIVERSIDAD":
                    if (partes.length == 2) {
                        return dbManager.eliminarUniversidad(Integer.parseInt(partes[1]));
                    }
                    return "ERROR: Formato incorrecto. Use: ELIMINAR_UNIVERSIDAD|id";

                // ========== OPERACIONES ESTUDIANTES ==========
                case "INSERTAR_ESTUDIANTE":
                    if (partes.length == 6) {
                        return dbManager.insertarEstudiante(
                                partes[1],  // nombre
                                partes[2],  // apellido
                                partes[3],  // email
                                Integer.parseInt(partes[4]),  // edad
                                Integer.parseInt(partes[5])   // universidad_id
                        );
                    }
                    return "ERROR: Formato incorrecto. Use: INSERTAR_ESTUDIANTE|nombre|apellido|email|edad|universidad_id";

                case "CONSULTAR_ESTUDIANTES":
                    return dbManager.consultarEstudiantes();

                case "ACTUALIZAR_ESTUDIANTE":
                    if (partes.length == 7) {
                        return dbManager.actualizarEstudiante(
                                Integer.parseInt(partes[1]),  // id
                                partes[2],  // nombre
                                partes[3],  // apellido
                                partes[4],  // email
                                Integer.parseInt(partes[5]),  // edad
                                Integer.parseInt(partes[6])   // universidad_id
                        );
                    }
                    return "ERROR: Formato incorrecto. Use: ACTUALIZAR_ESTUDIANTE|id|nombre|apellido|email|edad|universidad_id";

                case "ELIMINAR_ESTUDIANTE":
                    if (partes.length == 2) {
                        return dbManager.eliminarEstudiante(Integer.parseInt(partes[1]));
                    }
                    return "ERROR: Formato incorrecto. Use: ELIMINAR_ESTUDIANTE|id";

                case "SALIR":
                    return "✓ Conexión cerrada";

                default:
                    return "ERROR: Comando no reconocido: " + operacion;
            }

        } catch (NumberFormatException e) {
            return "ERROR: Formato de número inválido";
        } catch (Exception e) {
            return "ERROR: Error al procesar comando - " + e.getMessage();
        }
    }

    public void detener() {
        ejecutando = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        dbManager.cerrarConexion();
        System.out.println("\n✓ Servidor UDP detenido");
    }

    public static void main(String[] args) {
        ServidorUDP servidor = new ServidorUDP();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚠ Cerrando servidor...");
            servidor.detener();
        }));

        servidor.iniciar();
    }
}