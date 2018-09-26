package com.projects.mirai.koukin.pruebasmapa;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.projects.mirai.koukin.pruebasmapa.HelperClass.MapArrayAdapter;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.MyHelperSql;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.SavedMap;

import java.util.ArrayList;
import java.util.List;

public class SelectMapActivity extends AppCompatActivity {
    ListView lista;
    public int selectedItem = -1;
    MapArrayAdapter adapter;
    String[] valores = {"Holanda","España","Ecuador","Francia","USA"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_map);
        MyHelperSql myHelperDB = new MyHelperSql(SelectMapActivity.this,"MAPDB",null,1);
        SQLiteDatabase mSqliteDB = myHelperDB.getWritableDatabase();
        ArrayList<SavedMap> mapas =myHelperDB.loadMapsDb(mSqliteDB,this);
        lista = (ListView) findViewById(R.id.listview1);

        adapter = new MapArrayAdapter(this,R.layout.list_view_item,mapas);



        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1,valores);
        lista.setAdapter(adapter);

        lista.setOnItemClickListener(new OnItemClick());
        lista.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


    }





    public void loadMapActivity(){
        Intent intent = new Intent(getBaseContext(), MapaActivity.class);
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
