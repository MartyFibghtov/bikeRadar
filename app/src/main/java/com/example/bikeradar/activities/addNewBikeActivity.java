package com.example.bikeradar.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.example.bikeradar.R;

public class addNewBikeActivity extends AppCompatActivity {
    public EditText nameField;
    public EditText phoneNumberField;
    public Button addPictureButton;
    public Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_bike);

        nameField = findViewById(R.id.name_field);
        phoneNumberField = findViewById(R.id.phone_number_field);
        addPictureButton = findViewById(R.id.add_picture_button);
        submitButton = findViewById(R.id.submit_button);
        // TODO add bike to user
    }
}
