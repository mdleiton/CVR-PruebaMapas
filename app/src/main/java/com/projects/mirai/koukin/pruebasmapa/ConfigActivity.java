package com.projects.mirai.koukin.pruebasmapa;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.google.android.gms.maps.MapFragment;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.SBPDriverJ2XX;
import com.projects.mirai.koukin.pruebasmapa.HelperClass.Utils;
import com.swiftnav.sbp.SBPMessage;
import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.loggers.JSONLogger;
import com.swiftnav.sbp.logging.MsgLog;
import com.swiftnav.sbp.navigation.MsgPosLLH;
import com.swiftnav.sbp.observation.MsgObs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;

public class ConfigActivity extends AppCompatActivity {

    /* piksi */
    String TAG = "PiksiDroid";
    String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    SBPHandler piksiHandler;
    SBPDriverJ2XX piksiDriver;



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

        /* piksi */
        System.out.println("1");
        UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        System.out.println("2");
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        System.out.println("3");
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        System.out.println("4");
        registerReceiver(mUsbReceiver, filter);
        System.out.println("5");
        filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        System.out.println("6");
        registerReceiver(mUsbReceiverDisconnect, filter);

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

        for (UsbDevice device : deviceList.values()) {
            if ((device.getVendorId() == Utils.PIKSI_VID) && (device.getProductId() == Utils.PIKSI_PID))
                if (!mUsbManager.hasPermission(device)){
                    mUsbManager.requestPermission(device, mPermissionIntent);
                    System.out.println("sin permiso conectado");
                }
                else {
                   // ((EditText) findViewById(R.id.console)).setText("");
                    System.out.println("con permisos");
                    piksiConnected(device);
                }
        }



    }

    private void piksiConnected(UsbDevice usbdev) {
        if (piksiDriver != null) {
            piksiHandler.stop();
            piksiDriver.close();
            piksiDriver = null;
        }
        try {
            System.out.println("intento serial");
            piksiDriver = new SBPDriverJ2XX(this, usbdev);
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
            return;
        }
        System.out.println("creando piksi");
        piksiHandler = new SBPHandler(piksiDriver);

        try {
            File logfile = new File(getExternalFilesDir("logs"), "logfile");
            OutputStream logstream = new FileOutputStream(logfile);
            piksiHandler.addCallback(new JSONLogger(logstream));
        } catch (Exception e) {
            Log.e(TAG, "Error opening JSON log file: " + e.toString());
        }

        Log.d(TAG, "All ready to go...");
        System.out.println("iniciando piksi");

        piksiHandler.addCallback(MsgLog.TYPE, new SBPCallback() {
            @Override
            public void receiveCallback(SBPMessage msg) {
                if (piksiHandler == null) {
                    Log.e(TAG, "No piksi to send to!");
                    return;
                }
                MsgLog msg_ = (MsgLog) msg;
                System.out.print(msg_.text + ".\n");
                //piksiHandler.send(msg);
            }
         });

        piksiHandler.addCallback(MsgPosLLH.TYPE, new SBPCallback() {
            @Override
            public void receiveCallback(SBPMessage msg) {
                if (piksiHandler == null) {
                    Log.e(TAG, "No piksi to send to!");
                    return;
                }
                MsgPosLLH msg__ = (MsgPosLLH)msg;
                System.out.printf(
                        "lat[deg]: %f, lon[deg]: %f, ellipsoid alt[m]: %f, horizontal accuracy[m]: %f, vertical_accuracy[m]: %f, n_sats: %d .\n",
                        msg__.lat,
                        msg__.lon,
                        msg__.height,
                        msg__.h_accuracy / 1000.0,
                        msg__.v_accuracy / 1000.0,
                        msg__.n_sats);
                //piksiHandler.send(msg);
            }
        });
        piksiHandler.start();

    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        //((EditText) findViewById(R.id.console)).setText("");
                        System.out.println("algo conectado");
                        piksiConnected(device);
                    }
                } else {
                    System.out.println("nada conectado sin permiso");
                    Log.e(TAG, "Permission denied for device " + device);
                }
            }
        }
    };

    BroadcastReceiver mUsbReceiverDisconnect = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.e(TAG, "Device disconnected!");
                    System.out.println("Device disconnected!");
                    if (piksiDriver != null) {
                        piksiHandler.stop();
                        piksiDriver.close();
                        piksiDriver = null;
                        System.out.println("Piksi not connected!");
                        //((EditText) findViewById(R.id.console)).setText("Piksi not connected!");
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbReceiverDisconnect);
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
