package com.projects.mirai.koukin.pruebasmapa;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Position;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Constants;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.FechaJornada;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Permissions;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.RecorridoGuardar;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.SavedMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *  Actividad usada para escribir al correo al que se le enviará el mensaje por correo con los
 *  registros de las jornadas
 * @author luibasantes, mleiton
 * @version 1.0
 * @since 1.0
 */
public class SelectMailActivity extends AppCompatActivity {
    Button btn_enviar;

    AutoCompleteTextView email_auto;

    String fechaJornada;

    ArrayList<RecorridoGuardar> recorridos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_mail);
        recorridos = new ArrayList<>();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            fechaJornada = extras.getString("Jornada");
        }

        Log.d("Jornada Seleccionada",fechaJornada);

        btn_enviar = findViewById(R.id.btn_enviar3);

        email_auto = findViewById(R.id.email_auto_1);

        btn_enviar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(validateFields()){
                    String filename="Jornada_"+fechaJornada+".json";


                    if(generateFile(filename)){

                        File filelocation = new File(Constants.pathJornadas, filename);
                        Uri path = Uri.fromFile(filelocation);

                        recorridos.clear();
                        Intent intent = new Intent(Intent.ACTION_SEND);

                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email_auto.getText().toString()});
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Jordada del :"+fechaJornada);
                        intent.putExtra(Intent.EXTRA_TEXT, "Aqui va el archivo de la jornada adjunto. Saludos Cordiales.");

                        intent.putExtra(Intent.EXTRA_STREAM, path);

                        intent.setType("message/rfc822");

                        startActivity(Intent.createChooser(intent, "Elija aplicación para enviar mail :"));
                    }

                }

            }
        });
    }


    /**
     * Método que sirve para validar el campo del correo electrónico al que se enviará la jornada
     * @return
     */
    public boolean validateFields(){
        if(email_auto.getText().toString().matches("([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)")){
            Toast.makeText(this,"Email Valido",Toast.LENGTH_SHORT).show();

            return true;
        }else{
            AlertDialog.Builder alert = new AlertDialog.Builder(SelectMailActivity.this);
            alert.setTitle("Ingrese un correo electrónico valido.");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();
        }
        return false;
    }


    /**
     *
     * @param filename
     * @return
     */
    public boolean generateFile(String filename){
        String path = Constants.pathMapaArq;
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        Log.d("Files", "Size: "+ files.length);
        File archivo;

        for (int i = 0; i < files.length; i++)
        {
            archivo = files[i];
            //itero todos los archivos dentro del directorio
            if(archivo.isFile() && archivo.getName().endsWith(".json")){
                Log.d("Files", "FileName:" + archivo.getName());
                String[] elementos= archivo.getName().split("\\|");
                Log.d("generateFile","Elemento[1]:"+elementos[1]);
                //tomo los archivos cuya fecha es igual a la del dia de la expedicion
                if(fechaJornada.equals(elementos[1])){
                    RecorridoGuardar rec = loadFileToRec(archivo);
                    recorridos.add(rec);
                }

            }

        }
        if(recorridos.size()>0){
            return saveJourney(filename);
        }
        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(this);
        alert.setTitle("No se encontraron archivos para ese dia.");
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        alert.show();
        return false;
    }


    /**
     * Método que escribe un archivo con los recorridos del día
     * @param fileName nombre del archivo
     * @return boolean que define si se ha guardado el archivo de la jornada
     */
    public boolean saveJourney(String fileName){

        String path = Constants.pathJornadas + fileName;

        Log.d("saveJourney", "Size: "+ path);

        File file = new File(path);
        if(file.exists()){
            android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(this);
            alert.setTitle("Esa jornada ya fue generada");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();
        }

        // Create feature with geometry
        FeatureCollection features = new FeatureCollection();

        for (RecorridoGuardar rec: recorridos){
            for (JSONObject geo:rec.getHito()){

                GeoPoint point = null;

                try {
                    point = (GeoPoint) geo.get("point");

                    Feature feat = new Feature(new Point(point.getLatitude(),point.getLongitude()));
                    if(!geo.isNull("properties")){
                        System.out.println(rec.getHito());
                        feat.setProperties(geo.getJSONObject("properties"));
                    }

                    features.addFeature(feat);
                }catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        for (RecorridoGuardar rec: recorridos){
            LineString lineString = new LineString();
            for (Polyline linea :rec.getLines()){
                GeoPoint p1 = linea.getPoints().get(0);
                GeoPoint p2 = linea.getPoints().get(1);
                lineString.addPosition(new Position(p1.getLatitude(),p1.getLongitude()));
                lineString.addPosition(new Position(p2.getLatitude(),p2.getLongitude()));
            }
            features.addFeature(new Feature(lineString));
        }

        try{
            JSONObject geoJSON = features.toJSON();
            Permissions.verifyStoragePermissions(this);

            boolean created = file.createNewFile();
            Log.d("saveJourney", "Se Creo: "+ created);
            if(file.exists()){
                OutputStream fOut = new FileOutputStream(file);
                //OutputStreamWriter osw = new OutputStreamWriter(fOut);
                //osw.write(geoJSON.toString());
                fOut.write(geoJSON.toString().getBytes());
                System.out.println(geoJSON.toString());
                //osw.flush();
                //osw.close();
                Toast.makeText(getApplicationContext(), "Jornada generada", Toast.LENGTH_LONG).show();
                return true;
            }else{
                Toast.makeText(getApplicationContext(), "No se pudo crear la jornada", Toast.LENGTH_LONG).show();
                return false;
            }
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }


    /**
     * Método que guarda los datos de un archivo geoJSON en objetos de recorridos
     * @param arch archivo GEOJSON
     * @return el arreglo privado de recorridos lleno según los datos del archivo geoJSON
     */
    private RecorridoGuardar loadFileToRec(File arch){
        GeoJSONObject geoJSON;
        String textJson = getStringFromFile(arch);
        RecorridoGuardar rec = new RecorridoGuardar(true);
        try{
            geoJSON = GeoJSON.parse(textJson);
            JSONObject Json = geoJSON.toJSON();
            System.out.println("Probando:"+Json.getJSONArray("features"));
            JSONArray features = Json.getJSONArray("features");
            for(int i=0;i< features.length();i++){
                JSONObject hito = features.getJSONObject(i);
                JSONObject elemento = features.getJSONObject(i).getJSONObject("geometry");
                System.out.println("Elemento"+i+":"+elemento);

                if(elemento.get("type").equals("Point")){
                    JSONArray coordenadas = elemento.getJSONArray("coordinates");
                    GeoPoint point = new GeoPoint(coordenadas.getDouble(1),coordenadas.getDouble(0));
                    rec.addPoint(point);
                    rec.addHito(point,null);
                    if(!hito.isNull("properties")){
                        JSONObject propiedades = hito.getJSONObject("properties");
                        rec.addHito(point,propiedades);
                        System.out.println("hito: "+propiedades);
                    }

                }else if(elemento.get("type").equals("LineString")){
                    JSONArray coordenadas = elemento.getJSONArray("coordinates");

                    int colors[] = {Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW};
                    for(int j = 0;j< coordenadas.length()-1;j++){
                        Polyline linea = new Polyline();
                        JSONArray punto1 = coordenadas.getJSONArray(j);
                        JSONArray punto2 = coordenadas.getJSONArray(j+1);
                        List<GeoPoint> geoPoints = new ArrayList<>();
                        GeoPoint p1 = new GeoPoint(punto1.getDouble(1),punto1.getDouble(0));
                        GeoPoint p2 = new GeoPoint(new GeoPoint(punto2.getDouble(1),punto2.getDouble(0)));
                        geoPoints.add(p1);
                        geoPoints.add(p2);
                        linea.setPoints(geoPoints);
                        rec.addLine(linea);
                    }
                }

            }
        }catch(JSONException e){
            Toast.makeText(getBaseContext(), "No se pudo Parsear el json", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }catch(Exception ex){
            Toast.makeText(getBaseContext(), "Existe un problema con su json: "+ex.toString(), Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
        return rec;
    }

    /**
     * Método que obtiene una representación en String del contenido de un archivo
     * @param file de tipo File, archivo a convertir
     * @return String representación del archivo
     */
    public String getStringFromFile(File file){
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

}
