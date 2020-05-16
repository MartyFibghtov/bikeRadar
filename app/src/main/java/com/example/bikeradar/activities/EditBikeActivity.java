package com.example.bikeradar.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.example.bikeradar.AddBikeService;
import com.example.bikeradar.Constants;
import com.example.bikeradar.R;
import com.example.bikeradar.classes.Bike;

import java.util.HashMap;
import java.util.Map;

public class EditBikeActivity extends AppCompatActivity {

    public EditText nameField;
    public EditText phoneNumberField;
    public Button addPictureButton;
    public Button deleteBikeButton;
    public Button submitButton;
    String currentPhotoPath;
    String bikeId;
    Bike bike;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bike);
        Intent intent = getIntent();
        bikeId = intent.getStringExtra("bikeId");

        bike = new Bike();

        getBike(bikeId);

        nameField = findViewById(R.id.name_field);
        phoneNumberField = findViewById(R.id.phone_number_field);

        addPictureButton = findViewById(R.id.add_picture_button);
        deleteBikeButton = findViewById(R.id.delete_bike_button);
        submitButton = findViewById(R.id.submit_button);

        deleteBikeButton.setOnClickListener(deleteButtonListener);
        submitButton.setOnClickListener(submitButtonListener);
//        addPictureButton.setOnClickListener();



    }

    public void getBike(final String bikeId) {
        Backendless.Data.mapTableToClass("bikes", Bike.class ); // match table resp to class
        Backendless.Data.of("bikes").findById(bikeId, new AsyncCallback<Map>() {
            @Override
            public void handleResponse(Map response) {
                if (response != null) {
                    bike.setObjectId(bikeId);
                    bike.setName((String) response.get("name"));
                    bike.setPhoneNumber((String) response.get("phone_number"));
                    bike.setPhotoUrl((String) response.get("photo_url"));
                    nameField.setText(bike.name);
                    phoneNumberField.setText(bike.phone_number);

                }else{
                    Log.e("got null", bikeId);
                }
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e("Getting bike", fault.getMessage());
            }
        });
    }




    private View.OnClickListener deleteButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            deleteBike();
        }
    };

    private View.OnClickListener submitButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String regexStr = "^(8|\\+7)(\\d{3})(\\d{3})(\\d{2})(\\d{2})";
            final String name = nameField.getText().toString();
            String phoneNumber = phoneNumberField.getText().toString();
            phoneNumber = phoneNumber.replaceAll("[\\s-.^:,()]","");

            if (phoneNumber.matches(regexStr)){
                if (phoneNumber.startsWith("8")){
                    phoneNumber = phoneNumber.replaceFirst("8","+7");

                }
                Log.i("Final phone_num", phoneNumber);
                updateBike(name, phoneNumber, bikeId);
            } else {
                    Log.i("got", phoneNumber );
                Toast.makeText(getApplicationContext(), "Not a phone Number", Toast.LENGTH_LONG).show();
            }
        }
    };

    public void updateBike(String name, String phoneNumber, String bikeId) {
        HashMap<String, String> bike = new HashMap<String, String>();
        bike.put( "name", name );
        bike.put( "phone_number", phoneNumber );
        bike.put("objectId", bikeId);

        Backendless.Data.of( "bikes" ).save(bike, new AsyncCallback<Map>() {
            public void handleResponse( Map savedBike ){
                Log.i("Updated bike", "savedBike");
                Toast.makeText(getApplicationContext(), "Updated bike", Toast.LENGTH_SHORT).show();
                Intent backToMain = new Intent(getApplicationContext(), MainMenuActivity.class);
                startActivity(backToMain);
            }
            @Override
            public void handleFault( BackendlessFault fault ) {
                Log.i("Error updating bike", fault.getMessage());
                Toast.makeText(getApplicationContext(), "Error updating bike", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void deleteBike(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(EditBikeActivity.this)
                .setCancelable(false)
                .setMessage("Do you want to delete this bike?")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The user canceled. Do nothing
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        HashMap bike = new HashMap();
                        bike.put("objectId", bikeId);

                        Backendless.Data.of("bikes").remove(bike, new AsyncCallback<Long>() {
                            @Override
                            public void handleResponse(Long response) {
                                Toast.makeText(getApplicationContext(), "Deleted bike", Toast.LENGTH_SHORT).show();
                                Intent intent2 = new Intent(getApplicationContext(), MainMenuActivity.class);
                                startActivity(intent2);
//                                Intent intent = new Intent(Constants.BROADCAST_ADD_BIKE_SUCCESS);
//                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
//                                sendBroadcast(intent);
                            }

                            @Override
                            public void handleFault(BackendlessFault fault) {
                                Log.e("Not deleted", fault.getMessage());
                            }
                        });
                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

}
