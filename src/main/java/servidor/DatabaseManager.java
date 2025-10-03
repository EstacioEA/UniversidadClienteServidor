package servidor;

import modelo.Estudiante;
import java.sql.*;
import java.util.*;

/**
 * Clase para gestionar las operaciones de base de datos
 */
public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://192.168.131.22:5432/Universidad_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    private Connection connection;

    // Constructor
    public DatabaseManager() {
        conectar();
        crearTablaEstudiantes();
    }

    /**
     * Establece conexión con la base de datos
     */
    private void conectar() {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✓ Conexión a la base de datos exitosa");
        } catch (ClassNotFoundException e) {
            System.err.println("✗ Driver PostgreSQL no encontrado");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("✗ Error al conectar a la base de datos");
            e.printStackTrace();
        }
    }

    /**
     * Crea la tabla estudiantes si no existe
     */
    private void crearTablaEstudiantes() {
        String sql = "CREATE TABLE IF NOT EXISTS estudiantes (" +
                "id SERIAL PRIMARY KEY," +
                "nombre VARCHAR(100) NOT NULL," +
                "apellido VARCHAR(100) NOT NULL," +
                "carrera VARCHAR(100) NOT NULL," +
                "semestre INTEGER NOT NULL)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("✓ Tabla estudiantes verificada/creada");
        } catch (SQLException e) {
            System.err.println("✗ Error al crear tabla");
            e.printStackTrace();
        }
    }

    /**
     * Inserta un nuevo estudiante
     */
    public String insertarEstudiante(Estudiante estudiante) {
        String sql = "INSERT INTO estudiantes (nombre, apellido, carrera, semestre) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, estudiante.getNombre());
            pstmt.setString(2, estudiante.getApellido());
            pstmt.setString(3, estudiante.getCarrera());
            pstmt.setInt(4, estudiante.getSemestre());

            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return "SUCCESS: Estudiante insertado con ID: " + id;
                }
            }
            return "ERROR: No se pudo insertar el estudiante";
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Consulta todos los estudiantes
     */
    public String consultarEstudiantes() {
        String sql = "SELECT * FROM estudiantes ORDER BY id";
        StringBuilder resultado = new StringBuilder();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            resultado.append("=== LISTA DE ESTUDIANTES ===\n");
            int contador = 0;

            while (rs.next()) {
                contador++;
                resultado.append(String.format("ID: %d | %s %s | Carrera: %s | Semestre: %d\n",
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getString("carrera"),
                        rs.getInt("semestre")));
            }

            if (contador == 0) {
                resultado.append("No hay estudiantes registrados\n");
            } else {
                resultado.append("Total: ").append(contador).append(" estudiante(s)\n");
            }

            return resultado.toString();
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Actualiza un estudiante por ID
     */
    public String actualizarEstudiante(Estudiante estudiante) {
        String sql = "UPDATE estudiantes SET nombre=?, apellido=?, carrera=?, semestre=? WHERE id=?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, estudiante.getNombre());
            pstmt.setString(2, estudiante.getApellido());
            pstmt.setString(3, estudiante.getCarrera());
            pstmt.setInt(4, estudiante.getSemestre());
            pstmt.setInt(5, estudiante.getId());

            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                return "SUCCESS: Estudiante actualizado correctamente";
            } else {
                return "ERROR: No se encontró estudiante con ID: " + estudiante.getId();
            }
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Elimina un estudiante por ID
     */
    public String eliminarEstudiante(int id) {
        String sql = "DELETE FROM estudiantes WHERE id=?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                return "SUCCESS: Estudiante eliminado correctamente";
            } else {
                return "ERROR: No se encontró estudiante con ID: " + id;
            }
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Procesa comandos recibidos del cliente
     */
    public String procesarComando(String comando) {
        String[] partes = comando.split("\\|");
        String operacion = partes[0];

        switch (operacion) {
            case "INSERTAR":
                if (partes.length == 5) {
                    Estudiante est = new Estudiante(partes[1], partes[2], partes[3],
                            Integer.parseInt(partes[4]));
                    return insertarEstudiante(est);
                }
                return "ERROR: Formato incorrecto";

            case "CONSULTAR":
                return consultarEstudiantes();

            case "ACTUALIZAR":
                if (partes.length == 6) {
                    Estudiante est = new Estudiante(Integer.parseInt(partes[1]),
                            partes[2], partes[3], partes[4],
                            Integer.parseInt(partes[5]));
                    return actualizarEstudiante(est);
                }
                return "ERROR: Formato incorrecto";

            case "ELIMINAR":
                if (partes.length == 2) {
                    return eliminarEstudiante(Integer.parseInt(partes[1]));
                }
                return "ERROR: Formato incorrecto";

            default:
                return "ERROR: Operación no reconocida";
        }
    }

    /**
     * Cierra la conexión a la base de datos
     */
    public void cerrarConexion() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ Conexión cerrada");
            }
        } catch (SQLException e) {
            System.err.println("✗ Error al cerrar conexión");
            e.printStackTrace();
        }
    }
}