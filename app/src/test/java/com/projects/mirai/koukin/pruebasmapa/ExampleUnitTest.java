package com.projects.mirai.koukin.pruebasmapa;

import org.junit.Test;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.sql.SQLOutput;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
        DateFormat df = new SimpleDateFormat("d-MMM-yyyy|HH_mm");
        String date = df.format(Calendar.getInstance().getTime());
        System.out.println(date);
    }

    public void main(String args[]){
        addition_isCorrect();
    }
}