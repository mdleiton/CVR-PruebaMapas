package com.projects.mirai.koukin.pruebasmapa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
//import android.widget.Button;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.Point;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.DistanceCalculator;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Permissions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.modules.SqliteArchiveTileWriter;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
//import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import java.util.Calendar;
import java.util.List;

/**
 * Reference: https://github.com/googlesamples/android-play-location/tree/master/LocationUpdates
 * Actividad que permite descargar los tiles de una zona geográfica rodeada por medio de un BBOx
 * llevándote de vuelta al menú
 * @author mauricio, manuel, luis
 * @version 1.0
 */

public class MapaActivity extends AppCompatActivity implements MapEventsReceiver,SeekBar.OnSeekBarChangeListener, View.OnClickListener, TextWatcher {

    private static final String TAG = MapaActivity.class.getSimpleName();


    @BindView(R.id.location_result)
    TextView txtLocationResult;

    @BindView(R.id.updated_on)
    TextView txtUpdatedOn;

    @BindView(R.id.btn_start_location_updates)
    ImageButton btnStartUpdates;
    /*
    @BindView(R.id.btn_stop_location_updates)
    Button btnStopUpdates;
    */
    @BindView(R.id.map)
    MapView map;

    @BindView(R.id.btn_save)
    ImageButton btn_save;

    IMapController mapController;

    // location last updated time
    private String mLastUpdateTime;

    // location updates interval - 10sec
    private static  long UPDATE_INTERVAL_IN_MILLISECONDS = 15000;

    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    private static long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 15000;

    private static final int REQUEST_CHECK_SETTINGS = 100;

    // bunch of location related apis
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;

    // boolean flag to toggle the ui
    private Boolean mRequestingLocationUpdates;


    private Point lastKnowLocation;

    //Valida en que si es -1 no ha eligido ningun modulo.
    private int mode = -1;
    private double distancia = 10;
    private GeoPoint lastPoint = null;



    private boolean follow_on = false;

    ArrayList<Marker> marcadores;
    ArrayList<Polyline> lineas;

    SqliteArchiveTileWriter writer=null;
    AlertDialog downloadPrompt=null;
    private String sesionID;

    Button btnCache,executeJob;
    SeekBar zoom_min;
    SeekBar zoom_max;
    EditText cache_north, cache_south, cache_east,cache_west, cache_output;
    TextView cache_estimate;
    CacheManager mgr=null;
    AlertDialog alertDialog=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Permissions.verifyLocationPermission(this);
        Permissions.verifyStoragePermissions(this);
        marcadores = new ArrayList<>();
        lineas = new ArrayList<>();


        DateFormat df = new SimpleDateFormat("d-MMM-yyyy|HH_mm");
        String date = df.format(Calendar.getInstance().getTime());
        sesionID = "GeoJson"+date ;


        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        setContentView(R.layout.activity_mapa);
        ButterKnife.bind(this);

        // initialize the necessary libraries
        init();
        // restore the values from saved instance state
        restoreValuesFromBundle(savedInstanceState);

        setupMap();

        Bundle extras = getIntent().getExtras();
        //Si me envian un archivo a cargar , hago esto:
        if (extras != null) {
            String selectedFile = extras.getString("selectedFile");
            loadFile(selectedFile);
        }

    }


    /**
     * Método que crea el mapa
     */
    private void setupMap(){

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(5.0);

        MyLocationNewOverlay oMapLocationOverlay = new MyLocationNewOverlay(map);
        map.getOverlays().add(oMapLocationOverlay);

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        map.getOverlays().add(0, mapEventsOverlay);

        oMapLocationOverlay.enableFollowLocation();
        oMapLocationOverlay.enableMyLocation();
        oMapLocationOverlay.enableFollowLocation();

        mapController.setZoom(15.0);


        // Compass
        /*CompassOverlay compassOverlay = new CompassOverlay(this, map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);*/

    }






    private void init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                updateLocationUI();
            }
        };

        mRequestingLocationUpdates = false;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Restoring values from saved instance state
     */
    private void restoreValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("is_requesting_updates")) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates");
            }

            if (savedInstanceState.containsKey("last_known_location")) {
                mCurrentLocation = savedInstanceState.getParcelable("last_known_location");
            }

            if (savedInstanceState.containsKey("last_updated_on")) {
                mLastUpdateTime = savedInstanceState.getString("last_updated_on");
            }
        }

        updateLocationUI();
    }


    /**
     * Update the UI displaying the location data
     * and toggling the buttons
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null) {
            txtLocationResult.setText(
                    "Lat: " + mCurrentLocation.getLatitude() + ", " +
                            "Lng: " + mCurrentLocation.getLongitude()
            );
            if(mode==0){
                GeoPoint startPoint = new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

                Marker startMarker = new Marker(map);
                //startMarker.setIcon(getResources().getDrawable(R.drawable.marker));
                startMarker.setPosition(startPoint);
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                map.getOverlays().add(startMarker);
                marcadores.add(startMarker);
                mapController.setCenter(startPoint);
                if(marcadores.size()>1){
                    List <GeoPoint> geoPoints = new ArrayList<>();
                    geoPoints.add(marcadores.get(marcadores.size()-2).getPosition());
                    geoPoints.add(startPoint);
                    Polyline line = new Polyline();
                    line.setPoints(geoPoints);
                    map.getOverlayManager().add(line);

                }

            }else if(mode==1){
                if(marcadores.size()==0){
                    GeoPoint startPoint = new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                    Marker startMarker = new Marker(map);
                    //startMarker.setIcon(getResources().getDrawable(R.drawable.marker));
                    startMarker.setPosition(startPoint);
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(startMarker);
                    marcadores.add(startMarker);
                    mapController.setCenter(startPoint);
                }else{
                    GeoPoint startPoint = marcadores.get(marcadores.size()-1).getPosition();
                    GeoPoint newPoint = new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                    double distance = DistanceCalculator.calculateDistanceInMeters(startPoint,newPoint);
                    if(distance >= distancia){
                        Marker startMarker = new Marker(map);
                        startMarker.setPosition(newPoint);
                        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        map.getOverlays().add(startMarker);
                        marcadores.add(startMarker);
                        mapController.setCenter(newPoint);
                        List <GeoPoint> geoPoints = new ArrayList<>();
                        geoPoints.add(startPoint);
                        geoPoints.add(newPoint);
                        Polyline line = new Polyline();
                        line.setPoints(geoPoints);
                        map.getOverlayManager().add(line);
                    }
                }
            }else if(mode ==2){

            }
            //mapController.setZoom(18.0);
            // giving a blink animation on TextView
            txtLocationResult.setAlpha(0);
            txtLocationResult.animate().alpha(1).setDuration(300);

            // location last updated time
            txtUpdatedOn.setText("Ultima Actualizacion: " + mLastUpdateTime);
        }

        toggleButtons();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates);
        outState.putParcelable("last_known_location", mCurrentLocation);
        outState.putString("last_updated_on", mLastUpdateTime);

    }

    private void toggleButtons() {
        if (mRequestingLocationUpdates) {
            btnStartUpdates.setImageResource(R.drawable.pause);
        }
    }

    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */
    private void startLocationUpdates() {
        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        Toast.makeText(getApplicationContext(), "¡Actualizaciones de ubicación iniciadas!", Toast.LENGTH_SHORT).show();

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateLocationUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MapaActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(MapaActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

                        updateLocationUI();
                    }
                });
    }

    @OnClick(R.id.btn_start_location_updates)
    public void startLocationButtonClick() {
        // Requesting ACCESS_FINE_LOCATION using Dexter library
        if(mode != -1){
            follow_on = !follow_on;
            mapController.setZoom(18.0);
            if(follow_on){
                Dexter.withActivity(this)
                        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                mRequestingLocationUpdates = true;
                                startLocationUpdates();
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                if (response.isPermanentlyDenied()) {
                                    // open device settings when the permission is
                                    // denied permanently
                                    openSettings();
                                }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check();
            }else{
                mRequestingLocationUpdates = false;
                stopLocationUpdates();
                btnStartUpdates.setImageResource(R.drawable.playbutton);
            }
        }else{
            changeMode();

        }

    }


    @OnClick(R.id.btn_config)
    public void changeMode(){
        String[] colors = {"Tiempo", "Distancia", "Manual"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Elija un tipo de Marcado.");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                // the user clicked
                if(which==0){
                    Toast.makeText(MapaActivity.this, "Tiempo ha sido elegido", Toast.LENGTH_LONG).show();
                    AlertDialog.Builder alert = new AlertDialog.Builder(MapaActivity.this);
                    alert.setTitle("Ingrese el tiempo en segundos:");
                    final EditText input = new EditText(MapaActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    //input.setRawInputType(Configuration.KEYBOARD_12KEY);
                    alert.setView(input);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            long segundos =Integer.parseInt(input.getText().toString())*1000;
                            UPDATE_INTERVAL_IN_MILLISECONDS=segundos;
                            FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS=segundos;
                            mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
                            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
                            mode=0;
                            startLocationButtonClick();
                        }
                    });
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //Put actions for CANCEL button here, or leave in blank
                            mode = -1;
                        }
                    });
                    alert.show();
                }else if(which ==1){

                    Toast.makeText(MapaActivity.this, "Distancia ha sido elegido", Toast.LENGTH_LONG).show();
                    AlertDialog.Builder alert = new AlertDialog.Builder(MapaActivity.this);
                    alert.setTitle("Ingrese la distancia en metros:");
                    final EditText input = new EditText(MapaActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    alert.setView(input);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            distancia = Double.parseDouble(input.getText().toString());
                            UPDATE_INTERVAL_IN_MILLISECONDS=5000;
                            FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS=5000;
                            mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
                            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
                            mode=which;

                            startLocationButtonClick();
                        }
                    });
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //Put actions for CANCEL button here, or leave in blank
                            mode = -1;
                        }
                    });
                    alert.show();

                }else{
                    Toast.makeText(MapaActivity.this, "Manual ha sido elegido", Toast.LENGTH_LONG).show();
                    mode = 2;
                    startLocationButtonClick();
                }

            }
        });
        builder.show();
    }


    public void stopLocationUpdates() {
        // Removing location updates
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(getApplicationContext(), "¡Actualizaciones de ubicación detenidas!", Toast.LENGTH_SHORT).show();
                        toggleButtons();
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.e(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.e(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    @OnClick(R.id.btn_save)
    public void saveGeoPoints(){
        btn_save.animate().alpha(1).setDuration(300);
        if(marcadores.size() > 0){
            btn_save.setEnabled(false);
            // Create geometry

            // Create feature with geometry
            FeatureCollection features = new FeatureCollection();

            for (Marker marker:marcadores){
                GeoPoint punto = marker.getPosition();
                Point point = new Point(punto.getLatitude(),punto.getLongitude());
                features.addFeature(new Feature(point));
            }
            /*
            for (Polyline linea :lineas){
                LineString lineString = new LineString(new JSONArray());

                features.addFeature(new Feature(lineString));
            }*/

            try{
                JSONObject geoJSON = features.toJSON();
                Permissions.verifyStoragePermissions(this);


                String path;
                if(sesionID.endsWith(".json")){
                    path = Environment.getExternalStorageDirectory() + File.separator + sesionID;
                }else{
                    path = Environment.getExternalStorageDirectory() + File.separator + sesionID +".json";
                }

                //String path = "samplefile1.json";

                System.out.println(path);

                File file = new File(path);
                file.createNewFile();

                if(file.exists()){
                    OutputStream fOut = new FileOutputStream(file);
                    //OutputStreamWriter osw = new OutputStreamWriter(fOut);
                    //osw.write(geoJSON.toString());
                    fOut.write(geoJSON.toString().getBytes());
                    System.out.println(geoJSON.toString());
                    //osw.flush();
                    //osw.close();
                    Toast.makeText(getApplicationContext(), "Puntos Guardados", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), "No se pudo crear el archivo", Toast.LENGTH_LONG).show();
                }


                Toast.makeText(getApplicationContext(), "Puntos Guardados", Toast.LENGTH_LONG).show();
            }catch(Exception e){
                System.out.println(e);
                Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
            btn_save.setEnabled(true);




        }else{
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("No tiene ningun punto que guardar.");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();
        }

    }
    private void loadFile(String selectedFile){

        String textJson = getStringFromFile(selectedFile);
        GeoJSONObject geoJSON;
        try{
            geoJSON = GeoJSON.parse(textJson);
            JSONObject Json = geoJSON.toJSON();
            System.out.println("Texto Leido:"+selectedFile);
            System.out.println("Probando:"+Json.getJSONArray("features"));
            JSONArray features = Json.getJSONArray("features");
            for(int i=0;i< features.length();i++){
                JSONObject elemento = features.getJSONObject(i).getJSONObject("geometry");
                System.out.println("Elemento"+i+":"+elemento);

                if(elemento.get("type").equals("Point")){
                    JSONArray coordenadas = elemento.getJSONArray("coordinates");
                    Marker startMarker = new Marker(map);
                    //startMarker.setIcon(getResources().getDrawable(R.drawable.marker));
                    startMarker.setPosition(new GeoPoint(coordenadas.getDouble(1),coordenadas.getDouble(0)));
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(startMarker);
                    marcadores.add(startMarker);
                }

            }
            String elementos[] = selectedFile.split("/");
            sesionID = elementos[elementos.length-1];

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Archivo "+sesionID+" Cargado.");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();

            map.invalidate();
        }catch(JSONException e){
            Toast.makeText(getBaseContext(), "No se pudo Parsear el json", Toast.LENGTH_LONG).show();
            System.out.println(e.toString());
        }catch(Exception ex){
            Toast.makeText(getBaseContext(), "Existe un problema con su json", Toast.LENGTH_LONG).show();
            System.out.println(ex.toString());
        }
    }
    public String getStringFromFile(String selectedFile){

        System.out.println("Path:"+selectedFile);
        File file = new File(selectedFile);
        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            System.out.println("Error Lectura:"+e.toString());
        }
        return text.toString();
    }


    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package",
                BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        // Resuming location updates depending on button state and
        // allowed permissions
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        }

        updateLocationUI();
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        if (mRequestingLocationUpdates) {
            // pausing location updates
            stopLocationUpdates();
            saveGeoPoints();
        }
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        System.out.println(map.getZoomLevelDouble());
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        if(mode == 2){
            Marker startMarker = new Marker(map);
            startMarker.setPosition(p);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(startMarker);
            marcadores.add(startMarker);
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if(marcadores.size() >0){
            AlertDialog.Builder alert = new AlertDialog.Builder(MapaActivity.this);
            alert.setTitle("Seguro que desea Salir?");

            alert.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    MapaActivity.super.onBackPressed();
                }
            });
            alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    MapaActivity.super.onBackPressed();

                }
            });
            alert.show();
        }else{
            super.onBackPressed();
        }

    }


    //DESCARGA DE MAPA
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.executeJob:
                updateEstimate(true);
                System.out.println("Zoom Max solicitado: "+zoom_max.getProgress());
                System.out.println("Zoom Min solicitado: "+zoom_min.getProgress());
                break;

        }
    }


    private void updateEstimate(boolean startJob) {
        try {
            if (cache_east != null &&
                    cache_west != null &&
                    cache_north != null &&
                    cache_south != null &&
                    zoom_max != null &&
                    zoom_min != null &&
                    cache_output!=null) {
                double n = Double.parseDouble(cache_north.getText().toString());
                double s = Double.parseDouble(cache_south.getText().toString());
                double e = Double.parseDouble(cache_east.getText().toString());
                double w = Double.parseDouble(cache_west.getText().toString());
                if (startJob) {
                    String outputName = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "osmdroid" + File.separator + cache_output.getText().toString();
                    writer=new SqliteArchiveTileWriter(outputName);
                    mgr = new CacheManager(map, writer);
                } else {
                    if (mgr==null)
                        mgr = new CacheManager(map);
                }
                int zoommin = zoom_min.getProgress();
                int zoommax = zoom_max.getProgress();
                //nesw
                BoundingBox bb= new BoundingBox(n, e, s, w);
                int tilecount = mgr.possibleTilesInArea(bb, zoommin, zoommax);
                cache_estimate.setText(tilecount + " Cuadrillas");
                if (startJob)
                {
                    if ( downloadPrompt!=null) {
                        downloadPrompt.dismiss();
                        downloadPrompt=null;
                    }

                    //this triggers the download
                    mgr.downloadAreaAsync(MapaActivity.this, bb, zoommin, zoommax, new CacheManager.CacheManagerCallback() {
                        @Override
                        public void onTaskComplete() {
                            Toast.makeText(MapaActivity.this, "Descarga Completa!", Toast.LENGTH_LONG).show();
                            if (writer!=null)
                                writer.onDetach();
                        }

                        @Override
                        public void onTaskFailed(int errors) {
                            Toast.makeText(MapaActivity.this, "Descarga completa con " + errors + " errores", Toast.LENGTH_LONG).show();
                            if (writer!=null)
                                writer.onDetach();
                        }

                        @Override
                        public void updateProgress(int progress, int currentZoomLevel, int zoomMin, int zoomMax) {
                            //NOOP since we are using the build in UI
                        }

                        @Override
                        public void downloadStarted() {
                            //NOOP since we are using the build in UI
                        }

                        @Override
                        public void setPossibleTilesInArea(int total) {
                            //NOOP since we are using the build in UI
                        }
                    });
                }

            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }


    @OnClick(R.id.btn_dowload)
    public void downloadJobAlert() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MapaActivity.this);

        View view = View.inflate(MapaActivity.this, R.layout.sample_cachemgr_input, null);
        view.findViewById(R.id.cache_archival_section).setVisibility(View.VISIBLE);

        BoundingBox boundingBox = map.getBoundingBox();
        zoom_max=(SeekBar) view.findViewById(R.id.slider_zoom_max);
        zoom_max.setMax((int) map.getMaxZoomLevel());
        zoom_max.setOnSeekBarChangeListener(MapaActivity.this);

        zoom_max.setProgress(20);

        zoom_min=(SeekBar) view.findViewById(R.id.slider_zoom_min);
        zoom_min.setMax((int) map.getMaxZoomLevel());
        zoom_min.setProgress((int) map.getMinZoomLevel());

        zoom_min.setProgress(15);

        zoom_min.setOnSeekBarChangeListener(MapaActivity.this);
        cache_east= (EditText) view.findViewById(R.id.cache_east);
        cache_east.setText(boundingBox.getLonEast() +"");
        cache_north= (EditText) view.findViewById(R.id.cache_north);
        cache_north.setText(boundingBox.getLatNorth()  +"");
        cache_south= (EditText) view.findViewById(R.id.cache_south);
        cache_south.setText(boundingBox.getLatSouth()  +"");
        cache_west= (EditText) view.findViewById(R.id.cache_west);
        cache_west.setText(boundingBox.getLonWest()  +"");
        cache_estimate = (TextView) view.findViewById(R.id.cache_estimate);
        cache_output=(EditText) view.findViewById(R.id.cache_output);

        //change listeners for both validation and to trigger the download estimation
        cache_east.addTextChangedListener((TextWatcher) this);
        cache_north.addTextChangedListener((TextWatcher) this);
        cache_south.addTextChangedListener((TextWatcher) this);
        cache_west.addTextChangedListener((TextWatcher) this);
        executeJob= (Button) view.findViewById(R.id.executeJob);
        executeJob.setOnClickListener(MapaActivity.this);
        builder.setView(view);
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cache_east=null;
                cache_south=null;
                cache_estimate=null;
                cache_north=null;
                cache_west=null;
                executeJob=null;
                zoom_min=null;
                zoom_max=null;
                cache_output=null;
            }
        });
        downloadPrompt=builder.create();
        downloadPrompt.show();



    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateEstimate(false);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        updateEstimate(false);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

}