package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Position;
import com.projects.mirai.koukin.pruebasmapa.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.osmdroid.util.GeoPoint;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that contains methods to store data from geojson files into mapviews and back from registers
 * on arrays into geojson files
 *  @author manuel
 *  @version 1.0
 */
public class GeoJsonUtils {

    /**
     * Number of loaded files on the mapview
     */
    private static int numberOfFoadedFiles=0;


    /**
     * Method that saves the points and lines registers on a geojson file inside the local directory
     * of the app
     * @param markers arraylist of the markers
     * @param lines arraylist of polylines
     * @param app context of the application
     * @param sesionID String representation of the current sesion
     * @return a boolean value that discribes the success of the operation true if it was successful
     *          false if there was an error
     */
    public static boolean saveFile(ArrayList<Marker> markers, ArrayList<Polyline> lines, AppCompatActivity app,String sesionID){

        FeatureCollection features = new FeatureCollection();

        for (Marker marker:markers){
            GeoPoint punto = marker.getPosition();
            Point point = new Point(punto.getLatitude(),punto.getLongitude(), punto.getAltitude());

            features.addFeature(new Feature(point));
        }

        LineString lineString = new LineString();
        List<Position> posiciones = new ArrayList<>();
        for (Polyline linea :lines){
            GeoPoint p1 = linea.getPoints().get(0);
            GeoPoint p2 = linea.getPoints().get(1);
            posiciones.add(new Position(p1.getLatitude(),p1.getLongitude(), p1.getAltitude()));
            posiciones.add(new Position(p2.getLatitude(),p2.getLongitude(), p2.getAltitude()));
        }
        lineString.setPositions(posiciones);
        features.addFeature(new Feature(lineString));
        try{
            JSONObject geoJSON = features.toJSON();
            Permissions.verifyStoragePermissions(app);

            String path;
            path = Environment.getExternalStorageDirectory() + File.separator +"MapasArq"+File.separator + "Temp"+File.separator+sesionID +".json";

            System.out.println(path);

            File file = new File(path);
            file.createNewFile();

            if(file.exists()){
                OutputStream fOut = new FileOutputStream(file);
                fOut.write(geoJSON.toString().getBytes());
                Toast.makeText(app.getApplicationContext(), "Puntos Guardados", Toast.LENGTH_LONG).show();
                return true;
            }else{
                Toast.makeText(app.getApplicationContext(), "No se pudo crear el archivo", Toast.LENGTH_LONG).show();
                return false;
            }
            //Toast.makeText(app.getApplicationContext(), "Puntos Guardados", Toast.LENGTH_LONG).show();
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(app.getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }



    /**
     * Method that loads the data store on a geojson file onto a mapview
     * @param selectedFile the string representation of the name of the file
     * @param map the mapview where the data is going to be show
     * @param app the context of the application where is located the mapview
     * @param markers a list of markers
     * @param lines a list of polylines
     */
    public static void loadFileGeo(String selectedFile, MapView map, AppCompatActivity app, ArrayList<Marker> markers, ArrayList<Polyline> lines){
        String textJson = getStringFromFile(selectedFile);
        GeoJSONObject geoJSON;
        try{
            geoJSON = GeoJSON.parse(textJson);
            JSONObject Json = geoJSON.toJSON();
            JSONArray features = Json.getJSONArray("features");
            for(int i=0;i< features.length();i++){
                JSONObject hito = features.getJSONObject(i);
                JSONObject elemento = features.getJSONObject(i).getJSONObject("geometry");
                if(elemento.get("type").equals("Point")){
                    JSONArray coordenadas = elemento.getJSONArray("coordinates");
                    Marker startMarker = new Marker(map);
                    startMarker.setPosition(new GeoPoint(coordenadas.getDouble(1),coordenadas.getDouble(0),coordenadas.getDouble(2)));
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    if(!hito.isNull("properties")){
                        JSONObject propiedades = hito.getJSONObject("properties");
                        startMarker.setIcon(ContextCompat.getDrawable(app.getApplicationContext(), R.drawable.marker));
                        startMarker.setTitle(propiedades.getString("title"));
                        startMarker.setSubDescription(propiedades.getString("subdescription"));
                    }
                    map.getOverlays().add(startMarker);
                    markers.add(startMarker);
                }else if(elemento.get("type").equals("LineString")){
                    JSONArray coordenadas = elemento.getJSONArray("coordinates");
                    //int colors[] = {R.color.colorRed,R.color.colorBlue,R.color.colorGreen,R.color.colorYellow};
                    int colors[] = {Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW};
                    for(int j = 0;j< coordenadas.length()-1;j++){
                        Polyline linea = new Polyline();
                        JSONArray punto1 = coordenadas.getJSONArray(j);
                        JSONArray punto2 = coordenadas.getJSONArray(j+1);
                        List <GeoPoint> geoPoints = new ArrayList<>();
                        GeoPoint p1 = new GeoPoint(punto1.getDouble(1),punto1.getDouble(0),punto1.getDouble(2));
                        GeoPoint p2 = new GeoPoint(new GeoPoint(punto2.getDouble(1),punto2.getDouble(0),punto2.getDouble(2)));
                        geoPoints.add(p1);
                        geoPoints.add(p2);
                        linea.setPoints(geoPoints);
                        if(numberOfFoadedFiles>0){
                            linea.setColor(colors[numberOfFoadedFiles-1]);
                        }
                        lines.add(linea);
                        map.getOverlayManager().add(linea);
                    }
                }
            }
            String elementos[] = selectedFile.split("/");
            String fileName = elementos[elementos.length-1];
            numberOfFoadedFiles+=1;
            AlertDialog.Builder alert = new AlertDialog.Builder(app.getBaseContext());
            alert.setTitle("Archivo "+fileName+" Cargado.");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();
            map.invalidate();
        }catch(JSONException e){
            Toast.makeText(app.getBaseContext(), "No se pudo Parsear el json", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }catch(Exception ex){
            ex.printStackTrace();
            Toast.makeText(app.getBaseContext(), "Existe un problema con su json", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Method that helps to save a travel on the directory of the application, it works with the
     * creation of a features collection
     * @param fileName the path of the file
     * @param markers a list of the markers that are going to be stored
     * @param lines a list of polylines
     * @param app the context of the application
     * @param sesionID an string representation of the sesion
     * @return returns the state of the saving process
     */
    public static boolean saveTravel(String fileName, ArrayList<Marker> markers, ArrayList<Polyline> lines, AppCompatActivity app,String sesionID){
        String path = Environment.getExternalStorageDirectory() + File.separator + "MapasArq" + File.separator + fileName +".json";

        File file = new File(path);
        if(file.exists()){
            AlertDialog.Builder alert = new AlertDialog.Builder(app.getBaseContext());
            alert.setTitle("Ese nombre de Archivo Ya existe");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();
            return false;
        }
        // Create feature with geometry
        FeatureCollection features = new FeatureCollection();

        for (Marker marker:markers){
            GeoPoint punto = marker.getPosition();
            Point point = new Point(punto.getLatitude(),punto.getLongitude(), punto.getAltitude());
            point.describeContents();
            Feature mark = new Feature(point);
            if(marker.getTitle()!=null){
                JSONObject jhito = new JSONObject();
                try {
                    jhito.put("title", marker.getTitle());
                    jhito.put("subdescription", marker.getSubDescription());

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                mark.setProperties(jhito);
            }

            features.addFeature(mark);
        }

        LineString lineString = new LineString();
        for (Polyline linea :lines){
            GeoPoint p1 = linea.getPoints().get(0);
            GeoPoint p2 = linea.getPoints().get(1);
            lineString.addPosition(new Position(p1.getLatitude(),p1.getLongitude(), p1.getAltitude()));
            lineString.addPosition(new Position(p2.getLatitude(),p2.getLongitude(), p1.getAltitude()));

        }
        features.addFeature(new Feature(lineString));
        try{
            JSONObject geoJSON = features.toJSON();
            Permissions.verifyStoragePermissions(app);

            file.createNewFile();

            if(file.exists()){
                OutputStream fOut = new FileOutputStream(file);
                fOut.write(geoJSON.toString().getBytes());
                //osw.flush();
                //osw.close();
                Toast.makeText(app.getApplicationContext(), "Puntos Guardados", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(app.getApplicationContext(), "No se pudo crear el archivo", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(app.getApplicationContext(), "Puntos Guardados", Toast.LENGTH_LONG).show();
        }catch(Exception e){
            e.printStackTrace();

            Toast.makeText(app.getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return true;
    }

    /**
     * Method that parses to extract the name of the FILE
     * @param selectedFile full path of the file.
     * @return the name of the file
     */
    public static String getStringFromFile(String selectedFile){
        File file = new File(selectedFile);
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
            e.printStackTrace();
        }
        return text.toString();
    }


}
