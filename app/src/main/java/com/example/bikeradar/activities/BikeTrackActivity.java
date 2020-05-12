package com.example.bikeradar.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.bikeradar.R;
import com.example.bikeradar.classes.Bike;

public class BikeTrackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_track);
        Intent intent = getIntent();
        String bikeId = intent.getStringExtra("bikeId");
        Bike bike = new Bike();


        Toast.makeText(this, bikeId, Toast.LENGTH_SHORT).show();
    }
}
