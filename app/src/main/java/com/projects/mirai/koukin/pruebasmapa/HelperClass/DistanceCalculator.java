package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import org.osmdroid.util.GeoPoint;

public class DistanceCalculator {


    public final static double AVERAGE_RADIUS_OF_EARTH_MT = 6371000;
    public final static double AVERAGE_RADIUS_OF_EARTH_KM = 6371;

    public static double calculateDistanceInMeters(GeoPoint p1, GeoPoint p2) {
        double userLat=p1.getLatitude();
        double userLng=p1.getLongitude();
        double venueLat=p2.getLatitude();
        double venueLng=p2.getLongitude();

        double latDistance = Math.toRadians(userLat - venueLat);
        double lngDistance = Math.toRadians(userLng - venueLng);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(venueLat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return  (AVERAGE_RADIUS_OF_EARTH_MT * c);
    }


    public static double calculateDistanceInKM(GeoPoint p1, GeoPoint p2) {
        double userLat=p1.getLatitude();
        double userLng=p1.getLongitude();
        double venueLat=p2.getLatitude();
        double venueLng=p2.getLongitude();

        double latDistance = Math.toRadians(userLat - venueLat);
        double lngDistance = Math.toRadians(userLng - venueLng);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(venueLat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return  (AVERAGE_RADIUS_OF_EARTH_KM * c);
    }
}
