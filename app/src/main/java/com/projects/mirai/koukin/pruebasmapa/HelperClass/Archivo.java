package com.projects.mirai.koukin.pruebasmapa.HelperClass;

public class Archivo {
    private String nombre;
    private String fecha_creacion;


    public Archivo(){}

    public Archivo(String nombre,String fecha_creacion){
        this.nombre = nombre;
        this.fecha_creacion = fecha_creacion;

    }

    public Archivo(String nombre){
        this.nombre = nombre;
        this.evaluar_fecha();

    }

    public void evaluar_fecha(){
            String[] elementos = nombre.split("\\|");
            this.fecha_creacion = elementos[1];
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getFecha_creacion() {
        return fecha_creacion;
    }

    public void setFecha_creacion(String fecha_creacion) {
        this.fecha_creacion = fecha_creacion;
    }
}
