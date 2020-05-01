package com.example.bikeradar.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.example.bikeradar.AddBikeService;
import com.example.bikeradar.Constants;
import com.example.bikeradar.classes.Bike;
import com.example.bikeradar.R;

import java.util.ArrayList;

public class MainMenuActivity extends AppCompatActivity {

    private ArrayList<String> bikes;
    private ArrayAdapter<String> bikeListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        getSupportActionBar().hide();

        // Log out button
        Button logOutButton = findViewById(R.id.log_out_button);
        logOutButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(final View v) {
                Backendless.UserService.logout(new AsyncCallback<Void>() {
                    @Override
                    public void handleResponse(Void response) {
                        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
                        startActivity(intent);
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Toast.makeText(getApplicationContext(), "An error occurred!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Add bike button
        Button addBikeButton = findViewById(R.id.add_bike_button);
        addBikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("add_bike button", "Pressed");
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainMenuActivity.this)
                        .setCancelable(false)
                        .setMessage("Adding bike")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // The user canceled. Do nothing
                            }
                        })
                        .setNeutralButton("Add a new bike", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(getApplicationContext(), addNewBikeActivity.class);
                                startActivity(intent);
                            }
                        })
                        .setPositiveButton("Add an existing bike", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, int which) {
                                final EditText inputField = new EditText(MainMenuActivity.this);
                                AlertDialog.Builder alertBuilder2 = new AlertDialog.Builder(MainMenuActivity.this)
                                        .setView(inputField)
                                        .setCancelable(false)
                                        .setMessage("Input bike id")
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            // The user canceled. Do nothing
                                            }
                                        })
                                        .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        });
                                final AlertDialog alertDialog2 = alertBuilder2.create();
                                alertDialog2.show();
                                alertDialog2.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        String bikeId = inputField.getText().toString().trim();
                                        if (!bikeId.equals("")){
                                            addBike("254D4CF5-23AC-0D41-FF6D-8EEC19A0EE00");
                                            alertDialog2.dismiss();
                                        } else{
                                            inputField.setHint("this field mustn't be empty");
                                            inputField.setHintTextColor(getResources().getColor(R.color.red));
                                        }

                                    }
                                });
                            }
                        });


                AlertDialog alertDialog = alertBuilder.create();
                alertDialog.show();
            }
        });


        bikes = new ArrayList<>();
        bikeListAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, bikes);

        ListView bikesList= (ListView) findViewById(R.id.bikes_list);
        bikesList.setAdapter(bikeListAdapter);

        String currentUserId = Backendless.UserService.loggedInUser(); // get user id

        Backendless.Data.mapTableToClass("bikes", Bike.class ); // match table resp to class

        Backendless.Data.of(BackendlessUser.class).findById(currentUserId, new AsyncCallback<BackendlessUser>() { // Getting and setting bikes to ListView
            @Override
            public void handleResponse(BackendlessUser user) {
                Object[] bikeObjects = (Object[]) user.getProperty("bikes");
                if (bikeObjects.length > 0){
                    Bike[] bikeArray = (Bike[]) bikeObjects;
                    for (Bike bike : bikeArray){
                        String name = bike.name;
                        bikes.add(name);
                        bikeListAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e("Getting Bikes", fault.toString());
            }
        });
    }

    private void addBike(final String bikeId){
        final String currentUserId = Backendless.UserService.loggedInUser();
        Backendless.Data.of(BackendlessUser.class).findById(currentUserId, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser currUser) {
                Intent intent = new Intent(MainMenuActivity.this, AddBikeService.class);
                intent.setAction(Constants.ACTION_ADD_BIKE);
                intent.putExtra("userName", currUser.getProperty("username").toString());
                intent.putExtra("bikeId", bikeId);

                MainMenuActivity.this.startService(intent);
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.i("addBike", String.valueOf(fault));
            }
        });

    }
}
