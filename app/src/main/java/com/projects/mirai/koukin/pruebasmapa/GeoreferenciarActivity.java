package com.projects.mirai.koukin.pruebasmapa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Position;
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
import com.projects.mirai.koukin.pruebasmapa.HelperClass.CopyOfScaleBarOverlay;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Deg2UTM;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.DistanceCalculator;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.FileUtils;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.GPSsource;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Permissions;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.PiksiSource;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.PositioningSource;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.SavedMap;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.SerialLink;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.projects.mirai.koukin.pruebasmapa.HelperClass.GeoJsonUtils.loadFileGeo;
import static com.projects.mirai.koukin.pruebasmapa.HelperClass.GeoJsonUtils.saveFile;
import static com.projects.mirai.koukin.pruebasmapa.HelperClass.GeoJsonUtils.saveTravel;

/**
 * Reference: https://github.com/googlesamples/android-play-location/tree/master/LocationUpdates
 * Actividad que permite descargar marcar las rutas durante las expediciones y maneja el guardado de
 * puntos, y carga por segundos y distancia
 * @author mauricio, manuel, luis
 * @version 1.0
 */

public class GeoreferenciarActivity extends AppCompatActivity implements MapEventsReceiver{

    private static final String TAG = GeoreferenciarActivity.class.getSimpleName();

    SerialLink piksi;
    Runnable runnable;
    Handler schedulerRTK;
    public Marker markerPersonaRTK;
    public Polygon p;

    @BindView(R.id.location_result)
    TextView txtLocationResult;

    @BindView(R.id.updated_on)
    TextView txtUpdatedOn;

    @BindView(R.id.btn_start_location_updates)
    ImageButton btnStartUpdates;

    @BindView(R.id.map)
    MapView map;

    @BindView(R.id.btn_save)
    ImageButton btn_save;

    @BindView(R.id.btn_open)
    ImageButton btn_open;

    @BindView(R.id.btn_stop)
    ImageButton btn_stop;

    @BindView(R.id.btn_mark)
    ImageButton btn_mark;

    @BindView(R.id.btn_config)
    ImageButton btn_config;

    IMapController mapController;

    // location last updated time
    private String mLastUpdateTimeGPS;

    // location updates interval - 10sec
    private static long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

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
    private Location mCurrentLocationGPS;

    // boolean flag to toggle the ui
    private Boolean mRequestingLocationUpdates = false;

    private int mode = 1;
    //Distancia que se debe dezplazar para hacer otro punto.
    private double distancia = 1;
    //follow_on = true -> actualizar puntos en mapa
    //follow_on = false ->  actualizar puntos en mapas
    private boolean follow_on = false;
    private int gpsMode = 0;
    private GeoPoint lastPoint = null;
    ArrayList<Marker> marcadores;
    ArrayList<Polyline> lineas;

    private String sesionID;
    private SavedMap mapaCargado;
    private int numberOfFoadedFiles=0;

    private PositioningSource source;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Permissions.verifyLocationPermission(this);
        Permissions.verifyStoragePermissions(this);
        marcadores = new ArrayList<>();
        lineas = new ArrayList<>();

        DateFormat df = new SimpleDateFormat("d-MMM-yyyy|HH_mm");
        sesionID = df.format(Calendar.getInstance().getTime());

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_mapa_georeferenciar);
        ButterKnife.bind(this);

        //Recupero los datos guardados.
        sourceLocationSetup(savedInstanceState);
    }

    /**
     *
     * @param savedInstanceState
     */
    private void sourceLocationSetup(Bundle savedInstanceState){

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        //gps = True es para gps y False para RTK
        // gpsMode = 1 -> GPS
        // gpsMode = 0 -> RTK
        if(sharedPref.getBoolean("gps",false)){
            gpsMode = 1;
            init();                                                 // initialize the necessary libraries
            source= new GPSsource();
            source.init(map,this,marcadores,lineas,mode);

            Toast.makeText(this,"MODO GPS ACTIVADO",Toast.LENGTH_SHORT).show();
        }else{
            source = new PiksiSource();
            source.init(map,this,marcadores,lineas,mode);
            Toast.makeText(this,"MODO RTK ACTIVADO",Toast.LENGTH_SHORT).show();
            gpsMode = 0;
        }

        Bundle extras = getIntent().getExtras();                //Si me envian un archivo a cargar , hago esto:
        if (extras != null) {
            String maptoParse = extras.getString("selectedMap");
            mapaCargado = new SavedMap(maptoParse);
        }

        setupMap();
        if(gpsMode == 1){
            restoreValuesFromBundle(savedInstanceState);            // restore the values from saved instance state
        }
    }

    /**
     * Inicializo dependencias y librerias de los servicios de GPS
     */
    private void init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                mCurrentLocationGPS = locationResult.getLastLocation();
                mLastUpdateTimeGPS = DateFormat.getTimeInstance().format(new Date());
                updateLocationUI_GPS();
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
     * Se configura el mapa y todas sus dependencias.
     */
    private void setupMap(){
        map.setTileSource(TileSourceFactory.MAPNIK);
        //map.setBuiltInZoomControls(true);
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        map.getOverlays().add(new CopyOfScaleBarOverlay(this));

        if(gpsMode == 1){                   //Do something if GPS
            MyLocationNewOverlay oMapLocationOverlay = new MyLocationNewOverlay(map);
            map.getOverlays().add(oMapLocationOverlay);
            oMapLocationOverlay.enableFollowLocation();
            oMapLocationOverlay.enableMyLocation();
            oMapLocationOverlay.enableFollowLocation();
        }

        mapController.setZoom(15.0);
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        map.getOverlays().add(0, mapEventsOverlay);
        //Cargo los datos de la BD de mapas

        if(mapaCargado!=null){
            mapController.setCenter(new GeoPoint(mapaCargado.getcLatitude(), mapaCargado.getcLongitude()));
            mapController.setZoom(mapaCargado.getZoomlvl());
            //oMapLocationOverlay.enableMyLocation();
        }

        // Compass
        CompassOverlay compassOverlay = new CompassOverlay(this, map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);
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
                mCurrentLocationGPS = savedInstanceState.getParcelable("last_known_location");
            }

            if (savedInstanceState.containsKey("last_updated_on")) {
                mLastUpdateTimeGPS = savedInstanceState.getString("last_updated_on");
            }
        }
        if(mode == 1){
           updateLocationUI_GPS();
        }
    }


    /**
     * Update the UI displaying the location data and toggling the buttons GPS
     */
    private void updateLocationUI_GPS() {
        // falta revisar donde setea al hombrecito
        if (mCurrentLocationGPS != null) {
            Deg2UTM transform = new Deg2UTM(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude());
            txtLocationResult.setText("Ubicación: " + transform.toString()+" Altura:"+mCurrentLocationGPS.getAltitude());
            if(mode==0){
                GeoPoint startPoint = new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude(), mCurrentLocationGPS.getAltitude());
                Marker startMarker = new Marker(map);
                Drawable mark = ContextCompat.getDrawable(GeoreferenciarActivity.this,R.drawable.usericon);
                startMarker.setIcon(scaleImage(mark,0.8f));
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
                    lineas.add(line);
                    map.getOverlayManager().add(line);
                }
            }else if(mode==1){
                if(marcadores.size()==0){
                    GeoPoint startPoint = new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude(),mCurrentLocationGPS.getAltitude());
                    Marker startMarker = new Marker(map);
                    //startMarker.setIcon(getResources().getDrawable(R.drawable.marker));
                    startMarker.setPosition(startPoint);
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(startMarker);
                    marcadores.add(startMarker);
                    mapController.setCenter(startPoint);
                }else{
                    GeoPoint startPoint = marcadores.get(marcadores.size()-1).getPosition();
                    GeoPoint newPoint = new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude(), mCurrentLocationGPS.getAltitude());
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
                        lineas.add(line);
                        map.getOverlayManager().add(line);
                    }
                }
            }
            // giving a blink animation on TextView
            txtLocationResult.setAlpha(0);
            txtLocationResult.animate().alpha(1).setDuration(300);
            txtUpdatedOn.setText("Última Actualización: " + mLastUpdateTimeGPS);  //// location last updated time
        }
        toggleButtons();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates);
        outState.putParcelable("last_known_location", mCurrentLocationGPS);
        outState.putString("last_updated_on", mLastUpdateTimeGPS);
    }

    /**
     * Metodo que cambia la imagen del boton de guardar registros
     */
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
    private void startLocationUpdatesGPS() {
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

                        updateLocationUI_GPS();
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
                                    rae.startResolutionForResult(GeoreferenciarActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(GeoreferenciarActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                        updateLocationUI_GPS();
                    }
                });
    }

    /**
     * Método que inicia las llamadas a los hilos de los piksis para el modo RTK
     */
    private void startLocationUpdatesRTK() {
        startHandlerRTK();
    }

    @OnClick(R.id.btn_start_location_updates)
    public void startLocationButtonClick() {
        follow_on = !follow_on;

        if(gpsMode == 1){
            Toast.makeText(this,"Estado: actualizando mapa",Toast.LENGTH_SHORT).show();
            mapController.setZoom(18.0);
            if(follow_on){
                Dexter.withActivity(this)
                        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                mRequestingLocationUpdates = true;
                                startLocationUpdatesGPS();
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                if (response.isPermanentlyDenied()) {
                                    // open device settings when the permission is denied permanently
                                    openSettings();
                                }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check();
            }else{
                Toast.makeText(this,"Estado: en modo pausa",Toast.LENGTH_SHORT).show();
                mRequestingLocationUpdates = false;
                stopLocationUpdates();
                btnStartUpdates.setImageResource(R.drawable.playbutton);
            }
        }else{
            if(follow_on){
                mRequestingLocationUpdates = true;
                startLocationUpdatesRTK();
            }else{
                if (piksi != null){
                    if (piksi.isConnected) {
                        Toast.makeText(this, "Estado: en modo pausa", Toast.LENGTH_SHORT).show();
                    }
                } else{
                    Toast.makeText(this, "Piksi no se encuentra conectado", Toast.LENGTH_SHORT).show();
                }
                mRequestingLocationUpdates = false;
                btnStartUpdates.setImageResource(R.drawable.playbutton);
                stopHandlerRTK();
            }
        }
    }

    @OnClick(R.id.btn_config)
    public void changeMode(){
        String[] modes = {"Tiempo", "Distancia"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Elija un tipo de Marcado.");
        builder.setItems(modes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                // the user clicked
                if(which==0){
                    Toast.makeText(GeoreferenciarActivity.this, "Tiempo ha sido elegido", Toast.LENGTH_LONG).show();
                    AlertDialog.Builder alert = new AlertDialog.Builder(GeoreferenciarActivity.this);
                    alert.setTitle("Elija el tiempo a trabajar:");
                    String[] tiempos = {"5 Segundos","15 Segundos","30 Segundos","1 Minuto"};
                    alert.setItems(tiempos, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mode=which;
                            updateMode(i);
                        }
                    });
                    alert.show();
                }else if(which ==1){
                    Toast.makeText(GeoreferenciarActivity.this, "Distancia ha sido elegido", Toast.LENGTH_LONG).show();
                    AlertDialog.Builder alert = new AlertDialog.Builder(GeoreferenciarActivity.this);
                    alert.setTitle("Elija la distancia a trabajar:");
                    String[] distancias = {"1 Metro","2 Metros","5 Metros","10 Metros","15 Metros","30 Metros"};
                    alert.setItems(distancias, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mode=which;
                            updateMode(i);
                        }
                    });
                    alert.show();
                }
            }
        });
        builder.show();
    }

    /**
     * Método que muestra un mensaje cuando las actualizacion de ubicación se dejan de tomar
     */
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
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case RESULT_OK:
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

        if (requestCode == 1234 && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            String path = FileUtils.getPath(this, selectedfile);
            System.out.println("New Path:" + path);

            if (path.endsWith(".json")) {
                loadFileGeo(path,map, this, marcadores, lineas);
            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Debe elegir un archivo GeoJson");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                alert.show();
            }
        }else if (requestCode == 1111 && resultCode == RESULT_OK){
            String result=data.getStringExtra("result");
            System.out.println("Respuesta del fileManager:"+result);
            String path = Environment.getExternalStorageDirectory() + "/MapasArq/"+result;
            System.out.println("New Path:" + path);
            loadFileGeo(path,map, this, marcadores, lineas);
        }
    }


    /**
     * Metodo paa actualizar el modo en el que se toman los datos geográficos, en segundos o por
     * por distancia
     * @param valor el número de la opcion escogida
     */
    public void updateMode(int valor){

        follow_on=!follow_on;
        btn_config.animate().alpha(1).setDuration(600);
        if(mode==0){
            int[] tiempos = {5,15,30,60};
            int[] imagenesTiempo = {R.drawable.seg5,R.drawable.seg15,R.drawable.seg30,R.drawable.min1};
            long segundos=tiempos[valor]*1000;
            UPDATE_INTERVAL_IN_MILLISECONDS=segundos;
            FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS=segundos;
            if(gpsMode==1) {
                mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
                mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
            }else {
                //implementar
            }
            btn_config.setImageResource(imagenesTiempo[valor]);
        }else if(mode ==1){
            int[] distancias = {1,2,5,10,15,30};
            int[] imagenesDistancia = {R.drawable.mts1,R.drawable.mts2,R.drawable.mts5,R.drawable.mts10,R.drawable.mts15,R.drawable.mts30};

            distancia = distancias[valor];
            UPDATE_INTERVAL_IN_MILLISECONDS=5000;
            FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS=5000;
            if(gpsMode==1) {
                mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
                mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
            }else{
                //falta implementar
            }
            btn_config.setImageResource(imagenesDistancia[valor]);
        }
        startLocationButtonClick();
    }



    /**
     * Method to save the route on a geojson file
     */
    @OnClick(R.id.btn_save)
    public void saveGeoPoints(){
        btn_save.animate().alpha(1).setDuration(300);
        if(marcadores.size() > 0){
            btn_save.setEnabled(false);
            saveFile(marcadores,lineas,this,sesionID);
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

    /**
     * STOP Button, prepares everything yo save the points on a file.
     */
    @OnClick(R.id.btn_stop)
    public void stop(){
        btn_stop.animate().alpha(1).setDuration(300);
        if(marcadores.size() > 0){
            btn_save.setEnabled(false);
            btn_stop.setEnabled(false);

            final EditText input = new EditText(GeoreferenciarActivity.this);
            input.setText("Recorrido #1");
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Indique el nombre del Recorrido:");
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    savetravel(input.getText().toString()+"|"+sesionID);
                    if (gpsMode == 0){
                        stopHandlerRTK();
                        stopPiksy();
                    }
                }
            });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    btn_save.setEnabled(true);
                    btn_stop.setEnabled(true);
                }
            });
            alert.show();
        }else{
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("No tiene ningun punto que guardar.");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {}
            });
            alert.show();
        }
    }

    /**
     * Method that saves the final file of the route
     * @param fileName the path of the final file
     * filename es el nombre con el cual se guardara el recorrido.
     */
    public void savetravel(String fileName){
            boolean activity = saveTravel(fileName,marcadores,lineas,this,sesionID);
            if(activity){
                Toast.makeText(getApplicationContext(), "Puntos Guardados", Toast.LENGTH_LONG).show();
                Intent i = new Intent(GeoreferenciarActivity.this,MenuPrincipalActivity.class);
                startActivity(i);
                finish();
            }
            btn_save.setEnabled(true);
    }


    /**
     * Empieza el Handler que controla los tiempos de solicitud del RTK
     */
    public void startHandlerRTK(){
        schedulerRTK = new Handler();
        //delay =UPDATE_INTERVAL_IN_MILLISECONDS; //1 second=1000 milisecond, 15*1000=15seconds
        if(piksi==null){
            piksi = new SerialLink(this.getApplicationContext());
        }
        if(!piksi.isConnected){
            piksi.destroy();
            piksi = new SerialLink(this.getApplicationContext());
        }
        String data;
        if(!piksi.start()) {
            if(!piksi.start()){
                data = "Piksi no se encuentra conectado al dispositivo ";
                System.out.println(data);
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
                mRequestingLocationUpdates = false;
            }
        }else{
            mRequestingLocationUpdates = true;
            Toast.makeText(this,"Estado: actualizando mapa",Toast.LENGTH_SHORT).show();
            toggleButtons();
            schedulerRTK.postDelayed( runnable = new Runnable() {
                public void run() {
                    updateUI_RTK();
                    schedulerRTK.postDelayed(runnable, UPDATE_INTERVAL_IN_MILLISECONDS);
                }
            }, UPDATE_INTERVAL_IN_MILLISECONDS);
        }
    }

    /**
     * Detiene las tareas del handler para evitar que siga realizando peticiones al RTK
     */
    public void stopHandlerRTK(){

            schedulerRTK.removeCallbacks(runnable);


    }

    public void stopPiksy(){
        piksi.destroy();
        piksi = null;
    }

    /**
     * Metodo que actualiza la interfaz de usuario con los datos extraidos del RTK
     */
    public void updateUI_RTK(){
        if(!piksi.isConnected){
            Toast.makeText(this, "Piksi se ha desconectado", Toast.LENGTH_SHORT).show();
            mRequestingLocationUpdates = false;
            follow_on=false;
            btnStartUpdates.setImageResource(R.drawable.playbutton);
            stopHandlerRTK();
        }else{
            //Se solicita la latitud y longitud al RTK
            double lastKnowlatitudeRTK = piksi.getLat();
            double lastKnowLongitudeRTK = piksi.getLon();
            double lastKnowAltitudeRTK = piksi.getLat();
            String type = piksi.type;
            double height = piksi.height;
            txtUpdatedOn.setText(
                    "Última Actualización:"+DateFormat.getTimeInstance().format(new Date())+" |" + piksi.type  + "|" + String.valueOf(piksi.height + "m"));
            if(lastKnowlatitudeRTK > 91 || lastKnowlatitudeRTK < -91){
                return;
            }
            //Se encuentra el punto donde se encuentra actualmente
            GeoPoint startPoint = new GeoPoint(lastKnowlatitudeRTK,lastKnowLongitudeRTK,height);
            //Se actualiza el texto inferior
            Deg2UTM transform = new Deg2UTM(lastKnowlatitudeRTK,lastKnowLongitudeRTK);
            txtLocationResult.setText("Ubicación: " + transform.toString()+" Altura: "+height);

            // giving a blink animation on TextView
            txtLocationResult.setAlpha(0);
            txtLocationResult.animate().alpha(1).setDuration(300);

            if(mode==0){
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
                    line.setColor(R.color.colorGreen);
                    lineas.add(line);
                    map.getOverlayManager().add(line);
                }
            }else if(mode==1){
                if(marcadores.size()==0){
                    Marker startMarker = new Marker(map);
                    //startMarker.setIcon(getResources().getDrawable(R.drawable.marker));
                    startMarker.setPosition(startPoint);
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(startMarker);
                    marcadores.add(startMarker);
                    mapController.setCenter(startPoint);
                }else{
                    startPoint = marcadores.get(marcadores.size()-1).getPosition();
                    GeoPoint newPoint = new GeoPoint(lastKnowlatitudeRTK, lastKnowLongitudeRTK,height);
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
                        lineas.add(line);
                        map.getOverlayManager().add(line);
                    }
                }
            }

            //Se actualiza la markerPersonaRTK y las ubicaciones.
            map.getOverlays().remove(markerPersonaRTK);
            markerPersonaRTK = new Marker(map);
            markerPersonaRTK.setIcon(ContextCompat.getDrawable(GeoreferenciarActivity.this,R.drawable.usericon));
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
        }
    }


    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Metodo que se ejecuta cuando vuelve de pausa la aplicacion.
     */
    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if(mRequestingLocationUpdates){
            if(gpsMode==1){
                // Resuming location updates depending on button state and allowed permissions
                if (checkPermissions()) {
                    startLocationUpdatesGPS();
                }
                updateLocationUI_GPS();
            }else{
                startHandlerRTK();
            }
        }
    }

    /**
     * Metodo que se ejecuta cuando la actividad entra en pausa.
     */
    @Override
    protected void onPause() {
        map.onPause();
        if (mRequestingLocationUpdates) {
            if(gpsMode==1){
                // pausing location updates
                stopLocationUpdates();
            }else{
                stopHandlerRTK();
            }
        }
        super.onPause();
    }

    /**
     * Metodo que revisa los permisos para acceder al gps
     * @return Devuelve un booleano que indica si tiene o no los permisos.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Metodo que se ejecuta al mapa ser presionado una sola vez.
     * @param p -> Es el parametreo que contiene las coordenadas de donde fue presionado.
     * @return
     */
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        System.out.println(map.getZoomLevelDouble());
        return false;
    }

    /**
     * Metodo que se ejecuta al mantener presionado el mapa.
     * @param p -> Es el parametreo que contiene las coordenadas de donde fue presionado.
     * @return
     */
    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    /**
     * Metodo que se ejecuta al presionar el boton de BACK
     */
    @Override
    public void onBackPressed() {
        if(marcadores.size() >0){
            AlertDialog.Builder alert = new AlertDialog.Builder(GeoreferenciarActivity.this);
            alert.setTitle("Seguro que desea Salir?");
            alert.setMessage("Recomiendo que presione stop para guardar el recorrido finalizado.");

            alert.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if(piksi!=null){
                        stopPiksy();
                    }
                    GeoreferenciarActivity.super.onBackPressed();
                }
            });
            alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();
        }else{
            if(piksi!=null){
                stopPiksy();
            }
            super.onBackPressed();
        }
    }

    //Metodo viejo para telefonos con Android 6.
    //@OnClick(R.id.btn_open)

    /**
     *  Metodo antiguo para dispositivos con Android 6
     */
    public void openFile(){
        Permissions.verifyLocationPermission(GeoreferenciarActivity.this);
        Permissions.verifyStoragePermissions(GeoreferenciarActivity.this);
        if(numberOfFoadedFiles<5){
            Intent intent = new Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Elige un Archivo"), 1234);
        }else{
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("No puede abrir más archivos.");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();
        }
    }

    @OnClick(R.id.btn_open)
    public void openFile2(){
        Permissions.verifyLocationPermission(GeoreferenciarActivity.this);
        Permissions.verifyStoragePermissions(GeoreferenciarActivity.this);
        if(numberOfFoadedFiles<5){
            Intent i = new Intent(this, OpenFileActivity.class);
            startActivityForResult(i, 1111);
        }else{
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("No puede abrir mas archivos.");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();
        }
    }

    public Drawable scaleImage (Drawable image, float scaleFactor) {

        if ((image == null) || !(image instanceof BitmapDrawable)) {
            return image;
        }

        Bitmap b = ((BitmapDrawable)image).getBitmap();

        int sizeX = Math.round(image.getIntrinsicWidth() * scaleFactor);
        int sizeY = Math.round(image.getIntrinsicHeight() * scaleFactor);

        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, sizeX, sizeY, false);

        image = new BitmapDrawable(getResources(), bitmapResized);

        return image;

    }

    /**
     * Metodo que sirve para marcar el punto exacto donde esta parado, informando que encontro.
     */
    @OnClick(R.id.btn_mark)
    public void setMark(){
        if(mRequestingLocationUpdates){
            LinearLayout view = (LinearLayout) View.inflate(this,R.layout.spinner_list,null);
            final Spinner staticSpinner = (Spinner) view.findViewById(R.id.spinner) ;
            final EditText numeroProcedencia = (EditText) view.findViewById(R.id.input_numeroP);
            final LinearLayout others = (LinearLayout) view.findViewById(R.id.layoutOthers);
            final EditText others_name = (EditText) view.findViewById(R.id.other_hito);
            final String hito = "";
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Indique que encontró:");
            alert.setView(view);
            staticSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if(staticSpinner.getSelectedItem().equals("Otros")){
                        others.setVisibility(VISIBLE);
                    }else{
                        others.setVisibility(GONE);
                    }
                }
                public void onNothingSelected(AdapterView<?> adapterView) {
                    return;
                }
            });
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    GeoPoint punto;
                    if (gpsMode == 1) {
                        punto = new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude(), mCurrentLocationGPS.getAltitude());
                    } else {
                        if (piksi != null) {

                            punto = new GeoPoint(markerPersonaRTK.getPosition().getLatitude(), markerPersonaRTK.getPosition().getLongitude());
                        } else {
                            Toast.makeText(GeoreferenciarActivity.this, "Piksi no se encuentra conectado.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    Marker marker = new Marker(map);

                    //On this part you check wether is an stablished discoveriment or another category
                    if(staticSpinner.getSelectedItem().toString().equals("Otros")){
                        marker.setTitle(others_name.getText().toString());
                    }else{
                        marker.setTitle(staticSpinner.getSelectedItem().toString());
                    }

                    marker.setPosition(punto);
                    marker.setSubDescription(numeroProcedencia.getText().toString());
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    Drawable mark = ContextCompat.getDrawable(GeoreferenciarActivity.this, R.drawable.marker);
                    //mark.setBounds(0, 0, 30, 30);
                    marker.setIcon(scaleImage(mark,1.5f));
                    map.getOverlays().add(marker);
                    marcadores.add(marker);
                }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //Put actions for CANCEL button here, or leave in blank
                }
            });
            alert.show();
        }else{
            // porqué?
            Toast.makeText(this,"Necesita estar en play.",Toast.LENGTH_LONG).show();
        }
    }
}