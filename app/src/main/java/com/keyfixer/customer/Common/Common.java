package com.keyfixer.customer.Common;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.keyfixer.customer.HomeActivity;
import com.keyfixer.customer.Model.DataMessage;
import com.keyfixer.customer.Model.FCMResponse;
import com.keyfixer.customer.Model.Token;
import com.keyfixer.customer.Model.User;
import com.keyfixer.customer.Remote.FCMClient;
import com.keyfixer.customer.Remote.IFCMService;
import com.keyfixer.customer.Remote.IGoogleAPI;
import com.keyfixer.customer.Remote.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Common {
    public static boolean isExitFromCallFixerUI = false;
    public static boolean isFixerFound = false;
    public static String fixerid = "";
    public static boolean isFixDone = false;
    public static final String user_field = "customer_username";
    public static final String pwd_field = "customer_password";
    public static User currentUser;
    public static String service_want_to_fix;
    public static Location mLastLocation;
    public static final String fixer_tbl = "Fixers";
    public static final String fixer_inf_tbl = "Users";
    public static final String customer_tbl = "Customers";
    public static final String fix_request_tbl = "FixRequest";
    public static final String token_tbl = "Tokens";
    public static final String rate_detail_tbl = "RateDetails";
    public static final String fcmUrl = "https://fcm.googleapis.com/";
    public static final int PICK_IMAGE_REQUEST = 9999;

    public static IFCMService getFCMService(){
        return FCMClient.getClient(fcmUrl).create(IFCMService.class);
    }

    public static void sendRequestToFixer(String fixerid , final IFCMService ifcmService , final Context context, final Location currentLocation) {
        final DatabaseReference token = FirebaseDatabase.getInstance().getReference(Common.token_tbl);
        token.orderByKey().equalTo(fixerid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot:dataSnapshot.getChildren()){
                    Token token1 = postSnapshot.getValue(Token.class); // get token object from database with key
                    //make raw payload - convert latlng to json
                    String customer_token = FirebaseInstanceId.getInstance().getToken();

                    Map<String, String> content = new HashMap<>();
                    content.put("customer", customer_token);
                    content.put("lat", String.valueOf(currentLocation.getLatitude()));
                    content.put("lng", String.valueOf(currentLocation.getLongitude()));
                    content.put("service", service_want_to_fix);
                    DataMessage dataMessage = new DataMessage(token1.getToken(), content);
                    ifcmService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call , Response<FCMResponse> response) {
                            if (response.body().success == 1)
                                Toast.makeText(context , "Đã gửi yêu cầu" , Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(context , "Gửi yêu cầu thất bại" , Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call , Throwable t) {
                            Log.e("ERROR",t.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
