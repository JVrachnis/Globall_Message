package com.example.test1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";
    private EditText dateStart_editText,dateEnd_editText;
    private Calendar dateStart_cldr= Calendar.getInstance(),dateEnd_cldr = Calendar.getInstance();
    private DatePickerDialog picker1,picker2;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final SharedPreferences prefs = getSharedPreferences("messegesViewable"+currentUser.getUid(), 0);


        dateStart_editText=(EditText) findViewById(R.id.dateStart_editText);
        dateStart_editText.setInputType(InputType.TYPE_NULL);
        long dateStartmill= prefs.getLong("dateStart",0l);
        if(dateStartmill!=0&&!prefs.getBoolean("OnlyToday",true)){
            dateStart_cldr.setTimeInMillis(dateStartmill);
        }else{
            dateStart_cldr.set(Calendar.HOUR_OF_DAY,0);
            dateStart_cldr.set(Calendar.MINUTE,0);
            dateStart_cldr.set(Calendar.SECOND,0);
        }

        dateStart_editText.setText(dateStart_cldr.get(Calendar.DAY_OF_MONTH)+ "/" +(dateStart_cldr.get(Calendar.MONTH)+1)+ "/" +dateStart_cldr.get(Calendar.YEAR));
        dateStart_editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar cldr = Calendar.getInstance();
                int day = cldr.get(Calendar.DAY_OF_MONTH);
                int month = cldr.get(Calendar.MONTH);
                int year = cldr.get(Calendar.YEAR);
                // date picker dialog
                picker1 = new DatePickerDialog(SearchActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                dateStart_editText.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                                dateStart_cldr.set(year, monthOfYear, dayOfMonth,0,0,0);
                                SharedPreferences.Editor editor = prefs.edit();
                                Calendar now = Calendar.getInstance();
                                boolean isToday= (now.get(Calendar.DAY_OF_MONTH)==dayOfMonth)
                                        && (now.get(Calendar.MONTH)==monthOfYear)
                                        && (now.get(Calendar.YEAR)==year);

                                editor.putBoolean("OnlyToday",isToday);
                                editor.putLong("dateStart",dateStart_cldr.getTimeInMillis());
                                editor.apply();
                            }
                        }, year, month, day);
                picker1.getDatePicker().setMaxDate(dateEnd_cldr.getTimeInMillis());
                picker1.show();
            }
        });


        dateEnd_editText=(EditText) findViewById(R.id.dateEnd_editText);
        dateEnd_editText.setInputType(InputType.TYPE_NULL);

        long dateEndmill= prefs.getLong("dateEnd",0l);

        if((dateEndmill!=0)&&!prefs.getBoolean("EndDateCurrent",true)){
            dateEnd_cldr.setTimeInMillis(dateEndmill);
        }else{
            dateEnd_cldr.set(Calendar.HOUR_OF_DAY,23);
            dateEnd_cldr.set(Calendar.MINUTE,59);
            dateEnd_cldr.set(Calendar.SECOND,59);
        }

        dateEnd_editText.setText(dateEnd_cldr.get(Calendar.DAY_OF_MONTH)+ "/" +(dateEnd_cldr.get(Calendar.MONTH)+1)+ "/" +dateEnd_cldr.get(Calendar.YEAR));
        dateEnd_editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar cldr = Calendar.getInstance();
                int day = cldr.get(Calendar.DAY_OF_MONTH);
                int month = cldr.get(Calendar.MONTH);
                int year = cldr.get(Calendar.YEAR);
                // date picker dialog
                picker2 = new DatePickerDialog(SearchActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                dateEnd_editText.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                                dateEnd_cldr.set(year, monthOfYear, dayOfMonth,23,59,59);
                                SharedPreferences.Editor editor = prefs.edit();
                                Calendar now = Calendar.getInstance();
                                boolean isToday= (now.get(Calendar.DAY_OF_MONTH)==dayOfMonth)
                                        && (now.get(Calendar.MONTH)==monthOfYear)
                                        && (now.get(Calendar.YEAR)==year);

                                editor.putBoolean("EndDateCurrent",isToday);
                                editor.putLong("dateEnd",dateEnd_cldr.getTimeInMillis());
                                editor.apply();
                            }
                        }, year, month, day);
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day);
                picker2.getDatePicker().setMinDate(dateStart_cldr.getTimeInMillis());
                picker2.getDatePicker().setMaxDate(calendar.getTimeInMillis());
                picker2.show();

                Set<String> set = prefs.getStringSet("friendsChecked",(new HashSet<String>()));
            }
        });

        List<FirebaseUser> users =new ArrayList<>();
        users.add(currentUser);

        LinearLayout Llout;
        final LinearLayout FriendsVL = findViewById(R.id.FriendsVL);
        TextView tv = new TextView(this);
        tv.setText("test");
        tv.setVisibility(View.VISIBLE);
        FriendsVL.addView(tv);
        db.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        CheckBox ch = new CheckBox(getBaseContext());

                        ch.setTag(currentUser.getUid());
                        ch.setText(document.getString("Display Name"));
                        final Set<String> set = prefs.getStringSet("friendsViewable",(new HashSet<String>()));
                        ch.setChecked(set.contains(currentUser.getUid()));
                        ch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                        {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                            {
                                final SharedPreferences.Editor editor = prefs.edit();
                                if ( isChecked )
                                {
                                    set.add(buttonView.getTag().toString());
                                }else
                                {
                                    set.remove(buttonView.getTag().toString());
                                }

                                if(set.isEmpty()){
                                    editor.remove("friendsViewable");
                                }else{
                                    editor.putStringSet("friendsViewable",set);
                                }

                                editor.apply();
                            }
                        });
                        FriendsVL.addView(ch);
                        List<String> friends = (List<String>)document.get("Friends");
                        if(friends!=null) {
                            for (String friend : friends) {
                                final String f = friend;
                                db.collection("users").document(friend).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document.exists()) {
                                                CheckBox ch = new CheckBox(getBaseContext());

                                                ch.setTag(f);
                                                ch.setText(document.getString("Display Name"));
                                                final Set<String> set = prefs.getStringSet("friendsViewable", (new HashSet<String>()));
                                                ch.setChecked(set.contains(f));
                                                ch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                    @Override
                                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                        final SharedPreferences.Editor editor = prefs.edit();
                                                        if (isChecked) {
                                                            set.add(buttonView.getTag().toString());
                                                        } else {
                                                            set.remove(buttonView.getTag().toString());
                                                        }

                                                        if (set.isEmpty()) {
                                                            editor.remove("friendsViewable");
                                                        } else {
                                                            editor.putStringSet("friendsViewable", set);
                                                        }

                                                        editor.apply();
                                                    }
                                                });
                                                FriendsVL.addView(ch);
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        });
    }
}
