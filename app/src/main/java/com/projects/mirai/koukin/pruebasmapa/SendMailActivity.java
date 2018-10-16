package com.projects.mirai.koukin.pruebasmapa;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SendMailActivity extends AppCompatActivity {
    Button btn_enviar;
    AutoCompleteTextView email_auto;
    CalendarView calendarView;
    private int daySelected,monthSelected,yearSelected;
    SimpleDateFormat dayFormat,monthFormat,yearFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_mail);
        btn_enviar = (Button) findViewById(R.id.btn_enviar);
        email_auto = (AutoCompleteTextView) findViewById(R.id.email_auto);
        calendarView = (CalendarView) findViewById(R.id.calendarView);
        Date fecha = new Date(calendarView.getDate());
        dayFormat = new SimpleDateFormat("dd");
        monthFormat = new SimpleDateFormat("MM");
        yearFormat = new SimpleDateFormat("yyyy");


        daySelected=Integer.parseInt(dayFormat.format(calendarView.getDate()));

        monthSelected=Integer.parseInt(monthFormat.format(calendarView.getDate()));
        yearSelected=Integer.parseInt(yearFormat.format(calendarView.getDate()));


        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {

            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month,
                                            int dayOfMonth) {
                daySelected=dayOfMonth;
                yearSelected=year;
                monthSelected=month+1;
                Toast.makeText(SendMailActivity.this,daySelected+"/"+monthSelected+"/"+yearSelected,Toast.LENGTH_LONG).show();
            }
        });

        btn_enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateFields()){

                    String filename="RecorridCVR.json";
                    File filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename);
                    Uri path = Uri.fromFile(filelocation);


                    Intent intent = new Intent(Intent.ACTION_SEND);

                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email_auto.getText().toString()});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Jordada del :"+daySelected+"/"+monthSelected+"/"+yearSelected);
                    intent.putExtra(Intent.EXTRA_TEXT, "Aqui va el archivo de la jornada adjunto. Saludos Cordiales.");

                    intent.putExtra(Intent.EXTRA_STREAM, path);

                    intent.setType("message/rfc822");

                    startActivity(Intent.createChooser(intent, "Elija aplicación para enviar mail :"));
                }


            }
        });

    }



    public boolean validateFields(){
        if(email_auto.getText().toString().matches("([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)")){
            Toast.makeText(this,"Email Valido",Toast.LENGTH_SHORT).show();
            return true;
        }else{
            AlertDialog.Builder alert = new AlertDialog.Builder(SendMailActivity.this);
            alert.setTitle("Ingrese un correo electrónico valido.");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            alert.show();
        }
        return false;
    }
}
