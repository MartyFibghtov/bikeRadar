package com.example.bikeradar.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.example.bikeradar.R;

public class RegistrationActivity extends AppCompatActivity {
    public EditText usernameField;
    public EditText passwordField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        getSupportActionBar().hide();

        Button submit_button = (Button) findViewById(R.id.submit_button);

        usernameField = (EditText) findViewById(R.id.username_field);
        passwordField = (EditText) findViewById(R.id.password_field);

        submit_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                String username = usernameField.getText().toString().trim();
                System.out.println(username);
                String password = passwordField.getText().toString().trim();
                BackendlessUser backendlessUser = new BackendlessUser();
                backendlessUser.setPassword(password);
                backendlessUser.setProperty("username", username);

                Backendless.UserService.register(backendlessUser, new AsyncCallback<BackendlessUser>() {
                    @Override
                    public void handleResponse(BackendlessUser response) {
                        Intent intent_register = new Intent(v.getContext(), WelcomeActivity.class);
                        startActivity(intent_register);
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.e("Error in register", fault.getMessage());
                    }
                });

            }
        });
    }
}
