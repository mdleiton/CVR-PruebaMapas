package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.widget.Toast;

public class SavedMap {
    private int id;
    private String name;
    private double cLatitude,cLongitude,zoomlvl;
    private String date;


    public SavedMap() {
    }

    public SavedMap(int id, String name, double cLatitude, double cLongitude,double zoomlvl, String date) {
        this.id = id;
        this.name = name;
        this.cLatitude = cLatitude;
        this.cLongitude = cLongitude;
        this.zoomlvl = zoomlvl;
        this.date = date;
    }
    public SavedMap(String textoParsear){
        String[] elementos = textoParsear.split(",");
        try{
            this.id = Integer.parseInt(elementos[0]);
            this.name = elementos[1];
            this.cLatitude = Float.parseFloat(elementos[2]);
            this.cLongitude = Float.parseFloat(elementos[3]);
            this.zoomlvl = Float.parseFloat(elementos[4]);
            this.date= elementos[5];
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public double getZoomlvl() {
        return zoomlvl;
    }

    public void setZoomlvl(double zoomlvl) {
        this.zoomlvl = zoomlvl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getcLatitude() {
        return cLatitude;
    }

    public void setcLatitude(double cLatitude) {
        this.cLatitude = cLatitude;
    }

    public double getcLongitude() {
        return cLongitude;
    }

    public void setcLongitude(double cLongitude) {
        this.cLongitude = cLongitude;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
    public String toString(){
        return this.id+","+this.name+","+this.cLatitude+","+this.cLongitude+","+this.zoomlvl+","+this.date;
    }
}
