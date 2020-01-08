package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.projects.mirai.koukin.pruebasmapa.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Class that extends from PositioningSource and acts as an interface to get the Serial link values
 * that are extracted from the piksies and then project those on a map
 * @author manuel
 * @version 1.0
 */
public class PiksiSource extends PositioningSource {

    SerialLink piksi;
    Runnable runnable;
    Handler schedulerRTK;
    public Marker markerPersonaRTK;
    public Polygon p;

    private int distancia = 1;

    /**
     * @return
     */
    @Override
    public double returnLatitude() {

        if(markerPersonaRTK==null){return piksi.getLat();}
        return markerPersonaRTK.getPosition().getLatitude();
    }

    /**
     * @return
     */
    @Override
    public double returnLongitude() {
        if(markerPersonaRTK==null){return piksi.getLon();}
        return markerPersonaRTK.getPosition().getLongitude();
    }

    /**
     * @return
     */
    @Override
    public double returnHeight() {
        if(markerPersonaRTK==null){return  piksi.getElevation();}
        return markerPersonaRTK.getPosition().getAltitude();
    }

    /**
     * Method that inits all the required parameters or permissions necessary to start the
     * callbacks methods to get location updates
     *
     * @param map
     * @param app
     * @param markers
     * @param lines
     * @param mode
     */
    @Override
    public void init(MapView map, AppCompatActivity app, ArrayList<Marker> markers, ArrayList<Polyline> lines, int mode) {
        //delay =UPDATE_INTERVAL_IN_MILLISECONDS; //1 second=1000 milisecond, 15*1000=15seconds
        if(piksi==null){
            piksi = new SerialLink(app);
        }
        if(!piksi.isConnected){
            piksi.destroy();
            piksi = new SerialLink(app);
        }
    }

    /**
     * Method that prepares everything to start to receive location updates
     *
     * @param map
     * @param app
     * @param markers
     * @param lines
     * @param mode
     */
    @Override
    public void startLocationUpdates(final MapView map, final AppCompatActivity app, final ArrayList<Marker> markers, final ArrayList<Polyline> lines, final int mode) {
        schedulerRTK = new Handler();
        //delay =UPDATE_INTERVAL_IN_MILLISECONDS; //1 second=1000 milisecond, 15*1000=15seconds
        if(piksi==null){
            piksi = new SerialLink(app);
        }
        if(!piksi.isConnected){
            piksi.destroy();
            piksi = new SerialLink(app);
        }

        Toast.makeText(app, piksi.type+" connected:"+piksi.isConnected, Toast.LENGTH_LONG).show();
        String data;
        if(!piksi.start()) {
            if(!piksi.start()){
                data = "Piksi no se encuentra conectado al dispositivo ";
                Toast.makeText(app, data, Toast.LENGTH_LONG).show();
                mRequestingLocationUpdates = false;
            }
        }else{
            mRequestingLocationUpdates = true;
            Toast.makeText(app,"Estado: actualizando mapa",Toast.LENGTH_SHORT).show();

            schedulerRTK.postDelayed( runnable = new Runnable() {
                public void run() {
                    updateLocationUI(map,app, markers, lines, mode,txtLocationResult);
                    schedulerRTK.postDelayed(runnable, UPDATE_INTERVAL_IN_MILLISECONDS);
                }
            }, UPDATE_INTERVAL_IN_MILLISECONDS);
        }
    }

    /**
     * Method that gets the LocationUpdates and save them like markers on the map
     *
     * @param map
     * @param app
     * @param markers
     * @param lines
     * @param mode
     */
    @Override
    public String updateLocationUI(MapView map, AppCompatActivity app, ArrayList<Marker> markers, ArrayList<Polyline> lines, int mode) {

        if(piksi==null){
            startLocationUpdates(map,app,markers,lines,mode);
        }

        IMapController mapController = map.getController();
        if(!piksi.isConnected){
            Toast.makeText(app, "Piksi se ha desconectado", Toast.LENGTH_SHORT).show();
            mRequestingLocationUpdates = false;
            this.follow_on=false;
            stopHandlerRTK();
        }else{

            double lastKnowlatitudeRTK = piksi.getLat();
            double lastKnowLongitudeRTK = piksi.getLon();
            String type = piksi.type;
            double height = piksi.height;

            if(lastKnowlatitudeRTK > 91 || lastKnowlatitudeRTK < -91){
                return "";
            }
            //Se encuentra el punto donde se encuentra actualmente
            GeoPoint startPoint = new GeoPoint(lastKnowlatitudeRTK,lastKnowLongitudeRTK,height);
            //Se actualiza el texto inferior
            Deg2UTM transform = new Deg2UTM(lastKnowlatitudeRTK,lastKnowLongitudeRTK);

            if(mode==0){
                saveMarker(map,markers,startPoint);
                if(markers.size()>1){
                    saveLine(map,lines,markers,startPoint);
                }
            }else if(mode==1){
                if(markers.size()==0){
                    Marker startMarker = new Marker(map);
                    saveMarker(map,markers,startPoint);
                }else{
                    startPoint = markers.get(markers.size()-1).getPosition();
                    GeoPoint newPoint = new GeoPoint(lastKnowlatitudeRTK, lastKnowLongitudeRTK,height);
                    double distance = DistanceCalculator.calculateDistanceInMeters(startPoint,newPoint);
                    if(distance >= distancia){
                        saveMarker(map,markers,newPoint);
                        saveLine(map,lines,markers,startPoint);
                    }
                }
            }

            //Se actualiza la markerPersonaRTK y las ubicaciones.
            map.getOverlays().remove(markerPersonaRTK);
            markerPersonaRTK = new Marker(map);
            markerPersonaRTK.setIcon(ContextCompat.getDrawable(app,R.drawable.usericon));
            markerPersonaRTK.setPosition(new GeoPoint(lastKnowlatitudeRTK,lastKnowLongitudeRTK));
            markerPersonaRTK.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(markerPersonaRTK);
            List<GeoPoint> circle = Polygon.pointsAsCircle(new GeoPoint(lastKnowlatitudeRTK,lastKnowLongitudeRTK), 2);
            map.getOverlayManager().remove(p);
            p = new Polygon(map);
            p.setPoints(circle);
            p.setTitle("h: " + String.valueOf(piksi.h_accuracy) + "mm v: " + String.valueOf(piksi.v_accuracy) + "");
            map.getOverlayManager().add(p);
            map.invalidate();
            //return transformtoString(transform,height);

            txtLocationResult.setAlpha(0);
            txtLocationResult.animate().alpha(1).setDuration(300);
            onFollowon.setText("Última Actualización: " + DateFormat.getTimeInstance().format(new Date()));

            if (!self_reference) {
                txtLocationResult.setText(transformtoString(transform, height));
                return transformtoString(transform, height);
            } else {
                txtLocationResult.setText(selfReferenceString(base, new GeoPoint(lastKnowlatitudeRTK, lastKnowLongitudeRTK, height)));
                return selfReferenceString(base, new GeoPoint(lastKnowlatitudeRTK, lastKnowLongitudeRTK, height));
            }
        }
        txtLocationResult.setText("None yet");
        return "null";
    }

    /**
     * Method that gets the LocationUpdates and save them like markers on the map
     *
     * @param map
     * @param app
     * @param markers
     * @param lines
     * @param mode
     * @param txtLocationResult
     */
    @Override
    public String updateLocationUI(MapView map, AppCompatActivity app, ArrayList<Marker> markers, ArrayList<Polyline> lines, int mode, TextView txtLocationResult) {
        if(piksi==null){
            startLocationUpdates(map,app,markers,lines,mode);
        }
        if(!piksi.isConnected){
            Toast.makeText(app, "Piksi se encuentra desconectado", Toast.LENGTH_SHORT).show();
            mRequestingLocationUpdates = false;
            follow_on=false;
            stopHandlerRTK();
        }else{
            IMapController mapController = map.getController();
            //Se solicita la latitud y longitud al RTK
            double lastKnowlatitudeRTK = piksi.getLat();
            double lastKnowLongitudeRTK = piksi.getLon();
            double height = piksi.height;

            txtLocationResult.setText(
                    "Última Actualización:"+DateFormat.getTimeInstance().format(new Date())+" |" + piksi.type  + "|" + String.valueOf(piksi.height + "m"));
            if(lastKnowlatitudeRTK > 91 || lastKnowlatitudeRTK < -91){
                return "";
            }
            //Se encuentra el punto donde se encuentra actualmente
            GeoPoint startPoint = new GeoPoint(lastKnowlatitudeRTK,lastKnowLongitudeRTK);
            //Se actualiza el texto inferior
            Deg2UTM transform = new Deg2UTM(lastKnowlatitudeRTK,lastKnowLongitudeRTK);
            txtLocationResult.setText("Ubicación: " + transform.toString());

            // giving a blink animation on TextView
            txtLocationResult.setAlpha(0);
            txtLocationResult.animate().alpha(1).setDuration(300);

            if(mode==0){
                Marker startMarker = new Marker(map);
                //startMarker.setIcon(getResources().getDrawable(R.drawable.marker));
                startMarker.setPosition(startPoint);
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                map.getOverlays().add(startMarker);
                markers.add(startMarker);
                mapController.setCenter(startPoint);
                if(markers.size()>1){
                    List <GeoPoint> geoPoints = new ArrayList<>();
                    geoPoints.add(markers.get(markers.size()-2).getPosition());
                    geoPoints.add(startPoint);
                    Polyline line = new Polyline();
                    line.setPoints(geoPoints);
                    line.setColor(R.color.colorBlue);
                    lines.add(line);
                    map.getOverlayManager().add(line);
                }
            }else if(mode==1){
                if(markers.size()==0){
                    Marker startMarker = new Marker(map);
                    //startMarker.setIcon(getResources().getDrawable(R.drawable.marker));
                    startMarker.setPosition(startPoint);
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(startMarker);
                    markers.add(startMarker);
                    mapController.setCenter(startPoint);
                }else{
                    startPoint = markers.get(markers.size()-1).getPosition();
                    GeoPoint newPoint = new GeoPoint(lastKnowlatitudeRTK, lastKnowLongitudeRTK);
                    double distance = DistanceCalculator.calculateDistanceInMeters(startPoint,newPoint);
                    if(distance >= distancia){
                        Marker startMarker = new Marker(map);
                        startMarker.setPosition(newPoint);
                        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        map.getOverlays().add(startMarker);
                        markers.add(startMarker);
                        mapController.setCenter(newPoint);
                        List <GeoPoint> geoPoints = new ArrayList<>();
                        geoPoints.add(startPoint);
                        geoPoints.add(newPoint);
                        Polyline line = new Polyline();
                        line.setPoints(geoPoints);
                        lines.add(line);
                        map.getOverlayManager().add(line);
                    }
                }
            }

            //Se actualiza la markerPersonaRTK y las ubicaciones.
            map.getOverlays().remove(markerPersonaRTK);
            markerPersonaRTK = new Marker(map);
            markerPersonaRTK.setIcon(ContextCompat.getDrawable(app,R.drawable.usericon));
            markerPersonaRTK.setPosition(new GeoPoint(lastKnowlatitudeRTK,lastKnowLongitudeRTK));
            markerPersonaRTK.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(markerPersonaRTK);
            List<GeoPoint> circle = Polygon.pointsAsCircle(new GeoPoint(lastKnowlatitudeRTK,lastKnowLongitudeRTK), 2);
            map.getOverlayManager().remove(p);
            p = new Polygon(map);
            p.setPoints(circle);
            p.setTitle("h: " + String.valueOf(piksi.h_accuracy) + "mm v: " + String.valueOf(piksi.v_accuracy) + "");
            map.getOverlayManager().add(p);
            map.invalidate();
            txtLocationResult.setAlpha(0);
            txtLocationResult.animate().alpha(1).setDuration(300);
            onFollowon.setText("Última Actualización: " + DateFormat.getTimeInstance().format(new Date()));
            Toast.makeText(app,connectionStatus() , Toast.LENGTH_SHORT).show();
            if (!self_reference) {
                txtLocationResult.setText(transformtoString(transform, height));
                return transformtoString(transform, height);
            } else {
                txtLocationResult.setText(selfReferenceString(base, new GeoPoint(lastKnowlatitudeRTK, lastKnowLongitudeRTK, height)));
                return selfReferenceString(base, new GeoPoint(lastKnowlatitudeRTK, lastKnowLongitudeRTK, height));
            }
        }

        String estado_conex = connectionStatus();
        txtLocationResult.setText("Esperando... "+ estado_conex);
        return "null";
    }

    /**
     * Method that stops and prepares everything for stopping the location updates according to the
     * data source, current version only covers GPS and RTK modes
     *
     * @param map
     * @param app
     * @param markers
     * @param lines
     * @param mode
     */
    @Override
    public void stopLocationUpdates(MapView map, AppCompatActivity app, ArrayList<Marker> markers, ArrayList<Polyline> lines, int mode) {
        stopHandlerRTK();
    }

    /**
     * Method that positions the map on center with the actual position of the user
     *
     * @param map
     */
    @Override
    public void setActualPositiononMap(MapView map) {
        map.getController().setCenter(new GeoPoint(piksi.getLat(),piksi.getLon(),piksi.height));
        map.getController().setZoom(15.0);
    }

    /**
     * Method that returns a string representation of the actual position
     * @param utm      a Deg2UTM object, representation of utm coordinates
     * @param altitude the actual altitude
     * @return a string representation of the actual position
     */
    @Override
    public String transformtoString(Deg2UTM utm, double altitude) {
        return "Location: " +utm.toString()+"H:"+String.valueOf(altitude);
    }

    /**
     * @return the time of the last obtained update
     */
    @Override
    public String lastKnownUpdate() {
        return "Last update on: " + DateFormat.getTimeInstance().format(new Date()).toString();
    }

    /**
     * Restoring values from saved instance state
     *
     * @param savedInstanceState instace a Bundle object
     */
    @Override
    public void restoreValuesFromBundle(Bundle savedInstanceState) {
        //updateLocationUI(map,app,markers,lines,mode,txtLocationResult);
    }

    /**
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        return;
    }

    /**
     * @param interval
     */
    @Override
    public void setIntervalOnMillis(long interval) {
        this.UPDATE_INTERVAL_IN_MILLISECONDS = interval;
    }

    /**
     * fastest updates interval - 5 sec
     * location updates will be received if another app is requesting the location
     * than your app can handle
     *
     * @param fastInterval
     */
    @Override
    public void setFastestIntervalOnMillis(long fastInterval) {
        this.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = fastInterval;
    }


    /**
     * Method that draws and save the marker point on the map
     * @param map
     * @param markers
     * @param startPoint
     */
    private void saveMarker(MapView map, ArrayList<Marker> markers, GeoPoint startPoint){
        IMapController mapController = map.getController();
        Marker startMarker = new Marker(map);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
        markers.add(startMarker);
        mapController.setCenter(startPoint);
    }

    /**
     * Method to save the values of the current position on polylines
     * @param map
     * @param lines
     * @param markers
     * @param startPoint
     */
    private void saveLine(MapView map, ArrayList<Polyline> lines, ArrayList<Marker> markers, GeoPoint startPoint){
        List<GeoPoint> geoPoints = new ArrayList<>();
        geoPoints.add(markers.get(markers.size()-2).getPosition());
        geoPoints.add(startPoint);
        Polyline line = new Polyline();
        line.setPoints(geoPoints);
        line.setColor(R.color.colorGreen);
        lines.add(line);
        map.getOverlayManager().add(line);
    }

    /**
     * Method that remove the callbacks to the piksi api
     */
    public void stopHandlerRTK(){
        if(schedulerRTK!=null){
            schedulerRTK.removeCallbacks(runnable);
        }
    }

    public void stopPiksy(){
        piksi.destroy();
        piksi = null;
    }



    /**
     * Method that returns the state of comunication between the rover and the base piksies
     * So it returns an approximation of time required to have a connection of maximum accuracy
     */
    public String connectionStatus(){
        if(piksi==null)
            return "Conexión al 0% tiempo aproximado 15 minutos";
        String type = piksi.getType();
        ArrayList<String> fix_types = new ArrayList<String>(
                Arrays.asList("FIXED","DGPS","float","fixed","DR","SBAS","UNKNOWN","No fix"));
        int state = fix_types.indexOf(type);

        return "La conexión está al "+state/fix_types.size()*100+"%";
    }

}
