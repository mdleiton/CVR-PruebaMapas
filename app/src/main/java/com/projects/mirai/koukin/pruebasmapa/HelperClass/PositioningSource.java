package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

/**
 * Interface to implement if is needed a location data source
 * @author manuel
 * @version 1.0
 */
public abstract class PositioningSource {

    /**
     * The distance by which the position markers are taken
     */
    protected double distance = 1;

    /**
     *  The mode by which the current position is taken by position = 1 and by time = 0
     */
    protected int mode = 1;

    /**
     * Location updates interval
     */
    protected static long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * fastest updates interval - 5 sec
     * location updates will be received if another app is requesting the locations
     * than your app can handle
      */
    protected static long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 15000;

    protected static final int REQUEST_CHECK_SETTINGS = 100;

    /**
     * variable that states the reference system is is flase the reference system is utm, else is
     * a self reference system that takes the north and the east to position itself from a zero point
     */
    protected boolean self_reference = false;

    /**
     * The zero point location when using the self reference system
     */
    protected GeoPoint base;

    /**
     *
     */
    protected MapView map;

    /**
     *
     */
    protected AppCompatActivity app;

    /**
     *
     */
    protected ArrayList<Marker> markers;

    /**
     *
     */
    protected ArrayList<Polyline> lines;

    /**
     * Constructor
     */
    public PositioningSource(){}

    /**
     * Constructor that uses the more attributes that are defined on the activity that need the
     * source
     * @param map MapView, where it sets the position points
     * @param app the context
     * @param markers a list of markers where to store the geopoints
     * @param lines a list lines to save the route
     * @param mode the mode by which there are obteined the location points
     */
    public PositioningSource(MapView map, AppCompatActivity app, ArrayList<Marker> markers, ArrayList<Polyline> lines, int mode){
        this.map = map;
        this.app = app;
        this.markers = markers;
        this.lines = lines;
        this.mode = mode;
    }


    /**
     * Method that inits all the required parameters or permissions necessary to start the
     * callbacks methods to get location updates
     */
    public abstract void init(MapView map, AppCompatActivity app, ArrayList<Marker> markers, ArrayList<Polyline> lines, int mode);

    /**
     * Method that prepares everything to start to receive location updates
     */
    public abstract void startLocationUpdates(final MapView map, final AppCompatActivity app, final ArrayList<Marker> markers, final ArrayList<Polyline> lines, final int mode);

    /**
     * Method that gets the LocationUpdates and save them like markers on the map
     */
    public abstract String updateLocationUI(MapView map, AppCompatActivity app, ArrayList<Marker> markers, ArrayList<Polyline> lines, int mode);

    /**
     * Method that stops and prepares everything for stopping the location updates according to the
     * data source, current version only covers GPS and RTK modes
     */
    public abstract void stopLocationUpdates(final MapView map, final AppCompatActivity app, final ArrayList<Marker> markers, final ArrayList<Polyline> lines, final int mode);


    /**
     * Method that returns a string representation of the actual position
     * @param utm a Deg2UTM object, representation of utm coordinates
     * @param altitude the actual altitude
     * @return a string representation of the actual position
     */
    public abstract String transformtoString(Deg2UTM utm, double altitude);


    /**
     * Method that returns the string representation for the last received update
     * @return
     */
    public abstract String lastKnownUpdate();

    /**
     * Restoring values from saved instance state
     * @param savedInstanceState instace a Bundle object
     */
    public abstract void restoreValuesFromBundle(Bundle savedInstanceState);

    /**
     * Method to change the distance parameter
     * @param new_distance
     */
    public void changeDistance(double new_distance){
        distance = new_distance;
    }

    /**
     * Method to change the mode parameter
     * @param new_mode
     */
    public void changeMode(int new_mode){
        mode = new_mode;
    }

    /**
     * Method used for reescaling the markers on the mapView
     * @param image the drawable of the marker it could also be a map of bits
     * @param scaleFactor the factor of the size by which it multiplies
     * @param app the context
     * @return a drawable of the same image of the marker but with a new size
     */
    public Drawable scaleImage (Drawable image, float scaleFactor, AppCompatActivity app) {

        if (image == null) return image;
        else if (!(image instanceof BitmapDrawable)) {
            return image;
        }

        Bitmap b = ((BitmapDrawable)image).getBitmap();

        int sizeX = Math.round(image.getIntrinsicWidth() * scaleFactor);
        int sizeY = Math.round(image.getIntrinsicHeight() * scaleFactor);

        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, sizeX, sizeY, false);

        image = new BitmapDrawable(app.getResources(), bitmapResized);

        return image;
    }

    /**
     * Method that returns an string representation of the distance from a system of reference
     * @param station
     * @param rover
     * @return
     */
    public String selfReferenceString(Marker station, Marker rover){
        GeoPoint roverPos = rover.getPosition();
        GeoPoint stationPos = station.getPosition();
        double east = DistanceCalculator.calculateDistanceFromEast(roverPos,stationPos);
        double north = DistanceCalculator.calculateDistanceFromNorth(roverPos,stationPos);
        return "Moving to "+ String.valueOf(north)+" m North and "+String.valueOf(east)+" m East";
    }

    /**
     * Method that returns an string representation of the distance from a system of reference
     * @param station
     * @param rover
     * @return
     */
    protected String selfReferenceString(GeoPoint station, GeoPoint rover){
        double east = DistanceCalculator.calculateDistanceFromEast(rover,station);
        double north = DistanceCalculator.calculateDistanceFromNorth(rover,station);
        return "Moving to "+ String.valueOf(north)+" m North and "+String.valueOf(east)+" m East";
    }

    /**
     *
     * @param base
     * @param self_reference
     */
    protected void setSelf_reference(GeoPoint base, boolean self_reference){
        this.base = base;
        this.self_reference = self_reference;
    }

    /**
     *
     * @param mode
     * @return
     */
    protected boolean setMode(int mode){
        this.mode = mode;
        return true;
    }

    /**
     *
     * @param distance
     * @return
     */
    protected boolean setDistance(int distance){
        this.distance = distance;
        return true;
    }
}
