package cliente;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Cliente UDP para comunicarse con el servidor
 */
public class ClienteUDP {
    private static final String HOST = "localhost"; // Cambiar por IP del servidor si es necesario
    private static final int PUERTO = 5001;
    private static final int TAMAÑO_BUFFER = 65535;

    private DatagramSocket socket;
    private InetAddress direccionServidor;
    private Scanner scanner;

    public ClienteUDP() {
        scanner = new Scanner(System.in);
    }

    /**
     * Conecta con el servidor (inicializa el socket)
     */
    public boolean conectar() {
        try {
            socket = new DatagramSocket();
            direccionServidor = InetAddress.getByName(HOST);

            System.out.println("\n✓ Cliente UDP configurado para servidor en " + HOST + ":" + PUERTO);
            return true;

        } catch (SocketException | UnknownHostException e) {
            System.err.println("✗ Error al configurar el cliente: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envía un comando al servidor y recibe la respuesta
     */
    public String enviarComando(String comando) {
        try {
            // Enviar comando
            byte[] bufferSalida = comando.getBytes();
            DatagramPacket paqueteSalida = new DatagramPacket(
                    bufferSalida, bufferSalida.length, direccionServidor, PUERTO);
            socket.send(paqueteSalida);

            // Recibir respuesta
            byte[] bufferEntrada = new byte[TAMAÑO_BUFFER];
            DatagramPacket paqueteEntrada = new DatagramPacket(bufferEntrada, bufferEntrada.length);

            // Timeout de 5 segundos
            socket.setSoTimeout(5000);
            socket.receive(paqueteEntrada);

            String respuesta = new String(paqueteEntrada.getData(), 0, paqueteEntrada.getLength());
            return respuesta;

        } catch (SocketTimeoutException e) {
            return "ERROR: Tiempo de espera agotado. El servidor no respondió.";
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
            System.out.println("║        CLIENTE UDP - MENÚ PRINCIPAL      ║");
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
            if (socket != null && !socket.isClosed()) {
                enviarComando("SALIR");
                socket.close();
            }
            scanner.close();
            System.out.println("\n✓ Desconectado del servidor");
        } catch (Exception e) {
            System.err.println("✗ Error al cerrar conexión: " + e.getMessage());
        }
    }

    /**
     * Método main
     */
    public static void main(String[] args) {
        ClienteUDP cliente = new ClienteUDP();

        if (cliente.conectar()) {
            cliente.mostrarMenu();
            cliente.desconectar();
        }
    }
}