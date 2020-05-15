package com.example.bikeradar.activities;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.example.bikeradar.R;
import com.example.bikeradar.classes.Bike;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.bikeradar.Constants.ERROR_DIALOG_REQUEST;
import static com.example.bikeradar.Constants.MAPVIEW_BUNDLE_KEY;
import static com.example.bikeradar.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.example.bikeradar.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

public class BikeTrackActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String bikeId;
    private static final String TAG = "BikeTrack";
    private boolean mLocationPermissionGranted = false;
    private GoogleMap mGoogleMap;
    private LatLngBounds mMapLocation;
    private MapView mMapView;
    private Bike bike;
    public Button startTrackingButton;
    public Button stopTrackingButton;
    boolean trackingRequired;
    public SMS sms;
    public String sms_id;
    private Handler mHandler = new Handler();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_track);

        Intent intent = getIntent();
        bikeId = intent.getStringExtra("bikeId");
        if(!checkServices()){
            Intent intentBack = new Intent(this, MainMenuActivity.class);
            startActivity(intentBack);
        }

        bike = new Bike();
        Log.i("Bike id", bikeId);
        getBike(bikeId);




        // Buttons
        startTrackingButton = (Button) findViewById(R.id.start_tracking_button);
        stopTrackingButton = (Button) findViewById(R.id.stop_tracking_button);
        stopTrackingButton.setOnClickListener(stopTrackingListener);

        mMapView = findViewById(R.id.map);



        initGoogleMap(savedInstanceState);

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
                    onGotBike();
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

    public void onGotBike(){
        while (mGoogleMap==null){
            // do nothing
        }
        startTrackingButton.setOnClickListener(startTrackingListener);
        startTrackingButton.setActivated(true);


    }

    private boolean checkServices() { // check if maps are available
        if (!isServicesOK()) {
            Toast.makeText(this, "not available for your phone", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isLocationAllowed()){
            Toast.makeText(this, "allow location usage", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isSmsAllowed()){
            Toast.makeText(this, "allow SMS usage", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isGpsEnabled()){
            Toast.makeText(this, "enable gps", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;

    }

    public boolean isGpsEnabled(){ // IS GPS ENABLED?
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    public boolean isSmsAllowed() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            return true;

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PackageManager.PERMISSION_GRANTED);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PackageManager.PERMISSION_GRANTED);
            return false;

        }
    }

    public boolean isLocationAllowed() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;

        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
            return false;
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isServicesOK(){ // CHECK IF SERVICES ARE OK
        Log.d(TAG, "isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(BikeTrackActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(BikeTrackActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }


    @Override
    public void onMapReady(GoogleMap map) {
        mGoogleMap = map;
    }



    private View.OnClickListener stopTrackingListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            sendSMS(bike.phone_number, "230");
            stopRepeating();
            trackingRequired = false;
            Intent intent = new Intent(getApplicationContext(), BikeTrackActivity.class);
            startActivity(intent);
        }
    };


    private View.OnClickListener startTrackingListener = new View.OnClickListener(){
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onClick(View v) {
            startTracking();
        }
    };



    void startTracking(){
        trackingRequired = true;
        sendSMS(bike.phone_number, "150");  // Init tracker
        sms = checkSMS();
        sms_id = sms.id;
        startRepeating();

    }


    public void startRepeating() {
        //mHandler.postDelayed(smsRunnable, 5000);
        smsRunnable.run();
    }
    public void stopRepeating() {
        mHandler.removeCallbacks(smsRunnable);
    }

    private Runnable smsRunnable = new Runnable() {

        @Override
        public void run() {
            System.out.println("Repeating");
            System.out.println("sms.is" + sms.id.toString());
            System.out.println(sms.phone);
            System.out.println(bike.phone_number);
            System.out.println("sms_id" + sms_id);

            if (!sms.id.equals(sms_id)){// sms.phone.equals(bike.phone_number)) {
                System.out.println("opa");
                String[] coordinates = sms.text.split("!==!");
                double latitude = Double.parseDouble(coordinates[0]);
                double longitude = Double.parseDouble(coordinates[1]);
                mGoogleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latitude, longitude))
                                .title(bike.name)

                        //.icon()  TODO add image of bike
                );
                sms_id = sms.id;
            }
            sms = checkSMS();

            mHandler.postDelayed(this, 5000);
        }
    };



    public SMS checkSMS() {
        Uri smsURI = Uri.parse("content://sms/inbox");
        Cursor cursor = getContentResolver().query(smsURI, null, null, null, null);
        assert cursor != null;
        cursor.moveToFirst();

        String text = cursor.getString(12);
        String phone = cursor.getString(2);
        String id = cursor.getString(0);
        SMS sms = new SMS(id, text, phone);
        return sms;
    }

    public void sendSMS(String phoneNumber, String smsText){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, smsText, null, null);
    }


    private void initGoogleMap(Bundle savedInstanceState){

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

}


class SMS{
    String id;
    String text;
    String phone;

    SMS(String id, String text, String phone) {
        this.id = id;
        this.text = text;
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "SMS{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
