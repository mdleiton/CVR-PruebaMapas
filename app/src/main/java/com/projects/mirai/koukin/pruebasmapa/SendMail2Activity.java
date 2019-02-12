package com.projects.mirai.koukin.pruebasmapa;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.projects.mirai.koukin.pruebasmapa.HelperClass.Constants;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.FechaJornada;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.JornadasArrayAdapter;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.RecorridoGuardar;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class SendMail2Activity extends AppCompatActivity {
    ListView lista;
    public int selectedItem = -1;
    JornadasArrayAdapter adapter;
    ArrayList<FechaJornada> jornadas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_mail2);

        lista = (ListView) findViewById(R.id.list_view_jornadas);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        jornadas = encontrarJornadas();
        if(jornadas.size()==0){
            jornadas.add(new FechaJornada("No existen Jornadas.","01-01-1999"));
        }
        adapter = new JornadasArrayAdapter(this,R.layout.list_view_item_jornadas,jornadas);

        lista.setAdapter(adapter);

        lista.setOnItemClickListener(new OnItemClick());
        lista.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

    }



    public class OnItemClick implements AdapterView.OnItemClickListener{


        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            selectedItem = position;
            loadInsertEmailActivity();
        }

    }

    public void loadInsertEmailActivity(){
        Intent intent = new Intent(getBaseContext(), SelectMailActivity.class);
        intent.putExtra("Jornada", adapter.getJornada(selectedItem).getFecha());
        startActivity(intent);
    }

    public ArrayList<FechaJornada> encontrarJornadas(){
        ArrayList<FechaJornada> jornadas = new ArrayList<>();




        String path = Constants.pathMapaArq;
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        Log.d("Files", "Size: "+ files.length);
        File archivo;
        int contador =1;

        for (int i = 0; i < files.length; i++)
        {
            archivo = files[i];
            if(archivo.isFile() && archivo.getName().endsWith(".json")){
                Log.d("Files", "FileName:" + archivo.getName());
                String[] elementos= archivo.getName().split("\\|");
                Log.d("generateFile","Elemento[1]:"+elementos[1]);
                if(!existeJornada(jornadas,elementos[1])){
                    jornadas.add(new FechaJornada("Jornada#"+contador,elementos[1]));
                    contador+=1;
                }

            }

        }
        return jornadas;
    }


    public boolean existeJornada(ArrayList<FechaJornada> lista, String fecha){
        boolean existe = false;

        for(FechaJornada jornada:lista){
            if(jornada.getFecha().equals(fecha)){
                existe=true;
            }
        }
        return existe;
    }
}
