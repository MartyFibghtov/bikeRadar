package com.example.bikeradar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bikeradar.classes.Bike;

import java.util.List;

public class BikeAdapter  extends ArrayAdapter<Bike> {

    public BikeAdapter(Context context, List<Bike> arr) {
        super(context, R.layout.adapter_item, arr);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Bike bike = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_item, null);
        }

        // Заполняем адаптер
        assert bike != null;
        ((TextView) convertView.findViewById(R.id.textView)).setText(bike.name);
        // Выбираем картинку для месяца
        ((ImageView) convertView.findViewById(R.id.imageView)).setImageResource(R.drawable.ic_launcher_background);
        // TODO set image via link  if (url == null) setDefault

        return convertView;
    }

}
