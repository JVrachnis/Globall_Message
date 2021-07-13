package com.example.test1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MapSendMessageActivity extends AppCompatActivity {
    private static final String TAG = "MapSendMessageActivity";
    static final int REQUEST_IMAGE_CAPTURE = 3;
    private Uri photoURI;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FusedLocationProviderClient fusedLocationClient;
    private SensorManager mSensorManager;
    private Sensor mOrientation;
    StorageReference storageRef = storage.getReference();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_send_message);

        mSensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ImageView iv = findViewById(R.id.sendMessageImageView);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(MapSendMessageActivity.this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    // Logic to handle location object
                                    EditText titleET = findViewById(R.id.mapSendMessageTitleET);
                                    EditText messageET = findViewById(R.id.mapSendMessageTextET);
                                    String errorMessage="";
                                    if(photoURI==null){
                                        errorMessage+="select an image\n";
                                    }
                                    String title=titleET.getText().toString();
                                    if(title == null ||title.isEmpty()){
                                        errorMessage+="add a title\n";
                                    }
                                    String message=messageET.getText().toString();

                                    if(!errorMessage.isEmpty()){
                                        errorMessage = errorMessage.substring(0, errorMessage.length() - 1);
                                        Toast.makeText(MapSendMessageActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                    }else{
                                        uploadMessage(title,message,photoURI,new Timestamp(new Date()),new GeoPoint(location.getLatitude(), location.getLongitude()));
                                    }


                                }
                            }
                        });
            }
        });
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PNG_" + timeStamp + "_";
        File storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        //Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        //photoPickerIntent.setType("image/*");
        //startActivityForResult(photoPickerIntent, REQUEST_IMAGE_CAPTURE);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.test1.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

            }

        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE ) {
            if (resultCode == RESULT_OK) {
                Uri file = photoURI;
                ImageView iv = findViewById(R.id.sendMessageImageView);
                iv.setImageURI(file);
            }else {
                Log.d(TAG, "Image Uri: " + resultCode );
            }
        }
    }
    private void uploadMessage(final String Title,final String message,Uri imageUri,final Timestamp ts,final GeoPoint gp){
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        try {
            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            final String imageHighQuality = "images/highQuality/"+user.getUid()+"/"+ UUID.randomUUID().toString();
            final String imageLowQuality = "images/lowQuality/"+user.getUid()+"/"+UUID.randomUUID().toString();
            imageBitmap = RotateBitmap(imageBitmap,90);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.WEBP, 90, stream);
            byte[] byteArrayHighQuality = stream.toByteArray();

            stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.WEBP, 20, stream);
            final byte[] byteArrayLowQuality = stream.toByteArray();
            imageBitmap.recycle();
            StorageReference riversRef = storageRef.child(imageHighQuality);

            UploadTask uploadTask = riversRef.putBytes(byteArrayHighQuality);
            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    StorageReference riversRef = storageRef.child(imageLowQuality);
                    UploadTask uploadTask2 = riversRef.putBytes(byteArrayLowQuality);
                    uploadTask2.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            Map<String, Object> msg = new HashMap<>();
                            msg.put("userName",user.getDisplayName());
                            msg.put("Uid",user.getUid());
                            msg.put("highQualityDir", imageHighQuality);
                            msg.put("lowQualityDir",imageLowQuality);
                            msg.put("Title", Title);
                            msg.put("message", message);
                            msg.put("timeSend", ts );
                            msg.put("latLog", gp);
                            db.collection("Chats").document("global").collection("Messeges").add(msg);
                            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                            // ...
                        }
                    });
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    // ...
                }
            });
            finish();
        }catch (Exception e){
            //Toast.makeText(this,"Couldnt upload, Select image",3*Toast.LENGTH_LONG);
            Toast.makeText(this, "Couldnt upload, Select image", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error " + e );
        }
    }
    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

}
