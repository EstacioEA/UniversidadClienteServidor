package servidor;

import java.sql.*;

/**
 * Clase para manejar la conexión y operaciones con PostgreSQL
 */
public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://192.168.131.22:5432/universidad_db";
    private static final String USUARIO = "postgres";
    private static final String PASSWORD = "postgres";

    private Connection conexion;

    public DatabaseManager() {
        conectar();
    }

    /**
     * Establece la conexión a la base de datos
     */
    private void conectar() {
        try {
            conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);
            System.out.println("✓ Conexión a base de datos establecida");
        } catch (SQLException e) {
            System.err.println("✗ Error al conectar con la base de datos: " + e.getMessage());
        }
    }

    /**
     * Obtiene una conexión a la base de datos
     */
    private Connection obtenerConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conectar();
        }
        return conexion;
    }

    // ==================== OPERACIONES UNIVERSIDADES ====================

    /**
     * Inserta una nueva universidad
     */
    public String insertarUniversidad(String nombre, String ciudad, String pais) {
        String sql = "INSERT INTO universidades (nombre, ciudad, pais) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = obtenerConexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, ciudad);
            pstmt.setString(3, pais);

            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return "✓ Universidad insertada exitosamente con ID: " + id;
                }
            }
            return "✓ Universidad insertada exitosamente";

        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Consulta todas las universidades
     */
    public String consultarUniversidades() {
        String sql = "SELECT * FROM universidades ORDER BY id";
        StringBuilder resultado = new StringBuilder();

        try (Statement stmt = obtenerConexion().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            resultado.append("═══════════════════════════════════════════════════════════\n");
            resultado.append("                    LISTA DE UNIVERSIDADES\n");
            resultado.append("═══════════════════════════════════════════════════════════\n");

            while (rs.next()) {
                count++;
                resultado.append(String.format("ID: %-4d | %-30s | %-20s | %s\n",
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("ciudad"),
                        rs.getString("pais")));
            }

            resultado.append("═══════════════════════════════════════════════════════════\n");
            resultado.append(String.format("Total: %d universidad(es)\n", count));

            if (count == 0) {
                return "No hay universidades registradas en la base de datos.";
            }

            return resultado.toString();

        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Actualiza una universidad existente
     */
    public String actualizarUniversidad(int id, String nombre, String ciudad, String pais) {
        String sql = "UPDATE universidades SET nombre = ?, ciudad = ?, pais = ? WHERE id = ?";

        try (PreparedStatement pstmt = obtenerConexion().prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, ciudad);
            pstmt.setString(3, pais);
            pstmt.setInt(4, id);

            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                return "✓ Universidad actualizada exitosamente";
            } else {
                return "⚠ No se encontró ninguna universidad con ID: " + id;
            }

        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Elimina una universidad
     */
    public String eliminarUniversidad(int id) {
        String sql = "DELETE FROM universidades WHERE id = ?";

        try (PreparedStatement pstmt = obtenerConexion().prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                return "✓ Universidad eliminada exitosamente";
            } else {
                return "⚠ No se encontró ninguna universidad con ID: " + id;
            }

        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // ==================== OPERACIONES ESTUDIANTES ====================

    /**
     * Inserta un nuevo estudiante
     */
    public String insertarEstudiante(String nombre, String apellido, String email, int edad, int universidadId) {
        String sql = "INSERT INTO estudiantes (nombre, apellido, email, edad, universidad_id) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = obtenerConexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, apellido);
            pstmt.setString(3, email);
            pstmt.setInt(4, edad);
            pstmt.setInt(5, universidadId);

            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return "✓ Estudiante insertado exitosamente con ID: " + id;
                }
            }
            return "✓ Estudiante insertado exitosamente";

        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Consulta todos los estudiantes con su universidad
     */
    public String consultarEstudiantes() {
        String sql = "SELECT e.id, e.nombre, e.apellido, e.email, e.edad, e.universidad_id, u.nombre as universidad_nombre " +
                "FROM estudiantes e " +
                "LEFT JOIN universidades u ON e.universidad_id = u.id " +
                "ORDER BY e.id";
        StringBuilder resultado = new StringBuilder();

        try (Statement stmt = obtenerConexion().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            resultado.append("═══════════════════════════════════════════════════════════════════════════════\n");
            resultado.append("                           LISTA DE ESTUDIANTES\n");
            resultado.append("═══════════════════════════════════════════════════════════════════════════════\n");

            while (rs.next()) {
                count++;
                String universidad = rs.getString("universidad_nombre");
                if (universidad == null) {
                    universidad = "Sin asignar";
                }

                resultado.append(String.format("ID: %-4d | %-15s %-15s | %-25s | Edad: %-3d | %s\n",
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getString("email"),
                        rs.getInt("edad"),
                        universidad));
            }

            resultado.append("═══════════════════════════════════════════════════════════════════════════════\n");
            resultado.append(String.format("Total: %d estudiante(s)\n", count+1));

            if (count == 0) {
                return "No hay estudiantes registrados en la base de datos.";
            }

            return resultado.toString();

        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Actualiza un estudiante existente
     */
    public String actualizarEstudiante(int id, String nombre, String apellido, String email, int edad, int universidadId) {
        String sql = "UPDATE estudiantes SET nombre = ?, apellido = ?, email = ?, edad = ?, universidad_id = ? WHERE id = ?";

        try (PreparedStatement pstmt = obtenerConexion().prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, apellido);
            pstmt.setString(3, email);
            pstmt.setInt(4, edad);
            pstmt.setInt(5, universidadId);
            pstmt.setInt(6, id);

            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                return "✓ Estudiante actualizado exitosamente";
            } else {
                return "⚠ No se encontró ningún estudiante con ID: " + id;
            }

        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Elimina un estudiante
     */
    public String eliminarEstudiante(int id) {
        String sql = "DELETE FROM estudiantes WHERE id = ?";

        try (PreparedStatement pstmt = obtenerConexion().prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                return "✓ Estudiante eliminado exitosamente";
            } else {
                return "⚠ No se encontró ningún estudiante con ID: " + id;
            }

        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Cierra la conexión a la base de datos
     */
    public void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                System.out.println("✓ Conexión a base de datos cerrada");
            }
        } catch (SQLException e) {
            System.err.println("✗ Error al cerrar conexión: " + e.getMessage());
        }
    }
}