package modelos;

/**
 * Clase modelo para representar una universidad
 */
public class Universidad {
    private int id;
    private String nombre;
    private String ciudad;
    private String pais;

    // Constructor vacío
    public Universidad() {
    }

    // Constructor completo
    public Universidad(int id, String nombre, String ciudad, String pais) {
        this.id = id;
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.pais = pais;
    }

    // Constructor sin ID (para insertar)
    public Universidad(String nombre, String ciudad, String pais) {
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.pais = pais;
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

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    @Override
    public String toString() {
        return String.format("ID: %d | %s | %s, %s",
                id, nombre, ciudad, pais);
    }
}