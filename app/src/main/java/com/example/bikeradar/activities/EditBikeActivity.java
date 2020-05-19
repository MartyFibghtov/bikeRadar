package com.example.bikeradar.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.files.BackendlessFile;
import com.example.bikeradar.AddBikeService;
import com.example.bikeradar.Constants;
import com.example.bikeradar.DownloadImageTask;
import com.example.bikeradar.R;
import com.example.bikeradar.classes.Bike;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EditBikeActivity extends AppCompatActivity {

    private EditText nameField;
    private EditText phoneNumberField;
    private Button submitButton;
    private ImageView bikePictureView;
    private String bikePictureUrl;
    private Button addPictureButton;
    private Button deleteBikeButton;
    private String currentPhotoPath;
    private String bikeId;
    private Bike bike;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bike);

        //  Получение ID велосипеда
        Intent intent = getIntent();
        bikeId = intent.getStringExtra("bikeId");


        //  Создание объекта Bike
        bike = new Bike();
        getBike(bikeId);


        //  Нахождение кнопок
        addPictureButton = findViewById(R.id.add_picture_button);
        deleteBikeButton = findViewById(R.id.delete_bike_button);
        submitButton = findViewById(R.id.submit_button);

        //  Присвоение кнопок
        addPictureButton.setOnClickListener(uploadPhotoButtonListener);
        deleteBikeButton.setOnClickListener(deleteButtonListener);
        submitButton.setOnClickListener(submitButtonListener);



        //  Текстовые поля
        nameField = findViewById(R.id.name_field);
        phoneNumberField = findViewById(R.id.phone_number_field);


        //  Фотография велосипеда
        bikePictureView = findViewById(R.id.bike_image_view);





    }


    private View.OnClickListener uploadPhotoButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TakePictureIntent();
        }
    };

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
                updateBike(name, phoneNumber, bikeId);
            } else {
                Toast.makeText(getApplicationContext(), "Not a phone Number", Toast.LENGTH_LONG).show();
            }
        }
    };



    private void getBike(final String bikeId) {
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
                }
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e("Getting bike", fault.getMessage());
            }
        });
    }


    private void onGotBike(){
        new DownloadImageTask(bikePictureView).execute(bike.photo_url);
        nameField.setText(bike.name);
        phoneNumberField.setText(bike.phone_number);
    }








    private void updateBike(String name, String phoneNumber, String bikeId) {
        HashMap<String, String> bike_map = new HashMap<String, String>();
        bike_map.put( "name", name );
        bike_map.put( "phone_number", phoneNumber );
        bike_map.put("objectId", bikeId);
        bike_map.put("photo_url", bike.photo_url);
        Backendless.Data.of( "bikes" ).save(bike_map, new AsyncCallback<Map>() {
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

    private void deleteBike(){
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
                        HashMap<String, String> bike_map = new HashMap<String, String>();
                        bike_map.put("objectId", bikeId);

                        Backendless.Data.of("bikes").remove(bike_map, new AsyncCallback<Long>() {
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
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPic();
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
        bikePictureView.setVisibility(View.VISIBLE);
        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), contentUri);
        try {
            Bitmap bitmap = ImageDecoder.decodeBitmap(source);
            bikePictureView.setImageBitmap(bitmap);
            uploadPhoto(contentUri);


        } catch (IOException e) {
            e.printStackTrace();
        }


        mediaScanIntent.setData(contentUri);
        EditBikeActivity.this.sendBroadcast(mediaScanIntent);
    }

    private void uploadPhoto(Uri imageUri){
        Toast.makeText(getApplicationContext(), "Uploading photo", Toast.LENGTH_SHORT).show();
        submitButton.setClickable(false);
        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageUri);
        try {
            Bitmap photo = ImageDecoder.decodeBitmap(source);
            Bitmap converetdImage = getResizedBitmap(photo, 300);
            Backendless.Files.Android.upload(converetdImage,
                    Bitmap.CompressFormat.PNG,
                    100,
                    imageUri.getLastPathSegment(),
                    "bike_photos",
                    new AsyncCallback<BackendlessFile>() {
                        @Override
                        public void handleResponse(BackendlessFile response) {
                            bike.photo_url = response.getFileURL();
                            submitButton.setClickable(true);
                            Toast.makeText(getApplicationContext(), "Picture uploaded", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void handleFault(BackendlessFault fault) {
                            Toast.makeText(getApplicationContext(), fault.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });



        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }


}
