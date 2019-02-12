package com.projects.mirai.koukin.pruebasmapa.HelperClass;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.projects.mirai.koukin.pruebasmapa.R;

import java.util.ArrayList;

public class JornadasArrayAdapter extends ArrayAdapter<FechaJornada> {
    private final Context context;
    private final ArrayList<FechaJornada> jornadas;


    public JornadasArrayAdapter(Context context, int layoutLoad, ArrayList<FechaJornada> values) {
        super(context, layoutLoad, values);
        this.context = context;
        this.jornadas = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_view_item_jornadas, parent, false);
        TextView firstline = (TextView) rowView.findViewById(R.id.firstLine);
        TextView secondline = (TextView) rowView.findViewById(R.id.secondLine);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        firstline.setText(jornadas.get(position).getNombre());
        secondline.setText(jornadas.get(position).getFecha());
        return rowView;
    }

    public FechaJornada getJornada(int position){
        return jornadas.get(position);
    }
}
