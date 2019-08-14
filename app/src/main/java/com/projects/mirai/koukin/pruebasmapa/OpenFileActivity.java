package com.projects.mirai.koukin.pruebasmapa;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.projects.mirai.koukin.pruebasmapa.HelperClass.Archivo;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.ArchivosArrayAdapter;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Constants;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.FechaJornada;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.JornadasArrayAdapter;

import java.io.File;
import java.util.ArrayList;

/**
 * Activity donde se escogen los archivos para subir al dar clic en la opción subir archivos
 * en la Activity Georeferenciar
 * @author mauricio, manuel, luis
 * @version 1.0
 */

public class OpenFileActivity extends AppCompatActivity {

    ListView lista;
    public int selectedItem = -1;
    ArchivosArrayAdapter adapter;
    ArrayList<Archivo> archivos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_file);
        lista =findViewById(R.id.list_view_archivos);

        this.encontrarArchivos();

        if(archivos.size()==0){
            archivos.add(new Archivo("No existen Archivos Creados aun.","01-01-1999"));
        }
        adapter = new ArchivosArrayAdapter(this,R.layout.list_view_item_archivos,archivos);

        lista.setAdapter(adapter);

        lista.setOnItemClickListener(new OnItemClick());
        lista.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

    }

    public class OnItemClick implements AdapterView.OnItemClickListener{


        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            selectedItem = position;
            String result = archivos.get(selectedItem).getNombre();
            System.out.println(result);
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result",result);
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
            //loadInsertEmailActivity();
        }

    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, returnIntent);
        finish();

    }

    /**
     * Método que encuentra los Archivos en pathMapaArq y adjunta los archivos en
     * un ArrayList llamado archivos
     */
    public void encontrarArchivos(){
        archivos = new ArrayList<>();

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
                String[] elementos= archivo.getName().split("\\|");
                Log.d("generateFile","Elemento[1]:"+elementos[1]);
                archivos.add(new Archivo(archivo.getName()));
            }

        }

    }



}
