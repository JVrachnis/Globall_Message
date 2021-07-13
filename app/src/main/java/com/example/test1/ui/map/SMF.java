package com.example.test1.ui.map;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.GpsStatus;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.test1.MainActivity;
import com.example.test1.MapMessageActivity;
import com.example.test1.MapSendMessageActivity;
import com.example.test1.R;
import com.example.test1.SearchActivity;
import com.example.test1.ui.profile.profileFragment;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.google.firebase.firestore.model.Document;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import static android.app.Activity.RESULT_OK;

public class SMF extends Fragment {
    private static final String TAG = "SMF";
    private SupportMapFragment mapFragment;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION=1;
    private final int REQ_CODE_SPEECH_INPUT=2;
    private final int NOTIFICATION=4;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    //private String currentPhotoPath;
    private GoogleMap gMap ;
    private FirebaseUser user;
    private StorageReference storageRef = storage.getReference();
    private Uri photoURI;
    private ArrayList<Marker> markers =new ArrayList<Marker>();
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private Context context;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity().getBaseContext();
        View rootView = inflater.inflate(R.layout.activity_maps, container, false);
        user = FirebaseAuth.getInstance().getCurrentUser();
        FloatingActionButton fab_camera = rootView.findViewById(R.id.fab_camera);
        fab_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(context, MapSendMessageActivity.class);
                startActivity(intent);
                //dispatchTakePictureIntent();

            }
        });

        FloatingActionButton fab_search = rootView.findViewById(R.id.fab_search);
        fab_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(context, SearchActivity.class);
                startActivity(intent);

            }
        });
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        //if permission not granted

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            navController.navigate(R.id.nav_profile);
            //request permission

        }else {
            //if permission granted
            //initialize gps provider


        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    gMap= googleMap;
                    gMap.setMyLocationEnabled(true);
                    gMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            Intent intent = new Intent(context, MapMessageActivity.class);
                            Bundle b = new Bundle();
                            b.putString("MessageId", (String) marker.getTag()); //Your id
                            intent.putExtras(b); //Put your id to your next Intent
                            startActivity(intent);

                            return false;
                        }
                    });
                }
            });

        }
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                for(final Marker marker: markers){

                    DocumentReference docRef = db.collection("Chats").document("global").collection("Messeges").document(marker.getTag().toString());
                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {

                                final DocumentSnapshot document = task.getResult();
                                if (document.exists()) {

                                    String uid = document.getString("Uid");
                                    Date timeSend = document.getDate("timeSend");
                                    String title = document.getString("title");

                                    marker.setVisible(IsInSearchRange(uid,timeSend ,title));

                                } else {
                                    Log.d(TAG, "No such document");
                                }
                            } else {
                                Log.d(TAG, "get failed with ", task.getException());
                            }
                        }
                    });
                }
            }

        };
        SharedPreferences prefs = context.getSharedPreferences("messegesViewable"+user.getUid(), 0);
        prefs.registerOnSharedPreferenceChangeListener(listener);


        getChildFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();

        db.collection("Chats").document("global").collection("Messeges").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("TAG", "listen:error", e);
                    return;
                }
                //final LinearLayout LLout = rootView.findViewById(R.id.image_layout);

                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    final DocumentSnapshot doc = dc.getDocument();
                    if(doc.get("lowQualityDir") !=null){

                        switch (dc.getType()) {
                            case ADDED:
                                final String uid=doc.getString("Uid");
                                final String title=doc.getString("title");
                                final Date timeSend=doc.getDate("timeSend");

                                String dir = doc.get("lowQualityDir").toString();
                                final String msgId = doc.getId();
                                StorageReference riversRef = storageRef.child(dir);

                                final long ONE_MEGABYTE = 1024 * 1024;
                                riversRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                    @Override
                                    public void onSuccess(byte[] bytes) {
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(),bitmap);
                                        drawable.setCircular(true);
                                        drawable.setCornerRadius(50);

                                        final ImageView myImage = new ImageView(context);
                                        myImage.setImageDrawable(drawable);
                                        //LLout.addView(myImage);
                                        GeoPoint gp = doc.getGeoPoint("latLog");
                                        LatLng latLng = new LatLng(gp.getLatitude(),gp.getLongitude());

                                        Marker marker = gMap.addMarker(new MarkerOptions().position(latLng)
                                                .title(title).icon(BitmapDescriptorFactory.fromBitmap(addWhiteBorder(roundCorner(Bitmap.createScaledBitmap(bitmap, 200,200, true),360f)))));
                                        marker.setTag(msgId);
                                        markers.add(marker);

                                        if(IsInSearchRange(uid,timeSend ,title)) {
                                            gMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                                        }else{
                                            marker.setVisible(false);
                                        }
                                        handleNotifications(uid,msgId,doc.get("Title").toString(),doc.get("message").toString(),roundCorner(bitmap,60f));
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle any errors
                                    }
                                });

                                break;
                            case MODIFIED:
                                Log.d(TAG, "Modified Msg: " + dc.getDocument().getId());

                                break;
                            case REMOVED:
                                Log.d(TAG, "Removed Msg: " + dc.getDocument().getId());
                                break;
                        }
                    }
                }

            }
        });

        }
        return rootView;
    }

    private void handleNotifications(final String uid ,final String messageTag,final String messageTitle,final String messageBody,final Bitmap largeIcon) {
        db.collection("users").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        List<String> notifications = (List<String>)document.get("Notifications");
                        List<String> friends = (List<String>)document.get("Friends");

                        if((notifications==null || !notifications.contains(messageTag)&&(friends!=null && friends.contains(uid)))){
                            db.collection("users").document(user.getUid()).update("Notifications", FieldValue.arrayUnion(messageTag));
                            sendNotification(messageTag, messageTitle, messageBody, largeIcon);
                        }

                    }
                }
            }
        });
    }
    private void sendNotification(String messageTag,String messageTitle,String messageBody,Bitmap largeIcon) {
        Intent intent = new Intent(context,MapMessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("MessageId",messageTag);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                0);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(messageBody))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PNG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    public static Bitmap roundCorner(Bitmap src, float round) {
        // image size
        int width = src.getWidth();
        int height = src.getHeight();
        // create bitmap output
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // set canvas for painting
        Canvas canvas = new Canvas(result);
        canvas.drawARGB(0, 0, 0, 0);

        // config paint
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);

        // config rectangle for embedding
        final Rect rect = new Rect(0, 0, width, height);
        final RectF rectF = new RectF(rect);

        // draw rect to canvas
        canvas.drawRoundRect(rectF, round, round, paint);

        // create Xfer mode
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        // draw source image to canvas
        canvas.drawBitmap(src, rect, rect, paint);

        // return final image
        return result;
    }
    private Bitmap addWhiteBorder(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int radius = Math.min(h / 2, w / 2);
        Bitmap output = Bitmap.createBitmap(w + 8, h + 8, Bitmap.Config.ARGB_8888);

        Paint p = new Paint();
        p.setAntiAlias(true);

        Canvas c = new Canvas(output);
        c.drawARGB(0, 0, 0, 0);
        p.setStyle(Paint.Style.FILL);

        c.drawCircle((w / 2) + 4, (h / 2) + 4, radius, p);

        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        c.drawBitmap(bitmap, 4, 4, p);
        p.setXfermode(null);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.WHITE);
        p.setStrokeWidth(3);
        c.drawCircle((w / 2) + 4, (h / 2) + 4, radius, p);

        return output;
    }
    private boolean IsInSearchRange(String uid,Date date ,String title){

        SharedPreferences prefs = context.getSharedPreferences("messegesViewable"+user.getUid(), 0);

        Set<String> set = prefs.getStringSet("friendsViewable",(new HashSet<String>()));
        boolean viewable=false;
        if (set.isEmpty()){
            viewable=  true;
        }else if(uid ==null){
            viewable= false;
        }
        else{
            viewable= set.contains(uid);
        }
        if(!prefs.getBoolean("EndDateCurrent",true)){
            long dateEndmill= prefs.getLong("dateEnd",0l);
            viewable = viewable && (date.before(new Date(dateEndmill)));
        }
        long dateStartmill= prefs.getLong("dateStart",0l);
        viewable = viewable && (date.after(new Date(dateStartmill)));
        return viewable;
    }
}
