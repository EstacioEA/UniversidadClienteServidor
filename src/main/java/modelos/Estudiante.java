package modelos;

/**
 * Clase modelo para representar un estudiante
 */
public class Estudiante {
    private int id;
    private String nombre;
    private String apellido;
    private String email;
    private int edad;
    private int universidadId;
    private String universidadNombre; // Para mostrar en consultas

    // Constructor vacío
    public Estudiante() {
    }

    // Constructor completo con universidad
    public Estudiante(int id, String nombre, String apellido, String email, int edad, int universidadId) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.edad = edad;
        this.universidadId = universidadId;
    }

    // Constructor sin ID (para insertar)
    public Estudiante(String nombre, String apellido, String email, int edad, int universidadId) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.edad = edad;
        this.universidadId = universidadId;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public int getUniversidadId() {
        return universidadId;
    }

    public void setUniversidadId(int universidadId) {
        this.universidadId = universidadId;
    }

    public String getUniversidadNombre() {
        return universidadNombre;
    }

    public void setUniversidadNombre(String universidadNombre) {
        this.universidadNombre = universidadNombre;
    }

    @Override
    public String toString() {
        return String.format("ID: %d | %s %s | %s | Edad: %d | Universidad: %s",
                id, nombre, apellido, email, edad,
                universidadNombre != null ? universidadNombre : "ID: " + universidadId);
    }
}