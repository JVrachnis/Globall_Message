package com.example.test1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.test1.ui.profile.profileFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class seeProfile extends AppCompatActivity {

    private static final String TAG = "seeProfile";

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();
    private static final StorageReference storageRef = storage.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_profile);
        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        final Bundle b = getIntent().getExtras();
        final String userID =b.getString("userID");
        if(userID==null){

        }else{
            final EditText DisplayNameET = findViewById(R.id.DisplayNameEditText);
            final EditText firstname= findViewById(R.id.firstnameEditText);
            final EditText lastname= findViewById(R.id.lastnameEditText);
            final EditText birthday= findViewById(R.id.bdayEditText);
            final EditText bioEditText= findViewById(R.id.bioEditText);
            final Button addUser=findViewById(R.id.addUser);
            final Button removeUser=findViewById(R.id.removeUser);
            final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if(currentUser.getUid().equals(userID)){
                addUser.setVisibility(View.GONE);
                removeUser.setVisibility(View.GONE);
            }else{
                db.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {

                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d(TAG,document.getId());
                                List<String> friends = (List<String>)document.get("Friends");
                                if(friends!=null && friends.contains(userID)){
                                    removeUser.setVisibility(View.VISIBLE);
                                    addUser.setVisibility(View.GONE);
                                }else{
                                    removeUser.setVisibility(View.GONE);
                                    addUser.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                });
            }

            addUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    db.collection("users").document(currentUser.getUid()).update("Friends", FieldValue.arrayUnion(userID));
                    removeUser.setVisibility(View.VISIBLE);
                    addUser.setVisibility(View.GONE);
                }
            });
            removeUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    db.collection("users").document(currentUser.getUid()).update("Friends", FieldValue.arrayRemove(userID));
                    removeUser.setVisibility(View.GONE);
                    addUser.setVisibility(View.VISIBLE);
                }
            });
            db.collection("users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            DisplayNameET.setText(document.getString("Display Name"));
                            firstname.setText(document.getString("First Name"));
                            lastname.setText(document.getString("Last Name"));
                            Date date = document.getTimestamp("Birthday").toDate();
                            List<String> friends = (List<String>)document.get("Friends");
                            if(friends!=null){
                                final LinearLayout friendList = findViewById(R.id.friendList);
                                for (String friend :friends) {
                                    final String f=friend;
                                    db.collection("users").document(friend).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                DocumentSnapshot document = task.getResult();
                                                if (document.exists()) {
                                                    TextView tv = new TextView(getBaseContext());
                                                    tv.setText(document.getString("Display Name"));
                                                    tv.setTag(f);
                                                    tv.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(final View v) {
                                                            Intent intent = new Intent(getBaseContext(), seeProfile.class);
                                                            Bundle b = new Bundle();
                                                            b.putString("userID", (String) v.getTag()); //Your id
                                                            intent.putExtras(b); //Put your id to your next Intent
                                                            startActivity(intent);

                                                        }
                                                    });
                                                    friendList.addView(tv);
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                            birthday.setText(new SimpleDateFormat("dd/MM/yyyy").format(date));
                            bioEditText.setText(document.getString("Bio"));
                            final long ONE_MEGABYTE = 1024 * 1024;
                            StorageReference riversRef = storageRef.child(document.getString("Photo"));
                            riversRef.getBytes(ONE_MEGABYTE*2).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                    ImageView myImage = findViewById(R.id.profile_image);
                                    myImage.setImageBitmap(bitmap);
                                }}).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                }
                            });
                        } else {
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
        }
    }
}
