package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.widget.EditText;
import android.widget.TextView;

import com.projects.mirai.koukin.pruebasmapa.R;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.w3c.dom.Text;

public class HiloRTK implements Runnable {
    // to stop the thread
    private boolean exit;

    private String name;
    Thread t;
    private Context context;
    private SerialLink piksi;
    private long timer;
    private double lat,lon;
    MapView map;
    Marker persona;
    TextView txtLocationResult;

    public HiloRTK(String threadname, Context context, long timer,MapView map, TextView txtLocationResult)
    {
        name = threadname;
        this.context = context;
        this.timer =timer;
        this.map = map;
        this.txtLocationResult = txtLocationResult;



        t = new Thread(this, name);
        piksi = new SerialLink(context);
        System.out.println("New thread: " + t);
        exit = false;
        t.start(); // Starting the thread


        //Valores de defecto apuntando /espol
        lat = -2.1481404;
        lon = -79.9666772;
        persona = null;
    }

    // execution of thread starts from run() method
    public void run()
    {
        if(isPiksiON()){


            while (!exit) {
                //here goes the repetition.
                //lat = piksi.getLat();
                //lon = piksi.getLon();
                GeoPoint startPoint = new GeoPoint(lat,lon);
                map.getOverlays().remove(persona);
                persona = new Marker(map);
                persona.setPosition(startPoint);
                //map.invalidate();
                persona.setIcon(ContextCompat.getDrawable(context, R.drawable.usericon));
                //persona.setPosition(new GeoPoint(lat,lon));
                persona.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                map.getOverlays().add(persona);
                map.invalidate();

                //Se actualiza el texto inferior
                Deg2UTM transform = new Deg2UTM(lat,lon);
                /*txtLocationResult.setText(
                        "Ubicacion: " + transform.toString()
                );*/

                //Pruebas
                //lat += 0.0001404;
                //lon += 0.0001404;
                String data_1 = name + ": Nuevo datos del piksi -> lat: " + lat + ", log: "+ lon;
                System.out.println(data_1);
                try {
                    Thread.sleep(timer);
                }
                catch (InterruptedException e) {
                    System.out.println("Caught:" + e);
                }
            }
            System.out.println(name + ": Stopped.");
        }
        System.out.println(name + ": Piksi is off.");
    }

    // for stopping the thread

    public void stop()
    {
        map.getOverlays().remove(persona);
        piksi.destroy();
        exit = true;
    }

    public String getName(){
        return this.name;
    }

    public boolean isPiksiON(){
        return piksi.start();
    }
}
