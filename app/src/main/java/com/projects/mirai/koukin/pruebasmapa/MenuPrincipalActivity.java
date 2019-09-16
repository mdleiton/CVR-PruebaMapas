package com.projects.mirai.koukin.pruebasmapa;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.projects.mirai.koukin.pruebasmapa.HelperClass.FileUtils;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.MyHelperSql;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Permissions;

import java.io.File;
import java.util.ArrayList;

/**
 *  Actividad usada para mostrar el menu principal y recorrer por las demas actividades de la
 *  aplicación
 * @author luibasantes, mleiton
 * @version 1.0
 * @since 1.0
 */

public class MenuPrincipalActivity extends AppCompatActivity {

    private ImageButton btn_descargar_mapa, btn_cargar_puntos, btn_georeferenciar, btn_config, btn_enviar;

    private String path;

    private int OLD_FILE_ACTIVITY = 1234;

    private int FILE_ACTIVITY = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        MyHelperSql myHelperDB = new MyHelperSql(MenuPrincipalActivity.this,"MAPDB",null,1);
        SQLiteDatabase mSqliteDB = myHelperDB.getWritableDatabase();
        //myHelperDB.loadMapsDb(mSqliteDB,this);

        Permissions.verifyStoragePermissions(this);
        Permissions.verifyLocationPermission(this);


        path = Environment.getExternalStorageDirectory() + "/MapasArq";


        btn_descargar_mapa = findViewById(R.id.btn_mapa);
        btn_cargar_puntos = findViewById(R.id.btn_coord);
        btn_georeferenciar = findViewById(R.id.btn_recorridos);
        btn_config = findViewById(R.id.btn_config);
        btn_enviar = findViewById(R.id.btn_enviar);



        btn_descargar_mapa.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Permissions.verifyLocationPermission(MenuPrincipalActivity.this);
                //Permissions.verifyStoragePermissions(MenuPrincipalActivity.this);
                Intent i = new Intent(MenuPrincipalActivity.this, Descargar_Mapa.class);
                startActivity(i);
            }
        });

        btn_cargar_puntos.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Permissions.verifyLocationPermission(MenuPrincipalActivity.this);
                Permissions.verifyStoragePermissions(MenuPrincipalActivity.this);


                Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath()
                        +  File.separator + "MapasArq" + File.separator);
                Intent intent = new Intent()
                        .setAction(Intent.ACTION_GET_CONTENT);
                intent.setDataAndType(uri,"*/*");

                startActivityForResult(Intent.createChooser(intent, "Elige un Archivo"), FILE_ACTIVITY);
            }
        });


        btn_georeferenciar.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Permissions.verifyLocationPermission(MenuPrincipalActivity.this);
                Permissions.verifyStoragePermissions(MenuPrincipalActivity.this);
                Intent i = new Intent(MenuPrincipalActivity.this, SelectMapActivity.class);
                startActivity(i);
            }
        });


        btn_config.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Intent i = new Intent(MenuPrincipalActivity.this, ConfigActivity.class);
                startActivity(i);
            }
        });

        btn_enviar.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Permissions.verifyLocationPermission(MenuPrincipalActivity.this);
                Permissions.verifyStoragePermissions(MenuPrincipalActivity.this);

                Intent i = new Intent(MenuPrincipalActivity.this,SendMail2Activity.class);
                startActivity(i);
            }
        });


        File f = new File(path);
        //System.out.println("Existe:"+f.exists());
        //System.out.println("Es Directorio:"+f.isDirectory());
        if(!(f.exists() && f.isDirectory())){
            //System.out.println(f.mkdirs());
            File f1 = new File(path+File.separator+"Temp"+File.separator);
            f1.mkdirs();
            File f2 = new File(path+File.separator+"Jornadas"+File.separator);
            f2.mkdirs();

            Toast.makeText(this,"Dependencias Creadas",Toast.LENGTH_SHORT).show();
        }

    }


    /**
     * Metodo que llama una actividad dependiendo del código de request que se recibe
     * de parte de los Intents
     * @param requestCode   el código de request, en este método es devuelto para subir archivos
     *                      se los encuentra en GeoreferenciarActivity
     * @param data          contiene el archivo que se quiere almacenar
     */
    private void onRequestedCodes(int requestCode, Intent data){

        Uri selectedfile = data.getData(); //The uri with the location of the file
        String path = FileUtils.getPath(this, selectedfile);

        String err_msg = "Debe elegir un archivo GeoJson";

        if(requestCode == FILE_ACTIVITY){
            err_msg = "Debe elegir un archivo con extension .json";
        }

        if(requestCode == FILE_ACTIVITY || requestCode == OLD_FILE_ACTIVITY){

            if(path.endsWith(".json") || path.endsWith("C.json") || path.endsWith("P.json")){//nullpointer
                Intent intent = new Intent(getBaseContext(), MapaActivity.class);
                intent.putExtra("selectedFile", path);
                startActivity(intent);
            }else{
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle(err_msg);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                alert.show();
            }

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            onRequestedCodes(requestCode,data);
        }

    }
}