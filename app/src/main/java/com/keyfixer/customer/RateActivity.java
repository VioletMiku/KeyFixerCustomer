package com.keyfixer.customer;

import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.keyfixer.customer.Common.Common;
import com.keyfixer.customer.Model.Rate;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class RateActivity extends AppCompatActivity implements View.OnClickListener {

    Button btnSkip, btnPostComment;
    MaterialRatingBar ratingBar;
    MaterialEditText edtComment;

    FirebaseDatabase database;
    DatabaseReference rateDetailRef, fixerInformationRef;

    double ratingStars = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate);
        Initializing();
    }

    private void Initializing() {
        database = FirebaseDatabase.getInstance();
        rateDetailRef = database.getReference(Common.rate_detail_tbl);
        fixerInformationRef = database.getReference(Common.fixer_inf_tbl);

        btnSkip = (Button) findViewById(R.id.btn_skip);
        btnPostComment = (Button) findViewById(R.id.btn_postcomment);
        ratingBar = (MaterialRatingBar) findViewById(R.id.ratingBar);
        edtComment = (MaterialEditText) findViewById(R.id.edtComment);

        ratingBar.setOnRatingChangeListener(new MaterialRatingBar.OnRatingChangeListener() {
            @Override
            public void onRatingChanged(MaterialRatingBar ratingBar , float rating) {
                ratingStars = rating;
            }
        });
        btnPostComment.setOnClickListener(this);
        btnSkip.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_skip:

                break;
            case R.id.btn_postcomment:
                SubmitRateDetail(Common.fixerid);
                break;
        }
    }

    private void SubmitRateDetail(final String fixerid) {
        final AlertDialog alertDialog = new SpotsDialog(this);
        alertDialog.show();

        Rate rate = new Rate();
        rate.setRate(String.valueOf(ratingStars));
        rate.setComment(edtComment.getText().toString());

        rateDetailRef.child(fixerid)
                .push().setValue(rate).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                rateDetailRef.child(fixerid).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        double averageStars = 0.0;
                        int count = 0;
                        for (DataSnapshot postsnapshot : dataSnapshot.getChildren()){
                            Rate rate = postsnapshot.getValue(Rate.class);
                            averageStars += Double.parseDouble(rate.getRate());
                            count ++;
                        }
                        double finalAverage = averageStars / count;
                        DecimalFormat decimalFormat = new DecimalFormat("#.#");
                        String valueUpdate = decimalFormat.format(finalAverage);

                        Map<String, Object> fixerUpdateRate = new HashMap<>();
                        fixerUpdateRate.put("rates", valueUpdate);

                        fixerInformationRef.child(Common.fixerid).updateChildren(fixerUpdateRate)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        alertDialog.dismiss();
                                        Toast.makeText(RateActivity.this , "Cảm ơn đã đánh giá!" , Toast.LENGTH_SHORT).show();
                                        Common.isFixDone = true;
                                        finish();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        alertDialog.dismiss();
                                        Toast.makeText(RateActivity.this , "Đánh giá thất bại, lỗi hệ thống" , Toast.LENGTH_SHORT).show();
                                        Log.e("ERROR","Rate updated but can't write to fixer information");
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                alertDialog.dismiss();
                Toast.makeText(RateActivity.this , "Đánh giá thất bại" , Toast.LENGTH_SHORT).show();
            }
        });
    }
}
