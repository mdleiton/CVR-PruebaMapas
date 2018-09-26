package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MyHelperSql extends SQLiteOpenHelper {


    public MyHelperSql(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table mapas(_id integer primary key, name text NOT NULL, latitud real NOT NULL, longitud real NOT NULL , zoomlvl real NOT NULL , date text NOT NULL)");
        sqLiteDatabase.execSQL("create table emails(_id integer primary key,email text NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }


    public long insertMapInDb(SQLiteDatabase mSqliteDB,String mapName,double latitude,double longitude,double zoomlvl){
        DateFormat df = new SimpleDateFormat("d-MMM-yyyy|HH_mm");
        String date = df.format(Calendar.getInstance().getTime());

        ContentValues cv = new ContentValues();
        cv.put("name",mapName);
        cv.put("latitud",latitude);
        cv.put("longitud",longitude);
        cv.put("zoomlvl",zoomlvl);
        cv.put("date",date);

        long id = mSqliteDB.insert("mapas",null,cv);
        return id;
    }

    public ArrayList<SavedMap> loadMapsDb(SQLiteDatabase mSqliteDB,Context cont){
        ArrayList<SavedMap> mapas = new ArrayList<>();
        Cursor c = mSqliteDB.query("mapas",null,null,null,null
        ,null,null);
        while(c.moveToNext()){
            SavedMap m = new SavedMap(
                    c.getInt(c.getColumnIndex("_id")),
                    c.getString(c.getColumnIndex("name")),
                    c.getDouble(c.getColumnIndex("latitud")),
                    c.getDouble(c.getColumnIndex("longitud")),
                    c.getDouble(c.getColumnIndex("zoomlvl")),
                    c.getString(c.getColumnIndex("date"))
            );
            mapas.add(m);
            //Toast.makeText(cont,m.toString(),Toast.LENGTH_LONG).show();
        }
        return mapas;

    }


}
