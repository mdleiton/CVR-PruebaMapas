package com.projects.mirai.koukin.pruebasmapa;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.projects.mirai.koukin.pruebasmapa.HelperClass.FileUtils;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.MyHelperSql;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Permissions;

public class MenuPrincipalActivity extends AppCompatActivity {

    private ImageButton btn_descargar_mapa, btn_cargar_puntos, btn_georeferenciar, btn_config, btn_enviar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        MyHelperSql myHelperDB = new MyHelperSql(MenuPrincipalActivity.this,"MAPDB",null,1);
        SQLiteDatabase mSqliteDB = myHelperDB.getWritableDatabase();
        //myHelperDB.loadMapsDb(mSqliteDB,this);

        Permissions.verifyStoragePermissions(this);
        Permissions.verifyLocationPermission(this);

        btn_descargar_mapa = findViewById(R.id.btn_mapa);
        btn_cargar_puntos = findViewById(R.id.btn_coord);
        btn_georeferenciar = findViewById(R.id.btn_recorridos);
        btn_config = findViewById(R.id.btn_config);
        btn_enviar = findViewById(R.id.btn_enviar);



        btn_descargar_mapa.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Permissions.verifyLocationPermission(MenuPrincipalActivity.this);
                Permissions.verifyStoragePermissions(MenuPrincipalActivity.this);
                Intent i = new Intent(MenuPrincipalActivity.this, Descargar_Mapa.class);
                startActivity(i);
            }
        });

        btn_cargar_puntos.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Permissions.verifyLocationPermission(MenuPrincipalActivity.this);
                Permissions.verifyStoragePermissions(MenuPrincipalActivity.this);
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Elige un Archivo"), 123);
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

                AlertDialog.Builder alert = new AlertDialog.Builder(MenuPrincipalActivity.this);
                alert.setTitle("Funcion en Desarrollo");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
                alert.show();
            }
        });









        //DEPRECATED
        /*
        btn_descargar_mapa.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Permissions.verifyLocationPermission(MenuPrincipalActivity.this);
                Permissions.verifyStoragePermissions(MenuPrincipalActivity.this);
                Intent i = new Intent(MenuPrincipalActivity.this, MapaActivity.class);
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
                Intent i = new Intent(MenuPrincipalActivity.this, Descargar_Mapa.class);
                startActivity(i);

                AlertDialog.Builder alert = new AlertDialog.Builder(MenuPrincipalActivity.this);
                alert.setTitle("Funcion en Desarrollo");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
                alert.show();
        }
    });*/




    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            String path = FileUtils.getPath(this, selectedfile);
            System.out.println("New Path:" + path);

            if (path.endsWith(".json")) {
                Intent intent = new Intent(getBaseContext(), MapaActivity.class);
                intent.putExtra("selectedFile", path);
                startActivity(intent);
            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Debe elegir un archivo con extension .json");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
                alert.show();
            }
        } else if (requestCode == 1234 && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            String path = FileUtils.getPath(this, selectedfile);
            System.out.println("New Path:" + path);

            if (path.endsWith("C.json") || path.endsWith("P.json")) {
                Intent intent = new Intent(getBaseContext(), MapaActivity.class);
                intent.putExtra("selectedFile", path);
                startActivity(intent);
            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Debe elegir un archivo GeoJson");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                alert.show();

            }

        }

    }
}