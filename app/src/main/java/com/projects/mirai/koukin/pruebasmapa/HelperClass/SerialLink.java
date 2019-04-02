package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.swiftnav.sbp.SBPMessage;
import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.loggers.JSONLogger;
import com.swiftnav.sbp.logging.MsgLog;
import com.swiftnav.sbp.navigation.MsgPosLLH;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class SerialLink {
    String TAG = "PiksiCVR";
    String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    SBPHandler piksiHandler;
    SBPDriverJ2XX piksiDriver;
    Context context;
    private SBPHandler handler;
    private String[] fix_type = new String[8];
    private Queue<Double> lat_queue = new LinkedList<>();
    private Queue<Double> lon_queue = new LinkedList<>();

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
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
                }
            }
        }
        }
    };

    public SerialLink(Context context) {
        this.context = context;
        detect_piksi();
    }

    private void detect_piksi() {
        UsbManager mUsbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);
        filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(mUsbReceiverDisconnect, filter);

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

        for (UsbDevice device : deviceList.values()) {
            if ((device.getVendorId() == Utils.PIKSI_VID) && (device.getProductId() == Utils.PIKSI_PID))
                if (!mUsbManager.hasPermission(device)) {
                    mUsbManager.requestPermission(device, mPermissionIntent);
                    System.out.println("sin permiso conectado");
                } else {
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
            piksiDriver = new SBPDriverJ2XX(context, usbdev);
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
            return;
        }
        System.out.println("creando piksi");
        piksiHandler = new SBPHandler(piksiDriver);

        try {
            File logfile = new File(context.getExternalFilesDir("logs"), "logfile");
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
                MsgPosLLH msg__ = (MsgPosLLH) msg;
                lat_queue.add(msg__.lat);
                lon_queue.add(msg__.lon);
                System.out.printf(
                        ", lat[deg]: %f, lon[deg]: %f, ellipsoid alt[m]: %f, n_sats: %d .\n",
                        msg__.lat,
                        msg__.lon,
                        msg__.height,
                        msg__.n_sats);
                //piksiHandler.send(msg);
            }
        });
    }

    public void start(){
        piksiHandler.start();
    }
    public void destroy() {
        context.unregisterReceiver(mUsbReceiver);
        context.unregisterReceiver(mUsbReceiverDisconnect);
    }

    public double getLat() {
        if (!lat_queue.isEmpty()){
            return lat_queue.poll();
        }else{
            return -1;
        }
    }

    public double getLon() {
        if (!lon_queue.isEmpty()){
            return lon_queue.poll();
        }else{
            return -1;
        }
    }

}

