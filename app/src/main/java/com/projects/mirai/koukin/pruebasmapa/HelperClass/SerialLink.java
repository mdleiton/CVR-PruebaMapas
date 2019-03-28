package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import com.swiftnav.sbp.SBPMessage;
import com.swiftnav.sbp.client.SBPFramer;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.drivers.SBPDriverJSSC;
import com.swiftnav.sbp.logging.MsgLog;
import com.swiftnav.sbp.navigation.MsgPosLLH;
import com.swiftnav.sbp.navigation.MsgGPSTimeDepA;
import com.swiftnav.sbp.navigation.MsgPosLLHDepA;
import jssc.SerialPortException;

import java.util.LinkedList;
import java.util.Queue;

public class SerialLink {
    private SBPHandler handler;
    private SBPFramer framer;
    private String[] fix_type = new String[8];
    public Queue<Double> lat_queue = new LinkedList<>();
    public Queue<Double> lon_queue = new LinkedList<>();

    public SerialLink(String port, int baudrate) {
        populate_fix_type();
        try {
            framer = new SBPFramer(new SBPDriverJSSC(port, baudrate));
            handler = new SBPHandler(framer);
            handler.start();
        } catch (SerialPortException e) {
            System.err.println("Failed to open serial port: " + e.toString());
            System.exit(-2);
        }
        for (SBPMessage msg : handler) {
            switch (msg.type) {
                case MsgLog.TYPE:
                    logHandler(msg);
                    break;
                case MsgPosLLH.TYPE:
                    llhHandler(msg);
                    break;
                case MsgGPSTimeDepA.TYPE:
                    MsgGPSTimeDepAHandler(msg);
                    break;
                case MsgPosLLHDepA.TYPE:
                    MsgPosLLHDepAHandler(msg);
                    break;
                default:
                    break;
            }
        }
    }

    private void populate_fix_type() {
        fix_type[0] = "No fix";
        fix_type[1] = "SPP";
        fix_type[2] = "DGPS";
        fix_type[3] = "float";
        fix_type[4] = "fixed";
        fix_type[5] = "DR";
        fix_type[6] = "SBAS";
        fix_type[7] = "UNKNOWN";
    }

    public void MsgPosLLHDepAHandler(SBPMessage msg_) {
        MsgPosLLHDepA msg = (MsgPosLLHDepA) msg_;
        lat_queue.add(msg.lat);
        lon_queue.add(msg.lon);
        System.out.printf(
                "lat[deg]: %f, lon[deg]: %f, ellipsoid alt[m]: %f, horizontal accuracy[m]: %f, vertical_accuracy[m]: %f, n_sats: %d .\n",
                msg.lat,
                msg.lon,
                msg.height,
                msg.h_accuracy / 1000.0,
                msg.v_accuracy / 1000.0,
                msg.n_sats);
    }

    public void MsgGPSTimeDepAHandler(SBPMessage msg_) {
        MsgGPSTimeDepA msg = (MsgGPSTimeDepA) msg_;
        System.out.print(msg.tow + ".\n");
    }

    public void logHandler(SBPMessage msg_) {
        MsgLog msg = (MsgLog) msg_;
        System.out.print(msg.text + ".\n");
    }

    public void llhHandler(SBPMessage msg_) {
        MsgPosLLH msg = (MsgPosLLH) msg_;
        int fix_type = msg.flags & 0x7;
        System.out.printf(
                "POSLLH message received -- fix_type: %s, tow [ms]: %d", this.fix_type[fix_type], msg.tow);
        if (fix_type != 0) {
            System.out.printf(
                    ", lat[deg]: %f, lon[deg]: %f, ellipsoid alt[m]: %f, horizontal accuracy[m]: %f, vertical_accuracy[m]: %f, n_sats: %d .\n",
                    msg.lat,
                    msg.lon,
                    msg.height,
                    msg.h_accuracy / 1000.0,
                    msg.v_accuracy / 1000.0,
                    msg.n_sats);
        }
        System.out.println();
    }

    public double getLat() {
        return lat_queue.poll();
    }

    public double getLon() {
        return lon_queue.poll();
    }



    /* uso
    public static void main(String[] args) {
        String port =  "/dev/ttyUSB0";
        int baudrate = 1000000;
        SerialLink rtk = new SerialLink(port, baudrate);
        }

        */
}