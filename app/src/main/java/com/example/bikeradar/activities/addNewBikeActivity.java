package com.example.bikeradar.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.DirectAction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.example.bikeradar.AddBikeService;
import com.example.bikeradar.Constants;
import com.example.bikeradar.R;
import com.example.bikeradar.classes.Bike;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class addNewBikeActivity extends AppCompatActivity {
    public EditText nameField;
    public EditText phoneNumberField;
    public Button addPictureButton;
    public Button submitButton;
    String currentPhotoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_bike);

        nameField = findViewById(R.id.name_field);
        phoneNumberField = findViewById(R.id.phone_number_field);

        addPictureButton = findViewById(R.id.add_picture_button);
        submitButton = findViewById(R.id.submit_button);

        addPictureButton.setOnClickListener(uploadPhotoButtonListener);
        submitButton.setOnClickListener(submitButtonListener);



        // TODO add bike to user
    }

    private View.OnClickListener uploadPhotoButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TakePictureIntent();
        }
    };

    private View.OnClickListener submitButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //String regexStr = "^(\\s*)?(\\+)?([- _():=+]?\\d[- _():=+]?){10,14}(\\s*)?$";
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






    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    private void TakePictureIntent() {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // проверяем, что есть приложение способное обработать интент
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // создать файл для фотографии
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    // ошибка, возникшая в процессе создания файла
                }

                // если файл создан, запускаем приложение камеры
                if (photoFile != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                } else {
                    Log.i("File", "Not created");
                }
            }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            galleryAddPic();
            File f = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            assert f != null;
            File[] files = f.listFiles();
            for (File inFile : files) {
                System.out.println(inFile.getName());
            }
        } else {
            Log.i("Taking photo", String.valueOf(requestCode) + String.valueOf(resultCode));
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (!storageDir.exists()){
            Boolean res = storageDir.mkdirs();
        }
        Log.i("Path _to storage", storageDir.getAbsolutePath());
        Log.i("Storage exists:", String.valueOf(storageDir.exists()));

        File image = File.createTempFile(imageFileName, ".jpg", storageDir.getAbsoluteFile());
        currentPhotoPath = image.getAbsolutePath();
        Log.i("path to photo:", currentPhotoPath);

        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        addNewBikeActivity.this.sendBroadcast(mediaScanIntent);
    }

}