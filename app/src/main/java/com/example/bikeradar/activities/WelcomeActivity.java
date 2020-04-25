package com.example.bikeradar.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.backendless.persistence.local.UserTokenStorageFactory;
import com.example.bikeradar.R;
import com.example.bikeradar.activities.LoginActivity;
import com.example.bikeradar.activities.MainMenuActivity;
import com.example.bikeradar.activities.RegistrationActivity;

public class WelcomeActivity extends AppCompatActivity {
    public View.OnClickListener onClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.login_button:
                    System.out.println("login");
                    Intent intent_login = new Intent(v.getContext(), LoginActivity.class);
                    startActivity(intent_login);
                    break;

                case R.id.register_button:
                    System.out.println("register");
                    Intent intent_register = new Intent(v.getContext(), RegistrationActivity.class);
                    startActivity(intent_register);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        getSupportActionBar().hide();


        String userToken = UserTokenStorageFactory.instance().getStorage().get();
        Log.i("info", userToken);

        if (userToken != null && !userToken.equals("")){
            Intent intent_main_menu = new Intent(this, MainMenuActivity.class);
            startActivity(intent_main_menu);
        } else {
            final Button login_button = (Button) findViewById(R.id.login_button);
            final Button register_button = (Button) findViewById(R.id.register_button);
            login_button.setOnClickListener(onClickListener);
            register_button.setOnClickListener(onClickListener);
        }


    }
}
