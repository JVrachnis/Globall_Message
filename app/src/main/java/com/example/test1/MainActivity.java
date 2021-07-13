package com.example.test1;

import android.Manifest;
import androidx.fragment.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.example.test1.ui.login.LoginActivity;
import com.example.test1.ui.profile.profileFragment;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.storage.StorageReference;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.floor;

public class MainActivity extends AppCompatActivity  implements SensorEventListener {
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN=2;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION=1;
    private AppBarConfiguration mAppBarConfiguration;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private GoogleMap mMap;
    private boolean northOnce=false;
    private SensorManager mSensorManager;
    private List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.PhoneBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_maps,R.id.nav_findFriend,R.id.nav_profile)
                .setDrawerLayout(drawer)
                .build();
        final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        navController.navigate(R.id.nav_profile);
        final FirebaseUser user = mAuth.getCurrentUser();
        if(mAuth.getCurrentUser() == null){

            signIn();
            navController.navigate(R.id.nav_profile);
        }else{

            Toast.makeText(this, "Already logged in", Toast.LENGTH_LONG).show();
            db.collection("users").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            navController.navigate(R.id.nav_maps);
                        } else {
                            navController.navigate(R.id.nav_profile);
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
            navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                @Override
                public void onDestinationChanged(@NonNull NavController controller,
                                                 @NonNull final NavDestination destination, @Nullable Bundle arguments) {
                    if(destination.getId() == R.id.nav_maps) {
                        if(!GpsPermited()){
                            checkGpsPermissions();
                        }
                    }
                    db.collection("users").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if(!document.exists() && destination.getId() != R.id.nav_profile){
                                    Toast.makeText(MainActivity.this, "please complete your profile", Toast.LENGTH_LONG).show();
                                    navController.navigate(R.id.nav_profile);
                                }
                            } else {
                                Log.d(TAG, "get failed with ", task.getException());
                            }
                        }
                    });
                }
            });



        }

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    private  boolean GpsPermited(){
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    protected void checkGpsPermissions(){
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        //if permission not granted

        if (!GpsPermited()) {
            //request permission
            navController.navigate(R.id.nav_profile);
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

        }else {
            //if permission granted
            //initialize gps provider
            navController.navigate(R.id.nav_maps);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkGpsPermissions();

                }
                return;
            }

        }
    }

    private void signIn() {

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Log.d(TAG,"NOTIFICATION"+requestCode);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                mAuth = FirebaseAuth.getInstance();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Toast.makeText(this, "Successfully signed in", Toast.LENGTH_LONG).show();
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

                Fragment currentFragment = getSupportFragmentManager().findFragmentById(navController.getCurrentDestination().getId());
                if (currentFragment instanceof profileFragment) {
                    FragmentTransaction fragTransaction =   getSupportFragmentManager().beginTransaction();
                    fragTransaction.detach(currentFragment);
                    fragTransaction.attach(currentFragment);
                    fragTransaction.commit();}

                // ...
            } else {
                signIn();
            }
        }


    }
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        switch (event.sensor.getType()){
            case Sensor.TYPE_LINEAR_ACCELERATION:
                float totalAcceleration = abs(event.values[0]) + abs(event.values[1]) + abs(event.values[2]);
                //Log.d(TAG, "totalAcceleration:" + totalAcceleration);
                if (totalAcceleration > 40) {
                    navController.navigate(R.id.nav_findFriend);
                }
                break;
            case Sensor.TYPE_PROXIMITY:
                boolean close = abs(event.values[0])<0.5f;
                if(close) {
                    navController.navigate(R.id.nav_profile);
                }
                break;
            case Sensor.TYPE_ORIENTATION:
                float rotation = abs(event.values[0]);
                if (((rotation<6)||(rotation>354))&&!northOnce) {
                    navController.navigate(R.id.nav_maps);
                    northOnce=true;
                }else if((rotation>160)&&(rotation<200)){
                    northOnce=false;
                }
                break;
        }
    }
}
