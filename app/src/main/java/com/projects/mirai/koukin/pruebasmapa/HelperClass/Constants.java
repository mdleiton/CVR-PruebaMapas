package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.os.Environment;

import java.io.File;

public class Constants {
    public static  String pathMapaArq = Environment.getExternalStorageDirectory() + File.separator +"MapasArq"+File.separator;
    public static String pathJornadas = Environment.getExternalStorageDirectory() + File.separator +"MapasArq"+File.separator +"Jornadas" + File.separator;
    public static String pathTemp = Environment.getExternalStorageDirectory() + File.separator +"MapasArq"+File.separator + "Temp" + File.separator;
}
