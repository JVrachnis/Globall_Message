package com.example.test1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MapMessageActivity extends AppCompatActivity {
    private static final String TAG = "MapMessageActivity";
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user;
    private StorageReference storageRef = storage.getReference();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_message);
        user = FirebaseAuth.getInstance().getCurrentUser();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final Bundle b = getIntent().getExtras();
        String messageId =b.getString("MessageId"); // or other values

        final DocumentReference docRef = db.collection("Chats").document("global").collection("Messeges").document(messageId);
        final Button btn = findViewById(R.id.Commendbtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                btn.setVisibility(View.INVISIBLE);
                final LinearLayout CommentSection =findViewById(R.id.CommentSection);
                final EditText et = new EditText(getBaseContext());
                CommentSection.addView(et);

                final FloatingActionButton Commendfab = findViewById(R.id.Commendfab);
                Commendfab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        ((ViewManager)et.getParent()).removeView(et);
                        String message = et.getText().toString();
                        Map<String, Object> msg = new HashMap<>();
                        msg.put("userName",user.getDisplayName());
                        msg.put("Uid",user.getUid());
                        msg.put("message", message);
                        msg.put("timeSend", new Timestamp(new Date()) );
                        docRef.collection("Comments").add(msg);
                        Commendfab.hide();
                        btn.setVisibility(View.VISIBLE);
                    }
                });
                Commendfab.show();
            }
        });
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {

                    final DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        db.collection("users").document(document.getString("Uid")).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot doc = task.getResult();
                                    if (doc.exists()) {

                                        String dir = document.get("highQualityDir").toString();
                                        String imageLowQuality = document.get("lowQualityDir").toString();
                                        final String msgId = document.getId();
                                        final long ONE_MEGABYTE = 1024 * 1024;
                                        Log.d(TAG, "New Msg: " + msgId);

                                        TextView tv = findViewById(R.id.mapMessageTitleTV);
                                        tv.setText(document.get("Title").toString());
                                        TextView message = findViewById(R.id.mapMessageTV);
                                        message.setText(document.get("message").toString());
                                        TextView dateTV = findViewById(R.id.dateTV);

                                        String displayName=doc.getString("Display Name");
                                        TextView displayNameTV =findViewById(R.id.displayNameTV);
                                        displayNameTV.setText(displayName);
                                        displayNameTV.setTag(doc.getId());
                                        displayNameTV.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(final View v) {
                                                Intent intent = new Intent(getBaseContext(), seeProfile.class);
                                                Bundle b = new Bundle();
                                                b.putString("userID", (String) v.getTag()); //Your id
                                                intent.putExtras(b); //Put your id to your next Intent
                                                startActivity(intent);

                                            }
                                        });
                                        dateTV.setText(new SimpleDateFormat("dd/MM/yyyy-HH:mm:ss").format(document.getDate("timeSend")));

                                        StorageReference riversRef = storageRef.child(imageLowQuality);
                                        riversRef.getBytes(ONE_MEGABYTE / 4).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                                                //bitmap = Bitmap.createScaledBitmap(bitmap, 200,200, true);
                                                drawable.setCircular(true);
                                                drawable.setCornerRadius(50);

                                                ImageView myImage = findViewById(R.id.messageImageView);
                                                myImage.setImageDrawable(drawable);
                                                addCommentListener(docRef,msgId,document.getString("Uid"),bitmap, (LinearLayout) findViewById(R.id.CommentSection));
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception exception) {
                                                // Handle any errors
                                            }
                                        });
                                        StorageReference riversRef1 = storageRef.child(dir);

                                        riversRef1.getBytes(ONE_MEGABYTE * 10).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                                                //bitmap = Bitmap.createScaledBitmap(bitmap, 200,200, true);
                                                drawable.setCircular(true);
                                                drawable.setCornerRadius(50);

                                                ImageView myImage = findViewById(R.id.messageImageView);
                                                myImage.setImageDrawable(drawable);

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception exception) {
                                                // Handle any errors
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }
    private void addCommentListener(DocumentReference docRef,final String messageID,final String messageUserID,final Bitmap bitmap,final LinearLayout CommentSection){
        final CollectionReference c = docRef.collection("Comments");
        
        c.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }


                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    final DocumentSnapshot doc = dc.getDocument();
                    if(dc.getType()== DocumentChange.Type.ADDED){
                        Log.d(TAG,messageID);


                        db.collection("users").document(doc.getString("Uid")).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        final LinearLayout Llout=new LinearLayout(getBaseContext());
                                        final LinearLayout cLlout;
                                        Llout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                        Llout.setOrientation(LinearLayout.VERTICAL);
                                        Llout.setTag(doc.getId());
                                        if((messageUserID.equals(user.getUid()))&&(!user.getUid().equals(doc.getString("Uid")))){
                                            handleNotifications(document.getId(),doc.getId(),document.getString("Display Name"),doc.getString("message"),bitmap);
                                        }
                                        final TextView tvuser = new TextView(getBaseContext());
                                        tvuser.setText(document.getString("Display Name"));
                                        tvuser.setTag(doc.getString("Uid"));
                                        tvuser.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(final View v) {
                                                Intent intent = new Intent(getBaseContext(), seeProfile.class);
                                                Bundle b = new Bundle();
                                                b.putString("userID", (String) v.getTag()); //Your id
                                                intent.putExtras(b); //Put your id to your next Intent
                                                startActivity(intent);

                                            }
                                        });
                                        Llout.addView(tvuser);
                                        Space s = new Space(getBaseContext());
                                        Llout.addView(s);

                                        ViewGroup.LayoutParams lp = s.getLayoutParams();
                                        lp.height=3;
                                        s.setLayoutParams(lp);



                                        final TextView tv = new TextView(getBaseContext());
                                        tv.setText(doc.getString("message"));
                                        tv.setTag(doc.getId());

                                        tv.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(final View v) {
                                                final LinearLayout lout =CommentSection.findViewWithTag(v.getTag());
                                                final EditText et = new EditText(getBaseContext());
                                                lout.addView(et);

                                                final FloatingActionButton Commendfab = findViewById(R.id.Commendfab);
                                                Commendfab.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                                        ((ViewManager)et.getParent()).removeView(et);
                                                        String message = et.getText().toString();
                                                        Map<String, Object> msg = new HashMap<>();
                                                        msg.put("userName",user.getDisplayName());
                                                        msg.put("Uid",user.getUid());
                                                        msg.put("message", message);
                                                        msg.put("timeSend", new Timestamp(new Date()) );
                                                        doc.getReference().collection("Comments").add(msg);
                                                        Commendfab.hide();
                                                    }
                                                });
                                                Commendfab.show();
                                            }
                                        });

                                        Llout.addView(tv);



                                        s = new Space(getBaseContext());
                                        Llout.addView(s);
                                        lp = s.getLayoutParams();
                                        lp.height=10;
                                        s.setLayoutParams(lp);

                                        cLlout=new LinearLayout(getBaseContext());
                                        LinearLayout.LayoutParams cLloutParams = new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                        cLlout.setOrientation(LinearLayout.VERTICAL);
                                        cLloutParams.setMargins(15, 5, 0, 5);
                                        cLlout.setLayoutParams(cLloutParams);
                                        addCommentListener(doc.getReference(),messageID,messageUserID,bitmap,cLlout);
                                        Llout.addView(cLlout);
                                        CommentSection.addView(Llout);
                                    }
                                }
                            }
                        });




                    }
                }
            }
        });
    }
    private void handleNotifications(final String uid ,final String messageTag,final String messageTitle,final String messageBody,final Bitmap largeIcon) {

        db.collection("users").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        List<String> notifications = (List<String>)document.get("CommentNotifications");

                        if((notifications==null || !notifications.contains(messageTag))){
                            db.collection("users").document(user.getUid()).update("CommentNotifications", FieldValue.arrayUnion(messageTag));
                            sendNotification(messageTag, messageTitle, messageBody, largeIcon);
                        }

                    }
                }
            }
        });
    }
    private void sendNotification(String messageTag,String messageTitle,String messageBody,Bitmap largeIcon) {
        Intent intent = new Intent(this,MapMessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("MessageId",messageTag);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                0);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
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
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

}
