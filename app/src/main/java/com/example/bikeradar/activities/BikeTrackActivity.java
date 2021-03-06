package com.example.bikeradar.activities;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.example.bikeradar.DownloadImageTask;
import com.example.bikeradar.R;
import com.example.bikeradar.ViewWeightAnimationWrapper;
import com.example.bikeradar.classes.Bike;
import com.example.bikeradar.classes.SMS;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import android.widget.LinearLayout;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

import static com.example.bikeradar.Constants.ERROR_DIALOG_REQUEST;
import static com.example.bikeradar.Constants.MAPVIEW_BUNDLE_KEY;
import static com.example.bikeradar.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

@RequiresApi(api = Build.VERSION_CODES.O)
public class BikeTrackActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "BikeTrack";
    private String bikeId;
    private Bike bike;
    public SMS sms;  // костыльно

    private GoogleMap mGoogleMap;
    private MapView mMapView;



    boolean trackingRequired;

    public String sms_id;
    private Handler mHandler = new Handler();


    RelativeLayout mMapContainer;
    LinearLayout mBikeView;

    public TextView lastUpdatedTV;
    public TextView bikeNameView;


    public Button copyBikeIdButton;
    public Button editBikeIdButton;
    public Button startTrackingButton;
    public Button stopTrackingButton;

    public ImageButton fullScreenMapButton;
    ImageView bikeImageView;
    ProgressBar mProgressBar;

    PolylineOptions mPolylineOptions = new PolylineOptions();
    public ArrayList<LatLng> positionsList;







    private boolean changeCameraView;
    private int mMapLayoutState = 0;
    private static final int MAP_LAYOUT_STATE_CONTRACTED = 0; // состояние карты не полный экран
    private static final int MAP_LAYOUT_STATE_EXPANDED = 1; // состояние карты  полный экран


    private final int PERMISSION_REQUEST_LOCATION = 100;
    private final int PERMISSION_REQUEST_SMS = 101;
    private final String SMS_START_TRACKING = "150";
    private final String SMS_STOP_TRACKING = "230";

    DateTimeFormatter HHmmTimeFormat = DateTimeFormatter.ofPattern("HH:mm");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_track);

        Intent intent = getIntent();
        bikeId = intent.getStringExtra("bikeId");
        String bikeName = intent.getStringExtra("name");

        if (!isServicesOK()){
            Intent intent_back = new Intent(this, MainMenuActivity.class);
            startActivity(intent_back);
        }

        bikeNameView = findViewById(R.id.bike_name);
        bikeNameView.setText(bikeName);

        bike = new Bike();
        Log.i("Bike id", bikeId);
        getBike(bikeId);

        mPolylineOptions.color(Color.RED);
        mPolylineOptions.width(5);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);
        bikeImageView = findViewById(R.id.bike_image_view);

        positionsList = new ArrayList<>();

        lastUpdatedTV = findViewById(R.id.last_updated);
        bikeImageView = findViewById(R.id.bike_image_view);


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

        bikeNameView = findViewById(R.id.bike_name);
        bikeNameView.setText(bikeName);

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


    private View.OnClickListener copyToClipboardListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            copyToClipboard(bike.objectId);
            Toast.makeText(getApplicationContext(), "Copied bike ID!", Toast.LENGTH_SHORT).show();
        }
    };


    private View.OnClickListener startTrackingListener = new View.OnClickListener(){
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onClick(View v) {
            if (hasPermissions()) {
                startTracking();
            } else {
                requestPermissionsWithRationale();
            }
        }
    };


    private View.OnClickListener stopTrackingListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            sendSMS(bike.phone_number, SMS_STOP_TRACKING);
            stopRepeating();
            trackingRequired = false;
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    };


    private View.OnClickListener changeLayoutState = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mMapLayoutState == MAP_LAYOUT_STATE_CONTRACTED){
                mMapLayoutState = MAP_LAYOUT_STATE_EXPANDED;
                expandMapAnimation();
            }
            else if(mMapLayoutState == MAP_LAYOUT_STATE_EXPANDED){
                mMapLayoutState = MAP_LAYOUT_STATE_CONTRACTED;
                contractMapAnimation();
            }
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
                    bike.setPhotoUrl((String) response.get("photo_url"));
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
        bikeNameView.setText(bike.name);
        bikeImageView.setVisibility(View.VISIBLE);
        new DownloadImageTask(bikeImageView).execute(bike.photo_url);
        startTrackingButton.setOnClickListener(startTrackingListener);
        copyBikeIdButton.setOnClickListener(copyToClipboardListener);
        startTrackingButton.setActivated(true);


    }

    private boolean hasPermissions() { // check if maps are available
        int res;

        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS};
        for (String perms : permissions){
            res = checkCallingOrSelfPermission(perms);
            if (res != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;

    }



    private void requestPermsLocation(){
        String[] permissions_location = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(permissions_location, PERMISSION_REQUEST_LOCATION);
        }
    }

    private void requestPermsSMS(){
        String[] permissions_sms = new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(permissions_sms, PERMISSION_REQUEST_SMS);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allowed = true;
        String allowed_param = "";
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION:
                for (int res : grantResults){
                    allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
                }

                if (allowed) allowed_param = "location";

                break;

            case PERMISSION_REQUEST_SMS:
                for (int res : grantResults){
                    allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
                }
                if (allowed) allowed_param = "sms";

                break;

            default:
                allowed = false;
                break;

        }

        if (hasPermissions()) {
            startTracking();

        } else if (allowed_param.equals("location")){
            requestPermsSMS();
        } else if (allowed_param.equals("sms")) {
            requestPermsLocation();
        }else if (!allowed){ 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                    Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show();
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)){
                    Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show();
                } else {
                    showNoLocationSMSPermissionSnackbar();
                }
            }
        }

    }

    public void showNoLocationSMSPermissionSnackbar(){
        Snackbar.make(BikeTrackActivity.this.findViewById(R.id.activity_bike_track_view), "Location and SMS permission aren`t granted", Snackbar.LENGTH_LONG)
                .setAction("SETTINGS", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openApplicationSettings();

                        Toast.makeText(getApplicationContext(),
                                "Open permissions and grant sms and location permission",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                })
                .show();
    }

    public void openApplicationSettings(){
        Intent appSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(appSettingsIntent, PERMISSION_REQUEST_LOCATION);
        startActivityForResult(appSettingsIntent, PERMISSION_REQUEST_SMS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_REQUEST_LOCATION){
            if (hasPermissions()){
                startTracking();
            }
            return;
        } else if (requestCode == PERMISSION_REQUEST_SMS){
            if (hasPermissions()){
                startTracking();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void requestPermissionsWithRationale(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)){
            final String message = "Location permission is needed to show you on map";

            Snackbar.make(BikeTrackActivity.this.findViewById(R.id.activity_bike_track_view), message, Snackbar.LENGTH_LONG)
                    .setAction("GRANT", new View.OnClickListener(){

                        @Override
                        public void onClick(View v) {
                            requestPermsLocation();
                        }
                    })
                    .show();
        }
        else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.SEND_SMS) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_SMS)) {
            final String message = "SMS permission is needed to communicate with tracker";

            Snackbar.make(BikeTrackActivity.this.findViewById(R.id.activity_bike_track_view), message, Snackbar.LENGTH_LONG)
                    .setAction("GRANT", new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            requestPermsSMS();
                        }
                    })
                    .show();
        } else {
            requestPermsSMS();
        }
    }





    public boolean isGpsEnabled(){ // IS GPS ENABLED?
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        assert manager != null;
        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
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







    void startTracking(){
        trackingRequired = true;
        changeCameraView = true;
        mProgressBar.setVisibility(View.VISIBLE);

        sendSMS(bike.phone_number, "230");  // Init tracker
        sms = checkSMS();  // Первичная СМС
        sms_id = sms.id;
        startRepeating();

    }

    public void startRepeating() {
        trackingRunnable.run();
    }
    public void stopRepeating() {
        mHandler.removeCallbacks(trackingRunnable);
    }

    private Runnable trackingRunnable = new Runnable() {

        @Override
        public void run() {

            if (!sms.id.equals(sms_id) && sms.phone.equals(bike.phone_number)) { // Got new sms
                LocalDateTime now = LocalDateTime.now();
                String time = HHmmTimeFormat.format(now);
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

                                //TODO add image of bike .icon()
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
        assert clipboard != null;
        clipboard.setPrimaryClip(clip);
    }




}




