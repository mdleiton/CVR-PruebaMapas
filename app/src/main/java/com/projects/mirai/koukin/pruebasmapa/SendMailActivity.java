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

import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.RecorridoGuardar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SendMailActivity extends AppCompatActivity {
    Button btn_enviar;
    AutoCompleteTextView email_auto;
    CalendarView calendarView;
    private int daySelected,monthSelected,yearSelected;
    SimpleDateFormat dayFormat,monthFormat,yearFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_mail);
        btn_enviar = (Button) findViewById(R.id.btn_enviar);
        email_auto = (AutoCompleteTextView) findViewById(R.id.email_auto);
        calendarView = (CalendarView) findViewById(R.id.calendarView);
        Date fecha = new Date(calendarView.getDate());
        dayFormat = new SimpleDateFormat("dd");
        monthFormat = new SimpleDateFormat("MM");
        yearFormat = new SimpleDateFormat("yyyy");


        daySelected=Integer.parseInt(dayFormat.format(calendarView.getDate()));

        monthSelected=Integer.parseInt(monthFormat.format(calendarView.getDate()));
        yearSelected=Integer.parseInt(yearFormat.format(calendarView.getDate()));


        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {

            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month,
                                            int dayOfMonth) {
                daySelected=dayOfMonth;
                yearSelected=year;
                monthSelected=month+1;
                Toast.makeText(SendMailActivity.this,daySelected+"/"+monthSelected+"/"+yearSelected,Toast.LENGTH_LONG).show();
            }
        });

        btn_enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateFields()){
                    String filename="Jornada_"+daySelected+"/"+monthSelected+"/"+yearSelected+".json";
                    generateFile(filename);







                    File filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename);
                    Uri path = Uri.fromFile(filelocation);


                    Intent intent = new Intent(Intent.ACTION_SEND);

                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email_auto.getText().toString()});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Jordada del :"+daySelected+"/"+monthSelected+"/"+yearSelected);
                    intent.putExtra(Intent.EXTRA_TEXT, "Aqui va el archivo de la jornada adjunto. Saludos Cordiales.");

                    intent.putExtra(Intent.EXTRA_STREAM, path);

                    intent.setType("message/rfc822");

                    startActivity(Intent.createChooser(intent, "Elija aplicación para enviar mail :"));
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


    public void generateFile(String filename){
        String path = Environment.getExternalStorageDirectory().toString();
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        Log.d("Files", "Size: "+ files.length);
        File archivo;
        ArrayList <RecorridoGuardar> recorridos = new ArrayList<>();
        for (int i = 0; i < files.length; i++)
        {
            archivo = files[i];
            if(archivo.isFile() && archivo.getName().endsWith(".json") && !archivo.getName().startsWith("Temp")){
                Log.d("Files", "FileName:" + archivo.getName());
                if(checkDate(archivo.lastModified())){
                    RecorridoGuardar rec = loadFileToRec(archivo);
                    recorridos.add(rec);


                }

            }

        }

    }
    public boolean checkDate(long lastModified){
        Date fechaModificacion = new Date(lastModified);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        Date strDate;
        try{
            strDate = sdf.parse(daySelected+"/"+monthSelected+"/"+yearSelected);
        }catch(Exception e){
            return false;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(strDate); // Now use map date.
        Calendar c1 = Calendar.getInstance();
        c1.setTime(fechaModificacion);
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
                JSONObject elemento = features.getJSONObject(i).getJSONObject("geometry");
                System.out.println("Elemento"+i+":"+elemento);

                if(elemento.get("type").equals("Point")){
                    JSONArray coordenadas = elemento.getJSONArray("coordinates");
                    rec.addPoint(new GeoPoint(coordenadas.getDouble(1),coordenadas.getDouble(0)));
                }else if(elemento.get("type").equals("LineString")){
                    JSONArray coordenadas = elemento.getJSONArray("coordinates");
                    //int colors[] = {R.color.colorRed,R.color.colorBlue,R.color.colorGreen,R.color.colorYellow};
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
