package com.keyfixer.customer;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.keyfixer.customer.Common.Common;
import com.keyfixer.customer.Model.User;
import com.keyfixer.customer.Remote.IFCMService;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.keyfixer.customer.Common.Common.mLastLocation;

public class CallFixer extends AppCompatActivity implements View.OnClickListener {

    CircleImageView avatar_image;
    TextView txtFixerName, txtFixerPhone, txtFixerRates;
    Button btnCallByApp, btnCallByPhone, btnCancel;

    String fixerId;
    double lat, lng;
    Location mLastlocation;

    IFCMService ifcmService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_fixer);

        Initializing();
    }

    private void Initializing() {
        Common.isExitFromCallFixerUI = false;
        ifcmService = Common.getFCMService();
        //Init view
        avatar_image = (CircleImageView) findViewById(R.id.img_avatar);
        txtFixerName = (TextView) findViewById(R.id.txt_fixername);
        txtFixerPhone = (TextView) findViewById(R.id.txt_fixerphone);
        txtFixerRates = (TextView) findViewById(R.id.txt_rates);
        btnCallByApp = (Button) findViewById(R.id.btn_CallFixerbyapp);
        btnCallByPhone = (Button) findViewById(R.id.btn_CallFixerbyPhonenumber);
        btnCancel = (Button) findViewById(R.id.btnCancelRequest);

        btnCancel.setOnClickListener(this);
        btnCallByPhone.setOnClickListener(this);
        btnCallByApp.setOnClickListener(this);

        //get intent
        if (getIntent() != null){
            fixerId = getIntent().getStringExtra("fixerId");
            lat = getIntent().getDoubleExtra("lat", -1.0);
            lng = getIntent().getDoubleExtra("lng", -1.0);

            mLastlocation = new Location("");
            mLastlocation.setLatitude(lat);
            mLastlocation.setLongitude(lng);
            
            loadFixerInfo(fixerId);
        }
    }

    private void loadFixerInfo(final String fixerId) {
        FirebaseDatabase.getInstance().getReference(Common.fixer_inf_tbl).child(fixerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User fixer = dataSnapshot.getValue(User.class);
                if (!fixer.getAvatarUrl().isEmpty())
                    Picasso.with(getBaseContext()).load(fixer.getAvatarUrl()).into(avatar_image);
                txtFixerName.setText(fixer.getStrName());
                txtFixerPhone.setText(fixer.getStrPhone());
                txtFixerRates.setText(fixer.getRates());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode , KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            Common.isExitFromCallFixerUI = true;
            Intent intent = new Intent(CallFixer.this, HomeActivity.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_CallFixerbyapp:
                if (fixerId != null && !fixerId.isEmpty())
                    Common.sendRequestToFixer(fixerId, ifcmService, this, mLastlocation);
                break;
            case R.id.btn_CallFixerbyPhonenumber:
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + txtFixerPhone.getText().toString()));
                startActivity(intent);
                break;
            case R.id.btnCancelRequest:
                Intent intentHome = new Intent(CallFixer.this, HomeActivity.class);
                if (fixerId != null && !TextUtils.isEmpty(fixerId))
                    Common.sendCancelToFixer(fixerId, ifcmService, this);
                Common.isFixDone = true;
                startActivity(intentHome);
                break;
        }
    }

}
