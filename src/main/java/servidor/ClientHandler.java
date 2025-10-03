package servidor;

import java.io.*;
import java.net.Socket;

/**
 * Clase para manejar cada cliente TCP en un hilo separado
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private DatabaseManager dbManager;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String direccionCliente;

    public ClientHandler(Socket socket, DatabaseManager dbManager) {
        this.socket = socket;
        this.dbManager = dbManager;
        this.direccionCliente = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("✓ Manejador iniciado para cliente: " + direccionCliente);

            String mensajeCliente;

            while ((mensajeCliente = entrada.readLine()) != null) {
                System.out.println("📨 [" + direccionCliente + "] Comando recibido: " + mensajeCliente);

                if (mensajeCliente.equals("SALIR")) {
                    salida.println("SUCCESS: Conexión cerrada por el servidor. ¡Hasta pronto!");
                    System.out.println("👋 [" + direccionCliente + "] Cliente solicitó desconexión");
                    break;
                }

                String respuesta = procesarComando(mensajeCliente);
                enviarRespuesta(respuesta);
                System.out.println("📤 [" + direccionCliente + "] Respuesta enviada\n");
            }

        } catch (IOException e) {
            System.err.println("✗ [" + direccionCliente + "] Error en comunicación: " + e.getMessage());
        } finally {
            cerrarConexion();
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

                default:
                    return "ERROR: Comando no reconocido: " + operacion;
            }

        } catch (NumberFormatException e) {
            return "ERROR: Formato de número inválido";
        } catch (Exception e) {
            return "ERROR: Error al procesar comando - " + e.getMessage();
        }
    }

    private void enviarRespuesta(String respuesta) {
        if (respuesta.contains("\n")) {
            String[] lineas = respuesta.split("\n");
            for (String linea : lineas) {
                salida.println(linea);
            }
        } else {
            salida.println(respuesta);
        }
        salida.flush();
    }

    private void cerrarConexion() {
        try {
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("← [" + direccionCliente + "] Cliente desconectado\n");
        } catch (IOException e) {
            System.err.println("✗ [" + direccionCliente + "] Error al cerrar conexión: " + e.getMessage());
        }
    }

    public String getDireccionCliente() {
        return direccionCliente;
    }
}