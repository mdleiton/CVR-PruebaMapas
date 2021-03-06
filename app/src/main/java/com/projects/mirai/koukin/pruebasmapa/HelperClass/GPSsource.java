package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.projects.mirai.koukin.pruebasmapa.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;
import static com.projects.mirai.koukin.pruebasmapa.HelperClass.FileUtils.TAG;

/**
 * Class that extends from PositioningSource and acts as an interface to get GPS location and set
 * them on a map
 * @author manuel
 * @version 1.0
 */
public class GPSsource extends PositioningSource {

    public FusedLocationProviderClient mFusedLocationClient;
    public SettingsClient mSettingsClient;
    public LocationRequest mLocationRequest;
    public LocationSettingsRequest mLocationSettingsRequest;
    public LocationCallback mLocationCallback;
    public Location mCurrentLocationGPS;
    private String mLastUpdateTimeGPS;
    public Location actualPosition;
    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle

    /**
     * @return
     */
    @Override
    public double returnLatitude() {
        if (mCurrentLocationGPS == null) {
            return actualPosition.getLatitude();
        }
        return mCurrentLocationGPS.getLatitude();
    }

    /**
     * @return
     */
    @Override
    public double returnLongitude() {
        if (mCurrentLocationGPS == null) {
            return actualPosition.getLongitude();
        }
        return mCurrentLocationGPS.getLongitude();
    }

    /**
     * @return
     */
    @Override
    public double returnHeight() {
        if (mCurrentLocationGPS == null) {
            return actualPosition.getAltitude();
        }
        return mCurrentLocationGPS.getAltitude();
    }

    /**
     * Method that inits all the required parameters or permissions necessary to start the
     * callbacks methods to get location updates
     */
    @Override
    public void init(final MapView map, final AppCompatActivity app, final ArrayList<Marker> markers, final ArrayList<Polyline> lines, final int mode) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(app);
        mSettingsClient = LocationServices.getSettingsClient(app);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mCurrentLocationGPS = locationResult.getLastLocation();
                mLastUpdateTimeGPS = DateFormat.getTimeInstance().format(new Date());
                updateLocationUI(map, app, markers, lines, mode, txtLocationResult);
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
        actualPosition(app);
    }

    /**
     * Method that prepares everything to start to receive location updates
     */
    @Override
    public void startLocationUpdates(final MapView map, final AppCompatActivity app, final ArrayList<Marker> markers, final ArrayList<Polyline> lines, final int mode) {
        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(app, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateLocationUI(map, app, markers, lines, mode);
                    }
                })
                .addOnFailureListener(app, new OnFailureListener() {
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
                                    rae.startResolutionForResult(app, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(app, errorMessage, Toast.LENGTH_LONG).show();
                        }
                        updateLocationUI(map, app, markers, lines, mode);
                    }
                });

    }


    /**
     * This method gets a list off markers, lines and the mode by which the locations request are
     * been taken and returns an string representation of the current position
     * @param app The activity that is calling the method
     * @param markers a list of Marker objects where the location updates are gonna be stored
     * @param lines a list of lines that resembles the travel
     * @param mode if is mode 0 then the requirements are on time if those are 1 then the
     *             requirements are based on distance
     */
    @Override
    public String updateLocationUI(MapView map, AppCompatActivity app, ArrayList<Marker> markers, ArrayList<Polyline> lines, int mode) {
        IMapController mapController = map.getController();
        if (mCurrentLocationGPS != null) {
            Deg2UTM transform = new Deg2UTM(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude());
            if (markers.size() == 0) {
                GeoPoint startPoint = new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude(), mCurrentLocationGPS.getAltitude());
                Marker startMarker = new Marker(map);
                Drawable mark = ContextCompat.getDrawable(app.getBaseContext(), R.drawable.usericon);
                //Drawable mark = ContextCompat.getDrawable(app, R.drawable.usericon);
                startMarker.setIcon(scaleImage(mark, 0.8f, app));
                //Marker startMarker = new Marker(map);
                startMarker.setPosition(startPoint);
                map.getOverlays().add(startMarker);
                markers.add(startMarker);
                mapController.setCenter(startPoint);
            }
            if (markers.size() > 1) {
                if (mode == 0) {
                    updateBasedOnDistance(map, markers, lines);
                } else {
                    mLastUpdateTimeGPS = DateFormat.getTimeInstance().format(new Date());
                    List<GeoPoint> geoPoints = new ArrayList<>();
                    geoPoints.add(markers.get(markers.size() - 2).getPosition());
                    GeoPoint newPoint = new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude(), mCurrentLocationGPS.getAltitude());
                    geoPoints.add(newPoint);
                    Marker startMarker = new Marker(map);
                    startMarker.setPosition(newPoint);
                    Polyline line = new Polyline();
                    line.setPoints(geoPoints);
                    lines.add(line);
                    map.getOverlayManager().add(line);
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                }
            }


            if (!self_reference) {

                return transformtoString(transform, mCurrentLocationGPS.getAltitude());
            } else {
                return selfReferenceString(base, new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude()));
            }
        }
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
        IMapController mapController = map.getController();
        if (mCurrentLocationGPS != null) {
            Deg2UTM transform = new Deg2UTM(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude());
            if (markers.size() == 0) {
                GeoPoint startPoint = new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude(), mCurrentLocationGPS.getAltitude());
                Marker startMarker = new Marker(map);
                Drawable mark = ContextCompat.getDrawable(app.getBaseContext(), R.drawable.usericon);
                startMarker.setIcon(scaleImage(mark, 0.8f, app));
                startMarker.setPosition(startPoint);
                map.getOverlays().add(startMarker);
                markers.add(startMarker);
                mapController.setCenter(startPoint);
            }
            if (markers.size() > 1) {
                if (mode == 0) {
                    updateBasedOnDistance(map, markers, lines);
                } else {
                    mLastUpdateTimeGPS = DateFormat.getTimeInstance().format(new Date());
                    List<GeoPoint> geoPoints = new ArrayList<>();
                    geoPoints.add(markers.get(markers.size() - 2).getPosition());
                    GeoPoint newPoint = new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude(), mCurrentLocationGPS.getAltitude());
                    geoPoints.add(newPoint);
                    Marker startMarker = new Marker(map);
                    startMarker.setPosition(newPoint);
                    Polyline line = new Polyline();
                    line.setPoints(geoPoints);
                    lines.add(line);
                    map.getOverlayManager().add(line);
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                }
            }

            txtLocationResult.setAlpha(0);
            txtLocationResult.animate().alpha(1).setDuration(300);
            onFollowon.setText("Última Actualización: " + mLastUpdateTimeGPS);
            if (!self_reference) {
                txtLocationResult.setText(transformtoString(transform, mCurrentLocationGPS.getAltitude()));
                return transformtoString(transform, mCurrentLocationGPS.getAltitude());
            } else {
                txtLocationResult.setText(selfReferenceString(base, new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude(), mCurrentLocationGPS.getAltitude())));
                return selfReferenceString(base, new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude()));
            }
        }
        txtLocationResult.setText("None yet");
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
    public void stopLocationUpdates(MapView map, final AppCompatActivity app, ArrayList<Marker> markers, ArrayList<Polyline> lines, int mode) {
        this.saveLastPoint(map,app,markers,lines);
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(app, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(app.getApplicationContext(), "¡Actualizaciones de ubicación detenidas!", Toast.LENGTH_SHORT).show();

                    }
                });
    }

    @Override
    public void setActualPositiononMap(MapView map) {
        MyLocationNewOverlay oMapLocationOverlay = new MyLocationNewOverlay(map);
        map.getController().setCenter(oMapLocationOverlay.getMyLocation());
        map.getController().setZoom(15.0);
    }


    /**
     * Method that returns a string representation of the actual position
     *
     * @param utm      a Deg2UTM object, representation of utm coordinates
     * @param altitude the actual altitude
     * @return a string representation of the actual position
     */
    @Override
    public String transformtoString(Deg2UTM utm, double altitude) {
        return "Location: " + utm.toString() + " H:" + String.valueOf(Math.floor(altitude * 1000) / 1000);
    }

    /**
     * @return
     */
    @Override
    public String lastKnownUpdate() {
        return "Last update on: " + mLastUpdateTimeGPS;
    }

    /**
     * Restoring values from saved instance state
     * @param savedInstanceState instace a Bundle object
     */
    @Override
    public void restoreValuesFromBundle(Bundle savedInstanceState) {
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

    }

    /**
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates);
        outState.putParcelable("last_known_location", mCurrentLocationGPS);
        outState.putString("last_updated_on", mLastUpdateTimeGPS);
    }

    /**
     * @param interval
     */
    @Override
    public void setIntervalOnMillis(long interval) {
        this.getmLocationRequest().setInterval(interval);
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
        this.getmLocationRequest().setFastestInterval(fastInterval);
    }


    /**
     *
     * @param map
     * @param markers
     * @param lines
     */
    private void updateBasedOnDistance(MapView map, ArrayList<Marker> markers, ArrayList<Polyline> lines) {
        IMapController mapController = map.getController();
        GeoPoint startPoint = markers.get(markers.size() - 1).getPosition();
        GeoPoint newPoint = new GeoPoint(mCurrentLocationGPS.getLatitude(), mCurrentLocationGPS.getLongitude(), mCurrentLocationGPS.getAltitude());
        double dist = DistanceCalculator.calculateDistanceInMeters(startPoint, newPoint);
        if (dist >= distance) {
            Marker startMarker = new Marker(map);
            startMarker.setPosition(newPoint);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(startMarker);
            markers.add(startMarker);
            mapController.setCenter(newPoint);
            List<GeoPoint> geoPoints = new ArrayList<>();
            geoPoints.add(startPoint);
            geoPoints.add(newPoint);
            Polyline line = new Polyline();
            line.setPoints(geoPoints);
            lines.add(line);
            map.getOverlayManager().add(line);
        }
    }

    /**
     *
     * @param location
     */
    public void setmLocationRequest(LocationRequest location) {
        this.mLocationRequest = location;
    }


    /**
     *
     * @return
     */
    public LocationRequest getmLocationRequest() {
        return this.mLocationRequest;
    }

    private void actualPosition(AppCompatActivity app) {

        LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
        for (String provider : locationManager.getProviders(true)) {
            if (ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null)
            {
                actualPosition = location;
                System.out.println(actualPosition.toString());
                break;
            }
        }
    }

}


