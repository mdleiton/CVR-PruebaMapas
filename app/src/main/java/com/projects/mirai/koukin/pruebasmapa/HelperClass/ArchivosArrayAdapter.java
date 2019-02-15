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

public class ArchivosArrayAdapter extends ArrayAdapter<Archivo> {
    private final Context context;
    private final ArrayList<Archivo> archivos;


    public ArchivosArrayAdapter(Context context, int layoutLoad, ArrayList<Archivo> values) {
        super(context, layoutLoad, values);
        this.context = context;
        this.archivos = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_view_item_archivos, parent, false);
        TextView firstline = (TextView) rowView.findViewById(R.id.firstLine);
        TextView secondline = (TextView) rowView.findViewById(R.id.secondLine);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        firstline.setText(archivos.get(position).getNombre());
        secondline.setText(archivos.get(position).getFecha_creacion());
        return rowView;
    }

    public Archivo getArchivo(int position){
        return archivos.get(position);
    }
}
