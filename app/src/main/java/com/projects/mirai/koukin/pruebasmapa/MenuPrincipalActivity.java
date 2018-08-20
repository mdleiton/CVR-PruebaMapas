package com.projects.mirai.koukin.pruebasmapa;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

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
    }
}
