package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

public class RecorridoGuardar {

    private ArrayList<GeoPoint> geoPoints;
    private ArrayList<Polyline> lines;
    private int lineColor;
    private int idNumber;
    private String sesionId;

    public RecorridoGuardar(int lineColor, int idNumber, String sesionId) {
        this.geoPoints = new ArrayList<>();
        this.lines = new ArrayList<>();
        this.lineColor = lineColor;
        this.idNumber = idNumber;
        this.sesionId = sesionId;
    }

    public RecorridoGuardar(boolean bool) {
        if(bool){
            this.geoPoints = new ArrayList<>();
            this.lines = new ArrayList<>();
        }
    }

    public RecorridoGuardar(ArrayList<GeoPoint> geoPoints, ArrayList<Polyline> lines, int lineColor, int idNumber, String sesionId) {
        this.geoPoints = geoPoints;
        this.lines = lines;
        this.lineColor = lineColor;
        this.idNumber = idNumber;
        this.sesionId = sesionId;
    }

    public void addPoint(GeoPoint m){
        if(m!=null)
            this.geoPoints.add(m);
    }
    public void addLine(Polyline l){
        if(l!=null)
            this.lines.add(l);
    }
    public GeoPoint getLastMarker(){
        return this.geoPoints.get(this.geoPoints.size()-1);
    }

    public GeoPoint getLastPoint(){
        return this.geoPoints.get(this.geoPoints.size()-1);
    }
    public ArrayList<GeoPoint> getGeoPoints() {
        return geoPoints;
    }

    public void setGeoPoints(ArrayList<GeoPoint> geoPoints) {
        this.geoPoints = geoPoints;
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
