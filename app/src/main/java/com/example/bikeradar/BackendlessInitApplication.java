package com.example.bikeradar;

import android.app.Application;
import android.content.Intent;
import android.os.StrictMode;

import com.backendless.Backendless;

public class BackendlessInitApplication extends Application {
    public static final String APPLICATION_ID = "0A750D92-2EB1-C0A9-FF56-01DCEC9A9A00";
    public static final String API_KEY = "CEE6C899-5BBE-4503-97F8-A70C12D04ECE";
    public static final String SERVER_URL = "https://api.backendless.com";
    @Override
    public void onCreate() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        super.onCreate();
        Backendless.setUrl(SERVER_URL);
        Backendless.initApp( getApplicationContext(),
                APPLICATION_ID,
                API_KEY );
    }

}
