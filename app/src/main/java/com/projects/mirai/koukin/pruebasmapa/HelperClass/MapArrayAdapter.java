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

public class MapArrayAdapter extends ArrayAdapter<SavedMap> {
    private final Context context;
    private final ArrayList<SavedMap> mapas;


    public MapArrayAdapter(Context context,int layoutLoad, ArrayList<SavedMap> values) {
        super(context, layoutLoad, values);
        this.context = context;
        this.mapas = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_view_item, parent, false);
        TextView firstline = (TextView) rowView.findViewById(R.id.firstLine);
        TextView secondline = (TextView) rowView.findViewById(R.id.secondLine);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        firstline.setText(mapas.get(position).getName());
        secondline.setText(mapas.get(position).getDate());
        return rowView;
    }

    public SavedMap getMap(int position){
        return mapas.get(position);
    }
}
