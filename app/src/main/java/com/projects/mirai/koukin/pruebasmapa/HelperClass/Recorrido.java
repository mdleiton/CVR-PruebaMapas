package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

public class Recorrido {

    private ArrayList<Marker> markers;
    private ArrayList<Polyline> lines;
    private int lineColor;
    private int idNumber;
    private String sesionId;

    public Recorrido(ArrayList<Marker> markers, ArrayList<Polyline> lines, int lineColor, int idNumber, String sesionId) {
        this.markers = markers;
        this.lines = lines;
        this.lineColor = lineColor;
        this.idNumber = idNumber;
        this.sesionId = sesionId;
    }

    public Recorrido() {
    }

    public void addPoint(Marker m){
        if(m!=null)
            this.markers.add(m);
    }
    public void addLine(Polyline l){
        if(l!=null)
            this.lines.add(l);
    }
    public Marker getLastMarker(){
        return this.markers.get(this.markers.size()-1);
    }

    public GeoPoint getLastPoint(){
        return this.markers.get(this.markers.size()-1).getPosition();
    }




    public ArrayList<Marker> getMarkers() {
        return markers;
    }

    public void setMarkers(ArrayList<Marker> markers) {
        this.markers = markers;
    }

    public ArrayList<Polyline> getLines() {
        return lines;
    }

    public void setLines(ArrayList<Polyline> lines) {
        this.lines = lines;
    }

    public int getLineColor() {
        return lineColor;
    }

    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    public int getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(int idNumber) {
        this.idNumber = idNumber;
    }

    public String getSesionId() {
        return sesionId;
    }

    public void setSesionId(String sesionId) {
        this.sesionId = sesionId;
    }
}
