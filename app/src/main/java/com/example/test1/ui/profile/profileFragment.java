package com.example.test1.ui.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.test1.R;
import com.example.test1.seeProfile;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;


public class profileFragment extends Fragment {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth myauth=FirebaseAuth.getInstance();
    private static final String TAG = "profile";
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();
    private static final StorageReference storageRef = storage.getReference();
    private FirebaseUser user;
    private boolean firstTime=true;
    private boolean imageSelected=false;
    private String userID;
    private Calendar birthday_cldr = Calendar.getInstance();
    private DatePickerDialog picker2;
    private List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.PhoneBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build());
    //private DocumentReference documentReference=db.collection("users").document("xypHobyOHyh2JhxoYJk7");
    private static final int RC_SIGN_IN=2;
    private Uri photoURI;
    View root;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.profile, container, false);
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user ==null){
            signIn();

            return  root;
        }
        userID=user.getUid();


        final  EditText DisplayNameET = root.findViewById(R.id.DisplayNameEditText);
        DisplayNameET.setText(user.getDisplayName());
        final EditText firstname= root.findViewById(R.id.firstnameEditText);
        final EditText lastname= root.findViewById(R.id.lastnameEditText);
        final EditText birthday= root.findViewById(R.id.bdayEditText);
        final EditText bioEditText= root.findViewById(R.id.bioEditText);
        final ImageView profilePick= root.findViewById(R.id.profile_image);
        profilePick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
        birthday.setInputType(InputType.TYPE_NULL);

        birthday_cldr.set(Calendar.HOUR_OF_DAY,23);
        birthday_cldr.set(Calendar.MINUTE,59);
        birthday_cldr.set(Calendar.SECOND,59);
        birthday.setText(birthday_cldr.get(Calendar.DAY_OF_MONTH)+ "/" +(birthday_cldr.get(Calendar.MONTH)+1)+ "/" +(birthday_cldr.get(Calendar.YEAR)-18));
        birthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar cldr = Calendar.getInstance();
                cldr.set(Calendar.YEAR,cldr.get(Calendar.YEAR)-18);
                int day = cldr.get(Calendar.DAY_OF_MONTH);
                int month = cldr.get(Calendar.MONTH);
                int year = cldr.get(Calendar.YEAR);
                // date picker dialog
                picker2 = new DatePickerDialog(getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                birthday.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                                birthday_cldr.set(year, monthOfYear, dayOfMonth,23,59,59);
                            }
                        }, year, month, day);
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day);

                picker2.getDatePicker().setMaxDate(calendar.getTimeInMillis());
                picker2.show();
            }
        });

        final Button save =root.findViewById(R.id.addUser);
        final Button signout=root.findViewById(R.id.singout);
        final Button delete=root.findViewById(R.id.delete);
        db.collection("users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        firstTime=false;
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        DisplayNameET.setText(document.getString("Display Name"));
                        firstname.setText(document.getString("First Name"));
                        lastname.setText(document.getString("Last Name"));
                        birthday_cldr.setTime(document.getTimestamp("Birthday").toDate());
                        Date date = document.getTimestamp("Birthday").toDate();

                        birthday.setText(new SimpleDateFormat("dd/MM/yyyy").format(date));
                        bioEditText.setText(document.getString("Bio"));
                        final long ONE_MEGABYTE = 1024 * 1024;
                        Log.d(TAG,"photo:"+document.getString("Photo"));
                        StorageReference riversRef = storageRef.child(document.getString("Photo"));
                        riversRef.getBytes(ONE_MEGABYTE*2).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                Log.d(TAG,"photo ok:");
                                ImageView myImage = root.findViewById(R.id.profile_image);
                                myImage.setImageBitmap(bitmap);
                                imageSelected=true;
                            }}).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle any errors
                            }
                        });
                        List<String> friends = (List<String>)document.get("Friends");
                        if(friends!=null){
                            final LinearLayout friendList = root.findViewById(R.id.friendList);
                            for (String friend :friends) {
                                final String f=friend;
                                db.collection("users").document(friend).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document.exists()) {
                                                TextView tv = new TextView(getActivity());
                                                tv.setText(document.getString("Display Name"));
                                                tv.setTag(f);
                                                tv.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(final View v) {
                                                        Intent intent = new Intent(getActivity(), seeProfile.class);
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
                    } else {
                        firstTime=true;
                        DisplayNameET.setText(user.getDisplayName());

                        //profilePick.setImageDrawable(LoadImageFromWebOperations());
                        if(user.getPhotoUrl() != null){
                            imageSelected=true;
                            new GetImageFromUrl(profilePick).execute(user.getPhotoUrl().toString());
                        }else{
                            imageSelected=false;
                        }
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String,Object> docData=new HashMap<>();
                String DisplayName= DisplayNameET.getText().toString();

                String errorMessage="";
                if(!imageSelected){
                    errorMessage+="select an image\n";
                }
                if(DisplayName.length()<3){
                    errorMessage+="At least 3 characters long Display Name\n";
                }

                if(!errorMessage.isEmpty()){
                    errorMessage = errorMessage.substring(0, errorMessage.length() - 1);
                    Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
                }else {

                    docData.put("Display Name", DisplayName);
                    docData.put("First Name", firstname.getText().toString());
                    docData.put("Last Name", lastname.getText().toString());
                    docData.put("Birthday", new Timestamp(new Date(birthday_cldr.getTimeInMillis())));

                    Bitmap bm = ((BitmapDrawable) profilePick.getDrawable()).getBitmap();
                    String photoDir = uploadImage(bm);
                    if (photoDir != null) {
                        docData.put("Photo", photoDir);
                        docData.put("Bio", bioEditText.getText().toString());
                        db.collection("users").document(userID).set(docData);
                        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                        if (firstTime) {
                            navController.navigate(R.id.nav_maps);
                        }
                        Toast.makeText(getContext(), "Changes has been saved", Toast.LENGTH_LONG).show();

                    }else {
                        Toast.makeText(getContext(), "ErrorWithImage", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //db.collection("users").document(userID).delete();

                Toast.makeText(getContext(),"Profile has been deleted",Toast.LENGTH_LONG).show();

                deleteUser();
            }
        });
        signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();

                //Intent i=new Intent(this, )
            }
        });



        return root;
    }
    private  void deleteUser(){
        AuthUI.getInstance()
                .delete(getActivity())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        signIn();
                    }
                });
    }
    private void signOut() {
        AuthUI.getInstance()
                .signOut(getActivity())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        signIn();
                    }
                });
    }
    private void signIn() {
        //NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        //navController.navigate(R.id.nav_profile);
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(true).build(),
                RC_SIGN_IN);

    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Toast.makeText(getActivity(), "Successfully signed in", Toast.LENGTH_LONG).show();
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                navController.navigate(R.id.nav_profile);

                // ...
            } else {
                signIn();
            }
        }else if (requestCode == REQUEST_IMAGE_CAPTURE ) {
            if (resultCode == RESULT_OK) {
                Uri file = photoURI;
                ImageView iv = root.findViewById(R.id.profile_image);
                iv.setImageURI(file);
                imageSelected=true;
            }else {
                Log.d(TAG, "Image Uri: " + resultCode );
            }
        }


    }

    public class GetImageFromUrl extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;
        public GetImageFromUrl(ImageView img){
            this.imageView = img;
        }
        @Override
        protected Bitmap doInBackground(String... url) {
            String stringUrl = url[0];
            Bitmap bitmap = null;
            InputStream inputStream;
            try {
                inputStream = new java.net.URL(stringUrl).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap){
            super.onPostExecute(bitmap);
            imageView.setImageBitmap(bitmap);
        }
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PNG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(getActivity(),
                        "com.example.test1.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

            }

        }
    }
    private String uploadImage(Bitmap imageBitmap){
        //Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), photoURI);
        final String imageProfilePick = "images/profile/"+user.getUid()+"/"+ UUID.randomUUID().toString();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.WEBP, 40, stream);
        final byte[] byteArrayProfile = stream.toByteArray();
        imageBitmap.recycle();
        StorageReference riversRef = storageRef.child(imageProfilePick);
        riversRef.putBytes(byteArrayProfile);
        return imageProfilePick;
    }
}



