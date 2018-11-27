package com.projects.mirai.koukin.pruebasmapa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.projects.mirai.koukin.pruebasmapa.HelperClass.MyHelperSql;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Permissions;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.modules.SqliteArchiveTileWriter;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Descargar_Mapa extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, TextWatcher{

    private static final String TAG = Descargar_Mapa.class.getSimpleName();

    MyHelperSql myHelperDB;

    SQLiteDatabase mSqliteDB;

    @BindView(R.id.map)
    MapView map;

    @BindView(R.id.tv_ancho)
    TextView tv_ancho;

    @BindView(R.id.tv_alto)
    TextView tv_alto;

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




    SqliteArchiveTileWriter writer=null;
    AlertDialog downloadPrompt=null;



    Button btnCache,executeJob;
    SeekBar zoom_min;
    SeekBar zoom_max;
    EditText cache_north, cache_south, cache_east,cache_west, cache_output,mapName;
    TextView cache_estimate,tv_tamanioMB;
    CacheManager mgr=null;
    AlertDialog alertDialog=null;

    protected static final int DEFAULT_INACTIVITY_DELAY_IN_MILLISECS = 200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_descargar__mapa);
        Permissions.verifyLocationPermission(this);
        Permissions.verifyStoragePermissions(this);


        myHelperDB = new MyHelperSql(Descargar_Mapa.this,"MAPDB",null,1);
        mSqliteDB = myHelperDB.getWritableDatabase();
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        ButterKnife.bind(this);

        // initialize the necessary libraries
        init();
        // restore the values from saved instance state
        restoreValuesFromBundle(savedInstanceState);

        setupMap();

        Bundle extras = getIntent().getExtras();
        //Si me envian un archivo a cargar , hago esto:

    }

    private void setupMap(){


        map.setTileSource(TileSourceFactory.MAPNIK);
        //map.setBuiltInZoomControls(true);
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(17.0);

        MyLocationNewOverlay oMapLocationOverlay = new MyLocationNewOverlay(map);
        map.getOverlays().add(oMapLocationOverlay);

        //MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        //map.getOverlays().add(0, mapEventsOverlay);

        oMapLocationOverlay.enableFollowLocation();
        oMapLocationOverlay.enableMyLocation();
        oMapLocationOverlay.enableFollowLocation();

        mapController.setZoom(15.0);
        map.setMapListener(new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateDistance();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                updateDistance();
                return false;
            }
        }, DEFAULT_INACTIVITY_DELAY_IN_MILLISECS));

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
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates);
        outState.putParcelable("last_known_location", mCurrentLocation);
        outState.putString("last_updated_on", mLastUpdateTime);

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
                                    rae.startResolutionForResult(Descargar_Mapa.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(Descargar_Mapa.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

                    }
                });
    }


    public void stopLocationUpdates() {
        // Removing location updates
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(getApplicationContext(), "¡Actualizaciones de ubicación detenidas!", Toast.LENGTH_SHORT).show();

                    }
                });
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


    public void updateDistance(){
        DecimalFormat df = new DecimalFormat(".##");
        BoundingBox bb = map.getBoundingBox();
        double north,south,west,east;
        //arriba
        north=bb.getLatNorth();
        //Abajo
        south= bb.getLatSouth();
        //Izquierda
        west = bb.getLonWest();
        //Derecha
        east = bb.getLonEast();

        GeoPoint p1 = new GeoPoint(north,west);
        GeoPoint p2= new GeoPoint(north,east);
        double ancho = DistanceCalculator.calculateDistanceInKM(p1,p2);
        if(ancho > 1.0d){
            tv_ancho.setText(df.format(ancho)+" KM");

        }else
            tv_ancho.setText(df.format(ancho*1000)+" MT");







        GeoPoint p3 = new GeoPoint(north,west);
        GeoPoint p4 = new GeoPoint(south,west);
        double alto = DistanceCalculator.calculateDistanceInKM(p3,p4);
        if(alto >1.0d)
            tv_alto.setText(df.format(alto)+" KM");
        else
            tv_alto.setText(df.format(alto*1000)+" MT");
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
                    //String outputName = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "osmdroid" + File.separator + cache_output.getText().toString();
                    String outputName = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "osmdroid" + File.separator + mapName.getText().toString();
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

                double tamanio = (tilecount * 633)/1000;
                if(tamanio < 1000){
                    tv_tamanioMB.setText(tamanio + " KB");
                }else{
                    tamanio = tamanio /1000;
                    tv_tamanioMB.setText(tamanio + " MB");
                }


                if (startJob)
                {
                    if ( downloadPrompt!=null) {
                        downloadPrompt.dismiss();
                        downloadPrompt=null;
                    }

                    //this triggers the download
                    mgr.downloadAreaAsync(Descargar_Mapa.this, bb, zoommin, zoommax, new CacheManager.CacheManagerCallback() {
                        @Override
                        public void onTaskComplete() {
                            Toast.makeText(Descargar_Mapa.this, "Descarga Completa!", Toast.LENGTH_LONG).show();
                            saveMapInDb();
                            if (writer!=null)
                                writer.onDetach();
                        }

                        @Override
                        public void onTaskFailed(int errors) {
                            Toast.makeText(Descargar_Mapa.this, "Descarga completa con " + errors + " errores", Toast.LENGTH_LONG).show();
                            saveMapInDb();
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

    @OnClick(R.id.btn_zoomIn)
    public void zoomIn(){
        mapController.zoomIn();
    }

    @OnClick(R.id.btn_zoomOut)
    public void zoomOut(){
        mapController.zoomOut();
    }





    @OnClick(R.id.btn_descargar)
    public void downloadJobAlert() {
        /*try{
            AlertDialog.Builder builder = new AlertDialog.Builder(MapaActivity.this);

            String outputName = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "osmdroid" + File.separator + "ProbandoDowload".toString();
            writer=new SqliteArchiveTileWriter(outputName);
            CacheManager mgr = new CacheManager(map, writer);

            int zoommin = 15;
            int zoommax = 20;
            BoundingBox bb= map.getBoundingBox();
            int tilecount = mgr.possibleTilesInArea(bb, zoommin, zoommax);
            mgr.downloadAreaAsync(this.getApplicationContext(), bb, zoommin, zoommax, new CacheManager.CacheManagerCallback() {

                @Override
                public void onTaskComplete() {
                    Toast.makeText(MapaActivity.this, "Download complete!", Toast.LENGTH_LONG).show();
                    if (writer!=null)
                        writer.onDetach();
                }

                @Override
                public void onTaskFailed(int errors) {
                    Toast.makeText(MapaActivity.this, "Download complete with " + errors + " errors", Toast.LENGTH_LONG).show();
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

            downloadPrompt=builder.create();
            downloadPrompt.show();



        }catch(Exception e){
            Toast.makeText(getApplicationContext(), "¡Something Happen!", Toast.LENGTH_SHORT).show();
        }*/

        AlertDialog.Builder builder = new AlertDialog.Builder(Descargar_Mapa.this);

        View view = View.inflate(Descargar_Mapa.this, R.layout.sample_cachemgr_input, null);
        view.findViewById(R.id.cache_archival_section).setVisibility(View.VISIBLE);

        BoundingBox boundingBox = map.getBoundingBox();
        tv_tamanioMB = (TextView) view.findViewById(R.id.tv_tamanioMB);


        zoom_max=(SeekBar) view.findViewById(R.id.slider_zoom_max);
        zoom_max.setMax((int) map.getMaxZoomLevel());
        zoom_max.setOnSeekBarChangeListener(Descargar_Mapa.this);

        zoom_max.setProgress(20);

        zoom_min=(SeekBar) view.findViewById(R.id.slider_zoom_min);
        zoom_min.setMax((int) map.getMaxZoomLevel());
        zoom_min.setProgress((int) map.getMinZoomLevel());

        zoom_min.setProgress(17);

        zoom_min.setOnSeekBarChangeListener(Descargar_Mapa.this);
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

        mapName = (EditText) view.findViewById(R.id.map_name);

        //change listeners for both validation and to trigger the download estimation
        cache_east.addTextChangedListener((TextWatcher) this);
        cache_north.addTextChangedListener((TextWatcher) this);
        cache_south.addTextChangedListener((TextWatcher) this);
        cache_west.addTextChangedListener((TextWatcher) this);
        executeJob= (Button) view.findViewById(R.id.executeJob);
        executeJob.setOnClickListener(Descargar_Mapa.this);
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
            BoundingBox bb= new BoundingBox(n, e, s, w);

            int zoommin = zoom_min.getProgress();
            int zoommax = zoom_max.getProgress();

            if (mgr==null)
                mgr = new CacheManager(map);

            int tilecount = mgr.possibleTilesInArea(bb, zoommin, zoommax);
            cache_estimate.setText(tilecount + " Cuadrillas");
            double tamanio = (tilecount * 633)/1000;

            if(tamanio < 1000){
                tv_tamanioMB.setText(tamanio + " KB");
            }else {
                tamanio = tamanio / 1000;
                tv_tamanioMB.setText(tamanio + " MB");
            }

        }

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






    public void saveMapInDb(){
        IGeoPoint centro = map.getMapCenter();
        long id = myHelperDB.insertMapInDb(mSqliteDB,mapName.getText().toString(),centro.getLatitude(),centro.getLongitude(),map.getZoomLevelDouble());
        Toast.makeText(Descargar_Mapa.this,String.valueOf(id),Toast.LENGTH_LONG).show();
    }
}
