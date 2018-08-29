package com.projects.mirai.koukin.pruebasmapa;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class MenuPrincipalActivity extends AppCompatActivity {

    private ImageButton btn_mapa,btn_coord,btn_recorridos,btn_config,btn_enviar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);
        btn_mapa = findViewById(R.id.btn_mapa);
        btn_coord = findViewById(R.id.btn_coord);
        btn_recorridos = findViewById(R.id.btn_recorridos);
        btn_config = findViewById(R.id.btn_config);
        btn_enviar = findViewById(R.id.btn_enviar);



        btn_mapa.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Intent i = new Intent(MenuPrincipalActivity.this,MapaActivity.class);
                startActivity(i);
            }
        });


        btn_coord.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Elige un Archivo"), 123);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==123 && resultCode==RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            System.out.println(data.getData().getPath());

            if(selectedfile.getPath().endsWith(".json")){
                Toast.makeText(getApplicationContext(), selectedfile.getPath(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getBaseContext(), MapaActivity.class);
                intent.putExtra("selectedFile", selectedfile);
                startActivity(intent);
            }else{
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Debe elegir un archivo con extension .json");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
                alert.show();
            }
        }
    }
}
