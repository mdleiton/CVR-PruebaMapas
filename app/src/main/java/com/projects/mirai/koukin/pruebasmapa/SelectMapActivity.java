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

public class SelectMapActivity extends AppCompatActivity {
    ListView lista;
    public int selectedItem = -1;
    MapArrayAdapter adapter;
    String[] valores = {"Holanda","España","Ecuador","Francia","USA"};
    ArrayList<SavedMap>mapsRemove;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_map);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        MyHelperSql myHelperDB = new MyHelperSql(SelectMapActivity.this,"MAPDB",null,1);
        SQLiteDatabase mSqliteDB = myHelperDB.getWritableDatabase();
        ArrayList<SavedMap> mapas =myHelperDB.loadMapsDb(mSqliteDB,this);

        lista = (ListView) findViewById(R.id.listview1);
        mapsRemove = new ArrayList<>();
        for(SavedMap map: mapas){
            try{
                System.out.println("fecha:"+map.getDate().split("\\|")[0]);
                Date strDate = sdf.parse(map.getDate().split("\\|")[0]);
                Calendar c = Calendar.getInstance();
                c.setTime(strDate); // Now use map date.
                c.add(Calendar.DATE, 14); // Adding 2 weeks
                String output = sdf.format(c.getTime());
                System.out.println(output);
                if (System.currentTimeMillis() > c.getTimeInMillis()) {
                    //Expirado
                    boolean eliminado = myHelperDB.deleteMap(mSqliteDB,this,map.getId());
                    System.out.println("Mapa eliminado:"+map.getId()+" -"+eliminado);
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


        mapas.add(0,new SavedMap(-1,"Mapa General",-2.146214,-79.966363,15,"--/--/--"));
        adapter = new MapArrayAdapter(this,R.layout.list_view_item,mapas);



        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1,valores);
        lista.setAdapter(adapter);

        lista.setOnItemClickListener(new OnItemClick());
        lista.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


    }





    public void loadMapActivity(){
        Intent intent = new Intent(getBaseContext(), GeoreferenciarActivity.class);
        intent.putExtra("selectedMap", adapter.getMap(selectedItem).toString());
        startActivity(intent);
    }


    public class OnItemClick implements AdapterView.OnItemClickListener{


        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            selectedItem = position;
            loadMapActivity();
        }

    }

}
