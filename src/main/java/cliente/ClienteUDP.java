package cliente;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Cliente UDP para comunicarse con el servidor
 */
public class ClienteUDP {
    private static final String HOST = "localhost";
    private static final int PUERTO = 5001;
    private static final int TAMAÑO_BUFFER = 65535;

    private DatagramSocket socket;
    private InetAddress direccionServidor;
    private Scanner scanner;

    public ClienteUDP() {
        scanner = new Scanner(System.in);
    }

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

    public String enviarComando(String comando) {
        try {
            byte[] bufferSalida = comando.getBytes();
            DatagramPacket paqueteSalida = new DatagramPacket(
                    bufferSalida, bufferSalida.length, direccionServidor, PUERTO);
            socket.send(paqueteSalida);

            byte[] bufferEntrada = new byte[TAMAÑO_BUFFER];
            DatagramPacket paqueteEntrada = new DatagramPacket(bufferEntrada, bufferEntrada.length);

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

    public void mostrarMenu() {
        boolean continuar = true;

        while (continuar) {
            System.out.println("\n╔══════════════════════════════════════════╗");
            System.out.println("║     CLIENTE UDP - MENÚ PRINCIPAL         ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  1. Gestionar Universidades              ║");
            System.out.println("║  2. Gestionar Estudiantes                ║");
            System.out.println("║  3. Salir                                ║");
            System.out.println("╚══════════════════════════════════════════╝");
            System.out.print("Seleccione una opción: ");

            int opcion = scanner.nextInt();
            scanner.nextLine();

            switch (opcion) {
                case 1:
                    menuUniversidades();
                    break;
                case 2:
                    menuEstudiantes();
                    break;
                case 3:
                    continuar = false;
                    break;
                default:
                    System.out.println("⚠ Opción no válida");
            }
        }
    }

    private void menuUniversidades() {
        boolean continuar = true;

        while (continuar) {
            System.out.println("\n╔══════════════════════════════════════════╗");
            System.out.println("║       GESTIÓN DE UNIVERSIDADES           ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  1. Insertar Universidad                 ║");
            System.out.println("║  2. Consultar Universidades              ║");
            System.out.println("║  3. Actualizar Universidad               ║");
            System.out.println("║  4. Eliminar Universidad                 ║");
            System.out.println("║  5. Volver al menú principal             ║");
            System.out.println("╚══════════════════════════════════════════╝");
            System.out.print("Seleccione una opción: ");

            int opcion = scanner.nextInt();
            scanner.nextLine();

            switch (opcion) {
                case 1:
                    insertarUniversidad();
                    break;
                case 2:
                    consultarUniversidades();
                    break;
                case 3:
                    actualizarUniversidad();
                    break;
                case 4:
                    eliminarUniversidad();
                    break;
                case 5:
                    continuar = false;
                    break;
                default:
                    System.out.println("⚠ Opción no válida");
            }
        }
    }

    private void menuEstudiantes() {
        boolean continuar = true;

        while (continuar) {
            System.out.println("\n╔══════════════════════════════════════════╗");
            System.out.println("║        GESTIÓN DE ESTUDIANTES            ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  1. Insertar Estudiante                  ║");
            System.out.println("║  2. Consultar Estudiantes                ║");
            System.out.println("║  3. Actualizar Estudiante                ║");
            System.out.println("║  4. Eliminar Estudiante                  ║");
            System.out.println("║  5. Volver al menú principal             ║");
            System.out.println("╚══════════════════════════════════════════╝");
            System.out.print("Seleccione una opción: ");

            int opcion = scanner.nextInt();
            scanner.nextLine();

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

    // ========== MÉTODOS UNIVERSIDADES ==========
    private void insertarUniversidad() {
        System.out.println("\n--- INSERTAR UNIVERSIDAD ---");
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine();

        System.out.print("Ciudad: ");
        String ciudad = scanner.nextLine();

        System.out.print("País: ");
        String pais = scanner.nextLine();

        String comando = String.format("INSERTAR_UNIVERSIDAD|%s|%s|%s",
                nombre, ciudad, pais);
        String respuesta = enviarComando(comando);
        System.out.println("\n" + respuesta);
    }

    private void consultarUniversidades() {
        System.out.println("\n--- CONSULTAR UNIVERSIDADES ---");
        String respuesta = enviarComando("CONSULTAR_UNIVERSIDADES");
        System.out.println("\n" + respuesta);
    }

    private void actualizarUniversidad() {
        System.out.println("\n--- ACTUALIZAR UNIVERSIDAD ---");
        System.out.print("ID de la universidad a actualizar: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Nuevo nombre: ");
        String nombre = scanner.nextLine();

        System.out.print("Nueva ciudad: ");
        String ciudad = scanner.nextLine();

        System.out.print("Nuevo país: ");
        String pais = scanner.nextLine();

        String comando = String.format("ACTUALIZAR_UNIVERSIDAD|%d|%s|%s|%s",
                id, nombre, ciudad, pais);
        String respuesta = enviarComando(comando);
        System.out.println("\n" + respuesta);
    }

    private void eliminarUniversidad() {
        System.out.println("\n--- ELIMINAR UNIVERSIDAD ---");
        System.out.print("ID de la universidad a eliminar: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        String comando = String.format("ELIMINAR_UNIVERSIDAD|%d", id);
        String respuesta = enviarComando(comando);
        System.out.println("\n" + respuesta);
    }

    // ========== MÉTODOS ESTUDIANTES ==========
    private void insertarEstudiante() {
        System.out.println("\n--- INSERTAR ESTUDIANTE ---");
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine();

        System.out.print("Apellido: ");
        String apellido = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine();

        System.out.print("Edad: ");
        int edad = scanner.nextInt();

        System.out.print("ID de Universidad: ");
        int universidadId = scanner.nextInt();
        scanner.nextLine();

        String comando = String.format("INSERTAR_ESTUDIANTE|%s|%s|%s|%d|%d",
                nombre, apellido, email, edad, universidadId);
        String respuesta = enviarComando(comando);
        System.out.println("\n" + respuesta);
    }

    private void consultarEstudiantes() {
        System.out.println("\n--- CONSULTAR ESTUDIANTES ---");
        String respuesta = enviarComando("CONSULTAR_ESTUDIANTES");
        System.out.println("\n" + respuesta);
    }

    private void actualizarEstudiante() {
        System.out.println("\n--- ACTUALIZAR ESTUDIANTE ---");
        System.out.print("ID del estudiante a actualizar: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Nuevo nombre: ");
        String nombre = scanner.nextLine();

        System.out.print("Nuevo apellido: ");
        String apellido = scanner.nextLine();

        System.out.print("Nuevo email: ");
        String email = scanner.nextLine();

        System.out.print("Nueva edad: ");
        int edad = scanner.nextInt();

        System.out.print("Nuevo ID de Universidad: ");
        int universidadId = scanner.nextInt();
        scanner.nextLine();

        String comando = String.format("ACTUALIZAR_ESTUDIANTE|%d|%s|%s|%s|%d|%d",
                id, nombre, apellido, email, edad, universidadId);
        String respuesta = enviarComando(comando);
        System.out.println("\n" + respuesta);
    }

    private void eliminarEstudiante() {
        System.out.println("\n--- ELIMINAR ESTUDIANTE ---");
        System.out.print("ID del estudiante a eliminar: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        String comando = String.format("ELIMINAR_ESTUDIANTE|%d", id);
        String respuesta = enviarComando(comando);
        System.out.println("\n" + respuesta);
    }

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

    public static void main(String[] args) {
        ClienteUDP cliente = new ClienteUDP();

        if (cliente.conectar()) {
            cliente.mostrarMenu();
            cliente.desconectar();
        }
    }
}