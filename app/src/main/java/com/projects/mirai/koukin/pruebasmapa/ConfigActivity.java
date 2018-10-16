package com.projects.mirai.koukin.pruebasmapa;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

public class ConfigActivity extends AppCompatActivity {


    EditText txt_email,txt_ftp;
    RadioButton rb_gps,rb_rtk;
    Button guardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);


        txt_email = (EditText) findViewById(R.id.txt_email);
        txt_ftp = (EditText) findViewById(R.id.txt_ftp);
        rb_gps = (RadioButton) findViewById(R.id.rb_gps);
        rb_rtk = (RadioButton) findViewById(R.id.rb_rtk);

        guardar = (Button) findViewById(R.id.button);

        guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });



        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String email = sharedPref.getString("email", "");
        String ftp = sharedPref.getString("ftp","");
        //True es para gps y False para RTK
        Boolean gps = sharedPref.getBoolean("gps",false);


        txt_email.setText(email);
        txt_ftp.setText(ftp);
        if(gps){
            rb_gps.setChecked(true);
        }else{
            rb_rtk.setChecked(true);
        }

    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        AlertDialog.Builder alert = new AlertDialog.Builder(ConfigActivity.this);
        alert.setTitle("Desea Guardar las Configuraciones?");

        alert.setPositiveButton("Si", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("email", txt_email.getText().toString());
                editor.putString("ftp", txt_ftp.getText().toString());
                editor.putBoolean("gps", rb_gps.isChecked());
                editor.commit();
                ConfigActivity.super.onBackPressed();
            }
        });
        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                ConfigActivity.super.onBackPressed();

            }
        });
        alert.show();


    }
}
