package com.projects.mirai.koukin.pruebasmapa;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.CopyOfScaleBarOverlay;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Deg2UTM;
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
import org.osmdroid.views.overlay.infowindow.InfoWindow;
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

    @BindView(R.id.location_result)
    TextView txtLocationResult;

    @BindView(R.id.updated_on)
    TextView txtUpdatedOn;

    @BindView(R.id.textHitoMapa)
    TextView textHitoMapa;

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

    @BindView(R.id.locationMove)
    ImageButton locationMove;

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

    // boolean flag to toggle the ui
    private Boolean mRequestingLocationUpdates = false;

    /**
     * Mode for position requests mode->1 == by distance | mode->0 == by time
     */
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

    private double referenceNorth;
    private double referenceEast;

    private boolean own_reference = false;
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

        txtLocationResult.setTextSize(20);
        if(sharedPref.getBoolean("gps",false)){
            gpsMode = 1;
            // initialize the necessary libraries
            source= new GPSsource();
            source.setUI(txtLocationResult,txtUpdatedOn);
            source.init(map,this,marcadores,lineas,mode);
            Toast.makeText(this,"MODO GPS ACTIVADO",Toast.LENGTH_SHORT).show();
        }else{
            source = new PiksiSource();
            source.setUI(txtLocationResult,txtUpdatedOn);
            source.init(map,this,marcadores,lineas,mode);
            Toast.makeText(this,"MODO RTK ACTIVADO",Toast.LENGTH_SHORT).show();
            gpsMode = 0;
        }
        if(sharedPref.getBoolean("notutm",true)){
            own_reference = true;
            float longitude = sharedPref.getFloat("long",180);
            float latitude = sharedPref.getFloat("lat",90);
            referenceNorth = sharedPref.getFloat("norte",1000);
            referenceEast = sharedPref.getFloat("este",1000);
            GeoPoint pos = new GeoPoint(longitude,latitude);
            source.setSelf_reference(pos,true);
        }

        Bundle extras = getIntent().getExtras();                //Si me envian un archivo a cargar , hago esto:
        if (extras != null) {
            String maptoParse = extras.getString("selectedMap");
            mapaCargado = new SavedMap(maptoParse);
        }

        setupMap();
        restoreValuesFromBundle(savedInstanceState);            // restore the values from saved instance state
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
     * Function that positions the map on the position of the actual location of the user
     * @param
     */
    @OnClick(R.id.locationMove)
    public void actualPosition(){
        GeoPoint position = new GeoPoint(source.returnLatitude(),source.returnLongitude());
        mapController.setCenter(position);
        mapController.setZoom(15.0);
    }

    /**
     * Restoring values from saved instance state
     */
    private void restoreValuesFromBundle(Bundle savedInstanceState) {
        source.restoreValuesFromBundle(savedInstanceState);
        if(mode == 1){
           updateLocation();
        }
    }

    /**
     *
     */
    private void updateLocation(){
        String location = source.updateLocationUI(map,this,marcadores,lineas,mode,txtLocationResult);
        txtLocationResult.setAlpha(0);
        txtLocationResult.animate().alpha(1).setDuration(300);
        txtUpdatedOn.setText("Última Actualización: " + DateFormat.getTimeInstance().format(new Date()));
        if(manageReferenceSystem()){
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Está saliendo del área del sistema de referencia. ¿Desea Continuar?");
            alert.setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {}
            });
            alert.show();
        }
        toggleButtons();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        source.onSaveInstanceState(outState);
    }

    /**
     * Metodo que cambia la imagen del boton de guardar registros
     */
    private void toggleButtons() {
        if (source.getmRequestingLocationUpdates()) {
            btnStartUpdates.setImageResource(R.drawable.pause);
        }
    }


    @OnClick(R.id.btn_start_location_updates)
    public void startLocationButtonClick(){
        source.setFollow_on(!source.getFollow_on());
        follow_on = !follow_on;
        Toast.makeText(this,"Estado: actualizando mapa",Toast.LENGTH_SHORT).show();
        mapController.setZoom(18.0);
        if(source.getFollow_on()){
            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            source.setmRequestingLocation(true);
                            source.startLocationUpdates(map,GeoreferenciarActivity.this,marcadores,lineas,mode);
                            updateLocation();
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
            source.setmRequestingLocation(false);
            source.stopLocationUpdates(map,this,marcadores,lineas,mode);
            btnStartUpdates.setImageResource(R.drawable.playbutton);
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

        source.setFollow_on(!source.getFollow_on());
        btn_config.animate().alpha(1).setDuration(600);
        if(mode==0){
            int[] tiempos = {5,15,30,60};
            int[] imagenesTiempo = {R.drawable.seg5,R.drawable.seg15,R.drawable.seg30,R.drawable.min1};
            long segundos=tiempos[valor]*1000;
            UPDATE_INTERVAL_IN_MILLISECONDS=segundos;
            FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS=segundos;
            if(gpsMode==1) {
                GPSsource gps = (GPSsource) source;
                gps.getmLocationRequest().setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
                gps.getmLocationRequest().setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
            }
            btn_config.setImageResource(imagenesTiempo[valor]);
        }else if(mode ==1){
            int[] distancias = {1,2,5,10,15,30};
            int[] imagenesDistancia = {R.drawable.mts1,R.drawable.mts2,R.drawable.mts5,R.drawable.mts10,R.drawable.mts15,R.drawable.mts30};

            distancia = distancias[valor];
            UPDATE_INTERVAL_IN_MILLISECONDS=5000;
            FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS=5000;
            if(gpsMode==1) {
                GPSsource gps = (GPSsource) source;
                gps.getmLocationRequest().setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
                gps.getmLocationRequest().setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
            }
            btn_config.setImageResource(imagenesDistancia[valor]);
        }
        startLocationButtonClick();
    }


    private boolean manageReferenceSystem(){
        if(own_reference){
            boolean is_source = source.insideBoundaries(new GeoPoint(source.returnLatitude(),source.returnLongitude(),source.returnHeight()),referenceNorth,referenceEast);
            return is_source;
        }
        return false;
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
                        source.stopLocationUpdates(map,GeoreferenciarActivity.this,marcadores,lineas,mode);
                        PiksiSource piksy = (PiksiSource) source;
                        piksy.stopPiksy();
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
                final TextView input = new TextView(GeoreferenciarActivity.this);
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("¿Desea continuar capturando el recorrido o regresar al menú principal? ");
                alert.setView(input);
                alert.setPositiveButton("Regresar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (gpsMode == 0){
                            source.stopLocationUpdates(map,GeoreferenciarActivity.this,marcadores,lineas,mode);
                            PiksiSource piksy = (PiksiSource) source;
                            piksy.stopPiksy();
                        }
                        Intent i = new Intent(GeoreferenciarActivity.this,MenuPrincipalActivity.class);
                        startActivity(i);
                        finish();
                    }
                });
                alert.setNegativeButton("Continuar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        btn_save.setEnabled(true);
                        btn_stop.setEnabled(true);
                    }
                });
                alert.show();
            }
            btn_save.setEnabled(true);
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
        if(source.getmRequestingLocationUpdates()){
            if(checkPermissions()){
                source.startLocationUpdates(map,this,marcadores,lineas,mode);
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
                source.stopLocationUpdates(map,this,marcadores,lineas,mode);
            }else{
                //stopHandlerRTK();
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
                    source.stopLocationUpdates(map,GeoreferenciarActivity.this,marcadores,lineas,mode);
                    GeoreferenciarActivity.super.onBackPressed();
                }
            });
            alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();
        }else{

            super.onBackPressed();
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
        if(source.getmRequestingLocationUpdates()){
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
                    punto = new GeoPoint(source.returnLatitude(), source.returnLongitude(), source.returnHeight());
                    //Toast.makeText(GeoreferenciarActivity.this, "Piksi no se encuentra conectado.", Toast.LENGTH_LONG).show();
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
                    final InfoWindow default_iw = marker.getInfoWindow();
                    marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker, MapView mapView) {

                            if (marker.getSubDescription() == null || marker.getSubDescription().equals("")) {
                                return false;

                            }
                            marker.setInfoWindow(default_iw);
                            marker.showInfoWindow();
                            Deg2UTM utm = new Deg2UTM(marker.getPosition().getLatitude(),marker.getPosition().getLongitude());
                            textHitoMapa.setText("Position: "+utm.toString());
                            //textHitoMapa.setText("");

                            return true;

                        }
                    });

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