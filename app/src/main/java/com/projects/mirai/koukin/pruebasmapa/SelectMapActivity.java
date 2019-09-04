package com.projects.mirai.koukin.pruebasmapa;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.projects.mirai.koukin.pruebasmapa.HelperClass.MapArrayAdapter;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.MyHelperSql;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.SavedMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnCheckedChanged;

/**
 * Actividad donde se selecciona el mapa que se cargará en la actividad GeoreferenciarActivity
 * @author mauricio, luis
 * @version 1.0
 */
public class SelectMapActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{

    ListView lista;

    public int selectedItem = -1;
    private double clatitude = -2.146214;
    private double clongitude = -79.966363;
    private double zlevel = 15;

    @BindView(R.id.self_ref)
    Switch self_ref;

    @BindView(R.id.utm_ref)
    Switch utm_ref;

    MapArrayAdapter adapter;

    ArrayList<SavedMap>mapsRemove;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPreferences();
        setContentView(R.layout.activity_select_map);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");

        MyHelperSql myHelperDB = new MyHelperSql(SelectMapActivity.this,"MAPDB",null,1);

        SQLiteDatabase mSqliteDB = myHelperDB.getWritableDatabase();

        ArrayList<SavedMap> mapas = myHelperDB.loadMapsDb(mSqliteDB,this);

        lista = (ListView) findViewById(R.id.listview1);

        mapsRemove = new ArrayList<>();

        for(SavedMap map: mapas){
            try{

                Date strDate = sdf.parse(map.getDate().split("\\|")[0]);

                Calendar c = Calendar.getInstance();

                c.setTime(strDate); // Now use map date.

                c.add(Calendar.DATE, 14); // Adding 2 weeks

                String output = sdf.format(c.getTime());

                if (System.currentTimeMillis() > c.getTimeInMillis()) {
                    //Expirado

                    boolean eliminado = myHelperDB.deleteMap(mSqliteDB,this,map.getId());

                    Toast.makeText(this,"Mapa ha expirado:"+map.getId(),Toast.LENGTH_LONG).show();

                    mapsRemove.add(map);
                }
            }catch(Exception e){

                Toast.makeText(this,"Mapa expirado no se pudo eliminar:"+map.getId(),Toast.LENGTH_LONG).show();

                System.out.println("Error:"+e.toString());

            }
        }
        for(SavedMap map : mapsRemove){

            mapas.remove(map);
        }


        mapas.add(0,new SavedMap(-1,"Mapa General",-clatitude,clongitude,zlevel,"--/--/--"));
        adapter = new MapArrayAdapter(this,R.layout.list_view_item,mapas);

        lista.setAdapter(adapter);

        lista.setOnItemClickListener(new OnItemClick());
        lista.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        this.self_ref = (Switch) findViewById(R.id.self_ref);
        this.utm_ref = (Switch) findViewById(R.id.utm_ref);
        self_ref.setOnCheckedChangeListener(this);
        utm_ref.setOnCheckedChangeListener(this);
    }

    /**
     * Método que carga la actividad GeoreferenciarActivity
     * @result redirección a nueva activity con el mapa seleccionado
     */
    public void loadMapActivity(){
        Intent intent = new Intent(getBaseContext(), GeoreferenciarActivity.class);
        intent.putExtra("selectedMap", adapter.getMap(selectedItem).toString());
        startActivity(intent);
    }


    /**
     * Clase que sobreescribe el método onItemClick para customizar la carga del mapa en la
     * actividad GeoreferenciarActivity
     * @author mauricio, luis
     * @version 1.0
     */
    public class OnItemClick implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            selectedItem = position;
            loadMapActivity();
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Switch self = (Switch)findViewById(R.id.self_ref);
        boolean is_self = buttonView.getId()==self.getId();
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (isChecked && is_self){
            LinearLayout view = (LinearLayout) View.inflate(this,R.layout.self_reference,null);
            final EditText norte_ref = (EditText) view.findViewById(R.id.norte_ref);
            final EditText este_ref = (EditText) view.findViewById(R.id.este_ref);
            final EditText lon_ref = (EditText) view.findViewById(R.id.long_ref);
            final EditText lat_ref = (EditText) view.findViewById(R.id.lat_ref);
            utm_ref.setChecked(false);
            rangeChecker(lon_ref,-180,180);
            rangeChecker(lat_ref,-90,90);

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Ubíquese en la posición del punto cero");
            alert.setView(view);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    boolean lonb = checkRange(lon_ref.getText().toString(),-180,180);
                    boolean latb = checkRange(lat_ref.getText().toString(),-90,90);

                    if(lonb && latb){
                        float norte = Float.valueOf(norte_ref.getText().toString());
                        float este =   Float.valueOf(este_ref.getText().toString());
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putFloat("norte",norte);
                        editor.putFloat("este",este);
                        editor.putBoolean("utm",false);
                        editor.putFloat("long", Float.valueOf(lon_ref.getText().toString()));
                        editor.putFloat("lat", Float.valueOf(lat_ref.getText().toString()));
                        editor.commit();
                    }else{
                        Toast.makeText(SelectMapActivity.super.getBaseContext(), "¡Valores de longitud y latitud del punto cero fuera de rango, no se almacenaron!", Toast.LENGTH_SHORT).show();
                    }

                }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //Put actions for CANCEL button here, or leave in blank
                    }
                });
            alert.show();
        } else if (isChecked && !is_self) {
            self_ref.setChecked(false);
        }
    }

    /**
     * Method to create default preferences
     */
    private void createPreferences(){
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("norte",1000f);
        editor.putFloat("este", 1000f);
        editor.putBoolean("utm", true);
        editor.commit();
    }

    /**
     * Method used to check if the range of latitude and longitude on a edittext element are on range
     * @param min minimum value on latitude is -90 and longitude -180
     * @param max maximum value on latitude is  90 and longitude  180
     */
    private boolean rangeChecker(final EditText texto, final float min, final float max){


        texto.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(isNumeric(s.toString())){
                    float value = Float.valueOf(s.toString());
                    if(value < min || value > max ){
                        Toast.makeText(SelectMapActivity.super.getBaseContext(), "¡Valores fuera de rango!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        return true;

    }


    private boolean checkRange(String number, float min, float max){
        if(isNumeric(number)){
            float value = Float.valueOf(number);
            if(value > min && value < max ){
                Toast.makeText(SelectMapActivity.super.getBaseContext(), "¡Configuración correcta!", Toast.LENGTH_SHORT).show();
                return true;
            }else{
                Toast.makeText(SelectMapActivity.super.getBaseContext(), "¡Valores de latitud y longitud fuera de rango!", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        Toast.makeText(SelectMapActivity.super.getBaseContext(), "¡Valor para latitud y longitud incorrectos!", Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * Private method to check wether or not an input is a number type
     * @param strNum
     * @return
     */
    private static boolean isNumeric(String strNum) {
        try {
            double d = Float.parseFloat(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

}
