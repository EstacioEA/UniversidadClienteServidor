package modelos;

import java.io.Serializable;

/**
 * Clase modelo para representar un estudiante
 */
public class Estudiante implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String nombre;
    private String apellido;
    private String carrera;
    private int semestre;

    // Constructor vacío
    public Estudiante() {}

    // Constructor completo
    public Estudiante(int id, String nombre, String apellido, String carrera, int semestre) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.carrera = carrera;
        this.semestre = semestre;
    }

    // Constructor sin ID (para inserciones)
    public Estudiante(String nombre, String apellido, String carrera, int semestre) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.carrera = carrera;
        this.semestre = semestre;
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

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public int getSemestre() {
        return semestre;
    }

    public void setSemestre(int semestre) {
        this.semestre = semestre;
    }

    @Override
    public String toString() {
        return "ID: " + id + " | " + nombre + " " + apellido +
                " | Carrera: " + carrera + " | Semestre: " + semestre;
    }
}