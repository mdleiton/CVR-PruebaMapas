package com.projects.mirai.koukin.pruebasmapa;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.projects.mirai.koukin.pruebasmapa.HelperClass.MapArrayAdapter;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.MyHelperSql;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.SavedMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Actividad donde se selecciona el mapa que se cargará en la actividad GeoreferenciarActivity
 * @author mauricio, luis
 * @version 1.0
 */
public class SelectMapActivity extends AppCompatActivity {

    ListView lista;

    public int selectedItem = -1;
    private double clatitude = -2.146214;
    private double clongitude = -79.966363;
    private double zlevel = 15;

    MapArrayAdapter adapter;

    ArrayList<SavedMap>mapsRemove;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

}
