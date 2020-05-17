package com.example.bikeradar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bikeradar.classes.Bike;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static android.os.FileUtils.copy;

public class BikeAdapter  extends ArrayAdapter<Bike> {

    String TAG = "adapter";

    public BikeAdapter(Context context, List<Bike> arr) {
        super(context, R.layout.adapter_item, arr);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Bike bike = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_item, null);
        }

        assert bike != null;
        ((TextView) convertView.findViewById(R.id.textView)).setText(bike.name);
        ImageView iv = convertView.findViewById(R.id.imageView);

        new DownloadImageTask(iv).execute(bike.photo_url);

        return convertView;
    }
}


// show The Image in a ImageView




