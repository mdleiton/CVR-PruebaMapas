package com.projects.mirai.koukin.pruebasmapa;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Toast;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Position;
import com.google.gson.JsonObject;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Constants;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Permissions;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.RecorridoGuardar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SendMailActivity extends AppCompatActivity {
    Button btn_enviar;
    AutoCompleteTextView email_auto;
    CalendarView calendarView;
    private int daySelected,monthSelected,yearSelected, hourSelected, minuteSelected;
    SimpleDateFormat dayFormat,monthFormat,yearFormat, hourFormat, minuteFormat;
    ArrayList <RecorridoGuardar> recorridos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_mail);
        recorridos = new ArrayList<>();



        btn_enviar = (Button) findViewById(R.id.btn_enviar);
        email_auto = (AutoCompleteTextView) findViewById(R.id.email_auto);
        calendarView = (CalendarView) findViewById(R.id.calendarView);
        Date fecha = new Date(calendarView.getDate());
        dayFormat = new SimpleDateFormat("dd");
        monthFormat = new SimpleDateFormat("MM");
        yearFormat = new SimpleDateFormat("yyyy");
        hourFormat = new SimpleDateFormat("HH");
        minuteFormat = new SimpleDateFormat("mm");


        daySelected=Integer.parseInt(dayFormat.format(calendarView.getDate()));

        monthSelected=Integer.parseInt(monthFormat.format(calendarView.getDate()));
        yearSelected=Integer.parseInt(yearFormat.format(calendarView.getDate()));
        minuteSelected=Integer.parseInt(minuteFormat.format(calendarView.getDate()));
        hourSelected=Integer.parseInt(hourFormat.format(calendarView.getDate()));

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {

            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month,
                                            int dayOfMonth) {
                daySelected=dayOfMonth;
                yearSelected=year;
                monthSelected=month+1;
            }
        });

        btn_enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateFields()){
                    String filename="Jornada_"+daySelected+"-"+monthSelected+"-"+yearSelected+"-"+minuteSelected+"-"+hourSelected+".json";
                    System.out.println(filename);

                    if(generateFile(filename)){

                        File filelocation = new File(Constants.pathJornadas, filename);
                        Uri path = Uri.fromFile(filelocation);

                        recorridos.clear();
                        Intent intent = new Intent(Intent.ACTION_SEND);

                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email_auto.getText().toString()});
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Jordada del :"+daySelected+"-"+monthSelected+"-"+yearSelected);
                        intent.putExtra(Intent.EXTRA_TEXT, "Aqui va el archivo de la jornada adjunto. Saludos Cordiales.");

                        intent.putExtra(Intent.EXTRA_STREAM, path);

                        intent.setType("message/rfc822");

                        startActivity(Intent.createChooser(intent, "Elija aplicación para enviar mail :"));
                    }


                }


            }
        });

    }



    public boolean validateFields(){
        if(email_auto.getText().toString().matches("([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)")){
            Toast.makeText(this,"Email Valido",Toast.LENGTH_SHORT).show();

            return true;
        }else{
            AlertDialog.Builder alert = new AlertDialog.Builder(SendMailActivity.this);
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
     * Metodo que genera un archivo de la jornada de un dia
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
            if(archivo.isFile() && archivo.getName().endsWith(".json")){
                Log.d("Files", "FileName:" + archivo.getName());
                String[] elementos = archivo.getName().split("\\|");
                Log.d("generateFile","Elemento[1]:"+elementos[1]);
                if(checkDate(elementos[1])){
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

        // Create geometry

        // Create feature with geometry
        FeatureCollection features = new FeatureCollection();

        for (RecorridoGuardar rec: recorridos){
            for (JSONObject geoh:rec.getHito()){
                try{
                    GeoPoint geo = (GeoPoint) geoh.get("hito");
                    Point point = new Point(geo.getLatitude(),geo.getLongitude(), geo.getAltitude());
                    Feature feat = new Feature(point);
                    if(!geoh.isNull("properties")){
                        feat.setProperties(geoh.getJSONObject("properties"));
                    }
                    features.addFeature(feat);
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }

        }
        for (RecorridoGuardar rec: recorridos){
            LineString lineString = new LineString();
            for (Polyline linea :rec.getLines()){
                GeoPoint p1 = linea.getPoints().get(0);
                GeoPoint p2 = linea.getPoints().get(1);
                lineString.addPosition(new Position(p1.getLatitude(),p1.getLongitude(), p1.getAltitude()));
                lineString.addPosition(new Position(p2.getLatitude(),p2.getLongitude(), p2.getAltitude()));
            }
            features.addFeature(new Feature(lineString));

        }

        try{
            JSONObject geoJSON = features.toJSON();
            System.out.println(geoJSON);
            Permissions.verifyStoragePermissions(this);



            boolean created = file.createNewFile();
            Log.d("saveJourney", "Se Creo: "+ created);
            if(file.exists()){
                OutputStream fOut = new FileOutputStream(file);
                //OutputStreamWriter osw = new OutputStreamWriter(fOut);
                //osw.write(geoJSON.toString());
                System.out.println("GEOjSON : "+geoJSON.toString());
                fOut.write(geoJSON.toString().getBytes());
                //osw.flush();
                //osw.close();
                Toast.makeText(getApplicationContext(), "Jornada generada", Toast.LENGTH_LONG).show();
                return true;
            }else{
                Toast.makeText(getApplicationContext(), "No se pudo crear la jornada", Toast.LENGTH_LONG).show();
                return false;
            }
        }catch(Exception e){
            System.out.println(e);
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }


    public boolean checkDate(String lastModified){
        Date fechaModificacion = new Date(lastModified);


        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy");
        SimpleDateFormat sdf1 = new SimpleDateFormat("dd-MMM-yy");
        Date strDate;
        Date fechaM;
        try{
            Log.d("checkDate","FechaCalendario:"+daySelected+"-"+monthSelected+"-"+yearSelected);
            strDate = sdf.parse(daySelected+"-"+monthSelected+"-"+yearSelected);
            fechaM = sdf1.parse(lastModified);
            Log.d("checkDate","FechaModified:"+fechaModificacion.getDay()+"/"+fechaModificacion.getMonth()+"/"+fechaModificacion.getYear());

            Log.d("checkDate","fechaM:"+fechaM);
            Log.d("checkDate","strDate:"+strDate);

        }catch(Exception e){
            return false;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(strDate);
        Calendar c1 = Calendar.getInstance();
        c1.setTime(fechaM);
        Log.d("checkDate","tiempos:"+c.getTimeInMillis()+"-"+c1.getTimeInMillis());
        if(c.getTimeInMillis()==c1.getTimeInMillis()){
            return true;
        }

        return false;
    }

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

                if(elemento.get("type").equals("Point")){
                    JSONObject properties = new JSONObject();
                    if(!hito.isNull("properties")){
                        properties = hito.getJSONObject("properties");
                    }
                    JSONArray coordenadas = elemento.getJSONArray("coordinates");
                    rec.addHito(new GeoPoint(coordenadas.getDouble(1),coordenadas.getDouble(0),coordenadas.getDouble(2)),properties);

                }else if(elemento.get("type").equals("LineString")){
                    JSONArray coordenadas = elemento.getJSONArray("coordinates");
                    //int colors[] = {R.color.colorRed,R.color.colorBlue,R.color.colorGreen,R.color.colorYellow};
                    int colors[] = {Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW};
                    for(int j = 0;j< coordenadas.length()-1;j++){
                        Polyline linea = new Polyline();
                        JSONArray punto1 = coordenadas.getJSONArray(j);
                        JSONArray punto2 = coordenadas.getJSONArray(j+1);
                        List<GeoPoint> geoPoints = new ArrayList<>();
                        GeoPoint p1 = new GeoPoint(punto1.getDouble(1),punto1.getDouble(0),punto1.getDouble(2));
                        GeoPoint p2 = new GeoPoint(new GeoPoint(punto2.getDouble(1),punto2.getDouble(0),punto2.getDouble(2)));
                        geoPoints.add(p1);
                        geoPoints.add(p2);
                        linea.setPoints(geoPoints);
                        rec.addLine(linea);
                    }
                }

            }
        }catch(JSONException e){
            Toast.makeText(getBaseContext(), "No se pudo Parsear el json", Toast.LENGTH_LONG).show();
            System.out.println(e.toString());
        }catch(Exception ex){
            Toast.makeText(getBaseContext(), "Existe un problema con su json", Toast.LENGTH_LONG).show();
            System.out.println(ex.toString());
        }
        return rec;
    }

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
