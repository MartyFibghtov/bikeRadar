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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bike);

        addPictureButton = findViewById(R.id.add_picture_button);
        deleteBikeButton = findViewById(R.id.delete_bike_button);
        submitButton = findViewById(R.id.submit_button);
        deleteBikeButton.setOnClickListener(deleteButtonListener);

        Intent intent = getIntent();
        bikeId = intent.getStringExtra("bikeId");

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
            phoneNumber = phoneNumber.replaceAll("[\\s-+.^:,()]","");
                if (phoneNumber.matches(regexStr)){
                if (phoneNumber.startsWith("8")){
                    phoneNumber = phoneNumber.replaceFirst("8","+7");

                }
                Log.i("Final phone_num", phoneNumber);
                uploadBike(name, phoneNumber);
            } else {
                Toast.makeText(getApplicationContext(), "Not a phone Number", Toast.LENGTH_LONG).show();
            }
        }
    };

    public void uploadBike(String name, String phoneNumber) {
        HashMap<String, String> bike = new HashMap<String, String>();
        bike.put( "name", name );
        bike.put( "phone_number", phoneNumber );

        Backendless.Data.of( "Bikes" ).save(bike, new AsyncCallback<Map>() {
            public void handleResponse( Map savedBike ){
                final String currentUserId = Backendless.UserService.loggedInUser();
                final String bikeId = (String) savedBike.get("objectId");

                Backendless.Data.of(BackendlessUser.class).findById(currentUserId, new AsyncCallback<BackendlessUser>() {
                    @Override
                    public void handleResponse(BackendlessUser currUser) {
                        Intent intent = new Intent(getApplicationContext(), AddBikeService.class);
                        intent.setAction(Constants.ACTION_ADD_EXISTING_BIKE);
                        intent.putExtra("userId", currUser.getObjectId());
                        intent.putExtra("bikeId", bikeId);

                        getApplicationContext().startService(intent);
                        Intent intent2 = new Intent(getApplicationContext(), MainMenuActivity.class);
                        startActivity(intent2);
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.i("addBike", fault.getMessage());
                    }
                });
                Log.i("uploadedBike", savedBike.toString());
            }
            @Override
            public void handleFault( BackendlessFault fault ) {
                Log.i("Error uploading bike", fault.getMessage());

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

                        Backendless.Data.of("BIKES").remove(bike, new AsyncCallback<Long>() {
                            @Override
                            public void handleResponse(Long response) {
                                Toast.makeText(getApplicationContext(), "Deleted bike", Toast.LENGTH_SHORT).show();
                                Intent intent2 = new Intent(getApplicationContext(), MainMenuActivity.class);
                                startActivity(intent2);
                                Intent intent = new Intent(Constants.BROADCAST_ADD_BIKE_SUCCESS);
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                                sendBroadcast(intent);
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
