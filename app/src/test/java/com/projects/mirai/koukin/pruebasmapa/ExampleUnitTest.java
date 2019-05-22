package com.projects.mirai.koukin.pruebasmapa;

import com.projects.mirai.koukin.pruebasmapa.HelperClass.Deg2UTM;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.UTM2Deg;

import org.junit.Test;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.sql.SQLOutput;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    public final static double AVERAGE_RADIUS_OF_EARTH_MT = 6371000;


    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
        DateFormat df = new SimpleDateFormat("d-MMM-yyyy|HH_mm");
        String date = df.format(Calendar.getInstance().getTime());
        //System.out.println(date);
    }
    @Test
    public void calculateDistanceInMeters() {
        GeoPoint p1 = new GeoPoint(-2.190355, -80.054028);
        GeoPoint p2 = new GeoPoint(-2.193913, -80.061674);
        double userLat=p1.getAltitude();
        double userLng=p1.getLongitude();
        double venueLat=p2.getAltitude();
        double venueLng=p2.getLongitude();

        double latDistance = Math.toRadians(userLat - venueLat);
        double lngDistance = Math.toRadians(userLng - venueLng);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(venueLat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        //System.out.println((AVERAGE_RADIUS_OF_EARTH_MT * c));
    }


    public class MyThread implements Runnable {
        // to stop the thread
        private boolean exit;

        private String name;
        Thread t;

        MyThread(String threadname)
        {
            name = threadname;
            t = new Thread(this, name);
            System.out.println("New thread: " + t);
            exit = false;
            t.start(); // Starting the thread
        }

        // execution of thread starts from run() method
        public void run()
        {
            int i = 0;
            while (!exit) {
                System.out.println(name + ": " + i);
                i++;
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    System.out.println("Caught:" + e);
                }
            }
            System.out.println(name + " Stopped.");
        }

        // for stopping the thread
        public void stop()
        {
            exit = true;
        }
    }





    @Test
    public void main(){
        /*Deg2UTM prueba = new Deg2UTM(-0.149334,-78.407120);
        System.out.println(prueba);
        UTM2Deg prueba1 = new UTM2Deg(prueba.toString());
        System.out.println(prueba1);
        System.out.println(prueba1.getGeoPoint());*/


        MyThread hilo = new MyThread("Hilo1");
        try{
            Thread.sleep(20000);
        }catch (InterruptedException e){
            System.out.println("Caught:" + e);
        }
        hilo.stop();
        System.out.println("Exiting the main Thread");



    }
}