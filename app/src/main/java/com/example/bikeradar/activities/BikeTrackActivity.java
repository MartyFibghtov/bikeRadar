package com.example.bikeradar.activities;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.example.bikeradar.R;
import com.example.bikeradar.ViewWeightAnimationWrapper;
import com.example.bikeradar.classes.Bike;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.bikeradar.Constants.ERROR_DIALOG_REQUEST;
import static com.example.bikeradar.Constants.MAPVIEW_BUNDLE_KEY;
import static com.example.bikeradar.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.example.bikeradar.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

@RequiresApi(api = Build.VERSION_CODES.O)
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
    public ImageButton fullScreenMapButton;
    boolean trackingRequired;
    public SMS sms;
    public String sms_id;
    private Handler mHandler = new Handler();
    RelativeLayout mMapContainer;
    LinearLayout mBikeView;
    private static final int MAP_LAYOUT_STATE_CONTRACTED = 0; // состояние карты не полный экран
    private static final int MAP_LAYOUT_STATE_EXPANDED = 1; // состояние карты  полный экран
    private int mMapLayoutState = 0;
    ProgressBar mProgressBar;
    boolean changeCameraView;
    ArrayList<LatLng> positionsList;
    TextView lastUpdatedTV;
    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
    TextView bikeName;
    Button copyBikeIdButton;
    Button editBikeIdButton;

    PolylineOptions mPolylineOptions = new PolylineOptions();


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

        mPolylineOptions.color(Color.RED);
        mPolylineOptions.width(5);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);

        positionsList = new ArrayList<>();

        lastUpdatedTV = findViewById(R.id.last_updated);

        // Buttons
        startTrackingButton = (Button) findViewById(R.id.start_tracking_button);
        copyBikeIdButton = findViewById(R.id.copy_bike_id_button);
        stopTrackingButton = (Button) findViewById(R.id.stop_tracking_button);
        editBikeIdButton = (Button) findViewById(R.id.edit_bike_button);
        fullScreenMapButton = (ImageButton) findViewById(R.id.btn_full_screen_map);
        fullScreenMapButton.setOnClickListener(changeLayoutState);
        stopTrackingButton.setOnClickListener(stopTrackingListener);
        editBikeIdButton.setOnClickListener(editBikeListener);


        mMapView = findViewById(R.id.map);
        mBikeView = findViewById(R.id.bike_view);
        mMapContainer = findViewById(R.id.map_container);

        bikeName = findViewById(R.id.bike_name);

        initGoogleMap(savedInstanceState);

    }

    private View.OnClickListener editBikeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), EditBikeActivity.class);
            intent.putExtra("bikeId", bikeId);
            startActivity(intent);
        }
    };



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
        bikeName.setText(bike.name);
        startTrackingButton.setOnClickListener(startTrackingListener);
        copyBikeIdButton.setOnClickListener(copyToClipboardListener);
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
            mProgressBar.setVisibility(View.INVISIBLE);
//            Intent intent = new Intent(getApplicationContext(), BikeTrackActivity.class);
//            startActivity(intent);
        }
    };


    private View.OnClickListener startTrackingListener = new View.OnClickListener(){
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onClick(View v) {
            startTracking();
        }
    };

    private View.OnClickListener changeLayoutState = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_full_screen_map:{

                    if(mMapLayoutState == MAP_LAYOUT_STATE_CONTRACTED){
                        mMapLayoutState = MAP_LAYOUT_STATE_EXPANDED;
                        expandMapAnimation();
                    }
                    else if(mMapLayoutState == MAP_LAYOUT_STATE_EXPANDED){
                        mMapLayoutState = MAP_LAYOUT_STATE_CONTRACTED;
                        contractMapAnimation();
                    }
                    break;
                }

            }
        }
    };



    void startTracking(){
        trackingRequired = true;
        mProgressBar.setVisibility(View.VISIBLE);
        changeCameraView = true;
        sendSMS(bike.phone_number, "150");  // Init tracker
        sms = checkSMS();
        sms_id = sms.id;
        startRepeating();

    }

    public void startRepeating() {
        smsRunnable.run();
    }
    public void stopRepeating() {
        mHandler.removeCallbacks(smsRunnable);
    }

    private Runnable smsRunnable = new Runnable() {

        @Override
        public void run() {
            Log.i("tracking", "sms.is" + sms.id.toString());
            System.out.println("sms.is" + sms.id.toString());
            Log.i("tracking", sms.phone);
            Log.i("tracking", bike.phone_number);


            if (!sms.id.equals(sms_id) && sms.phone.equals(bike.phone_number)) {
                LocalDateTime now = LocalDateTime.now();
                String time = timeFormat.format(now);
                lastUpdatedTV.setText("Последнее обновление " + time);
                mProgressBar.setVisibility(View.INVISIBLE);
                String[] coordinates = sms.text.split("!==!");
                double latitude = Double.parseDouble(coordinates[0]);
                double longitude = Double.parseDouble(coordinates[1]);
                LatLng position = new LatLng(latitude, longitude);
                positionsList.add(position);
                
                mGoogleMap.clear();

                PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
                for (int i = 0; i < positionsList.size(); i++) {
                    LatLng point = positionsList.get(i);
                    options.add(point);
                }

                Polyline line = mGoogleMap.addPolyline(options);
                
                mGoogleMap.addPolyline(new PolylineOptions());

                if (changeCameraView){
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                    changeCameraView = false;
                }

                mGoogleMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(bike.name)

                                //.icon()  TODO add image of bike
                );
                Toast.makeText(getApplicationContext(), sms.text, Toast.LENGTH_SHORT).show();
                sms_id = sms.id;
            }
            sms = checkSMS();

            mHandler.postDelayed(this, 2000);
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
        stopRepeating();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private void expandMapAnimation(){
        ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
        ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                "weight",
                50,
                0);
        mapAnimation.setDuration(800);

        ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(mBikeView);
        ObjectAnimator buttonsAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                "weight",
                50,
                100);
        buttonsAnimation.setDuration(800);

        buttonsAnimation.start();
        mapAnimation.start();
    }

    private void contractMapAnimation(){
        ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
        ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                "weight",
                0,
                50);
        mapAnimation.setDuration(800);

        ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(mBikeView);
        ObjectAnimator recyclerAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                "weight",
                100,
                50);
        recyclerAnimation.setDuration(800);

        recyclerAnimation.start();
        mapAnimation.start();
    }

    void copyToClipboard(String text){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip =ClipData.newPlainText("BikeId", text);
        clipboard.setPrimaryClip(clip);
    }
    View.OnClickListener copyToClipboardListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            copyToClipboard(bike.objectId);
            Toast.makeText(getApplicationContext(), "Coppied bike ID!", Toast.LENGTH_SHORT).show();
        }
    };

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
