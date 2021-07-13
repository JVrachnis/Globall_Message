package com.example.test1.ui.findFriend;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.test1.R;
import com.example.test1.seeProfile;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


public class findFriendFragment extends Fragment {
    private static final String TAG = "findFriend";

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root =inflater.inflate(R.layout.fragment_find_friend, container, false);
        EditText searchEditText =root.findViewById(R.id.searchET);
        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(final Editable s) {
                final String search=s.toString().toLowerCase();
                final LinearLayout searchResultLL = root.findViewById(R.id.searchResultLL);
                searchResultLL.removeAllViews();
                if(!search.isEmpty()){
                    db.collection("users").get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    searchResultLL.removeAllViews();

                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        String DisplayName =document.getString("Display Name");
                                        String DisplayNameL=DisplayName.toLowerCase();
                                        String FirstNameL=document.getString("First Name").toLowerCase();
                                        String LastNameL=document.getString("Last Name").toLowerCase();
                                        boolean dnb=DisplayNameL.contains(search);
                                        boolean fnb=FirstNameL.contains(search);
                                        boolean lnb=LastNameL.contains(search);
                                        if(dnb||fnb||lnb){
                                        TextView tv = new TextView(getActivity());
                                        tv.setText(DisplayName);
                                        tv.setTag(document.getId());
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
                                        searchResultLL.addView(tv);
                                    }
                                    }
                                } else {
                                    Log.d(TAG, "Error getting documents: ", task.getException());
                                }
                            }
                        });
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start,int before, int count) {}
        });
        return root;
    }
}
