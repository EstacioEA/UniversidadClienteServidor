package cliente;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Cliente TCP para comunicarse con el servidor
 */
public class ClienteTCP {
    private static final String HOST = "localhost"; // Cambiar por IP del servidor si es necesario
    private static final int PUERTO = 5000;

    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private Scanner scanner;

    public ClienteTCP() {
        scanner = new Scanner(System.in);
    }

    /**
     * Conecta con el servidor
     */
    public boolean conectar() {
        try {
            socket = new Socket(HOST, PUERTO);
            salida = new PrintWriter(socket.getOutputStream(), true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("\n✓ Conectado al servidor TCP en " + HOST + ":" + PUERTO);
            return true;

        } catch (IOException e) {
            System.err.println("✗ Error al conectar con el servidor: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envía un comando al servidor y recibe la respuesta
     */
    public String enviarComando(String comando) {
        try {
            salida.println(comando);

            // Leer respuesta (puede ser multilínea)
            StringBuilder respuesta = new StringBuilder();
            String linea;

            // Para comandos de consulta que devuelven múltiples líneas
            if (comando.startsWith("CONSULTAR")) {
                while ((linea = entrada.readLine()) != null) {
                    respuesta.append(linea).append("\n");
                    // Terminar cuando se recibe la línea de total o error
                    if (linea.startsWith("Total:") || linea.startsWith("ERROR:") ||
                            linea.contains("No hay estudiantes")) {
                        break;
                    }
                }
            } else {
                // Para otros comandos, leer una sola línea
                linea = entrada.readLine();
                if (linea != null) {
                    respuesta.append(linea);
                }
            }

            return respuesta.toString();

        } catch (IOException e) {
            return "ERROR: No se pudo comunicar con el servidor - " + e.getMessage();
        }
    }

    /**
     * Muestra el menú interactivo
     */
    public void mostrarMenu() {
        boolean continuar = true;

        while (continuar) {
            System.out.println("\n╔══════════════════════════════════════════╗");
            System.out.println("║        CLIENTE TCP - MENÚ PRINCIPAL      ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  1. Insertar Estudiante                  ║");
            System.out.println("║  2. Consultar Estudiantes                ║");
            System.out.println("║  3. Actualizar Estudiante                ║");
            System.out.println("║  4. Eliminar Estudiante                  ║");
            System.out.println("║  5. Salir                                ║");
            System.out.println("╚══════════════════════════════════════════╝");
            System.out.print("Seleccione una opción: ");

            int opcion = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer

            switch (opcion) {
                case 1:
                    insertarEstudiante();
                    break;
                case 2:
                    consultarEstudiantes();
                    break;
                case 3:
                    actualizarEstudiante();
                    break;
                case 4:
                    eliminarEstudiante();
                    break;
                case 5:
                    continuar = false;
                    break;
                default:
                    System.out.println("⚠ Opción no válida");
            }
        }
    }

    /**
     * Inserta un nuevo estudiante
     */
    private void insertarEstudiante() {
        System.out.println("\n--- INSERTAR ESTUDIANTE ---");
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine();

        System.out.print("Apellido: ");
        String apellido = scanner.nextLine();

        System.out.print("Carrera: ");
        String carrera = scanner.nextLine();

        System.out.print("Semestre: ");
        int semestre = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer

        String comando = String.format("INSERTAR|%s|%s|%s|%d",
                nombre, apellido, carrera, semestre);
        String respuesta = enviarComando(comando);
        System.out.println("\n" + respuesta);
    }

    /**
     * Consulta todos los estudiantes
     */
    private void consultarEstudiantes() {
        System.out.println("\n--- CONSULTAR ESTUDIANTES ---");
        String respuesta = enviarComando("CONSULTAR");
        System.out.println("\n" + respuesta);
    }

    /**
     * Actualiza un estudiante existente
     */
    private void actualizarEstudiante() {
        System.out.println("\n--- ACTUALIZAR ESTUDIANTE ---");
        System.out.print("ID del estudiante a actualizar: ");
        int id = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer

        System.out.print("Nuevo nombre: ");
        String nombre = scanner.nextLine();

        System.out.print("Nuevo apellido: ");
        String apellido = scanner.nextLine();

        System.out.print("Nueva carrera: ");
        String carrera = scanner.nextLine();

        System.out.print("Nuevo semestre: ");
        int semestre = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer

        String comando = String.format("ACTUALIZAR|%d|%s|%s|%s|%d",
                id, nombre, apellido, carrera, semestre);
        String respuesta = enviarComando(comando);
        System.out.println("\n" + respuesta);
    }

    /**
     * Elimina un estudiante
     */
    private void eliminarEstudiante() {
        System.out.println("\n--- ELIMINAR ESTUDIANTE ---");
        System.out.print("ID del estudiante a eliminar: ");
        int id = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer

        String comando = String.format("ELIMINAR|%d", id);
        String respuesta = enviarComando(comando);
        System.out.println("\n" + respuesta);
    }

    /**
     * Cierra la conexión
     */
    public void desconectar() {
        try {
            if (salida != null) {
                salida.println("SALIR");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            scanner.close();
            System.out.println("\n✓ Desconectado del servidor");
        } catch (IOException e) {
            System.err.println("✗ Error al cerrar conexión: " + e.getMessage());
        }
    }

    /**
     * Método main
     */
    public static void main(String[] args) {
        ClienteTCP cliente = new ClienteTCP();

        // Agregar shutdown hook para cerrar correctamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCerrando cliente...");
            cliente.desconectar();
        }));

        if (cliente.conectar()) {
            cliente.mostrarMenu();
            cliente.desconectar();
        }
    }
}