package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import com.swiftnav.sbp.client.SBPHandler;

import java.util.LinkedList;
import java.util.Queue;

public class SerialLink {
    private SBPHandler handler;
    //private SBPFramer framer;
    private String[] fix_type = new String[8];
    public Queue<Double> lat_queue = new LinkedList<>();
    public Queue<Double> lon_queue = new LinkedList<>();


}
