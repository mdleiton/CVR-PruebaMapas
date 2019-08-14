package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.support.annotation.NonNull;
import android.text.TextPaint;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Class que permite graficar una barra con la escala actual del mapa
 */
public class CopyOfScaleBarOverlay extends Overlay {
    private static final String STR_CM = "cm";
    private static final String STR_M = "m";
    private static final String STR_KM = "km";

    //Constants
    private static float scaleBarProportion = 0.25f;
    private float cMarginLeft = 4;
    private float cLineTopSize = 8;
    private float cMarginTop = 6;
    private float cMarginBottom = 2;
    private float cTextSize = 12;
    private float distanceFromBottom = 100;


    //instantiation
    private Context context;

    private Paint paintLine, paintText, paintRectangle;
    private Location l0;
    private Location l1;
    private float ds;
    private int width, height, pi;
    private float marginLeft, marginTop, marginBottom, lineTopSize;
    private String unit;


    /**
     * Constructor of the CopyOfScaleBarOverlay class
     * @param context of the mapView
     */
    public CopyOfScaleBarOverlay(Context context) {
        super();
        this.context = context;

        paintText = new TextPaint();
        paintText.setARGB(255, 0, 0, 0);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);

        paintRectangle = new Paint();
        paintRectangle.setARGB(80, 255, 255, 255);
        paintRectangle.setAntiAlias(true);

        paintLine = new Paint();
        paintLine.setARGB(180, 0, 0, 0);
        paintLine.setAntiAlias(true);

        l0 = new Location("none");
        l1 = new Location("none");

        ds = this.context.getApplicationContext().getResources().getDisplayMetrics().density;
        width = this.context.getApplicationContext().getResources().getDisplayMetrics().widthPixels;
        height = this.context.getApplicationContext().getResources().getDisplayMetrics().heightPixels;
        pi = (int) (height - distanceFromBottom * ds);

        marginLeft = cMarginLeft * ds;
        lineTopSize = cLineTopSize * ds;
        marginTop = cMarginTop * ds;
        marginBottom = cMarginBottom * ds;

    }

    @Override
    public void draw(Canvas canvas, MapView mapview, boolean shadow) {
        if (mapview.getZoomLevelDouble() > 1) {

            ArrayList<Float> values = CalculateAreas(mapview);
            float bs = values.get(0);

            float x_size = values.get(1);

            float dcd02 = values.get(2);

            String text = String.format(Locale.US,"%.2f %s", dcd02, unit);
            //Draw rectangle
            canvas.drawRect(0, pi, x_size+2, pi + marginTop + paintText.getFontSpacing() + marginBottom, paintRectangle);

            //Draw line
            canvas.drawLine(marginLeft, pi + marginTop, marginLeft + bs, pi + marginTop, paintLine);

            //Draw line tops
            canvas.drawLine(marginLeft, pi + marginTop - lineTopSize / 2, marginLeft, pi + marginTop + lineTopSize / 2, paintLine);
            canvas.drawLine(marginLeft + bs, pi + marginTop - lineTopSize / 2, marginLeft + bs, pi + marginTop + lineTopSize / 2, paintLine);

            //Draw line midle
            canvas.drawLine(marginLeft + bs / 2, pi + marginTop - lineTopSize / 3, marginLeft + bs / 2, pi + marginTop + lineTopSize / 3, paintLine);

            //Draw line quarters
            canvas.drawLine(marginLeft + bs / 4, pi + marginTop - lineTopSize / 4, marginLeft + bs / 4, pi + marginTop + lineTopSize / 4, paintLine);
            canvas.drawLine(marginLeft + 3 * bs / 4, pi + marginTop - lineTopSize / 4, marginLeft + 3 * bs / 4, pi + marginTop + lineTopSize / 4, paintLine);

            //Draw text
            canvas.drawText(text, marginLeft + bs, pi + marginTop + paintText.getFontSpacing(), paintText);
        }
    }


    /**
     * Método que calcula la escala que se muestra actualmente en el mapa
     * @param mapview el mapa
     * @return retorna ArrayList con valores usados en graficacion de barra
     */
    private ArrayList<Float> CalculateAreas(@NonNull MapView mapview){
        //Calculate scale bar size and units

        ArrayList<Float> params = new ArrayList<>();

        GeoPoint g0 = (GeoPoint) mapview.getProjection().fromPixels(0, height / 2);
        GeoPoint g1 = (GeoPoint) mapview.getProjection().fromPixels(width, height / 2);
        l0.setLatitude(g0.getLatitude());
        l0.setLongitude(g0.getLongitude());
        l1.setLatitude(g1.getLatitude());
        l1.setLongitude(g1.getLongitude());
        float d01 = l0.distanceTo(l1);
        float d02 = d01 * scaleBarProportion;
        // multiply d02 by a unit conversion factor if needed
        float cd02;
        if (d02 > 1000) {
            unit = STR_KM;
            cd02 = d02 / 1000;
        } else {
            unit = STR_M;
            cd02 = d02;
        }

        int i = 1;
        do {
            i *= 10;
        } while (i <= cd02);
        i /= 10;

        float dcd02 =  (cd02 / i) * i;
        float bs = dcd02 * width / d01 * d02 / cd02;
        String text = String.format(Locale.US,"%.2f %s", dcd02, unit);
        paintText.setTextSize(cTextSize * ds);
        float text_x_size = paintText.measureText(text);
        float x_size = bs + text_x_size / 2 + 2 * marginLeft;
        params.add(bs);
        params.add(x_size);
        params.add(dcd02);
        return params;

    }
}
