package com.rcoem.enotice.enotice_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.github.javiersantos.bottomdialogs.BottomDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import es.dmoral.toasty.Toasty;

/**
 * Created by Akshat Shukla on 12-02-2017.
 */

public class AddTextNoticeFragment extends Fragment {
    View textView;
    private EditText title;
    private EditText desc;
    private Button btnsubmit;
    private Spinner spinnerText;

    private DatabaseReference mDatabase;
    private DatabaseReference mDataBaseDepartment;
    private FirebaseAuth mAuth;

    private ProgressDialog mProgress;
    private Calendar calendar;
    private TextView dateView;
    private int year, month, day;

    private String Approved;

    private String noticeType;

    private String [] typeArray =
            {       "For Teachers",
                    "For Students",
                    "Urgent",
                    "Normal",
                    "Assignment",
                    "Time Table"};

    Activity context;
    public AddTextNoticeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        context = getActivity();
        textView = inflater.inflate(R.layout.activity_add_text_notice, container, false);

        return textView;
    }

    public void onStart(){
        super.onStart();
        title = (EditText) context.findViewById(R.id.titleText);
        desc = (EditText) context.findViewById(R.id.descText);
        btnsubmit = (Button) context.findViewById(R.id.btnSubmitText);
        spinnerText = (Spinner) context.findViewById(R.id.spinnerText);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, typeArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerText.setAdapter(adapter);
        spinnerText.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                                               int arg2, long arg3) {

                        int position = spinnerText.getSelectedItemPosition();

                        noticeType = typeArray[+position];
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        // TODO Auto-generated method stub

                    }

                }
        );

        mAuth = FirebaseAuth.getInstance();
        mDataBaseDepartment = FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());

        btnsubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new BottomDialog.Builder(context)
                        .setTitle("Upload ("+noticeType+") Text Notice")
                        .setContent("Are you sure you want to submit it as your notice?")
                        .setPositiveText("Approve")
                        .setPositiveBackgroundColorResource(R.color.colorPrimary)
                        .setCancelable(false)
                        .setNegativeText("No")
                        .setPositiveTextColorResource(android.R.color.white)
                        //.setPositiveTextColor(ContextCompat.getColor(this, android.R.color.colorPrimary)
                        .onPositive(new BottomDialog.ButtonCallback() {
                            @Override
                            public void onClick(BottomDialog dialog) {
                                final String titletext = title.getText().toString().trim();
                                final String desctext =  desc.getText().toString().trim();
                                calendar = Calendar.getInstance();
                                year = calendar.get(Calendar.YEAR);

                                month = calendar.get(Calendar.MONTH) + 1;    //Month in Calendar API start with 0.
                                day = calendar.get(Calendar.DAY_OF_MONTH);
                                //  Toast.makeText(AddNoticeActivityAdmin.this,day + "/" + month + "/" + year, Toast.LENGTH_LONG).show();
                                final String currentDate = day + "/" + month + "/" + year;
                                final long currentLongTime = -1 * new Date().getTime();
                                final String currentTime = "" + currentLongTime;
                                if(!TextUtils.isEmpty(titletext) && !TextUtils.isEmpty(desctext)){

                                    final AlertDialog dialog1 = new SpotsDialog(getContext(), R.style.CustomProgress);
                                    dialog1.show();

                                    mDataBaseDepartment.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {

                                            final String Dept = dataSnapshot.child("department").getValue().toString().trim();
                                            final String lvlCheck = dataSnapshot.child("level").getValue().toString().trim();

                                            if (lvlCheck.equals("1")) {
                                                mDatabase  = FirebaseDatabase.getInstance().getReference().child("posts").child(dataSnapshot.child("department").getValue().toString().trim()).child("Pending").push();
                                                Approved = "false";
                                            }
                                            else if (lvlCheck.equals("2")){
                                                mDatabase  = FirebaseDatabase.getInstance().getReference().child("posts").child(dataSnapshot.child("department").getValue().toString().trim()).child("Approved").push();
                                                Approved = "true";
                                            }

                                            mDatabase.child("type").setValue(1);
                                            mDatabase.child("label").setValue(noticeType);
                                            mDatabase.child("title").setValue(titletext);
                                            mDatabase.child("Desc").setValue(desctext);
                                            mDatabase.child("UID").setValue(mAuth.getCurrentUser().getUid());
                                            mDatabase.child("email").setValue(mAuth.getCurrentUser().getEmail());
                                            mDatabase.child("username").setValue(dataSnapshot.child("name").getValue());
                                            mDatabase.child("profileImg").setValue(dataSnapshot.child("images").getValue());
                                            //Passing Default Text Image for Web App Viewing
                                            mDatabase.child("images").setValue("https://firebasestorage.googleapis.com/v0/b/e-notice-board-83d16.appspot.com/o/txt-file-symbol.png?alt=media&token=3a8beb43-561f-4f69-a6ad-58d2683abe81");
                                            mDatabase.child("time").setValue(currentDate);
                                            mDatabase.child("servertime").setValue(currentLongTime);
                                            mDatabase.child("link").setValue(null);
                                            mDatabase.child("department").setValue(dataSnapshot.child("department").getValue().toString().trim());
                                            mDatabase.child("approved").setValue(Approved);

                                            if (lvlCheck.equals("2")) {
                                                departmentPushDept(titletext,"Notice by HOD ".concat(dataSnapshot.child("name").getValue().toString()),Dept);
                                            }
                                            else if (lvlCheck.equals("1")) {
                                                departmentPushHOD(titletext,"Pending Notice Approval sent by ".concat(dataSnapshot.child("name").getValue().toString()),Dept);
                                            }

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            Toasty.error(context,"Connection Error").show();
                                        }

                                    });
                                    Toasty.success(context,"Successfully Posted").show();
                                    dialog1.dismiss();
                                    startActivity(new Intent(context,AddNoticeTabbed.class));
                                    getActivity().finish();
                                }
                                else if (TextUtils.isEmpty(titletext)) {
                                    Toasty.error(context,"Please Enter Title").show();
                                }
                                else if (TextUtils.isEmpty(desctext)) {
                                    Toasty.error(context,"Please Enter Description").show();
                                }
                            }
                        }).show();

            }
        });

    }

    private void departmentPushDept(final String t, final String m, final String dept) {
        final String title = t;
        final String message = m;
        final String email = "dhanajay@gmail.com";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, EndPoints.URL_SEND_SINGLE_PUSH_DEPT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // progressDialog.dismiss();

                        //Toast.makeText(AddNoticeActivityUser.this, response, Toast.LENGTH_LONG).show();
                        //Toasty.custom(getActivity().getApplicationContext(), "Department Teachers will be notified of your Notice", R.mipmap.ic_launcher, getResources().getColor(R.color.colorWhite), getResources().getColor(R.color.colorBg), 100, false, true).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("title", title);
                params.put("message", message);

                params.put("email", email);
                params.put("dept",dept);
                return params;
            }
        };

        MyVolley.getInstance(context).addToRequestQueue(stringRequest);
    }

    private void departmentPushHOD(final String t,final String m,final String dept) {
        final String title = t;
        final String message = m;
        final String email = "dhanajay@gmail.com";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, EndPoints.URL_SEND_SINGLE_PUSH_HOD,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // progressDialog.dismiss();

                        //Toast.makeText(AddNoticeActivityUser.this, response, Toast.LENGTH_LONG).show();
                        //Toasty.custom(getActivity().getApplicationContext(), "HOD will be notified of your Notice", R.mipmap.ic_launcher, getResources().getColor(R.color.colorWhite), getResources().getColor(R.color.colorBg), 100, false, true).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("title", title);
                params.put("message", message);

                params.put("email", email);
                params.put("dept",dept);
                return params;
            }
        };

        MyVolley.getInstance(context).addToRequestQueue(stringRequest);
    }

}
