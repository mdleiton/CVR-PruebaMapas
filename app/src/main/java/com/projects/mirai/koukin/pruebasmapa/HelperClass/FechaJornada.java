package com.projects.mirai.koukin.pruebasmapa.HelperClass;

public class FechaJornada {
    private String nombre,fecha;
    private String dia,mes,año;

    public FechaJornada(){}

    public FechaJornada(String nombre, String dia, String mes, String año, String fecha) {
        this.nombre = nombre;
        this.dia = dia;
        this.mes = mes;
        this.año = año;
        this.fecha = fecha;
    }
    public FechaJornada(String nombre, String fecha) {
        this.nombre = nombre;
        this.fecha = fecha;
        this.evaluar();
    }

    public void evaluar(){
        String[] elementos = fecha.split("-");
        this.dia=elementos[0];
        this.mes=elementos[1];
        this.año=elementos[2];
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDia() {
        return dia;
    }

    public void setDia(String dia) {
        this.dia = dia;
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public String getAño() {
        return año;
    }

    public void setAño(String año) {
        this.año = año;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    @Override
    public String toString() {
        return  nombre + ","+ fecha;
    }
}
